package dev.local.qingtiao;

/** Coalesces only repeated discovery reads, never authorizes or caches an action target. */
public final class TreeReadGate {
    private String owner="";private int win=-1,screen=-1,width,height;private long finished=-10000;
    public long delay(String pkg,int window,int display,int w,int h,long now,boolean priority,boolean opening) {
        if(priority||!owner.equals(pkg)||window!=win||display!=screen||w!=width||h!=height||now<finished)return 0;
        return Math.max(0,(opening?350:1000)-(now-finished));
    }
    public void readFinished(String pkg,int window,int display,int w,int h,long now){owner=pkg;win=window;screen=display;width=w;height=h;finished=now;}
    public void reset(){owner="";win=screen=-1;finished=-10000;}
    public boolean recent(long now){return !owner.isEmpty()&&now>=finished&&now-finished<1000;}
    public static boolean cue(CharSequence raw){
        if(raw==null||raw.length()==0||raw.length()>256)return false;
        String s=raw.toString();return MatchPolicy.isSkipText(s)||MatchPolicy.isAdEvidence(s,null)||s.contains("青少年")||s.contains("未成年");
    }
}
