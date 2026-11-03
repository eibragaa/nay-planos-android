package br.com.eibragaa.nayplanos.data.document

import br.com.eibragaa.nayplanos.domain.document.DocumentImage
import br.com.eibragaa.nayplanos.domain.document.PlanDocumentGenerator
import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import br.com.eibragaa.nayplanos.domain.validation.WeeklyPlanValidator
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Creates a minimal OOXML Word document using only the JDK ZIP and text APIs. */
class MinimalDocxGenerator : PlanDocumentGenerator {
    override fun generate(
        plan: WeeklyPlan,
        imagesByReference: Map<String, DocumentImage>,
    ): ByteArray {
        val validationErrors = WeeklyPlanValidator.validate(plan)
        require(validationErrors.isEmpty()) {
            "Não é possível gerar o documento: plano inválido ($validationErrors)."
        }

        val imageParts = linkedMapOf<String, ImagePart>()
        plan.days.forEach { day ->
            val reference = day.activity.imageReference?.takeIf(String::isNotBlank) ?: return@forEach
            val image = imagesByReference[reference]?.takeIf { it.jpegBytes.isNotEmpty() } ?: return@forEach
            if (reference !in imageParts) {
                val index = imageParts.size + 1
                imageParts[reference] = ImagePart("image$index.jpg", "rId${index + 1}", image)
            }
        }

        val entries = linkedMapOf<String, ByteArray>(
            "[Content_Types].xml" to contentTypes(imageParts.isNotEmpty()).toByteArray(UTF_8),
            "_rels/.rels" to packageRelationships.toByteArray(UTF_8),
            "word/document.xml" to documentXml(plan, imageParts).toByteArray(UTF_8),
            "word/_rels/document.xml.rels" to documentRelationships(imageParts).toByteArray(UTF_8),
            "word/styles.xml" to styles.toByteArray(UTF_8),
        )
        imageParts.values.forEach { part -> entries["word/media/${part.fileName}"] = part.image.jpegBytes }

        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun documentXml(plan: WeeklyPlan, imageParts: Map<String, ImagePart>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing" xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture"><w:body>""")
        append(paragraph("Plano semanal", style = "Title"))
        append(paragraph("Período: ${formatDate(plan.weekStart)} a ${formatDate(plan.weekStart.plusDays(4))}"))
        append("<w:tbl><w:tblPr><w:tblStyle w:val=\"TableGrid\"/></w:tblPr>")
        append("<w:tblGrid><w:gridCol w:w=\"2200\"/><w:gridCol w:w=\"7160\"/></w:tblGrid>")
        append(tableRow("Dia", "Atividade"))
        plan.days.forEachIndexed { dayIndex, day ->
            val weekday = day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("pt", "BR"))
                .replaceFirstChar { it.titlecase(Locale("pt", "BR")) }
            val activity = if (day.isNoClass) "Sem aula" else day.activity.text
            val reference = day.activity.imageReference?.takeIf(String::isNotBlank)
            val image = reference?.let(imageParts::get)
            val status = when {
                reference == null -> "Imagem: não vinculada"
                image == null -> "Imagem: referência sem arquivo"
                else -> "Imagem: incorporada"
            }
            append(tableRow("$weekday, ${formatDate(day.date)}", "$activity\n$status", image, dayIndex + 1))
        }
        append("</w:tbl>")
        append("<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr>")
        append("</w:body></w:document>")
    }

    private fun tableRow(day: String, activity: String, image: ImagePart? = null, drawingId: Int = 0): String =
        "<w:tr>${cell(day)}${cell(activity, image, drawingId)}</w:tr>"

    private fun cell(value: String, image: ImagePart? = null, drawingId: Int = 0): String =
        "<w:tc><w:tcPr><w:tcW w:w=\"0\" w:type=\"auto\"/></w:tcPr>${paragraph(value)}${image?.let { "<w:p>${drawing(it, drawingId)}</w:p>" }.orEmpty()}</w:tc>"

    private fun drawing(image: ImagePart, id: Int): String {
        val description = escapeXml(image.image.description.orEmpty())
        val (widthEmu, heightEmu) = image.image.dimensionsEmu()
        return """<w:r><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0"><wp:extent cx="$widthEmu" cy="$heightEmu"/><wp:docPr id="$id" name="${image.fileName}" descr="$description"/><wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr><a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture"><pic:pic><pic:nvPicPr><pic:cNvPr id="$id" name="${image.fileName}" descr="$description"/><pic:cNvPicPr><a:picLocks noChangeAspect="1"/></pic:cNvPicPr></pic:nvPicPr><pic:blipFill><a:blip r:embed="${image.relationshipId}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="$widthEmu" cy="$heightEmu"/></a:xfrm><a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic></wp:inline></w:drawing></w:r>"""
    }

    private fun DocumentImage.dimensionsEmu(): Pair<Long, Long> {
        val safeWidth = width.coerceAtLeast(1).toDouble()
        val safeHeight = height.coerceAtLeast(1).toDouble()
        val scale = minOf(MAX_IMAGE_WIDTH_EMU / safeWidth, MAX_IMAGE_HEIGHT_EMU / safeHeight)
        return (safeWidth * scale).toLong().coerceAtLeast(1) to
            (safeHeight * scale).toLong().coerceAtLeast(1)
    }

    private fun paragraph(value: String, style: String? = null): String {
        val styleXml = style?.let { "<w:pPr><w:pStyle w:val=\"$it\"/></w:pPr>" }.orEmpty()
        return "<w:p>$styleXml<w:r><w:t xml:space=\"preserve\">${escapeXml(value)}</w:t></w:r></w:p>"
    }

    private fun documentRelationships(images: Map<String, ImagePart>): String = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
        images.values.forEach { part ->
            append("<Relationship Id=\"${part.relationshipId}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/image\" Target=\"media/${part.fileName}\"/>")
        }
        append("</Relationships>")
    }

