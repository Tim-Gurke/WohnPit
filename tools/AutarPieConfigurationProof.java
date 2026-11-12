package de.wattpit.app;

import org.json.*;
import java.nio.file.*;

/** Real JSON validation and SQLite input/analysis separation; no radio or Android boot claim. */
public final class AutarPieConfigurationProof {
    private static void check(boolean pass,String text){if(!pass)throw new AssertionError(text);System.out.println("PASS "+text);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");LanSyncProof.TestContext c=new LanSyncProof.TestContext(Path.of(args[0]));Repository repo=LanSyncProof.repository(c);
        JSONObject input=new JSONObject(Files.readString(Path.of("app/src/test/resources/dashboard-preset.json"))),board=ConfigurationBackup.dashboard(input);JSONObject surface=new JSONObject("{\"mode\":\"gradient\",\"first\":\"#112233\",\"second\":\"#334455\",\"angle\":275}");board.getJSONArray("tiles").getJSONObject(0).put("backgroundSurface",surface);
        check(ConfigurationBackup.dashboard(board).getJSONArray("tiles").getJSONObject(0).getJSONObject("backgroundSurface").getInt("angle")==275,"single-tile gradients survive the portable configuration validator");
        JSONArray pages=new JSONArray();for(String id:new String[]{"overview","storage","solar","supplies","model","history","devices","settings"})pages.put(new JSONObject().put("id",id).put("name",id).put("board",board));pages.put(new JSONObject().put("id","custom-winter").put("name","Winter").put("board",board));JSONObject workspace=AutarPieConfig.workspace(new JSONObject().put("pages",pages).put("navigation",new JSONArray("[\"overview\",\"custom-winter\"]")).toString());check(workspace.getJSONArray("pages").length()==9,"custom dashboards and navigation preserve the existing complete boards");
        JSONObject privateConfig=new JSONObject().put("mac","00:00:00:00:00:04").put("secret","synthetic-private-marker");repo.prefs.edit().putString(EnergyStore.CHARGER,privateConfig.toString()).commit();JSONObject publicConfig=ChargerBle.publicConfig(repo);check(publicConfig.optBoolean("hasAuth")&&!publicConfig.has("secret"),"608PD public state never returns its private handshake");
        RollerLedger ledger=new RollerLedger(c,"autarpie-raw-proof.db");long a=System.currentTimeMillis()-20000,b=a+10000;String source="synthetic-plant";ledger.rawAdd(source,"synthetic-bms",a,a,b,100,200,0,1);
        check(ledger.read(source,a,b).length()==0,"central raw collection does not calculate charger input energy");ledger.derive(a,b);JSONArray intervals=ledger.read(source,a,b);check(intervals.length()==1&&Math.abs(intervals.getJSONObject(0).getDouble("wh")-150.0/360)<.000001,"display-side analysis derives energy only from recorded finite BMS intervals");JSONObject summary=ledger.summary(source,a,b);check(summary.getLong("observedMillis")==10000&&summary.getLong("gapMillis")==0,"display summaries retain measured coverage and do not fill gaps");ledger.close();
        de.wattpit.app.core.Reading one=new de.wattpit.app.core.Reading();one.timestamp=a;one.device="sample-regulator";one.kind="solar";one.origin="live";one.period=10;one.voltage=12.0;one.load=2.0;repo.db.insert(one);de.wattpit.app.core.Reading two=new de.wattpit.app.core.Reading();two.timestamp=b;two.device=one.device;two.kind="solar";two.origin="live";two.period=10;two.voltage=12.0;two.load=2.0;repo.db.insert(two);JSONObject load=repo.db.energyData(a,b,"loadEnergy","all");check(Math.abs(load.getJSONObject("summary").getDouble("last")-24.0/360)<.000001,"load-output Wh integrate the saved output current without adding another consumption stream");repo.db.close();
    }
}
