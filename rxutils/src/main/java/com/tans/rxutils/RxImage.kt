package com.tans.rxutils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import io.reactivex.Maybe

/**
 *
 * author: pengcheng.tan
 * date: 2020-01-13
 */

fun chooseImageFromGallery(
    context: FragmentActivity,
    mimeTypes: Array<String> = arrayOf("image/jpeg", "image/png")
): Maybe<Uri> {
    val i = Intent(Intent.ACTION_PICK)
    i.type = "image/*"
    i.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    return startActivityForResult(context, i)
        .flatMap { (resultCode, data) ->
            val uri = data.data
            if (resultCode == Activity.RESULT_OK && uri != null) {
                Maybe.just(uri)
            } else {
                Maybe.empty<Uri>()
            }
        }
}