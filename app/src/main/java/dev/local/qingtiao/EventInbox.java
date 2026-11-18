package dev.local.qingtiao;

/** Coalesces notifications while retaining the actual delivery and event times. */
public final class EventInbox {
    public static final class Event {
        public final String pkg;
        public final int types;
        public final long occurredAt,arrivedAt;
        public final boolean cue;
        Event(String pkg,int types,long occurredAt,long arrivedAt,boolean cue) {
            this.pkg=pkg;this.types=types;this.occurredAt=occurredAt;this.arrivedAt=arrivedAt;this.cue=cue;
        }
    }
    private Event latest;
    private boolean scheduled;
    public synchronized boolean offer(String pkg,int type,long occurredAt,long arrivedAt) {
        return offer(pkg,type,occurredAt,arrivedAt,false);
    }
    public synchronized boolean offer(String pkg,int type,long occurredAt,long arrivedAt,boolean cue) {
        int types=type|(latest==null?0:latest.types);
        latest=new Event(pkg,types,occurredAt,arrivedAt,cue||latest!=null&&latest.cue);
        if(scheduled)return false;
        scheduled=true;return true;
    }
    public synchronized Event take() {Event value=latest;latest=null;scheduled=false;return value;}
    public synchronized void clear(){latest=null;scheduled=false;}
}
