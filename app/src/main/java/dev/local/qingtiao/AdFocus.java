package dev.local.qingtiao;

/** A fresh explicit ad cue gets one short discovery burst; repeats never extend it. */
public final class AdFocus {
    private String owner="",cue="";
    private int window=-1;
    private long until=-1,lastSeen=-10000,lastArmed=-10000;
    public void observe(String pkg,int id,String current,long now){
        if(pkg==null||pkg.isEmpty()||id<0){reset();return;}
        if(!pkg.equals(owner)){reset();owner=pkg;}
        String key=current==null?"":current;
        if(key.isEmpty()){cue="";return;}
        boolean changed=id!=window||!key.equals(cue)||now-lastSeen>=1200;
        if(changed&&now-lastArmed>=8000){until=now+2500;lastArmed=now;}
        window=id;cue=key;lastSeen=now;
    }
    public boolean rapid(long now){return !cue.isEmpty()&&now>=lastSeen&&now-lastSeen<1200&&now<until;}
    public void reset(){owner=cue="";window=-1;until=-1;lastSeen=lastArmed=-10000;}
}
