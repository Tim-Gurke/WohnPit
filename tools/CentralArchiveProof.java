package de.wattpit.app;

import org.json.*;
import java.nio.file.*;

/** Exercises persisted per-device scheduling through the real repository entry points. */
public final class CentralArchiveProof {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);System.out.println("PASS "+message);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        LanSyncProof.TestContext context=new LanSyncProof.TestContext(Path.of(args[0]));Repository repo=LanSyncProof.repository(context);
        String solar="AA:11:22:33:44:55",shunt="AA:11:22:33:44:66";
        repo.prefs.edit().putString("devices",new JSONArray().put(new JSONObject().put("mac",solar).put("kind","solar")).put(new JSONObject().put("mac",shunt).put("kind","shunt")).toString()).commit();
        long before=System.currentTimeMillis();
        check(repo.automaticArchiveDevices(false,before).length()==2,"a new plant can fetch its missing archive without waiting one hour");
        repo.archiveAttemptFinished(solar,false,true);
        JSONArray due=repo.automaticArchiveDevices(false,System.currentTimeMillis()+60000);
        check(due.length()==1&&due.getJSONObject(0).getString("mac").equals(shunt),"a successful solar archive cannot restart a minute later or suppress the independent shunt");
        repo.archiveAttemptFinished(shunt,false,false);
        check(repo.automaticArchiveDevices(false,System.currentTimeMillis()+60000).length()==0,"a failed shunt archive also waits instead of repeatedly opening Bluetooth connections");
        check(repo.requestedArchiveDevices("MANUAL").length()==2&&repo.requestedArchiveDevices(solar).length()==1,"explicit manual recovery remains available during automatic cooldowns");
        long nextSolar=repo.prefs.getLong("archiveNext."+solar,0),nextShunt=repo.prefs.getLong("archiveNext."+shunt,0);
        check(nextSolar>=before+3600000&&nextShunt>=before+3600000,"both success and failure persist at least one hour of quiet time");
        Repository reopened=LanSyncProof.repository(context);
        check(reopened.automaticArchiveDevices(false,Math.min(nextSolar,nextShunt)-1).length()==0,"reopening the central keeps the persisted quiet interval");
        check(reopened.automaticArchiveDevices(false,Math.max(nextSolar,nextShunt)).length()==2,"genuinely missing data become eligible again once the quiet interval ends");
        repo.archiveAttemptFinished(solar,true,false);
        check(repo.prefs.getLong("archiveNext."+solar,0)>=before+3600000,"explicit cancellation retains the existing selected one-hour pause");
        repo.saveArchiveInterval(15);
        long finished=repo.prefs.getLong("archiveFinished."+shunt,0);
        check(repo.prefs.getLong("archiveNext."+shunt,0)==finished+900000,"selecting fifteen minutes recalculates eligibility from the recorded completion time");
        check(repo.prefs.getLong("archiveNext."+solar,0)==repo.prefs.getLong("archiveFinished."+solar,0)+1800000,"the fifteen-minute option retains at least thirty minutes of quiet after cancellation");
        repo.saveArchiveInterval(30);
        check(repo.prefs.getLong("archiveNext."+shunt,0)==finished+1800000,"selecting thirty minutes persists the matching per-device interval");
        repo.saveArchiveInterval(0);
        check(repo.automaticArchiveDevices(false,System.currentTimeMillis()+3600000).length()==0&&repo.requestedArchiveDevices("MANUAL").length()==2,"manual-only disables automatic archive work while preserving explicit recovery");
        repo.saveArchiveInterval(60);
        check(repo.archiveIntervalMinutes()==60&&repo.prefs.getLong("archiveNext."+shunt,0)==finished+3600000,"hourly automatic recovery can be restored without restarting the central");
        long deadline=repo.prefs.getLong("archiveNext."+shunt,0);
        check(repo.automaticArchiveCheckDelay(deadline-20000)==20000,"the central wakes at the pending cooldown deadline rather than waiting a second full hour");
        repo.db.close();reopened.db.close();
    }
}
