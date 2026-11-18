package dev.local.qingtiao;

import java.util.List;

/** A rendered bottom ad sheet can be recognized without a particular app or resource ID. */
final class RenderedAdPanel {
    static int visibleControls(List<AdMatcher.Node> nodes,int w,int h){
        if(nodes==null||nodes.size()>=1000||w<=0||h<w*1.4||h>w*3.0)return 0;
        int visible=0,unlabelled=0;
        boolean hasRenderer=false;
        for(AdMatcher.Node n:nodes){
            if(!n.visible)continue;
            if(n.enabled&&!n.editable&&renderer(n.className))hasRenderer=true;
            if(n.parent<0)continue;
            visible++;
            if(MatchPolicy.normalize(n.text).isEmpty()&&MatchPolicy.normalize(n.desc).isEmpty())unlabelled++;
        }
        boolean sparse=visible>0&&unlabelled==visible||visible>=12&&unlabelled*5>=visible*3;
        return hasRenderer&&sparse&&!AdMatcher.sensitive(nodes)?visible:0;
    }

    static boolean renderer(String type){
        return type!=null&&(type.contains("WebView")||type.contains("SurfaceView")
            ||type.contains("TextureView")||type.contains("Flutter"));
    }

    static boolean headerHeight(int y,int h){return y>=h*.20&&y<=h*.50;}

    /** Combine a disclosure at the same place in pixels and the tree with two current watch affordances. */
    private static boolean hasAdBody(List<AdMatcher.Node> nodes,int w,int h,int y,int labelX,int labelY){
        if(labelX<0||labelY<0)return false;
        AdMatcher.Node badge=null;
        for(AdMatcher.Node n:nodes){
            if(!bodyNode(n,w,h,y)||n.left>w*.09||n.right>w*.20||n.bottom-n.top>h*.05)continue;
            if(!(BiliPanel.adDisclosure(n.text)||BiliPanel.adDisclosure(n.desc)))continue;
            if(Math.abs((n.left+n.right)/2-labelX)>w*.04||Math.abs((n.top+n.bottom)/2-labelY)>w*.065)continue;
            if(badge!=null&&Math.abs(n.top-badge.top)>w*.018)return false;
            badge=n;
        }
        if(badge==null)return false;
        boolean inline=false,footer=false;
        for(AdMatcher.Node n:nodes){
            if(n==badge||!bodyNode(n,w,h,y)||n.bottom-n.top>h*.08
                ||!(BiliPanel.watch(n.text)||BiliPanel.watch(n.desc)))continue;
            if(n.bottom<=badge.top+w*.008&&badge.top-n.bottom<=h*.095&&n.right>=w*.23)inline=true;
            if(n.top>=h*.78&&n.top>badge.bottom&&n.left<=w*.58&&n.right>=w*.42)footer=true;
            if(inline&&footer)return true;
        }
        return false;
    }

    private static boolean bodyNode(AdMatcher.Node n,int w,int h,int y){
        return n.visible&&!n.editable&&!n.password&&!n.checkable&&n.left>=0&&n.right<=w&&n.left<n.right
            &&n.top>=Math.max(h*.45,y+w*.14)&&n.top<n.bottom&&n.bottom<=h;
    }

    private static boolean ordinaryTitleAtCloseRow(List<AdMatcher.Node> nodes,int w,int y){
        for(AdMatcher.Node n:nodes){
            if(n.visible&&n.left>=0&&n.left<w*.23&&n.right>n.left&&n.top<=y&&n.bottom>=y
                &&n.bottom-n.top<=w*.15&&Math.abs((n.top+n.bottom)/2-y)<=w*.025
                &&(BiliPanel.ordinaryTitle(n.text)||BiliPanel.ordinaryTitle(n.desc)))return true;
        }
        return false;
    }

    static String targetProblem(List<AdMatcher.Node> nodes,int w,int h,int x,int y,int labelX,int labelY,boolean rendererContext){
        if(nodes==null||nodes.size()>=1000||AdMatcher.sensitive(nodes))return "控件树过大或包含敏感控件";
        if(x<w*.89||x>w*.975||!headerHeight(y,h))return "关闭位置超出面板标题栏范围";
        int margin=(int)(w*.10);
        boolean ordinary=BiliPanel.ordinaryHeader(nodes,0,y-margin,w,y+margin,w);
        if(rendererContext&&!ordinary)return "";
        if(hasAdBody(nodes,w,h,y,labelX,labelY)&&!ordinaryTitleAtCloseRow(nodes,w,y))return "";
        return ordinary?"关闭按钮附近出现普通面板标题，广告位置未能排除重叠"
            :"当前控件树未保留绘制层，也未确认与画面同位置的广告标识和两处观看入口";
    }

    static BiliVisual.Result detect(int[] pixels,int w,int h){
        BiliVisual.Result close=BiliVisual.detect(pixels,w,h);
        if(!close.found())return close;
        PanelAdLabel.Position label=headerHeight(close.y,h)?PanelAdLabel.find(pixels,w,h,close.y):null;
        if(label==null){
            return new BiliVisual.Result(0,0,0,"绘制广告面板：已匹配标题栏，未确认下方独立「广告」字样与卡片边缘");
        }
        return new BiliVisual.Result(close.x,close.y,close.size,
            "绘制广告面板：已匹配 X、三点、拖动条及下方独立「广告」字样",true,label.x,label.y);
    }

    private RenderedAdPanel(){}
}
