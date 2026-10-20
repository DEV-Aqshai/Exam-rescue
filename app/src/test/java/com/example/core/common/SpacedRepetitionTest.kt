package com.example.core.common

import org.junit.Assert.*
import org.junit.Test

class SpacedRepetitionTest {

    @Test
    fun `rating again resets repetitions and interval to 1 day`() {
        val result = SpacedRepetitionCalculator.calculateNextReview(
            rating = FlashcardRating.AGAIN,
            currentIntervalDays = 10,
            currentEaseFactor = 2.5f,
            currentRepetitions = 3
        )

        assertEquals(1, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertTrue(result.easeFactor <= 2.5f)
        assertTrue(result.easeFactor >= 1.3f)
    }

    @Test
    fun `consecutive successful ratings increase interval`() {
        val firstReview = SpacedRepetitionCalculator.calculateNextReview(
            rating = FlashcardRating.GOOD,
            currentIntervalDays = 1,
            currentEaseFactor = 2.5f,
            currentRepetitions = 0
        )
        assertEquals(1, firstReview.intervalDays)
        assertEquals(1, firstReview.repetitions)

        val secondReview = SpacedRepetitionCalculator.calculateNextReview(
            rating = FlashcardRating.GOOD,
            currentIntervalDays = firstReview.intervalDays,
            currentEaseFactor = firstReview.easeFactor,
            currentRepetitions = firstReview.repetitions
        )
        assertEquals(3, secondReview.intervalDays)
        assertEquals(2, secondReview.repetitions)

        val thirdReview = SpacedRepetitionCalculator.calculateNextReview(
            rating = FlashcardRating.GOOD,
            currentIntervalDays = secondReview.intervalDays,
            currentEaseFactor = secondReview.easeFactor,
            currentRepetitions = secondReview.repetitions
        )
        assertTrue("Interval must expand beyond 3 days", thirdReview.intervalDays > 3)
    }

    @Test
    fun `easy rating expands interval and increases ease factor`() {
        val result = SpacedRepetitionCalculator.calculateNextReview(
            rating = FlashcardRating.EASY,
            currentIntervalDays = 3,
            currentEaseFactor = 2.5f,
            currentRepetitions = 2
        )

        assertTrue(result.intervalDays > 3)
        assertTrue(result.easeFactor > 2.5f)
    }
}
