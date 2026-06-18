package com.yourpax.app.data.api

import com.yourpax.app.util.Constants
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitProvider {

    private var currentBaseUrl: String = "http://${Constants.DEFAULT_IP}:${Constants.DEFAULT_PORT}/"
    private var apiService: YourPaxApiService? = null

    fun updateBaseUrl(ip: String, port: Int = Constants.DEFAULT_PORT) {
        val newUrl = "http://$ip:$port/"
        if (newUrl != currentBaseUrl) {
            currentBaseUrl = newUrl
            apiService = null
        }
    }

    fun getApiService(): YourPaxApiService {
        if (apiService == null) {
            val authInterceptor = Interceptor { chain ->
                val token = CsrfTokenManager.token
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("X-CSRF-Token", token)
                    .build()
                chain.proceed(request)
            }

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(YourPaxApiService::class.java)
        }
        return apiService!!
    }

    fun reset() {
        apiService = null
    }
}
