package com.learning.mylittleschool

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchoolClassTest {
    @Test
    fun containsPreschoolAndClassesOneThroughTwelve() {
        assertEquals(16, schoolClasses.size)
        assertEquals(
            listOf("pre_nursery", "nursery", "lkg", "ukg") + (1..12).map { "class_$it" },
            schoolClasses.map { it.id }
        )
    }

    @Test
    fun higherClassesDoNotReceiveGeneratedPlaceholderQuizzes() {
        (6..12).forEach { grade ->
            assertTrue(lessonsFor("class_$grade", "Mathematics").isEmpty())
        }
    }

    @Test
    fun bundledLessonsRemainAvailableOffline() {
        assertFalse(lessonsFor("class_5", "Mathematics").isEmpty())
    }
}
