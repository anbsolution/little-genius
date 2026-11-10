package com.learning.mylittleschool

import org.junit.Assert.assertEquals
import org.junit.Test

class SchoolClassTest {
    @Test
    fun containsNineLearningLevels() {
        assertEquals(9, listOf(
            "Pre-Nursery", "Nursery", "LKG", "UKG",
            "Class 1", "Class 2", "Class 3", "Class 4", "Class 5"
        ).size)
    }
}
