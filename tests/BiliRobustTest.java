package dev.local.qingtiao;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;

/** Vary real inputs and independently remove every mandatory affordance. No device claims. */
public final class BiliRobustTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static int rgb(int n){return 0xff000000|n<<16|n<<8|n;}
    private static int[] fixture(int shape)throws Exception {
        Method m=BiliVisualTest.class.getDeclaredMethod("fixture",int.class,int.class,int.class,boolean.class,boolean.class,boolean.class,double.class,double.class);
        m.setAccessible(true);return (int[])m.invoke(null,720,1602,shape,true,true,true,.94,.343);
    }
    private static List<AdMatcher.Node> nodes(int w,int h){
        List<AdMatcher.Node> n=new ArrayList<>();n.add(new AdMatcher.Node(-1,0,0,w,h,"",""));
        n.add(new AdMatcher.Node(0,0,(int)(h*.303),w,h,"",""));
        n.add(new AdMatcher.Node(1,20,(int)(h*.69),70,(int)(h*.70),"广告",""));return n;
    }
    private static AdMatcher.Node box(int parent,int w,int h,double l,double t,double r,double b,String text){
        return new AdMatcher.Node(parent,(int)(l*w),(int)(t*h),(int)(r*w),(int)(b*h),text,"");
    }
    private static List<AdMatcher.Node> pauseCard(int w,int h){
        List<AdMatcher.Node> n=new ArrayList<>();
        n.add(box(-1,w,h,0,0,1,1,""));
        n.add(box(0,w,h,.08,.19,.92,.48,""));
        AdMatcher.Node close=box(1,w,h,.82,.205,.89,.235,"");close.desc="关闭暂停广告";close.clickable=true;n.add(close);
        n.add(box(1,w,h,.67,.41,.74,.44,"广告"));return n;
    }
    private static int[] shades(int[] p,int glyph,int bar) {
        int[] out=p.clone();for(int i=0;i<out.length;i++){
            if(out[i]==0xffa9abad)out[i]=rgb(glyph);
            if(out[i]==0xff606367)out[i]=rgb(bar);
        }return out;
    }
    public static void main(String[] args)throws Exception {
        int w=720,h=1602;int[] base=fixture(0);
        for(int glyph:new int[]{48,80,169,235,255})for(int bar:new int[]{48,90,149,151,175,225,255}){
            BiliVisual.Result r=BiliVisual.detect(shades(base,glyph,bar),w,h);
            check(r.found()&&Math.abs(r.x-w*.94)<3&&Math.abs(r.y-h*.343)<3,"neutral contrast variant "+glyph+"/"+bar);
            if(glyph<55||glyph>218||bar>150||bar<55)check(r.requiresPair,"extended match must require fresh pair");
        }
        for(int shape=1;shape<=5;shape++)for(int shade:new int[]{48,169,255})
            check(!BiliVisual.detect(shades(fixture(shape),shade,255),w,h).found(),"non-X stays rejected after contrast fallback");
        for(int missing=0;missing<4;missing++){
            int[] p=shades(base,48,255);
            for(int y=0;y<h;y++)for(int x=0;x<w;x++){
                boolean erase=missing==0&&x>w*.90&&y>h*.32&&y<h*.36
                    ||missing==1&&x>w*.80&&x<w*.86&&y>h*.32&&y<h*.36
                    ||missing==2&&x>w*.40&&x<w*.60&&y>h*.29&&y<h*.33;
                if(erase)p[y*w+x]=0xff17181a;
                if(missing==3&&p[y*w+x]==0xff17181a)p[y*w+x]=0xffeeeeee;
            }
            check(!BiliVisual.detect(p,w,h).found(),"missing mandatory X/menu/handle/header="+missing);
        }
        check(!BiliVisual.detect(shades(base,35,255),w,h).found(),"insufficient X contrast rejected");
        check(!BiliVisual.detect(shades(base,255,35),w,h).found(),"insufficient handle contrast rejected");
        int[] colored=base.clone();for(int i=0;i<colored.length;i++)if(colored[i]==0xffa9abad)colored[i]=0xffff4000;
        check(!BiliVisual.detect(colored,w,h).found(),"colored decorative X rejected");
        String why=BiliVisual.detect(new int[w*h],w,h).reason;
        check(why.contains("X=")&&why.contains("三点=")&&why.contains("拖动条="),"failure identifies each stage");
        List<AdMatcher.Node> n=nodes(w,h);BiliPanel.Hint hint=BiliPanel.find(n,w,h);
        check(hint!=null,"current native ad ancestor identifies bottom sheet");
        check(BiliPanel.canProbe(n,w,h),"localized ad badge can wake current sheet before a CTA loads");
        BiliPanel.Inspection first=BiliPanel.inspect(n,w,h,true), nativeOnly=BiliPanel.inspect(n,w,h,false);
        check(BiliPanel.same(first.hint,hint)&&first.nativeEvidence&&!first.pixelLabelContext,"single discovery preserves prior scope and visual gating");
        check(BiliPanel.same(nativeOnly.hint,hint)&&!nativeOnly.nativeEvidence&&!nativeOnly.pixelLabelContext,"visual setting off preserves native scope");
        check(BiliPanel.confirmTarget(n,w,h,hint,(int)(w*.94),(int)(h*.343)),"live sheet header covers the actual X");
        check(!BiliPanel.confirmTarget(n,w,h,hint,(int)(w*.94),(int)(h*.46)),"X lower than verified sheet header denied by final recheck");
        check(BiliVisual.detect(base,w,h,hint).found(),"native scope still needs actual pictured X");
        check(!BiliVisual.detect(base,w,h,hint).requiresPair&&BiliVisual.detect(base,w,h).requiresPair,"known scoped legacy X is immediate; flat fallback needs two fresh frames");
        check(!BiliVisual.detect(new int[w*h],w,h,hint).found(),"scope alone cannot invent close coordinate");
        check(BiliPanel.same(hint,BiliPanel.find(nodes(w,h),w,h)),"scope survives snapshot node identity changes");
        for(int kind=0;kind<7;kind++){
            n=nodes(w,h);
            if(kind==0)n.get(2).visible=false;
            if(kind==1)n.get(2).text="普通内容";
            if(kind==2)n.get(2).parent=0;
            if(kind==3)n.get(1).top=0;
            if(kind==4)n.get(1).bottom=h/2;
            if(kind==5)n.get(1).editable=true;
            if(kind==6)n.get(2).top=0;
            check(BiliPanel.find(n,w,h)==null,"unqualified/missing/stale native scope="+kind);
        }
        check(!BiliPanel.same(hint,BiliPanel.find(nodes(1200,2670),1200,2670)),"source display size change invalidates scope");
        n=nodes(w,h);n.add(new AdMatcher.Node(0,0,h/2,w,h,"",""));n.add(new AdMatcher.Node(3,0,h*3/4,50,h*3/4+20,"广告",""));
        check(BiliPanel.find(n,w,h)==null,"conflicting current ad panels stay ambiguous");
        n.add(box(3,w,h,.16,.88,.83,.94,"前往观看"));
        check(!BiliPanel.canProbe(n,w,h)&&!BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"conflicting scopes never fall back to broad CTA evidence");
        BiliPanel.Inspection conflicting=BiliPanel.inspect(n,w,h,true);
        check(conflicting.hint==null&&!conflicting.nativeEvidence&&!conflicting.pixelLabelContext,
            "combined discovery cannot reopen an ambiguous scope");
        n=nodes(w,h);n.add(box(1,w,h,.03,.331,.15,.359,"评论"));
        n.add(box(1,w,h,.16,.90,.82,.95,"前往观看"));
        check(BiliPanel.find(n,w,h)==null&&!BiliPanel.canProbe(n,w,h),"comment sheet with an ad recommendation cannot trigger a panel screenshot");
        n=nodes(w,h);n.get(1).top=(int)(h*.66);n.get(2).top=(int)(h*.77);n.get(2).bottom=(int)(h*.785);
        BiliPanel.Hint low=BiliPanel.find(n,w,h);
        check(low!=null&&!BiliVisual.detect(base,w,h,low).found(),"valid live panel scope avoids matching the unrelated legacy-height X");
        Method f=BiliVisualTest.class.getDeclaredMethod("fixture",int.class,int.class,int.class,boolean.class,boolean.class,boolean.class,double.class,double.class);f.setAccessible(true);
        int[] lowFrame=(int[])f.invoke(null,w,h,0,true,true,true,.94,.693);
        check(BiliVisual.detect(lowFrame,w,h,low).found()&&!BiliVisual.detect(lowFrame,w,h).found(),"compact low sheet is searched only with a current native ad scope");
        check(BiliPanel.confirmTarget(n,w,h,low,(int)(w*.94),(int)(h*.693)),"compact detected X has the same fresh scope");
        n.get(2).visible=false;
        check(!BiliPanel.confirmTarget(n,w,h,low,(int)(w*.94),(int)(h*.693)),"disappeared ad scope cannot authorize a previously matched X");

        n=new ArrayList<>();n.add(box(-1,w,h,0,0,1,1,""));n.add(box(0,w,h,.03,.69,.09,.71,"广告"));
        check(!BiliPanel.canProbe(n,w,h),"flat feed with only a single ad mark must stay idle");
        n.add(box(0,w,h,.15,.91,.85,.96,"前往观看"));
        check(BiliPanel.find(n,w,h)==null&&BiliPanel.canProbe(n,w,h),"flat ad mark + exact watch CTA can request bounded discovery only");
        check(BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"flat independent evidence is below the actual X");
        n.get(2).top=(int)(h*.65);n.get(2).bottom=(int)(h*.70);
        check(!BiliPanel.canProbe(n,w,h),"flat generic body watch text cannot stand in for a dedicated lower CTA");
        n.get(2).top=(int)(h*.91);n.get(2).bottom=(int)(h*.96);
        n.get(1).top=(int)(h*.21);n.get(1).bottom=(int)(h*.23);
        check(!BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"ad from the player or unrelated region above X cannot confirm the target");
        n.get(1).top=(int)(h*.69);n.get(1).bottom=(int)(h*.71);n.add(box(0,w,h,.04,.329,.18,.355,"全部评论"));
        check(!BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"nearby normal sheet title denies flat evidence too");
        n.remove(n.size()-1);n.get(2).text="猜你喜欢";
        check(!BiliPanel.canProbe(n,w,h),"ad mark plus recommendation label is not a watch CTA");
        n.get(2).text="前往观看";n.get(1).visible=false;
        check(!BiliPanel.canProbe(n,w,h),"CTA alone is not ad evidence");

        // The 11:40 video card places its watch action inside the preview, before the label.
        n=new ArrayList<>();n.add(box(-1,w,h,0,0,1,1,""));
        AdMatcher.Node inline=box(0,w,h,.21,.641,.39,.666,"前往观看");n.add(inline);
        AdMatcher.Node disclosure=box(0,w,h,.033,.683,.078,.698,"广告");n.add(disclosure);
        check(BiliPanel.find(n,w,h)==null&&BiliPanel.canProbe(n,w,h),"inline watch action and disclosure can start bounded discovery in a flat tree");
        check(BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"inline video ad can confirm only the current visual X");
        check(!BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.64)),"unrelated lower X cannot borrow the inline evidence");
        n.add(box(0,w,h,.03,.33,.20,.37,"全部评论"));
        check(!BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"comment sheet title rules out inline ads as a close trigger");
        n.remove(n.size()-1);inline.bottom=(int)(h*.51);inline.top=(int)(h*.49);
        check(!BiliPanel.canProbe(n,w,h)&&BiliPanel.pixelLabelContext(n,w,h),
            "unlocated label pair can only start a bounded probe that requires the separate pixel disclosure");
        inline.top=(int)(h*.64);inline.bottom=(int)(h*.666);disclosure.text="相关视频";
        check(!BiliPanel.canProbe(n,w,h)&&!BiliPanel.pixelLabelContext(n,w,h),"inline watch text alone cannot start detection");

        // The real KFC pause card exposes an ad marker and a bare right-header touch area,
        // while its CTA and X semantics are drawn only in pixels.
        n=new ArrayList<>();n.add(box(-1,w,h,0,0,1,1,""));
        AdMatcher.Node header=box(0,w,h,.63,.307,.97,.342,"");n.add(header);
        AdMatcher.Node bareClose=box(1,w,h,.858,.307,.969,.342,"");bareClose.clickable=true;bareClose.className="android.view.View";n.add(bareClose);
        n.add(box(0,w,h,.03,.69,.09,.71,"广告"));
        check(BiliPanel.canProbe(n,w,h),"ad badge plus one bounded bare right-header control can request a visual pause-card check");
        check(BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"pixel-confirmed X may use the matching bare right-header control");
        bareClose.id="tv.danmaku.bili:id/share_icon";
        check(!BiliPanel.canProbe(n,w,h)&&!BiliPanel.confirmTarget(n,w,h,null,(int)(w*.94),(int)(h*.343)),"share control cannot become a pause-ad close fallback");
        bareClose.id="";n.get(3).visible=false;
        check(!BiliPanel.canProbe(n,w,h),"bare header control without the current ad marker stays idle");

        n=pauseCard(w,h);
        check(BiliPanel.nativePauseClose(n,w,h)==2&&BiliPanel.currentNativePauseControl(n.get(2)),"explicit close in bounded pause ad card is immediately actionable");
        AdMatcher.Node close=box(1,w,h,.91,.32,.96,.35,"关闭");close.clickable=true;close.desc="按钮";
        check(BiliPanel.currentScopedCloseControl(close),"a close label with a neutral role can be used in a verified header");
        close.desc="前往观看";check(!BiliPanel.currentScopedCloseControl(close),"conflicting watch action rejects a generic close label");
        close.desc="关闭";close.text="更多";check(!BiliPanel.currentScopedCloseControl(close),"menu semantics reject a conflicting close description");
        close.text="关闭";close.enabled=false;check(!BiliPanel.currentScopedCloseControl(close),"disabled controls cannot be used");
        for(int mode=0;mode<9;mode++){
            n=pauseCard(w,h);
            if(mode==0)n.get(2).desc="X";
            if(mode==1)n.get(3).parent=0;
            if(mode==2)n.get(3).visible=false;
            if(mode==3)n.get(2).enabled=false;
            if(mode==4)n.get(1).scrollable=true;
            if(mode==5)n.get(2).left=(int)(w*.13);
            if(mode==6){AdMatcher.Node other=box(1,w,h,.81,.25,.89,.28,"关闭广告");other.clickable=true;n.add(other);}
            if(mode==7)n.add(box(1,w,h,.15,.22,.35,.25,"播放设置"));
            if(mode==8)n.get(2).text="去看看";
            check(BiliPanel.nativePauseClose(n,w,h)<0,"native pause rejects generic/foreign/ambiguous menu and actions, case="+mode);
        }
        CanvasSkipVisual.Proof proof=new CanvasSkipVisual.Proof();BiliVisual.Result r=BiliVisual.detect(shades(base,48,255),w,h);
        check(!proof.confirm("tv.danmaku.bili",1,0,w,h,r,1000,1040),"contrast target waits for second independent frame");
        check(!proof.confirm("tv.danmaku.bili",1,0,w,h,r,1000,1050),"same screenshot is not second evidence");
        check(proof.confirm("tv.danmaku.bili",1,0,w,h,r,1350,1390),"fresh same-window frame confirms current X");
        check(!proof.confirm("tv.danmaku.bili",2,0,w,h,r,1700,1740),"window change cancels contrast pair");
        List<AdMatcher.Node> playing=new ArrayList<>();
        playing.add(new AdMatcher.Node(-1,0,0,1200,2670,"",""));
        playing.add(new AdMatcher.Node(0,42,1758,90,1790,"广告",""));
        playing.add(new AdMatcher.Node(0,504,2482,696,2547,"前往观看",""));
        playing.add(new AdMatcher.Node(0,587,1662,755,1718,"",""));playing.get(3).desc="前往观看";
        playing.add(new AdMatcher.Node(0,32,821,180,911,"简介",""));
        for(int i=1;i<4;i++)playing.get(i).enabled=false;
        check(BiliPanel.ordinaryHeader(playing,0,796,1200,1036,1200),"nearby underlying player tab can trip the original broad title guard");
        check(BiliPanel.drawnTargetProblem(playing,1200,2670,1128,916,66,1774).isEmpty(),
            "reported disclosure and independent inline/footer CTA verify the close target even when their labels are not enabled");
        check(!BiliPanel.drawnTargetProblem(playing,1200,2670,1128,916,66,1920).isEmpty(),
            "unrelated visual disclosure cannot borrow these native labels");
        playing.get(2).visible=false;
        check(!BiliPanel.drawnTargetProblem(playing,1200,2670,1128,916,66,1774).isEmpty(),
            "one native CTA alone cannot override the ambiguous title");
        playing.get(2).visible=true;
        playing.add(new AdMatcher.Node(0,40,897,180,937,"全部评论",""));
        check(!BiliPanel.drawnTargetProblem(playing,1200,2670,1128,916,66,1774).isEmpty(),
            "an actual ordinary title aligned with the X still blocks even with ad evidence underneath");
        BiliVisual.Result firstPanel=new BiliVisual.Result(676,550,16,"",true,41,1064);
        BiliVisual.Result movedLabel=new BiliVisual.Result(676,550,16,"",true,41,1091);
        proof.reset();
        check(!proof.confirm("dev.example.player",5,0,w,h,firstPanel,1000,1030),"first rendered panel frame only records a candidate");
        check(!proof.confirm("dev.example.player",5,0,w,h,movedLabel,1350,1380),"same X with moved ad label requires a new pair");
        check(proof.confirm("dev.example.player",5,0,w,h,movedLabel,1700,1730),"fresh consistent label and X confirm in a generic app");
        for(String path:args)image(path);
        System.out.println("PASS: "+checks+" Bili contrast/scope checks");
    }
    private static BufferedImage scale(BufferedImage src,int w,int h,Object interpolation){
        BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,interpolation);g.drawImage(src,0,0,w,h,null);g.dispose();return im;
    }
    private static void image(String path)throws Exception {
        boolean expected=!path.startsWith("!");if(!expected)path=path.substring(1);
        BufferedImage src=ImageIO.read(new File(path));
        for(Object interpolation:new Object[]{RenderingHints.VALUE_INTERPOLATION_BILINEAR,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR})
        for(int[] dim:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}){
            BufferedImage im=scale(src,dim[0],dim[1],interpolation);
            if(dim[0]>720)im=scale(im,720,Math.round(dim[1]*720f/dim[0]),RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w=im.getWidth(),h=im.getHeight();int[] p=im.getRGB(0,0,w,h,null,0,w);
            long started=System.nanoTime();BiliVisual.Result result=BiliVisual.detect(p,w,h);
            check(result.found()==expected,"image scale/resample "+path+" "+Arrays.toString(dim)+" "+result.reason);
            if(expected){
                check(Math.abs(result.x/(double)w-.94)<.025&&Math.abs(result.y/(double)h-.343)<.025,"actual close geometry retained");
                BiliVisual.Result scoped=BiliVisual.detect(p,w,h,BiliPanel.find(nodes(w,h),w,h));
                check(scoped.found()&&Math.abs(scoped.x-result.x)<3&&Math.abs(scoped.y-result.y)<3,"scope equals full actual target");
            }
            System.out.println(new File(path).getName()+" source="+Arrays.toString(dim)+" analyze="+w+"x"+h+" target="+result.found()+" @"+result.x+","+result.y+" "+(System.nanoTime()-started)/1000000+"ms");
        }
    }
}
