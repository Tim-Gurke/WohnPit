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
    private static Boolean requested,automatic,queuedManual;
    private static long queuedAt,commandVersion;
    private static boolean goalReached,manualOff,initialized;
    static synchronized JSONObject config(Repository repo)throws JSONException{
        if(!initialized){manualOff=repo.prefs.getBoolean("outputManualOff",false);goalReached=repo.prefs.getBoolean("outputGoalReached",false);initialized=true;}JSONObject c=new JSONObject(repo.prefs.getString(KEY,"{}"));if(!c.has("powerPath"))c.put("powerPath","direct");if(!c.has("assignment"))c.put("assignment","unassigned");if(!c.has("monitor"))c.put("monitor",true);if(!c.has("rules"))c.put("rules",new JSONObject().put("enabled",false));return c;
    }
    static synchronized void configure(Repository repo,String json)throws Exception{
        if(json.length()>10000)throw new IllegalArgumentException("Die Ausgangskonfiguration ist zu groß.");JSONObject c=new JSONObject(json);String assignment=c.getString("assignment");if(!Arrays.asList("unassigned","scooter","model","other").contains(assignment))throw new IllegalArgumentException("Ungültige Zuordnung.");
        JSONObject r=c.getJSONObject("rules"),clean=new JSONObject();
        for(String k:new String[]{"target","reserve","houseTarget","forecastReserveWh","startW","stopW","delaySeconds","powerW","efficiency"}){double v=r.getDouble(k);if(!Double.isFinite(v)||v<0||v>100000)throw new IllegalArgumentException("Ungültige Ladegrenze.");clean.put(k,v);}
        if(clean.getDouble("target")<1||clean.getDouble("target")>100||clean.getDouble("reserve")>100||clean.getDouble("houseTarget")>100||clean.getDouble("delaySeconds")>3600||clean.getDouble("efficiency")<=0||clean.getDouble("efficiency")>1||clean.getDouble("powerW")<=0||clean.getDouble("stopW")>=clean.getDouble("startW"))throw new IllegalArgumentException("Bitte gültige Ladeziele und getrennte Start-/Stoppschwellen wählen.");
        String mode=r.getString("mode");if(!Arrays.asList("live","forecast","both").contains(mode))throw new IllegalArgumentException("Ungültige Überschussart.");clean.put("mode",mode).put("enabled",r.getBoolean("enabled"));
        String regulator=c.optString("regulator","");if(!regulator.isEmpty()){boolean found=false;JSONArray devices=repo.configs();for(int i=0;i<devices.length();i++){JSONObject d=devices.getJSONObject(i);if(d.optString("kind").equals("solar")&&d.optString("mac").equals(regulator))found=true;}if(!found)throw new IllegalArgumentException("Der gewählte Solarregler ist nicht eingerichtet.");}
        String powerPath=c.optString("powerPath",config(repo).optString("powerPath","direct"));if(!Arrays.asList("direct","relay").contains(powerPath))throw new IllegalArgumentException("Unbekannter Strompfad.");
        JSONObject result=new JSONObject().put("powerPath",powerPath).put("regulator",regulator).put("assignment",assignment).put("monitor",c.getBoolean("monitor")).put("exclusiveScooter",c.optBoolean("exclusiveScooter",false)).put("rules",clean);
        if(assignment.equals("scooter")){RollerStore roller=RollerStore.get(repo.applicationContext());JSONObject profile=roller.snapshot().getJSONObject("profile");profile.put("chargePath",powerPath.equals("relay")?"relay":"load-output").put("idleWatts",0).put("conversionEfficiency",clean.getDouble("efficiency")).put("chargeWatts",clean.getDouble("powerW")*clean.getDouble("efficiency")).put("targetPercent",clean.getDouble("target"));if(clean.optBoolean("enabled"))profile.put("autoSpecialUse",true).put("planCharge",true);roller.configure(profile.toString());}
        if(!repo.prefs.edit().putString(KEY,result.toString()).commit())throw new java.io.IOException();plan=new JSONObject();authority="";goalReached=false;manualOff=false;repo.prefs.edit().putBoolean("outputManualOff",false).putBoolean("outputGoalReached",false).apply();
    }
    static void live(Repository repo,String mac,JSONObject sample)throws JSONException{
        boolean exclusive,learningPath;String path;
        synchronized(EnergyStore.class){
            solar.put(mac,new JSONObject(sample.toString()).put("device",mac));
            repo.prefs.edit().putLong("outputMeasuredAt",sample.optLong("at")).putInt("outputMeasuredState",sample.optInt("state",-1)).putFloat("outputMeasuredCurrent",(float)sample.optDouble("current",0)).apply();
            outputError="";JSONObject configuration=config(repo);
            exclusive=mac.equals(regulator(repo))&&configuration.optString("assignment").equals("scooter")&&configuration.optBoolean("exclusiveScooter")&&!configuration.optString("powerPath").equals("relay");
            learningPath=mac.equals(regulator(repo))&&configuration.optString("assignment").equals("scooter")&&configuration.optBoolean("exclusiveScooter")&&configuration.optBoolean("monitor",true);path=configuration.optString("powerPath").equals("relay")?"relay":"load-output";
        }
        // SQLite and BMS waits must not hold the shared live-state monitor.
        try{RollerLedger.get(repo.applicationContext()).loadRaw(RollerLedger.source(repo.prefs),mac,sample,exclusive,RollerStore.get(repo.applicationContext()).mac(),learningPath,path);}
        catch(Exception failure){error(false,"Lastmesswerte empfangen; Archivspeicherung fehlgeschlagen.");}
    }
    static synchronized void charger(JSONObject value){charger=value;chargerError="";}
    static synchronized void error(boolean isCharger,String text){if(isCharger)chargerError=text;else outputError=text;}
    static synchronized String regulator(Repository repo)throws JSONException{String selected=config(repo).optString("regulator");JSONArray configs=repo.configs();for(int i=0;i<configs.length();i++){JSONObject d=configs.getJSONObject(i);if(d.optString("kind").equals("solar")&&(selected.isEmpty()||selected.equals(d.optString("mac"))))return d.getString("mac");}return "";}
    static synchronized JSONObject output(Repository repo)throws JSONException{JSONObject value=solar.get(regulator(repo));return value==null?new JSONObject():new JSONObject(value.toString());}
    static synchronized void overlay(String mac,JSONObject reading,long now)throws JSONException{JSONObject v=solar.get(mac);if(v!=null&&now>=v.optLong("at")&&now-v.optLong("at")<=45000){if(v.has("current"))reading.put("load",v.get("current"));if(v.has("pvVoltage"))reading.put("pvVoltage",v.get("pvVoltage")).put("pvVoltageAt",v.getLong("at"));}}
    static synchronized JSONObject snapshot(Repository repo)throws JSONException{
        JSONArray outputs=new JSONArray();for(java.util.Map.Entry<String,JSONObject> item:solar.entrySet())outputs.put(new JSONObject(item.getValue().toString()).put("device",item.getKey()));
        JSONObject result=new JSONObject().put("outputs",outputs).put("config",config(repo)).put("output",output(repo)).put("outputError",outputError).put("charger",new JSONObject(charger.toString())).put("chargerError",chargerError).put("chargerConfig",ChargerBle.publicConfig(repo)).put("goalReached",goalReached).put("manualOff",manualOff).put("plan",new JSONObject(plan.toString()));
        if(queuedManual!=null)result.put("queued",queuedManual).put("queuedAt",queuedAt).put("queueReason",repo.archiveBusy?"Archivabruf wird für den Schaltwunsch geordnet beendet":repo.liveBusy?"Laufende Liveabfrage wird abgeschlossen":"Schaltauftrag wartet auf den Regler");
        if(requested!=null)result.put("requested",requested).put("requestedAt",requestedAt).put("automatic",Boolean.TRUE.equals(automatic));return result;
    }
    static synchronized void manual(Context context,boolean on)throws Exception{
        Repository repo=Repository.get(context);if(!repo.running)throw new IllegalArgumentException("Bitte zuerst die Aufzeichnung starten.");
        boolean hasRegulator=!regulator(repo).isEmpty();
        if(!hasRegulator)throw new IllegalArgumentException("Kein Solarladeregler eingerichtet.");
        manualOff=!on;repo.prefs.edit().putBoolean("outputManualOff",manualOff).commit();plan=new JSONObject();authority="";queuedManual=on;queuedAt=System.currentTimeMillis();commandVersion++;outputError="";
        if(repo.archiveBusy)context.startService(new Intent(context,ArchiveService.class).setAction("CONTROL_PREEMPT"));
        tick(context);
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
        long revision=++commandVersion;context.startForegroundService(new Intent(context,ArchiveService.class).setAction("OUTPUT").putExtra("mac",regulator(repo)).putExtra("desiredOutput",on).putExtra("commandVersion",revision));CentralLog.add(context,"Lastausgang",(auto?"Automatik: ":"Manuell: ")+(on?"Einschalten angefordert":"Ausschalten angefordert"));
    }
    /** Safety only: no energy forecast or optimisation runs on the central. */
    static synchronized void tick(Context context)throws Exception{
        Repository repo=Repository.get(context);long now=System.currentTimeMillis();
        if(queuedManual!=null){
            if(!repo.running||now-queuedAt>45000){queuedManual=null;outputError="Der wartende Schaltauftrag wurde nicht ausgeführt. Verbindung prüfen und erneut bedienen.";}
            else if(!repo.archiveBusy&&!repo.liveBusy){boolean on=queuedManual;queuedManual=null;command(context,on,false);}
            return;
        }
        if(automatic==null&&repo.prefs.getBoolean("automaticOutputWasOn",false)){if(!repo.archiveBusy&&!repo.liveBusy)command(context,false,true);return;}JSONObject cfg=config(repo),r=cfg.getJSONObject("rules"),out=output(repo);boolean active=out.optInt("state",-1)==1,wasAuto=Boolean.TRUE.equals(automatic);
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
            else if(now-autoStarted>60000&&(!cfg.optString("powerPath").equals("relay")&&out.optDouble("current",0)<.1||l.optDouble("current",0)<.1)){stop=true;manualOff=true;repo.prefs.edit().putBoolean("outputManualOff",true).apply();reason="Keine Ladung bestätigt; Anschluss prüfen und Automatik neu freigeben";}
        }
        if(stop){if(!repo.archiveBusy&&!repo.liveBusy&&now-lastCommand>10000){command(context,false,true);outputError=reason;}return;}
        if(!active&&requested==null&&plan.optBoolean("allow")&&plan.optLong("expires")>now&&r.optBoolean("enabled")&&!manualOff&&!goalReached&&now-lastCommand>15000){
            JSONObject bms=RollerStore.get(context).snapshot();double house=repo.liveHouseSoc(now);if(bms.optBoolean("fresh")&&house>r.optDouble("reserve",35)&&bms.optJSONObject("live")!=null&&bms.getJSONObject("live").optDouble("soc",100)<r.optDouble("target",85)&&!repo.archiveBusy&&!repo.liveBusy)command(context,true,true);
        }
    }
    static synchronized boolean currentCommand(long version){return version==commandVersion&&queuedManual==null&&requested!=null&&System.currentTimeMillis()-requestedAt<=45000;}
    static synchronized boolean commandPending(){return queuedManual!=null||requested!=null;}
    static synchronized boolean holdsRegulator(){return queuedManual!=null||requested!=null||Boolean.TRUE.equals(automatic)&&plan.optBoolean("allow")&&plan.optLong("expires")>System.currentTimeMillis();}
    static synchronized void shutdown(Context context){if(Boolean.TRUE.equals(automatic))try{plan=new JSONObject();command(context,false,true);}catch(Exception ignored){outputError="Automatisches Abschalten noch nicht bestätigt. Verbindung zum Regler prüfen.";}}
    private EnergyStore(){}
}
