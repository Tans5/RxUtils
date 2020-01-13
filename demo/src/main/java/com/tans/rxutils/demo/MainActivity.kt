package com.tans.rxutils.demo

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.jakewharton.rxbinding3.view.clicks
import com.tans.rxutils.loadingDialog
import com.tans.rxutils.optionDialogMaybe
import com.tans.rxutils.optionDialogSingle
import com.tans.rxutils.startActivityForResult
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loading_bt.clicks()
            .flatMapSingle {
                Single.just(Unit)
                    .delay(1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .loadingDialog(this)
            }
            .subscribe()

        option_bt.clicks()
            .flatMapMaybe {
                optionDialogMaybe(context = this, title = "Title", msg = "Msg",
                    positiveText = "OK", negativeText = "NO")
                    .doOnSuccess {
                        it.dismissDialog()
                        Toast.makeText(this, it.toString(), Toast.LENGTH_SHORT).show()
                    }
            }
            .subscribe()

        result_activity_bt.clicks()
            .flatMapMaybe {
                startActivityForResult(this, Intent(this, ResultActivity::class.java))
                    .doOnSuccess { (resultCode, data) ->
                        if (resultCode == Activity.RESULT_OK) {
                            Toast.makeText(this, data.getStringExtra("test_result"), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .subscribe()
    }
}
