package dev.local.qingtiao;

import java.util.List;

/** Current bounded ad scope: a search optimization and recheck, never a close coordinate. */
public final class BiliPanel {
    public static final class Hint {
        public final int left,top,right,bottom,width,height;
        Hint(AdMatcher.Node n,int w,int h){left=n.left;top=n.top;right=n.right;bottom=n.bottom;width=w;height=h;}
        public String key(){return left+","+top+","+right+","+bottom;}
        public boolean headerPoint(int x,int y){
            return x>=right-width*.16&&x<=right-width*.015&&y>=top+height*.008&&y<=Math.min(bottom,top+Math.min(height*.10,width*.18));
        }
    }
    private static final class Scope {
        Hint hint;
        boolean blocked,competing;
    }
    static final class Inspection {
        static final Inspection NONE=new Inspection(null,false,false);
        final Hint hint;
        final boolean nativeEvidence,pixelLabelContext;
        Inspection(Hint hint,boolean nativeEvidence,boolean pixelLabelContext){
            this.hint=hint;
            this.nativeEvidence=nativeEvidence;
            this.pixelLabelContext=pixelLabelContext;
        }
    }
    /** Reuses the native scope only within this scan. A later action still reads a fresh tree. */
    static Inspection inspect(List<AdMatcher.Node> nodes,int w,int h,boolean visual){
        if(!eligible(nodes,w,h))return Inspection.NONE;
        Scope current=scope(nodes,w,h);
        boolean nativeEvidence=visual&&nativeEvidence(nodes,w,h,current);
        boolean drawn=visual&&!nativeEvidence&&(pixelLabelContext(nodes,current)
            ||!current.competing&&drawnHeaderEvidence(nodes,w,h));
        return new Inspection(current.hint,nativeEvidence,drawn);
    }
    private static boolean active(AdMatcher.Node n){return n.visible&&n.enabled&&!n.password&&!n.editable&&!n.checkable;}
    private static boolean inside(AdMatcher.Node n,AdMatcher.Node b){return n.right>n.left&&n.bottom>n.top&&n.left>=b.left&&n.top>=b.top&&n.right<=b.right&&n.bottom<=b.bottom;}
    private static boolean insideScreen(AdMatcher.Node n,int w,int h){return n.right>n.left&&n.bottom>n.top&&n.left>=0&&n.top>=0&&n.right<=w&&n.bottom<=h;}
    private static boolean eligible(List<AdMatcher.Node> nodes,int w,int h){
        return nodes!=null&&!nodes.isEmpty()&&nodes.size()<1000&&w>0&&h>=w*1.4&&h<=w*3.0&&!AdMatcher.sensitive(nodes);
    }
    private static boolean contains(List<AdMatcher.Node> nodes,int index,int scope){
        for(int depth=0;index>=0&&index<nodes.size()&&depth<24;depth++){
            if(index==scope)return true;
            int parent=nodes.get(index).parent;if(parent==index)break;index=parent;
        }
        return false;
    }
    static boolean ordinaryTitle(String text){
        if(text==null||text.length()>16)return false;
        switch(MatchPolicy.normalize(text)){
            case "评论":case "评论区":case "全部评论":case "热门评论":case "全部回覆":case "全部回复":case "評論":case "全部評論":case "全部回復":
            case "分享":case "分享到":case "分享至":case "简介":case "簡介":case "设置":case "設定":case "播放设置":case "播放設定":case "播放列表":
            case "弹幕设置":case "弹幕列表":case "彈幕設定":case "彈幕列表":case "举报":case "舉報":case "相关推荐":case "相關推薦":
            case "选择清晰度":case "選擇畫質":case "清晰度":case "投屏":case "投幣":case "投币":case "收藏":case "下载缓存":case "下載緩存":
                return true;
            default:return false;
        }
    }
    static boolean ordinaryHeader(List<AdMatcher.Node> nodes,int left,int top,int right,int limit,int w){
        for(AdMatcher.Node n:nodes)if(n.visible&&n.left>=left&&n.right<=right&&n.left<left+w*.70
            &&n.top>=top&&n.bottom<=limit&&n.bottom>n.top&&n.right>n.left&&(ordinaryTitle(n.text)||ordinaryTitle(n.desc)))return true;
        return false;
    }
    private static boolean sheet(AdMatcher.Node b,int w,int h){
        return active(b)&&insideScreen(b,w,h)&&b.left<=w*.10&&b.right>=w*.90
            &&b.top>=h*.16&&b.top<=h*.70&&b.bottom>=h*.90&&b.bottom-b.top>=h*.28;
    }
    private static Scope scope(List<AdMatcher.Node> nodes,int w,int h){
        Scope result=new Scope();
        Hint chosen=null;
        for(int i=0;i<nodes.size();i++)if(active(nodes.get(i))&&nodes.get(i).evidence()) {
            AdMatcher.Node ad=nodes.get(i);Hint outer=null;
            for(int p=ad.parent,d=0;p>=0&&p<nodes.size()&&d<16;d++){
                AdMatcher.Node b=nodes.get(p);
                if(sheet(b,w,h)
                    &&inside(ad,b)&&ad.top>=b.top+h*.08&&(outer==null||b.top<outer.top))outer=new Hint(b,w,h);
                if(b.parent==p)break;p=b.parent;
            }
            if(outer!=null){if(chosen!=null&&!same(chosen,outer)){result.blocked=true;result.competing=true;return result;}chosen=outer;}
        }
        if(chosen!=null&&ordinaryHeader(nodes,chosen.left,chosen.top,chosen.right,
            chosen.top+(int)Math.min(h*.105,w*.19),w)){result.blocked=true;return result;}
        result.hint=chosen;return result;
    }
    public static Hint find(List<AdMatcher.Node> nodes,int w,int h){
        return eligible(nodes,w,h)?scope(nodes,w,h).hint:null;
    }
    static boolean watch(String text){
        if(text==null||text.length()>16)return false;
        String s=MatchPolicy.normalize(text);return s.equals("前往观看")||s.equals("前往觀看");
    }
    private static boolean localizedEvidence(List<AdMatcher.Node> nodes,int w,int h,int y){
        // Flat trees have no usable panel ancestor. Both the visible ad mark and the dedicated
        // watch CTA must be in the panel body, and must be different accessibility nodes.
        int body=Math.max((int)(h*.38),y+(int)(w*.07));int adTop=h,ctaBottom=0;
        for(int i=0;i<nodes.size();i++){
            AdMatcher.Node n=nodes.get(i);
            if(!active(n)||!insideScreen(n,w,h)||n.top<body||n.bottom-n.top>h*.13)continue;
            if(watch(n.text)||watch(n.desc)){
                if(n.top>=h*.78&&n.left<=w*.66&&n.right>=w*.34)ctaBottom=Math.max(ctaBottom,n.bottom);
            } else if(n.evidence()&&n.right-n.left<=w*.45)adTop=Math.min(adTop,n.top);
            if(adTop<ctaBottom)return true;
        }
        return false;
    }
    /** Some video ads place their watch affordance inline, just above the disclosure. */
    private static boolean inlineVideoEvidence(List<AdMatcher.Node> nodes,int w,int h,int y){
        int body=Math.max((int)(h*.48),y+(int)(w*.13));
        for(AdMatcher.Node badge:nodes){
            if(!active(badge)||!insideScreen(badge,w,h)||badge.top<body||badge.bottom>h*.88
                ||badge.left>w*.09||badge.right>w*.20||badge.bottom-badge.top>h*.05
                ||!(adDisclosure(badge.text)||adDisclosure(badge.desc)))continue;
            for(AdMatcher.Node action:nodes){
                if(action==badge||!active(action)||!insideScreen(action,w,h)||action.top<body
                    ||action.bottom>badge.top+w*.006||badge.top-action.bottom>h*.095
                    ||action.bottom-action.top>h*.075||action.right<badge.right+w*.04)continue;
                if(watch(action.text)||watch(action.desc))return true;
            }
        }
        return false;
    }
    static boolean adDisclosure(String value){
        if(value==null||value.length()>12)return false;
        String text=MatchPolicy.normalize(value);
        return "广告".equals(text)||"廣告".equals(text);
    }
    /**
     * Some pause cards expose neither their CTA nor their close description to accessibility.
     * Their only stable tree evidence is an ad mark plus one dedicated right-header touch area.
     * Pixels still have to confirm the X, menu, dark header and drag handle.
     */
    private static AdMatcher.Node flatPauseHeaderControl(List<AdMatcher.Node> nodes,int w,int h){
        boolean ad=false;AdMatcher.Node chosen=null;
        for(int i=1;i<nodes.size();i++){
            AdMatcher.Node n=nodes.get(i);
            if(active(n)&&insideScreen(n,w,h)&&n.evidence()&&n.top>=h*.28&&n.bottom<=h*.96)ad=true;
            if(!active(n)||!n.clickable||n.scrollable||!insideScreen(n,w,h)
                ||n.left<w*.84||n.right<w*.92||n.top<h*.19||n.bottom>h*.50
                ||n.right-n.left>w*.16||n.bottom-n.top>h*.08)continue;
            String id=n.id==null?"":n.id.toLowerCase(java.util.Locale.ROOT);
            if(id.contains("share")||id.contains("follow")||id.contains("arrow")||id.contains("more")||id.contains("menu"))continue;
            if(n.parent<=0||n.parent>=nodes.size())continue;
            AdMatcher.Node header=nodes.get(n.parent);int headerWidth=header.right-header.left,headerHeight=header.bottom-header.top;
            if(!active(header)||!insideScreen(header,w,h)||!inside(n,header)
                ||header.left<w*.55||header.right<w*.95||header.top<h*.18||header.bottom>h*.51
                ||headerWidth<w*.20||headerWidth>w*.48||headerHeight>h*.10)continue;
            if(chosen!=null)return null;
            chosen=n;
        }
        return ad?chosen:null;
    }
    private static boolean flatPauseEvidence(List<AdMatcher.Node> nodes,int w,int h){
        return flatPauseHeaderControl(nodes,w,h)!=null;
    }
    public static boolean canProbe(List<AdMatcher.Node> nodes,int w,int h){
        if(!eligible(nodes,w,h))return false;
        return nativeEvidence(nodes,w,h,scope(nodes,w,h));
    }
    private static boolean nativeEvidence(List<AdMatcher.Node> nodes,int w,int h,Scope scope){
        return !scope.blocked&&(scope.hint!=null||localizedEvidence(nodes,w,h,0)
            ||inlineVideoEvidence(nodes,w,h,0)||flatPauseEvidence(nodes,w,h));
    }
    /** A drawn tree can justify a bounded probe, while the frames must establish the ad. */
    static boolean drawnContext(List<AdMatcher.Node> nodes,int w,int h){
        return eligible(nodes,w,h)&&hasUnlabelledRenderer(nodes);
    }
    private static boolean hasUnlabelledRenderer(List<AdMatcher.Node> nodes){
        boolean renderer=false;
        for(AdMatcher.Node n:nodes){
            if(!n.visible)continue;
            if(!MatchPolicy.normalize(n.text).isEmpty()||!MatchPolicy.normalize(n.desc).isEmpty()||n.evidence())return false;
            if(active(n)&&RenderedAdPanel.renderer(n.className))renderer=true;
        }
        return renderer;
    }
    static boolean pixelLabelContext(List<AdMatcher.Node> nodes,int w,int h){
        return eligible(nodes,w,h)&&pixelLabelContext(nodes,scope(nodes,w,h));
    }
    private static boolean pixelLabelContext(List<AdMatcher.Node> nodes,Scope scope){
        if(hasUnlabelledRenderer(nodes))return true;
        if(scope.blocked)return false;
        boolean disclosure=false,action=false;
        for(AdMatcher.Node n:nodes){
            if(!active(n))continue;
            if(adDisclosure(n.text)||adDisclosure(n.desc))disclosure=true;
            else if(watch(n.text)||watch(n.desc))action=true;
            if(disclosure&&action)return true;
        }
        return false;
    }
    private static boolean anonymousHeaderControl(AdMatcher.Node node,int w,int h){
        int width=node.right-node.left,height=node.bottom-node.top;
        return active(node)&&node.clickable&&!node.scrollable&&insideScreen(node,w,h)
            &&"android.view.View".equals(node.className)&&(node.id==null||node.id.isEmpty())
            &&MatchPolicy.normalize(node.text).isEmpty()&&MatchPolicy.normalize(node.desc).isEmpty()
            &&width>=w*.06&&width<=w*.17&&height>=w*.06&&height<=w*.17;
    }
    private static boolean drawnHeaderEvidence(List<AdMatcher.Node> nodes,int w,int h){
        AdMatcher.Node header=drawnHeaderPair(nodes,w,h,-1,-1);
        return header!=null&&RenderedAdPanel.hasNativeDisclosureBelow(nodes,w,h,(header.top+header.bottom)/2);
    }

