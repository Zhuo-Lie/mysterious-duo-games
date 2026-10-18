package com.local.tapsumo;
final class PongRampTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static BallDuel run(int ramp,double dt,int steps){BallDuel b=new BallDuel(BallDuel.Mode.PING_PONG);b.ramp=ramp;b.reset(0);b.serve=0;b.vx=300;b.vy=0;for(int i=0;i<steps;i++)b.update(dt);return b;}
    public static void main(String[] args){
        BallDuel classic=run(0,1d/120,1200),gradual=run(1,1d/120,1200),fast=run(2,1d/120,1200);
        check(Math.abs(Math.hypot(classic.vx,classic.vy)-300)<.001,"Classic has no time-based acceleration");
        check(Math.hypot(gradual.vx,gradual.vy)>370&&Math.hypot(gradual.vx,gradual.vy)<374,"Gradual uses a warm-up and bounded acceleration");
        check(Math.hypot(fast.vx,fast.vy)>Math.hypot(gradual.vx,gradual.vy),"Fast setting accelerates more quickly");
        BallDuel cap=run(2,1d/60,6000);check(Math.abs(Math.hypot(cap.vx,cap.vy)-690)<.001,"Speed cannot grow without limit");
        BallDuel otherFrame=run(1,1d/60,600);check(Math.abs(Math.hypot(gradual.vx,gradual.vy)-Math.hypot(otherFrame.vx,otherFrame.vy))<.05,"Acceleration is independent of display frame rate");
        gradual.x=200;gradual.y=94;gradual.vx=0;gradual.vy=-400;gradual.update(.01);check(gradual.rallySeconds==0&&gradual.rallyHits==0&&gradual.serve>0,"New point starts with a fresh warm-up");
        BallDuel b=new BallDuel(BallDuel.Mode.PING_PONG);b.reset(0);b.serve=0;b.x=200;b.y=175;b.vy=-300;b.update(.04);check(b.rallyHits==1&&b.bestRally==1&&b.precisionAge>0&&b.precisionSide==0,"Center hit produces rally and precision feedback");check(b.score[0]==0&&b.score[1]==0,"Precision feedback adds no free points");
        System.out.println("Pong: warm-up, three settings, speed cap, frame independence, point reset and precision feedback passed.");
    }
}
