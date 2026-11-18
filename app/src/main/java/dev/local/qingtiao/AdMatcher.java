package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Snapshot-only matching, shared by the Android service and JVM tests. */
public final class AdMatcher {
    private static final java.util.regex.Pattern CAMEL=java.util.regex.Pattern.compile("([a-z0-9])([A-Z])");
    private static final java.util.regex.Pattern CLOSE_ID=java.util.regex.Pattern.compile("(?:[a-z0-9]+_)*close(?:_[a-z0-9]+)*");
    private static final java.util.regex.Pattern WITH_DIGIT=java.util.regex.Pattern.compile(".*[0-9].*");
    public static final class Node {
        public int parent, left,top,right,bottom;
        public long sampledAt;
        public String text="",desc="",id="",className="";
        public boolean visible=true,enabled=true,password,scrollable,editable,checkable,clickable;
        public Node(int parent,int l,int t,int r,int b,String text,String id) {
            this.parent=parent;left=l;top=t;right=r;bottom=b;this.text=text;this.id=id;
        }
        public boolean evidence() {return visible&&MatchPolicy.isAdEvidence(text,desc,id);}
    }
    private static final String[] SDK_CLOSE={"tt_video_ad_close_layout","tt_bu_close",
        "ksad_splash_close_btn","ksad_interstitial_close_btn","mbridge_interstitial_iv_close",
        "mbridge_iv_close","gdt_interstitial_close"};
    public static boolean closeText(String value) {
        String s=MatchPolicy.normalize(value);
        return s.equals("关闭")||s.equals("关闭广告")||s.equals("关闭按钮")||s.equals("关闭弹窗")
            ||s.equals("關閉")||s.equals("關閉廣告")||s.equals("關閉按鈕")||s.equals("關閉彈窗")||s.equalsIgnoreCase("closead")
            ||s.equals("×")||s.equals("✕")||s.equals("✖")||s.equals("X")||s.equals("x");
    }
    private static boolean sdkClose(String id) {
        if(id==null) return false;String s=id.substring(id.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
        for(String a:SDK_CLOSE) if(s.equals(a)) return true;return false;
    }
    private static boolean closeId(String id) {
        if(id==null) return false;String s=id.substring(id.lastIndexOf('/')+1).toLowerCase(Locale.ROOT);
        return s.equals("iv_close")||s.equals("btn_close")||s.equals("close")||s.equals("close_btn")
            ||s.equals("ad_close")||s.equals("iv_ad_close")||s.equals("close_button")||s.equals("img_close");
    }
    public static boolean closeControl(Node n) {
        return closeText(n.text)||closeText(n.desc)||closeId(n.id);
    }
    private static boolean explicitAdClose(Node n) {
        for(String value:new String[]{n.text,n.desc}) {
            String s=MatchPolicy.normalize(value);
            if(s.equals("关闭广告")||s.equals("關閉廣告")||s.equalsIgnoreCase("closead"))return true;
        }
        return false;
    }
    public static boolean biliCloseControl(Node n) {
        if(closeControl(n))return true;
        for(String value:new String[]{n.text,n.desc}) {
            String label=MatchPolicy.normalize(value).toLowerCase(Locale.ROOT);
            if(label.equals("close")||label.equals("closead")||label.equals("closebutton")||label.equals("关闭推广")
                ||label.equals("关闭广告面板")||label.equals("关闭卡片")||label.equals("关闭推荐卡片")||label.equals("收起广告"))return true;
        }
        String id=n.id==null?"":CAMEL.matcher(n.id.substring(n.id.lastIndexOf('/')+1)).replaceAll("$1_$2").replace('-','_').toLowerCase(Locale.ROOT);
        // Semantic ID tokens are hints, never a stand-alone Bilibili ad rule.
        return CLOSE_ID.matcher(id).matches();
    }
    private static boolean inside(Node n,Node box) {
        return n.right>n.left&&n.bottom>n.top&&n.left>=box.left&&n.top>=box.top
            &&n.right<=box.right&&n.bottom<=box.bottom;
    }
    private static boolean watchAction(Node n) {
        return MatchPolicy.normalize(n.text).equals("前往观看")||MatchPolicy.normalize(n.desc).equals("前往观看");
    }
    private static boolean iconLabel(String text,boolean more) {
        String s=MatchPolicy.normalize(text).toLowerCase(Locale.ROOT);
        return s.isEmpty()||s.equals("图片")||s.equals("按钮")
            ||(more&&(s.equals("更多")||s.equals("更多选项")||s.equals("more")||s.equals("menu")||s.equals("⋮")||s.equals("⋯")||s.equals("…")));
    }
    private static boolean wrongIconId(String id,boolean more) {
        String s=id==null?"":id.toLowerCase(Locale.ROOT);
        for(String hint:new String[]{"share","report","feedback","detail","watch","play","like","collect","comment","download","shop","buy","back","fullscreen"})
            if(s.contains(hint))return true;
        return !more&&(s.contains("more")||s.contains("menu"));
    }
    private static boolean imageClass(Node n) {
        String c=n.className==null?"":n.className.toLowerCase(Locale.ROOT);
        return c.endsWith("imageview")||c.endsWith("imagebutton")||c.endsWith("button");
    }
    private static boolean iconButton(List<Node> nodes,int index,int w,int h,boolean more) {
        Node n=nodes.get(index);int width=n.right-n.left,height=n.bottom-n.top;
        if(!n.visible||!n.enabled||!n.clickable||n.editable||n.checkable||n.scrollable||!small(n,w,h)
            ||width<w*.025||width>w*.14||height>h*.07||width<height*.5||width>height*2
            ||!iconLabel(n.text,more)||!iconLabel(n.desc,more)||wrongIconId(n.id,more))return false;
        boolean image=imageClass(n);
        for(int j=1;j<nodes.size();j++)if(j!=index&&descendant(nodes,j,index)) {
            Node child=nodes.get(j);if(!child.visible)continue;
            if(!inside(child,n)||!iconLabel(child.text,more)||!iconLabel(child.desc,more)||wrongIconId(child.id,more))return false;
            if(imageClass(child))image=true;
        }
        return image||"android.view.View".equals(n.className);
    }
    private static boolean rightHeaderIcon(Node n,Node box,int w,int h) {
        return inside(n,box)&&n.left>=box.right-w*.15&&n.right>=box.right-w*.06
            &&(n.left+n.right)/2f>=box.right-w*.11
            &&n.top>=box.top&&n.bottom<=box.top+Math.min(h*.12,(box.bottom-box.top)*.22);
    }
    private static boolean pairedIconClose(List<Node> nodes,int index,int scope,int w,int h) {
        Node target=nodes.get(index),box=nodes.get(scope);
        if(!iconButton(nodes,index,w,h,false)||!rightHeaderIcon(target,box,w,h))return false;
        float x=(target.left+target.right)/2f,y=(target.top+target.bottom)/2f;
        boolean pair=false;
        for(int j=1;j<nodes.size();j++)if(j!=index&&descendant(nodes,j,scope)) {
            Node n=nodes.get(j);if(!n.visible||!inside(n,box))continue;
            float cx=(n.left+n.right)/2f,cy=(n.top+n.bottom)/2f;
            if(Math.abs(y-cy)>Math.max(target.bottom-target.top,n.bottom-n.top)*.35f)continue;
            if((biliCloseControl(n)||iconButton(nodes,j,w,h,false))&&rightHeaderIcon(n,box,w,h)) {
                boolean sameSpot=Math.abs(x-cx)<=w*.01&&Math.abs(y-cy)<=h*.006;
                // A nested parent and child may expose the same touch target twice. Prefer the first.
                if(!sameSpot||j<index)return false;
                continue;
            }
            float gap=x-cx;
            float widthRatio=(n.right-n.left)/(float)(target.right-target.left);
            float heightRatio=(n.bottom-n.top)/(float)(target.bottom-target.top);
            if(gap>=w*.06&&gap<=w*.18&&n.right<=target.left&&n.top>=box.top
                &&widthRatio>=.6f&&widthRatio<=1.7f&&heightRatio>=.6f&&heightRatio<=1.7f
                &&iconButton(nodes,j,w,h,true))pair=true;
        }
        return pair;
    }
    public static boolean biliSheetClose(List<Node> nodes,int index,int w,int h) {
        if(index<=0||index>=nodes.size()||w<=0||h<w*1.2)return false;
        return biliSheetClose(nodes,index,w,h,biliCloseControl(nodes.get(index)));
    }
    private static boolean biliSheetClose(List<Node> nodes,int index,int w,int h,boolean semantic){
        Node target=nodes.get(index);
        if(!semantic&&(target.left<w*.75||target.top<h*.16||target.bottom>h*.74))return false;
        if(!target.visible||!target.enabled||target.editable||target.checkable||(!semantic&&!iconButton(nodes,index,w,h,false))
            ||!small(target,w,h)||target.right-target.left>w*.20||target.bottom-target.top>h*.08)return false;
        int parent=target.parent;
        for(int depth=0;parent>0&&parent<nodes.size()&&depth<12;depth++) {
            Node box=nodes.get(parent);
            boolean sheet=box.visible&&box.left>=0&&box.left<=w*.10&&box.right>=w*.90&&box.right<=w
                &&box.top>=h*.16&&box.top<=h*.62&&box.bottom>=h*.85&&box.bottom<=h
                &&box.bottom-box.top>=h*.35&&inside(target,box);
            boolean header=target.left>=box.right-w*.20&&target.bottom<=box.top+Math.min(h*.12,(box.bottom-box.top)*.22);
            if(sheet&&header) {
                boolean ad=false;
                for(int j=1;j<nodes.size();j++) {
                    Node n=nodes.get(j);
                    if(!n.visible||!inside(n,box)||n.top<target.bottom||!descendant(nodes,j,parent))continue;
                    if(n.evidence()){ad=true;break;}
                }
                // A scrollable recommendation list is allowed only in this bounded, explicitly marked ad sheet.
                if(ad&&(semantic||pairedIconClose(nodes,index,parent,w,h)))return true;
            }
            parent=box.parent;
        }
        return false;
    }
    public static String biliSummary(List<Node> nodes,int w,int h) {
        int ads=0,watch=0,close=0,icons=0,matched=0;
        StringBuilder cues=new StringBuilder();
        for(int i=1;i<nodes.size();i++) {
            Node n=nodes.get(i);if(!n.visible)continue;
            if(n.evidence()&&++ads<=3)appendBiliCue(cues,"广告标识",n);
            if(watchAction(n)&&++watch<=3)appendBiliCue(cues,"前往观看",n);
            boolean semantic=biliCloseControl(n);
            if(semantic)close++;
            if(n.left>=w*.75&&n.top>=h*.16&&n.bottom<=h*.74&&iconButton(nodes,i,w,h,false))icons++;
            if(w>0&&h>=w*1.2&&biliSheetClose(nodes,i,w,h,semantic))matched++;
        }
        return "B站广告面板：广告标识="+ads+"，前往观看="+watch+"，关闭语义="+close+"，右侧无文字图标="+icons+"，符合面板条件="+matched+cues;
    }
    private static void appendBiliCue(StringBuilder out,String type,Node n){
        out.append('\n').append(type).append("位置=").append(n.left).append(',').append(n.top).append(',').append(n.right).append(',').append(n.bottom);
    }
    public static String biliIconTrace(List<Node> nodes,int w,int h) {
        StringBuilder out=new StringBuilder("B站图标探测（仅控件类型、ID与位置）：");int count=0;
        for(int i=1;i<nodes.size()&&count<8;i++) {
            Node n=nodes.get(i);
            if(!n.visible||!small(n,w,h)||n.left<w*.70||n.top<h*.16||n.bottom>h*.74
                ||(!n.clickable&&!imageClass(n)))continue;
            count++;out.append("\n图标 ").append(count).append("：节点=").append(i).append("，父节点=").append(n.parent)
                .append("，类型=").append(shortValue(n.className,80))
                .append("，ID=").append(shortValue(n.id,120)).append("，可点击=").append(n.clickable)
                .append("，空标签=").append(iconLabel(n.text,false)&&iconLabel(n.desc,false))
                .append("，位置=").append(n.left).append(',').append(n.top).append(',').append(n.right).append(',').append(n.bottom);
            if(n.parent>0&&n.parent<nodes.size()) {Node p=nodes.get(n.parent);out.append("，父区域=")
                .append(p.left).append(',').append(p.top).append(',').append(p.right).append(',').append(p.bottom);}
            out.append("，面板匹配=").append(biliSheetClose(nodes,i,w,h));
        }
        if(count==0)out.append("未读到此范围内的小型图标控件");
        return out.toString();
    }
    private static String shortValue(String s,int max){return s==null?"null":s.length()>max?s.substring(0,max):s;}
    public static boolean biliAdHint(List<Node> nodes) {
        for(Node n:nodes)if(n.visible&&(n.evidence()||watchAction(n)))return true;
        return false;
    }
    public static boolean biliVisualContext(List<Node> nodes) {
        // The CTA can arrive after the close icon. Ad evidence alone starts a bounded visual probe.
        return biliAdHint(nodes)&&!sensitive(nodes);
    }
    private static boolean small(Node n,int w,int h) {
        return n.left>=0 && n.top>=0 && n.right<=w && n.bottom<=h && n.right>n.left && n.bottom>n.top
            && n.right-n.left<=w*.32 && n.bottom-n.top<=h*.10;
    }
    private static boolean descendant(List<Node> nodes,int child,int ancestor) {
        for(int step=0;child>=0 && child<nodes.size() && step<20;step++) {
            if(child==ancestor) return true;child=nodes.get(child).parent;
        }
        return false;
    }
    private static boolean popupScope(List<Node> nodes,int index,int w,int h) {
        Node target=nodes.get(index);int parent=target.parent;
        for(int depth=0;parent>0 && parent<nodes.size() && depth<5;depth++) {
            Node box=nodes.get(parent);long area=(long)(box.right-box.left)*(box.bottom-box.top);
            boolean partial=area>0 && area<(long)w*h*.85 && box.right-box.left>=w*.20;
            // A generic X must belong to the same bounded advertisement container and sit at its upper right.
            boolean corner=target.left>=box.left+(box.right-box.left)*.55
                && target.bottom<=box.top+(box.bottom-box.top)*.40
                && target.left>=box.left && target.right<=box.right && target.top>=box.top && target.bottom<=box.bottom;
            if(partial && corner) {
                boolean scrolling=false,ad=false;
                for(int j=0;j<nodes.size();j++) if(descendant(nodes,j,parent)) {
                    if(nodes.get(j).visible && nodes.get(j).scrollable) scrolling=true;
                    if(nodes.get(j).evidence()) ad=true;
                }
                if(ad&&!scrolling) return true;
            }
            parent=box.parent;
        }
        return false;
    }
    public static boolean sensitive(List<Node> nodes) {
        for(Node n:nodes) if(n.visible) {
            if(n.password) return true;
            String s=MatchPolicy.normalize(n.text);
            if(s.equals("支付密码")||s.equals("确认付款")||s.equals("确认支付")||s.equals("授权登录")
                ||s.equals("身份验证")||s.equals("监护人同意")||s.equals("已满14岁")) return true;
        }
        return false;
    }
    private static boolean countedSkip(String value) {
        String label=MatchPolicy.normalize(value);
        return MatchPolicy.isSkipText(label)&&WITH_DIGIT.matcher(label).matches();
    }
    /** A unique timed corner skip can identify a splash when the ad label is inaccessible. */
    public static boolean timedSplashSkip(Node n,int w,int h,boolean startup,String pkg) {
        return startup&&pkg!=null&&!pkg.isEmpty()
            &&n.visible&&n.enabled&&n.clickable&&!n.editable&&!n.checkable&&!n.scrollable&&small(n,w,h)
            &&n.left>=w*.72&&n.right>=w*.90&&n.bottom<=h*.12
            &&n.right-n.left>=w*.035&&n.right-n.left<=w*.26
            &&n.bottom-n.top>=h*.008&&n.bottom-n.top<=h*.065
            &&n.right-n.left>=(n.bottom-n.top)*1.2&&n.right-n.left<=(n.bottom-n.top)*5
            &&(countedSkip(n.text)||countedSkip(n.desc));
    }
    /** Explicit ad-dismissal wording is evidence even when a separate ad logo is inaccessible. */
    public static boolean explicitSplashSkip(Node n,int w,int h,boolean startup) {
        return startup&&w>0&&h>0&&n.visible&&n.enabled&&!n.editable&&!n.checkable&&!n.scrollable&&small(n,w,h)
            &&n.left>=w*.70&&n.right>=w*.87&&n.right<=w&&n.top>=h*.025&&n.bottom<=h*.14
            &&n.right-n.left>=w*.05&&n.right-n.left<=w*.28
            &&n.bottom-n.top>=h*.008&&n.bottom-n.top<=h*.06
            &&n.right-n.left>=(n.bottom-n.top)*1.2&&n.right-n.left<=(n.bottom-n.top)*8
            &&(MatchPolicy.explicitAdSkip(n.text)||MatchPolicy.explicitAdSkip(n.desc));
    }
    public static List<Integer> candidates(List<Node> nodes,int w,int h,boolean splashWindow,boolean browsing) {
        return candidates(nodes,w,h,splashWindow,browsing,false,"");
    }
    public static List<Integer> candidates(List<Node> nodes,int w,int h,boolean splashWindow,boolean browsing,boolean enhanced,String pkg) {
        return candidates(nodes,w,h,splashWindow,browsing,enhanced,pkg,NativeSplashSkip.find(nodes,w,h,splashWindow,pkg));
    }
    static List<Integer> candidates(List<Node> nodes,int w,int h,boolean splashWindow,boolean browsing,boolean enhanced,String pkg,int nativeSplash) {
        List<Integer> result=new ArrayList<>();if(w<=0||h<=0||sensitive(nodes)) return result;
        if(nativeSplash>=0){result.add(nativeSplash);return result;}
        if(nativeSplash==NativeSplashSkip.CONFLICT)return result;
        boolean evidence=false,pageScrolling=false,timedAmbiguous=false,explicitAmbiguous=false;int visibleCount=0;
        Node timedTarget=null,explicitTarget=null;
        for(Node n:nodes) {
            if(n.evidence())evidence=true;
            if(n.visible){visibleCount++;if(n.scrollable)pageScrolling=true;}
            if(timedSplashSkip(n,w,h,splashWindow,pkg)) {
                if(timedTarget==null)timedTarget=n;
                else if(Math.abs((n.left+n.right)-(timedTarget.left+timedTarget.right))>w*.024
                    ||Math.abs((n.top+n.bottom)-(timedTarget.top+timedTarget.bottom))>h*.012)timedAmbiguous=true;
            }
            if(explicitSplashSkip(n,w,h,splashWindow)) {
                if(explicitTarget==null)explicitTarget=n;
                else if(Math.abs((n.left+n.right)-(explicitTarget.left+explicitTarget.right))>w*.024
                    ||Math.abs((n.top+n.bottom)-(explicitTarget.top+explicitTarget.bottom))>h*.012)explicitAmbiguous=true;
            }
        }
        for(int i=0;i<nodes.size();i++) {
            Node n=nodes.get(i);if(!n.visible||!n.enabled||n.editable||n.checkable||!small(n,w,h)) continue;
            boolean skip=MatchPolicy.isSkipText(n.text)||MatchPolicy.isSkipText(n.desc);
            boolean sdkSkip=MatchPolicy.isSdkSkip(n.id);
            boolean collection=false;
            for(int ancestor=n.parent,depth=0;ancestor>=0&&ancestor<nodes.size()&&depth<20;depth++) {
                if(nodes.get(ancestor).scrollable){collection=true;break;}
                ancestor=nodes.get(ancestor).parent;
            }
            boolean number=MatchPolicy.isCountdownText(n.text)||MatchPolicy.isCountdownText(n.desc);
            boolean circular=enhanced&&splashWindow&&!collection&&number&&MatchPolicy.isCountdownId(n.id)
                &&(evidence||MatchPolicy.adSpecificId(n.id)||pkg.equals("com.tencent.qqmusic"))
                &&(n.right-n.left)<=w*.20&&(n.bottom-n.top)<=h*.10
                &&(n.right-n.left)/(float)(n.bottom-n.top)>=.5f&&(n.right-n.left)/(float)(n.bottom-n.top)<=2f
                &&MatchPolicy.plausibleBounds(n.left,n.top,n.right,n.bottom,w,h);
            boolean namedAdSkip=enhanced&&splashWindow&&!collection&&skip&&MatchPolicy.adSpecificId(n.id)
                &&n.id!=null&&n.id.toLowerCase(Locale.ROOT).contains("skip")
                &&MatchPolicy.plausibleBounds(n.left,n.top,n.right,n.bottom,w,h);
            // Explicit full-screen advertisements can appear after slow app startup or during browsing.
            boolean confirmedAdSkip=browsing&&evidence&&skip&&!pageScrolling&&visibleCount<=120
                &&MatchPolicy.plausibleBounds(n.left,n.top,n.right,n.bottom,w,h);
            if((splashWindow && (skip||sdkSkip)
                && (MatchPolicy.eligible(n.text,n.id,evidence,false,n.left,n.top,n.right,n.bottom,w,h)
                    || MatchPolicy.eligible(n.desc,n.id,evidence,false,n.left,n.top,n.right,n.bottom,w,h)))
                || (browsing && (sdkClose(n.id)||sdkSkip
                    ||((skip||closeControl(n))&&popupScope(nodes,i,w,h))))
                ||(browsing&&"tv.danmaku.bili".equals(pkg)&&biliSheetClose(nodes,i,w,h))
                ||(splashWindow&&explicitAdClose(n)&&!collection
                    &&MatchPolicy.plausibleBounds(n.left,n.top,n.right,n.bottom,w,h))
                ||(!timedAmbiguous&&n==timedTarget)||circular||namedAdSkip||confirmedAdSkip
                ||(!explicitAmbiguous&&!pageScrolling&&!collection&&n==explicitTarget)) result.add(i);
        }
        return result;
    }
    private AdMatcher() {}
}
