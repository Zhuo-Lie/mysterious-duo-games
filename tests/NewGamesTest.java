package com.local.tapsumo;

import java.util.Random;

public final class NewGamesTest {
    static int checks;
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void near(double a,double b,double tolerance,String message){check(Math.abs(a-b)<=tolerance,message+": "+a+" / "+b);}
    static void finite(double value){check(Double.isFinite(value),"Finite physics state");}
    static void boards(){
        int[][] dirs={{1,0},{0,1},{1,1},{1,-1}};
        for(int[] d:dirs){BoardMatch m=new BoardMatch(15,5,0);int sy=d[1]<0?8:2;for(int i=0;i<5;i++){check(m.play(2+d[0]*i,sy+d[1]*i),"Legal move");if(i<4)check(m.play(10,i),"Alternate");}check(m.winner==0,"Four line directions");check(!m.play(0,0),"Terminal board");}
        BoardMatch m=new BoardMatch(3,3,1);check(m.play(0,0)&&m.turn==0,"Starter");check(!m.play(0,0),"Occupied rejected");check(m.undo()&&m.turn==1,"Undo actual previous side");
        m.reset(0);int[][] draw={{0,0},{1,1},{2,2},{1,0},{1,2},{0,2},{2,0},{2,1},{0,1}};for(int[] xy:draw)check(m.play(xy[0],xy[1]),"Draw legal");check(m.draw,"Draw");
    }
    static void balls(){
        for(BallDuel.Mode mode:BallDuel.Mode.values()){
            BallDuel m=new BallDuel(mode);m.reset(0);int previousMap=m.map;
            for(int n=0;n<5;n++){m.serve=0;m.x=200;m.y=94;m.vx=0;m.vy=-400;m.update(1d/120);if(mode==BallDuel.Mode.PINBALL&&n<4){check(m.map!=previousMap,"Fresh obstacle layout each scoring round");previousMap=m.map;}}
            check(m.winner==1&&m.score[1]==5,"Target score "+mode);m.update(1);check(m.score[1]==5,"Stable terminal score");
        }
        BallDuel ping=new BallDuel(BallDuel.Mode.PING_PONG);ping.reset(0);ping.serve=0;ping.x=230;ping.y=180;ping.vx=0;ping.vy=-400;ping.update(.06);check(ping.vy>0&&ping.vx>0,"Paddle edge changes return angle");
        BallDuel hockey=new BallDuel(BallDuel.Mode.HOCKEY);hockey.reset(0);hockey.serve=0;hockey.x=95;hockey.y=125;hockey.vx=0;hockey.vy=-350;hockey.update(.08);check(hockey.score[1]==0&&hockey.vy>0,"Closed end wall");
        for(int side=0;side<2;side++){hockey.reset(0);hockey.serve=0;hockey.x=140;hockey.y=side==0?115:685;hockey.vx=-250;hockey.vy=side==0?-300:300;hockey.update(.12);check(hockey.score[1-side]==1,"Oblique goal throat");}
        hockey.aim(0,-900,900);check(hockey.tx[0]>=79&&hockey.ty[0]<=373,"Enlarged mallet constrained");
        BallDuel pin=new BallDuel(BallDuel.Mode.PINBALL);pin.reset(0);check(pin.press(0,0)&&pin.flip[1]==0&&pin.flip[2]==0,"Only selected arm moves");check(pin.press(0,1)&&pin.press(1,0)&&pin.press(1,1),"Four independent arms");check(!pin.press(0,0),"Arm recovers before next impulse");
        for(int map=0;map<3;map++){pin=new BallDuel(BallDuel.Mode.PINBALL);pin.map=map;pin.rotateMaps=false;pin.target=3;pin.reset(0);double time=0;while(pin.winner<0&&time<100){if(pin.vy<0&&pin.y<280)pin.press(0,pin.x<200?0:1);pin.update(1d/120);time+=1d/120;}check(pin.winner>=0,"Independent flipper match actually finishes on map "+map);System.out.printf("Pinball map %d: %.1fs, %d:%d%n",map,time,pin.score[0],pin.score[1]);}
        Random random=new Random(44);
        for(BallDuel.Mode mode:BallDuel.Mode.values()){BallDuel m=new BallDuel(mode);m.reset(0);for(int frame=0;frame<16000;frame++){if(m.winner>=0)m.reset(frame%2);if(frame%9==0){m.aim(0,random.nextDouble()*400,random.nextDouble()*400);m.aim(1,random.nextDouble()*400,400+random.nextDouble()*400);m.press(frame%2,frame/2%2);}m.update(1d/120);finite(m.x);finite(m.y);check(m.x>=m.left()-.01&&m.x<=m.right()+.01,"Expanded side bounds");check(Math.hypot(m.vx,m.vy)<=850*m.pace+.01,"Speed cap");}}
    }
    static void settle(PoolMatch m){for(int i=0;i<6000&&m.moving;i++)m.update(1d/240);check(!m.moving,"Shot always settles");}
    static void pot(PoolMatch m,int index){check(m.shoot(1,0,.05),"Start test shot");m.balls[0].vx=m.balls[0].vy=0;m.balls[index].x=50;m.balls[index].y=160;settle(m);}
    static void pool(){
        PoolMatch m=new PoolMatch(1);check(m.balls.length==11,"Ten targets plus cue");check(m.group[0]==-1&&m.group[1]==-1,"Open table");
        pot(m,1);check(m.group[1]==0&&m.group[0]==1,"Blue player may claim red targets");check(m.score[1]==1&&m.turn==1,"First legal pot assigns color and continues");
        pot(m,3);check(m.score[1]==2&&m.turn==1,"Assigned target keeps turn");pot(m,2);check(m.score[0]==1&&m.turn==0,"Opponent target belongs to assigned player");
        m.reset(0);m.shoot(1,0,.05);m.balls[0].vx=m.balls[0].vy=0;m.balls[1].x=72;m.balls[1].y=160;m.balls[1].vx=-480;m.balls[2].x=328.75;m.balls[2].y=160;m.balls[2].vx=480;settle(m);check(m.group[0]==1,"Same-substep first pot uses crossing time, not ball-array order");
        m.reset(0);check(m.shoot(1,0,.1),"Foul shot");m.balls[0].x=50;m.balls[0].y=160;m.balls[0].vx=m.balls[0].vy=0;m.balls[1].x=350;m.balls[1].y=160;settle(m);check(m.group[0]==-1&&!m.balls[1].sunk&&m.colorScore[0]==0&&m.turn==1,"Foul before assignment respots targets, no color chosen");
        m.reset(0);for(int index:new int[]{1,3,5,7,9})pot(m,index);check(m.winner==0&&m.score[0]==5,"Five assigned targets win");
        m.reset(1);for(int index:new int[]{2,4,6,8})pot(m,index);check(m.shoot(1,0,.1),"Clear scratch shot");m.balls[10].x=50;m.balls[10].y=160;m.balls[0].x=350;m.balls[0].y=160;m.balls[0].vx=m.balls[0].vy=0;settle(m);check(m.winner==0&&m.scratchLoss,"Scratch when clearing loses independent of player color");
        m.reset(0);for(int i=1;i<m.balls.length;i++)m.balls[i].sunk=true;m.balls[1].sunk=false;m.balls[1].x=200;m.balls[1].y=300;double[] hit=m.preview(0,-1);near(hit[1],320,.001,"First collision center");
        double[][] line=m.targetPath(1,180,300,155,1);double length=0;for(int i=1;i<line.length;i++)length+=Math.hypot(line[i][0]-line[i-1][0],line[i][1]-line[i-1][1]);near(length,155,.001,"Prediction has finite budget");check(line.length==3&&line[2][0]<340,"One reflected leg, no infinite guide");
    }
    static void race(){
        for(int map=0;map<3;map++){RaceMatch m=new RaceMatch();m.map=map;m.reset();double time=0;while(m.winner<0&&time<90){for(int side=0;side<2;side++){double[] target=m.track()[m.next[side]];double dx=target[0]-m.x[side],dy=target[1]-m.y[side],d=Math.hypot(dx,dy);m.steer(side,dx/d,dy/d);}m.update(1d/120);time+=1d/120;finite(m.x[0]);}check(m.winner>=0,"Joystick bot can finish map "+map);check(m.completed[m.winner==2?0:m.winner]==2,"Complete ordered laps");System.out.printf("Joystick race map %d: %.1fs%n",map,time);}
        RaceMatch m=new RaceMatch();m.reset();double y=m.y[0];m.update(2);near(m.y[0],y,.001,"No automatic motion");m.x[0]=m.track()[4][0];m.y[0]=m.track()[4][1];m.update(.01);check(m.next[0]==1&&m.completed[0]==0,"Cannot skip checkpoints");
        m.reset();m.x[1]=m.x[0];m.y[1]=m.y[0];while(m.winner<0){double[] target=m.track()[m.next[0]];double dx=target[0]-m.x[0],dy=target[1]-m.y[0],d=Math.hypot(dx,dy);m.steer(0,dx/d,dy/d);m.steer(1,dx/d,dy/d);m.update(1d/120);}check(m.winner==2,"Exact same pose and controls tie");
    }
    static void tanks(){
        TankMatch m=new TankMatch();m.reset();double heading=m.heading[0];m.control(0,0,1,0);m.update(.1);check(Math.abs(m.heading[0]-heading)<=.351,"Chassis turn rate bounded");check(m.x[0]-200<3,"No instant side strafe");double oldHeading=m.heading[0];m.release();m.control(0,1,1,0);m.update(.1);near(m.heading[0],oldHeading,.001,"Turret input does not rotate chassis");
        for(int map=0;map<3;map++){m.map=map;m.reset();for(int i=0;i<2;i++)check(!DriveMath.blocked(m.x[i],m.y[i],16,m.walls()),"Safe spawn for every map");check(mapHasRoute(m.walls()),"Map has a tank-width route between spawns: "+map);}
        m.map=0;m.maxHp=1;m.target=1;m.reset();m.shield[0]=m.shield[1]=0;m.cooldown[0]=m.cooldown[1]=0;m.shells.add(new TankMatch.Shell(190,195,Math.PI/2,0,0));m.shells.add(new TankMatch.Shell(210,605,-Math.PI/2,1,0));for(int i=0;i<1200&&m.winner<0;i++)m.update(1d/240);check(m.winner==2&&m.score[0]==1&&m.score[1]==1,"Simultaneous destruction resolves together");
        Random random=new Random(71);for(int map=0;map<3;map++){m=new TankMatch();m.map=map;m.reset();for(int i=0;i<16000;i++){if(m.winner>=0)m.reset();if(i%30==0)for(int side=0;side<2;side++){m.control(side,0,random.nextDouble()*2-1,random.nextDouble()*2-1);m.control(side,1,random.nextDouble()*2-1,random.nextDouble()*2-1);}m.update(1d/120);for(int side=0;side<2;side++){finite(m.x[side]);check(!DriveMath.blocked(m.x[side],m.y[side],15.99,m.walls()),"Tank never enters walls");}check(m.shells.size()<20,"Shell lifetime bounds workload");}}
    }
    static boolean mapHasRoute(double[][] walls){
        int width=69,height=141;boolean[][] seen=new boolean[height][width];java.util.ArrayDeque<int[]> queue=new java.util.ArrayDeque<int[]>();queue.add(new int[]{34,12});seen[12][34]=true;
        while(!queue.isEmpty()){int[] pos=queue.remove();double x=64+pos[0]*4,y=122+pos[1]*4;if(Math.hypot(x-200,y-630)<14)return true;for(int[] delta:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int xx=pos[0]+delta[0],yy=pos[1]+delta[1];if(xx>=0&&xx<width&&yy>=0&&yy<height&&!seen[yy][xx]&&!DriveMath.blocked(64+xx*4,122+yy*4,20,walls)){seen[yy][xx]=true;queue.add(new int[]{xx,yy});}}}return false;
    }
    public static void main(String[] args){boards();balls();pool();race();tanks();System.out.println("New games: "+checks+" assertions passed.");}
}
