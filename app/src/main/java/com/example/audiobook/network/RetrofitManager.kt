package com.example.audiobook.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.example.audiobook.BaseApplication
import com.example.audiobook.model.Book
import com.example.audiobook.utils.isNotNull
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import kotlinx.coroutines.Deferred
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object RetrofitManager {


    private const val BASE_URL =
        "https://firebasestorage.googleapis.com/v0/b/audiobooksapplication.appspot.com/o/"

    private val app: Application = BaseApplication.app

    private val endPoints: () -> EndPoints = { retrofit().create(EndPoints::class.java) }

    private fun retrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient().build())
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    private fun connectivityInterceptor(): Interceptor {
        return Interceptor { chain: Interceptor.Chain ->
            try {
                if (isNotInternetConnected()) throw NoInternetException()
                return@Interceptor chain.proceed(chain.request().newBuilder().build())
            } catch (e: Exception) {
                e.printStackTrace()
                throw NoInternetException()
            }
        }
    }


    private fun httpClient(): OkHttpClient.Builder =
        OkHttpClient.Builder().addConnectivityInterceptor()


    private fun OkHttpClient.Builder.addConnectivityInterceptor(): OkHttpClient.Builder =
        addInterceptor(connectivityInterceptor())

    private fun Application.isInternetConnected(): Boolean {
        val connMgr: ConnectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        return networkInfo.isNotNull() && networkInfo!!.isConnected
    }

    private fun isNotInternetConnected(): Boolean =
        !app.isInternetConnected()

    class NoInternetException : IOException() {
        override val message: String = "No internet connection"
    }

    object CallAPI {
        fun callBooksAsync(): Deferred<Book> =
            endPoints().getBooksAsync()
    }
}