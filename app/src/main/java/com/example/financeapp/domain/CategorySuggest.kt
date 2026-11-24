package com.example.financeapp.domain

import java.text.Normalizer

/** "Lider Express 123" → "lider express": minúsculas, sin tildes ni números, espacios simples. */
fun normalizeDescription(s: String): String =
    Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "")
        .replace(Regex("[^a-zñ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Categoría sugerida para [description] según movimientos anteriores ([history]: descripción
 * normalizada → categoría, la más reciente). Primero coincidencia exacta; si no, la primera palabra
 * significativa (≥ 4 letras): "entel plan" sugiere lo mismo que "entel".
 */
fun suggestCategory(history: Map<String, String>, description: String): String? {
    val d = normalizeDescription(description)
    if (d.length < 3) return null
    history[d]?.let { return it }
    val first = d.split(' ').firstOrNull { it.length >= 4 } ?: return null
    return history.entries.firstOrNull { (k, _) -> k.split(' ').any { it == first } }?.value
}
