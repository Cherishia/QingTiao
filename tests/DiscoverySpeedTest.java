package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.Arrays;

public final class DiscoverySpeedTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ArrayList<AdMatcher.Node> nodes(String id){ArrayList<AdMatcher.Node> n=new ArrayList<>();n.add(new AdMatcher.Node(-1,0,0,720,1602,"",null));n.add(new AdMatcher.Node(0,0,20,720,1570,"",id));return n;}
    private static int[] fixture(int style,int w,int h){
        int[] p=new int[w*h];Arrays.fill(p,0xff303030);int th=Math.round(w*.038f),tw=th*2,x=(int)(w*.845),y=(int)(h*.07);
        float[] ref=SkipGlyphs.reference(style,40,20);
        for(int yy=0;yy<th;yy++)for(int xx=0;xx<tw;xx++){int v=(int)ref[yy*20/th*40+xx*40/tw];p[(y+yy)*w+x+xx]=0xff000000|v<<16|v<<8|v;}
        return p;
    }
    public static void main(String[] args){
        TreeReadGate g=new TreeReadGate();check(g.delay("a",1,0,720,1602,1000,false,true)==0,"first discovery tree is never delayed");
        g.readFinished("a",1,0,720,1602,1663);
        check(g.delay("a",1,0,720,1602,1693,false,true)==320,"663ms traversal is not immediately repeated on a content flood");
        check(g.delay("a",1,0,720,1602,1693,false,false)==970,"quiet later read leaves an idle gap");
        check(g.recent(1693)&&!g.recent(2663),"recent metadata checks need no descendant prefetch");
        check(g.delay("a",1,0,720,1602,1693,true,true)==0,"current target or explicit skip/reminder event bypasses discovery coalescing");
        for(int changed=0;changed<5;changed++)check(g.delay(changed==0?"b":"a",changed==1?2:1,changed==2?1:0,changed==3?1200:720,changed==4?2670:1602,1693,false,true)==0,"owner/window/display/size change never reuses old waiting state="+changed);
        check(g.delay("a",1,0,720,1602,2013,false,true)==0&&g.delay("a",1,0,720,1602,2663,false,false)==0,"same window may be read when quiet gap expires");
        g.reset();check(!g.recent(1700)&&g.delay("a",1,0,720,1602,1700,false,false)==0,"screen/setting reset clears coalescing state");
        int before=0,after=0;long t=0;while(t<10000){before++;t+=663+30;}g.reset();t=0;
        while(t<10000){long wait=g.delay("a",1,0,720,1602,t,false,true);if(wait>0){t+=wait;continue;}after++;t+=663;g.readFinished("a",1,0,720,1602,t);t+=30;}
        check(after<before&&after>=6,"same costly discovery input needs fewer full tree reads without hiding roots");
        for(String cue:new String[]{"跳过","3 | 跳过","跳过广告","广告","青少年模式","未成年人模式提醒"})check(TreeReadGate.cue(cue),"explicit event cue gets immediate fresh tree="+cue);
        check(!TreeReadGate.cue("普通页面")&&!TreeReadGate.cue(null)&&!TreeReadGate.cue(new String(new char[257])),"unrelated/unknown/oversized text does not make work urgent");
        EventInbox inbox=new EventInbox();check(inbox.offer("a",1,100,110,true),"first event schedules once");check(!inbox.offer("a",2,120,130,false),"content flood coalesces");
        EventInbox.Event e=inbox.take();check(e.cue&&e.types==3&&e.arrivedAt==130,"an early explicit cue survives later ordinary notifications");
        inbox.offer("a",1,140,150);check(!inbox.take().cue,"next batch does not inherit cue");inbox.offer("a",1,160,170,true);inbox.clear();check(inbox.take()==null,"cancellation removes pending cue");
        StartupProbe plan=new StartupProbe();plan.reset(0);plan.window(1,0);
        for(long age:new long[]{0,700,2000,5000})plan.requested(age);
        check(plan.delay(7000)==4000,"reported late discovery would otherwise wait several seconds");plan.evidence(7000);
        check(plan.delay(7000)==0,"new explicit ad surface can request one existing extra opportunity immediately");plan.requested(7000);
        check(plan.count()==5&&plan.delay(7350)==3650,"evidence did not spend the later eleven-second regular probe");
        VisualReview frames=new VisualReview();frames.requested(7000);check(frames.requestDelay(7350,true)==0,"found target second frame remains available at 350ms despite sparse discovery wait");
        plan.evidence(7600);check(plan.delay(7600)==0,"second shared extra opportunity remains bounded");plan.requested(7600);
        plan.window(2,8200);plan.evidence(8200);check(plan.delay(8200)==2800,"window churn and SDK evidence share the same two-slot cap");
        plan.requested(11000);plan.requested(22000);plan.evidence(23000);check(plan.count()==8&&plan.delay(23000)<0,"extra evidence cannot enlarge six-plus-two discovery budget");
        check(FrameIdentity.mismatch("a",1,0,720,1602,"a",1,0,720,1602).isEmpty(),"identical source is valid");
        for(int change=0;change<5;change++){String s=FrameIdentity.mismatch("a",1,0,720,1602,change==0?"b":"a",change==1?2:1,change==2?1:0,change==3?1200:720,change==4?2670:1602);check(!s.isEmpty()&&s.contains("目标=")&&s.contains("当前="),"each actual identity mismatch is rejected and distinguishable="+change);}
        for(String id:new String[]{"sdk:id/tt_splash_root","sdk:id/ksad_splash_container","sdk:id/mbridge_splash_view","sdk:id/gdt_splash_view","host:id/startup_ad","host:id/startup_ad_container","host:id/splash_ad","host:id/ad_splash_container"}){
            ArrayList<AdMatcher.Node> n=nodes(id);check(!CanvasSkipVisual.nativeAd(n)&&AdSurface.present(n,720,1602)&&CanvasSkipVisual.nodeAd(n,720,1602),"explicit full-screen container can prove ad without a text marker="+id);
        }
        for(String cls:new String[]{"sdk.SplashAdView","host.AdSplashView","sdk.SplashAdLayout","sdk.SplashAdContainer"}){ArrayList<AdMatcher.Node> n=nodes(null);n.get(1).className=cls;check(AdSurface.present(n,720,1602),"explicit fullscreen ad class="+cls);}
        for(int bad=0;bad<10;bad++){
            ArrayList<AdMatcher.Node> n=nodes("host:id/startup_ad");AdMatcher.Node v=n.get(1);
            if(bad==0)v.visible=false;if(bad==1)v.enabled=false;if(bad==2)v.editable=true;if(bad==3)v.checkable=true;if(bad==4)v.scrollable=true;
            if(bad==5)v.bottom=500;if(bad==6)v.right=300;if(bad==7)n.get(0).password=true;if(bad==8)v.id="host:id/splash";if(bad==9)v.id="host:id/ad_preferences";
            check(!AdSurface.present(n,720,1602),"hidden/state/inline/sensitive/ordinary splash/settings not proof="+bad);
        }
        for(int style=0;style<3;style++)for(int[] size:new int[][]{{575,1280},{720,1602}}){
            int w=size[0],h=size[1];int[] p=fixture(style,w,h);BiliVisual.Result r=PlainSkipVisual.detect(p,w,h);
            check(r.found()&&r.requiresPair&&r.x>w*.84&&r.y<h*.10,"strict isolated skip glyphs="+style+"/"+w+" "+r.reason);
            check(CanvasSkipVisual.detect(p,w,h,true).found(),"shared ad-evidence path supports plain skip without countdown");
            check(!CanvasSkipVisual.detect(p,w,h,false).found(),"no ad context cannot borrow the word alone");
            for(int part=0;part<2;part++){int[] missing=p.clone();int x=(int)(w*.845)+part*Math.round(w*.038f),y=(int)(h*.07),th=Math.round(w*.038f);for(int yy=y;yy<y+th;yy++)Arrays.fill(missing,yy*w+x,yy*w+x+th,0xff303030);check(!PlainSkipVisual.detect(missing,w,h).found(),"each full character mandatory="+part);}
        }
        check(!PlainSkipVisual.detect(null,720,1602).found()&&!PlainSkipVisual.detect(new int[4],720,1602).found(),"invalid pixels fail closed");
        for(int[] dim:new int[][]{{575,1280},{720,1602}}){
            int w=dim[0],h=dim[1],th=Math.round(w*.038f),x=(int)(w*.845),top=(int)(h*.07),bottom=(int)(h*.935);
            int[] wordOnly=fixture(0,w,h),footer=new int[w*h];Arrays.fill(footer,0xff828282);
            for(int y=bottom-th/2;y<bottom+th*3/2;y++)Arrays.fill(footer,y*w+x-th/2,y*w+x+th*5/2,0xff303030);
            for(int y=0;y<th;y++)System.arraycopy(wordOnly,(top+y)*w+x,footer,(bottom+y)*w+x,th*2);
            BiliVisual.Result hit=CanvasSkipVisual.detect(footer,w,h,true);
            check(hit.found()&&hit.requiresPair&&hit.x>x&&hit.y>h*.93,"lower-right plain skip is reusable without countdown at "+w+": "+hit.reason);
            check(!CanvasSkipVisual.detect(footer,w,h,false).found(),"footer CTA without independent ad evidence cannot authorize action");
            int[] missing=footer.clone();for(int y=bottom;y<bottom+th;y++)Arrays.fill(missing,y*w+x+th,y*w+x+2*th,0xff303030);
            check(!CanvasSkipVisual.detect(missing,w,h,true).found(),"both footer glyphs are required");
            int[] flat=footer.clone();for(int y=bottom-2*th;y<bottom-th/2;y++)Arrays.fill(flat,y*w+w*3/4,(y+1)*w,0xff303030);
            check(!CanvasSkipVisual.detect(flat,w,h,true).found(),"footer word without separate upper capsule boundary is not enough");
        }
        CanvasSkipVisual.Proof pair=new CanvasSkipVisual.Proof();BiliVisual.Result word=PlainSkipVisual.detect(fixture(0,720,1602),720,1602);
        check(!pair.confirm("any.app",1,0,720,1602,word,1000,1040),"plain word requires first fresh frame");check(!pair.confirm("any.app",1,0,720,1602,word,1000,1090),"same frame is never a pair");check(pair.confirm("any.app",1,0,720,1602,word,1350,1390),"second actual detection can confirm");check(!pair.confirm("other.app",1,0,720,1602,word,1700,1740),"changed source cannot carry proof");
        System.out.println("PASS: "+checks+" discovery/coalescing/plain-skip checks; desktop replay tree reads="+before+" -> "+after+", not a phone power measurement");
    }
}
