package com.example.mobilnaaplikacijatim29.ui.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.google.android.material.color.MaterialColors;

public class ReportChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<String> labels = Collections.emptyList();
    private List<Double> values = Collections.emptyList();
    private String title = "";

    public ReportChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setData(String title, List<String> labels, List<Double> values) {
        this.title = title == null ? "" : title;
        this.labels = labels == null ? Collections.emptyList() : new ArrayList<>(labels);
        this.values = values == null ? Collections.emptyList() : new ArrayList<>(values);
        setContentDescription(this.title + ", " + accessibleSummary(this.values));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float left = dp(48);
        float right = width - dp(12);
        float top = dp(34);
        float bottom = height - dp(34);

        paint.setColor(resolveTextColor());
        paint.setTextSize(sp(15));
        paint.setFakeBoldText(true);
        canvas.drawText(title, left, dp(20), paint);
        paint.setFakeBoldText(false);

        if (values.isEmpty()) {
            paint.setTextSize(sp(13));
            canvas.drawText("Nema podataka", left, (top + bottom) / 2, paint);
            return;
        }

        double max = chartMaximum(values);
        paint.setStrokeWidth(dp(1));
        paint.setTextSize(sp(10));
        for (int line = 0; line <= 4; line++) {
            float y = bottom - (bottom - top) * line / 4f;
            paint.setColor(Color.argb(55, 100, 100, 100));
            canvas.drawLine(left, y, right, y, paint);
            paint.setColor(resolveTextColor());
            canvas.drawText(format(max * line / 4.0), dp(3), y + dp(4), paint);
        }

        float chartWidth = right - left;
        float slot = chartWidth / values.size();
        float barWidth = Math.max(dp(2), slot * 0.68f);
        paint.setColor(Color.rgb(30, 110, 190));
        for (int index = 0; index < values.size(); index++) {
            float center = left + slot * index + slot / 2f;
            float barHeight = (float) (Math.max(0, values.get(index)) / max * (bottom - top));
            canvas.drawRoundRect(center - barWidth / 2, bottom - barHeight,
                    center + barWidth / 2, bottom, dp(3), dp(3), paint);
        }

        paint.setColor(resolveTextColor());
        paint.setTextSize(sp(9));
        int labelStep = Math.max(1, (int) Math.ceil(labels.size() / 5.0));
        for (int index = 0; index < labels.size(); index += labelStep) {
            drawDateLabel(canvas, labels.get(index), left + slot * index + slot / 2f, bottom);
        }
        int last = labels.size() - 1;
        if (last >= 0 && last % labelStep != 0) {
            drawDateLabel(canvas, labels.get(last), left + slot * last + slot / 2f, bottom);
        }
    }

    private void drawDateLabel(Canvas canvas, String label, float center, float bottom) {
        String shortLabel = label != null && label.length() >= 10 ? label.substring(5) : label;
        if (shortLabel == null) return;
        float textWidth = paint.measureText(shortLabel);
        canvas.drawText(shortLabel, center - textWidth / 2, bottom + dp(16), paint);
    }

    static double chartMaximum(List<Double> values) {
        double max = 0;
        for (Double value : values) if (value != null) max = Math.max(max, value);
        return max <= 0 ? 1 : max;
    }

    private static String accessibleSummary(List<Double> values) {
        double sum = 0;
        for (Double value : values) if (value != null) sum += value;
        return values.size() + " dana, ukupno " + format(sum);
    }

    private static String format(double value) {
        return Math.abs(value - Math.rint(value)) < 0.001
                ? String.format(Locale.getDefault(), "%.0f", value)
                : String.format(Locale.getDefault(), "%.1f", value);
    }

    private int resolveTextColor() {
        return MaterialColors.getColor(this,
                com.google.android.material.R.attr.colorOnSurface);
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private float sp(float value) { return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, value, getResources().getDisplayMetrics()); }
}
