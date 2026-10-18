package com.local.tapsumo;
final class PolishTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        TankMatch t=new TankMatch();t.reset();t.shield[0]=t.shield[1]=0;
        t.shells.add(new TankMatch.Shell(190,390,Math.PI/2,0,0));t.shells.add(new TankMatch.Shell(190,410,-Math.PI/2,1,0));t.update(.05);
        check(t.shells.isEmpty()&&t.hp[0]==3&&t.hp[1]==3&&(t.events&16)!=0,"Opposing shells annihilate before tank damage");
        t.reset();t.shells.add(new TankMatch.Shell(190,390,0,0,0));t.shells.add(new TankMatch.Shell(190,390,0,0,0));t.update(.01);check(t.shells.size()==2,"Friendly shells do not cancel");
        BallDuel b=new BallDuel(BallDuel.Mode.PINBALL);
        for(int map=0;map<3;map++){b.map=map;b.clock=0;double start=map==1?0:b.obstacles()[0][0];for(int k=0;k<500;k++){b.clock=k*.02;double[][] o=b.obstacles();for(int i=0;i<o.length;i+=2){check(Math.abs(o[i][0]+o[i+1][0]-400)<1e-8&&Math.abs(o[i][1]+o[i+1][1]-800)<1e-8,"Moving obstacles remain symmetric");for(int j=i;j<i+2;j++)check(o[j][0]-o[j][2]>52&&o[j][0]+o[j][2]<348,"Obstacles remain inside arena");}}if(map!=1)check(Math.abs(b.obstacles()[0][0]-start)>1,"Every pillar layout moves");}
        check(new BallDuel(BallDuel.Mode.HOCKEY).goalHalfWidth()==72,"Hockey opening reduced gently");
        BallDuel hockey=new BallDuel(BallDuel.Mode.HOCKEY);hockey.reset(0);hockey.aim(1,300,500);hockey.update(.05);check(Math.hypot(hockey.px[1]-300,hockey.py[1]-500)<.01,"Hockey catches a large finger move within 50ms");
        for(int map=0;map<3;map++)for(int k=0;k<500;k++){b.map=map;b.clock=k*.02;double[][] r=b.rotors();if(r.length>0)check(Math.abs(r[0][0]+r[1][0]-400)<1e-8&&Math.abs(r[0][1]+r[1][1]-800)<1e-8,"Rotors preserve player symmetry");for(double[] rod:r)check(rod[0]-rod[3]-7>52&&rod[0]+rod[3]+7<348,"Rotating rods stay within side walls");}
        b.map=1;b.reset(0);b.serve=0;b.x=200;b.y=282;b.vx=0;b.vy=120;b.update(.03);check(b.vy<0&&b.hitAge>0,"Rotor has actual collision and contact feedback");
        System.out.println("Polish checks: opposing/friendly shells, three moving layouts and hockey goals passed.");
    }
}
