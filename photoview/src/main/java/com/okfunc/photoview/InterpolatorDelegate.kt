package com.okfunc.photoview

import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator


/**
 * Created by buck on 2017/10/20.
 */
class InterpolatorDelegate(var target: Interpolator = LinearInterpolator()) : Interpolator by target
