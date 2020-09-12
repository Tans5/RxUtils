package com.tans.rxutils

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.util.SizeF
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.File
import java.lang.RuntimeException

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


fun cropImage(context: FragmentActivity,
              sourceUri: Uri,
              cropRatio: SizeF? = null,
              cropMaxSize: Size? = null,
              cropName: String = "${System.currentTimeMillis()}.jpg"
): Maybe<Uri> {

    val desUri = insertMediaItem(
        context = context,
        mimeType = "image/jpg",
        name = cropName,
        saveMediaType = SaveMediaType.Images,
        relativePath = Environment.DIRECTORY_PICTURES,
        isPending = false)

    return if (desUri != null) {
        cropImage(
            context = context,
            sourceUri = sourceUri,
            destinationUri = desUri,
            cropRatio = cropRatio,
            cropMaxSize = cropMaxSize
        )
            .toSingle(Uri.EMPTY)
            .flatMapMaybe { uri ->
                if (uri == Uri.EMPTY) {
                    context.contentResolver.delete(desUri, null, null)
                    Maybe.empty()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        context.contentResolver.update(desUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                    }
                    Maybe.just(uri)
                }
            }
    } else {
        Maybe.error(RuntimeException("Can't create crop target uri."))
    }
}

fun cropImage(
    context: FragmentActivity,
    sourceUri: Uri,
    destinationUri: Uri,
    cropRatio: SizeF? = null,
    cropMaxSize: Size? = null
): Maybe<Uri> {

    return Maybe.empty()
}