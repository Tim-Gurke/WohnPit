package de.wattpit.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Real diagnostic export, persistent learning tables, and two already paired TCP viewers. */
public final class LearningMigrationProof {
    private static final long HOUR=3600000L,DAY=24*HOUR;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS "+label);}
    private static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static void insert(SQLiteDatabase db,String table,ContentValues value){if(db.insertWithOnConflict(table,null,value,SQLiteDatabase.CONFLICT_IGNORE)<0)throw new AssertionError("Fixture insert failed: "+table);}
    private static ContentValues observation(String source,JSONObject row)throws Exception{ContentValues v=new ContentValues();v.put("source",source);v.put("start",row.getLong("from"));v.put("end",row.getLong("to"));v.put("payload",row.toString());return v;}
    private static String invitation(LanSync host)throws Exception{Field c=LanSync.class.getDeclaredField("config"),s=LanSync.class.getDeclaredField("server");c.setAccessible(true);s.setAccessible(true);return de.wattpit.app.core.LanWire.encodeInvitation(new JSONObject(c.get(host).toString()).put("port",((java.net.ServerSocket)s.get(host)).getLocalPort()).put("addresses",new JSONArray().put("127.0.0.1")).toString());}
    private static JSONObject learning(long from,int slot,String key,String solar,String system,boolean away)throws Exception{
        long duration=(slot==0?11:slot==1?5:8)*HOUR,to=from+duration;
        JSONObject usage=new JSONObject().put("wh",duration*10.0/HOUR).put("duration",duration).put("estimatedDuration",0).put("estimatedWh",0).put("reconstructedDuration",0).put("reconstructedWh",0).put("measuredWh",duration*10.0/HOUR).put("latest",to).put("rollerAdjustedDuration",0).put("rollerInputWh",0);
        return new JSONObject().put("from",from).put("to",to).put("day",java.time.Instant.ofEpochMilli(from).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()).put("slot",slot).put("key",key).put("solarKey",solar).put("systemKey",system).put("learningVersion",2).put("mobileFree",true).put("solarEligible",true).put("portableEligible",true).put("solarCoverage",1).put("coverage",1).put("solar",duration*100.0/HOUR).put("usage",usage).put("usageByPresence",new JSONObject().put(away?"away":"home",new JSONObject(usage.toString())));
    }
    public static void main(String[] args){try{run(args);System.exit(0);}catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
    private static void run(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Path out=Path.of(args[0]);Files.createDirectories(out);
        long now=System.currentTimeMillis(),today=now/DAY*DAY;
        LanSyncProof.TestContext oldContext=new LanSyncProof.TestContext(out.resolve("old"));Repository old=LanSyncProof.repository(oldContext);
        String devices="[{\"mac\":\"AA:11:22:33:44:55\",\"kind\":\"solar\",\"name\":\"Solar\"},{\"mac\":\"AA:11:22:33:44:66\",\"kind\":\"shunt\",\"name\":\"Batterie\"}]";
        JSONObject equipment=Repository.checkedEquipment(new JSONObject("{\"version\":1,\"groups\":[{\"id\":\"fixed\",\"label\":\"Fest\",\"count\":2,\"watts\":100,\"placement\":\"fixed\",\"measurement\":\"solar\",\"tilt\":0}]}"));
        JSONObject setup=SolarPlanning.checked(new JSONObject(),equipment);setup.getJSONObject("groups").getJSONObject("fixed").put("shade",new JSONArray("[0,0,0]"));
        JSONObject place=new JSONObject("{\"latitude\":52.5,\"longitude\":13.4,\"name\":\"Testort\"}"),weather=new JSONObject().put("version",5).put("timezone","UTC").put("place",place).put("days",new JSONArray()).put("hours",new JSONArray()).put("solar",new JSONArray()).put("windows",new JSONArray()).put("signature",equipment+"|"+setup);
        old.prefs.edit().putString("devices",devices).putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",setup.toString()).putString("weatherPlace",place.toString()).putString("weatherCache",weather.toString()).commit();
        String source=RollerLedger.source(old.prefs),key=SolarKeys.key(equipment,setup,place,source,false,false),solar=SolarKeys.key(equipment,setup,place,source,true,false),system=SolarKeys.key(equipment,setup,new JSONObject("{\"latitude\":0,\"longitude\":0}"),source,true,true);
        JSONObject context=new JSONObject().put("equipment",equipment).put("setup",setup).put("place",place).put("systemKey",system);
        RollerLedger oldLedger=new RollerLedger(oldContext,"wattpit-roller-input.db");SolarPlanning oldPlan=new SolarPlanning(oldContext,old,v->{},"wattpit-planning.db",oldLedger);SQLiteDatabase oldDb=oldPlan.getWritableDatabase();
        ContentValues ctx=new ContentValues();ctx.put("at",today-500*DAY);ctx.put("source",source);ctx.put("key",key);ctx.put("solar_key",solar);ctx.put("mobile_free",1);ctx.put("payload",context.toString());insert(oldDb,"contexts",ctx);
        JSONArray rows=new JSONArray();
        for(int day=1;day<=22;day++){
            long date=today-(day<=20?day:day==21?400:401)*DAY;boolean away=day==22;
            for(int slot=0;slot<3;slot++){
                JSONObject row=learning(date+(slot==0?0:slot==1?11:16)*HOUR,slot,key,solar,system,away);rows.put(row);insert(oldDb,"observations",observation(source,row));
                JSONObject forecast=new JSONObject().put("key",solar).put("systemKey",system).put("from",row.getLong("from")).put("to",row.getLong("to")).put("issued",row.getLong("from")-2*DAY).put("wh",row.getDouble("solar"));
                ContentValues f=new ContentValues();f.put("key",solar);f.put("start",forecast.getLong("from"));f.put("end",forecast.getLong("to"));f.put("issued",forecast.getLong("issued"));f.put("payload",forecast.toString());insert(oldDb,"forecasts",f);
            }
        }
        long confirmedFrom=today-20*DAY;String day=java.time.Instant.ofEpochMilli(confirmedFrom).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString();
        JSONObject archived=new JSONObject(weather.toString()).put("complete",true).put("days",new JSONArray().put(new JSONObject().put("day",day).put("sunrise",confirmedFrom+6*HOUR).put("sunset",confirmedFrom+18*HOUR)));
        ContentValues confirmed=new ContentValues();confirmed.put("source",source);confirmed.put("id","previously-approved");confirmed.put("start",confirmedFrom);confirmed.put("end",confirmedFrom+DAY);confirmed.put("key",key);confirmed.put("solar_key",solar);confirmed.put("payload",context.toString());confirmed.put("weather",archived.toString());insert(oldDb,"confirmed_setups",confirmed);
        JSONObject oldKnowledge=oldPlan.calculateKnowledge(),diagnostic=oldPlan.diagnosticSnapshot().get();
        check(diagnostic.getJSONArray("observations").length()==66&&diagnostic.getJSONArray("forecasts").length()==66,"the actual diagnostic export contains the old learning observations and original forecast comparisons");
        LanSyncProof.TestContext nextContext=new LanSyncProof.TestContext(out.resolve("central"));Repository next=LanSyncProof.repository(nextContext);
        next.prefs.edit().putString("devices",devices).putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",setup.toString()).putString("weatherPlace",place.toString()).putString("weatherCache",weather.toString()).putFloat("capacity",130).putInt("archiveIntervalMinutes",60).putString("credentials-sentinel","keep-private").commit();
        RollerLedger nextLedger=new RollerLedger(nextContext,"wattpit-roller-input.db");SolarPlanning nextPlan=new SolarPlanning(nextContext,next,v->{},"wattpit-planning.db",nextLedger);SQLiteDatabase nextDb=nextPlan.getWritableDatabase();insert(nextDb,"contexts",ctx);
        for(int i=0;i<15;i++)insert(nextDb,"observations",observation(source,rows.getJSONObject(i)));
        confirmed.put("weather",new JSONObject(weather.toString()).put("complete",false).toString());insert(nextDb,"confirmed_setups",confirmed);
        JSONObject before=nextPlan.calculateKnowledge();check(before.getJSONArray("observations").length()==15,"reproduced: a migrated central retains only five recent days of learned data instead of the older learning history");
        LanSync host=LanSyncProof.sync(nextContext),one=null,two=null;
        try{
            host.create("Learning migration");host.startHost();String code=invitation(host);
            LanSyncProof.TestContext s20=new LanSyncProof.TestContext(out.resolve("s20"));LanSyncProof.repository(s20);one=LanSyncProof.sync(s20);one.join(code);JSONObject beforeViewer=one.analysisKnowledge(v->{});
            LanSyncProof.TestContext s10=new LanSyncProof.TestContext(out.resolve("s10"));LanSyncProof.repository(s10);two=LanSyncProof.sync(s10);two.join(code);two.analysisKnowledge(v->{});
            check(beforeViewer.getJSONArray("observations").length()==15,"an already paired viewer exposes the same reduced learning basis before recovery");
            Map<String,?> settings=new HashMap<>(next.prefs.getAll());
            HistoryMigration.Result result=HistoryMigration.restore(next,nextPlan,nextLedger,diagnostic);
            check(result.tables.getInt("observations")==51&&result.tables.getInt("forecasts")==66,"recovery adds all missing learning observations and forecast comparisons without duplicating the recent five days");
            JSONObject restoredWeather;try(Cursor c=nextDb.rawQuery("SELECT weather FROM confirmed_setups WHERE id='previously-approved'",null)){if(!c.moveToFirst())throw new AssertionError();restoredWeather=new JSONObject(c.getString(0));}
            check(result.updated==1&&restoredWeather.optBoolean("complete")&&restoredWeather.getJSONArray("days").length()==1,"an already installed confirmation receives its missing archived weather through the existing merge routine");
            check(settings.equals(next.prefs.getAll()),"restoring the learning ledger never overwrites current equipment, credentials, capacity or the selected archive interval");
            JSONObject after=nextPlan.calculateKnowledge(),viewerOne=one.analysisKnowledge(v->{}),viewerTwo=two.analysisKnowledge(v->{});
            check(after.getJSONArray("observations").length()==66,"the central calculation retains older seasonal learning instead of starting with its recent five days");
            check(viewerOne.getJSONArray("observations").length()==66&&viewerTwo.getJSONArray("observations").length()==66,"both previously paired viewers receive older imported learning rows over the unchanged incremental protocol");
            check(viewerOne.getJSONArray("forecasts").length()==66&&viewerTwo.getJSONArray("forecasts").length()==66,"original eligible forecast comparisons reach both viewer replicas");
            check(viewerOne.getJSONArray("confirmedSetups").getJSONObject(0).getBoolean("weatherComplete"),"the viewer also sees the recovered confirmation weather as complete");
            JSONObject fixture=new JSONObject().put("now",now).put("setup",setup).put("solarKey",solar).put("systemKey",system).put("before",before).put("original",oldKnowledge).put("central",after).put("s20",viewerOne).put("s10",viewerTwo);
            Files.writeString(out.resolve("learning-states.json"),fixture.toString());
            long revision=nextPlan.modelRevision(),observed=count(nextDb,"observations");HistoryMigration.Result repeated=HistoryMigration.restore(next,nextPlan,nextLedger,diagnostic);
            check(repeated.inserted==0&&repeated.updated==0&&nextPlan.modelRevision()==revision,"repeating the complete learning import is idempotent, including confirmation-weather completion");
            JSONObject legacy=new JSONObject(diagnostic.toString()).put("learningAlgorithm",1690);legacy.remove("mobileDisplay");
            check(HistoryMigration.restore(next,nextPlan,nextLedger,legacy).inserted==0,"the known version-2 learning export from Watt’n’Pit 1.6.10 remains usable without newer mobile-display tables");
            JSONObject conflict=new JSONObject(diagnostic.toString());JSONObject prior=conflict.getJSONArray("forecasts").getJSONObject(0);double originalWh=prior.getDouble("wh");prior.put("wh",originalWh+1);HistoryMigration.restore(next,nextPlan,nextLedger,conflict);
            double kept;try(Cursor c=nextDb.rawQuery("SELECT payload FROM forecasts WHERE key=? AND start=? AND end=? AND issued=?",new String[]{prior.getString("key"),""+prior.getLong("from"),""+prior.getLong("to"),""+prior.getLong("issued")})){if(!c.moveToFirst())throw new AssertionError();kept=new JSONObject(c.getString(0)).getDouble("wh");}
            check(kept==originalWh,"an old export cannot replace an existing original forecast comparison");
            JSONObject overlap=new JSONObject(diagnostic.toString()),part=new JSONObject(overlap.getJSONArray("observations").getJSONObject(0).toString());long a=part.getLong("from")+1000,b=part.getLong("to")-1000;
            part.put("from",a).put("to",b);JSONObject usage=part.getJSONObject("usage");usage.put("duration",b-a).put("wh",(b-a)*10.0/HOUR).put("latest",b);part.put("usageByPresence",new JSONObject().put("home",new JSONObject(usage.toString())));overlap.put("observations",new JSONArray().put(part));HistoryMigration.restore(next,nextPlan,nextLedger,overlap);
            check(count(nextDb,"observations")==observed,"overlapping old learning windows cannot inflate already recorded learning hours");
            JSONObject invalid=new JSONObject(diagnostic.toString());invalid.getJSONArray("contexts").getJSONObject(0).put("at",today-501*DAY);invalid.getJSONArray("observations").getJSONObject(0).getJSONObject("usage").put("wh",-1);boolean rejected=false;
            try{HistoryMigration.restore(next,nextPlan,nextLedger,invalid);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected&&nextPlan.modelRevision()==revision,"a malformed late learning record is rejected before any other migration data are written");
            rejected=false;try{HistoryMigration.restore(next,nextPlan,nextLedger,new JSONObject(diagnostic.toString()).put("learningAlgorithm",SolarPlanning.ALGORITHM+1));}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected&&nextPlan.modelRevision()==revision,"a different learning algorithm cannot silently seed an incompatible old ledger");
            JSONObject unfinished=new JSONObject(diagnostic.toString()),partial=learning(now-600000,0,key,solar,system,false);partial.put("to",now+HOUR);JSONObject u=partial.getJSONObject("usage");u.put("duration",600000).put("wh",600000*10.0/HOUR).put("latest",now);partial.put("usageByPresence",new JSONObject().put("home",new JSONObject(u.toString())));unfinished.put("observations",new JSONArray().put(partial));
            check(HistoryMigration.restore(next,nextPlan,nextLedger,unfinished).tables.getInt("observations")==1,"an unfinished day's slot can extend beyond export time while its actual observed learning remains in the past");
            partial.getJSONObject("usage").put("latest",now+HOUR);rejected=false;try{HistoryMigration.restore(next,nextPlan,nextLedger,unfinished);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"an unfinished slot cannot smuggle future measurement time into the learning ledger");
        }finally{LanSyncProof.close(host);if(one!=null)LanSyncProof.close(one);if(two!=null)LanSyncProof.close(two);nextPlan.shutdown();oldPlan.shutdown();nextPlan.close();oldPlan.close();nextLedger.close();oldLedger.close();}
    }
}
