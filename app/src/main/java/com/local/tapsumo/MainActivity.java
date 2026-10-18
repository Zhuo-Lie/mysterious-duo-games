package com.local.tapsumo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.*;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.*;
import android.media.AudioManager;
import android.util.SparseIntArray;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.Locale;

public class MainActivity extends Activity {
    private SumoView game;
    private ArcadeScreens.Lobby lobby;
    private MiniGameView mini;
    private boolean foreground;
    private final SeriesScore series=new SeriesScore();
    private final GameNavigator navigator=new GameNavigator();
    private ArcadeScreens.Actions actions;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(ArcadeTheme.BACKGROUND);
        getWindow().setNavigationBarColor(ArcadeTheme.BACKGROUND);
        int bars=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if(android.os.Build.VERSION.SDK_INT>=26)bars|=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        getWindow().getDecorView().setSystemUiVisibility(bars);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        game = new SumoView();
        series.wins[0]=getPreferences(MODE_PRIVATE).getInt("seriesRed",0);
        series.wins[1]=getPreferences(MODE_PRIVATE).getInt("seriesBlue",0);
        series.draws=getPreferences(MODE_PRIVATE).getInt("seriesDraws",0);
        actions=new ArcadeScreens.Actions(){
            @Override public void preview(GameCatalog.Id id){showPreview(id);}
            @Override public void start(GameCatalog.Id id){openGame(id);}
            @Override public void home(){showLobby();}
            @Override public void audio(){game.showAudioSettings();}
            @Override public void score(){showSeriesMenu();}
        };
        lobby=new ArcadeScreens.Lobby(this,actions);
        lobby.refreshScores(series);
        if(state!=null && state.containsKey("preview")){
            try{showPreview(GameCatalog.Id.valueOf(state.getString("preview")));}
            catch(IllegalArgumentException ignored){showLobby();}
        } else showLobby();
        if(android.os.Build.VERSION.SDK_INT>=33)Api33Back.register(this);
    }
    private void showLobby(){if(mini!=null){mini.setPaused(true);mini=null;}if(game.match.phase==Match.Phase.RESULT)game.syncSounds(game.gameTime());game.reset();game.audio.pause();navigator.home();setContentView(lobby);lobby.requestApplyInsets();}
    private void showPreview(GameCatalog.Id id){navigator.preview(id);setContentView(new ArcadeScreens.Preview(this,actions,navigator.selected()));}
    private void openGame(GameCatalog.Id id){
        navigator.preview(id);
        if(!navigator.start())return;
        game.reset();
        if(id==GameCatalog.Id.SUMO){mini=null;setContentView(game);game.requestApplyInsets();}
        else{
            mini=new MiniGameView(this,id,game.audio,new MiniGameView.Host(){
                @Override public void home(){showLobby();}
                @Override public void menu(){showMiniMenu();}
                @Override public void audio(){game.showAudioSettings();}
                @Override public void undo(){showUndoMenu();}
                @Override public void finished(int winner){recordWinner(winner);}
            });setContentView(mini);mini.requestApplyInsets();
        }
    }
    private void saveSeries(){
        getPreferences(MODE_PRIVATE).edit().putInt("seriesRed",series.wins[0]).putInt("seriesBlue",series.wins[1]).putInt("seriesDraws",series.draws).apply();
        if(lobby!=null)lobby.refreshScores(series);
    }
    private void recordWinner(int winner){series.record(winner);saveSeries();}
    private void showSeriesMenu(){
        if(!game.pauseForSettings())return;
        LinearLayout panel=game.paperPanel("今天谁更强？","本机累计 · 所有小游戏共用");
        final TextView tally=game.settingText("红方 "+series.wins[0]+"  :  "+series.wins[1]+" 蓝方",28);tally.setTypeface(Typeface.DEFAULT_BOLD);panel.addView(tally);
        final TextView detail=game.settingText(series.rounds()+" 局较量 · "+series.draws+" 局平手",14);detail.setPadding(0,game.dp(13),0,game.dp(23));panel.addView(detail);
        Button clear=game.paperButton("清零比分",game.paper),done=game.paperButton("继续挑游戏",game.yellow);
        panel.addView(clear,new LinearLayout.LayoutParams(-1,game.dp(55)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,game.dp(55));lp.topMargin=game.dp(10);panel.addView(done,lp);
        final AlertDialog dialog=game.showPaperSettings(panel);
        clear.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){series.clear();saveSeries();tally.setText("红方 0  :  0 蓝方");detail.setText("新一轮较量，开始！");}});
        done.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){dialog.dismiss();}});
    }
    private void showMiniMenu(){
        if(mini==null)return;
        if(mini.phase==MiniGameView.Phase.READY||mini.phase==MiniGameView.Phase.RESULT){showLobby();return;}
        if(!game.pauseForSettings())return;
        LinearLayout panel=game.paperPanel("中场休息","已暂停 · 返回大厅会结束本局");
        Button resume=game.paperButton("继续对战",game.yellow),restart=game.paperButton("重新开局",game.paper),home=game.paperButton("返回大厅",game.paper);
        for(Button button:new Button[]{resume,restart,home}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,game.dp(55));lp.topMargin=game.dp(9);panel.addView(button,lp);}
        final AlertDialog dialog=game.showPaperSettings(panel);
        resume.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){dialog.dismiss();}});
        restart.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){if(mini!=null)mini.reset(false);dialog.dismiss();}});
        home.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){dialog.dismiss();showLobby();}});
    }
    void saveMiniSounds(){game.saveSettings();}
    private void miniSlider(LinearLayout panel,final String label,final int[] values,final int index,final int min,int max,String unit){
        final TextView title=game.settingText(label+"  "+values[index]+unit,16);panel.addView(title);
        final String suffix=unit;SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(values[index]-min);game.styleSlider(bar);
        panel.addView(bar,new LinearLayout.LayoutParams(-1,game.dp(48)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int progress,boolean user){values[index]=progress+min;title.setText(label+"  "+values[index]+suffix);}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}});
    }
    private void miniChoice(LinearLayout panel,final String label,final String[] labels,final int[] values,final int index){
        final Button button=game.paperButton(label+"："+labels[values[index]],game.paper);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,game.dp(52));lp.topMargin=game.dp(9);panel.addView(button,lp);
        button.setOnClickListener(new View.OnClickListener(){public void onClick(View view){values[index]=(values[index]+1)%labels.length;button.setText(label+"："+labels[values[index]]);}});
    }
    void showMiniOptions(){
        if(mini==null||!game.pauseForSettings())return;final MiniGameView current=mini;GameOptions o=current.options;
        final int[] values={o.target,(int)Math.round(o.speed*100),o.map,o.guide,o.laps,o.health,(int)Math.round(o.size*100),(int)Math.round(o.steering*100),o.rotateMaps?1:0,o.first,o.ricochets,o.ramp,o.cover};
        LinearLayout panel=game.paperPanel("玩法调节",GameCatalog.find(current.id).name+" · 双方共用，保存后重新准备");
        if(current.board!=null)miniChoice(panel,"首局先手",new String[]{"红方","蓝方","随机"},values,9);
        else {
            miniSlider(panel,"节奏",values,1,65,140,"%");
            if(current.ball!=null||current.tank!=null)miniSlider(panel,"获胜分数",values,0,1,9," 分");
            if(current.race!=null){miniSlider(panel,"比赛圈数",values,4,1,5," 圈");miniSlider(panel,"转向灵敏度",values,7,65,160,"%");miniChoice(panel,"首个场地",RaceMatch.MAP_NAMES,values,2);}
            if(current.tank!=null){miniSlider(panel,"每车装甲",values,5,1,3," 格");miniSlider(panel,"炮弹反弹",values,10,0,2," 次");miniChoice(panel,"首个场地",TankMatch.MAP_NAMES,values,2);miniChoice(panel,"掩体",new String[]{"固定不可破坏","两发击碎","三发击碎","四发击碎"},values,12);}
            if(current.ball!=null&&current.ball.mode==BallDuel.Mode.PING_PONG){miniChoice(panel,"升温方式",BallDuel.RAMP_NAMES,values,11);panel.addView(game.settingText("每分重新慢速发球，持续对拉两秒后逐渐加速；有速度上限。正中拍面显示精准回击，不额外加分。",13));}
            if(current.ball!=null&&current.ball.mode==BallDuel.Mode.HOCKEY)miniSlider(panel,"球与球槌大小",values,6,90,120,"%");
            if(current.ball!=null&&current.ball.mode==BallDuel.Mode.PINBALL)miniChoice(panel,"首组障碍",BallDuel.MAP_NAMES,values,2);
            if(current.race!=null||current.tank!=null||current.id==GameCatalog.Id.PINBALL)miniChoice(panel,"回合变局",new String[]{"固定场地","随机轮换"},values,8);
            if(current.pool!=null){miniChoice(panel,"目标球预瞄",new String[]{"关闭","适度 · 限长一次反弹","加强 · 仍限长一次反弹"},values,3);panel.addView(game.settingText("十个目标球，首颗合法进球分色；预瞄不预测后续连碰，不保证进袋。",13));}
        }
        Button cancel=game.paperButton("取消，继续原局",game.paper),apply=game.paperButton("保存并重新准备",game.yellow);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,game.dp(56));lp.topMargin=game.dp(16);panel.addView(apply,lp);panel.addView(cancel,new LinearLayout.LayoutParams(-1,game.dp(54)));
        final AlertDialog dialog=game.showPaperSettings(panel);
        apply.setOnClickListener(new View.OnClickListener(){public void onClick(View view){GameOptions o=current.options;o.target=values[0];o.speed=values[1]/100d;o.map=values[2];o.guide=values[3];o.laps=values[4];o.health=values[5];o.size=values[6]/100d;o.steering=values[7]/100d;o.rotateMaps=values[8]==1;o.first=values[9];o.ricochets=values[10];o.ramp=values[11];o.cover=values[12];current.saveOptions();current.reset(false);dialog.dismiss();}});
        cancel.setOnClickListener(new View.OnClickListener(){public void onClick(View view){dialog.dismiss();}});
    }
    private void showUndoMenu(){
        if(mini==null||!mini.canUndo()||!game.pauseForSettings())return;
        LinearLayout panel=game.paperPanel("商量一下？","双方同意后，撤回最近的一手");
        Button yes=game.paperButton("同意，撤回一手",game.yellow),no=game.paperButton("继续原局",game.paper);
        panel.addView(yes,new LinearLayout.LayoutParams(-1,game.dp(58)));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,game.dp(58));lp.topMargin=game.dp(10);panel.addView(no,lp);
        final AlertDialog dialog=game.showPaperSettings(panel);
        yes.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){if(mini!=null)mini.undoMove();dialog.dismiss();}});
        no.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){dialog.dismiss();}});
    }
    @Override protected void onSaveInstanceState(Bundle out){
        if(navigator.screen()==GameNavigator.Screen.PREVIEW)out.putString("preview",navigator.selected().id.name());
        super.onSaveInstanceState(out);
    }
    @Override public void onBackPressed(){handleBack();}
    private void handleBack(){
        if(navigator.screen()==GameNavigator.Screen.MATCH){if(mini!=null)showMiniMenu();else game.showReturnMenu();}
        else if(navigator.screen()==GameNavigator.Screen.PREVIEW)showLobby();
        else finish();
    }
    private static final class Api33Back {
        static void register(final MainActivity activity){
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,new android.window.OnBackInvokedCallback(){
                    @Override public void onBackInvoked(){activity.handleBack();}
                });
        }
    }
    @Override protected void onPause() { foreground=false;super.onPause();if(game.match.phase==Match.Phase.RESULT)game.syncSounds(game.gameTime());if(mini!=null)mini.setPaused(true);else game.reset();game.audio.pause(); }
    @Override protected void onResume(){super.onResume();foreground=true;if(mini!=null&&game.settingsDialog==null)mini.setPaused(false);}
    @Override protected void onDestroy() { game.audio.release(); super.onDestroy(); }

    final class SumoView extends View {
        final Match match = new Match();
        final PushMotion[] motions={new PushMotion(),new PushMotion()};
        final DuelMotion duel=new DuelMotion();
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Path path = new Path();
        final int red=Color.rgb(255,88,94), blue=Color.rgb(0,191,225);
        final int ink=Color.rgb(32,34,41), paper=Color.rgb(255,249,229);
        final int yellow=Color.rgb(255,201,44), skin=Color.rgb(255,210,151);
        final SparseIntArray owners = new SparseIntArray();
        final long[] hit = {-10000,-10000};
        final GameAudio audio;
        Canvas c;
        float scale=1, originX, originY;
        long resultAt=-1;
        int countSound=-1, lastSide;
        boolean resultSound;
        AlertDialog settingsDialog;
        long pausedMillis, pausedAt=-1;

        SumoView() {
            super(MainActivity.this);
            setContentDescription("指尖相扑，红方在上蓝方在下，单指连续点击，将对方推出圈外获胜");
            setSoundEffectsEnabled(false);
            audio=new GameAudio(MainActivity.this,getPreferences(MODE_PRIVATE).getBoolean("sound",true));
            audio.settings.setClickVolume(getPreferences(MODE_PRIVATE).getFloat("clickVolume",.75f));
            audio.settings.setCountdownVolume(getPreferences(MODE_PRIVATE).getFloat("countdownVolume",.5f));
            audio.settings.setResultVolume(getPreferences(MODE_PRIVATE).getFloat("resultVolume",.7f));
            audio.settings.setStyle(getPreferences(MODE_PRIVATE).getInt("clickStyle",0));
            // The new unit is old 4x. Do not misread an old saved multiplier.
            match.setStrength(getPreferences(MODE_PRIVATE).getFloat("standardStrength",Match.DEFAULT_STRENGTH));
            setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
                    return insets;
                }
            });
        }
        void reset() {
            if(settingsDialog!=null)settingsDialog.dismiss();
            match.reset(); owners.clear(); resultAt=-1; resultSound=false; countSound=-1;
            motions[0].reset();motions[1].reset();
            duel.reset();
            hit[0]=hit[1]=-10000; invalidate();
        }
        long gameTime() {
            return (pausedAt>=0 ? pausedAt : SystemClock.elapsedRealtime())-pausedMillis;
        }
        void fill(int color) { p.setColor(color);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(0);p.setShader(null); }
        void stroke(int color,float width) {
            p.setShader(null);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(width);
            p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        }
        void box(float l,float t,float r,float b,float radius,int color,float outline) {
            fill(color);c.drawRoundRect(l,t,r,b,radius,radius,p);
            if(outline>0){stroke(ink,outline);c.drawRoundRect(l,t,r,b,radius,radius,p);}
        }
        void oval(float l,float t,float r,float b,int color,float outline) {
            fill(color);c.drawOval(l,t,r,b,p);
            if(outline>0){stroke(ink,outline);c.drawOval(l,t,r,b,p);}
        }
        void line(float x,float y,float x2,float y2,int color,float width) {
            stroke(color,width);c.drawLine(x,y,x2,y2,p);
        }
        void text(String s,float x,float y,float size,int color) {
            fill(color);p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));
            p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);
        }
        void outlinedText(String s,float x,float y,float size,int color) {
            p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);
            stroke(ink,5);c.drawText(s,x,y,p);fill(color);c.drawText(s,x,y,p);
        }
        @Override protected void onDraw(Canvas canvas) {
            c=canvas;
            long now=gameTime(); match.update(now); syncSounds(now);
            float width=getWidth()-getPaddingLeft()-getPaddingRight();
            float height=getHeight()-getPaddingTop()-getPaddingBottom();
            scale=Math.min(width/400f,height/800f);
            if(scale<=0)return;
            originX=getPaddingLeft()+(width-400*scale)/2;
            originY=getPaddingTop()+(height-800*scale)/2;
            c.drawColor(paper);c.save();c.translate(originX,originY);c.scale(scale,scale);
            drawBackground(); drawSide(0,now); drawSide(1,now);
            drawArena(now); drawStatus(now); drawSound();
            if(match.phase==Match.Phase.COUNTDOWN) {
                // Simple centered announcement keeps players' tapping zones clear.
                oval(148,348,252,452,paper,6);
                text(""+match.countdown(now),200,424,68,ink);
            }
            if(match.phase==Match.Phase.RESULT && now-resultAt>=650) drawResult(now);
            c.restore();
            if(isAttachedToWindow()) postInvalidateOnAnimation();
        }
        void syncSounds(long now) {
            if(match.phase==Match.Phase.COUNTDOWN) {
                int value=match.countdown(now);
                if(value!=countSound){countSound=value;audio.tick(false);}
            }
            if(match.phase==Match.Phase.PLAYING && countSound!=0){countSound=0;audio.tick(true);}
            if(match.phase==Match.Phase.RESULT && !resultSound){
                resultSound=true;resultAt=now;audio.win();recordWinner(match.winner()>0?0:1);
            }
        }
        void drawBackground() {
            fill(Color.rgb(255,232,189));c.drawRect(0,0,400,800,p);
            fill(Color.rgb(255,244,215));c.drawRect(0,195,400,605,p);
            for(int i=-1;i<9;i++) {
                c.save();c.translate(i*60,0);c.rotate(-25);
                fill(Color.argb(20,ink>>16&255,ink>>8&255,ink&255));c.drawRect(0,0,16,950,p);c.restore();
            }
            line(0,197,400,197,ink,5); line(0,603,400,603,ink,5);
        }
        void drawSide(int side,long now) {
            c.save();if(side==0)c.rotate(180,200,400);
            int color=side==0?red:blue;
            fill(color);c.drawRect(0,605,400,800,p);
            fill(Color.argb(30,255,255,255));path.reset();path.moveTo(0,605);path.lineTo(215,605);path.lineTo(75,800);path.lineTo(0,800);path.close();c.drawPath(path,p);
            text(side==0?"红方 / RED":"蓝方 / BLUE",200,630,14,ink);
            boolean active=now-hit[side]<100;
            float down=active?6:0;
            box(24,653,376,778,22,ink,0);
            box(24,644+down,376,767+down,22,paper,6);
            String title=match.phase==Match.Phase.READY ? (match.ready[side]?"已准备！":"点击准备")
                :match.phase==Match.Phase.PLAYING?"推！推！推！":match.phase==Match.Phase.COUNTDOWN?"准备发力":"漂亮的一局！";
            text(title,200,698+down,31,ink);
            String caption=match.phase==Match.Phase.READY?"双方准备后开始":match.phase==Match.Phase.PLAYING?"连续点击，把对方推出圈外":"单指操作 · 长按不连发";
            text(caption,200,742+down,13,ink);
            if(match.phase==Match.Phase.PLAYING) {
                for(int i=0;i<3;i++) {float x=56+i*11;line(x,716+down,x+6,710+down,color,4);line(x+6,710+down,x+12,716+down,color,4);}
            }
            c.restore();
        }
        void drawArena(long now) {
            box(30,248,370,566,32,ink,0);
            box(26,239,374,555,32,yellow,6);
            // Flat shaded clay mat, heavy outline and a white ring boundary.
            oval(41,248,359,552,Color.rgb(226,156,41),7);
            oval(44,246,356,542,Color.rgb(255,215,111),7);
            oval(57,258,343,530,Color.rgb(255,221,133),0);
            stroke(Color.rgb(255,250,224),8);c.drawCircle(200,394,128,p);
            for(int i=0;i<12;i++) {
                double angle=i*Math.PI/6;
                float x=200+(float)Math.cos(angle)*145,y=394+(float)Math.sin(angle)*137;
                line(x-3,y-2,x+3,y+2,Color.rgb(189,131,43),3);
            }
            line(174,377,190,377,Color.rgb(233,177,80),4);line(210,411,226,411,Color.rgb(233,177,80),4);
            float redPosition=motions[0].value(now),bluePosition=motions[1].value(now);
            float base=(VisualPosition.map(redPosition)+VisualPosition.map(bluePosition))*50;
            float redPulse=pulse(now,0), bluePulse=pulse(now,1);
            float resultTime=resultAt<0?0:Math.min(1,(now-resultAt)/550f);
            boolean ended=match.phase==Match.Phase.RESULT;
            float redFall=ended&&match.winner()<0?resultTime:0;
            float blueFall=ended&&match.winner()>0?resultTime:0;
            float redBase=VisualPosition.map(redPosition)*100,blueBase=VisualPosition.map(bluePosition)*100;
            if(!ended){redBase=Math.max(-95,Math.min(95,redBase));blueBase=Math.max(-95,Math.min(95,blueBase));}
            drawJumpTrail(motions[0],-32,red,now);drawJumpTrail(motions[1],32,blue,now);
            // Keep v1.4's planted cross-step: only an accepted tap sets a new
            // location. The brief transition softens it without any auto-coast.
            float redY=394-32+redBase-redFall*24,blueY=394+32+blueBase+blueFall*24;
            if(!ended){
                redY+=duel.offset(0,now)*VisualPosition.movementGain(redPosition);
                blueY+=duel.offset(1,now)*VisualPosition.movementGain(bluePosition);
                // Exertion never visually pushes a center across the boundary
                // before an actual scoring ring-out.
                redY=Math.max(268,Math.min(520,redY));blueY=Math.max(268,Math.min(520,blueY));
            }
            wrestler(200,redY,red,true,redPulse,redFall,motions[0].hop(now),motions[0].landing(now));
            wrestler(200,blueY,blue,false,bluePulse,blueFall,motions[1].hop(now),motions[1].landing(now));
            if(now-hit[lastSide]<150) {
                float age=(now-hit[lastSide])/150f;
                c.save();c.translate(200,394+base);c.scale(1+age*.5f,1+age*.5f);
                spark(-60,0,lastSide==0?red:blue);spark(60,0,lastSide==0?red:blue);
                c.restore();
            }
            if(ended && resultTime<1) outlinedText("出界！",200,405,40,Color.WHITE);
        }
        float pulse(long now,int side) {
            long age=now-hit[side];
            if(age<0||age>=190)return 0;
            // Fast 24ms attack, visible 166ms recoil. Independent of ring physics.
            if(age<24)return .45f+.55f*age/24f;
            return (float)Math.pow(1-(age-24)/166f,2);
        }
        void drawJumpTrail(PushMotion motion,float shift,int color,long now) {
            long age=motion.age(now);
            if(age<0||age>35||Math.abs(motion.destination()-motion.departure())<.025f)return;
            float start=394+shift+VisualPosition.map(motion.departure())*100;
            float end=394+shift+VisualPosition.map(motion.destination())*100;
            int trail=Color.argb(55,Color.red(color),Color.green(color),Color.blue(color));
            line(178,start,178,end,trail,3);line(222,start,222,end,trail,3);
        }
        void wrestler(float x,float y,int color,boolean upper,float thrust,float fall,float hop,float landing) {
            c.save();c.translate(x,y);if(upper)c.rotate(180);
            c.rotate(fall*(upper?-45:45));
            oval(-43+hop*6,5+hop*3,43-hop*6,37-hop*3,Color.argb(35,32,34,41),0);
            if(landing>0){
                oval(-53-landing*5,22,-40,30,Color.argb(130,255,249,229),0);
                oval(40,22,53+landing*5,30,Color.argb(130,255,249,229),0);
            }
            c.scale(1+thrust*.04f+hop*.09f+landing*.08f,1-thrust*.03f+hop*.09f-landing*.08f);
            // Feet, heavy body, colored mawashi, raised palms and signature topknot.
            oval(-30,17,-8,40,skin,5);oval(8,17,30,40,skin,5);
            oval(-43,-25,43,30,skin,6);
            box(-35,10,35,28,8,color,6);box(-10,13,10,34,4,color,5);
            float arm=thrust*5;
            oval(-53,-33-arm,-25,-4-arm,skin,5);
            oval(25,-33-arm,53,-4-arm,skin,5);
            line(-43,-22-arm,-37,-25-arm,ink,2.5f);line(37,-25-arm,43,-22-arm,ink,2.5f);
            oval(-26,-40,26,4,ink,4);
            oval(-23,-32,23,6,skin,4);
            // Hair cap and cheeks remain legible at phone scale.
            fill(ink);path.reset();path.moveTo(-24,-21);path.quadTo(-27,-40,0,-40);path.quadTo(27,-40,24,-21);
            path.quadTo(13,-19,4,-29);path.quadTo(-7,-18,-24,-21);path.close();c.drawPath(path,p);
            oval(-10,-49,10,-35,ink,3);box(-11,-38,11,-33,3,color,2.5f);
            line(-15,-15,-7,-13,ink,3);line(7,-13,15,-15,ink,3);
            line(-4,-3,4,-3,ink,2.5f);
            oval(-21,-9,-14,-4,Color.rgb(255,153,124),0);oval(14,-9,21,-4,Color.rgb(255,153,124),0);
            if(thrust>.25f) {
                line(-42,42,-45,50,Color.WHITE,3);line(42,42,45,50,Color.WHITE,3);
            }
            c.restore();
        }
        void spark(float x,float y,int color) {
            c.save();c.translate(x,y);if(x<0)c.rotate(180);
            path.reset();path.moveTo(0,0);path.lineTo(13,-14);path.lineTo(12,-3);path.lineTo(23,-3);
            path.lineTo(10,8);path.lineTo(11,19);path.close();fill(color);c.drawPath(path,p);stroke(ink,3);c.drawPath(path,p);c.restore();
        }
        void drawStatus(long now) {
            String title=match.phase==Match.Phase.READY?"指尖相扑":match.phase==Match.Phase.COUNTDOWN?"双方就位":match.phase==Match.Phase.RESULT?"胜负已分":"把对手推出圈外！";
            text(title,193,225,22,ink);
            String hint;
            if(match.phase==Match.Phase.READY) hint="一人一端 · 同时点击 · 推出即胜";
            else if(match.phase==Match.Phase.COUNTDOWN) hint="听到信号，一起发力！";
            else if(match.phase==Match.Phase.RESULT) hint="出界即结束，下一局再较量";
            else if(Math.abs(match.position())>.7f) hint=match.position()>0?"蓝方顶住！快到边缘了":"红方顶住！快到边缘了";
            else if(match.pressure(now)>1.02f) hint=String.format(Locale.ROOT,"加力阶段 · 推力 ×%.1f",match.pressure(now));
            else hint="连续点击 · 每一下都向前顶！";
            text(hint,200,586,14,ink);
            if(match.phase==Match.Phase.PLAYING) {
                box(88,559,312,567,4,Color.rgb(226,209,171),0);
                float shown=(motions[0].value(now)+motions[1].value(now))/2;
                oval(195+shown*105,555,205+shown*105,571,shown>=0?red:blue,2);
            }
        }
        void drawSound() {
            oval(351,203,389,237,paper,3);
            text(audio.enabled?"♪":"×",370,228,24,ink);
            oval(11,203,49,237,paper,3);
            text("力",30,227,19,ink);
            oval(11,562,49,596,paper,3);
            line(21,579,30,571,ink,2.8f);line(30,571,39,579,ink,2.8f);
            line(24,578,24,587,ink,2.8f);line(24,587,36,587,ink,2.8f);line(36,587,36,578,ink,2.8f);
        }
        void showReturnMenu(){
            if(match.phase==Match.Phase.READY||match.phase==Match.Phase.RESULT){showLobby();return;}
            if(!pauseForSettings())return;
            LinearLayout panel=paperPanel("歇一下","对战已暂停 · 返回大厅会结束本局");
            Button resume=paperButton("继续较量",yellow),home=paperButton("返回大厅",paper);
            panel.addView(resume,new LinearLayout.LayoutParams(-1,dp(58)));
            LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(58));hp.topMargin=dp(10);panel.addView(home,hp);
            final AlertDialog dialog=showPaperSettings(panel);
            resume.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){dialog.dismiss();}});
            home.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){dialog.dismiss();showLobby();}});
        }
        TextView settingText(String label,float size) {
            TextView view=new TextView(MainActivity.this);
            view.setText(label);view.setTextSize(size);view.setTextColor(ink);
            view.setGravity(Gravity.CENTER);return view;
        }
        int dp(float value) { return Math.round(value*getResources().getDisplayMetrics().density); }
        GradientDrawable rounded(int color,int radius,int width) {
            GradientDrawable drawable=new GradientDrawable();drawable.setColor(color);
            drawable.setCornerRadius(dp(radius));if(width>0)drawable.setStroke(dp(width),ink);
            return drawable;
        }
        Drawable paperButtonFace(int color,boolean pressed) {
            LayerDrawable layers=new LayerDrawable(new Drawable[]{rounded(ink,12,0),rounded(color,12,3)});
            layers.setLayerInset(1,0,pressed?dp(4):0,0,pressed?dp(2):dp(6));
            return layers;
        }
        Button paperButton(String label,int color) {
            Button button=new Button(MainActivity.this);button.setText(label);
            button.setTextColor(ink);button.setTextSize(15);button.setAllCaps(false);
            button.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));
            button.setGravity(Gravity.CENTER);button.setPadding(dp(6),0,dp(6),dp(5));
            StateListDrawable background=new StateListDrawable();
            background.addState(new int[]{android.R.attr.state_pressed},paperButtonFace(color,true));
            background.addState(new int[]{},paperButtonFace(color,false));
            button.setBackground(background);return button;
        }
        void styleSlider(SeekBar slider) {
            GradientDrawable bed=rounded(paper,8,3),progress=rounded(yellow,8,3);
            ClipDrawable fill=new ClipDrawable(progress,Gravity.LEFT,ClipDrawable.HORIZONTAL);
            LayerDrawable track=new LayerDrawable(new Drawable[]{bed,fill});
            track.setId(0,android.R.id.background);track.setId(1,android.R.id.progress);
            for(int i=0;i<2;i++){track.setLayerHeight(i,dp(13));track.setLayerGravity(i,Gravity.CENTER_VERTICAL);}
            slider.setProgressTintList(null);slider.setProgressBackgroundTintList(null);slider.setThumbTintList(null);
            slider.setProgressDrawable(track);
            GradientDrawable rim=new GradientDrawable();rim.setShape(GradientDrawable.OVAL);
            rim.setColor(paper);rim.setStroke(dp(3),ink);rim.setSize(dp(32),dp(32));
            GradientDrawable dot=new GradientDrawable();dot.setShape(GradientDrawable.OVAL);dot.setColor(blue);
            LayerDrawable thumb=new LayerDrawable(new Drawable[]{rim,dot});
            thumb.setLayerSize(1,dp(10),dp(10));thumb.setLayerGravity(1,Gravity.CENTER);
            slider.setThumb(thumb);slider.setThumbOffset(dp(16));slider.setSplitTrack(false);
            slider.setPadding(dp(17),0,dp(17),0);
        }
        boolean pauseForSettings() {
            if(settingsDialog!=null)return false;
            if(mini!=null)mini.setPaused(true);
            long now=gameTime();match.update(now);syncSounds(now);
            pausedAt=SystemClock.elapsedRealtime();
            owners.clear();audio.pause();
            return true;
        }
        LinearLayout paperPanel(String heading,String subtitle) {
            LinearLayout panel=new LinearLayout(MainActivity.this);
            panel.setOrientation(LinearLayout.VERTICAL);
            int padding=dp(25);
            panel.setPadding(padding,dp(29),padding,dp(30));
            TextView title=settingText(heading,25);title.setTypeface(Typeface.DEFAULT_BOLD);
            panel.addView(title);
            TextView tag=settingText(subtitle,12);
            tag.setPadding(0,dp(7),0,dp(17));panel.addView(tag);
            return panel;
        }
        void saveSettings() {
            getPreferences(MODE_PRIVATE).edit().putFloat("standardStrength",match.strength())
                .putBoolean("sound",audio.enabled).putFloat("clickVolume",audio.settings.clickVolume())
                .putFloat("countdownVolume",audio.settings.countdownVolume())
                .putFloat("resultVolume",audio.settings.resultVolume()).putInt("clickStyle",audio.settings.style()).apply();
        }
        AlertDialog showPaperSettings(LinearLayout panel) {
            ScrollView sheet=new ScrollView(MainActivity.this);
            sheet.setBackground(new PaperPanelDrawable(getResources().getDisplayMetrics().density));
            sheet.setClipToPadding(false);sheet.addView(panel);
            AlertDialog dialog=new AlertDialog.Builder(MainActivity.this).create();
            dialog.setView(sheet,0,0,0,0);settingsDialog=dialog;
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override public void onDismiss(DialogInterface ignored) {
                    if(pausedAt>=0){pausedMillis+=SystemClock.elapsedRealtime()-pausedAt;pausedAt=-1;}
                    settingsDialog=null;owners.clear();audio.pause();saveSettings();invalidate();
                    if(mini!=null&&foreground)mini.setPaused(false);
                }
            });
            dialog.show();
            if(dialog.getWindow()!=null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.getWindow().setLayout(Math.min(getResources().getDisplayMetrics().widthPixels-dp(24),dp(410)),WindowManager.LayoutParams.WRAP_CONTENT);
                dialog.getWindow().setDimAmount(.5f);
            }
            return dialog;
        }
        void showForceSettings() {
            if(!pauseForSettings())return;
            LinearLayout panel=paperPanel("推力调节","双方共用 · 拖动调整");
            final TextView value=settingText("",39);value.setTypeface(Typeface.DEFAULT_BOLD);
            panel.addView(value);
            final TextView level=settingText("",14);level.setPadding(0,dp(2),0,dp(12));panel.addView(level);
            final SeekBar slider=new SeekBar(MainActivity.this);
            styleSlider(slider);
            slider.setMax(Math.round((Match.MAX_STRENGTH-Match.MIN_STRENGTH)*1000));
            slider.setProgress(Math.round((match.strength()-Match.MIN_STRENGTH)*1000));
            slider.setContentDescription("双方共同的推力，零点二五倍到两倍，标准为一倍");
            value.setText(String.format(Locale.ROOT,"%.2f 倍",match.strength()));
            level.setText(Math.abs(match.strength()-1)<.005f?"标准推力":match.strength()<1?"轻推 · 更细腻":"强推 · 更干脆");
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar,int progress,boolean fromUser) {
                    match.setStrength(Match.MIN_STRENGTH+progress/1000f);
                    value.setText(String.format(Locale.ROOT,"%.2f 倍",match.strength()));
                    level.setText(Math.abs(match.strength()-1)<.005f?"标准推力":match.strength()<1?"轻推 · 更细腻":"强推 · 更干脆");
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            panel.addView(slider,new LinearLayout.LayoutParams(-1,dp(50)));
            LinearLayout ends=new LinearLayout(MainActivity.this);
            TextView light=settingText("0.25 倍 · 轻推",12),strong=settingText("2 倍 · 强推",12);
            light.setGravity(Gravity.LEFT);strong.setGravity(Gravity.RIGHT);
            ends.addView(light,new LinearLayout.LayoutParams(0,-2,1));
            ends.addView(strong,new LinearLayout.LayoutParams(0,-2,1));panel.addView(ends);
            TextView help=settingText("双方共用，调节时暂停。\n6 秒后缓慢加力，最多增加 35%。",13);
            help.setLineSpacing(dp(4),1);help.setPadding(0,dp(21),0,dp(20));panel.addView(help);
            LinearLayout buttons=new LinearLayout(MainActivity.this);
            Button restore=paperButton("恢复标准",paper),done=paperButton("好了，继续",yellow);
            buttons.addView(restore,new LinearLayout.LayoutParams(0,dp(62),1));
            LinearLayout.LayoutParams doneParams=new LinearLayout.LayoutParams(0,dp(62),1);doneParams.leftMargin=dp(10);
            buttons.addView(done,doneParams);panel.addView(buttons);
            final AlertDialog dialog=showPaperSettings(panel);
            restore.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View ignored) {
                    slider.setProgress(Math.round((Match.DEFAULT_STRENGTH-Match.MIN_STRENGTH)*1000));
                }
            });
            done.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View ignored) {dialog.dismiss();}
            });
        }
        void addVolumeControl(LinearLayout panel,final int category,String name) {
            final TextView label=settingText("",15);label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setPadding(0,dp(13),0,0);panel.addView(label);
            final SeekBar slider=new SeekBar(MainActivity.this);styleSlider(slider);slider.setMax(1000);
            float volume=category==0?audio.settings.clickVolume():category==1?audio.settings.countdownVolume():audio.settings.resultVolume();
            slider.setProgress(Math.round(volume*1000));slider.setContentDescription(name+"，百分之零到百分之一百");
            label.setText(name+"  "+Math.round(volume*100)+"%");
            final String title=name;
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar,int value,boolean fromUser) {
                    float level=value/1000f;
                    if(category==0)audio.settings.setClickVolume(level);
                    else if(category==1)audio.settings.setCountdownVolume(level);
                    else audio.settings.setResultVolume(level);
                    label.setText(title+"  "+Math.round(level*100)+"%");
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {
                    if(category==0){if(mini!=null)audio.action(mini.id,false);else audio.push(0,false);}else if(category==1)audio.tick(false);else audio.win();
                }
            });
            panel.addView(slider,new LinearLayout.LayoutParams(-1,dp(47)));
        }
        void showAudioSettings() {
            if(!pauseForSettings())return;
            LinearLayout panel=paperPanel("音效与音量",audio.enabled?"选择即试听 · 设置自动保存":"当前静音 · 下方可开启声音");
            final TextView audioTag=(TextView)panel.getChildAt(1);
            TextView choose=settingText(mini==null?"点击音效":"本游戏专属音效 · 动作与引擎共用音量",16);choose.setTypeface(Typeface.DEFAULT_BOLD);panel.addView(choose);
            final Button[] choices=new Button[SoundSettings.CLICK_NAMES.length];
            for(int i=0;mini==null&&i<choices.length;i++) {
                final int style=i;
                Button option=paperButton((audio.settings.style()==i?"✓  ":"试听  ")+SoundSettings.CLICK_NAMES[i],audio.settings.style()==i?yellow:paper);
                choices[i]=option;
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(48));params.topMargin=dp(6);panel.addView(option,params);
                option.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View ignored) {
                        audio.settings.setStyle(style);
                        for(int j=0;j<choices.length;j++) {
                            choices[j].setText((j==style?"✓  ":"试听  ")+SoundSettings.CLICK_NAMES[j]);
                            StateListDrawable background=new StateListDrawable();int color=j==style?yellow:paper;
                            background.addState(new int[]{android.R.attr.state_pressed},paperButtonFace(color,true));
                            background.addState(new int[]{},paperButtonFace(color,false));choices[j].setBackground(background);
                        }
                        audio.push(0,false);
                    }
                });
            }
            addVolumeControl(panel,0,mini==null?"点击音量":"动作／引擎音量");addVolumeControl(panel,1,"倒计时音量");addVolumeControl(panel,2,"结算音量");
            TextView help=settingText("0% 即静音，音量也受手机媒体音量影响。\n松开滑杆试听，设置会自动保存。",12);
            help.setLineSpacing(dp(3),1);help.setPadding(0,dp(7),0,dp(16));panel.addView(help);
            LinearLayout footer=new LinearLayout(MainActivity.this);
            final Button master=paperButton(audio.enabled?"声音：开":"声音：关",paper);
            Button done=paperButton("好了，继续",yellow);
            footer.addView(master,new LinearLayout.LayoutParams(0,dp(59),1));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,dp(59),1);params.leftMargin=dp(10);footer.addView(done,params);panel.addView(footer);
            final AlertDialog dialog=showPaperSettings(panel);
            master.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View ignored) {
                    audio.enabled=!audio.enabled;master.setText(audio.enabled?"声音：开":"声音：关");
                    audioTag.setText(audio.enabled?"选择即试听 · 设置自动保存":"当前静音 · 下方可开启声音");
                    if(audio.enabled){if(mini!=null)audio.action(mini.id,false);else audio.push(0,false);}else audio.pause();
                }
            });
            done.setOnClickListener(new View.OnClickListener() {@Override public void onClick(View ignored){dialog.dismiss();}});
        }
        void drawResult(long now) {
            fill(Color.argb(155,32,34,41));c.drawRect(-100,-200,500,1000,p);
            for(int i=0;i<16;i++) {
                float x=20+(i*73)%360,y=160+(i*91)%440;
                c.save();c.rotate(i*23,x,y);box(x,y,x+7,y+13,1,i%2==0?red:blue,0);c.restore();
            }
            box(35,251,373,577,23,ink,0);box(29,243,367,567,23,paper,6);
            int color=match.winner()>0?red:blue;
            box(51,265,345,324,14,color,4);
            text(match.winner()>0?"红方获胜！":"蓝方获胜！",198,305,31,ink);
            text("把对手推出了相扑垫",198,352,16,ink);
            String duration=String.format(Locale.ROOT,"用时 %.1f 秒",match.elapsed(now)/1000f);
            text(duration,198,384,22,ink);
            line(67,405,329,405,Color.rgb(224,211,183),2);
            text("红方",118,433,16,red);text("蓝方",278,433,16,blue);
            text(match.taps[0]+" 次",118,465,26,ink);text(match.taps[1]+" 次",278,465,26,ink);
            box(61,495,335,548,14,ink,0);box(61,489,335,542,14,yellow,4);
            text("再战一局",198,524,23,ink);
            box(101,582,299,633,13,paper,4);text("返回大厅",200,615,20,ink);
        }
        int ownerCount(int side) {
            int count=0;for(int i=0;i<owners.size();i++)if(owners.valueAt(i)==side)count++;return count;
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action=event.getActionMasked(),index=event.getActionIndex(),id=event.getPointerId(index);
            if(action==MotionEvent.ACTION_CANCEL){owners.clear();return true;}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){owners.delete(id);return true;}
            if(action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_POINTER_DOWN)return true;
            long now=gameTime();match.update(now);syncSounds(now);
            float x=(event.getX(index)-originX)/scale,y=(event.getY(index)-originY)/scale;
            if(match.phase==Match.Phase.RESULT) {
                if(now-resultAt>700&&event.getPointerCount()==1&&x>=61&&x<=335&&y>=489&&y<=548){reset();performClick();}
                else if(now-resultAt>700&&event.getPointerCount()==1&&x>=101&&x<=299&&y>=582&&y<=633){showLobby();performClick();}
                return true;
            }
            if(x>=6&&x<=54&&y>=558&&y<=600){showReturnMenu();performClick();return true;}
            if(x>=347&&x<=394&&y>=198&&y<=242) {
                showAudioSettings();performClick();return true;
            }
            if(x>=6&&x<=54&&y>=198&&y<=242) {showForceSettings();performClick();return true;}
            int side=y<400?0:1;
            float localY=side==0?800-y:y;
            if(x<24||x>376||localY<644||localY>778)return true;
            int held=ownerCount(side);owners.put(id,side);
            if(held>0)return true;
            if(match.phase==Match.Phase.READY){match.prepare(side,now);audio.tick(false);}
            else if(match.tap(side,now)) {
                duel.strike(side,now);
                motions[0].stepTo(match.position(),now,side==0);
                motions[1].stepTo(match.position(),now,side==1);
                hit[side]=now;lastSide=side;
                audio.push(side,Math.abs(match.position())>.7f);
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            }
            performClick();invalidate();return true;
        }
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
