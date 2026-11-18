package dev.local.qingtiao;
import java.util.Arrays;
import java.util.Collections;
public final class RootWindowsTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args) {
        RootWindows.Window old=new RootWindows.Window(3220,1,true,false,true),live=new RootWindows.Window(3221,2,true,true,false);
        check(RootWindows.choose(3220,Arrays.asList(old,live))==3221,"cached underlying root cannot override live app window");
        check(RootWindows.choose(3220,Arrays.asList(live,old))==3221,"metadata ordering does not choose the stale app");
        check(RootWindows.choose(3221,Arrays.asList(old,live))==3221,"already-current root is reused");
        check(RootWindows.choose(-1,Arrays.asList(old,live))==3221,"missing framework root uses live application");
        check(RootWindows.choose(-1,Arrays.asList(old))==3220,"focused app is a fallback only without active app");
        check(RootWindows.choose(3220,Collections.emptyList())==3220,"systems without window metadata retain framework behavior");
        check(RootWindows.choose(-1,Collections.emptyList())==-1,"no root or window means no action");
        RootWindows.Window ime=new RootWindows.Window(9,3,false,true,true);
        check(RootWindows.choose(9,Arrays.asList(live,ime))==9,"an active keyboard is never replaced with an underlying app");
        check(RootWindows.choose(3220,Arrays.asList(ime,live))==RootWindows.AMBIGUOUS,"stale app plus active system surface pauses processing");
        check(RootWindows.choose(3220,Arrays.asList(live,new RootWindows.Window(8,2,true,true,true)))==RootWindows.AMBIGUOUS,"equal-layer active owners are ambiguous");
        check(RootWindows.choose(3220,Arrays.asList(live,new RootWindows.Window(8,4,true,true,true)))==8,"highest active app layer wins");
        check(RootWindows.choose(3220,Arrays.asList(live,new RootWindows.Window(8,2,true,true,true),new RootWindows.Window(7,4,true,true,true)))==7,"lower-layer ambiguity cannot hide an unambiguous top window");
        check(RootWindows.choose(3221,Arrays.asList(live,new RootWindows.Window(7,8,true,false,true)))==3221,"a high focused but inactive window cannot replace active owner");
        check(RootWindows.confirmed(3221,Arrays.asList(old,live)),"active metadata confirms returned owner");
        check(!RootWindows.confirmed(3220,Arrays.asList(old,live)),"focused inactive root is not authoritative");
        check(!RootWindows.confirmed(3220,Collections.emptyList()),"framework-only fallback is unconfirmed");
        check(!RootWindows.confirmed(RootWindows.AMBIGUOUS,Arrays.asList(old,live)),"ambiguous metadata never confirms");
        System.out.println("PASS: "+checks+" foreground window checks");
    }
}
