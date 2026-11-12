package de.wattpit.app.core;

/** Boot restoration does not turn a paused recording on or promote a viewer to collector. */
public final class StartupPolicy {
    private StartupPolicy(){}
    public static boolean restoreNetwork(boolean enabled,String role,boolean unlocked){return enabled&&unlocked&&("central".equals(role)||"direct".equals(role));}
    public static boolean restoreCollector(boolean enabled,boolean recording,String role,boolean unlocked){return recording&&restoreNetwork(enabled,role,unlocked);}
}
