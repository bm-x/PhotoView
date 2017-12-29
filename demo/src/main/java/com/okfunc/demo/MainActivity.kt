package com.okfunc.demo

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun imageTransform(view: View) {
        startActivity(Intent(this, ImageTransformActivity::class.java))
    }

    fun basic(view: View) {
        startActivity(Intent(this, BasicActivity::class.java))
    }
}
