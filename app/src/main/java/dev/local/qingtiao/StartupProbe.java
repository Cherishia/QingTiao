package dev.local.qingtiao;

/** Spreads otherwise unclassified image probes across loading, instead of spending them at once. */
public final class StartupProbe {
    public static final long WINDOW=30000;
    private static final long[] AGES={0,700,2000,5000,11000,22000};
    private long entered,last=-10000;
    private int count,window=-1,extra;
    private boolean urgent,surfaceSeen,pullRegular,retryUsed;
    private int retryWindow=-1;
    public void reset(long now){entered=now;last=-10000;count=extra=0;window=retryWindow=-1;urgent=surfaceSeen=pullRegular=retryUsed=false;}
    public void window(int id,long now) {
        if(id<0||!active(now))return;
        if(window!=id){retryWindow=-1;if(window>=0&&extra<2)urgent=true;}
        window=id;
    }
    public boolean active(long now){return now>=entered&&now-entered<WINDOW;}
    /** Fresh explicit ad evidence may share, never enlarge, the two extra discovery slots. */
    public void evidence(long now){if(active(now)&&extra<2)urgent=true;}
    /** A first strong surface may pull forward one regular slot when weak cues used the extras. */
    public void surface(long now){
        if(!active(now)||surfaceSeen)return;
        surfaceSeen=true;
        if(extra<2)urgent=true;
        else if(count<regularLimit())pullRegular=true;
    }
    private int regularLimit(){return AGES.length-(retryUsed?1:0);}
    private long ordinaryDelay(long now) {
        if(!active(now))return -1;
        if(urgent&&extra<2||pullRegular&&count<regularLimit())return Math.max(0,450-(now-last));
        if(count>=regularLimit())return -1;
        return Math.max(0,Math.max(entered+AGES[count]-now,450-(now-last)));
    }
    public long delay(long now){return active(now)&&surfaceRetryPending()?Math.max(0,350-(now-last)):ordinaryDelay(now);}
    /** Move the last unused regular slot to the first strong same-window miss. Leave the next regular slot queued. */
    boolean retrySurface(int id,long now){
        if(!active(now)||id<0||id!=window||retryUsed||retryWindow>=0||count>=AGES.length-1||now<last||now-last>600)return false;
        long early=Math.max(0,350-(now-last)),ordinary=ordinaryDelay(now);
        if(ordinary<early+100||now+early-entered>=WINDOW)return false;
        retryWindow=id;return true;
    }
    boolean surfaceRetryPending(){return retryWindow>=0;}
    void cancelSurfaceRetry(){retryWindow=-1;}
    public void requested(long now){
        if(surfaceRetryPending()){retryWindow=-1;retryUsed=true;last=now;return;}
        if(urgent&&extra<2&&(count>=regularLimit()||now<entered+AGES[count]))extra++;
        else count++;
        urgent=pullRegular=false;last=now;
    }
    /** Absolute uptime for the existing slot after the shared capture-rate limiter. */
    long scanAt(long now,long captureWait){
        long planned=delay(now);if(planned<0||captureWait<0)return -1;
        long wait=Math.max(planned,captureWait);
        return wait<WINDOW-(now-entered)?now+wait:-1;
    }
    public int count(){return count+extra+(retryUsed?1:0);}
}
