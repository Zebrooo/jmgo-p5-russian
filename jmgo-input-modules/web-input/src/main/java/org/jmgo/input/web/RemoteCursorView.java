package org.jmgo.input.web;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

final class RemoteCursorView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int cursorX;
    private int cursorY;

    RemoteCursorView(Context context) {
        super(context);
        setClickable(false);
        setFocusable(false);
        fill.setColor(Color.WHITE);
        outline.setColor(Color.rgb(255, 82, 88));
        outline.setStyle(Paint.Style.STROKE);
        outline.setStrokeWidth(4.0f);
    }

    void moveTo(int x, int y) {
        cursorX = x;
        cursorY = y;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawCircle(cursorX, cursorY, 11.0f, fill);
        canvas.drawCircle(cursorX, cursorY, 16.0f, outline);
        canvas.drawLine(cursorX - 23, cursorY, cursorX + 23, cursorY, outline);
        canvas.drawLine(cursorX, cursorY - 23, cursorX, cursorY + 23, outline);
    }
}
