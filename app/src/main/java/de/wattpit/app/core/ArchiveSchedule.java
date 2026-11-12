package de.wattpit.app.core;

/** The central records live data continuously; archive connections need a quiet interval. */
public final class ArchiveSchedule {
    private ArchiveSchedule() { }
    public static final int DEFAULT_MINUTES=60;
    public static final long RETRY_MS=3600000,CANCEL_PAUSE_MS=1800000;
    public static long intervalMillis(int minutes){
        if(minutes==0||minutes==15||minutes==30||minutes==60)return minutes*60000L;
        throw new IllegalArgumentException("Bitte ein gültiges Archivintervall wählen.");
    }
    public static boolean automaticDue(long nextAllowed,long now){return nextAllowed<=now||nextAllowed-now>RETRY_MS;}
    public static boolean automaticDue(long nextAllowed,long now,boolean missing){return missing&&automaticDue(nextAllowed,now);}
    public static long nextAutomatic(long now,boolean cancelled){return nextAutomatic(now,cancelled,DEFAULT_MINUTES);}
    public static long nextAutomatic(long now,boolean cancelled,int minutes){
        long interval=intervalMillis(minutes);if(interval==0)interval=RETRY_MS;
        return now+Math.max(interval,cancelled?CANCEL_PAUSE_MS:0);
    }
    /** Wake at an outstanding deadline instead of skipping a second full interval. */
    public static long nextCheckDelay(long now,long interval,long[] deadlines){
        long delay=interval;
        for(long deadline:deadlines)if(deadline>now&&deadline-now<=RETRY_MS)delay=Math.min(delay,deadline-now);
        return Math.max(1000,delay);
    }
}
