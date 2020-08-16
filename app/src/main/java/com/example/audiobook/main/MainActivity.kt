package com.example.audiobook.main

import android.annotation.SuppressLint
import android.content.ComponentName
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.audiobook.R
import com.example.audiobook.home.HomeFragment
import com.example.audiobook.model.ListenHubAudioBooks
import com.example.audiobook.player.PlayerFragment
import com.example.audiobook.service.PlayerService
import com.example.audiobook.utils.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mediaBrowserCompat: MediaBrowserCompat
    lateinit var mediaControllerCompat: MediaControllerCompat
    private lateinit var currentBook: ListenHubAudioBooks
    private lateinit var currentIndex: String

    private lateinit var playerFragment: PlayerFragment
    val mainViewModel: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        addHomeFragment()
        connectWithPlayerService()
        setupNowPlayingObservers()
        setListener()
    }

    private fun setListener() {
        now_playing.setOnClickListener {
            goToPlayer(currentIndex, currentBook)
        }
    }

    fun addHomeFragment() {
        addFragment(lazy {
            HomeFragment()
        }, HomeFragment.TAG)

    }


    private fun connectWithPlayerService() {
        val serviceComponent = ComponentName(this, PlayerService::class.java)
        mediaBrowserCompat = MediaBrowserCompat(this, serviceComponent, connectionCallback, null)
        mediaBrowserCompat.connect()
    }

    private val connectionCallback by lazy {
        object : MediaBrowserCompat.ConnectionCallback() {
            override fun onConnected() {
                mediaControllerCompat =
                    MediaControllerCompat(this@MainActivity, mediaBrowserCompat.sessionToken)
                mediaControllerCompat.registerCallback(mCallback)
            }

            override fun onConnectionSuspended() {
            }

            override fun onConnectionFailed() {
            }
        }
    }

    private val mCallback: MediaControllerCompat.Callback by lazy {
        object : MediaControllerCompat.Callback() {

            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {

                state?.let { playbackState ->
                    when (playbackState.state) {
                        PlaybackStateCompat.STATE_PLAYING -> {
                            if (!playerFragment.isAdded && ::currentBook.isInitialized)
                                showNowPlayeng()
                        }
                        PlaybackStateCompat.STATE_STOPPED -> {
                            hideNowPlayeng()
                        }
                        PlaybackStateCompat.STATE_PAUSED -> {
                            if (!playerFragment.isAdded && ::currentBook.isInitialized)
                                showNowPlayeng()
                        }
                        PlaybackStateCompat.STATE_ERROR -> {
                            hideNowPlayeng()
                        }
                        PlaybackStateCompat.STATE_BUFFERING -> {
                            if (!playerFragment.isAdded && ::currentBook.isInitialized)
                                showNowPlayeng()
                        }
                        else -> {
                            hideNowPlayeng()
                        }

                    }
                }
            }
        }
    }

    private fun setupNowPlayingObservers() {
        mainViewModel.getNowPlaying().observe(this, Observer {
            currentBook = it.first
            currentIndex = it.second
        })
    }

    fun goToPlayer(chapterIndex: String?, book: ListenHubAudioBooks?) {
        playerFragment = PlayerFragment()
        val bundle = Bundle()
        bundle.putParcelable(BOOK_DATA, book)
        bundle.putString(SKIP_TO_CHAPTER, chapterIndex.toString())
        playerFragment.arguments = bundle
        if (playerFragment.isAdded) return
        addFragment(lazy {
            playerFragment
        }, PlayerFragment.TAG)

    }

    @SuppressLint("RestrictedApi")
    fun hideNowPlayeng() {
        now_playing.visibility = View.INVISIBLE
    }

    @SuppressLint("RestrictedApi")
    fun showNowPlayeng() {
        now_playing.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaBrowserCompat.isInitialized) mediaBrowserCompat.disconnect()
        if (::mediaControllerCompat.isInitialized) mediaControllerCompat.unregisterCallback(
            mCallback
        )
    }
}
