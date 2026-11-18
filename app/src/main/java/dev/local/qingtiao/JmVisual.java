package dev.local.qingtiao;

import java.util.List;

/** Recognizes only the supplied light ad page's red X, black circle and close-ad caption. */
public final class JmVisual {
    // Glyph-only reference: 關閉廣告. No advertisement images or URLs are embedded.
    private static final String[] CAPTION={
        "e7d5d6dad8ded8e1f0e8d3d2dfdbe3d3daf8f6eff5ded9dbdedbdae6f4f0ddd6dad9d5d7dce6f4f7f2f2f2eeedf3f4f7f5cbacd0eceeedecedeff2f4faf0f4f2d9d8eeeeebc67cd2f3f5f0f3f2f2f2f2",
        "b0837a80807c80a8d1be8675837f816f93e0f2ead98272757d757596c5cb9273767c746e8ec7e5f6f3ebe3d9d8dcd7d8cb80477ac6dbd9d8dbe2ebf0f4e6f8e6887cd1f9e19e36b6eef2eef5f2f2f2f2",
        "a1298894918a6168d0a147659e958e4f45dcf6f2bf3e49747c6b4b53b7b65e4c727877544daae5f5f7cb846d7771706f61502b3b656b6371747bb8ebf4f2f2d13a50d1dfd67b39a0dae8d5e7dceff7eb",
        "8f1599b2aea15f66c8904c7bb0b2a86246d6eff6c23664b5c7b06d4ba8af626db1beb17655acecf3f1b14b4c67676b4627515e5b513a41666d71ade5f2ebe98f28578b82844e206283887e7d7fb6eaf6",
        "9d127f827a7d4c5ed9963e717e7b7a4c3fd2eaf0ba344c8583784e44b1b0544d828a845845a6ecf3f7b84777bcb4a3673e7fafb58d50558fafc6e6f2f5f2d44f2c5454595a360a455f5c6157609fe1f7",
        "a5127b85937b4c68d3923564868d7e4538d2f3fac3374e8f8b825449b7b55852868c875f4ba5ebf2f2b54791c7968151295e878b773f45748cb5dee9f1e88f4091bcc4cbbb7c3097c5c0c8cac7dff6fa",
        "a109766c69544995d3a34b417677734136d3f3f2b73a59b1b8a5634bacb05f65a6b2a77155a7e8f3ebaf458cc1968a572a65939078404e8090b6e5f5f3df7063d3eaf5f2e7922ab7eeeee9f8f4f0ecee",
        "9a1ebcab85678bb4c5aa6d779c9ba6653fd2f0f5c03747908f7f5a5fb4b667608b8d885c4aa1e5f8efb54d84bca195613672a0a182444c88a0b4d6ece4cc858cbfc9ccd1c78225a5cad3c4ced0cfd8ee",
        "9d26d0a562689b8a8c7a559198a3c47840d2eef4c4385097958c899fd4cd8577918c8c634ea3e5f7f1b34877a9877f67516c6b67755a57808a91bfeacc96717074857b7e7353206a768278767481a8dc",
        "a424bf8e545263808371526971a7d27b3bd7f3f1c03b74d0d6cfd5dfe9d7938fbfceca8557a9e9f2f2b3457cb48d827f767245396d7d78878a9ad1ecd2927c7d75807779746d6274737a7e75737ca1d8",
        "a427cfaf7c5e5f99a5996c6175acdb823bddf4f2c03e83dab7a9acb0a6935f6193bdd89558aaebf1f4b4488bcc8b747a7972412f667c78747eb2e7ece7cccbcbc0c7c0c7c6c8cbc5c1c5ccc5c5c8d1ee",
        "a024cca25b576c78847f5a686a7fbf7e3bddecf1c2407fc67a6a6b6e5b401e2d5b88c29657a9eaf0f3b14892cc685a8591824934738e8d6254a4e7f2f2f1f6ccb5c0b6bbbabbbfbdb8bdbfb8d3f1eff8",
        "a524c2914c606d56605f5e7a6263aa743bd9eaf2c53e82d5a6a7a89266381b438fb4d39855a8e9ebf1ad4998ce595394a4914c3b7ba1a465459be8f2f3f1ef7d566b686e6c656d70696c674c9ae8f2f2",
        "a626d4bb89bc9d65928f93bea17fb8783ddaf2f3c03c87f2e4e3cc956f614666c4e9e79d59a8eaebf1a2459ed25342757776403269838150439fe8eeedf0f0503475828c8c7f878c81846a2770e6f7ef",
        "9d1dcba566d09a68af8f80b7a487c38141d6f5f4bc3b82f6e3b07c6383a46d6dc9f5e7995ca6eaf2f29141a7cd555fa6a79b5a408aada7684b9be5f1ecf1f24841bbd2dbe0d6dedfd7dead3d64ebf7f0",
        "a130e1a244976e6ec68454827079be8446cef2f5bf3f74c9945f5b91d1c2686fcef4ea995ba2e8f7ea7a48b8cc575499a4975d427da0a0674ea3eaeff1f5ec4645daf3f5f3ecf3f1f1fdbe3969eff0f0",
        "9823dfbd727b375bc27c42797cafcf814cd5f8f2bf47698a4159a0ccd4ab557ed5eae89b5ea3e5f6d15f61cedb886667555b605d66626b656eb4e6f3f7f4e8483cbfd8d9dddbdfdbdbe5ac3f6aede8ef",
        "9c1ee3efd2a25189d68f6fcddfe2d06f4fdffbf0ba3883d2b0dce3903c344298dfebd5815dafe7efae4580dfe5cfc0a48598cedaa37c9cc8dbeaeaf1f6ecef4d26697f7c7d7c7c7978795b2665eeebf2",
        "a829d7d8b4575fcaea8974d1c7b29b5456e0f2eec64083f0e7f8dba5778eb7b08c7a5f3152c0ebe9953c98e2ae6d3b2a4e8ed9e1a35532345ba6d7f0f6e9f64a09141f151a191a1c1c19110863eff3f5",
        "a536cdb8438ce1e8ea9b86c6720f0e45a6e8f4f2c0408ce3fbf6eeedf6efe8ab48081650a3e0f1eeb29acfa2474b88c5e9f4f3f1efedcf8a4d54a0edf8f0e65a38caf0eeebede7e2f1eea34475ddfce7"
    };
    private static final float[] LETTERS=letters(),COARSE=coarse();
    private static float[] coarse() {
        float[] out=new float[400];
        for(int y=0;y<10;y++)for(int x=0;x<40;x++)out[y*40+x]=LETTERS[(y*2+1)*80+x*2+1];
        return out;
    }
    private static float[] letters() {
        float[] out=new float[80*20];
        for(int y=0;y<20;y++)for(int x=0;x<80;x++)out[y*80+x]=Integer.parseInt(CAPTION[y].substring(x*2,x*2+2),16);
        return out;
    }
    public static boolean context(String pkg,boolean startup,List<AdMatcher.Node> nodes) {
        // Page complexity is not evidence against an image ad. Pixel recognition supplies the proof.
        return pkg!=null&&!pkg.isEmpty()&&startup&&!nodes.isEmpty()&&nodes.size()<1000&&!AdMatcher.sensitive(nodes);
    }
    public static boolean captionNodes(List<AdMatcher.Node> nodes) {
        for(AdMatcher.Node n:nodes)if(n.visible&&(caption(n.text)||caption(n.desc)))return true;
        return false;
    }
    static boolean caption(String text) {
        if(text==null||text.length()>120)return false;
        String s=text.replaceAll("[\\s\u00a0\u3000]","").replace('Ｘ','X').replace('ｘ','X').toUpperCase(java.util.Locale.ROOT);
        return s.contains("等待3秒")&&s.contains("右上")&&s.contains("X")&&(s.contains("關閉廣告")||s.contains("关闭广告"));
    }
    private static boolean red(int p){return ((p>>16)&255)>155&&((p>>8)&255)<95&&(p&255)<100;}
    private static int light(int p){return (((p>>16)&255)*77+((p>>8)&255)*150+(p&255)*29)>>8;}
    public static BiliVisual.Result detect(int[] p,int w,int h){return detect(p,w,h,false);}
    public static BiliVisual.Result detect(int[] p,int w,int h,boolean captionObserved) {
        if(p==null||w<240||h<w*1.4||h>w*3.0||p.length!=(long)w*h)return none("画面尺寸不适用");
        int left=(int)(w*.78),right=(int)(w*.98),top=(int)(h*.055),bottom=(int)(h*.22),rw=right-left;
        boolean[] seen=new boolean[rw*(bottom-top)];int[] queue=new int[seen.length];
        BiliVisual.Result found=null;
        int shapes=0,circles=0;double captionScore=-1;
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++) {
            int pos=(y-top)*rw+x-left;if(seen[pos]||!red(p[y*w+x]))continue;
            int first=0,last=1;queue[0]=pos;seen[pos]=true;int l=x,r=x,t=y,b=y;
            while(first<last) {
                int value=queue[first++],sx=left+value%rw,sy=top+value/rw;
                l=Math.min(l,sx);r=Math.max(r,sx);t=Math.min(t,sy);b=Math.max(b,sy);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int nx=sx+dx,ny=sy+dy;if(nx<left||nx>=right||ny<top||ny>=bottom)continue;
                    int at=(ny-top)*rw+nx-left;
                    if(!seen[at]&&red(p[ny*w+nx])){seen[at]=true;queue[last++]=at;}
                }
            }
            int cw=r-l+1,ch=b-t+1,cx=(l+r)/2,cy=(t+b)/2;
            if(cw<w*.022||cw>w*.052||ch<cw*.75||ch>cw*1.3||last<cw*ch*.17||last>cw*ch*.65)continue;
            int near=0;int[] corners=new int[4];
            for(int i=0;i<last;i++) {
                double dx=(left+queue[i]%rw-l+.5)/cw*2-1,dy=(top+queue[i]/rw-t+.5)/ch*2-1;
                if(Math.min(Math.abs(dx-dy),Math.abs(dx+dy))<.42)near++;
                if(Math.abs(dx)>.45&&Math.abs(dy)>.45)corners[(dx>0?1:0)+(dy>0?2:0)]++;
            }
            if(near<last*.83)continue;boolean all=true;for(int c:corners)if(c<2)all=false;if(!all)continue;
            shapes++;
            int dark=0,pale=0;
            for(int i=0;i<16;i++) {
                double a=i*Math.PI/8;
                int ix=(int)Math.round(cx+Math.cos(a)*cw*.95),iy=(int)Math.round(cy+Math.sin(a)*ch*.95);
                int ox=(int)Math.round(cx+Math.cos(a)*cw*1.65),oy=(int)Math.round(cy+Math.sin(a)*ch*1.65);
                if(ix>=0&&ix<w&&iy>=0&&iy<h&&light(p[iy*w+ix])<55)dark++;
                if(ox>=0&&ox<w&&oy>=0&&oy<h&&light(p[oy*w+ox])>215)pale++;
            }
            if(dark<14||pale<14)continue;
            circles++;
            double best=-1;double[] scores=new double[32];int[][] samples=new int[32][4];int used=0;
            // The close-ad caption must appear below the circle, independently of the red cross.
            for(int th=Math.max(10,(int)(ch*.85));!captionObserved&&th<=ch*1.3;th++) {
                int tw=Math.round(th*81/19f);
                for(int sy=cy+(int)(w*.035);sy<=cy+w*.09&&sy+th<h;sy++)
                    for(int sx=(int)(w*.56);sx+tw<=w*.79;sx++) {
                        double score=SplashVisual.score(p,w,sx,sy,tw,th,COARSE,40,10);
                        if(score<.35)continue;
                        int at=used;
                        if(used==32) {
                            at=0;for(int i=1;i<32;i++)if(scores[i]<scores[at])at=i;
                            if(score<=scores[at])continue;
                        } else used++;
                        scores[at]=score;samples[at]=new int[]{sx,sy,tw,th};
                    }
            }
            for(int i=0;i<used;i++) {
                int[] s=samples[i];best=Math.max(best,SplashVisual.score(p,w,s[0],s[1],s[2],s[3],LETTERS,80,20));
            }
            if(captionObserved)best=1;
            captionScore=Math.max(captionScore,best);
            if(best<.74)continue;
            if(found!=null)return none("多个符合条件的红色X，未点击");
            found=new BiliVisual.Result(cx,cy,ch,"红色X笔画、黑圆、浅色外围及"+(captionObserved?"无障碍关闭广告提示":"‘關閉廣告’字形")+"匹配；核对广告等待时机");
        }
        return found==null?none("JM未匹配：红X笔画="+shapes+"，黑圆及浅色外围="+circles+"，无障碍关闭提示="+captionObserved+"，关闭字形相似度="+String.format(java.util.Locale.ROOT,"%.2f",captionScore)):found;
    }
    private static BiliVisual.Result none(String reason){return new BiliVisual.Result(0,0,0,reason);}
    private JmVisual(){}
}
