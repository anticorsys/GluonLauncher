// app/src/main/java/com/gluon/launcher/core/auth/ProfileManager.kt
package com.gluon.launcher.core.auth

import android.content.Context
import android.net.Uri
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.gluon.launcher.core.data.ProfileRepository
import com.gluon.launcher.core.data.UserItem
import com.gluon.launcher.core.utils.AvatarUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileManager(
    private val authManager: AuthManager,
    private val profileRepository: ProfileRepository
) {

    val currentUser = authManager.currentUser

    private fun getUserId(): String? = authManager.currentUser.value?.id

    @OptIn(ExperimentalCoilApi::class)
    suspend fun deleteAvatar(context: Context): Result<UserItem> = withContext(Dispatchers.IO) {
        val userId = getUserId() ?: return@withContext Result.failure(Exception("Ошибка: отсутствует ID пользователя"))
        val appContext = context.applicationContext

        val response = profileRepository.forceDeleteAvatar(userId, mapOf("avatar" to ""))
        if (response.isSuccess) {
            authManager.currentUser.value?.getAvatarUrl()?.let { url ->
                appContext.imageLoader.diskCache?.remove(url)
                appContext.imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(url))
            }
            val updatedUser = response.getOrNull()!!.copy(avatar = "")
            authManager.updateLocalUser(updatedUser)
            Result.success(updatedUser)
        } else {
            response
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    suspend fun updateProfile(
        context: Context,
        newName: String,
        newGluonId: String,
        gender: String,
        bio: String,
        imageUri: Uri? = null
    ): Result<UserItem> = withContext(Dispatchers.IO) {
        val userId = getUserId() ?: return@withContext Result.failure(Exception("Ошибка: отсутствует ID пользователя"))
        val appContext = context.applicationContext
        var tempFile: File? = null

        try {
            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("full_name", newName.trim())
                .addFormDataPart("gluon_id", newGluonId.trim())
                .addFormDataPart("gender", gender)
                .addFormDataPart("bio", bio.trim())

            imageUri?.let { uri ->
                tempFile = AvatarUtils.processImageUri(appContext, uri)
                tempFile?.let { file ->
                    val reqFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    multipartBuilder.addFormDataPart("avatar", file.name, reqFile)

                    authManager.currentUser.value?.getAvatarUrl()?.let { oldUrl ->
                        appContext.imageLoader.diskCache?.remove(oldUrl)
                        appContext.imageLoader.memoryCache?.remove(coil.memory.MemoryCache.Key(oldUrl))
                    }
                }
            }

            val response = profileRepository.updateProfile(userId, multipartBuilder.build())
            if (response.isSuccess) {
                val updatedUser = response.getOrNull()!!
                authManager.updateLocalUser(updatedUser)
                Result.success(updatedUser)
            } else {
                response
            }
        } finally {
            tempFile?.delete()
        }
    }

    suspend fun changePassword(
        oldPass: String,
        newPass: String,
        confirmPass: String
    ): Result<UserItem> = withContext(Dispatchers.IO) {
        val userId = getUserId() ?: return@withContext Result.failure(Exception("Ошибка: отсутствует ID пользователя"))

        val body = mapOf(
            "oldPassword" to oldPass,
            "password" to newPass,
            "passwordConfirm" to confirmPass
        )

        val response = profileRepository.updatePasswordJson(userId, body)
        if (response.isSuccess) {
            val updated = response.getOrNull()!!
            authManager.updateLocalUser(updated)
            Result.success(updated)
        } else {
            response
        }
    }

    suspend fun deleteAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = getUserId() ?: return@withContext Result.failure(Exception("Ошибка: отсутствует ID пользователя"))
        val response = profileRepository.deleteAccount(userId)
        if (response.isSuccess) {
            authManager.clearData()
        }
        response
    }
}