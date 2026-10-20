package com.example.ai.agents

import com.example.ai.models.AiMessage
import com.example.ai.models.AiRequest
import com.example.ai.models.AiResult
import com.example.ai.providers.AiProviderFactory
import com.example.core.database.entities.AiProviderConfigEntity
import com.example.core.database.entities.FlashcardEntity
import com.example.core.database.entities.StudyTaskEntity
import com.example.core.security.KeystoreManager
import com.example.data.repositories.StudyRepository
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class AgentType(val displayNameResKey: String, val systemRole: String) {
    STUDY_PLANNER_COACH(
        "tutor_agent_planner_coach",
        "You are an encouraging Sri Lankan GCE O/L Study Planner Coach. Given upcoming exams and topic confidence, propose a realistic daily revision plan. Never overburden the student. Always offer solutions in their preferred language (Tamil or English)."
    ),
    BILINGUAL_CONCEPT_TUTOR(
        "tutor_agent_concept_tutor",
        "You are a master Sri Lankan GCE O/L bilingual tutor. Explain concepts clearly step-by-step using simple analogies and realistic textbook examples. When responding in Tamil, write natural Sri Lankan Tamil and retain standard English technical terms where helpful."
    ),
    ACTIVE_RECALL_COACH(
        "tutor_agent_recall_coach",
        "You are an Active Recall Coach for Sri Lankan GCE O/L. Ask the student exactly ONE focused question on their selected topic. Once they reply, give constructive feedback and a follow-up question. Do not reveal the full answer immediately."
    ),
    MISTAKE_DETECTIVE(
        "tutor_agent_mistake_detective",
        "You are the Mistake Detective. Analyze the student's incorrect answer compared to the reference answer. Gently identify the underlying misconception without shame, and explain the correct path."
    ),
    FLASHCARD_BUILDER(
        "tutor_agent_flashcard_builder",
        "You are a Flashcard Builder. Extract key definitions, formulas, or facts into concise front/back flashcard pairs. Output structured format so the student can review and approve."
    ),
    NOTES_SUMMARIZER(
        "tutor_agent_notes_summarizer",
        "You are a Study Notes Summarizer. Create concise, high-yield bullet points of the provided notes for quick GCE O/L revision. Highlight key formulas and terms."
    ),
    EXAM_COUNTDOWN_COACH(
        "tutor_agent_exam_countdown",
        "You are an Exam Countdown Coach. Provide a calm, reassuring 3-step action list prioritizing highest-impact review topics without panic or stress."
    ),
    PRACTICE_PAPER_BUILDER(
        "tutor_agent_paper_builder",
        "You are a Practice Question Builder for GCE O/L. Generate 2-3 sample questions with multiple choices, correct answers, and thorough explanations. Clearly mark content as sample practice questions."
    ),
    ANSWER_FEEDBACK_COACH(
        "tutor_agent_feedback_coach",
        "You are an Answer Feedback Coach. Review the student's written response against standard GCE O/L marking criteria. Provide constructive pointers for gaining full marks."
    ),
    FOCUS_SESSION_COACH(
        "tutor_agent_focus_coach",
        "You are a Focus Session Coach. Help the student select a manageable 25-minute study task and set an intention for deep, calm concentration."
    )
}

data class AgentExecutionResult(
    val replyText: String,
    val proposedTasks: List<StudyTaskEntity> = emptyList(),
    val proposedFlashcards: List<FlashcardEntity> = emptyList(),
    val modelUsed: String = ""
)

