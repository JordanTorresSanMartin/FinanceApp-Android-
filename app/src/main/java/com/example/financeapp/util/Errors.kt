package com.example.financeapp.util

import io.github.jan.supabase.exceptions.HttpRequestException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Traduce excepciones de red/Supabase a un mensaje en español para el usuario.
 * El detalle técnico no se muestra (puede incluir URLs internas o texto en inglés).
 */
fun friendlyError(e: Throwable, fallback: String = "Algo salió mal. Inténtalo de nuevo."): String {
    if (e is CancellationException) throw e
    val msg = e.message.orEmpty().lowercase()
    return when {
        e is HttpRequestException || e is IOException || e.cause is IOException ||
            "timeout" in msg || "unable to resolve host" in msg -> "Sin conexión. Revisa tu internet e inténtalo de nuevo."
        "invalid login credentials" in msg || "invalid_credentials" in msg -> "Correo o contraseña incorrectos."
        "email not confirmed" in msg -> "Confirma tu correo antes de iniciar sesión."
        "already registered" in msg || "user_already_exists" in msg -> "Ese correo ya tiene una cuenta. Inicia sesión."
        "rate limit" in msg || "over_email_send_rate_limit" in msg -> "Demasiados intentos. Espera unos minutos."
        "jwt" in msg || "not authenticated" in msg || "no autenticado" in msg -> "Tu sesión expiró. Inicia sesión de nuevo."
        else -> fallback
    }
}
