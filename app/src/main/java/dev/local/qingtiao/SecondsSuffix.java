package dev.local.qingtiao;

import java.util.ArrayList;

/** Local contour checks for (digit s), after an independently matched skip word. No screen search. */
final class SecondsSuffix {
    static int light(int c){return ((c>>16&255)*77+(c>>8&255)*150+(c&255)*29)>>8;}
    private static final class Part {
        int l,t,r,b,count;
        Part(int x,int y){l=x;t=y;r=x+1;b=y+1;}
        int w(){return r-l;}int h(){return b-t;}
    }
    static int wordThreshold(int[] p,int w,int x,int y,int tw,int th){
        int[] hist=new int[256];for(int yy=y;yy<y+th;yy++)for(int xx=x;xx<x+tw;xx++)hist[light(p[yy*w+xx])]++;
        return (percentile(hist,tw*th/2)+percentile(hist,tw*th*95/100))/2;
    }
    static boolean matches(int[] p,int w,int h,int x,int y,int tw,int th){
        int l=x+tw,r=Math.min((int)(w*.985),l+th*2),t=y,b=y+th;
        if(l<0||r-l<th||t<2||b+2>=h||th<10||th>64)return false;
        int rw=r-l,rh=b-t;int[] hist=new int[256];
        for(int yy=t;yy<b;yy++)for(int xx=l;xx<r;xx++)hist[light(p[yy*w+xx])]++;
        int low=percentile(hist,rw*rh/2),high=percentile(hist,rw*rh*95/100);
        if(high-low<40)return false;int threshold=(low+high)/2;
        byte[] mask=new byte[rw*rh];int[] queue=new int[mask.length];ArrayList<Part> parts=new ArrayList<>();
        for(int yy=t;yy<b;yy++)for(int xx=l;xx<r;xx++)if(light(p[yy*w+xx])>=threshold)mask[(yy-t)*rw+xx-l]=1;
        for(int at=0;at<mask.length;at++)if(mask[at]==1){
            Part z=new Part(at%rw,at/rw);int head=0,tail=1;queue[0]=at;mask[at]=2;
            while(head<tail){int a=queue[head++],xx=a%rw,yy=a/rw;z.count++;z.l=Math.min(z.l,xx);z.r=Math.max(z.r,xx+1);z.t=Math.min(z.t,yy);z.b=Math.max(z.b,yy+1);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int nx=xx+dx,ny=yy+dy;if(nx<0||nx>=rw||ny<0||ny>=rh)continue;int n=ny*rw+nx;if(mask[n]==1){mask[n]=2;queue[tail++]=n;}}
            }
            if(z.count>=Math.max(3,th/5)){parts.add(z);if(parts.size()>4)return false;}
        }
        if(parts.size()!=4)return false;parts.sort((a,c)->Integer.compare(a.l,c.l));
        Part open=parts.get(0),digit=parts.get(1),seconds=parts.get(2),close=parts.get(3);
        if(open.l>th*.30||close.r>th*1.65||Math.abs(open.t-close.t)>th*.16||Math.abs(open.b-close.b)>th*.16)return false;
        if(digit.t<open.t-th*.1||digit.b>open.b+th*.1||digit.b<open.b-th*.25||Math.abs(seconds.b-open.b)>th*.18)return false;
        for(int i=0;i<3;i++){Part a=parts.get(i),c=parts.get(i+1);if(c.l<=a.r||c.l-a.r>th*.35)return false;}
        if(!parenthesis(mask,rw,open,th,true)||!parenthesis(mask,rw,close,th,false)||!digit(mask,rw,digit,th)||!seconds(mask,rw,seconds,th))return false;
        // Require empty padding on both sides of the complete control, and a dark capsule above/below it.
        int pad=Math.max(2,th/6),ink=0;
        for(int yy=y;yy<y+th;yy++){
            for(int xx=Math.max(0,x-th/2);xx<x-pad;xx++)if(light(p[yy*w+xx])>=threshold)ink++;
            for(int xx=l+close.r+pad;xx<Math.min(w,l+close.r+th/2);xx++)if(light(p[yy*w+xx])>=threshold)ink++;
        }
        return ink<th*th*.035&&capsule(p,w,h,x+tw/2,y+th/2,th);
    }
    private static int percentile(int[] hist,int target){int n=0;for(int i=0;i<256;i++){n+=hist[i];if(n>=target)return i;}return 255;}
    private static double center(byte[] m,int rw,Part z,double from,double to){
        long sum=0;int count=0;for(int y=z.t+(int)(z.h()*from);y<z.t+(int)(z.h()*to);y++)for(int x=z.l;x<z.r;x++)if(m[y*rw+x]!=0){sum+=x;count++;}
        return count==0?Double.NaN:(double)sum/count;
    }
    private static boolean parenthesis(byte[] m,int rw,Part z,int th,boolean open){
        if(z.h()<th*.65||z.h()>th||z.w()<2||z.w()>th*.40||z.count<z.h()||z.count>z.w()*z.h()*.8)return false;
        double top=center(m,rw,z,0,.25),middle=center(m,rw,z,.35,.65),bottom=center(m,rw,z,.75,1);
        double direction=open?1:-1;return (top-middle)*direction>=z.w()*.12&&(bottom-middle)*direction>=z.w()*.12;
    }
    private static boolean digit(byte[] m,int rw,Part z,int th){
        if(z.h()<th*.50||z.h()>th*.95||z.w()<2||z.w()>th*.70||z.count<z.h()||z.count>z.w()*z.h()*.85)return false;
        // A single continuous numeric contour, with bounded width and at most three strokes per scanline.
        for(int y=z.t;y<z.b;y++){int runs=0;boolean ink=false;for(int x=z.l;x<z.r;x++){boolean at=m[y*rw+x]!=0;if(at&&!ink)runs++;ink=at;}if(runs==0||runs>3)return false;}
        return true;
    }
    private static boolean seconds(byte[] m,int rw,Part z,int th){
        if(z.h()<th*.40||z.h()>th*.80||z.w()<th*.25||z.w()>th*.65||z.count<z.h()||z.count>z.w()*z.h()*.8)return false;
        double upper=center(m,rw,z,.20,.45),lower=center(m,rw,z,.55,.80);
        return lower-upper>z.w()*.13&&Double.isFinite(center(m,rw,z,0,.20))&&Double.isFinite(center(m,rw,z,.80,1));
    }
    private static boolean capsule(int[] p,int w,int h,int cx,int cy,int th){
        for(int direction:new int[]{-1,1}){int inside=cy+direction*(int)(th*.67),outside=cy+direction*(int)(th*1.45);if(inside<0||outside<0||inside>=h||outside>=h||cx-th/2<0||cx+th/2>=w)return false;
            int darker=0;for(int dx:new int[]{-th/2,0,th/2})if(light(p[outside*w+cx+dx])-light(p[inside*w+cx+dx])>=12)darker++;
            if(darker<2)return false;
        }return true;
    }
    private SecondsSuffix(){}
}
