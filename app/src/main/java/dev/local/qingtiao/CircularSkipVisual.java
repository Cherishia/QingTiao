package dev.local.qingtiao;

import java.util.ArrayList;

/** Current-frame two-character skip plus enclosing progress ring; contains no ad artwork. */
public final class CircularSkipVisual {
    // Only the supplied skip-control glyphs, normalized to a 40x20 grid.
    private static final String[] WORD={
        "617a77787d7b7782a4ad7db7a87f7477736e6f77787c7675797671726b6870bac28969686e74716c",
        "d9d9e6e8c2927b8dd0e189e3cb8c707072747578dab5837d7d77747479786cd0e89d6e6968656667",
        "f8dddcefd7a28091d7e785e6ca8a7f7f6f70737ecde79d7d797a7a74737376dcef9d73716c6e7069",
        "fc9b90d5d8d0c395d5eb82e6cf95b4b881746c7b8fdee7afa2b7bdb9b8bbbaeef7cfb898706c6b6e",
        "f9897accdbdbddb6d9ef80e7cdb1d5be736f6f737d88d7bac8edefeee9f0eaf8fbefe9ba786d706b",
        "faaa9fdbd9bccbe3d9ee85e6c7d7c793706d7678717b93898e9d9c96999b97e9f1b89a88706e6e6b",
        "fbe2e1f3ddaab2f2daea7fe6eaf3b97d74767f92959f8d7a95beae71736c76ddeca17774716b6b6b",
        "dcb1e4c6a38c8bb7ddec79e5f1c395776d7886b3b0d5bc8d88bbdbad727075d6efa072706e6a6f6d",
        "d784d8bd978a8cb7ede87de7f4d59e7b76777e9292ead8927389bfe2a67777dbea9b6d6a69686c68",
        "e08adbe6d5c8cae3f1e47fe8e7efd5a6897c76726fe3d88c71728bc6c4757cdbea9b6d6a68666667",
        "dd7cd6c6b7d8d9b9ebd171e5c9acbdb27f645e5c59dfd17a5755576f806062d6e98c56514d464546",
        "dc69d18b5f8a907dedad5fe3c273707c644c49484edfcb6a3c393f4f586269dfe279433e352d281e",
        "e182d9965d595e8cec8c67e5cc8d6c7d8a6d646768e3d77c4f4c5f8faca9c0f2cb81656359504432",
        "e19ddeb8958281bfe77d83e9d1a596c4d6918f8daeeef19f726873a5c9bed0d6a18f938e82756057",
        "e4b4e0dcd9c1bbf2cd8e92eddeb2aed6d99ba0aff4d8dbbca190827f838291a6a6aaad9e87796662",
        "ded9d0cccbe0f2d1918e9de8efd4d6e8d4a3a7ecdd848fcbe5e1dcdcdee1e5edefe3dbc096776064",
        "ad907b6c9cd7cd98929c9cc8e5eae9d2b1a7a9cf997c6f87b3d1dce3e6e9ecedeeeadeb88b745f5c",
        "a48f7b6d6979878695989ca2a4979499a6b5a89e8b756a6968666a76848ea4acb0acaa987a67565b",
        "a0897966636d7888989b9e9699a0a1a7afb2ae978980675f6064676e8a9db7c5c5aca08d70635b5a",
        "a5917e6e6d7177889aa0a3a5a5a29b9fb0b7a99689756c64626766688eb8d2cec4a38d806f626775",
    };
    private static float[] reference(int w,int h) {
        float[] out=new float[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int sum=0;
            for(int dy=0;dy<20/h;dy++)for(int dx=0;dx<40/w;dx++) {
                int sx=x*(40/w)+dx,sy=y*(20/h)+dy;sum+=Integer.parseInt(WORD[sy].substring(sx*2,sx*2+2),16);
            }
            out[y*w+x]=sum/(float)((40/w)*(20/h));
        }
        return out;
    }
    private static final float[] COARSE=reference(20,10),FINE=reference(40,20);
    private static final double[] UNIT_X=new double[32],UNIT_Y=new double[32];
    static {for(int i=0;i<32;i++){double angle=i*Math.PI/16;UNIT_X[i]=Math.cos(angle);UNIT_Y[i]=Math.sin(angle);}}
    private static final class Hit {
        int x,y,w,h;double score;
        Hit(int x,int y,int w,int h,double score){this.x=x;this.y=y;this.w=w;this.h=h;this.score=score;}
    }
    private static int difference(int a,int b) {
        return Math.abs((a>>16&255)-(b>>16&255))+Math.abs((a>>8&255)-(b>>8&255))+Math.abs((a&255)-(b&255));
    }
    private static int pixel(int[] p,int w,int h,double x,double y) {
        int ix=(int)Math.round(x),iy=(int)Math.round(y);return ix<0||iy<0||ix>=w||iy>=h?-1:p[iy*w+ix]&0xffffff;
    }
    private static double ring(int[] p,int w,int h,double cx,double cy,double radius) {
        double scale=Math.max(.8,w/592.0),sum=0;int hits=0;int[] quadrants=new int[4];
        for(int i=0;i<32;i++) {
            double nx=UNIT_X[i],ny=UNIT_Y[i],best=0;
            for(int k=-1;k<=1;k++) {
                double r=radius+k*scale;
                int border=pixel(p,w,h,cx+nx*r,cy+ny*r),inside=pixel(p,w,h,cx+nx*(r-2.5*scale),cy+ny*(r-2.5*scale)),outside=pixel(p,w,h,cx+nx*(r+2.5*scale),cy+ny*(r+2.5*scale));
                if(border<0||inside<0||outside<0)continue;
                best=Math.max(best,Math.min(difference(border,inside),difference(border,outside)));
            }
            if(best>=65){hits++;quadrants[i/8]++;sum+=best;}
        }
        if(hits<20)return -1;for(int n:quadrants)if(n<3)return -1;
        return sum/hits;
    }
    public static BiliVisual.Result detect(int[] p,int w,int h) {
        if(p==null||w<240||h<w*1.4||h>w*3||p.length!=(long)w*h)return none("画面尺寸不适用");
        int left=(int)(w*.80),right=(int)(w*.985),top=(int)(h*.035),bottom=(int)(h*.13);
        int min=Math.max(10,(int)(w*.022)),max=(int)(w*.042);ArrayList<Hit> seeds=new ArrayList<>();
        for(int th=min;th<=max;th+=2)for(int aspect=18;aspect<=21;aspect++) {
            int tw=Math.round(th*aspect/10f);
            for(int y=top;y+th<=bottom;y+=2)for(int x=left;x+tw<=right;x+=2) {
                double score=SplashVisual.score(p,w,x,y,tw,th,COARSE,20,10);
                if(score<.55)continue;if(seeds.size()>=512)return none("候选过多");seeds.add(new Hit(x,y,tw,th,score));
            }
        }
        seeds.sort((a,b)->Double.compare(b.score,a.score));ArrayList<Hit> words=new ArrayList<>(),searched=new ArrayList<>();
        for(Hit s:seeds) {
            boolean nearby=false;
            for(Hit previous:searched)if(Math.abs(s.x+s.w/2-previous.x-previous.w/2)<w*.018&&Math.abs(s.y+s.h/2-previous.y-previous.h/2)<h*.006){nearby=true;break;}
            if(nearby)continue;if(searched.size()>=24)return none("不同位置的候选过多");searched.add(s);
            // Coarse sampling can favour a larger box; refinement scales with the image.
            int heightRange=Math.max(2,w/360),widthRange=Math.max(4,w/180),positionRange=Math.max(2,w/300);
            for(int dh=-heightRange;dh<=heightRange;dh++)for(int dw=-widthRange;dw<=widthRange;dw++)for(int dy=-positionRange;dy<=positionRange;dy++)for(int dx=-positionRange;dx<=positionRange;dx++) {
                int th=s.h+dh,tw=s.w+dw,x=s.x+dx,y=s.y+dy;
                if(th<min||th>max||x<left||y<top||x+tw>right||y+th>bottom)continue;
                double score=SplashVisual.score(p,w,x,y,tw,th,FINE,40,20);if(score<.78)continue;
                int at=-1;
                for(int j=0;j<words.size();j++)if(Math.abs(x+tw/2-words.get(j).x-words.get(j).w/2)<w*.02&&Math.abs(y+th/2-words.get(j).y-words.get(j).h/2)<h*.006){at=j;break;}
                if(at<0){if(words.size()>=8)return none("不同位置的文字候选过多");words.add(new Hit(x,y,tw,th,score));}
                else if(score>words.get(at).score)words.set(at,new Hit(x,y,tw,th,score));
            }
        }
        BiliVisual.Result result=null;double best=-1;
        for(Hit word:words) {
            for(int dy=-2;dy<=2;dy++)for(int dx=-2;dx<=2;dx++)for(int r=110;r<=165;r+=8) {
                double cx=word.x+word.w/2.0+dx*word.h*.1,cy=word.y+word.h/2.0+dy*word.h*.1,radius=word.h*r/100.0;
                double score=ring(p,w,h,cx,cy,radius);if(score<0)continue;score+=word.score*200;
                if(result!=null&&(Math.abs(result.x-cx)>w*.025||Math.abs(result.y-cy)>h*.01))return none("多个圆圈跳过，不点击");
                if(score>best){best=score;result=new BiliVisual.Result((int)Math.round(cx),(int)Math.round(cy),(int)Math.round(radius*2),"当前圆环与‘跳过’两字逐字匹配；文字相关度="+Math.round(word.score*100)+"%，触摸当前圆圈中心");}
            }
        }
        return result==null?none("未同时确认圆环和跳过两字"):result;
    }
    private static BiliVisual.Result none(String reason){return new BiliVisual.Result(0,0,0,"圆圈跳过："+reason);}
    private CircularSkipVisual(){}
}
