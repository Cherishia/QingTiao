package dev.local.qingtiao;

public final class ExcludedAppLaunchGateTest {
    private static void check(boolean condition,String name){if(!condition)throw new AssertionError(name);}
    public static void main(String[] args){
        ExcludedAppLaunchGate gate=new ExcludedAppLaunchGate(1000);
        check(gate.observe(false,1000)==ExcludedAppLaunchGate.Next.WAIT,"enabled service does not launch bank");
        check(gate.observe(true,1250)==ExcludedAppLaunchGate.Next.WAIT,"first stopped sample does not launch");
        check(gate.observe(false,1350)==ExcludedAppLaunchGate.Next.WAIT,"temporary confirmation resets after a rebound");
        check(gate.observe(true,1450)==ExcludedAppLaunchGate.Next.WAIT,"start a fresh disabled interval");
        check(gate.observe(true,1929)==ExcludedAppLaunchGate.Next.WAIT,"stable interval cannot be shortened");
        check(gate.observe(true,1930)==ExcludedAppLaunchGate.Next.OPEN,"confirmed stopped state allows launch");
        gate=new ExcludedAppLaunchGate(1000);
        check(gate.observe(false,5500)==ExcludedAppLaunchGate.Next.SETTINGS,"requires an actual settings change on timeout");
        System.out.println("PASS: 7 excluded app launch checks");
    }
}
