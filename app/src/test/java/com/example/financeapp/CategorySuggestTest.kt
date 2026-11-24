package com.example.financeapp

import com.example.financeapp.domain.normalizeDescription
import com.example.financeapp.domain.suggestCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CategorySuggestTest {
    private val history = mapOf(
        normalizeDescription("Entel") to "telefonia",
        normalizeDescription("Líder Express 1234") to "super",
        normalizeDescription("Copec Ruta 5") to "bencina",
    )

    @Test fun normalizes() = assertEquals("lider express", normalizeDescription("  Líder  Express 1234 "))

    @Test fun exactMatch() = assertEquals("telefonia", suggestCategory(history, "ENTEL"))

    @Test fun firstWordMatch() {
        assertEquals("super", suggestCategory(history, "Lider Vivaceta"))
        assertEquals("bencina", suggestCategory(history, "copec la florida"))
        assertEquals("telefonia", suggestCategory(history, "Entel plan noviembre"))
    }

    @Test fun noSuggestion() {
        assertNull(suggestCategory(history, "Ripley"))
        assertNull(suggestCategory(history, "a"))
    }
}
