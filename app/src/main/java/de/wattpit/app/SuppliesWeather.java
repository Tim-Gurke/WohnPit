package de.wattpit.app;

import org.json.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Infrequent weather collection on the data host; supply data never leaves the device. */
final class SuppliesWeather {
    private static final ExecutorService NETWORK=Executors.newSingleThreadExecutor();
    private static final AtomicBoolean BUSY=new AtomicBoolean();
    private static final AtomicLong ATTEMPT=new AtomicLong();
    static boolean busy(){return BUSY.get();}
    static void refresh(Repository repo,boolean force){
        try{SuppliesStore.recordSite(repo);}catch(Exception ignored){}
        long now=System.currentTimeMillis();if(!force&&now-ATTEMPT.get()<900000L)return;
        String place=repo.prefs.getString("weatherPlace","");if(place.isEmpty()||!BUSY.compareAndSet(false,true))return;ATTEMPT.set(now);
        String journalBefore=repo.prefs.getString(SuppliesStore.KEY,"{}");
        NETWORK.execute(()->{
            try{
                JSONObject journal=SuppliesStore.read(repo.prefs),saved=new JSONObject(repo.prefs.getString(SuppliesStore.WEATHER,"{}")),current=SuppliesStore.site(new JSONObject(place));
                if(!force&&saved.optLong("journalRevision",-1)==journal.optLong("revision")&&saved.optString("siteKey").equals(current.getString("key"))&&now-saved.optLong("updated")<6*3600000L)return;
                Map<String,JSONObject> history=new TreeMap<>();JSONArray prior=saved.optJSONArray("history");if(prior!=null)for(int i=0;i<prior.length();i++){JSONObject row=prior.getJSONObject(i);history.put(row.getString("site")+"/"+row.getLong("from"),row);}
                String base="https://api.open-meteo.com/v1/forecast?latitude="+current.getDouble("latitude")+"&longitude="+current.getDouble("longitude")+"&forecast_days=15&past_days=14&timeformat=unixtime&timezone=auto&hourly=temperature_2m,wind_speed_10m";
                JSONObject recent=WeatherClient.get(base);String timezone=recent.getString("timezone");ZoneId zone=ZoneId.of(timezone);long today=LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli();
                JSONArray parsed=days(recent,current.getString("key"),"recent",now),forecast=new JSONArray();
                for(int i=0;i<parsed.length();i++){JSONObject row=parsed.getJSONObject(i);String key=row.getString("site")+"/"+row.getLong("from");if(row.getLong("from")>=today){row.put("source","forecast");forecast.put(row);}else if(!history.containsKey(key)||!history.get(key).optString("source").equals("archive"))history.put(key,row);}
                Map<String,JSONObject> sites=new LinkedHashMap<>();Map<String,Long> starts=new HashMap<>();sites.put(current.getString("key"),current);
                JSONArray events=journal.getJSONArray("events");for(int i=0;i<events.length();i++){JSONObject event=events.getJSONObject(i),site=event.optJSONObject("site");if(site==null||!site.has("key"))continue;String key=site.getString("key");sites.put(key,site);starts.put(key,Math.min(starts.getOrDefault(key,now),event.getLong("at")));}
                boolean incomplete=false;int calls=0;for(Map.Entry<String,JSONObject> entry:sites.entrySet()){
                    String key=entry.getKey();Long first=starts.get(key);if(first==null)continue;JSONObject site=entry.getValue();
                    LocalDate start=Instant.ofEpochMilli(Math.max(first,now-1096L*86400000L)).atZone(zone).toLocalDate(),end=LocalDate.now(zone).minusDays(7),cursor=start;
                    while(!cursor.isAfter(end)&&calls<5){
                        LocalDate finish=cursor.plusDays(89);if(finish.isAfter(end))finish=end;boolean missing=false;
                        for(LocalDate d=cursor;!d.isAfter(finish);d=d.plusDays(1)){JSONObject row=history.get(key+"/"+d.atStartOfDay(zone).toInstant().toEpochMilli());if(row==null||!row.optString("source").equals("archive")){missing=true;break;}}
                        if(missing){calls++;try{JSONObject result=WeatherClient.get("https://archive-api.open-meteo.com/v1/archive?latitude="+site.getDouble("latitude")+"&longitude="+site.getDouble("longitude")+"&start_date="+cursor+"&end_date="+finish+"&timeformat=unixtime&timezone="+java.net.URLEncoder.encode(timezone,"UTF-8")+"&hourly=temperature_2m,wind_speed_10m");JSONArray rows=days(result,key,"archive",now);for(int i=0;i<rows.length();i++){JSONObject row=rows.getJSONObject(i);history.put(key+"/"+row.getLong("from"),row);}}catch(Exception unavailable){incomplete=true;}}
                        cursor=finish.plusDays(1);
                    }
                }
                JSONArray historic=new JSONArray();for(JSONObject row:history.values())historic.put(row);
                // Preserve issued forecasts independently from later weather reconstructions.
                JSONArray issued=saved.optJSONArray("forecastArchive"),kept=new JSONArray();String issuedDay=LocalDate.now(zone).toString();if(issued!=null)for(int i=Math.max(0,issued.length()-119);i<issued.length();i++){JSONObject row=issued.getJSONObject(i);if(!issuedDay.equals(row.optString("day"))||!current.getString("key").equals(row.optString("site")))kept.put(row);}
                kept.put(new JSONObject().put("day",issuedDay).put("site",current.getString("key")).put("issuedAt",now).put("days",forecast));
                JSONObject next=new JSONObject().put("version",1).put("timezone",timezone).put("siteKey",current.getString("key")).put("place",current).put("updated",now).put("journalRevision",journal.optLong("revision")).put("history",historic).put("forecast",forecast).put("forecastArchive",kept);
                if(place.equals(repo.prefs.getString("weatherPlace",""))){android.content.SharedPreferences.Editor edit=repo.prefs.edit().putString(SuppliesStore.WEATHER,next.toString());if(incomplete)edit.putString("suppliesWeatherError","Ein Teil des Wetterrückblicks fehlt noch. Vorhandene Werte und die aktuelle Vorhersage bleiben verfügbar.");else edit.remove("suppliesWeatherError");edit.commit();}
            }catch(Exception e){repo.prefs.edit().putString("suppliesWeatherError","Wetterabruf gerade nicht möglich. Gespeicherte Wetterwerte bleiben erhalten; fehlende Werte werden später ergänzt.").apply();}
            finally{BUSY.set(false);if(!place.equals(repo.prefs.getString("weatherPlace",""))||!journalBefore.equals(repo.prefs.getString(SuppliesStore.KEY,"{}")))refresh(repo,true);}
        });
    }
    static JSONArray days(JSONObject response,String site,String source,long issuedAt)throws Exception {
        JSONObject hourly=response.getJSONObject("hourly");JSONArray time=hourly.getJSONArray("time"),temp=hourly.getJSONArray("temperature_2m"),wind=hourly.getJSONArray("wind_speed_10m");
        ZoneId zone=ZoneId.of(response.getString("timezone"));Map<LocalDate,double[]> sums=new TreeMap<>();
        for(int i=0;i<time.length();i++){if(temp.isNull(i)||wind.isNull(i))continue;double t=temp.getDouble(i),w=wind.getDouble(i);if(!Double.isFinite(t)||!Double.isFinite(w)||t< -90||t>65||w<0||w>500)continue;LocalDate date=Instant.ofEpochMilli(time.getLong(i)*1000L).atZone(zone).toLocalDate();double[] s=sums.computeIfAbsent(date,d->new double[5]);double heat=Math.max(0,18-t);s[0]++;s[1]+=t;s[2]+=w;s[3]+=heat;s[4]+=heat*w/20;}
        JSONArray out=new JSONArray();for(Map.Entry<LocalDate,double[]> entry:sums.entrySet()){double[] s=entry.getValue();long from=entry.getKey().atStartOfDay(zone).toInstant().toEpochMilli(),to=entry.getKey().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();if(s[0]*3600000L<.9*(to-from))continue;out.put(new JSONObject().put("from",from).put("to",to).put("site",site).put("temperature",s[1]/s[0]).put("wind",s[2]/s[0]).put("heat",s[3]/s[0]).put("windHeat",s[4]/s[0]).put("source",source).put("issuedAt",issuedAt));}return out;
    }
    private SuppliesWeather(){}
}
