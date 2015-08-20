package com.bm.library;

import android.graphics.RectF;

/**
 * Created by liuheng on 2015/8/19.
 */
public class Info {
    // 内部图片在整个窗口的位置
    RectF mRect = new RectF();
    RectF mLocalRect = new RectF();
    RectF mImgRect = new RectF();
    RectF mWidgetRect = new RectF();
    float mScale;

    public Info(RectF rect, RectF local, RectF img, RectF widget, float scale) {
        this.mRect.set(rect);
        this.mLocalRect.set(local);
        this.mImgRect.set(img);
        this.mWidgetRect.set(widget);
        this.mScale = scale;
    }
}
