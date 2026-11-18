package dev.local.qingtiao;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.nio.charset.StandardCharsets;

/** Bounded diagnostic pages, including zero matches; never stores screenshots or page body text. */
public final class ReadingHistory {
    private static final java.util.regex.Pattern PACKAGE=java.util.regex.Pattern.compile("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+");
    public static final class Entry {
        public final String pkg,report;
        Entry(String pkg,String report){this.pkg=pkg;this.report=report;}
    }
    private final ArrayList<Entry> entries=new ArrayList<>();
    public synchronized void note(String pkg,String report) {
        if(pkg==null||!PACKAGE.matcher(pkg).matches()||report==null||report.isEmpty())return;
        for(int i=entries.size()-1;i>=0;i--)if(entries.get(i).pkg.equals(pkg))entries.remove(i);
        entries.add(0,new Entry(pkg,report.substring(0,Math.min(8000,report.length()))));
        while(entries.size()>8)entries.remove(entries.size()-1);
    }
    public synchronized List<Entry> entries(){return Collections.unmodifiableList(new ArrayList<>(entries));}
    public synchronized String encode() {
        StringBuilder out=new StringBuilder();
        for(Entry e:entries)out.append(b64(e.pkg)).append(':').append(b64(e.report)).append('\n');
        return out.toString();
    }
    public static ReadingHistory decode(String text) {
        ReadingHistory history=new ReadingHistory();
        if(text==null||text.length()>400000)return history;
        String[] lines=text.split("\n");
        for(int i=Math.min(8,lines.length)-1;i>=0;i--)try {
            int at=lines[i].indexOf(':');if(at<0)continue;
            history.note(un64(lines[i].substring(0,at)),un64(lines[i].substring(at+1)));
        } catch(IllegalArgumentException ignored){}
        return history;
    }
    private static String b64(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String un64(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
}
