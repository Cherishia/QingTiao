package dev.local.qingtiao;

import java.util.List;

/** Bili launch context: a UI-only monochrome wordmark, swipe chevron and live footer skip. */
public final class BiliLaunchVisual {
    private static final long[] MARK={0x000000000000L,0x00000c000008L,0x00000e00001eL,0x00000f00001eL,0x07000f07001eL,0x07000e06001eL,0x07000e06001eL,0x07000e06001eL,0x66e00e6ee01cL,0x76e00eeee01cL,0x76e00eeee01cL,0x06001c0e001cL,0x26401c4c801cL,0x76e01cedc01cL,0x76e01cedc03cL,0x76c3fcedc7fcL,0x7edffceddff8L,0x6fff7cedff78L,0x6dfe7cedbc78L,0x6ddf7ced9f78L,0x658ff8e99ff8L,0x6003f84007f8L,0x0000f80000f8L,0x000000000000L};
    private static final long MASK=(1L<<48)-1;
    private static final long[] NEAR=new long[24];
    private static final int COUNT;
    static {int n=0;for(int y=0;y<24;y++){
        long row=MARK[y];n+=Long.bitCount(row);if(y>0)row|=MARK[y-1];if(y<23)row|=MARK[y+1];
        NEAR[y]=(row|row<<1|row>>>1)&MASK;
    }COUNT=n;}
    public static boolean context(String pkg,boolean startup,boolean finished,List<AdMatcher.Node> nodes){
        return "tv.danmaku.bili".equals(pkg)&&!finished&&CanvasSkipVisual.context(pkg,startup,nodes);
    }
    private static double markScore(int[] p,int w,int x,int y,int tw,int th,long[] rows,int level){
        for(int sy=0;sy<24;sy++){
            long row=0;for(int sx=0;sx<48;sx++)if(FooterSkipVisual.white(p[(y+(2*sy+1)*th/48)*w+x+(2*sx+1)*tw/96])>=level)row|=1L<<sx;
            rows[sy]=row;
        }
        int count=0,precise=0,expected=0,exact=0;
        for(int sy=0;sy<24;sy++){
            long row=rows[sy],near=row;if(sy>0)near|=rows[sy-1];if(sy<23)near|=rows[sy+1];
            near=(near|near<<1|near>>>1)&MASK;
            count+=Long.bitCount(row);precise+=Long.bitCount(row&NEAR[sy]);expected+=Long.bitCount(near&MARK[sy]);exact+=Long.bitCount(row&MARK[sy]);
        }
        if(count<COUNT*.65||count>COUNT*1.55||2.0*exact/(count+COUNT)<.74)return -1;
        return Math.min(precise/(double)count,expected/(double)COUNT);
    }
    static boolean mark(int[] p,int w,int h){
        long[] rows=new long[24];int tried=0;
        for(int level:new int[]{160,220})for(FooterSkipVisual.Box box:FooterSkipVisual.components(p,w,(int)(w*.015),(int)(h*.88),(int)(w*.27),(int)(h*.98),level)){
            if(box.l>w*.12||box.h()<w*.045||box.h()>w*.09||box.w()<box.h()*.3||box.w()>box.h()*.9)continue;
            if(++tried>12)return false;
            for(int th=Math.max(16,box.h());th<=box.h()*1.14;th+=2){
                int tw=Math.round(th*96f/46);
                for(int dy=0;dy<=Math.max(2,th*.12);dy+=2)for(int dx=0;dx<=Math.max(2,th*.12);dx+=2){
                    int x=box.l-dx,y=box.t-dy;if(x<0||y<0||x+tw>=w||y+th>=h)continue;
                    if(markScore(p,w,x,y,tw,th,rows,level)>=.90)return true;
                }
            }
        }return false;
    }
    private static int light(int c){return ((c>>16&255)*77+(c>>8&255)*150+(c&255)*29)>>8;}
    private static boolean contour(int[] p,int w,int cx,int top,int radius,int rise){
        int yes=0;
        for(int side:new int[]{-1,1})for(int bin=1;bin<=4;bin++){
            int x=cx+side*radius*bin/5,y=top+rise*bin/5,peak=0,offset=Math.max(4,w/120);
            for(int dy=-1;dy<=1;dy++)peak=Math.max(peak,light(p[(y+dy)*w+x]));
            int above=light(p[(y-offset)*w+x]),below=light(p[(y+offset)*w+x]);
            if(peak-above>=4&&peak-below>=4&&peak*2-above-below>=18)yes++;
        }
        return yes>=7;
    }
    private static boolean contrastChevrons(int[] p,int w,int h){
        // A pale video can merge the alpha-blended arrows with its white backdrop. Require
        // two complete, stacked contours, each brighter than both local sides, in the UI region.
        for(int radius=Math.max(12,(int)(w*.025));radius<=w*.036;radius+=2)
            for(int cx=(int)(w*.475);cx<=w*.525;cx+=2)for(int top=(int)(h*.745);top<=h*.78;top+=2){
                int rise=Math.round(radius*.8f);if(!contour(p,w,cx,top,radius,rise))continue;
                for(int gap=Math.max(8,(int)(w*.020));gap<=w*.032;gap+=2)if(contour(p,w,cx,top+gap,radius,rise))return true;
            }
        return false;
    }
    static boolean chevron(int[] p,int w,int h){
        for(int level:new int[]{160,220})for(FooterSkipVisual.Box box:FooterSkipVisual.components(p,w,(int)(w*.40),(int)(h*.72),(int)(w*.60),(int)(h*.83),level)){
            int bw=box.w(),bh=box.h();
            if(bw<w*.045||bw>w*.09||bh<w*.012||bh>w*.065||bw<bh*.90||bw>bh*2.8||box.count<bw*bh*.10||box.count>bw*bh*.65)continue;
            int near=0,doubleNear=0,total=0;int[] bins=new int[4],doubleBins=new int[8];
            for(int y=box.t;y<box.b;y++)for(int x=box.l;x<box.r;x++)if(FooterSkipVisual.white(p[y*w+x])>=level){
                double xx=(x-box.l+.5)*2/bw-1,yy=(y-box.t+.5)/bh;total++;
                if(Math.abs(yy-Math.abs(xx))<=.35){near++;bins[Math.min(3,(x-box.l)*4/bw)]++;}
                double first=Math.abs(yy-Math.abs(xx)*.5),second=Math.abs(yy-(.5+Math.abs(xx)*.5));
                if(Math.min(first,second)<=.18){doubleNear++;doubleBins[(first<second?0:4)+Math.min(3,(x-box.l)*4/bw)]++;}
            }
            if(bw>=bh*1.5&&bh<=w*.045&&near>=total*.85&&bins[0]>0&&bins[1]>0&&bins[2]>0&&bins[3]>0)return true;
            boolean complete=true;for(int count:doubleBins)if(count==0)complete=false;
            if(bw<=bh*1.8&&doubleNear>=total*.85&&complete)return true;
        }return contrastChevrons(p,w,h);
    }
    public static BiliVisual.Result detect(int[] p,int w,int h){
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("尺寸不适用");
        if(!mark(p,w,h))return none("当前未确认左下角白色平台字标，不执行全页搜索");
        if(!chevron(p,w,h))return none("当前未确认独立向上滑动箭头");
        BiliVisual.Result word=FooterSkipVisual.detect(p,w,h);
        return word.found()?new BiliVisual.Result(word.x,word.y,word.size,"开屏白色平台字标、向上箭头与"+word.reason,true):none(word.reason);
    }
    private static BiliVisual.Result none(String why){return new BiliVisual.Result(0,0,0,"B站无原生标签开屏："+why);}
    private BiliLaunchVisual(){}
}
