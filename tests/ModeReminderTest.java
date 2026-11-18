package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.List;

public final class ModeReminderTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    private static AdMatcher.Node n(int parent,int l,int t,int r,int b,String text){return new AdMatcher.Node(parent,l,t,r,b,text,"");}
    private static ArrayList<AdMatcher.Node> popup(){
        ArrayList<AdMatcher.Node> a=new ArrayList<>();
        a.add(n(-1,0,0,1080,2400,""));a.add(n(0,0,0,1080,2400,""));a.add(n(1,80,500,1000,1800,""));
        a.add(n(2,180,580,900,650,"青少年模式"));a.add(n(2,150,720,930,1100,"为保护未成年人健康成长，平台提供青少年模式，可在设置中开启。"));
        a.add(n(2,180,1500,900,1650,"我知道了"));a.get(5).clickable=true;
        a.add(n(2,180,1260,900,1400,"开启青少年模式"));a.get(6).clickable=true;
        return a;
    }
    private static ModeReminder.Result find(List<AdMatcher.Node> a){return ModeReminder.find(a,1080,2400);}
    public static void main(String[] args){
        ArrayList<AdMatcher.Node> a=popup();check(find(a).present&&find(a).index==5,"explicit reminder closes neutral button without an ad marker");
        String[] labels={"我知道了","知道了","暂不开启","暂不启用","暂不设置","以后再说","稍后再说","暫不開啟","以後再說"};
        String[] titles={"青少年模式","未成年人模式","青少年保护模式","青少年守护模式","未成年人保护模式","青少年模式提醒","未成年人模式提示","青少年保護模式"};
        for(String title:titles)for(String label:labels){a=popup();a.get(3).text=title;a.get(5).text=label;check(find(a).index==5,"known explicit introduction and neutral labels: "+title+"/"+label);}
        for(int[] size:new int[][]{{575,1280},{720,1602},{1200,2670},{1440,3136}}){a=popup();for(AdMatcher.Node n:a){n.left=Math.round(n.left*size[0]/1080f);n.right=Math.round(n.right*size[0]/1080f);n.top=Math.round(n.top*size[1]/2400f);n.bottom=Math.round(n.bottom*size[1]/2400f);}check(ModeReminder.find(a,size[0],size[1]).index==5,"live display proportions");}
        for(String label:new String[]{"开启青少年模式","开启未成年人模式","关闭青少年模式","退出青少年模式","关闭未成年人模式","关闭模式","我已成年","我是家长","已满18岁","同意并继续","确定","取消","暂不","关闭","×","以后不再提醒"}){
            a=popup();a.get(5).text=label;check(find(a).index<0,"never reinterpret an enable/disable/age/consent/general action as neutral dismissal: "+label);}
        for(String state:new String[]{"青少年模式已开启","已启用未成年人模式","当前处于青少年模式","正在使用青少年模式","关闭青少年模式","退出未成年人模式","请输入密码","身份验证","监护人同意","请输入密碼"}){
            a=popup();a.add(n(2,200,1120,900,1200,state));check(find(a).index<0,"state changes and protected verification are not reminder dismissals: "+state);}
        for(int which=0;which<10;which++){
            a=popup();AdMatcher.Node target=a.get(5);
            if(which==0)target.visible=false;if(which==1)target.enabled=false;if(which==2)target.editable=true;if(which==3)target.checkable=true;if(which==4)target.scrollable=true;
            if(which==5){target.left=0;}if(which==6)target.top=800;if(which==7)target.bottom=2000;if(which==8){target.right=target.left+1;}if(which==9)target.clickable=false;
            check(find(a).index<0,"invalid/noninteractive/out-of-footer target rejected "+which);
        }
        for(int which=0;which<9;which++){
            a=popup();AdMatcher.Node panel=a.get(2);
            if(which==0)panel.visible=false;if(which==1)panel.enabled=false;if(which==2)panel.scrollable=true;if(which==3)panel.checkable=true;if(which==4)panel.editable=true;
            if(which==5){panel.left=0;panel.top=0;panel.right=1080;panel.bottom=2400;}if(which==6){panel.left=800;}if(which==7)panel.top=-1;if(which==8)panel.bottom=2450;
            check(find(a).index<0,"whole page or invalid scope not a modal reminder "+which);
        }
        a=popup();a.get(3).visible=false;check(!find(a).present,"hidden heading is not evidence");
        a=popup();a.get(4).visible=false;check(!find(a).present,"hidden introduction is not evidence");
        a=popup();a.get(3).text="开启青少年模式";check(!find(a).present,"settings action label cannot be a reminder heading");
        a=popup();a.get(4).text="这是一个普通通知，请选择。";check(!find(a).present,"heading alone cannot authorize a close");
        a=popup();a.get(4).text="青少年作品推荐，看看热门内容。";check(!find(a).present,"unrelated youth content is not protection introduction");
        a=popup();a.get(4).parent=1;check(!find(a).present,"body in a different branch cannot authorize footer action");
        a=popup();a.get(5).parent=1;check(find(a).present&&find(a).index<0,"neutral button outside current reminder cannot be used");
        a=popup();a.get(5).desc="开启青少年模式";check(find(a).index<0,"conflicting accessibility description rejected");
        a=popup();a.get(5).desc="按钮";check(find(a).index==5,"generic role label is compatible with neutral text");
        a=popup();a.get(5).desc=a.get(5).text;a.get(5).text="";check(find(a).index==5,"explicit neutral accessibility description supported");
        a=popup();a.get(5).password=true;check(find(a).index<0,"password flag rejects action");
        a=popup();a.get(6).text="确认支付";check(find(a).index<0,"existing global sensitive-page check retained");
        a=popup();a.get(5).clickable=false;a.add(n(2,170,1480,910,1670,""));a.get(7).clickable=true;a.get(5).parent=7;
        check(find(a).index==5,"nonclickable text can identify interior of an independently interactive neutral button");
        a.get(7).text="开启青少年模式";check(find(a).index<0,"interactive parent with a different action cannot authorize text touch");
        a=popup();a.get(5).clickable=false;a.add(n(2,170,1480,910,1670,""));a.get(7).clickable=true;a.get(5).parent=7;a.add(n(7,180,1500,350,1550,"开启青少年模式"));
        check(find(a).index<0,"mixed enable and neutral child content cannot form one safe interactive button");
        a=popup();a.get(5).text="";a.get(5).clickable=true;a.add(n(5,300,1520,700,1600,"知道了"));
        check(find(a).index==7,"label interior selected instead of invoking a parent");
        a=popup();a.add(n(5,300,1520,700,1600,"我知道了"));check(find(a).index==7,"duplicate nested label collapses to one interior target");
        a=popup();a.add(n(2,200,1680,880,1770,"以后再说"));a.get(7).clickable=true;check(find(a).present&&find(a).index<0,"two distinct neutral targets are ambiguous");
        a=popup();a.get(3).desc="青少年模式";check(find(a).index==5,"duplicate title semantics are not separate dialogs");
        a=popup();a.get(4).text="為保護未成年人健康成長，平台提供青少年模式。";check(find(a).index==5,"traditional Chinese introduction supported");
        // Actual dialog roots have no virtual screen parent in final pixel-context checks.
        a=popup();a.remove(1);a.remove(0);for(AdMatcher.Node n:a)n.parent=n.parent<=1?-1:n.parent-2;
        check(ModeReminder.find(a,1080,2400).index==3,"dialog-root context also blocks ad touches behind the reminder");
        check(ModeReminder.find(null,1080,2400).index<0&&ModeReminder.find(new ArrayList<AdMatcher.Node>(),1080,2400).index<0,"empty input rejected");
        check(ModeReminder.find(popup(),0,2400).index<0&&ModeReminder.find(popup(),1080,0).index<0,"invalid display rejected");
        a=popup();for(int i=0;i<1000;i++)a.add(n(1,0,0,10,10,""));check(find(a).index<0,"bounded tree work");
        a=popup();for(int i=0;i<9;i++)a.add(n(1,10,100,200,200,"青少年模式"));check(!find(a).present,"long ordinary title lists do not become reminder contexts");
        a=popup();a.get(2).parent=2;check(find(a).index<0,"cyclic ancestor cannot authorize a button through an invalid hierarchy");
        a=popup();a.get(1).scrollable=true;check(!find(a).present,"a scrollable list/chat card cannot be reinterpreted as a modal reminder");
        a=popup();a.get(1).checkable=true;check(!find(a).present,"settings state control cannot be an enclosing reminder scope");
        ArrayList<AdMatcher.Node> ordinary=new ArrayList<>();ordinary.add(n(-1,0,0,1080,2400,""));for(int i=0;i<300;i++)ordinary.add(n(0,0,0,100,100,"普通页面内容"));
        check(!find(ordinary).present,"ordinary page exits before modal/body/button search");
        ClickGate gate=new ClickGate();gate.record("reminder",1000);check(!gate.allows("reminder",10000),"persistent reminder never gets unlimited repeated touches");
        System.out.println("PASS: "+checks+" mode-reminder checks");
    }
}
