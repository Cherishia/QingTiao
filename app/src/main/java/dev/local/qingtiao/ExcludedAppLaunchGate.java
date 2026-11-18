package dev.local.qingtiao;

/** An app launch requires a stable, observed period with accessibility stopped. */
final class ExcludedAppLaunchGate {
    enum Next { WAIT, OPEN, SETTINGS }
    private static final long STABLE_MS=480, TIMEOUT_MS=4500;
    private final long deadline;
    private long stoppedSince=-1;

    ExcludedAppLaunchGate(long now){deadline=now+TIMEOUT_MS;}

    Next observe(boolean stopped,long now){
        if(now>=deadline)return Next.SETTINGS;
        if(!stopped)stoppedSince=-1;
        else if(stoppedSince<0||now<stoppedSince)stoppedSince=now;
        else if(now-stoppedSince>=STABLE_MS)return Next.OPEN;
        return Next.WAIT;
    }
}
