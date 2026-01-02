package com.example.taptalk

import org.junit.Assert.*
import org.junit.Test

class SettingsSectionTest {

    @Test
    fun sectionRunsContentLambda() {
        var executed = false

        val content = {
            executed = true
        }

        content()

        assertTrue(executed)
    }
}
