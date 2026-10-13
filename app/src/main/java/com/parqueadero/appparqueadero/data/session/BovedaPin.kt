package com.parqueadero.appparqueadero.data.session

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Resultado de preparar el cifrador para leer el PIN con la huella. */
sealed interface PreparacionHuella {
    data class Lista(val cifrador: Cipher) : PreparacionHuella
    /** Se registró o borró una huella en el teléfono: la clave quedó inválida y se borró la bóveda. */
    data object Invalidada : PreparacionHuella
    data object NoConfigurada : PreparacionHuella
}

/**
 * Guarda el PIN del usuario cifrado con una clave del Android Keystore que solo se puede usar tras
 * autenticar con huella (BiometricPrompt + CryptoObject). Con la huella se descifra el PIN y se hace
 * un login normal, así que el token siempre es nuevo. Se abstrae para probar los ViewModels sin Android.
 */
interface BovedaPin {
    val configurada: Flow<Boolean>
    suspend fun telefono(): String?

    /** Crea una clave nueva y devuelve un cifrador listo para autenticar con huella, o null si falla. */
    fun cifradorParaGuardar(): Cipher?
    suspend fun cifradorParaLeer(): PreparacionHuella

    /** `cifrador` debe venir autenticado por BiometricPrompt. */
    suspend fun guardar(cifrador: Cipher, telefono: String, pin: String): Boolean
    suspend fun leer(cifrador: Cipher): String?
    suspend fun borrar()
}

private val Context.huellaDataStore by preferencesDataStore(name = "huella")

class KeystoreBovedaPin(context: Context) : BovedaPin {

    private val store = context.applicationContext.huellaDataStore

    override val configurada: Flow<Boolean> = store.data.map { it[PIN] != null && it[IV] != null }

    override suspend fun telefono(): String? = store.data.first()[TELEFONO]

    override fun cifradorParaGuardar(): Cipher? = try {
        keyStore().deleteEntry(ALIAS)
        Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, generarClave()) }
    } catch (_: Exception) {
        null
    }

    override suspend fun cifradorParaLeer(): PreparacionHuella {
        val prefs = store.data.first()
        val iv = prefs[IV]?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return PreparacionHuella.NoConfigurada
        val clave = keyStore().getKey(ALIAS, null) as? SecretKey
        if (clave == null) {
            borrar()
            return PreparacionHuella.Invalidada
        }
        return try {
            PreparacionHuella.Lista(
                Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.DECRYPT_MODE, clave, GCMParameterSpec(128, iv)) }
            )
        } catch (_: KeyPermanentlyInvalidatedException) {
            borrar()
            PreparacionHuella.Invalidada
        } catch (_: Exception) {
            borrar()
            PreparacionHuella.Invalidada
        }
    }

    override suspend fun guardar(cifrador: Cipher, telefono: String, pin: String): Boolean = try {
        val cifrado = cifrador.doFinal(pin.toByteArray(Charsets.UTF_8))
        store.edit {
            it[TELEFONO] = telefono
            it[PIN] = Base64.encodeToString(cifrado, Base64.NO_WRAP)
            it[IV] = Base64.encodeToString(cifrador.iv, Base64.NO_WRAP)
        }
        true
    } catch (_: Exception) {
        false
    }

    override suspend fun leer(cifrador: Cipher): String? = try {
        val cifrado = store.data.first()[PIN]?.let { Base64.decode(it, Base64.NO_WRAP) }
        cifrado?.let { String(cifrador.doFinal(it), Charsets.UTF_8) }
    } catch (_: Exception) {
        null
    }

    override suspend fun borrar() {
        store.edit { it.clear() }
        try {
            keyStore().deleteEntry(ALIAS)
        } catch (_: Exception) {
        }
    }

    private fun keyStore() = KeyStore.getInstance(PROVEEDOR).apply { load(null) }

    private fun generarClave(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            // Una huella nueva en el teléfono invalida la clave: nadie puede añadir su dedo y entrar.
            .setInvalidatedByBiometricEnrollment(true)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                }
            }
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEEDOR).run {
            init(spec)
            generateKey()
        }
    }

    private companion object {
        const val PROVEEDOR = "AndroidKeyStore"
        const val ALIAS = "cupo_huella"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        val TELEFONO = stringPreferencesKey("telefono")
        val PIN = stringPreferencesKey("pin_cifrado")
        val IV = stringPreferencesKey("iv")
    }
}
