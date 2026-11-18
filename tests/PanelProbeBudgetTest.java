package dev.local.qingtiao;

public final class PanelProbeBudgetTest {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        PanelProbeBudget p=new PanelProbeBudget();
        p.observe(4,false,false,1000);check(p.delay(4,1000)<0,"ordinary paused video has no capture allowance");
        p.observe(4,true,true,1100);check(p.delay(4,1100)==0,"fresh scoped panel gets an immediate opportunity");
        for(int i=0;i<3;i++){long t=1100+500L*i;check(p.delay(4,t)==0,"scoped capture available");p.requested(4,t);check(p.delay(4,t)==(i<2?500:-1),"delay or exhausted allowance");}
        for(long t=2200;t<10000;t+=100)p.observe(4,true,true,t);
        check(p.delay(4,10000)<0,"unchanged panel cannot roll over the lease");
        check(p.treeDelay(4,10000)<0,"an exhausted unchanged panel schedules no tree scan");
        p.observe(4,false,false,10100);p.observe(4,true,true,10200);check(p.delay(4,10200)==0,"new appearance at same bounds may be examined");
        p.requested(4,10200);p.observe(4,true,false,10300);check(p.delay(4,10300)==400,"layout churn does not refill or reset the existing lease");
        p.observe(5,true,false,10500);check(p.delay(5,10500)<0&&p.delay(4,10500)<0,"windownumber churn shares cooldown and clears old work");
        check(p.treeDelay(5,10500)==2200&&p.treeDelay(4,10500)<0,"fresh evidence schedules one cooldown recheck for its own window");
        p.observe(5,true,false,12800);check(p.delay(5,12800)==0,"fresh weak context receives a bounded opportunity after cooldown");
        check(p.treeDelay(5,12800)<0,"arming the allowance consumes the deferred wake");
        p.requested(5,12800);p.requested(5,13300);check(p.delay(5,14000)<0,"weak evidence gets no third discovery screenshot");
        p.observe(4,false,false,15000);p.observe(4,true,true,15500);check(p.delay(4,18000)<0,"unused opportunities expire");
        p.observe(4,false,false,18100);p.observe(4,true,true,18200);p.attempted(4);check(p.delay(4,18200)<0,"after action only the separate bounded review is possible");
        p.observe(4,true,true,19000);check(p.delay(4,19000)<0&&p.attemptedIn(4),"panel movement does not repeat the attempt or switch to a second native action");
        check(!p.observe(4,false,false,19010)&&p.attemptedIn(4),"one animation miss does not erase an attempt");
        check(p.treeDelay(4,19010)==180&&p.treeDelay(5,19010)<0,"a first miss requests one same-window tree confirmation");
        p.observe(4,true,true,19060);check(p.treeDelay(4,19060)<0,"an animation reappearance cancels the tree wake");
        check(!p.observe(4,false,false,19210),"reappearance ends the consecutive disappearance observation");
        check(p.treeDelay(4,19400)==0,"fresh absence must be read once at the deadline");
        check(p.observe(4,false,false,19400)&&!p.attemptedIn(4),"fresh meaningful disappearance clears the attempted block");
        check(p.treeDelay(4,19400)<0,"confirmed disappearance does not renew the timer");
        p.observe(4,true,true,19420);
        check(p.delay(4,19420)<0&&p.treeDelay(4,19420)==1280,"new same-window evidence keeps the prior cooldown");
        p.observe(4,true,true,20700);check(p.delay(4,20700)==0,"the next same-window advertisement re-arms independently at cooldown end");
        p.reset();check(p.delay(4,20700)<0&&p.treeDelay(4,20700)<0,"environment reset clears old scene");
        RenderedPanelProbes rendered=new RenderedPanelProbes();rendered.window(27725);
        check(rendered.ready(88,1100),"mixed renderer scene can take one event-driven discovery frame");
        rendered.requested(88,1100);
        check(!rendered.ready(206,4500),"count changes still share the minimum spacing");
        check(!rendered.ready(95,6000),"ordinary updates in a stable mixed tree do not capture again");
        check(rendered.ready(126,6000),"a material scene change can use the second slot");
        rendered.requested(126,6000);rendered.window(27729);
        check(!rendered.ready(88,10000),"a window change does not refill the visit budget");
        rendered.reset();rendered.window(27725);rendered.markAttempt();
        check(!rendered.ready(88,11000),"the same weak panel cannot keep clicking");
        rendered.window(27729);
        check(!rendered.attempted()&&rendered.ready(88,11000),"a genuinely new window gets its own attempt state");
        System.out.println("PASS: "+checks+" bounded pause-ad discovery checks");
    }
}
