package com.example.data.repositories

import com.example.core.common.FlashcardRating
import com.example.core.common.SpacedRepetitionCalculator
import com.example.core.database.ExamRescueDatabase
import com.example.core.database.entities.*
import com.example.data.seeds.SampleDataSeed
import com.example.domain.planner.DeterministicPlanner
import com.example.domain.planner.ProposedTask
import com.example.domain.planner.TopicWithContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class StudyRepository(private val database: ExamRescueDatabase) {

    private val subjectDao = database.subjectDao()
    private val topicDao = database.topicDao()
    private val topicProgressDao = database.topicProgressDao()
    private val examDao = database.examDao()
    private val studyTaskDao = database.studyTaskDao()
    private val studySessionDao = database.studySessionDao()
    private val questionDao = database.questionDao()
    private val questionAttemptDao = database.questionAttemptDao()
    private val mistakeDao = database.mistakeDao()
    private val noteDao = database.noteDao()
    private val flashcardDao = database.flashcardDao()

    suspend fun seedInitialDataIfEmpty() {
        val existingSubjects = subjectDao.getAllSubjects().first()
        if (existingSubjects.isEmpty()) {
            subjectDao.insertSubjects(SampleDataSeed.getInitialSubjects())
            topicDao.insertTopics(SampleDataSeed.getInitialTopics())
            questionDao.insertQuestions(SampleDataSeed.getInitialQuestions())
            flashcardDao.insertFlashcards(SampleDataSeed.getInitialFlashcards())

            // Initialize default progress for first topic of each subject
            val initialProgress = SampleDataSeed.getInitialTopics().take(6).map {
                TopicProgressEntity(
                    topicId = it.id,
                    status = "LEARNING",
                    confidence = 2,
                    lastRevisedAt = System.currentTimeMillis() - 86_400_000L,
                    nextReviewAt = System.currentTimeMillis()
                )
            }
            topicProgressDao.insertAllProgress(initialProgress)
        }
    }

    // Subjects & Topics
    fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>> = topicDao.getTopicsForSubject(subjectId)
    fun getAllTopics(): Flow<List<TopicEntity>> = topicDao.getAllTopics()
    suspend fun getSubjectById(id: String) = subjectDao.getSubjectById(id)
    suspend fun insertSubject(subject: SubjectEntity) = subjectDao.insertSubject(subject)
    suspend fun deleteSubject(id: String) = subjectDao.deleteSubjectById(id)

    suspend fun insertTopic(topic: TopicEntity) = topicDao.insertTopic(topic)
    suspend fun deleteTopic(id: String) = topicDao.deleteTopicById(id)

    // Topic Progress
    fun getAllProgress(): Flow<List<TopicProgressEntity>> = topicProgressDao.getAllProgress()
    fun getProgressForTopic(topicId: String): Flow<TopicProgressEntity?> = topicProgressDao.getProgressForTopic(topicId)
    suspend fun updateTopicProgress(progress: TopicProgressEntity) = topicProgressDao.insertOrUpdateProgress(progress)

    suspend fun setTopicConfidence(topicId: String, confidence: Int, status: String = "REVISING") {
        val existing = topicProgressDao.getProgressForTopicDirect(topicId)
        val updated = existing?.copy(
            confidence = confidence,
            status = status,
            lastRevisedAt = System.currentTimeMillis()
        ) ?: TopicProgressEntity(
            topicId = topicId,
            confidence = confidence,
            status = status,
            lastRevisedAt = System.currentTimeMillis()
        )
        topicProgressDao.insertOrUpdateProgress(updated)
    }

    // Exams
    fun getAllExams(): Flow<List<ExamEntity>> = examDao.getAllExams()
    fun getUpcomingExams(): Flow<List<ExamEntity>> = examDao.getUpcomingExams()
    fun getNextExam(): Flow<ExamEntity?> = examDao.getNextExam()
    suspend fun insertExam(exam: ExamEntity) = examDao.insertExam(exam)
    suspend fun deleteExam(id: String) = examDao.deleteExamById(id)

    // Study Tasks
    fun getAllTasks(): Flow<List<StudyTaskEntity>> = studyTaskDao.getAllTasks()
    fun getPendingTasks(): Flow<List<StudyTaskEntity>> = studyTaskDao.getPendingTasks()
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudyTaskEntity>> =
        studyTaskDao.getTasksForDay(startOfDay, endOfDay)

    suspend fun insertTask(task: StudyTaskEntity) = studyTaskDao.insertTask(task)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>) = studyTaskDao.insertTasks(tasks)
    suspend fun updateTaskStatus(id: String, status: String, completedAt: Long? = if (status == "COMPLETED") System.currentTimeMillis() else null) =
        studyTaskDao.updateTaskStatus(id, status, completedAt)
    suspend fun deleteTask(id: String) = studyTaskDao.deleteTaskById(id)

    // Planner Generation
    suspend fun generateDailyTasks(dailyMinutes: Int, sessionLengthMinutes: Int): List<ProposedTask> {
        val topics = topicDao.getAllTopics().first()
        val subjects = subjectDao.getAllSubjects().first().associateBy { it.id }
        val progressMap = topicProgressDao.getAllProgress().first().associateBy { it.topicId }
        val exams = examDao.getAllExams().first().associateBy { it.subjectId }
        val completedToday = studyTaskDao.getAllTasks().first().filter { it.status == "COMPLETED" }

        val topicContexts = topics.mapNotNull { topic ->
            val subject = subjects[topic.subjectId] ?: return@mapNotNull null
            TopicWithContext(
                topic = topic,
                subject = subject,
                progress = progressMap[topic.id],
                relatedExam = exams[topic.subjectId]
            )
        }

        return DeterministicPlanner.generateDailyPlan(
            topics = topicContexts,
            existingCompletedTasks = completedToday,
            dailyAvailableMinutes = dailyMinutes,
            preferredSessionMinutes = sessionLengthMinutes
        )
    }

    // Sessions
    fun getAllSessions(): Flow<List<StudySessionEntity>> = studySessionDao.getAllSessions()
    fun getTotalStudySecondsSince(timestamp: Long): Flow<Int?> = studySessionDao.getTotalStudySecondsSince(timestamp)
    suspend fun insertSession(session: StudySessionEntity) = studySessionDao.insertSession(session)

    // Questions & Practice
    fun getAllQuestions(): Flow<List<QuestionEntity>> = questionDao.getAllQuestions()
    fun getQuestionsForSubject(subjectId: String): Flow<List<QuestionEntity>> = questionDao.getQuestionsForSubject(subjectId)
    suspend fun insertQuestion(question: QuestionEntity) = questionDao.insertQuestion(question)
    suspend fun deleteQuestion(id: String) = questionDao.deleteQuestionById(id)

    // Attempts & Accuracy
    fun getAllAttempts(): Flow<List<QuestionAttemptEntity>> = questionAttemptDao.getAllAttempts()
    fun getTotalAttemptsCount(): Flow<Int> = questionAttemptDao.getTotalAttemptsCount()
    fun getCorrectAttemptsCount(): Flow<Int> = questionAttemptDao.getCorrectAttemptsCount()
    suspend fun recordAttempt(attempt: QuestionAttemptEntity) = questionAttemptDao.recordAttempt(attempt)

    // Mistakes
    fun getAllMistakes(): Flow<List<MistakeEntity>> = mistakeDao.getAllMistakes()
    fun getActiveMistakes(): Flow<List<MistakeEntity>> = mistakeDao.getActiveMistakes()
    suspend fun insertMistake(mistake: MistakeEntity) = mistakeDao.insertMistake(mistake)
    suspend fun updateMistakeStatus(id: String, status: String) =
        mistakeDao.updateMistakeStatus(id, status, if (status == "UNDERSTOOD") System.currentTimeMillis() else null)
    suspend fun deleteMistake(id: String) = mistakeDao.deleteMistakeById(id)

    // Notes
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()
    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)
    suspend fun insertNote(note: NoteEntity) = noteDao.insertNote(note)
    suspend fun deleteNote(id: String) = noteDao.deleteNoteById(id)

    // Flashcards & Spaced Repetition
    fun getAllFlashcards(): Flow<List<FlashcardEntity>> = flashcardDao.getAllFlashcards()
    fun getDueFlashcards(): Flow<List<FlashcardEntity>> = flashcardDao.getDueFlashcards()
    fun getDueFlashcardsCount(): Flow<Int> = flashcardDao.getDueCount()
    suspend fun insertFlashcard(flashcard: FlashcardEntity) = flashcardDao.insertFlashcard(flashcard)
    suspend fun deleteFlashcard(id: String) = flashcardDao.deleteFlashcardById(id)

    suspend fun reviewFlashcard(flashcard: FlashcardEntity, rating: FlashcardRating) {
        val calcResult = SpacedRepetitionCalculator.calculateNextReview(
            rating = rating,
            currentIntervalDays = flashcard.intervalDays,
            currentEaseFactor = flashcard.easeFactor,
            currentRepetitions = flashcard.repetitions
        )
        val updated = flashcard.copy(
            intervalDays = calcResult.intervalDays,
            easeFactor = calcResult.easeFactor,
            repetitions = calcResult.repetitions,
            dueAt = calcResult.nextDueAtMillis,
            lastReviewedAt = System.currentTimeMillis()
        )
        flashcardDao.updateFlashcard(updated)
    }

    suspend fun clearAllData() {
        database.clearAllTables()
    }
}
