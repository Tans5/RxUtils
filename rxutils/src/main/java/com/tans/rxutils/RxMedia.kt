package com.tans.rxutils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import io.reactivex.Completable
import io.reactivex.Single
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 *
 * author: pengcheng.tan
 * date: 2020-01-14
 */

/**
 * file must be public for others' app
 */
fun galleryAddPublicPic(file: File, context: Context): Completable = Completable.fromAction {
    val i = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
    i.data = Uri.fromFile(file)
    context.sendBroadcast(i)
}

fun shareImageAndText(
    context: FragmentActivity,
    text: String? = null,
    imageFile: File? = null,
    authority: String? = null
): Single<Pair<Int, Intent?>> {
    val i = Intent(Intent.ACTION_SEND)
    if (imageFile != null) {
        i.type = "image/*"
        val data = FileProvider.getUriForFile(context, authority ?: "", imageFile)
        i.putExtra(Intent.EXTRA_STREAM, data)
    } else {
        i.type = "text/plain"
    }
    if (text != null) {
        i.putExtra(Intent.EXTRA_TEXT, text)
    }
    return startActivityForResult(context, i)
}

fun saveUriToLocal(uri: Uri, file: File, context: Context): Single<File> {
    val inputStream = context.contentResolver.openInputStream(uri)
    return readInputStream(file, inputStream ?: error("Can't create InputStream"))
}

fun readInputStream(file: File, inputStream: InputStream): Single<File> = Single.fromCallable {
    val bufferSize = 8 * 1024
    val fos = FileOutputStream(file).buffered(bufferSize)
    inputStream.buffered(bufferSize).use { bis: BufferedInputStream ->
        fos.use { bos ->
            val bytes = ByteArray(bufferSize)
            var count = bis.read(bytes, 0, bufferSize)
            while (count > 0) {
                bos.write(bytes, 0, count)
                count = bis.read(bytes)
            }
        }
    }
    file
}
