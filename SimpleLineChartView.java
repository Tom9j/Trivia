package com.example.trivia.store;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Collections;

public class SimpleLineChartView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Path linePath;
    private Path fillPath;
    private ArrayList<Float> dataPoints;
    private float minValue = 0f;
    private float maxValue = 100f;
    private int lineColor = Color.BLUE;
    private int fillColor = Color.argb(50, 0, 0, 255); // semi-transparent blue

    public SimpleLineChartView(Context context) {
        super(context);
        init();
    }

    public SimpleLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleLineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dataPoints = new ArrayList<>();

        // Initialize paint for line
        linePaint = new Paint();
        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(5f);
        linePaint.setAntiAlias(true);

        // Initialize paint for fill
        fillPaint = new Paint();
        fillPaint.setColor(fillColor);
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // Initialize paths
        linePath = new Path();
        fillPath = new Path();
    }

    /**
     * Set the data points to display in the chart
     * @param dataPoints ArrayList of Float values to display
     */
    public void setDataPoints(ArrayList<Float> dataPoints) {
        this.dataPoints = dataPoints;

        if (!dataPoints.isEmpty()) {
            // Update min and max values
            minValue = Collections.min(dataPoints);
            maxValue = Collections.max(dataPoints);

            // Add some padding to the min/max
            float range = maxValue - minValue;
            minValue = Math.max(0, minValue - range * 0.1f);
            maxValue = maxValue + range * 0.1f;
        }

        // Request to redraw the view
        invalidate();
    }

    /**
     * Set the color of the line
     * @param color the color to use
     */
    public void setLineColor(int color) {
        this.lineColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    /**
     * Set the color of the fill below the line
     * @param color the color to use with alpha component
     */
    public void setFillColor(int color) {
        this.fillColor = color;
        fillPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (dataPoints.isEmpty()) {
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float padding = 10f;
        float graphWidth = width - 2 * padding;
        float graphHeight = height - 2 * padding;

        float xStep = graphWidth / (dataPoints.size() - 1);

        // Reset paths
        linePath.reset();
        fillPath.reset();

        // Start fill path at the bottom left
        fillPath.moveTo(padding, height - padding);

        // Create paths
        for (int i = 0; i < dataPoints.size(); i++) {
            float x = padding + i * xStep;
            float valueRange = maxValue - minValue;
            float scaledValue = 0;
            if (valueRange > 0) {
                scaledValue = (dataPoints.get(i) - minValue) / valueRange;
            }
            float y = height - padding - (scaledValue * graphHeight);

            if (i == 0) {
                linePath.moveTo(x, y);
                fillPath.lineTo(x, y);
            } else {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        // Complete the fill path
        fillPath.lineTo(padding + graphWidth, height - padding);
        fillPath.close();

        // Draw fill and line
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }
}