package com.gluon.launcher.core.di

import android.content.Context
import com.gluon.launcher.core.auth.AuthManager
import com.gluon.launcher.core.auth.ProfileManager
import com.gluon.launcher.core.data.ProfileRepository
import com.gluon.launcher.core.data.ProfileRepositoryImpl
import com.gluon.launcher.core.data.RetrofitClient
import com.gluon.launcher.core.data.repository.AppRepository
import com.gluon.launcher.core.data.repository.WorkspaceRepository
import com.gluon.launcher.core.theme.ThemeManager

/**
 * Единый контейнер зависимостей (Dependency Injection Container) приложения Gluon Launcher.
 * Управляет жизненным циклом и потокобезопасным созданием сервисов, репозиториев и менеджеров.
 */
class DiContainer(private val appContext: Context) {

    val retrofitClient: RetrofitClient by lazy {
        RetrofitClient()
    }

    val authManager: AuthManager by lazy {
        AuthManager(appContext, retrofitClient)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(retrofitClient.api)
    }

    val profileManager: ProfileManager by lazy {
        ProfileManager(authManager, profileRepository)
    }

    val themeManager: ThemeManager by lazy {
        ThemeManager(appContext)
    }

    val appRepository: AppRepository by lazy {
        AppRepository(appContext)
    }

    val workspaceRepository: WorkspaceRepository by lazy {
        WorkspaceRepository(appContext)
    }
}