    private fun contentTypes(hasImages: Boolean): String =
        if (hasImages) contentTypesWithJpeg else contentTypesWithoutJpeg

    private fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("pt", "BR")))

    private fun escapeXml(value: String): String = buildString(value.length) {
        var index = 0
        while (index < value.length) {
            val codePoint = value.codePointAt(index)
            index += Character.charCount(codePoint)
            when {
                !isXml10Character(codePoint) -> Unit
                codePoint == '&'.code -> append("&amp;")
                codePoint == '<'.code -> append("&lt;")
                codePoint == '>'.code -> append("&gt;")
                codePoint == '"'.code -> append("&quot;")
                codePoint == '\''.code -> append("&apos;")
                else -> appendCodePoint(codePoint)
            }
        }
    }

    private fun isXml10Character(codePoint: Int): Boolean =
        codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD ||
            codePoint in 0x20..0xD7FF || codePoint in 0xE000..0xFFFD ||
            codePoint in 0x10000..0x10FFFF

    private data class ImagePart(val fileName: String, val relationshipId: String, val image: DocumentImage)

    private companion object {
        val UTF_8 = StandardCharsets.UTF_8
        const val MAX_IMAGE_WIDTH_EMU = 1828800.0 // 2 inches
        const val MAX_IMAGE_HEIGHT_EMU = 1371600.0 // 1.5 inches
        const val contentTypesWithoutJpeg = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/></Types>"""
        const val contentTypesWithJpeg = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Default Extension="jpg" ContentType="image/jpeg"/><Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/><Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/></Types>"""
        const val packageRelationships = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/></Relationships>"""
        const val styles = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/><w:rPr><w:sz w:val="22"/></w:rPr></w:style><w:style w:type="paragraph" w:styleId="Title"><w:name w:val="Title"/><w:basedOn w:val="Normal"/><w:pPr><w:jc w:val="center"/></w:pPr><w:rPr><w:b/><w:sz w:val="32"/></w:rPr></w:style><w:style w:type="table" w:styleId="TableGrid"><w:name w:val="Table Grid"/><w:tblPr><w:tblBorders><w:top w:val="single" w:sz="4"/><w:left w:val="single" w:sz="4"/><w:bottom w:val="single" w:sz="4"/><w:right w:val="single" w:sz="4"/><w:insideH w:val="single" w:sz="4"/><w:insideV w:val="single" w:sz="4"/></w:tblBorders></w:tblPr></w:style></w:styles>"""
    }
}
