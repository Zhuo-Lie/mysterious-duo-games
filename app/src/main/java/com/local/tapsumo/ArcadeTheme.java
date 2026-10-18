package com.local.tapsumo;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared native controls; Canvas is reserved for the original illustrations. */
final class ArcadeTheme {
    static final int INK=Color.rgb(32,34,41), PAPER=Color.rgb(255,249,229);
    static final int RED=Color.rgb(255,88,94), BLUE=Color.rgb(0,191,225), YELLOW=Color.rgb(255,201,44);
    static final int MUTED=Color.rgb(101,94,80), BACKGROUND=Color.rgb(247,239,216);
    static int brighten(int color){return Color.rgb(Math.min(255,Color.red(color)+9),Math.min(255,Color.green(color)+9),Math.min(255,Color.blue(color)+9));}
    static int dp(Context c,float n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    static GradientDrawable face(Context c,int color,int radius,int outline) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));
        if(outline>0)d.setStroke(dp(c,outline),INK);return d;
    }
    static LayerDrawable card(Context c,int color,boolean pressed) {
        LayerDrawable d=new LayerDrawable(new android.graphics.drawable.Drawable[]{face(c,INK,18,0),face(c,color,18,3)});
        d.setLayerInset(1,0,pressed?dp(c,4):0,0,pressed?dp(c,2):dp(c,6));return d;
    }
    static StateListDrawable buttonFace(Context c,int color) {
        StateListDrawable d=new StateListDrawable();
        d.addState(new int[]{android.R.attr.state_pressed},card(c,color,true));
        d.addState(new int[]{},card(c,color,false));return d;
    }
    static TextView text(Context c,String label,float size,boolean bold) {
        TextView t=new TextView(c);t.setText(label);t.setTextSize(size);t.setTextColor(INK);
        t.setFontFeatureSettings("kern");t.setLineSpacing(dp(c,2),1.05f);t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_SIMPLE);
        t.setTypeface(Typeface.create(bold?"sans-serif-black":"sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));
        t.setIncludeFontPadding(false);return t;
    }
    static Button button(Context c,String label,int color) {
        Button b=new Button(c);b.setText(label);b.setTextSize(15);b.setTextColor(INK);b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));b.setBackground(buttonFace(c,color));
        b.setGravity(Gravity.CENTER);b.setPadding(dp(c,8),0,dp(c,8),dp(c,5));b.setMinWidth(0);b.setMinimumWidth(0);
        return b;
    }
    static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private ArcadeTheme(){}
}
