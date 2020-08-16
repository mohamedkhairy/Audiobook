package com.example.audiobook.main

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.audiobook.model.ListenHubAudioBooks

class MainViewModel : ViewModel() {

    val liveNowPlaying = MutableLiveData<Pair<ListenHubAudioBooks , String>>()

    fun getNowPlaying() = liveNowPlaying

    fun updateNowPlaying(book: ListenHubAudioBooks , currentBookIndex: String) {
        liveNowPlaying.postValue(Pair(book , currentBookIndex))
    }
}