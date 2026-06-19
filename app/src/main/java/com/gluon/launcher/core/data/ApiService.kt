// app/src/main/java/com/gluon/launcher/core/data/ApiService.kt
package com.gluon.launcher.core.data

import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("api/collections/Gluon_Database/auth-with-password")
    suspend fun login(@Body body: Map<String, String>): Response<AuthResponse>

    @POST("api/collections/Gluon_Database/records")
    suspend fun register(@Body body: RequestBody): Response<UserItem>

    @POST("api/collections/Gluon_Database/request-verification")
    suspend fun requestVerification(@Body body: Map<String, String>): Response<Unit>

    @POST("api/collections/Gluon_Database/request-password-reset")
    suspend fun requestPasswordReset(@Body body: Map<String, String>): Response<Unit>

    @Suppress("unused")
    @POST("api/collections/Gluon_Database/confirm-password-reset")
    suspend fun confirmPasswordReset(@Body body: Map<String, String>): Response<Unit>

    @GET("api/collections/Gluon_Database/records")
    suspend fun getUserByEmail(@Query("filter") filter: String): Response<UserResponse>

    @PATCH("api/collections/Gluon_Database/records/{id}")
    suspend fun updateProfile(
        @Path("id") id: String,
        @Body body: RequestBody
    ): Response<UserItem>

    @PATCH("api/collections/Gluon_Database/records/{id}")
    suspend fun updatePasswordJson(
        @Path("id") id: String,
        @Body body: Map<String, String?>
    ): Response<UserItem>

    @PATCH("api/collections/Gluon_Database/records/{id}")
    suspend fun forceDeleteAvatar(
        @Path("id") id: String,
        @Body body: Map<String, String?>
    ): Response<UserItem>

    @DELETE("api/collections/Gluon_Database/records/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<Unit>
}