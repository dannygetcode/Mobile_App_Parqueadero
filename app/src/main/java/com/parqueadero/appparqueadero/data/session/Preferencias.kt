package com.parqueadero.appparqueadero.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.parqueadero.appparqueadero.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Ajustes no sensibles del dispositivo. */
interface Preferencias {
    val tema: Flow<ThemeMode>
    suspend fun guardarTema(modo: ThemeMode)

    /** Último teléfono con el que se inició sesión, para prellenar el login. */
    val telefonoRecordado: Flow<String?>
    suspend fun recordarTelefono(telefono: String)
}

private val Context.ajustesDataStore by preferencesDataStore(name = "ajustes")

class DataStorePreferencias(context: Context) : Preferencias {

    private val store = context.applicationContext.ajustesDataStore

    override val tema: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[TEMA]?.let { valor -> ThemeMode.entries.firstOrNull { it.name == valor } } ?: ThemeMode.SISTEMA
    }

    override suspend fun guardarTema(modo: ThemeMode) {
        store.edit { it[TEMA] = modo.name }
    }

    override val telefonoRecordado: Flow<String?> = store.data.map { it[TELEFONO] }

    override suspend fun recordarTelefono(telefono: String) {
        store.edit { it[TELEFONO] = telefono }
    }

    private companion object {
        val TEMA = stringPreferencesKey("tema")
        val TELEFONO = stringPreferencesKey("telefono")
    }
}
