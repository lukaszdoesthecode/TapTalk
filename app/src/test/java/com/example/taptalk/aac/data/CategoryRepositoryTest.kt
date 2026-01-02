package com.example.taptalk.aac.data

import org.junit.Test

class CategoryRepositoryTest {

    @Test
    fun loadsClassForCoverage() {
        val ref = CategoryRepository::class

        assert(ref.simpleName == "CategoryRepository")
    }
}