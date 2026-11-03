package br.com.eibragaa.nayplanos.data.document

import br.com.eibragaa.nayplanos.domain.document.DocumentImage
import br.com.eibragaa.nayplanos.domain.model.ActivityDraft
import br.com.eibragaa.nayplanos.domain.model.PlanDay
import br.com.eibragaa.nayplanos.domain.model.WeeklyPlan
import java.io.ByteArrayInputStream
import java.io.StringReader
import java.time.LocalDate
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.xml.sax.InputSource

class MinimalDocxGeneratorTest {
    private val generator = MinimalDocxGenerator()
    private val start = LocalDate.of(2026, 11, 9)

    @Test
    fun `gera pacote OOXML com partes obrigatorias e cinco dias`() {
        val plan = plan()
        val entries = unzip(generator.generate(plan))
        val required = setOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "word/document.xml",
            "word/_rels/document.xml.rels",
            "word/styles.xml",
        )

        assertTrue(entries.keys.containsAll(required))
        assertRequiredXmlParses(entries)
        val document = entries.text("word/document.xml")
        assertTrue(document.contains("Plano semanal"))
        assertTrue(document.contains("Período: 09/11/2026 a 13/11/2026"))
        assertEquals(6, Regex("<w:tr>").findAll(document).count())
        plan.days.forEach { day ->
            assertTrue(document.contains(day.date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
            assertTrue(document.contains(day.activity.text))
        }
        assertEquals(3, Regex("Imagem: referência sem arquivo").findAll(document).count())
        assertEquals(2, Regex("Imagem: não vinculada").findAll(document).count())
        assertFalse(entries.keys.any { it.startsWith("word/media/") })
    }

    @Test
    fun `incorpora JPEG uma vez e vincula drawing a celula do dia`() {
        val plan = plan(sharedReference = true)
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x11, 0x22, 0xFF.toByte(), 0xD9.toByte())
        val entries = unzip(generator.generate(
            plan,
            mapOf("image://shared" to DocumentImage(jpeg, "Foto & atividade", width = 1600, height = 900)),
        ))
        val media = entries.filterKeys { it.startsWith("word/media/") }

        assertEquals(setOf("word/media/image1.jpg"), media.keys)
        assertArrayEquals(jpeg, media.getValue("word/media/image1.jpg"))
        val types = entries.text("[Content_Types].xml")
        assertTrue(types.contains("""<Default Extension="jpg" ContentType="image/jpeg"/>"""))

        val relationships = entries.text("word/_rels/document.xml.rels")
        assertTrue(relationships.contains("""Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image1.jpg"""))
        val document = entries.text("word/document.xml")
        assertEquals(3, Regex("<w:drawing>").findAll(document).count())
        assertEquals(3, Regex("<pic:pic>").findAll(document).count())
        assertTrue(document.contains("""<a:blip r:embed="rId2"/>"""))
        assertTrue(document.contains("""descr="Foto &amp; atividade""""))
        assertEquals(3, Regex("<wp:extent cx=\"1828800\" cy=\"1028700\"/>").findAll(document).count())
        assertEquals(3, Regex("<a:ext cx=\"1828800\" cy=\"1028700\"/>").findAll(document).count())
        assertTrue(document.contains("Imagem: incorporada"))
        assertRequiredXmlParses(entries)
    }

    @Test
    fun `drawing fica dentro da celula da atividade correspondente`() {
        val entries = unzip(generator.generate(
            plan(sharedReference = true),
            mapOf("image://shared" to DocumentImage(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))),
        ))
        val parsed = parseXml(entries.text("word/document.xml"))
        val namespace = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
        val cells = parsed.getElementsByTagNameNS(namespace, "tc")
        val imageCell = (0 until cells.length)
            .map { cells.item(it) as org.w3c.dom.Element }
            .first { it.textContent.contains("Atividade 1") }
        assertEquals(1, imageCell.getElementsByTagNameNS(namespace, "drawing").length)
    }

    @Test
    fun `referencia sem bytes preserva status textual e nao gera drawing`() {
        val entries = unzip(generator.generate(plan(), mapOf("image://day-0" to DocumentImage(byteArrayOf()))))
        val document = entries.text("word/document.xml")

        assertTrue(document.contains("Imagem: referência sem arquivo"))
        assertFalse(document.contains("<w:drawing>"))
        assertFalse(entries.keys.any { it.startsWith("word/media/") })
        assertFalse(entries.text("[Content_Types].xml").contains("""Extension="jpg""""))
    }

    @Test
    fun `limita imagem retrato sem alterar proporcao e mantem extensoes iguais`() {
        val document = unzip(generator.generate(
            plan(sharedReference = true),
            mapOf("image://shared" to DocumentImage(byteArrayOf(1), width = 900, height = 1600)),
        )).text("word/document.xml")

        assertEquals(3, Regex("<wp:extent cx=\"771525\" cy=\"1371600\"/>").findAll(document).count())
        assertEquals(3, Regex("<a:ext cx=\"771525\" cy=\"1371600\"/>").findAll(document).count())
        assertTrue(document.contains("Imagem: incorporada"))
    }

    @Test
    fun `escapa caracteres XML no conteudo`() {
        val entries = unzip(generator.generate(plan(firstActivity = "Arte & <formas> \"vivas\" e 'cores'")))
        val document = entries.text("word/document.xml")

        assertTrue(document.contains("Arte &amp; &lt;formas&gt; &quot;vivas&quot; e &apos;cores&apos;"))
        assertFalse(document.contains("Arte & <formas>"))
        assertRequiredXmlParses(entries)
    }

    @Test
    fun `remove controles proibidos e unpaired e preserva whitespace e emoji`() {
        val input = "A\u0001B\u0009C\u000AD\u000DE\uD83D\uDE00F\uD800G\uDC00H"
        val entries = unzip(generator.generate(plan(firstActivity = input)))
        val document = entries.text("word/document.xml")

        assertRequiredXmlParses(entries)
        assertTrue(document.contains("AB\tC\nD\rE😀FGH"))
        assertFalse(document.contains('\u0001'))
        assertFalse(document.contains('\uD800'))
        assertFalse(document.contains('\uDC00'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejeita plano invalido`() {
        generator.generate(WeeklyPlan(start, emptyList()))
    }

    private fun plan(firstActivity: String = "Atividade 1", sharedReference: Boolean = false) = WeeklyPlan(
        weekStart = start,
        days = (0..4).map { offset ->
            PlanDay(
                date = start.plusDays(offset.toLong()),
                activity = ActivityDraft(
                    text = if (offset == 0) firstActivity else "Atividade ${offset + 1}",
                    imageReference = when {
                        sharedReference -> if (offset in 0..2) "image://shared" else null
                        offset % 2 == 0 -> "image://day-$offset"
                        else -> null
                    },
                ),
            )
        },
    )

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> = buildMap {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                put(entry.name, zip.readBytes())
                zip.closeEntry()
            }
        }
    }

    private fun Map<String, ByteArray>.text(name: String): String =
        getValue(name).toString(Charsets.UTF_8)

    private fun assertRequiredXmlParses(entries: Map<String, ByteArray>) {
        val requiredXml = listOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "word/document.xml",
            "word/_rels/document.xml.rels",
            "word/styles.xml",
        )
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        requiredXml.forEach { name ->
            val xml = entries.text(name)
            parseXml(xml)
        }
    }

    private fun parseXml(xml: String): org.w3c.dom.Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return try {
            factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
        } catch (error: Exception) {
            System.err.println("INVALID_XML=$xml")
            throw error
        }
    }
}
