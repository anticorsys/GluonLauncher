// app/src/main/java/com/gluon/launcher/core/data/ProfileRepository.kt
package com.gluon.launcher.core.data

import okhttp3.RequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface ProfileRepository {
    suspend fun updateProfile(userId: String, body: RequestBody): Result<UserItem>
    suspend fun updatePasswordJson(userId: String, body: Map<String, String?>): Result<UserItem>
    suspend fun forceDeleteAvatar(userId: String, body: Map<String, String?>): Result<UserItem>
    suspend fun deleteAccount(userId: String): Result<Unit>
}

class ProfileRepositoryImpl(private val api: ApiService) : ProfileRepository {

    private fun handleNetworkError(e: Exception): String {
        return when (e) {
            is SocketTimeoutException -> "Превышено время ожидания. Проверьте подключение."
            is UnknownHostException -> "Нет сети. Проверьте интернет-соединение."
            is IOException -> "Ошибка сети: ${e.message}"
            else -> e.message ?: "Неизвестная ошибка"
        }
    }

    override suspend fun updateProfile(userId: String, body: RequestBody): Result<UserItem> {
        return try {
            val response = api.updateProfile(userId, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Ошибка при обновлении профиля"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        }
    }

    override suspend fun updatePasswordJson(userId: String, body: Map<String, String?>): Result<UserItem> {
        return try {
            val response = api.updatePasswordJson(userId, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = when (response.code()) {
                    400 -> "Неверный текущий пароль"
                    else -> "Ошибка сервера при смене пароля"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        }
    }

    override suspend fun forceDeleteAvatar(userId: String, body: Map<String, String?>): Result<UserItem> {
        return try {
            val response = api.forceDeleteAvatar(userId, body)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Ошибка сервера при удалении аватара"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        }
    }

    override suspend fun deleteAccount(userId: String): Result<Unit> {
        return try {
            val response = api.deleteAccount(userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Не удалось удалить аккаунт"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        }
    }
}