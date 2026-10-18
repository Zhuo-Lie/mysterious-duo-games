package com.local.tapsumo;

/** Free steering, ordered checkpoints and equal physical distances for both cars. */
final class RaceMatch {
    static final String[] MAP_NAMES={"纸上环线","折角街区","沙丘回环"};
    static final double[][][] MAPS={
        {{288,580},{288,220},{252,165},{148,165},{112,220},{112,580},{148,635},{252,635}},
        {{280,585},{280,430},{202,366},{280,225},{245,164},{130,164},{108,255},{152,370},{108,510},{120,625},{224,625}},
        {{280,580},{286,365},{252,170},{148,170},{112,290},{146,410},{110,580},{155,630},{245,630}}
    };
    final double[] x={0,0},y={0,0},heading={0,0},speed={0,0},inputX={0,0},inputY={0,0};
    final int[] completed={0,0},next={1,1};
    int map,lapsToWin=2,winner=-1,events,grid;
    double pace=1,steering=1;
    double[][] track(){return MAPS[map];}
    void reset(){double[] start=track()[0];for(int i=0;i<2;i++){x[i]=start[0]+(i==grid?-12:12);y[i]=start[1];heading[i]=-Math.PI/2;speed[i]=inputX[i]=inputY[i]=0;completed[i]=0;next[i]=1;}winner=-1;events=0;}
    void release(){inputX[0]=inputX[1]=inputY[0]=inputY[1]=0;}
    void steer(int side,double dx,double dy){double d=Math.max(1,Math.hypot(dx,dy));inputX[side]=dx/d;inputY[side]=dy/d;}
    int laps(int side){return completed[side];}
    boolean onRoad(int side){return roadDistance(x[side],y[side])<33;}
    double roadDistance(double px,double py){double best=1e9;double[][] points=track();for(int i=0;i<points.length;i++){double[] a=points[i],b=points[(i+1)%points.length];double dx=b[0]-a[0],dy=b[1]-a[1],t=DriveMath.clamp(((px-a[0])*dx+(py-a[1])*dy)/(dx*dx+dy*dy),0,1);best=Math.min(best,Math.hypot(px-a[0]-t*dx,py-a[1]-t*dy));}return best;}
    void update(double dt){if(winner>=0)return;int n=Math.max(1,(int)Math.ceil(dt*120));double h=dt/n;
        for(int k=0;k<n&&winner<0;k++){
            boolean[] finish={false,false};
            for(int i=0;i<2;i++){
                double magnitude=Math.min(1,Math.hypot(inputX[i],inputY[i]));
                if(magnitude>.13){double error=DriveMath.wrap(Math.atan2(inputY[i],inputX[i])-heading[i]);heading[i]+=DriveMath.clamp(error,-4.3*steering*h,4.3*steering*h);}
                double target=magnitude>.13?185*pace*magnitude:0;if(!onRoad(i))target*=.38;
                speed[i]=DriveMath.approach(speed[i],target,(target<speed[i]?400:255)*h);
                x[i]=DriveMath.clamp(x[i]+Math.cos(heading[i])*speed[i]*h,65,335);y[i]=DriveMath.clamp(y[i]+Math.sin(heading[i])*speed[i]*h,130,670);
                double[] goal=track()[next[i]];
                if(Math.hypot(x[i]-goal[0],y[i]-goal[1])<32){if(next[i]==0){completed[i]++;events|=2;if(completed[i]>=lapsToWin)finish[i]=true;}next[i]=(next[i]+1)%track().length;}
            }
            if(finish[0]||finish[1])winner=finish[0]&&finish[1]?2:finish[0]?0:1;
        }
    }
}
