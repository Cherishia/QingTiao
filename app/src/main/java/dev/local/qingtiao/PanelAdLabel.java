package dev.local.qingtiao;

/** Checks the small, light 广告 disclosure just above the recommendation card. */
final class PanelAdLabel {
    private static final int COLUMNS=14, ROWS=8, LETTER_COLUMNS=COLUMNS/2, SAMPLE_COUNT=ROWS*LETTER_COLUMNS;
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
    private static final Template LIGHT=new Template(LABEL),DARK=new Template(DARK_LABEL);

    private static final class Template {
        final int[] samples;
        final int[] sum=new int[2],variance=new int[2];
        Template(int[] samples){
            this.samples=samples;
            for(int part=0;part<2;part++){
                int first=part*LETTER_COLUMNS, squares=0;
                for(int row=0;row<ROWS;row++)for(int col=first;col<first+LETTER_COLUMNS;col++){
                    int value=samples[row*COLUMNS+col];
                    sum[part]+=value;
                    squares+=value*value;
                }
                variance[part]=squares*SAMPLE_COUNT-sum[part]*sum[part];
            }
        }
    }

    static final class Position {
        final int x,y;
        Position(int x,int y){this.x=x;this.y=y;}
    }
    static boolean matchesPosition(int w,int h,int headerY,int x,int y){
        int offset=y-headerY;
        return x>=w*.04&&x<=w*.075&&y<h&&offset>=w*.53&&offset<=w*.94;
    }
    static Position find(int[] pixels,int width,int height,int headerY){
        int radius=Math.max(2,Math.round(width*.004f));
        int top=headerY+Math.round(width*.58f),bottom=Math.min(height-radius,headerY+Math.round(width*.94f));
        int maxGap=Math.round(width*.014f),backgroundX=Math.round(width*.108f);
        int firstHeight=Math.round(width*.024f),lastHeight=Math.round(width*.031f);
        int firstX=Math.round(width*.026f),lastX=Math.round(width*.038f);
        int firstWidth=Math.round(width*.044f),lastWidth=Math.round(width*.052f);
        for(int edge=top;edge<bottom;edge++){
            int direction=cardEdge(pixels,width,edge,radius);
            if(direction==0||edge>top&&cardEdge(pixels,width,edge-1,radius)==direction)continue;
            Template reference=direction<0?LIGHT:DARK;
            for(int heightPx=firstHeight;heightPx<=lastHeight;heightPx++){
                for(int gap=-radius;gap<=maxGap;gap++){
                    int y=edge-gap-heightPx;
                    for(int x=firstX;x<=lastX;x++){
                        for(int widthPx=firstWidth;widthPx<=lastWidth;widthPx++){
                            if(matchesLetter(pixels,width,x,y,widthPx,heightPx,backgroundX,reference,0)
                                &&matchesLetter(pixels,width,x,y,widthPx,heightPx,backgroundX,reference,1))
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

    private static boolean matchesLetter(int[] p,int w,int x,int y,int tw,int th,int backgroundX,Template template,int part){
        int a=0,aa=0,ab=0,first=part*LETTER_COLUMNS;
        for(int row=0;row<ROWS;row++){
            int y0=y+row*th/ROWS,y1=y+(row+1)*th/ROWS;
            int bg=light(p[(y0+y1-1)/2*w+backgroundX]);
            int endY=Math.max(y0+1,y1);
            for(int col=first;col<first+LETTER_COLUMNS;col++){
                int x0=x+col*tw/COLUMNS,x1=x+(col+1)*tw/COLUMNS;
                int endX=Math.max(x0+1,x1),sum=0;
                for(int sy=y0;sy<endY;sy++)for(int sx=x0;sx<endX;sx++)sum+=light(p[sy*w+sx]);
                int sample=sum/((endY-y0)*(endX-x0))-bg;
                a+=sample;
                aa+=sample*sample;
                ab+=sample*template.samples[row*COLUMNS+col];
            }
        }
        int variance=aa*SAMPLE_COUNT-a*a,covariance=ab*SAMPLE_COUNT-a*template.sum[part];
        return a>=SAMPLE_COUNT*2&&variance>=SAMPLE_COUNT*SAMPLE_COUNT*12&&covariance>0
            &&covariance>=.80*Math.sqrt((double)variance*template.variance[part]);
    }

    private static int light(int rgb){
        return (((rgb>>16)&255)*77+((rgb>>8)&255)*150+(rgb&255)*29)>>8;
    }
    private PanelAdLabel(){}
}
