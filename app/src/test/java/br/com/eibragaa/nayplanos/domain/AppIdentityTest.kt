package br.com.eibragaa.nayplanos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdentityTest {
    @Test
    fun appNameIsStable() {
        assertEquals("Nay Planos", APP_NAME)
    }
}
