package com.tans.rxutils

/**
 *
 * author: pengcheng.tan
 * date: 2020-01-13
 */

import android.content.Context
import android.view.View
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit

fun <T> Single<T>.switchThread(): Single<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())

fun <T> Observable<T>.switchThread(): Observable<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())

fun <T> Flowable<T>.switchThread(): Flowable<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())

fun <T> Maybe<T>.switchThread(): Maybe<T> = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())

fun Completable.switchThread(): Completable = this.subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())


fun <T> callToObservable(): Pair<Observable<T>, (T) -> Unit> {
    val obs = PublishSubject.create<T>().toSerialized()
    val call: (T) -> Unit = { obs.onNext(it) }
    return obs to call
}

fun <T> Observable<T>.ignoreSeveralClicks(duration: Long = 500L)
        : Observable<T> = this.timeInterval(TimeUnit.MILLISECONDS)
    .filter { it.time() >= duration }
    .map { it.value() }


fun <T> Single<T>.loadingDialog(
    context: Context,
    @LayoutRes resId: Int = R.layout.layout_loading,
    view: View? = null,
    cancelable: Boolean = false,
    extraDeal: AlertDialog.Builder.() -> AlertDialog.Builder = { this }
): Single<T> {
    return this.doOnSubscribe {
        createLoadingDialog(
            context = context,
            resId = resId,
            view = view,
            cancelable = cancelable,
            extraDeal = extraDeal
        ).show(DialogType.LoadingDialog)
    }.doFinally {
        dismissDialog(DialogType.LoadingDialog)
    }
}

fun <T> Maybe<T>.loadingDialog(
    context: Context,
    @LayoutRes resId: Int = R.layout.layout_loading,
    view: View? = null,
    cancelable: Boolean = false,
    extraDeal: AlertDialog.Builder.() -> AlertDialog.Builder = { this }
): Maybe<T> {
    return this.doOnSubscribe {
        createLoadingDialog(
            context = context,
            resId = resId,
            view = view,
            cancelable = cancelable,
            extraDeal = extraDeal
        ).show(DialogType.LoadingDialog)
    }.doFinally {
        dismissDialog(DialogType.LoadingDialog)
    }
}

fun Completable.loadingDialog(
    context: Context,
    @LayoutRes resId: Int = R.layout.layout_loading,
    view: View? = null,
    cancelable: Boolean = false,
    extraDeal: AlertDialog.Builder.() -> AlertDialog.Builder = { this }
): Completable {
    return this.doOnSubscribe {
        createLoadingDialog(
            context = context,
            resId = resId,
            view = view,
            cancelable = cancelable,
            extraDeal = extraDeal
        ).show(DialogType.LoadingDialog)
    }.doFinally {
        dismissDialog(DialogType.LoadingDialog)
    }
}

data class RetryError(val e: Throwable) : Throwable()

fun Completable.retryDialog(
    context: Context,
    retryText: String = context.getString(R.string.dialog_retry)
): Completable {

    return onErrorResumeNext { e ->
        Completable.create { emitter ->
            createOptionDialog(context = context,
                title = "Error",
                msg = e.message ?: "",
                positiveText = retryText,
                positiveHandler = {
                    dismissDialog(DialogType.OptionalDialog)
                    emitter.onError(RetryError(e))
                },
                negativeHandler = {
                    dismissDialog(DialogType.OptionalDialog)
                    emitter.onError(e)
                }).show(DialogType.OptionalDialog)
        }
    }.retry { times, error ->
        if (error is RetryError) {
            Timber.e(error.e)
            Timber.d("Retry times: $times ${error.e}")
            true
        } else {
            false
        }
    }

}

fun <T> Maybe<T>.retryDialog(
    context: Context,
    retryText: String = context.getString(R.string.dialog_retry)
): Maybe<T> {
    return this.onErrorResumeNext { e: Throwable ->
        Maybe.create<T> { emitter ->
            createOptionDialog(context = context,
                title = "Error",
                msg = e.message ?: "",
                positiveText = retryText,
                positiveHandler = {
                    dismissDialog(DialogType.OptionalDialog)
                    emitter.onError(RetryError(e))
                },
                negativeHandler = {
                    dismissDialog(DialogType.OptionalDialog)
                    emitter.onError(e)
                }).show(DialogType.OptionalDialog)
        }
    }
        .retry { times, error ->
            if (error is RetryError) {
                Timber.e(error.e)
                Timber.d("Retry times: $times ${error.e}")
                true
            } else {
                false
            }
        }
}

fun <T> Single<T>.retryDialog(
    context: Context,
    retryText: String = context.getString(R.string.dialog_retry)
): Single<T> {
    return onErrorResumeNext { e ->
        Single.create { emitter ->
            createOptionDialog(context = context,
                title = "Error",
                msg = e.message ?: "",
                positiveText = retryText,
                positiveHandler = {
                    dismissDialog(DialogType.OptionalDialog)
                    emitter.onError(RetryError(e))
                },
                negativeHandler = {
                    dismissDialog(DialogType.OptionalDialog)
                    emitter.onError(e)
                }).show(DialogType.OptionalDialog)
        }
    }.retry { times, error ->
        if (error is RetryError) {
            Timber.e(error.e)
            Timber.d("Retry times: $times ${error.e}")
            true
        } else {
            false
        }
    }
}

sealed class OptionDialogEvent {

    fun dismissDialog() {
        dismissDialog(DialogType.OptionalDialog)
    }

    object PositiveDialogEvent : OptionDialogEvent()

    object NegativeDialogEvent : OptionDialogEvent()

    object CancelDialogEvent : OptionDialogEvent()
}

fun optionDialogMaybe(
    context: Context,
    title: String,
    msg: String,
    positiveText: String,
    negativeText: String): Maybe<OptionDialogEvent> {
    return Maybe.create { emitter ->
        createOptionDialog(
            context = context,
            title = title,
            msg = msg,
            positiveText = positiveText,
            negativeText = negativeText,
            cancelable = true,
            positiveHandler = { d ->
                emitter.onSuccess(OptionDialogEvent.PositiveDialogEvent)
            },
            negativeHandler = { d ->
                emitter.onSuccess(OptionDialogEvent.NegativeDialogEvent)
            },
            cancelHandler = { d ->
                emitter.onSuccess(OptionDialogEvent.CancelDialogEvent)
            }
        ).show(DialogType.OptionalDialog)
    }
}

fun optionDialogSingle(
    context: Context,
    title: String,
    msg: String,
    positiveText: String,
    negativeText: String
): Single<OptionDialogEvent> {
    return Single.create { emitter ->
        createOptionDialog(
            context = context,
            title = title,
            msg = msg,
            positiveText = positiveText,
            negativeText = negativeText,
            cancelable = false,
            positiveHandler = { d ->
                emitter.onSuccess(OptionDialogEvent.PositiveDialogEvent)
            },
            negativeHandler = { d ->
                emitter.onSuccess(OptionDialogEvent.NegativeDialogEvent)
            }
        ).show(DialogType.OptionalDialog)
    }
}