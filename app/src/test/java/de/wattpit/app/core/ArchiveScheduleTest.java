package de.wattpit.app.core;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArchiveScheduleTest {
    @Test public void timeAloneNeverStartsAnArchiveConnection(){
        assertFalse(ArchiveSchedule.automaticDue(0,1000000,false));
        assertTrue(ArchiveSchedule.automaticDue(0,1000000,true));
    }
    @Test public void missingDataStillRespectTheCancellationPause(){
        assertFalse(ArchiveSchedule.automaticDue(ArchiveSchedule.nextAutomatic(1000,true),1001,true));
    }
    @Test public void repeatedForegroundEntriesCannotRestartDuringCooldown(){
        long next=ArchiveSchedule.nextAutomatic(1000,false);
        assertFalse(ArchiveSchedule.automaticDue(next,31001));assertFalse(ArchiveSchedule.automaticDue(next,300999));
        assertFalse(ArchiveSchedule.automaticDue(next,3600999));assertTrue(ArchiveSchedule.automaticDue(next,3601000));
    }
    @Test public void explicitCancellationRespectsTheDefaultHour(){
        long next=ArchiveSchedule.nextAutomatic(1000,true);
        assertFalse(ArchiveSchedule.automaticDue(next,1001));assertFalse(ArchiveSchedule.automaticDue(next,3600999));assertTrue(ArchiveSchedule.automaticDue(next,3601000));
    }
    @Test public void oneDevicesCooldownCannotSuppressAnotherDevice(){
        assertFalse(ArchiveSchedule.automaticDue(ArchiveSchedule.nextAutomatic(1000,false),1001));assertTrue(ArchiveSchedule.automaticDue(0,1001));
    }
    @Test public void aLargeBackwardPhoneClockChangeCannotBlockSyncIndefinitely(){
        assertTrue(ArchiveSchedule.automaticDue(7200000,1000));
    }
    @Test public void selectableIntervalsPreserveCancellationAndManualOnly(){
        assertEquals(0,ArchiveSchedule.intervalMillis(0));
        assertEquals(900000,ArchiveSchedule.intervalMillis(15));assertEquals(1800000,ArchiveSchedule.intervalMillis(30));assertEquals(3600000,ArchiveSchedule.intervalMillis(60));
        assertEquals(1801000,ArchiveSchedule.nextAutomatic(1000,true,15));assertEquals(901000,ArchiveSchedule.nextAutomatic(1000,false,15));
    }
    @Test public void anOutstandingCooldownDoesNotAddAnotherFullHour(){
        assertEquals(20000,ArchiveSchedule.nextCheckDelay(1000000,3600000,new long[]{1020000,1030000}));
        assertEquals(3600000,ArchiveSchedule.nextCheckDelay(1000000,3600000,new long[]{0,999999}));
    }
    @Test(expected=IllegalArgumentException.class) public void unsupportedIntervalsCannotEnterTheSchedule(){ArchiveSchedule.intervalMillis(120);}
}
