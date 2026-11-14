package de.wattpit.app;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import junit.framework.TestCase;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import de.wattpit.app.core.Reading;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;

/** Runs only against the isolated debug installation in the Android emulator. */
public final class UnifiedSmokeTest extends TestCase {
    private Instrumentation getInstrumentation(){return androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();}
    private Context target(){return getInstrumentation().getTargetContext();}
    private void stopped()throws Exception{Context c=target();c.stopService(new Intent(c,MonitorService.class));c.stopService(new Intent(c,ArchiveService.class));c.stopService(new Intent(c,LanService.class));Repository repo=Repository.get(c);repo.running=false;repo.archiveBusy=false;repo.prefs.edit().putBoolean("recording",false).remove("weatherPlace").commit();}
    public void testCentralUiDoesNotWaitForRegulatorMonitor()throws Exception{
        stopped();Context c=target();Repository repo=Repository.get(c);String devices=repo.prefs.getString("devices","[]");String mac="00:00:00:00:00:02";
        repo.prefs.edit().putString("devices","[{\"kind\":\"solar\",\"mac\":\"00:00:00:00:00:02\",\"name\":\"Testregler\"}]").commit();
        EnergyStore.live(repo,mac,new org.json.JSONObject().put("at",System.currentTimeMillis()).put("state",1).put("current",8.4).put("voltage",13));
        Activity central=select("Zentrale",CentralActivity.class);CountDownLatch held=new CountDownLatch(1),release=new CountDownLatch(1);AtomicBoolean responsive=new AtomicBoolean();
        Thread blocker=new Thread(()->{synchronized(EnergyStore.class){held.countDown();try{release.await(1500,TimeUnit.MILLISECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}},"test-regulator-busy");blocker.setDaemon(true);blocker.start();assertTrue(held.await(2,TimeUnit.SECONDS));
        try{
            Method refresh=CentralActivity.class.getDeclaredMethod("update");refresh.setAccessible(true);long start=System.nanoTime();
            getInstrumentation().runOnMainSync(()->{try{refresh.invoke(central);responsive.set(true);}catch(Exception e){throw new AssertionError(e);}});
            assertTrue("central UI waited for regulator monitor",responsive.get()&&TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<750);
        }finally{release.countDown();blocker.join(2500);}
        try{
            Field output=CentralActivity.class.getDeclaredField("output");output.setAccessible(true);long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);AtomicBoolean received=new AtomicBoolean();
            while(System.nanoTime()<until&&!received.get()){getInstrumentation().runOnMainSync(()->{try{received.set(((TextView)output.get(central)).getText().toString().contains("8,40 A"));}catch(Exception e){throw new AssertionError(e);}});if(!received.get())Thread.sleep(50);}
            assertTrue("confirmed current must reach the native console after the busy monitor is released",received.get());
        }finally{repo.prefs.edit().putString("devices",devices).commit();getInstrumentation().runOnMainSync(central::finish);getInstrumentation().waitForIdleSync();}
    }
    public void testEncryptedTakeoverBeforeChangingCurrentData()throws Exception{
        stopped();Context c=target();Repository repo=Repository.get(c);String mac="AA:00:00:00:00:77";Reading sample=new Reading();sample.device=mac;sample.kind="shunt";sample.timestamp=1770000000000L;sample.power=12.3;sample.period=10;repo.db.insert(sample);
        repo.prefs.edit().putString("proof-private-key","local-example-device-key").putLong("proof-long",123456789123L).putStringSet("proof-set",new HashSet<>(Arrays.asList("one","two"))).putString(SuppliesStore.KEY,"{\"version\":1,\"events\":[]}").commit();
        char[] password="Nur-ein-Testkennwort-123".toCharArray();ByteArrayOutputStream out=new ByteArrayOutputStream();MigrationBackup.export(new ContextWrapper(c){@Override public String getPackageName(){return "de.wohnpit.app";}},out,password);byte[] encrypted=out.toByteArray();assertFalse(new String(encrypted,"ISO-8859-1").contains("local-example-device-key"));
        repo.prefs.edit().putString("proof-private-key","target-before-import").putString("proof-target-only","preserve-until-checked").commit();
        try{MigrationBackup.prepare(c,new ByteArrayInputStream(encrypted),"Falsches-Kennwort-123".toCharArray());fail("Wrong password must fail");}catch(IOException expected){}
        assertEquals("target-before-import",repo.prefs.getString("proof-private-key",""));assertFalse(MigrationBackup.pending(c));
        MigrationBackup.prepare(c,new ByteArrayInputStream(encrypted),password);assertFalse(MigrationBackup.pending(c));assertEquals("target-before-import",repo.prefs.getString("proof-private-key",""));MigrationBackup.schedule(c);assertTrue(MigrationBackup.pending(c));assertEquals("target-before-import",repo.prefs.getString("proof-private-key",""));
        try{MigrationBackup.applyPending(c);fail("Same-process import requires cold restart");}catch(IOException expected){}
        repo.db.close();Field staged=MigrationBackup.class.getDeclaredField("stagedHere");staged.setAccessible(true);staged.setBoolean(null,false);MigrationBackup.applyPending(c);
        assertFalse(MigrationBackup.pending(c));assertEquals("local-example-device-key",repo.prefs.getString("proof-private-key",""));assertFalse(repo.prefs.contains("proof-target-only"));assertEquals(123456789123L,repo.prefs.getLong("proof-long",0));assertEquals(2,repo.prefs.getStringSet("proof-set",Collections.emptySet()).size());assertFalse(repo.prefs.getBoolean("recording",true));assertEquals(12.3,repo.db.last(mac).power,.001);
        byte[] damaged=encrypted.clone();damaged[damaged.length-1]^=1;try{MigrationBackup.prepare(c,new ByteArrayInputStream(damaged),password);fail("Changed final authentication must fail");}catch(IOException expected){}assertFalse(MigrationBackup.pending(c));assertEquals("local-example-device-key",repo.prefs.getString("proof-private-key",""));
        assertFalse(MigrationBackup.safeEntry("databases/../../files/other"));assertFalse(MigrationBackup.safeEntry("/absolute"));assertFalse(MigrationBackup.safeEntry("files/unexpected"));Arrays.fill(password,'\0');
    }
    public void testGeneratedDeviceIdStillShowsFirstRoleChoice()throws Exception{
        String prefix="inference-"+UUID.randomUUID()+"-";Context c=new ContextWrapper(target()){@Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+name,mode);}};
        c.getSharedPreferences("wattpit-lan",0).edit().putString("deviceId","example-generated-id").commit();assertEquals("",AppRole.infer(c));
        c.getSharedPreferences("wattpit-lan",0).edit().putString("config","{\"role\":\"viewer\"}").commit();assertEquals("viewer",AppRole.infer(c));
    }
    public void testNewDefaultsAndExistingLayouts()throws Exception{
        Context c=target();android.content.SharedPreferences fresh=c.getSharedPreferences("test-ui-preset-304",0);fresh.edit().clear().commit();
        UiPreset.install(c,fresh);assertEquals(75,fresh.getInt("display-scale-v1",0));assertEquals(21,new org.json.JSONObject(fresh.getString("dashboard-v1","{}")).getJSONArray("tiles").length());
        assertFalse(fresh.contains("devices"));assertFalse(fresh.contains("weatherPlace"));assertFalse(fresh.contains("equipment-v1"));assertFalse(fresh.contains("recording"));
        fresh.edit().putString("dashboard-v1","custom-layout").commit();UiPreset.install(c,fresh);assertEquals("custom-layout",fresh.getString("dashboard-v1",""));
        fresh.edit().clear().putString("dashboard-v1","existing-layout").putInt("display-scale-v1",110).commit();UiPreset.install(c,fresh);assertEquals("existing-layout",fresh.getString("dashboard-v1",""));assertEquals(110,fresh.getInt("display-scale-v1",0));fresh.edit().clear().commit();
    }
    private Button button(View view,String text){if(view instanceof Button&&((Button)view).getText().toString().startsWith(text))return (Button)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=button(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
    private boolean web(View view){if(view instanceof WebView)return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(web(group.getChildAt(i)))return true;}return false;}
    private Activity select(String label,Class<? extends Activity> type)throws Exception{
        Instrumentation in=getInstrumentation();Instrumentation.ActivityMonitor monitor=in.addMonitor(type.getName(),null,false);
        Activity launcher=in.startActivitySync(new Intent(target(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("chooseRole",true));in.waitForIdleSync();
        in.runOnMainSync(()->{Button button=button(launcher.getWindow().getDecorView(),label);assertNotNull(button);button.performClick();});Activity activity=in.waitForMonitorWithTimeout(monitor,10000);in.removeMonitor(monitor);assertNotNull(activity);in.waitForIdleSync();return activity;
    }
    private void screenshot(String name)throws Exception{File file=new File(target().getFilesDir(),name);Bitmap bitmap=getInstrumentation().getUiAutomation().takeScreenshot();try(OutputStream out=new FileOutputStream(file)){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();}
    public void testThreeRolesAndStandaloneTransport()throws Exception{
        stopped();Context c=target();Activity display=select("Eigenständig",DisplayActivity.class);assertEquals("direct",AppRole.get(c));assertFalse(LanSync.get(c).host());assertFalse(LanSync.get(c).viewer());assertTrue(web(display.getWindow().getDecorView()));screenshot("autarpie-3.0.5-direct.png");
        // Switching is an explicit confirmed action when a role was already set.
        c.getSharedPreferences(AppRole.PREFS,0).edit().remove(AppRole.KEY).commit();Activity central=select("Zentrale",CentralActivity.class);assertEquals("central",AppRole.get(c));assertTrue(LanSync.get(c).host());assertFalse(web(central.getWindow().getDecorView()));screenshot("autarpie-3.0.5-central.png");String host= c.getSharedPreferences("wattpit-lan",0).getString("config","");
        c.getSharedPreferences(AppRole.PREFS,0).edit().remove(AppRole.KEY).commit();Activity viewer=select("Anzeigehandy",DisplayActivity.class);assertEquals("viewer",AppRole.get(c));assertTrue(AppRole.viewerOnly(c));assertFalse(LanSync.get(c).host());assertTrue(web(viewer.getWindow().getDecorView()));
        c.startService(new Intent(c,MonitorService.class));Thread.sleep(500);assertFalse(Repository.get(c).running);assertFalse(Repository.get(c).prefs.getBoolean("recording",true));
        AppRole.save(c,"central");assertEquals(host,c.getSharedPreferences("wattpit-lan",0).getString("config",""));AppRole.save(c,"direct");assertFalse(LanSync.get(c).host());assertFalse(LanSync.get(c).viewer());
        getInstrumentation().runOnMainSync(viewer::finish);
    }
}
