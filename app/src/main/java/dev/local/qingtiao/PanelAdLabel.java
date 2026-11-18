package dev.local.qingtiao;

/** Checks the small, light 广告 disclosure just above the recommendation card. */
final class PanelAdLabel {
    private static final int COLUMNS=14, ROWS=8;
    // Luminance of the UI lettering alone, averaged to half scale.
    private static final int[] LABEL={
         5, 1, 7, 5, 4, 4, 4,  2, 4, 5, 5, 6, 6, 2,
         7, 3, 5, 5, 9, 3, 6,  5, 6, 6, 4, 3, 6, 4,
         7, 6,14,17,27,17, 9, 11,24,27,20,17, 6, 4,
         5, 9,29,16,17,16,12, 17,22,24,23,14, 6, 7,
         6, 5,30, 6, 6, 6, 6, 16,24,26,25,25, 8, 3,
         2,12,24, 6, 6, 6, 6, 15,29,22,21,25, 6, 9,
         5,18,16, 6, 6, 6, 6, 18,29,21,22,25, 2, 5,
         5,12, 4, 6, 6, 6, 6,  5, 7, 7, 9,10, 5, 6
    };
    private static final int[] DARK_LABEL={
         8,12, 8, 9,19, 9,11, 12,14,10,11,11, 7, 6,
        13, 3,33,43,79,30,23, 17,61,68,47,30,10,12,
         9,14,97,56,64,59,28, 43,71,91,63,41,13,15,
         8,12,77,10,14, 7,13, 65,70,83,74,68,22, 9,
         9,13,81,10,10,10,10, 38,84,71,70,69,13, 9,
        13,58,49,10,10,10,10, 38,93,69,73,87, 7,11,
        15,31, 6,10,10,10,10, 22,29,12,12,39,10,10,
         5, 8,12,10,10,10,10, 14, 8,13,12, 9, 9, 8
    };

    static final class Position {
        final int x,y;
        Position(int x,int y){this.x=x;this.y=y;}
    }
    static Position find(int[] pixels,int width,int height,int headerY){
        int radius=Math.max(2,Math.round(width*.004f));
        int top=headerY+Math.round(width*.58f),bottom=Math.min(height-radius,headerY+Math.round(width*.94f));
        int maxGap=Math.round(width*.014f);
        int[] glyph=new int[LABEL.length];
        for(int edge=top;edge<bottom;edge++){
            int direction=cardEdge(pixels,width,edge,radius);
            if(direction==0||edge>top&&cardEdge(pixels,width,edge-1,radius)==direction)continue;
            int[] reference=direction<0?LABEL:DARK_LABEL;
            for(int heightPx=Math.round(width*.024f);heightPx<=Math.round(width*.031f);heightPx++){
                for(int gap=-radius;gap<=maxGap;gap++){
                    int y=edge-gap-heightPx;
                    for(int x=Math.round(width*.026f);x<=Math.round(width*.038f);x++){
                        for(int widthPx=Math.round(width*.044f);widthPx<=Math.round(width*.052f);widthPx++){
                            if(letters(pixels,width,x,y,widthPx,heightPx,glyph,reference))
                                return new Position(x+widthPx/2,y+heightPx/2);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static int cardEdge(int[] p,int w,int y,int radius){
        int direction=0;
        for(int i=2;i<=8;i+=2){
            int x=w*i/10;
            int above=light(p[(y-radius)*w+x]), below=light(p[(y+radius)*w+x]);
            int next=below>92?0:above-below>=28?-1:above<=32&&below-above>=22?1:0;
            if(next==0||direction!=0&&next!=direction)return 0;
            direction=next;
        }
        return direction;
    }

    private static boolean letters(int[] p,int w,int x,int y,int tw,int th,int[] glyph,int[] reference){
        for(int row=0;row<ROWS;row++){
            int y0=y+row*th/ROWS,y1=y+(row+1)*th/ROWS;
            int bg=light(p[(y0+y1-1)/2*w+Math.round(w*.108f)]);
            for(int col=0;col<COLUMNS;col++){
                int x0=x+col*tw/COLUMNS,x1=x+(col+1)*tw/COLUMNS;
                int sum=0,count=0;
                for(int sy=y0;sy<Math.max(y0+1,y1);sy++)for(int sx=x0;sx<Math.max(x0+1,x1);sx++){
                    sum+=light(p[sy*w+sx]);
                    count++;
                }
                glyph[row*COLUMNS+col]=sum/count-bg;
            }
        }
        return matchesLetter(glyph,reference,0)&&matchesLetter(glyph,reference,COLUMNS/2);
    }

    private static boolean matchesLetter(int[] glyph,int[] template,int start){
        int a=0,b=0,aa=0,bb=0,ab=0,count=ROWS*COLUMNS/2;
        for(int i=start;i<LABEL.length;i+=COLUMNS){
            for(int j=i;j<i+COLUMNS/2;j++){
                int sample=glyph[j],reference=template[j];
                a+=sample;
                b+=reference;
                aa+=sample*sample;
                bb+=reference*reference;
                ab+=sample*reference;
            }
        }
        int variance=aa*count-a*a,covariance=ab*count-a*b;
        return a>=count*2&&variance>=count*count*12&&covariance>0
            &&covariance>=.80*Math.sqrt((double)variance*(bb*count-b*b));
    }

    private static int light(int rgb){
        return (((rgb>>16)&255)*77+((rgb>>8)&255)*150+(rgb&255)*29)>>8;
    }
    private PanelAdLabel(){}
}
