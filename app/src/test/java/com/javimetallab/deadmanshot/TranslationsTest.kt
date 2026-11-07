package com.javimetallab.deadmanshot

import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Every language has every text, with the same placeholders, and the plural
 * forms that language needs. Also checks the store listing lengths.
 */
class TranslationsTest {
    private val res = File("src/main/res")
    private val placeholder = Regex("%(\\d+\\$)?[sd]")

    private data class Texts(val strings: Map<String, String>, val plurals: Map<String, Map<String, String>>)

    private fun load(file: File): Texts {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        val nodes = doc.documentElement.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            if (e.getAttribute("translatable") == "false") continue
            when (e.tagName) {
                "string" -> strings[e.getAttribute("name")] = e.textContent
                "plurals" -> {
                    val items = e.getElementsByTagName("item")
                    plurals[e.getAttribute("name")] = (0 until items.length).associate {
                        val item = items.item(it) as Element
                        item.getAttribute("quantity") to item.textContent
                    }
                }
            }
        }
        return Texts(strings, plurals)
    }

    private fun holes(s: String) = placeholder.findAll(s).map { it.value }.toSortedSet()

    /** Plural categories Android uses for each language we ship. */
    private val needs = mapOf(
        "es" to setOf("one", "many", "other"), "pt" to setOf("one", "many", "other"),
        "fr" to setOf("one", "many", "other"), "it" to setOf("one", "many", "other"),
        "de" to setOf("one", "other"), "nl" to setOf("one", "other"), "hi" to setOf("one", "other"),
        "pl" to setOf("one", "few", "many", "other"), "ru" to setOf("one", "few", "many", "other"),
        "uk" to setOf("one", "few", "many", "other"),
        "zh-rCN" to setOf("other"), "ja" to setOf("other"), "ko" to setOf("other"), "th" to setOf("other"),
    )

    @Test fun everyLanguageIsComplete() {
        val base = load(File(res, "values/strings.xml"))
        val dirs = res.listFiles { f -> f.isDirectory && f.name.startsWith("values-") && File(f, "strings.xml").exists() }!!
        val problems = mutableListOf<String>()
        for (dir in dirs.sortedBy { it.name }) {
            val lang = dir.name.removePrefix("values-")
            val t = load(File(dir, "strings.xml"))
            (base.strings.keys - t.strings.keys).forEach { problems += "$lang: missing $it" }
            (t.strings.keys - base.strings.keys).forEach { problems += "$lang: unknown $it" }
            for ((k, v) in t.strings) {
                val en = base.strings[k] ?: continue
                if (holes(v) != holes(en)) problems += "$lang: $k has ${holes(v)}, English has ${holes(en)}"
                if (v.isBlank()) problems += "$lang: $k is empty"
            }
            (base.plurals.keys - t.plurals.keys).forEach { problems += "$lang: missing plurals $it" }
            for ((k, forms) in t.plurals) {
                val en = base.plurals[k]
                if (en == null) {
                    problems += "$lang: unknown plurals $k"
                    continue
                }
                val other = holes(en.getValue("other"))
                needs[lang]?.let { need ->
                    (need - forms.keys).forEach { problems += "$lang: plurals $k lacks '$it'" }
                }
                forms["other"]?.let { if (holes(it) != other) problems += "$lang: plurals $k/other has ${holes(it)}" }
                for ((q, text) in forms) {
                    if (!other.containsAll(holes(text))) problems += "$lang: plurals $k/$q has ${holes(text)}"
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun storeListingsFit() {
        val dir = File("../fastlane/metadata/android")
        val problems = mutableListOf<String>()
        dir.listFiles { f -> f.isDirectory }!!.forEach { loc ->
            fun len(name: String) = File(loc, name).takeIf { it.exists() }?.readText()?.trim()?.length
            val title = len("title.txt"); val short = len("short_description.txt"); val full = len("full_description.txt")
            if (title == null || title > 30) problems += "${loc.name}: title $title"
            if (short == null || short > 80) problems += "${loc.name}: short $short"
            if (full == null || full > 4000) problems += "${loc.name}: full $full"
            File(loc, "changelogs").listFiles()?.forEach {
                if (it.readText().trim().length > 500) problems += "${loc.name}: changelog ${it.name} too long"
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }
}
