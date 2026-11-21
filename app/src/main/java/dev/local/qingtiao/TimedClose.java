package dev.local.qingtiao;

/** Brief missed frames preserve elapsed time; a fresh positive observation is still required to act. */
public final class TimedClose {
    static final long WAIT_MS=3100,CONTINUITY_MS=2500,LATE_X_WINDOW_MS=1650;
    private static final long CHECKPOINT_MS=1800;
    private int window=-1,x,y;
    private long firstSeen=-1,lastSeen=-1;
    private int hits,misses;
    private String resetReason="尚未定位";
    private int captionWindow=-1;
    private long captionStarted=-1,captionLast=-1;
    private String origin="首次识别红X";
    public boolean active(){return firstSeen>=0;}
    /** A caption starts a waiting clock, but never grants an action or a target location. */
    public void caption(int currentWindow,boolean present,long now) {
        if(active()&&currentWindow!=window)clear("窗口变化");
        if(captionStarted>=0&&(currentWindow!=captionWindow||now<captionLast||now-captionLast>CONTINUITY_MS))clearCaption();
        if(present) {
            if(captionStarted<0){captionStarted=now;captionWindow=currentWindow;}
            captionLast=now;
        }
    }
    private void clearCaption(){captionWindow=-1;captionStarted=-1;captionLast=-1;}
    /** The caption alone schedules a text check or a deadline read, never a click. */
    long captionProbeAt(long now){
        if(active()||captionStarted<0||now<captionLast||now-captionLast>CONTINUITY_MS)return -1;
        return Math.min(captionStarted+WAIT_MS,captionLast+CHECKPOINT_MS);
    }
    boolean captionDue(int currentWindow,long now){
        return captionProbeAt(now)>=0&&currentWindow==captionWindow&&now>=captionStarted+WAIT_MS;
    }
    public long remaining(int currentWindow,int cx,int cy,int tolerance,long now) {
        if(firstSeen<0||currentWindow!=window||Math.abs(cx-x)>tolerance||Math.abs(cy-y)>tolerance||now<lastSeen||now-lastSeen>CONTINUITY_MS) {
            boolean borrowCaption=firstSeen<0&&captionStarted>=0&&captionWindow==currentWindow&&now>=captionLast&&now-captionLast<=CONTINUITY_MS;
            resetReason=firstSeen<0?resetReason:currentWindow!=window?"窗口变化":now<lastSeen?"时钟变化":now-lastSeen>CONTINUITY_MS?"超过2500ms未确认目标":"目标位置变化";
            window=currentWindow;x=cx;y=cy;firstSeen=borrowCaption?captionStarted:now;hits=0;misses=0;
            origin=borrowCaption?"首次读到广告关闭提示":"首次识别红X（未取得更早提示）";
            if(!borrowCaption)clearCaption();
        }
        lastSeen=now;hits++;
        return Math.max(0,WAIT_MS-(now-firstSeen));
    }
    /** Time only: a midpoint keeps the evidence continuous; the last sample is due at the deadline. */
    long nextProbeAt(long now){
        if(!active()||now<lastSeen)return now;
        long due=firstSeen+WAIT_MS;
        return Math.max(now,due-lastSeen>2200?lastSeen+CHECKPOINT_MS:due);
    }
    boolean readyFrame(long frameTime){return active()&&frameTime>=firstSeen+WAIT_MS;}
    public boolean missing(int currentWindow,long now) {
        if(!active())return false;
        misses++;
        if(currentWindow!=window||now<lastSeen||now-lastSeen>CONTINUITY_MS) {
            clear(currentWindow!=window?"漏帧时窗口变化":now<lastSeen?"时钟变化":"连续超过2500ms未确认原目标");return false;
        }
        return true;
    }
    public String describe(long now) {
        return "计时窗口="+window+"，计时起点="+origin+"，已等待="+(active()?Math.max(0,now-firstSeen):0)+"ms，确认帧="+hits+"，漏帧="+misses+"，最近计时重置原因="+resetReason;
    }
    public void clear(){clear("前台切换或识别取消");}
    public void clear(String reason){window=-1;firstSeen=-1;lastSeen=-1;resetReason=reason;clearCaption();}
}
