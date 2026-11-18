package dev.local.qingtiao;

/** A B站 pause sheet gets a short visual discovery allowance, not a polling loop. */
public final class PanelProbeBudget {
    private static final long PERIOD=2500, RETRY=500, REARM=2500;
    private int window=-1,left;
    private long until=-1,lastStart=-10000,lastRequest=-10000,missingSince=-1;
    private boolean gone=true,attempted,deferredEvidence;

    public boolean observe(int windowId,boolean evidence,boolean scoped,long now){
        boolean disappeared=false;
        if(windowId<0)return false;
        if(windowId!=window){disappeared=!gone||attempted;window=windowId;left=0;gone=true;attempted=deferredEvidence=false;missingSince=-1;}
        if(!evidence){
            left=0;deferredEvidence=false;
            if(attempted){
                if(missingSince<0||now<missingSince)missingSince=now;
                if(now-missingSince<180)return disappeared;
            }
            disappeared|=!gone||attempted;gone=true;attempted=false;missingSince=-1;return disappeared;
        }
        missingSince=-1;
        // Height, creative and CTA can change while animating. They do not refill discovery.
        if(gone&&now-lastStart>=REARM){
            gone=false;left=attempted?0:scoped?3:2;lastStart=now;until=now+PERIOD;lastRequest=-10000;
        }
        deferredEvidence=gone&&!attempted;
        return disappeared;
    }
    /** One fresh tree read can close an observed absence or re-evaluate positive evidence at cooldown end. */
    long treeDelay(int windowId,long now){
        if(windowId!=window)return -1;
        if(attempted&&missingSince>=0)return Math.max(0,missingSince+180-now);
        return deferredEvidence?Math.max(0,lastStart+REARM-now):-1;
    }
    public long delay(int windowId,long now){
        if(windowId!=window||gone||left<=0||now<lastStart||now>=until)return -1;
        long delay=Math.max(0,lastRequest+RETRY-now);
        return now+delay<until?delay:-1;
    }
    public void requested(int windowId,long now){if(delay(windowId,now)==0){lastRequest=now;left--;}}
    public void attempted(int windowId){if(windowId==window){left=0;attempted=true;deferredEvidence=false;}}
    public boolean attemptedIn(int windowId){return windowId==window&&attempted;}
    public void reset(){window=-1;left=0;gone=true;attempted=deferredEvidence=false;until=missingSince=-1;lastStart=lastRequest=-10000;}
}
