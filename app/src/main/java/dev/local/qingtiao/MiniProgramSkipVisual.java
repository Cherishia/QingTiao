package dev.local.qingtiao;

import java.util.List;
import java.util.Locale;

/** A left disclosure and skip label beside a separate mini-program menu. */
final class MiniProgramSkipVisual {
    static final String WECHAT="com.tencent.mm";
    // White strokes of the two disclosure characters and their downward chevron only.
    private static final long[] DISCLOSURE={
        0x0000000000000000L,0x0000000000000000L,0x0000000180003800L,0x000000018c003800L,
        0x000000018c003c00L,0x0000007ffe0fffe0L,0x0000000182001b60L,0x0000000183000020L,
        0x0000000180000060L,0x000001ffff800060L,0x0c0200e6b4000060L,0x0e06000000000060L,
        0x070c000000000060L,0x0318007ffe000060L,0x01f0006006000020L,0x00e0006006000030L,
        0x0040006006000030L,0x0000003c0e000038L,0x0000007ffe000018L,0x0000007006000000L,
        0x0000000000000000L,0x0000000000000000L
    };
    private static final float[] FONT_SCALES={1f,.92f,1.08f};
    private static final int[] RING_X={100,71,0,-71,-100,-71,0,71};
    private static final int[] RING_Y={0,71,100,71,0,-71,-100,-71};

    static boolean context(String pkg,boolean startup,List<AdMatcher.Node> nodes,int w,int h){
        if(!WECHAT.equals(pkg)||!startup||nodes==null||nodes.isEmpty()||nodes.size()>=1000||AdMatcher.sensitive(nodes))return false;
        for(AdMatcher.Node n:nodes)if(n.visible){
            if((n.editable||n.checkable)&&n.left<w*.36&&n.right>w*.025&&n.top<h*.13&&n.bottom>h*.04)return false;
            String role=((n.className==null?"":n.className)+" "+(n.id==null?"":n.id)).toLowerCase(Locale.ROOT);
            if(role.contains("gallery")||role.contains("imagepreview")||role.contains("image_preview")||role.contains("imageviewer")
                ||role.contains("image_viewer")||"查看原图".equals(n.text)||"保存图片".equals(n.text)
                ||"查看原图".equals(n.desc)||"保存图片".equals(n.desc))return false;
        }
        return true;
    }

    static BiliVisual.Result detect(int[] pixels,int w,int h){
        if(pixels==null||w<240||h<w*1.4||h>w*3||pixels.length!=(long)w*h)return none("画面尺寸不适用");
        int row=menuRow(pixels,w,h);
        if(row<0)return none("未确认右侧三点与圆形小程序菜单");
        SkipGlyphs.InkWork scratch=new SkipGlyphs.InkWork();
        float unit=w/575f;
        for(float font:FONT_SCALES){
            float scale=unit*font;
            int height=Math.round(DISCLOSURE.length*scale),width=Math.round(64*scale);
            int center=row+Math.round(2*unit)-height/2,margin=Math.max(2,Math.round(4*scale));
            for(int y=center-margin;y<=center+margin;y++){
                for(int x=Math.round(26*unit);x<=Math.round(43*unit);x++){
                    if(!strokeMatch(pixels,w,x,y,width,height,2,23,.52)
                        ||!strokeMatch(pixels,w,x,y,width,height,23,43,.52)
                        ||!strokeMatch(pixels,w,x,y,width,height,47,63,.57))continue;
                    BiliVisual.Result hit=skip(pixels,w,x,y,height,scale,scratch);
                    if(hit.found())return hit;
                }
            }
        }
        return none("未同时确认左侧广告字样、下箭头与跳过字形");
    }

