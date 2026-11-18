package dev.local.qingtiao;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Random;

/** Differential check against the original per-pixel math, including character-edge isolation. */
public final class VolunteerInkTest {
    private static Object field(Object o,String name)throws Exception {
        Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);
    }
    private static boolean white(int p) {
        int r=p>>16&255,g=p>>8&255,b=p&255;
        return Math.min(r,Math.min(g,b))>=170&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=65;
    }
    private static double reference(int[] p,int w,int x,int y,int tw,int th,Object mask,boolean fine)throws Exception {
        int mw=(Integer)field(mask,"w"),mh=(Integer)field(mask,"h");
        boolean[] ink=(boolean[])field(mask,"ink"),near=(boolean[])field(mask,"near"),actual=new boolean[mw*mh];
        int[] expected=(int[])field(mask,"count"),count=new int[2];
        for(int sy=0;sy<mh;sy++)for(int sx=0;sx<mw;sx++) {
            int px=x+Math.min(tw-1,(int)((sx+.5)*tw/mw)),py=y+Math.min(th-1,(int)((sy+.5)*th/mh));
            if(white(p[py*w+px])){actual[sy*mw+sx]=true;count[sx/(mw/2)]++;}
        }
        double worst=1;
        for(int part=0;part<2;part++) {
            if(count[part]<expected[part]*.40||count[part]>mw*mh*.38)return -1;
            int expectedHit=0,actualHit=0,exact=0;
            for(int sy=0;sy<mh;sy++)for(int sx=part*mw/2;sx<(part+1)*mw/2;sx++) {
                int at=sy*mw+sx;if(actual[at]&&near[at])actualHit++;if(actual[at]&&ink[at])exact++;
                if(!ink[at])continue;boolean found=false;
                for(int dy=-1;dy<=1&&!found;dy++)for(int dx=-1;dx<=1;dx++) {
                    int px=sx+dx,py=sy+dy;
                    if(px>=part*mw/2&&px<(part+1)*mw/2&&py>=0&&py<mh&&actual[py*mw+px]){found=true;break;}
                }
                if(found)expectedHit++;
            }
            double recall=expectedHit/(double)expected[part],precision=actualHit/(double)count[part];
            if(fine&&2.0*exact/(expected[part]+count[part])<.45)return -1;
            if(recall<(fine?.88:.55)||precision<(fine?.80:.45))return -1;
            double f1=2*recall*precision/(recall+precision),overlap=2.0*exact/(expected[part]+count[part]);
            worst=Math.min(worst,fine?f1:f1*.60+overlap*.40);
        }
        return worst;
    }
    public static void main(String[] args)throws Exception {
        Class<?> c=VolunteerVisual.class,frameType=Class.forName(c.getName()+"$InkFrame"),maskType=Class.forName(c.getName()+"$Mask");
        Constructor<?> frame=frameType.getDeclaredConstructor(int[].class,int.class,int.class);frame.setAccessible(true);
        Method optimized=c.getDeclaredMethod("ink",frameType,int.class,int.class,int.class,int.class,maskType,boolean.class);optimized.setAccessible(true);
        Random random=new Random(5718);int checks=0,accepted=0;int[] pixels=new int[240*480];
        for(boolean fine:new boolean[]{false,true}) {
            Field f=c.getDeclaredField(fine?"FINE":"COARSE");f.setAccessible(true);Object mask=f.get(null);
            int mw=(Integer)field(mask,"w"),mh=(Integer)field(mask,"h");boolean[] ink=(boolean[])field(mask,"ink");
            for(int i=0;i<256;i++) {
                Arrays.fill(pixels,0xff304060);int tw=12+random.nextInt(65),th=9+random.nextInt(29),x=150,y=12+random.nextInt(50);
                for(int py=0;py<th;py++)for(int px=0;px<tw;px++) {
                    boolean expected=ink[Math.min(mh-1,py*mh/th)*mw+Math.min(mw-1,px*mw/tw)];
                    int color=expected?0xffeeeeee:0xff304060;
                    if(i%4==0)color=random.nextInt(4)==0?0xffeeeeee:0xff304060;
                    else if(random.nextInt(20)==0)color=new int[]{0xffa9a9a9,0xffaaaaaa,0xffaacceb,0xffaaddec,0xff000000,0xffffffff}[random.nextInt(6)];
                    pixels[(y+py)*240+x+px]=color;
                }
                Object current=frame.newInstance(pixels,240,480);
                // Reuse the same scratch rows in alternating sizes as the production search does.
                for(int repeat=0;repeat<2;repeat++) {
                    int height=th-repeat;double a=reference(pixels,240,x,y,tw,height,mask,fine);
                    double b=(Double)optimized.invoke(null,current,x,y,tw,height,mask,fine);
                    if(Double.doubleToLongBits(a)!=Double.doubleToLongBits(b))throw new AssertionError("ink score changed: "+a+" vs "+b);
                    checks++;if(a>=0)accepted++;
                }
            }
        }
        if(accepted<50)throw new AssertionError("differential fixtures did not exercise positive scores");
        System.out.println("PASS: "+checks+" equivalent ink scores ("+accepted+" valid scores)");
    }
}
