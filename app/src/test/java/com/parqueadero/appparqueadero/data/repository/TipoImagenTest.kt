package com.parqueadero.appparqueadero.data.repository

import com.parqueadero.appparqueadero.support.FIRMA_JPEG
import com.parqueadero.appparqueadero.support.FIRMA_PNG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TipoImagenTest {

    @Test
    fun `detecta JPEG por firma`() {
        assertEquals(PagoRepository.MIME_JPEG, TipoImagen.detectar(FIRMA_JPEG))
        assertEquals(PagoRepository.MIME_JPEG, TipoImagen.detectar(FIRMA_JPEG.copyOf(3)))
    }

    @Test
    fun `detecta PNG por firma`() {
        assertEquals(PagoRepository.MIME_PNG, TipoImagen.detectar(FIRMA_PNG))
    }

    @Test
    fun `otros formatos o firmas truncadas no se aceptan`() {
        assertNull(TipoImagen.detectar("GIF89a..".toByteArray()))
        assertNull(TipoImagen.detectar("%PDF-1.7".toByteArray()))
        assertNull(TipoImagen.detectar(FIRMA_JPEG.copyOf(2)))
        assertNull(TipoImagen.detectar(FIRMA_PNG.copyOf(4)))
    }

    @Test
    fun `vacio no es imagen`() {
        assertNull(TipoImagen.detectar(ByteArray(0)))
    }
}
