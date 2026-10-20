package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subjects",
    indices = [Index(value = ["sortOrder"])]
)
data class SubjectEntity(
    @PrimaryKey val id: String,
    val nameKey: String,
    val name: String,
    val medium: String = "BOTH", // TAMIL, ENGLISH, BOTH
    val sortOrder: Int = 0,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "syllabus_packs")
data class SyllabusPackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val version: String,
    val language: String,
    val sourceName: String,
    val sourceUrlOrCitation: String,
    val reviewedAt: Long,
    val checksum: String = ""
)

@Entity(
    tableName = "topics",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["parentTopicId"])
    ]
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val parentTopicId: String? = null,
    val title: String,
    val description: String = "",
    val syllabusPackId: String? = null,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "topic_progress",
    foreignKeys = [
        ForeignKey(
            entity = TopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TopicProgressEntity(
    @PrimaryKey val topicId: String,
    val status: String = "NOT_STARTED", // NOT_STARTED, LEARNING, REVISING, CONFIDENT
    val confidence: Int = 1, // 1 to 5
    val lastRevisedAt: Long? = null,
    val nextReviewAt: Long? = null,
    val notes: String? = null
)

@Entity(
    tableName = "exams",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"]), Index(value = ["examAt"])]
)
data class ExamEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val title: String,
    val examAt: Long,
    val locationNote: String? = null,
    val reminderEnabled: Boolean = true
)

@Entity(
    tableName = "study_tasks",
    indices = [Index(value = ["subjectId"]), Index(value = ["status"]), Index(value = ["dueAt"])]
)
data class StudyTaskEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val topicId: String? = null,
    val title: String,
    val taskType: String = "REVIEW", // LEARN, RECALL, QUESTIONS, REVIEW
    val plannedMinutes: Int = 25,
    val priority: Int = 2, // 1=Low, 2=Medium, 3=High
    val dueAt: Long? = null,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, SKIPPED
    val createdBy: String = "LOCAL_RULES", // USER, LOCAL_RULES, AI_PROPOSAL
    val reasonExplanation: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(
    tableName = "study_sessions",
    indices = [Index(value = ["startedAt"])]
)
data class StudySessionEntity(
    @PrimaryKey val id: String,
    val taskId: String? = null,
    val subjectId: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val durationSeconds: Int = 0,
    val sessionType: String = "REVIEW",
    val selfRating: Int? = null,
    val notes: String? = null
)

@Entity(
    tableName = "questions",
    indices = [Index(value = ["subjectId"]), Index(value = ["topicId"]), Index(value = ["difficulty"])]
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val topicId: String? = null,
    val prompt: String,
    val type: String = "MULTIPLE_CHOICE", // MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, FLASHCARD
    val optionsJson: String? = null, // JSON array string e.g. ["A", "B", "C", "D"]
    val answerText: String,
    val explanation: String = "",
    val difficulty: String = "MEDIUM", // EASY, MEDIUM, HARD
    val language: String = "ENGLISH", // TAMIL, ENGLISH
    val sourceType: String = "SAMPLE_PACK", // SAMPLE_PACK, USER_CREATED, AI_GENERATED
    val sourceAttribution: String? = "Sample GCE O/L Material",
    val packId: String? = null
)

@Entity(
    tableName = "question_attempts",
    indices = [Index(value = ["questionId"]), Index(value = ["attemptedAt"])]
)
data class QuestionAttemptEntity(
    @PrimaryKey val id: String,
    val questionId: String,
    val responseText: String,
    val isCorrect: Boolean,
    val attemptedAt: Long = System.currentTimeMillis(),
    val timeSpentSeconds: Int? = null,
    val feedback: String? = null
)

@Entity(
    tableName = "mistakes",
    indices = [Index(value = ["subjectId"]), Index(value = ["status"])]
)
data class MistakeEntity(
    @PrimaryKey val id: String,
    val questionId: String? = null,
    val subjectId: String,
    val topicId: String? = null,
    val promptSnapshot: String,
    val studentAnswer: String,
    val answerSnapshot: String,
    val explanationSnapshot: String = "",
    val status: String = "NEEDS_PRACTICE", // NEEDS_PRACTICE, UNDERSTOOD
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(
    tableName = "notes",
    indices = [Index(value = ["subjectId"]), Index(value = ["topicId"]), Index(value = ["updatedAt"])]
)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flashcards",
    indices = [Index(value = ["subjectId"]), Index(value = ["dueAt"])]
)
data class FlashcardEntity(
    @PrimaryKey val id: String,
    val subjectId: String? = null,
    val topicId: String? = null,
    val front: String,
    val back: String,
    val language: String = "ENGLISH",
    val intervalDays: Int = 1,
    val easeFactor: Float = 2.5f,
    val repetitions: Int = 0,
    val dueAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null
)

@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val providerId: String,
    val modelId: String,
    val language: String = "ENGLISH",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_messages",
    indices = [Index(value = ["conversationId"])]
)
data class AiMessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // user, assistant, system, tool
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val providerMetadataJson: String? = null
)

@Entity(tableName = "ai_provider_configs")
data class AiProviderConfigEntity(
    @PrimaryKey val providerId: String, // gemini, openrouter, featherless, xai, custom
    val displayName: String,
    val endpoint: String,
    val modelId: String,
    val encryptedKeyBlob: String? = null,
    val enabled: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
