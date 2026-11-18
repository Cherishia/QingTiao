package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.ArrayList;

public final class SplashLatencyTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ArrayList<AdMatcher.Node> fixture(String owner,String label){
        ArrayList<AdMatcher.Node> n=new ArrayList<>();
        n.add(new AdMatcher.Node(-1,0,0,1200,2670,"",null));
        n.add(new AdMatcher.Node(0,0,0,1200,2670,"",owner+":id/root"));
        n.add(new AdMatcher.Node(1,0,0,1200,2670,"",owner+":id/splash_layout"));
        n.add(new AdMatcher.Node(2,0,0,1200,2670,"",owner+":id/splash_button_container"));
        n.add(new AdMatcher.Node(2,48,126,1152,2620,"",owner+":id/splash_interact_view"));
        n.add(new AdMatcher.Node(3,870,2441,1152,2582,label,owner+":id/count_down"));n.get(5).clickable=true;
        for(AdMatcher.Node z:n)z.className="android.widget.FrameLayout";n.get(5).className="android.widget.TextView";return n;
    }
    private static int find(ArrayList<AdMatcher.Node> n){return NativeSplashSkip.find(n,1200,2670,true,"tv.danmaku.bili");}
    private static ArrayList<AdMatcher.Node> markedSplash(String owner,String scope,String role,String label,String marker){
        int w=1200,h=2670;ArrayList<AdMatcher.Node> n=new ArrayList<>();
        n.add(new AdMatcher.Node(-1,0,0,w,h,"",null));
        n.add(new AdMatcher.Node(0,0,0,w,h,"",owner+":id/root"));
        n.add(new AdMatcher.Node(1,0,0,w,h,"",owner+":id/"+scope));
        n.add(new AdMatcher.Node(2,72,2470,174,2544,marker,null));
        n.add(new AdMatcher.Node(2,925,392,1150,492,label,owner+":id/"+role));
        n.get(4).clickable=true;n.get(1).className=n.get(2).className="android.widget.FrameLayout";return n;
    }
    private static ArrayList<AdMatcher.Node> paintedSplash(String owner,String role,String label){
        ArrayList<AdMatcher.Node> n=markedSplash(owner,"opening_overlay",role,label,"");
        n.set(3,new AdMatcher.Node(2,0,0,1200,2670,"",owner+":id/iv_advertisement_image"));
        n.get(3).className="android.widget.ImageView";return n;
    }
    private static void testReusableNativeFamilies(){
        for(String pkg:new String[]{"example.reading", "example.weather"}){
            for(String id:new String[]{"splash_ad_layout","ad_splash_root","opening_ad_container","startup_ad_view"}) {
                ArrayList<AdMatcher.Node> n=markedSplash(pkg,id,"splash_skip_button","跳过","广告");
                check(NativeSplashSkip.find(n,1200,2670,true,pkg)==4,"shared root and marker identify plain skip below the old top-corner thresholds "+pkg+"/"+id);
                check(AdMatcher.candidates(n,1200,2670,true,true,true,pkg).equals(java.util.Collections.singletonList(4)),"verified native result is acted on directly");
            }
            for(String label:new String[]{"跳过 2 秒","Skip 2s","3 | 跳過","跳过(1s)","Skip ad"}){
                ArrayList<AdMatcher.Node> n=paintedSplash(pkg,"btn_skip",label);
                check(NativeSplashSkip.find(n,1200,2670,true,pkg)==4,"explicit creative and exact timed/declared action: "+label);
                check(NativeSplashSkip.currentControl(n.get(4),1200,2670,pkg),"fresh valid control: "+label);
            }
        }
        String pkg="example.weather";
        ArrayList<AdMatcher.Node> explicit=markedSplash(pkg,"splash_ad_container","ad_skip","跳过广告","");
        check(NativeSplashSkip.find(explicit,1200,2670,true,pkg)==4,"explicit skip-ad wording corroborates an exact splash ad viewport");
        explicit.get(4).text="";explicit.get(4).desc="Skip ad";
        explicit.get(4).left=35;explicit.get(4).right=250;explicit.get(4).top=2450;explicit.get(4).bottom=2530;
        check(NativeSplashSkip.find(explicit,1200,2670,true,pkg)==4,"left footer control can have only the accessibility label");
        for(int bad=0;bad<18;bad++){
            ArrayList<AdMatcher.Node> n=markedSplash(pkg,"splash_ad_layout","skip_button","跳过","广告");AdMatcher.Node z=n.get(4),m=n.get(3);
            if(bad==0)m.visible=false;if(bad==1)m.text="广告列表";if(bad==2)m.text="广告详情";if(bad==3)m.parent=1;
            if(bad==4)n.get(2).id=pkg+":id/splash_ad_settings";if(bad==5)z.id="foreign:id/skip_button";if(bad==6)z.id=pkg+":id/skip_button_share";
            if(bad==7)z.desc="立即查看";if(bad==8)z.left=500;if(bad==9)z.top=1350;if(bad==10)n.get(2).bottom=1335;
            if(bad==11)n.get(2).scrollable=true;if(bad==12)n.get(1).className="com.demo.ImagePreview";if(bad==13)n.get(1).editable=true;
            if(bad==14)n.get(1).text="确认付款";if(bad==15)m.parent=4;
            if(bad==16)n.add(new AdMatcher.Node(2,950,42,1120,130,"跳过 2","cj.mobile.wm.allmodules:id/wm_splash_jump_tv"));
            if(bad==17)n.get(1).scrollable=true;
            check(NativeSplashSkip.find(n,1200,2670,true,pkg)<0,"scoped native family rejects weakened evidence: "+bad);
        }
        for(int bad=0;bad<9;bad++){
            ArrayList<AdMatcher.Node> n=paintedSplash(pkg,"skip_button","跳过 3");AdMatcher.Node image=n.get(3),z=n.get(4);
            if(bad==0)image.className="android.widget.FrameLayout";if(bad==1)image.id=pkg+":id/iv_advertisement_image_preview";
            if(bad==2)image.bottom=1200;if(bad==3)image.visible=false;if(bad==4)z.text="跳过";
            if(bad==5)z.text="3";if(bad==6)image.id="foreign:id/iv_advertisement_image";
            if(bad==7){n.get(2).parent=0;z.parent=1;}
            if(bad==8)image.parent=1; // Different full-screen overlay under the same app root.
            check(NativeSplashSkip.find(n,1200,2670,true,pkg)<0,"fullscreen image alone cannot authorize a changing or unrelated action: "+bad);
        }
        ArrayList<AdMatcher.Node> n=markedSplash(pkg,"splash_ad_layout","skip_button","跳过 2","广告");
        AdMatcher.Node twin=new AdMatcher.Node(2,930,2448,1140,2518,"跳过 2",pkg+":id/skip_ad");twin.clickable=true;n.add(twin);
        check(NativeSplashSkip.find(n,1200,2670,true,pkg)==NativeSplashSkip.CONFLICT,"two verified controls have no unique target");
        check(AdMatcher.candidates(n,1200,2670,true,true,true,pkg).isEmpty(),"ambiguity cannot fall back to the broader legacy native matcher");
        AdMatcher.Node shown=paintedSplash(pkg,"skip_button","跳过(3s)").get(4);
        AdMatcher.Node refreshed=paintedSplash(pkg,"skip_button","跳过(2s)").get(4);
        check(NativeSplashSkip.currentControl(shown,refreshed,1200,2670,pkg),"new countdown value does not change the native role");
        refreshed.text="跳过";
        check(!NativeSplashSkip.currentControl(shown,refreshed,1200,2670,pkg),"refresh cannot silently lose countdown evidence");
        shown.text="跳过广告";refreshed.text="跳过 2";
        check(!NativeSplashSkip.currentControl(shown,refreshed,1200,2670,pkg),"refresh cannot silently lose explicit ad wording");
    }
    private static BufferedImage lightFixture()throws Exception{
        BufferedImage im=new BufferedImage(720,1602,1);Graphics2D g=im.createGraphics();g.setColor(new Color(180,200,220));g.fillRect(0,0,720,1602);g.setColor(new Color(90,115,130));g.fillRoundRect(515,1455,140,85,70,70);g.dispose();
        float[] word=SkipGlyphs.reference(0,40,20);for(int y=0;y<26;y++)for(int x=0;x<52;x++){int v=(int)word[y*20/26*40+x*40/52];im.setRGB(550+x,1484+y,0xff000000|v<<16|v<<8|v);}
        g=im.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Serif",Font.PLAIN,25));g.drawString("2",613,1508);
        g.setColor(new Color(206,223,237));g.setStroke(new BasicStroke(4));
        for(int offset:new int[]{0,17}){g.drawLine(339,1236+offset,360,1219+offset);g.drawLine(360,1219+offset,381,1236+offset);}g.dispose();
        Field f=BiliLaunchVisual.class.getDeclaredField("MARK");f.setAccessible(true);long[] mark=(long[])f.get(null);
        for(int y=0;y<60;y++)for(int x=0;x<120;x++)if((mark[y*24/60]&(1L<<(x*48/120)))!=0)im.setRGB(47+x,1465+y,Color.WHITE.getRGB());return im;
    }
    private static BufferedImage copy(BufferedImage im){BufferedImage out=new BufferedImage(720,1602,1);Graphics2D g=out.createGraphics();g.drawImage(im,0,0,null);g.dispose();return out;}
    private static void erase(BufferedImage im,int x,int y,int w,int h){Graphics2D g=im.createGraphics();g.setColor(new Color(180,200,220));g.fillRect(x,y,w,h);g.dispose();}
    private static BiliVisual.Result detect(BufferedImage im){return BiliLaunchVisual.detect(im.getRGB(0,0,720,1602,null,0,720),720,1602);}
    public static void main(String[] args)throws Exception{
        testReusableNativeFamilies();
        ArrayList<AdMatcher.Node> report=fixture("tv.danmaku.bili","跳过 0");
        check(find(report)==5,"reported exact native count_down with three full-screen roles");
        check(AdMatcher.candidates(report,1200,2670,true,true,true,"tv.danmaku.bili").contains(5),"native button is actionable without inaccessible ad text or image probe");
        AdMatcher.Node refreshed=fixture("another.owner","跳过 2").get(5);check(NativeSplashSkip.currentControl(refreshed,1200,2670,"another.owner"),"current control check retains explicit full wording");
        refreshed.clickable=false;check(!NativeSplashSkip.currentControl(refreshed,1200,2670,"another.owner"),"refresh removing clickability cancels even where ordinary fingerprints omit this state");refreshed.clickable=true;
        refreshed.text="跳过";check(!NativeSplashSkip.currentControl(refreshed,1200,2670,"another.owner"),"skip-normalized fingerprint cannot hide countdown wording drift");refreshed.text="跳过 2";refreshed.id="another.owner:id/other";check(!NativeSplashSkip.currentControl(refreshed,1200,2670,"another.owner"),"changed current resource cancels");
        for(String owner:new String[]{"tv.danmaku.bili","another.layout.owner"})for(int digit=0;digit<=30;digit++){
            ArrayList<AdMatcher.Node> n=fixture(owner,"跳过 "+digit);check(NativeSplashSkip.find(n,1200,2670,true,owner)==5,"owner-independent layout and changing countdown="+owner+"/"+digit);
        }
        for(int bad=0;bad<30;bad++){
            ArrayList<AdMatcher.Node> n=fixture("tv.danmaku.bili","跳过 2");AdMatcher.Node z=n.get(5);
            if(bad==0)z.visible=false;if(bad==1)z.enabled=false;if(bad==2)z.clickable=false;if(bad==3)z.editable=true;if(bad==4)z.checkable=true;if(bad==5)z.scrollable=true;
            if(bad==6)z.text="领取 2";if(bad==7)z.text="跳过设置";if(bad==8)z.desc="前往广告";if(bad==9)z.id="tv.danmaku.bili:id/count_down_copy";
            if(bad==10)z.left=20;if(bad==11)z.top=2000;if(bad==12)z.bottom=2671;if(bad==13)z.right=1201;if(bad==14)z.parent=1;
            if(bad==15)n.get(2).visible=false;if(bad==16)n.get(3).id="tv.danmaku.bili:id/ordinary_buttons";if(bad==17)n.get(4).visible=false;
            if(bad==18)n.get(2).bottom=1400;if(bad==19)n.get(3).right=600;if(bad==20)n.get(4).bottom=800;if(bad==21)n.get(3).scrollable=true;
            if(bad==22)n.get(1).password=true;if(bad==23)n.get(1).editable=true;if(bad==24)n.get(1).className="ImagePreview";if(bad==25)n.get(1).text="保存图片";
            if(bad==26)n.get(4).id="other.owner:id/splash_interact_view";if(bad==27){n.add(new AdMatcher.Node(3,875,2441,1152,2582,"跳过 2",z.id));n.get(6).clickable=true;}
            if(bad==28)n.get(2).id="tv.danmaku.bili:id/splash_layout_fake";if(bad==29)n.get(4).parent=1;
            check(find(n)<0,"native contract rejects incomplete/ambiguous/unavailable/input/preview evidence="+bad);
        }
        check(NativeSplashSkip.find(report,1200,2670,false,"tv.danmaku.bili")<0,"startup deadline remains");
        check(NativeSplashSkip.find(fixture("com.openai.chatgpt","跳过 2"),1200,2670,true,"com.openai.chatgpt")<0,"pasted screenshot conversation cannot authorize action");
        check(NativeSplashSkip.find(fixture("com.weilaishanhai.oopz","跳过 2"),1200,2670,true,"com.weilaishanhai.oopz")<0,"WM/OOPZ visual-only policy remains");
        check(NativeSplashSkip.find(report,2670,1200,true,"tv.danmaku.bili")<0,"wrong orientation rejected");
        EventInbox inbox=new EventInbox();inbox.offer("tv.danmaku.bili",1,1000,1100,TreeReadGate.cue("跳过 2"));inbox.offer("tv.danmaku.bili",2,1001,1101,false);check(inbox.take().cue,"exact native skip signal survives a following ordinary event");
        BufferedImage im=lightFixture();BiliVisual.Result found=detect(im);check(found.found()&&found.requiresPair,"pale background with low-contrast double arrows and local numeric suffix "+found.reason);
        for(int[] box:new int[][]{{40,1460,140,70},{325,1200,70,65},{540,1475,33,45},{573,1475,33,45},{606,1475,28,45},{515,1455,140,29}}){BufferedImage missing=copy(im);erase(missing,box[0],box[1],box[2],box[3]);check(!detect(missing).found(),"mark/arrows/each word/digit/capsule feature remains mandatory="+java.util.Arrays.toString(box));}
        BufferedImage body=copy(im);erase(body,0,0,720,1100);check(detect(body).found(),"creative is not an image template");
        BufferedImage flat=copy(im);erase(flat,320,1180,80,100);check(!detect(flat).found(),"flat bright region does not replace two contour arrows");
        CanvasSkipVisual.Proof pair=new CanvasSkipVisual.Proof();check(!pair.confirm("tv.danmaku.bili",1,0,720,1602,found,1000,1050),"pale fallback is never a one-frame tap");check(pair.confirm("tv.danmaku.bili",1,0,720,1602,found,1350,1400),"fresh second frame retains original confirmation interval");
        System.out.println("PASS: "+checks+" native-splash/pale-frame checks; no phone latency or battery measurement");
    }
}
