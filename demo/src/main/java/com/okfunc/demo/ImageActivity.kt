package com.okfunc.demo

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.okfunc.photoview.PhotoInfo

import kotlinx.android.synthetic.main.activity_image.*

class ImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        var info: PhotoInfo? = null

        p1.setOnClickListener {
            p1.visibility = View.GONE
            p2.visibility = View.VISIBLE
            info = p1.getPhotoInfo()
            p2.animaFrom(info!!)
        }

        p2.setOnClickListener {
            p2.animaTo(info!!) {
                p1.visibility = View.VISIBLE
                p2.visibility = View.GONE
            }
        }

        p2.setSwipeCloseListener {
            p2.animaTo(info!!) {
                p1.visibility = View.VISIBLE
                p2.visibility = View.GONE
            }
        }
    }

}
