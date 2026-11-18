package dev.local.qingtiao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.WindowManager;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class SkipService extends AccessibilityService implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static volatile boolean connected;
    public static volatile String serviceVersion="尚未连接", sessionStarted="尚未连接";
    private static volatile SkipService instance;
    public static volatile int eventCount;
    public static volatile String realDiagnostic="尚未记录到真实应用的界面读取";
    public static volatile String adDiagnostic="尚未记录到广告或跳过按钮";
    public static volatile String connectionDetails="尚无服务能力信息";
    public static volatile String actionDiagnostic="尚无广告点击动作";
    public static volatile String visualDiagnostic="尚未进行画面识别";
    public static volatile String recentDiagnostics="",watchDiagnostic="前台观察尚未启动";
    public static volatile long lastScanStarted,lastScanFinished;
    private final HandlerThread scanner=startScanner();
    private final Handler handler=new Handler(scanner.getLooper());
    private static HandlerThread startScanner(){HandlerThread t=new HandlerThread("jump-scanner");t.start();return t;}
    private final ClickGate gate=new ClickGate();
    private final EventInbox events=new EventInbox();
    private final VisualReview visualReview=new VisualReview();
    private final VisualFollowup visualFollowup=new VisualFollowup();
    private final NegativeFrameCache negativeFrames=new NegativeFrameCache();
    private final TimedClose timedClose=new TimedClose();
    private final StartupProbe startupProbe=new StartupProbe();
    private final PanelProbeBudget panelProbes=new PanelProbeBudget();
    private final RenderedPanelProbes renderProbes=new RenderedPanelProbes();
    private final DelayedAdWatch delayedWatch=new DelayedAdWatch();
    private final SplashActions splashActions=new SplashActions();
    private final WmTouchReview wmTouchReview=new WmTouchReview();
    private final CanvasSkipVisual.Proof canvasProof=new CanvasSkipVisual.Proof();
    private final CanvasSkipVisual.Proof biliPairProof=new CanvasSkipVisual.Proof();
    private final CanvasSkipVisual.Proof renderedPairProof=new CanvasSkipVisual.Proof();
    private final AdFocus adFocus=new AdFocus();
    private final TreeReadGate treeReadGate=new TreeReadGate();
    private final WindowTransitionWatch windowTransition=new WindowTransitionWatch();
    private volatile int metadataConfirmedWindow=-1;
    private final OopzSurfaceVisual.Proof surfaceProof=new OopzSurfaceVisual.Proof();
    private final WmSplashVisual.PairProof wmPairProof=new WmSplashVisual.PairProof();
    private final OopzOrigin oopzOrigin=new OopzOrigin();
    private volatile String rootWindowDiagnostic="尚未核验窗口列表";
    private volatile String visualWindowMismatch="";
    private ReadingHistory readings=new ReadingHistory();
    private final ThreadPoolExecutor visionWorker=new ThreadPoolExecutor(0,1,10,TimeUnit.SECONDS,new LinkedBlockingQueue<>(),r->new Thread(r,"jump-vision"));
    private final Set<String> excluded=new HashSet<>();
    private final Set<String> shakeDestinations=new HashSet<>();
    private final AdShake shake=new AdShake();
    private SharedPreferences prefs;
    private String foreground="", lastDiagnostic="";
    private long enteredAt, lastScan=-1000, burstUntil, lastDiagnosticAt, adActiveUntil;
    private long lastRealEventAt,lastTraceSaved;
    private long lastRealEventCreatedAt,rootReadMs,walkMs,matchMs,scanStartedAt;
    private String lastRealEventPkg="",traceBase="";
    private boolean traceInteresting;
    private boolean douyuAdSeen;
    private volatile int generation,visualSerial;
    private int gestureSerial;
    private int captureWindow=-1;
    private boolean capturingVisual,picaSequence,pixelPairPending;
    private int visualKind;
    private String visualPackage="";
    private boolean visualSplash,visualInteresting;
    private String visualHistoryPackage="",lastVisualStep="";
    private final java.util.ArrayDeque<String> visualSteps=new java.util.ArrayDeque<>();
    private boolean sparseProbeWait,reminderIdle;
    private volatile boolean acceptingEvents,frameAccess;
    private volatile long treeReads,frameRequests;
    private static final int EVENT_TYPES=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED|AccessibilityEvent.TYPE_WINDOWS_CHANGED|AccessibilityEvent.TYPE_VIEW_SCROLLED;
    private boolean stopping;
    private String actionPackage="";
    private boolean busy, queued, receiverRegistered;
    private long queuedFor;
    private long queuedAt,queueWaitMs,queueLateMs,eventRescanAt;
    private boolean queuedStartup,scanFromStartup,eventRescan,eventRescanWindow,pixelOnly;
    private boolean forceTreeRead;
    private boolean weakStartupEvidenceSeen;
    private interface FrameReader {VisualFrame.Data read(ScreenshotResult screenshot,boolean fullDiscovery);}
    private static final int FULL=0,CANVAS_PAIR=1,BILI_LAUNCH_PAIR=2,BILI_PANEL_PAIR=3,JM_CLOCK=4;
    /** Immutable detector inputs only: no pixels, nodes or coordinates for an action. */
    private static final class CapturePlan {
        final String pkg;final boolean splash,strongSurfaceDiscovery;final int generation,window,display;final Point size;final FrameReader reader;
        CapturePlan(String pkg,boolean splash,int generation,int window,int display,Point size,boolean strongSurfaceDiscovery,FrameReader reader){
            this.pkg=pkg;this.splash=splash;this.generation=generation;this.window=window;this.display=display;this.size=new Point(size.x,size.y);this.strongSurfaceDiscovery=strongSurfaceDiscovery;this.reader=reader;
        }
    }
    private CapturePlan lightProbe;
    private int lightLane;
    private long lightProbeAt;
    private static final class Pending {
        final String pkg,key,traceAtAction;final ClickReview review;final int windowId;final long createdAt;
        final AccessibilityNodeInfo originalTarget;final boolean reminder,nativeSplash,nativePause;
        boolean notedUnstable,probed;
        Pending(String pkg,String key,String traceAtAction,int windowId,long now,AccessibilityNodeInfo target,boolean reminder,boolean nativeSplash,boolean nativePause){
            this.pkg=pkg;this.key=key;this.traceAtAction=traceAtAction;this.windowId=windowId;createdAt=now;review=new ClickReview(now,false);
            originalTarget=AccessibilityNodeInfo.obtain(target);this.reminder=reminder;this.nativeSplash=nativeSplash;this.nativePause=nativePause;
        }
        void close(){originalTarget.recycle();}
    }
    private Pending pending;
    private final Runnable scanTask=()-> {
        long now=SystemClock.uptimeMillis();queueWaitMs=now-queuedAt;queueLateMs=Math.max(0,now-queuedFor);
        scanFromStartup=queuedStartup;queued=queuedStartup=false;eventRescan=false;eventRescanWindow=false;scan();
    };
    private final Runnable watchTask=()->watchForeground();
    private volatile SensorManager shakeSensors;
    private final Runnable shakeTimeout=()->stopShakeSensor();
    private final SensorEventListener shakeMotion=new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent event) {
            shake.motion(event.values[0],event.values[1],event.values[2],SystemClock.uptimeMillis());
        }
        @Override public void onAccuracyChanged(Sensor sensor,int accuracy) {}
    };
    private final Runnable eventTask=()-> {
        EventInbox.Event e=events.take();if(e!=null)handleEvent(e.pkg,e.types,e.occurredAt,e.arrivedAt,e.cue);
    };
    private final BroadcastReceiver screenReceiver=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent intent) {
            handler.post(()->resetEnvironment("屏幕状态变化："+intent.getAction()));
        }
    };
    @Override protected void onServiceConnected() {
        prefs=getSharedPreferences(LocalLog.PREFS,0);prefs.registerOnSharedPreferenceChangeListener(this);
        connected=true;instance=this;refreshExclusions();
        ServiceRecovery.connected(this);
        serviceVersion="0.5.35";sessionStarted=new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.CHINA).format(new java.util.Date());
        AccessibilityServiceInfo info=getServiceInfo();
        frameAccess=canReadFrames(info);
        if(info!=null) {
            info.flags|=AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                |AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS|AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
            connectionDetails="能力="+info.getCapabilities()+"，标志="+info.flags+"，事件类型="+info.eventTypes;
        }
        realDiagnostic=prefs.getString("real_trace","尚未记录到真实应用的界面读取");
        adDiagnostic=prefs.getString("ad_trace","尚未记录到广告或跳过按钮");
        actionDiagnostic=prefs.getString("action_trace","尚无广告点击动作");
        visualDiagnostic=prefs.getString("visual_trace","尚未进行画面识别");
        readings=ReadingHistory.decode(prefs.getString("recent_reads",""));recentDiagnostics=readings.encode();
        handler.post(()-> {refreshShakeDestinations();resetEnvironment("系统连接无障碍服务，重新核验前台");});
        if(!receiverRegistered) {
            IntentFilter f=new IntentFilter(Intent.ACTION_SCREEN_OFF);f.addAction(Intent.ACTION_SCREEN_ON);f.addAction(Intent.ACTION_USER_PRESENT);
            if(Build.VERSION.SDK_INT>=33) registerReceiver(screenReceiver,f,Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(screenReceiver,f);
            receiverRegistered=true;
        }
        prefs.edit().putLong("service_ui",SystemClock.elapsedRealtimeNanos()).apply();
        LocalLog.add(this,"JUMP 0.5.35 服务已连接 · "+connectionDetails+" · Android "+Build.VERSION.RELEASE+" · "+Build.MANUFACTURER+" "+Build.MODEL);
    }
    /** The home screen invokes this only after the user confirms disabling the service. */
    static boolean disableAccessibilityFromHome() {
        SkipService service=instance;
        if(service==null||!connected)return false;
        try {service.disableSelf();return true;}
        catch(RuntimeException error){return false;}
    }
    public static boolean hasFrameAccess(){SkipService service=instance;return service!=null&&service.frameAccess;}
    static String currentWorkload(){
        SkipService service=instance;
        return service==null?"服务未连接":"完整控件扫描="+service.treeReads+"，截图请求="+service.frameRequests;
    }
    private static boolean canReadFrames(AccessibilityServiceInfo info) {
        return Build.VERSION.SDK_INT>=30&&info!=null&&(info.getCapabilities()&AccessibilityServiceInfo.CAPABILITY_CAN_TAKE_SCREENSHOT)!=0;
    }
    public static String currentReadings() {
        SkipService service=instance;
        return service==null?recentDiagnostics:service.readings.encode();
    }
    private boolean picaNoticeWatch(String pkg,long now) {
        return PicaVisual.trusted(pkg)&&(splashActions.noticeBudget()||!splashActions.finished())&&now>=enteredAt&&now-enteredAt<45000;
    }
    private boolean visualStartup(String pkg,long now) {
        return startupProbe.active(now)||DelayedAdWatch.active(pkg,splashActions.finished())||picaNoticeWatch(pkg,now);
    }
    private boolean visualStartup(long now){return visualStartup(foreground,now);}
    private void updateEventSubscription() {
        boolean enabled=!stopping&&prefs!=null&&prefs.getBoolean("running",true)&&interactive();
        acceptingEvents=enabled;
        AccessibilityServiceInfo info=getServiceInfo();
        frameAccess=canReadFrames(info);
        if(info!=null) {
            int types=enabled?EVENT_TYPES:0;
            if(enabled&&!shakeDestinations.isEmpty())types|=AccessibilityEvent.TYPE_VIEW_CLICKED;
            if(info.eventTypes!=types) {info.eventTypes=types;setServiceInfo(info);}
            connectionDetails="能力="+info.getCapabilities()+"，标志="+info.flags+"，事件类型="+info.eventTypes;
        }
    }
    private void resetEnvironment(String reason) {
        cancel(reason);foreground="";sparseProbeWait=false;
        updateEventSubscription();scheduleWatch();if(acceptingEvents)queue(30);
    }
    private boolean confirmedWork(long now) {
        return pending!=null||capturingVisual||visualFollowup.active(now)||pixelPairPending||wmTouchReview.active()
            ||timedClose.active()||now<adActiveUntil||adFocus.rapid(now);
    }
    private void watchForeground() {
        try {
            if(stopping||prefs==null)return;
            if(!prefs.getBoolean("running",true)){watchDiagnostic="总开关已关闭";return;}
            if(!interactive()){watchDiagnostic="屏幕关闭或已锁定，暂停观察";return;}
            if(busy||queued)return;
            if(watchActive(SystemClock.uptimeMillis())) {queue(0);return;}
            AccessibilityNodeInfo root=readRoot(false);
            if(root==null) {
                watchDiagnostic="前台窗口内容为空，未能确认画面归属；未提交点击";
                long now=SystemClock.uptimeMillis();
                if(!lastRealEventPkg.isEmpty()&&now-lastRealEventAt<3000) {
                    traceBase="应用："+lastRealEventPkg;traceInteresting=false;trace(lastRealEventPkg,watchDiagnostic,true);
                }
                return;
            }
            try {
                String pkg=str(root.getPackageName());
                if(deferTransition(root,SystemClock.uptimeMillis()))return;
                watchDiagnostic=(watchActive(SystemClock.uptimeMillis())?"启动/广告观察":"极省电观察（事件优先，30秒仅核验归属）")+"，窗口="+root.getWindowId()+"，应用="+pkg;
                if(MatchPolicy.diagnosticHost(pkg))watchDiagnostic="ChatGPT 对话页已排除广告检测，仅观察前台归属；窗口="+root.getWindowId();
                if(excludedPackage(pkg)||pkg.equals(getPackageName()))observeForeground(pkg,root.getWindowId(),SystemClock.uptimeMillis());
                else if(!pkg.equals(foreground)||visualEnabled(!"tv.danmaku.bili".equals(pkg))&&watchActive(SystemClock.uptimeMillis()))queue(0);
            } finally{root.recycle();}
        } catch(RuntimeException error){watchDiagnostic="前台观察异常："+error.getClass().getSimpleName();}
        finally{scheduleWatch();}
    }
    private boolean deferTransition(AccessibilityNodeInfo root,long now) {
        if(!windowTransition.defer(pending!=null||visualFollowup.active(now)||wmTouchReview.active(),root.getWindowId()>=0&&metadataConfirmedWindow==root.getWindowId(),foreground,str(root.getPackageName()),now))return false;
        resetPixelProofs();watchDiagnostic="前台归属变化，暂停触摸并短暂重读；等待稳定窗口，不复用旧目标";
        if(!visualPackage.isEmpty())visualStatus(watchDiagnostic);queue(350);return true;
    }
    private boolean urgentWork(long now) {
        return confirmedWork(now);
    }
    private boolean watchActive(long now) {
        return !excludedPackage(foreground)&&!foreground.equals(getPackageName())
            &&(pending!=null||visualFollowup.active(now)||wmTouchReview.active()||timedClose.active()
                ||!reminderIdle&&visualEnabled(!"tv.danmaku.bili".equals(foreground))&&(!splashActions.finished()&&startupProbe.active(now)||picaNoticeWatch(foreground,now)));
    }
    private void scheduleWatch() {
        if(stopping||prefs==null)return;
        if(eventRescan&&!busy) {
            long due=eventRescanAt;boolean window=eventRescanWindow;
            eventRescan=false;eventRescanWindow=false;
            queueWindow(Math.max(30,due-SystemClock.uptimeMillis()),window);
        }
        long now=SystemClock.uptimeMillis();boolean active=watchActive(now);
        long delay=ScanPacing.watchDelay(prefs.getBoolean("running",true),interactive(),active);
        if(delay>=0&&active&&sparseProbeWait&&!confirmedWork(now)){
            long due=startupProbe.scanAt(now,visualReview.requestDelay(now,startupProbe.surfaceRetryPending()));
            delay=ScanPacing.startupWatch(due<0?-1:due-now,Math.max(0,enteredAt+StartupProbe.WINDOW-now));
        }
        handler.removeCallbacks(watchTask);
        if(delay>=0&&active&&queued&&queuedStartup)return; // the same scan will re-arm the watch
        if(delay>=0)handler.postDelayed(watchTask,delay);
        else watchDiagnostic="省电休眠：总开关关闭、熄屏或锁屏，不再定时读取窗口或画面";
    }
    private void refreshExclusions() {
        excluded.clear();
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for(ResolveInfo info:getPackageManager().queryIntentActivities(home,0))
            if(info.activityInfo!=null) excluded.add(info.activityInfo.packageName);
        InputMethodManager manager=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        for(InputMethodInfo info:manager.getEnabledInputMethodList()) excluded.add(info.getPackageName());
    }
    private boolean excludedPackage(String pkg) {
        return pkg.isEmpty()||MatchPolicy.diagnosticHost(pkg)||pkg.equals("android")||pkg.startsWith("com.android.")
            ||pkg.equals("com.google.android.permissioncontroller")||pkg.equals("com.google.android.packageinstaller")
            ||pkg.endsWith(".systemui")||pkg.endsWith(".settings")||pkg.contains(".permissioncontroller")
            ||pkg.contains(".packageinstaller")||pkg.equals("com.miui.securitycenter")
            ||pkg.equals("com.huawei.systemmanager")||pkg.equals("com.coloros.safecenter")||excluded.contains(pkg)||userExcluded(pkg);
    }
    private boolean userExcluded(String pkg){
        return prefs!=null&&prefs.getStringSet(LocalLog.EXCLUDED,java.util.Collections.emptySet()).contains(pkg);
    }
    private void refreshShakeDestinations(){
        shakeDestinations.clear();
        if(!prefs.getBoolean(LocalLog.SHAKE,false))return;
        Intent browser=new Intent(Intent.ACTION_VIEW,Uri.parse("https://example.invalid/")).addCategory(Intent.CATEGORY_BROWSABLE);
        Intent store=new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id="+getPackageName()));
        int flags=android.content.pm.PackageManager.MATCH_DEFAULT_ONLY|android.content.pm.PackageManager.GET_RESOLVED_FILTER;
        for(ResolveInfo info:getPackageManager().queryIntentActivities(browser,flags))
            if(info.activityInfo!=null&&info.filter!=null&&info.filter.countDataAuthorities()==0)shakeDestinations.add(info.activityInfo.packageName);
        for(ResolveInfo info:getPackageManager().queryIntentActivities(store,flags))
            if(info.activityInfo!=null)shakeDestinations.add(info.activityInfo.packageName);
        shakeDestinations.remove(getPackageName());
    }
    private void armShake(String pkg,int window,long now){
        if(shakeDestinations.contains(pkg)||!shake.begin(pkg,window,now))return;
        stopShakeSensor();
        SensorManager manager=(SensorManager)getSystemService(SENSOR_SERVICE);
        Sensor accelerometer=manager==null?null:manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accelerometer!=null&&manager.registerListener(shakeMotion,accelerometer,SensorManager.SENSOR_DELAY_UI,handler)){
            shakeSensors=manager;handler.postDelayed(shakeTimeout,AdShake.OBSERVATION_MS);
        }
    }
    private void stopShakeSensor(){
        handler.removeCallbacks(shakeTimeout);
        if(shakeSensors!=null){shakeSensors.unregisterListener(shakeMotion);shakeSensors=null;}
    }
    private boolean returnFromShake(String pkg,AccessibilityNodeInfo root,long now){
        if(shakeDestinations.isEmpty()||!prefs.getBoolean(LocalLog.SHAKE,false))return false;
        int window=root.getWindowId();
        boolean landing=shakeSensors!=null&&metadataConfirmedWindow==window&&shakeDestinations.contains(pkg)
            &&!userExcluded(pkg)&&displayId(root)==Display.DEFAULT_DISPLAY;
        String source=shake.transition(pkg,window,landing,now);
        if(!shake.observing(now)&&shakeSensors!=null)stopShakeSensor();
        if(source.isEmpty()||userExcluded(source)||!stillForeground(pkg,window)||metadataConfirmedWindow!=window)return false;
        if(!performGlobalAction(GLOBAL_ACTION_BACK))return false;
        LocalLog.recordAction(this,source,"摇动后跳转 · 已发出返回",true);
        LocalLog.add(this,source+"：确认摇动后切换至 "+pkg+"，系统接受一次返回动作");
        return true;
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences p,String key) {
        if(LocalLog.SHAKE.equals(key)){
            handler.post(()-> {refreshShakeDestinations();resetEnvironment("摇一摇设置变化");});
        } else if(LocalLog.EXCLUDED.equals(key)||key.equals("running")||key.equals("browsing")||key.equals("enhanced")||key.equals("gesture_retry")||key.equals("bili_visual")||key.equals("splash_visual")||key.equals("mode_reminder"))
            handler.post(()->resetEnvironment("功能设置变化"));
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(!acceptingEvents)return;
        eventCount++;
        final String eventPackage=event.getPackageName()==null?null:event.getPackageName().toString();
        final int type=event.getEventType();
        if(eventPackage!=null&&userExcluded(eventPackage)){
            int windowEvents=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOWS_CHANGED;
            if((type&windowEvents)!=0&&events.offer(eventPackage,type,event.getEventTime(),SystemClock.uptimeMillis(),false))
                handler.post(eventTask);
            return;
        }
        if(type==AccessibilityEvent.TYPE_VIEW_CLICKED){
            if(shakeSensors==null)return;
            final long at=event.getEventTime();
            handler.post(()-> {shake.clicked(eventPackage,at);if(!shake.observing(SystemClock.uptimeMillis()))stopShakeSensor();});
            return;
        }
        if((type&AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)!=0&&WmSplashVisual.applies(eventPackage)) {
            final long arrived=SystemClock.uptimeMillis();handler.post(()->oopzOrigin.windowEvent(eventPackage,arrived));
        }
        boolean jm=DelayedAdWatch.jm(eventPackage),bili="tv.danmaku.bili".equals(eventPackage);
        boolean cue=eventCue(event.getContentDescription(),jm,bili);int texts=0;
        for(CharSequence value:event.getText()){if(++texts>8)break;cue|=eventCue(value,jm,bili);}
        if(events.offer(eventPackage,type,event.getEventTime(),SystemClock.uptimeMillis(),cue))handler.post(eventTask);
    }
    private static boolean eventCue(CharSequence value,boolean jm,boolean bili){
        return TreeReadGate.cue(value)||jm&&value!=null&&value.length()<=120&&JmVisual.caption(value.toString())
            ||bili&&value!=null&&value.length()<=24&&("前往观看".contentEquals(value)||"关闭广告".contentEquals(value)||"关闭暂停广告".contentEquals(value));
    }
    private void handleEvent(String eventPackage,int type,long occurredAt,long arrivedAt,boolean cue) {
        if(prefs==null||!prefs.getBoolean("running",true)||!interactive())return;
        if((type&(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOWS_CHANGED))!=0)forceTreeRead=true;
        if((type&AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)!=0)oopzOrigin.windowEvent(eventPackage,arrivedAt);
        if(eventPackage==null) {
            // TYPE_WINDOWS_CHANGED often has no package; the scan validates the actual foreground root.
            burstUntil=Math.max(burstUntil,SystemClock.uptimeMillis()+1500);queueWindow(120,true);return;
        }
        String pkg=eventPackage;
        if(excludedPackage(pkg)||pkg.equals(getPackageName())) {
            // Observe actual foreground transitions without traversing excluded windows.
            if((type&(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOWS_CHANGED))!=0) {
                burstUntil=Math.max(burstUntil,SystemClock.uptimeMillis()+300);queueWindow(120,true);
            }
            return;
        }
        if(!pkg.equals(getPackageName())) {lastRealEventPkg=pkg;lastRealEventAt=arrivedAt;lastRealEventCreatedAt=occurredAt;}
        if(cue)forceTreeRead=true;
        long now=SystemClock.uptimeMillis();
        boolean window=(type&(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED|AccessibilityEvent.TYPE_WINDOWS_CHANGED))!=0;
        boolean urgent=cue||urgentWork(now)||ScanPacing.transition(pkg,foreground);
        if(window||urgent)burstUntil=Math.max(burstUntil,now+1500);
        // Merge ordinary content/scroll floods without delaying real window changes or confirmed ads.
        queueWindow(ScanPacing.eventDelay(window,urgent,(type&AccessibilityEvent.TYPE_VIEW_SCROLLED)!=0,ScanPacing.opening(enteredAt,now)),window||cue||ScanPacing.transition(pkg,foreground));
    }
    private void queueWindow(long delay,boolean window) {
        if(busy) {
            long due=SystemClock.uptimeMillis()+delay;
            if(!eventRescan||due<eventRescanAt)eventRescanAt=due;
            eventRescan=true;eventRescanWindow|=window;return;
        }
        // Content floods need no new full tree while the next permitted frame is still pending.
        // Real window transitions and native click reviews always bypass this small wait.
        long now=SystemClock.uptimeMillis();
        if(!window&&pending==null){
            if(lightProbe!=null)delay=Math.max(delay,lightProbeAt-now);
            else if(timedClose.active()&&DelayedAdWatch.jm(foreground))delay=Math.max(delay,timedClose.nextProbeAt(now)-now);
            else if(pixelOnly)delay=Math.max(delay,Math.min(450,pixelDelay(now)));
        }
        long due=now+Math.max(delay,ScanPacing.minimum(urgentWork(now)||now<burstUntil,ScanPacing.opening(enteredAt,now))-(now-lastScan));
        if(!busy&&queued&&due<queuedFor){handler.removeCallbacks(scanTask);queued=false;}
        queue(delay);
    }
    private void queue(long delay) {
        if(!busy&&!queued&&prefs!=null&&prefs.getBoolean("running",true)) {
            queued=true;queuedStartup=false;long now=SystemClock.uptimeMillis();queuedAt=now;
            long wait=Math.max(delay,ScanPacing.minimum(urgentWork(now)||now<burstUntil,ScanPacing.opening(enteredAt,now))-(now-lastScan));queuedFor=now+wait;handler.postDelayed(scanTask,wait);
        }
    }
    private void queueStartupSlot(){
        long now=SystemClock.uptimeMillis(),due=startupProbe.scanAt(now,visualReview.requestDelay(now,startupProbe.surfaceRetryPending()));
        if(frameAccess&&due>=0)queueKnownRead(now,due,true);
    }
    private void queueKnownRead(long now,long due,boolean startup){
        if(busy||!acceptingEvents||prefs==null||!prefs.getBoolean("running",true)||!interactive())return;
        if(due<0||queued&&queuedFor<=due)return;
        if(queued)handler.removeCallbacks(scanTask);
        // Use the same scanner timer. The scan revalidates the current owner and evidence.
        queued=true;queuedStartup=startup;queuedAt=now;queuedFor=due;handler.postAtTime(scanTask,due);
    }
    private void cancel(){cancel("设置变更、系统中断或服务关闭");}
    private void cancel(String reason) {
        stopShakeSensor();shake.clear();
        endVisualFollowup("识别停止或设置变化");
        if(wmTouchReview.active())actionStep(visualPackage,"WM_REVIEW_CANCELLED："+reason+"；已尝试="+wmTouchReview.attempts()+"；不代表广告已关闭");
        if(timedClose.active())visualStatus("观察结束：识别取消；"+timedClose.describe(SystemClock.uptimeMillis()));
        adFocus.reset();panelProbes.reset();renderProbes.reset();treeReadGate.reset();forceTreeRead=false;weakStartupEvidenceSeen=false;eventRescan=false;eventRescanWindow=false;pixelOnly=false;
        windowTransition.reset();flushDiagnostics();negativeFrames.clear();sparseProbeWait=false;reminderIdle=false;adActiveUntil=0;generation++;busy=false;queued=queuedStartup=false;finishPending();gate.observe(java.util.Collections.emptySet());burstUntil=0;resetPixelProofs();
        capturingVisual=false;visualReview.clearScene();timedClose.clear();splashActions.reset();wmTouchReview.reset();delayedWatch.reset();picaSequence=false;visualKind=SplashActions.NONE;visualPackage="";startupProbe.reset(SystemClock.uptimeMillis());
        oopzOrigin.reset();handler.removeCallbacksAndMessages(null);events.clear();scheduleWatch();
    }
    private void finishPending(){if(pending!=null){pending.close();pending=null;}}
    private static String str(CharSequence value) {return value==null?"":value.toString();}
    private boolean interactive() {
        PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
        KeyguardManager lock=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        return power.isInteractive()&&!lock.isKeyguardLocked();
    }
    private void diagnostic(String pkg,String reason) {
        long now=SystemClock.uptimeMillis();String key=pkg+reason;
        if(!key.equals(lastDiagnostic)||now-lastDiagnosticAt>20000) {
            lastDiagnostic=key;lastDiagnosticAt=now;LocalLog.add(this,pkg+"："+reason);
        }
    }
    private void scan() {
        lastScanStarted=SystemClock.uptimeMillis();
        try {if(!runLightProbe())scanInternal();}
        catch(RuntimeException error) {
            busy=false;finishPending();
            String message="读取流程异常："+error.getClass().getSimpleName()+" "+str(error.getMessage());
            if(message.length()>240)message=message.substring(0,240);
            if(!lastRealEventPkg.isEmpty()) {
                traceBase="应用："+lastRealEventPkg;traceInteresting=false;trace(lastRealEventPkg,message,true);diagnostic(lastRealEventPkg,message);
            }
        } finally {lastScanFinished=SystemClock.uptimeMillis();scheduleWatch();}
    }
    private boolean runLightProbe(){
        if(lightProbe==null)return false;
        CapturePlan plan=lightProbe;int lane=lightLane;long now=SystemClock.uptimeMillis();
        if(forceTreeRead||busy||pending!=null||plan.generation!=generation||!plan.pkg.equals(foreground)||!visualEnabled(plan.pkg,plan.splash)){
            lightProbe=null;return false;
        }
        long delay=Math.max(lightProbeAt-now,pixelDelay(now));
        if(delay>0){queue(delay);return true;}
        lightProbe=null;
        if(!visualWindow(plan.pkg,plan.window,plan.display,plan.size,false,false,false)){
            if(lane==JM_CLOCK)timedClose.clear("轻量取帧时窗口或显示区域变化");
            resetPixelProofs();return false;
        }
        lastScan=now;return captureVisual(plan,lane,now);
    }
    private void scheduleLightProbe(CapturePlan plan,int lane,long due,long now){
        // No extra capture budget: use the same scanner queue and the same capture rate limits.
        lightProbe=plan;lightLane=lane;lightProbeAt=due;
        queueWindow(Math.max(0,due-now),false);
    }
    private void observeForeground(String pkg,int windowId,long now) {
        oopzOrigin.foreground(pkg,now,wmTouchReview.attempts());
        if(!pkg.equals(foreground)) {
            adFocus.reset();panelProbes.reset();renderProbes.reset();treeReadGate.reset();weakStartupEvidenceSeen=false;pixelOnly=false;
            windowTransition.reset();
            if(wmTouchReview.active())actionStep(visualPackage,"WM_REVIEW_CANCELLED："+(metadataConfirmedWindow==windowId?"交互元数据确认前台":"未确认的新归属持续超过短暂观察窗口，停止等待；读取由")+foreground+"变为"+pkg+"，当前窗口="+windowId+"；已尝试="+wmTouchReview.attempts()+"；停止复查，不代表广告已关闭");
            endVisualFollowup("真实前台已切换");
            flushDiagnostics();negativeFrames.clear();sparseProbeWait=false;reminderIdle=false;adActiveUntil=0;
            if(timedClose.active())visualStatus("观察结束：前台已切换；"+timedClose.describe(now));
            if(pending!=null){actionStep(pending.pkg,"前台应用变化，停止后续点击");diagnostic(pending.pkg,"前台应用变化，取消后续补点");finishPending();}
            generation++;busy=false;gate.observe(java.util.Collections.emptySet());foreground=pkg;enteredAt=now;resetPixelProofs();
            capturingVisual=false;visualReview.clearScene();timedClose.clear();splashActions.reset();wmTouchReview.reset();delayedWatch.reset();picaSequence=false;visualKind=SplashActions.NONE;visualPackage="";startupProbe.reset(now);
            douyuAdSeen=false;
            if("air.tv.douyu.android".equals(pkg)) {
                actionPackage=pkg;actionDiagnostic="JUMP 0.5.35\n应用："+pkg+"，窗口="+windowId+"\n斗鱼启动采样，尚未提交本应用的点击动作";
                prefs.edit().putString("action_trace",actionDiagnostic).apply();
            }
        }
    }
    private void scanInternal() {
        if(busy||prefs==null||!prefs.getBoolean("running",true)) return;
        if(!interactive())return;
        long now=SystemClock.uptimeMillis();lastScan=now;scanStartedAt=now;rootReadMs=walkMs=matchMs=0;sparseProbeWait=false;
        if(!visualFollowup.active(now))endVisualFollowup("有限复查已到期");
        if(wmTouchReview.expire(now))actionStep(visualPackage,"WM_REVIEW_EXPIRED：有限观察时限已到，停止补点；实际关闭结果未核实");
        boolean oopzRead=WmSplashVisual.applies(foreground)||(WmSplashVisual.applies(lastRealEventPkg)&&now-lastRealEventAt<3000);
        AccessibilityNodeInfo root=readRoot(pending==null&&!oopzRead&&!treeReadGate.recent(now));
        rootReadMs=SystemClock.uptimeMillis()-now;
        if(root==null) {
            wmTouchReview.pause(SystemClock.uptimeMillis());
            if(!lastRealEventPkg.isEmpty()&&now-lastRealEventAt<2500) {
                traceBase="应用："+lastRealEventPkg;traceInteresting=false;trace(lastRealEventPkg,"收到该应用事件，但窗口内容读取为空",false);
            }
            if(now<burstUntil) queue(250);
            return;
        }
        String pkg=str(root.getPackageName());
        if(deferTransition(root,now)){root.recycle();return;}
        observeForeground(pkg,root.getWindowId(),now);
        if(returnFromShake(pkg,root,now)){root.recycle();queue(120);return;}
        if(!visualFollowup.current(pkg,root.getWindowId(),now))endVisualFollowup("原窗口已变化");
        watchDiagnostic=(watchActive(now)?"启动/广告按需观察":"极省电观察（事件优先，30秒仅核验归属）")+"，窗口="+root.getWindowId()+"，应用="+pkg;
        startupProbe.window(root.getWindowId(),now);
        if(excludedPackage(pkg)||pkg.equals(getPackageName())){root.recycle();return;}
        List<AccessibilityNodeInfo> refs=new ArrayList<>();
        List<AdMatcher.Node> snapshot=new ArrayList<>();
        traceBase="应用："+pkg;traceInteresting=false;
        try {
            if(pending!=null&&probeOriginalTarget(pkg,root.getWindowId()))return;
            Point size=screenSize(root);
            if(size.x<=0||size.y<=0) return;
            long at=SystemClock.uptimeMillis(),planned=startupProbe.delay(at);
            boolean launchProbing=!splashActions.finished()&&visualEnabled(!"tv.danmaku.bili".equals(pkg));
            long panelRead="tv.danmaku.bili".equals(pkg)?panelProbes.treeDelay(root.getWindowId(),at):-1;
            boolean panelDue="tv.danmaku.bili".equals(pkg)&&visualEnabled(false)
                &&panelProbes.delay(root.getWindowId(),at)==0&&pixelDelay(at)==0;
            long treeDelay=treeReadGate.delay(pkg,root.getWindowId(),displayId(root),size.x,size.y,at,forceTreeRead||confirmedWork(at)||launchProbing&&planned==0||panelRead==0||panelDue,ScanPacing.opening(enteredAt,at));
            if(treeDelay>0){
                watchDiagnostic="同窗口控件读取已合并，剩余="+treeDelay+"ms；仍核验真实前台；窗口="+root.getWindowId()+"，应用="+pkg;
                queue(treeDelay);
                if(launchProbing&&planned>0)queueStartupSlot();
                if(panelRead>0)queueKnownRead(at,at+panelRead,false);
                return;
            }
            forceTreeRead=false;
            treeReads++;
            // Virtual full-screen parent distinguishes a real dialog window from a whole application page.
            refs.add(null);snapshot.add(new AdMatcher.Node(-1,0,0,size.x,size.y,"",""));
            refs.add(root);snapshot.add(capture(root,0,pkg));
            long walkStarted=SystemClock.uptimeMillis();
            for(int i=1;i<refs.size()&&refs.size()<1000;i++) {
                AccessibilityNodeInfo node=refs.get(i);
                int children=node.getChildCount();
                for(int j=0;j<children&&refs.size()<1000;j++) {
                    AccessibilityNodeInfo child=readChild(node,j,pkg);
                    if(child!=null) {refs.add(child);snapshot.add(capture(child,i,pkg));}
                }
            }
            walkMs=SystemClock.uptimeMillis()-walkStarted;
            treeReadGate.readFinished(pkg,root.getWindowId(),displayId(root),size.x,size.y,SystemClock.uptimeMillis());
            if(refs.size()>=1000) {traceBase="应用："+pkg;trace(pkg,"页面控件数达到上限，本次不点击",false);diagnostic(pkg,"页面控件过多，本次未执行点击");return;}
            if(AdMatcher.sensitive(snapshot)) {finishPending();if(wmTouchReview.active())wmTouchReview.stop();resetPixelProofs();endVisualFollowup("当前页面暂停自动处理");traceBase="应用："+pkg;trace(pkg,"检测到密码/付款确认等内容，暂停处理",false);return;}
            now=SystemClock.uptimeMillis();
            long matchStarted=now;
            ModeReminder.Result reminder=prefs.getBoolean("mode_reminder",true)?ModeReminder.find(snapshot,size.x,size.y):ModeReminder.none();
            reminderIdle=reminder.present;
            boolean bili="tv.danmaku.bili".equals(pkg);
            boolean biliAd=bili&&!reminder.present&&prefs.getBoolean("browsing",true);
            boolean visualPanel=biliAd&&visualEnabled(false);
            BiliPanel.Hint panel=biliAd?BiliPanel.find(snapshot,size.x,size.y):null;
            boolean nativePanelEvidence=visualPanel&&(panel!=null||BiliPanel.canProbe(snapshot,size.x,size.y));
            boolean drawnPanelContext=visualPanel&&!nativePanelEvidence&&BiliPanel.pixelLabelContext(snapshot,size.x,size.y);
            boolean panelEvidence=nativePanelEvidence||drawnPanelContext;
            boolean nativeBlocked=DelayedAdWatch.jm(pkg)||MatchPolicy.requiresVisualSkip(pkg)||oopzOrigin.delegated(pkg,now)||WmSplashVisual.hasControl(snapshot);
            boolean genericPanel=!bili&&!reminder.present&&!nativeBlocked&&visualEnabled(pkg,false)&&!PicaVisual.trusted(pkg);
            int renderedControls=(visualPanel||genericPanel)&&!panelEvidence?RenderedAdPanel.visibleControls(snapshot,size.x,size.y):0;
            renderProbes.window(root.getWindowId());
            int nativePause=biliAd&&!nativeBlocked?BiliPanel.nativePauseClose(snapshot,size.x,size.y):-1;
            int provenSplash=!nativeBlocked&&!reminder.present&&nativePause<0?NativeSplashSkip.find(snapshot,size.x,size.y,startupProbe.active(now),pkg):NativeSplashSkip.NO_MATCH;
            if(!shakeDestinations.isEmpty()&&prefs.getBoolean(LocalLog.SHAKE,false)){
                boolean ad=!reminder.present&&(provenSplash>=0||nativePause>=0||panel!=null
                    ||startupProbe.active(now)&&AdSurface.present(snapshot,size.x,size.y));
                if(ad&&metadataConfirmedWindow==root.getWindowId()&&displayId(root)==Display.DEFAULT_DISPLAY)armShake(pkg,root.getWindowId(),now);
                else if(shake.observing(now)){shake.clicked(pkg,now);stopShakeSensor();}
            }
            if(bili&&panelProbes.observe(root.getWindowId(),panelEvidence||panel!=null||nativePause>=0,panel!=null,now)&&!visualFollowup.active(now))visualReview.clearScene();
            if(bili) {
                long panelReadDelay=panelProbes.treeDelay(root.getWindowId(),now);
                if(panelReadDelay>=0)queueKnownRead(now,now+panelReadDelay,false);
            }
            boolean panelAttempted=bili&&panelProbes.attemptedIn(root.getWindowId());
            String focusCue=reminder.present?"":panel!=null&&panelProbes.delay(root.getWindowId(),now)>=0?"panel:"+panel.key()
                :!"tv.danmaku.bili".equals(pkg)&&visualStartup(pkg,now)&&CanvasSkipVisual.nodeAd(snapshot,size.x,size.y)?"startup-ad":"";
            if(!visualEnabled(!"tv.danmaku.bili".equals(pkg)))focusCue="";
            adFocus.observe(pkg,root.getWindowId(),focusCue,now);
            if(reminder.present) {
                resetPixelProofs();endVisualFollowup("当前页面已有模式提醒");
                if(pending!=null&&!pending.reminder){actionStep(pending.pkg,"模式提醒覆盖当前页面，取消旧广告目标复查");finishPending();}
            }
            List<Integer> choices;
            if(reminder.present)choices=reminder.index>=0?java.util.Collections.singletonList(reminder.index):java.util.Collections.emptyList();
            else if(nativeBlocked)choices=java.util.Collections.emptyList();
            else if(nativePause>=0&&(!panelAttempted||pending!=null&&pending.nativePause))choices=java.util.Collections.singletonList(nativePause);
            else {
                choices=AdMatcher.candidates(snapshot,size.x,size.y,startupProbe.active(now),prefs.getBoolean("browsing",true),prefs.getBoolean("enhanced",true),pkg,provenSplash);
                if(biliAd&&!choices.isEmpty())choices.removeIf(i->(i==nativePause||AdMatcher.biliSheetClose(snapshot,i,size.x,size.y))
                    &&(panelAttempted&&pending==null||!verifiedBiliClose(panel,snapshot.get(i))));
            }
            matchMs=SystemClock.uptimeMillis()-matchStarted;
            if(reminder.present)adActiveUntil=reminder.index>=0&&prefs.getBoolean("gesture_retry",true)&&gate.allows(fingerprint(pkg,snapshot.get(reminder.index)),now)?now+2000:0;
            else if(pending==null&&!visualFollowup.active(now)&&!pixelPairPending)adActiveUntil=0;
            if("tv.danmaku.bili".equals(pkg)&&AdMatcher.biliAdHint(snapshot)&&!pkg.equals(actionPackage)&&pending==null) {
                actionPackage=pkg;actionDiagnostic="JUMP 0.5.35\n应用："+pkg+"，窗口="+root.getWindowId()+"\n读到广告相关内容，尚未提交本应用的点击动作";
                prefs.edit().putString("action_trace",actionDiagnostic).apply();
            }
            traceBase=snapshotTrace(pkg,snapshot,choices,size,startupProbe.active(now),reminder)+"\n当前窗口ID="+root.getWindowId()
                +(bili?"\nB站面板探测：当前证据="+(nativePanelEvidence?"原生广告布局或局部标签":drawnPanelContext?"需画面核对广告字样"
                    :renderedControls>0?"绘制层混合控件，有限取帧核验":"未满足"):"")
                +"\n扫描调度：方式="+(scanFromStartup?"开屏探测到点直达":"事件或普通观察")+"，等待="+queueWaitMs+"ms，超过计划="+queueLateMs+"ms；短时广告跟踪="+adFocus.rapid(now)
                +"\n读取耗时：根窗口="+rootReadMs+"ms，遍历="+walkMs+"ms，匹配="+matchMs+"ms，本次已用="+(SystemClock.uptimeMillis()-scanStartedAt)+"ms"
                +(pkg.equals(lastRealEventPkg)?"\n最近事件：产生至到达="+(lastRealEventCreatedAt>0?lastRealEventAt-lastRealEventCreatedAt:-1)
                    +"ms，到达至本次开始="+(scanStartedAt-lastRealEventAt)+"ms（事件年龄，非排队耗时）":"");
            trace(pkg,choices.isEmpty()?"未找到符合规则的按钮":"找到候选，继续校验点击条件",false);
            Set<String> visible=new HashSet<>();
            // Keep a clicked button blocked while it is visible, even if its ad label temporarily disappears.
            for(AdMatcher.Node node:snapshot) if(node.visible) visible.add(fingerprint(pkg,node));
            gate.observe(visible);
            now=SystemClock.uptimeMillis();
            if(pending!=null && reviewClick(pkg,snapshot,choices,visible,root.getWindowId(),refs,size)) return;
            int selected=-1;
            for(int index:choices) if(gate.allows(fingerprint(pkg,snapshot.get(index)),now)) {selected=index;break;}
            if(selected<0) {
                pixelOnly=false;
                if(reminder.present){trace(pkg,"模式提醒："+reminder.reason+"；未重复点击或请求画面",false);return;}
                if(provenSplash==NativeSplashSkip.CONFLICT){resetPixelProofs();trace(pkg,"原生开屏证据冲突或当前页面不适用；不以坐标替代",false);return;}
                if(choices.isEmpty()&&pending==null){
                    if(genericPanel&&tryRenderedPanel(pkg,root,size,renderedControls))return;
                    if(tryVisual(pkg,root,size,snapshot,panel,panelEvidence,drawnPanelContext,bili?renderedControls:0))return;
                }
                if(!choices.isEmpty())trace(pkg,"候选被防重复或频率限制拦截",false);
                if(choices.isEmpty()) for(AdMatcher.Node node:snapshot) if(node.evidence()) {
                    diagnostic(pkg,"发现广告标识，未找到符合条件的跳过或关闭按钮");break;
                }
                return;
            }
            pixelOnly=false;
            AccessibilityNodeInfo target=refs.get(selected);AdMatcher.Node node=snapshot.get(selected);
            boolean nativePauseTarget=selected==nativePause;
            boolean biliOwnTarget=nativePauseTarget||biliAd&&verifiedBiliClose(panel,node);
            boolean nativeSplash=!nativePauseTarget&&selected==provenSplash;
            if((nativeSplash||biliOwnTarget)&&displayId(root)!=Display.DEFAULT_DISPLAY){trace(pkg,"当前显示屏不支持这条原生广告操作，未点击",false);return;}
            if(!targetStillMatches(target,pkg,fingerprint(pkg,node))) {trace(pkg,"候选刷新失败或文字/描述/位置/状态已变化，等待重读",false);return;}
            if(nativeSplash&&(!startupProbe.active(SystemClock.uptimeMillis())||!NativeSplashSkip.currentControl(node,capture(target,0,pkg),size.x,size.y,pkg))){trace(pkg,"原生开屏当前文字、可点击状态或观察阶段复查未通过，未提交点击",false);return;}
            if(biliOwnTarget) {
                AdMatcher.Node refreshed=capture(target,0,pkg);
                if(!(nativePauseTarget?BiliPanel.currentNativePauseControl(refreshed):verifiedBiliClose(panel,refreshed))){
                    trace(pkg,"B站暂停广告关闭按钮刷新后已变化，未点击",false);return;
                }
            }
            Rect current=new Rect();target.getBoundsInScreen(current);
            if(current.left!=node.left||current.top!=node.top||current.right!=node.right||current.bottom!=node.bottom) {trace(pkg,"按钮位置变化，等待重新读取",false);return;}
            if(!stillForeground(pkg,root.getWindowId())) {trace(pkg,"前台应用或窗口复查失败，未执行点击",false);return;}
            boolean dismissMode=reminder.present&&reminder.index==selected;
            if(dismissMode&&!reminderWindow(pkg,root.getWindowId(),size,fingerprint(pkg,node))){trace(pkg,"模式提醒：当前窗口、标题、说明、按钮或模拟触摸开关复查未通过，未操作",false);return;}
            boolean explicitLabel=!nativeSplash&&AdMatcher.explicitSplashSkip(node,size.x,size.y,startupProbe.active(SystemClock.uptimeMillis()));
            if(explicitLabel&&!prefs.getBoolean("gesture_retry",true)) {trace(pkg,"明确跳过广告标签需要模拟触摸，但此开关已关闭，未点击",false);return;}
            now=SystemClock.uptimeMillis();gate.record(fingerprint(pkg,node),now);busy=true;
            pending=new Pending(pkg,fingerprint(pkg,node),traceBase,root.getWindowId(),now,target,dismissMode,nativeSplash,nativePauseTarget);
            actionPackage=pkg;actionDiagnostic="JUMP 0.5.35\n应用："+pkg+"，窗口="+root.getWindowId()
                +"\n目标位置="+current.toShortString();
            if(dismissMode)actionDiagnostic+="\n目标方式=关闭模式提醒弹窗，仅触摸明确中性按钮";
            else if(nativeSplash)actionDiagnostic+="\n目标方式=原生开屏跳过文字、当前按钮和独立开屏证据核验，不等待画面探测，不调用父节点";
            else if(nativePauseTarget)actionDiagnostic+="\n目标方式=B站暂停广告区域内唯一且语义明确的关闭按钮，仅操作按钮本身";
            else if("tv.danmaku.bili".equals(pkg))actionDiagnostic+="\n目标方式="+(AdMatcher.biliCloseControl(node)?"关闭文字或ID语义":"广告面板顶部图标结构推断");
            actionStep(pkg,"识别和前台复查通过；本次读取至动作="+(now-scanStartedAt)+"ms，候选采样距动作="+(now-node.sampledAt)
                +"ms，最近本应用事件到达距动作="+(pkg.equals(lastRealEventPkg)?now-lastRealEventAt:-1)+"ms");
            if(dismissMode){trace(pkg,"模式提醒标题、说明与唯一中性按钮复查通过，只关闭提醒",true);tap(pkg,current,"模式提醒关闭");return;}
            if(biliOwnTarget)panelProbes.attempted(root.getWindowId());
            if(nativeSplash){
                resetPixelProofs();visualFollowup.clear();visualPackage=pkg;visualSplash=!"tv.danmaku.bili".equals(pkg);visualInteresting=true;
                visualStatus("原生开屏跳过：当前文字、ID、全屏布局与真实窗口核验通过，本次不请求截图");
            }
            if(explicitLabel||MatchPolicy.preferGesture(pkg,node.text,node.desc,node.id,false,prefs.getBoolean("gesture_retry",true))) {
                if(pending!=null&&!explicitLabel&&!nativeSplash)pending.review.allowNativeFallback();
                if(nativeSplash)splashActions.mark(SplashActions.SKIP);
                trace(pkg,explicitLabel?"明确跳过广告文字与位置复查通过，直接触摸标签，不调用父节点":"识别完成，采用该应用的优先手势策略",true);
                tap(pkg,current,explicitLabel?"跳过广告文字触摸":"优先手势");return;
            }
            boolean accepted=target.isClickable()&&target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if(!accepted&&!nativeSplash&&!biliOwnTarget)accepted=clickParent(target,pkg,current,size);
            actionStep(pkg,accepted?"NATIVE_ACCEPTED：系统接受控件操作，尚未确认跳过":"NATIVE_REJECTED：控件操作未接受");
            if(accepted) {if(nativeSplash)splashActions.mark(SplashActions.SKIP);clicked(pkg,"控件点击");}
            else if(prefs.getBoolean("gesture_retry",true)){if(nativeSplash)splashActions.mark(SplashActions.SKIP);tap(pkg,current,"控件拒绝后的手势");}
            else {busy=false;finishPending();diagnostic(pkg,"控件未接受点击，手势补点已关闭");}
        } catch(RuntimeException error) {
            busy=false;finishPending();
            String message="界面读取中断："+error.getClass().getSimpleName()+" "+str(error.getMessage());
            if(message.length()>240)message=message.substring(0,240);
            trace(pkg,message,true);
            diagnostic(pkg,message);
        } finally {
            if(refs.isEmpty()) root.recycle();
            for(AccessibilityNodeInfo n:refs) if(n!=null) n.recycle();
            if(!busy&&!sparseProbeWait&&SystemClock.uptimeMillis()<burstUntil) queue(ScanPacing.followup(confirmedWork(SystemClock.uptimeMillis())));
        }
    }
    private Point screenSize(AccessibilityNodeInfo root) {
        DisplayManager manager=(DisplayManager)getSystemService(DISPLAY_SERVICE);
        int displayId=Display.DEFAULT_DISPLAY;
        if(Build.VERSION.SDK_INT>=30) {
            AccessibilityWindowInfo window=root.getWindow();
            try {if(window!=null)displayId=window.getDisplayId();}finally {if(window!=null)window.recycle();}
        }
        Display display=manager==null?null:manager.getDisplay(displayId);Point size=new Point();
        if(display!=null)display.getRealSize(size);
        if(size.x<=0||size.y<=0) {Rect b=new Rect();root.getBoundsInScreen(b);size.set(b.right,b.bottom);}
        return size;
    }
    private String snapshotTrace(String pkg,List<AdMatcher.Node> nodes,List<Integer> choices,Point size,boolean startup,ModeReminder.Result reminder) {
        int count=0,ads=0,skip=0,close=0,timedMatches=0,explicitMatches=0,renderers=0,unlabelled=0;StringBuilder buttons=new StringBuilder(),closes=new StringBuilder();
        boolean bili="tv.danmaku.bili".equals(pkg);
        for(AdMatcher.Node n:nodes) {
            if(n.parent>=0&&n.visible) {
                if(n.text.isEmpty()&&n.desc.isEmpty())unlabelled++;
                if(n.className.contains("WebView")||n.className.contains("SurfaceView")||n.className.contains("TextureView")||n.className.contains("Flutter"))renderers++;
            }
            if(AdMatcher.timedSplashSkip(n,size.x,size.y,startup,pkg))timedMatches++;
            if(AdMatcher.explicitSplashSkip(n,size.x,size.y,startup))explicitMatches++;
            if(n.visible&&n.parent>=0)count++;if(n.evidence())ads++;
            boolean possible=MatchPolicy.isSkipText(n.text)||MatchPolicy.isSkipText(n.desc)
                ||(MatchPolicy.isCountdownId(n.id)&&(MatchPolicy.isCountdownText(n.text)||MatchPolicy.isCountdownText(n.desc)));
            if(possible) {
                skip++;
                String label=MatchPolicy.isSkipText(n.text)?n.text:MatchPolicy.isSkipText(n.desc)?n.desc:MatchPolicy.isCountdownText(n.text)?n.text:n.desc;
                if(skip<=5) buttons.append("\n按钮 ").append(skip).append("：").append(label)
                    .append("，ID=").append(n.id).append("，可见=").append(n.visible).append("，可点击=").append(n.clickable)
                    .append("，位置=").append(n.left).append(',').append(n.top).append(',').append(n.right).append(',').append(n.bottom);
            }
            if(n.visible&&(bili?AdMatcher.biliCloseControl(n):AdMatcher.closeControl(n))) {
                close++;
                String label=AdMatcher.closeText(n.text)?n.text:AdMatcher.closeText(n.desc)?n.desc:"关闭控件（ID提示）";
                if(close<=5)closes.append("\n关闭 ").append(close).append("：").append(label)
                    .append("，ID=").append(n.id).append("，可点击=").append(n.clickable)
                    .append("，位置=").append(n.left).append(',').append(n.top).append(',').append(n.right).append(',').append(n.bottom);
            }
        }
        traceInteresting=reminder.present||ads>0||skip>0||!choices.isEmpty()||(bili&&AdMatcher.biliAdHint(nodes));
        if("air.tv.douyu.android".equals(pkg)) {
            if(traceInteresting)douyuAdSeen=true;
            // Preserve zero-button startup reads, without overwriting a real ad with the home page.
            if(startup&&!douyuAdSeen)traceInteresting=true;
        }
        return "应用："+pkg+"\n控件="+(nodes.size()-1)+"，可见="+count+"，广告标识="+ads+"，跳过/倒计时="+skip+"，候选="+choices.size()
            +"，关闭控件="+close+"\n无文字控件="+unlabelled+"，网页/绘制容器="+renderers
            +"\n显示尺寸="+size.x+"×"+size.y+"，启动识别窗口="+startup+"，开屏画面观察="+visualStartup(SystemClock.uptimeMillis())+buttons+closes
            +(reminder.present?"\n模式提醒弹窗：候选="+(reminder.index>=0)+"；"+reminder.reason:"")
            +"\n窗口归属："+rootWindowDiagnostic
            +(oopzOrigin.delegated(pkg,SystemClock.uptimeMillis())?"\n广告来源：OOPZ启动后的短时承载窗口；仍以当前真实窗口及广告像素为准":"")
            +(bili?"\n"+AdMatcher.biliSummary(nodes,size.x,size.y)+"\n"+AdMatcher.biliIconTrace(nodes,size.x,size.y):"")
            +(bili?"\nB站绘制广告面板：无文字绘制窗口="+BiliPanel.drawnContext(nodes,size.x,size.y)
                +"；两张画面须同时匹配关闭图形与独立广告标识":"")
            +(bili?"\nB站全屏开屏：无原生标签探测可用="+BiliLaunchVisual.context(pkg,visualStartup(pkg,SystemClock.uptimeMillis()),splashActions.finished(),nodes)
                +"，已尝试="+splashActions.finished()+"，无文字探测="+startupProbe.count()+"/最多8；需平台字标、向上箭头、右下跳过/数字/胶囊及两帧核验":"")
            +(DelayedAdWatch.jm(pkg)?"\nJM流程：仅处理红X广告页，线路手动选择；读到关闭提示="+JmVisual.captionNodes(nodes):"")
            +(VolunteerVisual.applies(pkg)?"\n志愿汇：绘制容器画面观察="+VolunteerVisual.context(pkg,visualStartup(pkg,SystemClock.uptimeMillis()),nodes)+"；隐藏tvSkip不作点击目标，以当前圆环和两字笔画为准":"")
            +(WmSplashVisual.applies(pkg)||WmSplashVisual.hasControl(nodes)?"\nWM开屏流程：禁用控件/父节点点击；有效SDK控件="+(WmSplashVisual.hint(pkg,nodes,size.x,size.y,visualStartup(pkg,SystemClock.uptimeMillis()))!=null)
                +"，OOPZ控件缺失回退可用="+(WmSplashVisual.hint(pkg,nodes,size.x,size.y,visualStartup(pkg,SystemClock.uptimeMillis()))==null&&!wmTouchReview.belongsTo(pkg)&&OopzSurfaceVisual.context(pkg,visualStartup(pkg,SystemClock.uptimeMillis()),nodes)):"")
            +(WmSplashVisual.applies(pkg)||WmSplashVisual.hasControl(nodes)?"\nWM触摸复查：已尝试="+(wmTouchReview.belongsTo(pkg)?wmTouchReview.attempts():0)+"/2，状态="+(!wmTouchReview.belongsTo(pkg)?"尚未操作":wmTouchReview.awaitingTarget()?"当前目标缺失，等待重新确认":wmTouchReview.active()?"等待当前窗口的两帧核验":"已结束")+"，剩余观察="+wmTouchReview.remaining(SystemClock.uptimeMillis())+"ms":"")
            +(WmSplashVisual.applies(pkg)?"\nOOPZ独立跳过标签：有效="+(OopzLeftSkipVisual.hint(pkg,nodes,size.x,size.y,visualStartup(pkg,SystemClock.uptimeMillis()))!=null)+"；支持左上/左下，需当前可见广告标识、两字及倒计时；左下还需菜单":"")
            +(startup?"\n原生布局跳过：完整布局及唯一可见倒计时按钮="+(NativeSplashSkip.find(nodes,size.x,size.y,true,pkg)>=0)+"；无需截图，目标刷新与真实前台复查，禁止父节点动作":"")
            +(startup?"\n通用绘制跳过：上下文="+CanvasSkipVisual.context(pkg,startup,nodes)+"，当前原生广告标识="+CanvasSkipVisual.nativeAd(nodes)+"；按布局核验，支持跳过与数字两种顺序":"")
            +(startup?"\n通用独立跳过：全屏广告容器="+AdSurface.present(nodes,size.x,size.y)+"；支持独立两字/括号秒数，仍需当前广告证据与两帧":"")
            +(startup&&!CanvasSkipVisual.nativeAd(nodes)?"\n"+AdSurface.trace(nodes,size.x,size.y):"")
            +(startup&&skip>0?"\n通用开屏规则：右上角倒计时跳过匹配="+timedMatches+"，明确跳过广告标签="+explicitMatches+"（不依赖额外广告标识或控件ID）":"");
    }
    private void trace(String pkg,String result,boolean forceSave) {
        if(pkg.isEmpty()||pkg.equals(getPackageName())||excludedPackage(pkg))return;
        long now=SystemClock.uptimeMillis();
        String time=new java.text.SimpleDateFormat("MM-dd HH:mm:ss",java.util.Locale.CHINA).format(new java.util.Date());
        realDiagnostic="采样："+time+"\n采样服务版本："+serviceVersion+"\n"+traceBase+"\n结果："+result;
        if(traceInteresting)adDiagnostic=realDiagnostic;
        readings.note(pkg,realDiagnostic);
        if(forceSave||now-lastTraceSaved>=ScanPacing.RECORD_INTERVAL)flushDiagnostics();
    }
    private void flushDiagnostics() {
        if(prefs==null)return;
        lastTraceSaved=SystemClock.uptimeMillis();recentDiagnostics=readings.encode();
        SharedPreferences.Editor e=prefs.edit().putString("recent_reads",recentDiagnostics).putString("real_trace",realDiagnostic).putString("ad_trace",adDiagnostic);
        if(!visualPackage.isEmpty())e.putString("visual_trace",visualDiagnostic).putString("visual_trace:"+visualPackage,visualDiagnostic);
        e.apply();
    }
    private void actionStep(String pkg,String step) {
        if(!pkg.equals(actionPackage)||pkg.equals(getPackageName()))return;
        String time=new java.text.SimpleDateFormat("HH:mm:ss.SSS",java.util.Locale.CHINA).format(new java.util.Date());
        actionDiagnostic+="\n"+time+" "+step;
        if(actionDiagnostic.length()>5000)actionDiagnostic=actionDiagnostic.substring(0,5000);
        prefs.edit().putString("action_trace",actionDiagnostic).apply();
    }
    private boolean clickParent(AccessibilityNodeInfo target,String pkg,Rect child,Point size) {
        AccessibilityNodeInfo parent=target.getParent();
        try {
            for(int depth=0;parent!=null&&depth<4;depth++) {
                AccessibilityNodeInfo node=parent;parent=null;
                try {
                    Rect b=new Rect();node.getBoundsInScreen(b);
                    boolean safe=node.isVisibleToUser()&&node.isEnabled()&&!node.isEditable()&&!node.isCheckable()
                        &&pkg.equals(str(node.getPackageName()))&&b.width()<=size.x*.32&&b.height()<=size.y*.10&&b.contains(child);
                    if(!safe)return false;
                    if(node.isClickable()&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK))return true;
                    parent=node.getParent();
                } finally {node.recycle();}
            }
            return false;
        } finally {if(parent!=null)parent.recycle();}
    }
    private boolean targetStillMatches(AccessibilityNodeInfo target,String pkg,String key) {
        return target.refresh()&&target.isVisibleToUser()&&target.isEnabled()&&!target.isEditable()&&!target.isCheckable()
            &&key.equals(fingerprint(pkg,capture(target,0,pkg)));
    }
    private boolean probeOriginalTarget(String pkg,int windowId) {
        Pending action=pending;
        if(action==null||action.reminder||action.probed||!action.pkg.equals(pkg)||action.windowId!=windowId)return false;
        action.probed=true;long started=SystemClock.uptimeMillis();
        try {
            boolean refreshed=action.originalTarget.refresh();
            AdMatcher.Node current=refreshed?capture(action.originalTarget,0,pkg):null;
            long now=SystemClock.uptimeMillis();
            boolean visible=current!=null&&current.visible;
            boolean sameTarget=visible&&action.key.equals(fingerprint(pkg,current));
            actionStep(pkg,"FAST_PROBE：原节点刷新="+refreshed+"，可见="+visible+"，仍为原目标="+sameTarget
                +"，操作后="+(now-action.createdAt)+"ms，根窗口读取="+rootReadMs+"ms，快速读取耗时="+(now-started)+"ms；仅观察，不据此认定跳过成功");
            if(sameTarget&&action.review.inSettlingTime(now)){queue(180);return true;}
        } catch(RuntimeException error) {actionStep(pkg,"FAST_PROBE：读取异常 "+error.getClass().getSimpleName()+"，改用完整界面复查");}
        return false;
    }
    private boolean reviewClick(String pkg,List<AdMatcher.Node> nodes,List<Integer> choices,Set<String> visible,int windowId,List<AccessibilityNodeInfo> refs,Point size) {
        long now=SystemClock.uptimeMillis();
        Pending action=pending;boolean eligible=false;AdMatcher.Node target=null;
        int targetIndex=-1;
        for(int i:choices)if(action.key.equals(fingerprint(pkg,nodes.get(i)))){eligible=true;target=nodes.get(i);targetIndex=i;break;}
        if(eligible&&action.nativeSplash&&targetIndex!=NativeSplashSkip.find(nodes,size.x,size.y,startupProbe.active(now),pkg))eligible=false;
        if(eligible&&action.nativePause&&targetIndex!=BiliPanel.nativePauseClose(nodes,size.x,size.y))eligible=false;
        boolean sameApp=action.pkg.equals(pkg),sameWindow=action.windowId==windowId,targetVisible=visible.contains(action.key);
        ClickReview.Action decision=action.review.decide(now,sameApp&&sameWindow,targetVisible,eligible,prefs.getBoolean("gesture_retry",true));
        String currentTrace=traceBase;
        String detail="同应用="+sameApp+"，同窗口="+sameWindow+"，原按钮仍可见="+targetVisible
            +"，当前符合规则="+eligible+"，当前候选="+choices.size()+"，首次操作真实距今="+(now-action.createdAt)+"ms";
        if(decision==ClickReview.Action.WAIT){
            if(!eligible&&!action.notedUnstable){action.notedUnstable=true;actionStep(pkg,"条件暂时变化，等待重读；"+detail);}
            trace(pkg,"点击复查等待："+detail,false);queue(220);return true;
        }
        if(decision==ClickReview.Action.TRY_GESTURE&&target!=null) {
            if(!targetStillMatches(refs.get(targetIndex),pkg,action.key)||(action.reminder&&!reminderWindow(pkg,windowId,size,action.key))) {
                actionStep(pkg,"手势补点取消：原目标刷新校验未通过");finishPending();return true;
            }
            if(action.nativeSplash&&(!startupProbe.active(SystemClock.uptimeMillis())||!NativeSplashSkip.currentControl(target,capture(refs.get(targetIndex),0,pkg),size.x,size.y,pkg))){actionStep(pkg,"原生开屏补点取消：刷新后的文字、可点击状态或观察阶段已变化");finishPending();return true;}
            if(action.nativePause&&!BiliPanel.currentNativePauseControl(capture(refs.get(targetIndex),0,pkg))){actionStep(pkg,"B站暂停广告补点取消：原关闭控件已变化");finishPending();return true;}
            action.probed=false;
            actionStep(pkg,"复查条件通过，允许补一次手势；"+detail);
            traceBase=currentTrace+"\n首次操作时：\n"+action.traceAtAction;traceInteresting=true;trace(pkg,"原按钮仍在，补一次手势；"+detail,true);
            diagnostic(pkg,"按钮仍在且仍符合广告条件，补一次手势点击");
            tap(pkg,new Rect(target.left,target.top,target.right,target.bottom),"复查补点");return true;
        }
        if(decision==ClickReview.Action.TRY_NATIVE&&targetIndex>0) {
            AccessibilityNodeInfo ref=refs.get(targetIndex);
            if(!targetStillMatches(ref,pkg,action.key)||!stillForeground(pkg,windowId)) {
                actionStep(pkg,"控件补点取消：当前目标或前台校验未通过");finishPending();return true;
            }
            Rect b=new Rect();ref.getBoundsInScreen(b);busy=true;action.probed=false;
            actionStep(pkg,"手势后原按钮仍符合规则，完整界面复查通过，补一次控件点击；"+detail);
            boolean accepted=ref.isClickable()&&ref.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if(!accepted)accepted=clickParent(ref,pkg,b,size);
            action.review.markNative(SystemClock.uptimeMillis());
            actionStep(pkg,accepted?"NATIVE_FALLBACK_ACCEPTED：系统接受控件补点，实际结果待复查":"NATIVE_FALLBACK_REJECTED：系统未接受控件补点");
            if(accepted)clicked(pkg,"控件补点");else {busy=false;queue(180);}
            return true;
        }
        if(decision==ClickReview.Action.TARGET_GONE)diagnostic(pkg,"完整采样未再读到原跳过/关闭按钮，无法确认广告是否关闭");
        else if(decision==ClickReview.Action.STOP)diagnostic(pkg,"按钮仍存在，停止重复点击");
        else diagnostic(pkg,"界面或识别条件变化，取消后续补点");
        String reason=decision==ClickReview.Action.CANCEL?(!sameApp?"前台应用变化":!sameWindow?"窗口变化":"识别条件持续不满足")
            :decision==ClickReview.Action.TARGET_GONE?"完整采样未再读到原按钮，实际广告状态未知":"不再重复点击";
        traceBase=currentTrace+"\n首次操作时：\n"+action.traceAtAction;traceInteresting=true;
        trace(pkg,"点击复查："+decision+"，原因："+reason+"；"+detail,true);
        actionStep(pkg,"REVIEW_"+decision+"："+reason+"；"+detail);
        finishPending();return decision!=ClickReview.Action.TARGET_GONE;
    }
    private AccessibilityNodeInfo readRoot(){return readRoot(true);}
    private AccessibilityNodeInfo readRoot(boolean prefetch) {
        int flags=prefetch?AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST:0;
        AccessibilityNodeInfo root=Build.VERSION.SDK_INT>=33?getRootInActiveWindow(flags):getRootInActiveWindow();
        int framework=root==null?-1:root.getWindowId();metadataConfirmedWindow=-1;
        List<AccessibilityWindowInfo> windows=null;
        try {
            windows=getWindows();List<RootWindows.Window> descriptions=new ArrayList<>();
            for(AccessibilityWindowInfo window:windows)descriptions.add(new RootWindows.Window(window.getId(),window.getLayer(),window.getType()==AccessibilityWindowInfo.TYPE_APPLICATION,window.isActive(),window.isFocused()));
            int chosen=RootWindows.choose(framework,descriptions);
            if(RootWindows.confirmed(chosen,descriptions))metadataConfirmedWindow=chosen;
            rootWindowDiagnostic="系统根窗口="+framework+"，交互窗口核验="+chosen+(chosen==framework?"，归属一致":"，已拒绝复用旧根")+(chosen>=0&&metadataConfirmedWindow==chosen?"，元数据已确认":"，交互元数据未确认");
            if(chosen==framework)return root;
            if(root!=null){root.recycle();root=null;}
            if(chosen<0)return null;
            for(AccessibilityWindowInfo window:windows)if(window.getId()==chosen)return Build.VERSION.SDK_INT>=33?window.getRoot(flags):window.getRoot();
            return null;
        } catch(RuntimeException error){rootWindowDiagnostic="交互窗口核验异常="+error.getClass().getSimpleName()+"；仅保留系统当前根";return root;}
        finally{if(windows!=null)for(AccessibilityWindowInfo window:windows)window.recycle();}
    }
    private AccessibilityNodeInfo readChild(AccessibilityNodeInfo node,int index,String pkg){
        if(Build.VERSION.SDK_INT<33)return node.getChild(index);
        int flags=WmSplashVisual.applies(pkg)?0:index==0?AccessibilityNodeInfo.FLAG_PREFETCH_DESCENDANTS_BREADTH_FIRST:AccessibilityNodeInfo.FLAG_PREFETCH_SIBLINGS;
        return node.getChild(index,flags);
    }
    private AdMatcher.Node capture(AccessibilityNodeInfo node,int parent,String pkg) {
        Rect b=new Rect();node.getBoundsInScreen(b);
        AdMatcher.Node n=new AdMatcher.Node(parent,b.left,b.top,b.right,b.bottom,str(node.getText()),node.getViewIdResourceName());
        String owner=str(node.getPackageName());
        n.desc=str(node.getContentDescription());n.className=str(node.getClassName());n.visible=node.isVisibleToUser()&&(owner.isEmpty()||pkg.equals(owner));
        n.enabled=node.isEnabled();n.password=node.isPassword();n.scrollable=node.isScrollable();
        n.editable=node.isEditable();n.checkable=node.isCheckable();n.clickable=node.isClickable();n.sampledAt=SystemClock.uptimeMillis();return n;
    }
    private String fingerprint(String pkg,AdMatcher.Node n) {
        boolean countdown=MatchPolicy.isCountdownId(n.id);
        String text=MatchPolicy.isSkipText(n.text)?"skip":countdown&&MatchPolicy.isCountdownText(n.text)?"countdown":n.text;
        String desc=MatchPolicy.isSkipText(n.desc)?"skip":countdown&&MatchPolicy.isCountdownText(n.desc)?"countdown":n.desc;
        return pkg+"|"+n.id+"|"+text+"|"+desc+"|"+n.left+","+n.top+","+n.right+","+n.bottom
            +("tv.danmaku.bili".equals(pkg)?"|"+n.className+"|"+n.clickable:"");
    }
    private boolean reminderWindow(String pkg,int windowId,Point size,String expected) {
        if(!prefs.getBoolean("running",true)||!prefs.getBoolean("mode_reminder",true)||!prefs.getBoolean("gesture_retry",true)||!interactive())return false;
        AccessibilityNodeInfo root=readRoot(false);if(root==null)return false;
        List<AccessibilityNodeInfo> refs=new ArrayList<>();List<AdMatcher.Node> nodes=new ArrayList<>();
        try {
            if(!pkg.equals(str(root.getPackageName()))||root.getWindowId()!=windowId||displayId(root)!=Display.DEFAULT_DISPLAY)return false;
            Point live=screenSize(root);if(live.x!=size.x||live.y!=size.y)return false;
            refs.add(null);nodes.add(new AdMatcher.Node(-1,0,0,size.x,size.y,"",""));refs.add(root);nodes.add(capture(root,0,pkg));
            for(int i=1;i<refs.size()&&refs.size()<1000;i++)for(int n=0;n<refs.get(i).getChildCount()&&refs.size()<1000;n++) {
                AccessibilityNodeInfo child=readChild(refs.get(i),n,pkg);if(child!=null){refs.add(child);nodes.add(capture(child,i,pkg));}
            }
            ModeReminder.Result r=ModeReminder.find(nodes,size.x,size.y);
            return refs.size()<1000&&r.index>=0&&expected.equals(fingerprint(pkg,nodes.get(r.index)));
        } finally {
            if(refs.isEmpty())root.recycle();else for(AccessibilityNodeInfo ref:refs)if(ref!=null)ref.recycle();
        }
    }
    private boolean stillForeground(String pkg) {
        return stillForeground(pkg,pending==null?-1:pending.windowId);
    }
    private boolean stillForeground(String pkg,int expectedWindowId) {
        if(!interactive()||userExcluded(pkg)) return false;
        AccessibilityNodeInfo root=readRoot(false);if(root==null) return false;
        try {return pkg.equals(str(root.getPackageName()))&&(expectedWindowId<0||expectedWindowId==root.getWindowId());} finally {root.recycle();}
    }
    private void clicked(String pkg,String method) {
        busy=false;
        if(shakeSensors!=null){shake.clicked(pkg,SystemClock.uptimeMillis());stopShakeSensor();}
        LocalLog.recordAction(this,pkg,pending!=null&&pending.reminder?"关闭模式提醒":"广告点击",false);
        LocalLog.add(this,pkg+"：已执行"+method+"，实际效果待确认");
        if(!pkg.equals(getPackageName()))trace(pkg,"系统接受"+method+"，等待复查",true);
        burstUntil=SystemClock.uptimeMillis()+2500;queue(180);
    }
    private void tap(String pkg,Rect b,String purpose) {
        if(!stillForeground(pkg)) {busy=false;finishPending();actionStep(pkg,"手势未提交：前台应用或窗口复查失败");trace(pkg,"手势未提交：前台应用或窗口复查失败",true);return;}
        busy=true;if(pending!=null)pending.review.markGesture(SystemClock.uptimeMillis());
        final int token=generation,serial=++gestureSerial;
        actionStep(pkg,"请求 "+purpose+"，实际按钮中心=("+b.centerX()+","+b.centerY()+")");
        Path path=new Path();path.moveTo(b.exactCenterX(),b.exactCenterY());
        GestureDescription gesture=new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path,0,60)).build();
        boolean accepted=dispatchGesture(gesture,new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription g) {
                if(token==generation&&serial==gestureSerial){actionStep(pkg,"GESTURE_COMPLETED：系统报告手势完成，尚需确认按钮消失");clicked(pkg,"手势点击 GESTURE_COMPLETED");}
            }
            @Override public void onCancelled(GestureDescription g) {
                if(token==generation&&serial==gestureSerial) {
                    actionStep(pkg,"GESTURE_CANCELLED：系统取消手势");
                    busy=false;finishPending();trace(pkg,"GESTURE_CANCELLED：系统取消手势（不同于规则复查 CANCEL）",true);
                    diagnostic(pkg,"系统取消了点击手势");queue(650);
                }
            }
        },handler);
        if(!accepted) {
            busy=false;finishPending();actionStep(pkg,"GESTURE_REJECTED：系统未接受手势请求");
            trace(pkg,"GESTURE_REJECTED：系统未接受手势请求",true);diagnostic(pkg,"系统未接受手势请求");
        } else {
            actionStep(pkg,"GESTURE_SUBMITTED：系统接受手势请求");
            trace(pkg,"GESTURE_SUBMITTED："+purpose+"，位置=("+b.centerX()+","+b.centerY()+")，等待系统回调",true);
            handler.postDelayed(()-> {
                if(token==generation&&serial==gestureSerial&&busy) {
                    gestureSerial++;busy=false;finishPending();
                    actionStep(pkg,"GESTURE_CALLBACK_TIMEOUT：2秒内未收到系统手势回调");
                    trace(pkg,"GESTURE_CALLBACK_TIMEOUT：未确认手势结果，停止本次操作",true);
                }
            },2000);
        }
    }
    private void visualStatus(String message) {
        if(!visualPackage.equals(visualHistoryPackage)) {
            visualHistoryPackage=visualPackage;visualSteps.clear();lastVisualStep="";
        }
        if(!message.startsWith("正在请求")&&!message.startsWith("等待画面读取频率")&&!message.equals(lastVisualStep)) {
            lastVisualStep=message;
            String at=new java.text.SimpleDateFormat("HH:mm:ss.SSS",java.util.Locale.CHINA).format(new java.util.Date());
            visualSteps.addLast(at+" "+message.substring(0,Math.min(450,message.length())));
            while(visualSteps.size()>16)visualSteps.removeFirst();
        }
        StringBuilder steps=new StringBuilder();for(String step:visualSteps)steps.append('\n').append(step);
        visualDiagnostic="JUMP 0.5.35 · "+(visualSplash?"开屏画面识别":"本地广告面板识别")+"\n应用："+visualPackage+"\n"+message+"\n最近画面过程："+steps;
        // Snapshot encoding and persistence are batched by trace; action records stay immediate.
        traceInteresting=visualInteresting;trace(visualPackage,"画面识别："+message,false);
    }
    private int displayId(AccessibilityNodeInfo root) {
        if(Build.VERSION.SDK_INT<30)return Display.DEFAULT_DISPLAY;
        AccessibilityWindowInfo window=root.getWindow();
        try{return window==null?Display.DEFAULT_DISPLAY:window.getDisplayId();}
        finally{if(window!=null)window.recycle();}
    }
    private boolean visualWindow(String pkg,int windowId,int displayId,Point size,boolean requireAd,boolean timed,boolean requireCaption) {
        return visualWindow(pkg,windowId,displayId,size,requireAd,timed,requireCaption,null,false,false,null,false,false,null,false,false,-1,-1,-1,-1);
    }
    private boolean visualWindow(String pkg,int windowId,int displayId,Point size,boolean requireAd,boolean timed,boolean requireCaption,WmSplashVisual.Hint wmHint,boolean volunteer,boolean oopzSurface,OopzLeftSkipVisual.Hint oopzLeftHint,boolean canvasSkip,boolean canvasNativeAd,BiliPanel.Hint biliHint,boolean biliLaunch,boolean renderedPanel,int x,int y,int labelX,int labelY) {
        visualWindowMismatch="";
        if(!interactive()||userExcluded(pkg)){visualWindowMismatch="屏幕锁定或应用已排除";return false;}
        AccessibilityNodeInfo root=readRoot(requireAd&&renderedPanel);
        if(root==null){visualWindowMismatch="当前根窗口为空或交互窗口有歧义；"+rootWindowDiagnostic;return false;}
        try {
            Point current=screenSize(root);
            visualWindowMismatch=FrameIdentity.mismatch(pkg,windowId,displayId,size.x,size.y,str(root.getPackageName()),root.getWindowId(),displayId(root),current.x,current.y);
            if(!visualWindowMismatch.isEmpty())return false;
            if(!requireAd)return true;
            if(renderedPanel&&metadataConfirmedWindow!=windowId){visualWindowMismatch="当前窗口的交互元数据未确认";return false;}
            // Re-read ad/sensitive context just before touching a screenshot-derived word.
            List<AccessibilityNodeInfo> refs=new ArrayList<>();List<AdMatcher.Node> nodes=new ArrayList<>();
            refs.add(root);nodes.add(capture(root,-1,pkg));
            try {
                for(int i=0;i<refs.size()&&refs.size()<1000;i++) {
                    AccessibilityNodeInfo n=refs.get(i);
                    for(int j=0;j<n.getChildCount()&&refs.size()<1000;j++) {
                        AccessibilityNodeInfo child=readChild(n,j,pkg);if(child!=null){refs.add(child);nodes.add(capture(child,i,pkg));}
                    }
                }
                boolean startup=visualStartup(pkg,SystemClock.uptimeMillis());
                if(prefs.getBoolean("mode_reminder",true)&&ModeReminder.find(nodes,size.x,size.y).present){
                    visualWindowMismatch="当前窗口出现模式提醒";return false;
                }
                if(biliLaunch)return refs.size()<1000&&BiliLaunchVisual.context(pkg,startup,false,nodes)
                    &&NativeSplashSkip.find(nodes,size.x,size.y,startup,pkg)!=NativeSplashSkip.CONFLICT;
                if(renderedPanel){
                    visualWindowMismatch="tv.danmaku.bili".equals(pkg)
                        ?BiliPanel.drawnTargetProblem(nodes,size.x,size.y,x,y,labelX,labelY)
                        :RenderedAdPanel.targetProblem(nodes,size.x,size.y,x,y,labelX,labelY,RenderedAdPanel.visibleControls(nodes,size.x,size.y)>0);
                    return visualWindowMismatch.isEmpty();
                }
                if("tv.danmaku.bili".equals(pkg))return refs.size()<1000&&BiliPanel.confirmTarget(nodes,size.x,size.y,biliHint,x,y);
                if(canvasSkip)return refs.size()<1000&&!WmSplashVisual.hasControl(nodes)&&CanvasSkipVisual.context(pkg,startup,nodes)&&(!canvasNativeAd||CanvasSkipVisual.nodeAd(nodes,size.x,size.y))
                    &&NativeSplashSkip.find(nodes,size.x,size.y,startup,pkg)!=NativeSplashSkip.CONFLICT;
                if(oopzSurface)return refs.size()<1000&&OopzOrigin.context(pkg,startup,nodes,oopzOrigin.delegated(pkg,SystemClock.uptimeMillis()))
                    &&(oopzLeftHint==null||OopzLeftSkipVisual.same(oopzLeftHint,OopzLeftSkipVisual.hint(pkg,nodes,size.x,size.y,startup)));
                if(volunteer)return refs.size()<1000&&VolunteerVisual.context(pkg,startup,nodes);
                if(wmHint!=null||WmSplashVisual.applies(pkg))return refs.size()<1000&&WmSplashVisual.same(wmHint,WmSplashVisual.hint(pkg,nodes,size.x,size.y,startup));
                return refs.size()<1000&&(!requireCaption||JmVisual.captionNodes(nodes))&&(timed?(SplashVisual.context(pkg,startup,nodes)||JmVisual.context(pkg,startup,nodes))
                    :SplashVisual.context(pkg,startup,nodes));
            } finally {for(int i=1;i<refs.size();i++)refs.get(i).recycle();}
        } finally{root.recycle();}
    }
    private boolean visualEnabled(boolean splash){return frameAccess&&visualSelected(splash);}
    private boolean visualEnabled(String pkg,boolean splash){return frameAccess&&visualSelected(pkg,splash);}
    private boolean visualSelected(boolean splash){return visualSelected("tv.danmaku.bili",splash);}
    private boolean visualSelected(String pkg,boolean splash) {
        return prefs!=null&&prefs.getBoolean("running",true)&&prefs.getBoolean("gesture_retry",true)
            &&(splash?prefs.getBoolean("splash_visual",true):prefs.getBoolean("browsing",true)
                &&(!"tv.danmaku.bili".equals(pkg)||prefs.getBoolean("bili_visual",true)));
    }
    private static boolean verifiedBiliClose(BiliPanel.Hint panel,AdMatcher.Node node){
        return panel!=null&&panel.headerPoint((node.left+node.right)/2,(node.top+node.bottom)/2)
            &&BiliPanel.currentScopedCloseControl(node);
    }
    private void endVisualFollowup(String reason){
        if(!visualFollowup.pending())return;
        visualFollowup.clear();
        actionStep(visualPackage,"VISUAL_OUTCOME_UNVERIFIED："+reason+"；实际关闭结果未核实");
    }
    private boolean tryRenderedPanel(String pkg,AccessibilityNodeInfo root,Point size,int renderedControls){
        if(renderedControls<=0)return false;
        long now=SystemClock.uptimeMillis();
        int window=root.getWindowId();
        boolean samePanel=pkg.equals(visualPackage)&&!visualSplash;
        boolean reviewing=samePanel&&visualFollowup.current(pkg,window,now);
        boolean confirming=samePanel&&pixelPairPending;
        if(!reviewing&&!confirming&&!renderProbes.ready(renderedControls,now))return false;
        long delay=pixelDelay(now);
        if(delay>0){
            if(delay>=1500)return false;
            queueKnownRead(now,now+delay,false);
            return true;
        }
        if(!reviewing&&!confirming)renderProbes.requested(renderedControls,now);
        visualPackage=pkg;visualSplash=false;visualInteresting=reviewing||confirming;
        return captureVisual(new CapturePlan(pkg,false,generation,window,displayId(root),size,false,
            (screenshot,full)->VisualFrame.analyzeRenderedPanel(screenshot,negativeFrames)),FULL,now);
    }
    private boolean tryVisual(String pkg,AccessibilityNodeInfo root,Point size,List<AdMatcher.Node> nodes,BiliPanel.Hint biliHint,boolean panelEvidence,boolean drawnPanelContext,int renderedControls) {
        final boolean splash=!"tv.danmaku.bili".equals(pkg);
        long now=SystemClock.uptimeMillis();
        if(wmTouchReview.expire(now))actionStep(pkg,"WM_REVIEW_EXPIRED：有限观察时限已到，停止补点；不代表广告已关闭");
        if(wmTouchReview.active()&&!visualStartup(pkg,now))wmTouchReview.stop();
        boolean reviewing=(visualFollowup.current(pkg,root.getWindowId(),now)||wmTouchReview.active()&&wmTouchReview.belongsTo(pkg))&&pkg.equals(visualPackage)&&visualSplash==splash;
        visualPackage=pkg;visualSplash=splash;
        if(!visualSelected(splash)){
            endVisualFollowup("画面识别已关闭");
            visualStatus("未请求画面：对应画面识别、浏览广告或模拟手指点击开关已关闭");return false;
        }
        if(!frameAccess){
            endVisualFollowup("画面读取能力不可用");
            visualStatus(Build.VERSION.SDK_INT<30?"本地画面读取需要 Android 11 及以上":"VISUAL_CAPABILITY_MISSING：系统未授予截图能力，可在系统无障碍中重新启用轻跳");return false;
        }
        boolean startup=visualStartup(pkg,now);
        long panelDelay=!splash&&panelEvidence?panelProbes.delay(root.getWindowId(),now):-1;
        final boolean chargePanel=!splash&&panelEvidence&&!reviewing&&!pixelPairPending&&panelDelay>=0;
        final boolean biliLaunchEligible=!splash&&BiliLaunchVisual.context(pkg,startup,!reviewing&&splashActions.finished(),nodes);
        final boolean renderedContext=renderedControls>0&&(!renderProbes.attempted()||reviewing);
        final boolean chargeRendered=renderedContext&&!reviewing&&!pixelPairPending
            &&(!biliLaunchEligible||startupProbe.delay(now)!=0)&&renderProbes.ready(renderedControls,now);
        final boolean biliPanelContext=chargePanel||chargeRendered
            ||(panelEvidence||renderedContext)&&(reviewing||pixelPairPending);
        final boolean jmCaption=DelayedAdWatch.jm(pkg)&&JmVisual.captionNodes(nodes);
        final boolean volunteer=VolunteerVisual.context(pkg,startup,nodes);
        final boolean carrier=oopzOrigin.delegated(pkg,now);
        final WmSplashVisual.Hint wmHint=carrier?null:WmSplashVisual.hint(pkg,nodes,size.x,size.y,startup);
        final boolean oopzSurface=wmHint==null&&!wmTouchReview.belongsTo(pkg)&&OopzOrigin.context(pkg,startup,nodes,carrier);
        final OopzLeftSkipVisual.Hint oopzLeftHint=oopzSurface?OopzLeftSkipVisual.hint(pkg,nodes,size.x,size.y,startup):null;
        if(carrier&&!oopzSurface){resetPixelProofs();visualStatus("OOPZ来源窗口未通过当前页面校验，未请求画面或点击；图片预览、输入页和过期来源均不处理");return false;}
        if(!oopzSurface)surfaceProof.reset();
        if(wmHint==null)wmPairProof.reset();
        final boolean canvasEligible=wmHint==null&&!DelayedAdWatch.jm(pkg)&&!volunteer&&!PicaVisual.trusted(pkg)&&CanvasSkipVisual.context(pkg,startup,nodes);
        final boolean canvasSurface=canvasEligible&&splash&&AdSurface.present(nodes,size.x,size.y);
        final boolean canvasNativeAd=canvasEligible&&(canvasSurface||CanvasSkipVisual.nativeAd(nodes));
        if(canvasSurface)startupProbe.surface(now);
        else if(splash&&canvasNativeAd&&!weakStartupEvidenceSeen){startupProbe.evidence(now);weakStartupEvidenceSeen=true;}
        boolean slowWatch=!reviewing&&!timedClose.active()&&!jmCaption&&(DelayedAdWatch.active(pkg,splashActions.finished())
            ||picaNoticeWatch(pkg,now)&&(splashActions.finished()||now-enteredAt>=StartupProbe.WINDOW));
        boolean hasAd=SplashVisual.context(pkg,startup,nodes);
        // A label/image is a reason to bring an existing slot forward, not to turn on screen polling.
        boolean budgetedSplash=splash&&!slowWatch&&!DelayedAdWatch.jm(pkg)&&wmHint==null&&!volunteer&&!oopzSurface
            &&!reviewing&&!pixelPairPending&&!timedClose.active()&&!picaSequence&&(!hasAd||!PicaVisual.trusted(pkg));
        if(!canvasSurface||!budgetedSplash)startupProbe.cancelSurfaceRetry();
        visualInteresting=reviewing||(splash?hasAd||volunteer||oopzSurface:panelEvidence);
        if(!splash&&chargePanel&&panelDelay>0){queueKnownRead(now,now+Math.max(panelDelay,pixelDelay(now)),false);return false;}
        if((WmSplashVisual.applies(pkg)||WmSplashVisual.hasControl(nodes)||wmTouchReview.belongsTo(pkg))&&wmHint==null&&!oopzSurface) {
            boolean alreadyWaiting=wmTouchReview.awaitingTarget();
            if(startup&&wmTouchReview.missing(pkg,displayId(root),size.x,size.y,now)) {
                if(!alreadyWaiting)actionStep(pkg,"WM_REVIEW_GAP：暂未读到SDK按钮，清除旧目标和帧确认；保留剩余一次触摸预算，仅等待当前控件重新出现；不代表广告已关闭");
                visualStatus("WM复查暂停：当前无SDK跳过按钮，未点击；剩余观察="+wmTouchReview.remaining(now)+"ms；重新出现后需两帧核验");
                visualFollowup.clear();burstUntil=Math.max(burstUntil,now+1000);queue(450);return false;
            }
            visualStatus(wmTouchReview.belongsTo(pkg)?"WM开屏：此前已尝试触摸，复查时限或预算已结束；广告是否关闭仍未知":"WM开屏：未取得符合位置、文字和ID要求的SDK跳过控件，不点击");
            endVisualFollowup("当前未读到 SDK 跳过按钮");return false;
        }
        if(DelayedAdWatch.jm(pkg)&&!splashActions.finished())timedClose.caption(root.getWindowId(),jmCaption,now);
        if(DelayedAdWatch.jm(pkg)&&!ScanPacing.jmNeedsPixels(reviewing||timedClose.active(),jmCaption,hasAd)) {
            visualStatus("省电等待选线：未读到广告提示，不持续截屏；线路仍由你选择，界面事件到达后重新核验");return false;
        }
        if(DelayedAdWatch.jm(pkg)&&!reviewing&&timedClose.active()&&timedClose.nextProbeAt(now)>now){
            queue(timedClose.nextProbeAt(now)-now);return false;
        }
        if(splash) {
            if(!reviewing&&splashActions.finished()&&!picaNoticeWatch(pkg,now)){visualStatus("本次进入应用已尝试开屏画面点击，停止重复操作");return false;}
            if(!reviewing&&!JmVisual.context(pkg,startup,nodes)){visualStatus("未请求画面：当前不在广告观察阶段，或页面内容不适用");return false;}
            if(slowWatch&&delayedWatch.delay(now)>0) {
                visualStatus((DelayedAdWatch.jm(pkg)?"等待选线后的广告，线路请手动选择":"等待可能出现的下一条公告")+"；下次探测="+delayedWatch.delay(now)+"ms");
                queue(delayedWatch.delay(now));return false;
            }
            if(budgetedSplash) {
                sparseProbeWait=true;long planned=startupProbe.delay(now);
                if(planned<0){visualStatus("本次启动的画面探测计划已完成，未找到支持的目标");return false;}
                if(planned>0){queueStartupSlot();visualStatus("等待下一次启动画面探测，剩余="+planned+"ms");return false;}
            }
        } else if(!biliPanelContext) {
            if(!biliLaunchEligible){resetPixelProofs();endVisualFollowup("当前界面未再读到原广告上下文");visualStatus(panelEvidence?"B站暂停广告：本次面板有限探测结束，等待新广告证据":"未请求画面：无当前B站广告面板上下文，也不在开屏探测阶段");return false;}
            if(!reviewing&&!pixelPairPending){
                sparseProbeWait=true;long planned=startupProbe.delay(now);
                if(planned<0){visualStatus("B站开屏有限探测计划已完成，停止无标签画面读取");return false;}
                if(planned>0){queueStartupSlot();visualStatus("B站无标签开屏：等待下一次有限探测，剩余="+planned+"ms");return false;}
            }
        }
        now=SystemClock.uptimeMillis();boolean earlySurface=budgetedSplash&&startupProbe.surfaceRetryPending();
        long delay=earlySurface?visualReview.requestDelay(now,true):pixelDelay(now);
        if(delay>0){
            pixelOnly=true;visualStatus("等待画面读取频率限制，剩余="+delay+"ms");
            if(sparseProbeWait)queueStartupSlot();
            else if(delay<1500&&(now+delay<burstUntil||adFocus.rapid(now)||chargePanel||chargeRendered))queue(delay);
            return false;
        }
        if(budgetedSplash)startupProbe.requested(now);
        if(!splash&&!biliPanelContext&&!reviewing&&!pixelPairPending)startupProbe.requested(now);
        if(chargePanel){if(panelProbes.delay(root.getWindowId(),now)!=0)return false;panelProbes.requested(root.getWindowId(),now);}
        if(chargeRendered)renderProbes.requested(renderedControls,now);
        delayedWatch.requested(now);
        visualStatus("正在请求本地画面；本次启动的无文字探测次数="+startupProbe.count());
        int token=generation,windowId=root.getWindowId(),display=displayId(root);
        final String cacheScene=splash&&!confirmedWork(now)&&wmHint==null&&oopzLeftHint==null&&!volunteer&&!PicaVisual.trusted(pkg)&&!DelayedAdWatch.jm(pkg)
            ?pkg+"|"+token+"|"+windowId+"|"+display+"|"+size.x+"x"+size.y+"|"+canvasEligible+"|"+canvasNativeAd+"|"+oopzSurface+"|"+carrier:null;
        return captureVisual(new CapturePlan(pkg,splash,token,windowId,display,size,budgetedSplash&&canvasSurface,
            (screenshot,full)->VisualFrame.analyze(screenshot,splash,pkg,jmCaption,wmHint,volunteer,oopzSurface,oopzLeftHint,carrier,canvasEligible,canvasNativeAd,negativeFrames,full?cacheScene:null,biliHint,biliLaunchEligible,biliPanelContext,drawnPanelContext||renderedContext)),FULL,now);
    }
    private boolean captureVisual(CapturePlan plan,int lane,long now){
        int token=plan.generation,serial=++visualSerial,windowId=plan.window,display=plan.display;
        captureWindow=windowId;
        Point requestedSize=plan.size;String pkg=plan.pkg;boolean splash=plan.splash;
        visualReview.requested(now);frameRequests++;pixelOnly=true;capturingVisual=true;busy=true;
        try {
            takeScreenshot(display,visionWorker,new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult screenshot) {
                    if(token!=generation||serial!=visualSerial||prefs==null||!prefs.getBoolean("running",true)
                        ||!visualEnabled(pkg,splash)) {screenshot.getHardwareBuffer().close();return;}
                    try {
                        if(!visualWindow(pkg,windowId,display,requestedSize,false,false,false)) {
                            String why=visualWindowMismatch;screenshot.getHardwareBuffer().close();handler.post(()->visualCaptureFailed(token,serial,"画面校验取消："+why+"；未读取或点击"));return;
                        }
                    } catch(RuntimeException error) {
                        screenshot.getHardwareBuffer().close();handler.post(()->visualCaptureFailed(token,serial,"画面所属窗口校验异常"));return;
                    }
                    VisualFrame.Data frame=plan.reader.read(screenshot,lane==FULL);
                    handler.post(()-> {
                        try{visualFrameReady(plan,serial,lane,frame);}
                        catch(RuntimeException error){visualCaptureFailed(token,serial,"画面回调处理异常："+error.getClass().getSimpleName());}
                        finally{scheduleWatch();}
                    });
                }
                @Override public void onFailure(int error) {
                    handler.post(()->visualCaptureFailed(token,serial,"SCREENSHOT_ERROR_"+error
                        +(error==6?"：系统保护内容，不读取该画面":error==2?"：系统未授予画面读取能力，请重新开JUMP无障碍":"")));
                }
            });
            handler.postDelayed(()-> {
                if(token==generation&&serial==visualSerial&&capturingVisual) {
                    visualCaptureFailed(token,serial,"VISUAL_CAPTURE_TIMEOUT：2秒内未得到画面，未点击");
                    visualSerial++;
                }
            },2000);
            return true;
        } catch(RuntimeException error) {visualCaptureFailed(token,serial,"画面请求失败："+error.getClass().getSimpleName());return false;}
    }
    private long pixelDelay(long now){return visualReview.requestDelay(now,pixelPairPending);}
    private void queueVisualRetry() {
        long now=SystemClock.uptimeMillis();
        boolean bili="tv.danmaku.bili".equals(foreground);
        if(!visualEnabled(!bili))return;
        long panelDelay=bili?panelProbes.delay(captureWindow,now):-1;
        if(panelDelay>=0)queueKnownRead(now,now+Math.max(panelDelay,pixelDelay(now)),false);
        else if((!splashActions.finished()||wmTouchReview.active()||picaNoticeWatch(foreground,now))&&visualStartup(now))queue(500);
    }
    private void visualCaptureFailed(int token,int serial,String reason) {
        if(token!=generation||serial!=visualSerial)return;
        capturingVisual=false;busy=false;resetPixelProofs();endVisualFollowup("复查画面不可用");visualStatus(reason);queueVisualRetry();
        scheduleWatch();
    }
    private void visualFrameReady(CapturePlan plan,int serial,int lane,VisualFrame.Data frame) {
        String pkg=plan.pkg;boolean splash=plan.splash;int token=plan.generation,windowId=plan.window,display=plan.display;Point size=plan.size;
        if(token!=generation||serial!=visualSerial)return;
        capturingVisual=false;busy=false;
        if(frame.error!=null){resetPixelProofs();endVisualFollowup("复查画面处理失败");visualStatus("画面处理失败："+frame.error);queueVisualRetry();return;}
        long now=SystemClock.uptimeMillis(),age=now-frame.timestamp;
        if(!visualFollowup.current(pkg,windowId,now))endVisualFollowup("原窗口已变化或复查到期");
        if(!VisualReview.currentFrame(age,frame.width,frame.height,size.x,size.y)) {
            resetPixelProofs();endVisualFollowup("复查画面过期或尺寸变化");
            visualStatus("VISUAL_STALE：画面尺寸已变化或帧过期，年龄="+age+"ms，图形分析="+frame.analysisMs+"ms，未点击");queueVisualRetry();return;
        }
        BiliVisual.Result result=frame.result;pixelPairPending=false;
        if(!splash&&!frame.biliLaunch&&result.found()&&(panelProbes.attemptedIn(windowId)
            ||frame.renderedPanel&&renderProbes.attempted()&&visualFollowup.current(pkg,windowId,now))){
            resetPixelProofs();endVisualFollowup("复查仍匹配广告面板图形");visualStatus("B站暂停广告：仍匹配面板图形，本次已尝试，停止重复点击");return;
        }
        if(result.found()&&(lane==CANVAS_PAIR&&!frame.canvasSkip||lane==BILI_LAUNCH_PAIR&&!frame.biliLaunch
            ||lane==BILI_PANEL_PAIR&&(frame.biliLaunch||!result.requiresPair)||lane==JM_CLOCK&&!frame.timedClose)){
            resetPixelProofs();visualStatus("轻量复查目标类型已变化，等待完整扫描；未点击");queueVisualRetry();return;
        }
        if(!splash&&result.found()&&result.requiresPair) {
            if(frame.renderedPanel)visualInteresting=true;
            CanvasSkipVisual.Proof proof=frame.renderedPanel?renderedPairProof:biliPairProof;
            if(frame.renderedPanel)biliPairProof.reset();else renderedPairProof.reset();
            if(!proof.confirm(pkg,windowId,display,frame.width,frame.height,result,frame.timestamp,now)) {
                pixelPairPending=true;visualStatus(result.reason+"；等待第二张同窗口新画面，图形分析="+frame.analysisMs+"ms");
                burstUntil=Math.max(burstUntil,now+1500);
                if(lane==FULL)scheduleLightProbe(plan,frame.biliLaunch?BILI_LAUNCH_PAIR:BILI_PANEL_PAIR,now+Math.max(120,pixelDelay(now)),now);
                else queue(Math.max(120,pixelDelay(now)));return;
            }
        } else {biliPairProof.reset();renderedPairProof.reset();}
        if(result.found()||frame.picaWaiting)adActiveUntil=now+2000;
        if(frame.canvasSkip||frame.biliLaunch)visualInteresting=true;
        // A retry has its own consecutive-frame proof; do not make it wait for two nested pairs.
        if(!frame.wmPair||wmTouchReview.belongsTo(pkg))wmPairProof.reset();
        else if(!wmPairProof.confirm(pkg,windowId,display,frame.width,frame.height,frame.wmHint,result,frame.timestamp,now)) {
            pixelPairPending=true;visualStatus("WM：SDK控件和当前跳过字形已匹配，等待第二张同位置新画面；图形分析="+frame.analysisMs+"ms");
            burstUntil=Math.max(burstUntil,now+1500);queue(Math.max(120,pixelDelay(now)));return;
        }
        if(frame.canvasSkip) {
            if(!result.found())canvasProof.reset();
            else if(!canvasProof.confirm(pkg,windowId,display,frame.width,frame.height,result,frame.timestamp,now)) {
                pixelPairPending=true;visualInteresting=true;visualStatus("通用绘制跳过：当前广告证据及跳过布局已匹配，等待第二张当前窗口新画面；"+result.reason);
                burstUntil=Math.max(burstUntil,now+1500);
                if(lane==FULL&&!WmSplashVisual.applies(pkg)&&!frame.oopzCarrier)scheduleLightProbe(plan,CANVAS_PAIR,now+Math.max(120,pixelDelay(now)),now);
                else queue(Math.max(120,pixelDelay(now)));return;
            }
        } else canvasProof.reset();
        if(frame.oopzSurface) {
            if(!result.found())surfaceProof.reset();
            else if(!surfaceProof.confirm(pkg,windowId,display,frame.width,frame.height,result,frame.timestamp,now,frame.oopzCarrier)) {
                pixelPairPending=true;visualStatus((frame.oopzBottom?(frame.oopzLeftHint!=null?"OOPZ左下角：当前广告标识、独立标签、跳过两字、倒计时及菜单已匹配":"OOPZ左下角：当前跳过两字、倒计时、广告徽标及顶部菜单已匹配"):frame.oopzHeader?"OOPZ页头：跳过两字、后缀数字及圆环/圆点菜单已匹配":frame.oopzLeftHint!=null?"OOPZ左上：当前标签、跳过两字和右侧倒计时已匹配":"OOPZ：SDK控件缺失，当前帧已匹配倒计时、分隔线和跳过两字")+"，等待第二张同位置新画面；图形分析="+frame.analysisMs+"ms");
                burstUntil=Math.max(burstUntil,now+1500);queue(Math.max(120,pixelDelay(now)));return;
            }
        }
        if(frame.kind==SplashActions.PICA_NOTICE) {
            splashActions.noticeVisible();
            if(splashActions.allowsNotice(frame.noticeSignature,windowId))visualReview.clearScene();
            else {visualStatus("公告已尝试或本次两条公告预算已用完；已尝试="+splashActions.noticeCount()+"，未重复点击");queueVisualRetry();return;}
        } else if(PicaVisual.trusted(pkg))splashActions.noticeAbsent(now);
        if(frame.picaScene){picaSequence=true;visualInteresting=true;timedClose.clear("已确认其他广告阶段");}
        if(result.found()&&(!splash||frame.timedClose||frame.circularSkip||frame.wmHint!=null||visualInteresting))visualInteresting=true;
        if(frame.kind!=visualKind){visualReview.clearScene();visualKind=frame.kind;}
        if(frame.picaWaiting) {
            if(splashActions.finished()) {
                visualStatus("点击后未再看到左上角X；实际广告状态未知，停止重复操作");endVisualFollowup("当前画面未再次匹配关闭图形");return;
            }
            visualStatus(frame.result.reason+"；约450ms后继续探测，未提交点击");
            burstUntil=Math.max(burstUntil,Math.min(enteredAt+StartupProbe.WINDOW,now+4500));
            queue(450);return;
        }
        int x=Math.round(result.x*size.x/(float)frame.analysisWidth),y=Math.round(result.y*size.y/(float)frame.analysisHeight);
        final int wordX=x,wordY=y;
        if((frame.canvasSkip||frame.oopzSurface||frame.biliLaunch)&&result.found())x=Math.round(WmSplashVisual.touchX(result,false)*size.x/(float)frame.analysisWidth);
        boolean wmRetry=false;
        if(frame.wmHint!=null) {
            if(!result.found()&&wmTouchReview.active()) {
                wmTouchReview.pause(now);visualFollowup.clear();
                visualStatus("WM复查暂停：当前帧未看清跳过两字，未点击；剩余观察="+wmTouchReview.remaining(now)+"ms；需重新取得两帧完整目标");
                burstUntil=Math.max(burstUntil,now+1000);queue(450);return;
            }
            else if(result.found()) {
                WmTouchReview.Decision wm=wmTouchReview.observe(pkg,windowId,display,frame.wmHint,wordX,wordY,frame.timestamp,now);
                if(wm==WmTouchReview.Decision.WAIT) {
                    visualStatus("WM复查：当前窗口的SDK按钮和跳过文字已匹配，等待至少1800ms及两张连续新画面后补一次；已尝试="+wmTouchReview.attempts()+"，剩余观察="+wmTouchReview.remaining(now)+"ms");
                    queue(Math.max(120,Math.min(450,Math.max(wmTouchReview.delay(now),pixelDelay(now)))));return;
                }
                if(wm==WmTouchReview.Decision.STOP) {
                    actionStep(pkg,"WM_REVIEW_STOP：重复次数、时限、窗口或目标位置不符合补点条件，不再点击");
                    visualStatus("WM复查结束：停止重复触摸，实际广告状态仍需确认");endVisualFollowup("SDK 的独立复查已结束");return;
                }
                wmRetry=wm==WmTouchReview.Decision.RETRY;
                x=Math.round(WmSplashVisual.sdkTouchX(pkg,result,wmRetry)*size.x/(float)frame.analysisWidth);
            }
        }
        if(frame.timedClose&&!splashActions.finished()) {
            long wait=timedClose.remaining(windowId,x,y,Math.max(3,size.x/100),now);
            boolean jm=DelayedAdWatch.jm(pkg);
            if(wait>0||jm&&!timedClose.readyFrame(frame.timestamp)) {
                visualStatus("已定位红色X，等待广告计时结束；剩余="+wait+"ms；"+timedClose.describe(now));
                burstUntil=Math.max(burstUntil,Math.min(enteredAt+StartupProbe.WINDOW,now+wait+1000));
                if(jm)scheduleLightProbe(plan,JM_CLOCK,Math.max(timedClose.nextProbeAt(now),now+Math.max(120,pixelDelay(now))),now);
                else queue(Math.max(120,Math.min(500,Math.max(wait,pixelDelay(now)))));return;
            }
            visualStatus("广告计时已结束，当前红X已确认，立即校验点击；"+timedClose.describe(now));
        } else if(timedClose.active()) {
            boolean kept=timedClose.missing(windowId,now);
            visualStatus((kept?"本帧未确认红色X，保留计时，禁止按旧画面点击；":"红色X等待已重置；")+timedClose.describe(now));
            if(kept) {visualInteresting=true;burstUntil=Math.max(burstUntil,now+1000);queue(500);return;}
        }
        VisualReview.Action decision=wmRetry?VisualReview.Action.READY:visualReview.observe(result.found());
        String detail=result.reason+"；画面年龄="+age+"ms，图形分析="+frame.analysisMs+"ms，分析尺寸="+frame.analysisWidth+"×"+frame.analysisHeight
            +(result.found()?"，实际目标中心=("+x+","+y+")":"");
        if(decision==VisualReview.Action.NO_TARGET) {
            boolean wasReview=visualFollowup.pending();
            visualStatus((wasReview?"画面复查未再匹配广告目标，实际关闭结果未核实；":"未定位到目标；")+detail);
            endVisualFollowup("当前画面未再次匹配广告目标");
            if(lane==FULL&&plan.strongSurfaceDiscovery&&startupProbe.retrySurface(windowId,now)){
                sparseProbeWait=true;queueStartupSlot();
                visualStatus("当前全屏广告已确认，跳过字形尚未匹配；一次提前复查，使用未用的常规探测名额");return;
            }
            long panelNext=!splash?panelProbes.delay(windowId,now):-1;
            if(panelNext>=0){queueKnownRead(now,now+Math.max(panelNext,pixelDelay(now)),false);return;}
            long wait=pixelDelay(now);if(now+wait<burstUntil||adFocus.rapid(now))queue(Math.max(ScanPacing.followup(confirmedWork(now)),wait));return;
        }
        if(decision==VisualReview.Action.ALREADY_ATTEMPTED){
            visualStatus("原广告图形仍在，本次已尝试，停止重复点击；"+detail);
            endVisualFollowup("复查仍匹配广告目标");
            return;
        }
        if((splash||frame.biliLaunch)&&!wmRetry&&!splashActions.allows(frame.kind)) {
            visualStatus("该阶段已经尝试，停止重复点击；"+detail);queueVisualRetry();return;
        }
        long finalCheckStarted=SystemClock.uptimeMillis();
        int labelX=result.adLabelX<0?-1:Math.round(result.adLabelX*size.x/(float)frame.analysisWidth);
        int labelY=result.adLabelY<0?-1:Math.round(result.adLabelY*size.y/(float)frame.analysisHeight);
        String cancelReason="";
        if(!visualEnabled(pkg,splash))cancelReason="画面处理设置已关闭";
        else if(!visualWindow(pkg,windowId,display,size,true,frame.timedClose||frame.picaScene||frame.circularSkip,
            frame.requireCaption,frame.wmHint,frame.volunteer,frame.oopzSurface,frame.oopzLeftHint,
            frame.canvasSkip,frame.canvasNativeAd,frame.biliHint,frame.biliLaunch,frame.renderedPanel,x,y,labelX,labelY))
            cancelReason=visualWindowMismatch.isEmpty()?"当前广告上下文复查未通过":visualWindowMismatch;
        else if(!VisualReview.currentFrame(SystemClock.uptimeMillis()-frame.timestamp,frame.width,frame.height,size.x,size.y))
            cancelReason="最终画面已过期或显示尺寸变化";
        if(!cancelReason.isEmpty()) {
            String anchor=frame.renderedPanel?"；关闭=("+x+","+y+")，画面广告标识=("+labelX+","+labelY+")":"";
            visualStatus("VISUAL_CANCELLED："+cancelReason+"；未点击，最终画面年龄="+(SystemClock.uptimeMillis()-frame.timestamp)
                +"ms"+anchor+"；"+rootWindowDiagnostic);return;
        }
        String key=pkg+"|visual|"+x+","+y+(frame.kind==SplashActions.PICA_NOTICE?"|notice-window="+windowId:"");now=SystemClock.uptimeMillis();
        detail+="；取帧方式="+(lane==FULL?"完整扫描后请求":lane==JM_CLOCK?"按计时点轻量核验后请求":"第二帧轻量核验后请求")+"，最终完整复查="+(now-finalCheckStarted)+"ms";
        boolean nextNotice=PicaVisual.trusted(pkg)&&frame.kind==SplashActions.PICA_NOTICE&&splashActions.nextNotice(frame.noticeSignature,windowId);
        if(!(nextNotice?gate.allowsNextNotice(key,now):gate.allows(key,now))){visualStatus("视觉目标受到点击频率限制，未点击");queueVisualRetry();return;}
        endVisualFollowup("进入新的动作核验");
        visualReview.markAttempt();if(splash||frame.biliLaunch) {
            if(frame.kind==SplashActions.PICA_NOTICE)splashActions.markNotice(frame.noticeSignature,windowId);else splashActions.mark(frame.kind);
        }gate.record(key,now);busy=true;
        if(!splash&&!frame.biliLaunch)panelProbes.attempted(windowId);
        if(frame.renderedPanel)renderProbes.markAttempt();
        if(frame.timedClose&&DelayedAdWatch.jm(pkg))timedClose.clear("已提交红X动作，下一帧只作结果观察");
        if(frame.wmHint!=null)wmTouchReview.mark(pkg,windowId,display,frame.wmHint,wordX,wordY,frame.timestamp,now);
        if(!wmRetry) {actionPackage=pkg;actionDiagnostic="JUMP 0.5.35\n应用："+pkg+"，窗口="+windowId
            +"\n目标方式="+(splash?(frame.kind==SplashActions.PICA_NOTICE?"哔咔第"+splashActions.noticeCount()+"条公告，不再顯示":frame.kind==SplashActions.PICA_CLOSE?"哔咔开屏，左上角白色圆圈X已出现":frame.timedClose?"广告计时结束后关闭当前红X，不重复等待":frame.circularSkip?"本地匹配当前圆环与跳过两字":"本地匹配开屏‘跳过’字形"):frame.biliLaunch?"B站全屏开屏：两帧核验右下跳过和倒计时":"本地像素识别广告面板X，即时点击")+"\n实际目标中心=("+x+","+y+")";}
        if(wmRetry)actionStep(pkg,"WM_RETRY_CONFIRMED：已重新取得当前窗口="+windowId+"的SDK控件和两张连续新画面；剩余预算内补一次触摸，不复用旧窗口目标；位置=("+x+","+y+")");
        actionStep(actionPackage,"VISUAL_CONFIRMED："+(splash?"已复查开屏广告上下文；":frame.biliLaunch?"已复查当前开屏窗口及两帧右下跳过目标；":"当前帧广告面板特征匹配，无需等待前往观看；")+detail);
        if(!splash&&result.requiresPair&&!frame.renderedPanel)actionStep(pkg,frame.biliLaunch?"BILI_LAUNCH_PAIR_CONFIRMED：当前窗口两张新画面的平台字标、向上箭头、右下跳过、倒计时及胶囊均匹配；最终窗口和启动上下文复查通过，只触摸过字内部，无原生/父节点点击":"BILI_PAIR_CONFIRMED：当前窗口两张新画面的X、三点、深色栏及拖动条均匹配；最终当前广告上下文及面板归属复查通过");
        if(frame.renderedPanel)actionStep(pkg,"RENDERED_PANEL_CONFIRMED：两张新画面均匹配面板关闭图形和独立「广告」标识；当前广告窗口与前台归属复查通过");
        if(frame.canvasSkip)actionStep(pkg,"CANVAS_PAIR_CONFIRMED：同一真实窗口的两张新画面已确认当前广告证据和跳过布局；最终当前页面核验通过；规则不依赖应用包名；"+result.reason);
        if(frame.wmPair)actionStep(pkg,"WM_PAIR_CONFIRMED：两张独立新画面已确认当前窗口的相同SDK控件及跳过字形；最终SDK位置复查通过");
        if(frame.oopzSurface)actionStep(pkg,"OOPZ_SURFACE_TOUCH_ONLY："+(frame.oopzBottom?(frame.oopzLeftHint!=null?"两帧均确认左下角跳过、后缀数字及顶部菜单，最终真实窗口、当前广告标识与标签位置复查通过":"两帧均确认左下角跳过、后缀数字、广告徽标及顶部菜单，最终真实窗口复查通过"):frame.oopzHeader?"两帧均确认跳过字形、后缀数字及下方菜单；跳过位置一致，最终前台窗口复查通过":frame.oopzLeftHint!=null?"两帧左上跳过字形与倒计时位置一致，最终广告标识和标签位置复查通过":"两帧当前倒计时/分隔线/跳过位置一致")+"，只触摸‘过’字内部；无控件、父节点点击或补点");
        if(frame.wmHint!=null)actionStep(actionPackage,"WM_TOUCH_ONLY：当前SDK控件再次核验通过；"+(wmRetry?"短触‘过’字内部":"短触‘跳’字内部")+"，时长="+WmSplashVisual.sdkTouchDuration(pkg,wmRetry)+"ms，避开字间隙；根读取="+rootReadMs+"ms；第"+wmTouchReview.attempts()+"次；无控件、父节点或原生补点");
        tapVisual(pkg,token,serial,windowId,display,x,y,frame.wmHint!=null,wmRetry,frame.oopzSurface);
    }
    private void tapVisual(String pkg,int token,int serial,int windowId,int display,int x,int y,boolean wm,boolean retry,boolean surface) {
        Path path=new Path();path.moveTo(x,y);final int gestureToken=++gestureSerial;
        GestureDescription gesture=new GestureDescription.Builder().setDisplayId(display)
            .addStroke(new GestureDescription.StrokeDescription(path,0,wm?WmSplashVisual.sdkTouchDuration(pkg,retry):surface?140:80)).build();
        boolean accepted=dispatchGesture(gesture,new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription gesture) {
                if(token!=generation||serial!=visualSerial||gestureToken!=gestureSerial)return;
                long now=SystemClock.uptimeMillis();
                busy=false;visualFollowup.begin(pkg,windowId,now);if(wm)wmTouchReview.completed();
                if(shakeSensors!=null){shake.clicked(pkg,now);stopShakeSensor();}
                LocalLog.recordAction(SkipService.this,pkg,"画面识别点击",false);
                actionStep(pkg,"VISUAL_GESTURE_COMPLETED：系统报告触摸完成，等待画面观察");
                LocalLog.add(SkipService.this,pkg+"：已执行画面目标位置的手势，实际关闭效果待确认");
                visualStatus("手势已完成，等待画面复查；窗口="+windowId+"，位置=("+x+","+y+")");
                burstUntil=now+(DelayedAdWatch.jm(pkg)?650:3000);
                queue(visualKind==SplashActions.PICA_NOTICE?Math.max(120,pixelDelay(now)):650);
            }
            @Override public void onCancelled(GestureDescription gesture) {
                if(token!=generation||serial!=visualSerial||gestureToken!=gestureSerial)return;
                busy=false;if(wm)wmTouchReview.stop();actionStep(pkg,"VISUAL_GESTURE_CANCELLED：系统取消触摸");visualStatus("系统取消视觉目标手势，停止本次点击");
            }
        },handler);
        if(!accepted){busy=false;if(wm)wmTouchReview.stop();actionStep(pkg,"VISUAL_GESTURE_REJECTED");visualStatus("系统未接受视觉目标手势");}
        else {
            actionStep(pkg,"VISUAL_GESTURE_SUBMITTED：系统接受画面目标触摸请求");
            handler.postDelayed(()-> {
                if(token==generation&&serial==visualSerial&&gestureToken==gestureSerial&&busy&&!capturingVisual) {
                    gestureSerial++;busy=false;if(wm)wmTouchReview.stop();visualStatus("视觉手势2秒内无回调，停止本次动作");
                }
            },2000);
        }
    }
    @Override public void onInterrupt() {handler.post(()->resetEnvironment("系统中断，重新核验运行环境"));}
    private void resetPixelProofs(){lightProbe=null;pixelPairPending=false;canvasProof.reset();biliPairProof.reset();renderedPairProof.reset();surfaceProof.reset();wmPairProof.reset();wmTouchReview.pause(SystemClock.uptimeMillis());}
    @Override public void onDestroy() {
        stopping=true;acceptingEvents=false;
        if(instance==this){instance=null;connected=false;}
        handler.post(()-> {cancel();scanner.quitSafely();});
        if(prefs!=null) {
            prefs.unregisterOnSharedPreferenceChangeListener(this);
            prefs.edit().putLong("service_ui",SystemClock.elapsedRealtimeNanos()).apply();
            ServiceRecovery.check(this);
        }
        if(receiverRegistered) unregisterReceiver(screenReceiver);
        super.onDestroy();
    }
}
