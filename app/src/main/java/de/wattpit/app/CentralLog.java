package de.wattpit.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;

/** Bounded event log; no keys, pairing codes, raw frames or per-sample writes. */
final class CentralLog {
    private static final int LIMIT=120;
    static synchronized JSONArray read(Context context){try{return new JSONArray(context.getSharedPreferences("wohnpit-diagnostics",Context.MODE_PRIVATE).getString("events","[]"));}catch(JSONException ignored){return new JSONArray();}}
    static synchronized void add(Context context,String category,String message){try{
        SharedPreferences prefs=context.getSharedPreferences("wohnpit-diagnostics",Context.MODE_PRIVATE);JSONArray old=read(context);
        if(old.length()>0){JSONObject last=old.getJSONObject(old.length()-1);if(category.equals(last.optString("category"))&&message.equals(last.optString("message")))return;}
        JSONArray kept=new JSONArray();for(int i=Math.max(0,old.length()-LIMIT+1);i<old.length();i++)kept.put(old.get(i));
        kept.put(new JSONObject().put("at",System.currentTimeMillis()).put("category",category).put("message",message.substring(0,Math.min(240,message.length()))));prefs.edit().putString("events",kept.toString()).apply();
    }catch(JSONException ignored){}}
    private CentralLog(){}
}

