package de.wattpit.app;

import android.content.*;
import de.wattpit.app.core.*;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** A recent imported ledger must not hide older source data from the real viewer. */
public final class LearningRecoveryProof {
    static final long H=3600000L,D=24*H;
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("PASS "+text);}
    static ViewerAnalysis replica(LanSync sync)throws Exception{Field f=LanSync.class.getDeclaredField("analysis");f.setAccessible(true);return (ViewerAnalysis)f.get(sync);}
    static String invitation(LanSync host)throws Exception{
        Field c=LanSync.class.getDeclaredField("config"),s=LanSync.class.getDeclaredField("server");c.setAccessible(true);s.setAccessible(true);
        return de.wattpit.app.core.LanWire.encodeInvitation(new JSONObject(c.get(host).toString()).put("port",((java.net.ServerSocket)s.get(host)).getLocalPort()).put("addresses",new JSONArray().put("127.0.0.1")).toString());
    }
    static void invoke(LanSync sync,String name,Class<?>[] types,Object...args)throws Exception{Method m=LanSync.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(sync,args);}
    static boolean needsRecovery(SolarPlanning plan)throws Exception{Method m=SolarPlanning.class.getDeclaredMethod("needsHistoricalRecovery");m.setAccessible(true);return (Boolean)m.invoke(plan);}
    static double hours(JSONObject knowledge)throws Exception{double h=0;JSONArray rows=knowledge.getJSONArray("observations");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);if(!r.optBoolean("rebuildPending"))h+=r.getJSONObject("usage").optLong("duration")/(double)H;}return h;}
    public static void main(String[] args){try{
        Class.forName("org.sqlite.JDBC");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Path root=Path.of(args[0]);
        LanSyncProof.TestContext central=new LanSyncProof.TestContext(root.resolve("central")),phone=new LanSyncProof.TestContext(root.resolve("phone"));
        Repository source=LanSyncProof.repository(central);
        long now=System.currentTimeMillis(),from=now/D*D-30*D,to=now/D*D-D;
        JSONObject equipment=Repository.checkedEquipment(new JSONObject("{\"version\":1,\"groups\":[{\"id\":\"fixed\",\"label\":\"Fixed\",\"count\":1,\"watts\":200,\"placement\":\"fixed\",\"measurement\":\"solar\",\"tilt\":0}]}"));
        JSONObject setup=SolarPlanning.checked(new JSONObject(),equipment),place=new JSONObject("{\"name\":\"Neutral test site\",\"latitude\":0,\"longitude\":0}");
        source.prefs.edit().putString("devices","[{\"kind\":\"solar\",\"mac\":\"solar\",\"name\":\"Solar\"},{\"kind\":\"shunt\",\"mac\":\"shunt\",\"name\":\"Shunt\"}]")
            .putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",setup.toString()).putString("weatherPlace",place.toString())
            .putString("weatherCache",new JSONObject().put("version",5).put("forecastDays",16).put("pastDays",92).put("updated",now).put("signature",equipment+"|"+setup).put("timezone","UTC").put("place",place).put("days",new JSONArray()).put("hours",new JSONArray()).put("windows",new JSONArray()).put("solar",new JSONArray()).toString()).commit();
        source.db.getWritableDatabase().beginTransaction();try{
            for(long t=from;t<=to;t+=30*60000)for(int kind=0;kind<2;kind++){
                Reading r=new Reading();r.timestamp=t;r.period=1800;r.origin="archive";r.device=kind==0?"solar":"shunt";r.kind=kind==0?"solar":"shunt";r.voltage=13.0;r.current=kind==0?100.0/13:-10.0/13;
                if(kind==0){r.solar=100.0;r.state=3;r.error=0;}else{r.power=-10.0;r.soc=70.0;}source.db.insert(r);
            }source.db.getWritableDatabase().setTransactionSuccessful();
        }finally{source.db.getWritableDatabase().endTransaction();}
        LanSync host=LanSyncProof.sync(central);host.create("History recovery test");host.startHost();
        Field planningField=LanSync.class.getDeclaredField("planning");planningField.setAccessible(true);SolarPlanning plan=(SolarPlanning)planningField.get(host);String plant=RollerLedger.source(source.prefs),key=SolarKeys.key(equipment,setup,place,plant,false,false),solarKey=SolarKeys.key(equipment,setup,place,plant,true,false);
        ContentValues context=new ContentValues();context.put("at",from);context.put("key",key);context.put("source",plant);context.put("mobile_free",1);context.put("solar_key",solarKey);context.put("payload",new JSONObject().put("equipment",equipment).put("setup",setup).put("place",place).toString());plan.getWritableDatabase().insertOrThrow("contexts",null,context);
        long recent=to-D+11*H;JSONObject usage=new JSONObject().put("duration",5*H).put("wh",550).put("estimatedDuration",0).put("estimatedWh",0).put("latest",recent+5*H);
        JSONObject row=new JSONObject().put("from",recent).put("to",recent+5*H).put("day",java.time.Instant.ofEpochMilli(recent).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()).put("slot",1).put("learningVersion",2).put("key",key).put("solarKey",solarKey).put("usage",usage).put("usageByPresence",new JSONObject().put("home",usage));
        ContentValues old=new ContentValues();old.put("start",recent);old.put("end",recent+5*H);old.put("source",plant);old.put("payload",row.toString());plan.getWritableDatabase().insertOrThrow("observations",null,old);
        source.prefs.edit().putInt("planningEnergyVersion."+plant,SolarPlanning.ALGORITHM).commit();
        Repository own=LanSyncProof.repository(phone);LanSync viewer=LanSyncProof.sync(phone);
        own.prefs.edit().putString("dashboard-v1","DO_NOT_EXPORT_LOCAL_PHONE").commit();Reading unrelated=new Reading();unrelated.device="DO_NOT_EXPORT_LOCAL_PHONE";unrelated.kind="shunt";unrelated.origin="live";unrelated.timestamp=now;unrelated.power=999.0;own.db.insert(unrelated);
        viewer.join(invitation(host));JSONObject knowledge=viewer.analysisKnowledge(null);ViewerAnalysis copy=replica(viewer);
        check(copy.repo.db.stats().getLong("from")<=from,"an existing recent imported ledger still downloads the oldest original source measurement (expected="+from+", actual="+copy.repo.db.stats()+", scan="+knowledge.optLong("historyScanFrom")+")");
        check(hours(knowledge)>=29*24-.01,"all 29 supported days can rebuild instead of only the rolling week: "+hours(knowledge)+" h");
        check(own.db.stats().getLong("count")==1&&own.prefs.getString("dashboard-v1","").equals("DO_NOT_EXPORT_LOCAL_PHONE"),"rebuilding the linked plant preserves this handset's independent measurements and layout");
        viewer.analysisKnowledge(null);check(viewer.analysisDiagnostics().getLong("downloadedBlocks")==0,"a second learning refresh reuses the completed historical raw blocks");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();invoke(viewer,"exportAnalysisDiagnostics",new Class<?>[]{OutputStream.class},bytes);
        Map<String,String> entries=new HashMap<>();try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))){ZipEntry e;while((e=zip.getNextEntry())!=null)entries.put(e.getName(),new String(zip.readAllBytes(),StandardCharsets.UTF_8));}
        JSONObject diagnostic=new JSONObject(entries.get("diagnose.json"));check(diagnostic.getJSONObject("history").getLong("from")<=from&&diagnostic.getJSONArray("observations").length()>3,"viewer diagnostics contain the same historical replica and rebuilt learning ledger");
        check(!entries.get("verlauf.csv").contains("DO_NOT_EXPORT_LOCAL_PHONE"),"viewer CSV excludes this handset's unrelated own-plant data");
        check(diagnostic.getJSONObject("analysis").getBoolean("cached"),"export identifies its cached calculation inputs rather than claiming a fresh device reading");
        invoke(viewer,"rebuildLearningHistory",new Class<?>[]{});check(needsRecovery(copy.planning),"an explicit rebuild persists its pending state before another network request");
        host.stopHost();ByteArrayOutputStream offline=new ByteArrayOutputStream();invoke(viewer,"exportAnalysisDiagnostics",new Class<?>[]{OutputStream.class},offline);check(offline.size()>1000,"the already cached viewer diagnosis remains exportable with the central offline");
        boolean failed=false;try{viewer.analysisKnowledge(null);}catch(Exception expected){failed=true;}check(failed&&needsRecovery(copy.planning),"an interrupted rebuild never acknowledges a completed historical recovery");
        LanSyncProof.close(viewer);System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
