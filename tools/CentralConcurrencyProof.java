package de.wattpit.app;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.json.*;

/** Exercises the actual repository/energy monitors in the formerly reversed order. */
public final class CentralConcurrencyProof {
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");
        LanSyncProof.TestContext context=new LanSyncProof.TestContext(Path.of(args[0]));
        Repository repo=LanSyncProof.repository(context);
        repo.prefs.edit().putString("devices","[{\"kind\":\"solar\",\"mac\":\"00:00:00:00:00:02\",\"name\":\"Synthetic regulator\"}]").commit();
        EnergyStore.config(repo);
        CountDownLatch repositoryHeld=new CountDownLatch(1),energyHeld=new CountDownLatch(1);
        AtomicReference<Throwable> failure=new AtomicReference<>();
        Thread snapshot=new Thread(()->{try {
            synchronized(repo) {
                repositoryHeld.countDown();
                if(!energyHeld.await(2,TimeUnit.SECONDS))throw new AssertionError("energy worker did not start");
                repo.snapshot();
            }
        } catch(Throwable error){failure.compareAndSet(null,error);}},"central-snapshot");
        Thread regulator=new Thread(()->{try {
            if(!repositoryHeld.await(2,TimeUnit.SECONDS))throw new AssertionError("snapshot worker did not start");
            synchronized(EnergyStore.class) {
                energyHeld.countDown();
                EnergyStore.output(repo);
            }
        } catch(Throwable error){failure.compareAndSet(null,error);}},"central-regulator");
        snapshot.setDaemon(true);regulator.setDaemon(true);snapshot.start();regulator.start();
        snapshot.join(3000);regulator.join(1000);
        if(snapshot.isAlive()||regulator.isAlive()) {
            for(ThreadInfo info:ManagementFactory.getThreadMXBean().getThreadInfo(new long[]{snapshot.getId(),regulator.getId()},true,true))System.err.println(info);
            throw new AssertionError("central snapshot and regulator read deadlock; live collection and UI cannot progress");
        }
        if(failure.get()!=null)throw new AssertionError("concurrent central read failed",failure.get());
        System.out.println("PASS central snapshot and regulator query complete under the former inverted monitor schedule");
        synchronized(repo) {
            AtomicReference<JSONArray> result=new AtomicReference<>();
            Thread reader=new Thread(()->{try{result.set(repo.configs());}catch(Exception e){failure.set(e);}});
            reader.setDaemon(true);reader.start();reader.join(1000);
            if(reader.isAlive()||result.get()==null)throw new AssertionError("immutable preference reads must not wait for the repository monitor");
        }
        System.out.println("PASS reading configured devices never waits behind an unrelated repository operation");
        RollerLedger ledger=RollerLedger.get(context);
        Thread archive=new Thread(()->{try{EnergyStore.live(repo,"00:00:00:00:00:02",new JSONObject().put("at",System.currentTimeMillis()).put("state",1).put("current",8.4).put("voltage",13));}catch(Throwable e){failure.set(e);}},"central-load-archive");
        archive.setDaemon(true);
        AtomicReference<JSONObject> current=new AtomicReference<>();
        Thread display=new Thread(()->{try{current.set(EnergyStore.output(repo));}catch(Throwable e){failure.set(e);}},"central-output-state");
        display.setDaemon(true);
        synchronized(ledger) {
            archive.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
            while(archive.getState()!=Thread.State.BLOCKED&&archive.isAlive()&&System.nanoTime()<deadline)Thread.sleep(1);
            if(archive.getState()!=Thread.State.BLOCKED)throw new AssertionError("archive worker did not reach the held ledger");
            display.start();display.join(1000);
            if(display.isAlive()||current.get()==null||current.get().optDouble("current")!=8.4)throw new AssertionError("slow archive I/O must not hide freshly received output current");
        }
        archive.join(3000);if(archive.isAlive()||failure.get()!=null)throw new AssertionError("load archive did not finish after release",failure.get());
        System.out.println("PASS live output current remains readable while its archive storage is deliberately blocked");
        repo.db.close();
    }
}
