// app/src/main/java/com/gluon/launcher/core/data/Models.kt
package com.gluon.launcher.core.data

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val GLUON_DOMAIN = "https://gluoncore.ddns.net"

@Keep
@Serializable
@Parcelize
data class UserItem(
    @SerialName("id") val id: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("gender") val gender: String = "Male",
    @SerialName("gluon_id") val gluonId: String = "",
    @SerialName("bio") val bio: String = "",
    @SerialName("avatar") val avatar: String = "",
    @SerialName("collectionId") val collectionId: String = "",
    @SerialName("collectionName") val collectionName: String = "Gluon_Database",
    @SerialName("created") val created: String = "",
    @SerialName("updated") val updated: String = ""
) : Parcelable {

    fun getAvatarUrl(): String? {
        if (avatar.isBlank() || id.isBlank()) return null
        // Если бекенд не вернул collectionName, используем значение по умолчанию, чтобы аватар не ломался
        val cName = collectionName.takeIf { it.isNotBlank() } ?: "Gluon_Database"
        val numericTimestamp = updated.filter { it.isDigit() }
        val version = if (numericTimestamp.length >= 13) numericTimestamp.takeLast(13) else numericTimestamp
        return "$GLUON_DOMAIN/api/files/$cName/$id/$avatar?v=$version"
    }

    @Suppress("unused")
    fun isProfileComplete(): Boolean =
        fullName.trim().isNotBlank() && gluonId.trim().isNotBlank()
}

@Keep
@Serializable
@Parcelize
data class AuthResponse(
    @SerialName("token") val token: String = "",
    @SerialName("record") val record: UserItem = UserItem()
) : Parcelable

@Keep
@Serializable
@Parcelize
data class UserResponse(
    @SerialName("page") val page: Int = 1,
    @SerialName("perPage") val perPage: Int = 30,
    @SerialName("totalItems") val totalItems: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("items") val items: List<UserItem> = emptyList()
) : Parcelable

@Immutable
data class AppModel(
    val label: String,
    val packageName: String,
    val installTime: Long = 0L,
    val isSystem: Boolean = false,
    val usageCount: Int = 0,
    val iconBitmap: ImageBitmap? = null
)