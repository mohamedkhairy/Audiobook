package com.example.audiobook.player

import android.content.*
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.audiobook.R
import com.example.audiobook.main.MainActivity
import com.example.audiobook.model.ListenHubAudioBooks
import com.example.audiobook.model.SeekBarUpdates
import com.example.audiobook.service.PlayerService
import com.example.audiobook.utils.*
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.coroutines.*

class PlayerFragment : Fragment() {


    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaBrowser: MediaBrowserCompat
    private var chapterIdToSkipTo = INVALID_CHAPTER_ID


    private val progreaaIntentFilter: IntentFilter = IntentFilter()
    private val progressBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getParcelableExtra<SeekBarUpdates>(PROGRESS_DATA)?.let {
                updatedSeekBarUpdates(it)
            }
        }
    }


    private val currentBook: ListenHubAudioBooks by lazy {
        arguments?.getParcelable<ListenHubAudioBooks>(BOOK_DATA) as ListenHubAudioBooks
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(progressBroadcastReceiver, progreaaIntentFilter)
        (activity as? MainActivity)?.hideNowPlayeng()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progreaaIntentFilter.addAction(PROGRESS_KEY)
        iniMediaBrowser()
        mediaBrowser.connect()
        chapterIdToSkipTo = arguments?.getString(SKIP_TO_CHAPTER, INVALID_CHAPTER_ID)
            ?: INVALID_CHAPTER_ID
    }

    private fun iniMediaBrowser() {
        val serviceComponent = context?.let { ComponentName(it, PlayerService::class.java) }
        mediaBrowser = MediaBrowserCompat(activity, serviceComponent, mediaBrowserCallback, null)
    }


    private val mediaBrowserCallback: MediaBrowserCompat.ConnectionCallback =
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                onMediaBrowserServiceConnected()
                handleUIState()
                if (::mediaController.isInitialized) {

                    if (!isPlayingSameMedia()) {
                        GlobalScope.launch {
                            preparePlayer()
                            play()
                        }
                    }
                }
            }


        }


    private fun isPlayingSameMedia(): Boolean {
        if (!::mediaController.isInitialized || mediaController.metadata == null) return false
        else if (currentBook.chapters[chapterIdToSkipTo.toInt()].link == mediaController.metadata.getString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_URI
            )
        ) {
            return true
        }
        return false
    }


    private val mediaControllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            restorePlayerTitleState()

        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            when (state.state) {
                PlaybackStateCompat.STATE_PAUSED -> {
                    pauseView()
                    hideLoading()
                }
                PlaybackStateCompat.STATE_PLAYING -> {
                    playView()
                    hideLoading()
                }
                PlaybackStateCompat.STATE_BUFFERING -> {
                    showLoading()
                }
                PlaybackStateCompat.STATE_ERROR -> {
                    pauseView()
                    hideLoading()
                }
                PlaybackStateCompat.STATE_STOPPED -> {
                    pauseView()
                    hideLoading()
                }
            }
        }
    }


    private fun updatedSeekBarUpdates(seekBarUpdates: SeekBarUpdates) {
        seekBarUpdates.duration?.let { player_seek_bar.max = it.toInt() }
        player_seek_bar.progress = seekBarUpdates.position
        player_position.text = seekBarUpdates.position.toLong().formatTime()
    }

    private fun setListeners() {
        play_pause.setOnClickListener {
            if (isPlaying()) {
                mediaController.transportControls.pause()
            } else {
                if (mediaController.playbackState?.state == PlaybackStateCompat.STATE_ERROR) mediaController.transportControls.prepare()
                mediaController.transportControls.play()
            }
        }

        player_previous.setOnClickListener {
            mediaController.transportControls.skipToPrevious()
        }

        player_next.setOnClickListener {
            mediaController.transportControls.skipToNext()

        }
    }

    private fun restorePlayPauseState() {
        if (isPlaying()) playView() else pauseView()
    }


    private fun pauseView() {
        play_pause.setImageResource(R.mipmap.play)
    }

    private fun playView() {
        play_pause.setImageResource(R.mipmap.pause)
    }

    private fun isPlaying(): Boolean {
        if (!::mediaController.isInitialized || mediaController.metadata == null) return false
        return if (mediaController.playbackState == null) false else (mediaController.playbackState.state == PlaybackStateCompat.STATE_PLAYING || mediaController.playbackState.state == PlaybackStateCompat.STATE_BUFFERING)
    }

    private fun hideLoading() {
        player_loading.visibility = View.INVISIBLE
    }

    private fun showLoading() {
        player_loading.visibility = View.VISIBLE
    }

    private suspend fun preparePlayer() {
        GlobalScope.async {
            val bundle = Bundle()
            bundle.putParcelable(BOOK_DATA, currentBook)
            currentBook?.let {
                val chapterList = it.chapters
                if (chapterList.isNullOrEmpty()) return@async
                mediaController.transportControls.prepareFromMediaId(chapterIdToSkipTo, bundle)
            }
        }.await()
    }

    private fun onMediaBrowserServiceConnected() {
        val hostActivity = activity ?: return
        val intent = Intent(hostActivity, PlayerService::class.java)
        hostActivity.startService(intent)
        mediaController = MediaControllerCompat(activity, mediaBrowser.sessionToken)
        mediaController.registerCallback(mediaControllerCallback)
        MediaControllerCompat.setMediaController(hostActivity, mediaController)
        mediaController = MediaControllerCompat.getMediaController(hostActivity)
    }

    private fun restorePlayerTitleState() {
        mediaController.metadata?.let {
            with(it) {
                player_book_title.text = currentBook.title
                player_author.text = currentBook.authors[0].firstName
                player_chapter_title.text =
                    getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                player_narrator.text = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                player_duration.text =
                    getLong(MediaMetadataCompat.METADATA_KEY_DURATION).formatTime()
                player_position.text = "00:00:00"
                player_Image.loadAsyncImage(currentBook.imageUrl)
            }
        }
    }


    private fun handleUIState() {
        (activity as? MainActivity)?.hideNowPlayeng()

        GlobalScope.launch(Dispatchers.Main + CoroutineExceptionHandler { _, e ->
        }) {
            restorePlayPauseState()
            restorePlayerTitleState()
            restoreDurationState(mediaController)
            restorePositionState(mediaController)
        }
    }

    private fun restoreDurationState(mediaController: MediaControllerCompat) {

        val durationInMillis =
            mediaController.metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                ?: 0
        player_duration.text = durationInMillis.formatTime()
        player_seek_bar.max = durationInMillis.toInt()
    }

    private fun restorePositionState(mediaController: MediaControllerCompat) {
        val positionInMillis =
            mediaController.playbackState?.position
                ?: 0
        player_seek_bar.progress = positionInMillis.toInt()
    }


    private fun play() {
        mediaController.transportControls.play()
    }

    override fun onPause() {
        super.onPause()
        activity?.unregisterReceiver(progressBroadcastReceiver)
        (activity as? MainActivity)?.showNowPlayeng()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanUp()
    }

    private fun cleanUp() {
        if (::mediaController.isInitialized) mediaController.unregisterCallback(
            mediaControllerCallback
        )
        if (::mediaBrowser.isInitialized) mediaBrowser.disconnect()
    }


    companion object {

        const val TAG = "PlayerFragment"

    }

}