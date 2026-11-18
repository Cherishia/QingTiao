package dev.local.qingtiao;

import java.util.ArrayList;

/** A two-character UI watermark plus its adjacent blue/yellow ad-choice icon. */
final class MiniAdBadge {
    private static final String[] TEXT={
        "404042444744403c3f4446423a4246444b4442463e433d2e39414647443d3d454747474848474644",
        "4748494a4b48444143474a4a45484742494645474146464244484642454645424545444345464747",
        "4744424143454747464544424445413c41424143413f454e3f444238384748404544424141424446",
        "4543403e3f43464847454449534f463f414545434541444c404547444f4d48454444434342424243",
        "4647464442424345464556767e6c56444248484346474f5f67544c5c91694a444242424242424343",
        "44454645454a51575a5876abb0916f5853595444424a66909f705c7bce925d5058524b4541424345",
        "44413f424f6882969993a3c7d2b69c8e8f907b55435387cac59b8da8e8ba928a92826a5344424346",
        "4b423f475e8fb7ccc0b6b0b2c2b7b0b2b8b294654e70a7d7b1a4a8bfe7c4acafac95795d46444545",
        "493d3e4d578fac9d63595a5d5b58595e60564b474e91aa8c5a4e587cd6935c53554f4c4b3a464b44",
        "463f414b538aa48f5045413b41434445433f4250689aac9a88868fa5dcb8998f8789867649403e3b",
        "3f404349528da8904e4241414849484542404759749bb0aeaba9a8a9afaea9a5abada48e64544942",
        "404344465392ad9353464647484542424544444854596f8e85878b8b7d888f8d83745e463e44443d",
        "464543455894aa8a50424142433f3e414948433e425286c59b99a2a9999b9fa5b29a71454246453c",
        "474240486599a47f4f4442414645444449474341465897e08b787a8179757888aea27a4843444543",
        "443e41537ba29e714f4945404848454142403e3f475b9be381666266676a7182a9a37d4846444342",
        "463e476491a99560484543404846433f42424040455b9be59a898584838b929dbeae7c404746423e",
        "47414b668b957e513f3f4142454442414647464345588fcfa19796968b92979db9a2723e4343423f",
        "43414652636459483f4244424242424242424242424e6c8f6f5f5f685b60676f7e725e4a3e3c3f43",
        "454241454d4c453e3d4245424242424242424242423c4960443b3e463b4044444e4b46423f414345",
        "48454240403f3d3d404546424242424242424242424046503b3b41463d42423c3a3d414547464443",
    };
    // Alternate UI-only thin-font 广告 watermark, no advertiser or body pixels.
    private static final String[] THIN_TEXT={
        "84848485858585858585878788888889898989898989898989837361555354555656565557575758",
        "84848485858585858585868687878888888888888888888787817261555354555656565557575758",
        "83838384848484848483848586868687878787878887878787827260565454565757575658585858",
        "81828282828181828282838383838383838585868686878984807360555252545657575859595959",
        "818181828281807f8083898783818182848485858686868485817268706355545455565759595959",
        "7f7f807d7e80807e80848c89858282838484838384868687918b706c8871585a5b5a585659595959",
        "7e7e7f7c818a8c8b8c9092908e8c8c8c888483828386888d908d807d8e8173736f665d5659595959",
        "7d7d7e7b828e8e898687888889898989868482808285888f8b817171887561605e5c595559595959",
        "7c7c7d7d7d8f88827e808080808080808081818081858a908b8476758a7569656264665457575858",
        "7d7d7e7c7d9188817d7e7e7e7e7e7e7e8082828182868b918e8d8982837c7f7b807d755b57575858",
        "7d7e7e7b7e9186817c7b7e7e7e7e7e7e7e7f807f7f818485888882766e6d726c70665f5657585859",
        "7c7c7d7c8090837f7d7a7d7d7d7d7d7d7c7d7e7e7d7f80859090847c7a797c7e836959595859595a",
        "7a7b7b7c82907e7d7d79797979797979787a7c7b7a7b7c838b8371655f595b687f69565959595a5a",
        "7777777984897976797776767676767676767777777879828b8371675d5a5a697d6a565b59595a5a",
        "7373737b878274727472727272727272747474757576778190918a8a868885898b6f555759595a5a",
        "6c6c6c7175706b6e6f6c6e6e6e6e6e6e6f70717172727379807b6e665f5e5f646c61565a59595a5a",
        "626262616264676a6866686868686868696a6b6b6c6c6b6a6e6c625a545455585c59565959595a5a",
        "595959595b5f60605e5e616161616161626364646565656767665f57525458585859595659595a5a",
        "52535353545657565859595a5a5a5b5b5c5e5e5e5f5f5f5e5e5f5c54515356575659595659595a5a",
        "4d4e4f4f505050515151525354555657565657575858585a5a595753505356575859585859595a5a",
    };
    private static final float[] FINE=reference(40,20),COARSE=reference(20,10),THIN_FINE=reference(THIN_TEXT,40,20),THIN_COARSE=reference(THIN_TEXT,20,10);
    private static float[] reference(int w,int h){return reference(TEXT,w,h);}
    private static float[] reference(String[] glyph,int w,int h) {
        float[] out=new float[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int sum=0;for(int dy=0;dy<20/h;dy++)for(int dx=0;dx<40/w;dx++)sum+=Integer.parseInt(glyph[y*(20/h)+dy].substring((x*(40/w)+dx)*2,(x*(40/w)+dx)*2+2),16);
            out[y*w+x]=sum/(float)((40/w)*(20/h));
        }return out;
    }
    private static boolean blue(int c){int r=c>>16&255,g=c>>8&255,b=c&255;return b>=120&&g>=80&&r<=120&&b-r>=65;}
    private static double light(int c){return ((c>>16&255)*299+(c>>8&255)*587+(c&255)*114)/1000.0;}
    /** Match the resampling used by the UI reference instead of stretching nearest-neighbour pixels. */
    private static double smoothScore(int[] p,int w,int x,int y,int tw,int th,float[] ref,int sw,int sh,boolean dark) {
        double worst=1,inverted=1;
        for(int part=0;part<2;part++) {
            double a=0,b=0,aa=0,bb=0,ab=0;int count=sw*sh/2;
            for(int yy=0;yy<sh;yy++)for(int xx=part*sw/2;xx<(part+1)*sw/2;xx++) {
                double sx=Math.max(0,Math.min(tw-1,(xx+.5)*tw/sw-.5)),sy=Math.max(0,Math.min(th-1,(yy+.5)*th/sh-.5));
                int ix=(int)sx,iy=(int)sy,nx=Math.min(tw-1,ix+1),ny=Math.min(th-1,iy+1);double dx=sx-ix,dy=sy-iy;
                double v=(light(p[(y+iy)*w+x+ix])*(1-dx)+light(p[(y+iy)*w+x+nx])*dx)*(1-dy)
                    +(light(p[(y+ny)*w+x+ix])*(1-dx)+light(p[(y+ny)*w+x+nx])*dx)*dy,t=ref[yy*sw+xx];
                a+=v;b+=t;aa+=v*v;bb+=t*t;ab+=v*t;
            }
            double variance=aa-a*a/count,den=Math.sqrt(Math.max(0,variance*(bb-b*b/count)));
            if(variance/count<85||den==0)return -1;double correlation=(ab-a*b/count)/den;worst=Math.min(worst,correlation);inverted=Math.min(inverted,-correlation);
        }return dark?Math.max(worst,inverted):worst;
    }
    private static final class Box {int l,t,r,b,count;Box(int x,int y){l=x;t=y;r=x+1;b=y+1;}}
    static final class Result {
        final boolean found;final String reason;
        Result(boolean found,String reason){this.found=found;this.reason=reason;}
    }
    static boolean detect(int[] p,int w,int h){return analyze(p,w,h).found;}
    static Result analyze(int[] p,int w,int h){return analyze(p,w,h,false);}
    static Result analyze(int[] p,int w,int h,boolean dark) {
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return new Result(false,"画面尺寸无效");
        int l=(int)(w*.80),r=(int)(w*.98),t=(int)(h*.90),b=(int)(h*.99),rw=r-l,rh=b-t;
        byte[] mask=new byte[rw*rh];int[] q=new int[mask.length];ArrayList<Box> icons=new ArrayList<>();
        for(int y=t;y<b;y++)for(int x=l;x<r;x++)if(blue(p[y*w+x]))mask[(y-t)*rw+x-l]=1;
        for(int at=0;at<mask.length;at++)if(mask[at]==1) {
            int head=0,tail=1;q[0]=at;mask[at]=2;Box box=new Box(l+at%rw,t+at/rw);
            while(head<tail) {
                int at2=q[head++],x=l+at2%rw,y=t+at2/rw;box.count++;box.l=Math.min(box.l,x);box.r=Math.max(box.r,x+1);box.t=Math.min(box.t,y);box.b=Math.max(box.b,y+1);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++) {
                    int xx=at2%rw+dx,yy=at2/rw+dy;if(xx<0||xx>=rw||yy<0||yy>=rh)continue;int next=yy*rw+xx;
                    if(mask[next]==1){mask[next]=2;q[tail++]=next;}
                }
            }
            int bw=box.r-box.l,bh=box.b-box.t;
            if(bw>=w*.01&&bw<=w*.04&&bh>=w*.01&&bh<=w*.045&&bw>=bh*.45&&bw<=bh*1.6&&box.count>=bw*bh*.2)icons.add(box);
            if(icons.size()>16)return new Result(false,"广告图标候选过多");
        }
        int colours=0,candidates=0;double highest=-1;
        for(Box icon:icons) {
            int size=icon.b-icon.t,yellow=0;
            for(int y=Math.max(t,icon.t-2);y<Math.min(b,icon.b+2);y++)for(int x=Math.max(l,icon.l-size/2);x<icon.r;x++) {
                int c=p[y*w+x],red=c>>16&255,green=c>>8&255,blue=c&255;if(red>=160&&green>=110&&blue<=140&&red-blue>=60)yellow++;
            }
            if(yellow<Math.max(3,size*size*.025))continue;
            colours++;
            ArrayList<int[]> seeds=new ArrayList<>();
            // The connected blue triangle can shrink independently of the adjacent font after scaling.
            for(int th=Math.max(8,(int)(size*.7));th<=size*2.0;th+=2)for(int aspect=16;aspect<=22;aspect+=2) {
                int tw=Math.round(th*aspect/10f);
                for(int y=Math.max(t,icon.t-size/3);y<=icon.t+size/3;y+=2)for(int x=icon.r+1;x<=icon.r+size*.65;x+=2) {
                    if(x+tw>=r||y+th>=b)continue;
                    double score=Math.max(SplashVisual.score(p,w,x,y,tw,th,COARSE,20,10),smoothScore(p,w,x,y,tw,th,COARSE,20,10,dark));
                    if(dark)score=Math.max(score,smoothScore(p,w,x,y,tw,th,THIN_COARSE,20,10,true));
                    if(score>=.60)seeds.add(new int[]{x,y,tw,th,(int)(score*10000)});
                    if(seeds.size()>512) {
                        if(!dark)return new Result(false,"广告水印字形候选过多");
                        seeds.sort((a,c)->Integer.compare(c[4],a[4]));seeds.subList(256,seeds.size()).clear();
                    }
                }
            }
            seeds.sort((a,c)->Integer.compare(c[4],a[4]));
            candidates+=seeds.size();
            for(int i=0;i<Math.min(12,seeds.size());i++) {
                int[] s=seeds.get(i);
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++)for(int dh=-1;dh<=1;dh++)for(int dw=-1;dw<=1;dw++) {
                    int x=s[0]+dx,y=s[1]+dy,tw=s[2]+dw,th=s[3]+dh;if(x<l||y<t||x+tw>=r||y+th>=b)continue;
                    double score=Math.max(SplashVisual.score(p,w,x,y,tw,th,FINE,40,20),smoothScore(p,w,x,y,tw,th,FINE,40,20,dark));if(dark)score=Math.max(score,smoothScore(p,w,x,y,tw,th,THIN_FINE,40,20,true));highest=Math.max(highest,score);
                    if(score>=.84)return new Result(true,"双色图标及广告两字匹配，最低逐字分数="+Math.round(score*100)+"%");
                }
            }
        }
        return new Result(false,"图标="+icons.size()+"，双色="+colours+"，字形候选="+candidates+"，最高逐字分数="+(highest<0?"未匹配":Math.round(highest*100)+"%"));
    }
    private MiniAdBadge(){}
}
