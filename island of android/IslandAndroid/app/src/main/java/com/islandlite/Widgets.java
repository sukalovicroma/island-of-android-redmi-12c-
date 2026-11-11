package com.islandlite;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/** Три «столбика эквалайзера» для свёрнутого острова. */
class BarsView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    BarsView(Context c) {
        super(c);
        p.setColor(0xFF6E96FF);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        float bw = w / 5f;
        float[] hs = {0.5f, 0.95f, 0.7f};
        for (int i = 0; i < 3; i++) {
            float x = i * bw * 2f;
            float bh = h * hs[i];
            cv.drawRoundRect(x, h - bh, x + bw, h, bw / 2f, bw / 2f, p);
        }
    }
}

/** Полоска прогресса трека. */
class ProgressView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float fraction;

    ProgressView(Context c) {
        super(c);
    }

    void setFraction(float f) {
        if (f < 0f) f = 0f;
        if (f > 1f) f = 1f;
        if (Math.abs(f - fraction) > 0.002f) {
            fraction = f;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        float w = getWidth(), h = getHeight();
        p.setColor(0x33FFFFFF);
        cv.drawRoundRect(0, 0, w, h, h / 2f, h / 2f, p);
        if (fraction > 0f) {
            p.setColor(0xFF6E96FF);
            cv.drawRoundRect(0, 0, Math.max(h, w * fraction), h, h / 2f, h / 2f, p);
        }
    }
}

/** Кнопки плеера, нарисованные кодом: назад / играть / пауза / вперёд. */
class IconView extends View {
    static final int PREV = 0, PLAY = 1, PAUSE = 2, NEXT = 3;

    private int type;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    IconView(Context c, int type) {
        super(c);
        this.type = type;
        p.setColor(0xFFFFFFFF);
    }

    void setType(int t) {
        if (t != type) {
            type = t;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas cv) {
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float s = Math.min(getWidth(), getHeight()) * 0.32f;
        path.reset();
        switch (type) {
            case PLAY:
                path.moveTo(cx - s * 0.7f, cy - s);
                path.lineTo(cx - s * 0.7f, cy + s);
                path.lineTo(cx + s, cy);
                path.close();
                cv.drawPath(path, p);
                break;
            case PAUSE:
                cv.drawRoundRect(cx - s * 0.85f, cy - s, cx - s * 0.2f, cy + s, 4f, 4f, p);
                cv.drawRoundRect(cx + s * 0.2f, cy - s, cx + s * 0.85f, cy + s, 4f, 4f, p);
                break;
            case NEXT:
                path.moveTo(cx - s, cy - s * 0.8f);
                path.lineTo(cx - s, cy + s * 0.8f);
                path.lineTo(cx + s * 0.6f, cy);
                path.close();
                cv.drawPath(path, p);
                cv.drawRect(cx + s * 0.75f, cy - s * 0.8f, cx + s, cy + s * 0.8f, p);
                break;
            case PREV:
                path.moveTo(cx + s, cy - s * 0.8f);
                path.lineTo(cx + s, cy + s * 0.8f);
                path.lineTo(cx - s * 0.6f, cy);
                path.close();
                cv.drawPath(path, p);
                cv.drawRect(cx - s, cy - s * 0.8f, cx - s * 0.75f, cy + s * 0.8f, p);
                break;
            default:
                break;
        }
    }
}
