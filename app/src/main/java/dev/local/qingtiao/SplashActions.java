package dev.local.qingtiao;

/** Two separate notice appearances may accompany one final startup close. */
public final class SplashActions {
    public static final int NONE=0,SKIP=1,JM=2,PICA_NOTICE=3,PICA_CLOSE=4;
    private boolean closed,noticeGone;
    private int notices;
    private int noticeWindow=-1;
    private long noticeSignature,missingSince=-1;
    public boolean noticeBudget(){return notices<2;}
    public int noticeCount(){return notices;}
    public boolean allows(int kind){return kind==PICA_NOTICE?noticeBudget():kind!=NONE&&!closed;}
    public boolean allowsNotice(long signature){return allowsNotice(signature,noticeWindow);}
    public boolean allowsNotice(long signature,int window){return noticeBudget()&&(notices==0||window!=noticeWindow||noticeGone||Long.bitCount(signature^noticeSignature)>8);}
    public boolean nextNotice(long signature,int window){return notices==1&&allowsNotice(signature,window);}
    public void noticeVisible(){missingSince=-1;}
    public void noticeAbsent(long now){
        if(notices==0)return;
        if(missingSince<0||now<missingSince)missingSince=now;
        else if(now-missingSince>=700)noticeGone=true;
    }
    public void markNotice(long signature){markNotice(signature,0);}
    public void markNotice(long signature,int window){notices++;noticeSignature=signature;noticeWindow=window;noticeGone=false;missingSince=-1;}
    public void mark(int kind){if(kind==PICA_NOTICE)markNotice(0);else if(kind!=NONE)closed=true;}
    public boolean finished(){return closed;}
    public void reset(){notices=0;closed=false;noticeGone=false;missingSince=-1;noticeSignature=0;noticeWindow=-1;}
}
