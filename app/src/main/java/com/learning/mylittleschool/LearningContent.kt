package com.learning.mylittleschool

data class LearningLesson(
    val id: String,
    val classId: String,
    val subject: String,
    val title: String,
    val english: String,
    val hindi: String,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

private val starterLessons = listOf(
    LearningLesson("pn_shapes", "pre_nursery", "Mathematics", "Shapes Around Us", "A circle is round. A square has four equal sides.", "वृत्त गोल होता है। वर्ग की चार बराबर भुजाएँ होती हैं।", "Which shape is round?", listOf("Circle", "Square", "Triangle"), 0, "A circle is round and has no corners."),
    LearningLesson("n_a", "nursery", "English", "Meet the Letter A", "A is the first letter of the English alphabet. A is for apple.", "A अंग्रेज़ी वर्णमाला का पहला अक्षर है। A for Apple.", "Which word begins with A?", listOf("Ball", "Apple", "Cat"), 1, "Apple begins with the letter A."),
    LearningLesson("lkg_count", "lkg", "Mathematics", "Counting 1 to 5", "Count: one, two, three, four, five.", "गिनती करें: एक, दो, तीन, चार, पाँच।", "What comes after 3?", listOf("2", "4", "5"), 1, "Four comes after three."),
    LearningLesson("ukg_nouns", "ukg", "English", "Naming Words", "A naming word tells us the name of a person, place, animal or thing.", "Naming word किसी व्यक्ति, स्थान, जानवर या वस्तु का नाम बताता है।", "Which one is a naming word?", listOf("Run", "Happy", "School"), 2, "School is the name of a place."),
    LearningLesson("c1_add", "class_1", "Mathematics", "Addition within 10", "Addition means putting groups together. 2 + 3 = 5.", "जोड़ने का अर्थ है समूहों को एक साथ मिलाना। 2 + 3 = 5।", "What is 4 + 2?", listOf("5", "6", "7"), 1, "4 objects and 2 more make 6."),
    LearningLesson("c2_noun", "class_2", "English", "Nouns", "A noun names a person, place, animal or thing.", "Noun किसी व्यक्ति, स्थान, जानवर या वस्तु का नाम होता है।", "Which word is a noun?", listOf("Quickly", "Teacher", "Jump"), 1, "Teacher names a person."),
    LearningLesson("c3_plant", "class_3", "EVS / Science", "Parts of a Plant", "Roots absorb water. The stem supports the plant. Leaves make food.", "जड़ें पानी सोखती हैं। तना पौधे को सहारा देता है। पत्तियाँ भोजन बनाती हैं।", "Which part absorbs water?", listOf("Flower", "Root", "Leaf"), 1, "Roots absorb water from the soil."),
    LearningLesson("c4_water", "class_4", "EVS / Science", "Water Cycle", "The Sun heats water. It evaporates, forms clouds and returns as rain.", "सूरज पानी को गर्म करता है। पानी भाप बनता है, बादल बनते हैं और वर्षा होती है।", "What changes liquid water into vapour?", listOf("Freezing", "Evaporation", "Melting"), 1, "Evaporation changes liquid water into water vapour."),
    LearningLesson("c5_fraction", "class_5", "Mathematics", "Understanding Fractions", "A fraction represents equal parts of a whole. In 1/2, one of two equal parts is taken.", "भिन्न किसी पूरी वस्तु के बराबर भागों को दर्शाती है। 1/2 का अर्थ दो बराबर भागों में से एक भाग है।", "What does the denominator show?", listOf("Number of equal parts", "Chosen parts", "The whole number"), 0, "The denominator tells how many equal parts make the whole.")
)

fun lessonsFor(classId: String, subject: String): List<LearningLesson> {
    val curated = starterLessons.firstOrNull { it.classId == classId && it.subject == subject }
    if (curated != null) return listOf(curated)
    return listOf(
        LearningLesson(
            id = "${classId}_${subject.lowercase().replace(" ", "_").replace("/", "_")}",
            classId = classId,
            subject = subject,
            title = "Getting Started: $subject",
            english = "Explore the basics of $subject with a short guided learning activity.",
            hindi = "$subject की बुनियादी बातें एक छोटी सीखने की गतिविधि से समझें।",
            question = "Are you ready to explore $subject?",
            options = listOf("Yes, let's learn!", "Not now", "I don't know"),
            correctIndex = 0,
            explanation = "Great! Learning starts with curiosity."
        )
    )
}
