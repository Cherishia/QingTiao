package dev.local.qingtiao;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import javax.imageio.ImageIO;

public final class MiniProgramSkipVisualTest {
    private static int checks;
    private static void check(boolean passed,String message){
        checks++;
        if(!passed)throw new AssertionError(message);
    }

    private static int[] fixture()throws Exception{
        int w=575,h=1280;
        BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(new Color(110,120,130));g.fillRect(0,0,w,h);
        g.setColor(new Color(65,73,82));g.fillRoundRect(16,72,168,48,48,48);g.fillRoundRect(424,72,138,48,48,48);
        g.setColor(Color.WHITE);
        for(int x:new int[]{448,458,468})g.fillOval(x-3,92,7,7);
        g.fillOval(515,82,27,27);
        g.setColor(new Color(65,73,82));g.fillOval(518,85,21,21);
        g.setColor(Color.WHITE);g.fillOval(521,89,14,13);g.dispose();
        Field reference=MiniProgramSkipVisual.class.getDeclaredField("DISCLOSURE");reference.setAccessible(true);
        long[] strokes=(long[])reference.get(null);
        for(int y=0;y<strokes.length;y++)for(int x=0;x<64;x++)
            if((strokes[y]&(1L<<x))!=0)image.setRGB(33+x,85+y,Color.WHITE.getRGB());
        float[] word=SkipGlyphs.reference(0,40,20);
        for(int y=0;y<20;y++)for(int x=0;x<40;x++){
            int value=(int)word[y*40+x];image.setRGB(121+x,86+y,0xff000000|value<<16|value<<8|value);
        }
        return image.getRGB(0,0,w,h,null,0,w);
    }

    private static void erase(int[] p,int w,int x,int y,int width,int height){
        for(int row=y;row<y+height;row++)Arrays.fill(p,row*w+x,row*w+x+width,0xff414952);
    }

    private static void checkDistinctEvidence(int[] p,int w,int h){
        for(int[] area:new int[][]{{32,82,22,26},{54,82,23,26},{80,89,18,15},
            {120,82,22,26},{143,82,22,26},{441,87,32,16},{513,81,32,29}}){
            int[] changed=p.clone();
            erase(changed,w,area[0]*w/575,area[1]*w/575,(area[2]*w+574)/575,(area[3]*w+574)/575);
            check(!MiniProgramSkipVisual.detect(changed,w,h).found(),"missing proof at x="+area[0]);
        }
    }

    private static void replay(File file)throws Exception{
        BufferedImage source=ImageIO.read(file);
        for(int w:new int[]{480,575,720,1200}){
            int h=w==720?1602:w==1200?2670:Math.round(w*source.getHeight()/(float)source.getWidth());
            BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source,0,0,w,h,null);g.dispose();
            int[] p=image.getRGB(0,0,w,h,null,0,w);
            BiliVisual.Result hit=MiniProgramSkipVisual.detect(p,w,h);
            check(hit.found(),"captured mini-program ad at "+w+"x"+h+" "+hit.reason);
            int touchX=WmSplashVisual.touchX(hit,false);
            check(touchX/(double)w>=.232&&touchX/(double)w<=.285&&Math.abs(hit.y/(double)h-.074)<.009,
                "calibrated tap lies inside the left skip text");
            checkDistinctEvidence(p,w,h);
            System.out.println("Replay "+w+"x"+h+" => skip touch "+touchX+","+hit.y);
        }
    }

    public static void main(String[] args)throws Exception{
        int w=575,h=1280;
        int[] p=fixture();
        BiliVisual.Result hit=MiniProgramSkipVisual.detect(p,w,h);
        check(hit.found()&&hit.requiresPair&&hit.adLabelX<hit.x,"all mini-program elements with independent ad anchor "+hit.reason);
        checkDistinctEvidence(p,w,h);
        check(!MiniProgramSkipVisual.detect(p,h,w).found(),"landscape image rejected");
        AdMatcher.Node root=new AdMatcher.Node(-1,0,0,w,h,"","");
        AdMatcher.Node backgroundInput=new AdMatcher.Node(0,20,1100,w,1180,"","");
        backgroundInput.editable=true;
        java.util.List<AdMatcher.Node> nodes=Arrays.asList(root,backgroundInput);
        String owner=MiniProgramSkipVisual.WECHAT;
        check(MiniProgramSkipVisual.context(owner,true,nodes,w,h),"background input does not invalidate the separate top overlay");
        check(!MiniProgramSkipVisual.context(owner,false,nodes,w,h),"an expired startup window never opens the path");
        check(!MiniProgramSkipVisual.context("example.chat",true,nodes,w,h),"host must be the confirmed WeChat window");
        root.desc="查看原图";
        check(!MiniProgramSkipVisual.context(owner,true,nodes,w,h),"photo preview rejected");root.desc="";
        root.password=true;
        check(!MiniProgramSkipVisual.context(owner,true,nodes,w,h),"password screen rejected");root.password=false;
        backgroundInput.top=80;
        check(!MiniProgramSkipVisual.context(owner,true,nodes,w,h),"editable control in the touch area rejected");
        CanvasSkipVisual.Proof pair=new CanvasSkipVisual.Proof();
        check(!pair.confirm(owner,37908,0,w,h,hit,1000,1040),"first frame does not click");
        check(!pair.confirm(owner,37911,0,w,h,hit,1350,1390),"changed window cannot reuse the previous frame");
        check(pair.confirm(owner,37911,0,w,h,hit,1700,1740),"two fresh frames in the same window confirm the target");
        for(String arg:args)replay(new File(arg));
        System.out.println("PASS: "+checks+" mini-program layout, touch, and window checks");
    }
}
