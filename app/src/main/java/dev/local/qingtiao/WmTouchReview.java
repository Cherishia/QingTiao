package dev.local.qingtiao;

/** One bounded second touch. A missing tree suspends proof; no stored target authorizes a touch. */
public final class WmTouchReview {
    public enum Decision { FIRST, WAIT, RETRY, STOP }
    private String pkg="";
    private int window,originalWindow,display,wordX,wordY,originalX,originalY,attempts,confirmations;
    private long firstAt,lastFrame=-1,proofAfter;
    private WmSplashVisual.Hint hint,originalHint;
    private boolean completed,stopped;
    public boolean belongsTo(String value){return attempts>0&&pkg.equals(value);}
    public boolean active(){return attempts==1&&!stopped;}
    public int attempts(){return attempts;}
    public long delay(long now){return Math.max(0,1800-(now-firstAt));}
    private long span(){return 20000;}
    public long remaining(long now){return active()?Math.max(0,span()-(now-firstAt)):0;}
    public boolean expire(long now) {
        if(active()&&(now<firstAt||now-firstAt>span())){stop();return true;}return false;
    }
    public boolean awaitingTarget(){return active()&&hint==null;}
    /** Drops all positive frames without resetting the time limit or touch count. */
    public void pause(long now) {
        if(!active()||expire(now))return;
        hint=null;confirmations=0;lastFrame=-1;proofAfter=now;
    }
    public boolean missing(String value,int screen,int width,int height,long now) {
        if(!active())return false;
        if(!pkg.equals(value)||screen!=display||originalHint.width!=width||originalHint.height!=height||expire(now)){stop();return false;}
        pause(now);return true;
    }
    private boolean sameWord(WmSplashVisual.Hint control,int x,int y) {
        return WmSplashVisual.same(hint,control)&&Math.abs(x-wordX)<=Math.max(3,originalHint.width/100)
            &&Math.abs(y-wordY)<=Math.max(3,originalHint.height/200);
    }
    public Decision observe(String value,int win,int screen,WmSplashVisual.Hint current,int x,int y,long frame,long now) {
        if(current==null||value==null||value.isEmpty()||win<0||screen<0||now<frame||now-frame>600
            ||x<=current.left||x>=current.right||y<=current.top||y>=current.bottom) {if(attempts>0)stop();return Decision.STOP;}
        if(stopped)return Decision.STOP;
        if(attempts==0)return Decision.FIRST;
        if(!active()||expire(now))return Decision.STOP;
        if(!pkg.equals(value)||screen!=display||current.width!=originalHint.width||current.height!=originalHint.height) {stop();return Decision.STOP;}
        // A replacement window always starts a new pair, within the same owner and fixed touch budget.
        if(!completed||frame<=proofAfter||frame<=lastFrame)return Decision.WAIT;
        if(hint==null||win!=window||!sameWord(current,x,y)) {
            hint=current;window=win;wordX=x;wordY=y;confirmations=1;lastFrame=frame;return Decision.WAIT;
        }
        if(frame-lastFrame<300)return Decision.WAIT;
        confirmations=lastFrame<0||frame-lastFrame>1200?1:confirmations+1;lastFrame=frame;
        return confirmations>=2&&delay(now)==0?Decision.RETRY:Decision.WAIT;
    }
    public void mark(String value,int win,int screen,WmSplashVisual.Hint current,int x,int y,long frame,long now) {
        if(attempts>=2||stopped||current==null||value==null||value.isEmpty()||now<frame||now-frame>600)return;
        if(attempts==0) {
            pkg=value;originalWindow=window=win;display=screen;originalHint=hint=current;
            originalX=wordX=x;originalY=wordY=y;firstAt=proofAfter=now;lastFrame=frame;
        }
        attempts++;completed=false;
    }
    public void completed(){completed=true;}
    public void stop(){stopped=true;}
    public void reset(){pkg="";originalHint=hint=null;attempts=confirmations=0;completed=stopped=false;firstAt=proofAfter=0;lastFrame=-1;}
    public WmTouchReview(){}
}
