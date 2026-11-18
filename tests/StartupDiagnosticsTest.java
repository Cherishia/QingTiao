package dev.local.qingtiao;

public final class StartupDiagnosticsTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args) {
        StartupProbe probe=new StartupProbe();probe.reset(1000);
        check(probe.delay(1000)==0,"first launch probe immediate");probe.requested(1000);
        check(probe.delay(1100)==600,"ordinary page probes are spread out");
        check(probe.delay(1700)==0,"second planned probe");probe.requested(1700);
        check(probe.delay(1750)==1250,"wait for later loading opportunity");
        probe.requested(3000);probe.requested(6000);
        check(probe.delay(6100)>0,"four probes do not end late recognition");
        check(probe.active(16000)&&probe.delay(16000)==0,"late startup remains inspectable");
        probe.requested(16000);check(probe.delay(17000)==6000,"last probe reserved for late loading");
        probe.requested(23000);check(probe.count()==6&&probe.delay(24000)<0,"six no-evidence captures per entry");
        probe.window(10,24000);probe.window(11,25000);check(probe.delay(25000)==0,"new window still grants an urgent opportunity");probe.requested(25000);
        probe.window(12,26000);probe.requested(26000);check(probe.count()==8,"at most two extra window captures");
        probe.window(13,27000);check(probe.delay(27000)<0,"window churn cannot make captures unlimited");
        probe.reset(20000);check(probe.count()==0&&probe.delay(20000)==0,"new app restarts bounded schedule");
        check(!probe.active(19999)&&probe.delay(19999)<0,"negative age denied");
        check(probe.active(40000),"startup can still inspect past twenty seconds");
        check(!probe.active(50000)&&probe.delay(50000)<0,"thirty-second deadline preserved");
        probe.reset(1000);probe.window(10,1000);probe.requested(1000);probe.requested(1700);
        probe.window(11,1750);check(probe.delay(1750)==400,"new window respects capture spacing");
        check(probe.delay(2150)==0,"window preempts later plan");probe.requested(2150);
        check(probe.count()==3&&probe.delay(2151)==849,"extra capture preserves later planned opportunity");
        probe.window(12,2200);probe.requested(2600);check(probe.count()==4,"second extra remains bounded");
        probe.window(14,32000);check(!probe.active(32000)&&probe.delay(32000)<0,"windows never restart deadline");
        DelayedAdWatch delayed=new DelayedAdWatch();
        check(DelayedAdWatch.active("com.jiaohua_browser",false),"verified JM can wait for user-selected route beyond startup");
        check(!DelayedAdWatch.active("other.app",false)&&!DelayedAdWatch.active("com.jiaohua_browser",true),"no generic late monitoring or repeated final close");
        delayed.requested(40000);check(delayed.delay(41000)==1000,"manual route wait uses a slow probe rate");
        check(delayed.delay(120000)==0&&DelayedAdWatch.active("com.jiaohua_browser",false),"taking over a minute to select route does not expire later ad detection");
        delayed.reset();check(delayed.delay(120001)==0,"foreground reset releases old delayed probe schedule");
        ReadingHistory history=new ReadingHistory();String zero="采样：00:00:01\n应用：demo.jm\n广告标识=0，候选=0\n结果：画面读取错误6";
        history.note("demo.jm",zero);history.note("demo.pica","采样：00:00:02\n应用：demo.pica\n控件=120\n结果：未匹配字形");
        check(history.entries().size()==2,"unidentified pages remain available by app");
        check(history.entries().get(0).pkg.equals("demo.pica"),"most recent app first");
        history.note("demo.jm",zero+"\n重读");
        check(history.entries().size()==2&&history.entries().get(0).pkg.equals("demo.jm"),"same app updates and moves to top");
        ReadingHistory restored=ReadingHistory.decode(history.encode());
        check(restored.entries().size()==2&&restored.entries().get(0).report.endsWith("重读"),"restart retains failures and Chinese text");
        check(restored.entries().get(1).pkg.equals("demo.pica"),"restart preserves order");
        history.note("bad\npackage","malformed");check(history.entries().size()==2,"invalid package cannot corrupt history format");
        history.note("demo.empty","");check(history.entries().size()==2,"empty report ignored");
        for(int i=0;i<12;i++)history.note("demo.app"+i,"候选=0");
        check(history.entries().size()==8&&history.entries().get(0).pkg.equals("demo.app11")&&history.entries().get(7).pkg.equals("demo.app4"),"old entries evicted at eight apps");
        check(ReadingHistory.decode("corrupt:%%%\n").entries().isEmpty(),"bad saved data is tolerated");
        StringBuilder longText=new StringBuilder();for(int i=0;i<9000;i++)longText.append('字');
        for(int i=0;i<8;i++)history.note("demo.app"+i,longText.toString());
        restored=ReadingHistory.decode(history.encode());
        check(restored.entries().size()==8&&restored.entries().get(0).report.length()==8000,"maximum-sized Chinese history round trips and stays bounded");
        System.out.println("PASS: "+checks+" startup and recording checks");
    }
}
