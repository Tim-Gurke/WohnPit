package de.wattpit.app;

import org.json.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

/** Actual SQLite and encrypted viewer replication with a scooter absent 23.5 h/day. */
public final class RollerUsageLearningProof {
    static final long H=3600000L,D=24*H;static final String MAC="AA:00:00:00:00:31",REG="AA:00:00:00:00:32";
    static void check(boolean value,String text){if(!value)throw new AssertionError(text);System.out.println("PASS "+text);}
    static JSONObject entry(JSONObject profile,long from,long to,boolean pause)throws Exception{return new JSONObject().put("profile",profile).put("started",from).put("ended",to).put("active",false).put("startSoc",50).put("endSoc",50).put("endReason",pause?"Aufzeichnung pausiert":"Ladeende erkannt").put("calculationSource","display");}
    static JSONObject meta(long at,boolean assigned)throws Exception{return new JSONObject().put("at",at).put("chargeAssigned",assigned).put("chargePath","relay").put("soc",50);}
    static int completed(JSONObject data)throws Exception{int n=0;JSONObject days=data.getJSONObject("pathDays");for(Iterator<String> it=days.keys();it.hasNext();)if(days.getJSONObject(it.next()).getBoolean("complete"))n++;return n;}
    public static void main(String[] args){try{
        Class.forName("org.sqlite.JDBC");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Path root=Path.of(args[0]);
        LanSyncProof.TestContext central=new LanSyncProof.TestContext(root.resolve("central")),phone=new LanSyncProof.TestContext(root.resolve("phone"));Repository repo=LanSyncProof.repository(central);
        central.getSharedPreferences(AppRole.PREFS,0).edit().putString(AppRole.KEY,"central").commit();
        repo.prefs.edit().putString("devices","[{\"kind\":\"solar\",\"mac\":\"solar\",\"name\":\"Solar\"},{\"kind\":\"shunt\",\"mac\":\"shunt\",\"name\":\"Shunt\"}]").putString("weatherCache","{\"version\":5,\"timezone\":\"UTC\",\"days\":[],\"hours\":[],\"solar\":[]}").commit();
        JSONObject profile=RollerStore.checked(new JSONObject()).put("mac",MAC).put("chargePath","relay").put("idleWatts",0).put("conversionEfficiency",.8);
        central.getSharedPreferences("wattpit-roller",0).edit().putString("profile",profile.toString()).commit();
        RollerLedger ledger=RollerLedger.get(central);String source=RollerLedger.source(repo.prefs);long now=System.currentTimeMillis(),start=now/D*D-5*D;JSONArray journal=new JSONArray();
        ledger.getWritableDatabase().beginTransaction();try{
            for(int day=0;day<3;day++){
                long a=start+day*D,charge=a+10*H,end=charge+H/2;
                for(long t=a;t<=a+D;t+=30000){int state=t>charge&&t<end?1:0;ledger.loadRaw(source,REG,new JSONObject().put("at",t).put("state",state).put("voltage",13).put("current",.05),false,MAC,true,"relay");}
                for(long t=charge;t<end;t+=10000){long session=day==1&&t>=charge+H/4?charge+H/4:charge;ledger.rawAdd(source,MAC,session,t,t+10000,120,120,0,.8,meta(t,true),meta(t+10000,true));}
                if(day==1){journal.put(entry(profile,charge,charge+H/4,true));journal.put(entry(profile,charge+H/4,end,false));}
                else journal.put(entry(profile,charge,end,false));
            }
            // Neither the missing half-day nor an unrelated scooter certifies a zero day.
            long outage=start+3*D;for(long t=outage;t<=outage+12*H;t+=30000)ledger.loadRaw(source,REG,new JSONObject().put("at",t).put("state",0),false,MAC,true,"relay");
            long partial=start+4*D+10*H;for(long t=partial;t<partial+H/6;t+=10000)ledger.rawAdd(source,MAC,partial,t,t+10000,120,120,0,.8,meta(t,true),meta(t+10000,true));journal.put(entry(profile,partial,partial+H,false));
            ledger.rawAdd(source,"AA:00:00:00:00:99",start,start,start+10000,900,900,0,.8,meta(start,true),meta(start+10000,true));
            ledger.getWritableDatabase().setTransactionSuccessful();
        }finally{ledger.getWritableDatabase().endTransaction();}
        journal.put(new JSONObject(journal.getJSONObject(0).toString()));central.getSharedPreferences("wattpit-roller",0).edit().putString("journal",journal.toString()).commit();
        JSONObject roller=RollerStore.get(central).snapshot(),data=ledger.usageLearning(source,roller,now,"UTC");
        check(data.getJSONArray("charges").length()==3,"three short charges teach without 20 hours of BMS reception; duplicate journal and split recording are counted once");
        for(int i=0;i<3;i++)check(Math.abs(data.getJSONArray("charges").getJSONObject(i).getDouble("inputWh")-75)<1e-8,"charge "+(i+1)+" uses 75 Wh of BMS-derived input, never the relay coil's 0.65 W");
        check(data.getInt("excludedCharges")==1,"an incomplete one-hour charge with ten observed minutes remains excluded");
        for(Iterator<String> it=data.getJSONObject("pathDays").keys();it.hasNext();){JSONObject d=data.getJSONObject("pathDays").getJSONObject(it.next());if(d.optBoolean("complete"))check(Math.abs(d.getDouble("wh")-75)<1e-8,"a fully monitored day contains only the selected scooter charge energy");}
        check(completed(data)==3,"only three genuinely covered path days teach frequency; a central outage stays unknown");
        check(ledger.observedDays(source,MAC,start,now,"UTC").getJSONObject("days").getJSONObject(Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate().toString()).getLong("millis")<H,"the fixture really observes the BMS for less than one hour per day");
        check(data.toString().equals(ledger.usageLearning(source,roller,now,"UTC").toString()),"repeated derivation never duplicates energy or charges");
        check(central.getSharedPreferences("wattpit",0).getString("roller-usage-cache-v1","").isEmpty(),"the central stores raw inputs without doing the display's learning work");
        JSONObject clean=SpecialUses.checked(new JSONObject("{\"version\":1,\"profiles\":[{\"id\":\"roller\",\"kind\":\"roller\",\"mode\":\"learn\",\"name\":\"Test scooter\",\"chargesPerWeek\":3.5}]}"));check(clean.getJSONArray("profiles").getJSONObject(0).getDouble("chargesPerWeek")==3.5,"a fractional weekly planning assumption survives native validation");
        LanSync host=LanSyncProof.sync(central);host.create("Scooter learning test");host.startHost();LanSync viewer=LanSyncProof.sync(phone);
        try{viewer.join(LearningRecoveryProof.invitation(host));viewer.analysisKnowledge(null);JSONObject remote=new JSONObject(viewer.part("specialUses")).getJSONObject("rollerLearning");check(remote.getJSONArray("charges").length()==3&&completed(remote)==3,"the real encrypted viewer path derives the same short-charge learning from replicated raw inputs");
            viewer.analysisKnowledge(null);check(new JSONObject(viewer.part("specialUses")).getJSONObject("rollerLearning").getJSONArray("charges").length()==3,"another viewer refresh never adds duplicate charge events");
            Files.writeString(root.resolve("result.json"),new JSONObject().put("profile",profile).put("rollerLearning",remote).put("now",now).toString());
        }finally{LanSyncProof.close(viewer);LanSyncProof.close(host);}
        System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
