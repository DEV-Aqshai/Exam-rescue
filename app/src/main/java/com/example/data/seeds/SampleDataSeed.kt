package com.example.data.seeds

import com.example.core.database.entities.*
import java.util.UUID

object SampleDataSeed {

    fun getInitialSyllabusPack(): SyllabusPackEntity {
        return SyllabusPackEntity(
            id = "sl_gce_ol_sample_v1",
            title = "Sri Lankan GCE O/L Sample Syllabus Pack",
            version = "2026.1",
            language = "BILINGUAL",
            sourceName = "Curriculum Sample Reference",
            sourceUrlOrCitation = "Sample GCE Ordinary Level curriculum guide",
            reviewedAt = System.currentTimeMillis()
        )
    }

    fun getInitialSubjects(): List<SubjectEntity> {
        return listOf(
            SubjectEntity(id = "math", nameKey = "subject_math", name = "Mathematics (கணிதம்)", sortOrder = 1),
            SubjectEntity(id = "science", nameKey = "subject_science", name = "Science (விஞ்ஞானம்)", sortOrder = 2),
            SubjectEntity(id = "english", nameKey = "subject_english", name = "English Language (ஆங்கிலம்)", sortOrder = 3),
            SubjectEntity(id = "history", nameKey = "subject_history", name = "History (வரலாறு)", sortOrder = 4),
            SubjectEntity(id = "tamil", nameKey = "subject_tamil", name = "Tamil Language & Literature (தமிழ் மொழி)", sortOrder = 5),
            SubjectEntity(id = "ict", nameKey = "subject_ict", name = "ICT (தகவல் தொழினுட்பம்)", sortOrder = 6)
        )
    }

    fun getInitialTopics(): List<TopicEntity> {
        return listOf(
            // Mathematics
            TopicEntity("math_1", "math", null, "Real Numbers, Indices & Logarithms (மெய்யெண்கள், சுட்டிகளும் மடக்கைகளும்)", "Indices laws, logarithms, scientific notation", "sl_gce_ol_sample_v1", 1),
            TopicEntity("math_2", "math", null, "Algebraic Expressions & Quadratic Equations (அட்சரகணிதக் கோவைகளும் இருபடிச் சமன்பாடுகளும்)", "Factorization, completing square, quadratic formula", "sl_gce_ol_sample_v1", 2),
            TopicEntity("math_3", "math", null, "Geometry & Angle Theorems (கேத்திர கணிதமும் தேற்றங்களும்)", "Circle theorems, angle properties, congruency", "sl_gce_ol_sample_v1", 3),
            TopicEntity("math_4", "math", null, "Trigonometry, Bearings & Angles of Elevation (திரிகோணமிதியும் திசைகோள்களும்)", "Sin, Cos, Tan ratios, 3-figure bearings", "sl_gce_ol_sample_v1", 4),
            TopicEntity("math_5", "math", null, "Statistics & Probability (புள்ளியியலும் நிகழ்தகவும்)", "Grouped frequency, cumulative frequency, probability trees", "sl_gce_ol_sample_v1", 5),

            // Science
            TopicEntity("sci_1", "science", null, "Biological Processes & Human Body Systems (உயிரியல் செயன்முறைகளும் மனித உடலியலும்)", "Photosynthesis, respiration, circulatory and excretory systems", "sl_gce_ol_sample_v1", 1),
            TopicEntity("sci_2", "science", null, "Matter, Atomic Structure & Periodic Table (சடப்பொருள், அணு அமைப்பும் ஆவர்த்தன அட்டவணையும்)", "Elements, compounds, chemical bonding and reactions", "sl_gce_ol_sample_v1", 2),
            TopicEntity("sci_3", "science", null, "Mechanics, Newton's Laws & Energy (பொறிமுறையியல், நியூட்டனின் விதிகளும் சக்தியும்)", "Force, acceleration, work, power, gravitational potential", "sl_gce_ol_sample_v1", 3),
            TopicEntity("sci_4", "science", null, "Current Electricity & Magnetism (மின்னோட்டமும் காந்தவியலும்)", "Ohm's law, circuits, electromagnetic induction", "sl_gce_ol_sample_v1", 4),

            // English
            TopicEntity("eng_1", "english", null, "Grammar, Tenses & Active/Passive Voice", "Past/present/future perfect, conditional clauses", "sl_gce_ol_sample_v1", 1),
            TopicEntity("eng_2", "english", null, "Formal & Informal Letter Writing", "Letters to the editor, inquiries, invitations", "sl_gce_ol_sample_v1", 2),
            TopicEntity("eng_3", "english", null, "Reading Comprehension & Contextual Vocabulary", "Skimming, scanning, inference and summarization", "sl_gce_ol_sample_v1", 3),

            // History
            TopicEntity("hist_1", "history", null, "Ancient Hydraulic Civilization & Anuradhapura (பண்டைய நீர்ப்பாசன நாகரிகமும் அனுராதபுரமும்)", "Wewa irrigation network, Parakrama Samudra, ancient kingdoms", "sl_gce_ol_sample_v1", 1),
            TopicEntity("hist_2", "history", null, "Polonnaruwa Kingdom & Trade (பொலன்னறுவை இராச்சியமும் வர்த்தகமும்)", "Administration, cultural renaissance, South Indian interactions", "sl_gce_ol_sample_v1", 2),
            TopicEntity("hist_3", "history", null, "Colonial Era & 1948 Independence (குடியேற்றவாத ஆதிக்கமும் சுதந்திரமும்)", "Portuguese, Dutch, British administration and Colebrooke reforms", "sl_gce_ol_sample_v1", 3),

            // Tamil
            TopicEntity("tam_1", "tamil", null, "இலக்கணம்: புணர்ச்சி விதிகள் (Punarcci Rules)", "உடம்படுமெய், உயிர்ஈறு மெய்முதல், மெய்ஈறு உயிர்முதல் புணர்ச்சிகள்", "sl_gce_ol_sample_v1", 1),
            TopicEntity("tam_2", "tamil", null, "செய்யுள் நயமும் இலக்கியப் பகுப்பாய்வும் (Poetry & Literature)", "திருக்குறள், கம்பராமாயணம், நன்னெறி பாடல்கள்", "sl_gce_ol_sample_v1", 2),
            TopicEntity("tam_3", "tamil", null, "உரைநடை வாசிப்பும் கட்டுரை ஆக்கமும் (Essay & Comprehension)", "கருத்தாடல் கட்டுரை, விவாதக் கட்டுரை", "sl_gce_ol_sample_v1", 3),

            // ICT
            TopicEntity("ict_1", "ict", null, "Number Systems & Binary Logic (எண் முறைகளும் துவித தர்க்கமும்)", "Decimal, binary, hexadecimal, truth tables, logic gates (AND, OR, NOT)", "sl_gce_ol_sample_v1", 1),
            TopicEntity("ict_2", "ict", null, "Algorithms & Flowcharts (படிமுறைத்தீர்வுகளும் பாய்ச்சற்போக்குக் குறிப்படங்களும்)", "Sequence, selection, iteration, pseudo-code", "sl_gce_ol_sample_v1", 2)
        )
    }