    /** Native header geometry can start a probe; matching live pixels establish the purpose of the controls. */
    private static AdMatcher.Node drawnHeaderPair(List<AdMatcher.Node> nodes,int w,int h,int x,int y){
        AdMatcher.Node close=null;
        for(AdMatcher.Node n:nodes){
            int centerY=(n.top+n.bottom)/2;
            if(n.parent<0||n.parent>=nodes.size()||n.left<w*.85||n.right<w*.94
                ||!RenderedAdPanel.headerHeight(centerY,h)
                ||x>=0&&(Math.abs((n.left+n.right)/2-x)>w*.03||Math.abs(centerY-y)>w*.025)
                ||!anonymousHeaderControl(n,w,h))continue;
            if(close!=null)return null;
            close=n;
        }
        if(close==null)return null;
        AdMatcher.Node row=nodes.get(close.parent);
        if(!active(row)||!insideScreen(row,w,h)||!inside(close,row)||row.left>w*.12||row.right<w*.94
            ||row.bottom-row.top>w*.23)return null;
        int neighborCount=0;
        for(AdMatcher.Node n:nodes){
            if(n==close||n.parent!=close.parent||n.left<w*.70||n.left>=close.left
                ||Math.abs(n.right-close.left)>w*.025||Math.abs(n.top-close.top)>w*.015
                ||Math.abs(n.bottom-close.bottom)>w*.015||!inside(n,row)||!anonymousHeaderControl(n,w,h))continue;
            if(++neighborCount>1)return null;
        }
        return neighborCount==1?close:null;
    }

