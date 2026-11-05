package de.wattpit.app;

import android.content.*;
import org.json.*;

/** Device-local operation mode; never synchronized with the plant's shared settings. */
final class AppRole {
    static final String PREFS="wohnpit-role",KEY="role";
    static boolean valid(String role){return role.equals("central")||role.equals("viewer")||role.equals("direct");}
    static String get(Context c){return c.getSharedPreferences(PREFS,0).getString(KEY,"");}
    static boolean viewerOnly(Context c){return get(c).equals("viewer");}
    static String infer(Context c){
        String saved=get(c);if(valid(saved))return saved;
        try{String role=new JSONObject(c.getSharedPreferences("wattpit-lan",0).getString("config","{}")).optString("role");if(role.equals("viewer"))return "viewer";if(role.equals("host"))return "central";}catch(JSONException ignored){}
        return c.getSharedPreferences("wattpit",0).getAll().isEmpty()?"":"direct";
    }
    static void save(Context c,String role)throws Exception{
        if(!valid(role))throw new IllegalArgumentException("Unbekannte Betriebsart.");
        LanSync.get(c).applyDeviceRole(role);
        if(!c.getSharedPreferences(PREFS,0).edit().putString(KEY,role).commit())throw new java.io.IOException("Betriebsart konnte nicht gespeichert werden.");
    }
    private AppRole(){}
}
