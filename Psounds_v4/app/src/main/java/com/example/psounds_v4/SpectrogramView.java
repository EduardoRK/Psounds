package com.example.psounds_v4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SpectrogramView extends View {

    private static final int MAX_COLUMNS = 500;
    private List<float[]> spectrogramData = new ArrayList<>();
    private Paint paint = new Paint();
    private Paint linePaint = new Paint();

    private float lineStartX = 100;
    private float lineEndX = 500;
    private static final int TOUCH_RADIUS = 40;
    private boolean movingStartLine = false;
    private boolean movingEndLine = false;

    public SpectrogramView(Context context) {
        super(context);
        init();
    }

    public SpectrogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.FILL);

        linePaint.setColor(Color.YELLOW);
        linePaint.setStrokeWidth(4f);
    }

    public void addSpectrum(float[] spectrum) {
        if (spectrogramData.size() >= MAX_COLUMNS) {
            spectrogramData.remove(0); // Evita crecimiento infinito
        }
        spectrogramData.add(spectrum);
        invalidate(); // Redibuja la vista
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int numCols = spectrogramData.size();
        if (numCols == 0) return;

        int numRows = spectrogramData.get(0).length;
        float colWidth = width / (float) numCols;
        float rowHeight = height / (float) numRows;

        for (int x = 0; x < numCols; x++) {
            float[] spectrum = spectrogramData.get(x);
            for (int y = 0; y < spectrum.length; y++) {
                float magnitude = spectrum[y];
                int color = Color.HSVToColor(new float[]{
                        Math.max(0, 240 - magnitude * 240), 1f, 1f
                });
                paint.setColor(color);
                canvas.drawRect(x * colWidth, height - (y + 1) * rowHeight,
                        (x + 1) * colWidth, height - y * rowHeight, paint);
            }
        }

        // Líneas rojas de selección
        canvas.drawLine(lineStartX, 0, lineStartX, height, linePaint);
        canvas.drawLine(lineEndX, 0, lineEndX, height, linePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float touchX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                movingStartLine = Math.abs(touchX - lineStartX) < TOUCH_RADIUS;
                movingEndLine = Math.abs(touchX - lineEndX) < TOUCH_RADIUS;
                break;

            case MotionEvent.ACTION_MOVE:
                if (movingStartLine) {
                    lineStartX = Math.max(0, Math.min(touchX, lineEndX - 20));
                    invalidate();
                } else if (movingEndLine) {
                    lineEndX = Math.min(getWidth(), Math.max(touchX, lineStartX + 20));
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                movingStartLine = false;
                movingEndLine = false;
                break;
        }

        return true;
    }
    public void clearSpectrogram() {
        spectrogramData.clear();
        invalidate();
    }


    public float getStartRatio() {
        return lineStartX / getWidth();
    }

    public float getEndRatio() {
        return lineEndX / getWidth();
    }
}