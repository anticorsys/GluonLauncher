// app/src/main/java/com/gluon/launcher/core/auth/AuthManager.kt
package com.gluon.launcher.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.gluon.launcher.core.data.RetrofitClient
import com.gluon.launcher.core.data.UserItem
import com.gluon.launcher.core.utils.AvatarUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.Duration.Companion.milliseconds

class AuthManager(context: Context, private val retrofitClient: RetrofitClient) {

    private val appContext = context.applicationContext
    private val sessionMutex = Mutex()

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("AuthManager", "Unhandled coroutine exception", throwable)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

    private val masterKeyAlias: String by lazy {
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    }

    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            Log.e("AuthManager", "EncryptedSharedPreferences corruption detected. Recreating...", e)
            val prefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists()) {
                File(prefsDir, "gluon_secure_prefs.xml").delete()
                File(prefsDir, "__androidx_security_crypto_encrypted_prefs_key_keyset__.xml").delete()
                File(prefsDir, "__androidx_security_crypto_encrypted_prefs_value_keyset__.xml").delete()
            }
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        return EncryptedSharedPreferences.create(
            "gluon_secure_prefs",
            masterKeyAlias,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _currentUser = MutableStateFlow<UserItem?>(null)
    val currentUser: StateFlow<UserItem?> = _currentUser.asStateFlow()

    private val _isGuest = MutableStateFlow(false)
    val isGuest: StateFlow<Boolean> = _isGuest.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        scope.launch {
            loadSession()
            if (_currentUser.value != null) {
                validateSession()
            }
            _isReady.value = true
        }
    }

    fun isLoggedIn(): Boolean = _currentUser.value != null && !_isGuest.value

    fun isGuestMode(): Boolean = _isGuest.value

    private fun handleNetworkError(e: Exception): String {
        if (e is HttpException) {
            return when (e.code()) {
                400 -> "Некорректный запрос. Проверьте данные."
                401 -> "Сессия устарела. Пожалуйста, войдите заново."
                404 -> "Данные не найдены на сервере."
                500 -> "Внутренняя ошибка сервера. Попробуйте позже."
                else -> "Ошибка сервера (${e.code()})"
            }
        }
        return when (e) {
            is SocketTimeoutException -> "Превышено время ожидания. Проверьте подключение."
            is UnknownHostException -> "Нет сети. Проверьте интернет-соединение."
            is IOException -> {
                val msg = e.message
                if (msg != null && msg.contains("SSL")) "Ошибка безопасности соединения."
                else "Ошибка сети: $msg"
            }
            else -> e.message ?: "Неизвестная ошибка"
        }
    }

    private suspend fun loadSession() = sessionMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val savedToken = prefs.getString("auth_token", null)
                val savedId = prefs.getString("user_id", null)
                val savedIsGuest = prefs.getBoolean("is_guest", false)

                if (!savedToken.isNullOrEmpty() && !savedId.isNullOrEmpty()) {
                    retrofitClient.setAuthToken(savedToken)
                    _currentUser.value = UserItem(
                        id = savedId,
                        fullName = prefs.getString("user_name", "") ?: "",
                        email = prefs.getString("user_email", "") ?: "",
                        gluonId = prefs.getString("gluon_id", "") ?: "",
                        collectionName = "Gluon_Database",
                        avatar = prefs.getString("user_avatar", "") ?: "",
                        verified = prefs.getBoolean("user_verified", false),
                        bio = prefs.getString("user_bio", "") ?: "",
                        gender = prefs.getString("gender", "Male") ?: "Male"
                    )
                    _isGuest.value = false
                } else if (savedIsGuest) {
                    _isGuest.value = true
                }
            } catch (e: Exception) {
                Log.e("AuthManager", "Failed to load session, clearing data", e)
                clearData()
            }
        }
    }

    private suspend fun validateSession() {
        val email = _currentUser.value?.email ?: return
        try {
            val response = retrofitClient.api.getUserByEmail("(email='${email.trim()}')")
            if (response.code() == 401) {
                Log.w("AuthManager", "Token expired (401), clearing session")
                clearData()
            }
        } catch (e: Exception) {
            Log.w("AuthManager", "Session validation network error", e)
        }
    }

    suspend fun login(email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = mapOf("identity" to email.trim(), "password" to pass.trim())
            val response = retrofitClient.api.login(body)
            val authData = response.body()

            if (response.isSuccessful && authData != null) {
                sessionMutex.withLock { saveSession(authData.token, authData.record) }
                return@withContext Result.success(Unit)
            }
            return@withContext Result.failure(Exception("Неверный email или пароль"))
        } catch (e: Exception) {
            return@withContext Result.failure(Exception(handleNetworkError(e)))
        }
    }

    suspend fun register(
        email: String,
        pass: String,
        name: String,
        gender: String,
        avatarUri: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (name.trim().isEmpty()) return@withContext Result.failure(Exception("Имя не может быть пустым"))

        val sanitizedPrefix = email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "")
        var gluonId = sanitizedPrefix + (1000..9999).random()

        if (gluonId.length < 5) {
            gluonId = "user_" + (100000..999999).random()
        } else if (gluonId.length > 25) {
            gluonId = gluonId.take(25)
        }

        if (!gluonId.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            return@withContext Result.failure(Exception("Некорректный gluon_id. Используйте латинские буквы, цифры и _"))
        }

        var tempFile: File? = null
        try {
            val multipartBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("email", email.trim())
                .addFormDataPart("password", pass.trim())
                .addFormDataPart("passwordConfirm", pass.trim())
                .addFormDataPart("full_name", name.trim())
                .addFormDataPart("gender", gender)
                .addFormDataPart("gluon_id", gluonId)
                .addFormDataPart("emailVisibility", "true")

            avatarUri?.let { uri ->
                tempFile = AvatarUtils.processImageUri(appContext, uri)
                tempFile?.let { file ->
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    multipartBuilder.addFormDataPart("avatar", file.name, requestFile)
                }
            }

            val regResponse = retrofitClient.api.register(multipartBuilder.build())

            if (regResponse.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = regResponse.errorBody()?.string() ?: "Ошибка регистрации"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        } finally {
            tempFile?.delete()
        }
    }

    suspend fun checkEmailVerificationStatus(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = retrofitClient.api.getUserByEmail("(email='${email.trim()}')")
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val user = body.items.firstOrNull()
                if (user != null) {
                    if (user.verified) {
                        updateLocalUser(user)
                    }
                    Result.success(user.verified)
                } else {
                    Result.success(false)
                }
            } else {
                Result.failure(Exception("Ошибка сервера"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = retrofitClient.api.requestPasswordReset(mapOf("email" to email.trim()))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Не удалось отправить письмо для сброса"))
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        }
    }

    suspend fun resendVerificationEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = retrofitClient.api.requestVerification(mapOf("email" to email.trim()))
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Не удалось повторно отправить письмо"))
        } catch (e: Exception) {
            Result.failure(Exception(handleNetworkError(e)))
        }
    }

    fun saveSession(token: String, user: UserItem) {
        retrofitClient.setAuthToken(token)
        prefs.edit {
            putString("auth_token", token)
            putString("user_id", user.id)
            putString("user_name", user.fullName)
            putString("user_email", user.email)
            putString("gluon_id", user.gluonId)
            putString("user_avatar", user.avatar)
            putString("user_bio", user.bio)
            putString("gender", user.gender)
            putBoolean("user_verified", user.verified)
            putBoolean("is_guest", false)
        }
        _currentUser.value = user
        _isGuest.value = false
    }

    fun updateLocalUser(user: UserItem) {
        prefs.edit {
            putString("user_name", user.fullName)
            putString("user_avatar", user.avatar)
            putString("gluon_id", user.gluonId)
            putString("user_email", user.email)
            putString("user_bio", user.bio)
            putString("gender", user.gender)
            putBoolean("user_verified", user.verified)
            putBoolean("is_guest", false)
        }
        _currentUser.value = user
        _isGuest.value = false
    }

    fun setGuestMode(isGuest: Boolean) {
        scope.launch(Dispatchers.Main) {
            if (isGuest) {
                retrofitClient.setAuthToken(null)
                prefs.edit {
                    remove("auth_token")
                    remove("user_id")
                    remove("user_name")
                    remove("user_email")
                    remove("gluon_id")
                    remove("user_avatar")
                    remove("user_bio")
                    remove("gender")
                    remove("user_verified")
                    putBoolean("is_guest", true)
                }
                _currentUser.value = null
            } else {
                prefs.edit { putBoolean("is_guest", false) }
            }
            _isGuest.value = isGuest
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearData() {
        scope.launch(Dispatchers.IO) {
            try {
                appContext.imageLoader.diskCache?.clear()
                appContext.imageLoader.memoryCache?.clear()
                delay(100.milliseconds)
                withContext(Dispatchers.Main) {
                    retrofitClient.setAuthToken(null)
                    prefs.edit { clear() }
                }
                _currentUser.value = null
                _isGuest.value = false
            } catch (_: Exception) {}
        }
    }
}