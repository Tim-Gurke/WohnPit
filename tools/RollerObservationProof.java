package de.wattpit.app;

import de.wattpit.app.core.BmsPacket;
import org.json.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

public final class RollerObservationProof {
    static final long H=3600000L,D=24*H;
    static void reset(Class<?> type,String field,Object value)throws Exception{Field f=type.getDeclaredField(field);f.setAccessible(true);f.set(null,value);}
    static BmsPacket idle(){byte[] bytes=new byte[30];bytes[0]=(byte)0xdd;bytes[1]=3;bytes[3]=23;bytes[4]=0x13;bytes[5]=(byte)0x88;bytes[8]=0x07;bytes[9]=(byte)0xd0;bytes[10]=0x0a;bytes[11]=0x28;bytes[23]=75;bytes[24]=3;int sum=0;for(int i=2;i<27;i++)sum+=bytes[i]&255;int checksum=(-sum)&65535;bytes[27]=(byte)(checksum>>8);bytes[28]=(byte)checksum;bytes[29]=0x77;BmsPacket result=BmsPacket.decode(bytes);if(result==null)throw new AssertionError("Invalid neutral packet");return result;}
    static JSONObject scenario(Path root,String role)throws Exception{
        LanSyncProof.TestContext ctx=new LanSyncProof.TestContext(root);
        Repository repo=LanSyncProof.repository(ctx);
        ctx.getSharedPreferences(AppRole.PREFS,0).edit().putString(AppRole.KEY,role).commit();
        reset(RollerLedger.class,"instance",null);
        reset(SpecialUses.class,"observed",null);reset(SpecialUses.class,"observedMac","");reset(SpecialUses.class,"persisted",0L);
        Constructor<RollerStore> ctor=RollerStore.class.getDeclaredConstructor(android.content.Context.class);ctor.setAccessible(true);RollerStore store=ctor.newInstance(ctx);
        JSONObject profile=RollerStore.checked(new JSONObject()).put("mac","AA:BB:CC:DD:EE:FF");store.configure(profile.toString());
        BmsPacket sample=idle();long today=System.currentTimeMillis()/D*D;
        for(int age=3;age>=1;age--){long start=today-age*D;for(long t=start;t<=start+20*H+120000;t+=10000)store.accept(sample,t);}
        JSONObject special=SpecialUses.snapshot(repo.prefs),days=special.getJSONObject("rollerDays");
        if(role.equals("central")){RollerLedger ledger=RollerLedger.get(ctx);if(days.length()!=0)throw new AssertionError("Central must only collect raw data");JSONObject derived=ledger.observedDays(RollerLedger.source(repo.prefs),profile.getString("mac"),today-4*D,today,"UTC");days=derived.getJSONObject("days");special.put("rollerDays",days);JSONObject again=ledger.observedDays(RollerLedger.source(repo.prefs),profile.getString("mac"),today-4*D,today,"UTC");if(!derived.toString().equals(again.toString()))throw new AssertionError("Repeated derivation must be idempotent");ledger.derive(today-4*D,today);if(ledger.read(RollerLedger.source(repo.prefs),today-4*D,today).length()!=0)throw new AssertionError("Observed idle days must not become charge input");System.out.println("PASS Central retains three raw observed idle days; display derivation repeats without adding charge energy");}
        JSONObject snapshot=store.snapshot();if(snapshot.optBoolean("fresh")||snapshot.getJSONObject("lastKnown").optLong("at")<=0)throw new AssertionError("Old BMS readings remain cached but never fresh");
        store.disconnected();if(store.snapshot().getJSONObject("lastKnown").length()==0)throw new AssertionError("Disconnect must preserve last BMS reading");
        RollerLedger oldLedger=RollerLedger.get(ctx);long oldAt=today-2*D;
        JSONObject before=new JSONObject().put("at",oldAt-10000).put("voltage",49).put("current",2).put("soc",68),after=new JSONObject().put("at",oldAt).put("voltage",49).put("current",2).put("soc",69);
        oldLedger.rawAdd(RollerLedger.source(repo.prefs),profile.getString("mac"),oldAt-10000,oldAt-10000,oldAt,98,98,0,.9,before,after);
        ctx.getSharedPreferences("wattpit-roller",0).edit().remove("lastReading").remove("lastReadingMigrationDone").commit();RollerStore upgraded=ctor.newInstance(ctx);JSONObject restored=upgraded.snapshot();
        if(restored.optBoolean("fresh")||restored.getJSONObject("lastKnown").optInt("soc")!=69||restored.getJSONObject("lastKnown").optLong("at")!=oldAt)throw new AssertionError("Upgrade must recover original recorded BMS fields without marking them live");
        System.out.println("PASS "+role+" upgrade recovers the recorded BMS state while the roller is unreachable");
        int eligible=0;for(Iterator<String> keys=days.keys();keys.hasNext();)if(days.getJSONObject(keys.next()).optLong("millis")>=20*H)eligible++;
        System.out.println("OBSERVED "+role+": 3 days with over 20 h of valid BMS packets per day; stored eligible days="+eligible);
        return new JSONObject().put("role",role).put("eligibleDays",eligible).put("special",special).put("profile",profile);
    }
    public static void main(String[] args){try{Class.forName("org.sqlite.JDBC");TimeZone.setDefault(TimeZone.getTimeZone("UTC"));Path root=Path.of(args[0]);Files.createDirectories(root);JSONObject central=scenario(root.resolve("central"),"central"),direct=scenario(root.resolve("direct"),"direct");
        if(central.getInt("eligibleDays")!=3||direct.getInt("eligibleDays")!=3)throw new AssertionError("Unexpected native observation result");
        Files.writeString(root.resolve("result.json"),new JSONObject().put("now",System.currentTimeMillis()).put("central",central).put("direct",direct).toString());
        System.out.println("PASS Central and direct observation both supply three known days; unknown days are never fabricated");System.exit(0);
    }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
