package com.example.bm.photoview;

import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bm on 2015/6/21.
 */
public class PhotoView extends ImageView {

    private final static float MAX_SCALE = 2.5f;

    private Matrix mBaseMatrix = new Matrix();
    private Matrix mAnimaMatrix = new Matrix();
    private Matrix mSynthesisMatrix = new Matrix();
    private Matrix mTmpMatrix = new Matrix();

    private GestureDetector mDetector;
    private ScaleGestureDetector mScaleDetector;

    private boolean hasDrawable;
    private boolean isKnowSize;

    private boolean imgLargeWidth;
    private boolean imgLargeHeight;
    private boolean moved;

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
        mDetector = new GestureDetector(getContext(), mGestureListener);
        mScaleDetector = new ScaleGestureDetector(getContext(), mScaleListener);
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

        imgLargeWidth = mImgRect.width() >= mWidgetRect.width();
        imgLargeHeight = mImgRect.height() >= mWidgetRect.height();
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
        return true;
    }

    private ScaleGestureDetector.OnScaleGestureListener mScaleListener = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return false;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {

        }
    };

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (!imgLargeWidth && !imgLargeHeight) return true;

//            velocityX = velocityX / 4000;
//            velocityY = velocityY / 4000;
//
//            float distanceX = velocityX * DURATION_FLING;
//            float distanceY = velocityY * DURATION_FLING;
//
//            List<PropertyValuesHolder> animasHolder = new ArrayList<>();
//
//            if (canScrollWidth) {
//                if (mImgTranslateX + distanceX >= 0) distanceX = -mImgTranslateX;
//                if (mWidth - mImgTranslateX - distanceX >= mImgScaleWidth)
//                    distanceX = -(mImgScaleWidth - mWidth + mImgTranslateX);
//
//                PropertyValuesHolder translateX = PropertyValuesHolder.ofFloat("translateX", mTranslateX, mTranslateX + distanceX);
//                animasHolder.add(translateX);
//            }
//
//            if (canScrollHeight) {
//                if (mImgTranslateY - distanceY >= 0) distanceY = mImgTranslateY;
//                if (mHeight - mImgTranslateY + distanceY >= mImgScaleHeight)
//                    distanceY = mImgScaleHeight - mHeight + mImgTranslateY;
//
//                PropertyValuesHolder translateY = PropertyValuesHolder.ofFloat("translateY", mTranslateY, mTranslateY + distanceY);
//                animasHolder.add(translateY);
//            }
//
//            mAnimator.setValues(animasHolder.toArray(new PropertyValuesHolder[0]));
//            mAnimator.setTarget(PhotoView.this);
//            mAnimator.setInterpolator(new DecelerateInterpolator());
//            mAnimator.setDuration(DURATION_FLING);
//            mAnimator.start();

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            boolean handScroll = false;

            if (mTranslate.isRuning) {
                mTranslate.stop();
            }

            if (canScrollHorizontally(distanceX)) {
                if (distanceX < 0 && mImgRect.left - distanceX > 0) distanceX = mImgRect.left;
                if (distanceX > 0 && mImgRect.right - distanceX < mWidgetRect.right)
                    distanceX = mImgRect.right - mWidgetRect.right;

                mAnimaMatrix.postTranslate(-distanceX, 0);
                handScroll = true;
                mTranslateX += distanceX;
            }

            if (canScrollVertically(distanceY)) {
                if (distanceY > 0 && mImgRect.top - distanceY > 0) distanceY = mImgRect.top;
                if (distanceY < 0 && mImgRect.bottom - distanceY < mWidgetRect.bottom)
                    distanceY = mImgRect.bottom - mWidgetRect.bottom;

                mAnimaMatrix.postTranslate(0, -distanceY);
                handScroll = true;
                mTranslateY += distanceY;
            }

            if (handScroll) executeTranslate();

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {

            float from = 1;
            float to = 1;

            if (mScale == 1) {
                from = 1;
                to = MAX_SCALE;
                mDoubleTab.set(e.getX(), mScreenCenter.y);
            } else {
                from = mScale;
                to = 1;

                mTranslate.withTranslate(-mTranslateX, -mTranslateY, mTranslateX, mTranslateY);
            }

            mTranslate.withScale(from, to);
            mTranslate.start();

//            List<PropertyValuesHolder> animasHolder = new ArrayList<>();
//
//            PropertyValuesHolder scale = PropertyValuesHolder.ofFloat("scale", from, to);
//            animasHolder.add(scale);
//
//            if (hasTranslateX) {
//                hasTranslateX = false;
//                PropertyValuesHolder translateX = PropertyValuesHolder.ofFloat("translateX", mTranslateX, 0);
//                animasHolder.add(translateX);
//            }
//
//            if (hasTranslateY) {
//                hasTranslateY = false;
//                PropertyValuesHolder translateY = PropertyValuesHolder.ofFloat("translateY", mTranslateY, 0);
//                animasHolder.add(translateY);
//            }
//
//            mAnimator.setValues(animasHolder.toArray(new PropertyValuesHolder[0]));
//            mAnimator.setTarget(PhotoView.this);
//            mAnimator.setDuration(DURATION_SCALE);
//            mAnimator.start();

            return false;
        }
    };

    private PointF calculateTranslate() {
        PointF p = new PointF();


        return p;
    }

    private PointF getImgTranslate() {
        PointF p = new PointF();
        mAnimaMatrix.getValues(mValues);
        p.x = mValues[2];
        p.y = mValues[5];
        return p;
    }

    public boolean canScrollHorizontally(float direction) {
        if (mImgRect.width() < mWidgetRect.width()) return false;
        if (direction < 0 && mImgRect.left >= 0) return false;
        if (direction > 0 && mImgRect.right <= mWidgetRect.right) return false;
        return true;
    }

    public boolean canScrollVertically(float direction) {
        if (mImgRect.height() < mWidgetRect.height()) return false;
        if (direction < 0 && mImgRect.top >= 0) return false;
        if (direction > 0 && mImgRect.bottom <= mWidgetRect.bottom) return false;
        return true;
    }

    @Override
    public boolean canScrollHorizontally(int direction) {
        return canScrollHorizontally(direction);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return canScrollVertically(direction);
    }

    private class Transform implements Runnable {

        boolean isRuning;

        OverScroller mTranslateScroller;
        Scroller mScaleScroller;

        Transform() {
            mTranslateScroller = new OverScroller(getContext(), new DecelerateInterpolator());
            mScaleScroller = new Scroller(getContext(), new DecelerateInterpolator());
        }

        void withTranslate(int startX, int startY, int deltaX, int deltaY) {
            mTranslateScroller.startScroll(startX, startY, deltaX, deltaY);
        }

        void withScale(float form, float to) {
            mScaleScroller.startScroll((int) (form * 10000), 0, (int) ((to - form) * 10000), 0);
        }

        void withFling(float velocityX, float velocityY) {

        }

        void start() {
            isRuning = true;
            postOnAnimation(this);
        }

        void stopSelf() {
            removeCallbacks(this);
        }

        public void stop() {
            removeCallbacks(this);
            mTranslateScroller.abortAnimation();
            mScaleScroller.abortAnimation();
            isRuning = false;
        }

        @Override
        public void run() {

            boolean endAnima = true;

            if (mScaleScroller.computeScrollOffset()) {
                mScale = mScaleScroller.getCurrX() / 10000f;
                endAnima = false;
            }

            if (mTranslateScroller.computeScrollOffset()) {
                mTranslateX = mTranslateScroller.getCurrX();
                mTranslateY = mTranslateScroller.getCurrY();
                endAnima = false;
            }

            if (!endAnima) {
                mAnimaMatrix.reset();
                mAnimaMatrix.postScale(mScale, mScale, mDoubleTab.x, mDoubleTab.y);
                mAnimaMatrix.postTranslate(mTranslateX, mTranslateY);
                executeTranslate();
                postOnAnimation(this);
            } else {
                isRuning = false;
            }
        }
    }
}