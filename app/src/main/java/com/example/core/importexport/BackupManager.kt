package com.example.core.importexport

import com.example.core.database.entities.*
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class ExportResult(
    val jsonString: String,
    val itemsCount: Int
)

sealed class ImportResult {
    data class Success(val tasksCount: Int, val notesCount: Int, val flashcardsCount: Int) : ImportResult()
    data class Error(val reason: String) : ImportResult()
}

object BackupManager {
    private const val MAX_BACKUP_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB limit
    private const val BACKUP_SCHEMA_VERSION = 1

    suspend fun exportDataToJson(studyRepository: StudyRepository): ExportResult {
        val root = JSONObject()
        root.put("schemaVersion", BACKUP_SCHEMA_VERSION)
        root.put("app", "Exam Rescue")
        root.put("exportedAt", System.currentTimeMillis())

        val tasks = studyRepository.getAllTasks().first()
        val tasksArray = JSONArray()
        tasks.forEach {
            val t = JSONObject()
                .put("id", it.id)
                .put("subjectId", it.subjectId)
                .put("topicId", it.topicId ?: "")
                .put("title", it.title)
                .put("taskType", it.taskType)
                .put("plannedMinutes", it.plannedMinutes)
                .put("priority", it.priority)
                .put("status", it.status)
            tasksArray.put(t)
        }
        root.put("tasks", tasksArray)

        val notes = studyRepository.getAllNotes().first()
        val notesArray = JSONArray()
        notes.forEach {
            val n = JSONObject()
                .put("id", it.id)
                .put("title", it.title)
                .put("body", it.body)
                .put("subjectId", it.subjectId ?: "")
            notesArray.put(n)
        }
        root.put("notes", notesArray)

        val flashcards = studyRepository.getAllFlashcards().first()
        val flashcardsArray = JSONArray()
        flashcards.forEach {
            val f = JSONObject()
                .put("id", it.id)
                .put("subjectId", it.subjectId ?: "")
                .put("front", it.front)
                .put("back", it.back)
                .put("language", it.language)
                .put("intervalDays", it.intervalDays)
                .put("easeFactor", it.easeFactor.toDouble())
                .put("repetitions", it.repetitions)
            flashcardsArray.put(f)
        }
        root.put("flashcards", flashcardsArray)

        val mistakes = studyRepository.getAllMistakes().first()
        val mistakesArray = JSONArray()
        mistakes.forEach {
            val m = JSONObject()
                .put("id", it.id)
                .put("subjectId", it.subjectId)
                .put("prompt", it.promptSnapshot)
                .put("studentAnswer", it.studentAnswer)
                .put("correctAnswer", it.answerSnapshot)
                .put("explanation", it.explanationSnapshot)
                .put("status", it.status)
            mistakesArray.put(m)
        }
        root.put("mistakes", mistakesArray)

        val totalCount = tasks.size + notes.size + flashcards.size + mistakes.size
        return ExportResult(jsonString = root.toString(2), itemsCount = totalCount)
    }

    suspend fun importDataFromJson(jsonString: String, studyRepository: StudyRepository): ImportResult {
        if (jsonString.length > MAX_BACKUP_SIZE_BYTES) {
            return ImportResult.Error("Backup file exceeds maximum allowed size (5MB).")
        }

        return try {
            val root = JSONObject(jsonString)
            if (!root.has("schemaVersion")) {
                return ImportResult.Error("Invalid backup file: Missing schema version.")
            }

            var importedTasks = 0
            var importedNotes = 0
            var importedFlashcards = 0

            val tasksArray = root.optJSONArray("tasks")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)
                    val task = StudyTaskEntity(
                        id = obj.getString("id"),
                        subjectId = obj.optString("subjectId", "math"),
                        topicId = obj.optString("topicId").ifBlank { null },
                        title = obj.getString("title"),
                        taskType = obj.optString("taskType", "REVIEW"),
                        plannedMinutes = obj.optInt("plannedMinutes", 25),
                        priority = obj.optInt("priority", 2),
                        status = obj.optString("status", "PENDING"),
                        createdBy = "USER"
                    )
                    studyRepository.insertTask(task)
                    importedTasks++
                }
            }

            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.getJSONObject(i)
                    val note = NoteEntity(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        body = obj.getString("body"),
                        subjectId = obj.optString("subjectId").ifBlank { null }
                    )
                    studyRepository.insertNote(note)
                    importedNotes++
                }
            }

            val flashcardsArray = root.optJSONArray("flashcards")
            if (flashcardsArray != null) {
                for (i in 0 until flashcardsArray.length()) {
                    val obj = flashcardsArray.getJSONObject(i)
                    val flashcard = FlashcardEntity(
                        id = obj.getString("id"),
                        subjectId = obj.optString("subjectId").ifBlank { null },
                        front = obj.getString("front"),
                        back = obj.getString("back"),
                        language = obj.optString("language", "ENGLISH"),
                        intervalDays = obj.optInt("intervalDays", 1),
                        easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                        repetitions = obj.optInt("repetitions", 0)
                    )
                    studyRepository.insertFlashcard(flashcard)
                    importedFlashcards++
                }
            }

            ImportResult.Success(importedTasks, importedNotes, importedFlashcards)
        } catch (e: Exception) {
            ImportResult.Error("Failed to parse backup JSON: ${e.localizedMessage}")
        }
    }
}
