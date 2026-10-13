package com.parqueadero.appparqueadero

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras

class CupoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Acceso al contenedor desde las fábricas de ViewModel (`viewModelFactory { initializer { ... } }`). */
val CreationExtras.container: AppContainer
    get() = (this[APPLICATION_KEY] as CupoApp).container
