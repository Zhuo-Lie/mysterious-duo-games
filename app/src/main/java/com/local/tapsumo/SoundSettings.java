package com.local.tapsumo;

/** Per-category levels remain independent of the master switch and media volume. */
final class SoundSettings {
    static final String[] CLICK_NAMES={"蹬地重顶","闷鼓撞击","短促发力"};
    private float click=.75f, countdown=.5f, result=.7f;
    private int style;
    private static float level(float value,float previous) {
        return Float.isNaN(value)||Float.isInfinite(value)?previous:Math.max(0,Math.min(1,value));
    }
    float clickVolume(){return click;}
    float countdownVolume(){return countdown;}
    float resultVolume(){return result;}
    int style(){return style;}
    void setClickVolume(float value){click=level(value,click);}
    void setCountdownVolume(float value){countdown=level(value,countdown);}
    void setResultVolume(float value){result=level(value,result);}
    void setStyle(int value){style=Math.max(0,Math.min(CLICK_NAMES.length-1,value));}
}
