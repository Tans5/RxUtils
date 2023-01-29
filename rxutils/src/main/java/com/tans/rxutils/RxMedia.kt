package com.tans.rxutils

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
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

fun createNewFile(context: FragmentActivity, fileName: String, mimeType: String): Single<Pair<Int, Intent?>> {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = mimeType
    intent.putExtra(Intent.EXTRA_TITLE, fileName)
    return startActivityForResult(context, intent)
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


enum class SaveMediaType { Audio, Video, Images, Files, Downloads }

fun saveDataToMediaStore(
    context: Context,
    inputStream: InputStream,
    mimeType: String,
    name: String,
    saveMediaType: SaveMediaType,
    relativePath: String
): Completable {

    val uri = insertMediaItem(
        context = context,
        mimeType = mimeType,
        name = name,
        saveMediaType = saveMediaType,
        relativePath = relativePath,
        isPending = false
    )

    return if (uri == null) {
        Completable.error(RuntimeException("Can't create uri"))
    } else {
        val outputStream = context.contentResolver.openOutputStream(uri)
        if (outputStream == null) {
            Completable.error(RuntimeException("Can't create output stream."))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeDataToOutputStream(inputStream, outputStream)
                    .andThen(Completable.fromAction {
                        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                    })
            } else {
                writeDataToOutputStream(inputStream, outputStream)
            }
        }
    }
}


fun insertMediaItem(context: Context,
                    mimeType: String,
                    name: String,
                    saveMediaType: SaveMediaType,
                    relativePath: String,
                    isPending: Boolean = true): Uri? {

    val (displayName, relativePathColName, contentUri) = when (saveMediaType) {
        SaveMediaType.Audio -> {
            Triple (
                MediaStore.Audio.AudioColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Audio.AudioColumns.RELATIVE_PATH
                else
                    "",
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
        }
        SaveMediaType.Images -> {
            Triple(
                MediaStore.Images.ImageColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Images.ImageColumns.RELATIVE_PATH
                else
                    "",
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        SaveMediaType.Downloads -> {
            Triple(
                MediaStore.DownloadColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.DownloadColumns.RELATIVE_PATH
                else
                    "",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                else
                    error("Below Android Q, Can't insert download media.")
            )
        }
        SaveMediaType.Files -> {
            Triple(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    MediaStore.Files.FileColumns.RELATIVE_PATH
                else
                    "",
                MediaStore.Files.getContentUri(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Files.FileColumns.VOLUME_NAME else error("Below Android Q, Can't file download media."))
            )

        }
        SaveMediaType.Video -> {
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
            put(MediaStore.MediaColumns.IS_PENDING, if (isPending) 0 else 1)
        }
    }

    return context.contentResolver.insert(contentUri, contentValues)
}

sealed class QueryMediaType {
    object Image : QueryMediaType()
    object Audio : QueryMediaType()
    object Video : QueryMediaType()
    class Others(val uri: Uri) : QueryMediaType()
}

sealed class QueryMediaItem(
    val id: Long,
    val mimeType: String,
    val size: Long,
    val uri: Uri
) {
    class Image(
        id: Long,
        mimeType: String,
        size: Long,
        uri: Uri,
        val width: Int,
        val height: Int,
        val displayName: String,
        val dateModify: String
    ) : QueryMediaItem(id, mimeType, size, uri)

    class Audio(
        id: Long,
        mimeType: String,
        size: Long,
        uri: Uri,
        val displayName: String,
        val album: String,
        val albumId: Long,
        val artist: String,
        val track: Int
    ) : QueryMediaItem(id, mimeType, size, uri)

    class Video(
        id: Long,
        mimeType: String,
        size: Long,
        uri: Uri,
        val width: Int,
        val height: Int,
        val displayName: String,
        val artist: String,
        val dateModify: String,
        val album: String
    ) : QueryMediaItem(id, mimeType, size, uri)

    class Others(
        id: Long,
        mimeType: String,
        size: Long,
        uri: Uri
    ) : QueryMediaItem(id, mimeType, size, uri)
}

fun getMedia(
    context: Context,
    queryMediaType: QueryMediaType,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Single<List<QueryMediaItem>> {

    val (uri, projection) = when (queryMediaType) {
        QueryMediaType.Image -> {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_MODIFIED
            )
        }
        QueryMediaType.Audio -> {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.TRACK
            )
        }
        QueryMediaType.Video -> {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.ALBUM,
                MediaStore.Video.Media.ARTIST
            )
        }
        is QueryMediaType.Others -> {
            queryMediaType.uri to arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.SIZE
            )
        }
    }

    val cursor = context.contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )
    return cursor?.use {
        cursor.moveToLast()
        val result = List(cursor.position + 1) { index ->
            cursor.moveToPosition(index)
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
            when (queryMediaType) {
                QueryMediaType.Image -> {
                    QueryMediaItem.Image(
                        id = id,
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)).orEmpty(),
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                        uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)).orEmpty(),
                        width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)),
                        height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)),
                        dateModify = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)).orEmpty()
                    )
                }

                QueryMediaType.Audio -> {
                    QueryMediaItem.Audio(
                        id = id,
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)).orEmpty(),
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)).orEmpty(),
                        album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)).orEmpty(),
                        albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
                        artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)).orEmpty(),
                        track = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
                    )
                }

                QueryMediaType.Video -> {
                    QueryMediaItem.Video(
                        id = id,
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)).orEmpty(),
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                        uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)).orEmpty(),
                        album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)).orEmpty(),
                        artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)).orEmpty(),
                        width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)),
                        height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)),
                        dateModify = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)).orEmpty()
                    )
                }

                is QueryMediaType.Others -> {
                    QueryMediaItem.Others(
                        id = id,
                        mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)).orEmpty(),
                        size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)),
                        uri = ContentUris.withAppendedId(queryMediaType.uri, id)
                    )
                }
            }
        }
        Single.just(result)
    } ?: Single.just(emptyList())
}

