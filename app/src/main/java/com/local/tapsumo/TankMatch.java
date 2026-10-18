package com.local.tapsumo;

import java.util.ArrayList;

/** Differential tracks, independently rotating turret and bounded ricochets. */
final class TankMatch {
    static final String[] MAP_NAMES={"交错掩体","十字回廊","双岛要塞"};
    static final double[][][] MAPS={
        {{88,280,180,304},{220,496,312,520},{235,315,258,390},{142,410,165,485}},
        {{85,280,150,304},{250,496,315,520},{188,352,212,448}},
        {{100,290,164,350},{236,450,300,510},{235,250,258,320},{142,480,165,550}}
    };
    static final double SHELL_RADIUS=6;
    double clashX,clashY,clashLife;
    static final class Shell {double x,y,vx,vy,age;int owner,bounces;Shell(double x,double y,double a,int owner,int bounces){this.x=x;this.y=y;vx=Math.cos(a)*310;vy=Math.sin(a)*310;this.owner=owner;this.bounces=bounces;}}
    final double[] x={200,200},y={170,630},heading={Math.PI/2,-Math.PI/2},turret={Math.PI/2,-Math.PI/2},moveX={0,0},moveY={0,0},aimX={0,0},aimY={0,0},cooldown={0,0},shield={0,0},tread={0,0};
    final int[] hp={3,3},score={0,0};
    final double[] leftTread={0,0},rightTread={0,0};
    final ArrayList<Shell> shells=new ArrayList<Shell>();
    int map,target=3,maxHp=3,ricochets=1,winner=-1,events;
    double pace=1,roundPause;
    boolean rotateMaps=true;
    boolean destructible=true;
    int coverHits=3,wallMap=-1;
    int[] wallHp=new int[0],pendingDamage=new int[0];
    double[] wallFlash=new double[0];
    double[][] liveWalls=new double[0][];
    double coverX,coverY;
    final java.util.Random random=new java.util.Random();
    double[][] walls(){if(wallMap!=map)resetCover();return liveWalls;}
    void resetCover(){wallMap=map;wallHp=new int[MAPS[map].length];pendingDamage=new int[wallHp.length];wallFlash=new double[wallHp.length];java.util.Arrays.fill(wallHp,coverHits);rebuildWalls();}
    void rebuildWalls(){int count=0;for(int hp:wallHp)if(hp>0)count++;liveWalls=new double[count][];int j=0;for(int i=0;i<wallHp.length;i++)if(wallHp[i]>0)liveWalls[j++]=MAPS[map][i];}
    int coverAt(double x,double y,double radius){walls();for(int i=0;i<wallHp.length;i++){double[] r=MAPS[map][i];if(wallHp[i]>0){double dx=x-DriveMath.clamp(x,r[0],r[2]),dy=y-DriveMath.clamp(y,r[1],r[3]);if(dx*dx+dy*dy<radius*radius)return i;}}return -1;}
    void reset(){winner=-1;events=0;roundPause=0;clashLife=0;shells.clear();resetCover();for(int i=0;i<2;i++){score[i]=0;spawn(i);}release();}
    void spawn(int i){x[i]=200;y[i]=i==0?170:630;heading[i]=turret[i]=i==0?Math.PI/2:-Math.PI/2;hp[i]=maxHp;cooldown[i]=.8;shield[i]=1.4;tread[i]=leftTread[i]=rightTread[i]=0;}
    void release(){for(int i=0;i<2;i++){moveX[i]=moveY[i]=aimX[i]=aimY[i]=0;}}
    void control(int side,int stick,double dx,double dy){double d=Math.max(1,Math.hypot(dx,dy));if(stick==0){moveX[side]=dx/d;moveY[side]=dy/d;}else{aimX[side]=dx/d;aimY[side]=dy/d;}}
    void update(double dt){
        if(winner>=0)return;int n=Math.max(1,(int)Math.ceil(dt*240));double h=dt/n;
        for(int step=0;step<n&&winner<0;step++){
            clashLife=Math.max(0,clashLife-h);
            walls();java.util.Arrays.fill(pendingDamage,0);for(int i=0;i<wallFlash.length;i++)wallFlash[i]=Math.max(0,wallFlash[i]-h);
            if(roundPause>0){roundPause=Math.max(0,roundPause-h);continue;}
            for(int i=0;i<2;i++){
                cooldown[i]=Math.max(0,cooldown[i]-h);shield[i]=Math.max(0,shield[i]-h);
                double magnitude=Math.min(1,Math.hypot(moveX[i],moveY[i]));
                if(magnitude>.10){
                    double error=DriveMath.wrap(Math.atan2(moveY[i],moveX[i])-heading[i]),sign=1;
                    if(Math.abs(error)>2.15){error=DriveMath.wrap(error+Math.PI);sign=-1;}
                    double omega=DriveMath.clamp(error*5,-3.5,3.5),v=sign*108*pace*magnitude*Math.max(0,Math.cos(error));
                    double left=v-omega*14,right=v+omega*14;heading[i]+=(right-left)/28*h;
                    double nx=x[i]+Math.cos(heading[i])*(left+right)*.5*h,ny=y[i]+Math.sin(heading[i])*(left+right)*.5*h;
                    int other=1-i;if(!DriveMath.blocked(nx,ny,16,walls())&&Math.hypot(nx-x[other],ny-y[other])>=34){x[i]=nx;y[i]=ny;}
                    tread[i]+=(Math.abs(left)+Math.abs(right))*h;
                    leftTread[i]+=left*h;rightTread[i]+=right*h;
                }
                if(Math.hypot(aimX[i],aimY[i])>.22){
                    double error=DriveMath.wrap(Math.atan2(aimY[i],aimX[i])-turret[i]);turret[i]+=DriveMath.clamp(error,-5*h,5*h);
                    if(cooldown[i]<=0&&Math.abs(error)<.17){double sx=x[i]+Math.cos(turret[i])*25,sy=y[i]+Math.sin(turret[i])*25;if(!DriveMath.blocked(sx,sy,SHELL_RADIUS,walls())||(destructible&&coverAt(sx,sy,SHELL_RADIUS)>=0))shells.add(new Shell(sx,sy,turret[i],i,ricochets));cooldown[i]=.55;events|=4;shield[i]=0;}
                }
            }
            boolean[] dead={false,false};
            // Integrate every projectile before resolving opposing pairs and tank damage.
            for(int j=shells.size()-1;j>=0;j--){Shell s=shells.get(j);s.age+=h;double oldX=s.x,oldY=s.y;s.x+=s.vx*h;s.y+=s.vy*h;
                int cover=coverAt(oldX,oldY,SHELL_RADIUS);if(cover<0)cover=coverAt(s.x,s.y,SHELL_RADIUS);
                if(destructible&&cover>=0){pendingDamage[cover]++;coverX=s.x;coverY=s.y;shells.remove(j);events|=32;continue;}
                if(DriveMath.blocked(s.x,s.y,SHELL_RADIUS,walls())){if(s.bounces--<=0){shells.remove(j);continue;}boolean hitX=DriveMath.blocked(s.x,oldY,SHELL_RADIUS,walls()),hitY=DriveMath.blocked(oldX,s.y,SHELL_RADIUS,walls());if(hitX||!hitY)s.vx=-s.vx;if(hitY)s.vy=-s.vy;s.x=oldX;s.y=oldY;}
                if(s.age>3.2)shells.remove(j);
            }
            // Every shell in this substep sees the same cover snapshot.
            boolean destroyed=false;for(int i=0;i<wallHp.length;i++)if(pendingDamage[i]>0){wallHp[i]=Math.max(0,wallHp[i]-pendingDamage[i]);wallFlash[i]=.16;if(wallHp[i]==0){destroyed=true;events|=64;}}
            if(destroyed)rebuildWalls();
            boolean[] removed=new boolean[shells.size()];
            for(int a=0;a<shells.size();a++)for(int b=a+1;b<shells.size();b++){
                Shell sa=shells.get(a),sb=shells.get(b);
                if(!removed[a]&&!removed[b]&&sa.owner!=sb.owner&&Math.hypot(sa.x-sb.x,sa.y-sb.y)<=SHELL_RADIUS*2){removed[a]=removed[b]=true;clashX=(sa.x+sb.x)/2;clashY=(sa.y+sb.y)/2;clashLife=.20;events|=16;}
            }
            for(int j=removed.length-1;j>=0;j--)if(removed[j])shells.remove(j);
            for(int j=shells.size()-1;j>=0;j--){Shell s=shells.get(j);
                int victim=1-s.owner;
                if(shield[victim]<=0&&Math.hypot(s.x-x[victim],s.y-y[victim])<16+SHELL_RADIUS){hp[victim]--;shells.remove(j);events|=1;if(hp[victim]<=0)dead[victim]=true;continue;}
                if(s.age>3.2)shells.remove(j);
            }
            for(int i=0;i<2;i++)if(dead[i]){score[1-i]++;events|=2;}
            if(score[0]>=target||score[1]>=target)winner=score[0]>=target&&score[1]>=target?2:score[0]>=target?0:1;
            if((dead[0]||dead[1])&&winner<0){shells.clear();if(rotateMaps)map=(map+1+random.nextInt(2))%3;resetCover();spawn(0);spawn(1);roundPause=1.1;}
        }
    }
}
