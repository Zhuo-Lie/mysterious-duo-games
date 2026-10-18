package com.local.tapsumo;

/** A crisp first 80% with a small sliding finish; scoring targets never change. */
final class PushMotion {
    static final long TRAVEL_MS=80,HOP_MS=104;
    private float from, target;
    private long began;
    private int delay=12;
    private boolean attacking;
    private long hopBegan=-10000;
    private int hopDelay=12;
    void reset() { from = target = 0; began = hopBegan = -10000; attacking=false;delay=hopDelay=12; }
    void stepTo(float position, long now) {
        stepTo(position,now,true);
    }
    void stepTo(float position,long now,boolean attack) {
        from=value(now);target=position;began=now;delay=attack?12:24;
        // An opponent's same-frame target update must not erase this player's
        // own attack animation. Position and exertion have independent clocks.
        if(attack||!attacking||now-hopBegan>hopDelay+HOP_MS+48){
            hopBegan=now;attacking=attack;hopDelay=delay;
        }
    }
    float value(long now) {
        float t=Math.max(0,Math.min(1,(now-began-delay)/(float)TRAVEL_MS));
        float eased=1-(1-t)*(1-t)*(1-t);
        return from+(target-from)*eased;
    }
    float hop(long now) {
        float t=(now-hopBegan-hopDelay)/(float)HOP_MS;
        if(t<=0||t>=1)return 0;
        return (float)Math.sin(Math.PI*t)*(attacking?1:.55f);
    }
    float landing(long now) {
        float t=(now-hopBegan-hopDelay-HOP_MS)/48f;
        return t>0&&t<1 ? (float)Math.sin(Math.PI*t) : 0;
    }
    float departure() { return from; }
    float destination() { return target; }
    long age(long now) { return now-began-delay; }
}
