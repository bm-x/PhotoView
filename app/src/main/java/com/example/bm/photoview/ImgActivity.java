package com.example.bm.photoview;

import android.app.Activity;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.bm.library.Info;
import com.bm.library.PhotoView;

public class ImgActivity extends Activity {

    Info mRectF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        final PhotoView view = (PhotoView) findViewById(R.id.img);
        view.disenable();
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                PhotoView img2 = (PhotoView) findViewById(R.id.img2);
                img2.setVisibility(View.VISIBLE);
                mRectF = view.getInfo();
                img2.animaFrom(mRectF);
            }
        });

        final PhotoView img2 = (PhotoView) findViewById(R.id.img2);
        img2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                img2.animaTo(mRectF, new Runnable() {
                    @Override
                    public void run() {
                        img2.setVisibility(View.GONE);
                        view.setVisibility(View.VISIBLE);
                    }
                });
            }
        });
    }
}
