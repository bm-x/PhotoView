package com.example.bm.photoview;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import com.bm.library.PhotoView;

public class ApplyImageInfoActivity extends Activity {

    PhotoView one;
    PhotoView two;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_apply_image_info);

        one = findViewById(R.id.one);
        two = findViewById(R.id.two);

        one.enable().enableRotate();
        two.enable().enableRotate();
    }

    public void apply(View view) {
        two.apply(one.getInfo());
    }
}
