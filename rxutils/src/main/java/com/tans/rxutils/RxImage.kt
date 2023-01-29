package com.tans.rxutils

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.util.SizeF
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import com.yalantis.ucrop.UCrop
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
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


fun cropImage(context: FragmentActivity,
              sourceUri: Uri,
              cropRatio: SizeF? = null,
              cropMaxSize: Size? = null
): Maybe<File> {
    val parentFile = context.cacheDir
    val file = File(parentFile, "crop_image_catch_${System.currentTimeMillis()}.jpg")
    return cropImage(
            context = context,
            sourceUri = sourceUri,
            destinationFile = file,
            cropRatio = cropRatio,
            cropMaxSize = cropMaxSize
        )
            .flatMap { uri ->
                if (uri == Uri.EMPTY) {
                    if (file.exists()) { file.delete() }
                    Maybe.empty()
                } else {
                    if (uri.scheme == "file") {
                        Maybe.just(File(uri.path ?: ""))
                    } else {
                        Maybe.just(file)
                    }
                }
            }
}

/**
 * Crop image file is JPEG format.
 */
fun cropImage(
    context: FragmentActivity,
    sourceUri: Uri,
    destinationFile: File,
    cropRatio: SizeF? = null,
    cropMaxSize: Size? = null
): Maybe<Uri> {
    val intent = UCrop.of(sourceUri, Uri.fromFile(destinationFile))
        .withOptions(UCrop.Options().apply { setCompressionFormat(Bitmap.CompressFormat.JPEG) })
        .apply {
            if (cropRatio != null) {
                withAspectRatio(cropRatio.width, cropRatio.height)
            }
            if (cropMaxSize != null) {
                withMaxResultSize(cropMaxSize.width, cropMaxSize.height)
            }
        }
        .getIntent(context)

    return startActivityForResult(context, intent)
        .flatMapMaybe { (resultCode, resultIntent) ->
            val uri = if (resultIntent == null) null else UCrop.getOutput(resultIntent)
            if (resultCode == Activity.RESULT_OK && uri != null) {
                Maybe.just(uri)
            } else {
                Maybe.empty()
            }
        }
}