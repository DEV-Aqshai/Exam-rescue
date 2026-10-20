package com.example.core.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.core.database.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects ORDER BY sortOrder ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: String)
}

@Dao
interface TopicDao {
    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY sortOrder ASC")
    fun getTopicsForSubject(subjectId: String): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics ORDER BY sortOrder ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id LIMIT 1")
    suspend fun getTopicById(id: String): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: String)
}

@Dao
interface TopicProgressDao {
    @Query("SELECT * FROM topic_progress")
    fun getAllProgress(): Flow<List<TopicProgressEntity>>

    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId LIMIT 1")
    fun getProgressForTopic(topicId: String): Flow<TopicProgressEntity?>

    @Query("SELECT * FROM topic_progress WHERE topicId = :topicId LIMIT 1")
    suspend fun getProgressForTopicDirect(topicId: String): TopicProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: TopicProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProgress(progressList: List<TopicProgressEntity>)
}

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY examAt ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE examAt >= :now ORDER BY examAt ASC")
    fun getUpcomingExams(now: Long = System.currentTimeMillis()): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE examAt >= :now ORDER BY examAt ASC LIMIT 1")
    fun getNextExam(now: Long = System.currentTimeMillis()): Flow<ExamEntity?>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: String): ExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Query("DELETE FROM exams WHERE id = :id")
    suspend fun deleteExamById(id: String)
}

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY dueAt ASC, priority DESC")
    fun getAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE status != 'COMPLETED' AND status != 'SKIPPED' ORDER BY priority DESC, dueAt ASC")
    fun getPendingTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE dueAt BETWEEN :startOfDay AND :endOfDay ORDER BY priority DESC")
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): StudyTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<StudyTaskEntity>)

    @Update
    suspend fun updateTask(task: StudyTaskEntity)

    @Query("UPDATE study_tasks SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String, completedAt: Long?)

    @Query("DELETE FROM study_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE startedAt >= :sinceTimestamp ORDER BY startedAt DESC")
    fun getSessionsSince(sinceTimestamp: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE startedAt >= :sinceTimestamp")
    fun getTotalStudySecondsSince(sinceTimestamp: Long): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions")
    fun getAllQuestions(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE subjectId = :subjectId")
    fun getQuestionsForSubject(subjectId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE topicId = :topicId")
    fun getQuestionsForTopic(topicId: String): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getQuestionById(id: String): QuestionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE id = :id")
    suspend fun deleteQuestionById(id: String)
}

@Dao
interface QuestionAttemptDao {
    @Query("SELECT * FROM question_attempts ORDER BY attemptedAt DESC")
    fun getAllAttempts(): Flow<List<QuestionAttemptEntity>>

    @Query("SELECT COUNT(*) FROM question_attempts")
    fun getTotalAttemptsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM question_attempts WHERE isCorrect = 1")
    fun getCorrectAttemptsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordAttempt(attempt: QuestionAttemptEntity)
}

@Dao
interface MistakeDao {
    @Query("SELECT * FROM mistakes ORDER BY createdAt DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE status = 'NEEDS_PRACTICE' ORDER BY createdAt DESC")
    fun getActiveMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getMistakesForSubject(subjectId: String): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes WHERE id = :id LIMIT 1")
    suspend fun getMistakeById(id: String): MistakeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeEntity)

    @Query("UPDATE mistakes SET status = :status, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateMistakeStatus(id: String, status: String, resolvedAt: Long?)

    @Query("DELETE FROM mistakes WHERE id = :id")
    suspend fun deleteMistakeById(id: String)
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY updatedAt DESC")
    fun getNotesForSubject(subjectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY dueAt ASC")
    fun getAllFlashcards(): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE dueAt <= :now ORDER BY dueAt ASC")
    fun getDueFlashcards(now: Long = System.currentTimeMillis()): Flow<List<FlashcardEntity>>

    @Query("SELECT * FROM flashcards WHERE subjectId = :subjectId ORDER BY dueAt ASC")
    fun getFlashcardsForSubject(subjectId: String): Flow<List<FlashcardEntity>>

    @Query("SELECT COUNT(*) FROM flashcards WHERE dueAt <= :now")
    fun getDueCount(now: Long = System.currentTimeMillis()): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: FlashcardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<FlashcardEntity>)

    @Update
    suspend fun updateFlashcard(flashcard: FlashcardEntity)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: String)
}

@Dao
interface AiDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAt DESC")
    fun getConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<AiMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity)

    @Query("DELETE FROM ai_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)

    @Query("DELETE FROM ai_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM ai_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM ai_conversations")
    suspend fun deleteAllConversations()
}

@Dao
interface AiProviderConfigDao {
    @Query("SELECT * FROM ai_provider_configs")
    fun getAllConfigs(): Flow<List<AiProviderConfigEntity>>

    @Query("SELECT * FROM ai_provider_configs WHERE providerId = :providerId LIMIT 1")
    suspend fun getConfigById(providerId: String): AiProviderConfigEntity?

    @Query("SELECT * FROM ai_provider_configs WHERE enabled = 1 LIMIT 1")
    fun getActiveConfigFlow(): Flow<AiProviderConfigEntity?>

    @Query("SELECT * FROM ai_provider_configs WHERE enabled = 1 LIMIT 1")
    suspend fun getActiveConfigDirect(): AiProviderConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: AiProviderConfigEntity)

    @Query("UPDATE ai_provider_configs SET enabled = CASE WHEN providerId = :activeProviderId THEN 1 ELSE 0 END")
    suspend fun setActiveProvider(activeProviderId: String)

    @Query("UPDATE ai_provider_configs SET encryptedKeyBlob = NULL WHERE providerId = :providerId")
    suspend fun clearKeyForProvider(providerId: String)
}
