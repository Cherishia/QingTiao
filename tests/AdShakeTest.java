package dev.local.qingtiao;

public final class AdShakeTest {
    private static void check(boolean condition,String description){
        if(!condition)throw new AssertionError(description);
    }
    private static void shake(AdShake policy){
        policy.motion(0,0,19,1100);
        policy.motion(0,0,20,1165);
    }
    public static void main(String[] args){
        AdShake policy=new AdShake();
        check(policy.transition("browser",2,true,1200).isEmpty(),"browser without an ad");
        check(policy.begin("source",1,1000)&&!policy.begin("source",1,1050),"same ad window arms only once");
        policy.motion(0,0,10,1100);
        policy.motion(0,0,19,1165);
        check(policy.transition("browser",2,true,1200).isEmpty(),"single impulse does not return");
        policy.begin("source",1,1000);shake(policy);policy.clicked("source",1180);
        check(policy.transition("browser",2,true,1200).isEmpty(),"an accessible tap cancels the return");
        policy.begin("source",1,1000);shake(policy);
        check(policy.transition("message_app",2,false,1200).isEmpty(),"unrelated destination is never touched");
        policy.begin("source",1,1000);shake(policy);
        check(policy.transition("browser",2,true,2150).isEmpty(),"old shake cannot authorize a recent navigation");
        policy.begin("source",1,1000);shake(policy);
        check(policy.transition("browser",2,true,3450).isEmpty(),"ad observation expires");
        policy.begin("source",1,1000);shake(policy);
        check(policy.transition("source",3,false,1200).isEmpty(),"same app changing windows abandons the sample");
        check(policy.transition("browser",2,true,1240).isEmpty(),"new window cannot inherit an old shake");
        policy.begin("source",1,1000);shake(policy);
        check(policy.transition("source",1,false,1180).isEmpty(),"staying in the current window is passive");
        check("source".equals(policy.transition("browser",2,true,1200)),"verified recent ad and shake authorize one return");
        check(policy.transition("browser",2,true,1220).isEmpty(),"return authorization is consumed");
        System.out.println("PASS: 12 bounded shake-redirect checks");
    }
}
