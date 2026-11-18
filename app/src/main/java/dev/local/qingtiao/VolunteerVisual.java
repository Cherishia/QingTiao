package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;

/** Verified app/canvas plus current ring and bidirectional two-character ink proof. */
public final class VolunteerVisual {
    public static boolean applies(String pkg){return "com.zzw.october".equals(pkg);}
    public static boolean context(String pkg,boolean startup,List<AdMatcher.Node> nodes) {
        if(!applies(pkg)||!startup||AdMatcher.sensitive(nodes))return false;
        for(AdMatcher.Node n:nodes)if(n.visible&&(n.className.contains("WebView")||n.className.contains("SurfaceView")
            ||n.className.contains("TextureView")||n.className.contains("Flutter")))return true;
        return false;
    }
    private static final class Mask {
        int w,h;boolean[] ink,near;long[] rows,nearRows;int[] count=new int[2];
        Mask(int w,int h,float[] values,int threshold) {
            this.w=w;this.h=h;ink=new boolean[w*h];near=new boolean[w*h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++)if(values[y*w+x]>=threshold){ink[y*w+x]=true;count[x/(w/2)]++;}
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                int part=x/(w/2);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int sx=x+dx,sy=y+dy;
                    if(sx>=part*w/2&&sx<(part+1)*w/2&&sy>=0&&sy<h&&ink[sy*w+sx])near[y*w+x]=true;
                }
            }
            rows=new long[h];nearRows=new long[h];
            for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
                if(ink[y*w+x])rows[y]|=1L<<x;
                if(near[y*w+x])nearRows[y]|=1L<<x;
            }
        }
    }
    // Consensus of the two UI glyphs across four recording frames; no background pixels.
    private static final String[] INK={
        "0000000000000000000000000000000000000000",
        "0011000001010000000010000000000110000000",
        "1111100011010000000001000000000110000000",
        "1001110011011000000001100000000111000000",
        "1001111011011010000000100111111111100000",
        "1001101111011110000000000000000110000000",
        "1111100111011100000000000000000110000000",
        "1011000011011000000001000010000110000000",
        "1010000011011100000001100001000110000000",
        "1011111111011110000001100001000110000000",
        "1011011011011000000001100000000110000000",
        "1010000010010000000001100000000110000000",
        "1010000010011000000001100000000110000000",
        "1010000110011000100001100000111100000000",
        "1011110110011001100011110000000000000000",
        "1111111100011111100110011111111111100000",
        "0000011000001111000100001111111111100000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
        "0000000000000000000000000000000000000000",
    };
    private static Mask mask(int w,int h) {
        float[] values=new float[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            float sum=0;for(int dy=0;dy<20/h;dy++)for(int dx=0;dx<40/w;dx++)if(INK[y*(20/h)+dy].charAt(x*(40/w)+dx)=='1')sum+=255;
            values[y*w+x]=sum/((20/h)*(40/w));
        }
        return new Mask(w,h,values,w==40?127:110);
    }
    private static final Mask COARSE=mask(20,10),FINE=mask(40,20);
    private static final double[] NX=new double[32],NY=new double[32];
    static {for(int i=0;i<32;i++){NX[i]=Math.cos(i*Math.PI/16);NY[i]=Math.sin(i*Math.PI/16);}}
    private static int pixel(int[] p,int w,int h,double x,double y) {
        int ix=(int)Math.round(x),iy=(int)Math.round(y);return ix<0||iy<0||ix>=w||iy>=h?-1:p[iy*w+ix]&0xffffff;
    }
    private static int difference(int a,int b){return Math.abs((a>>16&255)-(b>>16&255))+Math.abs((a>>8&255)-(b>>8&255))+Math.abs((a&255)-(b&255));}
    private static boolean border(int p) {
        int r=p>>16&255,g=p>>8&255,b=p&255;
        return Math.max(r,Math.max(g,b))>=145&&(Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<90||g-r>10&&b-r>10);
    }
    private static double ring(int[] p,int w,int h,double cx,double cy,double radius,boolean coarse) {
        double scale=Math.max(.8,w/592.0),sum=0;int hits=0,misses=0;int[] q=new int[4];
        for(int i=0;i<32;i+=coarse?2:1) {
            double best=0;
            for(int k=-2;k<=2;k++) {
                double r=radius+k*scale*.75;
                int b=pixel(p,w,h,cx+NX[i]*r,cy+NY[i]*r),a=pixel(p,w,h,cx+NX[i]*(r-2.7*scale),cy+NY[i]*(r-2.7*scale)),c=pixel(p,w,h,cx+NX[i]*(r+2.7*scale),cy+NY[i]*(r+2.7*scale));
                if(b<0||a<0||c<0||!border(b))continue;best=Math.max(best,Math.min(difference(b,a),difference(b,c)));
            }
            if(best>=(coarse?45:55)){hits++;q[i/8]++;sum+=best;}else if(++misses>(coarse?7:14))return -1;
        }
        if(hits<(coarse?9:18))return -1;for(int n:q)if(n<(coarse?1:2))return -1;
        return hits*100+sum/hits;
    }
    private static boolean white(int p) {
        int r=p>>16&255,g=p>>8&255,b=p&255;
        return Math.min(r,Math.min(g,b))>=170&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=65;
    }
    private static final class InkFrame {
        final int width;final byte[] white;final long[] actual=new long[20];
        final int[][] coarseX,fineX;
        InkFrame(int[] pixels,int w,int h) {
            width=w;white=new byte[pixels.length];coarseX=new int[w+1][];fineX=new int[w+1][];
            // All word proposals are bounded by the upper ring search plus its radius.
            int end=Math.min(h,(int)Math.ceil(h*.16+w*.12+12))*w;
            for(int i=0;i<end;i++)if(VolunteerVisual.white(pixels[i]))white[i]=1;
        }
        int[] columns(int width,Mask mask) {
            int[][] cache=mask.w==20?coarseX:fineX;int[] result=cache[width];
            if(result==null) {
                result=new int[mask.w];for(int x=0;x<mask.w;x++)result[x]=(2*x+1)*width/(2*mask.w);
                cache[width]=result;
            }
            return result;
        }
    }
    private static double ink(InkFrame frame,int x,int y,int tw,int th,Mask m,boolean fine) {
        long[] actual=frame.actual;int[] columns=frame.columns(tw,m);
        for(int sy=0;sy<m.h;sy++) {
            int row=(y+(2*sy+1)*th/(2*m.h))*frame.width+x;long bits=0;
            for(int sx=0;sx<m.w;sx++)if(frame.white[row+columns[sx]]!=0)bits|=1L<<sx;
            actual[sy]=bits;
        }
        double worst=1;
        for(int part=0;part<2;part++) {
            long partMask=((1L<<(m.w/2))-1)<<(part*m.w/2);
            int count=0,expectedHit=0,actualHit=0,exact=0;
            for(int sy=0;sy<m.h;sy++) {
                long bits=actual[sy]&partMask,near=bits;
                if(sy>0)near|=actual[sy-1]&partMask;
                if(sy+1<m.h)near|=actual[sy+1]&partMask;
                near=(near|near<<1|near>>>1)&partMask;
                count+=Long.bitCount(bits);actualHit+=Long.bitCount(bits&m.nearRows[sy]);
                exact+=Long.bitCount(bits&m.rows[sy]);expectedHit+=Long.bitCount(near&m.rows[sy]);
            }
            if(count<m.count[part]*.40||count>m.w*m.h*.38)return -1;
            double recall=expectedHit/(double)m.count[part],precision=actualHit/(double)count;
            if(fine&&2.0*exact/(m.count[part]+count)<.45)return -1;
            if(recall<(fine?.88:.55)||precision<(fine?.80:.45))return -1;
            double f1=2*recall*precision/(recall+precision),overlap=2.0*exact/(m.count[part]+count);
            worst=Math.min(worst,fine?f1:f1*.60+overlap*.40);
        }
        return worst;
    }
    private static final class Circle {double x,y,r,score;Circle(double x,double y,double r,double s){this.x=x;this.y=y;this.r=r;score=s;}}
    private static final class Word {int x,y,w,h;double score;Word(int x,int y,int w,int h,double s){this.x=x;this.y=y;this.w=w;this.h=h;score=s;}}
    public static BiliVisual.Result detect(int[] p,int w,int h) {
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("画面尺寸不适用");
        int step=Math.max(3,w/144),radiusStep=Math.max(2,w/240);ArrayList<Circle> circles=new ArrayList<>();
        for(int radius=Math.max(10,(int)(w*.018));radius<=w*.080;radius+=radiusStep)
            for(int y=(int)(h*.018);y<h*.16;y+=step)for(int x=(int)(w*.80);x<w*.99;x+=step) {
                double score=ring(p,w,h,x,y,radius,true);if(score<0)continue;int at=-1;
                for(int i=0;i<circles.size();i++)if(Math.abs(x-circles.get(i).x)<w*.025&&Math.abs(y-circles.get(i).y)<h*.012){at=i;break;}
                if(at<0) {
                    if(circles.size()<32)circles.add(new Circle(x,y,radius,score));
                    else {int weakest=0;for(int j=1;j<circles.size();j++)if(circles.get(j).score<circles.get(weakest).score)weakest=j;
                        if(score>circles.get(weakest).score)circles.set(weakest,new Circle(x,y,radius,score));}
                }
                else if(score>circles.get(at).score)circles.set(at,new Circle(x,y,radius,score));
            }
        circles.sort((a,b)->Double.compare(b.score,a.score));BiliVisual.Result result=null;double best=-1,maxInk=-1;int wordChecks=0;
        InkFrame inkFrame=circles.isEmpty()?null:new InkFrame(p,w,h);
        for(int i=0;i<Math.min(8,circles.size());i++) {
            Circle c=circles.get(i);ArrayList<Word> words=new ArrayList<>();int unit=Math.max(1,(int)Math.round(c.r/10));
            for(int th=Math.max(9,(int)(c.r*.40));th<=c.r*.90;th+=2)for(int aspect=16;aspect<=24;aspect+=2) {
                int tw=Math.round(th*aspect/10f);if(tw>c.r*1.75)continue;
                for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                    int x=(int)Math.round(c.x-tw/2.0)+dx*unit,y=(int)Math.round(c.y-th/2.0)+dy*unit;
                    if(x<0||y<0||x+tw>w||y+th>h)continue;
                    double s=ink(inkFrame,x,y,tw,th,COARSE,false);if(s>=.66)words.add(new Word(x,y,tw,th,s));
                }
            }
            words.sort((a,b)->Double.compare(b.score,a.score));Word word=null;
            for(int j=0;j<Math.min(4,words.size());j++) {
                Word s=words.get(j);
                for(int dh=-2;dh<=2;dh++)for(int dw=-3;dw<=3;dw++)for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++) {
                    int x=s.x+dx,y=s.y+dy,tw=s.w+dw,th=s.h+dh;if(x<0||y<0||tw<12||th<9||x+tw>w||y+th>h)continue;
                    double score=ink(inkFrame,x,y,tw,th,FINE,true);wordChecks++;maxInk=Math.max(maxInk,score);
                    if(score>=.93&&(word==null||score>word.score))word=new Word(x,y,tw,th,score);
                }
            }
            if(word==null)continue;
            for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)for(int dr=-2;dr<=2;dr++) {
                double cx=c.x+dx,cy=c.y+dy,radius=c.r+dr;double score=ring(p,w,h,cx,cy,radius,false);if(score<0)continue;score+=word.score*2000;
                if(result!=null&&(Math.abs(cx-result.x)>w*.025||Math.abs(cy-result.y)>h*.012))return none("多个圆环及跳过字形，不点击");
                if(score>best){best=score;result=new BiliVisual.Result((int)Math.round(cx),(int)Math.round(cy),(int)Math.round(radius*2),"志愿汇当前圆环与两字白色笔画双向核验通过；最低字形分数="+Math.round(word.score*100)+"%；不使用隐藏tvSkip坐标");}
            }
        }
        if(result!=null)return result;
        String detail="圆环候选="+circles.size()+"，笔画精查="+wordChecks+"，最高双字分数="+(maxInk<0?"未通过覆盖校验":Math.round(maxInk*100)+"%");
        // Retain the stricter grey/ring channel for video-like antialiasing and backgrounds.
        BiliVisual.Result legacy=CircularSkipVisual.detect(p,w,h);
        if(legacy.found())return new BiliVisual.Result(legacy.x,legacy.y,legacy.size,"志愿汇灰度与圆环通道通过；"+legacy.reason+"；原生通道："+detail);
        return none(detail+"；灰度通道也未匹配");
    }
    private static BiliVisual.Result none(String why){return new BiliVisual.Result(0,0,0,"志愿汇原生画面："+why);}
    private VolunteerVisual(){}
}
