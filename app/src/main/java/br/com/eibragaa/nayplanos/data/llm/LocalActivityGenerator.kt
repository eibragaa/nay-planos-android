package br.com.eibragaa.nayplanos.data.llm

import android.graphics.BitmapFactory
import br.com.eibragaa.nayplanos.data.model.ModelInstallStore
import br.com.eibragaa.nayplanos.domain.device.LocalModel
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Gera texto de atividade a partir de uma imagem, usando o modelo local escolhido pela usuária.
 * O texto é só rascunho: quem decide o que vai para o plano é a revisão humana.
 */
class LocalActivityGenerator(
    private val store: ModelInstallStore,
    private val threads: Int,
) {
    private val lock = Mutex()
    private var loaded: Pair<String, NativeLlm>? = null

    /** Gera a descrição da imagem JPEG com o modelo `model`, carregando-o sob demanda. */
    suspend fun describe(model: LocalModel, jpeg: ByteArray): String = lock.withLock {
        withContext(Dispatchers.IO) {
            val (width, height, rgb) = decodeToRgb(jpeg)
            val llm = loadedFor(model)
            llm.describe(rgb, width, height, PROMPT, MAX_TOKENS).trim()
        }
    }

    /** Libera a memória nativa do modelo carregado. */
    suspend fun release() = lock.withLock {
        loaded?.second?.close()
        loaded = null
    }

    private fun loadedFor(model: LocalModel): NativeLlm {
        loaded?.takeIf { it.first == model.id }?.let { return it.second }
        loaded?.second?.close()
        loaded = null
        val llm = NativeLlm.load(
            modelPath = store.modelPath(model).absolutePath,
            mmprojPath = store.mmprojPath(model).absolutePath,
            threads = threads,
        )
        loaded = model.id to llm
        return llm
    }

    /** Decodifica o JPEG e reduz para no máximo [MAX_SIDE] px, em RGB linha a linha. */
    private fun decodeToRgb(jpeg: ByteArray): Triple<Int, Int, ByteArray> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Imagem ilegível." }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE * 2) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(
            jpeg, 0, jpeg.size, BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: throw IllegalArgumentException("Imagem ilegível.")
        val scale = minOf(1f, MAX_SIDE.toFloat() / maxOf(bitmap.width, bitmap.height))
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (width == bitmap.width && height == bitmap.height) {
            bitmap
        } else {
            android.graphics.Bitmap.createScaledBitmap(bitmap, width, height, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        }
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        scaled.recycle()
        val rgb = ByteArray(width * height * 3)
        var o = 0
        for (p in pixels) {
            rgb[o++] = (p shr 16).toByte()
            rgb[o++] = (p shr 8).toByte()
            rgb[o++] = p.toByte()
        }
        return Triple(width, height, rgb)
    }

    private companion object {
        const val MAX_SIDE = 448
        const val MAX_TOKENS = 220
        const val PROMPT =
            "Esta imagem é um registro de atividade de uma turma de Pré-Maternal (educação infantil). " +
                "Descreva em português do Brasil uma atividade pedagógica simples inspirada na imagem, " +
                "em duas ou três frases, sem listas e sem citar nomes de crianças."
    }
}
