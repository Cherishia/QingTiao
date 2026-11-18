package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Explicit neutral dismissal of a bounded mode introduction; never a mode-state action. */
public final class ModeReminder {
    public static final class Result {
        public final boolean present;
        public final int index;
        public final String reason;
        private Result(boolean present,int index,String reason){this.present=present;this.index=index;this.reason=reason;}
    }
    private static final Result NONE=new Result(false,-1,"未读到完整模式提醒");
    public static Result none(){return NONE;}
    private static boolean topic(String s){return s!=null&&(s.contains("青少年")||s.contains("未成年"));}
    private static String text(String s){return MatchPolicy.normalize(s).replace('護','护').replace('長','长').replace('開','开').replace('啟','启').replace('閉','闭').replace('設','设').replace('暫','暂').replace('後','后').replace('說','说').replace('時','时').replace('網','网').replace('體','体');}
    private static boolean title(String s){
        if(!topic(s))return false;String t=text(s);
        return t.equals("青少年模式")||t.equals("未成年人模式")||t.equals("青少年保护模式")||t.equals("青少年守护模式")||t.equals("未成年人保护模式")
            ||t.equals("青少年模式提醒")||t.equals("青少年模式提示")||t.equals("未成年人模式提醒")||t.equals("未成年人模式提示");
    }
    private static boolean neutral(String s){String t=text(s);return t.equals("我知道了")||t.equals("知道了")||t.equals("暂不开启")||t.equals("暂不启用")||t.equals("暂不设置")||t.equals("以后再说")||t.equals("稍后再说");}
    private static boolean compatible(String s){String t=text(s);return t.isEmpty()||neutral(t)||t.equals("按钮")||t.equalsIgnoreCase("button");}
    private static boolean body(String raw){
        if(!topic(raw)||raw.length()>2000)return false;String s=text(raw);
        return (s.contains("健康成长")||s.contains("身心健康")||s.contains("健康上网")||s.contains("保护"))
            &&(s.contains("模式")||s.contains("开启")||s.contains("启用")||s.contains("提供")||s.contains("设置"));
    }
    private static boolean protectedState(String raw){
        if(raw==null||raw.isEmpty())return false;String s=text(raw);
        if(s.contains("密码")||s.contains("密碼")||s.contains("验证")||s.contains("驗證")||s.contains("监护人同意")||s.contains("監护人同意"))return true;
        return topic(s)&&(s.contains("已开启")||s.contains("已启用")||s.contains("当前处于")||s.contains("正在使用")||s.contains("关闭青少年")||s.contains("退出青少年")||s.contains("关闭未成年")||s.contains("退出未成年"));
    }
    private static boolean inside(AdMatcher.Node n,AdMatcher.Node box){return n.right>n.left&&n.bottom>n.top&&n.left>=box.left&&n.top>=box.top&&n.right<=box.right&&n.bottom<=box.bottom;}
    private static boolean descendant(List<AdMatcher.Node> nodes,int child,int ancestor){for(int d=0;child>=0&&child<nodes.size()&&d<20;d++){if(child==ancestor)return true;child=nodes.get(child).parent;}return false;}
    private static boolean validScope(List<AdMatcher.Node> nodes,int at){
        HashSet<Integer> seen=new HashSet<>();
        for(int d=0;at>=0&&at<nodes.size()&&d<20;d++){
            if(!seen.add(at))return false;AdMatcher.Node n=nodes.get(at);
            if(n.scrollable||n.editable||n.checkable)return false;at=n.parent;
        }
        return at<0;
    }
    private static boolean panel(AdMatcher.Node n,int w,int h){
        int bw=n.right-n.left,bh=n.bottom-n.top;
        return n.visible&&n.enabled&&!n.editable&&!n.checkable&&!n.scrollable&&n.left>=0&&n.top>=h*.06&&n.right<=w&&n.bottom<=h
            &&bw>=w*.5&&bw<=w&&bh>=h*.18&&bh<=h*.78&&(long)bw*bh<=(long)w*h*.82;
    }
    private static boolean neutralContainer(List<AdMatcher.Node> nodes,int parent){
        for(int i=1;i<nodes.size();i++)if(descendant(nodes,i,parent)){
            AdMatcher.Node n=nodes.get(i);if(n.visible&&(!compatible(n.text)||!compatible(n.desc)||n.editable||n.checkable))return false;
        }
        return true;
    }
    private static boolean button(List<AdMatcher.Node> nodes,int index,int scope,int w,int h){
        AdMatcher.Node n=nodes.get(index),box=nodes.get(scope);
        if(!n.visible||!n.enabled||n.editable||n.checkable||n.scrollable||!inside(n,box)||n.top<box.top+(box.bottom-box.top)*.55
            ||n.right-n.left>w*.82||n.bottom-n.top>h*.10||n.right-n.left<w*.025||n.bottom-n.top<h*.006
            ||(!neutral(n.text)&&!neutral(n.desc))||!compatible(n.text)||!compatible(n.desc))return false;
        boolean interactive=false;
        for(int at=index,depth=0;at!=scope&&at>0&&at<nodes.size()&&depth<5;depth++) {
            AdMatcher.Node p=nodes.get(at);
            if(!p.visible||!p.enabled||p.editable||p.checkable||p.scrollable||!inside(n,p))return false;
            if(p.clickable&&p.right-p.left<=w*.85&&p.bottom-p.top<=h*.12&&neutralContainer(nodes,at))interactive=true;
            at=p.parent;if(at==scope)return interactive;
        }
        return false;
    }
    public static Result find(List<AdMatcher.Node> nodes,int w,int h){
        if(nodes==null||nodes.isEmpty()||nodes.size()>=1000||w<=0||h<=0)return NONE;
        ArrayList<Integer> titles=null;
        for(int i=1;i<nodes.size();i++){AdMatcher.Node n=nodes.get(i);if(n.visible&&n.enabled&&!n.checkable&&!n.editable&&(title(n.text)||title(n.desc))){
            if(titles==null)titles=new ArrayList<>();titles.add(i);if(titles.size()>8)return NONE;}}
        if(titles==null)return NONE;
        if(AdMatcher.sensitive(nodes))return new Result(true,-1,"敏感或验证页面，未操作");
        HashSet<Integer> scopes=new HashSet<>();int selected=-1;boolean present=false;
        for(int ti:titles){AdMatcher.Node heading=nodes.get(ti);
            for(int scope=heading.parent,depth=0;scope>=0&&scope<nodes.size()&&depth<12;depth++){
                AdMatcher.Node box=nodes.get(scope);int parent=box.parent;
                if(panel(box,w,h)&&inside(heading,box)&&heading.bottom<=box.top+(box.bottom-box.top)*.45&&scopes.add(scope)&&validScope(nodes,scope)) {
                    boolean introduction=false,blocked=false;
                    for(int i=1;i<nodes.size();i++)if(i!=scope&&descendant(nodes,i,scope)){
                        AdMatcher.Node n=nodes.get(i);if(!n.visible||!inside(n,box))continue;
                        if(n.editable||n.password||protectedState(n.text)||protectedState(n.desc))blocked=true;
                        if(i!=ti&&!title(n.text)&&!title(n.desc)&&(body(n.text)||body(n.desc)))introduction=true;
                    }
                    if(introduction){
                        present=true;if(blocked)return new Result(true,-1,"已开启、退出或验证相关页面，未操作");
                        for(int i=1;i<nodes.size();i++)if(descendant(nodes,i,scope)&&button(nodes,i,scope,w,h)){
                            if(selected<0)selected=i;
                            else if(selected!=i){
                                if(descendant(nodes,i,selected)&&inside(nodes.get(i),nodes.get(selected)))selected=i;
                                else if(!descendant(nodes,selected,i))return new Result(true,-1,"多个不同关闭目标，未操作");
                            }
                        }
                        break;
                    }
                }
                scope=parent;
            }
        }
        return present?new Result(true,selected,selected<0?"提醒已确认，但无唯一明确的中性关闭按钮":"同一弹窗的模式标题、保护提醒及中性关闭按钮匹配"):NONE;
    }
    private ModeReminder(){}
}
