package dev.local.qingtiao;
import java.util.List;
/** Drawn skip/countdown family, activated by current layout evidence rather than an owner allow-list. */
public final class CanvasSkipVisual {
    public static boolean context(String pkg,boolean startup,List<AdMatcher.Node> nodes) {
        if(pkg==null||pkg.isEmpty()||MatchPolicy.diagnosticHost(pkg)||!startup||nodes==null||nodes.isEmpty()||nodes.size()>=1000||AdMatcher.sensitive(nodes))return false;
        for(AdMatcher.Node n:nodes)if(n.visible) {
            String kind=((n.className==null?"":n.className)+" "+(n.id==null?"":n.id)).toLowerCase(java.util.Locale.ROOT);
            if(n.editable||kind.contains("gallery")||kind.contains("imagepreview")||kind.contains("image_preview")||kind.contains("imageviewer")||"查看原图".equals(n.text)||"保存图片".equals(n.text))return false;
        }return true;
    }
    public static boolean nativeAd(List<AdMatcher.Node> nodes){for(AdMatcher.Node n:nodes)if(n.evidence())return true;return false;}
    public static boolean nodeAd(List<AdMatcher.Node> nodes,int w,int h){return nativeAd(nodes)||AdSurface.present(nodes,w,h);}
    public static BiliVisual.Result detect(int[] p,int w,int h,boolean nativeAd) {
        if(!nativeAd) {MiniAdBadge.Result ad=MiniAdBadge.analyze(p,w,h,true);if(!ad.found)return new BiliVisual.Result(0,0,0,"通用绘制跳过：未确认当前双色广告图标和广告两字；"+ad.reason);}
        BiliVisual.Result word=OopzSurfaceVisual.detect(p,w,h,true);
        if(!word.found()) {
            BiliVisual.Result footer=FooterSkipVisual.detect(p,w,h);
            if(footer.found())return new BiliVisual.Result(footer.x,footer.y,footer.size,"通用绘制跳过：当前广告标识与"+footer.reason,true);
            BiliVisual.Result plain=PlainSkipVisual.detect(p,w,h);
            if(plain.found())return new BiliVisual.Result(plain.x,plain.y,plain.size,"通用绘制跳过：当前广告证据与"+plain.reason,true);
            BiliVisual.Result footerPlain=PlainSkipVisual.detectFooter(p,w,h);
            if(footerPlain.found())return new BiliVisual.Result(footerPlain.x,footerPlain.y,footerPlain.size,"通用绘制跳过：当前广告证据与"+footerPlain.reason,true);
            return new BiliVisual.Result(0,0,0,"通用绘制跳过："+word.reason+"；"+plain.reason);
        }
        return word.found()?new BiliVisual.Result(word.x,word.y,word.size,"通用绘制跳过：当前广告标识、跳过两字、竖分隔线及独立数字匹配（支持前/后缀顺序）；需两帧确认"):new BiliVisual.Result(0,0,0,"通用绘制跳过："+word.reason);
    }
    public static final class Proof {
        private String pkg="";private int win,screen,width,height,x,y,size,labelX,labelY;private long frame=-1;
        public boolean confirm(String owner,int window,int display,int w,int h,BiliVisual.Result word,long stamp,long now) {
            if(owner==null||owner.isEmpty()||window<0||display<0||!word.found()||!VisualReview.currentFrame(now-stamp,w,h,w,h)){reset();return false;}
            boolean same=owner.equals(pkg)&&window==win&&display==screen&&w==width&&h==height&&Math.abs(word.x-x)<=Math.max(2,size/6)&&Math.abs(word.y-y)<=Math.max(2,size/8)&&Math.abs(word.size-size)<=Math.max(2,size/5);
            if(same&&(word.adLabelX>=0||labelX>=0))
                same=word.adLabelX>=0&&labelX>=0&&Math.abs(word.adLabelX-labelX)<=Math.max(4,size/2)
                    &&Math.abs(word.adLabelY-labelY)<=Math.max(4,size/2);
            if(same&&stamp==frame)return false;boolean ready=same&&stamp-frame>=VisualReview.MIN_PAIR_GAP&&stamp-frame<=VisualReview.MAX_PAIR_GAP;
            pkg=owner;win=window;screen=display;width=w;height=h;x=word.x;y=word.y;size=word.size;
            labelX=word.adLabelX;labelY=word.adLabelY;frame=stamp;return ready;
        }
        long lastFrame(){return frame;}
        public void reset(){pkg="";frame=-1;}
    }
    private CanvasSkipVisual(){}
}
