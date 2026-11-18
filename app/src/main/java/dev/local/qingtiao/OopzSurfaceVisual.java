package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;

/** OOPZ's drawn countdown/skip control when the SDK exposes no accessible label. */
public final class OopzSurfaceVisual {
    public static boolean context(String pkg,boolean startup,List<AdMatcher.Node> nodes) {
        return WmSplashVisual.applies(pkg)&&startup&&!nodes.isEmpty()&&nodes.size()<1000&&!AdMatcher.sensitive(nodes);
    }
    // Neutral strokes propose a bar; shared two-glyph references verify the word separately.
    private static boolean white(int p,int level) {
        int r=p>>16&255,g=p>>8&255,b=p&255;
        return Math.min(r,Math.min(g,b))>=level&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=70;
    }
    private static final class Box {
        int l,t,r,b,count;Box(int x,int y){l=x;t=y;r=x+1;b=y+1;}
        int height(){return b-t;}int width(){return r-l;}
    }
    private static final class Hit {
        int x,y,w,h;double score;Hit(int x,int y,int w,int h,double score){this.x=x;this.y=y;this.w=w;this.h=h;this.score=score;}
    }
    private static boolean numeric(int[] p,int w,int h,Box bar,int order) {
        int size=bar.height(),l=order==0?Math.max(0,bar.l-(int)(size*2.5)):bar.r+(int)(size*.28),r=order==0?bar.l-(int)(size*.28):Math.min(w,bar.r+(int)(size*2.5));
        int t=Math.max(0,bar.t-(int)(size*.35)),b=Math.min(h,bar.b+(int)(size*.35)),rw=r-l,rh=b-t;
        if(rw<=0||rh<=0)return false;byte[] mask=new byte[rw*rh];int[] q=new int[mask.length];
        for(int y=t;y<b;y++)for(int x=l;x<r;x++)if(white(p[y*w+x],100))mask[(y-t)*rw+x-l]=1;
        int significant=0,digits=0;
        for(int at=0;at<mask.length;at++)if(mask[at]==1) {
            int head=0,tail=1;q[0]=at;mask[at]=2;Box box=new Box(l+at%rw,t+at/rw);
            while(head<tail){int n=q[head++],x=n%rw,y=n/rw;box.count++;box.l=Math.min(box.l,l+x);box.r=Math.max(box.r,l+x+1);box.t=Math.min(box.t,t+y);box.b=Math.max(box.b,t+y+1);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int sx=x+dx,sy=y+dy;if(sx<0||sx>=rw||sy<0||sy>=rh)continue;int next=sy*rw+sx;if(mask[next]==1){mask[next]=2;q[tail++]=next;}}
            }
            if(box.t==t&&box.b==b&&(box.l==l||box.r==r))continue; // surrounding bright backdrop is not a digit
            if(box.height()<size*.3||box.count<Math.max(2,size*size*.015))continue;significant++;
            if(box.height()>=size*.55&&box.height()<=size*1.6&&box.width()<=size*.9&&box.count>=size*size*.025&&box.count<=size*size*.65)digits++;
        }
        return significant==1&&digits==1;
    }
    private static boolean straightDivider(int[] p,int w,int h,Box bar,Hit word){
        // A neutral fragment of a digit or curved parenthesis is not the complete separator.
        int size=bar.height(),l=Math.max(0,bar.l-size/2),r=Math.min(w,bar.r+size/2),t=Math.max(0,bar.t-size/3),b=Math.min(h,bar.b+size/3),rw=r-l;
        int threshold=SecondsSuffix.wordThreshold(p,w,word.x,word.y,word.w,word.h),seed=-1,peak=-1;
        for(int y=bar.t;y<bar.b;y++)for(int x=bar.l;x<bar.r;x++){int v=SecondsSuffix.light(p[y*w+x]);if(v>=threshold&&v>peak){peak=v;seed=(y-t)*rw+x-l;}}
        if(seed<0)return false;byte[] seen=new byte[rw*(b-t)];int[] q=new int[seen.length];int head=0,tail=1;q[0]=seed;seen[seed]=1;Box full=new Box(l+seed%rw,t+seed/rw);
        while(head<tail){int at=q[head++],x=l+at%rw,y=t+at/rw;full.count++;full.l=Math.min(full.l,x);full.r=Math.max(full.r,x+1);full.t=Math.min(full.t,y);full.b=Math.max(full.b,y+1);
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int xx=x+dx,yy=y+dy;if(xx<l||xx>=r||yy<t||yy>=b)continue;int n=(yy-t)*rw+xx-l;if(seen[n]==0&&SecondsSuffix.light(p[yy*w+xx])>=threshold){seen[n]=1;q[tail++]=n;}}
        }
        return full.l>l&&full.r<r&&full.t>t&&full.b<b&&full.width()<=Math.max(2,full.height()*.22)&&full.count>=full.width()*full.height()*.70;
    }
    public static BiliVisual.Result detect(int[] p,int w,int h){return detect(p,w,h,false);}
    static BiliVisual.Result detect(int[] p,int w,int h,boolean reversed) {
        SkipGlyphs.InkWork inkWork=new SkipGlyphs.InkWork();
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("画面尺寸不适用");
        int left=(int)(w*.73),right=(int)(w*.985),top=(int)(h*.04),bottom=(int)(h*.23),rw=right-left,rh=bottom-top;
        byte[] white=new byte[rw*rh];int[] queue=new int[white.length];ArrayList<Box> bars=new ArrayList<>();
        for(int level:new int[]{150,85}) {
        java.util.Arrays.fill(white,(byte)0);
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++)if(white(p[y*w+x],level))white[(y-top)*rw+x-left]=1;
        for(int at=0;at<white.length;at++)if(white[at]==1) {
            int head=0,tail=1;queue[0]=at;white[at]=2;Box b=new Box(left+at%rw,top+at/rw);
            while(head<tail) {
                int next=queue[head++],x=left+next%rw,y=top+next/rw;b.count++;
                b.l=Math.min(b.l,x);b.r=Math.max(b.r,x+1);b.t=Math.min(b.t,y);b.b=Math.max(b.b,y+1);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int sx=next%rw+dx,sy=next/rw+dy;
                    if(sx<0||sx>=rw||sy<0||sy>=rh)continue;int n=sy*rw+sx;
                    if(white[n]==1){white[n]=2;queue[tail++]=n;}
                }
            }
            if(b.height()>=Math.max(7,w*.012)&&b.height()<=w*.055&&b.width()<=Math.max(2,b.height()*.24)
                &&b.count>=b.width()*b.height()*.65) {
                boolean duplicate=false;for(Box old:bars)if(Math.abs(old.l-b.l)<=1&&Math.abs(old.t-b.t)<=1&&Math.abs(old.b-b.b)<=2)duplicate=true;
                if(!duplicate)bars.add(b);
            }
            if(bars.size()>32)return none("分隔线候选过多，不点击");
        }
        }
        Hit best=null;ArrayList<Hit> matches=new ArrayList<>();int prefixes=0;double highest=-1;
        java.util.HashSet<Long> visited=new java.util.HashSet<>();
        for(Box bar:bars)for(int order=0;order<(reversed?2:1);order++) {
            int size=bar.height(),prefix=0,prefixTop=bar.b,prefixBottom=bar.t;
            int pl=order==0?Math.max(left,bar.l-(int)(size*1.3)):bar.r+(int)(size*.35),pr=order==0?bar.l-(int)(size*.35):Math.min(right,bar.r+(int)(size*1.3));
            for(int y=bar.t;y<bar.b;y++)for(int x=pl;x<pr;x++)if(white(p[y*w+x],100)) {
                prefix++;prefixTop=Math.min(prefixTop,y);prefixBottom=Math.max(prefixBottom,y+1);
            }
            if(prefix<size*size*.025||prefix>size*size*.55||prefixBottom-prefixTop<size*.55)continue;
            if(reversed&&!numeric(p,w,h,bar,order))continue;
            prefixes++;
            ArrayList<Hit> seeds=new ArrayList<>();
            for(int th=Math.max(10,(int)(size*.90));th<=size*1.55;th+=2)
                for(int aspect=18;aspect<=22;aspect+=2)
                for(int y=Math.max(top,bar.t-(int)(size*.4));y<=bar.t+size*.12;y+=2)
                    for(int gap=(int)(size*.20);gap<=size*.85;gap+=2) {
                        int tw=Math.round(th*aspect/10f),x=order==0?bar.r+gap:bar.l-gap-tw;if(x<left||x+tw>right||y+th>bottom)continue;
                        double score=SkipGlyphs.coarse(p,w,x,y,tw,th);
                        if(score>=.60)seeds.add(new Hit(x,y,tw,th,score));
                        if(seeds.size()>2048)return none("分隔线附近字形候选过多");
                    }
            seeds.sort((a,b)->Double.compare(b.score,a.score));
            for(int i=0;i<Math.min(20,seeds.size());i++) {
                Hit s=seeds.get(i);
                for(int dh=-1;dh<=1;dh++)for(int dw=-1;dw<=1;dw++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int x=s.x+dx,y=s.y+dy,th=s.h+dh,tw=s.w+dw;if(th<10||x<left||y<top||x+tw>right||y+th>bottom)continue;
                    if(!visited.add(SkipGlyphs.patchKey(x,y,tw,th)))continue;
                    double score=SkipGlyphs.fine(p,w,x,y,tw,th,inkWork);highest=Math.max(highest,score);if(score<.84)continue;
                    Hit hit=new Hit(x,y,tw,th,score);if(reversed&&!straightDivider(p,w,h,bar,hit))continue;
                    matches.add(hit);if(best==null||score>best.score)best=hit;
                }
            }
        }
        if(best==null)return none("分隔线候选="+bars.size()+"，独立前缀="+prefixes+"，两字最高分="+(highest<0?"未匹配":Math.round(highest*100)+"%")+"；未同时确认独立倒计时前缀、分隔线和两字字形");
        for(Hit hit:matches)if(Math.abs(hit.x+hit.w/2-best.x-best.w/2)>w*.02||Math.abs(hit.y+hit.h/2-best.y-best.h/2)>h*.008)return none("存在多个不同位置的跳过目标");
        return new BiliVisual.Result(best.x+best.w/2,best.y+best.h/2,best.h,"OOPZ绘制按钮：独立倒计时前缀、竖分隔线与两字逐字核验通过，最低字形分数="+Math.round(best.score*100)+"%；需两帧同位置确认");
    }
    public static final class Proof {
        private String pkg="";private int window,display,width,height,x,y,size;private long frame=-1;
        public boolean confirm(String owner,int win,int screen,int w,int h,BiliVisual.Result word,long stamp,long now) {
            return confirm(owner,win,screen,w,h,word,stamp,now,false);
        }
        public boolean confirm(String owner,int win,int screen,int w,int h,BiliVisual.Result word,long stamp,long now,boolean delegated) {
            if(!(WmSplashVisual.applies(owner)||delegated&&"com.tencent.mm".equals(owner))||!word.found()||!VisualReview.currentFrame(now-stamp,w,h,w,h)){reset();return false;}
            boolean same=owner.equals(pkg)&&win==window&&screen==display&&w==width&&h==height
                &&Math.abs(word.x-x)<=Math.max(2,size/6)&&Math.abs(word.y-y)<=Math.max(2,size/8)
                &&Math.abs(word.size-size)<=Math.max(2,size/5);
            if(same&&stamp==frame)return false;
            boolean ready=same&&stamp-frame>=300&&stamp-frame<=1200;
            pkg=owner;window=win;display=screen;width=w;height=h;x=word.x;y=word.y;size=word.size;frame=stamp;
            return ready;
        }
        public void reset(){pkg="";frame=-1;}
    }
    private static BiliVisual.Result none(String reason){return new BiliVisual.Result(0,0,0,"OOPZ控件缺失画面识别："+reason);}
    private OopzSurfaceVisual(){}
}
