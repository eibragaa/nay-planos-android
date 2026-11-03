package br.com.eibragaa.nayplanos.domain.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageReferencePolicyTest {
    @Test
    fun rejeitaReferenciaVaziaOuSoComEspacos() {
        assertEquals(
            ImageReferenceResult.Error(ImageReferenceError.EMPTY),
            ImageReferencePolicy.normalize("   \t\n"),
        )
    }

    @Test
    fun removeEspacosExternos() {
        assertEquals(
            ImageReferenceResult.Success("content://imagens/1"),
            ImageReferencePolicy.normalize("  content://imagens/1  "),
        )
    }

    @Test
    fun rejeitaReferenciaAcimaDoLimite() {
        val oversized = "x".repeat(ImageReferencePolicy.MAX_REFERENCE_LENGTH + 1)

        assertEquals(
            ImageReferenceResult.Error(ImageReferenceError.TOO_LONG),
            ImageReferencePolicy.normalize(oversized),
        )
    }

    @Test
    fun aceitaReferenciaNoLimite() {
        val reference = "x".repeat(ImageReferencePolicy.MAX_REFERENCE_LENGTH)

        assertEquals(
            ImageReferenceResult.Success(reference),
            ImageReferencePolicy.normalize(reference),
        )
    }

    @Test
    fun preservaReferenciaValida() {
        assertEquals(
            ImageReferenceResult.Success("content://media/external/images/42"),
            ImageReferencePolicy.normalize("content://media/external/images/42"),
        )
    }
}
