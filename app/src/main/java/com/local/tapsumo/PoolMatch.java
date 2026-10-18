package com.local.tapsumo;

/** Ten targets plus cue. First legal pot chooses a color independent of player. */
final class PoolMatch {
    static final class Ball {
        double x,y,vx,vy;final int owner;boolean sunk;
        Ball(double x,double y,int owner){this.x=x;this.y=y;this.owner=owner;}
    }
    final Ball[] balls=new Ball[11];
    final int[] score={0,0};
    final int[] group={-1,-1},colorScore={0,0};
    final boolean[] shotSunk=new boolean[11];
    int turn,winner=-1,events;
    boolean moving,scratch,scratchLoss;
    boolean ballInHand,placementSelected;
    double placementX,placementY;
    double shotTime;
    double hitX,hitY,hitStrength,hitAge;
    double pace=1;
    private int ownBefore,firstColor=-1;
    String notice="首颗合法进球决定目标颜色";
    PoolMatch(int starter){reset(starter);}
    // First contact preview uses the same radius and bounds as the simulation.
    // Returns cue-ball contact center and object-ball index (-1 for cushion).
    double[] preview(double dx,double dy){
        Ball cue=balls[0];double length=Math.hypot(dx,dy);if(length<.01)return new double[]{cue.x,cue.y,-1};
        double nx=dx/length,ny=dy/length,distance=Double.POSITIVE_INFINITY;int hit=-1;
        if(nx>0)distance=(340-cue.x)/nx;else if(nx<0)distance=(60-cue.x)/nx;
        if(ny>0)distance=Math.min(distance,(630-cue.y)/ny);else if(ny<0)distance=Math.min(distance,(170-cue.y)/ny);
        for(int i=1;i<balls.length;i++){
            Ball b=balls[i];if(b.sunk)continue;
            double bx=b.x-cue.x,by=b.y-cue.y,along=bx*nx+by*ny,perp=bx*bx+by*by-along*along;
            if(along<=0||perp>400)continue;
            double entry=along-Math.sqrt(Math.max(0,400-perp));
            if(entry>=0&&entry<distance){distance=entry;hit=i;}
        }
        distance=Math.max(0,distance);return new double[]{cue.x+nx*distance,cue.y+ny*distance,hit};
    }
    double[][] targetPath(int index,double contactX,double contactY,double budget,int maxBounces){
        Ball b=balls[index];double nx=b.x-contactX,ny=b.y-contactY,length=Math.hypot(nx,ny);if(length<.001)return new double[][]{{b.x,b.y}};nx/=length;ny/=length;
        java.util.ArrayList<double[]> points=new java.util.ArrayList<double[]>();double x=b.x,y=b.y;points.add(new double[]{x,y});
        for(int bounce=0;bounce<=maxBounces&&budget>0;bounce++){
            double wallX=nx>0?(340-x)/nx:nx<0?(60-x)/nx:1e9,wallY=ny>0?(630-y)/ny:ny<0?(170-y)/ny:1e9;
            double wall=Math.min(wallX,wallY),travel=Math.min(wall,budget);boolean contact=false;
            for(int j=1;j<balls.length;j++){Ball other=balls[j];if(j==index||other.sunk)continue;double dx=other.x-x,dy=other.y-y,along=dx*nx+dy*ny,perp=dx*dx+dy*dy-along*along;if(along>0&&perp<400){double hit=along-Math.sqrt(400-perp);if(hit>=0&&hit<travel){travel=hit;contact=true;}}}
            x+=nx*travel;y+=ny*travel;points.add(new double[]{x,y});budget-=travel;
            if(contact||travel<wall-.001||bounce==maxBounces)break;
            if(wallX<=wallY)nx=-nx;else ny=-ny;
        }return points.toArray(new double[points.size()][]);
    }
    void reset(int starter){hitAge=0;
        ballInHand=placementSelected=false;
        turn=starter;winner=-1;score[0]=score[1]=colorScore[0]=colorScore[1]=0;group[0]=group[1]=-1;moving=scratch=scratchLoss=false;events=0;ownBefore=0;shotTime=0;firstColor=-1;notice="首颗合法进球决定目标颜色";
        balls[0]=new Ball(200,520,-1);int index=1;
        for(int row=0;row<4;row++)for(int j=0;j<=row;j++)balls[index]=new Ball(200+(j-row*.5)*23,315-row*21,(index++-1)%2);
    }
    boolean shoot(double dx,double dy,double power){
        double length=Math.hypot(dx,dy);if(moving||ballInHand||balls[0].sunk||winner>=0||length<.01||power<.04)return false;
        double speed=(50+BallDuel.clamp(power,0,1)*840)*pace;
        balls[0].vx=dx/length*speed;balls[0].vy=dy/length*speed;
        moving=true;scratch=false;shotTime=0;ownBefore=score[turn];firstColor=-1;java.util.Arrays.fill(shotSunk,false);events|=1;notice="球在滚动，停稳后结算";return true;
    }
    void update(double dt){
        hitAge=Math.max(0,hitAge-dt);
        if(!moving||winner>=0)return;
        int steps=Math.max(1,(int)Math.ceil(dt*240));double h=dt/steps;
        for(int n=0;n<steps&&moving;n++){
            shotTime+=h;boolean active=false;double firstPocket=Double.POSITIVE_INFINITY;int substepColor=-1;
            for(Ball b:balls){if(b.sunk)continue;
                double oldX=b.x,oldY=b.y;
                b.x+=b.vx*h;b.y+=b.vy*h;
                if(inPocket(b)){b.sunk=true;b.vx=b.vy=0;if(b.owner>=0){colorScore[b.owner]++;shotSunk[indexOf(b)]=true;double entry=pocketEntry(oldX,oldY,b.x,b.y);if(firstColor<0&&entry<firstPocket){firstPocket=entry;substepColor=b.owner;}events|=2;}else scratch=true;continue;}
                if(b.x<60){impact(50,b.y,Math.abs(b.vx));b.x=60;b.vx=Math.abs(b.vx)*.91;events|=1;}if(b.x>340){impact(350,b.y,Math.abs(b.vx));b.x=340;b.vx=-Math.abs(b.vx)*.91;events|=1;}
                if(b.y<170){impact(b.x,160,Math.abs(b.vy));b.y=170;b.vy=Math.abs(b.vy)*.91;events|=1;}if(b.y>630){impact(b.x,640,Math.abs(b.vy));b.y=630;b.vy=-Math.abs(b.vy)*.91;events|=1;}
                double speed=Math.hypot(b.vx,b.vy),next=Math.max(0,speed-115*h);
                if(next<5){b.vx=b.vy=0;}else{b.vx*=next/speed;b.vy*=next/speed;active=true;}
            }
            if(firstColor<0&&substepColor>=0)firstColor=substepColor;
            for(int i=0;i<balls.length;i++)for(int j=i+1;j<balls.length;j++)collide(balls[i],balls[j]);
            if(!active&&shotTime>.2)finishTurn();
        }
    }
    void impact(double xx,double yy,double speed){hitX=xx;hitY=yy;hitStrength=Math.min(1,speed/500);hitAge=.12;}
    private int indexOf(Ball b){for(int i=1;i<balls.length;i++)if(balls[i]==b)return i;return 0;}
    private double pocketEntry(double oldX,double oldY,double x,double y){
        double best=1,dx=x-oldX,dy=y-oldY,a=dx*dx+dy*dy;
        for(int side=0;side<2;side++)for(int row=0;row<3;row++){
            double px=side==0?50:350,py=160+row*240;if(Math.hypot(x-px,y-py)>=21)continue;
            double ox=oldX-px,oy=oldY-py,c=ox*ox+oy*oy-441;if(c<=0||a<1e-12)return 0;
            double b=2*(ox*dx+oy*dy),disc=b*b-4*a*c;if(disc>=0)best=Math.min(best,BallDuel.clamp((-b-Math.sqrt(disc))/(2*a),0,1));
        }return best;
    }
    private boolean inPocket(Ball b){for(int side=0;side<2;side++)for(int row=0;row<3;row++)if(Math.hypot(b.x-(side==0?50:350),b.y-(160+row*240))<21)return true;return false;}
    private void collide(Ball a,Ball b){
        if(a.sunk||b.sunk)return;double dx=b.x-a.x,dy=b.y-a.y,d=Math.hypot(dx,dy);
        if(d>=20||d<.00001)return;double nx=dx/d,ny=dy/d,overlap=(20-d)/2+.01;
        a.x-=nx*overlap;a.y-=ny*overlap;b.x+=nx*overlap;b.y+=ny*overlap;
        double closing=(a.vx-b.vx)*nx+(a.vy-b.vy)*ny;
        if(closing>0){double impulse=.98*closing;a.vx-=impulse*nx;a.vy-=impulse*ny;b.vx+=impulse*nx;b.vy+=impulse*ny;hitX=(a.x+b.x)/2;hitY=(a.y+b.y)/2;hitStrength=Math.min(1,closing/500);hitAge=.12;events|=1;}
    }
    private void finishTurn(){
        moving=false;
        boolean assignedNow=false;
        if(group[turn]<0&&firstColor>=0&&!scratch){group[turn]=firstColor;group[1-turn]=1-firstColor;assignedNow=true;}
        if(group[turn]<0&&scratch){for(int i=1;i<balls.length;i++)if(shotSunk[i]){colorScore[balls[i].owner]--;placeSafe(balls[i]);}}
        if(group[0]>=0){score[0]=colorScore[group[0]];score[1]=colorScore[group[1]];}
        scratchLoss=scratch&&score[turn]==5;
        if(group[0]>=0){if(score[0]==5&&score[1]==5)winner=scratch?1-turn:turn;else if(score[1-turn]==5)winner=1-turn;else if(score[turn]==5)winner=scratch?1-turn:turn;}
        if(scratch&&winner<0){ballInHand=true;placementSelected=false;balls[0].sunk=true;balls[0].vx=balls[0].vy=0;}
        boolean keep=!scratch&&(assignedNow||score[turn]>ownBefore);
        notice=scratch?"白球落袋 · 对方获得自由球":assignedNow?"目标已分配 · 进本色可继续":keep?"进目标球 · 继续击球":"未进目标球 · 换手";
        if(winner<0&&!keep)turn=1-turn;
    }
    boolean canPlaceCue(double x,double y){
        if(Double.isNaN(x)||Double.isInfinite(x)||Double.isNaN(y)||Double.isInfinite(y)||x<60||x>340||y<170||y>630)return false;
        for(int side=0;side<2;side++)for(int row=0;row<3;row++)if(Math.hypot(x-(side==0?50:350),y-(160+row*240))<24)return false;
        for(int i=1;i<balls.length;i++)if(!balls[i].sunk&&Math.hypot(x-balls[i].x,y-balls[i].y)<22)return false;
        return true;
    }
    void selectCuePlacement(double x,double y){if(!ballInHand||moving||winner>=0)return;placementSelected=true;placementX=x;placementY=y;}
    boolean confirmCuePlacement(){if(!ballInHand||moving||winner>=0||!placementSelected||!canPlaceCue(placementX,placementY))return false;Ball cue=balls[0];cue.x=placementX;cue.y=placementY;cue.vx=cue.vy=0;cue.sunk=false;ballInHand=placementSelected=false;notice="自由球已摆好 · 请瞄准击球";return true;}
    private void placeSafe(Ball cue){
        cue.sunk=false;cue.vx=cue.vy=0;
        for(int row=0;row<18;row++)for(int col=0;col<12;col++){
            double xx=68+col*24,yy=580-row*22;boolean clear=true;
            for(int i=0;i<balls.length;i++){Ball b=balls[i];if(b!=cue&&!b.sunk&&Math.hypot(xx-b.x,yy-b.y)<23){clear=false;break;}}
            if(clear){cue.x=xx;cue.y=yy;return;}
        }
        cue.x=200;cue.y=520;
    }
}
