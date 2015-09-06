package com.example.bm.photoview;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.bm.library.PhotoView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;

import java.io.File;

/**
 * Created by liuheng on 2015/6/21.
 */
public class ImgActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        ((PhotoView) findViewById(R.id.img1)).enable();

        File f = Glide.getPhotoCacheDir(this);
        Log.i("bm", "onCreate   " + f.getAbsolutePath());

        Glide.with(this)
                .load("http://imgsrc.baidu.com/baike/pic/item/7af40ad162d9f2d339d2a789abec8a136227cc91.jpg")
                .crossFade()
                .into(((PhotoView) findViewById(R.id.img1)));
    }
}
