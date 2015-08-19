package com.example.bm.photoview;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;

import com.bm.library.Info;
import com.bm.library.PhotoView;

public class ImgClick extends Activity implements RadioGroup.OnCheckedChangeListener {

    Info mRectF;

    PhotoView mImg1;
    PhotoView mImg2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img_click);

        ((RadioGroup) findViewById(R.id.group)).setOnCheckedChangeListener(this);

        mImg1 = (PhotoView) findViewById(R.id.img1);
        mImg2 = (PhotoView) findViewById(R.id.img2);

        //设置不可以双指缩放移动放大等操作，跟普通的image一模一样
        mImg1.disenable();
        mImg1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mImg1.setVisibility(View.GONE);
                mImg2.setVisibility(View.VISIBLE);

                //获取img1的图片位置信息
                mRectF = mImg1.getInfo();
                //让img2从img1的位置变换到他本身的位置
                mImg2.animaFrom(mRectF);
            }
        });

        mImg2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 让img2从自身位置变换到原来img1图片的位置大小
                mImg2.animaTo(mRectF, new Runnable() {
                    @Override
                    public void run() {
                        mImg2.setVisibility(View.GONE);
                        mImg1.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        switch (checkedId) {
            case R.id.center:
                mImg1.setScaleType(ImageView.ScaleType.CENTER);
                break;
            case R.id.center_crop:
                mImg1.setScaleType(ImageView.ScaleType.CENTER_CROP);
                break;
            case R.id.center_inside:
                mImg1.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                break;
            case R.id.fit_center:
                mImg1.setScaleType(ImageView.ScaleType.FIT_CENTER);
                break;
            case R.id.fit_end:
                mImg1.setScaleType(ImageView.ScaleType.FIT_END);
                break;
            case R.id.fit_start:
                mImg1.setScaleType(ImageView.ScaleType.FIT_START);
                break;
            case R.id.fit_xy:
                // 建议用了fit_Xy就不要使用缩放或者animaFrom或animaTo
                mImg1.setScaleType(ImageView.ScaleType.FIT_XY);
                break;
        }
    }
}
