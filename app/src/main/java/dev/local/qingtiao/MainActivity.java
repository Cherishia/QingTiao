package dev.local.qingtiao;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final int ACCENT=0xff5245ad,INK=0xff222037,MUTED=0xff64617a;
    private static final int PAGE=0xfff5f3fa, LILAC=0xffeeebfa, WHITE=0xffffffff, RULE=0xffeeebf4, GREEN=0xff12654c;
    private static final int RECOVERY_NOTIFICATION_REQUEST=31;
    private LinearLayout content;
    private SharedPreferences prefs;
    private TextView status,statusHint,todayActions,totalActions,redirectActions,excludedSummary,recentActions;
    private LinearLayout excludedRows;
    private final SharedPreferences.OnSharedPreferenceChangeListener homeUpdates=(p,key)->{
        if(status==null)return;
        if("running".equals(key)||"service_ui".equals(key))updateStatus();
        if("running".equals(key))ServiceRecovery.sync(this);
        if(LocalLog.ACTIONS.equals(key))updateActivity();
        if(LocalLog.EXCLUDED.equals(key))updateExclusions();
    };
    @Override public void onCreate(Bundle state) {super.onCreate(state);prefs=getSharedPreferences(LocalLog.PREFS,0);}
    @Override protected void onResume() {
        super.onResume();
        render();
        prefs.registerOnSharedPreferenceChangeListener(homeUpdates);
        ServiceRecovery.sync(this);
        ServiceRecovery.check(this);
    }
    @Override protected void onPause() {
        prefs.unregisterOnSharedPreferenceChangeListener(homeUpdates);
        super.onPause();
    }
    private int dp(float value) {return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private TextView text(String value,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setLineSpacing(dp(3),1);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
    }
    private void gap(LinearLayout parent,int size) {parent.addView(new View(this),new LinearLayout.LayoutParams(1,dp(size)));}
    private GradientDrawable background(int color) {GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(22));return d;}
    private LinearLayout card() {
        LinearLayout c=new LinearLayout(this);c.setOrientation(1);c.setPadding(dp(18),dp(16),dp(18),dp(16));c.setBackground(background(WHITE));
        content.addView(c,new LinearLayout.LayoutParams(-1,-2));gap(content,12);return c;
    }
    private Button button(LinearLayout parent,String value,Runnable action) {
        Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(ACCENT);b.setTextSize(15);
        b.setMinHeight(dp(48));b.setPadding(dp(12),dp(12),dp(12),dp(12));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x225245ad),background(LILAC),null));
        LinearLayout.LayoutParams layout=new LinearLayout.LayoutParams(-1,-2);layout.topMargin=dp(4);layout.bottomMargin=dp(4);
        parent.addView(b,layout);b.setOnClickListener(v->action.run());return b;
    }
    private void actionRow(LinearLayout parent,String left,Runnable leftAction,String right,Runnable rightAction,boolean primary) {
        LinearLayout row=new LinearLayout(this);parent.addView(row);
        Button a=button(row,left,leftAction),b=button(row,right,rightAction);
        if(primary){
            a.setTextColor(WHITE);
            a.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30ffffff),background(ACCENT),null));
        }
        LinearLayout.LayoutParams al=new LinearLayout.LayoutParams(0,-2,1),bl=new LinearLayout.LayoutParams(0,-2,1);
        al.rightMargin=dp(5);bl.leftMargin=dp(5);al.topMargin=bl.topMargin=dp(4);al.bottomMargin=bl.bottomMargin=dp(4);
        a.setLayoutParams(al);b.setLayoutParams(bl);
    }
    private boolean enabledInSystem() {
        return ServiceRecovery.systemEnabled(this);
    }
    private void updateStatus() {
        if(status==null)return;
        boolean enabled=enabledInSystem(),live=SkipService.connected,running=prefs.getBoolean("running",true);
        boolean active=enabled&&live&&running;
        boolean pausedForApp=ServiceRecovery.pausedForApp(this);
        status.setText(!enabled?(pausedForApp?"已停用":"待开启"):!live?"待连接":!running?"已暂停":"运行中");
        status.setTextColor(active?GREEN:ACCENT);status.setBackground(background(active?0xffddf3e9:LILAC));
        statusHint.setText(!enabled?(pausedForApp?"用完排除的应用后，可在系统设置重新开启轻跳。":"在系统无障碍中开启轻跳。"):!live?"系统正在连接服务。"
            :!running?"打开自动处理开关即可继续。":"界面有变化时按需识别，锁屏后暂停。");
    }
    private void render() {
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(PAGE);
        content=new LinearLayout(this);content.setOrientation(1);content.setPadding(dp(18),dp(14),dp(18),dp(20));scroll.addView(content);
        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)-> {
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
        });scroll.requestApplyInsets();
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout name=new LinearLayout(this);name.setOrientation(1);
        name.addView(text("轻跳",31,INK,true));name.addView(text("自动处理开屏与浏览广告",13,MUTED,false));
        brand.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        ImageView icon=new ImageView(this);icon.setImageResource(R.mipmap.ic_launcher);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        brand.addView(icon,new LinearLayout.LayoutParams(dp(52),dp(52)));
        content.addView(brand);gap(content,16);

        LinearLayout state=card();state.setBackground(background(0xfffaf8ff));
        LinearLayout title=heading(state,"无障碍服务");
        status=text("",12,ACCENT,true);status.setPadding(dp(11),dp(4),dp(11),dp(4));title.addView(status);
        statusHint=text("",13,MUTED,false);gap(state,6);state.addView(statusHint);gap(state,8);
        actionRow(state,ServiceRecovery.pausedForApp(this)?"去恢复无障碍":"无障碍设置",this::accessibilitySettings,
            "后台运行",this::backgroundHelp,true);

        LinearLayout options=card();heading(options,"自动处理");
        feature(options,"自动跳过广告","开启轻跳的广告识别和操作","running",true);
        divider(options);
        feature(options,"处理浏览广告","关闭支持的暂停广告与弹窗","browsing",true);
        divider(options);
        feature(options,"摇一摇跳转返回","识别广告时短暂检测摇动，跳到浏览器或商店时返回",LocalLog.SHAKE,false);
        divider(options);
        feature(options,"操作后轻震","点击或返回后给出触感提示",LocalLog.VIBRATE,false);
        divider(options);
        Switch recovery=feature(options,"清理后恢复提醒","等待系统重连；仍未恢复时通知你",ServiceRecovery.ENABLED,false);
        recovery.setOnCheckedChangeListener((button,enabled)->{
            if(enabled&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                !=android.content.pm.PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},RECOVERY_NOTIFICATION_REQUEST);
            } else setRecoveryReminder(enabled);
        });

        LinearLayout stats=card();heading(stats,"处理统计");gap(stats,10);
        LinearLayout counts=new LinearLayout(this);stats.addView(counts);
        todayActions=counter(counts,"今日动作");totalActions=counter(counts,"累计动作");redirectActions=counter(counts,"拦截跳转");

        LinearLayout filter=card();link(heading(filter,"不处理的应用"),"管理",this::chooseExcludedApps);
        excludedSummary=text("",13,MUTED,false);gap(filter,5);filter.addView(excludedSummary);
        excludedRows=new LinearLayout(this);excludedRows.setOrientation(1);filter.addView(excludedRows);

        LinearLayout history=card();LinearLayout historyHeading=heading(history,"最近记录");
        link(historyHeading,"抓取记录",this::advertReport);
        recentActions=text("",13,MUTED,false);gap(history,5);history.addView(recentActions);
        TextView footer=text("轻跳 0.5.35",12,MUTED,false);footer.setGravity(Gravity.CENTER);content.addView(footer);
        updateStatus();updateActivity();updateExclusions();
    }
    private void setRecoveryReminder(boolean enabled){
        prefs.edit().putBoolean(ServiceRecovery.ENABLED,enabled).apply();
        ServiceRecovery.sync(this);
        if(enabled){
            if(ServiceRecovery.canNotify(this))ServiceRecovery.check(this);
            else toast("请在系统设置中允许轻跳发送通知");
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){
        super.onRequestPermissionsResult(request,permissions,results);
        if(request!=RECOVERY_NOTIFICATION_REQUEST)return;
        setRecoveryReminder(results.length>0&&results[0]==android.content.pm.PackageManager.PERMISSION_GRANTED);
        render();
    }
    private LinearLayout heading(LinearLayout parent,String label){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);parent.addView(row);
        row.addView(text(label,18,INK,true),new LinearLayout.LayoutParams(0,-2,1));return row;
    }
    private void link(LinearLayout parent,String label,Runnable action){
        TextView view=text(label,14,ACCENT,true);view.setGravity(Gravity.CENTER);view.setMinHeight(dp(48));
        view.setPadding(dp(9),0,dp(9),0);view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x225245ad),null,background(WHITE)));
        parent.addView(view,new LinearLayout.LayoutParams(-2,-2));view.setOnClickListener(v->action.run());
    }
    private void divider(LinearLayout parent){
        View line=new View(this);line.setBackgroundColor(RULE);parent.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
    }
    private Switch feature(LinearLayout parent,String label,String detail,String key,boolean defaultValue){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(10),0,dp(10));parent.addView(row);
        LinearLayout detailBox=new LinearLayout(this);detailBox.setOrientation(1);detailBox.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        detailBox.addView(text(label,15,INK,true));gap(detailBox,2);detailBox.addView(text(detail,12,MUTED,false));
        row.addView(detailBox,new LinearLayout.LayoutParams(0,-2,1));
        Switch item=new Switch(this);item.setMinHeight(dp(48));item.setMinWidth(dp(52));item.setPadding(dp(8),0,0,0);
        item.setContentDescription(label+"。"+detail);item.setChecked(prefs.getBoolean(key,defaultValue));
        item.setOnCheckedChangeListener((v,value)->prefs.edit().putBoolean(key,value).apply());
        row.addView(item,new LinearLayout.LayoutParams(-2,-2));
        detailBox.setOnClickListener(v->item.toggle());return item;
    }
    private TextView counter(LinearLayout row,String label){
        LinearLayout cell=new LinearLayout(this);cell.setOrientation(1);cell.setGravity(Gravity.CENTER);
        TextView value=text("0",25,ACCENT,true);cell.addView(value);cell.addView(text(label,12,MUTED,false));
        row.addView(cell,new LinearLayout.LayoutParams(0,-2,1));return value;
    }
    private Set<String> excludedApps(){return prefs.getStringSet(LocalLog.EXCLUDED,Collections.emptySet());}
    private void updateExclusions(){
        List<String> packages=new ArrayList<>(excludedApps());
        Map<String,String> labels=sortApps(packages);
        excludedSummary.setText(packages.isEmpty()?"添加后，轻跳会跳过这些应用。":"轻跳会跳过以下应用的识别与操作。");
        excludedRows.removeAllViews();
        for(String pkg:packages){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setMinimumHeight(dp(52));
            ImageView icon=new ImageView(this);icon.setImageDrawable(appIcon(pkg));icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            row.addView(icon,new LinearLayout.LayoutParams(dp(32),dp(32)));
            String label=labels.get(pkg);
            TextView title=text(label,15,INK,false);title.setPadding(dp(12),0,dp(8),0);
            row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            link(row,"打开",()->openExcludedApp(pkg,label));
            excludedRows.addView(row);
        }
    }
    private Map<String,String> sortApps(List<String> packages){
        Map<String,String> labels=new HashMap<>();
        for(String pkg:packages)labels.put(pkg,appLabel(pkg));
        Collator order=Collator.getInstance(Locale.SIMPLIFIED_CHINESE);
        packages.sort((a,b)->{int compare=order.compare(labels.get(a),labels.get(b));return compare!=0?compare:a.compareTo(b);});
        return labels;
    }
    private Drawable appIcon(String pkg){
        try{return getPackageManager().getApplicationIcon(pkg);}
        catch(android.content.pm.PackageManager.NameNotFoundException unavailable){return getPackageManager().getDefaultActivityIcon();}
    }
    private void chooseExcludedApps(){
        Set<String> chosen=new HashSet<>(excludedApps()),all=new HashSet<>(chosen);
        Intent launcher=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for(ResolveInfo app:getPackageManager().queryIntentActivities(launcher,0))
            if(app.activityInfo!=null&&!app.activityInfo.packageName.equals(getPackageName()))all.add(app.activityInfo.packageName);
        List<String> packages=new ArrayList<>(all);
        Map<String,String> labels=sortApps(packages);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,packages){
            @Override public View getView(int position,View recycled,ViewGroup parent){
                CheckedTextView row=(CheckedTextView)super.getView(position,recycled,parent);
                String pkg=getItem(position);
                row.setText(labels.get(pkg));
                Drawable icon=appIcon(pkg);icon.setBounds(0,0,dp(32),dp(32));
                row.setCompoundDrawablesRelative(icon,null,null,null);
                row.setCompoundDrawablePadding(dp(12));
                return row;
            }
        };
        AlertDialog picker=new AlertDialog.Builder(this).setTitle("选择不处理的应用").setAdapter(adapter,null)
            .setPositiveButton("保存",(d,w)->prefs.edit().putStringSet(LocalLog.EXCLUDED,chosen).apply()).setNegativeButton("取消",null).show();
        ListView list=picker.getListView();list.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        for(int i=0;i<packages.size();i++)if(chosen.contains(packages.get(i)))list.setItemChecked(i,true);
        list.setOnItemClickListener((parent,row,index,id)->{
            String pkg=packages.get(index);
            if(list.isItemChecked(index))chosen.add(pkg);else chosen.remove(pkg);
        });
    }
    private void openExcludedApp(String pkg,String label){
        Intent launch=getPackageManager().getLaunchIntentForPackage(pkg);
        if(launch==null){toast("找不到这个应用的启动入口");return;}
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setTitle("打开 "+label)
            .setMessage("停用并重开会等待系统确认轻跳已停用，再从应用入口重新打开。未保存的页面输入可能丢失。")
            .setPositiveButton("停用并重开",(d,w)->launchApp(excludedEntry(pkg,true)))
            .setNeutralButton("仅排除并打开",(d,w)->launchApp(launch)).setNegativeButton("取消",null);
        ShortcutManager shortcuts=getSystemService(ShortcutManager.class);
        if(shortcuts!=null){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14),0,dp(14),0);
            link(row,"添加桌面快捷方式",()->confirmPin(shortcuts,pkg,label));
            dialog.setView(row);
        }
        dialog.show();
    }
    private Intent excludedEntry(String pkg,boolean reopen){
        return new Intent(this,ExcludedAppActivity.class).setAction(ExcludedAppActivity.ACTION)
            .putExtra(ExcludedAppActivity.PACKAGE,pkg).putExtra(ExcludedAppActivity.REOPEN,reopen);
    }
    private void confirmPin(ShortcutManager shortcuts,String pkg,String label){
        new AlertDialog.Builder(this).setTitle("添加 "+label+" 的快捷方式")
            .setMessage("快捷入口会先停用轻跳，再打开应用。如果需要停用轻跳，会关闭这个应用的旧页面。支持长按菜单的桌面也可以从轻跳图标启动。")
            .setPositiveButton("添加",(dialog,which)->addShortcut(shortcuts,pkg,label)).setNegativeButton("取消",null).show();
    }
    private ShortcutInfo shortcut(String id,String pkg,String label){
        return new ShortcutInfo.Builder(this,id).setActivity(new ComponentName(this,MainActivity.class))
            .setShortLabel("停用 · "+label).setLongLabel("停用轻跳后打开 "+label)
            .setIcon(Icon.createWithResource(this,R.mipmap.ic_launcher)).setIntent(excludedEntry(pkg,false)).build();
    }
    private void addShortcut(ShortcutManager manager,String pkg,String label){
        boolean longPress=false,requested=false;
        try {
            longPress=manager.addDynamicShortcuts(Collections.singletonList(shortcut("excluded.longpress",pkg,label)));
        } catch(RuntimeException unavailable){
            LocalLog.add(this,"桌面暂未接受长按快捷入口");
        }
        try {
            if(manager.isRequestPinShortcutSupported()){
                Intent callback=new Intent(this,ShortcutPinnedReceiver.class)
                    .setData(new Uri.Builder().scheme("qingtiao").authority("pinned").appendPath(pkg).build());
                PendingIntent result=PendingIntent.getBroadcast(this,0,callback,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
                requested=manager.requestPinShortcut(shortcut("excluded."+pkg,pkg,label),result.getIntentSender());
            }
        } catch(RuntimeException unavailable){
            LocalLog.add(this,"桌面暂未接受固定快捷方式");
        }
        if(requested)toast(longPress?"已请求桌面确认添加；也可长按轻跳图标":"已请求桌面确认添加，请留意系统提示");
        else toast(longPress?"未能固定图标，可以长按轻跳图标打开":"当前桌面未接受快捷方式，可从轻跳内打开");
    }
    private void launchApp(Intent intent){
        try{startActivity(intent);}
        catch(RuntimeException unavailable){toast("无法打开这个应用");}
    }
    private String actions(int limit){
        StringBuilder text=new StringBuilder();int count=0;Map<String,String> labels=new HashMap<>();
        for(String line:prefs.getString(LocalLog.ACTIONS,"").split("\n")){
            String[] parts=line.split("\t",3);if(parts.length!=3)continue;
            if(count++>=limit)break;
            if(text.length()>0)text.append('\n');
            text.append(parts[0]).append("  ").append(labels.computeIfAbsent(parts[1],this::appLabel)).append(" · ").append(parts[2]);
        }
        return text.length()==0?"暂无动作记录":text.toString();
    }
    private void updateActivity(){
        String today=Integer.toString(LocalLog.todayCount(prefs)),total=Integer.toString(prefs.getInt("clicks",0)),returned=Integer.toString(prefs.getInt("redirects",0));
        todayActions.setText(today);totalActions.setText(total);redirectActions.setText(returned);
        recentActions.setText(actions(3));
    }
    private void accessibilitySettings() {
        try {startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
        catch(Exception e){toast("请到系统设置 → 无障碍 → 已下载的应用，开启轻跳");}
    }
    private void backgroundHelp() {
        new AlertDialog.Builder(this).setTitle("保持后台运行")
            .setMessage("平时按界面事件识别，锁屏后暂停。如果清理后台后轻跳失效，可在系统设置里允许后台运行、自启动，或把轻跳加入清理白名单。无障碍权限被系统关掉时，可回到无障碍设置重新启用。")
            .setPositiveButton("应用设置",(d,w)->appDetails()).setNeutralButton("自启动设置",(d,w)->autoStartSettings())
            .setNegativeButton("关闭",null).show();
    }
    private static String reportPackage(String report) {
        int start=report.indexOf("应用：");if(start<0)return "";start+=3;int end=report.indexOf('\n',start);
        String value=report.substring(start,end<0?report.length():end).trim();
        int comma=value.indexOf('，');return comma<0?value:value.substring(0,comma).trim();
    }
    private String matchingActions(String advert) {
        String actions=SkipService.connected?SkipService.actionDiagnostic:prefs.getString("action_trace",SkipService.actionDiagnostic);
        String pkg=reportPackage(advert);
        if(pkg.isEmpty()||!pkg.equals(reportPackage(actions)))return "暂无对应这条广告记录的动作摘要。";
        String sampled=reportWindow(advert,"\n当前窗口ID="),acted=reportWindow(actions,"，窗口=");
        return !sampled.isEmpty()&&!acted.isEmpty()&&!sampled.equals(acted)
            ?"同应用其他窗口的动作（窗口 "+acted+"，本条采样为 "+sampled+"）\n"+actions:actions;
    }
    private static String reportWindow(String report,String marker){
        int start=report.indexOf(marker);
        if(start<0)return "";
        start+=marker.length();int end=start;
        while(end<report.length()&&Character.isDigit(report.charAt(end)))end++;
        return report.substring(start,end);
    }
    private String visualReport(String advert) {
        String vision=SkipService.connected?SkipService.visualDiagnostic:prefs.getString("visual_trace",SkipService.visualDiagnostic);
        String pkg=reportPackage(advert);
        if(pkg.isEmpty())return "";
        if(!pkg.equals(reportPackage(vision)))vision=prefs.getString("visual_trace:"+pkg,"");
        if(vision.isEmpty())return "";
        return "\n\n画面识别：\n"+vision;
    }
    private ReadingHistory recentReadings() {
        return ReadingHistory.decode(SkipService.connected?SkipService.currentReadings():prefs.getString("recent_reads",""));
    }
    private String pageReport(String page) {
        return page+"\n\n动作过程：\n"+matchingActions(page)+visualReport(page)+"\n\n前台观察："+SkipService.watchDiagnostic;
    }
    private String appLabel(String pkg) {
        try{return getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}
        catch(Exception ignored){return pkg;}
    }
    private void advertReport() {
        java.util.List<ReadingHistory.Entry> pages=recentReadings().entries();
        String page=pages.isEmpty()?(SkipService.connected?SkipService.realDiagnostic:prefs.getString("real_trace",SkipService.realDiagnostic)):pages.get(0).report;
        showPage(page);
    }
    private void showPage(String page) {
        String report=pageReport(page);
        new AlertDialog.Builder(this).setTitle("界面识别记录 · 含未识别")
            .setMessage(report).setPositiveButton("关闭",null).setNeutralButton("复制",(d,w)->copy(report))
            .setNegativeButton("选择最近应用",(d,w)->selectReading()).show();
    }
    private void selectReading() {
        java.util.List<ReadingHistory.Entry> pages=recentReadings().entries();
        String[] labels=new String[pages.size()+1];labels[0]="最近识别到的广告";
        for(int i=0;i<pages.size();i++) {
            ReadingHistory.Entry e=pages.get(i);int end=e.report.indexOf('\n');
            labels[i+1]=appLabel(e.pkg)+"\n"+(end<0?"":e.report.substring(0,end));
        }
        new AlertDialog.Builder(this).setTitle("最近8个应用 · 未识别也保留").setItems(labels,(d,index)-> {
            if(index==0)showPage(SkipService.connected?SkipService.adDiagnostic:prefs.getString("ad_trace",SkipService.adDiagnostic));
            else showPage(pages.get(index-1).report);
        }).setPositiveButton("关闭",null).setNeutralButton("复制全部",(d,w)-> {
            StringBuilder all=new StringBuilder();for(ReadingHistory.Entry e:pages)all.append(pageReport(e.report)).append("\n\n");
            copy(all.length()==0?"尚无最近应用记录\n"+SkipService.watchDiagnostic:all.toString());
        }).show();
    }
    private void appDetails() {
        try {startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));}
        catch(Exception e){toast("请在系统设置中找到 JUMP 轻跳的应用详情");}
    }
    private void autoStartSettings() {
        if(!"Xiaomi".equalsIgnoreCase(Build.MANUFACTURER)&&!"Redmi".equalsIgnoreCase(Build.MANUFACTURER)) {
            toast("请在应用设置中允许自启动或后台运行");appDetails();return;
        }
        Intent intent=new Intent().setComponent(new ComponentName("com.miui.securitycenter","com.miui.permcenter.autostart.AutoStartManagementActivity"));
        try {startActivity(intent);}
        catch(Exception e){toast("请在应用设置中允许自启动或后台运行");appDetails();}
    }
    private void copy(String value){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("JUMP 诊断",value));toast("已复制");}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
}
