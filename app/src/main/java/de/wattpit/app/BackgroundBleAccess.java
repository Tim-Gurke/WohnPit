package de.wattpit.app;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/** Android 10/11 require a separate grant for BLE discovery after boot. */
final class BackgroundBleAccess {
    static boolean granted(Context context){return Build.VERSION.SDK_INT<29||Build.VERSION.SDK_INT>30||context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    static void offer(Activity activity){
        if(granted(activity)||activity.isFinishing()||activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED)return;
        SharedPreferences prefs=activity.getSharedPreferences("wattpit",0);
        if(!prefs.getBoolean("autostart",true)||prefs.getBoolean("background-ble-explained-304",false))return;
        new AlertDialog.Builder(activity).setTitle("Bluetooth nach dem Neustart")
            .setMessage("Damit AutarPie auf Android 10/11 auch nach einem Neustart bei geschlossenem Bildschirm Geräte findet, erlaube unter Berechtigungen → Standort → Immer zulassen. Der Standortdienst muss eingeschaltet bleiben. Diese Freigabe wird für die Bluetooth-Suche benötigt.")
            .setPositiveButton("Berechtigung einstellen",(d,w)->{
                prefs.edit().putBoolean("background-ble-explained-304",true).apply();
                if(Build.VERSION.SDK_INT==29)activity.requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},304);
                else activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+activity.getPackageName())));
            }).setNegativeButton("Später",(d,w)->prefs.edit().putBoolean("background-ble-explained-304",true).apply()).show();
    }
    private BackgroundBleAccess(){}
}
