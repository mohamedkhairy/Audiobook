package com.example.audiobook.player.notification

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.example.audiobook.main.MainActivity
import com.example.audiobook.R
import com.example.audiobook.utils.ACTION_OPEN_PLAYER

class BookNotificationManager {

    companion object {

        private const val MEDIA_NOTIFICATION_CHANNEL_ID = "AudioBook_ID"

        @SuppressLint("RestrictedApi")
        fun notify(
            context: Service,
            mediaSession: MediaSessionCompat,
            paused: Boolean,
            imageBitmap: Bitmap?
        ): Notification {
            var bitmap: Bitmap? = imageBitmap

            if (bitmap == null || bitmap.byteCount <= 0) bitmap =
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_notification)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createChannel(context)

            val notificationBuilder =
                NotificationCompat.Builder(context, MEDIA_NOTIFICATION_CHANNEL_ID)

            notificationBuilder.setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(R.mipmap.ic_notification)
                .setContentTitle(mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setOnlyAlertOnce(true)
                .addAction(
                    R.mipmap.previous,
                    context.getString(R.string.exo_controls_previous_description),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
                .addAction(
                    if (paused) R.mipmap.play else R.mipmap.pause,
                    if (paused) context.getString(R.string.notification_play) else context.getString(
                        R.string.notification_pause
                    ),
                    if (paused) MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PLAY
                    )
                    else MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_PAUSE
                    )
                )
                .addAction(
                    R.mipmap.next,
                    context.getString(R.string.exo_controls_next_description),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java).setAction(ACTION_OPEN_PLAYER),
                        0
                    )
                ).setLargeIcon(bitmap)

            val mediaStyle =
                androidx.media.app.NotificationCompat.MediaStyle().setShowCancelButton(true)
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    ).setMediaSession(mediaSession.sessionToken)
            mediaStyle.setShowActionsInCompactView(0, 1, 2)
            notificationBuilder.setStyle(mediaStyle)

            val notificationManager =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val notification = notificationBuilder.build()
            notificationManager.notify(1, notificationBuilder.build())
            return notification
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createChannel(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val name = "Now Playing"
            val description = "AudioBook playback controls"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(MEDIA_NOTIFICATION_CHANNEL_ID, name, importance)
            mChannel.description = description
            mChannel.setShowBadge(false)
            mChannel.setSound(null, null)
            mChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(mChannel)
        }
    }

}