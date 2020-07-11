package com.newpage.salamport.support

import android.content.Context
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener


/*

Usage:

  myView.setOnTouchListener(new OnSwipeTouchListener(this) {
    @Override
    public void onSwipeDown() {
      Toast.makeText(MainActivity.this, "Down", Toast.LENGTH_SHORT).show();
    }
  }

*/
abstract class GestureHandler(c: Context?) : OnTouchListener {
    private val gestureDetector: GestureDetector
    override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(motionEvent)
    }

    private inner class GestureListener : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        // Determines the fling velocity and then fires the appropriate swipe event accordingly
        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val result = false
            try {
                val diffY = e2.y - e1.y
                val diffX = e2.x - e1.x
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(
                            velocityX
                        ) > SWIPE_VELOCITY_THRESHOLD
                    ) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(
                            velocityY
                        ) > SWIPE_VELOCITY_THRESHOLD
                    ) {
                        if (diffY > 0) {
                            onSwipeDown()
                        } else {
                            onSwipeUp()
                        }
                    }
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }


    }

    abstract fun onSwipeRight()
    abstract fun onSwipeLeft()
    fun onSwipeUp() {}
    fun onSwipeDown() {}

    init {
        gestureDetector = GestureDetector(c, GestureListener())
    }

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }
}