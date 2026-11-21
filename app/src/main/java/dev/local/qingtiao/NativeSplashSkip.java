package dev.local.qingtiao;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Native startup controls require a known role and current ad evidence in the same viewport. */
public final class NativeSplashSkip {
    static final int NO_MATCH=-1, CONFLICT=-2;
    private static final Pattern DIGIT=Pattern.compile("[0-9]");
    private static final Pattern AD_ROOT=Pattern.compile("(?:splash_ad|ad_splash|startup_ad|opening_ad)(?:_(?:root|layout|container|view))?");
    private static final Pattern AD_IMAGE=Pattern.compile("(?:iv_|img_|image_)?(?:advertisement(?:_image|_img)?|(?:splash_ad|ad_splash|startup_ad)_(?:image|img))");
    private static final Pattern SKIP_ROLE=Pattern.compile("(?:splash_|startup_)?(?:(?:ad_)?skip(?:_(?:ad|button|btn|text|view|tv))?|(?:btn_|tv_|iv_)skip(?:_ad)?)");
    private static final Pattern TIMER_ROLE=Pattern.compile("(?:(?:tv|txt|text|btn)_)?(?:advertisement|splash_ad|ad_splash|startup_ad|opening_ad)_timer");
    private static final String WM_CONTROL="cj.mobile.wm.allmodules:id/wm_splash_jump_tv";

    static final class Target {
        final int index,timerImage;
        Target(int index,int timerImage){this.index=index;this.timerImage=timerImage;}
    }
    static final Target NONE=new Target(NO_MATCH,NO_MATCH);
    private static final Target AMBIGUOUS=new Target(CONFLICT,NO_MATCH);

