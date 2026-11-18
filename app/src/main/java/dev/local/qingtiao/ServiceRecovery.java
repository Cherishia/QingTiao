package dev.local.qingtiao;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

/** Optional status check. Android controls whether accessibility is enabled or bound. */
public final class ServiceRecovery extends JobService {
    static final String ENABLED="recovery_reminder";
    private static final String PAUSED="accessibility_paused_for_app";
    private static final String USED="accessibility_previously_connected";
    private static final String NOTICE="recovery_notice_state";
    private static final String CHANNEL="service_recovery";
    private static final int ID=5321,RECHECK=5322;
    private static final long PERIOD_MS=30*60*1000L;

    private static SharedPreferences prefs(Context context){
        return context.getSharedPreferences(LocalLog.PREFS,0);
    }

    static boolean systemEnabled(Context context){
        ComponentName service=new ComponentName(context,SkipService.class);
        String configured=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if(configured!=null){
            for(String name:configured.split(":")){
                if(service.equals(ComponentName.unflattenFromString(name)))return true;
            }
        }
        AccessibilityManager manager=context.getSystemService(AccessibilityManager.class);
        if(manager!=null)for(AccessibilityServiceInfo info:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){
            if(info.getResolveInfo()==null||info.getResolveInfo().serviceInfo==null)continue;
            android.content.pm.ServiceInfo registered=info.getResolveInfo().serviceInfo;
            if(service.equals(new ComponentName(registered.packageName,registered.name)))return true;
        }
        return false;
    }

    static boolean pausedForApp(Context context){return prefs(context).getBoolean(PAUSED,false);}

    static void pauseForApp(Context context){
        prefs(context).edit().putBoolean(PAUSED,true).apply();
        sync(context);
    }

    static void connected(Context context){
        SharedPreferences p=prefs(context);
        if(!p.getBoolean(USED,false)||p.contains(PAUSED)||p.contains(NOTICE))
            p.edit().putBoolean(USED,true).remove(PAUSED).remove(NOTICE).apply();
        cancelRecheck(context);
        cancelNotice(context);
        sync(context);
    }

    private static boolean remindersOn(SharedPreferences p){
        return p.getBoolean(ENABLED,false)&&p.getBoolean("running",true)
            &&p.getBoolean(USED,false)&&!p.getBoolean(PAUSED,false);
    }

    static boolean canNotify(Context context){
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            return false;
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager==null||!manager.areNotificationsEnabled())return false;
        NotificationChannel channel=manager.getNotificationChannel(CHANNEL);
        return channel==null||channel.getImportance()!=NotificationManager.IMPORTANCE_NONE;
    }

    static void sync(Context context){
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);
        if(scheduler==null)return;
        if(!remindersOn(prefs(context))||!canNotify(context)){
            if(scheduler.getPendingJob(ID)!=null)scheduler.cancel(ID);
            cancelRecheck(context);
            cancelNotice(context);
        } else if(scheduler.getPendingJob(ID)==null){
            try {
                scheduler.schedule(new JobInfo.Builder(ID,new ComponentName(context,ServiceRecovery.class))
                    .setPeriodic(PERIOD_MS).setPersisted(true).build());
            } catch(IllegalStateException|SecurityException error) {
                LocalLog.add(context,"系统暂未安排无障碍状态检查");
            }
        }
    }

    static void check(Context context){
        check(context,false);
    }

    private static void check(Context context,boolean verified){
        SharedPreferences p=prefs(context);
        if(!remindersOn(p)||!canNotify(context))return;
        boolean connected=SkipService.connected;
        RecoveryState.Next next=RecoveryState.assess(true,connected,!connected&&systemEnabled(context),verified);
        if(next==RecoveryState.Next.CLEAR){
            if(p.contains(NOTICE))p.edit().remove(NOTICE).apply();
            cancelNotice(context);cancelRecheck(context);return;
        }
        if(next==RecoveryState.Next.VERIFY_LATER){
            if(!RecoveryState.Next.REMIND_DISCONNECTED.name().equals(p.getString(NOTICE,"")))scheduleRecheck(context);
            return;
        }
        if(next==RecoveryState.Next.REMIND_DISABLED)cancelRecheck(context);
        if(next.name().equals(p.getString(NOTICE,"")))return;
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,"无障碍关闭提醒",NotificationManager.IMPORTANCE_LOW));
        Intent settings=new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent open=PendingIntent.getActivity(context,ID,settings,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        boolean disabled=next==RecoveryState.Next.REMIND_DISABLED;
        manager.notify(ID,new Notification.Builder(context,CHANNEL).setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(disabled?"轻跳的无障碍已关闭":"轻跳的无障碍暂未重连")
            .setContentText(disabled?"点此进入系统设置，重新开启轻跳。":"系统开关仍开启；点此检查，必要时重新启用轻跳。")
            .setContentIntent(open).setAutoCancel(true).build());
        p.edit().putString(NOTICE,next.name()).apply();
    }

    private static void scheduleRecheck(Context context){
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);
        if(scheduler==null||scheduler.getPendingJob(RECHECK)!=null)return;
        try {
            scheduler.schedule(new JobInfo.Builder(RECHECK,new ComponentName(context,ServiceRecovery.class))
                .setMinimumLatency(45000).build());
        } catch(IllegalStateException|SecurityException error){
            LocalLog.add(context,"系统暂未安排无障碍重连复查");
        }
    }

    private static void cancelRecheck(Context context){
        JobScheduler scheduler=context.getSystemService(JobScheduler.class);
        if(scheduler!=null&&scheduler.getPendingJob(RECHECK)!=null)scheduler.cancel(RECHECK);
    }

    private static void cancelNotice(Context context){
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager!=null)manager.cancel(ID);
    }

    @Override public boolean onStartJob(JobParameters parameters){check(this,parameters.getJobId()==RECHECK);return false;}
    @Override public boolean onStopJob(JobParameters parameters){return false;}
}
