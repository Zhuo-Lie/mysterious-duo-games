package com.local.tapsumo;

/** A subtle display-only curve. Center and ring-out endpoints stay anchored. */
final class VisualPosition {
    private static final float CURVE=.06f;
    private static float bounded(float position){return Math.max(-1,Math.min(1,position));}
    static float map(float position){
        float p=bounded(position);
        return p+CURVE*p*(1-p*p);
    }
    static float movementGain(float position){
        float p=bounded(position);
        return 1+CURVE*(1-3*p*p);
    }
}
