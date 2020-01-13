package com.tans.rxutils.demo

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.rxbinding3.view.clicks
import kotlinx.android.synthetic.main.activity_result.*

/**
 *
 * author: pengcheng.tan
 * date: 2020-01-13
 */

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        result_finish_bt.clicks()
            .doOnNext {
                setResult(Activity.RESULT_OK, Intent().putExtra("test_result", "Hello, World!!"))
                finish()
            }
            .subscribe()

        finish_bt.clicks()
            .doOnNext {
                finish()
            }
            .subscribe()
    }

}