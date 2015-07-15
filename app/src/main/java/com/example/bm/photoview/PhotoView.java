package com.example.bm.photoview;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bm on 2015/6/21.
 */
public class PhotoView extends ImageView {

    private final float MAX_SCALE = 2.5f;
    private final int DURATION_SCALE = 430;
    private final int DURATION_FLING = 750;

    private boolean canScrollWidth;
    private boolean canScrollHeight;
    private boolean canScrollHorizontally;
    private boolean canScrollVertically;
    private boolean hasDrawable;
    private boolean isKnowSize;
    private Matrix mBaseMatrix = new Matrix();
    private Matrix mMatrix = new Matrix();
    private Matrix mAnimaMatrix = new Matrix();
    private ObjectAnimator mAnimator = new ObjectAnimator();

    private GestureDetector mDetector;

    private int mScreenCenterX;
    private int mScreenCenterY;
    private int mWidth;
    private int mHeight;

    private int mImgWidth;
    private int mImgHeight;

    private float mImgScaleWidth;
    private float mImgScaleHeight;
    private float mImgTranslateX;
    private float mImgTranslateY;

    private boolean hasTranslateX;
    private boolean hasTranslateY;
    private float mScale = 1;
    private float mTranslateX;
    private float mTranslateY;
    private int mCenterX;
    private int mCenterY;

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

        mMatrix.reset();

        Drawable img = getDrawable();

        int w = getWidth();
        int h = getHeight();
        int imgw = img.getIntrinsicWidth();
        int imgh = img.getIntrinsicHeight();

        mImgWidth = imgw;
        mImgHeight = imgh;

        // center
        int tx = (w - imgw) / 2;
        int ty = (h - imgh) / 2;

        float sx = 1;
        float sy = 1;
        // scale
        if (imgw > w) {
            sx = (float) w / imgw;
        }

        if (imgh > h) {
            sy = (float) h / imgh;
        }

        float scale = sx < sy ? sx : sy;

        mBaseMatrix.reset();
        mBaseMatrix.postTranslate(tx, ty);
        mBaseMatrix.postScale(scale, scale, mScreenCenterX, mScreenCenterY);

