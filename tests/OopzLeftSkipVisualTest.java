package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public final class OopzLeftSkipVisualTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static OopzLeftSkipVisual.Hint scaled(int w,int h) {
        return new OopzLeftSkipVisual.Hint(Math.round(w*330/1200f),Math.round(h*165/2670f),Math.round(w*399/1200f),Math.round(h*210/2670f),w,h);
    }
    private static BiliVisual.Result detect(BufferedImage im,OopzLeftSkipVisual.Hint hint) {
        int w=im.getWidth(),h=im.getHeight();return OopzLeftSkipVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h,hint);
    }
    private static BufferedImage copy(BufferedImage im) {
        BufferedImage out=new BufferedImage(im.getWidth(),im.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=out.createGraphics();g.drawImage(im,0,0,null);g.dispose();return out;
    }
    private static void erase(BufferedImage im,int x,int y,int w,int h) {
        Graphics2D g=im.createGraphics();g.setColor(new Color(45,50,55));g.fillRect(x,y,w,h);g.dispose();
    }
    public static void main(String[] args)throws Exception {
        String pkg="com.weilaishanhai.oopz";ArrayList<AdMatcher.Node> nodes=new ArrayList<>();
        nodes.add(new AdMatcher.Node(-1,0,0,1200,2670,"",""));
        AdMatcher.Node ad=new AdMatcher.Node(0,1100,2570,1200,2630,"广告",null);nodes.add(ad);
        AdMatcher.Node label=new AdMatcher.Node(0,330,165,399,210,"跳过",null);label.clickable=false;nodes.add(label);
        OopzLeftSkipVisual.Hint hint=OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true);
        check(hint!=null&&hint.left==330&&hint.top==165,"reported nonclickable null-ID skip label plus visible ad evidence qualifies for pixel verification");
        check(AdMatcher.candidates(nodes,1200,2670,true,true,true,pkg).isEmpty(),"generic right-side rules do not become an accidental left-node native action");
        check(!MatchPolicy.preferGesture(pkg,label.text,label.desc,label.id,false,true),"new path does not authorize the ordinary native fallback sequence");
        check(OopzLeftSkipVisual.hint("other.app",nodes,1200,2670,true)==null,"exact owner required");
        check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,false)==null,"startup required");
        for(int mode=0;mode<6;mode++) {
            if(mode==0)label.visible=false;if(mode==1)label.enabled=false;if(mode==2)label.editable=true;
            if(mode==3)label.checkable=true;if(mode==4)label.scrollable=true;if(mode==5)label.password=true;
            check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)==null,"unsafe label/context rejected: "+mode);
            label.visible=label.enabled=true;label.editable=label.checkable=label.scrollable=label.password=false;
        }
        ad.visible=false;check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)==null,"hidden ad evidence is insufficient");ad.visible=true;
        ad.text="广告设置";check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)==null,"settings wording is not advertising evidence");ad.text="广告";
        for(String text:new String[]{"云闪付 >","反馈","跳过登录","点击跳转详情页","跳过广告","跳过 3"}) {
            label.text=text;check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)==null,"only the separate two-character label authorizes this glyph rectangle");
        }
        label.text="跳过";label.left=1000;label.right=1080;check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)==null,"feedback/menu side is outside this rule");label.left=330;label.right=399;
        label.top=800;label.bottom=845;check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)==null,"body-area skip-like text is rejected");label.top=165;label.bottom=210;
        AdMatcher.Node duplicate=new AdMatcher.Node(0,330,300,399,345,"跳过",null);nodes.add(duplicate);
        check(OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)==null,"two distinct left-side labels are ambiguous");nodes.remove(duplicate);
        check(OopzLeftSkipVisual.same(hint,OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)),"fresh identical label can be revalidated");
        label.left+=20;label.right+=20;check(!OopzLeftSkipVisual.same(hint,OopzLeftSkipVisual.hint(pkg,nodes,1200,2670,true)),"moved label cannot authorize a stale target");label.left-=20;label.right-=20;
        check(!OopzLeftSkipVisual.detect(null,720,1602,hint).found(),"null pixels rejected");
        check(!OopzLeftSkipVisual.detect(new int[10],720,1602,hint).found(),"short pixels rejected");
        check(!OopzLeftSkipVisual.detect(new int[720*1602],720,1602,null).found(),"pixels without label proof rejected");
        check(!OopzLeftSkipVisual.detect(new int[720*1602],720,1602,scaled(720,1602)).found(),"label alone does not authorize blank pixels");
        OopzSurfaceVisual.Proof proof=new OopzSurfaceVisual.Proof();BiliVisual.Result leftWord=new BiliVisual.Result(219,112,22,"left");
        check(!proof.confirm(pkg,1,0,1200,2670,leftWord,1000,1040),"left label still needs a first observation");
        check(proof.confirm(pkg,1,0,1200,2670,leftWord,1450,1490),"existing independent two-frame proof applies to the new left-side target");
        for(String arg:args) {
            boolean positive=!arg.startsWith("!");String path=positive?arg:arg.substring(1);BufferedImage source=ImageIO.read(new File(path));
            for(int[] size:new int[][]{{source.getWidth(),source.getHeight()},{720,1602},{1200,2670},{1440,3136}}) {
                int w=size[0],h=size[1];BufferedImage im=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,w,h,null);g.dispose();
                OopzLeftSkipVisual.Hint current=scaled(w,h);long start=System.nanoTime();BiliVisual.Result r=detect(im,current);
                System.out.println(new File(path).getName()+" "+w+"x"+h+" "+r.found()+" @"+r.x+","+r.y+" "+r.reason+" "+((System.nanoTime()-start)/1000000)+"ms");
                check(r.found()==positive,"real screenshot classification");if(!positive)continue;
                int touch=WmSplashVisual.touchX(r,false);check(touch>r.x&&touch<current.right&&r.y>current.top&&r.y<current.bottom,"touch is inside the final glyph of the current left label");
                for(int part=0;part<2;part++) {
                    BufferedImage single=copy(im);erase(single,r.x-r.size+part*r.size,r.y-r.size/2,r.size,r.size);
                    check(!detect(single,current).found(),"one remaining character is insufficient");
                }
                BufferedImage noDigit=copy(im);erase(noDigit,Math.round(w*193/575f),Math.round(h*80/1280f),Math.round(w*15/575f),Math.round(h*21/1280f));
                check(!detect(noDigit,current).found(),"separate countdown suffix is mandatory");
                for(int digit=0;digit<=9;digit++) {
                    BufferedImage countdown=copy(noDigit);g=countdown.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round(w*16/575f)));
                    g.drawString(Integer.toString(digit),Math.round(w*194/575f),Math.round(h*97/1280f));g.dispose();
                    check(detect(countdown,current).found(),"changing countdown suffix does not depend on the captured digit three");
                }
                BufferedImage noBody=copy(im);erase(noBody,0,h/5,w,h);check(detect(noBody,current).found(),"advertising creative never enters the glyph proof");
                check(!detect(im,new OopzLeftSkipVisual.Hint(Math.round(w*.05f),current.top,Math.round(w*.15f),current.bottom,w,h)).found(),"neighboring brand/CTA is not the skip word");
                check(!detect(im,new OopzLeftSkipVisual.Hint(Math.round(w*.86f),current.top,Math.round(w*.97f),current.bottom,w,h)).found(),"feedback control is not the skip word");
                int l=Math.round(w*120/575f),t=Math.round(h*73/1280f),bw=Math.round(w*125/575f),bh=Math.round(h*37/1280f),dy=Math.round(h*.03f);
                BufferedImage badge=copy(im.getSubimage(l,t,bw,bh)),moved=copy(im);erase(moved,l,t,bw,bh);
                g=moved.createGraphics();g.drawImage(badge,l,t+dy,null);g.dispose();
                check(!detect(moved,current).found(),"old node bounds cannot find a moved control");
                OopzLeftSkipVisual.Hint shifted=new OopzLeftSkipVisual.Hint(current.left,current.top+dy,current.right,current.bottom+dy,w,h);
                BiliVisual.Result live=detect(moved,shifted);check(live.found()&&live.y>r.y+dy*.8,"fresh label geometry follows a changed top inset");
            }
        }
        System.out.println("PASS: "+checks+" OOPZ left-label/pixel checks");
    }
}
