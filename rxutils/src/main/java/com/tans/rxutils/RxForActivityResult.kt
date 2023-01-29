package com.tans.rxutils

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import kotlin.random.Random

/**
 *
 * author: pengcheng.tan
 * date: 2020-01-13
 */

class ForResultFragment(val emitter: SingleEmitter<Pair<Int, Intent?>>? = null,
                        val intent: Intent? = null) : Fragment() {
    val requestCode: Int = Random(System.currentTimeMillis()).nextInt(0, 1000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent != null) {
            startActivityForResult(intent, requestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (emitter != null) {
            if (requestCode == requestCode) {
                emitter.onSuccess(resultCode to data)
            } else {
                emitter.onError(Throwable("RequestCode Error: $requestCode != ${this.requestCode}"))
            }
        }

        val transaction = activity?.supportFragmentManager?.beginTransaction()
        transaction?.remove(this)
        transaction?.commit()
    }
}

fun startActivityForResult(context: FragmentActivity, intent: Intent): Single<Pair<Int, Intent?>> {
    return Single.create { emitter ->
        val transaction = context.supportFragmentManager.beginTransaction()
        val fragment = ForResultFragment(emitter = emitter, intent = intent)
        transaction.add(fragment, "FragmentForResult")
        transaction.commit()
    }
}