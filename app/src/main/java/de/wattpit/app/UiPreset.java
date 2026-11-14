package de.wattpit.app;

import android.content.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.*;

/** Presentation defaults only. No plant, site, device or operating settings. */
final class UiPreset {
    static final String MARKER="ui-defaults-304";
    static JSONObject read(Context context)throws Exception{
        try(InputStream in=context.getAssets().open("ui-preset.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(out.size()+n>512000)throw new IOException();out.write(block,0,n);}return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}
    }
    static void install(Context context,SharedPreferences prefs)throws Exception{
        if(prefs.contains(MARKER))return;
        boolean existing=prefs.contains("dashboard-v1")||prefs.contains(AutarPieConfig.WORKSPACE)||prefs.contains(AutarPieConfig.APPEARANCE)||prefs.contains("theme")||!prefs.getString("devices","[]").equals("[]")||prefs.getBoolean("recording",false);
        SharedPreferences.Editor edit=prefs.edit().putBoolean(MARKER,!existing);
        if(!existing){JSONObject p=read(context),settings=p.getJSONObject("settings"),view=p.getJSONObject("viewSettings");
            edit.putString("dashboard-v1",p.getJSONObject("dashboard").toString()).putString(AutarPieConfig.WORKSPACE,p.getJSONObject("workspace").toString()).putString(AutarPieConfig.APPEARANCE,p.getJSONObject("appearance").toString())
                .putString("theme",settings.getString("theme")).putString("palette",settings.getString("palette")).putString("miniRange",settings.getString("miniRange")).putInt("display-scale-v1",view.getInt("displayScale")).putString("roller-values-v1",view.getJSONArray("rollerDisplay").toString())
                .putBoolean(DashboardPreset.MARKER,true).putBoolean(DashboardPreset.LAYOUT_191,true);
        }
        if(!edit.commit())throw new IOException("Designvorlage konnte nicht gespeichert werden.");
    }
    private UiPreset(){}
}
