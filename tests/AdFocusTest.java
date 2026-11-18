package dev.local.qingtiao;

public final class AdFocusTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        AdFocus f=new AdFocus();check(!f.rapid(0),"ordinary page has no discovery burst");
        f.observe("a.app",1,"panel",1000);check(f.rapid(1000),"fresh explicit cue starts short follow-up");
        for(long t=1100;t<60000;t+=100){f.observe("a.app",1,"panel",t);check(f.rapid(t)==(t<3500),"continuous content cannot extend capture burst");}
        f.observe("a.app",1,"",60000);check(!f.rapid(60000),"no current ad evidence stops fast work immediately");
        f.observe("a.app",1,"panel",60100);check(f.rapid(60100),"new evidence after long observation can start another bounded burst");
        f.observe("a.app",2,"panel",60200);f.observe("a.app",2,"panel",62599);
        check(f.rapid(62599)&&!f.rapid(62600),"window churn cannot extend the existing lease");
        f.observe("a.app",2,"",62700);f.observe("a.app",2,"panel",62800);check(!f.rapid(62800),"flicker cannot rearm before eight seconds");
        f.observe("a.app",3,"panel",68200);check(f.rapid(68200),"new window can rearm after cooldown");
        f.observe("b.app",4,"",68300);check(!f.rapid(68300),"owner transition removes old evidence");
        f.observe("b.app",4,"startup-ad",68400);check(f.rapid(68400),"other apps use same bounded evidence policy");
        check(!f.rapid(69600),"unrefreshed evidence expires at twelve hundred milliseconds");
        f.observe(null,4,"panel",69700);check(!f.rapid(69700),"unknown owner resets");
        f.observe("a.app",-1,"panel",69800);check(!f.rapid(69800),"unknown window resets");
        f.observe("a.app",1,"panel",69900);f.reset();check(!f.rapid(69901),"pause/lock reset removes fast work");
        System.out.println("PASS: "+checks+" bounded ad-focus checks");
    }
}