class AgentOrchestrator(
    private val studyRepository: StudyRepository
) {
    suspend fun executeAgent(
        agentType: AgentType,
        userInput: String,
        conversationHistory: List<AiMessage>,
        config: AiProviderConfigEntity,
        language: String = "ENGLISH",
        includeNotesContext: Boolean = false,
        includeMistakesContext: Boolean = false,
        selectedTopicId: String? = null
    ): AiResult<AgentExecutionResult> {
        val apiKey = KeystoreManager.decrypt(config.encryptedKeyBlob ?: "")
        if (apiKey.isBlank()) return AiResult.InvalidKey

        // Build context strictly with allowed local tools
        val contextBuilder = StringBuilder()
        contextBuilder.append("STUDENT PREFERRED LANGUAGE: $language\n")
        contextBuilder.append("TARGET LEVEL: Sri Lankan GCE Ordinary Level (O/L)\n")

        // Include upcoming exams
        val exams = studyRepository.getUpcomingExams().first()
        if (exams.isNotEmpty()) {
            contextBuilder.append("\nUPCOMING EXAMS:\n")
            exams.take(3).forEach {
                contextBuilder.append("- ${it.title} on ${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it.examAt))}\n")
            }
        }

        // Include selected topic if any
        if (!selectedTopicId.isNullOrBlank()) {
            val topics = studyRepository.getAllTopics().first()
            val topic = topics.firstOrNull { it.id == selectedTopicId }
            if (topic != null) {
                val progress = studyRepository.getProgressForTopic(topic.id).first()
                contextBuilder.append("\nCURRENT TOPIC: ${topic.title} (Confidence: ${progress?.confidence ?: 1}/5)\n")
            }
        }

        // Include user notes if consented
        if (includeNotesContext) {
            val notes = studyRepository.getAllNotes().first()
            if (notes.isNotEmpty()) {
                contextBuilder.append("\nUSER STUDY NOTES (EXCERPT):\n")
                notes.take(2).forEach {
                    contextBuilder.append("Title: ${it.title}\nContent: ${it.body.take(200)}\n")
                }
            }
        }

        // Include recent mistakes if consented
        if (includeMistakesContext) {
            val mistakes = studyRepository.getActiveMistakes().first()
            if (mistakes.isNotEmpty()) {
                contextBuilder.append("\nRECENT MISTAKES NEEDING REVIEW:\n")
                mistakes.take(2).forEach {
                    contextBuilder.append("Prompt: ${it.promptSnapshot}\nStudent gave: ${it.studentAnswer}\nCorrect: ${it.answerSnapshot}\n")
                }
            }
        }

        val systemPrompt = """
            ${agentType.systemRole}
            CONTEXT:
            $contextBuilder
            
            RULES:
            1. All answers must be tailored for Sri Lankan GCE O/L students.
            2. Reply in $language.
            3. Do not invent false exam citations or official marking schemes.
            4. If proposing study tasks, format them as: [PROPOSE_TASK: Title | Minutes | Type]
            5. If proposing flashcards, format them as: [PROPOSE_FLASHCARD: Front | Back]
        """.trimIndent()

        val messages = mutableListOf<AiMessage>()
        messages.addAll(conversationHistory.takeLast(6))
        messages.add(AiMessage(role = "user", content = userInput))

        val provider = AiProviderFactory.getProvider(config.providerId)
        val request = AiRequest(
            messages = messages,
            systemPrompt = systemPrompt,
            responseLanguage = language
        )

        return when (val result = provider.generate(request, config, apiKey)) {
            is AiResult.Success -> {
                val responseText = result.data.text
                val proposedTasks = parseProposedTasks(responseText, selectedTopicId)
                val proposedFlashcards = parseProposedFlashcards(responseText, selectedTopicId, language)
                val cleanedText = cleanProposalTags(responseText)

                AiResult.Success(
                    AgentExecutionResult(
                        replyText = cleanedText,
                        proposedTasks = proposedTasks,
                        proposedFlashcards = proposedFlashcards,
                        modelUsed = result.data.modelUsed
                    )
                )
            }
            is AiResult.InvalidKey -> AiResult.InvalidKey
            is AiResult.QuotaExceeded -> AiResult.QuotaExceeded
            is AiResult.Offline -> AiResult.Offline
            is AiResult.Timeout -> AiResult.Timeout
            is AiResult.ProviderError -> result
            is AiResult.MalformedResponse -> result
        }
    }

    private fun parseProposedTasks(text: String, topicId: String?): List<StudyTaskEntity> {
        val taskRegex = Regex("\\[PROPOSE_TASK:\\s*([^|]+)\\|\\s*(\\d+)\\s*\\|\\s*([^]]+)\\]")
        return taskRegex.findAll(text).map { match ->
            val title = match.groupValues[1].trim()
            val minutes = match.groupValues[2].trim().toIntOrNull() ?: 25
            val type = match.groupValues[3].trim().uppercase()
            StudyTaskEntity(
                id = UUID.randomUUID().toString(),
                subjectId = "math", // Default or topic subject
                topicId = topicId,
                title = title,
                taskType = if (type in listOf("LEARN", "RECALL", "QUESTIONS", "REVIEW")) type else "REVIEW",
                plannedMinutes = minutes,
                priority = 2,
                createdBy = "AI_PROPOSAL",
                reasonExplanation = "Proposed by AI Tutor"
            )
        }.toList()
    }

    private fun parseProposedFlashcards(text: String, topicId: String?, language: String): List<FlashcardEntity> {
        val cardRegex = Regex("\\[PROPOSE_FLASHCARD:\\s*([^|]+)\\|\\s*([^]]+)\\]")
        return cardRegex.findAll(text).map { match ->
            val front = match.groupValues[1].trim()
            val back = match.groupValues[2].trim()
            FlashcardEntity(
                id = UUID.randomUUID().toString(),
                topicId = topicId,
                front = front,
                back = back,
                language = language,
                intervalDays = 1
            )
        }.toList()
    }

    private fun cleanProposalTags(text: String): String {
        return text
            .replace(Regex("\\[PROPOSE_TASK:[^]]+\\]"), "")
            .replace(Regex("\\[PROPOSE_FLASHCARD:[^]]+\\]"), "")
            .trim()
    }
}
