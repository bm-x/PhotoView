package com.example.bm.photoview;

import android.app.Activity;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.Scroller;

import com.bm.library.Info;
import com.bm.library.PhotoView;

public class ImageViewActivity extends Activity {

    ImageView img;
    PhotoView photoView;

    Info mInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_view);

        img = (ImageView) findViewById(R.id.img);
        photoView = (PhotoView) findViewById(R.id.photoview);
        photoView.enable();

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mInfo = PhotoView.getImageViewInfo(img);
                img.setVisibility(View.GONE);
                photoView.setVisibility(View.VISIBLE);
                photoView.animaFrom(mInfo);
            }
        });

        photoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                photoView.animaTo(mInfo, new Runnable() {
                    @Override
                    public void run() {
                        photoView.setVisibility(View.GONE);
                        img.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }


    @Override
    public void onBackPressed() {
        if (photoView.getVisibility() == View.VISIBLE) {
            photoView.animaTo(mInfo, new Runnable() {
                @Override
                public void run() {
                    photoView.setVisibility(View.GONE);
                    img.setVisibility(View.VISIBLE);
                }
            });
        } else {
            super.onBackPressed();
        }
    }
}