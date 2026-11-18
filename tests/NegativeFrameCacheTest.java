package dev.local.qingtiao;

import java.util.Arrays;

public final class NegativeFrameCacheTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args) {
        NegativeFrameCache c=new NegativeFrameCache();int[] p=new int[16];Arrays.fill(p,0xff918273);
        NegativeFrameCache.Sample first=c.sample("owner/window/display/context",p,4,4,1000,1010);
        check(first!=null&&first.reason==null,"first frame must be analyzed");c.remember(first,"no supported target",true);
        check(c.sample("owner/window/display/context",p,4,4,1450,1460).reason!=null,"unchanged fresh frame can reuse negative result");
        check(c.sample("owner/window/display/context",p,4,4,1900,1910).reason!=null,"second reuse within fixed deadline");
        check(c.sample("owner/window/display/context",p,4,4,2000,2000).reason==null,"reuse never extends original one-second deadline");
        check(c.sample("owner/window/display/context",p,4,4,1000,1010).reason==null,"same screenshot cannot be treated as a new frame");
        check(c.sample("owner/window/display/context",p,4,4,999,1010).reason==null,"old frame cannot reuse later analysis");
        for(int i=0;i<p.length;i++) {c.remember(first,"none",true);p[i]^=1;check(c.sample("owner/window/display/context",p,4,4,1450,1460).reason==null,"any changed pixel forces detection");p[i]^=1;}
        for(String key:new String[]{"otherOwner/window/display/context","owner/otherWindow/display/context","owner/window/otherDisplay/context","owner/window/display/nativeAd","owner/window/display/newGeneration"})
            {c.remember(first,"none",true);check(c.sample(key,p,4,4,1450,1460).reason==null,"changed scene/context cannot inherit cached negative");}
        c.remember(first,"none",true);
        check(c.sample("owner/window/display/context",p,8,2,1450,1460).reason==null,"geometry participates in fingerprint");
        check(c.sample("owner/window/display/context",p,4,4,1450,1460).reason!=null,"valid unchanged frame after geometry probe still uses original bounded state");
        check(c.sample("owner/window/display/context",p,4,4,1450,1470).reason==null,"repeated cached screenshot is not another independent frame");
        c.remember(first,"found",false);check(c.sample("owner/window/display/context",p,4,4,1450,1460).reason==null,"positive result is never cached");
        c.remember(first,"none",true);c.clear();check(c.sample("owner/window/display/context",p,4,4,1450,1460).reason==null,"reset erases negative state");
        for(int which=0;which<6;which++) {
            c.remember(first,"none",true);
            NegativeFrameCache.Sample invalid=c.sample(which==0?null:"owner/window/display/context",which==1?null:p,which==2?0:which==3?3:4,4,which==4?1600:1000,which==5?1601:1500);
            check(invalid==null,"invalid dimensions/data/frame age rejected");
            check(c.sample("owner/window/display/context",p,4,4,1450,1460).reason==null,"invalid sample invalidates previous negative");
        }
        c.remember(null,"none",true);check(c.sample("x",p,4,4,1450,1460).reason==null,"null ticket cannot insert state");
        c.remember(first,null,true);check(c.sample("owner/window/display/context",p,4,4,1450,1460).reason==null,"null result cannot insert state");
        System.out.println("PASS: "+checks+" negative-frame checks");
    }
}
