package com.example.audiobook.player

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import arrow.core.None
import arrow.core.Option
import com.example.audiobook.model.ListenHubAudioBooks
import com.example.audiobook.model.SeekBarUpdates
import com.example.audiobook.player.notification.BookNotificationManager.Companion.notify
import com.example.audiobook.utils.*
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.*

class PlayerManager(val service: Service, private val mediaSession: MediaSessionCompat) : Runnable,
    AudioManager.OnAudioFocusChangeListener {

    companion object {

        private const val TAG = "PlayerManager"
        private const val INTERVAL_UPDATE: Long = 1000
        private const val USER_AGENT = "audio.bookst"

        private val handler = Handler()
    }

    private val wifiLock: WifiManager.WifiLock =
        (service.application.getSystemService(Context.WIFI_SERVICE) as WifiManager).createWifiLock(
            WifiManager.WIFI_MODE_FULL,
            "appWifiLock"
        )

    private lateinit var playbackAttribute: AudioAttributes
    private lateinit var audioFocusRequest: AudioFocusRequest

    private var lastPosition: Long = 0L
    private var playOnAudioFocus: Boolean = false

    private var playList: Option<List<MediaMetadataCompat>> = None
    private var book: Option<ListenHubAudioBooks> = None
    private var uris: Option<List<Uri>> = None

    private val audioManager: AudioManager =
        service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val playbackStateBuilder = PlaybackStateCompat.Builder()

    private val player: SimpleExoPlayer =
        ExoPlayerFactory.newSimpleInstance(service, DefaultTrackSelector())


    private val dataSourceFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(
            service,
            Util.getUserAgent(service, USER_AGENT),
            null
        )
    }

    private suspend fun getImageBitmap(): Bitmap? {
        return withContext(Dispatchers.Main) {
            var currentBitmap: Bitmap? = null

            return@withContext withContext(Dispatchers.IO) {
                book.fold({
                    return@fold currentBitmap
                }, {
                    val uri = Uri.parse(it.imageUrl)
                    currentBitmap = uri?.fromUritoBitmap(service.applicationContext)
                    return@fold currentBitmap
                })
            }
        }
    }

    init {

        player.addListener(object : PlayerListenerAdapter() {

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                setPlaybackState(player.playbackState, player.currentPosition)
            }


            override fun onPlayerError(error: ExoPlaybackException) {
                GlobalScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, e ->
                    Log.e(TAG, "CoroutineExceptionHandler", e)
                }) {
                    error.printStackTrace()
                    setPlaybackState(PlaybackStateCompat.STATE_ERROR)
                    handler.removeCallbacks(this@PlayerManager)
                    service.stopForeground(true)
                    lastPosition = player.currentPosition
                    abandonAudioFocus()
                }
            }