    fun getInitialQuestions(): List<QuestionEntity> {
        return listOf(
            QuestionEntity(
                id = "q_math_1",
                subjectId = "math",
                topicId = "math_1",
                prompt = "What is the value of 2^3 × 2^4?",
                type = "MULTIPLE_CHOICE",
                optionsJson = "[\"2^7 (128)\", \"2^12 (4096)\", \"4^7\", \"2^1\"]",
                answerText = "2^7 (128)",
                explanation = "Using index law a^m × a^n = a^(m+n): 2^(3+4) = 2^7 = 128.",
                difficulty = "EASY",
                language = "ENGLISH",
                sourceType = "SAMPLE_PACK",
                sourceAttribution = "Sample GCE O/L Mathematics Pack"
            ),
            QuestionEntity(
                id = "q_math_2",
                subjectId = "math",
                topicId = "math_2",
                prompt = "இருபடிச் சமன்பாடு x^2 - 5x + 6 = 0 இன் மூலங்கள் (roots) யாவை?",
                type = "MULTIPLE_CHOICE",
                optionsJson = "[\"x = 2 அல்லது x = 3\", \"x = -2 அல்லது x = -3\", \"x = 1 அல்லது x = 6\", \"x = -1 அல்லது x = 6\"]",
                answerText = "x = 2 அல்லது x = 3",
                explanation = "காரணிப்படுத்த: (x - 2)(x - 3) = 0. ஆகவே x = 2 அல்லது x = 3 ஆகும்.",
                difficulty = "MEDIUM",
                language = "TAMIL",
                sourceType = "SAMPLE_PACK",
                sourceAttribution = "Sample GCE O/L Mathematics Pack (Tamil)"
            ),
            QuestionEntity(
                id = "q_sci_1",
                subjectId = "science",
                topicId = "sci_3",
                prompt = "According to Newton's Second Law of Motion, what is the formula connecting Force (F), Mass (m), and Acceleration (a)?",
                type = "MULTIPLE_CHOICE",
                optionsJson = "[\"F = m × a\", \"F = m / a\", \"F = a / m\", \"F = m + a\"]",
                answerText = "F = m × a",
                explanation = "Force equals mass multiplied by acceleration (F = ma). SI unit is Newton (N) or kg·m/s².",
                difficulty = "EASY",
                language = "ENGLISH",
                sourceType = "SAMPLE_PACK",
                sourceAttribution = "Sample GCE O/L Science Pack"
            ),
            QuestionEntity(
                id = "q_sci_2",
                subjectId = "science",
                topicId = "sci_1",
                prompt = "ஒளித்தொகுப்பின் (Photosynthesis) போது உருவாகும் பிரதான விளைபொருள் மற்றும் வெளியேற்றப்படும் வாயு யாவை?",
                type = "MULTIPLE_CHOICE",
                optionsJson = "[\"குளுக்கோசு & ஒட்சிசன்\", \"சுண்ணம்பு & காபனீரொட்சைடு\", \"ஸ்டார்ச் & நைதரசன்\", \"புரதம் & ஹைட்ரஜன்\"]",
                answerText = "குளுக்கோசு & ஒட்சிசன்",
                explanation = "6CO2 + 6H2O + சூரிய ஒளி -> C6H12O6 (குளுக்கோசு) + 6O2 (ஒட்சிசன் வாயு).",
                difficulty = "EASY",
                language = "TAMIL",
                sourceType = "SAMPLE_PACK",
                sourceAttribution = "Sample GCE O/L Science Pack (Tamil)"
            ),
            QuestionEntity(
                id = "q_ict_1",
                subjectId = "ict",
                topicId = "ict_1",
                prompt = "What is the binary representation of the decimal number 13?",
                type = "MULTIPLE_CHOICE",
                optionsJson = "[\"1101\", \"1011\", \"1110\", \"1001\"]",
                answerText = "1101",
                explanation = "13 in binary: 8 + 4 + 0 + 1 = 1101_2.",
                difficulty = "MEDIUM",
                language = "ENGLISH",
                sourceType = "SAMPLE_PACK",
                sourceAttribution = "Sample GCE O/L ICT Pack"
            ),
            QuestionEntity(
                id = "q_hist_1",
                subjectId = "history",
                topicId = "hist_1",
                prompt = "இலங்கையின் பண்டைய நீர்ப்பாசன வரலாற்றில் 'மழைநீரில் ஒரு துளியேனும் கடலுக்குப் பயனற்றுச் செல்லலாகாது' என்று கூறிய மன்னர் யார்?",
                type = "MULTIPLE_CHOICE",
                optionsJson = "[\"மகா பராக்கிரமபாகு\", \"துட்டகைமுணு\", \"தாதுசேனன்\", \"விஜயபாகு\"]",
                answerText = "மகா பராக்கிரமபாகு",
                explanation = "மகா பராக்கிரமபாகு மன்னர் (Polonnaruwa காலம்) பராக்கிரம சமுத்திரம் உள்ளிட்ட பல பாரிய குளங்களை அமைத்த பெருமைக்குரியவர்.",
                difficulty = "EASY",
                language = "TAMIL",
                sourceType = "SAMPLE_PACK",
                sourceAttribution = "Sample GCE O/L History Pack (Tamil)"
            )
        )
    }