    private static boolean available(AdMatcher.Node n){return n.visible&&n.enabled&&!n.editable&&!n.checkable&&!n.scrollable;}
    private static boolean large(AdMatcher.Node n,int w,int h,boolean whole){
        return available(n)&&n.left>=0&&n.top>=0&&n.right<=w&&n.bottom<=h&&n.left<=w*.10&&n.top<=h*.10
            &&n.right-n.left>=w*(whole?.90:.80)&&n.bottom-n.top>=h*(whole?.90:.70)
            &&(long)(n.right-n.left)*(n.bottom-n.top)>=w*(long)h*(whole?.85:.65);
    }
    private static boolean below(List<AdMatcher.Node> nodes,int child,int ancestor,boolean safe){
        for(int steps=0;child>=0&&child<nodes.size()&&steps<30;steps++){
            AdMatcher.Node n=nodes.get(child);
            if(safe&&!available(n))return false;
            if(child==ancestor)return true;
            child=n.parent;
        }
        return false;
    }
    private static boolean inside(AdMatcher.Node n,AdMatcher.Node box){return n.left>=box.left&&n.top>=box.top&&n.right<=box.right&&n.bottom<=box.bottom;}
    private static String role(AdMatcher.Node n,String prefix){
        return n.id!=null&&n.id.startsWith(prefix)&&n.id.length()<=prefix.length()+80?n.id.substring(prefix.length()):"";
    }
    private static boolean countRole(String role){return role.equals("count_down")||role.equals("countdown");}
    private static boolean skipRole(String role){
        return !role.isEmpty()&&role.indexOf('/')<0&&(countRole(role)||SKIP_ROLE.matcher(role).matches()||MatchPolicy.isSdkSkip(role));
    }
    /** 0, plain skip, timed skip, or explicit skip-ad. No CTA, merely disabling sound, or numeral alone. */
    private static int labelValue(String value){
        if(value==null||value.isEmpty()||value.length()>64||!MatchPolicy.isSkipText(value))return 0;
        if(MatchPolicy.explicitAdSkip(value))return 3;
        return DIGIT.matcher(value).find()?2:1;
    }
    private static int label(AdMatcher.Node n){
        int text=labelValue(n.text),desc=labelValue(n.desc);
        String t=MatchPolicy.normalize(n.text),d=MatchPolicy.normalize(n.desc);
        if(text!=0&&(d.isEmpty()||d.equals("按钮")||d.equalsIgnoreCase("button")||desc!=0))return Math.max(text,desc);
        return t.isEmpty()?desc:0;
    }
    private static boolean bounds(AdMatcher.Node n,int w,int h){
        int bw=n.right-n.left,bh=n.bottom-n.top;
        return n.left>=0&&n.top>=0&&n.right<=w&&n.bottom<=h
            &&bw>=w*.035&&bw<=w*.30&&bh>=h*.008&&bh<=h*.065&&bw>=bh*1.10&&bw<=bh*6
            &&(n.left>=w*.69&&n.right>=w*.86||n.left<=w*.10&&n.right<=w*.32)
            &&(n.bottom<=h*.24||n.top>=h*.83&&n.bottom<=h*.993);
    }
    private static boolean timerRole(String role){return role.endsWith("_timer")&&TIMER_ROLE.matcher(role).matches();}
    private static int controlLabel(AdMatcher.Node n,int w,int h,String role,boolean timer){
        if(timer){
            if(n.left<w*.74||n.right<w*.90||n.bottom>h*.18||labelValue(n.text)!=2)return 0;
        } else if(!n.clickable||!skipRole(role)){
            return 0;
        }
        int label=label(n);
        return (timer||countRole(role))&&label<2?0:label;
    }
    /** Drift rejection for an already proven layout; this method alone never authorizes a target. */
    static boolean currentControl(AdMatcher.Node n,int w,int h,String pkg){
        if(n==null||pkg==null||w<=0||h<w*1.4||!available(n)||!bounds(n,w,h))return false;
        String role=role(n,pkg+":id/");
        return controlLabel(n,w,h,role,timerRole(role))>0;
    }
    /** A refresh may change the count, but cannot lose wording used as independent proof. */
    static boolean currentControl(AdMatcher.Node expected,AdMatcher.Node current,int w,int h,String pkg){
        return expected!=null&&currentControl(current,w,h,pkg)&&java.util.Objects.equals(expected.id,current.id)
            &&expected.clickable==current.clickable&&label(current)>=label(expected);
    }
    private static boolean markerValue(String value){
        if(value==null||value.isEmpty()||value.length()>64)return false;
        String t=MatchPolicy.normalize(value);
        return t.equals("广告")||t.equals("廣告")||t.equalsIgnoreCase("ad")||t.equalsIgnoreCase("advertisement")
            ||t.equals("互动广告")||t.equals("互動廣告")||t.equals("广告由第三方提供")||t.equals("廣告由第三方提供");
    }
    private static boolean marker(AdMatcher.Node n,String role,int w,int h){
        return available(n)&&n.left>=0&&n.top>=0&&n.right<=w&&n.bottom<=h&&n.right>n.left&&n.bottom>n.top
            &&n.right-n.left<=w*.28&&n.bottom-n.top<=h*.08
            &&(markerValue(n.text)||markerValue(n.desc)||role.equals("tt_splash_ad_logo")||role.equals("ksad_splash_logo")
                ||role.equals("gdt_ad_logo")||role.equals("mbridge_splash_adchoice")||role.equals("ad_source_logo"));
    }
    private static boolean image(AdMatcher.Node n,String role,int w,int h) {
        return n.className!=null&&n.className.endsWith("ImageView")&&large(n,w,h,true)&&AD_IMAGE.matcher(role).matches();
    }
    static boolean currentTimerImage(AdMatcher.Node expected,AdMatcher.Node current,AdMatcher.Node timer,int w,int h,String pkg){
        return java.util.Objects.equals(expected.id,current.id)&&image(current,role(current,pkg+":id/"),w,h)&&inside(timer,current);
    }
    private static boolean blocksPage(List<AdMatcher.Node> nodes){
        if(AdMatcher.sensitive(nodes))return true;
        for(AdMatcher.Node n:nodes)if(n.visible){
            if(n.editable||WM_CONTROL.equals(n.id)||"查看原图".equals(n.text)||"保存图片".equals(n.text)
                ||"查看原图".equals(n.desc)||"保存图片".equals(n.desc))return true;
            String kind=((n.id==null?"":n.id)+" "+(n.className==null?"":n.className)).toLowerCase(Locale.ROOT);
            if(kind.contains("gallery")||kind.contains("imagepreview")||kind.contains("image_preview")||kind.contains("imageviewer")||kind.contains("image_viewer"))return true;
        }
        return false;
    }
    private static boolean legacyControl(AdMatcher.Node n,String role,int w,int h){
        int bw=n.right-n.left,bh=n.bottom-n.top;
        return role.equals("count_down")&&n.left>=w*.70&&n.right>=w*.90&&n.top>=h*.84&&n.bottom<=h*.985
            &&bw>=w*.06&&bw>=bh*1.2;
    }
    private static int proof(List<AdMatcher.Node> nodes,String[] roles,int target,int label,int w,int h,boolean timer){
        AdMatcher.Node n=nodes.get(target);
        boolean previewImageScope=true;
        // Walk the contiguous visible ancestor chain. A virtual viewport isn't a real layout.
        for(int p=n.parent,d=0;p>=0&&p<nodes.size()&&d<30;d++){
            AdMatcher.Node viewport=nodes.get(p);
            if(!available(viewport))return NO_MATCH;
            if(large(viewport,w,h,true)&&inside(n,viewport)){
                String role=roles[p];
                boolean legacy=!timer&&role.equals("splash_layout")&&legacyControl(n,roles[target],w,h);
                boolean ownAdRoot=!timer&&!role.isEmpty()&&AD_ROOT.matcher(role).matches();
                boolean real=viewport.parent>=0||viewport.className!=null&&!viewport.className.isEmpty();
                if(ownAdRoot&&label==3)return p;
                boolean allowImage=previewImageScope&&label>=2;
                if(real&&(legacy||ownAdRoot||allowImage)){
                    boolean buttons=false,interact=false;
                    for(int i=0;i<nodes.size();i++){
                        if(i==target||i==p)continue;
                        AdMatcher.Node z=nodes.get(i);String r=roles[i];
                        boolean b=legacy&&r.equals("splash_button_container")&&large(z,w,h,true);
                        boolean a=legacy&&r.equals("splash_interact_view")&&large(z,w,h,false);
                        boolean evidence=ownAdRoot&&marker(z,r,w,h)||allowImage&&image(z,r,w,h)
                            &&(!timer||inside(n,z)&&!below(nodes,target,i,false));
                        if(!(b||a||evidence)||!inside(z,viewport)||!below(nodes,i,p,true))continue;
                        // Do not count labels drawn inside the button itself as independent ad evidence.
                        if(evidence&&!below(nodes,i,target,false))return i;
                        buttons|=b;interact|=a;
                        if(buttons&&interact)return p;
                    }
                }
                // A plain generic ancestor cannot join a control in one full-screen overlay
                // to an ad image in a separate overlay behind it. Exact ad roots and the
                // existing named layout still have their respective, independent proofs.
                if(real){
                    if(timer)return NO_MATCH;
                    previewImageScope=false;
                }
            }
            if(viewport.parent==p)break;
            p=viewport.parent;
        }
        return NO_MATCH;
    }
    private static boolean safeAncestry(List<AdMatcher.Node> nodes,int index){
        int p=nodes.get(index).parent;
        for(int d=0;p>=0&&p<nodes.size()&&d<30;d++){
            AdMatcher.Node n=nodes.get(p);
            if(!available(n))return false;
            p=n.parent;
        }
        return p<0;
    }
    public static int find(List<AdMatcher.Node> nodes,int w,int h,boolean startup,String pkg){
        return inspect(nodes,w,h,startup,pkg).index;
    }
    static Target inspect(List<AdMatcher.Node> nodes,int w,int h,boolean startup,String pkg){
        if(!startup||pkg==null||pkg.isEmpty()||MatchPolicy.diagnosticHost(pkg)||MatchPolicy.requiresVisualSkip(pkg)
            ||nodes==null||nodes.isEmpty()||nodes.size()>=1000||w<=0||h<w*1.4)return NONE;
        String prefix=pkg+":id/";String[] roles=null;int chosen=NO_MATCH,timerImage=NO_MATCH,candidates=0;
        for(int i=0;i<nodes.size();i++){
            AdMatcher.Node n=nodes.get(i);if(!available(n)||!bounds(n,w,h))continue;
            String r=role(n,prefix);boolean timer=timerRole(r);
            int label=controlLabel(n,w,h,r,timer);
            if(label==0)continue;
            if(roles==null){
                roles=new String[nodes.size()];
                for(int j=0;j<nodes.size();j++)roles[j]=role(nodes.get(j),prefix);
            }
            if(++candidates>8)return AMBIGUOUS; // Avoid searching pathological trees of competing controls.
            if(!safeAncestry(nodes,i))continue;
            int witness=proof(nodes,roles,i,label,w,h,timer);
            if(witness<0)continue;
            if(chosen>=0)return AMBIGUOUS;
            chosen=i;timerImage=timer?witness:NO_MATCH;
        }
        if(chosen<0)return NONE;
        return blocksPage(nodes)?AMBIGUOUS:new Target(chosen,timerImage);
    }
    private NativeSplashSkip(){}
}
