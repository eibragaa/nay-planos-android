package br.com.eibragaa.nayplanos.domain.generation

import org.junit.Assert.assertEquals
import org.junit.Test

class ImagePayloadPolicyTest {
    @Test
    fun aceitaJpegComAssinaturaValida() {
        val result = ImagePayloadPolicy.check(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))

        assertEquals(ImagePayloadPolicy.Result.Accepted, result)
    }

    @Test
    fun rejeitaBytesVazios() {
        assertEquals(
            ImagePayloadPolicy.Result.Rejected(ImagePayloadPolicy.Reason.EMPTY),
            ImagePayloadPolicy.check(ByteArray(0)),
        )
    }

    @Test
    fun rejeitaPngMesmoComTamanhoValido() {
        val pngHeader = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

        assertEquals(
            ImagePayloadPolicy.Result.Rejected(ImagePayloadPolicy.Reason.NOT_JPEG),
            ImagePayloadPolicy.check(pngHeader),
        )
    }

    @Test
    fun rejeitaPayloadAcimaDoLimite() {
        val oversized = ByteArray(ImagePayloadPolicy.MAX_PAYLOAD_BYTES + 1)
        oversized[0] = 0xFF.toByte(); oversized[1] = 0xD8.toByte(); oversized[2] = 0xFF.toByte()

        assertEquals(
            ImagePayloadPolicy.Result.Rejected(ImagePayloadPolicy.Reason.TOO_LARGE),
            ImagePayloadPolicy.check(oversized),
        )
    }
}
