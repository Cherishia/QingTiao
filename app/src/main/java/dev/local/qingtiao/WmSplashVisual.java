package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;

/** Shared WM startup rule: requires the exact SDK control and current-frame skip lettering. */
public final class WmSplashVisual {
    public static final String ID="cj.mobile.wm.allmodules:id/wm_splash_jump_tv";
    public static boolean applies(String pkg){return "com.weilaishanhai.oopz".equals(pkg);}
    public static boolean hasControl(List<AdMatcher.Node> nodes) {
        for(AdMatcher.Node n:nodes)if(n.visible&&ID.equals(n.id))return true;
        return false;
    }
    public static final class Hint {
        public final int left,top,right,bottom,width,height;
        public Hint(int l,int t,int r,int b,int w,int h){left=l;top=t;right=r;bottom=b;width=w;height=h;}
    }
    public static Hint hint(String pkg,List<AdMatcher.Node> nodes,int w,int h,boolean startup) {
        if(pkg==null||pkg.isEmpty()||!startup||w<=0||h<=0||AdMatcher.sensitive(nodes))return null;
        Hint found=null;
        for(AdMatcher.Node n:nodes) {
            if(!ID.equals(n.id)||!n.visible||!n.enabled||!n.clickable||n.editable||n.checkable||n.scrollable
                ||!(MatchPolicy.isSkipText(n.text)||MatchPolicy.isSkipText(n.desc)))continue;
            int cw=n.right-n.left,ch=n.bottom-n.top;
            if(n.left<w*.74||n.right>w||n.right<w*.92||n.top<h*.08||n.bottom>h*.23
                ||cw<w*.05||cw>w*.25||ch<h*.012||ch>h*.05||cw<ch*1.8||cw>ch*5.5)continue;
            Hint next=new Hint(n.left,n.top,n.right,n.bottom,w,h);
            if(found!=null&&!same(found,next))return null;
            found=next;
        }
        return found;
    }
    public static boolean same(Hint a,Hint b) {
        if(a==null||b==null||a.width!=b.width||a.height!=b.height)return false;
        int tx=Math.max(2,a.width/200),ty=Math.max(2,a.height/400);
        // A narrower countdown digit can move only the left edge of this right-aligned pill.
        return Math.abs(a.left-b.left)<=Math.max(2,a.width/80)&&Math.abs(a.right-b.right)<=tx&&Math.abs(a.top-b.top)<=ty&&Math.abs(a.bottom-b.bottom)<=ty;
    }
    // Only the two skip-control glyphs; no ad image, brand or landing text.
    private static final float[] FINE=SkipGlyphs.reference(0,40,20),DETAIL=detail(FINE);
    private static final class Hit {
        int x,y,w,h;double score;
        Hit(int x,int y,int w,int h,double score){this.x=x;this.y=y;this.w=w;this.h=h;this.score=score;}
    }
    // Remove the slowly changing backdrop visible through WM's translucent pill.
    // Work in the normalized glyph grid so the blur follows the text size.
    private static final class DetailWork {
        final float[] samples=new float[800],integral=new float[47*27],values=new float[800];
    }
    private static float[] detail(float[] samples){return detail(samples,new float[47*27],new float[800]);}
    private static float[] detail(float[] samples,float[] integral,float[] out) {
        final int sw=40,sh=20,radius=3,stride=sw+radius*2+1;
        java.util.Arrays.fill(integral,0);
        for(int y=0;y<sh+radius*2;y++) {
            float row=0;
            for(int x=0;x<sw+radius*2;x++) {
                row+=samples[Math.max(0,Math.min(sh-1,y-radius))*sw+Math.max(0,Math.min(sw-1,x-radius))];
                integral[(y+1)*stride+x+1]=integral[y*stride+x+1]+row;
            }
        }
        for(int y=0;y<sh;y++)for(int x=0;x<sw;x++) {
            int right=x+radius*2+1,bottom=y+radius*2+1;
            float sum=integral[bottom*stride+right]-integral[y*stride+right]-integral[bottom*stride+x]+integral[y*stride+x];
            out[y*sw+x]=samples[y*sw+x]-sum/49;
        }
        return out;
    }
    private static double detailScore(int[] pixels,int width,int x,int y,int tw,int th,DetailWork work) {
        float[] samples=work.samples;
        for(int sy=0;sy<20;sy++)for(int sx=0;sx<40;sx++) {
            int px=x+Math.min(tw-1,(int)((sx+.5)*tw/40)),py=y+Math.min(th-1,(int)((sy+.5)*th/20));
            int p=pixels[py*width+px];samples[sy*40+sx]=((p>>16&255)*299+(p>>8&255)*587+(p&255)*114)/1000f;
        }
        float[] values=detail(samples,work.integral,work.values);double worst=1;
        for(int part=0;part<2;part++) {
            double a=0,b=0,aa=0,bb=0,ab=0;int count=400;
            for(int sy=0;sy<20;sy++)for(int sx=part*20;sx<(part+1)*20;sx++) {
                int at=sy*40+sx;double p=values[at],t=DETAIL[at];a+=p;b+=t;aa+=p*p;bb+=t*t;ab+=p*t;
            }
            double variance=aa-a*a/count,denominator=Math.sqrt(Math.max(0,variance*(bb-b*b/count)));
            if(variance/count<12||denominator==0)return -1;
            worst=Math.min(worst,(ab-a*b/count)/denominator);
        }
        return worst;
    }
    public static final class Detection {
        public final BiliVisual.Result target;public final boolean requiresPair;
        private Detection(BiliVisual.Result target,boolean requiresPair){this.target=target;this.requiresPair=requiresPair;}
    }
    /** Independent fresh frames of the same SDK hint before any initial WM touch. */
    public static final class PairProof {
        private String pkg="";private int window,display,width,height,x,y,size;private long frame=-1;private Hint hint;
        public boolean confirm(String owner,int win,int screen,int w,int h,Hint current,BiliVisual.Result word,long stamp,long now) {
            if(owner==null||owner.isEmpty()||current==null||current.width!=w||current.height!=h||!word.found()||!VisualReview.currentFrame(now-stamp,w,h,w,h)){reset();return false;}
            boolean stable=owner.equals(pkg)&&win==window&&screen==display&&w==width&&h==height&&same(hint,current)
                &&Math.abs(word.x-x)<=Math.max(2,size/6)&&Math.abs(word.y-y)<=Math.max(2,size/8)&&Math.abs(word.size-size)<=Math.max(2,size/5);
            if(stable&&stamp==frame)return false;
            boolean ready=stable&&stamp-frame>=300&&stamp-frame<=1200;
            pkg=owner;window=win;display=screen;width=w;height=h;hint=current;x=word.x;y=word.y;size=word.size;frame=stamp;return ready;
        }
        public void reset(){pkg="";hint=null;frame=-1;}
    }
    public static BiliVisual.Result detect(int[] pixels,int w,int h,Hint hint){return analyze(pixels,w,h,hint).target;}
    public static Detection analyze(int[] pixels,int w,int h,Hint hint) {
        if(pixels==null||w<240||h<w*1.4||h>w*3||pixels.length!=(long)w*h||hint==null||hint.width<=0||hint.height<=0)return miss("未取得有效的当前SDK跳过控件");
        int l=Math.round(hint.left*w/(float)hint.width),r=Math.round(hint.right*w/(float)hint.width);
        int t=Math.round(hint.top*h/(float)hint.height),b=Math.round(hint.bottom*h/(float)hint.height);
        if(l<0||t<0||r>w||b>h||l>=r||t>=b)return miss("SDK控件超出当前画面");
        int left=l+(int)((r-l)*.35),top=t+Math.max(1,(b-t)/12),bottom=b-Math.max(1,(b-t)/12);
        int min=Math.max(10,(int)((b-t)*.35)),max=Math.min((int)(w*.06),(int)((b-t)*.75));
        ArrayList<Hit> seeds=new ArrayList<>();
        for(int th=min;th<=max;th+=2)for(int aspect=18;aspect<=22;aspect+=2) {
            int tw=Math.round(th*aspect/10f);
            for(int y=top;y+th<=bottom;y+=2)for(int x=left;x+tw<=r;x+=2) {
                double score=SkipGlyphs.coarse(pixels,w,x,y,tw,th);
                if(score<.50)continue;seeds.add(new Hit(x,y,tw,th,score));
                if(seeds.size()>1536)return miss("SDK区域中的字形候选过多");
            }
        }
        seeds.sort((a,c)->Double.compare(c.score,a.score));Hit best=null;ArrayList<Hit> matched=new ArrayList<>();
        double bestGray=-1,bestDetail=-1,bestInk=-1;Hit inkBest=null;ArrayList<Hit> inkMatched=new ArrayList<>();SkipGlyphs.InkWork inkWork=new SkipGlyphs.InkWork();DetailWork detailWork=null;
        java.util.HashSet<Long> visited=new java.util.HashSet<>();
        for(int i=0;i<Math.min(48,seeds.size());i++) {
            Hit s=seeds.get(i);
            for(int dh=-1;dh<=1;dh++)for(int dw=-1;dw<=1;dw++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                int th=s.h+dh,x=s.x+dx,y=s.y+dy,tw=s.w+dw;
                if(th<min||th>max||x<left||y<top||x+tw>r||y+th>bottom)continue;
                if(!visited.add(SkipGlyphs.patchKey(x,y,tw,th)))continue;
                double score=SplashVisual.score(pixels,w,x,y,tw,th,FINE,40,20);
                bestGray=Math.max(bestGray,score);
                boolean legacy=score>=.76;
                if(!legacy&&score>=.55) {
                    if(detailWork==null)detailWork=new DetailWork();
                    double local=detailScore(pixels,w,x,y,tw,th,detailWork);bestDetail=Math.max(bestDetail,local);
                    if(local>=.72){legacy=true;score=local;}
                }
                if(legacy) {
                    Hit hit=new Hit(x,y,tw,th,score);matched.add(hit);if(best==null||score>best.score)best=hit;
                } else {
                    double extended=-1;
                    for(int style=1;style<SkipGlyphs.STYLES;style++)extended=Math.max(extended,SkipGlyphs.gray(pixels,w,x,y,tw,th,style));
                    if(extended<.84)for(int style=0;style<SkipGlyphs.STYLES;style++) {
                        double ink=SkipGlyphs.ink(pixels,w,x,y,tw,th,style,inkWork);bestInk=Math.max(bestInk,ink);
                        if(ink>=.95)extended=Math.max(extended,ink);
                    }
                    if(extended>=.84) {
                        Hit hit=new Hit(x,y,tw,th,extended);inkMatched.add(hit);if(inkBest==null||extended>inkBest.score)inkBest=hit;
                    }
                }
            }
        }
        boolean extended=best==null&&inkBest!=null;
        if(extended){best=inkBest;matched=inkMatched;}
        if(best==null)return miss("SDK控件存在，但当前画面未确认‘跳过’两字，不点击；逐字灰度="+similarity(bestGray)+"，局部笔画="+similarity(bestDetail)+"，前景笔画="+similarity(bestInk));
        for(Hit hit:matched)if(Math.abs(hit.x+hit.w/2-best.x-best.w/2)>w*.02||Math.abs(hit.y+hit.h/2-best.y-best.h/2)>h*.008)return miss("SDK区域出现不同位置的跳过文字，不点击");
        return new Detection(new BiliVisual.Result(best.x+best.w/2,best.y+best.h/2,best.h,extended
            ?"WM控件与当前扩展字体/前景笔画核验通过；最低字形分数="+Math.round(best.score*100)+"%；需两帧确认后触摸字形内部"
            :"WM控件与当前‘跳过’逐字匹配（含半透明背景局部笔画核验），相关度="+Math.round(best.score*100)+"%；需两帧确认，仅触摸已确认字形内部，不调用控件或父节点动作"),true);
    }
    private static Detection miss(String why){return new Detection(none(why),false);}
    private static String similarity(double score){return score<0?"未匹配":Math.round(score*100)+"%";}
    /** Interior of the final glyph, away from the inter-character gap and countdown. */
    public static int touchX(BiliVisual.Result word,boolean retry) {
        return word.x+Math.round(word.size*(retry?.30f:.50f));
    }
    /** Shared verified SDK layout: first glyph, then a separately reverified final glyph. */
    public static int sdkTouchX(String pkg,BiliVisual.Result word,boolean retry) {
        if(word==null||!word.found())return 0;
        return word.x+Math.round(word.size*(retry?.50f:-.50f));
    }
    public static int sdkTouchDuration(String pkg,boolean retry){return retry?100:80;}
    private static BiliVisual.Result none(String why){return new BiliVisual.Result(0,0,0,"WM开屏："+why);}
    private WmSplashVisual(){}
}
