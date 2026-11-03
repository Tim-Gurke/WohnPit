package de.wattpit.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Restore existing calculation inputs from the existing Watt'n'Pit diagnostic export.
 * Does not change current equipment, credentials, measurements or BMS ownership. */
final class HistoryMigration {
    static final int MAX_JSON_BYTES=32*1024*1024;
    private static final int MAX_ROWS=250000;
    private static final long DAY=86400000L;
    private static final String INVALID="Keine unterstützte Watt’n’Pit-Mess- und Lerndiagnose.";
    private static final class Table {
        final String name;
        final List<ContentValues> rows=new ArrayList<>();
        Table(String name){this.name=name;}
    }
    static final class Result {
        final int inserted,existing;
        final JSONObject tables;
        Result(int inserted,int existing,JSONObject tables){this.inserted=inserted;this.existing=existing;this.tables=tables;}
    }

    static JSONObject readZip(InputStream input)throws Exception{
        try(ZipInputStream zip=new ZipInputStream(input)){
            byte[] bytes=new byte[8192];long scanned=0;ZipEntry entry;
            while((entry=zip.getNextEntry())!=null){
                if(!entry.isDirectory()&&entry.getName().equals("diagnose.json")){
                    try(ByteArrayOutputStream out=new ByteArrayOutputStream()){
                        int n;while((n=zip.read(bytes))!=-1){
                            if(out.size()+n>MAX_JSON_BYTES)throw new IllegalArgumentException("Die Diagnosedaten dürfen höchstens 32 MB groß sein.");
                            out.write(bytes,0,n);
                        }
                        try{return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}
                        catch(JSONException error){throw new IllegalArgumentException(INVALID);}
                    }
                }
                int n;while((n=zip.read(bytes))!=-1){scanned+=n;if(scanned>2L*MAX_JSON_BYTES)throw new IllegalArgumentException(INVALID);}
                zip.closeEntry();
            }
        }
        throw new IllegalArgumentException("In dieser ZIP-Datei fehlt diagnose.json. Bitte die Mess- und Lerndiagnose aus Watt’n’Pit auswählen.");
    }
    private static long time(JSONObject row,String key)throws JSONException{
        Object raw=row.get(key);if(!(raw instanceof Number))throw new IllegalArgumentException("Ungültiger Zeitpunkt in der Diagnose.");
        double value=((Number)raw).doubleValue();
        if(!Double.isFinite(value)||value<0||value!=Math.rint(value)||value>4102444800000.0)throw new IllegalArgumentException("Ungültiger Zeitpunkt in der Diagnose.");
        return ((Number)raw).longValue();
    }
    private static boolean flag(JSONObject row,String key)throws JSONException{
        if(!row.has(key))return false;Object value=row.get(key);
        if(value instanceof Boolean)return (Boolean)value;
        if(value instanceof Number&&(((Number)value).doubleValue()==0||((Number)value).doubleValue()==1))return ((Number)value).intValue()==1;
        throw new IllegalArgumentException("Ungültiger Status in der Diagnose.");
    }
    private static String text(JSONObject row,String key,int maximum)throws JSONException{
        String value=row.getString(key);if(value.length()>maximum)throw new IllegalArgumentException("Ein Diagnoseeintrag ist zu groß.");return value;
    }
    private static String context(JSONObject payload)throws Exception{
        JSONObject equipment=Repository.checkedEquipment(payload.getJSONObject("equipment"));
        JSONObject setup=payload.getJSONObject("setup");SolarPlanning.checked(setup,equipment);
        JSONObject groups=setup.getJSONObject("groups");JSONArray circles=equipment.getJSONArray("groups");
        for(int i=0;i<circles.length();i++)if(groups.optJSONObject(circles.getJSONObject(i).getString("id"))==null)throw new IllegalArgumentException("Eine historische Aufstellung ist unvollständig.");
        JSONObject place=payload.optJSONObject("place");if(place!=null&&place.length()>0)WeatherClient.checkedPlace(place);
        String value=payload.toString();if(value.length()>128000)throw new IllegalArgumentException("Eine historische Aufstellung ist zu groß.");
        return value;
    }
    private static void add(Table table,ContentValues row,int[] count){
        if(++count[0]>MAX_ROWS)throw new IllegalArgumentException("Die Diagnose enthält zu viele Zeitabschnitte.");table.rows.add(row);
    }
    private static JSONArray rows(JSONObject parent,String key)throws JSONException{
        if(!parent.has(key))return new JSONArray();return parent.getJSONArray(key);
    }
    private static void paused(Repository repo){
        if(repo.running||repo.archiveBusy)throw new IllegalArgumentException("Bitte zuerst die Aufzeichnung pausieren und einen laufenden Archivabruf beenden.");
    }

