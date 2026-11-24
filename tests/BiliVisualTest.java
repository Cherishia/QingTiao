package dev.local.qingtiao;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public final class BiliVisualTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        int w=575,h=1280;
        BiliVisual.Result found=BiliVisual.detect(fixture(w,h,0,true,true,true,.94,.343),w,h);
        check(found.found()&&Math.abs(found.x-w*.94)<3&&Math.abs(found.y-h*.343)<3,"synthetic matching X with all visual context");
        for(int shape=1;shape<=5;shape++)check(!BiliVisual.detect(fixture(w,h,shape,true,true,true,.94,.343),w,h).found(),"non-X shape rejected: "+shape);
        check(BiliVisual.detect(fixture(w,h,0,false,true,true,.94,.343),w,h).found(),"X closes before bottom pink CTA is present");
        int[] noHandle=fixture(w,h,0,false,true,true,.94,.343);
        for(int y=(int)(h*.343-w*.13);y<h*.343-w*.03;y++)Arrays.fill(noHandle,y*w+(int)(w*.4),y*w+(int)(w*.6),0xff17181a);
        check(!BiliVisual.detect(noHandle,w,h).found(),"a right-edge X and menu outside the pictured bottom sheet structure is rejected");
        check(!BiliVisual.detect(fixture(w,h,0,true,false,true,.94,.343),w,h).found(),"X without neighboring three dots rejected");
        check(!BiliVisual.detect(fixture(w,h,0,true,true,false,.94,.343),w,h).found(),"X outside dark header rejected");
        check(!BiliVisual.detect(fixture(w,h,0,true,true,true,.80,.343),w,h).found(),"non-right-edge X rejected");
        check(!BiliVisual.detect(fixture(w,h,0,true,true,true,.94,.12),w,h).found(),"player/status area X rejected");
        check(BiliVisual.detect(fixture(w,h,0,true,true,true,.93,.46),w,h).found(),"live X can move instead of using screenshot fixed coordinates");
        check(!BiliVisual.detect(null,w,h).found()&&!BiliVisual.detect(new int[10],w,h).found(),"invalid pixel payload rejected");
        check(!BiliVisual.detect(new int[1280*575],1280,575).found(),"landscape screenshot rejected");
        int[] two=fixture(w,h,0,true,true,true,.94,.343);symbol(two,w,h,0,.94,.46,true);
        check(!BiliVisual.detect(two,w,h).found(),"two equally valid X patterns are ambiguous");
        check(BiliVisual.detect(fixture(720,1602,0,true,true,true,.94,.343),720,1602).found(),"analysis-size generated frame supported");
        check(!RenderedAdPanel.detect(fixture(w,h,0,true,true,true,.94,.343),w,h).found(),
            "drawn sheet geometry and a pink CTA alone cannot establish an ad");
        AdMatcher.Node root=new AdMatcher.Node(-1,0,0,w,h,"","");
        AdMatcher.Node renderer=new AdMatcher.Node(0,0,65,w,h,"","");
        renderer.className="android.webkit.WebView";
        List<AdMatcher.Node> blankTree=Arrays.asList(root,renderer);
        check(!BiliPanel.canProbe(blankTree,w,h)&&BiliPanel.drawnContext(blankTree,w,h),"unlabelled renderer takes only the drawn discovery path");
        check(BiliPanel.drawnTargetProblem(blankTree,w,h,540,439,-1,-1).isEmpty()&&!BiliPanel.drawnTargetProblem(blankTree,w,h,500,439,-1,-1).isEmpty(),
            "final drawn target must stay within the right edge of the title bar");
        check(!BiliPanel.drawnContext(Arrays.asList(root),w,h),"blank tree without a renderer cannot start a probe");
        renderer.desc="评论";
        check(!BiliPanel.drawnTargetProblem(blankTree,w,h,540,439,-1,-1).isEmpty(),"ordinary accessible sheet cannot use the drawn fallback");
        renderer.desc="";
        renderer.editable=true;
        check(!BiliPanel.drawnTargetProblem(blankTree,w,h,540,439,-1,-1).isEmpty(),"editable renderer cannot authorize a drawn target");
        renderer.editable=false;renderer.scrollable=true;
        java.util.ArrayList<AdMatcher.Node> mixedTree=new java.util.ArrayList<>(blankTree);
        for(int i=1;i<88;i++)mixedTree.add(new AdMatcher.Node(0,20,620,160,680,i<25?"视频简介":"",""));
        check(!BiliPanel.pixelLabelContext(mixedTree,w,h)&&RenderedAdPanel.visibleControls(mixedTree,w,h)==88,
            "mixed labels and scrollable renderer from the reported scan can justify local frames without a native ad label");
        check(RenderedAdPanel.targetProblem(mixedTree,w,h,540,439,-1,-1,true).isEmpty()&&BiliPanel.drawnTargetProblem(mixedTree,w,h,540,439,-1,-1).isEmpty(),
            "both generic and Bili window guards can confirm the same drawn target after pixel proof");
        mixedTree.add(new AdMatcher.Node(0,20,421,160,450,"全部评论",""));
        check(!RenderedAdPanel.targetProblem(mixedTree,w,h,540,439,-1,-1,true).isEmpty(),"a normal panel title at the candidate header prevents a touch");
        mixedTree.remove(mixedTree.size()-1);renderer.visible=false;
        check(RenderedAdPanel.visibleControls(mixedTree,w,h)==0,"no visible renderer means no generic discovery");
        renderer.visible=true;
        List<AdMatcher.Node> unlabelled=new java.util.ArrayList<>();
        AdMatcher.Node viewport=new AdMatcher.Node(-1,0,0,w,h,"","");
        viewport.className="android.widget.FrameLayout";
        unlabelled.add(viewport);
        for(int i=0;i<37;i++)unlabelled.add(new AdMatcher.Node(0,20,620,160,680,"",""));
        for(int i=0;i<4;i++){
            AdMatcher.Node hidden=new AdMatcher.Node(0,20,620,160,680,"","");hidden.visible=false;unlabelled.add(hidden);
        }
        check(RenderedAdPanel.visibleControls(unlabelled,w,h)==0,"renderer-free discovery requires an explicitly verified window");
        check(RenderedAdPanel.visibleControls(unlabelled,w,h,true)==37,"reported 42-node window can use a bounded discovery opportunity after startup");
        check(BiliPanel.drawnTargetProblem(unlabelled,w,h,540,439,49,890).isEmpty(),"current blank viewport can verify window context after the full two-frame pixel match");
        unlabelled.get(1).editable=true;
        check(RenderedAdPanel.visibleControls(unlabelled,w,h,true)==0,"blank input context is ineligible");
        unlabelled.get(1).editable=false;
        viewport.desc="评论";
        check(RenderedAdPanel.visibleControls(unlabelled,w,h,true)==0
            &&!BiliPanel.drawnTargetProblem(unlabelled,w,h,540,439,49,890).isEmpty(),"ordinary labelled content cancels the no-renderer fallback");
        viewport.desc="";viewport.bottom=h/4;
        check(RenderedAdPanel.visibleControls(unlabelled,w,h,true)==0,"small overlay is not a usable viewport");
        VisualReview review=new VisualReview();
        VisualFollowup followup=new VisualFollowup();
        check(!followup.pending()&&!followup.active(1000),"no review before system completes a touch");
        followup.begin("tv.danmaku.bili",2,1000);
        check(followup.current("tv.danmaku.bili",2,1000),"same-window result observation starts on completion");
        check(!followup.current("other",2,1100)&&!followup.current("tv.danmaku.bili",3,1100),"no result observation on changed owner or window");
        check(followup.active(3999)&&!followup.active(4000),"follow-up work has an absolute three second limit");
        check(!followup.active(999),"backwards timestamps cannot extend observation");
        followup.clear();
        check(!followup.pending()&&!followup.current("tv.danmaku.bili",2,1100),"failed or completed observation stops work until a new touch");
        check(review.requestDelay(0)==0,"initial frame can be requested");review.requested(0);
        check(review.requestDelay(100)==350,"capture rate remains bounded during early recognition");
        check(review.requestDelay(450)==0,"fresh frames can be read without old 1200ms polling delay");
        check(review.observe(true)==VisualReview.Action.READY,"first complete fresh panel match permits immediate action");
        review.markAttempt();check(review.observe(true)==VisualReview.Action.ALREADY_ATTEMPTED,"persistent X is never tapped repeatedly");
        check(review.observe(false)==VisualReview.Action.NO_TARGET,"missing visual scene clears attempt state");
        check(review.observe(true)==VisualReview.Action.READY,"new panel can be closed immediately after previous panel disappears");
        review.markAttempt();review.clearScene();check(review.observe(true)==VisualReview.Action.READY,"context cancel releases prior scene");
        review=new VisualReview();for(int i=0;i<60;i++)review.requested(i*450L);
        check(review.requestDelay(28000)>=32000,"per-minute screenshot budget enforced");
        review.clearScene();check(review.requestDelay(28000)>=32000,"scene changes do not bypass screenshot budget");
        check(review.requestDelay(28000,true)>=32000,"pair priority cannot bypass per-minute budget");
        check(review.requestDelay(61000)==0,"screenshot rate budget expires");
        check(VisualReview.currentFrame(180,1200,2670,1200,2670),"current display-sized frame is usable");
        check(!VisualReview.currentFrame(601,1200,2670,1200,2670),"single-frame action rejects stale image");
        check(!VisualReview.currentFrame(-1,1200,2670,1200,2670),"future timestamp is rejected");
        check(!VisualReview.currentFrame(80,720,1602,1200,2670),"source display size change is rejected before coordinates are used");
        VisualReview pair=new VisualReview();pair.requested(1000);
        check(pair.requestDelay(1250,true)==100,"already-matched target can request second frame at 350ms");
        check(pair.requestDelay(1250,false)==200,"ordinary captures keep 450ms floor");
        check(pair.requestDelay(1349,true)==1&&pair.requestDelay(1350,true)==0,"pair capture never goes under 350ms");
        check(pair.requestDelay(1350)==100,"ordinary request is not promoted accidentally");
        System.out.println("PASS: "+checks+" visual checks");
        for(String path:args) {
            if(path.startsWith("drawnGame:")){
                replayDrawn(path.substring("drawnGame:".length()),851,865);
                continue;
            }
            if(path.startsWith("drawn:")||path.startsWith("drawnDark:")) {
                boolean dark=path.startsWith("drawnDark:");
                replayDrawn(path.substring(path.indexOf(':')+1),dark?872:880,dark?888:897);
                continue;
            }
            boolean expected=!path.startsWith("!");if(!expected)path=path.substring(1);
            BufferedImage image=ImageIO.read(new File(path));int iw=image.getWidth(),ih=image.getHeight();
            int[] p=image.getRGB(0,0,iw,ih,null,0,iw);long begin=System.nanoTime();
            BiliVisual.Result result=BiliVisual.detect(p,iw,ih);
            System.out.println(new File(path).getName()+": "+result.found()+" @ "+result.x+","+result.y+" size="+result.size+" "+result.reason+" "+((System.nanoTime()-begin)/1000000)+"ms");
            if(result.found()!=expected)throw new AssertionError("Unexpected screenshot result: "+path);
            if(expected) {
                int[] early=p.clone();Arrays.fill(early,(int)(ih*.85)*iw,early.length,0xff17181a);
                BiliVisual.Result withoutFooter=BiliVisual.detect(early,iw,ih);
                if(!withoutFooter.found()||withoutFooter.x!=result.x||withoutFooter.y!=result.y)throw new AssertionError("Footer-free scene must keep the same live X: "+path);
                System.out.println("  without footer: "+withoutFooter.found()+" @ "+withoutFooter.x+","+withoutFooter.y);
            }
            if(expected)for(int scaledWidth:new int[]{720,1200}) {
                int scaledHeight=Math.round(ih*(scaledWidth/(float)iw));
                BufferedImage scaled=new BufferedImage(scaledWidth,scaledHeight,BufferedImage.TYPE_INT_RGB);
                Graphics2D g=scaled.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(image,0,0,scaledWidth,scaledHeight,null);g.dispose();
                BiliVisual.Result transformed=BiliVisual.detect(scaled.getRGB(0,0,scaledWidth,scaledHeight,null,0,scaledWidth),scaledWidth,scaledHeight);
                if(!transformed.found()||Math.abs(transformed.x/(double)scaledWidth-result.x/(double)iw)>.01
                    ||Math.abs(transformed.y/(double)scaledHeight-result.y/(double)ih)>.01)throw new AssertionError("Scaled fixture mismatch: "+scaledWidth);
                System.out.println("  scale "+scaledWidth+"x"+scaledHeight+": "+transformed.x+","+transformed.y);
            }
        }
    }
    private static void replayDrawn(String path,int labelTop,int cardTop)throws Exception {
        BufferedImage source=ImageIO.read(new File(path));
        for(int[] display:new int[][]{{575,1280},{720,1602},{1200,2670}}) {
            int w=display[0],h=display[1];
            BufferedImage scaled=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics=scaled.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source,0,0,w,h,null);
            graphics.dispose();
            int[] pixels=scaled.getRGB(0,0,w,h,null,0,w);
            long start=System.nanoTime();
            BiliVisual.Result found=RenderedAdPanel.detect(pixels,w,h);
            System.out.println("Drawn panel "+w+"x"+h+": "+found.x+","+found.y+" "+found.reason
                +" in "+(System.nanoTime()-start)/1000000+" ms");
            check(found.found()&&found.requiresPair&&Math.abs(found.x-w*.94)<w*.009&&Math.abs(found.y-h*.343)<h*.007,
                "label-free real sheet must locate its live close icon and require a pair");
            for(int glyph=0;glyph<2;glyph++){
                int[] missing=pixels.clone();
                for(int y=h*(labelTop-2)/1280;y<h*cardTop/1280;y++){
                    Arrays.fill(missing,y*w+w*(16+14*glyph)/575,y*w+w*(32+16*glyph)/575,pixels[y*w+w*62/575]);
                }
                check(!RenderedAdPanel.detect(missing,w,h).found(),"each disclosure character is mandatory: "+glyph);
            }
            int[] noEdge=pixels.clone();
            for(int y=h*(cardTop-2)/1280;y<h*(cardTop+15)/1280;y++){
                Arrays.fill(noEdge,y*w+w/6,y*w+w*5/6,pixels[(h*(cardTop-3)/1280)*w+w/2]);
            }
            check(!RenderedAdPanel.detect(noEdge,w,h).found(),"label requires the adjacent card boundary");
            int[] noClose=pixels.clone();
            int r=Math.round(w*.021f);
            for(int y=found.y-r;y<=found.y+r;y++){
                Arrays.fill(noClose,y*w+found.x-r,y*w+found.x+r,pixels[y*w+found.x-r-1]);
            }
            check(!RenderedAdPanel.detect(noClose,w,h).found(),"disclosure cannot authorize a missing X");
        }
        System.out.println("PASS: "+checks+" visual checks including drawn panel replay");
    }
    private static int[] fixture(int w,int h,int shape,boolean bar,boolean more,boolean dark,double cx,double cy) {
        int[] p=new int[w*h];Arrays.fill(p,dark?0xff17181a:0xffeeeeee);
        if(bar)for(int y=(int)(h*.918);y<(int)(h*.966);y++)for(int x=(int)(w*.13);x<(int)(w*.88);x++)p[y*w+x]=0xffdf477f;
        symbol(p,w,h,shape,cx,cy,more);return p;
    }
    private static void symbol(int[] p,int w,int h,int shape,double rx,double ry,boolean more) {
        int cx=(int)(w*rx),cy=(int)(h*ry),r=Math.max(5,(int)(w*.013));
        int handleY=cy-(int)(w*.066),handleH=Math.max(2,(int)(w*.006));
        for(int y=handleY-handleH/2;y<=handleY+handleH/2;y++)for(int x=(int)(w*.465);x<=(int)(w*.535);x++)
            if(y>=0&&y<h)p[y*w+x]=0xff606367;
        for(int dy=-r;dy<=r;dy++)for(int dx=-r;dx<=r;dx++) {
            boolean on=shape==0?(Math.abs(dx-dy)<=1||Math.abs(dx+dy)<=1):shape==1?(Math.abs(dx)<=1||Math.abs(dy)<=1)
                :shape==2?(Math.abs(dx*dx+dy*dy-r*r)<=r*2):shape==3?Math.abs(dx-dy)<=1:shape==4?Math.abs(dy)<=1:false;
            int x=cx+dx,y=cy+dy;if(on&&x>=0&&x<w&&y>=0&&y<h)p[y*w+x]=0xffa9abad;
        }
        if(more)for(int d=-1;d<=1;d++) {
            int x=cx-(int)(w*.104),y=cy+d*Math.max(4,(int)(w*.0104));
            for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)if(dx*dx+dy*dy<=1&&x+dx>=0&&x+dx<w&&y+dy>=0&&y+dy<h)p[(y+dy)*w+x+dx]=0xffa9abad;
        }
    }
}
