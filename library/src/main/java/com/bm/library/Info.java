package com.bm.library;

import android.graphics.RectF;
import android.widget.ImageView;

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
    ImageView.ScaleType mScaleType;

    public Info(RectF rect, RectF local, RectF img, RectF widget, float scale,ImageView.ScaleType scaleType) {
        this.mRect.set(rect);
        this.mLocalRect.set(local);
        this.mImgRect.set(img);
        this.mWidgetRect.set(widget);
        this.mScale = scale;
        this.mScaleType = scaleType;
    }
}
