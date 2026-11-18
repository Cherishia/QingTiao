package dev.local.qingtiao;
/** Suspends pending review briefly when a window first reports a different owner, without permitting any old-target action. */
public final class WindowTransitionWatch {
    private long since=-1;
    public boolean defer(boolean pending,boolean confirmed,String expected,String observed,long now) {
        if(!pending||expected==null||expected.isEmpty()||expected.equals(observed)){reset();return false;}
        if(since<0)since=now;
        return now>=since&&now-since<(confirmed?350:1500);
    }
    public void reset(){since=-1;}
}