    static String drawnTargetProblem(List<AdMatcher.Node> nodes,int w,int h,int x,int y,int labelX,int labelY){
        if(!eligible(nodes,w,h))return "控件树过大、包含敏感控件或方向不适用";
        Scope current=scope(nodes,w,h);
        if(current.competing)return "多个原生广告面板范围重叠";
        boolean verifiedDisclosure=PanelAdLabel.matchesPosition(w,h,y,labelX,labelY);
        boolean pairedHeader=verifiedDisclosure&&RenderedAdPanel.nativeDisclosureMatches(nodes,w,h,y,labelX,labelY)
            &&drawnHeaderPair(nodes,w,h,x,y)!=null;
        boolean context=pairedHeader||!current.blocked&&(pixelLabelContext(nodes,current)||RenderedAdPanel.visibleControls(nodes,w,h,true)>0);
        return RenderedAdPanel.targetProblem(nodes,w,h,x,y,labelX,labelY,context,verifiedDisclosure);
    }

    public static boolean confirmTarget(List<AdMatcher.Node> nodes,int w,int h,Hint expected,int x,int y){
        if(!eligible(nodes,w,h)||x<w*.89||x>w*.98||y<h*.19||y>h*.785)return false;
        Scope s=scope(nodes,w,h);
        if(s.blocked)return false;
        if(expected!=null)return same(expected,s.hint)&&s.hint.headerPoint(x,y);
        if(s.hint!=null)return s.hint.headerPoint(x,y);
        // The unscoped fallback may only use the old, familiar panel area, not arbitrary Xs.
        if(y<=h*.63&&!ordinaryHeader(nodes,0,Math.max(0,y-(int)(w*.10)),w,y+(int)(w*.10),w)
            &&(localizedEvidence(nodes,w,h,y)||inlineVideoEvidence(nodes,w,h,y)))return true;
        AdMatcher.Node control=flatPauseHeaderControl(nodes,w,h);
        return control!=null&&x>=control.left&&x<=control.right
            &&y>=control.top-h*.02&&y<=control.bottom+h*.035;
    }
    private static boolean closeLabel(String text){
        if(text==null||text.length()>32)return false;
        switch(MatchPolicy.normalize(text).toLowerCase(java.util.Locale.ROOT)){
            case "关闭广告":case "關閉廣告":case "关闭暂停广告":case "關閉暫停廣告":case "closead":case "closepausead":return true;
            default:return false;
        }
    }
    private static boolean compatibleClose(String raw){
        if(raw==null||raw.isEmpty()||closeLabel(raw))return true;
        if(raw.length()>12)return false;
        switch(MatchPolicy.normalize(raw).toLowerCase(java.util.Locale.ROOT)){
            case "":case "×":case "✕":case "x":case "关闭":case "關閉":case "按钮":case "按鈕":case "button":return true;
            default:return false;
        }
    }
    public static boolean currentNativePauseControl(AdMatcher.Node n){
        return n!=null&&active(n)&&n.clickable&&!n.scrollable&&n.right>n.left&&n.bottom>n.top
            &&(closeLabel(n.text)||closeLabel(n.desc))&&compatibleClose(n.text)&&compatibleClose(n.desc);
    }
    static boolean currentScopedCloseControl(AdMatcher.Node n){
        if(n==null||!active(n)||!n.clickable||n.scrollable||!compatibleClose(n.text)||!compatibleClose(n.desc))return false;
        String t=MatchPolicy.normalize(n.text),d=MatchPolicy.normalize(n.desc);
        return currentNativePauseControl(n)||"关闭".equals(t)||"關閉".equals(t)||"关闭".equals(d)||"關閉".equals(d);
    }
    public static int nativePauseClose(List<AdMatcher.Node> nodes,int w,int h){
        if(!eligible(nodes,w,h))return -1;
        int chosen=-1;
        for(int i=0;i<nodes.size();i++){
            AdMatcher.Node n=nodes.get(i);
            if(!currentNativePauseControl(n)||!insideScreen(n,w,h)||n.right-n.left>w*.18||n.bottom-n.top>h*.08)continue;
            for(int p=n.parent,d=0;p>=0&&p<nodes.size()&&d<12;d++){
                AdMatcher.Node b=nodes.get(p);int bw=b.right-b.left,bh=b.bottom-b.top;
                if(active(b)&&!b.scrollable&&b.top>=h*.05&&b.bottom<=h*.78&&bw>=w*.30&&bh>=h*.10&&bh<=h*.53
                    &&insideScreen(b,w,h)&&inside(n,b)&&n.top>=b.top&&n.bottom<=b.top+bh*.43
                    &&n.left>=b.right-bw*.26&&!ordinaryHeader(nodes,b.left,b.top,b.right,b.top+(int)(bh*.35),w)){
                    boolean evidence=false;
                    for(int j=0;j<nodes.size();j++)if(j!=i&&j!=p){
                        AdMatcher.Node ad=nodes.get(j);
                        if(active(ad)&&ad.evidence()&&inside(ad,b)&&contains(nodes,j,p)&&!contains(nodes,j,i)){
                            evidence=true;break;
                        }
                    }
                    if(evidence){
                        if(chosen>=0&&chosen!=i)return -1;
                        chosen=i;break;
                    }
                }
                if(b.parent==p)break;p=b.parent;
            }
        }
        return chosen;
    }
    public static boolean same(Hint a,Hint b){return a!=null&&b!=null&&a.width==b.width&&a.height==b.height&&a.left==b.left&&a.top==b.top&&a.right==b.right&&a.bottom==b.bottom;}
    private BiliPanel(){}
}
