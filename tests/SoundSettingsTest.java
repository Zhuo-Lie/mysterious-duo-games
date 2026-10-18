package com.local.tapsumo;

public final class SoundSettingsTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        SoundSettings s=new SoundSettings();
        s.setClickVolume(0);check(s.clickVolume()==0,"Zero click volume mutes clicks");
        check(s.countdownVolume()==.5f&&s.resultVolume()==.7f,"Click level does not alter countdown or result");
        s.setCountdownVolume(1);check(s.countdownVolume()==1&&s.clickVolume()==0,"Countdown independent");
        s.setResultVolume(.321f);s.setClickVolume(.812f);
        check(s.clickVolume()==.812f&&s.resultVolume()==.321f,"Continuous levels retained");
        s.setCountdownVolume(10);check(s.countdownVolume()==1,"Upper volume bounded");
        s.setResultVolume(-1);check(s.resultVolume()==0,"Lower volume bounded");
        s.setClickVolume(Float.NaN);s.setClickVolume(Float.POSITIVE_INFINITY);
        check(s.clickVolume()==.812f,"Invalid volume ignored");
        for(int i=0;i<3;i++){s.setStyle(i);check(s.style()==i,"Each click style selectable");}
        s.setStyle(999);check(s.style()==2,"Upper style bound");s.setStyle(-1);check(s.style()==0,"Lower style bound");
        check(s.clickVolume()==.812f,"Changing style preserves independent volume");
        System.out.println("Sound settings: all checks passed.");
    }
}
