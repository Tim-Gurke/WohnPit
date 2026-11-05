package de.wattpit.app;

import de.wattpit.app.core.LanWire;
import org.json.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Real encrypted host/client traffic while a viewer opens, closes and serves ScootPit. */
public final class ViewerIdleProof {
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS "+name);}
    private static Field field(String name)throws Exception{Field f=LanSync.class.getDeclaredField(name);f.setAccessible(true);return f;}
    private static int requests(LanSync host)throws Exception{Map<?,?> all=(Map<?,?>)field("requests").get(host);synchronized(all){return all.size();}}
    private static boolean scheduled(LanSync viewer)throws Exception{ScheduledFuture<?> f=(ScheduledFuture<?>)field("refreshTask").get(viewer);return f!=null&&!f.isCancelled();}
    private static void await(BooleanSupplier condition,String name)throws Exception{
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        while(!condition.getAsBoolean()){if(System.nanoTime()>end)throw new AssertionError(name);Thread.sleep(20);}
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path root=Path.of(args[0]);
        LanSyncProof.TestContext main=new LanSyncProof.TestContext(root.resolve("host"));Repository source=LanSyncProof.repository(main);source.running=true;
        RollerStore.get(main).configure(RollerStore.checked(new JSONObject()).put("mac","AA:BB:CC:DD:EE:FF").toString());
        LanSync host=LanSyncProof.sync(main),viewer=null,restarted=null;
        try{
            host.create("Idle-proof host");host.startHost();
            JSONObject invitation=new JSONObject(((JSONObject)field("config").get(host)).toString()).put("port",((java.net.ServerSocket)field("server").get(host)).getLocalPort()).put("addresses",new JSONArray().put("127.0.0.1"));
            LanSyncProof.TestContext display=new LanSyncProof.TestContext(root.resolve("viewer"));LanSyncProof.repository(display);viewer=LanSyncProof.sync(display);
            Field repository=Repository.class.getDeclaredField("instance");repository.setAccessible(true);repository.set(null,source);
            viewer.join(LanWire.encodeInvitation(invitation.toString()));final LanSync phone=viewer;
            check(!phone.backgroundPresenceEnabled()&&!scheduled(phone),"a paired, closed viewer has no automatic refresh task by default");
            LanService.reconcile(display,phone);
            check(display.startedServices==0&&display.stoppedServices>0,"a normal viewer stops its old plant service instead of starting a foreground service");

            long before=phone.state().getLong("receivedAt");Thread.sleep(5);
            phone.foreground(bundle->{});
            await(()->phone.state().optLong("receivedAt")>before,"foreground snapshot");
            check(scheduled(phone)&&phone.state().getBoolean("connected"),"opening the display immediately refreshes actual host data and starts the visible refresh loop");
            phone.background();check(!scheduled(phone),"closing the display cancels the refresh loop");
            // Drain any already queued work, then exercise repeated refresh requests as an NSD callback would.
            ((ScheduledExecutorService)field("client").get(phone)).submit(()->{}).get(8,TimeUnit.SECONDS);
            int idle=requests(host);long saved=phone.state().getLong("receivedAt");
            phone.refreshNow();phone.refreshNow();Thread.sleep(3400);
            check(requests(host)==idle&&phone.state().getLong("receivedAt")==saved,"a closed viewer sends no bundle requests and unsolicited discovery refreshes cannot resume polling");

            // Block the real host after authentication so the display closes with an RPC in flight.
            int next=requests(host)+1;long closedAt=phone.state().getLong("receivedAt");
            synchronized(host){
                phone.foreground(bundle->{});
                await(()->{try{return requests(host)>=next;}catch(Exception e){throw new RuntimeException(e);}},"authenticated in-flight request");
                phone.background();
            }
            ((ScheduledExecutorService)field("client").get(phone)).submit(()->{}).get(8,TimeUnit.SECONDS);
            check(phone.state().getLong("receivedAt")==closedAt&&!scheduled(phone),"a reply arriving after the display closes cannot refresh its cache or restart automatic work");

            JSONObject link=new JSONObject().put("mac","AA:BB:CC:DD:EE:FF").put("token","01234567-89ab-cdef-0123-456789abcdef").put("epoch",1);
            int start=requests(host);long unchanged=phone.state().getLong("receivedAt");
            JSONObject reply=phone.rollerLink("inspect",link);
            check(reply.getInt("protocol")==2&&reply.getBoolean("available")&&requests(host)==start+1,"ScootPit can still make an authenticated on-demand BMS request while the display is closed");
            check(!scheduled(phone)&&phone.state().getLong("receivedAt")==unchanged&&display.startedServices==0,"an on-demand BMS request neither fetches a display bundle nor starts a permanent viewer service");
            reply=phone.rollerLink("take",link);
            check(reply.getBoolean("available")&&RollerHandover.get(main).remoteOwns()&&source.running,"the existing BMS takeover reserves the reader from a closed viewer while central recording remains active");
            phone.rollerLink("cancel",link);

            phone.backgroundPresence(true);LanService.reconcile(display,phone);phone.backgroundViewer(true);
            check(phone.state().getBoolean("backgroundPresence")&&scheduled(phone)&&display.startedServices==1,"optional background absence reminders explicitly start their foreground service and refresh loop");
            long notifiedAt=phone.state().getLong("receivedAt");Thread.sleep(5);phone.refreshNow();
            await(()->phone.state().optLong("receivedAt")>notifiedAt,"background reminder snapshot");
            check(!host.backgroundPresenceEnabled(),"background reminder preference belongs to this display and does not modify its host");
            restarted=LanSyncProof.sync(display);
            check(restarted.backgroundPresenceEnabled()&&!scheduled(restarted),"the local reminder choice survives process restart without starting an unowned refresh task");
            phone.backgroundPresence(false);LanService.reconcile(display,phone);
            check(!scheduled(phone)&&!phone.state().getBoolean("backgroundPresence"),"turning background reminders off immediately cancels their automatic polling");

            long reopenAt=phone.state().getLong("receivedAt");Thread.sleep(5);phone.foreground(bundle->{});
            await(()->phone.state().optLong("receivedAt")>reopenAt,"reopened foreground snapshot");
            check(scheduled(phone)&&phone.state().getBoolean("connected"),"reopening the paused display reconnects with fresh host data");
            phone.background();LanService.reconcile(main,host);
            check(main.startedServices==1&&host.state().getBoolean("hosting")&&source.running,"host role retains its persistent service and recording independently of viewer pauses");
            LanSyncProof.TestContext direct=new LanSyncProof.TestContext(root.resolve("direct"));LanSyncProof.repository(direct);LanSync standalone=LanSyncProof.sync(direct);
            try{LanService.reconcile(direct,standalone);check(direct.startedServices==0&&!standalone.viewer(),"standalone use does not start an unrelated LAN foreground service");}finally{LanSyncProof.close(standalone);}
        }finally{if(viewer!=null)LanSyncProof.close(viewer);if(restarted!=null)LanSyncProof.close(restarted);LanSyncProof.close(host);}
    }
}
