package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;

/** Left header/footer label: accessible ad evidence plus current pixels, never a native/parent action. */
public final class OopzLeftSkipVisual {
    public static final class Hint {
        public final int left,top,right,bottom,width,height;
        public final boolean footer;
        public Hint(int l,int t,int r,int b,int w,int h){left=l;top=t;right=r;bottom=b;width=w;height=h;footer=h>0&&t>=h*.88;}
    }
    private static boolean word(String s){String n=MatchPolicy.normalize(s);return n.equals("跳过")||n.equals("跳過");}
    public static Hint hint(String pkg,List<AdMatcher.Node> nodes,int w,int h,boolean startup) {
        if(!OopzSurfaceVisual.context(pkg,startup,nodes)||w<=0||h<=0)return null;
        boolean ad=false;for(AdMatcher.Node n:nodes)if(n.evidence())ad=true;if(!ad)return null;
        Hint found=null;
        for(AdMatcher.Node n:nodes) {
            if(!n.visible||!n.enabled||n.editable||n.checkable||n.scrollable||!(word(n.text)||word(n.desc)))continue;
            int cw=n.right-n.left,ch=n.bottom-n.top;
            boolean header=n.left>=w*.08&&n.right<=w*.52&&n.top>=h*.025&&n.bottom<=h*.15;
            boolean footer=n.left>=w*.015&&n.right<=w*.55&&n.top>=h*.88&&n.bottom<=h*.99;
            if((!header&&!footer)||cw<w*.025||cw>w*.15
                ||ch<h*.008||ch>h*.04||cw<ch*1.1||cw>ch*4.8)continue;
            Hint next=new Hint(n.left,n.top,n.right,n.bottom,w,h);
            if(found!=null&&!same(found,next))return null;found=next;
        }
        return found;
    }
    public static boolean same(Hint a,Hint b) {
        if(a==null||b==null||a.width!=b.width||a.height!=b.height)return false;
        int dx=Math.max(2,a.width/400),dy=Math.max(2,a.height/600);
        return Math.abs(a.left-b.left)<=dx&&Math.abs(a.right-b.right)<=dx&&Math.abs(a.top-b.top)<=dy&&Math.abs(a.bottom-b.bottom)<=dy;
    }
    // Two UI glyphs only, excluding the countdown, brand chip, and advertising body.
    private static final class Hit {
        int x,y,w,h;double score;Hit(int x,int y,int w,int h,double s){this.x=x;this.y=y;this.w=w;this.h=h;score=s;}
    }
    private static boolean suffix(int[] p,int w,int h,Hit word) {
        int left=word.x+word.w+1,right=Math.min(w,left+(int)(word.h*1.1)),count=0,top=word.y+word.h,bottom=word.y;
        if(left>=right)return false;
        for(int y=word.y;y<word.y+word.h&&y<h;y++)for(int x=left;x<right;x++) {
            int color=p[y*w+x],r=color>>16&255,g=color>>8&255,b=color&255;
            if(Math.min(r,Math.min(g,b))>=130&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<=65) {
                count++;top=Math.min(top,y);bottom=Math.max(bottom,y+1);
            }
        }
        return count>=word.h*word.h*.025&&count<=word.h*word.h*.45&&bottom-top>=word.h*.4;
    }
    public static BiliVisual.Result detect(int[] p,int w,int h,Hint hint) {
        SkipGlyphs.InkWork inkWork=new SkipGlyphs.InkWork();
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h||hint==null||hint.width<=0||hint.height<=0)return none("缺少有效独立跳过标签或画面");
        if(hint.footer&&!OopzHeaderSkipVisual.menuPresent(p,w,h))return none("左下标签存在，但当前未确认唯一小程序圆环/圆点菜单");
        int l=Math.round(hint.left*w/(float)hint.width),r=Math.round(hint.right*w/(float)hint.width);
        int t=Math.round(hint.top*h/(float)hint.height),b=Math.round(hint.bottom*h/(float)hint.height),pad=Math.max(2,w/400);
        if(l<0||t<0||r>w||b>h||l>=r||t>=b)return none("标签边界无效");
        int left=Math.max(0,l-pad),right=Math.min(w,r+pad),top=Math.max(0,t-pad),bottom=Math.min(h,b+pad);
        int min=Math.max(8,(int)((b-t)*.48)),max=Math.min((int)(w*.05),(int)((b-t)*1.1));
        ArrayList<Hit> seeds=new ArrayList<>();
        for(int th=min;th<=max;th+=2)for(int aspect=16;aspect<=22;aspect+=2) {
            int tw=Math.round(th*aspect/10f);if(tw<(r-l)*.65||tw>(r-l)*1.12)continue;
            for(int y=top;y+th<=bottom;y+=2)for(int x=left;x+tw<=right;x+=2) {
                double score=SkipGlyphs.coarse(p,w,x,y,tw,th);
                if(score>=.60)seeds.add(new Hit(x,y,tw,th,score));
                if(seeds.size()>256)return none("标签区域字形候选过多");
            }
        }
        seeds.sort((a,c)->Double.compare(c.score,a.score));Hit best=null;int glyphs=0;double highest=-1;
        java.util.HashSet<Long> visited=new java.util.HashSet<>();
        for(int i=0;i<Math.min(12,seeds.size());i++) {
            Hit s=seeds.get(i);
            for(int dh=-1;dh<=1;dh++)for(int dw=-2;dw<=2;dw++)for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                int x=s.x+dx,y=s.y+dy,tw=s.w+dw,th=s.h+dh;
                if(x<left||y<top||tw<12||th<8||x+tw>right||y+th>bottom)continue;
                if(!visited.add(SkipGlyphs.patchKey(x,y,tw,th)))continue;
                double score=SkipGlyphs.fine(p,w,x,y,tw,th,inkWork);highest=Math.max(highest,score);if(score<.84)continue;glyphs++;
                Hit next=new Hit(x,y,tw,th,score);
                if(hint.footer?!OopzHeaderSkipVisual.numericSuffix(p,w,h,x,y,tw,th):!suffix(p,w,h,next))continue;
                if(best==null||score>best.score)best=next;
            }
        }
        if(best==null)return none("跳过两字匹配="+glyphs+"，最高字形/前景分数="+(highest<0?"未匹配":Math.round(highest*100)+"%")+"；未同时确认当前文字与右侧倒计时");
        return new BiliVisual.Result(best.x+best.w/2,best.y+best.h/2,best.h,(hint.footer?"OOPZ左下：当前可见广告标识、独立跳过标签、小程序菜单与当前两字/倒计时核验通过；无需依赖微小水印字形":"OOPZ左上广告标签与当前‘跳过’两字、右侧倒计时笔画核验通过")+"；最低字形分数="+Math.round(best.score*100)+"%；需两帧确认，只触摸文字内部");
    }
    private static BiliVisual.Result none(String why){return new BiliVisual.Result(0,0,0,"OOPZ独立跳过标签识别："+why);}
    private OopzLeftSkipVisual(){}
}
