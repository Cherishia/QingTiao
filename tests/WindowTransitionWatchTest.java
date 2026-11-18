package dev.local.qingtiao;
public final class WindowTransitionWatchTest {
    private static int checks;private static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
    public static void main(String[] args) {
        WindowTransitionWatch w=new WindowTransitionWatch();
        check(w.defer(true,false,"ad.app","launcher",1000),"framework-only new owner pauses pending review");
        check(w.defer(true,false,"ad.app","launcher",1400),"brief metadata gap is tolerated");
        check(w.defer(true,false,"ad.app","another.root",2400),"changing weak hints cannot restart original deadline");
        check(!w.defer(true,false,"ad.app","launcher",2500),"weak ownership cannot stall forever");
        w.reset();check(w.defer(true,true,"ad.app","launcher",3000),"new confirmed owner pauses actions immediately while preserving state for one short re-read");
        check(!w.defer(true,true,"ad.app","launcher",3350),"stable confirmed desktop ends review after 350ms");
        check(!w.defer(false,false,"ad.app","launcher",3001),"ordinary browsing gains no extra polls");
        check(w.defer(true,false,"ad.app","launcher",4000),"a later review has a fresh short deadline");
        check(!w.defer(true,false,"ad.app","ad.app",4100),"current expected owner returns and resets transition gate");
        check(w.defer(true,false,"ad.app","launcher",4200),"new weak transition starts only after resolution");
        check(!w.defer(true,false,"ad.app","launcher",4199),"clock reversal cannot hold a target");
        w.reset();check(!w.defer(true,false,"","launcher",5000),"empty expected owner is not a target");
        System.out.println("PASS: "+checks+" bounded window-transition checks");
    }
}
