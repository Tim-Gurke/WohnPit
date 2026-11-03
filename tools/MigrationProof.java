package de.wattpit.app;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.wattpit.app.core.CsvCodec;
import de.wattpit.app.core.Reading;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Actual legacy CSV and diagnostic export, separate old/new SQLite databases. */
public final class MigrationProof {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
    private static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static JSONObject energy(SolarPlanning plan,long from,long to)throws Exception{return plan.energyFlow(from,to,from,to,"all");}
    private static byte[] zip(JSONObject diagnosis)throws Exception{ByteArrayOutputStream out=new ByteArrayOutputStream();try(ZipOutputStream zip=new ZipOutputStream(out)){zip.putNextEntry(new ZipEntry("diagnose.json"));zip.write(diagnosis.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();zip.putNextEntry(new ZipEntry("verlauf.csv"));zip.write("intentionally not a second measurement import".getBytes(StandardCharsets.UTF_8));zip.closeEntry();}return out.toByteArray();}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path root=Path.of(args[0]);
        long hour=3600000L,from=(System.currentTimeMillis()/hour-30)*hour,to=from+hour;
        LanSyncProof.TestContext oldContext=new LanSyncProof.TestContext(root.resolve("old-wattnpit"));Repository old=LanSyncProof.repository(oldContext);
        String devices="[{\"mac\":\"AA:11:22:33:44:55\",\"kind\":\"solar\",\"name\":\"Solar\"},{\"mac\":\"AA:11:22:33:44:66\",\"kind\":\"shunt\",\"name\":\"Batterie\"}]";
        old.prefs.edit().putString("devices",devices).commit();
        JSONObject equipment=Repository.checkedEquipment(new JSONObject("{\"version\":1,\"groups\":[{\"id\":\"fixed\",\"label\":\"Fixed\",\"count\":2,\"watts\":100,\"placement\":\"fixed\",\"measurement\":\"solar\",\"tilt\":0},{\"id\":\"mobile\",\"label\":\"Dokio\",\"count\":1,\"watts\":100,\"placement\":\"mobile\",\"measurement\":\"shunt\",\"controller\":\"Dokio PWM\",\"tilt\":0}]}"));
        JSONObject setup=SolarPlanning.checked(new JSONObject(),equipment),place=new JSONObject("{\"name\":\"Test site\",\"latitude\":52.5,\"longitude\":13.4}");
        for(String id:new String[]{"fixed","mobile"})setup.getJSONObject("groups").getJSONObject(id).put("active",true).put("shade",new JSONArray("[0,0,0]"));
        old.prefs.edit().putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",setup.toString()).putString("weatherPlace",place.toString()).commit();
        RollerLedger oldLedger=new RollerLedger(oldContext,"wattpit-roller-input.db");SolarPlanning oldPlan=new SolarPlanning(oldContext,old,value->{},"wattpit-planning.db",oldLedger);
        JSONObject historic=new JSONObject().put("equipment",equipment).put("setup",setup).put("place",place);String source=RollerLedger.source(old.prefs);
        SQLiteDatabase oldDb=oldPlan.getWritableDatabase();
        oldDb.execSQL("INSERT INTO display_contexts(source,at,payload) VALUES(?,?,?)",new Object[]{source,from-1,historic.toString()});
        oldDb.execSQL("INSERT INTO model_weather(place,geometry,end,value,slot,night,sunrise,sunset) VALUES(?,?,?,1000,1,0,0,0)",new Object[]{"52500:13400","0:0",to});
        for(long at=from;at<=to;at+=hour/2){Reading solar=new Reading();solar.device="AA:11:22:33:44:55";solar.kind="solar";solar.origin="archive";solar.timestamp=at;solar.period=1800;solar.solar=155.0;old.db.insert(solar);Reading battery=new Reading();battery.device="AA:11:22:33:44:66";battery.kind="shunt";battery.origin="archive";battery.timestamp=at;battery.period=1800;battery.power=49.0;battery.voltage=13.35;battery.current=49.0/13.35;old.db.insert(battery);}
        JSONObject before=energy(oldPlan,from,to);
        check(Math.abs(before.getJSONObject("consumption").getJSONObject("summary").getDouble("last")-166)<1e-7,"old recorded setup and weather produce 155 + 60 - 49 = 166 Wh consumption");
        JSONObject profile=new JSONObject().put("kind","solar").put("at",from-1000).put("values",new JSONObject().put("absorptionVoltage",14.2));old.db.deviceSettings("AA:11:22:33:44:55",profile);
        oldLedger.add(source,"AA:22:33:44:55:66",from-600000,from-600000,from-598000,1.25);
        ByteArrayOutputStream csv=new ByteArrayOutputStream();old.db.exportTo(csv);
        JSONObject diagnostic=oldPlan.diagnosticSnapshot().get().put("appVersion","1.8.1");
        LanSyncProof.TestContext nextContext=new LanSyncProof.TestContext(root.resolve("wohnpit"));Repository next=LanSyncProof.repository(nextContext);
        JSONObject current=new JSONObject(setup.toString());current.getJSONObject("groups").getJSONObject("mobile").put("active",false);
        next.prefs.edit().putString("devices",devices).putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",current.toString()).putString("weatherPlace",place.toString()).putFloat("capacity",130).putInt("sampleSeconds",30).putString("credentials-sentinel","do-not-touch").commit();
        CsvCodec.Table table=CsvCodec.parse(csv.toString(StandardCharsets.UTF_8));int[] imported=next.db.importTable(table,CsvCodec.guess(table.headers),"solar");
        RollerLedger nextLedger=new RollerLedger(nextContext,"wattpit-roller-input.db");SolarPlanning nextPlan=new SolarPlanning(nextContext,next,value->{},"wattpit-planning.db",nextLedger);
        JSONObject present=new JSONObject(historic.toString()).put("setup",current);long presentAt=System.currentTimeMillis();
        nextPlan.getWritableDatabase().execSQL("INSERT INTO display_contexts(source,at,payload) VALUES(?,?,?)",new Object[]{source,presentAt,present.toString()});
        try{
            JSONObject afterCsv=energy(nextPlan,from,to);
            check(imported[0]==6&&count(next.db.getReadableDatabase(),"readings")==6,"native CSV restores all solar and shunt rows including their original device and source");
            check(Math.abs(afterCsv.getJSONObject("solar").getJSONObject("summary").getDouble("last")-155)<1e-7&&Math.abs(afterCsv.getJSONObject("balance").getJSONObject("summary").getDouble("last")-49)<1e-7,"solar and battery histories remain present after the ordinary CSV migration");
            check(afterCsv.getJSONObject("consumption").getJSONObject("summary").isNull("last"),"reproduced: CSV alone leaves historical consumption unknown because its earlier setup/weather are missing");
            Map<String,?> preferences=new HashMap<>(next.prefs.getAll());
            JSONObject parsed=HistoryMigration.readZip(new ByteArrayInputStream(zip(diagnostic)));
            HistoryMigration.Result restored=HistoryMigration.restore(next,nextPlan,nextLedger,parsed);
            JSONObject after=energy(nextPlan,from,to);
            check(Math.abs(after.getJSONObject("consumption").getJSONObject("summary").getDouble("last")-166)<1e-7,"existing diagnostic data restores the original 166 Wh historic consumption without a new calculation algorithm");
            check(Math.abs(after.getJSONObject("mobile").getJSONObject("summary").getDouble("last")-60)<1e-7,"the formerly active PWM panel is reconstructed from its recorded setup and model weather");
            check(count(next.db.getReadableDatabase(),"readings")==6,"restoring calculation inputs never changes or reimports measurement rows from the ZIP");
            check(preferences.equals(next.prefs.getAll()),"current equipment activation, device credentials, battery capacity and sample interval remain untouched");
            check(count(nextPlan.getReadableDatabase(),"display_contexts")==2,"the recent current setup is retained alongside the historic setup");
            check(restored.tables.getInt("device_settings")==1&&restored.tables.getInt("input_intervals")==1,"historical device profiles and observed roller charging intervals are carried over");
            check(Math.abs(nextLedger.read(source,from-600000,from).getJSONObject(0).getDouble("wh")-1.25)<1e-7,"the previous roller input retains its measured interval and energy");
            long revision=nextPlan.modelRevision();HistoryMigration.Result repeated=HistoryMigration.restore(next,nextPlan,nextLedger,parsed);
            check(repeated.inserted==0&&nextPlan.modelRevision()==revision,"repeating the import is idempotent and does not duplicate or revise existing historic inputs");
            JSONObject conflicting=new JSONObject(parsed.toString());conflicting.getJSONObject("mobileDisplay").getJSONArray("weather").getJSONObject(0).put("value",2000);
            HistoryMigration.restore(next,nextPlan,nextLedger,conflicting);
            check(Math.abs(energy(nextPlan,from,to).getJSONObject("consumption").getJSONObject("summary").getDouble("last")-166)<1e-7,"a conflicting diagnostic row cannot overwrite a model-weather value already stored on the new central");
            JSONObject foreign=new JSONObject(parsed.toString()).put("source","not-the-same-plant");boolean rejected=false;
            try{HistoryMigration.restore(next,nextPlan,nextLedger,foreign);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected&&nextPlan.modelRevision()==revision,"another plant is rejected before modifying any historical input");
            JSONObject invalid=new JSONObject(parsed.toString());invalid.getJSONObject("mobileDisplay").getJSONArray("contexts").getJSONObject(0).put("at",from-2);invalid.getJSONObject("mobileDisplay").getJSONArray("weather").put(new JSONObject(invalid.getJSONObject("mobileDisplay").getJSONArray("weather").getJSONObject(0).toString()).put("value",-1));rejected=false;
            try{HistoryMigration.restore(next,nextPlan,nextLedger,invalid);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected&&count(nextPlan.getReadableDatabase(),"display_contexts")==2,"a late malformed record is validated before any otherwise valid historical context is written");
            next.running=true;rejected=false;try{HistoryMigration.restore(next,nextPlan,nextLedger,parsed);}catch(IllegalArgumentException expected){rejected=true;}finally{next.running=false;}
            check(rejected&&nextPlan.modelRevision()==revision,"migration requires paused recording and cannot change a running central");
            next.archiveBusy=true;rejected=false;try{HistoryMigration.restore(next,nextPlan,nextLedger,parsed);}catch(IllegalArgumentException expected){rejected=true;}finally{next.archiveBusy=false;}
            check(rejected,"an active device archive import must finish before historical inputs can be merged");
            JSONObject empty=new JSONObject(parsed.toString());empty.put("contexts",new JSONArray());empty.getJSONObject("mobileDisplay").put("contexts",new JSONArray());rejected=false;
            try{HistoryMigration.restore(next,nextPlan,nextLedger,empty);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"a diagnosis without historical setups cannot report a successful historical-consumption recovery");
            JSONObject legacy=new JSONObject(parsed.toString());legacy.getJSONObject("mobileDisplay").put("contexts",new JSONArray());legacy.put("contexts",new JSONArray().put(new JSONObject().put("at",from-3).put("key","legacy-context").put("solarKey","legacy-solar").put("mobileFree",false).put("payload",historic)));
            check(HistoryMigration.restore(next,nextPlan,nextLedger,legacy).tables.getInt("contexts")==1,"the older recorded context table is restored when the source has no native display-context entry");
            ByteArrayOutputStream wrongZip=new ByteArrayOutputStream();try(ZipOutputStream z=new ZipOutputStream(wrongZip)){z.putNextEntry(new ZipEntry("../diagnose.json"));z.write(parsed.toString().getBytes(StandardCharsets.UTF_8));z.closeEntry();}rejected=false;
            try{HistoryMigration.readZip(new ByteArrayInputStream(wrongZip.toByteArray()));}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"ZIP paths are never extracted or treated as the official root diagnosis file");
            check(!next.running&&!next.archiveBusy&&preferences.equals(next.prefs.getAll()),"all import scenarios leave recording paused and the current device configuration intact");
        }finally{nextPlan.shutdown();oldPlan.shutdown();nextPlan.close();oldPlan.close();nextLedger.close();oldLedger.close();}
    }
}
