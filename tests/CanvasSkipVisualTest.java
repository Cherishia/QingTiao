package dev.local.qingtiao;
import java.awt.*;import java.awt.image.BufferedImage;import java.io.File;import java.util.ArrayList;import javax.imageio.ImageIO;
public final class CanvasSkipVisualTest {
    private static int checks;private static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    private static BiliVisual.Result detect(BufferedImage im,boolean nativeAd){int w=im.getWidth(),h=im.getHeight();return CanvasSkipVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h,nativeAd);}
    private static BufferedImage copy(BufferedImage s){BufferedImage im=new BufferedImage(s.getWidth(),s.getHeight(),1);Graphics2D g=im.createGraphics();g.drawImage(s,0,0,null);g.dispose();return im;}
    private static void erase(BufferedImage im,int x,int y,int w,int h){Graphics2D g=im.createGraphics();g.setColor(new Color(30,30,30));g.fillRect(Math.round(x*im.getWidth()/575f),Math.round(y*im.getHeight()/1280f),Math.round(w*im.getWidth()/575f),Math.round(h*im.getHeight()/1280f));g.dispose();}
    public static void main(String[] args)throws Exception {
        ArrayList<AdMatcher.Node> nodes=new ArrayList<>();AdMatcher.Node n=new AdMatcher.Node(-1,0,0,1200,2670,"",null);nodes.add(n);
        check(CanvasSkipVisual.context("any.first.app",true,nodes)&&CanvasSkipVisual.context("any.second.app",true,nodes),"no owner allow-list for drawn layout");
        check(!CanvasSkipVisual.context("any.app",false,nodes),"startup deadline remains");
        check(!CanvasSkipVisual.context("com.openai.chatgpt",true,nodes),"pasted screenshots are not live ads");
        check(!CanvasSkipVisual.context(null,true,nodes),"known current owner required");
        check(!CanvasSkipVisual.context("any.app",true,new ArrayList<AdMatcher.Node>()),"empty tree rejected");
        n.password=true;check(!CanvasSkipVisual.context("any.app",true,nodes),"sensitive page rejected");n.password=false;
        n.editable=true;check(!CanvasSkipVisual.context("any.app",true,nodes),"chat or input page rejected");n.editable=false;
        n.className="ImagePreview";check(!CanvasSkipVisual.context("any.app",true,nodes),"photo preview cannot authorize a screenshot tap");n.className="";
        n.text="广告";check(CanvasSkipVisual.nativeAd(nodes),"exact visible native ad evidence");n.visible=false;check(!CanvasSkipVisual.nativeAd(nodes),"hidden evidence rejected");n.visible=true;n.text="广告设置";check(!CanvasSkipVisual.nativeAd(nodes),"unrelated settings rejected");
        check(!CanvasSkipVisual.detect(new int[720*1602],720,1602,true).found(),"native evidence without glyphs cannot touch");
        check(!CanvasSkipVisual.detect(null,720,1602,false).found(),"invalid pixels rejected");
        CanvasSkipVisual.Proof proof=new CanvasSkipVisual.Proof();BiliVisual.Result word=new BiliVisual.Result(620,90,24,"current");
        check(!proof.confirm("any.first.app",1,0,720,1602,word,1000,1040),"first frame never authorizes touch");
        check(!proof.confirm("any.first.app",1,0,720,1602,word,1000,1090),"same screenshot cannot count twice");
        check(proof.confirm("any.first.app",1,0,720,1602,word,1350,1390),"two fresh frames can confirm any current owner");
        for(int mode=0;mode<8;mode++){proof.reset();proof.confirm("any.first.app",1,0,720,1602,word,1000,1040);BiliVisual.Result next=mode==5?new BiliVisual.Result(650,90,24,"moved"):word;
            check(!proof.confirm(mode==0?"any.second.app":"any.first.app",mode==1?2:1,mode==2?1:0,mode==3?721:720,mode==4?1603:1602,next,mode==6?2400:1350,mode==7?2000:mode==6?2440:1390),"changed owner/window/display/size/word/gap/stale proof rejected");}
        for(String path:args){BufferedImage src=ImageIO.read(new File(path));boolean suffix=path.contains("9733");for(int[] size:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}){int w=size[0],h=size[1];BufferedImage im=new BufferedImage(w,h,1);Graphics2D g=im.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(src,0,0,w,h,null);g.dispose();long start=System.nanoTime();BiliVisual.Result r=detect(im,false);System.out.println(new File(path).getName()+" "+w+"x"+h+" "+r.found()+" @"+r.x+","+r.y+" "+r.reason+" "+(System.nanoTime()-start)/1000000+"ms");
            check(r.found(),"both drawn orders work without SDK or native ad label");check(r.x>w*.78&&r.x<w*.96&&r.y<h*.23,"word geometry follows current upper-right control");
            int x0=Math.round((r.x-r.size*.9f)*575/w),y0=Math.round((r.y-r.size*.45f)*1280/h),sw=Math.max(8,Math.round(r.size*.85f*575/w)),sh=Math.max(10,Math.round(r.size*.9f*1280/h));
            for(int part=0;part<2;part++){BufferedImage missing=copy(im);g=missing.createGraphics();g.setColor(new Color(30,30,30));int gw=Math.round(r.size*1.15f);g.fillRect(part==0?r.x-gw:r.x,r.y-Math.round(r.size*.65f),gw,Math.round(r.size*1.3f));g.dispose();BiliVisual.Result absent=detect(missing,false);check(!absent.found(),"each full skip character is mandatory, part="+part+", original="+r.x+","+r.y+","+r.size+", remaining="+absent.x+","+absent.y);}
            for(int[] rect:suffix?new int[][]{{494,43,11,41},{507,43,22,41},{490,1236,27,30},{516,1234,24,34},{540,1234,25,34}}:new int[][]{{498,185,8,39},{482,185,15,39},{497,1240,26,30},{522,1238,17,30},{539,1238,18,30}}){BufferedImage missing=copy(im);erase(missing,rect[0],rect[1],rect[2],rect[3]);check(!detect(missing,false).found(),"separator/digit/icon/each ad glyph mandatory without native evidence "+java.util.Arrays.toString(rect));}
            for(int digit=0;digit<=9;digit++){BufferedImage countdown=copy(im);erase(countdown,suffix?507:482,suffix?43:185,suffix?22:15,39);g=countdown.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round((suffix?20:18)*w/575f)));g.drawString(Integer.toString(digit),Math.round((suffix?512:485)*w/575f),Math.round((suffix?74:213)*h/1280f));g.dispose();check(detect(countdown,false).found(),"changing countdown digit "+digit);}
            for(String other:new String[]{"领取","登录"}){BufferedImage text=copy(im);erase(text,suffix?507:457,suffix?43:185,suffix?65:40,39);g=text.createGraphics();g.setColor(Color.WHITE);g.setFont(new Font("Microsoft YaHei",Font.PLAIN,Math.round((suffix?20:18)*w/575f)));g.drawString(other,Math.round((suffix?512:458)*w/575f),Math.round((suffix?74:213)*h/1280f));g.dispose();check(!detect(text,false).found(),"neighboring words are not an isolated countdown");}
            BufferedImage noAd=copy(im);erase(noAd,480,1225,95,55);check(!detect(noAd,false).found()&&detect(noAd,true).found(),"bitmap ad evidence can be replaced only by independently current native evidence");
            BufferedImage body=copy(im);erase(body,0,300,575,850);check(detect(body,false).found(),"creative is not a template");
        }}
        System.out.println("PASS: "+checks+" generic drawn skip checks");
    }
}
