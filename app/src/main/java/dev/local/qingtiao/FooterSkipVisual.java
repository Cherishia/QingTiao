package dev.local.qingtiao;

import java.util.ArrayList;

/** Footer skip/countdown family. The word geometry always comes from current pixels. */
public final class FooterSkipVisual {
    static final class Box {
        int l,t,r,b,count;Box(int x,int y){l=x;t=y;r=x+1;b=y+1;}
        int w(){return r-l;}int h(){return b-t;}int x(){return (l+r)/2;}int y(){return (t+b)/2;}
    }
    private static final class Hit {
        int x,y,w,h;double score;
        Hit(int x,int y,int w,int h,double score){this.x=x;this.y=y;this.w=w;this.h=h;this.score=score;}
    }
    static int white(int c){int r=c>>16&255,g=c>>8&255,b=c&255,low=Math.min(r,Math.min(g,b)),high=Math.max(r,Math.max(g,b));return high-low<=65?low:0;}
    static ArrayList<Box> components(int[] p,int w,int l,int t,int r,int b) {
        return components(p,w,l,t,r,b,160);
    }
    static ArrayList<Box> components(int[] p,int w,int l,int t,int r,int b,int level) {
        int rw=r-l,rh=b-t;byte[] mask=new byte[rw*rh];int[] q=new int[mask.length];ArrayList<Box> out=new ArrayList<>();
        for(int y=t;y<b;y++)for(int x=l;x<r;x++)if(white(p[y*w+x])>=level)mask[(y-t)*rw+x-l]=1;
        for(int at=0;at<mask.length;at++)if(mask[at]==1){
            int head=0,tail=1;q[0]=at;mask[at]=2;Box box=new Box(l+at%rw,t+at/rw);
            while(head<tail){int v=q[head++],x=l+v%rw,y=t+v/rw;box.count++;
                box.l=Math.min(box.l,x);box.r=Math.max(box.r,x+1);box.t=Math.min(box.t,y);box.b=Math.max(box.b,y+1);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    int xx=v%rw+dx,yy=v/rw+dy;if(xx<0||xx>=rw||yy<0||yy>=rh)continue;
                    int n=yy*rw+xx;if(mask[n]==1){mask[n]=2;q[tail++]=n;}
                }
            }
            if(box.count>=3)out.add(box);if(out.size()>512)return new ArrayList<>();
        }return out;
    }
    private static int light(int c){return ((c>>16&255)*77+(c>>8&255)*150+(c&255)*29)>>8;}
    private static boolean capsule(int[] p,int w,int h,Hit word) {
        return capsuleAt(p,w,h,word.x+word.w/2,word.y+word.h/2,word.h);
    }
    private static boolean capsuleAt(int[] p,int w,int h,int cx,int cy,int th) {
        // Both horizontal boundaries must darken in the text-free padding of the capsule.
        int edges=0;
        for(int direction:new int[]{-1,1}){
            int inside=cy+direction*th,outside=cy+direction*(int)(th*1.8);
            if(inside<0||outside<0||inside>=h||outside>=h||cx-th/2<0||cx+th/2>=w)return false;
            int darker=0;
            for(int dx:new int[]{-th/2,0,th/2})if(light(p[outside*w+cx+dx])-light(p[inside*w+cx+dx])>=10)darker++;
            if(darker>=2)edges++;
        }
        return edges==2;
    }
    private static boolean numericSuffix(int[] p,int w,int h,Hit word){
        if(OopzHeaderSkipVisual.numericSuffix(p,w,h,word.x,word.y,word.w,word.h))return true;
        int th=word.h,l=word.x+word.w+1,r=Math.min(w,l+(int)(th*1.35)),t=word.y,b=t+th,pieces=0,digits=0;
        // The old 2.5-character search included bright scenery beyond the capsule. A local digit
        // is allowed only when a second, independent capsule boundary encloses the suffix too.
        if(!capsuleAt(p,w,h,word.x+word.w+th/2,word.y+th/2,th))return false;
        for(Box z:components(p,w,l,t,r,b,130)){
            if(z.h()<th*.25||z.count<th*th*.02)continue;pieces++;
            if(z.r<r&&z.l>l&&z.r<=l+th*1.15&&z.h()>=th*.4&&z.h()<=th*.95&&z.w()<=th*.80&&z.count>=th*th*.025&&z.count<=th*th*.45)digits++;
        }
        return pieces==1&&digits==1;
    }
    public static BiliVisual.Result detect(int[] p,int w,int h) {
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("尺寸或方向不适用");
        ArrayList<Box> boxes=components(p,w,(int)(w*.70),(int)(h*.86),(int)(w*.985),(int)(h*.985));
        ArrayList<Hit> seeds=new ArrayList<>();int digits=0;
        for(Box digit:boxes){
            int dh=digit.h();
            if(digit.l<w*.78||digit.r>w*.965||dh<Math.max(8,w*.018)||dh>w*.05||digit.w()>dh*.85
                ||digit.count<dh||digit.count>dh*dh*.65)continue;
            digits++;
            for(int th=Math.max(10,(int)(dh*1.05));th<=dh*1.65;th+=2)for(int ratio=17;ratio<=23;ratio+=2){
                int tw=Math.round(th*ratio/10f);
                for(int gap=1;gap<=th*.65;gap+=2)for(int y=digit.t-(int)(th*.35);y<=digit.t+dh*.10;y+=2){
                    int x=digit.l-gap-tw;if(x<w*.70||y<h*.86||y+th>h*.985)continue;
                    double score=SkipGlyphs.coarse(p,w,x,y,tw,th);if(score>=.60)seeds.add(new Hit(x,y,tw,th,score));
                    if(seeds.size()>2048)return none("字形候选过多");
                }
            }
        }
        seeds.sort((a,b)->Double.compare(b.score,a.score));Hit best=null;ArrayList<Hit> matches=new ArrayList<>();
        SkipGlyphs.InkWork scratch=new SkipGlyphs.InkWork();java.util.HashSet<Long> visited=new java.util.HashSet<>();double highest=-1;
        ArrayList<Hit> searched=new ArrayList<>();
        for(Hit s:seeds){
            boolean nearby=false;
            for(Hit old:searched)if(Math.abs(s.x+s.w/2-old.x-old.w/2)<w*.02&&Math.abs(s.y+s.h/2-old.y-old.h/2)<h*.008){nearby=true;break;}
            if(nearby)continue;if(searched.size()>=8)return none("不同位置字形候选过多");searched.add(s);
            refine:
            for(int dh=-1;dh<=1;dh++)for(int dw=-1;dw<=1;dw++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                int x=s.x+dx,y=s.y+dy,tw=s.w+dw,th=s.h+dh;
                if(x<0||y<0||x+tw>=w||y+th>=h||!visited.add(SkipGlyphs.patchKey(x,y,tw,th)))continue;
                double score=SkipGlyphs.fine(p,w,x,y,tw,th,scratch);highest=Math.max(highest,score);if(score<.84)continue;
                Hit hit=new Hit(x,y,tw,th,score);
                if(!numericSuffix(p,w,h,hit)||!capsule(p,w,h,hit))continue;
                matches.add(hit);if(best==null||score>best.score)best=hit;
                if(score>=.95)break refine;
            }
        }
        if(best==null)return none("独立数字候选="+digits+"，两字最高分="+(highest<0?"未匹配":Math.round(highest*100)+"%")+"；未同时确认跳过、后缀数字和深色胶囊");
        for(Hit m:matches)if(Math.abs(m.x+m.w/2-best.x-best.w/2)>w*.02||Math.abs(m.y+m.h/2-best.y-best.h/2)>h*.008)return none("跳过目标位置不唯一");
        return new BiliVisual.Result(best.x+best.w/2,best.y+best.h/2,best.h,"右下角跳过两字、独立后缀数字及深色胶囊匹配；字形="+Math.round(best.score*100)+"%；需两帧确认",true);
    }
    private static BiliVisual.Result none(String why){return new BiliVisual.Result(0,0,0,"底部倒计时跳过："+why);}
    private FooterSkipVisual(){}
}
