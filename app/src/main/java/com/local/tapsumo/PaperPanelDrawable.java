package com.local.tapsumo;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Original code-drawn paper card, matching the game's flat, heavy outlines. */
final class PaperPanelDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path sheet=new Path(), fold=new Path();
    private final float dp;
    private final int ink=Color.rgb(32,34,41);
    PaperPanelDrawable(float density) { dp=density; }
    @Override public void draw(Canvas canvas) {
        Rect b=getBounds();
        float l=b.left+4*dp,t=b.top+7*dp,r=b.right-10*dp,bottom=b.bottom-12*dp;
        float corner=25*dp;
        sheet.reset();sheet.moveTo(l+7*dp,t);sheet.lineTo(r-corner,t);
        sheet.lineTo(r,t+corner);sheet.lineTo(r,bottom-5*dp);
        // Subtle irregular lower edge rather than a perfect system-dialog box.
        for(float x=r-8*dp;x>l+7*dp;x-=13*dp) {
            sheet.lineTo(x,bottom+(Math.round(x/dp)%2==0?0:1.4f*dp));
        }
        sheet.lineTo(l,bottom-4*dp);sheet.lineTo(l,t+7*dp);sheet.close();
        paint.setStyle(Paint.Style.FILL);paint.setColor(ink);
        canvas.save();canvas.translate(5*dp,6*dp);canvas.drawPath(sheet,paint);canvas.restore();
        paint.setColor(Color.rgb(255,249,229));canvas.drawPath(sheet,paint);
        canvas.save();canvas.clipPath(sheet);
        paint.setColor(Color.argb(17,164,127,65));paint.setStrokeWidth(.7f*dp);
        for(float y=t+15*dp;y<bottom;y+=11*dp)canvas.drawLine(l,y,r,y,paint);
        paint.setColor(Color.argb(25,156,117,65));
        for(int i=0;i<90;i++) {
            float x=l+((i*79)%997)/997f*(r-l);
            float y=t+((i*137)%991)/991f*(bottom-t);
            canvas.drawCircle(x,y,.55f*dp,paint);
        }
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3.5f*dp);
        paint.setStrokeJoin(Paint.Join.ROUND);paint.setColor(ink);canvas.drawPath(sheet,paint);
        fold.reset();fold.moveTo(r-corner,t);fold.lineTo(r-corner,t+corner);
        fold.lineTo(r,t+corner);fold.close();
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(244,211,140));canvas.drawPath(fold,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2.5f*dp);paint.setColor(ink);canvas.drawPath(fold,paint);
        // Two small pieces of colored tape tie the card to both players.
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(255,88,94));
        canvas.save();canvas.rotate(-5,l+44*dp,t+3*dp);
        canvas.drawRect(l+23*dp,t-4*dp,l+64*dp,t+9*dp,paint);canvas.restore();
        paint.setColor(Color.rgb(0,191,225));canvas.save();canvas.rotate(5,r-67*dp,t+3*dp);
        canvas.drawRect(r-88*dp,t-4*dp,r-47*dp,t+9*dp,paint);canvas.restore();
    }
    @Override public void setAlpha(int alpha) { /* The sheet remains opaque. */ }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
