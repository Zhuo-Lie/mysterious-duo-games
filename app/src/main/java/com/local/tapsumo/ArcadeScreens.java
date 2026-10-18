package com.local.tapsumo;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.TranslateAnimation;
import android.view.animation.Animation;
import android.widget.ViewFlipper;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Native scrolling, focus, clicks and text sizing around the vector game art. */
final class ArcadeScreens {
    interface Actions {
        void preview(GameCatalog.Id id);
        void start(GameCatalog.Id id);
        void home();
        void audio();
        void score();
    }
    static class Page extends LinearLayout {
        final Context context;
        final Actions actions;
        Page(Context context,Actions actions){
            super(context);this.context=context;this.actions=actions;setOrientation(VERTICAL);
            setBackgroundColor(ArcadeTheme.BACKGROUND);
            setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener(){
                @Override public WindowInsets onApplyWindowInsets(View v,WindowInsets insets){
                    setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
                }
            });
        }
        int dp(float n){return ArcadeTheme.dp(context,n);}
        LinearLayout column(){return ArcadeTheme.column(context);}
        TextView text(String s,float size,boolean bold){return ArcadeTheme.text(context,s,size,bold);}
        Button button(String s,int color){return ArcadeTheme.button(context,s,color);}
        void gap(LinearLayout parent,int height){parent.addView(new View(context),new LinearLayout.LayoutParams(1,dp(height)));}
        LinearLayout toolbar(boolean back){
            LinearLayout bar=new LinearLayout(context);bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setPadding(dp(18),dp(14),dp(18),dp(12));
            if(back){
                Button b=button("‹ 返回",ArcadeTheme.PAPER);bar.addView(b,new LinearLayout.LayoutParams(dp(80),dp(49)));
                b.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){actions.home();}});
                TextView title=text("神秘双人小游戏",16,true);title.setGravity(Gravity.CENTER);
                bar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            } else {
                android.widget.ImageView mascot=new android.widget.ImageView(context);mascot.setImageResource(getResources().getIdentifier("chicken_duel","drawable",context.getPackageName()));mascot.setContentDescription("神秘双人小游戏封面");LinearLayout.LayoutParams iconSize=new LinearLayout.LayoutParams(dp(40),dp(40));iconSize.rightMargin=dp(9);bar.addView(mascot,iconSize);
                LinearLayout names=column();TextView label=text("红方 × 蓝方  /  本地双人",11,true);label.setTextColor(ArcadeTheme.MUTED);
                names.addView(label);gap(names,6);TextView title=text("神秘双人小游戏",22,true);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);names.addView(title);
                bar.addView(names,new LinearLayout.LayoutParams(0,-2,1));
            }
            Button sound=button("♪",ArcadeTheme.PAPER);sound.setTextSize(24);sound.setContentDescription("音效与音量设置");
            bar.addView(sound,new LinearLayout.LayoutParams(dp(49),dp(49)));
            sound.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){actions.audio();}});
            addView(bar);
            LinearLayout stripe=new LinearLayout(context);
            View red=new View(context),blue=new View(context);red.setBackgroundColor(ArcadeTheme.RED);blue.setBackgroundColor(ArcadeTheme.BLUE);
            stripe.addView(red,new LinearLayout.LayoutParams(0,dp(4),1));stripe.addView(blue,new LinearLayout.LayoutParams(0,dp(4),1));addView(stripe);
            return bar;
        }
        LinearLayout scrollBody(){
            ScrollView scroll=new ScrollView(context);scroll.setFillViewport(true);scroll.setClipToPadding(false);
            scroll.setVerticalScrollBarEnabled(false);
            LinearLayout body=column();body.setPadding(dp(18),dp(18),dp(18),dp(22));scroll.addView(body);
            addView(scroll,new LinearLayout.LayoutParams(-1,0,1));return body;
        }
    }
    static final class Lobby extends Page {
        private final LinearLayout grid;
        int recommendation;
        boolean autoRecommend=true;
        TextView heroEyebrow,heroHeadline,heroDetail;
        LinearLayout heroCard;
        HeroPager heroPager;
        final TextView[] pageTitles=new TextView[GameCatalog.ALL.size()],pageDetails=new TextView[GameCatalog.ALL.size()];
        final Runnable recommendationTick=new Runnable(){public void run(){if(isShown()&&autoRecommend){if(!heroPager.touching&&!heroCard.isPressed()&&!heroPager.hasFocus()&&!heroPager.isAccessibilityFocused())rotateRecommendation();postDelayed(this,5000);}}};
        void restartRecommendationTimer(){removeCallbacks(recommendationTick);if(autoRecommend&&isAttachedToWindow())postDelayed(recommendationTick,5000);}
        void rotateRecommendation(){showRecommendation((recommendation+1)%GameCatalog.ALL.size(),1,true);}
        void showRecommendation(int index,int direction,boolean animate){
            recommendation=(index+GameCatalog.ALL.size())%GameCatalog.ALL.size();
            if(animate){TranslateAnimation incoming=new TranslateAnimation(Animation.RELATIVE_TO_SELF,direction,Animation.RELATIVE_TO_SELF,0,Animation.RELATIVE_TO_SELF,0,Animation.RELATIVE_TO_SELF,0),outgoing=new TranslateAnimation(Animation.RELATIVE_TO_SELF,0,Animation.RELATIVE_TO_SELF,-direction,Animation.RELATIVE_TO_SELF,0,Animation.RELATIVE_TO_SELF,0);incoming.setDuration(260);outgoing.setDuration(260);heroPager.setInAnimation(incoming);heroPager.setOutAnimation(outgoing);}else{heroPager.setInAnimation(null);heroPager.setOutAnimation(null);}
            heroPager.setDisplayedChild(recommendation);heroHeadline=pageTitles[recommendation];heroDetail=pageDetails[recommendation];
            GameCatalog.Game g=GameCatalog.ALL.get(recommendation);heroEyebrow.setText("今日擂台  ·  "+(recommendation+1)+" / "+GameCatalog.ALL.size());
            heroPager.setContentDescription(g.name+"，"+g.tagline+"，左右滑动换游戏，点击开局");heroCard.setContentDescription(heroPager.getContentDescription());
        }
        final class HeroPager extends ViewFlipper {
            boolean touching,multi;float downX,downY;final int slop;
            HeroPager(Context c){super(c);slop=ViewConfiguration.get(c).getScaledTouchSlop();setClickable(true);setFocusable(true);setClipChildren(true);setMeasureAllChildren(true);}
            @Override public boolean onTouchEvent(MotionEvent e){int action=e.getActionMasked();
                if(action==MotionEvent.ACTION_DOWN){touching=true;multi=false;downX=e.getX();downY=e.getY();Lobby.this.removeCallbacks(recommendationTick);getParent().requestDisallowInterceptTouchEvent(true);return true;}
                if(action==MotionEvent.ACTION_POINTER_DOWN){multi=true;return true;}
                if(action==MotionEvent.ACTION_MOVE){if(Math.abs(e.getY()-downY)>slop&&Math.abs(e.getY()-downY)>Math.abs(e.getX()-downX))getParent().requestDisallowInterceptTouchEvent(false);return true;}
                if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){float dx=e.getX()-downX,dy=e.getY()-downY;touching=false;getParent().requestDisallowInterceptTouchEvent(false);
                    if(action==MotionEvent.ACTION_UP&&!multi){if(Math.abs(dx)>dp(40)&&Math.abs(dx)>Math.abs(dy)*1.25)showRecommendation(recommendation+(dx<0?1:-1),dx<0?1:-1,true);else if(Math.hypot(dx,dy)<slop)performClick();}
                    restartRecommendationTimer();return true;
                }return true;
            }
            @Override public boolean performClick(){super.performClick();return true;}
            @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(info);info.setScrollable(true);info.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);info.addAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);}
            @Override public boolean performAccessibilityAction(int action,android.os.Bundle args){if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){int direction=action==android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1;showRecommendation(recommendation+direction,direction,true);restartRecommendationTimer();return true;}return super.performAccessibilityAction(action,args);}
        }
        @Override protected void onAttachedToWindow(){super.onAttachedToWindow();removeCallbacks(recommendationTick);postDelayed(recommendationTick,5000);}
        @Override protected void onDetachedFromWindow(){removeCallbacks(recommendationTick);super.onDetachedFromWindow();}
        @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);removeCallbacks(recommendationTick);if(visibility==VISIBLE&&isAttachedToWindow())postDelayed(recommendationTick,5000);}

        private final TextView count;
        private final TextView series;
        private final Button[] tabs=new Button[GameCatalog.Category.values().length];
        private GameCatalog.Category filter=GameCatalog.Category.ALL;
        Lobby(Context c,Actions a){
            super(c,a);toolbar(false);LinearLayout body=scrollBody();
            LinearLayout hero=column();hero.setPadding(dp(10),dp(8),dp(10),dp(14));
            hero.setBackground(ArcadeTheme.buttonFace(c,ArcadeTheme.YELLOW));
            TextView eyebrow=text("今日擂台  ·  相扑  1/9",13,true);eyebrow.setPadding(dp(4),0,0,dp(2));heroEyebrow=eyebrow;heroCard=hero;
            LinearLayout recommendationBar=new LinearLayout(c);recommendationBar.setGravity(Gravity.CENTER_VERTICAL);recommendationBar.addView(eyebrow,new LinearLayout.LayoutParams(0,-2,1));final Button rotation=button("Ⅱ",ArcadeTheme.PAPER);rotation.setTextSize(13);rotation.setContentDescription("暂停推荐轮播");recommendationBar.addView(rotation,new LinearLayout.LayoutParams(dp(32),dp(32)));hero.addView(recommendationBar);
            rotation.setOnClickListener(new View.OnClickListener(){public void onClick(View v){autoRecommend=!autoRecommend;rotation.setText(autoRecommend?"Ⅱ":"▶");rotation.setContentDescription(autoRecommend?"暂停推荐轮播":"继续推荐轮播");removeCallbacks(recommendationTick);if(autoRecommend)postDelayed(recommendationTick,5000);}});
            gap(hero,4);heroPager=new HeroPager(c);
            for(GameCatalog.Game game:GameCatalog.ALL){
                LinearLayout page=new LinearLayout(c);page.setGravity(Gravity.CENTER_VERTICAL);page.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
                GameArtView art=new GameArtView(c,game.id);page.addView(art,new LinearLayout.LayoutParams(0,dp(104),1.05f));
                LinearLayout words=column();TextView title=text(game.name,20,true);title.setSingleLine(true);title.setEllipsize(TextUtils.TruncateAt.END);words.addView(title);gap(words,3);
                TextView detail=text(game.tagline,11,false);detail.setMaxLines(2);detail.setMinLines(2);detail.setEllipsize(TextUtils.TruncateAt.END);words.addView(detail);gap(words,4);
                TextView play=text("开局 ›",15,true);play.setGravity(Gravity.CENTER);play.setBackground(ArcadeTheme.face(c,ArcadeTheme.PAPER,10,3));words.addView(play,new LinearLayout.LayoutParams(dp(78),dp(34)));
                LinearLayout.LayoutParams wordsSize=new LinearLayout.LayoutParams(0,-2,1);wordsSize.leftMargin=dp(10);page.addView(words,wordsSize);
                pageTitles[heroPager.getChildCount()]=title;pageDetails[heroPager.getChildCount()]=detail;heroPager.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-2));
            }
            hero.addView(heroPager,new LinearLayout.LayoutParams(-1,-2));
            Button previous=button("‹",ArcadeTheme.PAPER),next=button("›",ArcadeTheme.PAPER);previous.setContentDescription("上一个推荐游戏");next.setContentDescription("下一个推荐游戏");
            recommendationBar.addView(previous,1,new LinearLayout.LayoutParams(dp(32),dp(32)));recommendationBar.addView(next,2,new LinearLayout.LayoutParams(dp(32),dp(32)));
            previous.setOnClickListener(new View.OnClickListener(){public void onClick(View v){showRecommendation(recommendation-1,-1,true);restartRecommendationTimer();}});next.setOnClickListener(new View.OnClickListener(){public void onClick(View v){rotateRecommendation();restartRecommendationTimer();}});
            View.OnClickListener openRecommendation=new View.OnClickListener(){public void onClick(View v){actions.start(GameCatalog.ALL.get(recommendation).id);}};heroPager.setOnClickListener(openRecommendation);hero.setOnClickListener(openRecommendation);hero.setFocusable(false);
            showRecommendation(0,1,false);
            body.addView(hero);gap(body,16);
            LinearLayout heading=new LinearLayout(c);heading.setGravity(Gravity.CENTER_VERTICAL);
            heading.addView(text("挑一个，来一局",22,true),new LinearLayout.LayoutParams(0,-2,1));
            count=text("9 个游戏",12,true);count.setTextColor(ArcadeTheme.MUTED);heading.addView(count);body.addView(heading);gap(body,12);
            LinearLayout filters=new LinearLayout(c);
            GameCatalog.Category[] categories=GameCatalog.Category.values();
            for(int i=0;i<categories.length;i++){
                final GameCatalog.Category category=categories[i];Button tab=button(category.label,i==0?ArcadeTheme.YELLOW:ArcadeTheme.PAPER);tab.setTextSize(13);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(47),1);if(i>0)lp.leftMargin=dp(7);
                filters.addView(tab,lp);tabs[i]=tab;
                tab.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){filter=category;populate();}});
            }
            body.addView(filters);gap(body,12);grid=column();body.addView(grid);populate();gap(body,4);
            TextView note=text("九款游戏，全部可以开局。\n找个搭子，把胜负留在这里。",12,false);
            note.setTextColor(ArcadeTheme.MUTED);note.setGravity(Gravity.CENTER);note.setLineSpacing(dp(5),1);body.addView(note);
            series=text("红方 0  :  0 蓝方  ·  点击查看比分",13,true);series.setGravity(Gravity.CENTER);
            series.setBackground(ArcadeTheme.buttonFace(c,ArcadeTheme.PAPER));series.setClickable(true);series.setFocusable(true);
            addView(series,new LinearLayout.LayoutParams(-1,dp(49)));
            series.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){actions.score();}});
        }
        void refreshScores(SeriesScore score){series.setText("红方 "+score.wins[0]+"  :  "+score.wins[1]+" 蓝方   ·   "+score.draws+" 平局  ›");series.setContentDescription("查看本地总比分，红方"+score.wins[0]+"胜，蓝方"+score.wins[1]+"胜，平局"+score.draws+"局");}
        private void populate(){
            grid.removeAllViews();int n=0;LinearLayout row=null;
            for(GameCatalog.Game game:GameCatalog.ALL){
                if(filter!=GameCatalog.Category.ALL&&game.category!=filter)continue;
                if(n%2==0){row=new LinearLayout(context);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(12);grid.addView(row,rp);}
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-2,1);if(n%2==1)cp.leftMargin=dp(12);
                row.addView(gameCard(game),cp);n++;
            }
            if(n%2==1){LinearLayout.LayoutParams empty=new LinearLayout.LayoutParams(0,1,1);empty.leftMargin=dp(12);row.addView(new View(context),empty);}
            count.setText(n+" 个游戏");
            for(int i=0;i<tabs.length;i++){
                boolean selected=GameCatalog.Category.values()[i]==filter;
                tabs[i].setBackground(ArcadeTheme.buttonFace(context,selected?ArcadeTheme.YELLOW:ArcadeTheme.PAPER));tabs[i].setSelected(selected);
            }
        }
        private View gameCard(final GameCatalog.Game game){
            LinearLayout card=column();card.setPadding(dp(8),dp(8),dp(8),dp(18));card.setBackground(ArcadeTheme.buttonFace(context,ArcadeTheme.PAPER));
            card.addView(new GameArtView(context,game.id),new LinearLayout.LayoutParams(-1,dp(91)));gap(card,9);
            TextView name=text(game.name,19,true);name.setPadding(dp(3),0,0,0);card.addView(name);gap(card,6);
            TextView tag=text(game.tagline,11,false);tag.setTextColor(ArcadeTheme.MUTED);tag.setSingleLine(true);tag.setEllipsize(TextUtils.TruncateAt.END);tag.setPadding(dp(3),0,0,0);card.addView(tag);gap(card,11);
            TextView status=text(game.playable?"可以对战  ›":"玩法预览  ›",12,true);status.setPadding(dp(7),dp(5),dp(7),dp(5));
            status.setBackground(ArcadeTheme.face(context,game.playable?ArcadeTheme.BLUE:0xffeae0c7,7,0));card.addView(status);
            for(int i=0;i<card.getChildCount();i++)card.getChildAt(i).setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            card.setClickable(true);card.setFocusable(true);card.setContentDescription(game.name+"，"+(game.playable?"可以对战":"玩法预览")+"，"+game.tagline);
            card.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){actions.preview(game.id);}});return card;
        }
    }
    static final class Preview extends Page {
        Preview(Context c,Actions a,final GameCatalog.Game game){
            super(c,a);toolbar(true);LinearLayout body=scrollBody();
            TextView label=text(game.playable?"离线双人 · 现在开场":"正在筹备 · 玩法预览",12,true);label.setTextColor(ArcadeTheme.MUTED);body.addView(label);gap(body,8);
            body.addView(text(game.name,34,true));gap(body,7);body.addView(text(game.tagline,15,false));gap(body,18);
            LinearLayout art=column();art.setPadding(dp(8),dp(8),dp(8),dp(14));art.setBackground(ArcadeTheme.card(c,ArcadeTheme.PAPER,false));
            art.addView(new GameArtView(c,game.id),new LinearLayout.LayoutParams(-1,dp(181)));body.addView(art);gap(body,18);
            LinearLayout rules=column();rules.setPadding(dp(23),dp(28),dp(25),dp(28));
            rules.setBackground(new PaperPanelDrawable(getResources().getDisplayMetrics().density));
            addRule(rules,"怎么操作",game.controls);gap(rules,18);addRule(rules,"怎么获胜",game.goal);gap(rules,18);addRule(rules,"这一局的乐趣",game.rule);
            body.addView(rules);gap(body,12);
            if(!game.playable){
                TextView tip=text("这里先展示美术与玩法方向，完整对战会在后续加入。",12,false);tip.setTextColor(ArcadeTheme.MUTED);tip.setLineSpacing(dp(4),1);body.addView(tip);
            }
            LinearLayout bottom=column();bottom.setPadding(dp(18),dp(10),dp(18),dp(13));bottom.setBackgroundColor(ArcadeTheme.PAPER);
            Button primary=button(game.playable?"面对面，开始对战":"即将开场",game.playable?ArcadeTheme.YELLOW:0xffeae0c7);
            primary.setEnabled(game.playable);primary.setTextColor(game.playable?ArcadeTheme.INK:ArcadeTheme.MUTED);
            bottom.addView(primary,new LinearLayout.LayoutParams(-1,dp(57)));
            primary.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){actions.start(game.id);}});
            if(!game.playable){gap(bottom,7);Button sumo=button("先来一局相扑  ›",ArcadeTheme.BLUE);bottom.addView(sumo,new LinearLayout.LayoutParams(-1,dp(51)));
                sumo.setOnClickListener(new View.OnClickListener(){@Override public void onClick(View v){actions.start(GameCatalog.Id.SUMO);}});}
            addView(bottom);
        }
        void addRule(LinearLayout parent,String title,String description){
            parent.addView(text(title,17,true));gap(parent,8);TextView paragraph=text(description,14,false);
            paragraph.setLineSpacing(dp(5),1);paragraph.setTextColor(ArcadeTheme.MUTED);parent.addView(paragraph);
        }
    }
    private ArcadeScreens(){}
}
