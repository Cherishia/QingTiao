package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

public final class JmVisualTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static BiliVisual.Result detect(BufferedImage image) {
        int w=image.getWidth(),h=image.getHeight();return JmVisual.detect(image.getRGB(0,0,w,h,null,0,w),w,h);
    }
    private static void replayCaptionDeadline(){
        TimedClose clock=new TimedClose();
        int window=33131;
        long start=10000; // 23:05:14.354 in the report, measured relative to an arbitrary uptime.
        check(clock.captionProbeAt(start)==-1,"route selection alone schedules no frame");
        clock.caption(window,true,start);
        long checkpoint=clock.captionProbeAt(start);
        check(checkpoint==start+1800&&!clock.captionDue(window,checkpoint),"first checkpoint only refreshes caption evidence");
        clock.caption(window,true,checkpoint);
        long deadline=clock.captionProbeAt(checkpoint);
        check(deadline==start+3100,"native caption schedules first screenshot at the original deadline");
        check(!clock.captionDue(window,deadline-1)&&clock.captionDue(window,deadline),"a native read can bypass coalescing at the deadline");
        check(!clock.captionDue(window+1,deadline)&&!clock.readyFrame(deadline),"old window and text alone cannot authorize a gesture");
        check(start+4213-deadline==1113,"report's X confirmation was 1113ms after the scheduled target");
        clock.caption(window,true,deadline);
        check(clock.remaining(window,1085,337,12,deadline)==0&&clock.readyFrame(deadline),"on-time X is actionable if the fresh geometry matches");
        check(!clock.readyFrame(deadline-1),"a frame captured early cannot be used after slow processing");
        clock.clear();clock.caption(window,true,start);
        check(clock.captionProbeAt(deadline)==-1,"unrefreshed caption expires before it can open the deadline path");
        clock.caption(window+1,false,start+1000);
        check(clock.captionProbeAt(start+1000)==-1,"window replacement clears pending caption work");
    }
    public static void main(String[] args)throws Exception {
        replayCaptionDeadline();
        java.util.List<AdMatcher.Node> sparse=new ArrayList<>();sparse.add(new AdMatcher.Node(-1,0,0,1080,2400,"",""));
        check(JmVisual.context("custom.webview",true,sparse),"sparse startup may be inspected without readable ad text");
        check(!JmVisual.context("custom.webview",false,sparse),"outside startup denied");
        check(!JmVisual.context("",true,sparse),"unknown foreground denied");
        check(!JmVisual.context("custom.webview",true,Collections.emptyList()),"no root identity denied");
        sparse.get(0).password=true;check(!JmVisual.context("custom.webview",true,sparse),"password denied");sparse.get(0).password=false;
        sparse.get(0).text="請等待 3 秒後按右上 Ｘ 關閉廣告";
        check(JmVisual.captionNodes(sparse),"exact close-ad instruction may be obtained from accessibility");
        sparse.get(0).text="请选择线路，3秒后重试";
        check(!JmVisual.captionNodes(sparse),"route selection does not authorize an ad action");
        sparse.get(0).text="确定";check(!JmVisual.captionNodes(sparse),"ordinary confirmation is not an ad caption");sparse.get(0).text="";
        for(int i=0;i<41;i++)sparse.add(new AdMatcher.Node(0,0,0,1,1,"",""));
        check(JmVisual.context("custom.webview",true,sparse),"image-ad probes must not be blocked just because there are more than 40 nodes");
        for(int i=sparse.size();i<1000;i++)sparse.add(new AdMatcher.Node(0,0,0,1,1,"",""));
        check(!JmVisual.context("custom.webview",true,sparse),"incomplete traversal still denied");
        TimedClose timer=new TimedClose();
        check(!timer.active(),"no pending timed scene initially");
        check(timer.remaining(1,520,162,6,1000)==3100,"do not click on first observation");
        check(timer.active(),"verified timed scene remains observable beyond sparse probe budget");
        timer.remaining(1,520,162,6,2800);
        check(timer.remaining(1,521,163,6,3999)==101,"still wait before three seconds");
        check(timer.remaining(1,520,162,6,4100)==0,"ready only after waiting 3100ms");
        check(timer.remaining(2,520,162,6,4200)==3100,"new window restarts wait");
        check(timer.remaining(2,490,162,6,4300)==3100,"different target restarts wait");
        check(timer.remaining(2,490,162,6,100)==3100,"backwards clock resets wait");
        timer.clear();check(!timer.active(),"clearing scene also clears pending state");
        check(timer.remaining(2,490,162,6,9000)==3100,"disappearance/cancel clears wait");
        timer.clear();timer.remaining(3,500,150,8,10000);
        check(timer.missing(3,11000)&&timer.active(),"one missed frame preserves pending timer without granting an action");
        check(timer.remaining(3,501,151,8,12000)==1100,"miss does not restart elapsed wait");
        check(timer.missing(3,12500),"another short miss preserves wait");
        check(timer.remaining(3,500,150,8,13200)==0,"new positive frame may proceed after original wait despite misses");
        check(!timer.missing(3,15800)&&!timer.active(),"long observed disappearance expires timer");
        check(timer.describe(15800).contains("2500ms"),"expiration reason remains diagnosable");
        check(timer.remaining(3,500,150,8,15900)==3100,"reappearing target after long disappearance waits again");
        check(!timer.missing(4,16000),"window change never carries old wait across windows");
        timer.remaining(4,500,150,8,17000);timer.missing(4,17500);
        check(timer.remaining(4,500,150,8,20000)==3100,"long gap following a missed observation restarts wait on reappearance");
        timer.clear();timer.caption(178,true,0);timer.caption(178,true,472);timer.caption(178,true,930);timer.caption(178,true,2492);
        check(!timer.active(),"mature caption alone never grants an X target or an action");
        check(timer.remaining(178,1085,337,12,2959)==141,"replay: user log's first red X has only 141ms left, rather than another 3100ms");
        check(timer.remaining(178,1085,337,12,3100)==0,"replay: original caption deadline suffices when a fresh X is found");
        check(timer.describe(3100).contains("首次读到广告关闭提示"),"diagnostic identifies the earlier timing source");
        timer.clear();timer.caption(1,true,1000);timer.caption(1,true,1500);
        check(timer.remaining(1,500,150,8,2000)==2100,"an early visible X still waits until the caption deadline");
        timer.caption(1,true,2500);check(timer.remaining(1,500,150,8,3000)==1100,"re-reading the caption never restarts the clock");
        check(timer.remaining(1,530,150,8,3100)==3100,"a moved target cannot borrow old mature timing");
        timer.clear();timer.caption(1,true,1000);timer.caption(2,false,1500);
        check(timer.remaining(2,500,150,8,5000)==3100,"caption clock does not carry into a new window");
        timer.clear();timer.caption(1,true,1000);timer.caption(1,false,4000);
        check(timer.remaining(1,500,150,8,4100)==3100,"expired caption cannot shorten a later unrelated wait");
        timer.clear();timer.caption(1,true,1000);timer.caption(1,true,500);
        check(timer.remaining(1,500,150,8,600)==3000,"backwards clock resets the caption origin");
        timer.clear();timer.caption(1,true,1000);timer.clear();
        check(timer.remaining(1,500,150,8,5000)==3100,"foreground or setting cancellation clears pre-X caption timing too");
        timer.clear();timer.caption(1,false,1000);
        check(timer.remaining(1,500,150,8,5000)==3100,"route choice without a close-ad caption never starts the ad clock");
        timer.clear();timer.caption(24101,true,0);
        check(timer.nextProbeAt(0)==0&&!timer.readyFrame(3100),"caption does not establish a pixel continuation");
        check(timer.remaining(24101,1085,337,12,56)==3044,"new report models the initial X at 56ms");
        check(timer.nextProbeAt(56)==1856&&timer.nextProbeAt(628)==1856&&timer.nextProbeAt(1199)==1856,"content churn leaves one continuity checkpoint instead of repeated half-second samples");
        check(timer.remaining(24101,1085,337,12,1856)==1244&&timer.nextProbeAt(1856)==3100,"second observation schedules the original deadline, not a new interval");
        check(!timer.readyFrame(3099)&&timer.readyFrame(3100),"processing finishing late cannot authorize a frame captured before the deadline");
        check(timer.remaining(24101,1086,337,12,3130)==0,"fresh final observation is the third positive frame, vs seven in the report");
        timer.clear("已提交动作");check(!timer.active()&&!timer.readyFrame(4000),"post-action review has no remaining countdown authority");
        timer.caption(24101,true,5000);timer.remaining(24101,1085,337,12,5056);
        check(timer.missing(24101,6856)&&timer.nextProbeAt(6856)==6856,"miss at a planned checkpoint does not create a new waiting period or authorize the lost X");
        check(timer.remaining(24101,1085,337,12,8110)==3100,"long unconfirmed gap cannot authorize a late action");
        timer.caption(24102,false,8120);check(!timer.active(),"a changed window clears the earlier timer before taking more frames");
        check(timer.remaining(24102,1085,337,12,8200)==3100,"same coordinate in a replacement window needs independent timing");
        check(timer.remaining(24102,1101,337,12,8400)==3100,"shifted red X does not carry over the old clock");
        check(!timer.readyFrame(11299)&&timer.readyFrame(11500),"moved target requires a frame at its own deadline");
        check(!JmVisual.detect(null,575,1280).found(),"null rejected");
        check(!JmVisual.detect(new int[10],575,1280).found(),"truncated buffer rejected");
        check(!JmVisual.detect(new int[575*1280],1280,575).found(),"landscape rejected");
        BufferedImage fake=new BufferedImage(575,1280,BufferedImage.TYPE_INT_RGB);Graphics2D g=fake.createGraphics();
        g.setColor(new Color(242,242,242));g.fillRect(0,0,575,1280);g.setColor(Color.BLACK);g.fillOval(494,136,52,52);
        g.setColor(Color.RED);g.setStroke(new BasicStroke(5));g.drawLine(512,154,528,170);g.drawLine(528,154,512,170);g.dispose();
        check(!detect(fake).found(),"red X in black circle alone must not close ordinary content");
        check(JmVisual.detect(fake.getRGB(0,0,575,1280,null,0,575),575,1280,true).found(),"fresh accessibility caption plus complete red-X control can replace font-dependent pixel caption");
        System.out.println("PASS: "+checks+" timed close visual checks");
        for(String path:args) {
            boolean expected=!path.startsWith("!");if(!expected)path=path.substring(1);
            BufferedImage source=ImageIO.read(new File(path));
            for(int sw:new int[]{source.getWidth(),720,1080,1200}) {
                int sh=Math.round(source.getHeight()*(sw/(float)source.getWidth()));
                BufferedImage im=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);g=im.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source,0,0,sw,sh,null);g.dispose();long begin=System.nanoTime();
                BiliVisual.Result r=detect(im);
                System.out.println(new File(path).getName()+" "+sw+"x"+sh+": "+r.found()+" @ "+r.x+","+r.y+" "+r.reason+" "+((System.nanoTime()-begin)/1000000)+"ms");
                check(r.found()==expected,"fixture outcome at "+sw+": "+path);
                if(expected) {
                    check(Math.abs(r.x/(double)sw-.904)<.012&&Math.abs(r.y/(double)sh-.126)<.008,"red X center from frame");
                    // The ad artwork itself must be irrelevant to close-target matching.
                    g=im.createGraphics();g.setColor(new Color(242,242,242));g.fillRect(0,(int)(sh*.19),sw,sh);g.dispose();
                    check(detect(im).found(),"ad artwork is not a matching prerequisite");
                    BufferedImage captionOnly=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);g=captionOnly.createGraphics();
                    g.drawImage(im,0,0,null);g.setColor(new Color(242,242,242));g.fillRect((int)(sw*.85),(int)(sh*.10),(int)(sw*.11),(int)(sh*.05));g.dispose();
                    check(!detect(captionOnly).found(),"close caption without red X denied");
                    check(!JmVisual.detect(captionOnly.getRGB(0,0,sw,sh,null,0,sw),sw,sh,true).found(),"accessibility caption never authorizes clicking an absent X");
                    g=captionOnly.createGraphics();g.setColor(Color.BLACK);g.fillOval((int)(sw*.86),(int)(sh*.106),(int)(sw*.09),(int)(sw*.09));
                    g.setColor(Color.RED);g.setStroke(new BasicStroke(Math.max(2,sw*.008f)));
                    int cx=Math.round(sw*.905f),cy=Math.round(sh*.126f),arm=Math.round(sw*.014f);
                    g.drawLine(cx-arm,cy,cx+arm,cy);g.drawLine(cx,cy-arm,cx,cy+arm);g.dispose();
                    check(!detect(captionOnly).found(),"red plus with close caption is not the close X");
                    check(!JmVisual.detect(captionOnly.getRGB(0,0,sw,sh,null,0,sw),sw,sh,true).found(),"semantic caption cannot turn a red plus into a close X");
                    g=im.createGraphics();g.setColor(new Color(242,242,242));g.fillRect((int)(sw*.24),(int)(sh*.144),(int)(sw*.52),(int)(sh*.027));g.dispose();
                    check(!detect(im).found(),"red X without close-ad caption denied");
                    check(JmVisual.detect(im.getRGB(0,0,sw,sh,null,0,sw),sw,sh,true).found(),"fresh matching accessibility caption supports an otherwise unfamiliar rendered caption");
                }
            }
        }
    }
}
