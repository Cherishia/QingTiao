package dev.local.qingtiao;

import java.util.List;

/** A rendered bottom ad sheet can be recognized without a particular app or resource ID. */
final class RenderedAdPanel {
    static int visibleControls(List<AdMatcher.Node> nodes,int w,int h){
        return visibleControls(nodes,w,h,false);
    }
    static int visibleControls(List<AdMatcher.Node> nodes,int w,int h,boolean allowUnlabelledWindow){
        if(nodes==null||nodes.size()>=1000||w<=0||h<w*1.4||h>w*3.0)return 0;
        int visible=0,unlabelled=0;
        boolean hasRenderer=false,hasViewport=false,hasInput=false,hasLabel=false;
        for(AdMatcher.Node n:nodes){
            if(!n.visible)continue;
            if(n.enabled&&!n.editable&&renderer(n.className))hasRenderer=true;
            if(allowUnlabelledWindow){
                hasInput|=n.editable||n.password||n.checkable;
                hasViewport|=n.enabled&&n.className!=null&&!n.className.isEmpty()
                    &&n.right-n.left>=w*.88&&n.bottom-n.top>=h*.48;
            }
            if(n.parent<0&&!allowUnlabelledWindow)continue;
            boolean blank=MatchPolicy.normalize(n.text).isEmpty()&&MatchPolicy.normalize(n.desc).isEmpty();
            hasLabel|=!blank;
            if(n.parent<0)continue;
            visible++;
            if(blank)unlabelled++;
        }
        boolean sparse=visible>0&&unlabelled==visible||visible>=12&&unlabelled*5>=visible*3;
        boolean blankWindow=allowUnlabelledWindow&&hasViewport&&!hasInput&&!hasLabel&&visible>=8&&visible<=80;
        return (hasRenderer&&sparse||blankWindow)&&!AdMatcher.sensitive(nodes)?visible:0;
    }

    static boolean renderer(String type){
        return type!=null&&(type.contains("WebView")||type.contains("SurfaceView")
            ||type.contains("TextureView")||type.contains("Flutter"));
    }

    static boolean headerHeight(int y,int h){return y>=h*.20&&y<=h*.50;}

    private static boolean disclosureNode(AdMatcher.Node n,int w,int h,int y){
        return bodyNode(n,w,h,y)&&n.left<=w*.09&&n.right<=w*.20&&n.bottom-n.top<=h*.05
            &&(BiliPanel.adDisclosure(n.text)||BiliPanel.adDisclosure(n.desc));
    }

    static boolean hasNativeDisclosureBelow(List<AdMatcher.Node> nodes,int w,int h,int headerY){
        for(AdMatcher.Node n:nodes)if(disclosureNode(n,w,h,headerY)
            &&PanelAdLabel.matchesPosition(w,h,headerY,(n.left+n.right)/2,(n.top+n.bottom)/2))return true;
        return false;
    }

    private static AdMatcher.Node matchingDisclosure(List<AdMatcher.Node> nodes,int w,int h,int y,int labelX,int labelY,boolean exact){
        if(labelX<0||labelY<0)return null;
        AdMatcher.Node badge=null;
        int slop=Math.max(2,w/100);
        for(AdMatcher.Node n:nodes){
            if(!disclosureNode(n,w,h,y))continue;
            if(exact){
                if(labelX<n.left-slop||labelX>n.right+slop||labelY<n.top-slop||labelY>n.bottom+slop)continue;
            }else if(Math.abs((n.left+n.right)/2-labelX)>w*.04||Math.abs((n.top+n.bottom)/2-labelY)>w*.065)continue;
            if(badge!=null&&Math.abs(n.top-badge.top)>w*.018)return null;
            badge=n;
        }
        return badge;
    }

    static boolean nativeDisclosureMatches(List<AdMatcher.Node> nodes,int w,int h,int y,int labelX,int labelY){
        return matchingDisclosure(nodes,w,h,y,labelX,labelY,true)!=null;
    }

    /** Combine a disclosure at the same place in pixels and the tree with two current watch affordances. */
    private static boolean hasAdBody(List<AdMatcher.Node> nodes,int w,int h,int y,int labelX,int labelY){
        AdMatcher.Node badge=matchingDisclosure(nodes,w,h,y,labelX,labelY,false);
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
            if(n.visible&&n.left>=0&&n.left<w*.70&&n.right<=w&&n.right>n.left&&n.top<=y&&n.bottom>=y
                &&n.bottom-n.top<=w*.15&&Math.abs((n.top+n.bottom)/2-y)<=w*.025
                &&(BiliPanel.ordinaryTitle(n.text)||BiliPanel.ordinaryTitle(n.desc)))return true;
        }
        return false;
    }

    static String targetProblem(List<AdMatcher.Node> nodes,int w,int h,int x,int y,int labelX,int labelY,boolean rendererContext){
        return targetProblem(nodes,w,h,x,y,labelX,labelY,rendererContext,false);
    }

    static String targetProblem(List<AdMatcher.Node> nodes,int w,int h,int x,int y,int labelX,int labelY,
                                boolean rendererContext,boolean verifiedDisclosure){
        if(nodes==null||nodes.size()>=1000||AdMatcher.sensitive(nodes))return "控件树过大或包含敏感控件";
        if(x<w*.89||x>w*.975||!headerHeight(y,h))return "关闭位置超出面板标题栏范围";
        int margin=(int)(w*.10);
        // With a verified disclosure in the sheet, a nearby background tab is not its title.
        boolean sameRowTitle=ordinaryTitleAtCloseRow(nodes,w,y);
        boolean ordinary=sameRowTitle||!verifiedDisclosure&&BiliPanel.ordinaryHeader(nodes,0,y-margin,w,y+margin,w);
        if(rendererContext&&!ordinary)return "";
        if(!sameRowTitle&&hasAdBody(nodes,w,h,y,labelX,labelY))return "";
        return ordinary?(verifiedDisclosure?"与关闭图标同一标题行出现普通面板标题"
            :"关闭按钮附近出现普通面板标题，广告位置未能排除重叠")
            :"当前控件树未确认绘制层或与画面位置一致的原生面板证据";
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
