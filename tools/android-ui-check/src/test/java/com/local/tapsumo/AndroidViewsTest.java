package com.local.tapsumo;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.SeekBar;
import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowSystemClock;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.android.controller.ActivityController;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(manifest=Config.NONE,sdk=28)
@GraphicsMode(GraphicsMode.Mode.NATIVE) @LooperMode(LooperMode.Mode.PAUSED)
public class AndroidViewsTest {
    static final MiniGameView.Host HOST=new MiniGameView.Host(){public void home(){}public void menu(){}public void audio(){}public void undo(){}public void finished(int winner){}};
    static void draw(View v,Bitmap b){v.measure(View.MeasureSpec.makeMeasureSpec(b.getWidth(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(b.getHeight(),View.MeasureSpec.EXACTLY));v.layout(0,0,b.getWidth(),b.getHeight());v.draw(new Canvas(b));}
    static void touch(View v,int action,int[] ids,float[][] xy){long t=SystemClock.uptimeMillis();MotionEvent.PointerProperties[] pp=new MotionEvent.PointerProperties[ids.length];MotionEvent.PointerCoords[] pc=new MotionEvent.PointerCoords[ids.length];for(int i=0;i<ids.length;i++){pp[i]=new MotionEvent.PointerProperties();pp[i].id=ids[i];pp[i].toolType=MotionEvent.TOOL_TYPE_FINGER;pc[i]=new MotionEvent.PointerCoords();pc[i].x=xy[i][0];pc[i].y=xy[i][1];pc[i].pressure=pc[i].size=1;}MotionEvent e=MotionEvent.obtain(t,t,action,ids.length,pp,pc,0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0);v.dispatchTouchEvent(e);e.recycle();}
    static void tap(View v,float x,float y){touch(v,MotionEvent.ACTION_DOWN,new int[]{0},new float[][]{{x,y}});touch(v,MotionEvent.ACTION_UP,new int[]{0},new float[][]{{x,y}});Shadows.shadowOf(Looper.getMainLooper()).idle();}
    static void save(Bitmap b,String name)throws Exception{File d=new File(System.getProperty("arcade.root"),"docs/界面预览");d.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(d,name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}}
    static View named(View root,String text){if(root instanceof TextView&&text.equals(((TextView)root).getText().toString()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View found=named(g.getChildAt(i),text);if(found!=null)return found;}}return null;}
    static void click(View root,String label){View found=named(root,label);assertNotNull(label,found);assertTrue(found.performClick());Shadows.shadowOf(Looper.getMainLooper()).idle();}
    static SeekBar slider(View root){if(root instanceof SeekBar)return (SeekBar)root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){SeekBar s=slider(g.getChildAt(i));if(s!=null)return s;}}return null;}
    static Object field(Object target,String name)throws Exception{java.lang.reflect.Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    static void open(MainActivity a,GameCatalog.Id id)throws Exception{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("openGame",GameCatalog.Id.class);m.setAccessible(true);m.invoke(a,id);}

    @Test @Config(sdk={28,35}) public void everyMatchRendersAndStartsWithActualTouches()throws Exception{
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();GameAudio audio=new GameAudio(activity,false);Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);
        for(GameCatalog.Game entry:GameCatalog.ALL){if(entry.id==GameCatalog.Id.SUMO)continue;MiniGameView v=new MiniGameView(activity,entry.id,audio,HOST);draw(v,b);tap(v,200,400);assertEquals(MiniGameView.Phase.READY,v.phase);tap(v,200,260);tap(v,200,540);assertEquals(MiniGameView.Phase.COUNTDOWN,v.phase);for(int i=0;i<205;i++){ShadowSystemClock.advanceBy(Duration.ofMillis(16));draw(v,b);}assertEquals(MiniGameView.Phase.PLAYING,v.phase);
            if(v.board!=null){if(v.board.size==3){tap(v,108,286);assertEquals(1,v.board.moves);}else{tap(v,200,394);tap(v,200,112);assertEquals(1,v.board.moves);}}
            if(v.pool!=null){tap(v,200,300);draw(v,b);save(b,"pool-aim");tap(v,200,v.pool.turn==0?33:767);assertTrue(v.pool.moving);}
            double seconds=v.seconds;v.setPaused(true);ShadowSystemClock.advanceBy(Duration.ofSeconds(4));draw(v,b);assertEquals(seconds,v.seconds,.001);v.setPaused(false);v.resumeCountdown=0;draw(v,b);save(b,entry.id.name().toLowerCase());
        }audio.release();activity.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void tankFourFingerChannelsAndPinballArmsStayIndependent()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();GameAudio audio=new GameAudio(a,false);Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);MiniGameView v=new MiniGameView(a,GameCatalog.Id.TANK,audio,HOST);v.phase=MiniGameView.Phase.PLAYING;draw(v,b);
        float[][] all={{296,30},{108,75},{104,770},{292,725}};
        for(int count=1;count<=4;count++){int[] ids=new int[count];float[][] xy=new float[count][];for(int j=0;j<count;j++){ids[j]=j;xy[j]=all[j];}touch(v,count==1?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_POINTER_DOWN|((count-1)<<8),ids,xy);}
        assertEquals(4,v.owners.size());assertTrue(v.tank.moveY[0]<0);assertTrue(v.tank.aimY[0]>0);assertTrue(v.tank.moveY[1]>0);assertTrue(v.tank.aimY[1]<0);
        touch(v,MotionEvent.ACTION_POINTER_UP,new int[]{0,1,2,3},all);assertEquals(0,v.tank.moveY[0],.001);assertTrue(v.tank.aimY[0]>0);assertTrue(v.tank.moveY[1]>0);
        touch(v,MotionEvent.ACTION_CANCEL,new int[]{1,2,3},new float[][]{all[1],all[2],all[3]});assertEquals(0,v.tank.aimY[0],.001);assertEquals(0,v.tank.moveY[1],.001);
        touch(v,MotionEvent.ACTION_DOWN,new int[]{7},new float[][]{{350,728}});assertEquals(1,v.owners.size());assertTrue(v.tank.aimX[1]>.99);touch(v,MotionEvent.ACTION_UP,new int[]{7},new float[][]{{350,728}});
        MiniGameView pin=new MiniGameView(a,GameCatalog.Id.PINBALL,audio,HOST);pin.phase=MiniGameView.Phase.PLAYING;draw(pin,b);tap(pin,100,50);assertTrue(pin.ball.flip[0]>0);assertEquals(0,pin.ball.flip[1],.001);tap(pin,280,50);assertTrue(pin.ball.flip[1]>0);tap(pin,100,750);assertTrue(pin.ball.flip[2]>0);tap(pin,280,750);assertTrue(pin.ball.flip[3]>0);
        v.setPaused(true);v.setPaused(false);assertEquals(1.5,v.resumeCountdown,.001);double y=v.tank.y[0];for(int i=0;i<50;i++){ShadowSystemClock.advanceBy(Duration.ofMillis(16));draw(v,b);}assertEquals(y,v.tank.y[0],.001);audio.release();a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void optionsPersistCancelAndAudioUsePaperDialogs()throws Exception{
        RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).setup();MainActivity a=controller.get();open(a,GameCatalog.Id.TANK);MiniGameView v=(MiniGameView)field(a,"mini");Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);draw(v,b);tap(v,377,394);assertTrue(v.paused);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View panel=dialog.findViewById(android.R.id.content);assertNotNull(named(panel,"玩法调节"));slider(panel).setProgress(55);assertEquals(1,v.options.speed,.001);click(panel,"保存并重新准备");assertFalse(v.paused);assertEquals(1.2,v.options.speed,.001);assertEquals(1.2,v.tank.pace,.001);open(a,GameCatalog.Id.TANK);v=(MiniGameView)field(a,"mini");assertEquals(1.2,v.options.speed,.001);draw(v,b);tap(v,377,394);dialog=ShadowAlertDialog.getLatestAlertDialog();panel=dialog.findViewById(android.R.id.content);slider(panel).setProgress(10);click(panel,"取消，继续原局");assertEquals(1.2,v.options.speed,.001);
        v.phase=MiniGameView.Phase.PLAYING;tap(v,377,330);dialog=ShadowAlertDialog.getLatestAlertDialog();panel=dialog.findViewById(android.R.id.content);slider(panel).setProgress(123);click(panel,"好了，继续");assertEquals(.123f,a.getPreferences(Activity.MODE_PRIVATE).getFloat("clickVolume",0),.001f);
        tap(v,377,267);dialog=ShadowAlertDialog.getLatestAlertDialog();assertTrue(v.paused);controller.pause().resume();assertTrue(v.paused);assertTrue(dialog.isShowing());click(dialog.findViewById(android.R.id.content),"继续对战");assertFalse(v.paused);
        v.finish(0);v.finish(1);assertEquals(1,a.getPreferences(Activity.MODE_PRIVATE).getInt("seriesRed",0));assertEquals(0,a.getPreferences(Activity.MODE_PRIVATE).getInt("seriesBlue",0));controller.pause().stop().destroy();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void smallScreenInverseCoordinatesAndBothResultEndsWork()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();GameAudio audio=new GameAudio(a,false);MiniGameView v=new MiniGameView(a,GameCatalog.Id.GOMOKU,audio,HOST);v.phase=MiniGameView.Phase.PLAYING;Bitmap b=Bitmap.createBitmap(320,568,Bitmap.Config.ARGB_8888);draw(v,b);tap(v,v.ox+200*v.scale,v.oy+394*v.scale);tap(v,v.ox+200*v.scale,v.oy+688*v.scale);assertEquals(0,v.board.moves);tap(v,v.ox+200*v.scale,v.oy+112*v.scale);assertEquals(1,v.board.moves);
        v.board.reset(0);for(int i=0;i<5;i++){v.board.play(i+5,7);if(i<4)v.board.play(i+5,3);}v.finish(0);v.resultAge=1;Bitmap full=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);draw(v,full);save(full,"facing-result");tap(v,200,175);assertEquals(1,v.starter);assertEquals(MiniGameView.Phase.READY,v.phase);v.phase=MiniGameView.Phase.PLAYING;v.finish(1);v.resultAge=1;tap(v,200,625);assertEquals(0,v.starter);audio.release();a.finish();b.recycle();full.recycle();
    }
    @Test @Config(sdk={28,35}) public void lobbyAndThreeDrivingMapsRender()throws Exception{
        RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();Bitmap b=Bitmap.createBitmap(432,880,Bitmap.Config.ARGB_8888);View root=a.findViewById(android.R.id.content);draw(root,b);save(b,"lobby-native-test");assertNotNull(named(root,"9 个游戏"));
        for(GameCatalog.Game g:GameCatalog.ALL){open(a,g.id);draw(root,b);if(g.id==GameCatalog.Id.SUMO)save(b,"sumo");}
        for(GameCatalog.Id id:new GameCatalog.Id[]{GameCatalog.Id.RACING,GameCatalog.Id.TANK,GameCatalog.Id.PINBALL})for(int map=0;map<3;map++){open(a,id);MiniGameView v=(MiniGameView)field(a,"mini");v.options.map=map;v.reset(false);v.phase=MiniGameView.Phase.PLAYING;draw(root,b);save(b,id.name().toLowerCase()+"-map-"+map);}
        a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void poolCoordinatesAndLargerRaceStick()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();GameAudio audio=new GameAudio(a,false);Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);
        MiniGameView pool=new MiniGameView(a,GameCatalog.Id.POOL,audio,HOST);pool.phase=MiniGameView.Phase.PLAYING;draw(pool,b);tap(pool,90,300);assertEquals(90,pool.aimX,.001);assertEquals(300,pool.aimY,.001);
        MiniGameView race=new MiniGameView(a,GameCatalog.Id.RACING,audio,HOST);race.phase=MiniGameView.Phase.PLAYING;draw(race,b);touch(race,MotionEvent.ACTION_DOWN,new int[]{0},new float[][]{{233,728}});assertEquals(.5,race.race.inputX[1],.001);assertEquals(66,race.stickRadius(),.001);assertEquals(ArcadeTheme.RED,b.getPixel(5,10));assertEquals(ArcadeTheme.BLUE,b.getPixel(390,790));touch(race,MotionEvent.ACTION_CANCEL,new int[]{0},new float[][]{{233,728}});touch(race,MotionEvent.ACTION_DOWN,new int[]{0},new float[][]{{270,728}});assertEquals(1,race.owners.size());assertEquals(1,race.race.inputX[1],.001);race.setPaused(true);assertEquals(0,race.race.inputX[1],.001);
        MiniGameView pin=new MiniGameView(a,GameCatalog.Id.PINBALL,audio,HOST);pin.phase=MiniGameView.Phase.PLAYING;pin.ball.clock=1.5;draw(pin,b);save(b,"pinball-moving");audio.release();a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void recommendationDustAndCompactSparks()throws Exception{
        RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();ArcadeScreens.Lobby lobby=(ArcadeScreens.Lobby)field(a,"lobby");assertEquals(0,lobby.recommendation);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(5100));assertEquals(1,lobby.recommendation);assertTrue(lobby.heroHeadline.getText().toString().contains("弹球"));click(lobby,"Ⅱ");assertFalse(lobby.autoRecommend);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));assertEquals(1,lobby.recommendation);click(lobby,"▶");assertTrue(lobby.autoRecommend);lobby.heroCard.performClick();assertEquals(GameCatalog.Id.PING_PONG,((MiniGameView)field(a,"mini")).id);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));assertEquals(1,lobby.recommendation);
        open(a,GameCatalog.Id.RACING);MiniGameView race=(MiniGameView)field(a,"mini");race.phase=MiniGameView.Phase.PLAYING;race.race.speed[0]=185;race.effects(.3);int count=0;for(MiniGameView.Particle q:race.particles)if(q.life>0)count++;assertTrue(count>5);race.race.steer(0,0,-1);for(int i=0;i<100;i++){race.race.update(1d/120);race.effects(1d/120);}Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);draw(race,b);save(b,"racing-dust");race.race.speed[0]=0;race.effects(2);for(MiniGameView.Particle q:race.particles)assertTrue(q.life<=0);a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void illustratedCarouselSwipesAndOpensCurrentGame()throws Exception{
        RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();ArcadeScreens.Lobby lobby=(ArcadeScreens.Lobby)field(a,"lobby");Bitmap b=Bitmap.createBitmap(432,880,Bitmap.Config.ARGB_8888);draw(a.findViewById(android.R.id.content),b);
        assertTrue("Compact recommendation stays below 180dp",lobby.heroCard.getHeight()<=180*a.getResources().getDisplayMetrics().density);assertEquals(9,lobby.heroPager.getChildCount());for(int i=0;i<9;i++){ViewGroup page=(ViewGroup)lobby.heroPager.getChildAt(i);assertTrue(page.getChildAt(0) instanceof GameArtView);assertEquals(GameCatalog.ALL.get(i).id,field(page.getChildAt(0),"id"));}
        float y=80,left=30,right=lobby.heroPager.getWidth()-30;
        touch(lobby.heroPager,MotionEvent.ACTION_DOWN,new int[]{0},new float[][]{{right,y}});Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(6));assertEquals(0,lobby.recommendation);touch(lobby.heroPager,MotionEvent.ACTION_MOVE,new int[]{0},new float[][]{{left,y}});touch(lobby.heroPager,MotionEvent.ACTION_UP,new int[]{0},new float[][]{{left,y}});assertEquals(1,lobby.recommendation);assertEquals(1,lobby.heroPager.getDisplayedChild());assertNull(field(a,"mini"));
        touch(lobby.heroPager,MotionEvent.ACTION_DOWN,new int[]{0},new float[][]{{left,y}});touch(lobby.heroPager,MotionEvent.ACTION_UP,new int[]{0},new float[][]{{right,y}});assertEquals(0,lobby.recommendation);
        touch(lobby.heroPager,MotionEvent.ACTION_DOWN,new int[]{0},new float[][]{{100,40}});touch(lobby.heroPager,MotionEvent.ACTION_MOVE,new int[]{0},new float[][]{{105,160}});touch(lobby.heroPager,MotionEvent.ACTION_CANCEL,new int[]{0},new float[][]{{105,160}});assertEquals(0,lobby.recommendation);assertNull(field(a,"mini"));
        lobby.showRecommendation(2,1,false);draw(a.findViewById(android.R.id.content),b);save(b,"lobby-carousel-pool");lobby.showRecommendation(8,1,false);draw(a.findViewById(android.R.id.content),b);save(b,"lobby-carousel-tank");tap(lobby.heroPager,100,80);assertEquals(GameCatalog.Id.TANK,((MiniGameView)field(a,"mini")).id);a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void pongRampSettingPersistsAndRallyRenders()throws Exception{
        RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();open(a,GameCatalog.Id.PING_PONG);MiniGameView v=(MiniGameView)field(a,"mini");assertEquals(1,v.ball.ramp);Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);draw(v,b);tap(v,377,394);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View sheet=dialog.findViewById(android.R.id.content);click(sheet,"升温方式：渐进 · 越打越快");click(sheet,"保存并重新准备");assertEquals(2,v.ball.ramp);open(a,GameCatalog.Id.PING_PONG);v=(MiniGameView)field(a,"mini");assertEquals(2,v.options.ramp);assertEquals(2,v.ball.ramp);v.phase=MiniGameView.Phase.PLAYING;v.ball.serve=0;v.ball.rallyHits=12;v.ball.precisionSide=0;v.ball.precisionAge=.4;v.ball.vx=320;v.ball.vy=400;draw(v,b);save(b,"pong-rally");double time=v.ball.rallySeconds;v.setPaused(true);ShadowSystemClock.advanceBy(Duration.ofSeconds(3));draw(v,b);assertEquals(time,v.ball.rallySeconds,.001);a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void poolFreeBallTouchFlowBothEnds()throws Exception{
        Activity a=Robolectric.buildActivity(Activity.class).setup().get();GameAudio audio=new GameAudio(a,false);Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);
        for(int starter=0;starter<2;starter++){MiniGameView v=new MiniGameView(a,GameCatalog.Id.POOL,audio,HOST);v.pool.reset(starter);v.phase=MiniGameView.Phase.PLAYING;v.pool.shoot(1,0,.05);v.pool.balls[0].x=50;v.pool.balls[0].y=160;v.pool.balls[0].vx=v.pool.balls[0].vy=0;for(int i=0;i<200&&v.pool.moving;i++)v.pool.update(1d/240);assertTrue(v.pool.ballInHand);assertEquals(1-starter,v.pool.turn);draw(v,b);
            float confirmY=v.pool.turn==0?33:767;tap(v,(float)v.pool.balls[1].x,(float)v.pool.balls[1].y);tap(v,200,confirmY);assertTrue(v.pool.ballInHand);touch(v,MotionEvent.ACTION_DOWN,new int[]{0},new float[][]{{180,460}});touch(v,MotionEvent.ACTION_MOVE,new int[]{0},new float[][]{{220,470}});touch(v,MotionEvent.ACTION_UP,new int[]{0},new float[][]{{220,470}});assertEquals(220,v.pool.placementX,.001);assertEquals(470,v.pool.placementY,.001);tap(v,200,800-confirmY);assertTrue(v.pool.ballInHand);draw(v,b);save(b,starter==0?"pool-free-ball-blue":"pool-free-ball-red");v.setPaused(true);assertTrue(v.pool.ballInHand&&v.pool.placementSelected);v.setPaused(false);v.resumeCountdown=0;tap(v,200,confirmY);assertFalse(v.pool.ballInHand);assertFalse(v.pool.moving);assertEquals(220,v.pool.balls[0].x,.001);tap(v,200,250);tap(v,200,confirmY);assertTrue(v.pool.moving);
        }audio.release();a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void destructibleCoverOptionsAndCracksRender()throws Exception{
        RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();open(a,GameCatalog.Id.TANK);MiniGameView v=(MiniGameView)field(a,"mini");Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);draw(v,b);tap(v,377,394);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View sheet=dialog.findViewById(android.R.id.content);click(sheet,"掩体：三发击碎");click(sheet,"保存并重新准备");assertEquals(4,v.tank.coverHits);open(a,GameCatalog.Id.TANK);v=(MiniGameView)field(a,"mini");assertEquals(3,v.options.cover);assertEquals(4,v.tank.coverHits);v.phase=MiniGameView.Phase.PLAYING;
        for(int i=0;i<3;i++){v.tank.shells.add(new TankMatch.Shell(120,270,Math.PI/2,0,1));v.tank.update(.06);}assertEquals(1,v.tank.wallHp[0]);draw(v,b);save(b,"tank-cover-cracked");v.tank.shells.add(new TankMatch.Shell(120,270,Math.PI/2,0,1));v.tank.update(.06);assertEquals(0,v.tank.wallHp[0]);ShadowSystemClock.advanceBy(Duration.ofMillis(16));draw(v,b);save(b,"tank-cover-destroyed");tap(v,377,394);sheet=ShadowAlertDialog.getLatestAlertDialog().findViewById(android.R.id.content);click(sheet,"掩体：四发击碎");click(sheet,"保存并重新准备");assertFalse(v.tank.destructible);a.finish();b.recycle();
    }
    @Test @Config(sdk={28,35}) public void poolRandomStarterAssignmentAndOptionsSheetRender()throws Exception{
        RuntimeEnvironment.getApplication().setTheme(android.R.style.Theme_Material_Light_NoActionBar);MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();open(a,GameCatalog.Id.POOL);MiniGameView v=(MiniGameView)field(a,"mini");v.random.setSeed(21);int redStarts=0;for(int i=0;i<30;i++){v.reset(false);if(v.pool.turn==0)redStarts++;}assertTrue("Seeded random opening chooses both sides",redStarts>0&&redStarts<30);
        v.pool.reset(1);v.phase=MiniGameView.Phase.PLAYING;v.pool.shoot(1,0,.05);v.pool.balls[0].vx=v.pool.balls[0].vy=0;v.pool.balls[1].x=50;v.pool.balls[1].y=160;for(int i=0;i<200&&v.pool.moving;i++)v.pool.update(1d/240);assertEquals(0,v.pool.group[1]);assertEquals(1,v.pool.group[0]);
        Bitmap b=Bitmap.createBitmap(400,800,Bitmap.Config.ARGB_8888);draw(v,b);save(b,"pool-assigned");tap(v,377,394);AlertDialog dialog=ShadowAlertDialog.getLatestAlertDialog();View sheet=dialog.findViewById(android.R.id.content);draw(sheet,b);save(b,"game-options");assertNotNull(named(sheet,"目标球预瞄：适度 · 限长一次反弹"));click(sheet,"取消，继续原局");a.finish();b.recycle();
    }
}
