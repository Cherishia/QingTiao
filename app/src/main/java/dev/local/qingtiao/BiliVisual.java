package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;

/** Local pixel analysis for the pictured dark Bilibili ad sheet. No Android or network API. */
public final class BiliVisual {
    public static final class Result {
        public final int x,y,size,adLabelX,adLabelY;
        public final String reason;
        public final boolean requiresPair;
        Result(int x,int y,int size,String reason){this(x,y,size,reason,false);}
        Result(int x,int y,int size,String reason,boolean pair){this(x,y,size,reason,pair,-1,-1);}
        Result(int x,int y,int size,String reason,boolean pair,int adLabelX,int adLabelY){
            this.x=x;this.y=y;this.size=size;this.reason=reason;requiresPair=pair;
            this.adLabelX=adLabelX;this.adLabelY=adLabelY;
        }
        public boolean found(){return size>0;}
    }
    private static final class Component {
        int left,top,right,bottom,count;
        int[] positions;
    }
    private static int light(int rgb){return (((rgb>>16)&255)*77+((rgb>>8)&255)*150+(rgb&255)*29)>>8;}
    private static boolean gray(int rgb) {
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255,l=light(rgb);
        return l>=55&&l<=218&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=35;
    }
    private static boolean contrast(int rgb,int background) {
        int r=(rgb>>16)&255,g=(rgb>>8)&255,b=rgb&255;
        return background<70&&light(rgb)>=background+18&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=35;
    }
    private static int background(int[] p,int w,int y,int[] sample) {
        for(int i=0;i<5;i++)sample[i]=light(p[y*w+(int)(w*(.30+i*.10))]);
        java.util.Arrays.sort(sample);return sample[2];
    }
    private static boolean handle(int[] p,int w,int h,Component c,boolean adaptive) {
        int cy=(c.top+c.bottom)/2,rows=0;
        int pad=Math.max(3,(int)(w*.016));
        int required=Math.max(2,(int)(w*.0025));int[] sample=adaptive?new int[5]:null;
        for(int y=Math.max(pad,cy-(int)(w*.12));y<cy-w*.035&&y+pad<h;y++) {
            int bg=adaptive?background(p,w,y,sample):0;
            int run=-1;
            for(int x=(int)(w*.40);x<=(int)(w*.60);x++) {
                int value=p[y*w+x];boolean on=adaptive?contrast(value,bg):gray(value)&&light(value)<=150;
                if(on&&run<0)run=x;
                if(!on&&run>=0) {
                    int width=x-run,mid=(run+x)/2;
                    if(width>=w*.045&&width<=w*.13&&Math.abs(mid-w*.5)<=w*.025
                        &&light(p[(y-pad)*w+mid])<70&&light(p[(y+pad)*w+mid])<70
                        &&light(p[y*w+Math.max(0,run-pad)])<70&&light(p[y*w+Math.min(w-1,x+pad)])<70) {
                        if(++rows>=required)return true;
                        break;
                    }
                    run=-1;
                }
            }
        }
        return false;
    }
    private static List<Component> components(int[] p,int w,int h,boolean adaptive,int top,int bottom) {
        int left=(int)(w*.73),right=(int)(w*.99);
        int rw=right-left,rh=bottom-top;byte[] mask=new byte[rw*rh];int[] sample=adaptive?new int[5]:null;
        for(int y=0;y<rh;y++) {
            int bg=adaptive?background(p,w,top+y,sample):0;
            if(adaptive&&bg>=70)continue;
            int src=(top+y)*w+left,row=y*rw;
            for(int x=0;x<rw;x++)if(adaptive?contrast(p[src+x],bg):gray(p[src+x]))mask[row+x]=1;
        }
        int[] queue=new int[mask.length];List<Component> out=new ArrayList<>();
        for(int i=0;i<mask.length;i++)if(mask[i]!=0) {
            int begin=0,end=1;queue[0]=i;mask[i]=0;
            Component c=new Component();c.left=w;c.top=h;
            while(begin<end) {
                int value=queue[begin++],x=value%rw,y=value/rw;
                c.left=Math.min(c.left,left+x);c.top=Math.min(c.top,top+y);
                c.right=Math.max(c.right,left+x+1);c.bottom=Math.max(c.bottom,top+y+1);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=x+dx,ny=y+dy;if(nx<0||ny<0||nx>=rw||ny>=rh)continue;
                    int n=ny*rw+nx;if(mask[n]!=0){mask[n]=0;queue[end++]=n;}
                }
            }
            c.count=end;int cw=c.right-c.left,ch=c.bottom-c.top;
            if(cw<=w*.05&&ch<=w*.065&&end>=2) {
                c.positions=new int[end];for(int j=0;j<end;j++)c.positions[j]=(top+queue[j]/rw)*w+left+queue[j]%rw;
                out.add(c);
            }
        }
        return out;
    }
    private static boolean cross(Component c,int w) {
        int cw=c.right-c.left,ch=c.bottom-c.top;
        if(cw<w*.014||cw>w*.043||ch<cw*.7||ch>cw*1.4||c.count<cw*ch*.09||c.count>cw*ch*.58)return false;
        int near=0;int[] corners=new int[4],coverage=new int[8];
        for(int value:c.positions) {
            float x=((value%w-c.left+.5f)/cw)*2-1,y=((value/w-c.top+.5f)/ch)*2-1;
            if(Math.min(Math.abs(x-y),Math.abs(x+y))<=.32f) {
                near++;int bin=Math.min(7,(int)((x+1)*4));coverage[bin]++;
                if(Math.abs(x)>.45&&Math.abs(y)>.45)corners[(x>0?1:0)+(y>0?2:0)]++;
            }
        }
        if(near<c.count*.80)return false;
        for(int n:corners)if(n<2)return false;
        int filled=0;for(int n:coverage)if(n>0)filled++;
        return filled>=7;
    }
    private static boolean dots(Component x,List<Component> all,int w) {
        float cx=(x.left+x.right)/2f,cy=(x.top+x.bottom)/2f;
        List<Component> ds=new ArrayList<>();
        for(Component d:all) {
            int dw=d.right-d.left,dh=d.bottom-d.top;float dx=(d.left+d.right)/2f,dy=(d.top+d.bottom)/2f;
            if(dw>=Math.max(1,w*.002)&&dw<=w*.012&&dh>=dw*.55&&dh<=dw*1.8
                &&cx-dx>=w*.065&&cx-dx<=w*.16&&Math.abs(dy-cy)<=w*.025)ds.add(d);
        }
        if(ds.size()>32)return false;
        for(Component a:ds)for(Component b:ds)for(Component c:ds) {
            float ay=(a.top+a.bottom)/2f,by=(b.top+b.bottom)/2f,ccy=(c.top+c.bottom)/2f;
            float ax=(a.left+a.right)/2f,bx=(b.left+b.right)/2f,ccx=(c.left+c.right)/2f;
            float ab=by-ay,bc=ccy-by;
            if(ab>=w*.004&&ab<=w*.016&&bc>=w*.004&&bc<=w*.016&&ab/bc>=.65&&ab/bc<=1.55
                &&Math.max(ax,Math.max(bx,ccx))-Math.min(ax,Math.min(bx,ccx))<=w*.004
                &&Math.abs(by-cy)<=w*.008)return true;
        }
        return false;
    }
    /** JPEG compression can leave the smallest of the three dots below component size. */
    private static boolean dottedColumn(int[] p,int w,int h,Component x){
        int cx=(x.left+x.right)/2,cy=(x.top+x.bottom)/2;
        int left=Math.max(0,cx-(int)(w*.135)),right=Math.min(w,cx-(int)(w*.07));
        int first=cy-(int)(w*.04),last=cy+(int)(w*.04),groups=0,previous=-100;
        int[] centers=new int[4];
        for(int y=Math.max(0,first);y<=Math.min(h-1,last);y++){
            int hits=0;
            for(int px=left;px<right;px++){
                int value=p[y*w+px];
                if(gray(value)&&light(value)>=95){hits++;break;}
            }
            if(hits==0)continue;
            if(y-previous>1){
                if(groups>=centers.length)return false;
                centers[groups++]=y;
            }
            previous=y;
        }
        if(groups!=3)return false;
        int a=centers[1]-centers[0],b=centers[2]-centers[1];
        return a>=w*.004&&a<=w*.020&&b>=w*.004&&b<=w*.020&&a/(float)b>=.55f&&a/(float)b<=1.8f;
    }
    private static boolean header(int[] p,int w,int h,Component c,BiliPanel.Hint hint) {
        int cx=(c.left+c.right)/2,cy=(c.top+c.bottom)/2,side=Math.max(c.right-c.left,c.bottom-c.top);
        if(cx<w*.89||cx>w*.975||cy<h*.19||cy>h*.785)return false;
        if(hint==null&&(cy>h*.63||cy<h*.20))return false;
        if(hint!=null&&!hint.headerPoint(Math.round(cx*hint.width/(float)w),Math.round(cy*hint.height/(float)h)))return false;
        int dark=0,total=0;
        for(float row:new float[]{-.035f,.035f})for(float col:new float[]{.30f,.40f,.50f,.60f,.70f}) {
            int y=cy+(int)(w*row);if(y<0||y>=h)return false;
            total++;if(light(p[y*w+(int)(w*col)])<70)dark++;
        }
        for(int[] offset:new int[][]{{side,0},{-side,0},{0,side},{0,-side}}) {
            int x=cx+offset[0],y=cy+offset[1];if(x<0||x>=w||y<0||y>=h||light(p[y*w+x])>80)return false;
        }
        return dark>=total*.8;
    }
    private static final class Pass {
        Component chosen;int xs,headers,menus,complete;
        String describe(){return "X="+xs+"，深色栏="+headers+"，三点="+menus+"，拖动条="+complete;}
    }
    private static Pass pass(int[] p,int w,int h,boolean adaptive,int top,int bottom,BiliPanel.Hint hint) {
        Pass result=new Pass();List<Component> all=components(p,w,h,adaptive,top,bottom);
        for(Component c:all) {
            if(!cross(c,w))continue;result.xs++;
            if(!header(p,w,h,c,hint))continue;result.headers++;
            if(!dots(c,all,w)&&!dottedColumn(p,w,h,c))continue;result.menus++;
            if(!handle(p,w,h,c,adaptive))continue;result.complete++;result.chosen=c;
        }return result;
    }
    private static Result match(Pass p,boolean adaptive,String scope) {
        Component c=p.chosen;
        return new Result((c.left+c.right)/2,(c.top+c.bottom)/2,Math.max(c.right-c.left,c.bottom-c.top),
            scope+"X笔画+三点+标题栏对比+顶部拖动条"+(adaptive?"；按当前背景核验明暗笔画，需两帧确认":scope.isEmpty()?"；无原生面板边界，需两帧确认":""),adaptive||scope.isEmpty());
    }
    public static Result detect(int[] pixels,int w,int h){return detect(pixels,w,h,null);}
    public static Result detect(int[] pixels,int w,int h,BiliPanel.Hint hint) {
        if(pixels==null||w<240||h<w*1.4||h>w*3.0||pixels.length!=(long)w*h)return new Result(0,0,0,"尺寸或方向不适用");
        int top=(int)(h*.18),bottom=(int)(h*.66);
        if(hint!=null&&hint.width>0&&hint.height>0) {
            int start=Math.max(top,(int)((long)hint.top*h/hint.height));
            int end=Math.min(h,start+(int)(w*.20));
            if(end>start) {
                Pass scoped=pass(pixels,w,h,false,start,end,hint);
                if(scoped.complete==1)return match(scoped,false,"当前广告面板范围：");
                if(scoped.complete>1)return new Result(0,0,0,"当前面板关闭图形不唯一");
                scoped=pass(pixels,w,h,true,start,end,hint);
                if(scoped.complete==1)return match(scoped,true,"当前广告面板范围：");
                if(scoped.complete>1)return new Result(0,0,0,"当前面板明暗关闭图形不唯一");
            }
            // A confirmed native ad scope never authorizes an unrelated X elsewhere.
            return new Result(0,0,0,"当前广告面板范围内未匹配完整 X、三点与拖动条");
        }
        Pass legacy=pass(pixels,w,h,false,top,bottom,null);
        if(legacy.complete==1)return match(legacy,false,"");
        if(legacy.complete>1)return new Result(0,0,0,"关闭图形不唯一；"+legacy.describe());
        Pass adaptive=pass(pixels,w,h,true,top,bottom,null);
        if(adaptive.complete==1)return match(adaptive,true,"");
        if(adaptive.complete>1)return new Result(0,0,0,"背景对比关闭图形不唯一；"+adaptive.describe());
        return new Result(0,0,0,"逐项核验：灰度["+legacy.describe()+"]；背景对比["+adaptive.describe()+"]");
    }
    private BiliVisual(){}
}
