package com.local.tapsumo;

final class DriveMath {
    static double wrap(double a){while(a>Math.PI)a-=2*Math.PI;while(a<-Math.PI)a+=2*Math.PI;return a;}
    static double clamp(double a,double lo,double hi){return Math.max(lo,Math.min(hi,a));}
    static double approach(double a,double b,double max){return a+clamp(b-a,-max,max);}
    static boolean blocked(double x,double y,double radius,double[][] walls){
        if(x-radius<55||x+radius>345||y-radius<120||y+radius>680)return true;
        for(double[] r:walls){double dx=x-clamp(x,r[0],r[2]),dy=y-clamp(y,r[1],r[3]);if(dx*dx+dy*dy<radius*radius)return true;}return false;
    }
}
