package dev.local.qingtiao;

import java.util.ArrayList;

/** Corner skip glyphs. Only independently verified ad context can use the resulting geometry. */
public final class PlainSkipVisual {
    private static final class Hit {int x,y,w,h;double score;Hit(int x,int y,int w,int h,double score){this.x=x;this.y=y;this.w=w;this.h=h;this.score=score;}}
    private static boolean isolated(int[] p,int w,int h,Hit s){
        int pad=Math.max(2,s.h/6),left=0,right=0,threshold=SecondsSuffix.wordThreshold(p,w,s.x,s.y,s.w,s.h);
        for(int y=s.y;y<s.y+s.h;y++){
            for(int x=s.x+s.w+pad;x<Math.min(w,s.x+s.w+s.h);x++)if(SecondsSuffix.light(p[y*w+x])>=threshold)right++;
            for(int x=Math.max(0,s.x-s.h);x<s.x-pad;x++)if(SecondsSuffix.light(p[y*w+x])>=threshold)left++;
        }
        return left<s.h*s.h*.025&&right<s.h*s.h*.025;
    }
    public static BiliVisual.Result detect(int[] p,int w,int h){
        return detect(p,w,h,false);
    }
    static BiliVisual.Result detectFooter(int[] p,int w,int h){return detect(p,w,h,true);}
    private static boolean footerCapsule(int[] p,int w,int h,int x,int y,int tw,int th){
        int cx=x+tw/2,cy=y+th/2,inner=Math.max(1,(int)(th*.78)),outer=(int)(th*1.85);
        if(cy-outer<0||cy+outer>=h||cx-tw/3<0||cx+tw/3>=w)return false;
        for(int side=-1;side<=1;side+=2){
            int darker=0;
            for(int dx=-1;dx<=1;dx++){
                int col=cx+dx*tw/3;
                if(SecondsSuffix.light(p[(cy+side*outer)*w+col])-SecondsSuffix.light(p[(cy+side*inner)*w+col])>=14)darker++;
            }
            if(darker<2)return false;
        }
        return true;
    }
    private static BiliVisual.Result detect(int[] p,int w,int h,boolean footer){
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("尺寸不适用");
        int l=(int)(w*(footer?.72:.70)),t=(int)(h*(footer?.87:.035)),r=(int)(w*.985),bmax=(int)(h*(footer?.985:.21));
        ArrayList<FooterSkipVisual.Box> boxes=FooterSkipVisual.components(p,w,l,t,r,bmax);
        ArrayList<Hit> seeds=new ArrayList<>();
        for(FooterSkipVisual.Box b:boxes){
            if(b.h()<Math.max(9,w*.015)||b.h()>w*.05||b.w()>b.h()*1.3||b.count<b.h())continue;
            for(int th=Math.max(10,b.h());th<=Math.min(b.h()*1.4,w*.052);th+=2)for(int ratio=18;ratio<=22;ratio+=2){
                int tw=Math.round(th*ratio/10f);
                for(int part=0;part<2;part++)for(int dy=0;dy<=th*.22;dy+=2)for(int dx=0;dx<=th*.22;dx+=2){
                    int x=b.l-part*tw/2-dx,y=b.t-dy;if(x<l||x+tw>r||y<t||y+th>bmax)continue;
                    if(footer&&!footerCapsule(p,w,h,x,y,tw,th))continue;
                    double score=SkipGlyphs.coarse(p,w,x,y,tw,th);if(score>=.60)seeds.add(new Hit(x,y,tw,th,score));
                    if(seeds.size()>2048)return none("字形候选过多");
                }
            }
        }
        seeds.sort((a,b)->Double.compare(b.score,a.score));ArrayList<Hit> searched=new ArrayList<>(),found=new ArrayList<>();
        SkipGlyphs.InkWork scratch=new SkipGlyphs.InkWork();java.util.HashSet<Long> seen=new java.util.HashSet<>();Hit best=null;double highest=-1;
        for(Hit s:seeds){
            boolean near=false;for(Hit old:searched)if(Math.abs(s.x+s.w/2-old.x-old.w/2)<w*.02&&Math.abs(s.y+s.h/2-old.y-old.h/2)<h*.008){near=true;break;}
            if(near)continue;if(searched.size()>=8)return none("不同位置字形过多");searched.add(s);
            refine:for(int dh=-1;dh<=1;dh++)for(int dw=-1;dw<=1;dw++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                int x=s.x+dx,y=s.y+dy,tw=s.w+dw,th=s.h+dh;
                if(x<0||y<0||x+tw>=w||y+th>=h||!seen.add(SkipGlyphs.patchKey(x,y,tw,th)))continue;
                double score=SkipGlyphs.fine(p,w,x,y,tw,th,scratch);highest=Math.max(highest,score);
                Hit hit=new Hit(x,y,tw,th,score);if(score<.84)continue;
                if(footer&&!footerCapsule(p,w,h,x,y,tw,th))continue;
                boolean seconds=!footer&&SecondsSuffix.matches(p,w,h,x,y,tw,th);
                if(!seconds&&(score<.90||!isolated(p,w,h,hit)))continue;
                found.add(hit);if(best==null||score>best.score)best=hit;if(score>=.95)break refine;
            }
        }
        if(best==null)return none("未确认独立跳过两字，最高分="+(highest<0?"未匹配":Math.round(highest*100)+"%"));
        for(Hit s:found)if(Math.abs(s.x+s.w/2-best.x-best.w/2)>w*.02||Math.abs(s.y+s.h/2-best.y-best.h/2)>h*.008)return none("目标不唯一");
        String layout=footer?"右下独立跳过两字、深色胶囊":SecondsSuffix.matches(p,w,h,best.x,best.y,best.w,best.h)?"右上跳过两字、括号秒数轮廓及深色胶囊":"右上独立跳过两字";
        return new BiliVisual.Result(best.x+best.w/2,best.y+best.h/2,best.h,layout+"，逐字="+Math.round(best.score*100)+"%；需当前广告证据及两张新画面",true);
    }
    private static BiliVisual.Result none(String why){return new BiliVisual.Result(0,0,0,"无倒计时跳过："+why);}
    private PlainSkipVisual(){}
}
