package de.wattpit.app;

import android.content.*;
import de.wattpit.app.core.StartupPolicy;

/** Restores the saved operating state; a deliberately paused installation stays paused. */
public final class AutostartReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null)return;String action=intent.getAction();
        if(!Intent.ACTION_BOOT_COMPLETED.equals(action)&&!Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)&&!Intent.ACTION_USER_UNLOCKED.equals(action))return;
        android.os.UserManager users=context.getSystemService(android.os.UserManager.class);if(users!=null&&!users.isUserUnlocked())return;
        android.content.SharedPreferences prefs=context.getSharedPreferences("wattpit",0);String role=AppRole.infer(context);
        if(!StartupPolicy.restoreNetwork(prefs.getBoolean("autostart",true),role,true))return;
        try{
            LanService.reconcile(context,LanSync.get(context));
            if(StartupPolicy.restoreCollector(true,prefs.getBoolean("recording",false),role,true))context.startForegroundService(new Intent(context,MonitorService.class).setAction("AUTOSTART"));
            prefs.edit().putLong("autostartAt",System.currentTimeMillis()).remove("autostartError").apply();CentralLog.add(context,"Autostart","Gespeicherten Betriebszustand nach Neustart / Update wieder angefordert.");
        }catch(Exception e){prefs.edit().putString("autostartError","AutarPie öffnen und Bluetooth-/Hintergrundberechtigungen prüfen.").apply();CentralLog.add(context,"Autostart","Android hat den Hintergrundstart nicht freigegeben.");}
    }
}
