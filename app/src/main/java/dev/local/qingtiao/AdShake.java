package dev.local.qingtiao;

/** A recent shake may corroborate one immediate transition from a verified startup ad. */
final class AdShake {
    static final long OBSERVATION_MS=2400;
    private String source="";
    private int window=-1;
    private long since,impulse=-1,shake=-1;

    boolean begin(String pkg,int windowId,long now){
        if(pkg.equals(source)&&windowId==window)return false;
        source=pkg;
        window=windowId;
        since=now;
        impulse=shake=-1;
        return true;
    }

    boolean observing(long now){return !source.isEmpty()&&now>=since&&now-since<OBSERVATION_MS;}

    void motion(float x,float y,float z,long now){
        if(!observing(now))return;
        double magnitude=Math.sqrt(x*x+y*y+z*z);
        if(!(Math.abs(magnitude-9.81)>=6.5))return;
        if(impulse>=0&&now-impulse>=55&&now-impulse<=300)shake=now;
        if(impulse<0||now-impulse>=55)impulse=now;
    }

    void clicked(String pkg,long at){
        if(source.equals(pkg)&&observing(at))since=at-OBSERVATION_MS;
    }

    String transition(String pkg,int windowId,boolean landing,long now){
        if(source.isEmpty()||pkg.equals(source)&&windowId==window)return "";
        String result=observing(now)&&landing&&!pkg.equals(source)&&shake>=0&&now>=shake&&now-shake<=850?source:"";
        clear();
        return result;
    }

    void clear(){source="";window=-1;impulse=shake=-1;}
}
