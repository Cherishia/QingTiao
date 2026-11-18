package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class OopzHeaderSkipVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static BufferedImage copy(BufferedImage src) {BufferedImage im=new BufferedImage(src.getWidth(),src.getHeight(),1);Graphics2D g=im.createGraphics();g.drawImage(src,0,0,null);g.dispose();return im;}
    private static BufferedImage scale(BufferedImage src,int w,int h) {BufferedImage im=new BufferedImage(w,h,1);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(src,0,0,w,h,null);g.dispose();return im;}
    private static BiliVisual.Result detect(BufferedImage im) {int w=im.getWidth(),h=im.getHeight();return OopzHeaderSkipVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h);}
    private static void erase(BufferedImage im,int x,int y,int w,int h) {Graphics2D g=im.createGraphics();g.setColor(new Color(35,40,45));g.fillRect(x,y,w,h);g.dispose();}
    private static void rect(BufferedImage im,int x,int y,int w,int h){erase(im,Math.round(x*im.getWidth()/575f),Math.round(y*im.getHeight()/1280f),Math.round(w*im.getWidth()/575f),Math.round(h*im.getHeight()/1280f));}
    public static void main(String[] args)throws Exception {
        check(!OopzHeaderSkipVisual.detect(null,720,1602).found(),"null frame rejected");
        check(!OopzHeaderSkipVisual.detect(new int[10],720,1602).found(),"short frame rejected");
        for(int color:new int[]{0,0xffffff,0x444444,0x334466}) {
            BufferedImage im=new BufferedImage(720,1602,1);Graphics2D g=im.createGraphics();g.setColor(new Color(color));g.fillRect(0,0,720,1602);g.dispose();
            check(!detect(im).found(),"uniform page is not a mini-app skip header");
        }
        for(String text:new String[]{"反馈","美团 >","云闪付 >","跳过登录","了解更多","3 |","跳过1"}) {
            BufferedImage im=new BufferedImage(720,1602,1);Graphics2D g=im.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,20));g.drawString(text,220,120);g.dispose();
            check(!detect(im).found(),"text without the independent mini-app menu is insufficient: "+text);
        }
        OopzSurfaceVisual.Proof proof=new OopzSurfaceVisual.Proof();String pkg="com.weilaishanhai.oopz";BiliVisual.Result target=new BiliVisual.Result(240,113,20,"header");
        check(!proof.confirm(pkg,1,0,1200,2670,target,1000,1040),"first header observation cannot touch");
        check(!proof.confirm(pkg,1,0,1200,2670,target,1000,1090),"reused image is not a pair");
        check(proof.confirm(pkg,1,0,1200,2670,target,1450,1490),"same header in independent fresh frames can qualify");
        for(String arg:args) {
            boolean blocked=arg.startsWith("!");String path=blocked?arg.substring(1):arg;BufferedImage source=ImageIO.read(new File(path));
            for(int[] size:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}) {
                BufferedImage im=scale(source,size[0],size[1]);long begin=System.nanoTime();BiliVisual.Result found=detect(im);
                System.out.println(new File(path).getName()+" "+size[0]+"x"+size[1]+" "+found.found()+" @"+found.x+","+found.y+" "+found.reason+" "+(System.nanoTime()-begin)/1000000+"ms");
                check(found.found()!=blocked,"actual left header / occluded right menu classification");if(blocked)continue;
                check(found.x>im.getWidth()*.30&&found.x<im.getWidth()*.37&&found.y>im.getHeight()*.06&&found.y<im.getHeight()*.08,"current left skip word rather than brand or feedback");
                int touch=WmSplashVisual.touchX(found,false);check(touch>found.x&&touch<im.getWidth()*.36,"touch stays within the last glyph");
                OopzLeftSkipVisual.Hint label=new OopzLeftSkipVisual.Hint(Math.round(174*im.getWidth()/575f),Math.round(78*im.getHeight()/1280f),Math.round(209*im.getWidth()/575f),Math.round(104*im.getHeight()/1280f),im.getWidth(),im.getHeight());
                check(OopzLeftSkipVisual.detect(im.getRGB(0,0,im.getWidth(),im.getHeight(),null,0,im.getWidth()),im.getWidth(),im.getHeight(),label).found(),"same new lettering also qualifies when an accessible left label exists");
                for(int[] area:new int[][]{{175,79,15,23},{191,79,17,23},{209,79,15,23},{503,117,35,37},{437,125,30,22}}) {
                    BufferedImage missing=copy(im);rect(missing,area[0],area[1],area[2],area[3]);
                    check(!detect(missing).found(),"each character, suffix, ring and separate dots are necessary");
                }
                for(int digit=0;digit<=9;digit++) {
                    BufferedImage countdown=copy(im);rect(countdown,209,79,15,23);Graphics2D cg=countdown.createGraphics();cg.setColor(Color.WHITE);cg.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round(16*im.getWidth()/575f)));cg.drawString(Integer.toString(digit),Math.round(211*im.getWidth()/575f),Math.round(97*im.getHeight()/1280f));cg.dispose();
                    check(detect(countdown).found(),"suffix countdown is not fixed to the supplied one: "+digit);
                }
                for(String word:new String[]{"反馈 1","确定 1","跳转 1","过跳 1","跳过登录"}) {
                    BufferedImage other=copy(im);rect(other,171,78,95,29);Graphics2D cg=other.createGraphics();cg.setColor(Color.WHITE);cg.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round(16*im.getWidth()/575f)));cg.drawString(word,Math.round(177*im.getWidth()/575f),Math.round(97*im.getHeight()/1280f));cg.dispose();
                    check(!detect(other).found(),"menu plus another word or login prompt is insufficient: "+word);
                }
                BufferedImage body=copy(im);erase(body,0,im.getHeight()/5,im.getWidth(),im.getHeight());check(detect(body).found(),"creative content is irrelevant");
                // Relocate the actual unchanged button pixels to the opposite top corner.
                BufferedImage right=copy(im);int l=Math.round(170*im.getWidth()/575f),t=Math.round(77*im.getHeight()/1280f),bw=Math.round(57*im.getWidth()/575f),bh=Math.round(29*im.getHeight()/1280f),dx=Math.round(287*im.getWidth()/575f);
                BufferedImage badge=copy(im.getSubimage(l,t,bw,bh));erase(right,l,t,bw,bh);erase(right,l+dx,t,bw,bh);Graphics2D g=right.createGraphics();g.drawImage(badge,l+dx,t,null);g.dispose();
                BiliVisual.Result moved=detect(right);check(moved.found()&&moved.x>im.getWidth()*.80&&moved.x<im.getWidth()*.90,"same live pixels qualify on the right without a separator or node label");
                BufferedImage blockedMenu=copy(right);rect(blockedMenu,510,115,65,60);check(!detect(blockedMenu).found(),"partially covered menu cannot authorize a blind touch");
                BufferedImage duplicate=copy(im);g=duplicate.createGraphics();g.drawImage(badge,l+dx,t,null);g.dispose();
                check(!detect(duplicate).found(),"two distinct skip words are ambiguous");
            }
        }
        System.out.println("PASS: "+checks+" OOPZ mini-app header checks");
    }
}
