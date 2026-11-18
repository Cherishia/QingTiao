package dev.local.qingtiao;

import java.util.List;
import java.util.Locale;

/** An explicitly named, currently visible full-screen ad container; no owner allow-list. */
public final class AdSurface {
    private static final java.util.regex.Pattern AD_IMAGE=java.util.regex.Pattern.compile("(?:iv_|img_|image_)?advertisement(?:_image|_img)?");
    public static boolean present(List<AdMatcher.Node> nodes,int w,int h){
        if(nodes==null||nodes.size()>=1000||w<=0||h<w*1.4||AdMatcher.sensitive(nodes))return false;
        for(AdMatcher.Node n:nodes){
            if(!n.visible||!n.enabled||n.editable||n.checkable||n.scrollable||n.left<0||n.top<0||n.right>w||n.bottom>h
                ||n.right-n.left<w*.80||n.bottom-n.top<h*.70||(long)(n.right-n.left)*(n.bottom-n.top)<w*(long)h*.65)continue;
            String id=n.id==null?"":n.id.substring(n.id.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
            String cls=n.className==null?"":n.className.toLowerCase(Locale.ROOT);
            if(id.contains("splash_ad")||id.contains("ad_splash")||id.equals("startup_ad")||id.startsWith("startup_ad_")
                ||id.startsWith("tt_splash_")||id.startsWith("ksad_splash_")||id.startsWith("mbridge_splash_")||id.startsWith("gdt_splash_")
                ||cls.endsWith("splashadview")||cls.endsWith("adsplashview")||cls.endsWith("splashadlayout")||cls.endsWith("splashadcontainer")
                ||cls.endsWith("imageview")&&AD_IMAGE.matcher(id).matches())return true;
        }return false;
    }
    public static String trace(List<AdMatcher.Node> nodes,int w,int h){
        StringBuilder out=new StringBuilder("全屏广告容器线索（仅ID/类型，不作点击依据）：");int count=0;
        for(AdMatcher.Node n:nodes){
            String id=n.id==null?"":n.id,cls=n.className==null?"":n.className,s=(id+" "+cls).toLowerCase(Locale.ROOT);
            if(!n.visible||n.right-n.left<w*.8||n.bottom-n.top<h*.7||!(s.contains("ad")||s.contains("splash")||s.contains("startup")))continue;
            out.append("\n容器 ").append(++count).append("：ID=").append(id.substring(0,Math.min(100,id.length()))).append("，类型=").append(cls.substring(0,Math.min(100,cls.length())))
                .append("，位置=").append(n.left).append(',').append(n.top).append(',').append(n.right).append(',').append(n.bottom);
            if(count==4)break;
        }
        if(count==0)out.append("未读到明确线索");return out.toString();
    }
    private AdSurface(){}
}
