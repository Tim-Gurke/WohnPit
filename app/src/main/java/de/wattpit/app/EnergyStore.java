package de.wattpit.app;

import android.content.*;
import org.json.*;
import java.util.*;

/** Shared device state and bounded charge permits. Forecasts are supplied by a display. */
final class EnergyStore {
    static final String KEY="autarpie-energy-v1",CHARGER="autarpie-charger-v1";
    private static final Map<String,JSONObject> solar=new HashMap<>();
    private static JSONObject charger=new JSONObject(),plan=new JSONObject();
    private static String outputError="",chargerError="",authority="";
    private static long requestedAt,autoStarted,lastCommand,authorityUntil,sequence;
    private static Boolean requested,automatic;
    private static boolean goalReached,manualOff,initialized;
    static synchronized JSONObject config(Repository repo)throws JSONException{
        if(!initialized){manualOff=repo.prefs.getBoolean("outputManualOff",false);goalReached=repo.prefs.getBoolean("outputGoalReached",false);initialized=true;}JSONObject c=new JSONObject(repo.prefs.getString(KEY,"{}"));if(!c.has("assignment"))c.put("assignment","unassigned");if(!c.has("monitor"))c.put("monitor",true);if(!c.has("rules"))c.put("rules",new JSONObject().put("enabled",false));return c;
    }
    static synchronized void configure(Repository repo,String json)throws Exception{
        if(json.length()>10000)throw new IllegalArgumentException("Die Ausgangskonfiguration ist zu groß.");JSONObject c=new JSONObject(json);String assignment=c.getString("assignment");if(!Arrays.asList("unassigned","scooter","model","other").contains(assignment))throw new IllegalArgumentException("Ungültige Zuordnung.");
        JSONObject r=c.getJSONObject("rules"),clean=new JSONObject();
        for(String k:new String[]{"target","reserve","houseTarget","forecastReserveWh","startW","stopW","delaySeconds","powerW","efficiency"}){double v=r.getDouble(k);if(!Double.isFinite(v)||v<0||v>100000)throw new IllegalArgumentException("Ungültige Ladegrenze.");clean.put(k,v);}
        if(clean.getDouble("target")<1||clean.getDouble("target")>100||clean.getDouble("reserve")>100||clean.getDouble("houseTarget")>100||clean.getDouble("delaySeconds")>3600||clean.getDouble("efficiency")<=0||clean.getDouble("efficiency")>1||clean.getDouble("powerW")<=0||clean.getDouble("stopW")>=clean.getDouble("startW"))throw new IllegalArgumentException("Bitte gültige Ladeziele und getrennte Start-/Stoppschwellen wählen.");
        String mode=r.getString("mode");if(!Arrays.asList("live","forecast","both").contains(mode))throw new IllegalArgumentException("Ungültige Überschussart.");clean.put("mode",mode).put("enabled",r.getBoolean("enabled"));
        JSONObject result=new JSONObject().put("assignment",assignment).put("monitor",c.getBoolean("monitor")).put("rules",clean);
        if(assignment.equals("scooter")){RollerStore roller=RollerStore.get(repo.applicationContext());JSONObject profile=roller.snapshot().getJSONObject("profile");profile.put("chargePath","load-output").put("idleWatts",0).put("conversionEfficiency",clean.getDouble("efficiency")).put("chargeWatts",clean.getDouble("powerW")*clean.getDouble("efficiency")).put("targetPercent",clean.getDouble("target"));if(clean.optBoolean("enabled"))profile.put("autoSpecialUse",true).put("planCharge",true);roller.configure(profile.toString());}
        if(!repo.prefs.edit().putString(KEY,result.toString()).commit())throw new java.io.IOException();plan=new JSONObject();authority="";goalReached=false;manualOff=false;repo.prefs.edit().putBoolean("outputManualOff",false).putBoolean("outputGoalReached",false).apply();
    }
    static synchronized void live(Repository repo,String mac,JSONObject sample)throws JSONException{solar.put(mac,new JSONObject(sample.toString()));repo.prefs.edit().putLong("outputMeasuredAt",sample.optLong("at")).putInt("outputMeasuredState",sample.optInt("state",-1)).putFloat("outputMeasuredCurrent",(float)sample.optDouble("current",0)).apply();outputError="";}
    static synchronized void charger(JSONObject value){charger=value;chargerError="";}
    static synchronized void error(boolean isCharger,String text){if(isCharger)chargerError=text;else outputError=text;}
    static synchronized JSONObject output(Repository repo)throws JSONException{JSONArray configs=new JSONArray(repo.prefs.getString("devices","[]"));for(int i=0;i<configs.length();i++){JSONObject c=configs.getJSONObject(i);if(c.optString("kind").equals("solar")){JSONObject value=solar.get(c.getString("mac"));if(value!=null)return new JSONObject(value.toString());}}return new JSONObject();}
    static synchronized void overlay(String mac,JSONObject reading,long now)throws JSONException{JSONObject v=solar.get(mac);if(v!=null&&now>=v.optLong("at")&&now-v.optLong("at")<=45000){if(v.has("current"))reading.put("load",v.get("current"));if(v.has("pvVoltage"))reading.put("pvVoltage",v.get("pvVoltage")).put("pvVoltageAt",v.getLong("at"));}}
    static synchronized JSONObject snapshot(Repository repo)throws JSONException{
        JSONObject result=new JSONObject().put("config",config(repo)).put("output",output(repo)).put("outputError",outputError).put("charger",new JSONObject(charger.toString())).put("chargerError",chargerError).put("chargerConfig",ChargerBle.publicConfig(repo)).put("goalReached",goalReached).put("manualOff",manualOff).put("plan",new JSONObject(plan.toString()));
        if(requested!=null)result.put("requested",requested).put("requestedAt",requestedAt).put("automatic",Boolean.TRUE.equals(automatic));return result;
    }
    static synchronized void manual(Context context,boolean on)throws Exception{
        Repository repo=Repository.get(context);if(!repo.running)throw new IllegalArgumentException("Bitte zuerst die Aufzeichnung starten.");if(repo.archiveBusy||repo.liveBusy)throw new IllegalArgumentException("Ein Geräteabruf belegt den Regler. Abruf abbrechen oder Abschluss abwarten.");
        manualOff=!on;repo.prefs.edit().putBoolean("outputManualOff",manualOff).apply();plan=new JSONObject();authority="";command(context,on,false);
    }
    static synchronized void permit(Context context,String json)throws Exception{
        Repository repo=Repository.get(context);JSONObject c=config(repo),rules=c.getJSONObject("rules"),p=new JSONObject(json);long now=System.currentTimeMillis();String owner=p.getString("owner");long seq=p.getLong("sequence"),expires=p.getLong("expires");
        if(!owner.matches("[a-zA-Z0-9_-]{8,80}")||expires<=now||expires>now+65000||Math.abs(now-p.getLong("issued"))>15000)throw new IllegalArgumentException("Der Ladeplan ist abgelaufen. Bitte neu berechnen.");
        if(!authority.isEmpty()&&!authority.equals(owner)&&authorityUntil>now)throw new IllegalArgumentException("Ein anderes Anzeigehandy führt die Ladeautomatik. Nach Ablauf seines Plans ist die Übernahme möglich.");
        if(authority.equals(owner)&&seq<=sequence)throw new IllegalArgumentException("Veralteter Ladeplan.");
        boolean allow=p.getBoolean("allow");if(allow&&(!rules.optBoolean("enabled")||!c.optString("assignment").equals("scooter")||manualOff||goalReached))throw new IllegalArgumentException("Ladeautomatik ist nicht freigegeben.");
        authority=owner;authorityUntil=expires;sequence=seq;plan=new JSONObject().put("owner",owner).put("expires",expires).put("allow",allow).put("reason",p.optString("reason").substring(0,Math.min(160,p.optString("reason").length())));
    }
    private static void command(Context context,boolean on,boolean auto)throws Exception{
        Repository repo=Repository.get(context);if(repo.archiveBusy||repo.liveBusy)throw new IllegalArgumentException("Regler ist durch einen Geräteabruf belegt.");requested=on;requestedAt=System.currentTimeMillis();lastCommand=requestedAt;automatic=auto;if(auto&&on)repo.prefs.edit().putBoolean("automaticOutputWasOn",true).apply();outputError="";if(on&&auto)autoStarted=requestedAt;
        context.startForegroundService(new Intent(context,ArchiveService.class).setAction("OUTPUT").putExtra("desiredOutput",on));CentralLog.add(context,"Lastausgang",(auto?"Automatik: ":"Manuell: ")+(on?"Einschalten angefordert":"Ausschalten angefordert"));
    }
    /** Safety only: no energy forecast or optimisation runs on the central. */
    static synchronized void tick(Context context)throws Exception{
        Repository repo=Repository.get(context);long now=System.currentTimeMillis();if(automatic==null&&repo.prefs.getBoolean("automaticOutputWasOn",false)){if(!repo.archiveBusy&&!repo.liveBusy)command(context,false,true);return;}JSONObject cfg=config(repo),r=cfg.getJSONObject("rules"),out=output(repo);boolean active=out.optInt("state",-1)==1,wasAuto=Boolean.TRUE.equals(automatic);
        if(requested!=null&&out.optLong("at")>=requestedAt&&out.optInt("state",-1)==(requested?1:0)){if(!requested)repo.prefs.edit().putBoolean("automaticOutputWasOn",false).apply();requested=null;outputError="";}
        if(requested!=null&&now-requestedAt>45000){requested=null;outputError="Keine Zustandsbestätigung vom Regler. Verbindung prüfen.";}
        boolean stop=false;String reason="";
        if(wasAuto&&(active||Boolean.TRUE.equals(requested)||repo.prefs.getBoolean("automaticOutputWasOn",false))){
            JSONObject bms=RollerStore.get(context).snapshot(),l=bms.optJSONObject("live");double house=repo.liveHouseSoc(now),soc=l==null?Double.NaN:l.optDouble("soc",Double.NaN);
            if(!repo.running||!r.optBoolean("enabled")||!cfg.optString("assignment").equals("scooter")){stop=true;reason="Automatik nicht mehr freigegeben";}
            else if(!bms.optBoolean("fresh")||!Double.isFinite(house)||!Double.isFinite(soc)){stop=true;reason="Aktuelle Akkudaten fehlen";}
            else if(soc>=r.optDouble("target",85)){stop=true;goalReached=true;repo.prefs.edit().putBoolean("outputGoalReached",true).apply();reason="Ladeziel erreicht";}
            else if(house<=r.optDouble("reserve",35)){stop=true;reason="Wohnwagenreserve schützen";}
            else if(plan.optLong("expires")<=now||!plan.optBoolean("allow")){stop=true;reason="Kein gültiger freigegebener Ladeplan";}
            else if(now-autoStarted>60000&&(out.optDouble("current",0)<.1||l.optDouble("current",0)<.1)){stop=true;manualOff=true;repo.prefs.edit().putBoolean("outputManualOff",true).apply();reason="Keine Ladung bestätigt; Anschluss prüfen und Automatik neu freigeben";}
        }
        if(stop){if(!repo.archiveBusy&&!repo.liveBusy&&now-lastCommand>10000){command(context,false,true);outputError=reason;}return;}
        if(!active&&requested==null&&plan.optBoolean("allow")&&plan.optLong("expires")>now&&r.optBoolean("enabled")&&!manualOff&&!goalReached&&now-lastCommand>15000){
            JSONObject bms=RollerStore.get(context).snapshot();double house=repo.liveHouseSoc(now);if(bms.optBoolean("fresh")&&house>r.optDouble("reserve",35)&&bms.optJSONObject("live")!=null&&bms.getJSONObject("live").optDouble("soc",100)<r.optDouble("target",85)&&!repo.archiveBusy&&!repo.liveBusy)command(context,true,true);
        }
    }
    static synchronized boolean holdsRegulator(){return requested!=null||Boolean.TRUE.equals(automatic)&&plan.optBoolean("allow")&&plan.optLong("expires")>System.currentTimeMillis();}
    static synchronized void shutdown(Context context){if(Boolean.TRUE.equals(automatic))try{plan=new JSONObject();command(context,false,true);}catch(Exception ignored){outputError="Automatisches Abschalten noch nicht bestätigt. Verbindung zum Regler prüfen.";}}
    private EnergyStore(){}
}
