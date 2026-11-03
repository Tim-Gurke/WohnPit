package de.wattpit.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Best-effort names for saved coordinates. This never requests a GPS fix. */
final class GpsPlaceNames {
    static final String CACHE="gps-place-name-v1";
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"wattpit-place-name");t.setDaemon(true);return t;});
    private static final ScheduledExecutorService TIMER=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"wattpit-place-timeout");t.setDaemon(true);return t;});
    private static final Map<SharedPreferences,Map<String,Request>> PENDING=new IdentityHashMap<>();
    static boolean unresolved(String name){return name.equals("GPS-Standort")||name.equals("GPS");}
    static JSONObject display(Context context,SharedPreferences prefs,JSONObject place)throws JSONException{
        if(!unresolved(place.optString("name"))||!place.has("latitude")||!place.has("longitude"))return place;
        double lat=place.optDouble("latitude"),lon=place.optDouble("longitude");if(!valid(lat,lon))return place;
        JSONObject cached=cached(prefs,lat,lon);String name=cached.optString("name");
        if(!name.isEmpty())return new JSONObject(place.toString()).put("name","GPS · "+name);
        if(System.currentTimeMillis()-cached.optLong("attempted")>=15*60000L)resolve(context,prefs,lat,lon,false,null);
        return place;
    }
    private static boolean valid(double lat,double lon){return Double.isFinite(lat)&&Double.isFinite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
    static JSONObject cached(SharedPreferences prefs,double lat,double lon){
        try{JSONObject value=new JSONObject(prefs.getString(CACHE,"{}"));if(Double.compare(value.optDouble("latitude"),lat)==0&&Double.compare(value.optDouble("longitude"),lon)==0)return value;}catch(JSONException ignored){}return new JSONObject();
    }
    static String locality(List<Address> addresses){
        if(addresses==null)return "";
        for(Address a:addresses)if(a!=null)for(String value:new String[]{a.getLocality(),a.getSubLocality(),a.getSubAdminArea()}){
            if(value==null)continue;String clean=value.replaceAll("[\\p{Cntrl}\\s]+"," ").trim();
            if(!clean.isEmpty()&&clean.length()<=160)return clean;
        }
        return "";
    }
    static void resolve(Context context,SharedPreferences prefs,double lat,double lon,boolean force,Consumer<String> callback){
        if(!valid(lat,lon)){if(callback!=null)callback.accept("");return;}
        JSONObject cache=cached(prefs,lat,lon);String known=cache.optString("name");
        if(!known.isEmpty()||!force&&System.currentTimeMillis()-cache.optLong("attempted")<15*60000L){if(callback!=null)callback.accept(known);return;}
        Request request;String key=lat+","+lon;
        synchronized(PENDING){
            Map<String,Request> requests=PENDING.computeIfAbsent(prefs,p->new HashMap<>());request=requests.get(key);
            if(request!=null){if(callback!=null)request.callbacks.add(callback);return;}
            request=new Request(prefs,lat,lon,key);if(callback!=null)request.callbacks.add(callback);requests.put(key,request);
        }
        Request task=request;task.timeout=TIMER.schedule(()->task.finish(""),5,TimeUnit.SECONDS);
        WORK.execute(()->{
            if(task.finished.get())return;
            try{
                if(!Geocoder.isPresent()){task.finish("");return;}Geocoder geocoder=new Geocoder(context.getApplicationContext(),Locale.getDefault());
                if(Build.VERSION.SDK_INT>=33)geocoder.getFromLocation(lat,lon,3,new Geocoder.GeocodeListener(){
                    @Override public void onGeocode(List<Address> addresses){task.finish(locality(addresses));}
                    @Override public void onError(String error){task.finish("");}
                });
                else task.finish(locality(geocoder.getFromLocation(lat,lon,3)));
            }catch(Exception|LinkageError ignored){task.finish("");}
        });
    }
    private static final class Request {
        final SharedPreferences prefs;final double lat,lon;final String key;final long started=System.currentTimeMillis();final AtomicBoolean finished=new AtomicBoolean();
        final List<Consumer<String>> callbacks=new ArrayList<>();volatile ScheduledFuture<?> timeout;
        Request(SharedPreferences prefs,double lat,double lon,String key){this.prefs=prefs;this.lat=lat;this.lon=lon;this.key=key;}
        void finish(String name){
            if(!finished.compareAndSet(false,true))return;if(timeout!=null)timeout.cancel(false);
            List<Consumer<String>> listeners;
            synchronized(PENDING){
                try{JSONObject previous=new JSONObject(prefs.getString(CACHE,"{}"));if(previous.optLong("attempted")<=started)prefs.edit().putString(CACHE,new JSONObject().put("latitude",lat).put("longitude",lon).put("name",name).put("attempted",started).toString()).apply();}catch(JSONException ignored){}
                Map<String,Request> requests=PENDING.get(prefs);if(requests!=null){requests.remove(key);if(requests.isEmpty())PENDING.remove(prefs);}listeners=new ArrayList<>(callbacks);
            }
            for(Consumer<String> listener:listeners)listener.accept(name);
        }
    }
    private GpsPlaceNames(){}
}
