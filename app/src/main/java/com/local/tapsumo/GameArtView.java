package com.local.tapsumo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Eight original vector scenes, using the sumo palette and heavy silhouettes. */
final class GameArtView extends View {
    private final GameCatalog.Id id;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private Canvas c;
    private static final int INK=ArcadeTheme.INK,RED=ArcadeTheme.RED,BLUE=ArcadeTheme.BLUE;
    private static final int PAPER=ArcadeTheme.PAPER,YELLOW=ArcadeTheme.YELLOW,SKIN=0xffffd297;
    GameArtView(Context context,GameCatalog.Id id){
        super(context);this.id=id;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    int background(){
        switch(id){
            case SUMO:return YELLOW;
            case PING_PONG:return 0xff54bc65;
            case POOL:return 0xff31bfc2;
            case TIC_TAC_TOE:return RED;
            case AIR_HOCKEY:return 0xff4a99de;
            case PINBALL:return 0xff8d74b3;
            case RACING:return 0xff88cb45;
            case TANK:return 0xffd7bd74;
            default:return 0xffe7b771;
        }
    }
    void fill(int color){p.setStyle(Paint.Style.FILL);p.setColor(color);}
    void edge(float width){p.setStyle(Paint.Style.STROKE);p.setColor(INK);p.setStrokeWidth(width);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);}
    void rect(float l,float t,float r,float b,float radius,int color,float width){
        fill(color);c.drawRoundRect(l,t,r,b,radius,radius,p);
        if(width>0){edge(width);c.drawRoundRect(l,t,r,b,radius,radius,p);}
    }
    void oval(float l,float t,float r,float b,int color,float width){
        fill(color);c.drawOval(l,t,r,b,p);if(width>0){edge(width);c.drawOval(l,t,r,b,p);}
    }
    void line(float x,float y,float x2,float y2,int color,float width){
        edge(width);p.setColor(color);c.drawLine(x,y,x2,y2,p);
    }
    @Override protected void onDraw(Canvas canvas){
        c=canvas;float scale=Math.min(getWidth()/320f,getHeight()/180f);
        if(scale<=0)return;
        c.save();c.translate((getWidth()-320*scale)/2,(getHeight()-180*scale)/2);c.scale(scale,scale);
        rect(0,0,320,180,15,background(),0);
        c.save();path.reset();path.addRoundRect(0,0,320,180,15,15,Path.Direction.CW);c.clipPath(path);
        for(int i=-1;i<7;i++)line(i*80,-10,i*80+100,190,0x16ffffff,22);
        switch(id){
            case SUMO:sumo();break;
            case PING_PONG:pingPong();break;
            case POOL:pool();break;
            case TIC_TAC_TOE:ticTacToe();break;
            case AIR_HOCKEY:hockey();break;
            case PINBALL:pinball();break;
            case RACING:racing();break;
            case GOMOKU:gomoku();break;
            case TANK:tanks();break;
        }
        c.restore();c.restore();
    }
    private void wrestler(float x,float y,int color,float rotation){
        c.save();c.translate(x,y);c.rotate(rotation);
        oval(-39,18,39,38,0x28202229,0);
        oval(-27,13,-8,33,SKIN,5);oval(8,13,27,33,SKIN,5);
        oval(-36,-26,36,22,SKIN,6);rect(-29,7,29,22,6,color,5);
        rect(-8,11,8,30,3,color,4);
        oval(-45,-32,-23,-8,SKIN,5);oval(23,-32,45,-8,SKIN,5);
        oval(-23,-40,23,-4,INK,5);oval(-20,-30,20,0,SKIN,4);
        fill(INK);path.reset();path.moveTo(-21,-23);path.quadTo(-25,-42,0,-38);
        path.quadTo(26,-39,21,-23);path.lineTo(4,-31);path.quadTo(-5,-20,-21,-23);c.drawPath(path,p);
        oval(-8,-49,8,-36,INK,3);line(-7,-36,7,-36,color,4);
        line(-13,-15,-6,-13,INK,3);line(6,-13,13,-15,INK,3);line(-3,-4,3,-4,INK,2);
        c.restore();
    }
    private void sumo(){
        oval(63,27,269,179,0x33202229,0);
        oval(54,13,263,170,0xffdda134,6);oval(54,7,263,163,0xffffdf85,6);
        p.setStyle(Paint.Style.STROKE);p.setColor(PAPER);p.setStrokeWidth(7);c.drawOval(73,22,244,149,p);
        wrestler(114,92,RED,72);wrestler(203,88,BLUE,-106);
        line(153,35,160,24,INK,4);line(168,36,174,26,INK,4);
        line(156,143,162,153,PAPER,4);line(169,143,175,153,PAPER,4);
    }
    private void pingPong(){
        c.save();c.rotate(-12,160,90);
        rect(49,20,273,164,3,0xff20864c,7);
        line(60,32,263,32,PAPER,3);line(60,152,263,152,PAPER,3);
        line(60,32,60,152,PAPER,3);line(263,32,263,152,PAPER,3);
        line(60,92,263,92,PAPER,2);rect(155,37,232,51,6,RED,5);rect(80,132,157,146,6,BLUE,5);
        c.restore();
        line(136,111,190,67,0x88fff9e5,10);oval(181,49,202,70,YELLOW,4);
    }
    private void pool(){
        c.save();c.rotate(-12,160,90);
        rect(41,10,280,172,8,0xffa36540,7);rect(54,23,267,159,3,0xff31a75a,5);
        for(int x=57;x<270;x+=103){oval(x-8,18,x+8,35,INK,0);oval(x-8,146,x+8,163,INK,0);}
        line(20,138,115,104,0xfff9d990,6);line(92,113,119,103,PAPER,4);
        oval(118,89,137,108,PAPER,4);
        for(int row=0;row<4;row++)for(int j=0;j<=row;j++)
            oval(210+row*12,61+j*20-row*10,226+row*12,77+j*20-row*10,(row+j)%2==0?RED:BLUE,3);
        c.restore();
    }
    private void ticTacToe(){
        c.save();c.rotate(12,160,90);
        for(int n=0;n<2;n++){line(130+n*60,17,130+n*60,162,INK,7);line(84,65+n*52,237,65+n*52,INK,7);}
        oval(90,21,120,51,BLUE,5);oval(151,73,181,103,BLUE,5);oval(210,126,240,156,BLUE,5);
        cross(210,26,25);cross(90,126,25);cross(151,21,25);c.restore();
    }
    private void cross(float x,float y,float s){line(x,y,x+s,y+s,PAPER,9);line(x+s,y,x,y+s,PAPER,9);line(x,y,x+s,y+s,INK,3);line(x+s,y,x,y+s,INK,3);}
    private void hockey(){
        c.save();c.rotate(-13,160,90);
        rect(43,10,281,169,19,INK,0);rect(49,9,275,161,15,PAPER,6);
        line(161,11,161,158,RED,4);p.setStyle(Paint.Style.STROKE);p.setColor(0xff9ec7d7);p.setStrokeWidth(3);c.drawCircle(161,86,30,p);
        line(52,63,52,111,BLUE,8);line(272,63,272,111,RED,8);
        oval(68,64,110,106,BLUE,5);oval(78,74,100,96,PAPER,4);
        oval(214,64,256,106,RED,5);oval(224,74,246,96,PAPER,4);
        line(158,121,190,99,0x553a687d,12);oval(182,87,204,109,INK,3);c.restore();
    }
    private void pinball(){
        rect(59,-19,264,202,27,0xff363c64,7);rect(75,-12,248,204,19,0xff575187,4);
        oval(103,19,143,59,YELLOW,5);oval(180,53,220,93,BLUE,5);
        oval(135,63,171,99,RED,5);
        line(88,99,106,119,PAPER,5);line(234,105,216,123,PAPER,5);
        c.save();c.rotate(15,120,151);rect(84,137,145,157,10,RED,5);c.restore();
        c.save();c.rotate(-15,207,151);rect(175,137,236,157,10,BLUE,5);c.restore();
        line(183,109,201,87,0x66fff9e5,9);oval(193,74,215,96,PAPER,5);
    }
    private void car(float x,float y,int color){
        c.save();c.translate(x,y);
        rect(-25,-24,-15,-7,4,INK,3);rect(15,-24,25,-7,4,INK,3);
        rect(-25,16,-15,33,4,INK,3);rect(15,16,25,33,4,INK,3);
        rect(-17,-29,17,33,8,color,5);rect(-11,-6,11,12,5,INK,4);
        rect(-24,-34,24,-23,3,color,4);rect(-23,28,23,38,3,color,4);
        line(-7,-19,7,-19,PAPER,3);c.restore();
    }
    private void racing(){
        c.save();c.rotate(20,160,90);rect(65,-60,258,241,0,0xff44474d,6);
        line(72,-10,72,200,PAPER,5);line(251,-10,251,200,PAPER,5);
        for(int y=-20;y<220;y+=39)line(161,y,161,y+18,YELLOW,4);
        car(117,112,BLUE);car(209,65,RED);c.restore();
        for(int i=0;i<4;i++)oval(14+i*12,25+i*36,33+i*12,44+i*36,INK,0);
    }
    private void gomoku(){
        c.save();c.rotate(-10,160,90);rect(66,-9,259,190,5,0xfff4cc8d,6);
        for(int i=0;i<8;i++){line(79+i*24,8,79+i*24,176,0xff956d3a,1.8f);line(79,8+i*24,247,8+i*24,0xff956d3a,1.8f);}
        int[][] black={{1,1},{2,2},{3,3},{4,4}},white={{2,1},{3,2},{3,4},{4,3}};
        for(int[] pos:black)stone(79+pos[0]*24,8+pos[1]*24,false);
        for(int[] pos:white)stone(79+pos[0]*24,8+pos[1]*24,true);
        p.setStyle(Paint.Style.STROKE);p.setColor(RED);p.setStrokeWidth(3);c.drawCircle(175,104,13,p);c.restore();
    }
    private void stone(float x,float y,boolean white){
        oval(x-10,y-7,x+12,y+13,0x33202229,0);oval(x-10,y-10,x+10,y+10,white?PAPER:INK,2);
        oval(x-5,y-6,x-1,y-2,white?Color.WHITE:0xff6b6b70,0);
    }
    private void tanks(){
        rect(121,14,148,80,4,0xffa78b65,5);rect(178,108,205,174,4,0xffa78b65,5);
        for(int i=0;i<2;i++){c.save();c.translate(i==0?82:245,i==0?119:65);c.rotate(i==0?-45:135);rect(-29,-32,-15,33,5,INK,3);rect(15,-32,29,33,5,INK,3);rect(-20,-27,20,28,7,i==0?RED:BLUE,5);line(0,0,0,-49,INK,13);line(0,0,0,-49,i==0?RED:BLUE,7);oval(-14,-14,14,14,i==0?RED:BLUE,4);c.restore();}
        oval(150,85,164,99,YELLOW,3);line(139,113,155,97,PAPER,5);
    }
}
