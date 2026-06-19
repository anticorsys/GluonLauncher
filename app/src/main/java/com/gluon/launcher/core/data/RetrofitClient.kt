package com.gluon.launcher.core.data

import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class RetrofitClient {

    companion object {
        const val BASE_URL = "https://gluoncore.ddns.net/"
    }

    // Использование AtomicReference обеспечивает Lock-Free потокобезопасность
    // и исключает накладные расходы на тяжелую синхронизацию в интерцепторе.
    private val authTokenReference = AtomicReference<String?>(null)

    fun setAuthToken(token: String?) {
        authTokenReference.set(token)
    }

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val customDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (_: UnknownHostException) {
                try {
                    InetAddress.getAllByName(hostname).toList()
                } catch (_: Exception) {
                    throw UnknownHostException("GluonCore Network Error: Сервер $hostname не найден в сети.")
                }
            }
        }
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("Accept", "application/json")
            .header("X-Gluon-App", "Android-Core")

        val currentToken = authTokenReference.get()
        currentToken?.let {
            val tokenValue = if (it.startsWith("Bearer ")) it else "Bearer $it"
            requestBuilder.header("Authorization", tokenValue)
        }

        chain.proceed(requestBuilder.build())
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(customDns)
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(jsonConfig.asConverterFactory(contentType))
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}