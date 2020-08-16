package com.example.audiobook.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.MediaMetadataCompat
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import arrow.core.Either
import arrow.core.Try
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.audiobook.R
import com.example.audiobook.network.RetrofitManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit


suspend fun <T> callAPI(f: suspend () -> Deferred<T>): Either<Result.Error, Result.Success<T>> =
    Try { f().await() }.fold({ t ->
        t.printStackTrace()
        return@fold when (t) {
            is RetrofitManager.NoInternetException -> Either.left(Result.Error(Errors.NoConnectionError))
            else -> Either.left(Result.Error(Errors.UnknownError))
        }
    }, { r: T ->
        return@fold if (r.isNotNull()) Either.right(Result.Success(r))
        else Either.left(Result.Error(Errors.UnknownError))
    })


sealed class Result {
    data class Success<T>(val t: T) : Result()
    data class Error(val error: Errors = Errors.UnknownError) : Result()
}

sealed class Errors {
    object NoConnectionError : Errors()
    object UnknownError : Errors()
}

fun Any?.isNotNull(): Boolean =
    this != null

fun Any?.isNull(): Boolean =
    this == null

fun List<*>?.isNotNullOrEmpty(): Boolean =
    this != null && this.isNotEmpty()


fun AppCompatActivity.addFragment(
    incomingFragment: Lazy<Fragment>,
    tag: String,
    slide: Boolean = false,
    allowDuplicates: Boolean = false
) {
    if (!allowDuplicates) {
        val lastFragmentIndex = supportFragmentManager.backStackEntryCount - 1
        if (lastFragmentIndex >= 0 && supportFragmentManager.getBackStackEntryAt(lastFragmentIndex) != null) if (supportFragmentManager.getBackStackEntryAt(
                lastFragmentIndex
            ).name == tag
        ) return
    }
    val ft = supportFragmentManager.beginTransaction()
    if (!slide) ft.setCustomAnimations(R.anim.enter_b, R.anim.exit_a, R.anim.enter_a, R.anim.exit_b)
    else ft.setCustomAnimations(
        R.anim.enter_b_vertical,
        R.anim.exit_a_vertical,
        R.anim.enter_a_vertical,
        R.anim.exit_b_vertical
    )
    ft.add(R.id.container, incomingFragment.value, tag).addToBackStack(tag)
        .commitAllowingStateLoss()
}

fun ImageView.loadAsyncImage(url: String?) {
    val requestOptions = RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL)
    Glide.with(this.context)
        .load(url)
        .apply(requestOptions)
        .into(this)

}

suspend fun Uri.fromUritoBitmap(context: Context): Bitmap? {
    return withContext(Dispatchers.Main) {
        val glideOptions = RequestOptions()
            .fallback(R.drawable.default_art)
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        withContext(Dispatchers.IO) {
            Glide.with(context).applyDefaultRequestOptions(glideOptions)
                .asBitmap()
                .load(this@fromUritoBitmap)
                .submit()
                .get()
        }
    }
}


fun Long.formatTime(): String {
    if (this <= 0) return "00:00:00"
    return String.format(
        Locale.ENGLISH,
        "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(this),
        TimeUnit.MILLISECONDS.toMinutes(this) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(this)),
        TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(this)))
}

fun MediaMetadataCompat.getIndex(): String? =
    getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)

fun MediaMetadataCompat.getAuthor(): String? =
    getString(MediaMetadataCompat.METADATA_KEY_AUTHOR)

fun MediaMetadataCompat.getDuration(): Long =
    getLong(MediaMetadataCompat.METADATA_KEY_DURATION)

fun MediaMetadataCompat.getGenre(): String? =
    getString(MediaMetadataCompat.METADATA_KEY_GENRE)

fun MediaMetadataCompat.getNarrator(): String? =
    getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

fun MediaMetadataCompat.getTitle(): String =
    getString(MediaMetadataCompat.METADATA_KEY_TITLE)

fun MediaMetadataCompat.getUri(): Uri =
    Uri.parse(getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))

fun MediaMetadataCompat.getImageUri(): String? =
    getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI)