        executeTranslate();
    }


    private void executeTranslate() {
        mMatrix.set(mBaseMatrix);
        mMatrix.postConcat(mAnimaMatrix);
        setImageMatrix(mMatrix);

        mMatrix.getValues(mValues);
        mImgScaleWidth = mValues[0] * mImgWidth;
        mImgScaleHeight = mValues[0] * mImgHeight;
        mImgTranslateX = mValues[2];
        mImgTranslateY = mValues[5];

        canScrollWidth = mImgScaleWidth >= mWidth;
        canScrollHeight = mImgScaleHeight >= mHeight;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mScreenCenterX = w / 2;
        mScreenCenterY = h / 2;
        mWidth = w;
        mHeight = h;

        if (!isKnowSize) {
            isKnowSize = true;
            doInit();
        }
    }

    private void setScale(float scale) {
        mScale = scale;
    }

    private void setTranslateX(float x) {
        mTranslateX = x;
    }

    private void setTranslateY(float y) {
        mTranslateY = y;
    }

    private ValueAnimator.AnimatorUpdateListener mValueUpdate = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mAnimaMatrix.setScale(mScale, mScale, mCenterX, mCenterY);
            mAnimaMatrix.postTranslate(mTranslateX, mTranslateY);
            executeTranslate();
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        mDetector.onTouchEvent(event);
        return true;
    }

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            if (!canScrollWidth && !canScrollHeight) return true;

            velocityX = velocityX / 4000;
            velocityY = velocityY / 4000;

            float distanceX = velocityX * DURATION_FLING;
            float distanceY = velocityY * DURATION_FLING;

            List<PropertyValuesHolder> animasHolder = new ArrayList<>();

            if (canScrollWidth) {
                if (mImgTranslateX + distanceX >= 0) distanceX = -mImgTranslateX;
                if (mWidth - mImgTranslateX - distanceX >= mImgScaleWidth)
                    distanceX = -(mImgScaleWidth - mWidth + mImgTranslateX);

                PropertyValuesHolder translateX = PropertyValuesHolder.ofFloat("translateX", mTranslateX, mTranslateX + distanceX);
                animasHolder.add(translateX);
            }

            if (canScrollHeight) {
                if (mImgTranslateY - distanceY >= 0) distanceY = mImgTranslateY;
                if (mHeight - mImgTranslateY + distanceY >= mImgScaleHeight)
                    distanceY = mImgScaleHeight - mHeight + mImgTranslateY;

                PropertyValuesHolder translateY = PropertyValuesHolder.ofFloat("translateY", mTranslateY, mTranslateY + distanceY);
                animasHolder.add(translateY);
            }

            mAnimator.setValues(animasHolder.toArray(new PropertyValuesHolder[0]));
            mAnimator.setTarget(PhotoView.this);
            mAnimator.setInterpolator(new DecelerateInterpolator());
            mAnimator.setDuration(DURATION_FLING);
            mAnimator.start();

            return super.onFling(e1, e2, velocityX, velocityY);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            boolean handScroll = false;

            if (mAnimator.isRunning()) {
                mAnimator.end();
            }

            if (canScrollWidth) {
                if (mImgTranslateX >= 0 && distanceX < 0) {
                    canScrollHorizontally = false;
                } else if (mWidth - mImgTranslateX >= mImgScaleWidth && distanceX > 0) {
                    canScrollHorizontally = false;
                } else {
                    if (mImgTranslateX - distanceX >= 0) distanceX = mImgTranslateX;
                    if (mWidth - mImgTranslateX + distanceX >= mImgScaleWidth)
                        distanceX = mImgScaleWidth - mWidth + mImgTranslateX;

                    mTranslateX -= distanceX;
                    mAnimaMatrix.postTranslate(-distanceX, 0);
                    hasTranslateX = true;
                    handScroll = true;
                }
            }

            if (canScrollHeight) {
                if (mImgTranslateY >= 0 && distanceY < 0) {
                    canScrollVertically = false;
                } else if (mHeight - mImgTranslateY >= mImgScaleHeight && distanceY > 0) {
                    canScrollVertically = false;
                } else {
                    if (mImgTranslateY - distanceY >= 0) distanceY = mImgTranslateY;
                    if (mHeight - mImgTranslateY + distanceY >= mImgScaleHeight)
                        distanceY = mImgScaleHeight - mHeight + mImgTranslateY;

                    mTranslateY -= distanceY;
                    mAnimaMatrix.postTranslate(0, -distanceY);
                    hasTranslateY = true;
                    handScroll = true;
                }
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
                mCenterX = (int) e.getX();
                mCenterY = mScreenCenterY;
            } else {
                from = MAX_SCALE;
                to = 1;
            }

            List<PropertyValuesHolder> animasHolder = new ArrayList<>();

            PropertyValuesHolder scale = PropertyValuesHolder.ofFloat("scale", from, to);
            animasHolder.add(scale);

            if (hasTranslateX) {
                hasTranslateX = false;
                PropertyValuesHolder translateX = PropertyValuesHolder.ofFloat("translateX", mTranslateX, 0);
                animasHolder.add(translateX);
            }

            if (hasTranslateY) {
                hasTranslateY = false;
                PropertyValuesHolder translateY = PropertyValuesHolder.ofFloat("translateY", mTranslateY, 0);
                animasHolder.add(translateY);
            }

            mAnimator.setValues(animasHolder.toArray(new PropertyValuesHolder[0]));
            mAnimator.setTarget(PhotoView.this);
            mAnimator.setDuration(DURATION_SCALE);
            mAnimator.start();

            return false;
        }
    };

    @Override
    public boolean canScrollHorizontally(int direction) {
        return canScrollHorizontally;
    }

    @Override
    public boolean canScrollVertically(int direction) {
        return canScrollVertically;
    }

    {
        mAnimator.addUpdateListener(mValueUpdate);
    }

    public static class MultiScaleGestureDetector extends GestureDetector {

        public interface OnScaleGestureListener extends OnGestureListener {
            void onScale(float originDx,float currentDx);
        }

        public static class OnSimpleScaleGestureListener implements OnScaleGestureListener {
            public void onScale(float originDx,float currentDx) {
            }

            public boolean onDown(MotionEvent e) {
                return false;
            }

            public void onShowPress(MotionEvent e) {
            }

            public boolean onSingleTapUp(MotionEvent e) {
                return false;
            }

            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return false;
            }

            public void onLongPress(MotionEvent e) {
            }

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return false;
            }
        }

        private OnScaleGestureListener mListener;

        public MultiScaleGestureDetector(Context context, OnScaleGestureListener listener) {
            super(context, listener);
            mListener = listener;
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {

            final int Action = ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK;

            switch (Action) {
                case MotionEvent.ACTION_POINTER_DOWN:

                    break;
                case MotionEvent.ACTION_POINTER_UP:

                    break;
            }

            return super.onTouchEvent(ev);
        }
    }
}