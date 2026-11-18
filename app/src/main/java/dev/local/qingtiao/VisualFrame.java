package dev.local.qingtiao;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.hardware.HardwareBuffer;
import android.os.SystemClock;
import java.util.Arrays;

/** Holds only geometry/results after the temporary pixel buffers have been released. */
final class VisualFrame {
    static final class Data {
        int width,height,analysisWidth,analysisHeight;
        long timestamp,analysisMs;
        BiliVisual.Result result;
        String error;
        boolean reusedNegative,canvasSkip,canvasNativeAd,timedClose,picaScene,picaWaiting,requireCaption,circularSkip,volunteer,oopzSurface,oopzHeader,oopzBottom,oopzCarrier,wmPair,biliLaunch,renderedPanel;
        long noticeSignature;
        WmSplashVisual.Hint wmHint;
        BiliPanel.Hint biliHint;
        OopzLeftSkipVisual.Hint oopzLeftHint;
        int kind;
    }
    static Data analyzeRenderedPanel(AccessibilityService.ScreenshotResult screenshot,NegativeFrameCache negatives){
        return analyze(screenshot,false,"",false,null,false,false,null,false,false,false,
            negatives,null,null,false,true,true);
    }
    static Data analyze(AccessibilityService.ScreenshotResult screenshot,boolean splash,String pkg,boolean jmCaption,WmSplashVisual.Hint wmHint,boolean volunteer,boolean oopzSurface,OopzLeftSkipVisual.Hint oopzLeftHint,boolean carrier,boolean canvasEligible,boolean canvasNativeAd,NegativeFrameCache negatives,String cacheScene,BiliPanel.Hint biliHint,boolean biliLaunchEligible,boolean biliPanelContext,boolean drawnPanelContext) {
        Data d=new Data();d.timestamp=screenshot.getTimestamp();long started=SystemClock.uptimeMillis();
        HardwareBuffer buffer=screenshot.getHardwareBuffer();Bitmap hardware=null,software=null,small=null;int[] pixels=null;NegativeFrameCache.Sample sampled=null;
        try {
            hardware=Bitmap.wrapHardwareBuffer(buffer,screenshot.getColorSpace());
            if(hardware==null){d.error="无法读取图像缓冲";return d;}
            d.width=hardware.getWidth();d.height=hardware.getHeight();
            if(d.width<240||d.height<d.width*1.4||d.height>d.width*3.0||d.width*(long)d.height>16000000){d.error="画面尺寸或方向不适用";return d;}
            software=hardware.copy(Bitmap.Config.ARGB_8888,false);
            if(software==null){d.error="无法转为本地图形数据";return d;}
            int aw=Math.min(720,d.width),ah=Math.round(d.height*(aw/(float)d.width));
            small=aw==d.width?software:Bitmap.createScaledBitmap(software,aw,ah,true);
            d.analysisWidth=aw;d.analysisHeight=ah;pixels=new int[aw*ah];small.getPixels(pixels,0,aw,0,0,aw,ah);
            if(cacheScene!=null) {
                sampled=negatives.sample(cacheScene,pixels,aw,ah,d.timestamp,SystemClock.uptimeMillis());
                if(sampled!=null&&sampled.reason!=null) {
                    d.result=new BiliVisual.Result(0,0,0,"静止画面短时复用未定位结果；"+sampled.reason);d.reusedNegative=true;return d;
                }
            } else negatives.clear();
            String canvasReason="";
            if(splash&&canvasEligible) {
                BiliVisual.Result canvas=CanvasSkipVisual.detect(pixels,aw,ah,canvasNativeAd);
                if(canvas.found()){d.result=canvas;d.canvasSkip=true;d.canvasNativeAd=canvasNativeAd;d.kind=SplashActions.SKIP;return d;}
                canvasReason=canvas.reason;
            }
            if(splash&&oopzSurface) {
                d.oopzSurface=true;d.oopzLeftHint=oopzLeftHint;d.oopzCarrier=carrier;
                d.oopzBottom=oopzLeftHint!=null&&oopzLeftHint.footer;
                d.result=carrier?new BiliVisual.Result(0,0,0,"OOPZ广告承载窗口：需当前菜单、广告徽标和跳过字形"):oopzLeftHint==null?OopzSurfaceVisual.detect(pixels,aw,ah):OopzLeftSkipVisual.detect(pixels,aw,ah,oopzLeftHint);
                if(!d.result.found()&&oopzLeftHint==null) {
                    BiliVisual.Result header=carrier&&!MiniAdBadge.detect(pixels,aw,ah)?new BiliVisual.Result(0,0,0,"未确认广告徽标"):OopzHeaderSkipVisual.detect(pixels,aw,ah);
                    if(header.found()){d.result=header;d.oopzHeader=true;}
                    else {
                        BiliVisual.Result bottom=OopzHeaderSkipVisual.detectBottom(pixels,aw,ah);
                        if(bottom.found()){d.result=bottom;d.oopzBottom=true;}
                        else d.result=new BiliVisual.Result(0,0,0,d.result.reason+"；"+header.reason+"；"+bottom.reason);
                    }
                }
                if(d.result.found())d.kind=SplashActions.SKIP;
            } else if(splash&&(wmHint!=null||WmSplashVisual.applies(pkg))) {
                d.wmHint=wmHint;WmSplashVisual.Detection match=WmSplashVisual.analyze(pixels,aw,ah,wmHint);d.result=match.target;d.wmPair=match.requiresPair;
                if(d.result.found())d.kind=SplashActions.SKIP;
            } else if(splash&&DelayedAdWatch.jm(pkg)) {
                d.result=JmVisual.detect(pixels,aw,ah,jmCaption);d.timedClose=d.result.found();
                d.requireCaption=d.timedClose&&jmCaption;if(d.timedClose)d.kind=SplashActions.JM;
            } else if(splash&&volunteer&&VolunteerVisual.applies(pkg)) {
                d.volunteer=true;d.result=VolunteerVisual.detect(pixels,aw,ah);
                if(d.result.found()){d.circularSkip=true;d.kind=SplashActions.SKIP;}
            } else if(splash) {
                PicaVisual.Result pica=PicaVisual.detect(pixels,aw,ah,pkg);
                d.result=pica.target;d.kind=pica.kind;d.picaWaiting=pica.waiting;
                d.picaScene=pica.waiting||pica.kind==SplashActions.PICA_NOTICE||pica.kind==SplashActions.PICA_CLOSE;
                if(pica.kind==SplashActions.PICA_NOTICE)d.noticeSignature=PicaVisual.noticeSignature(pixels,aw,ah);
                if(!d.result.found()&&!d.picaWaiting) {
                    d.result=JmVisual.detect(pixels,aw,ah);d.timedClose=d.result.found();
                    if(d.timedClose)d.kind=SplashActions.JM;
                    else {d.result=SplashVisual.detect(pixels,aw,ah);if(d.result.found())d.kind=SplashActions.SKIP;}
                    if(!d.result.found()) {d.result=CircularSkipVisual.detect(pixels,aw,ah);if(d.result.found()){d.kind=SplashActions.SKIP;d.circularSkip=true;}}
                    if(!d.result.found()&&PicaVisual.trusted(pkg))d.result=pica.target;
                }
            } else {
                d.biliHint=biliHint;
                boolean checkDrawn=drawnPanelContext&&(biliPanelContext||biliLaunchEligible);
                if(checkDrawn){
                    d.result=RenderedAdPanel.detect(pixels,aw,ah);
                    d.renderedPanel=d.result.found();
                } else if(biliPanelContext)d.result=BiliVisual.detect(pixels,aw,ah,biliHint);
                else d.result=new BiliVisual.Result(0,0,0,"未读到当前B站底部面板上下文");
                if(!d.result.found()&&biliLaunchEligible) {
                    BiliVisual.Result launch=BiliLaunchVisual.detect(pixels,aw,ah);
                    if(launch.found()){d.result=launch;d.biliHint=null;d.biliLaunch=true;d.kind=SplashActions.SKIP;}
                    else if(checkDrawn||biliPanelContext)d.result=new BiliVisual.Result(0,0,0,"B站面板："+d.result.reason+"；"+launch.reason);
                    else d.result=launch;
                }
            }
            if(!canvasReason.isEmpty()&&!d.result.found())d.result=new BiliVisual.Result(0,0,0,canvasReason+"；"+d.result.reason);
            return d;
        } catch(RuntimeException error){d.error=error.getClass().getSimpleName();return d;}
        catch(OutOfMemoryError error){d.error="画面处理内存不足";return d;}
        finally {
            if(!d.reusedNegative)negatives.remember(sampled,d.result==null?null:d.result.reason,d.error==null&&d.result!=null&&!d.result.found()&&!d.picaScene&&!d.picaWaiting&&!d.timedClose);
            d.analysisMs=SystemClock.uptimeMillis()-started;
            if(pixels!=null)Arrays.fill(pixels,0);
            if(small!=null&&small!=software)small.recycle();
            if(software!=null)software.recycle();if(hardware!=null)hardware.recycle();buffer.close();
        }
    }
    private VisualFrame(){}
}
