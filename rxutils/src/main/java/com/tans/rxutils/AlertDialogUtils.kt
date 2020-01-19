package com.tans.rxutils

import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog

/**
 *
 * author: pengcheng.tan
 * date: 2019-08-06
 */

val showingDialog: MutableMap<DialogType, AlertDialog> = HashMap()

sealed class DialogType(val tag: String) {

    object LoadingDialog : DialogType("loading_dialog")

    object OptionalDialog : DialogType("optional_dialog")

    class CustomDialog(tag: String) : DialogType(tag)
}

fun createLoadingDialog(
    context: Context,
    @LayoutRes resId: Int = R.layout.layout_loading,
    view: View? = null,
    cancelable: Boolean = false,
    extraDeal: AlertDialog.Builder.() -> AlertDialog.Builder = { this }
): AlertDialog {
    return AlertDialog.Builder(context)
        .setCancelable(cancelable)
        .setView(resId).apply { if (view != null) setView(view) }
        .extraDeal()
        .create()
        .transparentBackground(true)
}

fun createOptionDialog(
    context: Context,
    title: String,
    msg: String,
    cancelable: Boolean = true,
    positiveText: String = context.getString(R.string.dialog_ok),
    negativeText: String = context.getString(R.string.dialog_cancel),
    positiveHandler: (DialogInterface) -> Unit = { it.dismiss() },
    negativeHandler: (DialogInterface) -> Unit = { it.dismiss() },
    cancelHandler: (DialogInterface) -> Unit = { },
    extraDeal: AlertDialog.Builder.() -> AlertDialog.Builder = { this }
): AlertDialog {
    return AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(msg)
        .setCancelable(cancelable)
        .setPositiveButton(positiveText) { dialog, _ ->
            positiveHandler(dialog)
        }
        .setNegativeButton(negativeText) { dialog, _ ->
            negativeHandler(dialog)
        }
        .setOnCancelListener {
            cancelHandler(it)
        }
        .extraDeal()
        .create()
}


fun AlertDialog.transparentBackground(clearBg: Boolean = false): AlertDialog {
    if (clearBg) {
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    return this
}

fun AlertDialog.show(type: DialogType) {
    showingDialog.remove(type)?.dismiss()
    showingDialog[type] = this
    show()
}

fun dismissDialog(type: DialogType) {
    showingDialog.remove(type)?.dismiss()
}