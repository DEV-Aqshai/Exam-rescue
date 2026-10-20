package com.example.core.common

import kotlin.math.max
import kotlin.math.roundToInt

enum class FlashcardRating(val value: Int) {
    AGAIN(0),
    HARD(1),
    GOOD(2),
    EASY(3)
}

data class SpacedRepetitionResult(
    val intervalDays: Int,
    val easeFactor: Float,
    val repetitions: Int,
    val nextDueAtMillis: Long
)

object SpacedRepetitionCalculator {
    private const val ONE_DAY_MILLIS = 86_400_000L
    private const val MIN_EASE_FACTOR = 1.3f

    fun calculateNextReview(
        rating: FlashcardRating,
        currentIntervalDays: Int,
        currentEaseFactor: Float,
        currentRepetitions: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): SpacedRepetitionResult {
        return when (rating) {
            FlashcardRating.AGAIN -> {
                // Reset on failure
                val newEase = max(MIN_EASE_FACTOR, currentEaseFactor - 0.2f)
                SpacedRepetitionResult(
                    intervalDays = 1,
                    easeFactor = newEase,
                    repetitions = 0,
                    nextDueAtMillis = currentTimeMillis + ONE_DAY_MILLIS
                )
            }
            FlashcardRating.HARD -> {
                val newInterval = max(1, (currentIntervalDays * 1.2f).roundToInt())
                val newEase = max(MIN_EASE_FACTOR, currentEaseFactor - 0.15f)
                SpacedRepetitionResult(
                    intervalDays = newInterval,
                    easeFactor = newEase,
                    repetitions = currentRepetitions + 1,
                    nextDueAtMillis = currentTimeMillis + (newInterval * ONE_DAY_MILLIS)
                )
            }
            FlashcardRating.GOOD -> {
                val newInterval = when (currentRepetitions) {
                    0 -> 1
                    1 -> 3
                    else -> max(1, (currentIntervalDays * currentEaseFactor).roundToInt())
                }
                SpacedRepetitionResult(
                    intervalDays = newInterval,
                    easeFactor = currentEaseFactor,
                    repetitions = currentRepetitions + 1,
                    nextDueAtMillis = currentTimeMillis + (newInterval * ONE_DAY_MILLIS)
                )
            }
            FlashcardRating.EASY -> {
                val newEase = currentEaseFactor + 0.15f
                val newInterval = when (currentRepetitions) {
                    0 -> 2
                    1 -> 5
                    else -> max(2, (currentIntervalDays * newEase * 1.3f).roundToInt())
                }
                SpacedRepetitionResult(
                    intervalDays = newInterval,
                    easeFactor = newEase,
                    repetitions = currentRepetitions + 1,
                    nextDueAtMillis = currentTimeMillis + (newInterval * ONE_DAY_MILLIS)
                )
            }
        }
    }
}
