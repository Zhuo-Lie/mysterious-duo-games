package com.local.tapsumo;

/** Each completed game contributes one win, or one draw, to the local series. */
final class SeriesScore {
    final int[] wins=new int[2];
    int draws;
    void record(int winner){if(winner==0||winner==1)wins[winner]++;else if(winner==2)draws++;else throw new IllegalArgumentException("Unknown winner");}
    void clear(){wins[0]=wins[1]=draws=0;}
    int rounds(){return wins[0]+wins[1]+draws;}
}
