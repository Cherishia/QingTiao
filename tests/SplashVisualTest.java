package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

public final class SplashVisualTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static java.util.List<AdMatcher.Node> context(String text) {
        return Arrays.asList(new AdMatcher.Node(-1,0,0,1080,2400,text,""));
    }
    private static BiliVisual.Result detect(BufferedImage image) {
        int w=image.getWidth(),h=image.getHeight();return SplashVisual.detect(image.getRGB(0,0,w,h,null,0,w),w,h);
    }
    public static void main(String[] args)throws Exception {
        check(SplashVisual.context("com.MobileTicket",true,context("广告")),"12306 read ad evidence permits visual fallback");
        check(SplashVisual.context("example.app",true,context("广告")),"no per-app allowlist");
        check(!SplashVisual.context("com.MobileTicket",false,context("广告")),"outside startup denied");
        check(!SplashVisual.context("com.MobileTicket",true,context("普通内容")),"without ad evidence denied");
        check(!SplashVisual.context("",true,context("广告")),"missing foreground identity denied");
        java.util.List<AdMatcher.Node> sensitive=new ArrayList<>(context("广告"));
        AdMatcher.Node password=new AdMatcher.Node(0,0,0,1,1,"","");password.password=true;sensitive.add(password);
        check(!SplashVisual.context("com.MobileTicket",true,sensitive),"sensitive page denied");
        check(!SplashVisual.detect(null,576,1280).found(),"null frame denied");
        check(!SplashVisual.detect(new int[10],576,1280).found(),"truncated pixels denied");
        check(!SplashVisual.detect(new int[576*1280],1280,576).found(),"landscape denied");
        int w=576,h=1280;int[] blank=new int[w*h];Arrays.fill(blank,0xff999999);
        check(!SplashVisual.detect(blank,w,h).found(),"gray capsule alone denied");
        Random random=new Random(12306);for(int i=0;i<blank.length;i++)blank[i]=0xff000000|random.nextInt(0x1000000);
        check(!SplashVisual.detect(blank,w,h).found(),"textured pixels denied");
        for(String text:new String[]{"返回","跳转","关闭","确定","详情","观看","跳","过","×","123"}) {
            BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
            g.setColor(new Color(150,150,150));g.fillRect(0,0,w,h);g.setColor(Color.WHITE);
            g.setFont(new Font("Microsoft YaHei",Font.PLAIN,20));g.drawString(text,479,108);g.dispose();
            check(!detect(im).found(),"different glyph rejected: "+text);
        }
        System.out.println("PASS: "+checks+" splash visual checks");
        for(String path:args) {
            boolean expected=!path.startsWith("!");if(!expected)path=path.substring(1);
            BufferedImage source=ImageIO.read(new File(path));
            for(int sw:new int[]{source.getWidth(),720,1080,1200}) {
                int sh=Math.round(source.getHeight()*(sw/(float)source.getWidth()));
                BufferedImage im=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(source,0,0,sw,sh,null);g.dispose();long begin=System.nanoTime();
                BiliVisual.Result r=detect(im);
                System.out.println(new File(path).getName()+" "+sw+"x"+sh+": "+r.found()+" @ "+r.x+","+r.y+" "+r.reason+" "+((System.nanoTime()-begin)/1000000)+"ms");
                check(r.found()==expected,"fixture outcome at "+sw+": "+path);
                if(expected) {
                    check(Math.abs(r.x/(double)sw-.867)<.012&&Math.abs(r.y/(double)sh-.077)<.008,"actual skip glyph center");
                    int left=(int)(sw*479/576f),top=(int)(sh*89/1280f),gw=Math.round(sw*41/576f),gh=Math.round(sh*20/1280f);
                    BufferedImage letters=new BufferedImage(gw,gh,BufferedImage.TYPE_INT_RGB);Graphics2D copy=letters.createGraphics();
                    copy.drawImage(im,0,0,gw,gh,left,top,left+gw,top+gh,null);copy.dispose();
                    g=im.createGraphics();g.drawImage(letters,left-Math.round(sw*48/576f),top,null);g.dispose();
                    check(!detect(im).found(),"two spatially separate skip words are ambiguous");
                    // Erase only the observed glyph, leaving the rounded ad/capsule intact.
                    g=im.createGraphics();g.setColor(new Color(155,155,155));g.fillRect((int)(sw*.82),(int)(sh*.065),(int)(sw*.10),(int)(sh*.025));g.dispose();
                    BiliVisual.Result moved=detect(im);
                    check(moved.found()&&moved.x<r.x-sw*.06,"locate moved word rather than fixed screenshot corner coordinates");
                    g=im.createGraphics();g.setColor(new Color(155,155,155));g.fillRect((int)(sw*.72),(int)(sh*.065),(int)(sw*.11),(int)(sh*.025));g.dispose();
                    check(!detect(im).found(),"capsule without skip glyph must not be clicked");
                }
            }
        }
    }
}