    fun getInitialFlashcards(): List<FlashcardEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            FlashcardEntity(
                id = "fc_1",
                subjectId = "math",
                topicId = "math_4",
                front = "Trigonometric Ratios in a Right-Angled Triangle (SOH CAH TOA)",
                back = "Sin θ = Opposite / Hypotenuse\nCos θ = Adjacent / Hypotenuse\nTan θ = Opposite / Adjacent",
                language = "ENGLISH",
                intervalDays = 1,
                dueAt = now
            ),
            FlashcardEntity(
                id = "fc_2",
                subjectId = "science",
                topicId = "sci_4",
                front = "ஓமின் விதி (Ohm's Law) சமன்பாடு என்ன?",
                back = "V = I × R\n(மின்னழுத்த வேறுபாடு = மின்னோட்டம் × மின்தடை)",
                language = "TAMIL",
                intervalDays = 1,
                dueAt = now
            ),
            FlashcardEntity(
                id = "fc_3",
                subjectId = "ict",
                topicId = "ict_1",
                front = "What is the output of an AND gate when inputs are A=1 and B=0?",
                back = "Output is 0 (False).\nAn AND gate only produces 1 when ALL inputs are 1.",
                language = "ENGLISH",
                intervalDays = 1,
                dueAt = now
            )
        )
    }
}
