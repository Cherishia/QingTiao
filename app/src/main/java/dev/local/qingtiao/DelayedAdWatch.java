package dev.local.qingtiao;

/** The verified JM container can show its ad after a user-controlled route choice. */
public final class DelayedAdWatch {
    private long last=-10000;
    public static boolean jm(String pkg){return "com.jiaohua_browser".equals(pkg);}
    public static boolean active(String pkg,boolean closed){return jm(pkg)&&!closed;}
    public long delay(long now){return Math.max(0,2000-(now-last));}
    public void requested(long now){last=now;}
    public void reset(){last=-10000;}
}
