package de.wattpit.app;

import android.content.SharedPreferences;
import org.json.*;

/** Shared manual supply journal. No forecasts or learned consumption are calculated here. */
final class SuppliesStore {
    static final String KEY="supplies-v1", WEATHER="supplies-weather-v1";
    static synchronized JSONObject read(SharedPreferences prefs)throws JSONException {
        JSONObject value=new JSONObject(prefs.getString(KEY,"{}"));
        if(value.optInt("version",1)!=1)throw new IllegalArgumentException("Unbekanntes Gas-/Wasserformat.");
        if(!value.has("events"))value.put("events",new JSONArray());
        if(!value.has("defaults"))value.put("defaults",new JSONObject().put("water",20).put("gas",11));
        return value.put("version",1).put("protocol",1);
    }
    static JSONObject site(JSONObject value)throws Exception {
        if(value==null||value.length()==0)return new JSONObject();
        JSONObject checked=WeatherClient.checkedPlace(value);
        checked.put("key",Math.round(checked.getDouble("latitude")*1000)+":"+Math.round(checked.getDouble("longitude")*1000));return checked;
    }
    static synchronized void command(Repository repo,String json)throws Exception {
        if(json==null||json.length()>10000)throw new IllegalArgumentException("Ungültiger Gas-/Wassereintrag.");
        JSONObject command=new JSONObject(json),value=read(repo.prefs);String op=command.getString("op");
        JSONArray events=value.getJSONArray("events");
        switch(op){
            case "defaults":{
                JSONObject defaults=new JSONObject();for(String kind:new String[]{"water","gas"}){double amount=command.getDouble(kind);checkAmount(amount,kind,false);defaults.put(kind,amount);}value.put("defaults",defaults);break;
            }
            case "delete":{
                String id=command.getString("id");JSONArray kept=new JSONArray();for(int i=0;i<events.length();i++)if(!id.equals(events.getJSONObject(i).getString("id")))kept.put(events.get(i));value.put("events",kept);break;
            }
            case "add":case "edit":{
                JSONObject input=command.getJSONObject("event");String id=input.getString("id"),kind=input.getString("kind"),type=input.getString("type");
                if(!id.matches("[A-Za-z0-9_-]{8,80}")||!(kind.equals("water")||kind.equals("gas"))||!(type.equals("stock")||type.equals(kind.equals("gas")?"swap":"supply")))throw new IllegalArgumentException("Ungültiger Eintragstyp.");
                long at=input.getLong("at");double amount=input.getDouble("amount");checkAmount(amount,kind,type.equals("stock"));
                if(at<946684800000L||at>System.currentTimeMillis()+300000L)throw new IllegalArgumentException("Bitte einen vergangenen oder aktuellen Zeitpunkt wählen.");
                String note=input.optString("note","").trim();if(note.length()>240)throw new IllegalArgumentException("Die Notiz ist zu lang.");
                int found=-1;JSONObject old=null;for(int i=0;i<events.length();i++)if(id.equals(events.getJSONObject(i).getString("id"))){found=i;old=events.getJSONObject(i);break;}
                if(op.equals("edit")&&found<0)throw new IllegalArgumentException("Der Eintrag wurde inzwischen entfernt. Bitte neu laden.");
                JSONObject location=input.has("site")?site(input.getJSONObject("site")):old!=null?old.optJSONObject("site"):site(new JSONObject(repo.prefs.getString("weatherPlace","{}")));
                JSONObject event=new JSONObject().put("id",id).put("kind",kind).put("type",type).put("at",at).put("amount",amount).put("empty",input.optBoolean("empty",true)).put("note",note).put("order",old!=null?old.optLong("order"):value.optLong("revision")+1).put("site",location==null?new JSONObject():location);
                if(op.equals("add")&&found>=0){if(!same(old,event))throw new IllegalArgumentException("Diese Eintragskennung wurde bereits verwendet.");return;}
                if(found<0&&events.length()>=5000)throw new IllegalArgumentException("Das Journal enthält bereits 5000 Einträge. Bitte vor weiteren Einträgen sichern.");
                if(found>=0)events.put(found,event);else events.put(event);
                java.util.List<JSONObject> sorted=new java.util.ArrayList<>();for(int i=0;i<events.length();i++)sorted.add(events.getJSONObject(i));
                sorted.sort((a,b)->{int c=Long.compare(a.optLong("at"),b.optLong("at"));if(c!=0)return c;c=Long.compare(a.optLong("order"),b.optLong("order"));return c!=0?c:a.optString("id").compareTo(b.optString("id"));});
                JSONArray ordered=new JSONArray();for(JSONObject e:sorted)ordered.put(e);value.put("events",ordered);break;
            }
            default:throw new IllegalArgumentException("Unbekannte Gas-/Wasseraktion.");
        }
        value.put("revision",value.optLong("revision")+1).put("updated",System.currentTimeMillis());
        if(!repo.prefs.edit().putString(KEY,value.toString()).commit())throw new java.io.IOException("Der Eintrag konnte nicht gespeichert werden.");
        SuppliesWeather.refresh(repo,true);
    }
    private static boolean same(JSONObject a,JSONObject b){return a.optString("kind").equals(b.optString("kind"))&&a.optString("type").equals(b.optString("type"))&&a.optLong("at")==b.optLong("at")&&Double.compare(a.optDouble("amount"),b.optDouble("amount"))==0&&a.optBoolean("empty",true)==b.optBoolean("empty",true)&&a.optString("note").equals(b.optString("note"))&&a.optJSONObject("site").optString("key").equals(b.optJSONObject("site").optString("key"));}
    private static void checkAmount(double amount,String kind,boolean zero){if(!Double.isFinite(amount)||amount<(zero?0:0.01)||amount>(kind.equals("water")?10000:100))throw new IllegalArgumentException("Bitte eine gültige Menge in "+(kind.equals("water")?"Litern":"Kilogramm")+" eingeben.");}
    static synchronized void recordSite(Repository repo)throws Exception {
        JSONObject current=site(new JSONObject(repo.prefs.getString("weatherPlace","{}")));JSONArray sites=new JSONArray(repo.prefs.getString("supplies-sites-v1","[]"));
        JSONObject previous=sites.length()==0?null:sites.getJSONObject(sites.length()-1).optJSONObject("site");
        if(previous!=null&&previous.optString("key").equals(current.optString("key")))return;
        sites.put(new JSONObject().put("at",System.currentTimeMillis()).put("site",current));
        if(!repo.prefs.edit().putString("supplies-sites-v1",sites.toString()).commit())throw new java.io.IOException();
    }
    static JSONObject snapshot(Repository repo)throws JSONException {
        JSONObject value=read(repo.prefs).put("sites",new JSONArray(repo.prefs.getString("supplies-sites-v1","[]"))),weather=new JSONObject(repo.prefs.getString(WEATHER,"{}"));weather.remove("forecastArchive");
        JSONObject chosen=new JSONObject(repo.prefs.getString("weatherPlace","{}"));String chosenKey=chosen.length()==0?"":Math.round(chosen.optDouble("latitude")*1000)+":"+Math.round(chosen.optDouble("longitude")*1000);if(!chosenKey.equals(weather.optString("siteKey")))weather.put("forecast",new JSONArray()).put("siteKey",chosenKey);
        weather.put("enabled",!repo.prefs.getString("weatherPlace","").isEmpty()).put("pending",SuppliesWeather.busy()).put("error",repo.prefs.getString("suppliesWeatherError",""));
        return value.put("weather",weather);
    }
    private SuppliesStore(){}
}
