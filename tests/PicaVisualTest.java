package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class PicaVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static PicaVisual.Result detect(BufferedImage image) {
        return detect(image,"com.picacomic.fregata");
    }
    private static PicaVisual.Result detect(BufferedImage image,String pkg) {
        int w=image.getWidth(),h=image.getHeight();return PicaVisual.detect(image.getRGB(0,0,w,h,null,0,w),w,h,pkg);
    }
    public static void main(String[] args)throws Exception {
        SplashActions gate=new SplashActions();
        check(gate.allowsNotice(0),"notice initially allowed");gate.markNotice(0);
        check(!gate.allowsNotice(0)&&!gate.allowsNotice(7),"same notice and tiny frame differences not pressed repeatedly");
        check(gate.allowsNotice(-1),"distinct following notice allowed without requiring an intermediate empty frame");
        gate.markNotice(-1);check(!gate.allowsNotice(0)&&gate.noticeCount()==2,"two notices bound repeated actions");
        check(gate.allows(SplashActions.PICA_CLOSE)&&!gate.finished(),"closing notice does not end following ad recognition");
        gate.mark(SplashActions.PICA_CLOSE);check(gate.finished()&&!gate.allows(SplashActions.PICA_CLOSE),"ad close bounded to one attempt");
        check(!gate.allows(SplashActions.PICA_NOTICE)&&!gate.allows(SplashActions.JM),"final close blocks subsequent startup actions");
        gate.reset();check(!gate.finished()&&gate.allows(SplashActions.SKIP),"foreground change resets phases");
        gate.mark(SplashActions.SKIP);check(!gate.allows(SplashActions.JM)&&!gate.allows(SplashActions.SKIP),"ordinary skip remains final for ad close actions");
        gate.reset();gate.markNotice(0);gate.noticeAbsent(1000);gate.noticeVisible();
        check(!gate.allowsNotice(0),"one missed frame cannot rearm the same notice");
        gate.noticeAbsent(2000);gate.noticeAbsent(2700);
        check(gate.allowsNotice(0),"same-looking popup can reappear after confirmed absence");
        gate.markNotice(0);check(!gate.allowsNotice(-1),"reappeared popup also consumes second and final notice budget");
        gate.reset();gate.mark(SplashActions.PICA_CLOSE);
        check(gate.allowsNotice(0)&&!gate.allows(SplashActions.PICA_CLOSE),"notice appearing after full-screen close has a separate budget");
        gate.reset();gate.markNotice(123,216);
        check(!gate.allowsNotice(123,216),"same notice in same window cannot repeat");
        check(gate.nextNotice(123,215),"a separately verified new popup window is a new notice even with the same layout");
        gate.markNotice(123,215);check(!gate.allowsNotice(-1,214),"window changes cannot bypass the two-notice budget");
        for(int count=1;count<=2;count++) {
            gate.reset();for(int i=0;i<count;i++)gate.markNotice(i,900+i);
            check(gate.allows(SplashActions.PICA_CLOSE),"either one or two notices allow a following full-screen close");
            gate.mark(SplashActions.PICA_CLOSE);check(!gate.allows(SplashActions.PICA_CLOSE),"both two-ad and three-ad flows retain a bounded final close");
        }
        check(!PicaVisual.detect(null,575,1280).target.found(),"null frame rejected");
        check(!PicaVisual.detect(new int[10],575,1280).target.found(),"short buffer rejected");
        check(!PicaVisual.detect(new int[575*1280],1280,575).target.found(),"landscape rejected");
        BufferedImage blank=new BufferedImage(575,1280,BufferedImage.TYPE_INT_RGB);Graphics2D g=blank.createGraphics();
        g.setColor(new Color(219,84,125));g.fillRect(0,0,575,65);g.setColor(Color.BLACK);g.fillRect(0,65,575,1215);
        g.setColor(Color.WHITE);g.setStroke(new BasicStroke(2));g.drawOval(9,74,39,39);g.drawLine(20,84,37,101);g.drawLine(20,101,37,84);g.dispose();
        check(!detect(blank,"ordinary.app").target.found(),"ordinary circled X with pink toolbar but no ad frame denied");
        check(PicaVisual.trusted("com.picacomic.fregata")&&!PicaVisual.trusted("com.picacomic.fregata.fake")&&!PicaVisual.trusted(null),"exact observed package only");
        check(detect(blank).target.found(),"verified Pica uses current circled X without artwork banner");
        BufferedImage island=new BufferedImage(575,1280,BufferedImage.TYPE_INT_RGB);g=island.createGraphics();g.drawImage(blank,0,0,null);
        g.setColor(Color.BLACK);g.fillRoundRect(208,8,222,50,40,40);g.dispose();
        check(detect(island).target.found(),"central system live-activity pill does not hide pink evidence on both sides");
        check(!detect(island,"ordinary.app").target.found(),"side-band header exception never applies to unrelated apps");
        g=island.createGraphics();g.setColor(Color.BLACK);g.fillRect(430,0,145,65);g.dispose();
        check(!detect(island).target.found(),"a single pink side is insufficient when the centre is occluded");
        g=blank.createGraphics();g.setColor(new Color(165,72,115));g.setStroke(new BasicStroke(2));g.drawRect(5,81,565,51);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(3));g.drawOval(9,74,39,39);g.dispose();
        PicaVisual.Result synthetic=detect(blank);check(synthetic.target.found(),"circled X plus the ad framework qualifies");
        g=blank.createGraphics();g.setColor(Color.BLACK);g.fillRect(8,72,42,44);g.setColor(Color.BLACK);g.fillOval(40,75,40,40);
        g.setColor(Color.WHITE);g.setStroke(new BasicStroke(3));g.drawOval(40,75,39,39);g.setStroke(new BasicStroke(2));g.drawLine(51,85,68,102);g.drawLine(51,102,68,85);g.dispose();
        PicaVisual.Result moved=detect(blank);check(moved.target.found()&&moved.target.x>50,"target follows current circle instead of fixed screenshot coordinates");
        java.util.ArrayList<Long> notices=new java.util.ArrayList<>();
        for(String arg:args) {
            String[] parts=arg.split("=",2);String type=parts[0];BufferedImage source=ImageIO.read(new File(parts[1]));
            long signature=PicaVisual.noticeSignature(source.getRGB(0,0,source.getWidth(),source.getHeight(),null,0,source.getWidth()),source.getWidth(),source.getHeight());
            if(type.equals("notice"))notices.add(signature);
            int[][] sizes={{source.getWidth(),source.getHeight()},{720,Math.round(source.getHeight()*720f/source.getWidth())},{1080,Math.round(source.getHeight()*1080f/source.getWidth())},{1200,Math.round(source.getHeight()*1200f/source.getWidth())},{720,1602},{1080,2400},{1200,2670}};
            for(int[] size:sizes) {
                int sw=size[0],sh=size[1];BufferedImage im=new BufferedImage(sw,sh,BufferedImage.TYPE_INT_RGB);
                g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,sw,sh,null);g.dispose();
                long begin=System.nanoTime();PicaVisual.Result r=detect(im,type.equals("unscoped")||type.equals("legacy")?"ordinary.app":"com.picacomic.fregata");
                System.out.println(type+" "+sw+"x"+sh+": kind="+r.kind+" found="+r.target.found()+" waiting="+r.waiting+" @ "+r.target.x+","+r.target.y+" "+r.target.reason+" "+((System.nanoTime()-begin)/1000000)+"ms");
                if(type.equals("notice"))check(r.target.found()&&r.kind==SplashActions.PICA_NOTICE&&Math.abs(r.target.x/(double)sw-.285)<.03,"notice targets never-show button");
                else if(type.equals("before"))check(!r.target.found()&&r.waiting,"no tap before X appears");
                else if(type.equals("close")||type.equals("legacy"))check(r.target.found()&&r.kind==SplashActions.PICA_CLOSE&&Math.abs(r.target.x/(double)sw-.05)<.02&&Math.abs(r.target.y/(double)sh-.073)<.015,"live circled X target");
                else check(!r.target.found(),"unrelated screenshot rejected");
                if(type.equals("notice")) {
                    long scaledSignature=PicaVisual.noticeSignature(im.getRGB(0,0,sw,sh,null,0,sw),sw,sh);
                    check(Long.bitCount(signature^scaledSignature)<=8,"same notice identity survives scale and resampling");
                    g=im.createGraphics();g.setColor(Color.WHITE);g.fillRect((int)(sw*.19),(int)(sh*.928),(int)(sw*.19),(int)(sh*.027));g.dispose();
                    check(!detect(im).target.found(),"do not fall back to confirm if never-show glyph is missing");
                }
                if(type.equals("close")) {
                    // Discard all creative pixels below the close controls; matching must still work.
                    g=im.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,(int)(sh*.14),sw,sh);g.dispose();
                    check(detect(im).target.found(),"ad body is irrelevant to close matching");
                    g=im.createGraphics();g.setColor(new Color(23,23,23));g.fillRect((int)(sw*.032),(int)(sh*.064),(int)(sw*.037),(int)(sh*.019));g.dispose();
                    PicaVisual.Result ringOnly=detect(im);check(!ringOnly.target.found()&&ringOnly.waiting,"circle without X never clicked");
                    int cx=Math.round(sw*.05f),cy=Math.round(sh*.073f),arm=Math.round(sw*.014f);
                    g=im.createGraphics();g.setColor(Color.WHITE);g.setStroke(new BasicStroke(Math.max(1,sw*.003f)));
                    g.drawLine(cx-arm,cy,cx+arm,cy);g.drawLine(cx,cy-arm,cx,cy+arm);g.dispose();
                    PicaVisual.Result plus=detect(im);
                    check(!plus.target.found(),"a plus in the same circle is not a close X");
                }
            }
        }
        if(notices.size()>=2) {
            gate.reset();gate.markNotice(notices.get(0));
            System.out.println("notice identity difference="+Long.bitCount(notices.get(0)^notices.get(1)));
            check(gate.allowsNotice(notices.get(1)),"the two supplied notices can close consecutively");
            gate.markNotice(notices.get(1));check(!gate.allowsNotice(notices.get(0)),"actual two-notice flow stops at its budget");
        }
        System.out.println("PASS: "+checks+" Pica visual/phase checks");
    }
}
