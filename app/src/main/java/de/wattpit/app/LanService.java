package de.wattpit.app;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;

/** Keeps a host available; viewers run this service only for opted-in absence reminders. */
public final class LanService extends Service {
    private static final String CHANNEL="wattpit-lan";
    private PowerManager.WakeLock presenceWake;
    private final java.util.concurrent.ScheduledExecutorService presence=java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    private boolean maintenanceStarted;
    static void reconcile(Context context,LanSync lan){
        Intent service=new Intent(context,LanService.class);
        if(lan.host()||lan.viewer()&&lan.backgroundPresenceEnabled())context.startForegroundService(service);
        else context.stopService(service);
    }
    @android.annotation.SuppressLint("WakelockTimeout")
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        LanSync lan=LanSync.get(this);if(!lan.host()&&(!lan.viewer()||!lan.backgroundPresenceEnabled())){stopSelf();return START_NOT_STICKY;}
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Anlagenverbund",NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_bolt).setContentTitle("AutarPie · Anlagenverbund")
            .setContentText(lan.viewer()?"Abwesenheitshinweise auch bei geschlossener Anzeige":"Hauptgerät im lokalen WLAN verfügbar").setContentIntent(open).setOngoing(true).build();
        try{
            if(Build.VERSION.SDK_INT>=29)startForeground(12,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);else startForeground(12,notification);
            if(lan.host()){
                if(presenceWake==null){PowerManager power=getSystemService(PowerManager.class);if(power!=null){presenceWake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"wattpit:presence");presenceWake.setReferenceCounted(false);presenceWake.acquire();}}
                if(!maintenanceStarted){maintenanceStarted=true;presence.scheduleWithFixedDelay(()->{try{PresenceStore.get(this).tick();lan.backgroundInputs();}catch(Exception ignored){}},0,10,java.util.concurrent.TimeUnit.SECONDS);}
                lan.startHost();
            }else lan.backgroundViewer(true);
        }catch(Exception e){lan.hostFailed();stopSelf();return START_NOT_STICKY;}
        return START_STICKY;
    }
    @Override public void onDestroy(){presence.shutdownNow();if(presenceWake!=null&&presenceWake.isHeld())presenceWake.release();LanSync lan=LanSync.get(this);if(lan.host())lan.stopHost();else lan.backgroundViewer(false);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
