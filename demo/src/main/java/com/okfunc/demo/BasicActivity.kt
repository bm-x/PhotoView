package com.okfunc.demo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.okfunc.photoview.PhotoView

/**
 * Created by clyde on 2017/12/29.
 */

class BasicActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic)

        PhotoView.CUSTOM_TYPE_CENTER_FIT_END
    }
}