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
import android.widget.ScrollView;
import android.widget.Scroller;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bm on 2015/6/21.
 */
public class PhotoView extends ImageView {

    private final static float MAX_SCALE = 2.5f;
    private int MAX_OVER_SCROLL = 0;
    private int MAX_FLING_OVER_SCROLL = 0;

    private Matrix mBaseMatrix = new Matrix();
    private Matrix mAnimaMatrix = new Matrix();
    private Matrix mSynthesisMatrix = new Matrix();
    private Matrix mTmpMatrix = new Matrix();

    private GestureDetector mDetector;
    private ScaleGestureDetector mScaleDetector;

    private boolean hasDrawable;
    private boolean isKnowSize;
    private boolean hasTranslate;

    private boolean imgLargeWidth;
    private boolean imgLargeHeight;
    private boolean moved;

    private float mScale = 1.0f;
    private int mTranslateX;
    private int mTranslateY;
    private float mOverTranslateX;
    private float mOverTranslateY;

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
        MAX_OVER_SCROLL = (int) (getResources().getDisplayMetrics().density * 30);
        MAX_FLING_OVER_SCROLL = (int) (getResources().getDisplayMetrics().density * 20);
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
        final int Action = event.getAction();
        if (Action == MotionEvent.ACTION_UP || Action == MotionEvent.ACTION_CANCEL) onUp(event);
        return true;
    }

    private void onUp(MotionEvent ev) {
        mOverTranslateX = 0;
        mOverTranslateY = 0;
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
        public boolean onDown(MotionEvent e) {
            hasTranslate = false;
            return super.onDown(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (!imgLargeWidth && !imgLargeHeight) return false;
            if (!mTranslate.mFlingScroller.isFinished()) return false;

            mTranslate.withFling(velocityX, velocityY);
            mTranslate.start();

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            boolean handScroll = false;

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
                handScroll = true;
//                hasTranslate = true;
            }

            if (canScrollVerticallySelf(distanceY)) {
                if (distanceY > 0 && mImgRect.top - distanceY > mWidgetRect.left)
                    distanceY = mImgRect.top;
                if (distanceY < 0 && mImgRect.bottom - distanceY < mWidgetRect.bottom)
                    distanceY = mImgRect.bottom - mWidgetRect.bottom;

                mAnimaMatrix.postTranslate(0, -distanceY);
                handScroll = true;
                mTranslateY += distanceY;
//                hasTranslate = true;
            }

            if (handScroll) executeTranslate();
//            else if (hasTranslate) onOverScroll(-distanceX, -distanceY);

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

                mTranslate.withTranslate(mTranslateX, mTranslateY, -mTranslateX, -mTranslateY);
            }

            mTranslate.withScale(from, to);
            mTranslate.start();

            return false;
        }
    };

    private void onOverScroll(float distanceX, float distanceY) {
//        float dx = 0;
//        float dy = 0;
//
//        if (mOverTranslateX < 0 && distanceX > 0) {
//            if (mOverTranslateX - distanceX >= 0) dx = mOverTranslateX;
//            else dx = distanceX;
//        } else {
//            dx = distanceX * Math.abs((Math.abs(mOverTranslateX) - MAX_OVER_SCROLL) / MAX_OVER_SCROLL);
//        }
//
//        if (mOverTranslateX > 0 && distanceX < 0) {
//            if (mOverTranslateX - distanceX <= 0) dx = mOverTranslateX;
//            else dx = distanceX;
//        } else {
//            dx = distanceX * Math.abs((Math.abs(mOverTranslateX) - MAX_OVER_SCROLL) / MAX_OVER_SCROLL);
//        }
//
//        if (mOverTranslateY < 0 && distanceY > 0) {
//            if (mOverTranslateY - distanceY >= 0) dy = mOverTranslateY;
//            else dy = distanceY;
//        } else {
//            dy = distanceY * Math.abs((Math.abs(mOverTranslateY) - MAX_OVER_SCROLL) / MAX_OVER_SCROLL);
//        }
//
//        if(mOverTranslateY > 0 && distanceY < 0){
//            if (mOverTranslateY - distanceY <= 0) dy = mOverTranslateY;
//            else dy = distanceY;
//        } else {
//            dy = distanceY * Math.abs((Math.abs(mOverTranslateY) - MAX_OVER_SCROLL) / MAX_OVER_SCROLL);
//        }
//
//
//        mOverTranslateX += dx;
//        mOverTranslateY += dy;
//        mAnimaMatrix.postTranslate(dx, dy);
//        executeTranslate();
    }

    public boolean canScrollHorizontallySelf(float direction) {
        if (mOverTranslateX != 0) return false;
        if (mImgRect.width() < mWidgetRect.width()) return false;
        if (direction < 0 && Math.round(mImgRect.left) >= mWidgetRect.left) return false;
        if (direction > 0 && Math.round(mImgRect.right) <= mWidgetRect.right) return false;
        if (mOverTranslateX != 0) return false;
        return true;
    }

    public boolean canScrollVerticallySelf(float direction) {
        if (mOverTranslateY != 0) return false;
        if (mImgRect.height() < mWidgetRect.height()) return false;
        if (direction < 0 && Math.round(mImgRect.top) >= mWidgetRect.left) return false;
        if (direction > 0 && Math.round(mImgRect.bottom) <= mWidgetRect.bottom) return false;
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

        int mLastX;
        int mLastY;

        int mTmpX;
        int mTmpY;

        Transform() {
            mTranslateScroller = new OverScroller(getContext(), new DecelerateInterpolator());
            mScaleScroller = new Scroller(getContext(), new DecelerateInterpolator());
            mFlingScroller = new OverScroller(getContext(), new LinearInterpolator());
        }

        void withTranslate(int startX, int startY, int deltaX, int deltaY) {
            mTranslateScroller.startScroll(startX, startY, deltaX, deltaY);
        }

        void withScale(float form, float to) {
            mScaleScroller.startScroll((int) (form * 10000), 0, (int) ((to - form) * 10000), 0);
        }

        void withFling(float velocityX, float velocityY) {
            mLastX = velocityX < 0 ? Integer.MAX_VALUE : 0;
            mLastY = velocityY < 0 ? Integer.MAX_VALUE : 0;
            int distanceX = (int) (velocityX > 0 ? Math.abs(mImgRect.left) : mImgRect.right - mWidgetRect.right);
            int distanceY = mImgRect.height() > mWidgetRect.height() ? (int) (velocityY > 0 ? Math.abs(mImgRect.top) : mImgRect.bottom - mWidgetRect.bottom) : 0;
            distanceX = velocityX < 0 ? Integer.MAX_VALUE - distanceX : distanceX;
            distanceY = velocityY < 0 ? Integer.MAX_VALUE - distanceY : distanceY;

            int minX = velocityX < 0 ? distanceX : 0;
            int maxX = velocityX < 0 ? Integer.MAX_VALUE : distanceX;
            int minY = velocityY < 0 ? distanceY : 0;
            int maxY = distanceY < 0 ? Integer.MAX_VALUE : distanceY;

            mFlingScroller.fling(mLastX, mLastY, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY, MAX_FLING_OVER_SCROLL, MAX_FLING_OVER_SCROLL);
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
            mFlingScroller.abortAnimation();


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

            if (mFlingScroller.computeScrollOffset()) {
                int x = mFlingScroller.getCurrX() - mLastX;
                int y = mFlingScroller.getCurrY() - mLastY;

                mLastX = mFlingScroller.getCurrX();
                mLastY = mFlingScroller.getCurrY();

                Log.i("bm", "run  " + mFlingScroller.getCurrX());

                mTranslateX += x;
                mTranslateY += y;
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