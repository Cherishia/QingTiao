package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;

/** Local glyph template matching, not general OCR. Coordinates come from this frame. */
public final class SplashVisual {
    // A glyph-only grayscale reference for the supplied 12306 skip lettering.
    // No ad creative, screenshot, brand or fixed touch coordinates are embedded.
    private static final String[] GLYPHS={
        "979ea2aaaba8a7ababaeb7c0b6b6beb3a9a9a8aaaaaeb4b5afa7a9b1b1b1b4b2a7abc2c29c878f90",
        "96a6aba5a8afb0a4a1b1c9d7c6cbd5bdaba7a3afabafc9d3c3b4b1b3aea8a9a7a9b7d5d7ad92938d",
        "a0d8f1e7e8ece3beb0aecee1c9d3e1bca4a4a4aba8afcee8d6b9afb0b1b0b3b1afbcdbdfbba39e90",
        "39ade0d5d7e1dcb2acaecde1c5d2e2baa5b0b0a6a3a9badee0c2aeacafafafaaa6b8dbdeb59e9e93",
        "2e9ab07780c1f2d6d2bbcedfc2d0e0beb9cec4aba6a7a8c6e2d3c1c5cdcececacdd5e9ebd2c6bda3",
        "359e9d393a96eadde5cfd1dfc3d0e0c7d0ddc3aea7a5a7b3c7c4c3d9ebf3fafef5f0f9fdf2efdfb8",
        "3b9fba5d467ec0b9d9dcd2dec3d0e0cddbd5afa5a1a0aaa7aba9adc1c9c3c3c5c3cde7e8cdc4c1aa",
        "40a4ddbbbacbba7ba7e2d9dfbdcce1dde6c9aba2aac5d2ccc9beafaeb9c4c0b5aeb5dde1bcaca5a6",
        "3e679cc1e5b377486f99b5d8b8d2dfcdcabaacb1bdd5e5ebe9ccaeafc7ded7b1a9b3dce0bdaea9aa",
        "42474d88c57642304257a2cf9ec9e2c1a89e9ba8b1babfcdebd3aba9c1e4e9bcacb3dce0bdaea9ab",
        "4fa57484c3825c3c357cc8c773a7ecd9b89f99a0a7a8a5b5e2d2afa8acceebd5b3b4dce0bbaca7a9",
        "5cb17a83e7cbad8f8bcff0bd5f89e2e4dcbba89da1a8a9c0e5d2b4aba2badfd9b3b2d9ddbaaca8ab",
        "64c88580ddb190a4bdc8daaf6d87b9a4c7dac0a39aa0a2bbe3cfada6a4b0c3beadb0d8dcbaaeabaf",
        "64c17a74c07d78b8c189b6a26c8ea4657fbbbbaaa0a19fb7e2cfa7a1a5a7abadb0b8dde0bbadaaad",
        "61bf8289c46763886a6fb59c518eb4623e5a8191979f9eb8e5d6ada3a6a1a4b6bcc2e6e4bdaba5a6",
        "6ec17986cc80715f488ac276558dae613439395a88999ab3e5d9b0a7a7a1b1dae5ecead5b7a6a4a8",
        "5cbd979ae0d7ca9470bba8534891b96648817c484f88b7c7e4e4c5aca2a7b3bfd3d7cfbcaeabafb1",
        "9fe7ded0d7b07d86b0ce7e3b4086af634faab950245fbae9e7e8ecdec7b8b3afb7b4aba7abb4b7b3",
        "bdcdb28a71685fa1cc934b384889ceb9b1cda449408dcaccb8b7cce0e7e0dde1e0dddcdfe7ece3d5",
        "5d4d43311e4c8cc8a7572e363761b1c8cbab633b4b88946b819aa1b1c6d9e2e4e9eceeedede8dacb"
    };
    private static final float[] COARSE=template(20,10),FINE=template(40,20);
    public static boolean context(String pkg,boolean startup,List<AdMatcher.Node> nodes) {
        if(pkg==null||pkg.isEmpty()||!startup||AdMatcher.sensitive(nodes))return false;
        for(AdMatcher.Node n:nodes)if(n.evidence())return true;
        return false;
    }
    private static float[] template(int width,int height) {
        float[] out=new float[width*height];
        for(int y=0;y<height;y++)for(int x=0;x<width;x++) {
            int value=0,count=0;
            for(int dy=0;dy<20/height;dy++)for(int dx=0;dx<40/width;dx++) {
                int sx=x*(40/width)+dx,sy=y*(20/height)+dy;
                value+=Integer.parseInt(GLYPHS[sy].substring(sx*2,sx*2+2),16);count++;
            }
            out[y*width+x]=value/(float)count;
        }
        return out;
    }
    private static int light(int rgb){return (((rgb>>16)&255)*77+((rgb>>8)&255)*150+(rgb&255)*29)>>8;}
    private static final class Hit {
        int x,y,w,h;double score;
        Hit(int x,int y,int w,int h,double score){this.x=x;this.y=y;this.w=w;this.h=h;this.score=score;}
    }
    /** Normalized correlation for each Chinese character independently. */
    static double score(int[] pixels,int width,int left,int top,int tw,int th,float[] template,int sw,int sh) {
        double worst=1;
        for(int part=0;part<2;part++) {
            double a=0,b=0,aa=0,bb=0,ab=0;int count=sw*sh/2;
            for(int y=0;y<sh;y++)for(int x=part*sw/2;x<(part+1)*sw/2;x++) {
                int sx=left+Math.min(tw-1,(int)((x+.5)*tw/sw)),sy=top+Math.min(th-1,(int)((y+.5)*th/sh));
                double p=light(pixels[sy*width+sx]),t=template[y*sw+x];
                a+=p;b+=t;aa+=p*p;bb+=t*t;ab+=p*t;
            }
            double variance=aa-a*a/count,denominator=Math.sqrt(Math.max(0,variance*(bb-b*b/count)));
            if(variance/count<85||denominator==0)return -1;
            worst=Math.min(worst,(ab-a*b/count)/denominator);
        }
        return worst;
    }
    public static BiliVisual.Result detect(int[] pixels,int width,int height) {
        if(pixels==null||width<240||height<width*1.4||height>width*3.0||pixels.length!=(long)width*height)
            return none("画面尺寸不适用");
        // A small top-right word, below the status area; never the bottom landing CTA.
        int left=(int)(width*.73),right=(int)(width*.985),top=(int)(height*.035),bottom=(int)(height*.145);
        int min=Math.max(10,(int)(width*.027)),max=Math.max(min,(int)(width*.047));
        List<Hit> seeds=new ArrayList<>();
        for(int h=min;h<=max;h+=2) {
            int w=h*2;
            for(int y=top;y+h<=bottom;y+=2)for(int x=left;x+w<=right;x+=2) {
                double value=score(pixels,width,x,y,w,h,COARSE,20,10);
                if(value>.52) {
                    if(seeds.size()>=4096)return none("字形候选过多，未点击");
                    seeds.add(new Hit(x,y,w,h,value));
                }
            }
        }
        seeds.sort((a,b)->Double.compare(b.score,a.score));
        List<Hit> matches=new ArrayList<>();
        // Bound fine work even on textured/hostile images.
        List<Hit> searched=new ArrayList<>();
        for(Hit s:seeds) {
            boolean nearby=false;
            for(Hit previous:searched)if(Math.abs(s.x+s.w/2-previous.x-previous.w/2)<width*.018
                &&Math.abs(s.y+s.h/2-previous.y-previous.h/2)<height*.007){nearby=true;break;}
            if(nearby)continue;
            if(searched.size()>=24)return none("字形候选过多，未点击");
            searched.add(s);Hit best=null;
            for(int h=Math.max(min,s.h-1);h<=Math.min(max,s.h+1);h++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                int x=s.x+dx,y=s.y+dy,w=h*2;
                if(x<left||y<top||x+w>right||y+h>bottom)continue;
                double value=score(pixels,width,x,y,w,h,FINE,40,20);
                if(value>=.72&&(best==null||value>best.score))best=new Hit(x,y,w,h,value);
            }
            if(best!=null)matches.add(best);
        }
        if(matches.isEmpty())return none("右上角未匹配到支持的‘跳过’字形");
        Hit best=matches.get(0);
        for(Hit hit:matches)if(hit.score>best.score)best=hit;
        for(Hit hit:matches)if(Math.abs(hit.x+hit.w/2-best.x-best.w/2)>width*.025
            ||Math.abs(hit.y+hit.h/2-best.y-best.h/2)>height*.01)return none("多个不同位置的跳过字形，未点击");
        return new BiliVisual.Result(best.x+best.w/2,best.y+best.h/2,best.h,
            "右上角两字逐字匹配‘跳过’，最低相关度="+Math.round(best.score*100)+"%");
    }
    private static BiliVisual.Result none(String reason){return new BiliVisual.Result(0,0,0,reason);}
    private SplashVisual(){}
}
