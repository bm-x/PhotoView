package com.example.bm.photoview;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.OverScroller;
import android.widget.Scroller;

/**
 * Created by bm on 2015/6/21.
 */
public class PhotoView extends ImageView {

    private final static int ANIMA_DURING = 300;
    private final static float MAX_SCALE = 2.5f;
    private int MAX_OVER_SCROLL = 0;
    private int MAX_FLING_OVER_SCROLL = 0;

    private Matrix mBaseMatrix = new Matrix();
    private Matrix mAnimaMatrix = new Matrix();
    private Matrix mSynthesisMatrix = new Matrix();
    private Matrix mTmpMatrix = new Matrix();

    private GestureDetector mDetector;
    private ScaleGestureDetector mScaleDetector;

    private boolean hasMultiTouch;
    private boolean hasDrawable;
    private boolean isKnowSize;
    private boolean hasOverTranslate;

    private boolean imgLargeWidth;
    private boolean imgLargeHeight;

    private float mScale = 1.0f;
    private int mTranslateX;
    private int mTranslateY;

    private RectF mWidgetRect = new RectF();
    private RectF mBaseRect = new RectF();
    private RectF mImgRect = new RectF();
    private RectF mTmpRect = new RectF();

    private PointF mScreenCenter = new PointF();
    private PointF mDoubleTab = new PointF();

    private Transform mTranslate = new Transform();

    private float[] mValues = new float[16];

    public PhotoView(Context context) {
        super(context);
        init();
    }

