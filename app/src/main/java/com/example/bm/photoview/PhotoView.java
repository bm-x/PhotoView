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
import android.widget.ImageView;

/**
 * Created by bm on 2015/6/21.
 */
public class PhotoView extends ImageView {

    private final float MAX_SCALE = 2.5f;

    private final String NAME_SCALE = "scale";
    private final String NAME_TRANSLATE = "translate";

    private boolean canScroll;
    private boolean canScrollHorizontally;
    private boolean hasDrawable;
    private boolean isInit;
    private boolean isKnowSize;
    private float[] mValues = new float[16];
    private RectF mRect = new RectF();
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
    private float mImgScaleWidth;
    private float mImgTranslate;

    private boolean hasTranslate;
    private float mScale = 1;
    private int mTranslateX;
    private int mCenterX;
    private int mCenterY;

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

        mBaseMatrix.postTranslate(tx, ty);
        mBaseMatrix.postScale(scale, scale, mScreenCenterX, mScreenCenterY);

        executeTranslate();

        isInit = true;
    }

    private void getRectFromMatrix(){

    }

    private void executeTranslate() {
        mMatrix.set(mBaseMatrix);
        mMatrix.postConcat(mAnimaMatrix);
        setImageMatrix(mMatrix);

        mMatrix.getValues(mValues);
        mImgScaleWidth = mValues[0] * mImgWidth;
        mImgTranslate = mValues[2];

        canScroll = mImgScaleWidth >= mWidth;
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

    private void setTranslateX(int x) {
        mTranslateX = x;
    }

    private ValueAnimator.AnimatorUpdateListener mValueUpdate = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            mAnimaMatrix.setScale(mScale, mScale, mCenterX, mCenterY);
            mAnimaMatrix.postTranslate(mTranslateX, 0);
            executeTranslate();
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        canScrollHorizontally = mDetector.onTouchEvent(event);
        return true;
    }

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

            if (canScroll) {

                if (mImgTranslate >= 0 && distanceX < 0) return false;
                if (mWidth - mImgTranslate >= mImgScaleWidth && distanceX > 0) return false;

                Log.i("bm", mWidth + "    " + mImgTranslate+ "   " + mImgWidth);

                if (mImgTranslate - distanceX >= 0) distanceX = mImgTranslate;
                if (mWidth - mImgTranslate + distanceX >= mImgScaleWidth)
                    distanceX = mImgScaleWidth - mWidth + mImgTranslate;

                hasTranslate = true;
                mTranslateX += distanceX;
                mAnimaMatrix.postTranslate(-distanceX, 0);
                executeTranslate();
            }
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
                mCenterY = (int) e.getY();
            } else {
                from = MAX_SCALE;
                to = 1;
            }

            if (hasTranslate) {
                hasTranslate = true;
                PropertyValuesHolder translateX = PropertyValuesHolder.ofInt("translateX", -mTranslateX, 0);
                PropertyValuesHolder scale = PropertyValuesHolder.ofFloat("scale", from, to);
                mAnimator.setValues(translateX, scale);
                mTranslateX = 0;
            } else {
                PropertyValuesHolder scale = PropertyValuesHolder.ofFloat("scale", from, to);
                mAnimator.setValues(scale);
            }

            mAnimator.setTarget(PhotoView.this);
            mAnimator.start();

            return false;
        }
    };

    @Override
    public boolean canScrollHorizontally(int direction) {
        return canScrollHorizontally;
    }

    {
        mAnimator.addUpdateListener(mValueUpdate);
        mAnimator.setDuration(400);
    }
}
