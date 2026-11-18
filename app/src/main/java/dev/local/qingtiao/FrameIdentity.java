package dev.local.qingtiao;

/** The guard remains exact; diagnostics distinguish legitimate cancellations. */
public final class FrameIdentity {
    public static String mismatch(String expected,int win,int display,int w,int h,String actual,int currentWin,int currentDisplay,int cw,int ch){
        String before=expected+"/窗口"+win+"/屏"+display+"/"+w+"×"+h,after=actual+"/窗口"+currentWin+"/屏"+currentDisplay+"/"+cw+"×"+ch;
        if(!expected.equals(actual))return "前台应用变化：目标="+before+"，当前="+after;
        if(win!=currentWin)return "同应用窗口已替换：目标="+before+"，当前="+after;
        if(display!=currentDisplay)return "显示屏变化：目标="+before+"，当前="+after;
        if(w!=cw||h!=ch)return "显示尺寸变化：目标="+before+"，当前="+after;
        return "";
    }
    private FrameIdentity(){}
}
