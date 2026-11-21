package dev.local.qingtiao;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

/** Owns one user-initiated update across activity recreation. All UI state lives on the main thread. */
final class AppUpdate {
    private static AppUpdate instance;
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final String version;
    private MainActivity screen;
    private boolean busy,ready,installWhenVisible,waitingForPermission;
    private String label="检查更新";

    static AppUpdate get(Context context){
        if(instance==null)instance=new AppUpdate(context.getApplicationContext());
        return instance;
    }

    private AppUpdate(Context context){
        this.context=context;
        try {version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName;}
        catch(PackageManager.NameNotFoundException invalid){throw new IllegalStateException(invalid);}
    }

    String version(){return version;}

    void attach(MainActivity screen){
        this.screen=screen;
        publish();
        if(waitingForPermission){
            waitingForPermission=false;
            if(context.getPackageManager().canRequestPackageInstalls())install();
        } else if(installWhenVisible)install();
    }

    void detach(MainActivity screen){
        if(this.screen==screen)this.screen=null;
    }

    void check(){
        if(busy)return;
        if(ready&&UpdateFileProvider.apk(context).isFile()){install();return;}
        ready=false;busy=true;label="检查中…";publish();
        new Thread(()-> {
            try {
                UpdateRelease release=UpdateRelease.findNewer(version);
                if(release==null){main.post(()->finished("已是最新版"));return;}
                main.post(()-> {label="下载 "+release.version+"…";publish();});
                release.downloadVerified(context);
                main.post(()-> {
                    ready=true;installWhenVisible=true;finished(null);
                    if(screen!=null)install();
                });
            } catch(UpdateRelease.Problem reason){
                main.post(()->finished(reason.getMessage()));
            } catch(Exception failure) {
                main.post(()->finished("更新失败，请检查网络后重试"));
            }
        },"QingTiao-update").start();
    }

    private void finished(String message){
        busy=false;label=ready?"安装更新":"检查更新";publish();
        if(message!=null)show(message);
    }

    private void install(){
        if(screen==null)return;
        installWhenVisible=false;
        if(!ready||!UpdateFileProvider.apk(context).isFile()){
            ready=false;finished("安装包已清理，请重新检查更新");return;
        }
        try {
            if(!context.getPackageManager().canRequestPackageInstalls()){
                Intent permission=new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+context.getPackageName()));
                waitingForPermission=true;
                screen.startActivity(permission);
                show("请允许轻跳安装更新，然后返回");
                return;
            }
            Uri apk=UpdateFileProvider.uri(context);
            Intent installer=new Intent(Intent.ACTION_VIEW).setDataAndType(apk,UpdateFileProvider.APK_TYPE)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installer.setClipData(ClipData.newRawUri("QingTiao.apk",apk));
            screen.startActivity(installer);
        } catch(RuntimeException unavailable){
            waitingForPermission=false;show("无法打开系统安装界面");
        }
    }

    private void publish(){
        if(screen!=null)screen.showUpdateState(label,!busy);
    }

    private void show(String message){
        if(screen!=null)Toast.makeText(screen,message,Toast.LENGTH_LONG).show();
    }
}