    public PhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        mDetector = new GestureDetector(getContext(), mGestureListener);
        mScaleDetector = new ScaleGestureDetector(getContext(), mScaleListener);
        MAX_OVER_SCROLL = (int) (getResources().getDisplayMetrics().density * 30);
        MAX_FLING_OVER_SCROLL = (int) (getResources().getDisplayMetrics().density * 30);
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        super.setScaleType(ScaleType.MATRIX);
    }

    @Override
    public void setImageResource(int resId) {
        setImageDrawable(getResources().getDrawable(resId));
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);

        if (!hasDrawable) {
            hasDrawable = true;
        }

        doInit();
    }

    private void doInit() {
        if (!hasDrawable) return;
        if (!isKnowSize) return;

        mBaseMatrix.reset();

        Drawable img = getDrawable();

        int w = getWidth();
        int h = getHeight();
        int imgw = img.getIntrinsicWidth();
        int imgh = img.getIntrinsicHeight();

        mBaseRect.set(0, 0, imgw, imgh);

        // 以图片中心点居中位移
        int tx = (w - imgw) / 2;
        int ty = (h - imgh) / 2;

        float sx = 1;
        float sy = 1;

        // 缩放，默认不超过屏幕大小
        if (imgw > w) {
            sx = (float) w / imgw;
        }

        if (imgh > h) {
            sy = (float) h / imgh;
        }

        float scale = sx < sy ? sx : sy;

        mBaseMatrix.reset();
        mBaseMatrix.postTranslate(tx, ty);
        mBaseMatrix.postScale(scale, scale, mScreenCenter.x, mScreenCenter.y);
        mBaseMatrix.mapRect(mBaseRect);

        executeTranslate();
    }

    private void executeTranslate() {
        mSynthesisMatrix.set(mBaseMatrix);
        mSynthesisMatrix.postConcat(mAnimaMatrix);
        setImageMatrix(mSynthesisMatrix);

        mAnimaMatrix.mapRect(mImgRect, mBaseRect);

        imgLargeWidth = mImgRect.width() > mWidgetRect.width();
        imgLargeHeight = mImgRect.height() > mWidgetRect.height();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidgetRect.set(0, 0, w, h);
        mScreenCenter.set(w / 2, h / 2);

        if (!isKnowSize) {
            isKnowSize = true;
            doInit();
        }
    }


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        mDetector.onTouchEvent(event);
        mScaleDetector.onTouchEvent(event);
        final int Action = event.getAction();
        if (Action == MotionEvent.ACTION_UP || Action == MotionEvent.ACTION_CANCEL) onUp(event);
        return true;
    }

    private void onUp(MotionEvent ev) {
        if (mTranslate.isRuning) return;

        float scale = mScale;
        if (mScale < 1) {
            scale = 1;
            mTranslate.withScale(mScale, 1);
        } else if (mScale > MAX_SCALE) {
            scale = MAX_SCALE;
            mTranslate.withScale(mScale, MAX_SCALE);
        }

        mAnimaMatrix.getValues(mValues);
        float s = mValues[Matrix.MSCALE_X];
        float tx = mValues[Matrix.MTRANS_X];
        float ty = mValues[Matrix.MTRANS_Y];

        float cpx = tx - mTranslateX;
        float cpy = ty - mTranslateY;

        mDoubleTab.x = -cpx / (s - 1);
        mDoubleTab.y = -cpy / (s - 1);

        mTmpRect.set(mImgRect);

        if (scale != mScale) {
            mTmpMatrix.setScale(scale, scale, mDoubleTab.x, mDoubleTab.y);
            mTmpMatrix.postTranslate(mTranslateX, mTranslateY);
            mTmpMatrix.mapRect(mTmpRect, mBaseRect);
        }

        doTranslateReset(mTmpRect);
        mTranslate.start();
    }

    private void doTranslateReset(RectF imgRect) {
        int tx = 0;
        int ty = 0;

        if (imgRect.width() < mWidgetRect.width()) {
            if (!isImageCenterWidth())
                tx = -(int) ((mWidgetRect.width() - imgRect.width()) / 2 - imgRect.left);
        } else {
            if (imgRect.left > mWidgetRect.left) {
                tx = (int) (imgRect.left - mWidgetRect.left);
            } else if (imgRect.right < mWidgetRect.right) {
                tx = (int) (imgRect.right - mWidgetRect.right);
            }
        }

        if (imgRect.height() < mWidgetRect.height()) {
            if (!isImageCenterHeight())
                ty = -(int) ((mWidgetRect.height() - imgRect.height()) / 2 - imgRect.top);
        } else {
            if (imgRect.top > mWidgetRect.top) {
                ty = (int) (imgRect.top - mWidgetRect.top);
            } else if (imgRect.bottom < mWidgetRect.bottom) {
                ty = (int) (imgRect.bottom - mWidgetRect.bottom);
            }
        }

        if (tx != 0 || ty != 0) {
            if (!mTranslate.mFlingScroller.isFinished()) mTranslate.mFlingScroller.abortAnimation();
            mTranslate.withTranslate(mTranslateX, mTranslateY, -tx, -ty);
        }
    }

    private boolean isImageCenterHeight() {
        return Math.round(mImgRect.top) == (mWidgetRect.height() - mImgRect.height()) / 2;
    }

    private boolean isImageCenterWidth() {
        return Math.round(mImgRect.left) == (mWidgetRect.width() - mImgRect.width()) / 2;
    }


    private ScaleGestureDetector.OnScaleGestureListener mScaleListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();

            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor))
                return false;

            mScale *= scaleFactor;
            mDoubleTab.set(detector.getFocusX(), detector.getFocusY());
            mAnimaMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            executeTranslate();

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            hasMultiTouch = true;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onDown(MotionEvent e) {
            hasOverTranslate = false;
            hasMultiTouch = false;
            return super.onDown(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (hasMultiTouch) return false;
            if (!imgLargeWidth && !imgLargeHeight) return false;
            if (!mTranslate.mFlingScroller.isFinished()) return false;

            float vx = velocityX;
            float vy = velocityY;

            if (Math.round(mImgRect.left) >= mWidgetRect.left || Math.round(mImgRect.right) <= mWidgetRect.right) {
//                if (imgLargeWidth) return false;
//                else
                vx = 0;
            }

            if (Math.round(mImgRect.top) >= mWidgetRect.top || Math.round(mImgRect.bottom) <= mWidgetRect.bottom) {
//                if (imgLargeHeight) return false;
//                else
                vy = 0;
            }

            doTranslateReset(mImgRect);
            mTranslate.withFling(vx, vy);
            mTranslate.start();

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (mTranslate.isRuning) {
                mTranslate.stop();
            }

            if (canScrollHorizontallySelf(distanceX)) {
                if (distanceX < 0 && mImgRect.left - distanceX > mWidgetRect.left)
                    distanceX = mImgRect.left;
                if (distanceX > 0 && mImgRect.right - distanceX < mWidgetRect.right)
                    distanceX = mImgRect.right - mWidgetRect.right;

                mAnimaMatrix.postTranslate(-distanceX, 0);
                mTranslateX -= distanceX;
            } else if (imgLargeWidth || hasMultiTouch || hasOverTranslate) {
                mAnimaMatrix.postTranslate(-distanceX, 0);
                mTranslateX -= distanceX;
                hasOverTranslate = true;
            }

            if (canScrollVerticallySelf(distanceY)) {
                if (distanceY < 0 && mImgRect.top - distanceY > mWidgetRect.top)
                    distanceY = mImgRect.top;
                if (distanceY > 0 && mImgRect.bottom - distanceY < mWidgetRect.bottom)
                    distanceY = mImgRect.bottom - mWidgetRect.bottom;

                mAnimaMatrix.postTranslate(0, -distanceY);
                mTranslateY -= distanceY;
            } else if (imgLargeHeight || hasOverTranslate || hasMultiTouch) {
                mAnimaMatrix.postTranslate(0, -distanceY);
                mTranslateY -= distanceY;
                hasOverTranslate = true;
            }

            executeTranslate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mTranslate.stop();

            float from = 1;
            float to = 1;

            if (mScale == 1) {
                from = 1;
                to = MAX_SCALE;

                if (mImgRect.width() < mWidgetRect.width())
                    mDoubleTab.set(mScreenCenter.x, mScreenCenter.y);
                else
                    mDoubleTab.set(e.getX(), mScreenCenter.y);
            } else {
                from = mScale;
                to = 1;

                mTranslate.withTranslate(mTranslateX, mTranslateY, -mTranslateX, -mTranslateY);
            }

            mTranslate.withScale(from, to);
            mTranslate.start();

            return false;
        }
    };

    public boolean canScrollHorizontallySelf(float direction) {
        if (mImgRect.width() <= mWidgetRect.width()) return false;
        if (direction < 0 && Math.round(mImgRect.left) - direction >= mWidgetRect.left)
            return false;
        if (direction > 0 && Math.round(mImgRect.right) - direction <= mWidgetRect.right)
            return false;
        return true;
    }

    public boolean canScrollVerticallySelf(float direction) {
        if (mImgRect.height() <= mWidgetRect.height()) return false;
        if (direction < 0 && Math.round(mImgRect.top) - direction >= mWidgetRect.top) return false;
        if (direction > 0 && Math.round(mImgRect.bottom) - direction <= mWidgetRect.bottom)
            return false;
        return true;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
//        if (hasTranslate) return false;
        return canScrollHorizontallySelf(direction);
    }

    @Override
    public boolean canScrollVertically(int direction) {
//        if (hasTranslate) return false;
        return canScrollVerticallySelf(direction);
    }

    private class Transform implements Runnable {

        boolean isRuning;

        OverScroller mTranslateScroller;
        OverScroller mFlingScroller;
        Scroller mScaleScroller;

        int mLastFlingX;
        int mLastFlingY;

        int mLastTranslateX;
        int mLastTranslateY;

        Transform() {
            mTranslateScroller = new OverScroller(getContext(), new DecelerateInterpolator());
            mScaleScroller = new Scroller(getContext(), new DecelerateInterpolator());
            mFlingScroller = new OverScroller(getContext(), new DecelerateInterpolator());
        }

        void withTranslate(int startX, int startY, int deltaX, int deltaY) {
            mLastTranslateX = 0;
            mLastTranslateY = 0;
            mTranslateScroller.startScroll(0, 0, deltaX, deltaY, ANIMA_DURING);
        }

        void withScale(float form, float to) {
            mScaleScroller.startScroll((int) (form * 10000), 0, (int) ((to - form) * 10000), 0, ANIMA_DURING);
        }

        void withFling(float velocityX, float velocityY) {
            mLastFlingX = velocityX < 0 ? Integer.MAX_VALUE : 0;
            int distanceX = (int) (velocityX > 0 ? Math.abs(mImgRect.left) : mImgRect.right - mWidgetRect.right);
            distanceX = velocityX < 0 ? Integer.MAX_VALUE - distanceX : distanceX;
            int minX = velocityX < 0 ? distanceX : 0;
            int maxX = velocityX < 0 ? Integer.MAX_VALUE : distanceX;
            int overX = velocityX < 0 ? Integer.MAX_VALUE - minX : distanceX;


            mLastFlingY = velocityY < 0 ? Integer.MAX_VALUE : 0;
            int distanceY = (int) (velocityY > 0 ? Math.abs(mImgRect.top) : mImgRect.bottom - mWidgetRect.bottom);
            distanceY = velocityY < 0 ? Integer.MAX_VALUE - distanceY : distanceY;
            int minY = velocityY < 0 ? distanceY : 0;
            int maxY = velocityY < 0 ? Integer.MAX_VALUE : distanceY;
            int overY = velocityY < 0 ? Integer.MAX_VALUE - minY : distanceY;

            if (velocityX == 0) {
                maxX = 0;
                minX = 0;
            }

            if (velocityY == 0) {
                maxY = 0;
                minY = 0;
            }

            mFlingScroller.fling(mLastFlingX, mLastFlingY, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY, Math.abs(overX) < MAX_FLING_OVER_SCROLL * 2 ? 0 : MAX_FLING_OVER_SCROLL, Math.abs(overY) < MAX_FLING_OVER_SCROLL * 2 ? 0 : MAX_FLING_OVER_SCROLL);
        }

        void start() {
            isRuning = true;
            post(this);
        }

        void stopSelf() {
            removeCallbacks(this);
        }

        void stop() {
            removeCallbacks(this);
            mTranslateScroller.abortAnimation();
            mScaleScroller.abortAnimation();
            mFlingScroller.abortAnimation();

            isRuning = false;
        }

        @Override
        public void run() {

            if (!isRuning) return;

            boolean endAnima = true;

            if (mScaleScroller.computeScrollOffset()) {
                mScale = mScaleScroller.getCurrX() / 10000f;
                endAnima = false;
            }

            if (mTranslateScroller.computeScrollOffset()) {
                int tx = mTranslateScroller.getCurrX() - mLastTranslateX;
                int ty = mTranslateScroller.getCurrY() - mLastTranslateY;
                mTranslateX += tx;
                mTranslateY += ty;
                mLastTranslateX = mTranslateScroller.getCurrX();
                mLastTranslateY = mTranslateScroller.getCurrY();
                endAnima = false;
            }

            if (mFlingScroller.computeScrollOffset()) {
                int x = mFlingScroller.getCurrX() - mLastFlingX;
                int y = mFlingScroller.getCurrY() - mLastFlingY;

                mLastFlingX = mFlingScroller.getCurrX();
                mLastFlingY = mFlingScroller.getCurrY();

                mTranslateX += x;
                mTranslateY += y;
                endAnima = false;
            }

            if (!endAnima) {
                mAnimaMatrix.reset();
                mAnimaMatrix.postScale(mScale, mScale, mDoubleTab.x, mDoubleTab.y);
                mAnimaMatrix.postTranslate(mTranslateX, mTranslateY);
                executeTranslate();
                post(this);
            } else {
                isRuning = false;
            }
        }
    }
}