    static Result restore(Repository repo,SolarPlanning plan,RollerLedger ledger,JSONObject input)throws Exception{
        synchronized(repo){
            paused(repo);
            if(!input.optString("format").equals("Watt-n-Pit-Diagnose")||input.optInt("version")!=2)throw new IllegalArgumentException(INVALID);
            String source=RollerLedger.source(repo.prefs);
            if(!source.equals(input.optString("source"))||repo.configs().length()==0)throw new IllegalArgumentException("Die Diagnose gehört zu anderen Energiegeräten. Bitte SmartSolar und SmartShunt mit denselben Bluetooth-Adressen wie bisher einrichten.");
            long issued=time(input,"at");
            if(issued<=0||issued>System.currentTimeMillis()+300000)throw new IllegalArgumentException("Der Zeitpunkt dieser Diagnose ist ungültig.");
            JSONObject mobile=input.getJSONObject("mobileDisplay");
            if(mobile.optInt("version")!=2)throw new IllegalArgumentException(INVALID);

            Table contexts=new Table("contexts"),display=new Table("display_contexts"),weather=new Table("model_weather"),profiles=new Table("device_settings"),roller=new Table("input_intervals");
            List<Table> tables=Arrays.asList(contexts,display,weather,profiles,roller);int[] count={0};
            JSONArray given=rows(input,"contexts");
            for(int i=0;i<given.length();i++){
                JSONObject row=given.getJSONObject(i),payload=row.getJSONObject("payload");
                // Very old key-only records are not evidence for an equipment configuration.
                if(!payload.has("equipment")||!payload.has("setup"))continue;
                long at=time(row,"at");if(at<=0||at>issued+300000)throw new IllegalArgumentException("Eine historische Aufstellung liegt außerhalb des Diagnosezeitraums.");
                ContentValues value=new ContentValues();value.put("at",at);value.put("source",source);value.put("key",text(row,"key",200));
                value.put("mobile_free",flag(row,"mobileFree")?1:0);value.put("solar_key",row.has("solarKey")?text(row,"solarKey",200):"");value.put("payload",context(payload));add(contexts,value,count);
            }
            given=rows(mobile,"contexts");
            for(int i=0;i<given.length();i++){
                JSONObject row=given.getJSONObject(i);long at=time(row,"at");
                if(at<=0||at>issued+300000)throw new IllegalArgumentException("Eine historische Aufstellung liegt außerhalb des Diagnosezeitraums.");
                ContentValues value=new ContentValues();value.put("source",source);value.put("at",at);value.put("payload",context(row.getJSONObject("payload")));add(display,value,count);
            }
            if(contexts.rows.isEmpty()&&display.rows.isEmpty())throw new IllegalArgumentException("Diese Diagnose enthält keine früheren Aufstellungen. Bitte den Export auf der bisherigen G5-Zentrale durchführen.");
            given=rows(mobile,"weather");
            for(int i=0;i<given.length();i++){
                JSONObject row=given.getJSONObject(i);long end=time(row,"end");double watts=row.getDouble("value");int slot=row.getInt("slot");
                if(end<=0||end>issued+17*DAY||!Double.isFinite(watts)||watts<0||slot<0||slot>2)throw new IllegalArgumentException("Ungültige historische Wetterdaten.");
                ContentValues value=new ContentValues();value.put("place",text(row,"place",100));value.put("geometry",text(row,"geometry",80));value.put("end",end);value.put("value",watts);value.put("slot",slot);
                value.put("night",flag(row,"night")?1:0);value.put("sunrise",row.has("sunrise")?time(row,"sunrise"):0);value.put("sunset",row.has("sunset")?time(row,"sunset"):0);add(weather,value,count);
            }
            Map<String,String> devices=new HashMap<>();JSONArray configured=repo.configs();
            for(int i=0;i<configured.length();i++){JSONObject d=configured.getJSONObject(i);devices.put(d.getString("mac"),d.getString("kind"));}
            given=rows(input,"devices");
            for(int i=0;i<given.length();i++){
                JSONObject device=given.getJSONObject(i);String address=device.getString("device");
                if(!device.getString("kind").equals(devices.get(address)))throw new IllegalArgumentException("Ein Diagnose-Gerät passt nicht zur eingerichteten Anlage.");
                JSONArray history=rows(device,"profiles");
                for(int j=0;j<history.length();j++){
                    JSONObject row=history.getJSONObject(j);long start=time(row,"since"),end=time(row,"until"),at=time(row,"at");
                    if(start<=0||end<start||end>issued+300000||at>end||!row.getString("kind").equals(device.getString("kind")))throw new IllegalArgumentException("Ungültiges historisches Geräteprofil.");
                    row.getJSONObject("values");
                    JSONObject payload=new JSONObject(row.toString());payload.remove("since");payload.remove("until");
                    ContentValues value=new ContentValues();value.put("device",address);value.put("start",start);value.put("end",end);value.put("payload",payload.toString());add(profiles,value,count);
                }
            }
            given=rows(input,"rollerInputIntervals");
            for(int i=0;i<given.length();i++){
                JSONObject row=given.getJSONObject(i);long from=time(row,"from"),to=time(row,"to");double wh=row.getDouble("wh");
                if(from<=0||to<=from||to>issued+300000||!Double.isFinite(wh)||wh<0)throw new IllegalArgumentException("Ungültiger historischer Rollerabschnitt.");
                ContentValues value=new ContentValues();value.put("source",source);value.put("start",from);value.put("end",to);value.put("wh",wh);value.put("mac","");value.put("session",0L);add(roller,value,count);
            }

            // Validate everything before touching any existing database.
            List<SQLiteDatabase> transactions=new ArrayList<>();int inserted=0,existing=0;JSONObject counts=new JSONObject();
            try{
                for(Table table:tables)if(!table.rows.isEmpty()){
                    SQLiteDatabase db=AnalysisExchange.database(table.name,repo,plan,ledger,true);
                    if(!transactions.contains(db)){db.beginTransaction();transactions.add(db);}
                }
                for(Table table:tables){
                    int changed=0;
                    if(!table.rows.isEmpty()){
                        SQLiteDatabase db=AnalysisExchange.database(table.name,repo,plan,ledger,true);
                        for(ContentValues row:table.rows){
                            if(table.name.equals("input_intervals")){
                                try(Cursor c=db.rawQuery("SELECT 1 FROM input_intervals WHERE source=? AND end>? AND start<? LIMIT 1",new String[]{source,row.get("start").toString(),row.get("end").toString()})){
                                    if(c.moveToFirst()){existing++;continue;}
                                }
                            }
                            if(db.insertWithOnConflict(table.name,null,row,SQLiteDatabase.CONFLICT_IGNORE)==-1)existing++;else{inserted++;changed++;}
                        }
                    }
                    counts.put(table.name,changed);
                }
                paused(repo);if(!source.equals(RollerLedger.source(repo.prefs)))throw new IllegalArgumentException("Die eingerichteten Geräte wurden inzwischen geändert. Bitte erneut versuchen.");
                for(SQLiteDatabase db:transactions)db.setTransactionSuccessful();
            }finally{
                RuntimeException failure=null;
                for(int i=transactions.size()-1;i>=0;i--)try{transactions.get(i).endTransaction();}catch(RuntimeException e){if(failure==null)failure=e;else failure.addSuppressed(e);}
                if(failure!=null)throw failure;
            }
            if(inserted>0){repo.db.clearDisplayPrior();repo.invalidateStats();}
            return new Result(inserted,existing,counts);
        }
    }
    private HistoryMigration(){}
}
