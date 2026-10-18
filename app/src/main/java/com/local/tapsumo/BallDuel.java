package com.local.tapsumo;

/** Enlarged shared arena; independent pinball arms and per-round obstacles. */
final class BallDuel {
    enum Mode { PING_PONG, HOCKEY, PINBALL }
    static final String[] MAP_NAMES={"流动弹柱","双翼风车","错位组合"};
    static final double[][][] OBSTACLES={{{137,330,23},{263,470,23}},{},{{120,350,19},{280,450,19}}};
    final Mode mode;
    final int[] score={0,0};
    final double[] px={200,200},py={148,652},tx={200,200},ty={148,652},pvx={0,0},pvy={0,0},flip={0,0,0,0};
    double x=200,y=400,vx,vy,serve=.8,clock,flatTime,rescueDirection,pace=1,size=1;
    int winner=-1,events,serveSide,map,target=5,sequence;
    boolean rotateMaps=true;
    final java.util.Random random=new java.util.Random();
    final double[][][] movingObstacles={new double[2][5],new double[0][5],new double[2][5]};
    final double[][][] spinning={new double[0][5],new double[2][5],new double[2][5]};
    double hitX,hitY,hitStrength,hitAge;
    static final String[] RAMP_NAMES={"经典 · 接球加速","渐进 · 越打越快","紧张 · 更快升温"};
    int ramp=1,rallyHits,bestRally,precisionSide=-1;
    double rallySeconds,precisionAge;
    double[][] rotors(){double[][] rods=spinning[map];for(int i=0;i<rods.length;i++){double sign=i==0?1:-1;double[] r=rods[i];r[0]=map==2?200+sign*50:200;r[1]=400-sign*(map==1?100:120);r[2]=clock*(map==1?.8:-.65);r[3]=map==1?38:28;r[4]=map==1?.8:-.65;}return rods;}
    BallDuel(Mode mode){this.mode=mode;}
    double radius(){return mode==Mode.HOCKEY?12*size:10;}
    double malletRadius(){return 27*size;}
    double left(){return 52+radius();}double right(){return 348-radius();}
    double goalHalfWidth(){return mode==Mode.PINBALL?88:72;}
    double[][] obstacles(){
        double[][] base=OBSTACLES[map],out=movingObstacles[map];
        for(int i=0;i<base.length;i++){
            double sign=i%2==0?1:-1,a=clock*(map==1?1.15:.9)+(map==1&&i>=2?Math.PI:0);
            double rx=map==0?28:8,ry=map==0?0:8,w=.9;
            double dy=map==1?ry*(1-Math.cos(a)):ry*Math.sin(2*a),vy=map==1?ry*w*Math.sin(a):2*ry*w*Math.cos(2*a);
            out[i][0]=base[i][0]+sign*rx*Math.sin(a);out[i][1]=base[i][1]+sign*dy;out[i][2]=base[i][2];out[i][3]=sign*rx*w*Math.cos(a);out[i][4]=sign*vy;
        }return out;
    }
    void reset(int starter){score[0]=score[1]=0;winner=-1;events=0;clock=0;hitAge=0;sequence=0;bestRally=0;serveSide=starter;for(int i=0;i<2;i++){px[i]=tx[i]=200;py[i]=ty[i]=i==0?148:652;pvx[i]=pvy[i]=0;}java.util.Arrays.fill(flip,0);center();}
    private void center(){x=200;y=400;vx=vy=0;serve=.85;flatTime=rescueDirection=0;rallySeconds=0;rallyHits=0;precisionSide=-1;precisionAge=0;}
    void aim(int side,double x,double y){tx[side]=clamp(x,mode==Mode.HOCKEY?52+malletRadius():99,mode==Mode.HOCKEY?348-malletRadius():301);if(mode==Mode.HOCKEY)ty[side]=clamp(y,side==0?108+malletRadius():400+malletRadius(),side==0?400-malletRadius():692-malletRadius());}
    boolean press(int side,int arm){int index=side*2+arm;if(mode!=Mode.PINBALL||winner>=0||flip[index]>.035)return false;flip[index]=.20;return true;}
    boolean press(int side){boolean a=press(side,0),b=press(side,1);return a||b;}
    void release(){for(int i=0;i<2;i++){tx[i]=px[i];ty[i]=py[i];}}
    int drainEvents(){int e=events;events=0;return e;}
    double tipY(int side,int arm){double f=flip[side*2+arm],raised=f>0?Math.sin(Math.PI*(1-f/.20)):0;return side==0?153+70*raised:647-70*raised;}
    void update(double dt){if(winner>=0)return;int n=Math.max(1,(int)Math.ceil(dt*240));for(int k=0;k<n;k++)step(dt/n);}
    private void step(double dt){
        if(winner>=0)return;clock+=dt;hitAge=Math.max(0,hitAge-dt);precisionAge=Math.max(0,precisionAge-dt);for(int j=0;j<4;j++)flip[j]=Math.max(0,flip[j]-dt);
        for(int i=0;i<2;i++){double ox=px[i],oy=py[i],dx=tx[i]-ox,dy=ty[i]-oy,d=Math.hypot(dx,dy),max=(mode==Mode.HOCKEY?4200:1050)*dt;if(d>max){dx*=max/d;dy*=max/d;}px[i]+=dx;py[i]+=dy;pvx[i]=dx/dt;pvy[i]=dy/dt;double speed=Math.hypot(pvx[i],pvy[i]);if(mode==Mode.HOCKEY&&speed>1500){pvx[i]*=1500/speed;pvy[i]*=1500/speed;}}
        if(serve>0){serve-=dt;if(serve<=0){vx=(sequence++%2==0?1:-1)*100*pace;vy=(serveSide==0?-1:1)*280*pace;}return;}
        if(mode==Mode.PING_PONG){rallySeconds+=dt;if(ramp>0&&rallySeconds>2){double speed=Math.hypot(vx,vy),next=Math.min(690*pace,speed+(ramp==1?9:16)*pace*dt);if(speed>.001){vx*=next/speed;vy*=next/speed;}}}
        double oldY=y;x+=vx*dt;y+=vy*dt;
        if(x<left()){x=left();impact(52,y,Math.abs(vx));vx=Math.abs(vx)*(mode==Mode.HOCKEY?.97:1);events|=1;}if(x>right()){x=right();impact(348,y,Math.abs(vx));vx=-Math.abs(vx)*(mode==Mode.HOCKEY?.97:1);events|=1;}
        if(mode==Mode.PING_PONG){
            for(int i=0;i<2;i++){double plane=py[i]+(i==0?20:-20);boolean crossing=i==0?vy<0&&oldY>=plane&&y<=plane:vy>0&&oldY<=plane&&y>=plane;
                if(crossing&&Math.abs(x-px[i])<=59){double speed=Math.min(690*pace,Math.hypot(vx,vy)+20*pace),offset=clamp((x-px[i])/53,-1,1);vx=clamp(offset*speed*.78+pvx[i]*.05,-speed*.87,speed*.87);vy=(i==0?1:-1)*Math.sqrt(speed*speed-vx*vx);y=plane;rallyHits++;bestRally=Math.max(bestRally,rallyHits);if(Math.abs(offset)<.22){precisionSide=i;precisionAge=.42;}impact(x,plane,speed);events|=1;}}
            if(y<95)goal(1);else if(y>705)goal(0);
        }else{
            if(mode==Mode.HOCKEY){for(int i=0;i<2;i++)contact(px[i],py[i],malletRadius(),pvx[i],pvy[i],0);vx*=Math.exp(-.17*dt);vy*=Math.exp(-.17*dt);}
            else{
                for(int side=0;side<2;side++)for(int arm=0;arm<2;arm++)flipper(side,arm);
                for(double[] b:obstacles())contact(b[0],b[1],b[2],b[3],b[4],60);
                for(double[] r:rotors()){double ux=Math.cos(r[2]),uy=Math.sin(r[2]),along=clamp((x-r[0])*ux+(y-r[1])*uy,-r[3],r[3]),dx=ux*along,dy=uy*along;contact(r[0]+dx,r[1]+dy,7,-r[4]*dy,r[4]*dx,10);}
                flatTime=Math.abs(vy)<55?flatTime+dt:0;
                if(flatTime>1.5&&rescueDirection==0){rescueDirection=Math.abs(vy)>.01?Math.signum(vy):serveSide==0?-1:1;flatTime=0;}
                if(rescueDirection!=0){if(vy*rescueDirection<0)rescueDirection=0;else{vy+=rescueDirection*240*dt;if(Math.abs(vy)>130)rescueDirection=0;}}
            }
            double opening=goalHalfWidth(),lo=200-opening+radius(),hi=200+opening-radius(),top=108+radius(),bottom=692-radius();
            if(y<top){if(oldY>=top&&(x<lo||x>hi)){y=top;vy=Math.abs(vy);events|=1;}else if(oldY<top){if(x<lo){x=lo;vx=Math.abs(vx);}if(x>hi){x=hi;vx=-Math.abs(vx);}if(vy<=0&&vy>-85)vy=-85;}}
            if(y>bottom){if(oldY<=bottom&&(x<lo||x>hi)){y=bottom;vy=-Math.abs(vy);events|=1;}else if(oldY>bottom){if(x<lo){x=lo;vx=Math.abs(vx);}if(x>hi){x=hi;vx=-Math.abs(vx);}if(vy>=0&&vy<85)vy=85;}}
            if(y<97)goal(1);else if(y>703)goal(0);
        }
        if(x<left()){x=left();vx=Math.abs(vx);}if(x>right()){x=right();vx=-Math.abs(vx);}double speed=Math.hypot(vx,vy);if(speed>850*pace){vx*=850*pace/speed;vy*=850*pace/speed;}
    }
    void impact(double xx,double yy,double speed){hitX=xx;hitY=yy;hitStrength=Math.min(1,speed/650);hitAge=.14;}
    private void contact(double bx,double by,double radius,double mx,double my,double boost){double dx=x-bx,dy=y-by,dist=Math.hypot(dx,dy),limit=radius+radius();if(dist>=limit)return;if(dist<.001){dx=0;dy=vy>=0?-1:1;dist=1;}double nx=dx/dist,ny=dy/dist;x=bx+nx*(limit+.1);y=by+ny*(limit+.1);double relative=(vx-mx)*nx+(vy-my)*ny;if(relative<0){double restitution=mode==Mode.HOCKEY?.92:.96;hitX=x-nx*radius();hitY=y-ny*radius();hitStrength=Math.min(1,-relative/650);hitAge=.14;vx-=(1+restitution)*relative*nx;vy-=(1+restitution)*relative*ny;vx+=nx*boost;vy+=ny*boost;events|=1;}}
    private void flipper(int side,int arm){double ax=arm==0?64:336,ay=side==0?180:620,bx=arm==0?156:244,by=tipY(side,arm),dx=bx-ax,dy=by-ay,t=clamp(((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy),0,1),active=flip[side*2+arm]>.05?(side==0?550:-550):0;contact(ax+t*dx,ay+t*dy,10,0,active,0);}
    private void goal(int side){score[side]++;events|=2;serveSide=1-side;if(score[side]>=target){winner=side;vx=vy=0;}else{if(mode==Mode.PINBALL&&rotateMaps)map=(map+1+random.nextInt(2))%3;center();}}
    static double clamp(double x,double min,double max){return Math.max(min,Math.min(max,x));}
}
