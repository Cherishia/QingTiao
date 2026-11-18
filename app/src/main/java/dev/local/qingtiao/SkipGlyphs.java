package dev.local.qingtiao;

import java.util.Arrays;

/** Shared UI-only reference bank: standard WM, handwriting, and compact web skip text. */
final class SkipGlyphs {
    private static final String[][] TEXT={
        {
            "92938b8f92908e8f929595928f9495919092908d9096938f93968f8d8c898a8f94938e8b8e909190",
            "93979a9a9ea09a91909ba6a4a0a8a3948f8e90878da4a9948a91959393919098a7afa3918b8c8f91",
            "949bc4bcbbc2b99d92a3b7b2aebcb098928e919291b0c7af90898e8d91918c95b1c2ae8f8c8a8c92",
            "949fd6c3bdcdc9ab99acc0b6b2c6b89f9e9592968ca8ceccaa94939094958e96b6cab596978e8b91",
            "95a3ceb4a9bfcab8a9b6c3b6b1c8bbaab2a090928996b9c9b7a6aaafb2b3afb6ccd9cab3a9988c8f",
            "96a5ccafa0b5cac6babcc5b8b0c8bdb5c4aa8e918f8d9db0acabbfc9c8c6c6cdd6d9d3c8baa18d8d",
            "93a3d7c3c0cecabbbac7ccb6adcbcecbbe9e8b8e9794989e989aa9b3afababb4c8d0c1b1a1978f8d",
            "90a1c7c6d5d7bdadb7cacfb6adcbcfc6ac969394acb5baaf9898a9b0a598909bbccab19991908f8e",
            "8e9fb0b7d2cca898a6becab7aecac5b0978d9496b4c9d9c79c96adc2baa38f92b7cbb19490908e8e",
            "8fa1b3adc7c5a3919cbaccb6adcdc8af948b8d8ea4b4d7d09d8e9dc0c7b59a92b1c8b0948f8f8e8d",
            "93a7c5afc4ccb9adb2cdd7b5abd1d4c3af9e908e9395c9d29f8a90aab9b7a398b2c6ad908f8e8e8d",
            "97adcbb3cad1c7caccd6d9b6a8cdd2cac3b092929189c4d49e8b8f99a2a69a97b6cbb0918e8d8d8c",
            "98afc6b0cac6b6c0c2c4cab1a6c6beb3b8ad898f9489c6d398898f9193968f93b5cbb1938d8d8c8c",
            "96aec9adc5ba9ea2a8b9c1a7a9c8b19ba8ac8b89948ac8d5998a8d8c95a8aeb1c4caa98c8c8c8b8b",
            "95acceafc5c2a096a3c4c29eabceb495abbb978a9493cfdea9928c8b9ec2d6d7d5c49f888c8a8b8e",
            "9cb0cebcccd9baa8bbc9b599a5c6b997b5ca938e99b9dce1c6a797919aabb7b6ab9c9392948d8b93",
            "a8c3ebdad7d7ced1d6c5a5919ec0c2afc2c79386bad9cec2cbd1c8b6b1b2b0aba8a9aeb3b09c8c8c",
            "a5bfc6bdb1abb5cacaa98d8b9dc4d8d2ceb88b96c7cc9c85a7cbd2d7dcdcdbd9d9dbded9b9a18c88",
            "919d9096928b97aaaa978e8f8f9ba9ada99d8d8f9fa08b828e9a9b9fa5a7abaeaeaaa7a5a595898a",
            "858687918f8a8f93908f908e8783888d88858c8f84848d928d888a8b8a878586878888898d8a888d",
        },
        {
            "0000000000000000000000000100000000000000000000000000000000011d391801000000000000",
            "0000000000000000000000152e100100000000000000000000000000000351bc9712000000000000",
            "0001010101070e09050c0448c177090100000000000000000000000101010e92c520000000000000",
            "031007113b7496693d895732c59b0c0000000000032638260b0000020301088ac620000000010000",
            "2d998799c0d1dc8525aec254cc8d0e0d03000000086cc1bf831100020301088bc620020200000000",
            "1fa0cf8043a0bf330b88cd59c885499526010001011046a2b927000203010b8ecb4f534b18010000",
            "013dc66c40c0a526177dca5bcd8b9cc9440201020401021b3f0d061327466bb9dccad2c9800b0000",
            "000fa6c0c3c28390a0abc45dd7cac15d0e030102020102081a0f43a0bbc7bbc9d573514f36040000",
            "000461bedb581240a4d5bb64d7bd4e08030101042b4f77a0bb722e8e7a422177cc2f020201000000",
            "00020c48cb907d5535aeae69d7882a0c0202010b6bb6b3d3cc4d0e6fb37b1f75cc2b000000000000",
            "003b5a43c4cca8789ad49b67dbd2b8812704010211242db7a415022095d05881cb28000000000000",
            "006ac970c0913fb1ced48266d38aa3d1870c060301010fa0ab1704051b5a2887c926000000000000",
            "0049d77cc09a81be88ca6065cf501a66670b482601000568cb5a060505122091c623000000010000",
            "003bd691c8d5a94e65c13461d454040609078a6705050e32bbb31f0205358eb3c11e000000010101",
            "0a51d9d0be6a21179d981155d769090405119ca4538399a1cccc57230c158edbb619000000020201",
            "51c4cc8d32070959b940032ac0b6604e6493cdd2a99481889fb6c7bda06e74cb880f050505090802",
            "36834f1106011686650704094da7c8c6c2b8a072290802030d2041699bc2d3d3a67a6b6b7985570a",
            "050a06010100061a09020103071b2f332e2113070202020202020206153669a4cedcdad7c4953d04",
            "010001000000000101010000010201010101000002020202020202010102081732616e45240d0300",
            "01000100000000040500010101000000000000010302020202020201010303040402020000000000",
        },
        {
            "39393d3d383b3635383c3c2e38323337343b3638342b2d2c3b403e3b3c3b3e3e3c3d323f41352f47",
            "39393e3a313f383a3a3d443f483d424a3739383737393f453e3536413b39343639374f49453d394e",
            "393947555d6c6759433848716e606b6e3e3435353a485c7c583e3538423c393c3e49a468433a3c3c",
            "39395c869b939f885f414d9e93808e8e4a3b3c37364567a98b614132393a3d3b3a56dc854a373730",
            "39396a9fa878a8aa8b6660a7a08b999e615c5545383c5ba6a7805f5a555e5f5d5d74eaa37356453c",
            "39396c9c944a95b5ab91849c9d8798a981876a503f394378917d77929099949599a2f1bf9f7e5a47",
            "39396a9a98549db6aaa1ab9e9e869ab8a2a369483c3a364a605d68909c9796979aa9f6bd9a7b5939",
            "3939689eb39ebfab8c95c5afa58ca6c9b29b53373c505f645c494763865f5a545476e89c6750442c",
            "38395c80a0ddbc84637dbeb8a18aadd1a26a403b588bc2dbb06d4e74cc7b4b3c405bde8f4e363c43",
            "3839536476bd9e6d464171c4a98898a974523e3b4d6a85bdb97c4859b6c079483e5eec96573b3b42",
            "3a3a80a490b7bc88616face2a691b8d7a57a4d32344a58aaae7645445db6c6813c56e388523e3a3a",
            "3b3a88ad92bfd0aeaac3dbd59c87acd4bcb2844f363633acb577423e326672564065f18b55413734",
            "383786a686c4a2899dac9bc68e7d99ab85907d533a393ea0aa77463c3d4840374070e87f4c3a373c",
            "35358cad86c0915f5b697ad07a799e9f5a676c503a3b4da4af7d45303f4e7b969fb0e1764739363a",
            "383b8aae92c0b47954619ac5636d9a9c59778767463f59bbcd99593943559dc1beaea255403d3a3c",
            "404794c5bdd7d3b18e92b898526394a06d959b73525d94bcccbb97715e64899a93846d52544f423e",
            "46529ecfc3c0a6afb8b8a363455d92b29fa98b655b7fc2938ea6b8b29d9c9d9d9b9b9699967b5540",
            "44527e8c6e5d46679aa5703d3a4e78a7c2a66d545b75895c44557da7babfc4c5c2bfc4caba956744",
            "3a414642384134435d6143313739404c564d3b353d4642393334393d424c575a55505157564c403a",
            "3433312e30463f3c3f3c303c403c352f2f33383a3b3c434949423935383b3b3c3c39363635333132",
        },
    };
    static final int STYLES=3;
    static long patchKey(int x,int y,int w,int h){return ((long)x<<48)|((long)y<<32)|((long)w<<16)|h;}
    private static final float[][] COARSE=new float[STYLES][],FINE=new float[STYLES][];
    private static final long[][] INK=new long[STYLES][20],NEAR=new long[STYLES][20];
    private static final int[][] COUNT=new int[STYLES][2];
    static {
        int[] threshold={180,100,130};
        for(int style=0;style<STYLES;style++) {
            COARSE[style]=reference(style,20,10);FINE[style]=reference(style,40,20);
            for(int y=0;y<20;y++)for(int x=0;x<40;x++)if(FINE[style][y*40+x]>=threshold[style]){INK[style][y]|=1L<<x;COUNT[style][x/20]++;}
            for(int y=0;y<20;y++)for(int part=0;part<2;part++) {
                long mask=((1L<<20)-1)<<(part*20),row=INK[style][y]&mask;
                if(y>0)row|=INK[style][y-1]&mask;if(y<19)row|=INK[style][y+1]&mask;
                NEAR[style][y]|=(row|row<<1|row>>>1)&mask;
            }
        }
    }
    static float[] reference(int style,int w,int h) {
        float[] values=new float[w*h];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++) {
            int sum=0;for(int dy=0;dy<20/h;dy++)for(int dx=0;dx<40/w;dx++) {
                int sx=x*(40/w)+dx,sy=y*(20/h)+dy;sum+=Integer.parseInt(TEXT[style][sy].substring(sx*2,sx*2+2),16);
            }
            values[y*w+x]=sum/(float)((40/w)*(20/h));
        }
        return values;
    }
    static double gray(int[] p,int w,int x,int y,int tw,int th,int style){return SplashVisual.score(p,w,x,y,tw,th,FINE[style],40,20);}
    private static double colorCoarse(int[] pixels,int width,int x,int y,int tw,int th,int style) {
        double worst=1;
        for(int part=0;part<2;part++) {
            double a=0,b=0,aa=0,bb=0,ab=0;int count=100;
            for(int sy=0;sy<10;sy++)for(int sx=part*10;sx<(part+1)*10;sx++) {
                int color=pixels[(y+(2*sy+1)*th/20)*width+x+(2*sx+1)*tw/40];
                int r=color>>16&255,g=color>>8&255,blue=color&255,low=Math.min(r,Math.min(g,blue)),high=Math.max(r,Math.max(g,blue));
                double v=Math.max(0,(3*low-high)/2),t=COARSE[style][sy*20+sx];a+=v;b+=t;aa+=v*v;bb+=t*t;ab+=v*t;
            }
            double variance=aa-a*a/count,denominator=Math.sqrt(Math.max(0,variance*(bb-b*b/count)));
            if(variance/count<85||denominator==0)return -1;worst=Math.min(worst,(ab-a*b/count)/denominator);
        }
        return worst;
    }
    static double coarse(int[] p,int w,int x,int y,int tw,int th) {
        double best=-1;for(int style=0;style<STYLES;style++)best=Math.max(best,Math.max(SplashVisual.score(p,w,x,y,tw,th,COARSE[style],20,10),colorCoarse(p,w,x,y,tw,th,style)));return best;
    }
    static final class InkWork {int[] values=new int[800],chroma=new int[800],histogram=new int[256];long[] rows=new long[20];}
    private static int quantile(int[] hist,int rank){int count=0;for(int i=0;i<256;i++){count+=hist[i];if(count>=rank)return i;}return 255;}
    static double ink(int[] pixels,int width,int x,int y,int tw,int th,int style,InkWork work) {
        for(int sy=0;sy<20;sy++)for(int sx=0;sx<40;sx++) {
            int color=pixels[(y+(2*sy+1)*th/40)*width+x+(2*sx+1)*tw/80],r=color>>16&255,g=color>>8&255,b=color&255;
            int low=Math.min(r,Math.min(g,b)),high=Math.max(r,Math.max(g,b)),at=sy*40+sx;
            work.values[at]=Math.max(0,(3*low-high)/2);work.chroma[at]=high-low;
        }
        Arrays.fill(work.rows,0);
        for(int part=0;part<2;part++) {
            // A bright rectangle or low-contrast edge must not stand in for a character's internal strokes.
            {
                double a=0,b=0,aa=0,bb=0,ab=0;
                for(int sy=0;sy<20;sy++)for(int sx=part*20;sx<(part+1)*20;sx++) {
                    int at=sy*40+sx;double value=work.values[at],ref=FINE[style][at];a+=value;b+=ref;aa+=value*value;bb+=ref*ref;ab+=value*ref;
                }
                double variance=aa-a*a/400,den=Math.sqrt(Math.max(0,variance*(bb-b*b/400)));
                if(variance/400<85||den==0||(ab-a*b/400)/den<.72)return -1;
            }
            Arrays.fill(work.histogram,0);for(int sy=0;sy<20;sy++)for(int sx=part*20;sx<(part+1)*20;sx++)work.histogram[work.values[sy*40+sx]]++;
            int low=quantile(work.histogram,80),high=quantile(work.histogram,340);if(high-low<15)return -1;
            for(int sy=0;sy<20;sy++)for(int sx=part*20;sx<(part+1)*20;sx++) {
                int at=sy*40+sx;if(work.chroma[at]<=65&&work.values[at]*20>=low*9+high*11)work.rows[sy]|=1L<<sx;
            }
        }
        double worst=1;
        for(int part=0;part<2;part++) {
            long mask=((1L<<20)-1)<<(part*20);int count=0,expected=0,precise=0,exact=0;
            for(int sy=0;sy<20;sy++) {
                long row=work.rows[sy]&mask,near=row;if(sy>0)near|=work.rows[sy-1]&mask;if(sy<19)near|=work.rows[sy+1]&mask;
                near=(near|near<<1|near>>>1)&mask;count+=Long.bitCount(row);expected+=Long.bitCount(near&INK[style][sy]);
                precise+=Long.bitCount(row&NEAR[style][sy]);exact+=Long.bitCount(row&INK[style][sy]);
            }
            if(count<COUNT[style][part]*.60||count>COUNT[style][part]*1.9||2.0*exact/(count+COUNT[style][part])<.75)return -1;
            double recall=expected/(double)COUNT[style][part],precision=precise/(double)count;
            if(recall<.90||precision<.90)return -1;worst=Math.min(worst,2*recall*precision/(recall+precision));
        }
        return worst;
    }
    static double fine(int[] p,int w,int x,int y,int tw,int th,InkWork work) {
        double best=-1;for(int style=0;style<STYLES;style++)best=Math.max(best,gray(p,w,x,y,tw,th,style));
        if(best>=.84)return best;
        for(int style=0;style<STYLES;style++){double ink=ink(p,w,x,y,tw,th,style,work);if(ink>=.95)best=Math.max(best,ink);}return best;
    }
    private SkipGlyphs(){}
}
