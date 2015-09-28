package com.example.bm.photoview;

import android.app.Activity;
import android.os.Bundle;

import com.bm.library.PhotoView;
import com.bumptech.glide.Glide;

/**
 * Created by liuheng on 2015/6/21.
 */
public class ImgActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        ((PhotoView) findViewById(R.id.img1)).enable();

//        使用Glide加载的gif图片同样支持缩放功能
//        Glide.with(this)
//                .load("http://imgsrc.baidu.com/baike/pic/item/7af40ad162d9f2d339d2a789abec8a136227cc91.jpg")
//                .crossFade()
//                .into(((PhotoView) findViewById(R.id.img1)));
    }
}
