package com.example.psounds_v4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;

public class WaveformView extends View {

    private List<Float> amplitudes;
    private Paint wavePaint;
    private Paint linePaint;
    private Paint textPaint;
    private float audioDuration = 1f; // segundos

    private float lineStartX = 100;
    private float lineEndX = 500;

    private static final int TOUCH_RADIUS = 40;
    private boolean movingStartLine = false;
    private boolean movingEndLine = false;

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformView(Context context) {
        super(context);
        init();
    }

    private void init() {
        wavePaint = new Paint();
        wavePaint.setColor(Color.BLUE);
        wavePaint.setStrokeWidth(2f);

        linePaint = new Paint();
        linePaint.setColor(Color.CYAN);
        linePaint.setStrokeWidth(4f);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30f);
        textPaint.setAntiAlias(true);
    }

    public void setAmplitudes(List<Float> amps) {
        this.amplitudes = amps;
        invalidate();
    }

    public void setAudioDuration(float duration) {
        this.audioDuration = duration;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (amplitudes == null || amplitudes.isEmpty()) return;

        float height = getHeight();
        float centerY = height / 2f;
        float scaleX = getWidth() / (float) amplitudes.size();
        float scaleY = height / 2f;

        for (int i = 0; i < amplitudes.size(); i++) {
            float x = i * scaleX;
            float amp = amplitudes.get(i);
            float y = amp * scaleY;
            canvas.drawLine(x, centerY - y, x, centerY + y, wavePaint);
        }

        // Líneas y tiempos
        drawLineWithTime(canvas, lineStartX, "Inicio");
        drawLineWithTime(canvas, lineEndX, "Fin");
    }

    private void drawLineWithTime(Canvas canvas, float x, String label) {
        float height = getHeight();
        canvas.drawLine(x, 0, x, height, linePaint);

        float seconds = (x / getWidth()) * audioDuration;
        String timeLabel = String.format("%s: %.2f s", label, seconds);
        canvas.drawText(timeLabel, x + 10, 40, textPaint);
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

    public float getStartRatio() {
        return lineStartX / getWidth();
    }

    public float getEndRatio() {
        return lineEndX / getWidth();
    }
}
