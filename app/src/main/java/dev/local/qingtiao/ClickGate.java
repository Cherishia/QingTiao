package dev.local.qingtiao;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/** Requires a clicked target to disappear before it can be clicked again. */
public final class ClickGate {
    private final Set<String> blocked=new HashSet<>();
    private final ArrayDeque<Long> clicks=new ArrayDeque<>();
    private long lastClick=-10000;
    public void observe(Set<String> visible) {blocked.retainAll(visible);}
    /** Return whether the pending control is still visible, independently of the repeat-click block. */
    <T> boolean observe(Iterable<T> snapshot,Function<T,String> visibleKey,String pendingKey){
        if(blocked.isEmpty()&&pendingKey==null)return false;
        Set<String> visibleBlocked=new HashSet<>();
        boolean pendingVisible=false;
        for(T item:snapshot){
            String key=visibleKey.apply(item);
            if(key==null)continue;
            if(key.equals(pendingKey))pendingVisible=true;
            if(blocked.contains(key))visibleBlocked.add(key);
        }
        observe(visibleBlocked);
        return pendingVisible;
    }
    public void releasePrefix(String prefix) {blocked.removeIf(key->key.startsWith(prefix));}
    public boolean allows(String key,long now){return allows(key,now,1800);}
    /** Used only after a second Pica notice has independently passed scene-identity checks. */
    public boolean allowsNextNotice(String key,long now){return allows(key,now,700);}
    private boolean allows(String key,long now,long interval) {
        while(!clicks.isEmpty() && now-clicks.peekFirst()>=60000) clicks.removeFirst();
        return !blocked.contains(key) && now-lastClick>=interval && clicks.size()<8;
    }
    public void record(String key,long now) {blocked.add(key);lastClick=now;clicks.addLast(now);}
}
