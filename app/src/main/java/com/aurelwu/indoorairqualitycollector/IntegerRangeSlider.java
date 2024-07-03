package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class IntegerRangeSlider extends View {

    private Paint textPaint;
    private int previousLength = 0;
    private float thumbRadius = 20f;
    private int min = 0;
    private int max = 1;
    private int minValue = 0;
    private int maxValue = 1;

    private Paint paint;
    private boolean hasTouchedSliderBefore = false;
    private String descriptionText = "↑↑↑  ↑↑↑\r\nUse slider to trim start & end if required";

    public IntegerRangeSlider(Context context) {
        super(context);
        init();
    }

    public IntegerRangeSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public IntegerRangeSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void ReInit() {
        maxValue = 1;
        minValue = 0;
        min = 0;
        max = 1;
        previousLength = 0;
        hasTouchedSliderBefore = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw track
        paint.setColor(Color.GRAY);
        canvas.drawLine(getPaddingLeft(), getHeight() / 2f, getWidth() - getPaddingRight(), getHeight() / 2f, paint);

        // Draw range
        paint.setColor(Color.WHITE);
        if (!hasTouchedSliderBefore) {
            minValue = 0;
            maxValue = max;
        }
        float minX = valueToX(minValue);
        float maxX = valueToX(maxValue);
        canvas.drawLine(minX, getHeight() / 2f, maxX, getHeight() / 2f, paint);

        // Draw thumbs
        paint.setColor(Color.WHITE);
        canvas.drawCircle(minX, getHeight() / 2f, thumbRadius, paint);
        canvas.drawCircle(maxX, getHeight() / 2f, thumbRadius, paint);

        // Draw description text
        float textX = getWidth() / 2f;
        float textY = getHeight() / 2f + thumbRadius + 40; // Adjust the Y position as per your requirement
        for (String line : descriptionText.split("\r\n")) {
            canvas.drawText(line, textX, textY, textPaint);
            textY += textPaint.getTextSize() + 10; // Adjust line spacing as needed
        }
    }

    private float valueToX(int value) {
        float range = max - min;
        float proportion = (value - min) / range;
        return getPaddingLeft() + proportion * (getWidth() - getPaddingLeft() - getPaddingRight());
    }

    private int xToValue(float x) {
        float proportion = (x - getPaddingLeft()) / (getWidth() - getPaddingLeft() - getPaddingRight());
        return Math.round(min + proportion * (max - min));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        hasTouchedSliderBefore = true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                if (Math.abs(x - valueToX(minValue)) < Math.abs(x - valueToX(maxValue))) {
                    minValue = Math.max(min, Math.min(maxValue, xToValue(x)));
                } else {
                    maxValue = Math.max(minValue, Math.min(max, xToValue(x)));
                }
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void setMin(int min) {
        this.min = min;
        invalidate();
    }

    public void setMax(int max) {
        this.max = max;
        invalidate();
    }

    public int getMinValue() {
        return minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void SetDataRange(int length) {
        min = 0; //always 0
        max = Math.max(1, length - 1);

        if (length > previousLength) {
            previousLength = length;
            if (maxValue == max - 1) maxValue = max;
        }

        invalidate();
    }
}
