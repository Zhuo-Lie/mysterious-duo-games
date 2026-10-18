package com.local.tapsumo;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.util.SparseIntArray;
import android.view.*;
import java.util.Locale;

/** Shared match shell; individual rule models contain no Android dependencies. */
final class MiniGameView extends View {
    interface Host { void home();void menu();void audio();void undo();void finished(int winner); }
    enum Phase { READY, COUNTDOWN, PLAYING, RESULT }
    final GameCatalog.Id id;
    final GameAudio audio;
    final Host host;
    final BoardMatch board;
    final BallDuel ball;
    final RaceMatch race;
    final PoolMatch pool;
    final TankMatch tank;
    final GameOptions options=new GameOptions();
    final java.util.Random random=new java.util.Random();
    final SparseIntArray channels=new SparseIntArray();
    final SparseIntArray owners=new SparseIntArray();
    final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    final Path path=new Path();
    final boolean[] ready={false,false};
    Phase phase=Phase.READY;
    Canvas c;
    float scale=1,ox,oy;
    long lastFrame,lastImpact;
    double accumulator,countdown,seconds,resultAge,resumeCountdown;
    boolean paused;
    int starter,winner=-1,countSound=-1,candidateX=-1,candidateY=-1,powerPointer=-1,aimPointer=-1;
    double aimX=200,aimY=250,power=.55;
    static final class Particle {double x,y,vx,vy,life,total,r;int kind;}
    final Particle[] particles=new Particle[160];
    final java.util.Random fxRandom=new java.util.Random(45);
    double dustClock;
    final Shader dustMaterial=new RadialGradient(0,0,1,new int[]{0x99c8ad7f,0x00c8ad7f},new float[]{0,1},Shader.TileMode.CLAMP);
    {for(int i=0;i<particles.length;i++)particles[i]=new Particle();}
    void particle(double x,double y,double vx,double vy,double life,double r,int kind){for(Particle q:particles)if(q.life<=0){q.x=x;q.y=y;q.vx=vx;q.vy=vy;q.life=q.total=life;q.r=r;q.kind=kind;break;}}
    void effects(double dt){
        for(Particle q:particles)if(q.life>0){q.life=Math.max(0,q.life-dt);q.x+=q.vx*dt;q.y+=q.vy*dt;double drag=Math.exp(-(q.kind==0?2:5)*dt);q.vx*=drag;q.vy*=drag;}
        if(race!=null){dustClock+=dt;while(dustClock>=.03){dustClock-=.03;for(int i=0;i<2;i++)if(race.speed[i]>25){double a=race.heading[i],side=fxRandom.nextBoolean()?1:-1,xx=race.x[i]-Math.cos(a)*19-Math.sin(a)*side*9,yy=race.y[i]-Math.sin(a)*19+Math.cos(a)*side*9;particle(xx,yy,-Math.cos(a)*race.speed[i]*.15+(fxRandom.nextDouble()-.5)*24,-Math.sin(a)*race.speed[i]*.15+(fxRandom.nextDouble()-.5)*24,.45+fxRandom.nextDouble()*.4,2+fxRandom.nextDouble()*2,0);}}}
    }
    void drawParticles(int kind){for(Particle q:particles)if(q.life>0&&q.kind==kind){float t=(float)(q.life/q.total),x=(float)q.x,y=(float)q.y;if(kind==0){float r=(float)(q.r+(1-t)*9);c.save();c.translate(x,y);c.scale(r,r*.8f);fill(PAPER);p.setShader(dustMaterial);p.setAlpha((int)(t*230));c.drawCircle(0,0,1,p);p.setShader(null);p.setAlpha(255);c.restore();}else if(kind==2){c.save();c.rotate((float)(q.vx*3+q.life*180),x,y);rect(x-(float)q.r,y-(float)q.r,x+(float)q.r,y+(float)q.r,0,((int)(t*255)<<24)|0xa78b65,0);c.restore();}else line(x,y,x-(float)q.vx*.025f,y-(float)q.vy*.025f,((int)(t*255)<<24)|0xffd66e,1.5f);}}
    private static final int INK=ArcadeTheme.INK,PAPER=ArcadeTheme.PAPER,RED=ArcadeTheme.RED,BLUE=ArcadeTheme.BLUE,YELLOW=ArcadeTheme.YELLOW;
    MiniGameView(Context context,GameCatalog.Id id,GameAudio audio,Host host){
        super(context);this.id=id;this.audio=audio;this.host=host;
        board=id==GameCatalog.Id.TIC_TAC_TOE?new BoardMatch(3,3,0):id==GameCatalog.Id.GOMOKU?new BoardMatch(15,5,0):null;
        ball=id==GameCatalog.Id.PING_PONG?new BallDuel(BallDuel.Mode.PING_PONG):id==GameCatalog.Id.AIR_HOCKEY?new BallDuel(BallDuel.Mode.HOCKEY):id==GameCatalog.Id.PINBALL?new BallDuel(BallDuel.Mode.PINBALL):null;
        race=id==GameCatalog.Id.RACING?new RaceMatch():null;pool=id==GameCatalog.Id.POOL?new PoolMatch(0):null;
        tank=id==GameCatalog.Id.TANK?new TankMatch():null;
        android.content.SharedPreferences prefs=context.getSharedPreferences("game-options",0);String key=id.name()+".";
        options.target=prefs.getInt(key+"target",tank!=null?3:5);options.map=prefs.getInt(key+"map",0);options.guide=prefs.getInt(key+"guide",1);options.laps=prefs.getInt(key+"laps",2);options.health=prefs.getInt(key+"health",3);
        options.speed=prefs.getFloat(key+"speed",1);options.size=prefs.getFloat(key+"size",1);options.steering=prefs.getFloat(key+"steering",1);options.rotateMaps=prefs.getBoolean(key+"rotate",true);
        options.cover=Math.max(0,Math.min(3,prefs.getInt(key+"cover",2)));options.ramp=Math.max(0,Math.min(2,prefs.getInt(key+"ramp",1)));options.first=prefs.getInt(key+"first",0);options.ricochets=prefs.getInt(key+"ricochets",1);
        setSoundEffectsEnabled(false);setContentDescription(GameCatalog.find(id).name+"，本地双人对战");
        setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener(){
            @Override public WindowInsets onApplyWindowInsets(View v,WindowInsets insets){
                setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
            }
        });reset(false);
    }
    void reset(boolean alternate){
        if(alternate)starter=1-starter;
        else if(board!=null)starter=options.first==2?(random.nextBoolean()?0:1):options.first;
        if(pool!=null)starter=random.nextBoolean()?0:1;
        if(alternate&&options.rotateMaps&&(race!=null||tank!=null))options.map=(options.map+1+random.nextInt(2))%3;
        applyOptions();
        phase=Phase.READY;ready[0]=ready[1]=false;countdown=3;seconds=resultAge=resumeCountdown=0;winner=-1;
        candidateX=candidateY=-1;powerPointer=aimPointer=-1;aimX=200;aimY=250;power=.55;
        countSound=-1;lastFrame=0;accumulator=0;clearInput();
        dustClock=0;for(Particle q:particles)q.life=0;
        if(board!=null)board.reset(starter);if(ball!=null)ball.reset(starter);if(race!=null)race.reset();if(pool!=null)pool.reset(starter);if(tank!=null)tank.reset();invalidate();
    }
    void applyOptions(){
        if(ball!=null){ball.ramp=options.ramp;ball.target=options.target;ball.pace=options.speed;ball.size=options.size;ball.map=options.map;ball.rotateMaps=options.rotateMaps;}
        if(race!=null){race.map=options.map;race.lapsToWin=options.laps;race.pace=options.speed;race.steering=options.steering;race.grid=starter;}
        if(pool!=null)pool.pace=options.speed;
        if(tank!=null){tank.destructible=options.cover!=0;tank.coverHits=options.cover+1;tank.map=options.map;tank.target=options.target;tank.maxHp=options.health;tank.pace=options.speed;tank.rotateMaps=options.rotateMaps;tank.ricochets=options.ricochets;}
    }
    void saveOptions(){String key=id.name()+".";getContext().getSharedPreferences("game-options",0).edit()
        .putInt(key+"target",options.target).putInt(key+"map",options.map).putInt(key+"guide",options.guide).putInt(key+"laps",options.laps).putInt(key+"health",options.health)
        .putFloat(key+"speed",(float)options.speed).putFloat(key+"size",(float)options.size).putFloat(key+"steering",(float)options.steering).putBoolean(key+"rotate",options.rotateMaps).putInt(key+"cover",options.cover).putInt(key+"ramp",options.ramp).putInt(key+"first",options.first).putInt(key+"ricochets",options.ricochets).apply();}
    void setPaused(boolean value){if(paused&&!value&&phase==Phase.PLAYING)resumeCountdown=1.5;paused=value;lastFrame=0;accumulator=0;clearInput();invalidate();}
    void clearInput(){audio.engine(0);owners.clear();channels.clear();powerPointer=aimPointer=-1;if(ball!=null)ball.release();if(race!=null)race.release();if(tank!=null)tank.release();}
    void undoMove(){if(board!=null&&board.undo()){candidateX=candidateY=-1;audio.action(id,false);invalidate();}}
    boolean canUndo(){return board!=null&&board.moves>0&&phase==Phase.PLAYING;}
    void finish(int player){if(phase!=Phase.PLAYING)return;winner=player;phase=Phase.RESULT;resultAge=0;clearInput();audio.engine(0);audio.win();host.finished(player);}
    private void step(double dt){
        if(resumeCountdown>0){resumeCountdown=Math.max(0,resumeCountdown-dt);return;}
        if(phase==Phase.COUNTDOWN){
            countdown-=dt;int number=(int)Math.ceil(countdown);
            if(number>0&&number!=countSound){countSound=number;audio.tick(false);}
            if(countdown<=0){phase=Phase.PLAYING;audio.tick(true);}
        }else if(phase==Phase.PLAYING){
            seconds+=dt;
            effects(dt);
            int events=0;
            if(ball!=null){ball.update(dt);events=ball.drainEvents();if(ball.winner>=0)finish(ball.winner);}
            if(race!=null){race.update(dt);events=race.events;race.events=0;if(race.winner>=0)finish(race.winner);}
            if(pool!=null){pool.update(dt);events=pool.events;pool.events=0;if(pool.winner>=0)finish(pool.winner);}
            if(tank!=null){tank.update(dt);events=tank.events;tank.events=0;if(tank.winner>=0)finish(tank.winner);}
            if(race!=null)audio.engine(phase==Phase.PLAYING?Math.max(race.speed[0],race.speed[1])/185:0);
            long now=SystemClock.uptimeMillis();
            if((events&96)!=0){audio.cover((events&64)!=0);for(int i=0;i<((events&64)!=0?12:4);i++){double a=fxRandom.nextDouble()*Math.PI*2,v=20+fxRandom.nextDouble()*45;particle(tank.coverX,tank.coverY,Math.cos(a)*v,Math.sin(a)*v,.2+fxRandom.nextDouble()*.2,1+fxRandom.nextDouble()*2,2);}}
            if((events&16)!=0){audio.clash();for(int i=0;i<9;i++){double a=fxRandom.nextDouble()*Math.PI*2,v=25+fxRandom.nextDouble()*65;particle(tank.clashX,tank.clashY,Math.cos(a)*v,Math.sin(a)*v,.10+fxRandom.nextDouble()*.11,1,1);}}
            if((events&4)!=0)audio.action(id,false);
            if((events&2)!=0&&phase!=Phase.RESULT)audio.action(id,true);
            else if((events&1)!=0&&now-lastImpact>65){lastImpact=now;audio.action(id,false,ball!=null?ball.hitStrength:pool!=null?pool.hitStrength:.8);}
        }else if(phase==Phase.RESULT)resultAge+=dt;
    }
    @Override protected void onDraw(Canvas canvas){
        long now=SystemClock.uptimeMillis();
        if(lastFrame>0&&!paused){accumulator+=Math.min(.1,(now-lastFrame)/1000d);while(accumulator>=1d/120){step(1d/120);accumulator-=1d/120;}}
        lastFrame=now;c=canvas;
        float width=getWidth()-getPaddingLeft()-getPaddingRight(),height=getHeight()-getPaddingTop()-getPaddingBottom();
        scale=Math.min(width/400f,height/800f);if(scale<=0)return;
        ox=getPaddingLeft()+(width-400*scale)/2;oy=getPaddingTop()+(height-800*scale)/2;
        c.drawColor(ArcadeTheme.BACKGROUND);c.save();c.translate(ox,oy);c.scale(scale,scale);
        drawPlayerBackdrop();
        if(board!=null){c.save();c.scale(.84f,1,200,400);drawBoard();c.restore();}else if(pool!=null)drawPool();
        else if(ball!=null)drawBall();else if(race!=null)drawRace();else if(tank!=null)drawTank();
        if(phase!=Phase.RESULT)drawHeader();
        drawSideData();
        if(resumeCountdown>0&&phase==Phase.PLAYING){
            for(int side=0;side<2;side++){c.save();face(side);rect(77,365,323,397,9,PAPER,2);text("重新按好 · "+String.format(Locale.ROOT,"%.1f",resumeCountdown),200,387,16,INK);c.restore();}
        }
        if(phase==Phase.READY)drawReady();else if(phase==Phase.COUNTDOWN)drawCountdown();else if(phase==Phase.RESULT)drawResult();
        c.restore();if(isAttachedToWindow()&&!paused)postInvalidateOnAnimation();
    }
    void fill(int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setShader(null);}
    void stroke(int color,float width){p.setStyle(Paint.Style.STROKE);p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    void rect(float l,float t,float r,float b,float radius,int color,float outline){fill(color);c.drawRoundRect(l,t,r,b,radius,radius,p);if(outline>0){stroke(INK,outline);c.drawRoundRect(l,t,r,b,radius,radius,p);}}
    void circle(float x,float y,float radius,int color,float outline){fill(color);c.drawCircle(x,y,radius,p);if(outline>0){stroke(INK,outline);c.drawCircle(x,y,radius,p);}}
    void line(float x,float y,float x2,float y2,int color,float width){stroke(color,width);c.drawLine(x,y,x2,y2,p);}
    void text(String s,float x,float y,float size,int color){fill(color);p.setSubpixelText(true);p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);c.drawText(s,x,y,p);}
    void button(String s,float l,float t,float r,float b,int color){rect(l,t+7,r,b+7,17,INK,0);rect(l,t,r,b,17,color,4.5f);float size=20;p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextSize(size);while(size>12&&p.measureText(s)>r-l-24)p.setTextSize(--size);text(s,(l+r)/2,(t+b)/2+size*.34f,size,INK);}
    void drawPlayerBackdrop(){for(int side=0;side<2;side++){c.save();face(side);fill(side==0?RED:BLUE);c.drawRect(0,660,400,800,p);fill(0x1effffff);path.reset();path.moveTo(0,660);path.lineTo(215,660);path.lineTo(100,800);path.lineTo(0,800);path.close();c.drawPath(path,p);line(0,660,400,660,INK,4);c.restore();}}
    float stickRadius(){return race!=null?66:57;}
    static float stickY(int side){return side==0?72:728;}
    private String who(int side){return side==0?"红方":"蓝方";}
    private void drawHeader(){
        for(int i=0;i<4;i++){float y=250+i*64,x=381;rect(366,y+4,399,y+45,9,INK,0);rect(365,y,398,y+41,9,i==2?YELLOW:PAPER,2.5f);
            if(i==0){line(x-8,y+20,x,y+12,INK,3);line(x,y+12,x+8,y+20,INK,3);rect(x-6,y+20,x+6,y+30,1,INK,0);rect(x-2,y+23,x+2,y+30,0,PAPER,0);}
            else if(i==2){for(int k=0;k<3;k++){float xx=x-8+k*8;line(xx,y+11,xx,y+31,INK,2);circle(xx,y+15+k*5,3,PAPER,2);}}
            else{rect(x-9,y+17,x-4,y+25,1,INK,0);path.reset();path.moveTo(x-4,y+17);path.lineTo(x+2,y+12);path.lineTo(x+2,y+30);path.lineTo(x-4,y+25);path.close();fill(INK);c.drawPath(path,p);if(i==1){stroke(INK,2);c.drawArc(x-2,y+13,x+10,y+29,-65,130,false,p);}else if(!audio.enabled){line(x+5,y+16,x+11,y+26,RED,3);line(x+11,y+16,x+5,y+26,RED,3);}else{circle(x+8,y+21,3,BLUE,0);}}
        }
    }
    private String playerData(int side){
        if(ball!=null)return ""+ball.score[side];
        if(race!=null)return ""+race.laps(side);
        if(tank!=null)return ""+tank.score[side];
        if(pool!=null)return ""+pool.score[side];
        return id==GameCatalog.Id.GOMOKU?(side==starter?"黑":"白"):(side==0?"○":"×");
    }
    private void drawSideData(){
        for(int side=0;side<2;side++){
            float cy=side==0?325:475;c.save();if(side==0)c.rotate(180,17,cy);
            rect(3,cy-69,34,cy+72,11,INK,0);rect(1,cy-74,32,cy+67,11,side==0?RED:BLUE,2.5f);
            text(side==0?"红":"蓝",16,cy-43,17,INK);text(playerData(side),16,cy-8,29,INK);
            String foot=ball!=null?"/"+ball.target:race!=null?"/"+race.lapsToWin:tank!=null?"/"+tank.target:pool!=null?"/5":"";
            text(foot,16,cy+16,13,INK);
            if(pool!=null){int group=pool.group[side];if(group>=0){circle(16,cy+35,7,group==0?RED:BLUE,2);text(group==0?"红球":"蓝球",16,cy+57,10,INK);}else text("待分",16,cy+43,11,INK);}
            else if(tank!=null){for(int hp=0;hp<tank.maxHp;hp++)circle(7+hp*8,cy+43,3.5f,hp<tank.hp[side]?PAPER:INK,0);}
            else if((board!=null&&board.turn==side)||(pool!=null&&pool.turn==side))circle(16,cy+42,6,YELLOW,2);
            if(pool!=null&&pool.turn==side)circle(26,cy-59,3.5f,YELLOW,1);
            c.restore();
        }
    }
    private void face(int side){if(side==0)c.rotate(180,200,400);}
    private void drawBoardControls(){
        c.save();face(board.turn);
        if(board.size==15)button(candidateX<0?"点棋盘选择落点":who(board.turn)+"确认 · "+(candidateY+1)+"行"+(candidateX+1)+"列",32,662,368,719,candidateX<0?0xffe4d8b9:YELLOW);
        else text("点击空格落子 · 三子连线获胜",200,699,15,INK);
        button(board.moves>0?"双方同意后悔棋":"落子后可悔棋",55,739,345,781,board.moves>0?PAPER:0xffe4d8b9);
        text(who(board.turn)+"落子 · 第 "+(board.moves+1)+" 手",200,648,14,INK);
        c.restore();
    }
    private void drawReady(){
        fill(0x66202229);c.drawRect(50,108,350,692,p);
        for(int side=0;side<2;side++){
            c.save();if(side==0)c.rotate(180,200,400);
            button(ready[side]?"已准备，等对手":"蓝方 · 点击准备",52,507,348,577,ready[side]?YELLOW:BLUE);
            if(side==0){rect(52,507,348,577,13,ready[side]?YELLOW:RED,3);text(ready[side]?"已准备，等对手":"红方 · 点击准备",200,549,20,INK);}
            text("一人一边 · 双方准备后开始",200,609,13,PAPER);c.restore();
        }
    }
    private void drawCountdown(){circle(200,400,55,PAPER,6);text(""+Math.max(1,(int)Math.ceil(countdown)),200,423,68,INK);}
    private void drawBoard(){
        boolean small=board.size==3;float left=small?38:32,top=small?232:226,step=small?108:24;
        rect(21,165,379,632,19,0xffdfad65,5);rect(25,161,375,625,19,0xfff5d69c,5);
        if(small){
            for(int i=1;i<3;i++){line(left+i*step,top,left+i*step,top+324,INK,5);line(left,top+i*step,left+324,top+i*step,INK,5);}
        }else{
            for(int i=0;i<15;i++){line(left+i*step,top,left+i*step,top+336,0xff9b703c,1.3f);line(left,top+i*step,left+336,top+i*step,0xff9b703c,1.3f);}
            int[] stars={3,7,11};for(int y:stars)for(int x:stars)circle(left+x*step,top+y*step,2.5f,INK,0);
        }
        for(int y=0;y<board.size;y++)for(int x=0;x<board.size;x++)if(board.cells[y][x]>=0){
            float xx=left+(x+(small?.5f:0))*step,yy=top+(y+(small?.5f:0))*step;
            if(small){
                if(board.cells[y][x]==0)circle(xx,yy,29,RED,5);
                else{line(xx-23,yy-23,xx+23,yy+23,BLUE,11);line(xx+23,yy-23,xx-23,yy+23,BLUE,11);line(xx-23,yy-23,xx+23,yy+23,INK,3);line(xx+23,yy-23,xx-23,yy+23,INK,3);}
            }else{
                circle(xx+1,yy+2,10.5f,0x33202229,0);circle(xx,yy,10.3f,board.cells[y][x]==starter?INK:PAPER,1.5f);
                circle(xx-3,yy-3,2,board.cells[y][x]==starter?0xff77777d:0xffffffff,0);
            }
        }
        if(board.moves>0){int x=board.hx[board.moves-1],y=board.hy[board.moves-1];float xx=left+(x+(small?.5f:0))*step,yy=top+(y+(small?.5f:0))*step;
            circle(xx,yy,small?5:3,YELLOW,small?2:0);}
        if(!small&&candidateX>=0&&board.cells[candidateY][candidateX]<0){
            float xx=left+candidateX*step,yy=top+candidateY*step;
            circle(xx,yy,10,board.turn==starter?0x99202229:0xbbfff9e5,1.5f);stroke(RED,2);c.drawCircle(xx,yy,13,p);
            text((candidateY+1)+" 行 · "+(candidateX+1)+" 列",200,601,13,INK);
        }else text(small?"连成三子获胜":"自由规则 · 连成五子获胜 · 不设禁手",200,601,13,INK);
        drawBoardControls();
    }
    private void drawBall(){
        int field=ball.mode==BallDuel.Mode.PING_PONG?0xff45ae62:ball.mode==BallDuel.Mode.HOCKEY?0xffedf7f4:0xff55507f;
        rect(49,106,351,696,18,INK,0);rect(52,102,348,692,16,field,4);
        line(54,400,346,400,ball.mode==BallDuel.Mode.HOCKEY?RED:PAPER,2.5f);
        if(ball.mode==BallDuel.Mode.PING_PONG){
            line(54,396,346,396,INK,5);
            for(int i=0;i<2;i++){float xx=(float)ball.px[i],yy=(float)ball.py[i];rect(xx-50,yy-9,xx+50,yy+9,8,i==0?RED:BLUE,4);}
        }else{
            float gap=ball.mode==BallDuel.Mode.PINBALL?88:72;
            line(52,108,200-gap,108,INK,8);line(200+gap,108,348,108,INK,8);line(52,692,200-gap,692,INK,8);line(200+gap,692,348,692,INK,8);
            line(200-gap,105,200+gap,105,RED,5);line(200-gap,695,200+gap,695,BLUE,5);
            if(ball.mode==BallDuel.Mode.HOCKEY){stroke(0xffabcdd5,3);c.drawCircle(200,400,53,p);for(int i=0;i<2;i++){float x=(float)ball.px[i],y=(float)ball.py[i],r=(float)ball.malletRadius();circle(x+1,y+4,r,0x33202229,0);circle(x,y,r,i==0?RED:BLUE,4);circle(x,y,r*.48f,PAPER,3);}}
            else{
                int rail=0;for(double[] base:BallDuel.OBSTACLES[ball.map]){path.reset();double sign=rail++%2==0?1:-1;for(int k=0;k<=48;k++){double a=k*Math.PI/24,rx=ball.map==0?28:8,ry=ball.map==0?0:8;float xx=(float)(base[0]+sign*rx*Math.sin(a)),yy=(float)(base[1]+sign*(ball.map==1?ry*(1-Math.cos(a)):ry*Math.sin(2*a)));if(k==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}stroke(0x55fff9e5,2);c.drawPath(path,p);}
                for(double[] obstacle:ball.obstacles()){circle((float)obstacle[0]+1,(float)obstacle[1]+4,(float)obstacle[2],0x44202229,0);circle((float)obstacle[0],(float)obstacle[1],(float)obstacle[2],YELLOW,4);circle((float)obstacle[0],(float)obstacle[1],8,PAPER,3);}
                for(double[] r:ball.rotors()){float dx=(float)(Math.cos(r[2])*r[3]),dy=(float)(Math.sin(r[2])*r[3]),xx=(float)r[0],yy=(float)r[1];line(xx-dx,yy-dy+3,xx+dx,yy+dy+3,0x55202229,19);line(xx-dx,yy-dy,xx+dx,yy+dy,INK,18);line(xx-dx,yy-dy,xx+dx,yy+dy,YELLOW,12);circle(xx,yy,7,PAPER,2);line(xx-3,yy,xx+3,yy,INK,2);}
                for(int side=0;side<2;side++)for(int arm=0;arm<2;arm++){float ax=arm==0?64:336,ay=side==0?180:620,bx=arm==0?156:244,by=(float)ball.tipY(side,arm);line(ax,ay,bx,by,INK,25);line(ax,ay,bx,by,side==0?RED:BLUE,17);circle(ax,ay,6,PAPER,2);}
            }
        }
        if(ball.mode==BallDuel.Mode.PING_PONG&&ball.precisionAge>0){c.save();face(ball.precisionSide);rect(143,600,257,628,9,PAPER,2);text("精准回击",200,620,14,INK);c.restore();}
        if(ball.hitAge>0){stroke(0x88fff9e5,2);c.drawCircle((float)ball.hitX,(float)ball.hitY,(float)(4+(1-ball.hitAge/.14)*9*ball.hitStrength),p);}
        if(ball.serve<=0)line((float)(ball.x-ball.vx*.025),(float)(ball.y-ball.vy*.025),(float)ball.x,(float)ball.y,0x66ffffff,10);
        circle((float)ball.x+1,(float)ball.y+3,(float)ball.radius(),0x33202229,0);circle((float)ball.x,(float)ball.y,(float)ball.radius(),ball.mode==BallDuel.Mode.HOCKEY?INK:YELLOW,3);
        for(int side=0;side<2;side++){c.save();face(side);
            if(ball.mode==BallDuel.Mode.PINBALL){button("左挡板",64,723,188,777,side==0?RED:BLUE);button("右挡板",212,723,336,777,side==0?RED:BLUE);text(BallDuel.MAP_NAMES[ball.map]+" · 看准时机按",200,713,12,INK);}
            else{text(side==0?"红方 · 拖动接球":"蓝方 · 拖动接球",200,744,17,INK);text(ball.mode==BallDuel.Mode.PING_PONG?"对拉 "+ball.rallyHits+" 拍 · "+(ball.ramp==0?"经典":ball.ramp==1?"渐进加速":"快速升温"):"先得 "+ball.target+" 分 · 右侧可调",200,773,12,INK);}
            c.restore();}
    }
    private void drawStick(int side,int stick,float x,float y,String label){
        double dx=0,dy=0;if(race!=null){dx=race.inputX[side];dy=race.inputY[side];}else{dx=stick==0?tank.moveX[side]:tank.aimX[side];dy=stick==0?tank.moveY[side]:tank.aimY[side];}
        float r=stickRadius(),travel=r-25.5f;circle(x,y+(side==0?-5:5),r,INK,0);circle(x,y,r,PAPER,4.5f);circle(x,y,race!=null?51:43.5f,0xffe7dab6,1.5f);circle(x+(float)dx*travel,y+(float)dy*travel,25.5f,side==0?RED:BLUE,4.5f);
        
    }
    private void drawRace(){
        c.save();c.scale(.9f,.9f,200,400);
        rect(50,112,350,688,18,race.map==2?0xffdfbd75:0xff8dca66,4);
        path.reset();double[][] points=race.track();path.moveTo((float)points[0][0],(float)points[0][1]);for(int i=1;i<points.length;i++)path.lineTo((float)points[i][0],(float)points[i][1]);path.close();stroke(INK,72);c.drawPath(path,p);stroke(PAPER,64);c.drawPath(path,p);stroke(0xff4a4c57,57);c.drawPath(path,p);stroke(YELLOW,2);p.setPathEffect(new DashPathEffect(new float[]{10,12},0));c.drawPath(path,p);p.setPathEffect(null);
        c.save();c.clipRect(52,114,348,686);drawParticles(0);c.restore();
        for(int row=0;row<2;row++)for(int col=0;col<6;col++)rect((float)points[0][0]-24+col*8,(float)points[0][1]+row*8,(float)points[0][0]-16+col*8,(float)points[0][1]+8+row*8,0,(row+col)%2==0?PAPER:INK,0);
        for(int i=0;i<2;i++){double[] goal=points[race.next[i]];stroke(i==0?RED:BLUE,2);c.drawCircle((float)goal[0],(float)goal[1],18+i*4,p);
            c.save();c.translate((float)race.x[i],(float)race.y[i]);c.rotate((float)Math.toDegrees(race.heading[i])+90);rect(-14,-16,14,17,5,INK,0);rect(-10,-20,10,21,5,i==0?RED:BLUE,3);rect(-7,-5,7,7,3,INK,1);line(-13,-18,13,-18,PAPER,3);c.restore();}
        text(RaceMatch.MAP_NAMES[race.map],200,392,16,INK);text("依次穿过圆环",200,417,12,INK);
        c.restore();drawStick(0,0,200,stickY(0),"方向");drawStick(1,0,200,stickY(1),"方向");for(int side=0;side<2;side++){c.save();face(side);text("方向",320,735,19,INK);c.restore();}
    }
    private void drawTank(){
        c.save();c.scale(.9f,.9f,200,400);
        rect(50,113,350,687,16,0xffd7c48f,4);for(int y=130;y<680;y+=30)line(56,y,344,y,0x18916e3d,1);
        tank.walls();for(int index=0;index<TankMatch.MAPS[tank.map].length;index++){if(tank.wallHp[index]<=0)continue;double[] wall=TankMatch.MAPS[tank.map][index];float l=(float)wall[0],t=(float)wall[1],r=(float)wall[2],b=(float)wall[3],cx=(l+r)/2,cy=(t+b)/2;rect(l,t+4,r,b+4,5,INK,0);rect(l,t,r,b,5,tank.wallFlash[index]>0?0xffdfb86e:0xffa78b65,3);
            if(tank.destructible&&tank.wallHp[index]<tank.coverHits){c.save();c.clipRect(l+3,t+3,r-3,b-3);line(cx-7,t+3,cx+3,cy-2,INK,2);line(cx+3,cy-2,cx-4,cy+5,INK,2);line(cx-4,cy+5,cx+6,b-3,INK,2);if(tank.wallHp[index]==1){line(l+3,cy-5,cx+3,cy-2,INK,2);line(cx-4,cy+5,r-3,cy+9,INK,2);}c.restore();}}
        drawParticles(2);
        drawParticles(1);
        for(TankMatch.Shell shell:tank.shells){line((float)(shell.x-shell.vx*.03),(float)(shell.y-shell.vy*.03),(float)shell.x,(float)shell.y,0x55fff9e5,5);circle((float)shell.x,(float)shell.y,6,shell.owner==0?RED:BLUE,1.5f);}
        for(int side=0;side<2;side++){
            if(tank.shield[side]>0){stroke(0x88fff9e5,2);c.drawCircle((float)tank.x[side],(float)tank.y[side],24,p);}
            c.save();c.translate((float)tank.x[side],(float)tank.y[side]);c.rotate((float)Math.toDegrees(tank.heading[side])+90);
            rect(-16,-19,-8,19,3,INK,1);rect(8,-19,16,19,3,INK,1);for(int j=0;j<5;j++){float yl=-18+j*8+(float)((tank.leftTread[side]%6+6)%6),yr=-18+j*8+(float)((tank.rightTread[side]%6+6)%6);if(yl<=18)line(-15,yl,-9,yl,PAPER,1);if(yr<=18)line(9,yr,15,yr,PAPER,1);}rect(-11,-17,11,17,4,side==0?RED:BLUE,3);c.restore();
            float x=(float)tank.x[side],y=(float)tank.y[side];line(x,y,x+(float)Math.cos(tank.turret[side])*25,y+(float)Math.sin(tank.turret[side])*25,INK,9);line(x,y,x+(float)Math.cos(tank.turret[side])*25,y+(float)Math.sin(tank.turret[side])*25,side==0?RED:BLUE,5);circle(x,y,9,side==0?RED:BLUE,3);
        }
        c.restore();for(int side=0;side<2;side++){drawStick(side,0,side==0?296:104,stickY(side),"移动");drawStick(side,1,side==0?108:292,stickY(side),"瞄准开火");c.save();face(side);rect(170,689,230,721,9,PAPER,2);text("← 移动",200,710,12,INK);rect(170,735,230,767,9,PAPER,2);text("开火 →",200,756,12,INK);c.restore();}
        if(tank.roundPause>0)for(int side=0;side<2;side++){c.save();face(side);rect(80,405,320,451,10,PAPER,2);text("下一回合 · 更换掩体",200,424,14,INK);text(TankMatch.MAP_NAMES[tank.map],200,444,12,INK);c.restore();}
    }
    private void drawPool(){
        rect(34,135,366,665,19,0xff9a623e,6);rect(49,159,351,641,8,0xff31985a,4);
        for(int y=178;y<635;y+=16)line(57,y,343,y,0x09333322,1);
        for(int side=0;side<2;side++)for(int row=0;row<3;row++)circle(side==0?50:350,160+row*240,20,INK,3);
        PoolMatch.Ball cue=pool.balls[0];
        if(!pool.moving&&!pool.ballInHand&&phase==Phase.PLAYING){
            double dx=aimX-cue.x,dy=aimY-cue.y,d=Math.max(1,Math.hypot(dx,dy));
            float nx=(float)(dx/d),ny=(float)(dy/d),cx=(float)cue.x,cy=(float)cue.y;
            double[] preview=pool.preview(dx,dy);float hitX=(float)preview[0],hitY=(float)preview[1];
            c.save();c.clipRect(49,159,351,641);stroke(0xccfff9e5,2);p.setPathEffect(new DashPathEffect(new float[]{6,7},(float)(-seconds*13%13)));c.drawLine(cx,cy,hitX,hitY,p);p.setPathEffect(null);
            stroke(0xaafff9e5,1.5f);c.drawCircle(hitX,hitY,10,p);
            if(preview[2]>=0&&options.guide>0){PoolMatch.Ball target=pool.balls[(int)preview[2]];double qx=(target.x-hitX)/20,qy=(target.y-hitY)/20,dot=nx*qx+ny*qy,rx=nx-dot*qx,ry=ny-dot*qy;line(hitX,hitY,hitX+(float)rx*65,hitY+(float)ry*65,0x8800d8ef,1.5f);circle((float)(hitX+qx*10),(float)(hitY+qy*10),2.5f,YELLOW,0);}

            if(preview[2]>=0&&options.guide>0){PoolMatch.Ball target=pool.balls[(int)preview[2]];double transfer=Math.max(.12,(nx*(target.x-hitX)+ny*(target.y-hitY))/20);double[][] route=pool.targetPath((int)preview[2],hitX,hitY,(options.guide==1?155:230)*transfer,1);for(int i=1;i<route.length;i++){stroke(i==1?0xccffdf72:0x66ffdf72,2);p.setPathEffect(new DashPathEffect(new float[]{5,8},0));c.drawLine((float)route[i-1][0],(float)route[i-1][1],(float)route[i][0],(float)route[i][1],p);p.setPathEffect(null);}}
            c.restore();
            line(cx-nx*18,cy-ny*18,cx-nx*(52+(float)power*30),cy-ny*(52+(float)power*30),INK,7);
            line(cx-nx*18,cy-ny*18,cx-nx*(52+(float)power*30),cy-ny*(52+(float)power*30),0xfff9dba4,4);
        }
        for(PoolMatch.Ball b:pool.balls)if(!b.sunk){circle((float)b.x+1,(float)b.y+3,10,0x33202229,0);circle((float)b.x,(float)b.y,10,b.owner<0?PAPER:b.owner==0?RED:BLUE,2.3f);circle((float)b.x-3,(float)b.y-3,2,0xaaffffff,0);}
        if(pool.ballInHand&&pool.placementSelected){float x=(float)pool.placementX,y=(float)pool.placementY;boolean legal=pool.canPlaceCue(x,y);circle(x,y,10,0xccfff9e5,2);stroke(legal?0xff247b48:RED,2);p.setPathEffect(new DashPathEffect(new float[]{4,4},0));c.drawCircle(x,y,15,p);p.setPathEffect(null);if(!legal){line(x-8,y-8,x+8,y+8,RED,3);line(x+8,y-8,x-8,y+8,RED,3);}}
        if(pool.hitAge>0){stroke(0x88fff9e5,1.5f);c.drawCircle((float)pool.hitX,(float)pool.hitY,(float)(3+(1-pool.hitAge/.12)*8*pool.hitStrength),p);}
        c.save();face(pool.turn);
        if(pool.ballInHand){boolean legal=pool.placementSelected&&pool.canPlaceCue(pool.placementX,pool.placementY);text(who(pool.turn)+"自由球 · 点选或拖动白球位置",200,679,14,INK);text(!pool.placementSelected?"可摆整个球桌，避开球与袋口":legal?"位置可用 · 确认后再瞄准击球":"位置不合法 · 请避开球、袋口与边界",200,710,12,INK);button(legal?"确认摆球":"先选合法位置",65,746,335,786,legal?(pool.turn==0?RED:BLUE):PAPER);c.restore();return;}
        text(pool.moving?"球停稳后结算本杆":pool.notice,200,676,13,INK);
        line(57,698,343,698,INK,13);line(57,698,57+(float)power*286,698,YELLOW,8);circle(57+(float)power*286,698,13,PAPER,3);
        text("力度 "+Math.round(power*100)+"%",200,730,12,INK);
        button(pool.moving?"球在滚动…":who(pool.turn)+" · 击球",65,746,335,786,pool.moving?0xffe4d8b9:pool.turn==0?RED:BLUE);
        c.restore();
    }
    private void drawResult(){
        fill(0x99202229);c.drawRect(0,0,400,800,p);
        for(int i=0;i<12;i++){float x=29+(i*71)%335,y=170+(i*53)%510;c.save();c.rotate(i*19,x,y);rect(x,y,x+6,y+12,1,i%2==0?RED:BLUE,0);c.restore();}
        String detail=board!=null?(winner==2?"棋盘已满，再较量一局吧":id==GameCatalog.Id.GOMOKU?"五子连线，妙手！":"三子连线，漂亮！"):
            ball!=null?ball.score[0]+" : "+ball.score[1]:race!=null?"完成 "+race.lapsToWin+" 圈！":tank!=null?"坦克对决 "+tank.score[0]+" : "+tank.score[1]:pool.scratchLoss?"清台时白球落袋":"五个目标球全部入袋";
        for(int side=0;side<2;side++){
            c.save();face(side);rect(36,454,372,782,22,INK,0);rect(28,446,364,774,22,PAPER,5);
            rect(49,466,343,515,12,winner==2?YELLOW:winner==0?RED:BLUE,3);
            text(winner==2?"不分伯仲，平局！":who(winner)+"获胜！",196,500,26,INK);
            text(detail,196,549,18,INK);text(String.format(Locale.ROOT,"用时 %.1f 秒",seconds),196,578,15,INK);
            button("再战一局",58,600,334,654,YELLOW);button("返回大厅",58,681,334,735,PAPER);c.restore();
        }
    }
    private boolean oneOwner(int side){for(int i=0;i<owners.size();i++)if(owners.valueAt(i)==side)return true;return false;}
    private void playBoard(int x,int y){
        if(board.play(x,y)){candidateX=candidateY=-1;audio.action(id,false);performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            if(board.winner>=0)finish(board.winner);else if(board.draw)finish(2);}
    }
    private void boardDown(float x,float y){
        float controlY=board.turn==0?800-y:y;
        if(controlY>=739&&controlY<=786&&x>=55&&x<=345){if(canUndo())host.undo();return;}
        if(board.size==15){
            if(controlY>=662&&controlY<=724&&x>=32&&x<=368){if(candidateX>=0)playBoard(candidateX,candidateY);return;}
            int xx=Math.round((x-32)/24),yy=Math.round((y-226)/24);
            if(x>=20&&x<=380&&y>=214&&y<=574&&xx>=0&&xx<15&&yy>=0&&yy<15&&board.cells[yy][xx]<0){candidateX=xx;candidateY=yy;}
        }else if(x>=38&&x<362&&y>=232&&y<556)playBoard((int)((x-38)/108),(int)((y-232)/108));
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        int action=e.getActionMasked(),index=e.getActionIndex(),pointer=e.getPointerId(index);
        float x=(e.getX(index)-ox)/scale,y=(e.getY(index)-oy)/scale;
        if(action==MotionEvent.ACTION_CANCEL){clearInput();return true;}
        if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_POINTER_UP){
            int side=owners.get(pointer,-1),channel=channels.get(pointer,-1);owners.delete(pointer);channels.delete(pointer);if(race!=null&&side>=0)race.steer(side,0,0);if(tank!=null&&side>=0&&channel>=0)tank.control(side,channel,0,0);
            if(pointer==powerPointer)powerPointer=-1;if(pointer==aimPointer)aimPointer=-1;return true;
        }
        if(paused)return true;
        if(action==MotionEvent.ACTION_MOVE){
            for(int i=0;i<e.getPointerCount();i++){
                int finger=e.getPointerId(i),side=owners.get(finger,-1);float xx=(e.getX(i)-ox)/scale,yy=(e.getY(i)-oy)/scale;
                if(ball!=null&&side>=0&&ball.mode!=BallDuel.Mode.PINBALL)ball.aim(side,xx,yy);
                if(race!=null&&side>=0)race.steer(side,(xx-200)/stickRadius(),(yy-stickY(side))/stickRadius());
                if(tank!=null&&side>=0){int channel=channels.get(finger,-1);if(channel>=0){float cx=side==0?(channel==0?296:108):(channel==0?104:292);tank.control(side,channel,(xx-cx)/stickRadius(),(yy-stickY(side))/stickRadius());}}
                if(board!=null)xx=(xx-200)/.84f+200;
                if(pool!=null&&!pool.moving){if(pool.ballInHand){if(finger==aimPointer)pool.selectCuePlacement(xx,yy);}else{if(finger==powerPointer)power=BallDuel.clamp(((pool.turn==0?400-xx:xx)-57)/286,.05,1);if(finger==aimPointer)setPoolAim(xx,yy);}}
            }return true;
        }
        if(action!=MotionEvent.ACTION_DOWN&&action!=MotionEvent.ACTION_POINTER_DOWN)return true;
        if(x<0||x>400||y<0||y>800)return true;
        if(phase!=Phase.RESULT&&x>=364){if(y>=246&&y<=295)host.menu();else if(y>=310&&y<=359)host.audio();else if(y>=374&&y<=423){if(getContext() instanceof MainActivity)((MainActivity)getContext()).showMiniOptions();}else if(y>=438&&y<=487){audio.enabled=!audio.enabled;if(!audio.enabled)audio.engine(0);if(getContext() instanceof MainActivity)((MainActivity)getContext()).saveMiniSounds();}return true;}
        if(phase==Phase.RESULT){float resultX=y<400?400-x:x,resultY=y<400?800-y:y;if(resultAge>.55){if(resultX>=58&&resultX<=334&&resultY>=600&&resultY<=659){reset(true);performClick();}else if(resultX>=58&&resultX<=334&&resultY>=681&&resultY<=740){host.home();performClick();}}return true;}
        if(x<46)return true;
        int side=y<400?0:1;
        if(phase==Phase.READY){
            float readyY=side==0?800-y:y;
            if(x<52||x>348||readyY<507||readyY>577)return true;
            ready[side]=true;audio.tick(false);if(ready[0]&&ready[1]){phase=Phase.COUNTDOWN;countdown=3;countSound=3;clearInput();}performClick();return true;
        }
        if(phase!=Phase.PLAYING)return true;
        if(board!=null)x=(x-200)/.84f+200;
        if(board!=null){if(resumeCountdown<=0)boardDown(x,y);return true;}
        if(pool!=null){
            if(pool.moving||resumeCountdown>0)return true;
            float controlX=pool.turn==0?400-x:x,controlY=pool.turn==0?800-y:y;
            if(pool.ballInHand){if(controlX>=65&&controlX<=335&&controlY>=746&&controlY<=791){if(pool.confirmCuePlacement()){aimPointer=powerPointer=-1;aimX=200;aimY=pool.balls[0].y>400?250:550;audio.action(id,false);performClick();}}else if(x>=49&&x<=351&&y>=159&&y<=641&&aimPointer<0){aimPointer=pointer;pool.selectCuePlacement(x,y);}return true;}
            if(controlX>=43&&controlX<=357&&controlY>=677&&controlY<=718){if(powerPointer<0){powerPointer=pointer;power=BallDuel.clamp((controlX-57)/286,.05,1);}}
            else if(controlX>=65&&controlX<=335&&controlY>=746&&controlY<=791){PoolMatch.Ball cue=pool.balls[0];if(pool.shoot(aimX-cue.x,aimY-cue.y,power)){powerPointer=aimPointer=-1;lastImpact=SystemClock.uptimeMillis();audio.action(id,false);performClick();}}
            else if(x>=49&&x<=351&&y>=159&&y<=641&&aimPointer<0){aimPointer=pointer;setPoolAim(x,y);}return true;
        }
        if(race!=null||tank!=null){
            float cy=stickY(side);int channel=0;float cx=200;
            if(tank!=null){channel=side==0?(x>200?0:1):(x<200?0:1);cx=side==0?(channel==0?296:108):(channel==0?104:292);}
            if(Math.hypot(x-cx,y-cy)>stickRadius()+8)return true;
            for(int i=0;i<owners.size();i++)if(owners.valueAt(i)==side&&channels.get(owners.keyAt(i),-1)==channel)return true;
            owners.put(pointer,side);channels.put(pointer,channel);
            if(race!=null)race.steer(side,(x-cx)/stickRadius(),(y-cy)/stickRadius());else tank.control(side,channel,(x-cx)/stickRadius(),(y-cy)/stickRadius());return true;
        }
        if(ball!=null&&ball.mode==BallDuel.Mode.PINBALL){int arm=x<200?0:1;owners.put(pointer,side);channels.put(pointer,arm);if(ball.press(side,arm))audio.action(id,false);return true;}
        if(oneOwner(side))return true;owners.put(pointer,side);if(ball!=null)ball.aim(side,x,y);return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    private void setPoolAim(float x,float y){PoolMatch.Ball cue=pool.balls[0];if(Math.hypot(x-cue.x,y-cue.y)>18){aimX=x;aimY=y;}}
}
