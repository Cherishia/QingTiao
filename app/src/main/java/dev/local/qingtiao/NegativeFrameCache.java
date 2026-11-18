package dev.local.qingtiao;

/** Reuses only a negative result briefly; retains two hashes, never pixels or a touch target. */
public final class NegativeFrameCache {
    public static final long MAX_AGE=1000;
    public static final class Sample {
        private final String scene;
        private final long a,b,frame;
        public final String reason;
        private Sample(String scene,long a,long b,long frame,String reason){this.scene=scene;this.a=a;this.b=b;this.frame=frame;this.reason=reason;}
    }
    private String scene="",reason;
    private long a,b,analyzedAt=-1,frame=-1;
    public synchronized Sample sample(String key,int[] pixels,int width,int height,long timestamp,long now) {
        if(key==null||key.isEmpty()||pixels==null||width<=0||height<=0||(long)width*height!=pixels.length||now<timestamp||now-timestamp>600){clear();return null;}
        long x=0xcbf29ce484222325L,y=0x9e3779b97f4a7c15L;
        for(int p:pixels){x=(x^(p&0xffffffffL))*0x100000001b3L;y=Long.rotateLeft(y,7)^(p&0xffffffffL);y*=0x9e3779b185ebca87L;}
        x^=((long)width<<32)|height;y^=pixels.length;
        boolean same=key.equals(scene)&&x==a&&y==b&&timestamp>frame&&timestamp>analyzedAt&&now>=analyzedAt&&now-analyzedAt<MAX_AGE;
        if(same)frame=timestamp;
        return new Sample(key,x,y,timestamp,same?reason:null);
    }
    public synchronized void remember(Sample s,String negativeReason,boolean negative) {
        if(s==null||!negative||negativeReason==null){clear();return;}
        scene=s.scene;a=s.a;b=s.b;frame=analyzedAt=s.frame;reason=negativeReason;
    }
    public synchronized void clear(){scene="";reason=null;analyzedAt=frame=-1;}
}
