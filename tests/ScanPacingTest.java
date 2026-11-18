package dev.local.qingtiao;

public final class ScanPacingTest {
    private static int checks;
    private static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    public static void main(String[] args) {
        check(ScanPacing.watchDelay(false,true,true)<0,"off switch schedules no polling");
        check(ScanPacing.watchDelay(true,false,true)<0,"screen off or keyguard schedules no polling");
        check(ScanPacing.watchDelay(true,false,false)<0,"idle screen off has zero poll wakeups");
        check(ScanPacing.watchDelay(true,true,false)==30000,"ordinary foreground fallback reads only twice a minute");
        check(ScanPacing.watchDelay(true,true,true)==1000,"bounded launch/known ad observation stays responsive");
        check(ScanPacing.eventDelay(true,false,false)==30,"new window is urgent even after idle");
        check(ScanPacing.eventDelay(false,true,true)==30,"confirmed ad bypasses scroll throttling");
        check(ScanPacing.eventDelay(false,false,true)==2500,"ordinary scroll flood is merged");
        check(ScanPacing.eventDelay(false,false,false)==1500,"ordinary content is merged");
        check(ScanPacing.minimum(true)==120&&ScanPacing.minimum(false)==1500,"queued work follows same mode floor");
        check(ScanPacing.opening(1000,1000)&&ScanPacing.opening(1000,6999),"short startup stays fast");
        check(!ScanPacing.opening(1000,7000)&&!ScanPacing.opening(1000,999),"events cannot prolong opening period");
        check(!ScanPacing.jmNeedsPixels(false,false,false),"manual route page is not a reason for infinite screenshots");
        for(int i=0;i<3;i++)check(ScanPacing.jmNeedsPixels(i==0,i==1,i==2),"caption, ad or review re-enables capture");
        long last=-10000,due=-1;int scans=0;
        for(long now=0;now<60000;now+=10) {
            long target=now+Math.max(ScanPacing.eventDelay(false,false,false),ScanPacing.minimum(false)-(now-last));
            if(due<0||target<due)due=target;
            if(due<=now){scans++;last=now;due=-1;}
        }
        check(scans<=40&&scans>=35,"6000 ordinary content events cannot drive more than forty scans per minute");
        check(60000/ScanPacing.watchDelay(true,true,false)==2,"idle fallback bounded to two root reads per minute");
        check(ScanPacing.eventDelay(true,false,false)<ScanPacing.eventDelay(false,false,false),"ad window preempts existing idle wait");
        check(ScanPacing.transition("new.app","old.app"),"new foreground content can wake scanning without a window-state event");
        check(!ScanPacing.transition("same.app","same.app")&&!ScanPacing.transition(null,"old.app")&&!ScanPacing.transition("","old.app"),"same/unknown package cannot get transition priority");
        check(ScanPacing.eventDelay(false,ScanPacing.transition("new.app","old.app"),false)==30,"missed launch event does not leave a 900ms content delay");
        check(ScanPacing.minimum(true,true)==120&&ScanPacing.eventDelay(false,true,true,true)==30,"confirmed target retains urgent timing during launch");
        check(ScanPacing.minimum(false,true)==450&&ScanPacing.eventDelay(false,false,false,true)==300,"early content without evidence is merged without becoming idle");
        check(ScanPacing.eventDelay(true,false,true,false)==30,"new real window still preempts ordinary scroll wait");
        check(ScanPacing.followup(true)==220&&ScanPacing.followup(false)==700,"only discovery followups are spaced out");
        check(ScanPacing.startupWatch(600,29000)==600,"timer wakes at next planned probe instead of polling each second");
        check(ScanPacing.startupWatch(7000,16000)==7000,"late sparse opportunity is not spent on intermediate root reads");
        check(ScanPacing.startupWatch(-1,8000)==8000,"completed probe budget sleeps until startup deadline");
        check(ScanPacing.startupWatch(0,8000)==120&&ScanPacing.startupWatch(-1,0)==120,"no timer spin at immediate or ended plan");
        check(ScanPacing.RECORD_INTERVAL==10000,"ordinary history serialization is bounded to six batches a minute");
        System.out.println("PASS: "+checks+" power pacing checks; simulated ordinary content scans/min="+scans);
    }
}
