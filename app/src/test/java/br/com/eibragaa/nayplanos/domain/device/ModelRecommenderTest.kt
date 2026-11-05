package br.com.eibragaa.nayplanos.domain.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRecommenderTest {
    private val catalog = LocalModelCatalog.models
    private val model2b = catalog.first { it.id == "qwen2-vl-2b" }
    private val model3b = catalog.first { it.id == "qwen2.5-vl-3b" }

    @Test
    fun aparelho16GbRecebeOpcoesDoMaiorParaOMenor() {
        // Orçamento de 8 GB: 7B, 3B e 2B cabem; a lista vem ordenada com o maior primeiro.
        val result = ModelRecommender.recommend(device(totalRamMb = 16_384, freeStorageMb = 30_000), catalog)

        val options = (result as ModelRecommendation.Compatible).options
        assertEquals(listOf("qwen2.5-vl-7b", "qwen2.5-vl-3b", "qwen2-vl-2b"), options.map { it.id })
    }

    @Test
    fun aparelhoMedidoS25UltraNaoRecebe7BComOrcamentoDeCincoGb() {
        // 11 GB de RAM -> orçamento de ~5,5 GB: 7B (6656 MB) fica de fora.
        val result = ModelRecommender.recommend(device(totalRamMb = 11_379, freeStorageMb = 200_000), catalog)

        val options = (result as ModelRecommendation.Compatible).options
        assertEquals(listOf("qwen2.5-vl-3b", "qwen2-vl-2b"), options.map { it.id })
    }

    @Test
    fun abiNaoSuportadaNaoRecebeModelo() {
        val result = ModelRecommender.recommend(device(cpuAbi = "armeabi-v7a"), catalog)

        assertEquals(ModelRecommendation.NoneFits(NoFitReason.UNSUPPORTED_ABI), result)
    }

    @Test
    fun ramInsuficienteInformaMotivo() {
        val result = ModelRecommender.recommend(device(totalRamMb = 3_000), catalog)

        assertEquals(ModelRecommendation.NoneFits(NoFitReason.INSUFFICIENT_RAM), result)
    }

    @Test
    fun armazenamentoInsuficienteInformaMotivo() {
        // RAM sobra para o 2B, mas o espaço livre não cobre os arquivos mais a folga.
        val result = ModelRecommender.recommend(
            device(totalRamMb = 11_379, freeStorageMb = 100),
            catalog,
        )

        assertEquals(ModelRecommendation.NoneFits(NoFitReason.INSUFFICIENT_STORAGE), result)
    }

    @Test
    fun margemDeArmazenamentoEhExataParaOModeloDeMenorTamanho() {
        val required = ModelRecommender.requiredStorageMb(model2b)
        val justEnough = ModelRecommender.recommend(
            device(totalRamMb = 8_000, freeStorageMb = required),
            catalog,
        )
        val justBelow = ModelRecommender.recommend(
            device(totalRamMb = 8_000, freeStorageMb = required - 1),
            catalog,
        )

        assertTrue((justEnough as ModelRecommendation.Compatible).options.any { it.id == model2b.id })
        assertEquals(ModelRecommendation.NoneFits(NoFitReason.INSUFFICIENT_STORAGE), justBelow)
    }

    @Test
    fun espacoExatoParaOTresBEntraNasOpcoes() {
        // Espaço igual ao necessário pelo 3B: ele deve aparecer entre as opções (a RAM de 11 GB comporta).
        val result = ModelRecommender.recommend(
            device(totalRamMb = 11_379, freeStorageMb = ModelRecommender.requiredStorageMb(model3b)),
            catalog,
        )

        assertTrue((result as ModelRecommendation.Compatible).options.any { it.id == model3b.id })
    }

    private fun device(
        totalRamMb: Long = 11_379,
        availableRamMb: Long = 8_000,
        cpuAbi: String = "arm64-v8a",
        freeStorageMb: Long = 100_000,
    ) = DeviceProfile(
        totalRamMb = totalRamMb,
        availableRamMb = availableRamMb,
        cpuAbi = cpuAbi,
        cpuCores = 8,
        freeStorageMb = freeStorageMb,
    )
}
