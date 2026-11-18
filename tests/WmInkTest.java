package dev.local.qingtiao;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;

public final class WmInkTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static final WmSplashVisual.Hint HINT=new WmSplashVisual.Hint(589,233,702,275,720,1602);
    private static WmSplashVisual.Detection detect(BufferedImage im) {
        return WmSplashVisual.analyze(im.getRGB(0,0,720,1602,null,0,720),720,1602,HINT);
    }
    private static BufferedImage text(String word,int size,Color background) {
        BufferedImage im=new BufferedImage(720,1602,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
        g.setColor(background);g.fillRect(0,0,720,1602);g.setColor(new Color(240,240,240));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font("Microsoft YaHei",Font.PLAIN,size));FontMetrics metrics=g.getFontMetrics();
        g.drawString(word,662-metrics.stringWidth(word)/2,255+(metrics.getAscent()-metrics.getDescent())/2);g.dispose();return im;
    }
    private static BufferedImage fixture(float[] reference,int variant,int erase) {
        BufferedImage im=new BufferedImage(720,1602,BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();
        g.setColor(new Color(140,170,90));g.fillRect(0,0,720,1602);g.dispose();
        for(int y=0;y<24;y++)for(int x=0;x<48;x++) {
            int sx=x*40/48,sy=y*20/24;
            boolean ink=reference[sy*40+sx]>=180&&!(erase==1&&sx<20)&&!(erase==2&&sx>=20)&&erase!=3;
            int color;
            if(ink)color=0xfff5f5f5;
            else if(variant%3==0)color=((x+variant*3)/4%2==0?new Color(225,225,5):new Color(5,55,170)).getRGB();
            else if(variant%3==1)color=new Color(20+(x*47+y*23+variant*7)%180,170+(x*13+y*7)%70,5+(x*31+y*13)%80).getRGB();
            else color=new Color(140+(x*7+y*5)%90,20+(x*17+y*3)%50,5+(x*29+y*11)%70).getRGB();
            im.setRGB(638+x,243+y,color);
        }
        return im;
    }
    public static void main(String[] args)throws Exception {
        Field f=WmSplashVisual.class.getDeclaredField("FINE");f.setAccessible(true);float[] reference=(float[])f.get(null);int inkOnly=0;
        for(int variant=0;variant<6;variant++) {
            WmSplashVisual.Detection result=detect(fixture(reference,variant,0));
            System.out.println("tinted background "+variant+" found="+result.target.found()+" inkOnly="+result.requiresPair+" "+result.target.reason);
            check(result.target.found(),"same UI strokes remain recognizable over changing colored backgrounds");
            check(result.requiresPair,"all initial WM targets require two frames, including standard lettering");
            if(result.target.reason.contains("扩展字体"))inkOnly++;
            for(int erase=1;erase<=3;erase++)check(!detect(fixture(reference,variant,erase)).target.found(),"missing either character or both never qualifies against a colored pattern");
        }
        check(inkOnly>0,"fixtures exercise the independent foreground proof, not just the old grey channel");
        for(Color bg:new Color[]{new Color(145,180,100),new Color(180,80,65),new Color(125,125,125)})
            for(int size:new int[]{16,18,20})for(String label:new String[]{"关闭","确定","允许","跳转","过跳","广告","5","跳","过"})
                check(!detect(text(label,size,bg)).target.found(),"SDK label is insufficient when current pixels show another word: "+label+" size="+size);
        WmSplashVisual.PairProof proof=new WmSplashVisual.PairProof();BiliVisual.Result word=new BiliVisual.Result(662,255,24,"word");String owner="com.weilaishanhai.oopz";
        check(!proof.confirm(owner,1,0,720,1602,HINT,word,1000,1040),"new ink channel cannot click on its first frame");
        check(!proof.confirm(owner,1,0,720,1602,HINT,word,1000,1080),"duplicate screenshot cannot complete proof");
        check(proof.confirm(owner,1,0,720,1602,HINT,word,1450,1490),"independent current frames of the same SDK hint qualify");
        for(int mode=0;mode<10;mode++) {
            proof.reset();proof.confirm(owner,1,0,720,1602,HINT,word,1000,1040);
            WmSplashVisual.Hint next=mode==0?null:mode==1?new WmSplashVisual.Hint(579,233,692,275,720,1602):HINT;
            BiliVisual.Result target=mode==2?new BiliVisual.Result(650,255,24,"moved"):mode==3?new BiliVisual.Result(662,255,36,"resized"):word;
            String pkg=mode==4?"other.app":owner;int win=mode==5?2:1,display=mode==6?1:0,w=mode==7?721:720;
            long stamp=mode==8?2300:1450,now=mode==9?2100:stamp+40;
            check(!proof.confirm(pkg,win,display,w,1602,next,target,stamp,now),"changed or stale proof is rejected: "+mode);
        }
        proof.reset();check(!proof.confirm("other.sdk.app",1,0,720,1602,HINT,word,1000,1040),"another shared SDK owner still needs first observation");
        check(proof.confirm("other.sdk.app",1,0,720,1602,HINT,word,1450,1490),"same verified SDK proof supports another stable owner");
        proof.reset();proof.confirm(owner,1,0,720,1602,HINT,word,1000,1040);
        check(!proof.confirm(owner,1,0,720,1602,HINT,new BiliVisual.Result(0,0,0,"missing"),1450,1490),"missing word breaks consecutive proof");
        check(!proof.confirm(owner,1,0,720,1602,HINT,word,1900,1940),"returning word requires a fresh pair");
        System.out.println("PASS: "+checks+" WM foreground/pair checks");
    }
}
