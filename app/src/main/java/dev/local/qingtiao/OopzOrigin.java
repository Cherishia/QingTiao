package dev.local.qingtiao;

/** A short OOPZ startup handoff may host the same mini-app ad in WeChat; ordinary WeChat is excluded. */
public final class OopzOrigin {
    private String foreground="";
    private long entered=-1,lastOopzWindow=-1,delegatedUntil=-1;
    public void windowEvent(String pkg,long now){if(WmSplashVisual.applies(pkg))lastOopzWindow=now;}
    public void foreground(String pkg,long now) {
        foreground(pkg,now,0);
    }
    public void foreground(String pkg,long now,int previousTouches) {
        if(pkg.equals(foreground))return;
        boolean fromStartup=WmSplashVisual.applies(foreground)&&now>=entered&&now-entered<30000;
        boolean recentWindow=lastOopzWindow>=0&&now>=lastOopzWindow&&now-lastOopzWindow<=2500;
        delegatedUntil="com.tencent.mm".equals(pkg)&&previousTouches<2&&(fromStartup||recentWindow)?now+30000:-1;
        foreground=pkg;entered=now;
    }
    public boolean delegated(String pkg,long now){return "com.tencent.mm".equals(pkg)&&pkg.equals(foreground)&&now>=entered&&now<delegatedUntil;}
    public static boolean context(String pkg,boolean startup,java.util.List<AdMatcher.Node> nodes,boolean delegated) {
        if((!WmSplashVisual.applies(pkg)&&!(delegated&&"com.tencent.mm".equals(pkg)))||!startup||nodes.isEmpty()||nodes.size()>=1000||AdMatcher.sensitive(nodes))return false;
        for(AdMatcher.Node n:nodes)if(n.visible) {
            String kind=((n.className==null?"":n.className)+" "+(n.id==null?"":n.id)).toLowerCase(java.util.Locale.ROOT);
            if(kind.contains("gallery")||kind.contains("imagepreview")||kind.contains("image_preview")||kind.contains("imageviewer")
                ||"查看原图".equals(n.text)||"保存图片".equals(n.text)||delegated&&n.editable)return false;
        }return true;
    }
    public void reset(){foreground="";entered=lastOopzWindow=delegatedUntil=-1;}
}
