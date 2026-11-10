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
    LearningLesson("pn_shapes","pre_nursery","Mathematics","Shapes Around Us","A circle is round. A square has four equal sides.","वृत्त गोल होता है। वर्ग की चार बराबर भुजाएँ होती हैं।","Which shape is round?",listOf("Circle","Square","Triangle"),0,"A circle is round and has no corners."),
    LearningLesson("n_a","nursery","English","Meet the Letter A","A is the first letter of the English alphabet. A is for apple.","A अंग्रेज़ी वर्णमाला का पहला अक्षर है। A for Apple.","Which word begins with A?",listOf("Ball","Apple","Cat"),1,"Apple begins with the letter A."),
    LearningLesson("lkg_count","lkg","Mathematics","Counting 1 to 5","Count: one, two, three, four, five.","गिनती करें: एक, दो, तीन, चार, पाँच।","What comes after 3?",listOf("2","4","5"),1,"Four comes after three."),
    LearningLesson("ukg_words","ukg","English","Naming Words","A naming word tells us the name of a person, place, animal or thing.","Naming word किसी व्यक्ति, स्थान, जानवर या वस्तु का नाम बताता है।","Which one is a naming word?",listOf("Run","Happy","School"),2,"School is the name of a place."),
    LearningLesson("c1_add","class_1","Mathematics","Addition within 10","Addition means putting groups together. 2 + 3 = 5.","जोड़ने का अर्थ है समूहों को एक साथ मिलाना। 2 + 3 = 5।","What is 4 + 2?",listOf("5","6","7"),1,"4 objects and 2 more make 6."),
    LearningLesson("c2_nouns","class_2","English","Nouns","A noun names a person, place, animal or thing.","Noun किसी व्यक्ति, स्थान, जानवर या वस्तु का नाम होता है।","Which word is a noun?",listOf("Quickly","Teacher","Jump"),1,"Teacher names a person."),
    LearningLesson("c3_plant","class_3","EVS / Science","Parts of a Plant","Roots absorb water. The stem supports the plant. Leaves make food.","जड़ें पानी सोखती हैं। तना पौधे को सहारा देता है। पत्तियाँ भोजन बनाती हैं।","Which part absorbs water?",listOf("Flower","Root","Leaf"),1,"Roots absorb water from the soil."),
    LearningLesson("c4_water","class_4","EVS / Science","Water Cycle","The Sun heats water. It evaporates, forms clouds and returns as rain.","सूरज पानी को गर्म करता है। पानी भाप बनता है, बादल बनते हैं और वर्षा होती है।","What changes liquid water into vapour?",listOf("Freezing","Evaporation","Melting"),1,"Evaporation changes liquid water into water vapour."),
    LearningLesson("c5_fraction","class_5","Mathematics","Understanding Fractions","A fraction represents equal parts of a whole. In 1/2, one of two equal parts is taken.","भिन्न किसी पूरी वस्तु के बराबर भागों को दर्शाती है। 1/2 का अर्थ दो बराबर भागों में से एक भाग है।","What does the denominator show?",listOf("Number of equal parts","Chosen parts","The whole number"),0,"The denominator tells how many equal parts make the whole.")
)


