package de.wattpit.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.*;

/** Explicit shared app operations. Device firmware, arbitrary methods and file paths are not exposed. */
final class PlantCommands {
    @SuppressLint("MissingPermission")
    static JSONArray bonded(Context context){
        JSONArray out=new JSONArray();try{BluetoothManager manager=context.getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
            if(adapter!=null)for(BluetoothDevice device:adapter.getBondedDevices())out.put(new JSONObject().put("mac",device.getAddress()).put("name",device.getName()));
        }catch(Exception ignored){}return out;
    }
    static void apply(Context context,Repository repo,SolarPlanning planning,String action,JSONArray args)throws Exception{
        boolean changesSolar=java.util.Arrays.asList("saveSolarInstallation","saveSolarSetup","saveSolarSite","weatherPlace","saveEquipment","saveDevice","removeDevice","importSettings").contains(action);
        if(changesSolar)planning.captureNative();
        switch(action){
            case "saveSolarInstallation":{
                arity(args,1);String json=args.getString(0);if(json.length()>70000)throw new IllegalArgumentException("Die Solaranlage ist zu groß.");JSONObject input=new JSONObject(json),equipment=Repository.checkedEquipment(input.getJSONObject("equipment")),setup=SolarPlanning.checked(input.getJSONObject("setup"),equipment),place=input.getJSONObject("place");
                if(place.length()>0)place=WeatherClient.checkedPlace(place);SharedPreferences.Editor edit=repo.prefs.edit().putString("equipment-v1",equipment.toString()).putString("solar-setup-v1",setup.toString()).remove("weatherCache").remove("weatherError");if(place.length()>0)edit.putString("weatherPlace",place.toString());if(!edit.commit())throw new java.io.IOException();WeatherClient.refresh(repo,true);break;
            }
            case "saveLoadConfiguration":arity(args,1);EnergyStore.configure(repo,args.getString(0));break;
            case "setLoadOutput":arity(args,1);bluetoothReady(context);EnergyStore.manual(context,args.getBoolean(0));break;
            case "submitChargePlan":arity(args,1);EnergyStore.permit(context,args.getString(0));break;
            case "saveCharger":arity(args,1);ChargerBle.configure(context,args.getString(0));break;
            case "chargerCommand":arity(args,1);ChargerBle.get(context).command(args.getString(0));break;
            case "saveAppearance":arity(args,1);if(!repo.prefs.edit().putString(AutarPieConfig.APPEARANCE,AutarPieConfig.appearance(args.getString(0)).toString()).commit())throw new java.io.IOException();break;
            case "saveCentralSettings":{
                arity(args,1);JSONObject value=new JSONObject(args.getString(0));int seconds=value.getInt("archiveSeconds");
                if(!java.util.Arrays.asList(0,900,1800,3600).contains(seconds))throw new IllegalArgumentException("Ungültiges Archivintervall.");
                repo.saveArchiveInterval(seconds/60);if(!repo.prefs.edit().putBoolean("autostart",value.getBoolean("autostart")).commit())throw new java.io.IOException();break;
            }
            case "saveSolarSetup":arity(args,2);planning.save(args.getString(0),args.getBoolean(1));break;
            case "saveSolarSite":arity(args,2);planning.saveSite(args.getString(0),args.getString(1));break;
            case "weatherPlace":arity(args,1);WeatherClient.place(repo,new JSONObject(args.getString(0)));break;
            case "requestWeatherTrend":arity(args,0);WeatherClient.trend(repo);break;
            case "refreshWeather":arity(args,0);WeatherClient.refresh(repo,true);break;
            case "saveEquipment":arity(args,1);if(args.getString(0).length()>20000)throw new IllegalArgumentException("Die Anlagenkonfiguration ist zu groß.");repo.saveEquipment(new JSONObject(args.getString(0)));WeatherClient.refresh(repo,true);break;
            case "savePresence":arity(args,1);PresenceStore.get(context).mode(args.getString(0));break;
            case "saveAbsence":arity(args,1);PresenceStore.get(context).save(args.getString(0));break;
            case "resolveAbsence":arity(args,2);PresenceStore.get(context).resolve(args.getString(0),args.getString(1));break;
            case "saveRoller":arity(args,1);RollerStore.get(context).configure(args.getString(0));break;
            case "saveSupplies":arity(args,1);SuppliesStore.command(repo,args.getString(0));break;
            case "refreshSuppliesWeather":arity(args,0);SuppliesWeather.refresh(repo,true);break;
            case "saveSpecialUses":arity(args,1);SpecialUses.save(repo.prefs,args.getString(0));planning.invalidateLearning(LearningWindow.since(System.currentTimeMillis()));break;
            case "rollerReconnect":arity(args,0);RollerHandover.get(context).reconnect();break;
            case "saveDevice":arity(args,1);repo.saveDevice(new JSONObject(args.getString(0)));break;
            case "removeDevice":arity(args,1);repo.removeDevice(args.getString(0));break;
            case "settings":{
                arity(args,1);JSONObject given=new JSONObject(args.getString(0)),shared=new JSONObject();
                for(String key:new String[]{"capacity","sampleSeconds"})if(given.has(key))shared.put(key,given.get(key));
                if(shared.length()!=given.length())throw new IllegalArgumentException("Diese Darstellungseinstellung gehört auf das jeweilige Handy.");
                repo.saveSettings(shared);break;
            }
            case "importSettings":arity(args,1);repo.importSharedConfiguration(args.getString(0));WeatherClient.refresh(repo,true);break;
            case "start":arity(args,0);bluetoothReady(context);context.startForegroundService(new Intent(context,MonitorService.class));break;
            case "stop":arity(args,0);repo.prefs.edit().putBoolean("recording",false).commit();context.stopService(new Intent(context,MonitorService.class));break;
            case "importArchive":case "importMissingArchive":{
                arity(args,action.equals("importArchive")?1:0);bluetoothReady(context);
                String mac=action.equals("importArchive")?args.getString(0):"MANUAL";
                if(!mac.equals("MANUAL")){boolean found=false;JSONArray devices=repo.configs();for(int i=0;i<devices.length();i++)if(mac.equals(devices.getJSONObject(i).optString("mac")))found=true;if(!found)throw new IllegalArgumentException("Dieses Gerät ist am Hauptgerät nicht eingerichtet.");}
                if(repo.archiveBusy||repo.liveBusy)throw new IllegalArgumentException("Ein Geräteabruf läuft bereits. Bitte kurz warten.");if(EnergyStore.holdsRegulator())throw new IllegalArgumentException("Vor dem Archivabruf die automatische Rollerladung ausschalten.");
                context.startForegroundService(new Intent(context,ArchiveService.class).putExtra("mac",mac));break;
            }
            case "cancelArchive":arity(args,0);if(repo.archiveBusy)context.startService(new Intent(context,ArchiveService.class).setAction("CANCEL"));break;
            case "backgroundArchive":arity(args,1);if(!repo.archiveRun.background(args.getLong(0)))throw new IllegalArgumentException("Dieser Synchronisationslauf ist bereits beendet. Bitte erneut versuchen.");break;
            default:throw new IllegalArgumentException("Diese Bedienaktion wird nicht unterstützt. Bitte beide Geräte aktualisieren.");
        }
        if(changesSolar){planning.captureNative();SuppliesWeather.refresh(repo,true);}
        repo.invalidateStats();planning.request();
    }
    private static void arity(JSONArray args,int count){if(args.length()!=count)throw new IllegalArgumentException("Ungültige Bedienanfrage.");}
    @SuppressLint("MissingPermission")
    private static void bluetoothReady(Context c){
        boolean allowed=Build.VERSION.SDK_INT>=31?c.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED&&c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED:c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
        if(!allowed)throw new IllegalArgumentException("Bitte AutarPie am Hauptgerät öffnen und die Bluetooth-Berechtigung erteilen.");
        BluetoothManager manager=c.getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();
        if(adapter==null||!adapter.isEnabled())throw new IllegalArgumentException("Bitte Bluetooth am Hauptgerät einschalten.");
    }
    private PlantCommands(){}
}
