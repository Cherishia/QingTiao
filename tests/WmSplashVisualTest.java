package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public final class WmSplashVisualTest {
    private static int checks;
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static WmSplashVisual.Hint scaledHint(int w,int h) {
        return new WmSplashVisual.Hint(Math.round(w*981/1200f),Math.round(h*389/2670f),Math.round(w*1170/1200f),Math.round(h*458/2670f),w,h);
    }
    private static BiliVisual.Result detect(BufferedImage im,WmSplashVisual.Hint hint) {
        int w=im.getWidth(),h=im.getHeight();return WmSplashVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h,hint);
    }
    private static BufferedImage copy(BufferedImage im) {
        BufferedImage next=new BufferedImage(im.getWidth(),im.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=next.createGraphics();g.drawImage(im,0,0,null);g.dispose();return next;
    }
    public static void main(String[] args)throws Exception {
        String pkg="com.weilaishanhai.oopz";ArrayList<AdMatcher.Node> nodes=new ArrayList<>();
        AdMatcher.Node n=new AdMatcher.Node(0,981,389,1170,458,"5 | 跳过",WmSplashVisual.ID);n.clickable=true;nodes.add(n);
        WmSplashVisual.Hint hint=WmSplashVisual.hint(pkg,nodes,1200,2670,true);
        check(hint!=null,"exact observed package, SDK control and countdown label qualify");
        check(WmSplashVisual.hint("other.app",nodes,1200,2670,true)!=null,"same complete SDK ID and control proof can be reused in another application");
        check(WmSplashVisual.hint("",nodes,1200,2670,true)==null,"unknown owner rejected");
        check(WmSplashVisual.hasControl(nodes),"recognized SDK control selects visual-only handling");
        check(WmSplashVisual.hint(pkg,nodes,1200,2670,false)==null,"no activation outside startup");
        n.text="点击按钮了解更多";check(WmSplashVisual.hint(pkg,nodes,1200,2670,true)==null,"promotion wording is never a skip hint");n.text="5 | 跳过";
        n.id="wrong:id/wm_splash_jump_tv";check(WmSplashVisual.hint(pkg,nodes,1200,2670,true)==null,"ID suffix alone is insufficient");check(!WmSplashVisual.hasControl(nodes),"other SDK namespace cannot activate shared WM handling");n.id=WmSplashVisual.ID;
        n.top=2000;n.bottom=2069;check(WmSplashVisual.hint(pkg,nodes,1200,2670,true)==null,"bottom landing control is rejected");n.top=389;n.bottom=458;
        n.password=true;check(WmSplashVisual.hint(pkg,nodes,1200,2670,true)==null,"sensitive page rejected");n.password=false;
        n.text="1 | 跳过";check(WmSplashVisual.same(hint,WmSplashVisual.hint(pkg,nodes,1200,2670,true)),"countdown changes do not change physical control identity");
        n.left=988;check(WmSplashVisual.same(hint,WmSplashVisual.hint(pkg,nodes,1200,2670,true)),"narrower digit changes only left edge while skip word stays right aligned");n.left=981;
        n.left-=50;check(!WmSplashVisual.same(hint,WmSplashVisual.hint(pkg,nodes,1200,2670,true)),"moved SDK bounds cannot authorize old screenshot target");n.left+=50;
        AdMatcher.Node other=new AdMatcher.Node(0,981,480,1170,549,"2 | 跳过",WmSplashVisual.ID);other.clickable=true;nodes.add(other);
        check(WmSplashVisual.hint(pkg,nodes,1200,2670,true)==null,"two separate SDK controls are ambiguous");nodes.remove(other);
        check(!WmSplashVisual.detect(null,720,1602,hint).found(),"null image rejected");
        check(!WmSplashVisual.detect(new int[10],720,1602,hint).found(),"short buffer rejected");
        check(!WmSplashVisual.detect(new int[720*1602],720,1602,null).found(),"no accessible SDK proof means no screenshot click");
        BufferedImage blank=new BufferedImage(720,1602,BufferedImage.TYPE_INT_RGB);
        check(!detect(blank,scaledHint(720,1602)).found(),"SDK hint without visible skip glyphs cannot trigger a touch");
        for(int mode=0;mode<3;mode++) {
            BufferedImage texture=new BufferedImage(720,1602,BufferedImage.TYPE_INT_RGB);WmSplashVisual.Hint s=scaledHint(720,1602);
            for(int y=s.top;y<s.bottom;y++)for(int x=s.left;x<s.right;x++) {
                int value=mode==0?((x+y)%31<15?230:70):mode==1?((x*17+y*31)%256):Math.min(255,40+(x-s.left)*2+(y-s.top)*3);
                texture.setRGB(x,y,new Color(value,value,value).getRGB());
            }
            check(!detect(texture,s).found(),"diagonal edges, noise and gradients without two glyphs never authorize a touch");
        }
        BiliVisual.Result glyph=new BiliVisual.Result(1100,425,38,"verified word");
        check(WmSplashVisual.sdkTouchX(pkg,glyph,false)>glyph.x-glyph.size&&WmSplashVisual.sdkTouchX(pkg,glyph,false)<glyph.x,"OOPZ first point inside first glyph, away from countdown and word gap");
        check(WmSplashVisual.sdkTouchX(pkg,glyph,true)>glyph.x&&WmSplashVisual.sdkTouchX(pkg,glyph,true)<glyph.x+glyph.size,"OOPZ retry inside different reverified final glyph");
        check(WmSplashVisual.sdkTouchDuration(pkg,false)==80&&WmSplashVisual.sdkTouchDuration(pkg,true)==100,"OOPZ uses short touch rather than lengthening failed press");
        check(WmSplashVisual.sdkTouchX("other.app",glyph,false)==WmSplashVisual.sdkTouchX(pkg,glyph,false)&&WmSplashVisual.sdkTouchDuration("other.app",true)==100,"same verified SDK layout shares touch strategy across owners");
        check(WmSplashVisual.sdkTouchX(pkg,null,false)==0&&WmSplashVisual.sdkTouchX(pkg,new BiliVisual.Result(0,0,0,"missing"),true)==0,"no verified glyph provides no touch point");
        for(String arg:args) {
            boolean positive=!arg.startsWith("!");String path=positive?arg:arg.substring(1);BufferedImage source=ImageIO.read(new File(path));
            int[][] sizes={{source.getWidth(),source.getHeight()},{720,Math.round(source.getHeight()*720f/source.getWidth())},{1080,Math.round(source.getHeight()*1080f/source.getWidth())},{1200,Math.round(source.getHeight()*1200f/source.getWidth())},{720,1602},{1080,2400},{1200,2670}};
            for(int[] size:sizes) {
                int sw=size[0],sh=size[1];BufferedImage im=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,sw,sh,null);g.dispose();
                WmSplashVisual.Hint s=scaledHint(sw,sh);long begin=System.nanoTime();BiliVisual.Result result=detect(im,s);
                System.out.println(new File(path).getName()+" "+sw+"x"+sh+": "+result.found()+" @ "+result.x+","+result.y+" "+result.reason+" "+((System.nanoTime()-begin)/1000000)+"ms");
                check(result.found()==positive,"actual screenshot classification");if(!positive)continue;
                check(WmSplashVisual.analyze(im.getRGB(0,0,sw,sh,null,0,sw),sw,sh,s).requiresPair,"every initial WM font path requires independent frames");
                check(result.x>s.left+(s.right-s.left)/2&&result.x<s.right&&result.y>s.top&&result.y<s.bottom,"touch the skip word, away from countdown and landing CTA");
                int first=WmSplashVisual.touchX(result,false),retry=WmSplashVisual.touchX(result,true);
                check(first>result.x&&first<result.x+result.size&&first<s.right,"first touch lies inside final glyph rather than the word gap");
                check(retry>result.x&&retry<result.x+result.size&&retry<s.right&&retry!=first,"retry remains inside the same final glyph with a distinct interior point");
                int sdkFirst=WmSplashVisual.sdkTouchX(pkg,result,false),sdkRetry=WmSplashVisual.sdkTouchX(pkg,result,true);
                check(sdkFirst>result.x-result.size&&sdkFirst<result.x&&sdkFirst>s.left&&sdkFirst<s.right,"current OOPZ first glyph stays in current SDK control");
                check(sdkRetry>result.x&&sdkRetry<result.x+result.size&&sdkRetry>s.left&&sdkRetry<s.right,"retry uses separately recognized other glyph inside current control");
                BufferedImage erased=copy(im);g=erased.createGraphics();g.setColor(new Color(150,150,150));g.fillRect(s.left+(s.right-s.left)/3,s.top,s.right-s.left,s.bottom-s.top);g.dispose();
                check(!detect(erased,s).found(),"remaining digit and separator are not skip lettering");
                for(int part=0;part<2;part++) {
                    BufferedImage single=copy(im);g=single.createGraphics();g.setColor(new Color(150,150,150));
                    int glyphWidth=result.size*2; // Result size is the matched glyph height.
                    g.fillRect(result.x-glyphWidth/2+part*glyphWidth/2,result.y-result.size/2,glyphWidth/2,result.size);g.dispose();
                    check(!detect(single,s).found(),"one remaining Chinese character cannot authorize a touch");
                }
                WmSplashVisual.Hint narrow=new WmSplashVisual.Hint(s.left+Math.round(sw*7/1200f),s.top,s.right,s.bottom,sw,sh);
                check(detect(im,narrow).found(),"same word with observed narrower one-digit SDK bounds");
                BufferedImage body=copy(im);g=body.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,s.bottom+3,sw,sh);g.dispose();
                check(detect(body,s).found(),"advertising creative and landing CTA are irrelevant");
                BufferedImage moved=copy(im);BufferedImage pill=im.getSubimage(s.left,s.top,s.right-s.left,s.bottom-s.top);int dy=Math.round(sh*.025f);
                g=moved.createGraphics();g.setColor(new Color(150,150,150));g.fillRect(s.left,s.top,s.right-s.left,s.bottom-s.top);g.drawImage(pill,s.left,s.top+dy,null);g.dispose();
                WmSplashVisual.Hint shifted=new WmSplashVisual.Hint(s.left,s.top+dy,s.right,s.bottom+dy,sw,sh);BiliVisual.Result live=detect(moved,shifted);
                check(live.found()&&live.y>result.y+dy*.75,"locate current SDK target instead of fixed screenshot coordinates");
            }
        }
        System.out.println("PASS: "+checks+" WM control/pixel checks");
    }
}
