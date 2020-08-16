package com.example.audiobook.network

import com.example.audiobook.model.Book
import kotlinx.coroutines.Deferred
import retrofit2.http.GET

interface EndPoints {

    @GET("ListenHub1593292901394.json?alt=media&token=6076663c-18b9-46f9-a5d5-2823b093a5b2")
    fun getBooksAsync(): Deferred<Book>
}