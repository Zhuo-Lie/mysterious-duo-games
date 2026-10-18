package com.local.tapsumo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.SparseBooleanArray;
import java.io.IOException;

/** Preloaded, overlapping short samples. Never decodes on a tap. */
final class GameAudio {
    private final SoundPool pool;
    private final SparseBooleanArray loaded = new SparseBooleanArray();
    private final int[] push=new int[3],impact=new int[3];
    private final int tick, win;
    private final int[] actions=new int[8];
    private int motor, motorStream, clash, rubble;
    final SoundSettings settings=new SoundSettings();
    boolean enabled;
    GameAudio(Context context, boolean enabled) {
        this.enabled = enabled;
        pool = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override public void onLoadComplete(SoundPool soundPool, int sample, int status) {
                synchronized (loaded) { loaded.put(sample, status == 0); }
            }
        });
        String[] normal={"push.wav","drum.wav","grunt.wav"};
        String[] heavy={"impact.wav","drum-impact.wav","grunt-impact.wav"};
        for(int i=0;i<3;i++){push[i]=load(context,normal[i]);impact[i]=load(context,heavy[i]);}
        tick = load(context, "tick.wav"); win = load(context, "win.wav");
        String[] files={"rebound","cue","chalk","puck","flipper","checkpoint","stone","cannon"};
        for(int i=0;i<files.length;i++)actions[i]=load(context,files[i]+".wav");
        motor=load(context,"engine.wav");clash=load(context,"clash.wav");rubble=load(context,"rubble.wav");
    }
    private int load(Context context, String file) {
        try (AssetFileDescriptor fd = context.getAssets().openFd("audio/" + file)) {
            return pool.load(fd, 1);
        } catch (IOException unavailable) { return 0; }
    }
    private void play(int sample, float volume, float rate) {
        if (!enabled || sample == 0 || volume <= 0) return;
        synchronized (loaded) { if (!loaded.get(sample)) return; }
        volume=Math.min(1,volume*1.3f);pool.play(sample, volume, volume, 1, 0, rate);
    }
    void push(int side, boolean danger) {
        int style=settings.style();
        play(danger?impact[style]:push[style],settings.clickVolume()*(danger?1:.9f),side==0?.98f:1.02f);
    }
    void tick(boolean go) { play(tick,settings.countdownVolume(),go?1.5f:1f); }
    void win() { play(win,settings.resultVolume(),1f); }
    void action(GameCatalog.Id id,boolean score){
        action(id,score,1);
    }
    void action(GameCatalog.Id id,boolean score,double strength){
        int i=id==GameCatalog.Id.PING_PONG?0:id==GameCatalog.Id.POOL?1:id==GameCatalog.Id.TIC_TAC_TOE?2:id==GameCatalog.Id.AIR_HOCKEY?3:id==GameCatalog.Id.PINBALL?4:id==GameCatalog.Id.RACING?5:id==GameCatalog.Id.GOMOKU?6:7;
        float weight=(float)(.35+.65*Math.max(0,Math.min(1,strength)));
        play(actions[i],(score?settings.resultVolume():settings.clickVolume())*weight,score?1.3f:(float)(.92+.12*strength));
    }
    void cover(boolean broken){play(rubble,settings.clickVolume()*(broken?1:.65f),broken?.8f:1.15f);}
    void clash(){play(clash,settings.clickVolume()*.65f,1);}
    void engine(double speed){
        if(!enabled||speed<.025||settings.clickVolume()<=0){if(motorStream!=0)pool.stop(motorStream);motorStream=0;return;}
        synchronized(loaded){if(!loaded.get(motor))return;}
        float level=settings.clickVolume()*(float)(.38+.42*Math.min(1,speed)),rate=(float)(.65+.8*Math.min(1.4,speed));
        if(motorStream==0)motorStream=pool.play(motor,level,level,2,-1,rate);
        else{pool.setVolume(motorStream,level,level);pool.setRate(motorStream,rate);}
    }
    void pause() { engine(0);pool.autoPause(); }
    void release() { engine(0);pool.release(); }
}
