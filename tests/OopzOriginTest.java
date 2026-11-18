package dev.local.qingtiao;
import java.util.ArrayList;
public final class OopzOriginTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args) {
        String oopz="com.weilaishanhai.oopz",carrier="com.tencent.mm";OopzOrigin o=new OopzOrigin();
        o.foreground(carrier,1000);check(!o.delegated(carrier,1100),"ordinary WeChat is not an OOPZ ad context");
        o.foreground(oopz,2000);o.foreground(carrier,2500,1);check(o.delegated(carrier,2600),"recent OOPZ startup handoff is bounded and explicit");
        check(!o.delegated(oopz,2600)&&!o.delegated("other.app",2600),"carrier permission is exact");
        o.foreground(carrier,32000);check(!o.delegated(carrier,32500),"repeated reads cannot renew 30-second lifetime");
        o.foreground("other.app",33000);o.foreground(carrier,34000);check(!o.delegated(carrier,34001),"an intervening owner clears handoff");
        o.reset();o.windowEvent(oopz,1000);o.foreground(carrier,2000);check(o.delegated(carrier,2100),"uncoalesced OOPZ window event can establish source when old root was cached");
        o.reset();o.windowEvent(oopz,1000);o.foreground(carrier,4000);check(!o.delegated(carrier,4100),"old background event cannot arm another app");
        o.reset();o.foreground(oopz,1000);o.foreground(carrier,2000,2);check(!o.delegated(carrier,2100),"two spent OOPZ touches cannot gain another action by handoff");
        o.reset();o.foreground(oopz,1000);o.foreground(carrier,40000);check(!o.delegated(carrier,41000),"normal switch long after startup does not arm a carrier");
        o.reset();o.windowEvent(oopz,2000);o.foreground(carrier,1000);check(!o.delegated(carrier,1100),"future origin event is rejected");
        ArrayList<AdMatcher.Node> nodes=new ArrayList<>();AdMatcher.Node n=new AdMatcher.Node(-1,0,0,1200,2670,"","");nodes.add(n);
        check(OopzOrigin.context(oopz,true,nodes,false),"actual OOPZ root supports drawn ad observation");
        check(!OopzOrigin.context(carrier,true,nodes,false),"carrier pixels alone do not establish permission");
        check(OopzOrigin.context(carrier,true,nodes,true),"explicit carrier can request strongly verified ad pixels");
        check(!OopzOrigin.context(carrier,false,nodes,true),"handoff does not bypass startup window");
        n.className="com.tencent.mm.ui.chatting.gallery.ImageGalleryUI";check(!OopzOrigin.context(carrier,true,nodes,true),"native photo preview is never treated as a live ad");
        n.className="android.view.View";n.editable=true;check(!OopzOrigin.context(carrier,true,nodes,true),"carrier conversation input is excluded");
        n.editable=false;n.password=true;check(!OopzOrigin.context(oopz,true,nodes,false),"sensitive context excluded");
        System.out.println("PASS: "+checks+" OOPZ source/context checks");
    }
}
