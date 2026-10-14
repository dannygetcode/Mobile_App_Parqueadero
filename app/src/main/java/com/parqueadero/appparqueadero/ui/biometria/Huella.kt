package com.parqueadero.appparqueadero.ui.biometria

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

enum class HardwareHuella {
    DISPONIBLE,

    /** Hay lector pero no hay huellas registradas en el teléfono. */
    SIN_HUELLAS,

    /** Sin lector o sin biometría fuerte: la opción se oculta. */
    NO_DISPONIBLE,
}

fun hardwareHuella(context: Context): HardwareHuella =
    when (BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG)) {
        BiometricManager.BIOMETRIC_SUCCESS -> HardwareHuella.DISPONIBLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> HardwareHuella.SIN_HUELLAS
        else -> HardwareHuella.NO_DISPONIBLE
    }

/** BiometricPrompt necesita una FragmentActivity (MainActivity lo es). */
tailrec fun Context.fragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.fragmentActivity()
    else -> null
}

/**
 * Muestra el diálogo del sistema para la huella con un [cifrador] del Keystore (CryptoObject).
 * [onExito] recibe el cifrador ya autenticado. [onFalla] recibe un mensaje, o null si el usuario canceló.
 */
fun pedirHuella(
    actividad: FragmentActivity,
    titulo: String,
    subtitulo: String,
    textoAlternativo: String,
    cifrador: Cipher,
    onExito: (Cipher) -> Unit,
    onFalla: (String?) -> Unit,
) {
    val prompt = BiometricPrompt(
        actividad,
        ContextCompat.getMainExecutor(actividad),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val autenticado = result.cryptoObject?.cipher
                if (autenticado != null) onExito(autenticado) else onFalla("No pudimos verificar la huella.")
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelado = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                onFalla(if (cancelado) null else errString.toString())
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(titulo)
        .setSubtitle(subtitulo)
        .setNegativeButtonText(textoAlternativo)
        .setAllowedAuthenticators(BIOMETRIC_STRONG)
        .setConfirmationRequired(false)
        .build()
    prompt.authenticate(info, BiometricPrompt.CryptoObject(cifrador))
}
