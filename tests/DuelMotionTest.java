package com.local.tapsumo;

public final class DuelMotionTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        DuelMotion duel=new DuelMotion();duel.strike(0,1000);
        check(duel.offset(0,1030)==0&&duel.offset(1,1030)==0,"One-sided stride has no extra displacement");
        duel.strike(1,1000);
        check(duel.offset(0,1020)>3&&duel.offset(1,1020)<-3,"Simultaneous inputs show both wrestlers exerting instead of canceling visually");
        check(duel.offset(0,1200)==0&&duel.offset(1,1200)==0,"After settling both return to the exact scoring locations");
        Match m=new Match();m.prepare(0,0);m.prepare(1,0);m.update(3000);
        PushMotion[] motion={new PushMotion(),new PushMotion()};motion[0].reset();motion[1].reset();duel.reset();
        for(int i=0;i<50;i++){
            long now=3000+i*83;
            for(int side=0;side<2;side++){
                check(m.tap(side,now),"High-frequency input accepted");duel.strike(side,now);
                motion[0].stepTo(m.position(),now,side==0);motion[1].stepTo(m.position(),now,side==1);
            }
            check(Math.abs(m.position())<.000001&&m.phase==Match.Phase.PLAYING,"Equal twelve-taps-per-second players remain tied fairly");
            float minimum=Float.MAX_VALUE,maximum=-Float.MAX_VALUE;
            for(long frame=now+20;frame<now+83;frame+=8){
                float offset=duel.offset(0,frame);minimum=Math.min(minimum,offset);maximum=Math.max(maximum,offset);
                check(Math.abs(offset)<=6&&Math.abs(duel.offset(1,frame))<=6,"Visual contest movement is bounded");
            }
            check(maximum-minimum>1.5f,"Every matched rapid-click cycle retains visible back-and-forth movement");
            check(motion[0].hop(now+64)>.99f&&motion[1].hop(now+64)>.99f,"Same-frame opponent input cannot erase either player's own hop");
        }
        long last=3000+49*83;
        check(duel.offset(0,last+200)==0&&duel.offset(1,last+200)==0,"No residual added movement after clicks stop");
        m.update(last+1000);check(m.position()==0&&m.phase==Match.Phase.PLAYING,"Visual effort never becomes score or a fake winner");
        DuelMotion mirrored=new DuelMotion();duel.reset();
        duel.strike(0,10000);duel.strike(1,10017);mirrored.strike(1,10000);mirrored.strike(0,10017);
        for(long t=10017;t<10200;t++){
            check(Math.abs(duel.offset(0,t)+mirrored.offset(1,t))<.00001,"Team-swapped visuals remain symmetric");
        }
        duel.reset();check(duel.offset(0,15000)==0&&duel.offset(1,15000)==0,"Reset clears exertion");
        System.out.println("Balanced rapid-click animation and unchanged-score checks passed.");
    }
}
