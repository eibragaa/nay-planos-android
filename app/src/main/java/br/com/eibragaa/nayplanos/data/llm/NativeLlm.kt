package br.com.eibragaa.nayplanos.data.llm

/**
 * Fronteira JNI com o llama.cpp (libnayllm.so). Não contém regra de negócio.
 * Uma instância é um handle nativo: deve ser liberada com [close].
 */
class NativeLlm private constructor(private var handle: Long) : AutoCloseable {

    /** Descreve uma imagem RGB com o prompt dado. Chamadas são serializadas pelo chamador. */
    fun describe(rgb: ByteArray, width: Int, height: Int, prompt: String, maxTokens: Int): String {
        check(handle != 0L) { "Modelo já foi liberado." }
        return nativeDescribe(handle, rgb, width, height, prompt, maxTokens)
    }

    override fun close() {
        if (handle != 0L) {
            nativeFree(handle)
            handle = 0L
        }
    }

    companion object {
        private const val LIBRARY = "nayllm"

        /** Carrega a biblioteca nativa; lança UnsatisfiedLinkError se o ABI não for suportado. */
        fun load(modelPath: String, mmprojPath: String, threads: Int): NativeLlm {
            System.loadLibrary(LIBRARY)
            val handle = nativeLoad(modelPath, mmprojPath, threads)
            check(handle != 0L) { "Falha ao inicializar o modelo." }
            return NativeLlm(handle)
        }

        @JvmStatic
        private external fun nativeLoad(modelPath: String, mmprojPath: String, threads: Int): Long

        @JvmStatic
        private external fun nativeDescribe(
            handle: Long,
            rgb: ByteArray,
            width: Int,
            height: Int,
            prompt: String,
            maxTokens: Int,
        ): String

        @JvmStatic
        private external fun nativeFree(handle: Long)
    }
}
