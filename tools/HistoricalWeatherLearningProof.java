package de.wattpit.app;

import android.content.ContentValues;
import de.wattpit.app.core.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Expiring today's weather cache must not discard correctly archived old-site weather. */
public final class HistoricalWeatherLearningProof {
    static final long H=3600000L,D=24*H;
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("PASS "+text);}
    public static void main(String[] args){try{
        Class.forName("org.sqlite.JDBC");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        LanSyncProof.TestContext ctx=new LanSyncProof.TestContext(Path.of(args[0]));Repository repo=LanSyncProof.repository(ctx);
        JSONObject equipment=Repository.checkedEquipment(new JSONObject("{\"version\":1,\"groups\":[{\"id\":\"fixed\",\"label\":\"Fixed\",\"count\":1,\"watts\":200,\"placement\":\"fixed\",\"measurement\":\"solar\",\"tilt\":0},{\"id\":\"mobile\",\"label\":\"Mobile\",\"count\":1,\"watts\":100,\"placement\":\"mobile\",\"measurement\":\"shunt\",\"tilt\":0,\"controller\":\"PWM\",\"yieldMode\":\"manual\",\"yieldFactor\":1}]}"));
        JSONObject setup=SolarPlanning.checked(new JSONObject(),equipment);for(String id:new String[]{"fixed","mobile"})setup.getJSONObject("groups").getJSONObject(id).put("active",true).put("shade",new JSONArray("[0,0,0]"));
        long now=System.currentTimeMillis(),day=now/D*D-20*D,from=day+11*H,to=day+16*H;
        JSONObject oldSite=new JSONObject("{\"name\":\"Old neutral site\",\"latitude\":52.5,\"longitude\":13.4}"),newSite=new JSONObject("{\"name\":\"New neutral site\",\"latitude\":51,\"longitude\":11}");
        repo.prefs.edit().putString("devices","[{\"kind\":\"solar\",\"mac\":\"solar\"},{\"kind\":\"shunt\",\"mac\":\"shunt\"}]").putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",setup.toString()).putString("weatherPlace",newSite.toString()).putString("weatherCache",new JSONObject().put("version",5).put("timezone","UTC").put("place",newSite).put("signature",equipment+"|"+setup).put("days",new JSONArray()).put("hours",new JSONArray()).put("solar",new JSONArray()).put("windows",new JSONArray()).toString()).commit();
        RollerLedger ledger=new RollerLedger(ctx,"weather-roller.db");SolarPlanning plan=new SolarPlanning(ctx,repo,v->{},"weather-planning.db",ledger);String source=RollerLedger.source(repo.prefs),key=SolarKeys.key(equipment,setup,oldSite,source,false,false);
        ContentValues context=new ContentValues();context.put("at",day);context.put("source",source);context.put("key",key);context.put("solar_key",SolarKeys.key(equipment,setup,oldSite,source,true,false));context.put("mobile_free",0);context.put("payload",new JSONObject().put("equipment",equipment).put("setup",setup).put("place",oldSite).toString());plan.getWritableDatabase().insertOrThrow("contexts",null,context);
        ContentValues profile=new ContentValues();profile.put("device","solar");profile.put("start",day);profile.put("end",day+D);profile.put("payload","{\"values\":{\"absorptionVoltage\":14.4,\"maxChargeCurrent\":15,\"temperatureCompensation\":0}}");repo.db.getWritableDatabase().insertOrThrow("device_settings",null,profile);
        for(long t=from;t<=to;t+=60000){Reading pv=new Reading();pv.device="solar";pv.kind="solar";pv.timestamp=t;pv.origin="live";pv.period=60;pv.voltage=13.0;pv.current=100.0/13;pv.solar=100.0;pv.state=3;pv.error=0;repo.db.insert(pv);Reading battery=new Reading();battery.device="shunt";battery.kind="shunt";battery.timestamp=t;battery.origin="live";battery.period=60;battery.voltage=13.0;battery.current=40.0/13;battery.power=40.0;repo.db.insert(battery);}
        for(long end=from+H;end<=to;end+=H){ContentValues h=new ContentValues();h.put("place","52500:13400");h.put("geometry","0:0");h.put("end",end);h.put("value",500.0);h.put("slot",1);h.put("night",0);h.put("sunrise",day+6*H);h.put("sunset",day+18*H);plan.getWritableDatabase().insertOrThrow("model_weather",null,h);}
        JSONObject snapshot=new JSONObject().put("equipment",equipment).put("setup",setup).put("place",oldSite);
        MobileSolarDisplay.LearningInputs input=plan.display.learningInputs(from,to);
        check(Math.abs(input.estimate(snapshot,from,to)-250)<1e-6,"saved old-site hourly geometry supplies 250 Wh without the current rolling cache");
        check(input.estimate(new JSONObject(snapshot.toString()).put("place",newSite),from,to)==null,"another site's weather cannot silently fill historical generation");
        setup.getJSONObject("groups").getJSONObject("mobile").put("shade",new JSONArray("[0,1,0]"));
        check(Math.abs(input.estimate(snapshot,from,to)-200)<1e-6,"the actual saved shade and controller factor affect the historical model");
        setup.getJSONObject("groups").getJSONObject("mobile").put("shade",new JSONArray("[0,0,0]"));
        plan.analysisPresence(new JSONArray(),new JSONObject(),from,to);JSONObject knowledge=plan.calculateKnowledge();double wh=0,hours=0;
        for(int i=0;i<knowledge.getJSONArray("observations").length();i++){JSONObject r=knowledge.getJSONArray("observations").getJSONObject(i);wh+=r.getJSONObject("usage").optDouble("wh");hours+=r.getJSONObject("usage").optLong("duration")/(double)H;}
        check(Math.abs(hours-5)<1e-6&&Math.abs(wh-550)<1e-6,"production learning recovers 5 old-site hours and 550 Wh with explicitly modelled mobile input ("+hours+" h, "+wh+" Wh)");
        plan.getWritableDatabase().delete("model_weather","end=?",new String[]{""+(from+2*H)});
        check(plan.display.learningInputs(from,to).estimate(snapshot,from,to)==null,"a missing archived hour stays unknown instead of being replaced by a forecast");
        plan.shutdown();System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
