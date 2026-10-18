package com.local.tapsumo;
final class CoverAndFreeBallTest {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static void settle(PoolMatch p){for(int i=0;i<3000&&p.moving;i++)p.update(1d/240);check(!p.moving,"Shot settles");}
    static void scratch(PoolMatch p){check(p.shoot(1,0,.05),"Begin scratch shot");p.balls[0].x=50;p.balls[0].y=160;p.balls[0].vx=p.balls[0].vy=0;settle(p);}
    static void shootWall(TankMatch t){t.shells.add(new TankMatch.Shell(120,270,Math.PI/2,0,1));t.update(.06);}
    public static void main(String[] args){
        PoolMatch p=new PoolMatch(0);scratch(p);check(p.turn==1&&p.ballInHand&&p.balls[0].sunk,"Scratch gives opponent ball in hand");check(!p.shoot(1,0,.5),"Cannot shoot before placement");check(!p.confirmCuePlacement(),"Cannot confirm without selecting");
        for(double[] bad:new double[][]{{50,400},{200,160},{60,170},{350,640},{Double.NaN,400},{p.balls[1].x,p.balls[1].y}}){p.selectCuePlacement(bad[0],bad[1]);check(!p.canPlaceCue(bad[0],bad[1])&&!p.confirmCuePlacement(),"Invalid placement rejected");}
        p.selectCuePlacement(200,200);check(p.confirmCuePlacement()&&!p.ballInHand&&!p.balls[0].sunk&&p.turn==1,"Legal placement anywhere on table preserves turn");check(p.balls[0].x==200&&p.balls[0].y==200&&p.balls[0].vx==0,"Placement has no launch impulse");check(p.shoot(0,1,.05),"Can shoot after confirming");settle(p);
        p.reset(0);check(!p.ballInHand&&!p.placementSelected,"Restart clears free-ball state");p.shoot(1,0,.05);settle(p);check(!p.ballInHand,"Ordinary missed shot does not grant free ball");
        TankMatch t=new TankMatch();t.reset();shootWall(t);check(t.wallHp[0]==2&&t.walls().length==4&&t.shells.isEmpty(),"First hit chips cover and absorbs shell");shootWall(t);check(t.wallHp[0]==1,"Second hit leaves cracked cover");shootWall(t);check(t.wallHp[0]==0&&t.walls().length==3,"Third hit destroys cover");check(!DriveMath.blocked(120,292,16,t.walls()),"Destroyed cover opens tank movement");check((t.events&64)!=0,"Destruction event emitted");t.reset();check(t.wallHp[0]==3&&t.walls().length==4,"Restart restores cover");
        t.coverHits=1;t.reset();t.shells.add(new TankMatch.Shell(110,270,Math.PI/2,0,0));t.shells.add(new TankMatch.Shell(130,270,Math.PI/2,1,0));t.update(.06);check(t.wallHp[0]==0&&t.shells.isEmpty(),"Simultaneous hits both see pre-destruction cover");
        t.coverHits=3;t.reset();t.x[0]=120;t.y[0]=257;t.cooldown[0]=0;t.control(0,1,0,1);t.update(.02);check(t.wallHp[0]==2&&t.shells.isEmpty(),"Point-blank fire can damage cover even if muzzle overlaps it");
        t.destructible=false;t.coverHits=3;t.reset();shootWall(t);check(t.wallHp[0]==3&&t.walls().length==4&&t.shells.get(0).vy<0,"Classic fixed cover still ricochets");
        t.destructible=true;t.maxHp=1;t.rotateMaps=false;t.reset();shootWall(t);shootWall(t);shootWall(t);t.shield[1]=0;t.shells.add(new TankMatch.Shell(200,605,Math.PI/2,0,0));t.update(.1);check(t.score[0]==1&&t.roundPause>0&&t.wallHp[0]==3,"New round restores cover even on same map");
        System.out.println("Cover and free ball: destruction, batch fairness, ricochet, round reset and legal placement checks passed.");
    }
}
