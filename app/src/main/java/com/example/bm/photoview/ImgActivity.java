package com.example.bm.photoview;

import android.app.Activity;
import android.os.Bundle;

import com.bm.library.PhotoView;

/**
 * Created by liuheng on 2015/6/21.
 */
public class ImgActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        ((PhotoView)findViewById(R.id.img1)).enable();
    }
}
