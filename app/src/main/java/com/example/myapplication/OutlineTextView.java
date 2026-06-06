package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.AttributeSet;

public class OutlineTextView extends androidx.appcompat.widget.AppCompatTextView {

    // Define the outline color and width here or via attributes
    private int strokeColor = Color.parseColor("#2ECC71");
    private float strokeWidth = 8f;

    public OutlineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Save the original text color (e.g., the Black from your XML)
        int originalTextColor = getCurrentTextColor();
        TextPaint paint = getPaint();

        // 1. Setup the STROKE (The Outline)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        paint.setStrokeJoin(Paint.Join.ROUND); // Makes corners look better
        paint.setStrokeCap(Paint.Cap.ROUND);

        // Temporarily set text color to green and draw
        this.setTextColor(strokeColor);
        super.onDraw(canvas);

        // 2. Setup the FILL (The Center)
        // We draw this second so it sits ON TOP of the stroke
        paint.setStyle(Paint.Style.FILL);

        // Restore the original color (Black) and draw
        this.setTextColor(originalTextColor);
        super.onDraw(canvas);
    }
}