package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public final class AdImageSkipTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static ArrayList<AdMatcher.Node> nodes(String id){
        ArrayList<AdMatcher.Node> out=new ArrayList<>();AdMatcher.Node n=new AdMatcher.Node(-1,0,0,1200,2670,"",id);n.className="android.widget.ImageView";out.add(n);return out;
    }
    private static BufferedImage scale(BufferedImage im,int w,int h,Object interpolation){BufferedImage out=new BufferedImage(w,h,1);Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,interpolation);g.drawImage(im,0,0,w,h,null);g.dispose();return out;}
    private static BufferedImage copy(BufferedImage im){return scale(im,im.getWidth(),im.getHeight(),RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);}
    private static BiliVisual.Result detect(BufferedImage im,boolean ad){int w=im.getWidth(),h=im.getHeight();return CanvasSkipVisual.detect(im.getRGB(0,0,w,h,null,0,w),w,h,ad);}
    private static void erase(BufferedImage im,int l,int t,int r,int b){Graphics2D g=im.createGraphics();g.setColor(new Color(105,105,0));g.fillRect(l,t,r-l,b-t);g.dispose();}
    private static void shape(BufferedImage im,String[] rows,int l,int t,int w,int h){
        Graphics2D g=im.createGraphics();g.setColor(new Color(235,235,235));for(int yy=0;yy<h;yy++)for(int xx=0;xx<w;xx++)if(rows[yy*rows.length/h].charAt(xx*rows[0].length()/w)=='#')g.fillRect(l+xx,t+yy,1,1);g.dispose();
    }
    private static BufferedImage fixture(){
        BufferedImage im=new BufferedImage(720,1602,1);Graphics2D g=im.createGraphics();g.setColor(new Color(220,220,180));g.fillRect(0,0,720,1602);g.setColor(new Color(60,60,0));g.fillRoundRect(585,70,120,52,40,40);g.dispose();
        float[] ref=SkipGlyphs.reference(0,40,20);for(int y=0;y<25;y++)for(int x=0;x<48;x++){int v=Math.round(ref[y*20/25*40+x*40/48]);im.setRGB(600+x,84+y,0xff000000|v<<16|v<<8|v);}
        shape(im,new String[]{"..#",".#.","#..","#..","#..","#..","#..",".#.","..#"},649,86,5,21);
        shape(im,new String[]{".#.","##.",".#.",".#.",".#.",".#.","###"},658,88,6,17);
        shape(im,new String[]{".####","##...","##...",".###.","...##","...##","####."},667,92,10,14);
        shape(im,new String[]{"#..",".#.","..#","..#","..#","..#","..#",".#.","#.."},681,86,5,21);return im;
    }
    public static void main(String[] args)throws Exception{
        for(String id:new String[]{"one.owner:id/iv_advertisement_image","two.owner:id/advertisement_image","another:id/advertisement","sdk:id/img_advertisement","sdk:id/image_advertisement_img"}){
            ArrayList<AdMatcher.Node> n=nodes(id);check(AdSurface.present(n,1200,2670)&&CanvasSkipVisual.nodeAd(n,1200,2670)&&!CanvasSkipVisual.nativeAd(n),"full-screen explicit image resource is independent evidence="+id);
            check(CanvasSkipVisual.context("any.unlisted.owner",true,n),"layout path has no application allow-list");
        }
        for(String id:new String[]{"host:id/advertisement_settings","host:id/not_advertisement_image","host:id/advertisement_image_preview","host:id/iv_advertisement_image_copy","host:id/iv_advertisement_images","host:id/iv_advertisement_imagery","host:id/iv_ad_image","advertisement.owner:id/ordinary_image"})check(!AdSurface.present(nodes(id),1200,2670),"substring/settings/preview/owner is not ad proof="+id);
        for(int bad=0;bad<10;bad++){
            ArrayList<AdMatcher.Node> n=nodes("host:id/iv_advertisement_image");AdMatcher.Node z=n.get(0);
            if(bad==0)z.visible=false;if(bad==1)z.enabled=false;if(bad==2)z.editable=true;if(bad==3)z.checkable=true;if(bad==4)z.scrollable=true;if(bad==5)z.right=700;if(bad==6)z.bottom=1000;if(bad==7)z.password=true;if(bad==8)z.className="android.widget.TextView";if(bad==9)z.left=-1;
            check(!AdSurface.present(n,1200,2670),"hidden/disabled/editable/inline/sensitive/wrong-type/offscreen rejected="+bad);
        }
        ArrayList<AdMatcher.Node> preview=nodes("host:id/iv_advertisement_image");preview.add(new AdMatcher.Node(0,0,0,1200,2670,"查看原图",null));check(!CanvasSkipVisual.context("any.app",true,preview),"even an ad-named image does not authorize a photo viewer");
        BufferedImage im=fixture();BiliVisual.Result target=detect(im,true);check(target.found()&&target.requiresPair&&target.reason.contains("括号"),"generic seconds fixture "+target.reason);
        check(!detect(im,false).found(),"the control cannot supply its own ad evidence");
        for(int part=0;part<6;part++){BufferedImage missing=copy(im);int[][] rect={{600,84,624,109},{624,84,648,109},{648,84,656,109},{657,84,665,109},{666,84,678,109},{679,84,687,109}};int[] z=rect[part];erase(missing,z[0],z[1],z[2],z[3]);check(!detect(missing,true).found(),"each glyph and countdown contour mandatory="+part);}
        String[][] digits={{".###.","#...#","#...#","#...#","#...#","#...#",".###."},{"..#..",".##..","..#..","..#..","..#..","..#..",".###."},{".###.","#...#","....#","...#.","..#..",".#...","#####"},{"####.","....#","....#",".###.","....#","....#","####."},{"#...#","#...#","#...#","#####","....#","....#","....#"},{"#####","#....","#....","####.","....#","....#","####."},{".###.","#....","#....","####.","#...#","#...#",".###."},{"#####","....#","...#.","..#..","..#..","..#..","..#.."},{".###.","#...#","#...#",".###.","#...#","#...#",".###."},{".###.","#...#","#...#",".####","....#","....#",".###."}};
        for(int value=0;value<10;value++){BufferedImage counter=copy(im);erase(counter,657,84,666,109);shape(counter,digits[value],657,88,8,17);int[] p=counter.getRGB(0,0,720,1602,null,0,720);check(SecondsSuffix.matches(p,720,1602,600,84,48,25),"single numeric contour supports countdown="+value);check(detect(counter,true).found(),"countdown change keeps shared glyph route="+value);}
        BufferedImage noCapsule=copy(im);Graphics2D g=noCapsule.createGraphics();g.setColor(new Color(220,220,180));g.fillRect(585,70,120,14);g.fillRect(585,109,120,14);g.dispose();check(!detect(noCapsule,true).found(),"seconds suffix requires capsule, not creative lettering");
        BufferedImage mirrored=copy(im);erase(mirrored,648,84,656,109);shape(mirrored,new String[]{"#..",".#.","..#","..#","..#","..#","..#",".#.","#.."},649,86,5,21);check(!detect(mirrored,true).found(),"wrong-facing bracket is not a seconds control");
        BufferedImage extra=copy(im);shape(extra,new String[]{"#","#","#","#","#"},689,88,2,17);check(!detect(extra,true).found(),"extra suffix character rejected");
        BufferedImage ambiguous=copy(im);g=ambiguous.createGraphics();g.drawImage(im.getSubimage(585,70,120,52),585,190,null);g.dispose();check(!detect(ambiguous,true).found(),"two separate upper-right controls cannot choose an arbitrary target");
        for(String path:args)actual(path);
        System.out.println("PASS: "+checks+" ad-image/parenthesized-countdown checks");
    }
    private static void actual(String path)throws Exception{
        BufferedImage src=ImageIO.read(new File(path));
        for(Object interpolation:new Object[]{RenderingHints.VALUE_INTERPOLATION_BILINEAR,RenderingHints.VALUE_INTERPOLATION_BICUBIC})for(int[] size:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}){
            BufferedImage im=scale(src,size[0],size[1],interpolation);if(size[0]>720)im=scale(im,720,Math.round(size[1]*720f/size[0]),RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w=im.getWidth(),h=im.getHeight();BiliVisual.Result r=detect(im,true);check(r.found()&&r.requiresPair&&r.reason.contains("括号"),"actual parenthesized countdown="+size[0]+" "+r.reason);
            int tx=WmSplashVisual.touchX(r,false);check(tx>w*.85&&tx<w*.90&&r.y>h*.047&&r.y<h*.068,"touch lies in current 过 glyph, never creative CTA");
            check(!detect(im,false).found(),"same picture without independent current native ad evidence rejected");
            int[] pixels=im.getRGB(0,0,w,h,null,0,w);CanvasSkipVisual.Proof proof=new CanvasSkipVisual.Proof();
            check(!proof.confirm("any.app",1,0,w,h,r,1000,1050),"actual first frame cannot touch");check(!proof.confirm("any.app",1,0,w,h,r,1000,1090),"rereading same actual pixels is not a pair");check(proof.confirm("any.app",1,0,w,h,r,1350,1400),"fresh same-source geometry can confirm");
            for(int[] rect:new int[][]{{479,66,497,88},{497,66,516,88},{516,66,523,88},{524,66,531,88},{532,66,542,88},{542,66,549,88}}){BufferedImage missing=copy(im);erase(missing,Math.round(rect[0]*w/575f),Math.round(rect[1]*h/1280f),Math.round(rect[2]*w/575f),Math.round(rect[3]*h/1280f));BiliVisual.Result miss=detect(missing,true);check(!miss.found(),"actual full character/parenthesis/digit/unit mandatory="+java.util.Arrays.toString(rect)+"/"+w+" => "+miss.reason+" @"+miss.x+","+miss.y+" size="+miss.size);}
            BufferedImage body=copy(im);erase(body,0,h/5,w,h);check(detect(body,true).found(),"ad creative and bottom CTA are not templates");
            BufferedImage moved=new BufferedImage(w,h,1);Graphics2D g=moved.createGraphics();g.setColor(new Color(100,100,0));g.fillRect(0,0,w,h);g.drawImage(im,0,h/3,null);g.dispose();check(!detect(moved,true).found(),"body text cannot become upper-right skip");
            System.out.println("actual source="+size[0]+"x"+size[1]+" analyze="+w+"x"+h+" target="+tx+","+r.y+" "+r.reason);
        }
    }
}
