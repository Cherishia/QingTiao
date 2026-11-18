package dev.local.qingtiao;

/** Timing only: this class never authorizes a target or extends an ad observation window. */
public final class ScanPacing {
    public static final long IDLE_SCAN=1500, IDLE_WATCH=30000, RECORD_INTERVAL=10000;
    public static boolean opening(long entered,long now){return now>=entered&&now-entered<6000;}
    public static boolean transition(String eventPkg,String foreground){return eventPkg!=null&&!eventPkg.isEmpty()&&!eventPkg.equals(foreground);}
    public static long minimum(boolean urgent){return urgent?120:IDLE_SCAN;}
    public static long minimum(boolean urgent,boolean opening){return urgent?120:opening?450:IDLE_SCAN;}
    public static long eventDelay(boolean window,boolean urgent,boolean scroll){return window||urgent?30:scroll?2500:IDLE_SCAN;}
    public static long eventDelay(boolean window,boolean urgent,boolean scroll,boolean opening){return window||urgent?30:opening?300:eventDelay(false,false,scroll);}
    public static long followup(boolean confirmed){return confirmed?220:700;}
    public static long startupWatch(long planned,long remaining){return Math.max(120,planned>=0?planned:remaining);}
    public static long watchDelay(boolean running,boolean interactive,boolean active){return !running||!interactive?-1:active?1000:IDLE_WATCH;}
    public static boolean jmNeedsPixels(boolean review,boolean caption,boolean ad){return review||caption||ad;}
    private ScanPacing(){}
}
