package dev.local.qingtiao;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class LocalLog {
    static final String PREFS="qingtiao";
    static synchronized void add(Context c,String message) {
        SharedPreferences p=c.getSharedPreferences(PREFS,0);
        String line=new SimpleDateFormat("MM-dd HH:mm:ss",Locale.CHINA).format(new Date())+"  "+message;
        String old=p.getString("log","");String all=line+"\n"+old;
        String[] lines=all.split("\n");StringBuilder out=new StringBuilder();
        for(int i=0;i<Math.min(lines.length,80);i++) out.append(lines[i]).append('\n');
        p.edit().putString("log",out.toString()).apply();
    }
    static final String ACTIONS="recent_actions", EXCLUDED="excluded_apps", SHAKE="shake_return", VIBRATE="action_vibration";
    private static String today(Date date){return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(date);}
    static int todayCount(SharedPreferences p){
        return today(new Date()).equals(p.getString("action_day",""))?p.getInt("today_clicks",0):0;
    }
    static synchronized void recordAction(Context c,String pkg,String description,boolean redirect) {
        SharedPreferences p=c.getSharedPreferences(PREFS,0);
        Date now=new Date();
        String old=p.getString(ACTIONS,"");
        String time=new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(now);
        String entries=time+"\t"+pkg+"\t"+description+"\n"+old;
        int end=0;
        for(int i=0;i<40&&end<entries.length();i++){
            int next=entries.indexOf('\n',end);
            end=next<0?entries.length():next+1;
        }
        SharedPreferences.Editor edit=p.edit().putString(ACTIONS,entries.substring(0,end));
        if(redirect)edit.putInt("redirects",p.getInt("redirects",0)+1);
        else {
            String day=today(now);
            int count=day.equals(p.getString("action_day",""))?p.getInt("today_clicks",0):0;
            edit.putString("action_day",day).putInt("today_clicks",count+1).putInt("clicks",p.getInt("clicks",0)+1);
        }
        edit.apply();
        if(p.getBoolean(VIBRATE,false))feedback(c);
    }
    private static void feedback(Context c) {
        try {
            android.os.Vibrator vibrator=(android.os.Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);
            if(vibrator!=null&&vibrator.hasVibrator())vibrator.vibrate(android.os.VibrationEffect.createOneShot(20,75));
        } catch(RuntimeException ignored) { /* Feedback does not affect the completed action. */ }
    }
    private LocalLog() {}
}
