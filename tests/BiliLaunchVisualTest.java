package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.ImageIO;

public final class BiliLaunchVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static int rgb(int n){return 0xff000000|n<<16|n<<8|n;}
    private static BiliVisual.Result detect(BufferedImage im){int w=im.getWidth(),h=im.getHeight();return BiliLaunchVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h);}
    private static BufferedImage copy(BufferedImage s){return scale(s,s.getWidth(),s.getHeight(),RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);}
    private static BufferedImage scale(BufferedImage src,int w,int h,Object interp){BufferedImage im=new BufferedImage(w,h,1);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,interp);g.drawImage(src,0,0,w,h,null);g.dispose();return im;}
    private static void erase(BufferedImage im,int x,int y,int w,int h){Graphics2D g=im.createGraphics();g.setColor(new Color(0x607080));g.fillRect(x,y,w,h);g.dispose();}
    private static BufferedImage fixture(int digit)throws Exception {
        int w=720,h=1602;BufferedImage im=new BufferedImage(w,h,1);erase(im,0,0,w,h);
        Graphics2D g=im.createGraphics();g.setColor(new Color(0x202028));g.fillRoundRect(515,1455,160,85,70,70);g.dispose();
        float[] word=SkipGlyphs.reference(0,40,20);
        for(int y=0;y<26;y++)for(int x=0;x<52;x++)im.setRGB(550+x,1484+y,rgb((int)word[y*20/26*40+x*40/52]));
        g=im.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Serif",Font.PLAIN,25));g.drawString(Integer.toString(digit),613,1508);
        g.setStroke(new BasicStroke(5));g.drawLine(338,1240,360,1219);g.drawLine(360,1219,382,1240);g.dispose();
        Field f=BiliLaunchVisual.class.getDeclaredField("MARK");f.setAccessible(true);long[] mark=(long[])f.get(null);
        for(int y=0;y<60;y++)for(int x=0;x<120;x++)if((mark[y*24/60]&(1L<<(x*48/120)))!=0)im.setRGB(47+x,1465+y,Color.WHITE.getRGB());
        return im;
    }
    public static void main(String[] args)throws Exception {
        ArrayList<AdMatcher.Node> nodes=new ArrayList<>();AdMatcher.Node root=new AdMatcher.Node(-1,0,0,1200,2670,"",null);nodes.add(root);
        check(!AdMatcher.biliVisualContext(nodes),"reported native tree has neither ad nor CTA marker");
        check(BiliLaunchVisual.context("tv.danmaku.bili",true,false,nodes),"same zero-marker tree can enter bounded startup probe");
        check(!BiliLaunchVisual.context("tv.danmaku.bili",false,false,nodes),"ordinary browsing cannot run blind launch probes");
        check(!BiliLaunchVisual.context("tv.danmaku.bili",true,true,nodes),"already attempted launch is not repeated");
        check(!BiliLaunchVisual.context("other.app",true,false,nodes),"brand-only fallback is not inherited by unrelated hosts");
        check(!BiliLaunchVisual.context("com.openai.chatgpt",true,false,nodes),"pasted picture conversation excluded");
        for(int bad=0;bad<4;bad++){
            root.editable=bad==0;root.password=bad==1;root.className=bad==2?"ImagePreview":"";root.text=bad==3?"保存图片":"";
            check(!BiliLaunchVisual.context("tv.danmaku.bili",true,false,nodes),"input/sensitive/gallery/save exclusions="+bad);
        }
        root.editable=root.password=false;root.className=root.text="";
        StartupProbe plan=new StartupProbe();plan.reset(1000);plan.window(1,1000);ArrayList<Long> times=new ArrayList<>();
        for(long t=1000;t<31000;t+=10)if(BiliLaunchVisual.context("tv.danmaku.bili",plan.active(t),false,nodes)&&plan.delay(t)==0){times.add(t-1000);plan.requested(t);}
        check(times.equals(Arrays.asList(0L,700L,2000L,5000L,11000L,22000L)),"ordinary content events cannot spend more than original sparse launch plan");
        check(plan.count()==6&&!plan.active(31000),"no new indefinite launch watcher");
        plan.reset(0);plan.window(1,0);for(long t:new long[]{0,700,2000,5000,11000,22000})plan.requested(t);
        for(int id=2;id<100;id++){plan.window(id,25000+(id-2)*10);if(plan.delay(25000+(id-2)*10)==0)plan.requested(25000+(id-2)*10);}
        check(plan.count()<=8,"window churn keeps maximum eight unclassified probes");
        check(!BiliLaunchVisual.detect(null,720,1602).found()&&!BiliLaunchVisual.detect(new int[5],720,1602).found(),"invalid pixels rejected");
        check(!BiliLaunchVisual.detect(new int[720*1602],720,1602).found(),"startup eligibility alone never provides target");
        for(int digit=0;digit<10;digit++)check(detect(fixture(digit)).found(),"synthetic countdown digit "+digit);
        BufferedImage im=fixture(2);
        for(int missing=0;missing<6;missing++){
            BufferedImage m=copy(im);
            if(missing==0)erase(m,40,1460,135,70);
            if(missing==1)erase(m,328,1210,65,40);
            if(missing==2)erase(m,550,1480,26,35);
            if(missing==3)erase(m,576,1480,27,35);
            if(missing==4){Graphics2D g=m.createGraphics();g.setColor(new Color(0x202028));g.fillRect(610,1478,25,35);g.dispose();}
            if(missing==5){for(int y=1450;y<1545;y++)for(int x=510;x<680;x++)if(m.getRGB(x,y)==0xff202028)m.setRGB(x,y,0xff607080);}
            check(!detect(m).found(),"remove mark/chevron/each skip glyph/countdown/capsule="+missing);
        }
        BufferedImage moved=copy(im);erase(moved,510,1450,180,100);Graphics2D g=moved.createGraphics();g.drawImage(im,200,800,360,885,515,1455,675,1540,null);g.dispose();
        check(!detect(moved).found(),"same lettering in creative body is not a footer target");
        SplashActions actions=new SplashActions();check(actions.allows(SplashActions.SKIP),"launch has one gesture budget");actions.mark(SplashActions.SKIP);
        check(!BiliLaunchVisual.context("tv.danmaku.bili",true,actions.finished(),nodes),"persistent ad does not create another launch gesture");
        nodes.add(new AdMatcher.Node(0,0,500,1200,2670,"广告",null));check(AdMatcher.biliVisualContext(nodes),"later bottom panel remains eligible after launch budget used");
        CanvasSkipVisual.Proof proof=new CanvasSkipVisual.Proof();BiliVisual.Result word=detect(im);
        check(word.requiresPair&&!proof.confirm("tv.danmaku.bili",1,0,720,1602,word,1000,1050),"first frame cannot authorize footer touch");
        check(!proof.confirm("tv.danmaku.bili",1,0,720,1602,word,1000,1090),"same screenshot is not independent evidence");
        check(proof.confirm("tv.danmaku.bili",1,0,720,1602,word,1350,1400),"new matching frame confirms");
        for(int changed=0;changed<7;changed++){
            proof.reset();proof.confirm("tv.danmaku.bili",1,0,720,1602,word,1000,1050);
            check(!proof.confirm(changed==0?"other.app":"tv.danmaku.bili",changed==1?2:1,changed==2?1:0,changed==3?1440:720,changed==4?3136:1602,
                word,changed==5?2400:1350,changed==6?2000:changed==5?2450:1400),"owner/window/display/source/gap/age change="+changed);
        }
        for(String path:args)images(path);
        System.out.println("PASS: "+checks+" Bili launch/footer/pacing checks");
    }
    private static void images(String path)throws Exception {
        boolean expected=!path.startsWith("!");if(!expected)path=path.substring(1);BufferedImage src=ImageIO.read(new File(path));
        for(Object interpolation:new Object[]{RenderingHints.VALUE_INTERPOLATION_BILINEAR,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR})
        for(int[] dim:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}){
            BufferedImage im=scale(src,dim[0],dim[1],interpolation);if(dim[0]>720)im=scale(im,720,Math.round(dim[1]*720f/dim[0]),RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            long begin=System.nanoTime();BiliVisual.Result r=detect(im);check(r.found()==expected,"actual image "+path+" "+Arrays.toString(dim)+" "+r.reason);
            if(expected){
                int w=im.getWidth(),h=im.getHeight();check(r.requiresPair&&r.x>w*.78&&r.x<w*.88&&r.y>h*.92&&r.y<h*.955,"live footer glyph geometry");
                for(int[] rect:new int[][]{{35,1170,105,53},{265,968,46,30},{443,1185,28,32},{471,1185,27,32},{499,1186,23,31}}){
                    BufferedImage m=copy(im);erase(m,rect[0]*w/575,rect[1]*h/1280,rect[2]*w/575+1,rect[3]*h/1280+1);check(!detect(m).found(),"each actual UI feature mandatory "+Arrays.toString(rect));
                }
                BufferedImage plain=copy(im);erase(plain,0,0,w,h*70/100);check(detect(plain).found(),"upper ad creative is not a reference");
                int[] pixels=im.getRGB(0,0,w,h,null,0,w);check(CanvasSkipVisual.detect(pixels,w,h,true).found(),"other apps with independent current ad evidence share footer family");
                check(!CanvasSkipVisual.detect(pixels,w,h,false).found(),"generic native-label-free case cannot borrow Bili branding proof");
            }
            System.out.println(new File(path).getName()+" source="+Arrays.toString(dim)+" analyze="+im.getWidth()+"x"+im.getHeight()+" found="+r.found()+" @"+r.x+","+r.y+" "+(System.nanoTime()-begin)/1000000+"ms");
        }
    }
}
