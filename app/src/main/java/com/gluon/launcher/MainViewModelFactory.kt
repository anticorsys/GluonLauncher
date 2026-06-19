package com.gluon.launcher

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gluon.launcher.core.di.DiContainer

// Внедрен потокобезопасный Singleton для исключения утечек памяти (Memory Leaks)
object DiProvider {
    @Volatile
    private var instance: DiContainer? = null

    fun getInstance(application: Application): DiContainer {
        return instance ?: synchronized(this) {
            instance ?: DiContainer(application).also { instance = it }
        }
    }
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            // Подключаем наш DI Контейнер как Singleton
            val di = DiProvider.getInstance(application)

            @Suppress("UNCHECKED_CAST")
            return MainViewModel(
                application,
                di.authManager,
                di.themeManager,
                di.profileManager,
                di.appRepository,
                di.workspaceRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}