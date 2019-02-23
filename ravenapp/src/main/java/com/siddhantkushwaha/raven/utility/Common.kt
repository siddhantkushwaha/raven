package com.siddhantkushwaha.raven.utility

import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity


class Common {

    companion object {

        fun clearData(activity: Activity) {
            (activity.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
        }

        fun getPlaystoreIntent(packageName: String): Intent {
            return Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        }

        fun getColorStateList(color: Int): ColorStateList {

            val states = arrayOf(
                    intArrayOf(android.R.attr.state_enabled)
            )

            val colors = intArrayOf(
                    color
            )

            return ColorStateList(states, colors)
        }
    }
}