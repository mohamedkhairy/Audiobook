package com.example.audiobook.service

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import arrow.core.None
import arrow.core.Option
import com.example.audiobook.model.Chapter
import com.example.audiobook.model.ListenHubAudioBooks
import com.example.audiobook.player.PlayerManager
import com.example.audiobook.utils.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayerService : MediaBrowserServiceCompat() {

    companion object {
        private const val TAG = "PlayerService"
        private var lastBook: Option<ListenHubAudioBooks> = None
        private var lastChapterId: Option<String> = None
        private var lastCover: Option<Bitmap> = None
    }


    private lateinit var playerManger: PlayerManager
    private lateinit var mediaSession: MediaSessionCompat
    private val mediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {


            override fun onPrepareFromMediaId(mediaId: String, extras: Bundle) {
                GlobalScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, e ->
                    Log.e(TAG, "CoroutineExceptionHandler", e)
                }) {
                    extras.classLoader = ListenHubAudioBooks::javaClass.javaClass.classLoader

                    lastBook = Option.fromNullable(extras.getParcelable(BOOK_DATA))
                    lastBook.fold({
                    }, { book ->

                        lastChapterId = Option.fromNullable(mediaId)
                        lastChapterId.fold({
                        }, { chapterId ->
                            val chapterList = book.chapters
                            if (chapterList.isNullOrEmpty()) return@launch
                            val playListToPlay =
                                getListToPlayForId(book, chapterId).mapIndexed { index, chapter ->
                                    mapChapterToMediaMetaData(index.toString(), chapter, book)
                                }
                            playerManger.preparePlayerForBook(playListToPlay, book)
                        })
                    })
                }
            }


            private fun getListToPlayForId(
                book: ListenHubAudioBooks,
                mediaId: String
            ): List<Chapter> {
                if (mediaId.toInt() == -1) Log.e(
                    TAG,
                    "Error chapter androidId passed to the service in not in the passed book"
                )
                return book.chapters.filterIndexed { index, _ -> index >= mediaId.toInt() }
                    ?: listOf()
            }


            override fun onPlay() {
                playerManger.play()
            }

            override fun onPause() {
                playerManger.pause()
            }

            override fun onSkipToPrevious() {
                playerManger.previous()
            }

            override fun onSkipToNext() {
                playerManger.next()
            }

            override fun onStop() {
                playerManger.stop()
            }


        }

    override fun onCreate() {
        super.onCreate()
        initMediaSession()
        initPlayerManager()
    }

    @SuppressLint("WrongConstant")
    private fun mapChapterToMediaMetaData(
        index: String,
        chapter: Chapter,
        book: ListenHubAudioBooks
    ): MediaMetadataCompat {
        val builder = MediaMetadataCompat.Builder()
        return with(builder) {

            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, index)
            putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, chapter.link)
            putString(MediaMetadataCompat.METADATA_KEY_TITLE, chapter.title)
            putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI, book.imageUrl)
            putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, book.authors[0].firstName)
            putString(MediaMetadataCompat.METADATA_KEY_GENRE, book.genres)
            putString(MediaMetadataCompat.METADATA_KEY_ARTIST, chapter.narratedBy)

            return@with build()
        }
    }


    private fun initPlayerManager() {
        playerManger = PlayerManager(this, mediaSession)
    }


    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS or MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS)
        mediaSession.setCallback(mediaSessionCallback)
        sessionToken = mediaSession.sessionToken
        mediaSession.isActive = true
    }

    private fun releaseMediaSession() {
        mediaSession.isActive = false
        mediaSession.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        releaseMediaSession()
        playerManger.release()
        lastBook = None
        lastChapterId = None
        lastCover = None
        super.onDestroy()
    }


    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        result.sendResult(mutableListOf())
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? =
        BrowserRoot("Root", null)


}