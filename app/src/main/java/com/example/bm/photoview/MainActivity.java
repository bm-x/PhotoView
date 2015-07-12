package com.example.bm.photoview;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends Activity {

    private PhotoView mImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mImg = (PhotoView) findViewById(R.id.img);
    }

    public void btn(View view){

    }

    public void translate(View  view){

    }
}
