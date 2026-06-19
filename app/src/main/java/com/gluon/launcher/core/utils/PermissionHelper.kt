package com.gluon.launcher.core.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

object PermissionHelper {

    @Composable
    fun rememberImagePickerPermissionLauncher(
        onPermissionGranted: () -> Unit,
        onPermissionDenied: () -> Unit = {}
    ): () -> Unit {
        val context = androidx.compose.ui.platform.LocalContext.current

        // Разрешение зависит от версии Android
        val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                onPermissionGranted()
            } else {
                onPermissionDenied()
            }
        }

        return {
            when {
                hasImagePickerPermission(context) -> onPermissionGranted()
                else -> launcher.launch(requiredPermission)
            }
        }
    }

    fun hasImagePickerPermission(context: Context): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}