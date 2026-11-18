package dev.local.qingtiao;

/** Never retry solely because ACTION_CLICK returned true; re-read the current eligible target. */
public final class ClickReview {
    public enum Action { WAIT, TARGET_GONE, CANCEL, TRY_GESTURE, TRY_NATIVE, STOP }
    private long since;
    private boolean gestureUsed,nativeUsed=true;
    public ClickReview(long now,boolean gestureUsed){since=now;this.gestureUsed=gestureUsed;}
    public void markGesture(long now){gestureUsed=true;since=now;}
    public void allowNativeFallback(){nativeUsed=false;}
    public void markNative(long now){nativeUsed=true;since=now;}
    public boolean inSettlingTime(long now){return now-since<400;}
    public Action decide(long now,boolean sameContext,boolean visible,boolean eligible,boolean gestureAllowed) {
        if(!sameContext)return Action.CANCEL;
        if(!visible)return Action.TARGET_GONE;
        if(inSettlingTime(now))return Action.WAIT;
        // A temporarily missing ad label is not evidence that the button is safe to tap.
        // Re-read briefly instead of permanently cancelling on the first unstable frame.
        if(!eligible)return now-since<1800?Action.WAIT:Action.CANCEL;
        if(!gestureUsed&&gestureAllowed){markGesture(now);return Action.TRY_GESTURE;}
        if(!nativeUsed){markNative(now);return Action.TRY_NATIVE;}
        return Action.STOP;
    }
}
