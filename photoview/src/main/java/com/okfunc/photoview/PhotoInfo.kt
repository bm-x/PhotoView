package com.okfunc.photoview

import android.graphics.RectF
import android.widget.ImageView

/**
 *
 * Created by buck on 2017/10/17.
 */
data class PhotoInfo(
        val scaleType: ImageView.ScaleType,
        val extendType: Int?,
        val degrees: Float,
        val scale: Float,
        val translateX: Int,
        val translateY: Int,

        val screenRect: RectF,  // 图片在手机屏幕的位置
        val widgetRect: RectF,  // 控件的大小
        val imageRect: RectF    // 图片的大小
)