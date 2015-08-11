package com.example.bm.photoview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void img(View view){
        startActivity(new Intent(this,ImgActivity.class));
    }

    public void viewpager(View  view){
        startActivity(new Intent(this,ViewPagerActivity.class));
    }
}