package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public final class VolunteerVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static BiliVisual.Result detect(BufferedImage im) {
        int w=im.getWidth(),h=im.getHeight();return VolunteerVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h);
    }
    private static BufferedImage copy(BufferedImage im) {
        BufferedImage out=new BufferedImage(im.getWidth(),im.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.drawImage(im,0,0,null);g.dispose();return out;
    }
    private static BufferedImage control(String label,int fontSize,boolean ring) {
        BufferedImage im=new BufferedImage(592,1280,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
        g.setColor(new Color(65,80,110));g.fillRect(0,0,592,1280);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,fontSize));FontMetrics fm=g.getFontMetrics();
        g.drawString(label,547-fm.stringWidth(label)/2,86+(fm.getAscent()-fm.getDescent())/2);
        if(ring){g.setStroke(new BasicStroke(2));g.drawOval(520,59,54,54);}g.dispose();return im;
    }
    public static void main(String[] args)throws Exception {
        String pkg="com.zzw.october";ArrayList<AdMatcher.Node> nodes=new ArrayList<>();nodes.add(new AdMatcher.Node(-1,0,0,1200,2608,"",""));
        AdMatcher.Node hidden=new AdMatcher.Node(0,0,0,180,75,"跳过","com.zzw.october:id/tvSkip");hidden.visible=false;nodes.add(hidden);
        check(!VolunteerVisual.context(pkg,true,nodes),"hidden skip placeholder alone never activates the native canvas path");
        AdMatcher.Node canvas=new AdMatcher.Node(0,0,0,1200,2608,"","");canvas.className="android.webkit.WebView";nodes.add(canvas);
        check(VolunteerVisual.context(pkg,true,nodes),"exact reported app with current visible rendering container qualifies for pixel observation only");
        check(!VolunteerVisual.context("com.zzw.october.fake",true,nodes)&&!VolunteerVisual.context(pkg,false,nodes),"wrong owner or expired startup denied");
        canvas.visible=false;check(!VolunteerVisual.context(pkg,true,nodes),"hidden renderer denied");canvas.visible=true;
        for(String type:new String[]{"android.view.SurfaceView","android.view.TextureView","io.flutter.FlutterView"}) {canvas.className=type;check(VolunteerVisual.context(pkg,true,nodes),"reported renderer bucket also covers "+type);}
        canvas.password=true;check(!VolunteerVisual.context(pkg,true,nodes),"sensitive context denied");canvas.password=false;
        check(AdMatcher.candidates(nodes,1200,2608,true,true,true,pkg).isEmpty(),"hidden label and its top-left coordinates never become an action target");
        check(!VolunteerVisual.detect(null,720,1565).found(),"null pixels denied");check(!VolunteerVisual.detect(new int[10],720,1565).found(),"short pixels denied");
        for(int fontSize:new int[]{16,18,20})for(String label:new String[]{"关闭","确定","允许","跳转","过跳","广告","5","跳","过"})check(!detect(control(label,fontSize,true)).found(),"ring with wrong text denied: "+label+" at "+fontSize);
        for(int size:new int[]{16,18,20}) {
            BufferedImage im=control("跳过",size,true);BiliVisual.Result r=detect(im);System.out.println("native font "+size+" "+r.found()+" "+r.reason);
            check(r.found(),"sharp antialiased native glyphs accepted independently of video grey values");
            System.out.println("old grey template at native font "+size+": "+CircularSkipVisual.detect(im.getRGB(0,0,592,1280,null,0,592),592,1280).found());
        }
        check(!detect(control("跳过",18,false)).found(),"native lettering without a ring denied");
        BufferedImage original=control("跳过",18,true),badge=original.getSubimage(518,57,60,60);Graphics2D duplicate=original.createGraphics();duplicate.drawImage(badge,458,57,null);duplicate.dispose();
        check(!detect(original).found(),"two current native circle/word targets are ambiguous");
        for(int offset:new int[]{-36,84}) {
            BufferedImage moved=control("跳过",18,true);BufferedImage part=moved.getSubimage(518,57,60,60);BufferedImage saved=copy(part);
            Graphics2D g=moved.createGraphics();g.setColor(new Color(65,80,110));g.fillRect(518,57,60,60);g.drawImage(saved,518,57+offset,null);g.dispose();
            BiliVisual.Result r=detect(moved);check(r.found()&&Math.abs(r.y-86-offset)<8,"native circle follows a changed fullscreen/status-bar inset");
        }
        for(String arg:args) {
            boolean positive=!arg.startsWith("!");String path=positive?arg:arg.substring(1);BufferedImage source=ImageIO.read(new File(path));
            for(int[] size:new int[][]{{source.getWidth(),source.getHeight()},{720,1565},{1200,2608}}) {
                BufferedImage im=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,size[0],size[1],null);g.dispose();
                long begin=System.nanoTime();BiliVisual.Result r=detect(im);System.out.println(new File(path).getName()+" "+size[0]+"x"+size[1]+" "+r.found()+" @"+r.x+","+r.y+" "+r.reason+" "+((System.nanoTime()-begin)/1000000)+"ms");
                check(r.found()==positive,"real frame classification");if(!positive)continue;
                check(r.x>size[0]*.87&&r.x<size[0]*.98&&r.y>size[1]*.04&&r.y<size[1]*.12,"target follows the current upper-right ring, not hidden node coordinates");
                int w=size[0],h=size[1],left=Math.round(w*529/592f),top=Math.round(h*76/1280f),gw=Math.round(w*36/592f),gh=Math.round(h*19/1280f);
                for(int part=0;part<2;part++) {
                    BufferedImage erased=copy(im);g=erased.createGraphics();g.setColor(Color.GRAY);g.fillRect(left+part*gw/2,top,gw/2+1,gh);g.dispose();
                    check(!detect(erased).found(),"real ring and a single remaining character never trigger a touch");
                }
                BufferedImage word=im.getSubimage(left,top,gw,gh),noRing=copy(im);g=noRing.createGraphics();g.setColor(Color.GRAY);
                g.fillRect(Math.round(w*510/592f),Math.round(h*54/1280f),Math.round(w*82/592f),Math.round(h*72/1280f));g.drawImage(word,left,top,null);g.dispose();
                check(!detect(noRing).found(),"real lettering without ring never triggers a touch");
                BufferedImage noBody=copy(im);g=noBody.createGraphics();g.setColor(Color.BLACK);g.fillRect(0,(int)(h*.16),w,h);g.dispose();
                check(detect(noBody).found(),"photo and brand content are irrelevant to both recognition channels");
            }
        }
        System.out.println("PASS: "+checks+" volunteer native-frame checks");
    }
}
