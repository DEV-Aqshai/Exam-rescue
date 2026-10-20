package com.example.domain.planner

import com.example.core.database.entities.ExamEntity
import com.example.core.database.entities.StudyTaskEntity
import com.example.core.database.entities.SubjectEntity
import com.example.core.database.entities.TopicEntity
import com.example.core.database.entities.TopicProgressEntity
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

data class TopicWithContext(
    val topic: TopicEntity,
    val subject: SubjectEntity,
    val progress: TopicProgressEntity?,
    val relatedExam: ExamEntity?
)

data class ProposedTask(
    val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val subjectName: String,
    val topicId: String,
    val topicTitle: String,
    val plannedMinutes: Int,
    val priorityScore: Float,
    val priorityLevel: Int, // 1=Low, 2=Medium, 3=High
    val primaryReason: String,
    val taskType: String
)

object DeterministicPlanner {

    private const val ONE_DAY_MILLIS = 86_400_000L

    /**
     * Calculates transparent priority score:
     * - 35% Exam proximity (closer exam = higher score)
     * - 30% Low confidence (1 star = 1.0, 5 stars = 0.0)
     * - 20% Overdue review (past nextReviewAt = 1.0)
     * - 10% Never studied (status NOT_STARTED or progress null = 1.0)
     * - 5% User importance / subject order
     */
    fun calculateTopicScore(
        topicContext: TopicWithContext,
        now: Long = System.currentTimeMillis()
    ): Pair<Float, String> {
        var examScore = 0f
        var examReason = ""
        val exam = topicContext.relatedExam
        if (exam != null && exam.examAt > now) {
            val daysUntilExam = (exam.examAt - now) / ONE_DAY_MILLIS
            examScore = when {
                daysUntilExam <= 7 -> 1.0f
                daysUntilExam <= 14 -> 0.85f
                daysUntilExam <= 30 -> 0.65f
                daysUntilExam <= 60 -> 0.40f
                else -> 0.20f
            }
            if (daysUntilExam <= 30) {
                examReason = "Exam approaching in $daysUntilExam days"
            }
        }

        val progress = topicContext.progress
        val confidenceScore = if (progress != null) {
            // Confidence: 1 -> 1.0, 2 -> 0.75, 3 -> 0.5, 4 -> 0.25, 5 -> 0.0
            (5 - progress.confidence) / 4f
        } else {
            1.0f // Unrated
        }

        var overdueScore = 0f
        if (progress?.nextReviewAt != null && progress.nextReviewAt < now) {
            val overdueDays = (now - progress.nextReviewAt) / ONE_DAY_MILLIS
            overdueScore = min(1.0f, 0.5f + (overdueDays * 0.1f))
        }

        val neverStudiedScore = if (progress == null || progress.status == "NOT_STARTED") 1.0f else 0.0f
        val subjectOrderScore = max(0f, 1.0f - (topicContext.subject.sortOrder * 0.1f))

        val totalScore = (0.35f * examScore) +
                (0.30f * confidenceScore) +
                (0.20f * overdueScore) +
                (0.10f * neverStudiedScore) +
                (0.05f * subjectOrderScore)

        // Select the primary human-readable explanation
        val primaryReason = when {
            examReason.isNotEmpty() && examScore >= 0.65f -> examReason
            confidenceScore >= 0.75f -> "Low confidence (${progress?.confidence ?: 1}/5)"
            overdueScore > 0f -> "Review is overdue"
            neverStudiedScore > 0f -> "Not studied yet"
            else -> "Scheduled revision cycle"
        }

        return Pair(totalScore, primaryReason)
    }

    /**
     * Generates a balanced day plan that fits strictly within dailyAvailableMinutes.
     * Interleaves subjects so the student does not get burned out on a single subject.
     */
    fun generateDailyPlan(
        topics: List<TopicWithContext>,
        existingCompletedTasks: List<StudyTaskEntity>,
        dailyAvailableMinutes: Int = 90,
        preferredSessionMinutes: Int = 25,
        now: Long = System.currentTimeMillis()
    ): List<ProposedTask> {
        val completedMinutes = existingCompletedTasks.sumOf { it.plannedMinutes }
        val remainingBudget = max(0, dailyAvailableMinutes - completedMinutes)
        if (remainingBudget < 15 || topics.isEmpty()) return emptyList()

        // Score each topic
        val scored = topics.map { context ->
            val (score, reason) = calculateTopicScore(context, now)
            val priorityLevel = when {
                score >= 0.65f -> 3 // High
                score >= 0.40f -> 2 // Medium
                else -> 1 // Low
            }
            val taskType = when {
                context.progress == null || context.progress.status == "NOT_STARTED" -> "LEARN"
                context.progress.confidence <= 2 -> "RECALL"
                score >= 0.6f -> "QUESTIONS"
                else -> "REVIEW"
            }
            ProposedTask(
                subjectId = context.subject.id,
                subjectName = context.subject.name,
                topicId = context.topic.id,
                topicTitle = context.topic.title,
                plannedMinutes = min(preferredSessionMinutes, remainingBudget),
                priorityScore = score,
                priorityLevel = priorityLevel,
                primaryReason = reason,
                taskType = taskType
            )
        }.sortedByDescending { it.priorityScore }

        // Balance across subjects: group by subject and take top topics round-robin
        val bySubject = scored.groupBy { it.subjectId }.toMutableMap()
        val plannedList = mutableListOf<ProposedTask>()
        var allocatedMinutes = 0

        while (allocatedMinutes + 15 <= remainingBudget && bySubject.isNotEmpty()) {
            val emptyKeys = mutableListOf<String>()
            for ((subjectId, taskList) in bySubject) {
                if (taskList.isNotEmpty()) {
                    val candidate = taskList.first()
                    bySubject[subjectId] = taskList.drop(1)
                    val taskDuration = min(preferredSessionMinutes, remainingBudget - allocatedMinutes)
                    if (taskDuration >= 15) {
                        plannedList.add(candidate.copy(plannedMinutes = taskDuration))
                        allocatedMinutes += taskDuration
                    }
                    if (allocatedMinutes + 15 > remainingBudget) break
                } else {
                    emptyKeys.add(subjectId)
                }
            }
            emptyKeys.forEach { bySubject.remove(it) }
        }

        return plannedList
    }
}