private val phaseSixLessons = listOf(
    LearningLesson("pn_colors","pre_nursery","Mathematics","Colours Around Us","Red, blue and yellow are colours. Look for colours around you.","लाल, नीला और पीला रंग हैं। अपने आसपास रंग पहचानें।","Which one is a colour?",listOf("Blue","Table","Book"),0,"Blue is a colour."),
    LearningLesson("pn_big_small","pre_nursery","Mathematics","Big and Small","A ball can be big or small. We compare sizes.","गेंद बड़ी या छोटी हो सकती है। हम आकार की तुलना करते हैं।","Which is bigger?",listOf("An elephant","An ant","A seed"),0,"An elephant is bigger than an ant."),
    LearningLesson("n_letter_b","nursery","English","Meet the Letter B","B is a letter. B is for ball and bat.","B एक अक्षर है। B for Ball और Bat।","Which word begins with B?",listOf("Apple","Ball","Cat"),1,"Ball begins with B."),
    LearningLesson("n_count_10","nursery","Mathematics","Counting to 10","Numbers help us count objects from one to ten.","संख्याओं से हम वस्तुओं को एक से दस तक गिनते हैं।","What comes after 7?",listOf("6","8","10"),1,"Eight comes after seven."),
    LearningLesson("lkg_vowels","lkg","English","The Vowels","A, E, I, O and U are the five vowels in English.","A, E, I, O और U अंग्रेज़ी के पाँच स्वर हैं।","Which is a vowel?",listOf("B","E","T"),1,"E is a vowel."),
    LearningLesson("lkg_compare","lkg","Mathematics","Comparing Numbers","We can compare numbers to find which is greater or smaller.","हम संख्याओं की तुलना करके बड़ी और छोटी संख्या पहचान सकते हैं।","Which number is greater?",listOf("3","5","2"),1,"Five is greater than three and two."),
    LearningLesson("ukg_sentence","ukg","English","Make a Sentence","A sentence is a group of words that gives a complete idea.","वाक्य शब्दों का ऐसा समूह है जो पूरा अर्थ देता है।","Which is a sentence?",listOf("The red","Birds can fly.","Under the"),1,"Birds can fly is a complete sentence."),
    LearningLesson("ukg_subtract","ukg","Mathematics","Simple Subtraction","Subtraction means taking away. 5 minus 2 equals 3.","घटाने का अर्थ है कम करना। 5 में से 2 घटाने पर 3 बचते हैं।","What is 5 - 2?",listOf("2","3","4"),1,"Five take away two leaves three."),
    LearningLesson("c1_subtract","class_1","Mathematics","Subtraction within 10","Subtraction takes one number away from another. 7 - 3 = 4.","घटाव में एक संख्या में से दूसरी संख्या घटाते हैं। 7 - 3 = 4।","What is 8 - 5?",listOf("2","3","4"),1,"Eight take away five leaves three."),
    LearningLesson("c1_nouns","class_1","English","Naming People and Places","A noun names a person, place, animal or thing.","Noun किसी व्यक्ति, स्थान, जानवर या वस्तु का नाम बताता है।","Which word names a place?",listOf("Park","Sing","Blue"),0,"Park is the name of a place."),
    LearningLesson("c2_place_value","class_2","Mathematics","Tens and Ones","In 24, the digit 2 means two tens and 4 means four ones.","24 में 2 का अर्थ दो दहाई और 4 का अर्थ चार इकाई है।","How many tens are in 35?",listOf("3","5","35"),0,"There are three tens in 35."),
    LearningLesson("c2_punctuation","class_2","English","End Marks","A full stop ends a statement. A question mark ends a question.","पूर्ण विराम कथन के अंत में और प्रश्नवाचक चिह्न सवाल के अंत में आता है।","Which mark ends a question?",listOf(".","? ",","),1,"A question mark ends a question."),
    LearningLesson("c3_multiply","class_3","Mathematics","Introduction to Multiplication","Multiplication is repeated addition. 3 groups of 2 make 6.","गुणा बार-बार जोड़ने का तरीका है। 2 के 3 समूह मिलकर 6 होते हैं।","What is 3 × 2?",listOf("5","6","8"),1,"Three groups of two make six."),
    LearningLesson("c3_habitats","class_3","EVS / Science","Animal Habitats","A habitat is the natural home of a plant or animal.","आवास किसी पौधे या जानवर का प्राकृतिक घर होता है।","Where does a camel live naturally?",listOf("Desert","Ocean","Polar ice"),0,"Camels are adapted to desert habitats."),
    LearningLesson("c4_fractions","class_4","Mathematics","Equivalent Fractions","Equivalent fractions have the same value, such as 1/2 and 2/4.","समतुल्य भिन्नों का मान समान होता है, जैसे 1/2 और 2/4।","Which fraction equals 1/2?",listOf("2/4","1/3","3/4"),0,"Two quarters equal one half."),
    LearningLesson("c4_grammar","class_4","English","Pronouns","A pronoun is used in place of a noun, such as he, she or they.","Pronoun का प्रयोग Noun के स्थान पर होता है, जैसे he, she या they।","Which word is a pronoun?",listOf("Riya","They","School"),1,"They is a pronoun."),
    LearningLesson("c5_decimals","class_5","Mathematics","Understanding Decimals","Decimals show parts of a whole. 0.5 is equal to one half.","दशमलव किसी पूरी संख्या के भाग दिखाते हैं। 0.5 का मान आधा है।","What is 0.5 as a fraction?",listOf("1/2","1/4","1/5"),0,"0.5 is equal to 1/2."),
    LearningLesson("c5_solar","class_5","EVS / Science","Our Solar System","The Solar System has the Sun and the objects that orbit it.","सौरमंडल में सूर्य और उसके चारों ओर घूमने वाले खगोलीय पिंड शामिल हैं।","Which star is at the centre of our Solar System?",listOf("Sirius","The Sun","Polaris"),1,"The Sun is at the centre of our Solar System.")
)

fun lessonsFor(classId: String, subject: String): List<LearningLesson> {
    val matching = (starterLessons + phaseSixLessons).filter { it.classId == classId && it.subject == subject }
    if (matching.isNotEmpty()) return matching
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
