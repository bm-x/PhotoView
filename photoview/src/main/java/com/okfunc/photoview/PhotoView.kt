package com.okfunc.photoview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.OverScroller
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.max


/**
 * Created by buck on 2017/10/17.
 */
open class PhotoView(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0) : ImageView(context, attrs, defStyleAttr) {

    protected var isInit = false
    protected var hasMultiTouch = false
    protected var hasDrawbale = false
    protected var isKnowSize = false
    protected var isZoomUp = false
    protected var isImageWidthOverScreen = false
    protected var isImageHeightOverScreen = false

    protected var isOverTranslate = false
    protected var canRotate = false
    protected var mRotateFlag = 0f
    protected var mDegrees = 0f
    protected var mScale = 1.0f
    protected var mTranslateX = 0
    protected var mTranslateY = 0

    protected val mDetector: GestureDetector
    protected val mScaleDetector: ScaleGestureDetector

    protected val widgetRect = RectF()    // PhotoView控件的大小
    protected val baseRect = RectF()      // 后期所有图片变化的基准大小
    protected val imageRect = RectF()     // 图片由基准大小经过变换后的实际显示大小
    protected val tmpRect = RectF()
    protected var clipRect: RectF? = null

    protected val screenCenter = PointF() // 屏幕中心点
    protected val scaleCenter = PointF()  // 缩放中心点
    protected val rotateCenter = PointF() // 旋转中心的

    protected val baseMatrix = Matrix()
    protected val animaMatrix = Matrix()
    protected val synthesisMatrix = Matrix()
    protected val tmpMatrix = Matrix()

    protected val transform = Transform()
    protected var swipeCloseController: SwipeCloseController? = null

    protected var _clickListener: OnClickListener? = null
    protected var _longClickListener: OnLongClickListener? = null

    protected var mScaleType: ScaleType? = null
    protected var fromInfo: PhotoInfo? = null

    var swipeDownCloseListener: SwipeCloseListener? = null

    var swipeClose = false
        set(value) {
            field = value
            swipeCloseController = if (field) SwipeCloseController() else null
        }
    var swipeMinScaleDownLevel: Float = 0f
    var maxSwipeCloseDistance = 0
    var maxOverScrollResistance = 0
    var maxFlingOverScroll = 0
    var animaDuring: Int = 0
        set (value) = if (value >= 0) field = value else Unit
    var maxOverScroll = 0
    // 最少旋转多少度才让真正开始旋转
    var minRotateStep = 35
    var maxSacleLevel = 2.5f
        set (value) = if (value >= 1) field = value else Unit
    var rotatable = false
    var zoomable = false
    var touchable = false

    var extendType: Int? = null
        set(value) {
            field = value
            mScaleType = ScaleType.MATRIX
        }

    private val density: Float

    constructor(context: Context) : this(context, null, 0, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0)

    init {
        density = resources.displayMetrics.density

        initAttributes(context, attrs, defStyleAttr, 0)
        mDetector = GestureDetector(context, GestureListener())
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
    }

    private fun dp2px(dp: Float) = (dp * density + 0.5f).toInt()

    private fun initAttributes(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        super.setScaleType(ScaleType.MATRIX)

        val a = context.obtainStyledAttributes(attrs, R.styleable.PhotoView, defStyleAttr, defStyleRes)
        if (a.hasValue(R.styleable.PhotoView_extendType)) {
            extendType = a.getInt(R.styleable.PhotoView_extendType, 0)
        }
        zoomable = a.getBoolean(R.styleable.PhotoView_zoomable, false)
        rotatable = a.getBoolean(R.styleable.PhotoView_rotatable, false)
        touchable = a.getBoolean(R.styleable.PhotoView_touchable, false)
        maxSacleLevel = a.getFloat(R.styleable.PhotoView_maxScaleLevel, 2.5f)
        animaDuring = a.getInt(R.styleable.PhotoView_animaDuring, 320)
        maxOverScroll = a.getDimension(R.styleable.PhotoView_maxOverScroll, dp2px(30f).toFloat()).toInt()
        maxFlingOverScroll = a.getDimension(R.styleable.PhotoView_maxFlingOverScroll, dp2px(30f).toFloat()).toInt()
        maxOverScrollResistance = a.getDimension(R.styleable.PhotoView_maxOverScrollResistance, dp2px(140f).toFloat()).toInt()
        swipeClose = a.getBoolean(R.styleable.PhotoView_swipeClose, false)
        maxSwipeCloseDistance = a.getDimension(R.styleable.PhotoView_maxSwipeCloseDistance, dp2px(300f).toFloat()).toInt()
        swipeMinScaleDownLevel = a.getFloat(R.styleable.PhotoView_swipeMinScaleDownLevel, 0.6f)
        a.recycle()
    }

    override fun setScaleType(scaleType: ScaleType) {
        mScaleType = scaleType
        initDrawableState()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        widgetRect.set(0f, 0f, w.toFloat(), h.toFloat())
        screenCenter.set(w.toFloat() / 2, h.toFloat() / 2)

        if (!isKnowSize) {
            isKnowSize = true
            initDrawableState()
        }
    }

    fun setSwipeCloseListener(l: () -> Unit) {
        swipeCloseController?.closeListener = l
    }

    override fun setOnClickListener(l: OnClickListener?) {
        _clickListener = l;
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        _longClickListener = l
    }

    override fun setImageResource(resId: Int) {
        setImageDrawable(resources.getDrawable(resId))
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)

        hasDrawbale = drawable != null && hasSize(drawable)

        if (!hasDrawbale) return

