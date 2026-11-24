package dev.local.qingtiao;

/** Weak panel discovery is charged only when an existing scan requests a frame. */
final class RenderedPanelProbes {
    private static final long SPACING_MS=3500;
    static final long RECHECK_MS=45000;
    private static final int CONTROL_CHANGE=16;
    enum State { READY, NO_CONTEXT, ATTEMPTED, COOLDOWN, STABLE, EXHAUSTED }

    private int window=-1, lastCount=-1, remaining=2;
    private long nextAllowed,nextRecheck;
    private boolean attempted;

    void window(int id){
        if(id==window)return;
        window=id;
        lastCount=-1;
        attempted=false;
    }

    boolean ready(int visible,long now){
        return state(visible,now,false)==State.READY;
    }

    void requested(int visible,long now){
        requested(visible,now,false);
    }

    State state(int visible,long now,boolean allowRecheck){
        if(visible<=0)return State.NO_CONTEXT;
        if(attempted)return State.ATTEMPTED;
        if(now<nextAllowed)return State.COOLDOWN;
        // Additional Bili probes are allowed on an existing scan, never scheduled by this cooldown.
        if(allowRecheck&&now>=nextRecheck)return State.READY;
        if(remaining==0)return allowRecheck?State.COOLDOWN:State.EXHAUSTED;
        return lastCount<0||Math.abs(lastCount-visible)>=CONTROL_CHANGE?State.READY:State.STABLE;
    }

    void requested(int visible,long now,boolean allowRecheck){
        if(state(visible,now,allowRecheck)!=State.READY)return;
        lastCount=visible;
        if(remaining>0)remaining--;
        nextAllowed=now+SPACING_MS;
        nextRecheck=now+RECHECK_MS;
    }

    boolean attempted(){return attempted;}
    void markAttempt(){attempted=true;}

    void reset(){
        window=lastCount=-1;
        remaining=2;
        nextAllowed=nextRecheck=0;
        attempted=false;
    }
}
