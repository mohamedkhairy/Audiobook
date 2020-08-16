package com.example.audiobook.home

import arrow.core.Either
import com.example.audiobook.model.Book
import com.example.audiobook.network.RetrofitManager
import com.example.audiobook.utils.Result
import com.example.audiobook.utils.callAPI

class HomeRepository {

    suspend fun getBookFromBackend(): Either<Result.Error, Result.Success<Book>> = callAPI {
        RetrofitManager.CallAPI.callBooksAsync()
    }
}