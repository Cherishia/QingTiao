package dev.local.qingtiao;

import java.util.ArrayDeque;

/** A fresh complete panel pattern allows one immediate touch, with bounded capture rate. */
public final class VisualReview {
    static final long MIN_PAIR_GAP=300,MAX_PAIR_GAP=1200;
    public enum Action { NO_TARGET, READY, ALREADY_ATTEMPTED }
    private long lastRequest=-10000;
    private boolean attempted;
    private final ArrayDeque<Long> requests=new ArrayDeque<>();
    public static boolean currentFrame(long age,int frameWidth,int frameHeight,int width,int height) {
        return age>=0&&age<=600&&width>0&&height>0&&frameWidth==width&&frameHeight==height;
    }
    public long requestDelay(long now){return requestDelay(now,false);}
    public long requestDelay(long now,boolean confirmingPair) {
        while(!requests.isEmpty()&&now-requests.peekFirst()>=60000)requests.removeFirst();
        long wait=Math.max(0,(confirmingPair?350:450)-(now-lastRequest));
        if(requests.size()>=60)wait=Math.max(wait,60000-(now-requests.peekFirst()));
        return wait;
    }
    public void requested(long now){lastRequest=now;requests.addLast(now);}
    long pairedRequestAt(long now,long firstFrame){
        return Math.max(firstFrame+MIN_PAIR_GAP,now+requestDelay(now,true));
    }
    long pairedRefreshAt(long now,long lastFrame){
        if(lastFrame<0||now<lastFrame)return -1;
        long due=pairedRequestAt(now,lastFrame);
        // Leave time for the live-window check and screenshot acquisition.
        return due-lastFrame<=MAX_PAIR_GAP-120?due:-1;
    }
    public Action observe(boolean found) {
        if(!found){clearScene();return Action.NO_TARGET;}
        if(attempted)return Action.ALREADY_ATTEMPTED;
        return Action.READY;
    }
    public void markAttempt(){attempted=true;}
    public void clearScene(){attempted=false;}
}
