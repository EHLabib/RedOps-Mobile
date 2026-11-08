package com.redops.mobile.data.remote

import com.redops.mobile.data.remote.api.ToolkitApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(
    private val baseUrlProvider: () -> String,
    private val tokenProvider: () -> String
) {
    private var currentBaseUrl: String = ""
    private var currentToken: String = ""
    private var cachedService: ToolkitApiService? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = tokenProvider().trim()
        val builder = original.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            
        if (token.isNotEmpty()) {
            builder.header("Authorization", "Bearer $token")
        }
        
        chain.proceed(builder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Synchronized
    fun getService(): ToolkitApiService {
        val baseUrl = baseUrlProvider().let { if (!it.endsWith("/")) "$it/" else it }
        val token = tokenProvider()

        if (cachedService != null && currentBaseUrl == baseUrl && currentToken == token) {
            return cachedService!!
        }

        currentBaseUrl = baseUrl
        currentToken = token

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ToolkitApiService::class.java)
        cachedService = service
        return service
    }
}

