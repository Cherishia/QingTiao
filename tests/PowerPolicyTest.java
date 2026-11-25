package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.Arrays;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Desktop scheduler replay and optional image regression; not a phone battery measurement. */
public final class PowerPolicyTest {
    private static int checks;
    private static volatile int sink;
    private static void check(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception {
        StartupProbe probe=new StartupProbe();probe.reset(0);probe.window(1,0);
        ArrayList<Long> captures=new ArrayList<>();long now=0;int wakes=0;
        while(now<StartupProbe.WINDOW&&wakes<100) {
            wakes++;if(probe.delay(now)==0){captures.add(now);probe.requested(now);}
            now+=ScanPacing.startupWatch(probe.delay(now),StartupProbe.WINDOW-now);
        }
        check(captures.equals(Arrays.asList(0L,700L,2000L,5000L,11000L,22000L)),"quiet launch keeps all original early/late pixel opportunities");
        check(wakes==6&&now==30000,"no intermediate one-second polling while waiting on sparse plan");
        check(probe.count()==6&&!probe.active(now),"deadline and six-probe budget unchanged");
        probe.reset(0);probe.window(1,0);for(long t:captures)probe.requested(t);
        probe.window(2,25000);check(probe.delay(25000)==0,"late new window keeps first extra chance");probe.requested(25000);
        probe.window(3,26000);check(probe.delay(26000)==0,"second extra chance remains");probe.requested(26000);
        probe.window(4,27000);check(probe.count()==8&&probe.delay(27000)<0,"window churn cannot create unbounded screenshots");
        probe.reset(38969);probe.window(24267,38969);probe.requested(38969);probe.requested(39669);
        long reportedScan=40937,legacyWatch=ScanPacing.watchDelay(true,true,true);
        check(probe.delay(reportedScan)==32&&legacyWatch==1000,"reported residual competes with one-second active watch");
        check(probe.scanAt(reportedScan,0)==40969,"arm the existing slot exactly, without the ordinary 120/450/1500ms scan floor");
        check(probe.scanAt(reportedScan,250)==41187,"shared global capture delay still wins over the planned slot");
        check(probe.scanAt(reportedScan,30000)<0,"global capture cap beyond the launch horizon must not arm a wake");
        check(probe.scanAt(68969,0)<0,"expired startup cannot arm a late probe");
        probe.reset(0);probe.window(1,0);for(long t:new long[]{0,700,2000})probe.requested(t);
        probe.window(2,2500);probe.requested(2500);probe.window(3,3000);probe.requested(3000);
        probe.evidence(3250);check(probe.scanAt(3250,0)==5000,"weak text and window events cannot enlarge the two extra slots");
        probe.surface(3250);check(probe.scanAt(3250,0)==3450,"first full-screen ad surface can pull an existing slot forward after churn, with the 450ms floor");
        probe.requested(3450);probe.surface(3500);check(probe.count()==6&&probe.scanAt(3500,0)==11000,"repeated image evidence does not keep pulling slots forward");
        probe.requested(11000);probe.requested(22000);check(probe.count()==8&&probe.scanAt(23000,0)<0,"pulling forward changes time, not screenshot count");
        probe.reset(24000);probe.window(9,24000);check(probe.scanAt(24000,0)==24000,"foreground reset starts an independent plan; old pull state is gone");
        probe.reset(43127);probe.window(24405,43127);probe.requested(43147);probe.requested(43828);
        probe.surface(44380);probe.requested(44390);
        check(probe.scanAt(44500,0)==45127,"stable rendered container miss would retain the reported sparse deadline");
        check(!probe.retrySurface(24406,44500),"another window cannot borrow this frame's evidence");
        check(probe.retrySurface(24405,44500)&&probe.surfaceRetryPending(),"first strong current-window miss can move the last regular slot forward");
        check(probe.scanAt(44500,0)==44740,"accelerated screenshot still respects the 350ms floor");
        check(probe.scanAt(44500,400)==44900,"shared capture cap still overrides the faster retry");
        probe.requested(44740);
        check(!probe.surfaceRetryPending()&&probe.scanAt(44740,0)==45190,"if early frame misses, the normal near-term slot still exists, subject to 450ms spacing");
        check(!probe.retrySurface(24405,44830),"late reserve cannot cause repeated retries");
        for(long t:new long[]{45190,48127,54127})probe.requested(t);
        probe.window(24406,55000);probe.requested(55000);
        check(probe.count()==8&&probe.scanAt(65127,0)<0,"earlier frame replaces the final regular slot; seven other requests plus it exhaust the same cap");
        probe.reset(0);probe.window(1,0);probe.requested(0);probe.requested(700);
        check(probe.retrySurface(1,790),"test an armed retry before the next scheduled slot");
        probe.window(2,810);check(!probe.surfaceRetryPending()&&probe.count()==2,"changing actual window cancels without spending the reserve");
        probe.requested(1150);check(probe.retrySurface(2,1250),"new current surface can independently earn the unspent retry");
        probe.cancelSurfaceRetry();check(!probe.surfaceRetryPending()&&probe.count()==3,"fresh tree without the full-screen surface cancels without charging");
        probe.reset(0);probe.window(1,0);probe.requested(0);check(!probe.retrySurface(1,601),"stale negative callback cannot arm faster recheck");
        long idleDue=ScanPacing.watchDelay(true,true,false),lastScan=11000,arrived=12500;
        long eventDue=arrived+Math.max(ScanPacing.eventDelay(true,false,false,false),ScanPacing.minimum(true)-(arrived-lastScan));
        check(eventDue==12530&&eventDue<22000&&eventDue<idleDue,"window event preempts sparse plan and idle timer");
        check(ScanPacing.eventDelay(false,true,true,false)==30&&ScanPacing.minimum(true)==120,"confirmed ads are not slowed by ordinary scrolling policy");
        VisualReview review=new VisualReview();review.requested(12530);check(review.requestDelay(12600,true)==280,"matched pair retains 350ms request interval");
        check(review.pairedRequestAt(12600,12548)==12880,"prompt first capture uses the existing screenshot-rate limit");
        check(review.pairedRequestAt(12760,12700)==13000,"delayed first capture waits for a genuinely new pair");
        check(review.pairedRequestAt(13120,12548)==13120,"a slow first analysis does not add another fixed sleep");
        check(review.requestDelay(12600,false)==380,"ordinary screenshot interval remains 450ms");
        for(int i=0;i<59;i++)review.requested(13000+i*450);
        check(review.requestDelay(40000,true)>0,"per-minute screenshot cap retained even for confirmation");
        check(review.pairedRefreshAt(40000,39106)<0,"expired panel recovery also respects the minute cap");
        replaySlowPanelReview();
        check(ScanPacing.watchDelay(false,true,false)<0&&ScanPacing.watchDelay(true,false,true)<0,"disabled/locked schedule has no timer wakeups");
        check(ScanPacing.watchDelay(true,true,false)>0&&ScanPacing.eventDelay(true,false,false,false)==30,"unlock/enable can rearm observation and immediate event scan");
        for(String path:args)replayImage(path);
        System.out.println("PASS: "+checks+" power-policy replay checks; quiet launch timer scans="+wakes+", idle root checks/min="+(60000/idleDue));
    }
    private static void replaySlowPanelReview(){
        String owner="tv.danmaku.bili";
        int w=1200,h=2670,window=3976;
        BiliVisual.Result panel=new BiliVisual.Result(677,549,20,"ad panel",true,40,1071);
        CanvasSkipVisual.Proof proof=new CanvasSkipVisual.Proof();
        check(!proof.confirm(owner,window,0,w,h,panel,20353,20415),"first panel frame waits for confirmation");
        check(proof.confirm(owner,window,0,w,h,panel,20697,20765),"second frame confirms before the full tree check");
        long checkedAt=21591;
        check(!VisualReview.currentFrame(checkedAt-proof.lastFrame(),w,h,w,h),"reported 894ms-old frame cannot authorize a touch");
        VisualReview review=new VisualReview();
        review.requested(20680);
        check(review.pairedRefreshAt(checkedAt,proof.lastFrame())==checkedAt,"894ms case can request a replacement immediately");
        check(review.pairedRefreshAt(21778,proof.lastFrame())<0,"a late scanner cannot use the old confirmation");
        check(proof.confirm(owner,window,0,w,h,panel,21635,21700),"fresh replacement pairs with the previous matching frame");
        check(VisualReview.currentFrame(21731-proof.lastFrame(),w,h,w,h),"example replacement remains fresh after a 31ms full check");
        check(!proof.confirm(owner,window+1,0,w,h,panel,22000,22065),"a different real window invalidates the pair");
        BiliVisual.Result movedLabel=new BiliVisual.Result(677,549,20,"other content",true,40,1100);
        check(!proof.confirm(owner,window+1,0,w,h,movedLabel,22350,22415),"matching X with a shifted disclosure cannot authorize a touch");
        proof.reset();
        check(review.pairedRefreshAt(checkedAt,proof.lastFrame())<0,"window or settings reset clears recovery evidence");
    }
    private static void replayImage(String path)throws Exception {
        BufferedImage source=ImageIO.read(new File(path)),im=new BufferedImage(720,1602,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,720,1602,null);g.dispose();
        int[] positive=im.getRGB(0,0,720,1602,null,0,720),negative=positive.clone();
        BiliVisual.Result word=CanvasSkipVisual.detect(positive,720,1602,false);check(word.found(),"reported ad layout still detects before scheduling changes");
        for(int y=Math.max(0,word.y-word.size);y<Math.min(1602,word.y+word.size);y++)for(int x=Math.max(0,word.x-word.size*2);x<Math.min(720,word.x+word.size*2);x++)negative[y*720+x]=0xff282828;
        BiliVisual.Result absent=CanvasSkipVisual.detect(negative,720,1602,false);check(!absent.found(),"same background before complete skip lettering is not actionable");
        NegativeFrameCache cache=new NegativeFrameCache();NegativeFrameCache.Sample first=cache.sample("a/1/0/context",negative,720,1602,1000,1040);cache.remember(first,absent.reason,true);
        check(cache.sample("a/1/0/context",negative,720,1602,1450,1490).reason!=null,"static unrecognized frame skips redundant detector work");
        check(cache.sample("a/1/0/context",positive,720,1602,1500,1540).reason==null,"new skip lettering immediately invalidates negative reuse");
        BiliVisual.Result found=CanvasSkipVisual.detect(positive,720,1602,false);cache.remember(null,found.reason,false);
        CanvasSkipVisual.Proof proof=new CanvasSkipVisual.Proof();check(!proof.confirm("a.app",1,0,720,1602,found,1500,1540),"first newly found target still waits for independent confirmation");
        check(cache.sample("a/1/0/context",positive,720,1602,1850,1890).reason==null,"positive target never comes from cache");
        BiliVisual.Result second=CanvasSkipVisual.detect(positive,720,1602,false);check(proof.confirm("a.app",1,0,720,1602,second,1850,1890),"second independently detected fresh target still confirms");
        cache.clear();check(cache.sample("newOwner/2/0/context",negative,720,1602,1950,1990).reason==null,"foreground/window change removes all negative reuse");
        // Warm the detector, then compare identical synthetic frame arrivals at 450ms spacing.
        for(int i=0;i<4;i++)CanvasSkipVisual.detect(negative,720,1602,false);
        long begin=System.nanoTime();for(int i=0;i<20;i++){BiliVisual.Result r=CanvasSkipVisual.detect(negative,720,1602,false);sink^=r.x;}long before=System.nanoTime()-begin;
        cache.clear();int analyses=0,reuses=0;begin=System.nanoTime();
        for(int i=0;i<20;i++){long t=10000+i*450;NegativeFrameCache.Sample s=cache.sample("stable",negative,720,1602,t,t+40);
            if(s.reason!=null)reuses++;else {BiliVisual.Result r=CanvasSkipVisual.detect(negative,720,1602,false);analyses++;cache.remember(s,r.reason,!r.found());sink^=r.x;}}
        long after=System.nanoTime()-begin;check(analyses==7&&reuses==13,"twenty identical negative frames need seven detector runs with fixed periodic reanalysis");
        System.out.println(new File(path).getName()+" desktop-only repeated-negative ns: full="+before+", cache="+after+", detector runs="+analyses+", reuses="+reuses);
    }
}
