package com.bm.library;

import android.graphics.RectF;

/**
 * Created by q2366 on 2015/8/19.
 */
public class Info {
    RectF mRect;
    RectF mWidgetRect;
    float mScale;

    public Info(RectF rect, RectF img, float scale) {
        this.mRect = rect;
        this.mWidgetRect = img;
        this.mScale = scale;
    }
}
