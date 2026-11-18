package dev.local.qingtiao;

public final class WmTouchReviewTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final String PKG="com.weilaishanhai.oopz";
    private static final WmSplashVisual.Hint HINT=new WmSplashVisual.Hint(981,389,1170,458,1200,2670);
    private static WmTouchReview armed() {WmTouchReview r=new WmTouchReview();r.mark(PKG,752,0,HINT,1103,425,1000,1100);return r;}
    private static WmTouchReview.Decision observe(WmTouchReview r,long frame,long now) {return r.observe(PKG,752,0,HINT,1103,425,frame,now);}
    public static void main(String[] args) {
        WmTouchReview r=new WmTouchReview();
        check(observe(r,1000,1100)==WmTouchReview.Decision.FIRST,"first verified target needs the initial pair proof");
        check(r.observe(PKG,752,0,null,1103,425,1000,1100)==WmTouchReview.Decision.STOP,"absent SDK never authorizes a touch");
        r=armed();check(observe(r,1700,1800)==WmTouchReview.Decision.WAIT,"completion required");r.completed();
        check(observe(r,1750,1850)==WmTouchReview.Decision.WAIT,"first new frame only observes");
        check(observe(r,2300,2400)==WmTouchReview.Decision.WAIT,"two frames cannot bypass 1800ms interval");
        check(observe(r,2850,2950)==WmTouchReview.Decision.RETRY,"two frames and interval allow one retry");
        r.mark(PKG,752,0,HINT,1103,425,2850,2950);r.completed();
        check(observe(r,4800,4900)==WmTouchReview.Decision.STOP&&r.attempts()==2,"no third touch");
        r.pause(5000);check(!r.active()&&!r.missing(PKG,0,1200,2670,5100),"missing controls cannot restore spent budget");
        r.mark(PKG,753,0,HINT,1103,425,5200,5300);check(r.attempts()==2,"new window cannot reset cap");
        r=armed();r.completed();check(observe(r,2900,3000)==WmTouchReview.Decision.WAIT,"time without two frames is insufficient");
        check(observe(r,2900,3100)==WmTouchReview.Decision.WAIT,"same screenshot does not count twice");
        check(observe(r,3100,3200)==WmTouchReview.Decision.WAIT,"less than 300ms does not count twice");
        check(observe(r,3200,3300)==WmTouchReview.Decision.RETRY,"independent second frame qualifies");
        r=armed();r.completed();observe(r,2900,3000);
        check(observe(r,4500,4600)==WmTouchReview.Decision.WAIT,"gap over 1200ms discards previous positive frame");
        check(observe(r,4950,5050)==WmTouchReview.Decision.RETRY,"new consecutive pair qualifies");
        r=armed();r.completed();check(r.missing(PKG,0,1200,2670,1500),"temporary missing node suspends proof");
        check(r.awaitingTarget()&&r.active()&&r.attempts()==1,"one budget remains but no target proof");
        check(observe(r,1400,1600)==WmTouchReview.Decision.WAIT,"pre-gap frame cannot be reused");
        check(observe(r,2900,3000)==WmTouchReview.Decision.WAIT,"returning first frame cannot tap");
        check(observe(r,3350,3450)==WmTouchReview.Decision.RETRY,"two new frames can resume");

        // Supplied 01:04:12.652 -> 12.967 missing node -> 01:04:29 replacement window.
        r=new WmTouchReview();r.mark(PKG,3138,0,HINT,1103,425,12534,12652);r.completed();
        check(r.missing(PKG,0,1200,2670,12967),"reported missing scan does not latch STOP");
        check(r.active()&&r.remaining(20502)>0,"later app-entry latch still permits bounded observation");
        WmSplashVisual.Hint narrow=new WmSplashVisual.Hint(988,389,1170,458,1200,2670);
        check(r.observe(PKG,3140,0,narrow,1103,425,28000,28100)==WmTouchReview.Decision.WAIT,"replacement window needs fresh pair");
        check(r.observe(PKG,3140,0,narrow,1103,425,28450,28550)==WmTouchReview.Decision.RETRY,"reported replacement window can use remaining touch");
        r.mark(PKG,3140,0,narrow,1103,425,28450,28550);r.completed();
        check(r.observe(PKG,3142,0,narrow,1103,425,29000,29100)==WmTouchReview.Decision.STOP,"third ad window cannot create third touch");
        r=armed();r.completed();
        for(int i=0;i<3;i++)check(r.observe(PKG,753+i,0,HINT,1103,425,2900+i*450,3000+i*450)==WmTouchReview.Decision.WAIT,"different windows cannot combine frames");
        check(r.observe(PKG,755,0,HINT,1103,425,4250,4350)==WmTouchReview.Decision.RETRY,"two frames in current window qualify");
        r=armed();r.completed();
        check(r.observe(PKG,752,0,HINT,1130,425,2900,3000)==WmTouchReview.Decision.WAIT,"moving word is reacquired instead of reusing old coordinates");
        check(r.observe(PKG,752,0,HINT,1130,425,3350,3450)==WmTouchReview.Decision.RETRY,"stable relocated OOPZ word qualifies");
        r=armed();r.completed();observe(r,2900,3000);r.pause(3100);
        check(observe(r,3350,3450)==WmTouchReview.Decision.WAIT,"missing-word frame breaks positive pair");
        for(int mode=0;mode<7;mode++) {
            r=armed();r.completed();String owner=mode==0?"other.app":PKG;int screen=mode==1?1:0;
            WmSplashVisual.Hint hint=mode==2?new WmSplashVisual.Hint(981,389,1170,458,1201,2670):mode==3?new WmSplashVisual.Hint(981,389,1170,458,1200,2671):HINT;
            long now=mode==4?21101:mode==5?1000:3000,frame=mode==6?2300:now-100;
            check(r.observe(owner,752,screen,hint,1103,425,frame,now)==WmTouchReview.Decision.STOP,"changed owner/display/size/deadline/clock/stale frame stops: "+mode);
            check(observe(r,3100,3200)==WmTouchReview.Decision.STOP,"hard cancellation cannot resume: "+mode);
        }
        for(int mode=0;mode<4;mode++) {
            r=armed();r.completed();
            check(!r.missing(mode==0?"other.app":PKG,mode==1?1:0,mode==2?1440:1200,2670,mode==3?21101:1500),"missing tree cannot hide changed context or deadline");
            check(!r.active(),"unsafe missing context is terminal");
        }
        r=armed();r.completed();r.pause(19000);check(r.expire(21101)&&!r.active(),"pause does not restart 20-second clock");
        r=armed();r.completed();check(observe(r,3100,3000)==WmTouchReview.Decision.STOP,"future frame rejected");
        r=armed();r.stop();r.completed();check(observe(r,2900,3000)==WmTouchReview.Decision.STOP,"late completion cannot undo stop");
        r.reset();check(!r.active()&&!r.belongsTo(PKG)&&observe(r,3100,3200)==WmTouchReview.Decision.FIRST,"foreground reset clears entry budget");
        for(int mode=0;mode<3;mode++) {
            r=new WmTouchReview();r.mark("other.sdk.app",752,0,HINT,1103,425,1000,1100);r.completed();
            int window=mode==0?753:752,x=mode==1?1130:1103;long frame=mode==2?5900:2900,now=frame+100;
            check(r.observe("other.sdk.app",window,0,HINT,x,425,frame,now)==WmTouchReview.Decision.WAIT,"shared SDK recovery needs new pair after target/window/gap change");
            check(r.observe("other.sdk.app",window,0,HINT,x,425,frame+450,now+450)==WmTouchReview.Decision.RETRY,"shared SDK recovery is layout based rather than owner allow-list");
        }
        r=new WmTouchReview();r.mark("other.sdk.app",752,0,HINT,1103,425,1000,1100);r.completed();
        check(r.observe("other.sdk.app",752,0,HINT,1103,425,21100,21200)==WmTouchReview.Decision.STOP,"all SDK owners keep fixed twenty-second limit");
        // 0.5.13 01:56:32.980 first touch: the 33.372 record is WAIT, not a failed recognition.
        r=new WmTouchReview();r.mark(PKG,3578,0,HINT,1104,424,32875,32980);r.completed();
        check(r.observe(PKG,3578,0,HINT,1104,424,33300,33372)==WmTouchReview.Decision.WAIT,"reported first post-touch frame still waits");
        check(r.observe(PKG,3578,0,HINT,1104,424,33760,33830)==WmTouchReview.Decision.WAIT,"two early frames do not bypass retry interval");
        check(r.observe(PKG,3578,0,HINT,1104,424,34300,34370)==WmTouchReview.Decision.WAIT,"continued observation before 1800ms");
        check(r.observe(PKG,3578,0,HINT,1104,424,34750,34820)==WmTouchReview.Decision.RETRY,"same current target plus fresh frames can retry after 1840ms");
        System.out.println("PASS: "+checks+" WM touch/recovery checks");
    }
}
