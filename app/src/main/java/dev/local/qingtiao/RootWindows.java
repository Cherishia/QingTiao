package dev.local.qingtiao;
import java.util.List;

/** Cross-check a possibly cached framework root against current interactive window metadata. */
public final class RootWindows {
    public static final int AMBIGUOUS=-2;
    public static final class Window {
        public final int id,layer;public final boolean application,active,focused;
        public Window(int id,int layer,boolean application,boolean active,boolean focused){this.id=id;this.layer=layer;this.application=application;this.active=active;this.focused=focused;}
    }
    public static int choose(int framework,List<Window> windows) {
        Window active=null,focused=null;boolean tie=false;
        for(Window w:windows) {
            if(w.active&&!w.application)return w.id==framework?framework:AMBIGUOUS;
            if(!w.application)continue;
            if(w.active) {
                if(active!=null&&active.layer==w.layer&&active.id!=w.id)tie=true;
                if(active==null||w.layer>active.layer){active=w;tie=false;}
            }
            if(w.focused&&(focused==null||w.layer>focused.layer))focused=w;
        }
        if(active!=null)return tie?AMBIGUOUS:active.id;
        if(framework>=0)return framework;
        return focused==null?-1:focused.id;
    }
    public static boolean confirmed(int chosen,List<Window> windows) {
        if(chosen<0)return false;
        for(Window w:windows)if(w.id==chosen&&w.active)return true;
        return false;
    }
    private RootWindows(){}
}
