package com.tans.rxutils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import io.reactivex.Completable
import io.reactivex.Single
import java.io.*
import java.lang.RuntimeException

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
    return writeDataToFile(file, inputStream ?: error("Can't create InputStream"))
}

fun writeDataToFile(file: File, inputStream: InputStream)
        : Single<File> = writeDataToOutputStream(inputStream, FileOutputStream(file))
    .andThen(Single.just(file))

fun writeDataToOutputStream(
    inputStream: InputStream,
    outputStream: OutputStream,
    bufferedSize: Int = 8 * 1024 // 8 KB
): Completable = Completable.fromAction {
    val osb = outputStream.buffered(bufferedSize)
    inputStream.buffered(bufferedSize).use { `is` ->
        osb.use { os ->
            val bytes = ByteArray(bufferedSize)
            var count = `is`.read(bytes, 0, bufferedSize)
            while (count > 0) {
                os.write(bytes, 0, count)
                count = `is`.read(bytes, 0, bufferedSize)
            }
        }
    }
}


enum class MediaType { Audio, Video, Images, Files, Downloads }

fun saveDataToMediaStore(
    context: Context,
    inputStream: InputStream,
    mimeType: String,
    name: String,
    mediaType: MediaType,
    relativePath: String
): Completable {

    val (displayName, relativePathColName, contentUri) = when (mediaType) {
        MediaType.Audio -> {
            Triple (
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.AudioColumns.RELATIVE_PATH
                else
                    "",
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
        }
        MediaType.Images -> {
            Triple(
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.ImageColumns.RELATIVE_PATH
                else
                    "",
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        MediaType.Downloads -> {
            Triple(
                MediaStore.DownloadColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.DownloadColumns.RELATIVE_PATH
                else
                    "",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                else
                    Uri.parse("content://downloads/")
            )
        }
        MediaType.Files -> {
            Triple(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Files.FileColumns.RELATIVE_PATH
                else
                    "",
                MediaStore.Files.getContentUri(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Files.FileColumns.VOLUME_NAME else "")
            )

        }
        MediaType.Video -> {
            Triple(
                MediaStore.Video.VideoColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Video.VideoColumns.RELATIVE_PATH
                else
                    "",
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
        }
    }

    val contentValues = ContentValues().apply {
        put(displayName, name)

        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(relativePathColName, relativePath)
        }


    }

    val uri = context.contentResolver.insert(contentUri, contentValues)
    return if (uri == null) {
        Completable.error(RuntimeException("Can't create uri"))
    } else {
        val outputStream = context.contentResolver.openOutputStream(uri)
        if (outputStream == null) {
            Completable.error(RuntimeException("Can't create output stream."))
        } else {
            writeDataToOutputStream(inputStream, outputStream)
        }
    }
}
