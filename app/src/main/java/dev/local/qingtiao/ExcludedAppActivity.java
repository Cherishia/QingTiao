package dev.local.qingtiao;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Collections;

/** Private entry point of the user-approved desktop shortcut or the confirmed in-app action. */
public final class ExcludedAppActivity extends Activity {
    static final String ACTION="dev.local.qingtiao.action.OPEN_EXCLUDED";
    static final String PACKAGE="excluded_package";
    static final String REOPEN="reopen_task";
    private final Handler handler=new Handler(Looper.getMainLooper());
    private Intent launch;
    private String destination;
    private TextView message;
    private ExcludedAppLaunchGate gate;
    private boolean requested,waitingForSettings,reopen;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        String pkg=getIntent().getStringExtra(PACKAGE);
        SharedPreferences prefs=getSharedPreferences(LocalLog.PREFS,0);
        if(pkg==null||!prefs.getStringSet(LocalLog.EXCLUDED,Collections.emptySet()).contains(pkg)){
            stop("这个应用已从排除列表移除");
            return;
        }
        launch=getPackageManager().getLaunchIntentForPackage(pkg);
        if(launch==null){stop("找不到这个应用的启动入口");return;}
        try {destination=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
        catch(android.content.pm.PackageManager.NameNotFoundException missing){stop("这个应用已卸载");return;}
        reopen=getIntent().getBooleanExtra(REOPEN,false)||!accessibilityStopped();
        if(state!=null){
            waitingForSettings=state.getBoolean("settings");
            reopen=state.getBoolean("reopen");
        }
        ServiceRecovery.pauseForApp(this);
        int padding=(int)(24*getResources().getDisplayMetrics().density+.5f);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);layout.setPadding(padding,padding,padding,padding);
        message=new TextView(this);message.setTextSize(17);message.setGravity(Gravity.CENTER);
        message.setText("正在停用轻跳，即将打开 "+destination);layout.addView(message);
        Button cancel=new Button(this);cancel.setText("取消打开");cancel.setOnClickListener(v->finish());layout.addView(cancel);
        setContentView(layout);
    }

    @Override protected void onResume(){
        super.onResume();
        if(launch==null||isFinishing())return;
        requested=false;
        gate=new ExcludedAppLaunchGate(SystemClock.uptimeMillis());
        checkStopped();
    }

    @Override protected void onPause(){
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state){
        state.putBoolean("settings",waitingForSettings);
        state.putBoolean("reopen",reopen);
        super.onSaveInstanceState(state);
    }

    private boolean accessibilityStopped(){return !ServiceRecovery.systemEnabled(this)&&!SkipService.connected;}

    private void checkStopped(){
        boolean stopped=accessibilityStopped();
        switch(gate.observe(stopped,SystemClock.uptimeMillis())){
            case OPEN:
                Intent target=new Intent(launch);
                if(reopen)target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                try {startActivity(target);finish();}
                catch(RuntimeException unavailable){stop("无法打开 "+destination);}
                return;
            case SETTINGS:
                if(waitingForSettings){stop("轻跳无障碍仍处于启用状态，请先停用");return;}
                waitingForSettings=true;
                message.setText("请停用轻跳，然后返回这里打开 "+destination);
                try {startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
                catch(RuntimeException unavailable){stop("请到系统无障碍设置停用轻跳后再打开应用");}
                return;
            case WAIT:
                if(!stopped&&!requested&&!waitingForSettings)requested=SkipService.disableAccessibilityFromHome();
                handler.postDelayed(this::checkStopped,120);
        }
    }

    private void stop(String reason){Toast.makeText(this,reason,Toast.LENGTH_LONG).show();finish();}
}
