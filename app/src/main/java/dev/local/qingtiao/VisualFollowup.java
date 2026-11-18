package dev.local.qingtiao;

/** Bounds observation after one dispatched action. Never authorizes an action. */
final class VisualFollowup {
    private static final long WINDOW=3000;
    private int window=-1;
    private String pkg="";
    private long started=-1;

    void begin(String pkg,int window,long now){this.pkg=pkg;this.window=window;started=now;}
    boolean pending(){return started>=0;}
    boolean active(long now){return started>=0&&now>=started&&now-started<WINDOW;}
    boolean current(String owner,int window,long now){return active(now)&&this.window==window&&pkg.equals(owner);}
    void clear(){window=-1;pkg="";started=-1;}
}
