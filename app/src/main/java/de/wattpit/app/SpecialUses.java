package de.wattpit.app;

import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** User-assigned uses are planning inputs, never additional measured energy. */
final class SpecialUses {
    static final String KEY="special-uses-v1";
    static JSONObject read(SharedPreferences prefs)throws JSONException{
        JSONObject value=new JSONObject(prefs.getString(KEY,"{}"));
        return value.put("version",1).put("profiles",value.optJSONArray("profiles")==null?new JSONArray():value.getJSONArray("profiles"))
            .put("events",value.optJSONArray("events")==null?new JSONArray():value.getJSONArray("events"));
    }
    static JSONObject checked(JSONObject input)throws JSONException{
        if(input.optInt("version",1)!=1||input.toString().length()>180000)throw new IllegalArgumentException("Ungültige Sondernutzungen.");
        JSONArray rows=input.optJSONArray("profiles"),out=new JSONArray();Set<String> ids=new HashSet<>();
        if(rows!=null){if(rows.length()>24)throw new IllegalArgumentException("Höchstens 24 Sondernutzungen möglich.");
            for(int i=0;i<rows.length();i++){
                JSONObject p=rows.getJSONObject(i);String id=p.getString("id"),name=p.getString("name").trim(),mode=p.optString("mode","learn"),kind=p.optString("kind","device");
                if(!id.matches("[A-Za-z0-9_-]{1,70}")||!ids.add(id)||name.isEmpty()||name.length()>60||!Arrays.asList("learn","manual").contains(mode)||!Arrays.asList("device","roller").contains(kind)||kind.equals("roller")&&!id.equals("roller"))throw new IllegalArgumentException("Bitte Bezeichnung und Art der Sondernutzung prüfen.");
                JSONObject clean=new JSONObject().put("id",id).put("name",name).put("kind",kind).put("mode",mode).put("enabled",p.optBoolean("enabled",true));
                for(String key:new String[]{"wh","watts","minutes","hour","whPerKm","km"}){
                    double value=p.optDouble(key,key.equals("minutes")?30:key.equals("hour")?12:key.equals("whPerKm")?19:0),max=key.equals("hour")?23:key.equals("minutes")?1440:key.equals("km")?2000:100000;
                    if(!Double.isFinite(value)||value<0||value>max)throw new IllegalArgumentException("Ungültiger Energie- oder Zeitwert.");clean.put(key,value);
                }
                out.put(clean);
            }
        }
        JSONArray events=input.optJSONArray("events"),valid=new JSONArray();Set<String> eventIds=new HashSet<>();
        if(events!=null){if(events.length()>700)throw new IllegalArgumentException("Zu viele erfasste Nutzungen.");for(int i=0;i<events.length();i++){
            JSONObject e=events.getJSONObject(i);String id=e.getString("id"),profile=e.getString("profileId");long from=e.getLong("from"),to=e.getLong("to");double wh=e.getDouble("wh");
            if(!id.matches("[A-Za-z0-9_-]{1,90}")||!eventIds.add(id)||!ids.contains(profile)||from<=0||to<=from||to-from>86400000L||!Double.isFinite(wh)||wh<0||wh>100000)throw new IllegalArgumentException("Ungültige erfasste Nutzung.");
            valid.put(new JSONObject().put("id",id).put("profileId",profile).put("from",from).put("to",to).put("wh",wh));
        }}
        return new JSONObject().put("version",1).put("profiles",out).put("events",valid);
    }
    static void save(SharedPreferences prefs,String json)throws JSONException{
        JSONObject clean=checked(new JSONObject(json));
        if(!prefs.edit().putString(KEY,clean.toString()).commit())throw new IllegalArgumentException("Sondernutzungen konnten nicht gespeichert werden.");
    }
    static JSONObject snapshot(SharedPreferences prefs)throws JSONException{
        return read(prefs).put("rollerDays",new JSONObject(prefs.getString("special-roller-days-v1","{}")));
    }
    static JSONArray windows(SharedPreferences prefs,JSONArray roller)throws JSONException{
        JSONArray out=new JSONArray(roller.toString()),events=read(prefs).getJSONArray("events");long now=System.currentTimeMillis();
        for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);if(e.getLong("from")<=now&&e.getDouble("wh")>0)out.put(new JSONObject().put("type","special-use").put("from",e.getLong("from")).put("to",e.getLong("to")).put("profileId",e.getString("profileId")));}
        return out;
    }
    private static JSONObject observed;private static String observedMac="";private static long persisted;
    static synchronized void observeRoller(SharedPreferences prefs,String mac,long from,long to,Double inputWh,boolean unassigned)throws JSONException{
        if(from<=0||to<=from||to-from>10000)return;
        if(observed==null||!observedMac.equals(mac)){observed=new JSONObject(prefs.getString("special-roller-days-v1","{}"));observedMac=mac;}
        java.time.ZoneId zone=java.time.ZoneId.systemDefault();long cursor=from;
        while(cursor<to){java.time.LocalDate day=java.time.Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate();long end=Math.min(to,day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli());String key=day.toString();JSONObject value=observed.optJSONObject(key);if(value==null||!mac.equals(value.optString("mac")))value=new JSONObject().put("mac",mac).put("millis",0).put("wh",0).put("unassignedMillis",0);value.put("millis",Math.min(90000000,value.optLong("millis")+end-cursor)).put("wh",value.optDouble("wh",0)+(inputWh!=null&&Double.isFinite(inputWh)&&inputWh>=0?inputWh*(end-cursor)/(to-from):0)).put("unassignedMillis",value.optLong("unassignedMillis")+(unassigned?end-cursor:0));observed.put(key,value);cursor=end;}
        if(to-persisted>=60000){List<String> days=new ArrayList<>();Iterator<String> keys=observed.keys();while(keys.hasNext())days.add(keys.next());Collections.sort(days);for(int i=0;i<days.size()-90;i++)observed.remove(days.get(i));prefs.edit().putString("special-roller-days-v1",observed.toString()).apply();persisted=to;}
    }
    private SpecialUses(){}
}
