package com.local.tapsumo;

public final class MatchTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Match start(){Match m=new Match();m.prepare(0,0);m.prepare(1,0);m.update(3000);return m;}
    static Match simulate(double redHz,double blueHz,boolean mirror,boolean fine){
        Match m=start();int red=0,blue=0;
        for(long t=3000;t<=183000&&m.phase!=Match.Phase.RESULT;t++){
            if(red<Math.floor((t-3000)*redHz/1000)){m.tap(mirror?1:0,t);red++;}
            if(blue<Math.floor((t-3000)*blueHz/1000)){m.tap(mirror?0:1,t);blue++;}
            if(fine)m.update(t);
        }
        m.update(183000);return m;
    }
    static float secondStep(long gap){
        Match m=start();m.tap(0,3000);float before=m.position();m.tap(0,3000+gap);return m.position()-before;
    }
    public static void main(String[] args){
        Match m=new Match();check(!m.tap(0,0),"No tap before ready");m.prepare(0,0);
        check(m.phase==Match.Phase.READY,"Both prepare");m.prepare(1,0);
        check(!m.tap(0,2999),"No countdown taps");check(m.tap(0,3000),"First tap accepted");
        float first=m.position();double oldFirst=.72*.22*1.15;
        check(first>0&&Math.abs(first/oldFirst-.075/.22)<.000001,"v1.4 stride shortened to roughly one third");
        check(first<.07,"First standard shove advances fewer than seven design pixels");
        for(long now=3001;now<6000;now++){
            m.update(now);check(m.position()==first,"No time-driven position change between taps");
        }
        m.update(90000);check(m.position()==first&&m.phase==Match.Phase.PLAYING,"No auto-coasting or deadline victory");
        float fastBonus=secondStep(1)-first;
        check(fastBonus>0,"Retain v1.4 rhythm buildup");
        check((secondStep(1000)-first)/fastBonus<.15,"Most buildup decays within a second");
        check((secondStep(2000)-first)/fastBonus<.015,"Buildup essentially resets within two seconds");
        m=start();for(int i=0;i<10;i++)check(m.tap(0,3000+i*100),"Standard opening taps are accepted");
        check(m.phase==Match.Phase.PLAYING&&m.position()<.8,"Ten rapid standard taps no longer finish a one-sided game");
        for(double[] rates:new double[][]{{6,4},{6,4.5},{8,5},{6,0},{6,5.5},{6,5.8},{6,5.9}}){
            Match a=simulate(rates[0],rates[1],false,true),b=simulate(rates[0],rates[1],true,false);
            check(a.winner()==1&&b.winner()==-1,"Faster side wins symmetrically");
            check(a.elapsed(183000)==b.elapsed(183000),"Rendering frequency cannot alter the result");
            check(!a.tap(1,200000),"Result freezes taps");
            System.out.printf("Short v1.4 stride: %.1f vs %.1f taps/s = %.3f seconds%n",rates[0],rates[1],a.elapsed(183000)/1000.0);
        }
        m=simulate(6,6,false,true);check(m.phase==Match.Phase.PLAYING&&Math.abs(m.position())<.00001,"Equal rhythm has no input-order drift");
        m=start();for(int i=0;i<3;i++)m.tap(0,3000+i*100);float ahead=m.position();
        for(int i=0;i<12;i++)m.tap(1,3300+i*100);
        check(m.position()<ahead,"Counter-steps push the other way");
        m.setStrength(1.375f);m.reset();check(m.position()==0&&m.taps[0]==0&&!m.ready[0]&&m.winner()==0,"Reset clears duel and buildup");
        check(m.strength()==1.375f,"Reset preserves strength");m.setStrength(Float.NaN);check(m.strength()==1.375f,"Reject invalid strength");
        m.setStrength(100);check(m.strength()==2,"Upper strength bound");m.setStrength(-1);check(m.strength()==.25f,"Lower strength bound");
        m=start();check(m.strength()==1&&Match.IMPULSE==.72,"Preserve strength setting scale");
        check(m.pressure(9000)==1&&Math.abs(m.pressure(13000)-1.1)<.00001,"Preserve gentle late boost");
        check(Math.abs(m.pressure(23000)-1.35)<.00001&&m.pressure(99000)==m.pressure(23000),"Boost still capped at 35 percent");
        m.setStrength(2);m.tap(0,30000);float planted=m.position();m.update(90000);
        check(m.phase==Match.Phase.PLAYING&&m.position()==planted,"Late single shove cannot win by coasting");
        PushMotion attacker=new PushMotion(),defender=new PushMotion();
        attacker.reset();defender.reset();attacker.stepTo(.0621f,1000,true);defender.stepTo(.0621f,1000,false);
        check(attacker.value(1012)==0&&defender.value(1024)==0,"Retain short v1.4 impact reaction offsets");
        check(attacker.value(1020)>0&&attacker.value(1020)<.0621f,"Small transition softens the old teleport");
        float previous=0;
        for(long now=1012;now<=1092;now++){
            float value=attacker.value(now);check(value>=previous&&value<=.0621f,"Transition is monotonic and bounded");previous=value;
        }
        float front=attacker.value(1044)/.0621f;
        check(front>.75f&&front<.81f,"First 32ms keeps a crisp roughly eighty percent cross-step");
        check(attacker.value(1072)>attacker.value(1044)&&attacker.value(1072)<.0621f,"Last small portion slides gently toward the same target");
        check(attacker.value(1092)==.0621f&&defender.value(1104)==.0621f,"Finish at exactly the unchanged landing point in 80ms");
        check(attacker.hop(1064)>.99f&&defender.hop(1076)>.54f,"Preserve the v1.4 hop profile");
        for(long now=1200;now<10000;now+=11){
            check(attacker.value(now)==.0621f&&attacker.hop(now)==0&&attacker.landing(now)==0,"Stand completely still without another tap");
        }
        attacker.stepTo(-.03f,10000,true);float before=attacker.value(10025);
        attacker.stepTo(.04f,10025,true);check(attacker.value(10025)==before,"Rapid counter taps do not teleport");
        check(attacker.value(10117)==.04f,"Interrupted step settles normally");
        attacker.reset();check(attacker.value(11000)==0,"Visual reset centers fighter");
        System.out.println("Short-stride and planted-motion checks passed.");
    }
}