        initDrawableState()
    }

    protected fun initDrawableState() {
        if (!hasDrawbale || !isKnowSize) return

        baseMatrix.reset()
        animaMatrix.reset()

        /**
         * step 1: 保存图片原始数据
         */

        isZoomUp = false

        val width = width
        val height = height
        val (imgWidth, imgHeight) = getDrawableSize(drawable)

        baseRect.set(0f, 0f, imgWidth.toFloat(), imgHeight.toFloat())

        /**
         * step 2: 将原图转为fit_center
         */

        val tx = (width - imgWidth).toFloat() / 2
        val ty = (height - imgHeight).toFloat() / 2

        var sx = 1f;
        var sy = 1f;

        if (imgWidth > width) sx = width / imgWidth.toFloat()
        if (imgHeight > height) sy = height / imgHeight.toFloat()

        val scale = Math.min(sx, sy)

        baseMatrix.reset()
        baseMatrix.postTranslate(tx, ty)
        baseMatrix.postScale(scale, scale, screenCenter.x, screenCenter.y)
        baseMatrix.mapRect(baseRect)

        scaleCenter.set(screenCenter)
        rotateCenter.set(screenCenter)

        executeTranslate()

        /**
         * step 3: 在fit_center的基础上转变为对应的scaletype
         * 后期的所有变化都基于此
         */
        when (mScaleType) {
            ScaleType.CENTER -> applyCenter()
            ScaleType.CENTER_CROP -> applyCenterCrop()
            ScaleType.CENTER_INSIDE -> applyCenterInside()
            ScaleType.FIT_CENTER -> applyFitCenter()
            ScaleType.FIT_START -> applyFitStart()
            ScaleType.FIT_END -> applyFitEnd()
            ScaleType.FIT_XY -> applyFitXY()
            ScaleType.MATRIX -> when (extendType) {
                CUSTOM_TYPE_CENTER_FIT_START -> applyCenterFitStart()
                CUSTOM_TYPE_CENTER_FIT_END -> applyCenterFitEnd()
                CUSTOM_TYPE_FIT_CENTER_START -> applyFitCenterStart()
                CUSTOM_TYPE_FIT_CENTER_END -> applyFitCenterEnd()
                CUSTOM_TYPE_CENTER_CROP_START -> applyCenterCropStart()
                CUSTOM_TYPE_CENTER_CROP_END -> applyCenterCropEnd()
                else -> applyExtendType(extendType ?: 0)
            }
        }

        isInit = true

        if (fromInfo != null) {
            animaFrom(fromInfo!!)
            fromInfo = null
        }
    }

    /**
     * 将当前synthesisMatrix中的数据作为baseMatrix,baseRect
     */
    private fun applySynthesisToBase() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)
        baseRect.set(0f, 0f, imageWidth.toFloat(), imageHeight.toFloat())
        baseMatrix.set(synthesisMatrix)
        baseMatrix.mapRect(baseRect)
        mScale = 1f
        mTranslateX = 0
        mTranslateY = 0
        animaMatrix.reset()
    }

    open protected fun applyExtendType(extendType: Int) {

    }

    protected fun applyCenterFitStart() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)

        if (imageWidth < widgetRect.width() && imageHeight < widgetRect.height()) {
            applyCenter()
        } else {
            applyFitCenterStart()
        }
    }

    protected fun applyCenterFitEnd() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)

        if (imageWidth < widgetRect.width() && imageHeight < widgetRect.height()) {
            applyCenter()
        } else {
            applyFitCenterEnd()
        }
    }

    protected fun applyCenterCropStart() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)

        if (imageWidth < widgetRect.width() && imageHeight < widgetRect.height()) {
            applyCenterCrop()
        } else {
            applyFitCenterStart()
        }
    }

    protected fun applyCenterCropEnd() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)

        if (imageWidth < widgetRect.width() && imageHeight < widgetRect.height()) {
            applyCenterCrop()
        } else {
            applyFitCenterEnd()
        }
    }

    protected fun applyFitCenterStart() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)

        fun fitHorizontalEnd() {
            applyCenterCrop()

            animaMatrix.postTranslate(imageRect.left, 0f)
            executeTranslate()
            applySynthesisToBase()
        }

        fun fitVerticalEnd() {
            applyCenterCrop()

            animaMatrix.postTranslate(0f, imageRect.top)
            executeTranslate()
            applySynthesisToBase()
        }

        if (imageWidth > widgetRect.width() && imageHeight > widgetRect.height()) {
            if (imageWidth > imageHeight) {
                fitHorizontalEnd()
            } else {
                fitVerticalEnd()
            }
        } else if (imageWidth > widgetRect.width()) {
            fitHorizontalEnd()
        } else if (imageHeight > widgetRect.height()) {
            fitVerticalEnd()
        }
    }

    protected fun applyFitCenterEnd() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)

        fun fitHorizontalStart() {
            applyCenterCrop()

            animaMatrix.postTranslate(-imageRect.left, 0f)
            executeTranslate()
            applySynthesisToBase()
        }

        fun fitVerticalStart() {
            applyCenterCrop()

            animaMatrix.postTranslate(0f, -imageRect.top)
            executeTranslate()
            applySynthesisToBase()
        }

        if (imageWidth > widgetRect.width() && imageHeight > widgetRect.height()) {
            if (imageWidth > imageHeight) {
                fitHorizontalStart()
            } else {
                fitVerticalStart()
            }
        } else if (imageWidth > widgetRect.width()) {
            fitHorizontalStart()
        } else if (imageHeight > widgetRect.height()) {
            fitVerticalStart()
        }
    }

    protected fun applyCenter() {
        val (imageWidth, imageHeight) = getDrawableSize(drawable)

        if (imageWidth > widgetRect.width() || imageHeight > widgetRect.height()) {
            val scaleX = imageWidth / imageRect.width()
            val scaleY = imageHeight / imageRect.height()

            mScale = Math.max(scaleX, scaleY)

            animaMatrix.postScale(mScale, mScale, screenCenter.x, screenCenter.y)

            executeTranslate()
            applySynthesisToBase()
        }
    }

    protected fun applyCenterCrop() {
        if (imageRect.width() < widgetRect.width() || imageRect.height() < widgetRect.height()) {
            val scaleX = widgetRect.width() / imageRect.width()
            val scaleY = widgetRect.height() / imageRect.height()

            mScale = Math.max(scaleX, scaleY)

            animaMatrix.postScale(mScale, mScale, screenCenter.x, screenCenter.y)

            executeTranslate()
            applySynthesisToBase()
        }
    }

    protected fun applyCenterInside() {
        if (imageRect.width() > widgetRect.width() || imageRect.height() > widgetRect.height()) {
            val scaleX = widgetRect.width() / imageRect.width()
            val scaleY = widgetRect.height() / imageRect.height()

            mScale = Math.min(scaleX, scaleY)

            animaMatrix.postScale(mScale, mScale, screenCenter.x, screenCenter.y)

            executeTranslate()
            applySynthesisToBase()
        }
    }

    protected fun applyFitCenter() {
        if (imageRect.width() < widgetRect.width() || imageRect.height() < widgetRect.height()) {
            val scaleX = widgetRect.width() / imageRect.width()
            val scaleY = widgetRect.height() / imageRect.height()

            mScale = Math.min(scaleX, scaleY)

            animaMatrix.postScale(mScale, mScale, screenCenter.x, screenCenter.y)

            executeTranslate()
            applySynthesisToBase()
        }
    }

    protected fun applyFitStart() {
        applyFitCenter()

        if (imageRect.width() < widgetRect.width()) {
            animaMatrix.postTranslate(-imageRect.left, 0f)
            executeTranslate()
            applySynthesisToBase()
        } else if (imageRect.height() < widgetRect.height()) {
            animaMatrix.postTranslate(0f, -imageRect.top)
            executeTranslate()
            applySynthesisToBase()
        }
    }

    protected fun applyFitEnd() {
        applyFitCenter()

        if (imageRect.width() < widgetRect.width()) {
            animaMatrix.postTranslate(imageRect.left, 0f)
            executeTranslate()
            applySynthesisToBase()
        } else if (imageRect.height() < widgetRect.height()) {
            animaMatrix.postTranslate(0f, imageRect.top)
            executeTranslate()
            applySynthesisToBase()
        }
    }

    protected fun applyFitXY() {
        val scaleX = widgetRect.width() / imageRect.width()
        val scaleY = widgetRect.height() / imageRect.height()

        animaMatrix.postScale(scaleX, scaleY, screenCenter.x, screenCenter.y)

        executeTranslate()
        applySynthesisToBase()
    }

    protected fun executeTranslate() {
        synthesisMatrix.set(baseMatrix)
        synthesisMatrix.postConcat(animaMatrix)
        imageMatrix = synthesisMatrix

        animaMatrix.mapRect(imageRect, baseRect)

        isImageWidthOverScreen = imageRect.width() > widgetRect.width()
        isImageHeightOverScreen = imageRect.height() > widgetRect.height()
    }

    protected fun hasSize(drawable: Drawable)
            = !((drawable.getIntrinsicHeight() <= 0 || drawable.getIntrinsicWidth() <= 0)
            && (drawable.getMinimumWidth() <= 0 || drawable.getMinimumHeight() <= 0)
            && (drawable.getBounds().width() <= 0 || drawable.getBounds().height() <= 0))

    protected fun canScrollHorizontallySelf(direction: Float): Boolean
            = imageRect.width() > widgetRect.width()
            && !(direction < 0 && Math.round(imageRect.left) - direction >= widgetRect.left)
            && !(direction > 0 && Math.round(imageRect.right) - direction <= widgetRect.right)

    protected fun canScrollVerticallySelf(direction: Float): Boolean
            = imageRect.height() > widgetRect.height()
            && !(direction < 0 && Math.round(imageRect.top) - direction >= widgetRect.top)
            && !(direction > 0 && Math.round(imageRect.bottom) - direction <= widgetRect.bottom)

    override fun canScrollHorizontally(direction: Int)
            = if (hasMultiTouch) true else canScrollHorizontallySelf(direction.toFloat())

    override fun canScrollVertically(direction: Int)
            = if (hasMultiTouch) true else canScrollVerticallySelf(direction.toFloat())

    protected fun resistanceScrollBy(overScroll: Float, detal: Float, resistance: Int = maxOverScrollResistance)
            = detal * (Math.abs(Math.abs(overScroll) - resistance) / resistance.toFloat())

    protected fun checkRect(out: RectF) = if (!isOverTranslate) mapRect(widgetRect, imageRect, out) else Unit

    /**
     * 按给定的目标，以动画的形式移动到相对居中的位置
     */
    protected fun animaResetToBaseReferTarget(imgRect: RectF) {
        var tx = 0
        var ty = 0

        if (imgRect.width() <= widgetRect.width()) {
            if (!isImageCenterWidth(imgRect))
                tx = -((widgetRect.width() - imgRect.width()) / 2 - imgRect.left).toInt()
        } else {
            if (imgRect.left > widgetRect.left) {
                tx = (imgRect.left - widgetRect.left).toInt()
            } else if (imgRect.right < widgetRect.right) {
                tx = (imgRect.right - widgetRect.right).toInt()
            }
        }

        if (imgRect.height() <= widgetRect.height()) {
            if (!isImageCenterHeight(imgRect))
                ty = -((widgetRect.height() - imgRect.height()) / 2 - imgRect.top).toInt()
        } else {
            if (imgRect.top > widgetRect.top) {
                ty = (imgRect.top - widgetRect.top).toInt()
            } else if (imgRect.bottom < widgetRect.bottom) {
                ty = (imgRect.bottom - widgetRect.bottom).toInt()
            }
        }

        if (tx != 0 || ty != 0) {
            if (!transform.flingScroller.isFinished) transform.flingScroller.abortAnimation()
            transform.withTranslate(-tx, -ty)
        }
    }

    protected fun isImageCenterHeight(rect: RectF)
            = Math.abs(Math.round(rect.top) - (widgetRect.height() - rect.height()) / 2) < 1

    protected fun isImageCenterWidth(rect: RectF)
            = Math.abs(Math.round(rect.left) - (widgetRect.width() - rect.width()) / 2) < 1

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        return if (rotatable || zoomable || touchable) onTouchEvent(event) else false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (event.pointerCount >= 2) hasMultiTouch = true

        if (event.action == MotionEvent.ACTION_DOWN) onDown()

        if (touchable) mDetector.onTouchEvent(event)

        if (rotatable) mRotateDetector.onTouchEvent(event)

        if (zoomable) mScaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> onUp()
        }

        return true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val cr = clipRect
        clipRect = null
        if (cr != null) {
            canvas.clipRect(cr)
        }
    }

    protected fun onDown() {
        swipeCloseController?.onDown()
        canRotate = false
        hasMultiTouch = false
        isOverTranslate = false
        removeCallbacks(mClickRunnable)
    }

    protected fun onUp() {
        if (swipeCloseController != null && swipeCloseController!!.onUp()) return
        if (transform.isRunning) return

        if (canRotate || mDegrees % 90 != 0f) {
            var toDegrees = ((mDegrees / 90).toInt() * 90).toFloat()
            val remainder = mDegrees % 90

            if (remainder > 45)
                toDegrees += 90f
            else if (remainder < -45)
                toDegrees -= 90f

            transform.withRotate(mDegrees.toInt(), toDegrees.toInt())

            mDegrees = toDegrees
        }

        var scale = mScale

        if (mScale < 1) {
            scale = 1f
            transform.withScale(mScale, 1f)
        } else if (mScale > maxSacleLevel) {
            scale = maxSacleLevel
            transform.withScale(mScale, maxSacleLevel)
        }

        val cx = imageRect.left + imageRect.width() / 2
        val cy = imageRect.top + imageRect.height() / 2

        scaleCenter.set(cx, cy)
        rotateCenter.set(cx, cy)

        mTranslateX = 0
        mTranslateY = 0

        tmpMatrix.reset()
        tmpMatrix.postTranslate(-baseRect.left, -baseRect.top)
        tmpMatrix.postTranslate(cx - baseRect.width() / 2, cy - baseRect.height() / 2)
        tmpMatrix.postScale(scale, scale, cx, cy)
        tmpMatrix.postRotate(mDegrees, cx, cy)
        tmpMatrix.mapRect(tmpRect, baseRect)

        animaResetToBaseReferTarget(tmpRect)

        transform.start()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onLongPress(e: MotionEvent) {
            _longClickListener?.onLongClick(this@PhotoView)
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            postDelayed(mClickRunnable, 250)
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (hasMultiTouch) return false
            if (!isImageWidthOverScreen && !isImageHeightOverScreen) return false
            if (transform.isRunning) return false
            if (swipeCloseController != null && swipeCloseController!!.onFling(e1, e2, velocityX, velocityY)) return true

            var vx = velocityX
            var vy = velocityY

            if (Math.round(imageRect.left) >= widgetRect.left || Math.round(imageRect.right) <= widgetRect.right) {
                vx = 0f
            }

            if (Math.round(imageRect.top) >= widgetRect.top || Math.round(imageRect.bottom) <= widgetRect.bottom) {
                vy = 0f
            }

            if (canRotate || mDegrees % 90 != 0f) {
                var toDegrees = ((mDegrees / 90).toInt() * 90).toFloat()
                val remainder = mDegrees % 90

                if (remainder > 45)
                    toDegrees += 90f
                else if (remainder < -45)
                    toDegrees -= 90f

                transform.withRotate(mDegrees.toInt(), toDegrees.toInt())

                mDegrees = toDegrees
            }

            animaResetToBaseReferTarget(imageRect)

            transform.withFling(vx, vy)
            transform.start()

            return true
        }

        override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (transform.isRunning) transform.stop()
            if (swipeCloseController != null && swipeCloseController!!.onScroll(e1, e2, distanceX, distanceY)) return true

            if (canScrollHorizontallySelf(distanceX)) {
                var dx = distanceX
                if (dx < 0 && imageRect.left - dx > widgetRect.left) dx = imageRect.left
                if (dx > 0 && imageRect.right - dx < widgetRect.right) dx = imageRect.right - widgetRect.right

                animaMatrix.postTranslate(-dx, 0f)
                mTranslateX = (mTranslateX - dx).toInt()
            } else if (isImageWidthOverScreen || hasMultiTouch || isOverTranslate) {
                var dx = distanceX
                checkRect(tmpRect)
                if (!hasMultiTouch) {
                    if (dx < 0 && imageRect.left - dx > tmpRect.left)
                        dx = resistanceScrollBy(imageRect.left - tmpRect.left, dx)
                    if (dx > 0 && imageRect.right - dx < tmpRect.right)
                        dx = resistanceScrollBy(imageRect.right - tmpRect.right, dx)
                }
                animaMatrix.postTranslate(-dx, 0f)
                mTranslateX = (mTranslateX - dx).toInt()
                isOverTranslate = true
            }

            if (canScrollVerticallySelf(distanceY)) {
                var dy = distanceY
                if (dy < 0 && imageRect.top - dy > widgetRect.top) dy = imageRect.top
                if (dy > 0 && imageRect.bottom - dy < widgetRect.bottom) dy = imageRect.bottom - widgetRect.bottom

                animaMatrix.postTranslate(0f, -dy)
                mTranslateY = (mTranslateY - dy).toInt()
            } else if (isImageHeightOverScreen || hasMultiTouch || isOverTranslate) {
                var dy = distanceY
                checkRect(tmpRect)
                if (!hasMultiTouch) {
                    if (dy < 0 && imageRect.top - dy > tmpRect.top)
                        dy = resistanceScrollBy(imageRect.top - tmpRect.top, dy)
                    if (dy > 0 && imageRect.bottom - dy < tmpRect.bottom)
                        dy = resistanceScrollBy(imageRect.bottom - tmpRect.bottom, dy)
                }

                animaMatrix.postTranslate(0f, -dy)
                mTranslateY = (mTranslateY - dy).toInt()
                isOverTranslate = true
            }

            executeTranslate()

            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            removeCallbacks(mClickRunnable)

            transform.stop()

            var from = 1f
            var to = 1f

            val imgcx = imageRect.left + imageRect.width() / 2
            val imgcy = imageRect.top + imageRect.height() / 2

            scaleCenter.set(imgcx, imgcy)
            rotateCenter.set(imgcx, imgcy)

            mTranslateX = 0
            mTranslateY = 0

            if (isZoomUp) {
                from = mScale
                to = 1f
            } else {
                from = mScale
                to = maxSacleLevel

                scaleCenter.set(e.x, e.y)
            }

            tmpMatrix.reset()
            tmpMatrix.postTranslate(-baseRect.left, -baseRect.top)
            tmpMatrix.postTranslate(rotateCenter.x, rotateCenter.y)
            tmpMatrix.postTranslate(-baseRect.width() / 2f, -baseRect.height() / 2f)
            tmpMatrix.postRotate(mDegrees, rotateCenter.x, rotateCenter.y)
            tmpMatrix.postScale(to, to, scaleCenter.x, scaleCenter.y)
            tmpMatrix.postTranslate(mTranslateX.toFloat(), mTranslateY.toFloat())
            tmpMatrix.mapRect(tmpRect, baseRect)

            animaResetToBaseReferTarget(tmpRect)

            isZoomUp = !isZoomUp
            transform.withScale(from, to)
            transform.start()
            return true
        }
    }

    private val mRotateDetector = RotateGestureDetector(object : RotateListener {
        override fun onRotate(degrees: Float, focusX: Float, focusY: Float) {
            if (canRotate) {
                mDegrees += degrees
                animaMatrix.postRotate(degrees, focusX, focusY)
                executeTranslate()
            } else {
                mRotateFlag += degrees

                if (Math.abs(mRotateFlag) >= minRotateStep) {
                    canRotate = true
                    mRotateFlag = 0f
                }
            }
        }
    })

    private inner class ScaleListener : ScaleGestureDetector.OnScaleGestureListener {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean = true

        override fun onScaleEnd(detector: ScaleGestureDetector) = Unit

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val scaleFactor = detector.scaleFactor

            if (java.lang.Float.isNaN(scaleFactor) || java.lang.Float.isInfinite(scaleFactor)) return false

            mScale *= scaleFactor
            animaMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY())
            executeTranslate()

            return true
        }
    }

    protected val mClickRunnable = Runnable {
        _clickListener?.onClick(this@PhotoView)
    }

    interface ClipCalculate {
        fun calculateTop(): Float
    }

    protected inner class START : ClipCalculate {
        override fun calculateTop() = imageRect.top
    }

    protected inner class END : ClipCalculate {
        override fun calculateTop() = imageRect.bottom
    }

    protected inner class OTHER : ClipCalculate {
        override fun calculateTop() = (imageRect.top + imageRect.bottom) / 2
    }

    inner class Transform : Runnable {

        var isRunning = false

        val interpolatorDelegate = InterpolatorDelegate()

        val translateScroller = OverScroller(context, interpolatorDelegate)
        val flingScroller = OverScroller(context, interpolatorDelegate)
        val scaleScroller = Scroller(context, interpolatorDelegate)
        val clipScroller = Scroller(context, interpolatorDelegate)
        val rotateScroller = Scroller(context, interpolatorDelegate)

        val lastFling = Point()
        val lastTranslate = Point()
        val clipRect = RectF()

        var C: ClipCalculate? = null

        var completeBlock: (() -> Unit)? = null

        fun setInterpolator(interpolator: Interpolator) {
            interpolatorDelegate.target = interpolator
        }

        fun withTranslate(deltaX: Int, deltaY: Int) {
            lastTranslate.set(0, 0)
            translateScroller.startScroll(0, 0, deltaX, deltaY, animaDuring)
        }

        fun withScale(form: Float, to: Float)
                = scaleScroller.startScroll((form * 10000).toInt(), 0, ((to - form) * 10000).toInt(), 0, animaDuring)

        fun withClip(fromX: Float, fromY: Float, deltaX: Float, deltaY: Float, d: Int, c: ClipCalculate) {
            clipScroller.startScroll((fromX * 10000).toInt(), (fromY * 10000).toInt(), (deltaX * 10000).toInt(), (deltaY * 10000).toInt(), d)
            C = c
        }

        fun withRotate(fromDegrees: Int, toDegrees: Int)
                = rotateScroller.startScroll(fromDegrees, 0, toDegrees - fromDegrees, 0, animaDuring)

        fun withRotate(fromDegrees: Int, toDegrees: Int, during: Int)
                = rotateScroller.startScroll(fromDegrees, 0, toDegrees - fromDegrees, 0, during)

        fun withFling(velocityX: Float, velocityY: Float) {
            lastFling.x = if (velocityX < 0) Integer.MAX_VALUE else 0
            var distanceX = (if (velocityX > 0) Math.abs(imageRect.left) else imageRect.right - widgetRect.right).toInt()
            distanceX = if (velocityX < 0) Integer.MAX_VALUE - distanceX else distanceX
            var minX = if (velocityX < 0) distanceX else 0
            var maxX = if (velocityX < 0) Integer.MAX_VALUE else distanceX
            val overX = if (velocityX < 0) Integer.MAX_VALUE - minX else distanceX

            lastFling.y = if (velocityY < 0) Integer.MAX_VALUE else 0
            var distanceY = (if (velocityY > 0) Math.abs(imageRect.top) else imageRect.bottom - widgetRect.bottom).toInt()
            distanceY = if (velocityY < 0) Integer.MAX_VALUE - distanceY else distanceY
            var minY = if (velocityY < 0) distanceY else 0
            var maxY = if (velocityY < 0) Integer.MAX_VALUE else distanceY
            val overY = if (velocityY < 0) Integer.MAX_VALUE - minY else distanceY

            if (velocityX == 0f) {
                maxX = 0
                minX = 0
            }

            if (velocityY == 0f) {
                maxY = 0
                minY = 0
            }

            flingScroller.fling(lastFling.x, lastFling.y, velocityX.toInt(), velocityY.toInt(), minX, maxX, minY, maxY, if (Math.abs(overX) < maxFlingOverScroll * 2) 0 else maxFlingOverScroll, if (Math.abs(overY) < maxFlingOverScroll * 2) 0 else maxFlingOverScroll)
        }

        fun start() {
            isRunning = true
            postExecute()
        }

        fun stop() {
            translateScroller.abortAnimation()
            scaleScroller.abortAnimation()
            flingScroller.abortAnimation()
            rotateScroller.abortAnimation()
            isRunning = false
        }

        private fun postExecute() {
            if (isRunning) post(this)
        }

        override fun run() {
            var isAnimaFinish = true

            if (scaleScroller.computeScrollOffset()) {
                mScale = scaleScroller.currX / 10000f
                isAnimaFinish = false
            }

            if (translateScroller.computeScrollOffset()) {
                mTranslateX += translateScroller.currX - lastTranslate.x
                mTranslateY += translateScroller.currY - lastTranslate.y
                lastTranslate.set(translateScroller.currX, translateScroller.currY)
                isAnimaFinish = false
            }

            if (flingScroller.computeScrollOffset()) {
                mTranslateX += flingScroller.currX - lastFling.x
                mTranslateY += flingScroller.currY - lastFling.y
                lastFling.set(flingScroller.currX, flingScroller.currY)
                isAnimaFinish = false
            }

            if (rotateScroller.computeScrollOffset()) {
                mDegrees = rotateScroller.currX.toFloat()
                isAnimaFinish = false
            }

            if (clipScroller.computeScrollOffset() && C != null) {
                val sx = clipScroller.currX / 10000f
                val sy = clipScroller.currY / 10000f
                tmpMatrix.setScale(sx, sy, (imageRect.left + imageRect.right) / 2, C!!.calculateTop())
                tmpMatrix.mapRect(clipRect, imageRect)

                if (sx == 1f) {
                    clipRect.left = widgetRect.left
                    clipRect.right = widgetRect.right
                }

                if (sy == 1f) {
                    clipRect.top = widgetRect.top
                    clipRect.bottom = widgetRect.bottom
                }

                this@PhotoView.clipRect = this.clipRect
                isAnimaFinish = false
            }

            if (!isAnimaFinish) {
                applyTransform()
                postExecute()
            } else {
                isRunning = false

                // 修复动画结束后边距有些空隙，
                var needFix = false

                if (isImageWidthOverScreen) {
                    if (imageRect.left > 0) {
                        mTranslateX -= imageRect.left.toInt()
                    } else if (imageRect.right < widgetRect.width()) {
                        mTranslateX -= (widgetRect.width() - imageRect.right).toInt()
                    }
                    needFix = true
                }

                if (isImageHeightOverScreen) {
                    if (imageRect.top > 0) {
                        mTranslateY -= imageRect.top.toInt()
                    } else if (imageRect.bottom < widgetRect.height()) {
                        mTranslateY -= (widgetRect.height() - imageRect.bottom).toInt()
                    }
                    needFix = true
                }

                if (needFix) {
                    applyTransform()
                }

                invalidate()

                completeBlock?.invoke()
                completeBlock = null
            }
        }

        /**
         * baserect通过该步骤的变化，能得到当前的图像
         */
        protected fun applyTransform() {
            animaMatrix.reset()
            animaMatrix.postTranslate(-baseRect.left, -baseRect.top)
            animaMatrix.postTranslate(rotateCenter.x, rotateCenter.y)
            animaMatrix.postTranslate(-baseRect.width() / 2, -baseRect.height() / 2)
            animaMatrix.postRotate(mDegrees, rotateCenter.x, rotateCenter.y)
            animaMatrix.postScale(mScale, mScale, scaleCenter.x, scaleCenter.y)
            animaMatrix.postTranslate(mTranslateX.toFloat(), mTranslateY.toFloat())
            executeTranslate()
        }
    }

    inner class SwipeCloseController {

        var isFirstAction = true
        var handlerAction = false
        var startTranslateY = 0

        var closeListener: (() -> Unit)? = null

        fun onDown() {
            isFirstAction = true
            handlerAction = false
            startTranslateY = mTranslateY
        }

        fun onUp(): Boolean {
            if (handlerAction) closeListener?.invoke()
            return handlerAction
        }

        fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

            return false
        }

        // distanceY < 0  向下滑动
        // distanceY > 0  向上滑动
        fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            // if (mScale > 1f) return false // 已经放大了的情况下不支持滑动关闭

            if (hasMultiTouch) {
                handlerAction = false
                return false
            }

            if (!isFirstAction && !handlerAction) return false

