package de.wattpit.app;

import de.wattpit.app.core.LanWire;
import org.json.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Production encrypted TCP and SQLite with an input update during the first read. */
public final class ViewerRefreshProof {
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);System.out.println("PASS "+label);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));Path root=Path.of(args[0]);
        LanSyncProof.TestContext central=new LanSyncProof.TestContext(root.resolve("central"));Repository source=LanSyncProof.repository(central);
        source.prefs.edit().putString("devices","[{\"kind\":\"solar\",\"mac\":\"solar\",\"name\":\"Solar\"},{\"kind\":\"shunt\",\"mac\":\"shunt\",\"name\":\"Shunt\"}]").putString("weatherCache","{\"version\":5,\"updated\":1}").commit();
        long to=System.currentTimeMillis()-60000,from=to-86400000;
        Method seed=ViewerAnalysisProof.class.getDeclaredMethod("seed",Repository.class,long.class,long.class);seed.setAccessible(true);seed.invoke(null,source,from,to);
        LanSync host=LanSyncProof.sync(central),viewer=null;boolean success=false;
        try{
            host.create("Continuous input proof");host.startHost();Field c=LanSync.class.getDeclaredField("config"),s=LanSync.class.getDeclaredField("server");c.setAccessible(true);s.setAccessible(true);
            Method read=LanSync.class.getDeclaredMethod("read",String.class,JSONArray.class);read.setAccessible(true);read.invoke(host,"bundle",new JSONArray());
            String invitation=LanWire.encodeInvitation(new JSONObject(c.get(host).toString()).put("port",((java.net.ServerSocket)s.get(host)).getLocalPort()).put("addresses",new JSONArray().put("127.0.0.1")).toString());
            LanSyncProof.TestContext display=new LanSyncProof.TestContext(root.resolve("display"));LanSyncProof.repository(display);viewer=LanSyncProof.sync(display);viewer.join(invitation);
            JSONArray query=new JSONArray().put(from).put(to).put("all").put(from).put(to);AtomicBoolean changed=new AtomicBoolean();
            JSONObject first=viewer.query("energyHistoryWindow",query,message->{if(changed.compareAndSet(false,true))source.prefs.edit().putString("weatherCache","{\"version\":5,\"updated\":2}").commit();});
            check(changed.get(),"the central input really changes during the cold encrypted statistics read");
            check(first.optBoolean("stale")&&first.getJSONObject("analysis").optBoolean("inputsChanged"),"a complete first calculation survives a newer central input with an explicit old-data marker");
            for(String key:new String[]{"solar","consumption","balance","mobile"})check(first.getJSONObject(key).getJSONObject("summary").has("last")&&first.getJSONObject(key).optBoolean("stale"),"the completed "+key+" series remains available while new inputs wait");
            JSONObject fresh=viewer.query("energyHistoryWindow",query);
            check(!fresh.optBoolean("stale")&&!fresh.getJSONObject("analysis").optBoolean("inputsChanged"),"the next incremental read settles on the newer input without a stale marker");
            check(fresh.getJSONObject("solar").getJSONObject("summary").getDouble("last")>0&&fresh.getJSONObject("consumption").getJSONObject("summary").getDouble("last")>0,"real imported solar and household consumption are available after the refresh");
            Files.createDirectories(root);Files.writeString(root.resolve("statistics.json"),new JSONObject().put("from",from).put("to",to).put("first",first).put("fresh",fresh).toString());
            Field analysis=LanSync.class.getDeclaredField("analysis");analysis.setAccessible(true);Object replica=analysis.get(viewer);Field progress=ViewerAnalysis.class.getDeclaredField("progressAt");progress.setAccessible(true);progress.setLong(replica,0);
            AtomicBoolean replaced=new AtomicBoolean();boolean rejected=false;
            try{viewer.query("history",new JSONArray().put(from).put(to).put("power").put("all"),message->{if(message.startsWith("Auswertung")&&replaced.compareAndSet(false,true))source.db.getWritableDatabase().execSQL("UPDATE analysis_meta SET epoch='replacement-during-read'");});}catch(java.io.IOException expected){rejected=expected.getMessage().contains("Datenbestand");}
            check(replaced.get()&&rejected,"replacing the source database during a calculation still rejects its old result");
            success=true;
        }catch(Throwable failure){failure.printStackTrace();}finally{if(viewer!=null)LanSyncProof.close(viewer);LanSyncProof.close(host);System.exit(success?0:1);}
    }
}