    private static BiliVisual.Result skip(int[] pixels,int w,int x,int y,int height,float scale,SkipGlyphs.InkWork scratch){
        int start=x+Math.round(88*scale),width=Math.round(41*scale),slop=Math.max(2,Math.round(4*scale));
        int bestX=0,bestY=0,bestHeight=0;double best=.85;
        for(int i=start-slop;i<=start+slop;i++)for(int j=y-1;j<=y+1;j++)
            for(int dw=-1;dw<=1;dw++)for(int dh=-2;dh<=2;dh++){
                int boxW=width+dw,boxH=height+dh;
                double score=SkipGlyphs.fine(pixels,w,i,j,boxW,boxH,scratch);
                if(score>best){best=score;bestX=i+boxW/2;bestY=j+boxH/2;bestHeight=boxH;}
            }
        return bestHeight==0?none("左侧跳过字形不匹配"):new BiliVisual.Result(bestX,bestY,bestHeight,
            "小程序广告：广告字样、下箭头、跳过两字及右侧三点圆环匹配",true,x+Math.round(21*scale),y+height/2);
    }

    private static boolean strokeMatch(int[] pixels,int w,int x,int y,int tw,int th,int start,int end,double threshold){
        int sum=0,squares=0,on=0,ink=0,count=(end-start)*DISCLOSURE.length;
        for(int i=0;i<DISCLOSURE.length;i++)for(int j=start;j<end;j++){
            int sample=Math.max(0,white(pixels[(y+(2*i+1)*th/(2*DISCLOSURE.length))*w+x+(2*j+1)*tw/128])-125);
            sum+=sample;squares+=sample*sample;
            if((DISCLOSURE[i]&(1L<<j))!=0){on++;ink+=sample;}
        }
        double covariance=(double)ink*count-(double)sum*on;
        double variance=(double)squares*count-(double)sum*sum;
        return ink>=on*24&&covariance>0&&covariance>=threshold*Math.sqrt(variance*on*(count-on));
    }

    private static int menuRow(int[] p,int w,int h){
        int left=Math.round(w*.766f),right=Math.round(w*.837f);
        int minGap=Math.max(2,Math.round(w*.010f)),maxGap=Math.round(w*.025f);
        int maxDot=Math.round(w*.023f);
        for(int y=Math.round(h*.045f);y<=h*.118;y++){
            int first=-1,second=-1;
            for(int x=left;x<right;){
                if(white(p[y*w+x])<175){x++;continue;}
                int start=x++;
                while(x<right&&white(p[y*w+x])>=175)x++;
                if(start==left||x==right||x-start>maxDot){first=second=-1;continue;}
                int dot=(start+x)/2,gap=dot-second;
                if(first>=0&&gap>=minGap&&gap<=maxGap&&second-first>=minGap&&second-first<=maxGap
                    &&Math.abs(gap-(second-first))<=w*.008){
                    int cy=ringRow(p,w,dot,y);
                    if(cy>=0)return cy;
                }
                first=second;second=dot;
            }
        }
        return -1;
    }

    private static int ringRow(int[] p,int w,int dot,int row){
        int slop=Math.max(2,Math.round(w*.004f)),minR=Math.round(w*.017f),maxR=Math.round(w*.026f);
        int left=Math.max(Math.round(w*.89f),dot+Math.round(w*.07f));
        int right=Math.min(Math.round(w*.95f),dot+Math.round(w*.13f));
        for(int cy=row-slop;cy<=row+slop;cy++)for(int cx=left;cx<=right;cx++){
            if(white(p[cy*w+cx])<175)continue;
            for(int r=minR;r<=maxR;r++){
                int matches=0;
                for(int i=0;i<RING_X.length;i++){
                    int gap=radial(p,w,cx,cy,i,(r*68+50)/100),rim=0;
                    for(int d=-1;d<=1;d++)rim=Math.max(rim,radial(p,w,cx,cy,i,r+d));
                    if(rim>=180&&rim-gap>=30)matches++;
                }
                if(matches>=6)return cy;
            }
        }
        return -1;
    }

    private static int radial(int[] pixels,int w,int x,int y,int axis,int r){
        int dx=RING_X[axis]*r,dy=RING_Y[axis]*r;
        return white(pixels[(y+Math.round(dy*.01f))*w+x+Math.round(dx*.01f)]);
    }

    private static int white(int color){return Math.min(color>>16&255,Math.min(color>>8&255,color&255));}
    private static BiliVisual.Result none(String reason){return new BiliVisual.Result(0,0,0,"小程序广告："+reason);}
    private MiniProgramSkipVisual(){}
}
