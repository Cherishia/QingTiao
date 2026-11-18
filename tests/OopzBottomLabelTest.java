package dev.local.qingtiao;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import java.util.ArrayList;import javax.imageio.ImageIO;

public final class OopzBottomLabelTest {
    private static int checks;
    private static final String PKG="com.weilaishanhai.oopz";
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static OopzLeftSkipVisual.Hint scaled(int w,int h){return new OopzLeftSkipVisual.Hint(Math.round(363*w/1200f),Math.round(2556*h/2670f),Math.round(432*w/1200f),Math.round(2601*h/2670f),w,h);}
    private static BufferedImage copy(BufferedImage s){BufferedImage im=new BufferedImage(s.getWidth(),s.getHeight(),1);Graphics2D g=im.createGraphics();g.drawImage(s,0,0,null);g.dispose();return im;}
    private static void erase(BufferedImage im,int x,int y,int w,int h){Graphics2D g=im.createGraphics();g.setColor(new Color(30,30,30));g.fillRect(Math.round(x*im.getWidth()/575f),Math.round(y*im.getHeight()/1280f),Math.round(w*im.getWidth()/575f),Math.round(h*im.getHeight()/1280f));g.dispose();}
    private static BiliVisual.Result detect(BufferedImage im,OopzLeftSkipVisual.Hint hint){int w=im.getWidth(),h=im.getHeight();return OopzLeftSkipVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h,hint);}
    public static void main(String[] args)throws Exception {
        ArrayList<AdMatcher.Node> nodes=new ArrayList<>();AdMatcher.Node label=new AdMatcher.Node(0,363,2556,432,2601,"跳过",null),ad=new AdMatcher.Node(0,1060,2560,1140,2620,"广告",null);nodes.add(new AdMatcher.Node(-1,0,0,1200,2670,"",""));nodes.add(label);nodes.add(ad);
        OopzLeftSkipVisual.Hint hint=OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,true);
        check(hint!=null&&hint.footer,"reported nonclickable null-ID bottom label and ad marker qualify");
        check(!label.clickable,"native action is not a prerequisite or fallback");
        check(OopzLeftSkipVisual.same(hint,scaled(1200,2670)),"current source rectangle retained exactly");
        check(OopzLeftSkipVisual.hint("other.app",nodes,1200,2670,true)==null,"owner is exact");
        check(OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,false)==null,"startup guard remains");
        for(int mode=0;mode<7;mode++) {
            if(mode==0)ad.visible=false;if(mode==1)label.visible=false;if(mode==2)label.enabled=false;if(mode==3)label.editable=true;if(mode==4)label.scrollable=true;if(mode==5)label.checkable=true;if(mode==6)label.password=true;
            check(OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,true)==null,"missing evidence or unsafe context rejected: "+mode);
            ad.visible=label.visible=label.enabled=true;label.editable=label.scrollable=label.checkable=label.password=false;
        }
        for(String word:new String[]{"跳转","跳过登录","领取","关闭","跳过 3"}) {label.text=word;check(OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,true)==null,"only the independent exact skip label qualifies");}label.text="跳过";
        ad.text="广告设置";check(OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,true)==null,"settings text is not ad evidence");ad.text="广告";
        label.top=1800;label.bottom=1845;check(OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,true)==null,"mid-body label rejected");label.top=2556;label.bottom=2601;
        label.left+=40;label.right+=40;check(!OopzLeftSkipVisual.same(hint,OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,true)),"changed bounds cannot authorize old frame");label.left-=40;label.right-=40;
        AdMatcher.Node duplicate=new AdMatcher.Node(0,500,2556,569,2601,"跳过",null);nodes.add(duplicate);check(OopzLeftSkipVisual.hint(PKG,nodes,1200,2670,true)==null,"two labels are ambiguous");nodes.remove(duplicate);
        check(!OopzLeftSkipVisual.detect(new int[720*1602],720,1602,scaled(720,1602)).found(),"native hint cannot authorize blank pixels");
        OopzSurfaceVisual.Proof proof=new OopzSurfaceVisual.Proof();BiliVisual.Result word=new BiliVisual.Result(239,1547,20,"bottom");
        check(!proof.confirm(PKG,3357,0,1200,2670,word,1000,1040),"native label still needs first current frame");
        check(proof.confirm(PKG,3357,0,1200,2670,word,1450,1490),"two current matching frames qualify");
        for(String path:args) {
            BufferedImage src=ImageIO.read(new File(path));
            for(int[] size:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}) {
                int w=size[0],h=size[1];BufferedImage im=new BufferedImage(w,h,1);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(src,0,0,w,h,null);g.dispose();
                OopzLeftSkipVisual.Hint current=scaled(w,h);long begin=System.nanoTime();BiliVisual.Result r=detect(im,current);System.out.println(new File(path).getName()+" "+w+"x"+h+" native-label="+r.found()+" @"+r.x+","+r.y+" "+r.reason+" "+(System.nanoTime()-begin)/1000000+"ms");
                check(r.found(),"current screenshot word recognized within native bounds");
                int touch=WmSplashVisual.touchX(r,false);check(touch>r.x&&touch<current.right&&r.y>current.top&&r.y<current.bottom,"last-glyph touch stays inside current label");
                BufferedImage noBadge=copy(im);erase(noBadge,488,1225,64,33);check(detect(noBadge,current).found(),"tiny visual watermark cannot veto independently proven native ad evidence");
                for(int digit=0;digit<=9;digit++) {
                    BufferedImage next=copy(im);erase(next,208,1224,18,25);g=next.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round(16*w/575f)));g.drawString(Integer.toString(digit),Math.round(211*w/575f),Math.round(1243*h/1280f));g.dispose();
                    check(detect(next,current).found(),"native countdown changes: "+digit);
                }
                for(String suffix:new String[]{"登录","领取"}) {
                    BufferedImage other=copy(im);erase(other,208,1224,40,25);g=other.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round(16*w/575f)));g.drawString(suffix,Math.round(211*w/575f),Math.round(1243*h/1280f));g.dispose();
                    check(!detect(other,current).found(),"neighboring words are not an independent numeric suffix");
                }
                for(int[] a:new int[][]{{173,1224,17,25},{190,1224,18,25},{208,1224,18,25},{500,73,43,38},{433,78,40,25}}) {BufferedImage missing=copy(im);erase(missing,a[0],a[1],a[2],a[3]);check(!detect(missing,current).found(),"each skip character, suffix and current menu remains mandatory");}
                BufferedImage body=copy(im);erase(body,0,200,575,940);check(detect(body,current).found(),"creative is irrelevant");
            }
        }
        System.out.println("PASS: "+checks+" OOPZ bottom-label checks");
    }
}
