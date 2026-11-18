package dev.local.qingtiao;

import java.util.ArrayList;

/** Drawn mini-app startup header: a suffix digit, both skip glyphs, and a separate menu. */
public final class OopzHeaderSkipVisual {
    private static final class Box {
        int l,t,r,b,count;Box(int x,int y){l=x;t=y;r=x+1;b=y+1;}
        int w(){return r-l;}int h(){return b-t;}int x(){return (l+r)/2;}int y(){return (t+b)/2;}
    }
    private static final class Hit {
        int x,y,w,h;double score;Hit(int x,int y,int w,int h,double s){this.x=x;this.y=y;this.w=w;this.h=h;score=s;}
    }
    private static int neutral(int c){int r=c>>16&255,g=c>>8&255,b=c&255,low=Math.min(r,Math.min(g,b)),high=Math.max(r,Math.max(g,b));return high-low<=65?low:0;}
    private static ArrayList<Box> components(int[] p,int w,int l,int t,int r,int b) {
        int rw=r-l,rh=b-t;byte[] mask=new byte[rw*rh];int[] queue=new int[mask.length];ArrayList<Box> out=new ArrayList<>();
        for(int y=t;y<b;y++)for(int x=l;x<r;x++)if(neutral(p[y*w+x])>=130)mask[(y-t)*rw+x-l]=1;
        for(int at=0;at<mask.length;at++)if(mask[at]==1) {
            int head=0,tail=1;queue[0]=at;mask[at]=2;Box box=new Box(l+at%rw,t+at/rw);
            while(head<tail) {
                int q=queue[head++],x=l+q%rw,y=t+q/rw;box.count++;box.l=Math.min(box.l,x);box.r=Math.max(box.r,x+1);box.t=Math.min(box.t,y);box.b=Math.max(box.b,y+1);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int xx=q%rw+dx,yy=q/rw+dy;if(xx<0||xx>=rw||yy<0||yy>=rh)continue;int n=yy*rw+xx;
                    if(mask[n]==1){mask[n]=2;queue[tail++]=n;}
                }
            }
            if(box.count>=2)out.add(box);
            if(out.size()>512)return new ArrayList<>();
        }
        return out;
    }
    private static boolean menu(int[] p,int w,int h,Box ring,ArrayList<Box> boxes,boolean footer) {
        double radius=(ring.w()+ring.h())/4.0;int cx=ring.x(),cy=ring.y();
        if(cx<w*.78||cx>w*.965||cy<h*(footer?.05:.075)||cy>h*.15||radius<w*.009||radius>w*.032
            ||ring.w()<ring.h()*.8||ring.w()>ring.h()*1.25||ring.count>ring.w()*ring.h()*.72)return false;
        int radial=0;
        for(int i=0;i<16;i++) {
            double a=i*Math.PI/8,cs=Math.cos(a),sn=Math.sin(a);int bright=0;
            for(double rr=.78;rr<=1.01;rr+=.07)bright=Math.max(bright,neutral(p[(cy+(int)Math.round(sn*radius*rr))*w+cx+(int)Math.round(cs*radius*rr)]));
            int inner=neutral(p[(cy+(int)Math.round(sn*radius*.55))*w+cx+(int)Math.round(cs*radius*.55)]);
            int ox=cx+(int)Math.round(cs*radius*1.3),oy=cy+(int)Math.round(sn*radius*1.3);
            if(ox<0||ox>=w||oy<0||oy>=h)return false;
            if(bright>=150&&bright-inner>=30&&bright-neutral(p[oy*w+ox])>=30)radial++;
        }
        if(radial<12)return false;
        int dots=0;
        for(Box dot:boxes)if(dot.x()<cx-radius*3&&dot.x()>cx-radius*9&&Math.abs(dot.y()-cy)<=radius*.4
            &&dot.w()<=radius*.8&&dot.h()<=radius*.8&&dot.w()>=radius*.15&&dot.h()>=radius*.15
            &&dot.count>=dot.w()*dot.h()*.4) {
            dots++;
        }
        return dots>=2;
    }
    public static BiliVisual.Result detect(int[] p,int w,int h) {
        return detect(p,w,h,false);
    }
    static boolean menuPresent(int[] p,int w,int h) {
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return false;
        ArrayList<Box> boxes=components(p,w,(int)(w*.20),(int)(h*.04),(int)(w*.985),(int)(h*.155));
        int count=0;for(Box b:boxes)if(menu(p,w,h,b,boxes,true))count++;return count==1;
    }
    static boolean numericSuffix(int[] p,int w,int h,int x,int y,int tw,int th) {
        int l=x+tw+1,r=Math.min(w,l+(int)(th*2.5)),t=Math.max(0,y),b=Math.min(h,y+th);
        if(l>=r||t>=b)return false;
        ArrayList<Box> boxes=components(p,w,l,t,r,b);int digits=0,pieces=0;
        for(Box box:boxes) {
            if(box.h()>=th*.25&&box.count>=th*th*.02)pieces++;
            if(box.r<=l+th*1.15&&box.h()>=th*.4&&box.h()<=th*.95&&box.w()<=th*.80&&box.count>=th*th*.025&&box.count<=th*th*.45)digits++;
        }
        return digits==1&&pieces==1;
    }
    public static BiliVisual.Result detectBottom(int[] p,int w,int h) {
        return detect(p,w,h,true);
    }
    private static BiliVisual.Result detect(int[] p,int w,int h,boolean footer) {
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("画面尺寸不适用");
        ArrayList<Box> boxes=components(p,w,(int)(w*.20),(int)(h*.04),(int)(w*.985),(int)(h*.155));
        ArrayList<Box> menus=new ArrayList<>();for(Box b:boxes)if(menu(p,w,h,b,boxes,footer))menus.add(b);
        if(menus.size()!=1)return none("小程序圆环及独立圆点菜单="+menus.size()+"，未确认唯一广告页头");
        if(footer) {
            MiniAdBadge.Result badge=MiniAdBadge.analyze(p,w,h);
            if(!badge.found)return none("左下角规则：当前右下角未同时确认广告图标及‘广告’两字；"+badge.reason);
        }
        if(footer)boxes=components(p,w,(int)(w*.015),(int)(h*.88),(int)(w*.55),(int)(h*.99));
        Box menu=menus.get(0);ArrayList<Hit> seeds=new ArrayList<>();int digits=0;
        for(Box digit:boxes) {
            int dh=digit.h();
            if(digit.t<h*(footer?.88:.045)||digit.b>h*(footer?.985:.10)||digit.l<w*(footer?.05:.25)||digit.r>w*(footer?.50:.96)||dh<Math.max(7,w*.010)||dh>w*.04
                ||digit.w()>dh*1.05||digit.w()<1||digit.count<dh*1.1||digit.count>dh*dh*.7
                ||!footer&&(menu.y()-digit.y()<h*.02||menu.y()-digit.y()>h*.085))continue;
            digits++;
            for(int th=Math.max(9,(int)(dh*1.10));th<=dh*1.65;th+=2)for(int aspect=16;aspect<=22;aspect+=2) {
                int tw=Math.round(th*aspect/10f);
                for(int gap=1;gap<=th*.65;gap+=2)for(int y=digit.t-(int)(th*.35);y<=digit.t+dh*.15;y+=2) {
                    int x=digit.l-gap-tw;if(x<w*(footer?.015:.20)||y<h*(footer?.88:.04)||y+th>h*(footer?.99:.105))continue;
                    double score=SkipGlyphs.coarse(p,w,x,y,tw,th);if(score>=.60)seeds.add(new Hit(x,y,tw,th,score));
                    if(seeds.size()>2048)return none("后缀笔画附近字形候选过多");
                }
            }
        }
        seeds.sort((a,b)->Double.compare(b.score,a.score));Hit best=null;ArrayList<Hit> matches=new ArrayList<>();double highest=-1;SkipGlyphs.InkWork scratch=new SkipGlyphs.InkWork();
        java.util.HashSet<Long> visited=new java.util.HashSet<>();
        for(int i=0;i<Math.min(20,seeds.size());i++) {
            Hit s=seeds.get(i);
            for(int dh=-1;dh<=1;dh++)for(int dw=-1;dw<=1;dw++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                int x=s.x+dx,y=s.y+dy,tw=s.w+dw,th=s.h+dh;if(x<0||y<0||x+tw>=w||y+th>=h)continue;
                if(!visited.add(SkipGlyphs.patchKey(x,y,tw,th)))continue;
                double score=SkipGlyphs.fine(p,w,x,y,tw,th,scratch);highest=Math.max(highest,score);if(score<.84)continue;
                Hit hit=new Hit(x,y,tw,th,score);matches.add(hit);if(best==null||score>best.score)best=hit;
            }
        }
        if(best==null)return none("后缀数字笔画="+digits+"，两字最高分="+(highest<0?"未匹配":Math.round(highest*100)+"%")+"；未同时确认跳过两字及数字");
        for(Hit hit:matches)if(Math.abs(hit.x+hit.w/2-best.x-best.w/2)>w*.02||Math.abs(hit.y+hit.h/2-best.y-best.h/2)>h*.008)return none("存在不同位置的跳过目标");
        return new BiliVisual.Result(best.x+best.w/2,best.y+best.h/2,best.h,(footer?"OOPZ左下角广告：当前广告徽标、跳过两字、右侧数字及顶部圆环/圆点菜单匹配":"OOPZ小程序页头：跳过两字、右侧数字笔画及下方圆环/圆点菜单匹配")+"；字形分数="+Math.round(best.score*100)+"%；需两帧确认，只触摸‘过’字内部");
    }
    private static BiliVisual.Result none(String why){return new BiliVisual.Result(0,0,0,"OOPZ无标签页头识别："+why);}
    private OopzHeaderSkipVisual(){}
}
