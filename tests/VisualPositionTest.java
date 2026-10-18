package com.local.tapsumo;

public final class VisualPositionTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        check(VisualPosition.map(0)==0,"Center stays anchored");
        check(VisualPosition.map(-1)==-1&&VisualPosition.map(1)==1,"Both ring-out endpoints stay anchored");
        float previous=-1;
        for(int i=0;i<=2000;i++){
            float p=-1+i/1000f,mapped=VisualPosition.map(p);
            check(mapped>=previous&&mapped>=-1&&mapped<=1,"Curve never reverses or leaves the arena");previous=mapped;
            check(Math.abs(mapped+VisualPosition.map(-p))<.000001,"Both players have symmetric visual treatment");
            check(Math.abs(mapped-p)<=.0232f,"Display deviation is under 2.32 design pixels");
            float gain=VisualPosition.movementGain(p);
            check(gain>=.8799f&&gain<=1.0601f,"Local changes remain subtle: plus six to minus twelve percent");
        }
        float center=VisualPosition.map(.03f)-VisualPosition.map(-.03f);
        float edge=VisualPosition.map(.92f)-VisualPosition.map(.86f);
        check(center>.06f&&center<.064f,"A central step is only slightly larger");
        check(edge<.06f&&edge>.053f,"An identical edge step is only slightly smaller");
        check(VisualPosition.movementGain(0)>VisualPosition.movementGain(.5f)
            &&VisualPosition.movementGain(.5f)>VisualPosition.movementGain(.9f),"Gain smoothly tapers toward the edge");
        Match m=new Match();m.prepare(0,0);m.prepare(1,0);m.tap(0,3000);
        float scorePosition=m.position();int clicks=m.taps[0];Match.Phase phase=m.phase;
        for(int i=0;i<1000;i++){VisualPosition.map(m.position());VisualPosition.movementGain(m.position());}
        check(m.position()==scorePosition&&m.taps[0]==clicks&&m.phase==phase,"Display mapping has no scoring side effects");
        System.out.println("Subtle center/edge display curve: all checks passed.");
    }
}
