package com.tans.rxutils.demo

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import com.jakewharton.rxbinding3.view.clicks
import com.tans.rxutils.*
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.TimeUnit

val fileProviderAuth = "com.tans.rxutils.demo.fileprovider"

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
            .flatMapSingle {
                startActivityForResult(this, Intent(this, ResultActivity::class.java))
                    .doOnSuccess { (resultCode, data) ->
                        if (resultCode == Activity.RESULT_OK) {
                            Toast.makeText(this, data?.getStringExtra("test_result"), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .subscribe()

        choose_image_bt.clicks()
            .flatMapMaybe {
                chooseImageFromGallery(this)
                    .doOnSuccess {
                        gallery_iv.setImageURI(it)
                    }
            }
            .subscribe()

        capture_photo_bt.clicks()
            .flatMapSingle {
                val file = createImageFile()
                captureFromCamera(this, file, fileProviderAuth)
                    .doOnSuccess { (code, data) ->
                        if (code == Activity.RESULT_OK) {
                            gallery_iv.setImageDrawable(BitmapDrawable.createFromPath(file.absolutePath))
                        } else {
                            file.delete()
                        }
                    }
            }
            .subscribe()

        capture_share_bt.clicks()
            .flatMapMaybe {
                val file = createImageFile()
                captureFromCamera(this, file, fileProviderAuth)
                    .flatMapMaybe {(code, _) ->
                        if (code == Activity.RESULT_OK) {
                            Maybe.just(file)
                        } else {
                            file.delete()
                            Maybe.empty<File>()
                        }
                    }
                    .flatMap { f ->
                        shareImageAndText(
                            context = this,
                            text = "Hello, World!!",
                            imageFile = f,
                            authority = fileProviderAuth)
                            .doOnSuccess {
                                println(it)
                            }
                            .toMaybe()
                    }
            }
            .subscribe()

        choose_image_save_bt.clicks()
            .flatMapMaybe {
                chooseImageFromGallery(this)
                    .flatMap { uri ->
                        val file = createImageFile()
                        saveUriToLocal(uri, file, this)
                            .switchThread()
                            .doOnSuccess {
                                gallery_iv.setImageDrawable(BitmapDrawable.createFromPath(it.path))
                            }
                            .toMaybe()
                    }
            }
            .subscribe()
    }

    fun createImageFile(): File {
        val parent = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File(parent, "${System.currentTimeMillis()}.jpg")
    }

}
