package br.com.eibragaa.nayplanos.domain.document

import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan

/** Generates a downloadable Word document for a validated weekly plan. */
interface PlanDocumentGenerator {
    /** Returns the complete DOCX file as a ZIP byte array. */
    fun generate(
        plan: WeeklyPlan,
        imagesByReference: Map<String, DocumentImage> = emptyMap(),
    ): ByteArray
}
