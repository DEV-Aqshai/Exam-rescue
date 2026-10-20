package com.example.domain.planner

import com.example.core.database.entities.*
import org.junit.Assert.*
import org.junit.Test

class DeterministicPlannerTest {

    @Test
    fun `topic with approaching exam and low confidence receives high priority score`() {
        val now = System.currentTimeMillis()
        val subject = SubjectEntity(id = "math", nameKey = "math", name = "Mathematics")
        val topic = TopicEntity(id = "t1", subjectId = "math", title = "Quadratic Equations")
        val exam = ExamEntity(id = "e1", subjectId = "math", title = "Math Paper 1", examAt = now + (7L * 86_400_000L))
        val progress = TopicProgressEntity(topicId = "t1", status = "LEARNING", confidence = 1)

        val context = TopicWithContext(topic, subject, progress, exam)
        val (score, reason) = DeterministicPlanner.calculateTopicScore(context, now)

        assertTrue("Expected score >= 0.65 for high priority, but was $score", score >= 0.65f)
        assertTrue(reason.contains("Exam approaching") || reason.contains("Low confidence"))
    }

    @Test
    fun `topic with high confidence and no approaching exam receives lower score`() {
        val now = System.currentTimeMillis()
        val subject = SubjectEntity(id = "hist", nameKey = "hist", name = "History")
        val topic = TopicEntity(id = "t2", subjectId = "hist", title = "Ancient Era")
        val progress = TopicProgressEntity(topicId = "t2", status = "CONFIDENT", confidence = 5)

        val context = TopicWithContext(topic, subject, progress, null)
        val (score, _) = DeterministicPlanner.calculateTopicScore(context, now)

        assertTrue("Expected score < 0.35 for mastered topic without exam, was $score", score < 0.35f)
    }

    @Test
    fun `daily plan respects daily budget and does not exceed limit`() {
        val now = System.currentTimeMillis()
        val subject = SubjectEntity(id = "math", nameKey = "math", name = "Mathematics")
        val topics = (1..6).map {
            TopicWithContext(
                topic = TopicEntity(id = "top_$it", subjectId = "math", title = "Topic $it"),
                subject = subject,
                progress = TopicProgressEntity(topicId = "top_$it", confidence = 2),
                relatedExam = null
            )
        }

        val proposed = DeterministicPlanner.generateDailyPlan(
            topics = topics,
            existingCompletedTasks = emptyList(),
            dailyAvailableMinutes = 60,
            preferredSessionMinutes = 25,
            now = now
        )

        val totalMinutes = proposed.sumOf { it.plannedMinutes }
        assertTrue("Total minutes ($totalMinutes) must not exceed daily budget (60)", totalMinutes <= 60)
        assertTrue("Should have proposed at least 2 tasks", proposed.size >= 2)
    }

    @Test
    fun `preserves completed tasks and reduces available budget accordingly`() {
        val now = System.currentTimeMillis()
        val subject = SubjectEntity(id = "sci", nameKey = "sci", name = "Science")
        val completedTask = StudyTaskEntity(
            id = "c1",
            subjectId = "sci",
            title = "Completed Session",
            plannedMinutes = 40,
            status = "COMPLETED"
        )
        val topics = listOf(
            TopicWithContext(
                topic = TopicEntity(id = "top_1", subjectId = "sci", title = "Photosynthesis"),
                subject = subject,
                progress = null,
                relatedExam = null
            )
        )

        val proposed = DeterministicPlanner.generateDailyPlan(
            topics = topics,
            existingCompletedTasks = listOf(completedTask),
            dailyAvailableMinutes = 60, // 60 - 40 = 20 remaining
            preferredSessionMinutes = 25,
            now = now
        )

        val totalProposed = proposed.sumOf { it.plannedMinutes }
        assertTrue("Remaining budget must be <= 20 minutes", totalProposed <= 20)
    }
}
