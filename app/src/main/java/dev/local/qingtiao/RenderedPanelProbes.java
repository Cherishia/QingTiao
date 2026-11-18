package dev.local.qingtiao;

/** Two event-driven discovery opportunities per foreground app visit. */
final class RenderedPanelProbes {
    private static final long SPACING_MS=3500;
    private static final int CONTROL_CHANGE=16;
    private int window=-1, lastCount=-1, remaining=2;
    private long nextAllowed;
    private boolean attempted;

    void window(int id){
        if(id==window)return;
        window=id;
        lastCount=-1;
        attempted=false;
    }

    boolean ready(int visible,long now){
        return visible>0&&!attempted&&remaining>0&&now>=nextAllowed
            &&(lastCount<0||Math.abs(lastCount-visible)>=CONTROL_CHANGE);
    }

    void requested(int visible,long now){
        if(!ready(visible,now))return;
        lastCount=visible;
        remaining--;
        nextAllowed=now+SPACING_MS;
    }

    boolean attempted(){return attempted;}
    void markAttempt(){attempted=true;}

    void reset(){
        window=lastCount=-1;
        remaining=2;
        nextAllowed=0;
        attempted=false;
    }
}
