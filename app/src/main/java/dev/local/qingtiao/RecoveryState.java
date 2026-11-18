package dev.local.qingtiao;

/** Wait for Android to reconnect an enabled service before reporting a disconnect. */
final class RecoveryState {
    enum Next { CLEAR, VERIFY_LATER, REMIND_DISCONNECTED, REMIND_DISABLED }

    static Next assess(boolean watching,boolean connected,boolean enabled,boolean verified){
        if(!watching||connected)return Next.CLEAR;
        if(!enabled)return Next.REMIND_DISABLED;
        return verified?Next.REMIND_DISCONNECTED:Next.VERIFY_LATER;
    }

    private RecoveryState(){}
}
