package de.wattpit.app;

import android.annotation.SuppressLint;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import de.wattpit.app.core.EnergyProtocol;
import org.json.*;
import java.util.*;

/** Dedicated 608PD link. The private handshake is provisioned locally, never shipped. */
@SuppressLint("MissingPermission")
final class ChargerBle {
    private static ChargerBle instance;
    static synchronized ChargerBle get(Context context){if(instance==null)instance=new ChargerBle(context.getApplicationContext());return instance;}
    private static final UUID SERVICE=uuid(0xaf00),DATA=uuid(0xaf01),AUTH=uuid(0xaf02),CCCD=uuid(0x2902);
    private static UUID uuid(int value){return UUID.fromString(String.format(Locale.ROOT,"%08x-0000-1000-8000-00805f9b34fb",value));}
    private final Context context;private final Repository repo;private final Handler main=new Handler(Looper.getMainLooper());
    private BluetoothGatt gatt;private BluetoothGattCharacteristic data,auth;private boolean enabled,ready,busy;private long lastReceive,lastConnect;private String expected="";private long expectedAt;
    private final ArrayDeque<Runnable> operations=new ArrayDeque<>();private Runnable nextStage;
    private ChargerBle(Context context){this.context=context;repo=Repository.get(context);}
    static void configure(Context context,String json)throws Exception{
        Repository repo=Repository.get(context);if(json.length()>4000)throw new IllegalArgumentException("Ungültiges Laderprofil.");JSONObject in=new JSONObject(json),old=new JSONObject(repo.prefs.getString(EnergyStore.CHARGER,"{}"));String mac=in.optString("mac").toUpperCase(Locale.ROOT),role=in.optString("role","model");boolean enabled=in.optBoolean("enabled");
        if(!mac.isEmpty()&&!BluetoothAdapter.checkBluetoothAddress(mac))throw new IllegalArgumentException("Bitte die Bluetooth-Adresse des 608PD eingeben.");if(!Arrays.asList("model","house-source","other").contains(role))throw new IllegalArgumentException("Ungültiger Energiepfad.");
        JSONObject clean=new JSONObject().put("mac",mac).put("name",in.optString("name","608PD").substring(0,Math.min(60,in.optString("name","608PD").length()))).put("enabled",enabled).put("role",role);
        String key=in.optString("auth","");if(!key.isEmpty()){if(!key.matches("[0-9a-fA-F]{32}"))throw new IllegalArgumentException("Die private Verbindungskennung muss 32 Hexzeichen haben.");byte[] b=new byte[16];for(int i=0;i<16;i++)b[i]=(byte)Integer.parseInt(key.substring(i*2,i*2+2),16);clean.put("secret",KeyVault.encrypt(b));}
        else if(mac.equals(old.optString("mac"))&&old.has("secret"))clean.put("secret",old.getString("secret"));
        if(enabled&&(mac.isEmpty()||!clean.has("secret")))throw new IllegalArgumentException("Für den 608PD zuerst Adresse und private Verbindung aus der eigenen Aufzeichnung übernehmen.");
        if(!repo.prefs.edit().putString(EnergyStore.CHARGER,clean.toString()).commit())throw new java.io.IOException();get(context).restart();
    }
    static JSONObject publicConfig(Repository repo)throws JSONException{JSONObject value=new JSONObject(repo.prefs.getString(EnergyStore.CHARGER,"{}"));value.put("hasAuth",value.has("secret"));value.remove("secret");return value;}
    private final BluetoothGattCallback callback=new BluetoothGattCallback(){
        @Override public void onConnectionStateChange(BluetoothGatt g,int status,int state){main.post(()->{if(g!=gatt)return;if(status==0&&state==BluetoothProfile.STATE_CONNECTED){if(!g.discoverServices())fail("Die Laderdienste konnten nicht gelesen werden.");}else if(state==BluetoothProfile.STATE_DISCONNECTED)fail("608PD getrennt; erneuter Versuch folgt.");});}
        @Override public void onServicesDiscovered(BluetoothGatt g,int status){main.post(()->{if(g!=gatt)return;BluetoothGattService service=g.getService(SERVICE);if(status!=0||service==null){fail("Kein unterstützter 608PD-Dienst.");return;}data=service.getCharacteristic(DATA);auth=service.getCharacteristic(AUTH);if(data==null||auth==null){fail("608PD-Schnittstelle unvollständig.");return;}subscribe(data);subscribe(auth);enqueue(()->authenticate());});}
        @Override public void onDescriptorWrite(BluetoothGatt g,BluetoothGattDescriptor d,int status){main.post(()->complete(g,status));}
        @Override public void onCharacteristicWrite(BluetoothGatt g,BluetoothGattCharacteristic c,int status){main.post(()->complete(g,status));}
        @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c,byte[] value){main.post(()->{if(g==gatt)receive(c.getUuid(),value);});}
        @SuppressWarnings("deprecation") @Override public void onCharacteristicChanged(BluetoothGatt g,BluetoothGattCharacteristic c){if(Build.VERSION.SDK_INT<33){byte[] value=c.getValue();main.post(()->{if(g==gatt)receive(c.getUuid(),value);});}}
    };
    void start(){main.post(()->{enabled=true;tick();});}
    void stop(){main.post(()->{enabled=false;close();});}
    private void restart(){main.post(()->{close();if(repo.running){enabled=true;lastConnect=0;tick();}});}
    private final Runnable poll=()->tick();
    private void tick(){
        main.removeCallbacks(poll);if(!enabled||!repo.running)return;try{
            JSONObject c=new JSONObject(repo.prefs.getString(EnergyStore.CHARGER,"{}"));if(!c.optBoolean("enabled"))return;
            long now=System.currentTimeMillis();if(gatt==null&&now-lastConnect>=15000){lastConnect=now;BluetoothManager manager=context.getSystemService(BluetoothManager.class);BluetoothAdapter adapter=manager==null?null:manager.getAdapter();if(adapter==null||!adapter.isEnabled()){EnergyStore.error(true,"Bluetooth ist ausgeschaltet.");}else gatt=adapter.getRemoteDevice(c.getString("mac")).connectGatt(context,false,callback,BluetoothDevice.TRANSPORT_LE,BluetoothDevice.PHY_LE_1M_MASK,main);}
            else if(gatt!=null&&!ready&&now-lastConnect>20000){fail("608PD-Anmeldung wurde nicht rechtzeitig bestätigt.");}
            else if(ready){if(lastReceive>0&&now-lastReceive>10000){fail("608PD antwortet nicht mehr.");}else enqueue(()->write(data,EnergyProtocol.chargerPoll()));}
            if(!expected.isEmpty()&&now-expectedAt>8000){EnergyStore.error(true,"Der Lader hat den angeforderten Betriebszustand noch nicht bestätigt.");expected="";}
        }catch(Exception e){fail("608PD-Verbindung nicht möglich. Einrichtung und Berechtigungen prüfen.");}
        main.postDelayed(poll,1000);
    }
    private void enqueue(Runnable action){if(operations.size()>8)return;operations.add(action);advance();}
    private void advance(){if(busy||gatt==null)return;Runnable action=operations.poll();if(action!=null)action.run();}
    private void complete(BluetoothGatt g,int status){if(g!=gatt)return;busy=false;if(status!=0){fail("Bluetooth-Auftrag vom 608PD abgelehnt.");return;}advance();}
    @SuppressWarnings("deprecation") private void subscribe(BluetoothGattCharacteristic c){enqueue(()->{BluetoothGattDescriptor d=c.getDescriptor(CCCD);if(d==null||!gatt.setCharacteristicNotification(c,true)){fail("608PD-Benachrichtigungen fehlen.");return;}busy=true;byte[] v=BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;boolean accepted;if(Build.VERSION.SDK_INT>=33)accepted=gatt.writeDescriptor(d,v)==BluetoothStatusCodes.SUCCESS;else{d.setValue(v);accepted=gatt.writeDescriptor(d);}if(!accepted)fail("608PD-Benachrichtigungen konnten nicht aktiviert werden.");});}
    @SuppressWarnings("deprecation") private void write(BluetoothGattCharacteristic c,byte[] value){if(gatt==null||c==null)return;busy=true;int type=(c.getProperties()&BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)!=0?BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;boolean accepted;if(Build.VERSION.SDK_INT>=33)accepted=gatt.writeCharacteristic(c,value,type)==BluetoothStatusCodes.SUCCESS;else{c.setWriteType(type);c.setValue(value);accepted=gatt.writeCharacteristic(c);}if(!accepted)fail("Bluetooth-Schreibauftrag nicht angenommen.");}
    private void authenticate(){try{JSONObject c=new JSONObject(repo.prefs.getString(EnergyStore.CHARGER,"{}"));byte[] key=KeyVault.decrypt(c.getString("secret"));if(key.length!=16)throw new IllegalArgumentException();byte[] packet=new byte[19];packet[0]=0x18;System.arraycopy(key,0,packet,1,16);write(auth,packet);Arrays.fill(key,(byte)0);}catch(Exception e){fail("Private 608PD-Verbindung erneut aus eigener Aufzeichnung übernehmen.");}}
    private void receive(UUID characteristic,byte[] value){try{
        if(AUTH.equals(characteristic)){if(value.length==2&&(value[0]&255)==0x19){if(value[1]!=0){fail("608PD hat die private Verbindung abgelehnt.");return;}enqueue(()->write(auth,new byte[]{(byte)0xe0}));}
            else if(value.length>=2&&(value[0]&255)==0xe1){ready=true;lastReceive=System.currentTimeMillis();}return;}
        EnergyProtocol.ChargerSample sample=EnergyProtocol.charger(value);if(sample==null)return;long now=System.currentTimeMillis();lastReceive=now;JSONArray cells=new JSONArray();for(double v:sample.cellVoltages)cells.put(v);
        EnergyStore.charger(new JSONObject().put("at",now).put("mode",sample.mode).put("current",sample.current).put("voltage",sample.voltage).put("elapsed",sample.elapsed).put("capacity",sample.capacity).put("cells",cells));
        if(expected.equals("stop")&&sample.mode==0||expected.equals("start")&&sample.mode==1)expected="";
    }catch(Exception e){EnergyStore.error(true,"608PD-Daten konnten nicht gelesen werden.");}}
    void command(String action)throws Exception{
        if(!action.equals("stop")&&!action.equals("start"))throw new IllegalArgumentException("Die Powerbank-Funktion bitte am 608PD starten.");
        if(!repo.running||!ready)throw new IllegalArgumentException("Zuerst den 608PD verbinden und aktuelle Werte abwarten.");
        if(action.equals("start")){JSONObject c=publicConfig(repo);if(!c.optString("role").equals("model"))throw new IllegalArgumentException("Wohnwagenladung bitte am 608PD mit dem passenden Batterieprofil starten.");}
        main.post(()->{expected=action;expectedAt=System.currentTimeMillis();enqueue(()->write(data,action.equals("stop")?EnergyProtocol.chargerStop():EnergyProtocol.chargerStart(3,4210,500)));});
    }
    private void fail(String text){EnergyStore.error(true,text);close();}
    private void close(){main.removeCallbacks(poll);ready=false;busy=false;operations.clear();BluetoothGatt old=gatt;gatt=null;data=null;auth=null;if(old!=null){try{old.disconnect();old.close();}catch(Exception ignored){}}if(enabled&&repo.running)main.postDelayed(poll,15000);}
}
