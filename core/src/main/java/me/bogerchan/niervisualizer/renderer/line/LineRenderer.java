/**
 * Copyright 2011, Felix Palmer
 * <p>
 * Licensed under the MIT license:
 * http://creativecommons.org/licenses/MIT/
 */
package me.bogerchan.niervisualizer.renderer.line;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.jetbrains.annotations.NotNull;

import me.bogerchan.niervisualizer.renderer.IRenderer;

public class LineRenderer implements IRenderer {
    private Paint mPaint;
    private Paint mFlashPaint;
    private boolean mCycleColor;
    private float mAmplitude = 0;
    private boolean useFlashPaint = false;
    private float[] mPoints;

    public LineRenderer(boolean cycleColor) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.CYAN);
        mPaint.setStrokeWidth(5f);
        mFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFlashPaint.setStrokeWidth(5f);
        mFlashPaint.setColor(Color.CYAN);
        mCycleColor = cycleColor;
    }

    public LineRenderer(Paint paint, Paint flashPaint) {
        this(paint, flashPaint, false);
    }

    public LineRenderer(Paint paint,
                        Paint flashPaint,
                        boolean cycleColor) {
        mPaint = paint;
        mFlashPaint = flashPaint;
        mCycleColor = cycleColor;
    }

    private float colorCounter = 0;

    private void cycleColor() {
        int r = (int) Math.floor(128 * (Math.sin(colorCounter) + 3));
        int g = (int) Math.floor(128 * (Math.sin(colorCounter + 1) + 1));
        int b = (int) Math.floor(128 * (Math.sin(colorCounter + 7) + 1));
        mPaint.setColor(Color.argb(128, r, g, b));
        colorCounter += 0.03;
    }

    @Override
    public void onStart(int captureSize) {
        mPoints = new float[captureSize * 4];
    }

    @Override
    public void onStop() {

    }

    @Override
    public void calculate(@NotNull Rect rect, @NotNull byte[] data) {
        if (mCycleColor) {
            cycleColor();
        }
        // Calculate points for line
        for (int i = 0; i < data.length - 1; i++) {
            mPoints[i * 4] = rect.width() * i / (data.length - 1);
            mPoints[i * 4 + 1] = rect.height() / 2
                    + ((byte) (data[i] + 128)) * (rect.height() / 3) / 128;
            mPoints[i * 4 + 2] = rect.width() * (i + 1) / (data.length - 1);
            mPoints[i * 4 + 3] = rect.height() / 2
                    + ((byte) (data[i + 1] + 128)) * (rect.height() / 3) / 128;
        }

        // Calc amplitude for this waveform
        float accumulator = 0;
        for (int i = 0; i < data.length - 1; i++) {
            accumulator += Math.abs(data[i]);
        }

        float amp = accumulator / (128 * data.length);
        if (amp > mAmplitude) {
            // Amplitude is bigger than normal, make a prominent line
            mAmplitude = amp;
            useFlashPaint = true;
        } else {
            // Amplitude is nothing special, reduce the amplitude
            mAmplitude *= 0.99;
            useFlashPaint = false;
        }
    }

    @Override
    public void render(@NotNull Canvas canvas) {
        canvas.drawLines(mPoints, useFlashPaint ? mFlashPaint : mPaint);
    }

    @NotNull
    @Override
    public DataType getInputDataType() {
        return DataType.WAVE;
    }
}
