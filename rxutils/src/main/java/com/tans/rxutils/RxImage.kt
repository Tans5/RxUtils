package com.tans.rxutils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File

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
    return chooseImageFromGallery(context, i)
}

fun chooseImageFromGallery(context: FragmentActivity,
                           intent: Intent): Maybe<Uri> {
    return startActivityForResult(context, intent)
        .flatMapMaybe { (resultCode, data) ->
            val uri = data?.data
            if (resultCode == Activity.RESULT_OK && uri != null) {
                Maybe.just(uri)
            } else {
                Maybe.empty<Uri>()
            }
        }
}

fun captureFromCamera(
    context: FragmentActivity,
    file: File,
    authority: String
): Single<Pair<Int, Intent?>> {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(context, authority, file))
    return startActivityForResult(context, intent)
}