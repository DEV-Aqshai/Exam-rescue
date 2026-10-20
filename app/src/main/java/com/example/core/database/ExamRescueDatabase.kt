package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.daos.*
import com.example.core.database.entities.*

@Database(
    entities = [
        SubjectEntity::class,
        SyllabusPackEntity::class,
        TopicEntity::class,
        TopicProgressEntity::class,
        ExamEntity::class,
        StudyTaskEntity::class,
        StudySessionEntity::class,
        QuestionEntity::class,
        QuestionAttemptEntity::class,
        MistakeEntity::class,
        NoteEntity::class,
        FlashcardEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class,
        AiProviderConfigEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ExamRescueDatabase : RoomDatabase() {
    abstract fun subjectDao(): SubjectDao
    abstract fun topicDao(): TopicDao
    abstract fun topicProgressDao(): TopicProgressDao
    abstract fun examDao(): ExamDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun questionDao(): QuestionDao
    abstract fun questionAttemptDao(): QuestionAttemptDao
    abstract fun mistakeDao(): MistakeDao
    abstract fun noteDao(): NoteDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun aiDao(): AiDao
    abstract fun aiProviderConfigDao(): AiProviderConfigDao

    companion object {
        @Volatile
        private var INSTANCE: ExamRescueDatabase? = null

        fun getInstance(context: Context): ExamRescueDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExamRescueDatabase::class.java,
                    "exam_rescue_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
