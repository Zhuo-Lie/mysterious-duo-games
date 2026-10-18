package com.local.tapsumo;

/** v1.4 planted shoves, with shorter strides. Only taps can move the position. */
public final class Match {
    public enum Phase { READY, COUNTDOWN, PLAYING, RESULT }
    // Normalized ring: -1 is red's edge; +1 is blue's edge.
    // Keep the existing strength scale; shorten the stride independently.
    static final double IMPULSE = .72;
    static final double DRAG = 2.5;
    static final double STRIDE_PER_IMPULSE = .075;
    public static final float DEFAULT_STRENGTH = 1f;
    public static final float MIN_STRENGTH = .25f, MAX_STRENGTH = 2f;
    public Phase phase = Phase.READY;
    public final int[] taps = new int[2];
    public final boolean[] ready = new boolean[2];
    private long startsAt, endsAt, simulatedAt;
    private double position;
    private final double[] momentum=new double[2];
    private float strength = DEFAULT_STRENGTH;
    private int victor;

    public void reset() {
        phase = Phase.READY;
        taps[0] = taps[1] = 0;
        ready[0] = ready[1] = false;
        startsAt = endsAt = simulatedAt = 0;
        position = momentum[0] = momentum[1] = 0;
        victor = 0;
    }
    public void prepare(int side, long now) {
        if (phase != Phase.READY) return;
        ready[side] = true;
        if (ready[0] && ready[1]) {
            startsAt = simulatedAt = now + 3000;
            phase = Phase.COUNTDOWN;
        }
    }
    public void update(long now) {
        if (phase == Phase.COUNTDOWN && now >= startsAt) phase = Phase.PLAYING;
        if (phase != Phase.PLAYING || now <= simulatedAt) return;
        double seconds = (now-simulatedAt)/1000.0;
        double decay=Math.exp(-DRAG*seconds);
        momentum[0]*=decay;momentum[1]*=decay;
        simulatedAt = now;
    }
    public boolean tap(int side, long now) {
        update(now);
        if (phase != Phase.PLAYING) return false;
        taps[side]++;
        momentum[side]+=1;
        double rhythm=1+.45*momentum[side]/(2+momentum[side]);
        double stride=IMPULSE*STRIDE_PER_IMPULSE*strength*pressure(now)*rhythm;
        position+=(side==0?stride:-stride);
        if(Math.abs(position)>=1){
            position=Math.copySign(1,position);victor=position>0?1:-1;
            endsAt=now;phase=Phase.RESULT;
        }
        return true;
    }
    public void setStrength(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) return;
        strength = Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, value));
    }
    public float strength() { return strength; }
    // No deadline victory. After six seconds, both players gain the same gradual
    // gentle boost, not an exponential late-game shove.
    // Snapshot the boost on taps so render frequency cannot change the result.
    public float pressure(long now) {
        double seconds = elapsed(now) / 1000.0;
        // +2.5% per second after 6s; at 20s it reaches the +35% ceiling.
        return (float)(1 + Math.min(.35, Math.max(0, seconds-6)*.025));
    }
    public int countdown(long now) { return (int)Math.max(1, (startsAt-now+999)/1000); }
    public float position() { return (float)position; }
    public long elapsed(long now) {
        if (phase == Phase.READY || phase == Phase.COUNTDOWN) return 0;
        return Math.max(0, (phase == Phase.RESULT ? endsAt : now)-startsAt);
    }
    public int winner() { return victor; }
}
