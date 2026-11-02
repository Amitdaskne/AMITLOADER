package com.privatesrcwala;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Floating cyan, violet and pink light fields for glass UI. */
public class AnimatedBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long startedAt;
    public AnimatedBackgroundView(Context context) { super(context); startedAt = System.currentTimeMillis(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); int w=getWidth(), h=getHeight(); if(w<=0||h<=0)return;
        canvas.drawColor(Color.rgb(10,16,36)); float t=(System.currentTimeMillis()-startedAt)/1000f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(50,25,201,226)); canvas.drawCircle(w*.08f,h*(.14f+.02f*(float)Math.sin(t)),w*.30f,paint);
        paint.setColor(Color.argb(44,155,92,232)); canvas.drawCircle(w*.95f,h*.74f,w*.28f,paint);
        paint.setColor(Color.argb(36,224,82,145)); canvas.drawCircle(w*.88f,h*.10f,w*.12f,paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2.2f); paint.setColor(Color.argb(72,25,201,226));
        Path p=new Path(); p.moveTo(-w*.08f,h*.70f); p.cubicTo(w*.22f,h*.57f,w*.66f,h*.86f,w*1.06f,h*.62f); canvas.drawPath(p,paint);
        paint.setColor(Color.argb(62,224,82,145)); Path q=new Path(); q.moveTo(-w*.04f,h*.76f); q.cubicTo(w*.28f,h*.63f,w*.67f,h*.92f,w*1.04f,h*.69f); canvas.drawPath(q,paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(190,100,240,214)); canvas.drawCircle(w*.90f,h*(.18f+.02f*(float)Math.sin(t*1.2)),5f,paint);
        postInvalidateDelayed(80L);
    }
}