//            @SuppressLint("SwitchIntDef") override fun onPositionDiscontinuity(reason: Int) {
//                GlobalScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, e ->
//                    Log.e(TAG, "CoroutineExceptionHandler", e)
//                }) {
//                        when (reason) {
//                            Player.DISCONTINUITY_REASON_PERIOD_TRANSITION -> {
//                                setMetaData()
//                                setPlaybackState(PlaybackStateCompat.STATE_PLAYING, player.currentPosition)
//                                service.startForeground(1, notify(service, mediaSession, false , getImageBitmap()))
//                                if (stopPlaybackOnNextPeriod) {
//                                    stopPlaybackOnNextPeriod = false
//                                    pause()
//                                }
//                                lastIndex = player.currentWindowIndex
//                            }
//                            Player.DISCONTINUITY_REASON_SEEK              -> {
//                                setMetaData()
//                                if (lastIndex != player.currentWindowIndex) {
//                                            lastIndex = player.currentWindowIndex
//                                            Unit
////                                        })
//                                }
////                                playList.fold({ logE(TAG, "Error playlist mediaMetaData is null") }, { /* logPlayEvent(it)*/ })
//                            }
//                        }
////                    }
//                }
//            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                GlobalScope.launch(Dispatchers.IO + CoroutineExceptionHandler { _, e ->
                    Log.e(TAG, "CoroutineExceptionHandler", e)
                }) {
                    when (playbackState) {
                        Player.STATE_READY -> {

                            if (playWhenReady) {
                                setMetaData()
                                setPlaybackState(
                                    PlaybackStateCompat.STATE_PLAYING,
                                    player.currentPosition
                                )
                                handler.post(this@PlayerManager)
                                service.startForeground(
                                    1,
                                    notify(service, mediaSession, false, getImageBitmap())
                                )
                            } else {
                                setPlaybackState(
                                    PlaybackStateCompat.STATE_PAUSED,
                                    player.currentPosition
                                )
                                handler.removeCallbacks(this@PlayerManager)
                                service.startForeground(
                                    1,
                                    notify(service, mediaSession, true, getImageBitmap())
                                )
                                service.stopForeground(false)
                            }
                        }

                        Player.STATE_IDLE -> {
                            handler.removeCallbacks(this@PlayerManager)
                            service.stopForeground(true)
                            setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
                        }

                        Player.STATE_ENDED -> {
                            service.stopForeground(true)
                            abandonAudioFocus()
                        }

                        Player.STATE_BUFFERING -> {
                            setMetaData()
                            handler.removeCallbacks(this@PlayerManager)
                            setPlaybackState(
                                PlaybackStateCompat.STATE_BUFFERING,
                                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN
                            )
                            service.stopForeground(false)
                        }
                    }
                }
            }

        })
        initAudioFocusRequest()
    }

    private fun initAudioFocusRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            playbackAttribute =
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(
                    AudioAttributes.CONTENT_TYPE_MUSIC
                )
                    .build()
            audioFocusRequest =
                AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(playbackAttribute)
                    .setAcceptsDelayedFocusGain(false).setOnAudioFocusChangeListener(this).build()
        }
    }

    private fun setPlaybackState(state: Int, position: Long = 0, extras: Bundle? = null) {
        val actions: Long = if (state == PlaybackStateCompat.STATE_PAUSED) {
            PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        } else {
            PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_FAST_FORWARD or PlaybackStateCompat.ACTION_REWIND or PlaybackStateCompat.ACTION_STOP or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        playbackStateBuilder.setActions(actions)
        playbackStateBuilder.setState(
            state,
            if (position < 0) PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN else position,
            player.playbackParameters.speed
        )
        if (extras != null) playbackStateBuilder.setExtras(extras)
        mediaSession.setPlaybackState(playbackStateBuilder.build())

    }


    override fun run() {
        val seekBarUpdates = SeekBarUpdates(player.currentPosition.toInt(), player.duration)
        service.applicationContext.sendBroadcast(
            Intent(PROGRESS_KEY).putExtra(
                PROGRESS_DATA,
                seekBarUpdates
            )
        )
        handler.postDelayed(this, INTERVAL_UPDATE)
    }

    @SuppressLint("WrongConstant")
    private fun setMetaData() {
        val currentIndex = player.currentWindowIndex
        val metaDataBuilder = MediaMetadataCompat.Builder()
        playList.fold({
            Log.e(TAG, "setMetaData : Error can not set metadata playlist is null")
        }, { playList ->
            with(metaDataBuilder) {
                uris.fold({
                    Log.e(TAG, "setMetaData can not set metadata playlist uris is null")
                }, { _ ->
                    putString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                        playList[currentIndex].getIndex()
                    )
                    putString(
                        MediaMetadataCompat.METADATA_KEY_TITLE,
                        playList[currentIndex].getTitle()
                    )
                    putString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON_URI,
                        playList[currentIndex].getImageUri()
                    )
                    putLong(MediaMetadataCompat.METADATA_KEY_DURATION, player.duration)
                    putString(
                        MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                        playList[currentIndex].getUri().toString()
                    )
                    putString(
                        MediaMetadataCompat.METADATA_KEY_AUTHOR,
                        playList[currentIndex].getAuthor()
                    )
                    putString(
                        MediaMetadataCompat.METADATA_KEY_GENRE,
                        playList[currentIndex].getGenre()
                    )
                    putString(
                        MediaMetadataCompat.METADATA_KEY_ARTIST,
                        playList[currentIndex].getNarrator()
                    )

                    mediaSession.setMetadata(metaDataBuilder.build())
                })
            }
        })
    }


    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION") return audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }


    fun preparePlayerForBook(playList: List<MediaMetadataCompat>, book: ListenHubAudioBooks) {
        if (!wifiLock.isHeld) wifiLock.acquire()
        this@PlayerManager.playList = Option.just(playList)
        this@PlayerManager.book = Option.just(book)

        this@PlayerManager.uris = Option.just(playList.map { it.getUri() })
        val mediaSources = mutableListOf<ExtractorMediaSource>()

        uris.fold({
            Log.e(TAG, "preparePlayerForBook : Error uris is null can not prepare player")
        }, { urisToPlay ->
            urisToPlay.mapIndexed { index, uri ->
                val mediaSource = internalPrepare(dataSourceFactory, index, playList, uri)
                mediaSources.add(mediaSource)
            }
        })


        if (mediaSources.isEmpty()) {
            setPlaybackState(PlaybackStateCompat.STATE_STOPPED)
            Log.e(TAG, "preparePlayerForBook : Error media sources is empty!")
            return
        }

        val concatenatingMediaSource = ConcatenatingMediaSource(*mediaSources.toTypedArray())
        player.prepare(concatenatingMediaSource)

    }

    private fun internalPrepare(
        dataSource: DataSource.Factory,
        index: Int,
        playList: List<MediaMetadataCompat>,
        uri: Uri
    ) =
        ExtractorMediaSource.Factory(DataSource.Factory { dataSource.createDataSource() })
            .setMinLoadableRetryCount(Int.MAX_VALUE).createMediaSource(uri)


    fun play() {
        if (requestAudioFocus()) player.playWhenReady = true
    }

    fun pause() {
        player.playWhenReady = false
        if (!playOnAudioFocus) abandonAudioFocus()
    }

    fun previous() {
        uris.fold({
            Log.e(TAG, "previous Error : can not skip to previous")
        }, {
            var previousPosition = player.currentWindowIndex - 1
            if (previousPosition < 0) previousPosition = it.size - 1
            player.seekTo(previousPosition, 0L)
        })
    }

    fun next() {
        uris.fold({
            Log.e(TAG, "next Error : can not skip to next")
        }, {
            var nextPosition = player.currentWindowIndex + 1
            if (nextPosition > (it.size - 1)) nextPosition = 0
            player.seekTo(nextPosition, 0L)
        })

    }


    fun release() {
        player.release()
        if (wifiLock.isHeld) wifiLock.release()
    }

    fun stop() {
        service.stopForeground(true)
        player.stop()
        service.stopSelf()
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION") audioManager.abandonAudioFocus(this)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (playOnAudioFocus && !player.playWhenReady) play()
                playOnAudioFocus = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                playOnAudioFocus = true
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                playOnAudioFocus = false
                pause()
            }
        }
    }


    open class PlayerListenerAdapter : Player.EventListener {
        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
        override fun onSeekProcessed() {}
        override fun onTracksChanged(
            trackGroups: TrackGroupArray?,
            trackSelections: TrackSelectionArray?
        ) {
        }

        override fun onPlayerError(error: ExoPlaybackException) {}
        override fun onLoadingChanged(isLoading: Boolean) {}
        override fun onPositionDiscontinuity(reason: Int) {}
        override fun onRepeatModeChanged(repeatMode: Int) {}
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {}
    }
}