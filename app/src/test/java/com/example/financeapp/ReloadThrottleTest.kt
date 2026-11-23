package com.example.financeapp

import com.example.financeapp.util.ReloadThrottle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReloadThrottleTest {
    @Test fun reloadsFirstTimeThenOnlyWhenStaleOrDataChanged() {
        val t = ReloadThrottle(minMs = 30_000)
        assertTrue("primera vez carga", t.ready(now = 1_000, version = 0))
        assertFalse("volver a la pestaña 5 s después no recarga", t.ready(now = 6_000, version = 0))
        assertTrue("si la app cambió datos, recarga aunque sea pronto", t.ready(now = 7_000, version = 1))
        assertFalse(t.ready(now = 8_000, version = 1))
        assertTrue("pasados 30 s, recarga", t.ready(now = 38_000, version = 1))
    }
}
