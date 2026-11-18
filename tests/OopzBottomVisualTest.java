package dev.local.qingtiao;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import javax.imageio.ImageIO;
public final class OopzBottomVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static BufferedImage copy(BufferedImage s){BufferedImage im=new BufferedImage(s.getWidth(),s.getHeight(),1);Graphics2D g=im.createGraphics();g.drawImage(s,0,0,null);g.dispose();return im;}
    private static void rect(BufferedImage im,int x,int y,int w,int h){Graphics2D g=im.createGraphics();g.setColor(new Color(30,30,30));g.fillRect(Math.round(x*im.getWidth()/575f),Math.round(y*im.getHeight()/1280f),Math.round(w*im.getWidth()/575f),Math.round(h*im.getHeight()/1280f));g.dispose();}
    private static BiliVisual.Result detect(BufferedImage im){int w=im.getWidth(),h=im.getHeight();return OopzHeaderSkipVisual.detectBottom(im.getRGB(0,0,w,h,null,0,w),w,h);}
    public static void main(String[] args)throws Exception {
        check(!OopzHeaderSkipVisual.detectBottom(null,720,1602).found(),"no image rejected");
        check(!OopzHeaderSkipVisual.detectBottom(new int[10],720,1602).found(),"short image rejected");
        check(!detect(new BufferedImage(720,1602,1)).found(),"blank image cannot authorize bottom touch");
        OopzSurfaceVisual.Proof proof=new OopzSurfaceVisual.Proof();BiliVisual.Result word=new BiliVisual.Result(239,1547,20,"footer");
        check(!proof.confirm("com.tencent.mm",1,0,720,1602,word,1000,1040),"unrelated carrier denied");
        check(!proof.confirm("com.tencent.mm",1,0,720,1602,word,1000,1040,true),"explicit carrier still needs first frame");
        check(proof.confirm("com.tencent.mm",1,0,720,1602,word,1450,1490,true),"explicit carrier needs two current same-window frames");
        for(String path:args) {
            BufferedImage src=ImageIO.read(new File(path));
            for(int[] size:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}) {
                int w=size[0],h=size[1];BufferedImage im=new BufferedImage(w,h,1);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(src,0,0,w,h,null);g.dispose();
                long begin=System.nanoTime();BiliVisual.Result r=detect(im);System.out.println(w+"x"+h+" bottom="+r.found()+" @"+r.x+","+r.y+" "+r.reason+" "+(System.nanoTime()-begin)/1000000+"ms");
                check(r.found(),"provided OOPZ bottom skip recognized");
                check(r.x>w*.30&&r.x<w*.37&&r.y>h*.94&&r.y<h*.98,"skip is separate from the brand chip and landing CTA");
                check(WmSplashVisual.touchX(r,false)>r.x&&WmSplashVisual.touchX(r,false)<w*.37,"touch stays in final glyph");
                for(int[] a:new int[][]{{173,1224,17,25},{190,1224,18,25},{208,1224,18,25},{500,73,43,38},{433,78,40,25},{490,1228,24,28},{513,1228,16,29},{529,1228,18,29}}) {
                    BufferedImage missing=copy(im);rect(missing,a[0],a[1],a[2],a[3]);check(!detect(missing).found(),"each word character, countdown, menu or ad icon/word is mandatory");
                }
                for(int digit=0;digit<=9;digit++) {
                    BufferedImage next=copy(im);rect(next,208,1224,18,25);g=next.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round(16*w/575f)));g.drawString(Integer.toString(digit),Math.round(211*w/575f),Math.round(1243*h/1280f));g.dispose();
                    check(detect(next).found(),"bottom countdown can change: "+digit);
                }
                BufferedImage body=copy(im);rect(body,0,200,575,940);check(detect(body).found(),"ad creative is not part of recognition");
                int bx=Math.round(170*w/575f),by=Math.round(1223*h/1280f),bw=Math.round(60*w/575f),bh=Math.round(29*h/1280f);
                BufferedImage badge=copy(im.getSubimage(bx,by,bw,bh)),moved=copy(im);rect(moved,170,1223,60,29);
                g=moved.createGraphics();g.drawImage(badge,Math.round(20*w/575f),by-Math.round(35*h/1280f),null);g.dispose();
                BiliVisual.Result live=detect(moved);check(live.found()&&live.x<w*.15&&live.y<r.y,"bottom target is located after a horizontal and vertical move");
            }
        }
        System.out.println("PASS: "+checks+" OOPZ bottom-ad checks");
    }
}
