package com.aurelwu.indoorairqualitycollector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class    LineChartView extends View {
    private Paint paint;
    private int[] data;

    public LineChartView(Context context) {
        super(context);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);
        data = new int[]{0}; // Sample data, you can replace it with your own
    }

    public void setData(int[] newData) {
        data = newData;
        invalidate(); // Force the view to redraw with the new data
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        // Calculate the usable area after considering padding
        int usableWidth = width - paddingLeft - paddingRight;
        int usableHeight = height - paddingTop - paddingBottom;

        canvas.drawColor(Color.BLACK);

        // Draw X and Y axes
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, height - paddingBottom, paint); // Y-axis
        canvas.drawLine(paddingLeft, height - paddingBottom, width - paddingRight, height - paddingBottom, paint); // X-axis

        // Calculate scaling factors
        float xScale = usableWidth / (float) (data.length - 1);
        float yScale = usableHeight / (float) (getMaxDataValue() - 300); // Adjust for min value

        // Draw X-axis tick marks
        paint.setStrokeWidth(2);
        paint.setColor(Color.GRAY);
        for (int i = 0; i < data.length; i++) {
            float x = paddingLeft + i * xScale;
            if (i % 5 == 0) {
                canvas.drawLine(x+2, height - paddingBottom-2, x+2, height - paddingBottom - 10, paint); // Bigger tick
            } else {
                canvas.drawLine(x+2, height - paddingBottom-2, x+2, height - paddingBottom - 5, paint); // Regular tick
            }
        }

        // Draw Y-axis tick marks and labels
        paint.setStrokeWidth(2);
        paint.setColor(Color.GRAY);
        for (int i = 400; i <= getMaxDataValue(); i += 400) {
            float y = height - paddingBottom - (i - 300) * yScale; // Adjust for min value
            canvas.drawLine(paddingLeft + 10, y, paddingLeft+2, y, paint); // Tick mark
            paint.setTextSize(20);
            canvas.drawText(String.valueOf(i), paddingLeft + 15, y + 10, paint); // Label
        }
        if(data.length==0) return; //happens at start when we have no data recorded yet
        // Draw data points
        paint.setColor(Color.WHITE);
        float prevX = paddingLeft;
        float prevY = height - paddingBottom - (data[0] - 300) * yScale; // Adjust for min value
        for (int i = 0; i < data.length; i++)
        {
            float x = paddingLeft + i * xScale;
            float y = height - paddingBottom - (data[i] - 300) * yScale; // Adjust for min value
            if(i>0) {
                canvas.drawLine(prevX, prevY, x, y, paint);
                prevX = x;
                prevY = y;
            }
            if(data.length==1)
            {
                canvas.drawCircle(usableWidth/2, y, 8, paint); // Adjust the radius as needed for the circle

            }
            else if(data.length<=25)
            {
                canvas.drawCircle(x, y, 8, paint); // Adjust the radius as needed for the circle
            }
            else if(data.length<=50)
            {
                canvas.drawCircle(x, y, 6, paint); // Adjust the radius as needed for the circle
            }
            else
            {
                canvas.drawCircle(x, y, 4, paint); // Adjust the radius as needed for the circle
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Get the size of the parent container
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);

        // Set the size of the LineChartView to match the parent container
        setMeasuredDimension(parentWidth, parentHeight);
    }

    // Helper method to get the maximum value in the data array
    private int getMaxDataValue() {
        if(data.length == 0)
        {
            return 1000;
        }
        int max = data[0];
        for (int value : data) {
            if (value > max) {
                max = value;
            }
        }
        // Round up to the next multiple of 400
        return ((((max + 399) / 400) * 400) +50);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {
            // Trigger redraw of the chart
            invalidate(); // or postInvalidate() if you're not on the UI thread
        }
    }
}