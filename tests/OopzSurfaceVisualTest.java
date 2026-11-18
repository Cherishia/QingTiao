package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public final class OopzSurfaceVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static BiliVisual.Result detect(BufferedImage im) {
        int w=im.getWidth(),h=im.getHeight();return OopzSurfaceVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h);
    }
    private static BufferedImage copy(BufferedImage im) {
        BufferedImage out=new BufferedImage(im.getWidth(),im.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.drawImage(im,0,0,null);g.dispose();return out;
    }
    private static void erase(BufferedImage im,int l,int t,int r,int b) {
        Graphics2D g=im.createGraphics();g.setColor(new Color(20,20,20));g.fillRect(l,t,r-l,b-t);g.dispose();
    }
    private static void actualWm(String path)throws Exception {
        BufferedImage source=ImageIO.read(new File(path));
        for(int[] size:new int[][]{{588,1280},{720,1568},{1440,3136}}) {
            int w=size[0],h=size[1];BufferedImage im=new BufferedImage(w,h,1);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,w,h,null);g.dispose();
            long begin=System.nanoTime();BiliVisual.Result target=detect(im);
            System.out.println("handwriting "+w+"x"+h+" "+target.found()+" @"+target.x+","+target.y+" "+target.reason+" "+(System.nanoTime()-begin)/1000000+"ms");
            check(target.found(),"handwriting with missing SDK is recognized at source, runtime analysis and display sizes");
            check(target.x>w*.89&&target.x<w*.96&&target.y>h*.14&&target.y<h*.18,"current lower WM word is located");
            WmSplashVisual.Hint hint=new WmSplashVisual.Hint(Math.round(w*981/1200f),Math.round(h*389/2670f),Math.round(w*1170/1200f),Math.round(h*458/2670f),w,h);
            WmSplashVisual.Detection sdk=WmSplashVisual.analyze(im.getRGB(0,0,w,h,null,0,w),w,h,hint);
            check(sdk.target.found()&&sdk.requiresPair,"SDK handwriting requires independent two-frame proof");
            for(int[] area:new int[][]{{488,188,503,220},{506,188,515,220},{517,188,539,220},{539,188,565,220}}) {
                BufferedImage missing=copy(im);erase(missing,Math.round(area[0]*w/588f),Math.round(area[1]*h/1280f),Math.round(area[2]*w/588f),Math.round(area[3]*h/1280f));
                check(!detect(missing).found(),"independent prefix, separator and each character remain necessary");
            }
            BufferedImage faint=copy(im);
            for(int y=Math.round(188*h/1280f);y<Math.round(220*h/1280f);y++)for(int x=Math.round(506*w/588f);x<Math.round(515*w/588f);x++) {
                int c=faint.getRGB(x,y),r=c>>16&255,green=c>>8&255,b=c&255;faint.setRGB(x,y,new Color((int)(r*.65),(int)(green*.65),(int)(b*.65)).getRGB());
            }
            BiliVisual.Result dim=detect(faint);System.out.println("faint: "+dim.reason);check(dim.found(),"a faint separator can propose a candidate but still needs both complete glyphs");
            BufferedImage body=copy(im);erase(body,0,h/4,w,h);check(detect(body).found(),"creative content is not a template");
            int l=Math.round(482*w/588f),t=Math.round(184*h/1280f),r=Math.round(570*w/588f),b=Math.round(224*h/1280f),dy=Math.round(h*.025f);
            BufferedImage badge=copy(im.getSubimage(l,t,r-l,b-t)),moved=copy(im);erase(moved,l,t,r,b);g=moved.createGraphics();g.drawImage(badge,l,t+dy,null);g.dispose();
            BiliVisual.Result live=detect(moved);check(live.found()&&live.y>target.y+dy*.8,"drawn control is located from current pixels when inset changes");
        }
    }
    public static void main(String[] args)throws Exception {
        String pkg="com.weilaishanhai.oopz";ArrayList<AdMatcher.Node> nodes=new ArrayList<>();
        for(int i=0;i<53;i++)nodes.add(new AdMatcher.Node(i-1,0,0,1440,3136,"",""));
        check(OopzSurfaceVisual.context(pkg,true,nodes),"reported owner and blank tree can request observation without SDK IDs");
        check(!OopzSurfaceVisual.context("other.app",true,nodes),"no expansion to unknown owners");
        check(!OopzSurfaceVisual.context(pkg,false,nodes),"outside startup rejected");
        check(!OopzSurfaceVisual.context(pkg,true,new ArrayList<AdMatcher.Node>()),"empty owner tree rejected");
        nodes.get(0).password=true;check(!OopzSurfaceVisual.context(pkg,true,nodes),"sensitive page rejected");nodes.get(0).password=false;
        check(!OopzSurfaceVisual.detect(null,720,1568).found(),"null frame rejected");
        check(!OopzSurfaceVisual.detect(new int[10],720,1568).found(),"short frame rejected");
        check(!OopzSurfaceVisual.detect(new int[720*1568],720,1568).found(),"blank tree and frame do not imply permission to touch");
        OopzSurfaceVisual.Proof proof=new OopzSurfaceVisual.Proof();BiliVisual.Result word=new BiliVisual.Result(660,115,24,"test");
        check(!proof.confirm(pkg,1,0,720,1568,word,1000,1040),"first complete match never authorizes a click");
        check(!proof.confirm(pkg,1,0,720,1568,word,1000,1080),"same screenshot never counts as a second frame");
        check(proof.confirm(pkg,1,0,720,1568,word,1450,1490),"two independent current frames with stable geometry qualify");
        for(int mode=0;mode<9;mode++) {
            proof.reset();proof.confirm(pkg,1,0,720,1568,word,1000,1040);
            String owner=mode==0?"other.app":pkg;int win=mode==1?2:1,display=mode==2?1:0,w=mode==3?721:720,h=mode==4?1569:1568;
            BiliVisual.Result target=mode==5?new BiliVisual.Result(650,115,24,"moved"):mode==6?new BiliVisual.Result(660,115,35,"resized"):word;
            long stamp=mode==7?2300:1450,now=mode==8?2100:stamp+40;
            check(!proof.confirm(owner,win,display,w,h,target,stamp,now),"changed owner/window/display/size/target or stale proof rejected: "+mode);
        }
        proof.reset();proof.confirm(pkg,1,0,720,1568,word,1000,1040);
        check(!proof.confirm(pkg,1,0,720,1568,new BiliVisual.Result(0,0,0,"missing"),1450,1490),"missing target breaks consecutive proof");
        check(!proof.confirm(pkg,1,0,720,1568,word,1900,1940),"target returning after absence needs a new pair");
        for(String arg:args) {
            if(arg.startsWith("wm:")){actualWm(arg.substring(3));continue;}
            boolean expected=!arg.startsWith("!");String path=expected?arg:arg.substring(1);BufferedImage source=ImageIO.read(new File(path));
            for(int[] size:new int[][]{{source.getWidth(),source.getHeight()},{720,1568},{1440,3136}}) {
                int w=size[0],h=size[1];BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,w,h,null);g.dispose();
                long start=System.nanoTime();BiliVisual.Result r=detect(im);
                System.out.println(new File(path).getName()+" "+w+"x"+h+" "+r.found()+" @"+r.x+","+r.y+" "+r.reason+" "+((System.nanoTime()-start)/1000000)+"ms");
                check(r.found()==expected,"real screenshot classification");if(!expected)continue;
                check(r.x>w*.88&&r.x<w*.97&&r.y>h*.05&&r.y<h*.10,"current upper target is found without old SDK coordinates");
                check(WmSplashVisual.touchX(r,false)>r.x&&WmSplashVisual.touchX(r,false)<r.x+r.size,"touch is inside the final glyph");
                for(int digit=0;digit<=9;digit++) {
                    BufferedImage countdown=copy(im);erase(countdown,Math.round(w*1068/1280f),Math.round(h*180/2787f),Math.round(w*1098/1280f),Math.round(h*236/2787f));
                    g=countdown.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round(w*30/1280f)));
                    g.drawString(Integer.toString(digit),Math.round(w*1073/1280f),Math.round(h*222/2787f));g.dispose();
                    check(detect(countdown).found(),"a changing countdown digit is not bound to the screenshot's five");
                }
                for(int part=0;part<2;part++) {
                    BufferedImage erased=copy(im);erase(erased,r.x-r.size+part*r.size,r.y-r.size/2,r.x+part*r.size,r.y-r.size/2+r.size);
                    check(!detect(erased).found(),"a single remaining Chinese character cannot authorize a touch");
                }
                for(int[] rect:new int[][]{{1104,180,1117,236},{1068,180,1098,236}}) {
                    BufferedImage erased=copy(im);erase(erased,Math.round(w*rect[0]/1280f),Math.round(h*rect[1]/2787f),Math.round(w*rect[2]/1280f),Math.round(h*rect[3]/2787f));
                    check(!detect(erased).found(),"separator and independent countdown prefix are both mandatory");
                }
                BufferedImage body=copy(im);erase(body,0,h/4,w,h);check(detect(body).found(),"ad body and promotion link are irrelevant");
                int l=Math.round(w*1068/1280f),t=Math.round(h*180/2787f),right=Math.round(w*1224/1280f),bottom=Math.round(h*237/2787f);
                BufferedImage badge=copy(im.getSubimage(l,t,right-l,bottom-t)),moved=copy(im);erase(moved,l,t,right,bottom);
                int dy=Math.round(h*.06f);g=moved.createGraphics();g.drawImage(badge,l,t+dy,null);g.dispose();BiliVisual.Result live=detect(moved);
                check(live.found()&&live.y>r.y+dy*.8,"follow the current button when top inset changes");
                BufferedImage duplicate=copy(im);g=duplicate.createGraphics();g.drawImage(badge,l-Math.round(w*.10f),t,null);g.dispose();
                check(!detect(duplicate).found(),"two distinct matching controls are ambiguous");
            }
        }
        System.out.println("PASS: "+checks+" OOPZ surface/proof checks");
    }
}
