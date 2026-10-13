package com.parqueadero.appparqueadero.data.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

data class SesionGuardada(
    val token: String,
    /** Instante de vencimiento en milisegundos epoch, o null si el backend no lo envió. */
    val expiraEnMillis: Long?,
)

/** Persistencia de la sesión. Se abstrae para poder probar [SessionManager] sin Android. */
interface SesionStorage {
    suspend fun leer(): SesionGuardada?
    suspend fun guardar(sesion: SesionGuardada)
    suspend fun borrar()
}

private val Context.sesionDataStore by preferencesDataStore(name = "sesion")

/** Guarda el token en DataStore cifrado con una clave del Android Keystore; nunca en claro. */
class DataStoreSesionStorage(
    context: Context,
    private val cipher: KeystoreCipher = KeystoreCipher(),
) : SesionStorage {

    private val store = context.applicationContext.sesionDataStore

    override suspend fun leer(): SesionGuardada? {
        val prefs = store.data.first()
        val cifrado = prefs[TOKEN] ?: return null
        val token = cipher.descifrar(cifrado) ?: return null
        return SesionGuardada(token, prefs[EXPIRA])
    }

    override suspend fun guardar(sesion: SesionGuardada) {
        val cifrado = cipher.cifrar(sesion.token)
        store.edit { prefs ->
            prefs[TOKEN] = cifrado
            val expira = sesion.expiraEnMillis
            if (expira != null) prefs[EXPIRA] = expira else prefs.remove(EXPIRA)
        }
    }

    override suspend fun borrar() {
        store.edit { it.clear() }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token_cifrado")
        val EXPIRA = longPreferencesKey("expira_en")
    }
}
