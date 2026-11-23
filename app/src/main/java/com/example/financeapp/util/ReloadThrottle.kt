package com.example.financeapp.util

import android.os.SystemClock

/** Se incrementa cada vez que la app modifica datos (lo llama el repositorio al escribir). */
object DataVersion {
    @Volatile var value = 0L
        private set

    fun bump() { value++ }
}

/**
 * Evita recargar una pantalla cada vez que vuelve a mostrarse (cambiar de pestaña): solo si pasó
 * [minMs] o si la app cambió datos desde la última carga. Menos red, CPU y batería, y la animación
 * de entrada no compite con una recarga.
 */
class ReloadThrottle(private val minMs: Long = 30_000) {
    private var lastAt = 0L
    private var lastVersion = -1L

    fun ready(now: Long = SystemClock.elapsedRealtime(), version: Long = DataVersion.value): Boolean {
        if (lastAt != 0L && version == lastVersion && now - lastAt < minMs) return false
        lastAt = now
        lastVersion = version
        return true
    }
}
