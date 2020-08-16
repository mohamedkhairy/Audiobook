package com.example.audiobook.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import arrow.core.Either
import kotlinx.coroutines.Dispatchers


class HomeViewModel : ViewModel() {

    val homeRepository = HomeRepository()

    fun getBooks() = liveData(Dispatchers.IO) {
        val result = homeRepository.getBookFromBackend()
        when (result) {
            is Either.Right -> {
                emit(result.b)
            }
            is Either.Left -> {
                emit(result.a)
            }
        }
    }
}