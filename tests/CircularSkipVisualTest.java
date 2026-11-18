package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;

public final class CircularSkipVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static BiliVisual.Result detect(BufferedImage im) {
        int w=im.getWidth(),h=im.getHeight();return CircularSkipVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h);
    }
    private static BufferedImage copy(BufferedImage im) {
        BufferedImage next=new BufferedImage(im.getWidth(),im.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=next.createGraphics();g.drawImage(im,0,0,null);g.dispose();return next;
    }
    public static void main(String[] args)throws Exception {
        check(!CircularSkipVisual.detect(null,720,1565).found(),"null frame denied");
        check(!CircularSkipVisual.detect(new int[10],720,1565).found(),"truncated frame denied");
        check(!CircularSkipVisual.detect(new int[720*1565],1565,720).found(),"landscape denied");
        int[] pixels=new int[592*1280];Arrays.fill(pixels,0xff888888);check(!CircularSkipVisual.detect(pixels,592,1280).found(),"flat field denied");
        Random random=new Random(55);for(int i=0;i<pixels.length;i++)pixels[i]=0xff000000|random.nextInt(0x1000000);
        check(!CircularSkipVisual.detect(pixels,592,1280).found(),"texture without text/ring denied");
        for(String label:new String[]{"关闭","确定","广告","跳","过","5"}) {
            BufferedImage im=new BufferedImage(592,1280,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();g.setColor(Color.GRAY);g.fillRect(0,0,592,1280);
            g.setColor(Color.WHITE);g.setStroke(new BasicStroke(2));g.drawOval(519,59,54,54);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,18));g.drawString(label,530,94);g.dispose();
            check(!detect(im).found(),"circle with unrelated control label denied: "+label);
        }
        for(String arg:args) {
            boolean positive=!arg.startsWith("!");String path=positive?arg:arg.substring(1);BufferedImage source=ImageIO.read(new File(path));
            int[][] sizes={{source.getWidth(),source.getHeight()},{720,Math.round(source.getHeight()*720f/source.getWidth())},{1080,Math.round(source.getHeight()*1080f/source.getWidth())},{1200,Math.round(source.getHeight()*1200f/source.getWidth())},{720,1565},{1080,2347},{1200,2608}};
            for(int[] size:sizes) {
                int w=size[0],h=size[1];BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,w,h,null);g.dispose();
                long begin=System.nanoTime();BiliVisual.Result r=detect(im);System.out.println(new File(path).getName()+" "+w+"x"+h+" "+r.found()+" @"+r.x+","+r.y+" "+r.reason+" "+((System.nanoTime()-begin)/1000000)+"ms");
                check(r.found()==positive,"recorded frame classification");if(!positive)continue;
                check(r.x>w*.88&&r.x<w*.97&&r.y>h*.045&&r.y<h*.095,"touch lies inside the observed circular control");
                int left=Math.round(w*529/592f),top=Math.round(h*76/1280f),gw=Math.round(w*36/592f),gh=Math.round(h*19/1280f);
                for(int part=0;part<2;part++) {
                    BufferedImage erased=copy(im);g=erased.createGraphics();g.setColor(Color.GRAY);g.fillRect(left+part*gw/2,top,gw/2+1,gh);g.dispose();
                    check(!detect(erased).found(),"a ring and one remaining character are insufficient");
                }
                BufferedImage letters=im.getSubimage(left,top,gw,gh),noRing=copy(im);g=noRing.createGraphics();g.setColor(Color.GRAY);
                g.fillRect(Math.round(w*510/592f),Math.round(h*54/1280f),Math.round(w*82/592f),Math.round(h*72/1280f));g.drawImage(letters,left,top,null);g.dispose();
                check(!detect(noRing).found(),"lettering without an enclosing ring is insufficient");
                BufferedImage body=copy(im);g=body.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,(int)(h*.16),w,h);g.dispose();
                check(detect(body).found(),"ad creative and brand are irrelevant to the control");
                int bl=Math.round(w*516/592f),bt=Math.round(h*57/1280f),bw=Math.round(w*61/592f),bh=Math.round(h*60/1280f),shift=Math.round(w*30/592f);
                BufferedImage badge=im.getSubimage(bl,bt,bw,bh),moved=copy(im);g=moved.createGraphics();g.setColor(Color.GRAY);g.fillRect(bl,bt,bw,bh);g.drawImage(badge,bl-shift,bt,null);g.dispose();
                BiliVisual.Result next=detect(moved);check(next.found()&&next.x<r.x-w*.035,"locate the current moved circle rather than a fixed corner coordinate");
                BufferedImage twins=copy(im);g=twins.createGraphics();g.setColor(Color.GRAY);g.fillRect(Math.round(w*455/592f),bt,Math.round(w*137/592f),bh);
                g.drawImage(badge,bl-Math.round(w*54/592f),bt,null);g.drawImage(badge,bl+Math.round(w*8/592f),bt,null);g.dispose();
                check(!detect(twins).found(),"two live circular skip controls are ambiguous");
            }
        }
        System.out.println("PASS: "+checks+" circular skip checks");
    }
}
