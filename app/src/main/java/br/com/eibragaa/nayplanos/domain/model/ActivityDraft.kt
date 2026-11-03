package br.com.eibragaa.nayplanos.domain.model

/** Rascunho textual de uma atividade e referência opcional à imagem de apoio. */
data class ActivityDraft(
    val text: String = "",
    /** Referência opaca gerida pela camada de dados (por exemplo, um URI serializado). */
    val imageReference: String? = null,
)
