package de.wattpit.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import de.wattpit.app.core.Reading;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Production SQLite regression: invalidation never erases unrebuildable evidence. */
public final class LearningRetentionProof {
    static final long H=3600000L,D=24*H;
    static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static double hours(JSONObject knowledge)throws Exception{double total=0;JSONArray rows=knowledge.getJSONArray("observations");for(int i=0;i<rows.length();i++)total+=rows.getJSONObject(i).getJSONObject("usage").optLong("duration")/(double)H;return total;}
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("PASS "+text);}
    static void scenario(Path path,String label,boolean raw,boolean moved,boolean mismatch,boolean expectLoss)throws Exception{
        LanSyncProof.TestContext ctx=new LanSyncProof.TestContext(path);
        Repository repo=LanSyncProof.repository(ctx);
        ctx.getSharedPreferences(AppRole.PREFS,0).edit().putString(AppRole.KEY,"viewer").commit();
        long now=System.currentTimeMillis(),from=now/D*D-10*D+11*H,to=from+5*H;
        String devices="[{\"mac\":\"AA:11:22:33:44:55\",\"kind\":\"solar\",\"name\":\"Solar\"},{\"mac\":\"AA:11:22:33:44:66\",\"kind\":\"shunt\",\"name\":\"Shunt\"}]";
        JSONObject equipment=Repository.checkedEquipment(new JSONObject("{\"version\":1,\"groups\":[{\"id\":\"fixed\",\"label\":\"Fest\",\"count\":2,\"watts\":100,\"placement\":\"fixed\",\"measurement\":\"solar\",\"tilt\":0}]}"));
        JSONObject setup=SolarPlanning.checked(new JSONObject(),equipment);
        JSONObject oldPlace=new JSONObject("{\"latitude\":52.5,\"longitude\":13.4,\"name\":\"Old neutral site\"}");
        JSONObject currentPlace=new JSONObject(oldPlace.toString());if(moved)currentPlace.put("latitude",51.0).put("name","New neutral site");
        JSONObject weather=new JSONObject().put("version",5).put("timezone","UTC").put("place",currentPlace).put("days",new JSONArray()).put("hours",new JSONArray()).put("solar",new JSONArray()).put("windows",new JSONArray()).put("updated",now).put("signature",mismatch?"previous-configuration":equipment+"|"+setup);
        repo.prefs.edit().putString("devices",devices).putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",setup.toString()).putString("weatherPlace",currentPlace.toString()).putString("weatherCache",weather.toString()).commit();
        String source=RollerLedger.source(repo.prefs),key=SolarKeys.key(equipment,setup,oldPlace,source,false,false),solar=SolarKeys.key(equipment,setup,oldPlace,source,true,false);
        RollerLedger ledger=new RollerLedger(ctx,"audit-roller.db");
        SolarPlanning plan=new SolarPlanning(ctx,repo,v->{},"audit-planning.db",ledger);
        SQLiteDatabase db=plan.getWritableDatabase();
        JSONObject payload=new JSONObject().put("equipment",equipment).put("setup",setup).put("place",oldPlace);
        ContentValues context=new ContentValues();context.put("at",from-D);context.put("source",source);context.put("key",key);context.put("solar_key",solar);context.put("mobile_free",1);context.put("payload",payload.toString());db.insertOrThrow("contexts",null,context);
        JSONObject usage=new JSONObject().put("duration",5*H).put("wh",550).put("estimatedDuration",0).put("estimatedWh",0).put("reconstructedDuration",0).put("reconstructedWh",0).put("latest",to);
        JSONObject row=new JSONObject().put("from",from).put("to",to).put("day",java.time.Instant.ofEpochMilli(from).atZone(java.time.ZoneOffset.UTC).toLocalDate().toString()).put("slot",1).put("key",key).put("solarKey",solar).put("solar",500).put("coverage",1).put("solarCoverage",1).put("mobileFree",true).put("learningVersion",2).put("usage",usage).put("usageByPresence",new JSONObject().put("home",usage));
        ContentValues value=new ContentValues();value.put("start",from);value.put("end",to);value.put("source",source);value.put("payload",row.toString());db.insertOrThrow("observations",null,value);
        if(raw)for(long t=from;t<=to;t+=60000){
            Reading pv=new Reading();pv.device="AA:11:22:33:44:55";pv.kind="solar";pv.origin="live";pv.timestamp=t;pv.period=60;pv.solar=100.0;pv.voltage=13.0;pv.current=100.0/13;pv.state=3;pv.error=0;repo.db.insert(pv);
            Reading shunt=new Reading();shunt.device="AA:11:22:33:44:66";shunt.kind="shunt";shunt.origin="live";shunt.timestamp=t;shunt.period=60;shunt.power=-10.0;shunt.voltage=13.0;shunt.current=-10.0/13;shunt.soc=70.0;repo.db.insert(shunt);
        }
        plan.analysisPresence(new JSONArray(),new JSONObject(),from,now);
        JSONObject baseline=plan.calculateKnowledge();double before=hours(baseline);long rawBefore=count(repo.db.getReadableDatabase(),"readings");
        check(before==5,label+": saved 5 h survive an ordinary recalculation");
        plan.invalidateLearning(from);
        plan.analysisPresence(new JSONArray(),new JSONObject(),from,now);
        JSONObject after=plan.calculateKnowledge();double remaining=hours(after);long rawAfter=count(repo.db.getReadableDatabase(),"readings");
        check(rawAfter==rawBefore,label+": original measurements unchanged ("+rawAfter+" rows)");
        if(expectLoss){check(remaining>=5,label+": saved learning remains available for later rebuilding");
            boolean pending=false;for(int i=0;i<after.getJSONArray("observations").length();i++)pending|=after.getJSONArray("observations").getJSONObject(i).optBoolean("rebuildPending");
            check(pending,label+": retained evidence is explicitly suspended pending complete inputs");
            check(plan.learningSince(now)<=from,label+": old pending range is retried beyond the rolling week");}
        else {check(remaining>=4.9,label+": complete inputs allow reconstruction without lost hours ("+remaining+" h)");
            boolean pending=false;for(int i=0;i<after.getJSONArray("observations").length();i++)pending|=after.getJSONArray("observations").getJSONObject(i).optBoolean("rebuildPending");check(!pending,label+": complete replacement clears pending state");}
        JSONObject report=new JSONObject().put("label",label).put("beforeHours",before).put("afterHours",remaining).put("rawBefore",rawBefore).put("rawAfter",rawAfter).put("after",after);
        Files.writeString(path.resolve("result.json"),report.toString());
        plan.shutdown();plan.close();ledger.close();repo.db.close();
    }
    public static void main(String[] args){try{Class.forName("org.sqlite.JDBC");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Path root=Path.of(args[0]);
        scenario(root.resolve("complete-inputs"),"Complete original inputs",true,false,false,false);
        scenario(root.resolve("imported-knowledge"),"Imported knowledge without original samples",false,false,false,true);
        scenario(root.resolve("changed-site"),"Measured historical knowledge after a site change",true,true,false,false);
        scenario(root.resolve("weather-pending"),"Weather cache still for previous configuration",true,false,true,true);
        System.exit(0);
    }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
