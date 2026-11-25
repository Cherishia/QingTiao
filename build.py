"""Dependency-free Android build. Python 3 + JDK 17 + Android SDK 35 build tools."""
import argparse
import os
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent

def run(*args):
    print('>', ' '.join(str(a) for a in args), flush=True)
    subprocess.run([str(a) for a in args], check=True, cwd=ROOT)

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--build-tools', required=True)
    parser.add_argument('--android-jar', required=True)
    parser.add_argument('--out', default=str(ROOT.parent/'QingTiao-0.5.45.apk'))
    parser.add_argument('--build-dir', default=str(ROOT/'build'))
    args=parser.parse_args()
    if not args.java_home: parser.error('--java-home or JAVA_HOME is required')
    jdk=Path(args.java_home); tools=Path(args.build_tools); android=Path(args.android_jar).resolve()
    exe='.exe' if os.name=='nt' else ''
    java=jdk/'bin'/('java'+exe); javac=jdk/'bin'/('javac'+exe)
    aapt=tools/('aapt2'+exe);align=tools/('zipalign'+exe)
    build=Path(args.build_dir).resolve();build.mkdir(exist_ok=True,parents=True)
    source=ROOT/'app/src/main';classes=build/'classes';gen=build/'generated';dex=build/'dex';test=build/'tests'
    for folder in (classes,gen,dex,test):folder.mkdir(exist_ok=True)
    policy=source/'java/dev/local/qingtiao'
    run(javac,'-encoding','UTF-8','-d',test,policy/'MatchPolicy.java',policy/'NativeSplashSkip.java',policy/'AdMatcher.java',policy/'ClickGate.java',policy/'ClickReview.java',policy/'EventInbox.java',ROOT/'tests/MatchPolicyTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.MatchPolicyTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,ROOT/'tests/TextPolicyParityTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.TextPolicyParityTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'MatchPolicy.java',policy/'AdMatcher.java',policy/'BiliPanel.java',policy/'PanelAdLabel.java',policy/'RenderedAdPanel.java',policy/'BiliVisual.java',policy/'VisualReview.java',policy/'VisualFollowup.java',policy/'SplashVisual.java',policy/'JmVisual.java',policy/'TimedClose.java',ROOT/'tests/BiliVisualTest.java',ROOT/'tests/SplashVisualTest.java',ROOT/'tests/JmVisualTest.java',policy/'StartupProbe.java',policy/'DelayedAdWatch.java',policy/'ReadingHistory.java',ROOT/'tests/StartupDiagnosticsTest.java',policy/'PicaVisual.java',policy/'SplashActions.java',ROOT/'tests/PicaVisualTest.java',policy/'SkipGlyphs.java',policy/'WmSplashVisual.java',ROOT/'tests/WmSplashVisualTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.BiliVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.SplashVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.JmVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.StartupDiagnosticsTest')
    run(java,'-cp',test,'dev.local.qingtiao.PicaVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.WmSplashVisualTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'WmTouchReview.java',ROOT/'tests/WmTouchReviewTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.WmTouchReviewTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'CircularSkipVisual.java',ROOT/'tests/CircularSkipVisualTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.CircularSkipVisualTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'VolunteerVisual.java',ROOT/'tests/VolunteerVisualTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.VolunteerVisualTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,ROOT/'tests/VolunteerInkTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.VolunteerInkTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'SecondsSuffix.java',policy/'OopzSurfaceVisual.java',ROOT/'tests/OopzSurfaceVisualTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.OopzSurfaceVisualTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'OopzLeftSkipVisual.java',ROOT/'tests/OopzLeftSkipVisualTest.java',policy/'MiniAdBadge.java',policy/'OopzHeaderSkipVisual.java',ROOT/'tests/OopzHeaderSkipVisualTest.java',ROOT/'tests/OopzBottomVisualTest.java',ROOT/'tests/OopzBottomLabelTest.java',policy/'RootWindows.java',ROOT/'tests/RootWindowsTest.java',policy/'OopzOrigin.java',ROOT/'tests/OopzOriginTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.OopzLeftSkipVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.OopzHeaderSkipVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.OopzBottomVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.OopzBottomLabelTest')
    run(java,'-cp',test,'dev.local.qingtiao.RootWindowsTest')
    run(java,'-cp',test,'dev.local.qingtiao.OopzOriginTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'CanvasSkipVisual.java',policy/'FooterSkipVisual.java',policy/'PlainSkipVisual.java',policy/'AdSurface.java',policy/'TreeReadGate.java',policy/'FrameIdentity.java',policy/'BiliLaunchVisual.java',ROOT/'tests/BiliLaunchVisualTest.java',ROOT/'tests/CanvasSkipVisualTest.java',ROOT/'tests/DiscoverySpeedTest.java',ROOT/'tests/AdImageSkipTest.java',ROOT/'tests/SplashLatencyTest.java',policy/'WindowTransitionWatch.java',ROOT/'tests/WindowTransitionWatchTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.CanvasSkipVisualTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'MiniProgramSkipVisual.java',ROOT/'tests/MiniProgramSkipVisualTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.MiniProgramSkipVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.BiliLaunchVisualTest')
    run(java,'-cp',test,'dev.local.qingtiao.DiscoverySpeedTest')
    run(java,'-cp',test,'dev.local.qingtiao.AdImageSkipTest')
    run(java,'-cp',test,'dev.local.qingtiao.SplashLatencyTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'AdFocus.java',policy/'PanelProbeBudget.java',policy/'RenderedPanelProbes.java',ROOT/'tests/PanelProbeBudgetTest.java',ROOT/'tests/AdFocusTest.java',ROOT/'tests/BiliRobustTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.AdFocusTest')
    run(java,'-cp',test,'dev.local.qingtiao.PanelProbeBudgetTest')
    run(java,'-cp',test,'dev.local.qingtiao.BiliRobustTest')
    run(java,'-cp',test,'dev.local.qingtiao.WindowTransitionWatchTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,ROOT/'tests/WmInkTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.WmInkTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'ScanPacing.java',ROOT/'tests/ScanPacingTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.ScanPacingTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'NegativeFrameCache.java',ROOT/'tests/NegativeFrameCacheTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.NegativeFrameCacheTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,ROOT/'tests/PowerPolicyTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.PowerPolicyTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'AdShake.java',ROOT/'tests/AdShakeTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.AdShakeTest')
    run(javac,'-encoding','UTF-8','-d',test,policy/'ExcludedAppLaunchGate.java',ROOT/'tests/ExcludedAppLaunchGateTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.ExcludedAppLaunchGateTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'RecoveryState.java',ROOT/'tests/RecoveryStateTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.RecoveryStateTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'ModeReminder.java',ROOT/'tests/ModeReminderTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.ModeReminderTest')
    run(javac,'-encoding','UTF-8','-cp',test,'-d',test,policy/'UpdateReleasePolicy.java',ROOT/'tests/UpdateReleasePolicyTest.java')
    run(java,'-cp',test,'dev.local.qingtiao.UpdateReleasePolicyTest')
    run(aapt,'compile','--dir',source/'res','-o',build/'resources.zip')
    assets=source/'assets'
    asset_args=['-A',assets] if assets.is_dir() else []
    run(aapt,'link','-o',build/'resources.apk','-I',android,'--manifest',source/'AndroidManifest.xml',
        '--java',gen,*asset_args,'--auto-add-overlay',build/'resources.zip')
    sources=list((source/'java').rglob('*.java'))+list(gen.rglob('*.java'))
    # A response file keeps command length below the Windows limit.
    response=build/'javac.args'
    response.write_text('\n'.join('"'+str(p).replace('\\','/')+'"' for p in sources),encoding='utf8')
    bootclasspath=str(android)+os.pathsep+str(tools/'core-lambda-stubs.jar')
    run(javac,'-encoding','UTF-8','-source','8','-target','8','-bootclasspath',bootclasspath,'-d',classes,'@'+str(response))
    with zipfile.ZipFile(build/'classes.jar','w',zipfile.ZIP_DEFLATED) as z:
        for p in classes.rglob('*.class'):z.write(p,p.relative_to(classes).as_posix())
    run(java,'-cp',tools/'lib/d8.jar','com.android.tools.r8.D8','--lib',android,'--min-api','26',
        '--output',dex,build/'classes.jar')
    with zipfile.ZipFile(build/'resources.apk') as src,zipfile.ZipFile(build/'unsigned.apk','w') as dst:
        for item in src.infolist():dst.writestr(item,src.read(item.filename))
        for p in dex.glob('*.dex'):dst.write(p,p.name,compress_type=zipfile.ZIP_DEFLATED)
    run(align,'-f','4',build/'unsigned.apk',build/'aligned.apk')
    signing=ROOT/'signing';signing.mkdir(exist_ok=True);key=signing/'local-test.p12'
    password='qingtiao-local-v1'
    if not key.exists():
        run(jdk/'bin'/('keytool'+exe),'-genkeypair','-keystore',key,'-storetype','PKCS12',
            '-storepass',password,'-keypass',password,'-alias','qingtiao','-keyalg','RSA',
            '-keysize','2048','-validity','10000','-dname','CN=QingTiao Local Test')
    output=Path(args.out).resolve();output.parent.mkdir(parents=True,exist_ok=True)
    run(java,'-jar',tools/'lib/apksigner.jar','sign','--ks',key,'--ks-key-alias','qingtiao',
        '--ks-pass','pass:'+password,'--key-pass','pass:'+password,'--out',output,build/'aligned.apk')
    run(java,'-jar',tools/'lib/apksigner.jar','verify','--verbose','--print-certs',output)
    run(align,'-c','4',output)
    print('APK:',output)

if __name__=='__main__':main()
