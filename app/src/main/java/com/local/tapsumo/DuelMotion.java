package com.local.tapsumo;

/** Independent visual effort when both players push; never changes the score. */
final class DuelMotion {
    private static final long EFFECT_MS=144,RECENT_MS=180;
    private final long[] hitAt={-10000,-10000};
    private final float[] carried={0,0};
    private long overlapAt;
    void reset(){hitAt[0]=hitAt[1]=-10000;carried[0]=carried[1]=0;overlapAt=0;}
    private boolean bothRecent(long now){
        return now>=hitAt[0]&&now>=hitAt[1]&&now-hitAt[0]<RECENT_MS&&now-hitAt[1]<RECENT_MS;
    }
    void strike(int side,long now){
        boolean overlapping=bothRecent(now);
        carried[side]=effort(side,now);hitAt[side]=now;
        if(!overlapping&&bothRecent(now))overlapAt=now;
    }
    private float effort(int side,long now){
        long age=now-hitAt[side];
        if(age<0||age>=EFFECT_MS)return 0;
        if(age<18){float t=age/18f;return carried[side]+(1-carried[side])*(1-(1-t)*(1-t));}
        if(age<42)return 1-(age-18)/24f*.1f;
        float t=(age-42)/(float)(EFFECT_MS-42);
        return .9f*(1-t)*(1-t);
    }
    float offset(int side,long now){
        if(!bothRecent(now))return 0;
        float blend=Math.max(0,Math.min(1,(now-overlapAt)/20f));
        float own=effort(side,now),opponent=effort(1-side,now);
        // Local exertion and opposite incoming recoil remain visible even when
        // simultaneous equal taps cancel all real advancement.
        return (side==0?1:-1)*(6*own-2*opponent)*blend;
    }
}
