package dev.local.qingtiao;

public final class RecoveryStateTest {
    private static void check(boolean ok){if(!ok)throw new AssertionError("Incorrect recovery state");}
    public static void main(String[] args){
        check(RecoveryState.assess(true,false,true,false)==RecoveryState.Next.VERIFY_LATER);
        check(RecoveryState.assess(true,false,true,true)==RecoveryState.Next.REMIND_DISCONNECTED);
        check(RecoveryState.assess(true,true,true,true)==RecoveryState.Next.CLEAR);
        check(RecoveryState.assess(true,false,false,false)==RecoveryState.Next.REMIND_DISABLED);
        check(RecoveryState.assess(false,false,false,true)==RecoveryState.Next.CLEAR);
        System.out.println("PASS: 5 accessibility recovery state checks");
    }
}
