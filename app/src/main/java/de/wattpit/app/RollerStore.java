package de.wattpit.app;

import android.content.*;
import de.wattpit.app.core.*;
import org.json.*;

/** Independent roller ledger: never adds its input estimate to the Shunt totals. */
final class RollerStore {
    private static RollerStore instance;
    static synchronized RollerStore get(Context c){if(instance==null)instance=new RollerStore(c.getApplicationContext());return instance;}
    private final SharedPreferences prefs,plantPrefs;
    private final RollerLedger ledger;private final boolean central;
    private JSONObject profile,live=new JSONObject(),lastKnown=new JSONObject();
    private long lastReadingSaved;
    private double[] cells=new double[0];private long cellsAt;
    private RollerEnergy.Session session=new RollerEnergy.Session();
    private final RollerEnergy.AutoCharge detector=new RollerEnergy.AutoCharge();
    private boolean automatic;
    private long lastAcceptedAt,autoWindowFrom,autoWindowTo;
    private double lastAcceptedPower;
    private boolean continuousObservation;
    private long revision;
    private String lastCommand="",endReason="";private Double startSoc,endSoc;private long startSocAt,endSocAt;
    private String status="Nicht verbunden";
    private RollerStore(Context c){
        ledger=RollerLedger.get(c);central=c.getSharedPreferences("wohnpit-role",Context.MODE_PRIVATE).getString("role","").equals("central");
        prefs=c.getSharedPreferences("wattpit-roller",Context.MODE_PRIVATE);plantPrefs=c.getSharedPreferences("wattpit",Context.MODE_PRIVATE);
        try{lastKnown=new JSONObject(prefs.getString("lastReading","{}"));profile=checked(new JSONObject(prefs.getString("profile","{}")));JSONObject s=new JSONObject(prefs.getString("session","{}"));
            if(!prefs.getBoolean("lastReadingMigrationDone",false)&&lastKnown.optLong("at")<=0&&!profile.optString("mac").isEmpty()){
                lastKnown=ledger.lastReading(RollerLedger.source(plantPrefs),profile.optString("mac"));
                if(lastKnown.optLong("at")<=0&&!s.isNull("endSoc")&&s.optLong("endSocAt")>0){double soc=s.optDouble("endSoc",Double.NaN);if(Double.isFinite(soc)&&soc>=0&&soc<=100)lastKnown=new JSONObject().put("at",s.getLong("endSocAt")).put("soc",soc).put("source","Gespeichertes Ladejournal");}
                if(lastKnown.optLong("at")>0)prefs.edit().putString("lastReading",lastKnown.toString()).apply();
            }
            prefs.edit().putBoolean("lastReadingMigrationDone",true).apply();
            startSoc=s.isNull("startSoc")?null:s.has("startSoc")?s.optDouble("startSoc"):null;endSoc=s.isNull("endSoc")?null:s.has("endSoc")?s.optDouble("endSoc"):null;startSocAt=s.optLong("startSocAt");endSocAt=s.optLong("endSocAt");endReason=s.optString("endReason");session.started=s.optLong("started");session.ended=s.optLong("ended");session.observedMillis=s.optLong("observedMillis");session.gapMillis=s.optLong("gapMillis");session.batteryWh=s.optDouble("batteryWh",0);session.inputWh=s.optDouble("inputWh",0);automatic=s.optBoolean("automatic");detector.suppressed=prefs.getBoolean("autoSuppressed",false);revision=prefs.getLong("sessionRevision",0);lastCommand=prefs.getString("lastCommand","");
            // A process restart ends the session rather than inventing missing energy.
            if(s.optBoolean("active")){endReason="App-Neustart · Aufzeichnung unterbrochen";session.active=true;session.lastAt=s.optLong("savedAt",session.started);session.stop(automatic?session.lastAt:System.currentTimeMillis());revision++;saveSession();archiveSession();}
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    static JSONObject checked(JSONObject in)throws JSONException{
        String name=in.optString("name","Roller").trim(),mac=in.optString("mac","").trim().toUpperCase(java.util.Locale.ROOT),tech=in.optString("technology","Li-Ion");
        if(name.isEmpty()||name.length()>60||(!mac.isEmpty()&&!mac.matches("([0-9A-F]{2}:){5}[0-9A-F]{2}"))||!java.util.Arrays.asList("Li-Ion","LiFePO4").contains(tech))throw new IllegalArgumentException("Bitte Name, Technologie und Bluetooth-Adresse prüfen.");
        JSONObject o=new JSONObject().put("planCharge",in.optBoolean("planCharge",false)).put("autoSpecialUse",in.optBoolean("autoSpecialUse",false)).put("name",name).put("mac",mac).put("technology",tech);
        String[] keys={"capacityAh","nominalVoltage","reservePercent","targetPercent","idleWatts","conversionEfficiency","chargeWatts","rideKm","whPerKm","rideReservePercent","departure","returnAt"};
        double[] defaults={26,48.1,20,90,21,96.0/117.0,96,0,19,20,0,0};
        double[] min={1,1,0,1,0,.1,1,0,1,0,0,0},max={1000,200,99,100,500,1,5000,2000,200,100,4102444800000.0,4102444800000.0};
        for(int i=0;i<keys.length;i++){double v=in.has(keys[i])?in.getDouble(keys[i]):defaults[i];if(!Double.isFinite(v)||v<min[i]||v>max[i])throw new IllegalArgumentException("Ungültiger Wert: "+keys[i]);o.put(keys[i],v);}
        if(o.getDouble("targetPercent")<o.getDouble("reservePercent"))throw new IllegalArgumentException("Ladeziel muss mindestens der Mindestreserve entsprechen.");
        if(o.getDouble("returnAt")>0&&o.getDouble("returnAt")<=o.getDouble("departure"))throw new IllegalArgumentException("Rückkehr muss nach der Abfahrt liegen.");
        String path=in.optString("chargePath",o.optDouble("idleWatts")>0?"inverter":"load-output");if(!java.util.Arrays.asList("inverter","load-output","relay","other").contains(path))throw new IllegalArgumentException("Unbekannter Ladeweg.");o.put("chargePath",path);return o;
    }
    synchronized void configure(String json)throws JSONException{
        if(json.length()>8000)throw new IllegalArgumentException("Profil zu groß");
        if(session.active)throw new IllegalArgumentException("Bitte zuerst die Ladeaufzeichnung beenden.");
        JSONObject next=checked(new JSONObject(json));boolean addressChanged=!next.optString("mac").equals(profile.optString("mac"));SharedPreferences.Editor edit=prefs.edit().putString("profile",next.toString()).putBoolean("autoSuppressed",false);if(addressChanged)edit.remove("lastReading");if(!edit.commit())throw new IllegalArgumentException("Profil konnte nicht gespeichert werden.");if(addressChanged)lastKnown=new JSONObject();
        try{JSONObject cfg=new JSONObject(plantPrefs.getString("autarpie-energy-v1","{}"));String path=next.optString("chargePath");if(cfg.optString("assignment").equals("scooter")&&(path.equals("relay")||path.equals("load-output")))plantPrefs.edit().putString("autarpie-energy-v1",cfg.put("powerPath",path.equals("relay")?"relay":"direct").toString()).commit();}catch(JSONException ignored){}
        profile=next;detector.suppressed=false;detector.interrupt();live=new JSONObject();lastAcceptedAt=0;
    }
    synchronized String mac(){return profile.optString("mac");}
    synchronized void status(String value){status=value;}
    synchronized void disconnected(){rememberReading(true);live=new JSONObject();cells=new double[0];cellsAt=0;continuousObservation=false;detector.interrupt();session.interrupt(System.currentTimeMillis());saveSession();}
    synchronized void acceptCells(double[] values,long at){cells=values.clone();cellsAt=at;rememberReading(false);}
    synchronized void accept(BmsPacket p,long now){accept(p,now,"Direkt");}
    synchronized void accept(BmsPacket p,long now,String source){try{
        if(now<=lastAcceptedAt)return;JSONObject previousLive=live;double watts=p.voltage*p.current,idle=profile.optDouble("idleWatts"),efficiency=profile.optDouble("conversionEfficiency");
        JSONArray temperatures=new JSONArray();for(double temperature:p.temperatures)temperatures.put(Double.isFinite(temperature)?temperature:JSONObject.NULL);
        live=new JSONObject().put("source",source).put("at",now).put("voltage",p.voltage).put("current",p.current).put("power",watts).put("soc",p.soc<0?JSONObject.NULL:p.soc).put("remainingAh",p.remainingAh).put("fullAh",p.fullAh).put("chargeEnabled",p.chargeEnabled).put("dischargeEnabled",p.dischargeEnabled).put("temperatures",temperatures);
        boolean outputPath=profile.optString("chargePath").equals("load-output")||profile.optString("chargePath").equals("relay");
        boolean assignedOutput=!outputPath||now>=plantPrefs.getLong("outputMeasuredAt",0)&&now-plantPrefs.getLong("outputMeasuredAt",0)<=45000&&plantPrefs.getInt("outputMeasuredState",-1)==1&&(profile.optString("chargePath").equals("relay")||plantPrefs.getFloat("outputMeasuredCurrent",0)>.1f);
        int action=0;boolean started=false;
        if(profile.optBoolean("autoSpecialUse")&&(!session.active||automatic))action=detector.sample(now,assignedOutput?watts:0,session.active);
        if(action==1){automatic=true;session.start(detector.startAt);startSoc=p.soc>=0?(double)p.soc:null;startSocAt=now;endSoc=startSoc;endSocAt=startSocAt;endReason="";revision++;if(!central)session.sample(detector.startAt,detector.startPower,idle,efficiency);autoWindowFrom=detector.startAt;autoWindowTo=now;started=true;}
        if(assignedOutput&&session.active&&automatic&&!started&&continuousObservation&&lastAcceptedAt>0&&now-lastAcceptedAt<=RollerEnergy.MAX_GAP_MS&&(lastAcceptedPower>RollerEnergy.AutoCharge.STOP_WATTS||watts>RollerEnergy.AutoCharge.STOP_WATTS)){
            // Finite observed intervals: a lost BMS never excludes hours of ordinary demand.
            if(autoWindowTo!=lastAcceptedAt)autoWindowFrom=lastAcceptedAt;
            autoWindowTo=now;
        }
        Double learnedInput=null;
        boolean interval=continuousObservation&&lastAcceptedAt>0&&now-lastAcceptedAt<=RollerEnergy.MAX_GAP_MS;
        boolean assigned=session.active&&(!automatic||assignedOutput)&&lastAcceptedAt>=session.started;
        live.put("chargeAssigned",assigned).put("chargePath",profile.optString("chargePath"));
        if(interval){
            // The central records idle observations as well as charge intervals.
            // Daily coverage and energy are derived only by a display handset.
            JSONObject before=new JSONObject(previousLive.toString()).put("chargeAssigned",assigned);
            ledger.rawAdd(RollerLedger.source(plantPrefs),mac(),assigned?session.started:0,lastAcceptedAt,now,lastAcceptedPower,watts,idle,efficiency,before,live);
            if(assigned&&!central){
                learnedInput=(RollerEnergy.inputWatts(Math.max(0,lastAcceptedPower),idle,efficiency)+RollerEnergy.inputWatts(Math.max(0,watts),idle,efficiency))*.5*(now-lastAcceptedAt)/3600000.0;
                try{ledger.add(RollerLedger.source(plantPrefs),mac(),session.started,lastAcceptedAt,now,learnedInput);}catch(RuntimeException failure){plantPrefs.edit().putBoolean("rollerInputStorageError",true).apply();}
            }
        }
        status="BMS verbunden";if(!central){if(!automatic||assignedOutput)session.sample(now,watts,idle,efficiency);else session.interrupt(now);}else if(session.active)session.lastAt=now;
        if(continuousObservation&&!central)SpecialUses.observeRoller(plantPrefs,mac(),lastAcceptedAt,now,learnedInput,learnedInput==null&&(watts>RollerEnergy.AutoCharge.STOP_WATTS||lastAcceptedPower>RollerEnergy.AutoCharge.STOP_WATTS));
        if(session.active){endSoc=p.soc>=0?(double)p.soc:null;endSocAt=now;}lastAcceptedAt=now;lastAcceptedPower=watts;continuousObservation=true;
        if(action==-1){endReason="Ladeende erkannt";session.stop(now);revision++;archiveSession();detector.interrupt();}
        rememberReading(false);saveSession();
    }catch(JSONException ignored){}}
    synchronized void toggle(boolean start){
        long now=System.currentTimeMillis();
        if(start){if(session.active)return;if(!fresh(now))throw new IllegalArgumentException("Zuerst aktuelle BMS-Werte empfangen.");automatic=false;detector.interrupt();session.start(Math.max(now,session.started+1));startSoc=live.isNull("soc")?null:live.optDouble("soc");startSocAt=live.optLong("at");endSoc=startSoc;endSocAt=startSocAt;endReason="";revision++;if(!central)session.sample(now,live.optDouble("power"),profile.optDouble("idleWatts"),profile.optDouble("conversionEfficiency"));}
        else {if(!session.active)return;endReason="Manuell beendet";if(profile.optBoolean("autoSpecialUse"))detector.suppress();session.stop(now);revision++;archiveSession();}
        saveSession();
    }
    synchronized void pause(){if(session.active){endReason="Aufzeichnung pausiert";session.stop(System.currentTimeMillis());revision++;archiveSession();}detector.interrupt();saveSession();}
    synchronized void command(boolean start,long expectedRevision,String id){
        if(!id.matches("[A-Za-z0-9_-]{22}"))throw new IllegalArgumentException("Ungültige Ladeanfrage.");
        if(id.equals(lastCommand))return;
        if(expectedRevision!=revision)throw new IllegalArgumentException("Der Ladestatus hat sich inzwischen geändert. Bitte aktualisieren und erneut versuchen.");
        toggle(start);lastCommand=id;saveSession();
    }
    private boolean fresh(long now){long at=live.optLong("at");return at>0&&now>=at&&now-at<=10000;}
    private JSONObject sessionJson()throws JSONException{return new JSONObject().put("startSoc",startSoc==null?JSONObject.NULL:startSoc).put("startSocAt",startSocAt).put("endSoc",endSoc==null?JSONObject.NULL:endSoc).put("endSocAt",endSocAt).put("endReason",endReason).put("calculationSource",central?"display":"local").put("usageType","roller-charge").put("usageLabel","Sondernutzung · Rollerladung").put("automatic",automatic).put("started",session.started).put("ended",session.ended).put("active",session.active).put("batteryWh",central?JSONObject.NULL:session.batteryWh).put("inputWh",central?JSONObject.NULL:session.inputWh).put("observedMillis",session.observedMillis).put("gapMillis",session.gapMillis).put("savedAt",lastAcceptedAt>0?lastAcceptedAt:System.currentTimeMillis());}
    private void saveSession(){try{prefs.edit().putString("session",sessionJson().toString()).putBoolean("autoSuppressed",detector.suppressed).putLong("sessionRevision",revision).putString("lastCommand",lastCommand).apply();
        JSONArray windows=new JSONArray(plantPrefs.getString("rollerChargeWindows","[]")),out=new JSONArray();
        long from=automatic?autoWindowFrom:session.started,to=automatic?autoWindowTo:session.active?0:session.ended;
        for(int i=Math.max(0,windows.length()-4999);i<windows.length();i++){JSONObject w=windows.getJSONObject(i);if(from<=0||w.optLong("from")!=from)out.put(w);}
        if(from>0&&(!automatic||to>from))out.put(new JSONObject().put("type","roller-charge").put("from",from).put("to",to));
        plantPrefs.edit().putString("rollerChargeWindows",out.toString()).apply();
    }catch(JSONException ignored){}}
    private void archiveSession(){try{if(session.started<=0)return;JSONArray rows=new JSONArray(prefs.getString("journal","[]")),out=new JSONArray();for(int i=Math.max(0,rows.length()-199);i<rows.length();i++)out.put(rows.getJSONObject(i));out.put(sessionJson().put("profile",new JSONObject(profile.toString())));prefs.edit().putString("journal",out.toString()).apply();}catch(JSONException ignored){}}
    private void rememberReading(boolean force){try{
        if(live.optLong("at")<=0)return;lastKnown=new JSONObject(live.toString());
        if(cellsAt>0&&Math.abs(cellsAt-live.optLong("at"))<=30000){JSONArray values=new JSONArray();for(double v:cells)values.put(v);lastKnown.put("cells",values).put("cellsAt",cellsAt);}
        long now=System.currentTimeMillis();if(force||now-lastReadingSaved>=60000){prefs.edit().putString("lastReading",lastKnown.toString()).apply();lastReadingSaved=now;}
    }catch(JSONException ignored){}}
    synchronized JSONObject snapshot()throws JSONException{
        long now=System.currentTimeMillis();boolean fresh=fresh(now);double watts=fresh?Math.max(0,live.optDouble("power")):0;
        if(fresh&&cellsAt>0&&now>=cellsAt&&now-cellsAt<=30000){JSONArray values=new JSONArray();for(double v:cells)values.put(v);live.put("cells",values).put("cellsAt",cellsAt);}else{live.remove("cells");live.remove("cellsAt");}
        JSONObject s=sessionJson();if(central&&s.optLong("started")>0)s.put("batteryWh",JSONObject.NULL).put("inputWh",JSONObject.NULL);s.put("gapMillis",session.gapMillis+(session.active?session.pendingGap(now):0));
        return new JSONObject().put("calculationsOnDisplay",central).put("journal",new JSONArray(prefs.getString("journal","[]"))).put("profile",new JSONObject(profile.toString())).put("live",fresh?new JSONObject(live.toString()):new JSONObject()).put("lastKnown",new JSONObject(lastKnown.toString())).put("fresh",fresh).put("autoSuppressed",detector.suppressed).put("sessionRevision",revision).put("status",status).put("session",s).put("estimatedInputWatts",fresh&&!central?RollerEnergy.inputWatts(watts,profile.optDouble("idleWatts"),profile.optDouble("conversionEfficiency")):JSONObject.NULL);
    }
}