//            if (isFirstAction && !handlerAction && distanceY < 0
//                    && ((isImageHeightOverScreen && imageRect.top <= 1f) || (!isImageHeightOverScreen))) {
//                handlerAction = true
//            }

            if (isFirstAction && !handlerAction && abs(distanceX) < abs(distanceY)) {
                if (distanceY < 0 && imageRect.top >= 0) handlerAction = true
                else if (distanceY > 0 && imageRect.bottom <= widgetRect.bottom) handlerAction = true
            }

            if (handlerAction) {
                var dx = distanceX
                val dy = distanceY

                checkRect(tmpRect)

                if (dx < 0 && imageRect.left - dx > tmpRect.left)
                    dx = resistanceScrollBy(imageRect.left - tmpRect.left, dx, (maxOverScrollResistance * 0.8f).toInt())
                if (dx > 0 && imageRect.right - dx < tmpRect.right)
                    dx = resistanceScrollBy(imageRect.right - tmpRect.right, dx, (maxOverScrollResistance * 0.8f).toInt())

                mTranslateX = (mTranslateX - dx).toInt()
                mTranslateY = (mTranslateY - dy).toInt()

                animaMatrix.postTranslate(-dx, -dy)

                val scrolled = Math.abs(mTranslateY - startTranslateY)

                var scale = 1.0f

                if (scrolled < maxSwipeCloseDistance) {
                    scale = 1 - Math.min((scrolled / maxSwipeCloseDistance.toFloat()), 1f) * swipeMinScaleDownLevel

                    val deltaScale = 1 - (mScale - scale)

                    val imgcx = imageRect.left + imageRect.width() / 2
                    val imgcy = imageRect.top + imageRect.height() / 2

                    mScale = scale

                    animaMatrix.postScale(deltaScale, deltaScale, imgcx, imgcy)
                }

                executeTranslate()
            }

            isFirstAction = false

            return handlerAction
        }
    }

    fun getPhotoInfo(): PhotoInfo {
        val screenRect = RectF()
        val p = IntArray(2)
        getLocation(this, p)
        screenRect.set(p[0] + imageRect.left, p[1] + imageRect.top, p[0] + imageRect.right, p[1] + imageRect.bottom)

        return PhotoInfo(
                mScaleType!!,
                extendType,
                mDegrees,
                mScale,
                mTranslateX,
                mTranslateY,
                screenRect,
                widgetRect,
                imageRect)
    }

    protected fun reset() {
        animaMatrix.reset()
        executeTranslate()
        mScale = 1f
        mTranslateX = 0
        mTranslateY = 0
    }

    /**
     * 在PhotoView内部还没有图片的时候同样可以调用该方法
     * 此时并不会播放动画，当给PhotoView设置图片后会自动播放动画。
     * 若等待时间过长也没有给控件设置图片，则会忽略该动画，若要再次播放动画则需要重新调用该方法
     * (等待的时间默认500毫秒，可以通过setMaxAnimFromWaiteTime(int)设置最大等待时间)
     */
    fun animaFrom(info: PhotoInfo) {
        if (isInit) {
            reset()

            val mine = getPhotoInfo()

            val scaleX = info.imageRect.width() / mine.imageRect.width()
            val scaleY = info.imageRect.height() / mine.imageRect.height()
            val scale = if (scaleX < scaleY) scaleX else scaleY

            val ocx = info.screenRect.left + info.screenRect.width() / 2
            val ocy = info.screenRect.top + info.screenRect.height() / 2

            val mcx = mine.screenRect.left + mine.screenRect.width() / 2
            val mcy = mine.screenRect.top + mine.screenRect.height() / 2

            animaMatrix.reset()
            animaMatrix.postTranslate(ocx - mcx, ocy - mcy)
            animaMatrix.postScale(scale, scale, ocx, ocy)
            animaMatrix.postRotate(info.degrees, ocx, ocy)
            executeTranslate()

            scaleCenter.set(ocx, ocy)
            rotateCenter.set(ocx, ocy)

            transform.withTranslate((-(ocx - mcx)).toInt(), (-(ocy - mcy)).toInt())
            transform.withScale(scale, 1f)
            transform.withRotate(info.degrees.toInt(), 0)

            if (info.widgetRect.width() < info.imageRect.width() || info.widgetRect.height() < info.imageRect.height()) {
                var clipX = info.widgetRect.width() / info.imageRect.width()
                var clipY = info.widgetRect.height() / info.imageRect.height()
                clipX = if (clipX > 1) 1f else clipX
                clipY = if (clipY > 1) 1f else clipY

                val c = when {
                    info.scaleType == ImageView.ScaleType.FIT_START -> START()
                    info.scaleType == ImageView.ScaleType.FIT_END -> END()
                    else -> OTHER()
                }

                transform.withClip(clipX, clipY, 1 - clipX, 1 - clipY, animaDuring / 3, c)

                tmpMatrix.setScale(clipX, clipY, (imageRect.left + imageRect.right) / 2, c.calculateTop())
                tmpMatrix.mapRect(transform.clipRect, imageRect)
                clipRect = transform.clipRect
            }

            transform.start()
        } else {
            fromInfo = info
        }
    }

    fun animaTo(to: PhotoInfo, complete: (() -> Unit)? = null) {
        if (!isInit) return

        transform.stop()

        mTranslateX = 0
        mTranslateY = 0

        val tcx = to.screenRect.left + to.screenRect.width() / 2
        val tcy = to.screenRect.top + to.screenRect.height() / 2

        scaleCenter.set(imageRect.left + imageRect.width() / 2, imageRect.top + imageRect.height() / 2)
        rotateCenter.set(scaleCenter)

        // 将图片旋转回正常位置，用以计算
        animaMatrix.postRotate(-mDegrees, scaleCenter.x, scaleCenter.y)
        animaMatrix.mapRect(imageRect, baseRect)

        val scaleX = to.imageRect.width() / baseRect.width()
        val scaleY = to.imageRect.height() / baseRect.height()
        val scale = max(scaleX, scaleY)

        animaMatrix.postRotate(mDegrees, scaleCenter.x, scaleCenter.y)
        animaMatrix.mapRect(imageRect, baseRect)

        mDegrees %= 360

        transform.withTranslate((tcx - scaleCenter.x).toInt(), (tcy - scaleCenter.y).toInt())
        transform.withScale(mScale, scale)
        transform.withRotate(mDegrees.toInt(), to.degrees.toInt(), animaDuring * 2 / 3)

        if (to.widgetRect.width() < to.screenRect.width() || to.widgetRect.height() < to.screenRect.height()) {
            var clipX = to.widgetRect.width() / to.screenRect.width()
            var clipY = to.widgetRect.height() / to.screenRect.height()
            if (clipX > 1) clipX = 1f
            if (clipY > 1) clipY = 1f

            val c = when (to.scaleType) {
                ScaleType.FIT_START -> START()
                ScaleType.FIT_END -> END()
                else -> OTHER()
            }

            postDelayed(
                    { transform.withClip(1f, 1f, -1 + clipX, -1 + clipY, animaDuring / 2, c) },
                    (animaDuring / 2).toLong()
            )
        }

        transform.completeBlock = complete
        transform.start()
    }

    companion object {
        // 图片真实带下小于控件大小，则使用center, 若图大小大于控件大小，使用 fit_center_*
        val CUSTOM_TYPE_CENTER_FIT_START = -100
        val CUSTOM_TYPE_CENTER_FIT_END = -101
        // 将图片center_crop -> fit_start
        val CUSTOM_TYPE_FIT_CENTER_START = -102
        val CUSTOM_TYPE_FIT_CENTER_END = -103
        // 图片真实带下小于控件大小，则使用center_crop, 若图大小大于控件大小，使用 fit_center_*
        val CUSTOM_TYPE_CENTER_CROP_START = -104
        val CUSTOM_TYPE_CENTER_CROP_END = -105

        var REFRESH_RATE = (1000 / 60f).toLong()

        /**
         * 匹配两个Rect确定有无重叠部分的公共部分，用以判断图片是否已经超出控件大小
         * 若无公共部分，则输出0.0.0.0
         */
        private fun mapRect(r1: RectF, r2: RectF, out: RectF) {
            val l: Float = if (r1.left > r2.left) r1.left else r2.left
            val r: Float = if (r1.right < r2.right) r1.right else r2.right

            if (l > r) {
                out.set(0f, 0f, 0f, 0f)
                return
            }

            val t: Float = if (r1.top > r2.top) r1.top else r2.top
            val b: Float = if (r1.bottom < r2.bottom) r1.bottom else r2.bottom

            if (t > b) {
                out.set(0f, 0f, 0f, 0f)
                return
            }

            out.set(l, t, r, b)
        }

        private fun getLocation(target: View, position: IntArray) {

            position[0] += target.left
            position[1] += target.top

            var viewParent = target.parent
            while (viewParent is View) {
                val view = viewParent as View

                if (view.id == android.R.id.content) return

                position[0] -= view.scrollX
                position[1] -= view.scrollY

                position[0] += view.left
                position[1] += view.top

                viewParent = view.parent
            }

            position[0] = (position[0] + 0.5f).toInt()
            position[1] = (position[1] + 0.5f).toInt()
        }

        fun getDrawableSize(drawable: Drawable): Pair<Int, Int> {
            var width = drawable.minimumWidth
            var height = drawable.minimumHeight
            if (width == 0) width = drawable.bounds.width()
            if (height == 0) height = drawable.bounds.height()
            return Pair<Int, Int>(width, height)
        }

        fun getImageViewInfo(imgView: ImageView): PhotoInfo {
            val p = IntArray(2)
            getLocation(imgView, p)

            val drawable = imgView.drawable

            val matrix = imgView.imageMatrix

            val (width, height) = getDrawableSize(drawable)

            val imgRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            matrix.mapRect(imgRect)

            val screenRect = RectF(p[0] + imgRect.left, p[1] + imgRect.top, p[0] + imgRect.right, p[1] + imgRect.bottom)
            val widgetRect = RectF(0f, 0f, imgView.width.toFloat(), imgView.height.toFloat())

            return PhotoInfo(
                    imgView.scaleType,
                    null,
                    0f,
                    1.0f,
                    0,
                    0,
                    screenRect,
                    widgetRect,
                    imgRect)
        }
    }
}
