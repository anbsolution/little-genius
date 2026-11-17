package com.learning.mylittleschool

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ActivityBlue = Color(0xFF2457D6)
private val ActivityInk = Color(0xFF17233C)
private val ActivitySoftBlue = Color(0xFFEAF1FF)

private data class PracticeChallenge(
    val id: String,
    val title: String,
    val subtitle: String,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val emoji: String
)

private data class PracticalGuide(
    val id: String,
    val title: String,
    val goal: String,
    val materials: List<String>,
    val steps: List<String>,
    val safety: String,
    val emoji: String
)

private fun isPreSchoolClass(classId: String) = classId in setOf("pre_nursery", "nursery")

private fun isEarlyClass(classId: String) = classId in setOf(
    "pre_nursery", "nursery", "lkg", "ukg"
)

private fun isLowerPrimary(classId: String) = classId in setOf("class_1", "class_2")

private fun gamesFor(classId: String): List<PracticeChallenge> = when {
    isPreSchoolClass(classId) -> listOf(
        PracticeChallenge(
            "game_animal_sound_$classId",
            "Animal Sound Match / जानवर की आवाज़",
            "Match an animal to its sound.",
            "Which animal says “meow”? / “म्याऊँ” कौन करता है?",
            listOf("Cat / बिल्ली", "Cow / गाय", "Duck / बत्तख"),
            0,
            "A cat says meow. / बिल्ली म्याऊँ करती है।",
            "🐱"
        ),
        PracticeChallenge(
            "game_count_two_$classId",
            "Count the Apples / सेब गिनो",
            "Count the pictures.",
            "🍎 🍎 How many apples? / कितने सेब हैं?",
            listOf("1 / एक", "2 / दो", "3 / तीन"),
            1,
            "There are two apples. / यहाँ दो सेब हैं।",
            "🍎"
        ),
        PracticeChallenge(
            "game_big_animal_$classId",
            "Big or Small? / बड़ा या छोटा?",
            "Compare two animals.",
            "Which animal is bigger? / कौन-सा जानवर बड़ा है?",
            listOf("Elephant / हाथी", "Ant / चींटी", "Both same / दोनों समान"),
            0,
            "An elephant is much bigger than an ant. / हाथी चींटी से बहुत बड़ा होता है।",
            "🐘"
        )
    )
    isEarlyClass(classId) -> listOf(
        PracticeChallenge(
            "game_shape_safari_$classId",
            "Shape Safari / आकृति सफारी",
            "Find the shape with three sides.",
            "Which shape has 3 sides? / किस आकृति की 3 भुजाएँ होती हैं?",
            listOf("Circle / वृत्त", "Triangle / त्रिभुज", "Square / वर्ग"),
            1,
            "A triangle has 3 sides. / त्रिभुज की 3 भुजाएँ होती हैं।",
            "🔺"
        ),
        PracticeChallenge(
            "game_number_hop_$classId",
            "Number Hop / संख्या छलाँग",
            "Follow the number pattern.",
            "2, 4, 6, __. Which number comes next? / आगे कौन-सी संख्या आएगी?",
            listOf("7", "8", "10"),
            1,
            "We add 2 each time: 2, 4, 6, 8. / हर बार 2 जोड़ते हैं: 2, 4, 6, 8।",
            "🐸"
        ),
        PracticeChallenge(
            "game_color_mixer_$classId",
            "Colour Mixer / रंग मिलाओ",
            "Think about mixing paint colours.",
            "Red and yellow paint make which colour? / लाल और पीला रंग मिलाने से कौन-सा रंग बनता है?",
            listOf("Green / हरा", "Orange / नारंगी", "Purple / बैंगनी"),
            1,
            "Red and yellow make orange. / लाल और पीला मिलकर नारंगी बनाते हैं।",
            "🎨"
        )
    )
    isLowerPrimary(classId) -> listOf(
        PracticeChallenge(
            "game_word_builder_$classId",
            "Word Builder / शब्द बनाओ",
            "Choose the opposite word.",
            "What is the opposite of fast? / Fast का उल्टा शब्द क्या है?",
            listOf("Slow / धीमा", "Bright / चमकीला", "Near / पास"),
            0,
            "Slow is the opposite of fast. / Slow, fast का उल्टा शब्द है।",
            "🔤"
        ),
        PracticeChallenge(
            "game_number_train_$classId",
            "Number Train / संख्या रेल",
            "Keep the number train moving.",
            "5, 10, 15, __. What comes next? / आगे कौन-सी संख्या आएगी?",
            listOf("18", "20", "25"),
            1,
            "The pattern adds 5 each time, so 20 comes next. / हर बार 5 जोड़ते हैं, इसलिए अगली संख्या 20 है।",
            "🚂"
        ),
        PracticeChallenge(
            "game_plant_match_$classId",
            "Plant Match / पौधे का मिलान",
            "Match a plant part to its job.",
            "Which plant part takes in water from the soil? / पौधे का कौन-सा भाग मिट्टी से पानी लेता है?",
            listOf("Root / जड़", "Flower / फूल", "Fruit / फल"),
            0,
            "Roots take in water from the soil. / जड़ें मिट्टी से पानी लेती हैं।",
            "🌱"
        )
    )
    else -> listOf(
        PracticeChallenge(
            "game_fraction_match_$classId",
            "Fraction Match / भिन्न मिलान",
            "Find an equivalent fraction.",
            "Which fraction has the same value as 1/2? / 1/2 के बराबर कौन-सी भिन्न है?",
            listOf("2/4", "1/3", "3/5"),
            0,
            "1/2 and 2/4 represent the same part of a whole. / 1/2 और 2/4 पूरे के बराबर भाग दिखाते हैं।",
            "🍕"
        ),
        PracticeChallenge(
            "game_science_sprint_$classId",
            "Science Sprint / विज्ञान दौड़",
            "Choose the correct science word.",
            "What is it called when liquid water changes into water vapour? / तरल पानी के जलवाष्प बनने की प्रक्रिया क्या कहलाती है?",
            listOf("Freezing / जमना", "Evaporation / वाष्पीकरण", "Melting / पिघलना"),
            1,
            "Evaporation changes liquid water into water vapour. / वाष्पीकरण में तरल पानी जलवाष्प बनता है।",
            "💧"
        ),
        PracticeChallenge(
            "game_multiply_maze_$classId",
            "Multiplication Maze / गुणा भूलभुलैया",
            "Find your way through the multiplication facts.",
            "What is 7 × 8? / 7 × 8 कितना होता है?",
            listOf("54", "56", "64"),
            1,
            "7 groups of 8 make 56. / 8 के 7 समूह मिलकर 56 होते हैं।",
            "🧩"
        )
    )
}

private fun puzzlesFor(classId: String): List<PracticeChallenge> = when {
    isPreSchoolClass(classId) -> listOf(
        PracticeChallenge(
            "puzzle_color_pattern_$classId",
            "Colour Pattern / रंग का क्रम",
            "See which colour repeats next.",
            "🔴 🔵 🔴 🔵 __. What comes next? / आगे कौन-सा रंग आएगा?",
            listOf("🔴", "🔵", "🟡"),
            0,
            "Red and blue take turns, so red comes next. / लाल और नीला बारी-बारी आते हैं, इसलिए अगला लाल है।",
            "🎨"
        ),
        PracticeChallenge(
            "puzzle_same_shape_$classId",
            "Find the Match / समान आकृति खोजो",
            "Choose the same shape.",
            "Which one matches this circle? 🔵 / इस गोले जैसी आकृति कौन-सी है? 🔵",
            listOf("🔺", "🔵", "🟥"),
            1,
            "The blue circle is the same shape. / नीला गोला वही आकृति है।",
            "🔍"
        ),
        PracticeChallenge(
            "puzzle_big_small_$classId",
            "Big and Small / बड़ा और छोटा",
            "Choose the smaller object.",
            "Which is smaller? / इनमें छोटा कौन है?",
            listOf("A bus / बस", "A toy car / खिलौना कार", "Same size / एक ही आकार"),
            1,
            "A toy car is smaller than a bus. / खिलौना कार बस से छोटी होती है।",
            "🧸"
        )
    )
    isEarlyClass(classId) -> listOf(
        PracticeChallenge(
            "puzzle_pattern_$classId",
            "Pattern Puzzle / क्रम पहेली",
            "Spot what repeats.",
            "⭐ 🔵 ⭐ 🔵 __. What comes next? / आगे क्या आएगा?",
            listOf("⭐", "🔵", "🔺"),
            0,
            "The star and circle take turns, so the star comes next. / तारा और गोला बारी-बारी आते हैं, इसलिए अगला तारा है।",
            "🧩"
        ),
        PracticeChallenge(
            "puzzle_odd_one_$classId",
            "Odd One Out / अलग चीज़ पहचानो",
            "Find the one that does not belong.",
            "Cat, dog, chair: which one is not an animal? / बिल्ली, कुत्ता, कुर्सी: इनमें जानवर कौन नहीं है?",
            listOf("Cat / बिल्ली", "Dog / कुत्ता", "Chair / कुर्सी"),
            2,
            "A chair is furniture, not an animal. / कुर्सी फर्नीचर है, जानवर नहीं।",
            "🐾"
        ),
        PracticeChallenge(
            "puzzle_fruit_count_$classId",
            "Fruit Basket / फलों की टोकरी",
            "Solve a little take-away puzzle.",
            "There are 4 mangoes. 1 is taken away. How many are left? / 4 आम में से 1 ले लिया। कितने बचे?",
            listOf("2", "3", "5"),
            1,
            "4 − 1 = 3 mangoes. / 4 − 1 = 3 आम।",
            "🥭"
        )
    )
    isLowerPrimary(classId) -> listOf(
        PracticeChallenge(
            "puzzle_missing_number_$classId",
            "Missing Number / लुप्त संख्या",
            "Fill the empty place.",
            "11, 12, __, 14. Which number is missing? / कौन-सी संख्या छूटी है?",
            listOf("13", "15", "10"),
            0,
            "The numbers go up by one: 11, 12, 13, 14. / संख्याएँ एक-एक बढ़ती हैं।",
            "🔢"
        ),
        PracticeChallenge(
            "puzzle_odd_number_$classId",
            "Odd Number Out / अलग संख्या",
            "Find the number that breaks the pattern.",
            "2, 4, 6, 9: which number is odd? / 2, 4, 6, 9 में कौन-सी संख्या विषम है?",
            listOf("2", "6", "9"),
            2,
            "9 is odd; 2, 4 and 6 are even. / 9 विषम है; 2, 4 और 6 सम हैं।",
            "🕵️"
        ),
        PracticeChallenge(
            "puzzle_shape_sides_$classId",
            "Shape Clue / आकृति संकेत",
            "Use the clue to find a shape.",
            "Which shape has 4 equal sides? / किस आकृति की 4 बराबर भुजाएँ होती हैं?",
            listOf("Triangle / त्रिभुज", "Square / वर्ग", "Circle / वृत्त"),
            1,
            "A square has 4 equal sides. / वर्ग की 4 बराबर भुजाएँ होती हैं।",
            "🔍"
        )
    )
    else -> listOf(
        PracticeChallenge(
            "puzzle_number_code_$classId",
            "Number Code / संख्या कोड",
            "Work out the rule.",
            "3, 6, 12, 24, __. What comes next? / आगे कौन-सी संख्या आएगी?",
            listOf("36", "42", "48"),
            2,
            "Each number doubles: 24 × 2 = 48. / हर संख्या दोगुनी होती है: 24 × 2 = 48।",
            "🔐"
        ),
        PracticeChallenge(
            "puzzle_square_numbers_$classId",
            "Square Number Sleuth / वर्ग संख्या जासूस",
            "Find the number that is not a square number.",
            "16, 25, 36, 40: which is not a square number? / इनमें कौन-सी पूर्ण वर्ग संख्या नहीं है?",
            listOf("16", "36", "40"),
            2,
            "16, 25 and 36 are 4², 5² and 6². 40 is not a square number. / 16, 25 और 36 क्रमशः 4², 5² और 6² हैं। 40 पूर्ण वर्ग नहीं है।",
            "🕵️"
        ),
        PracticeChallenge(
            "puzzle_bird_logic_$classId",
            "Logic Link / तर्क कड़ी",
            "Follow both clues.",
            "All birds are animals. A sparrow is a bird. What must be true? / सभी पक्षी जानवर हैं। गौरैया पक्षी है। सही निष्कर्ष क्या है?",
            listOf("A sparrow is an animal / गौरैया जानवर है", "All animals are birds / सभी जानवर पक्षी हैं", "A sparrow is a fish / गौरैया मछली है"),
            0,
            "If every bird is an animal and a sparrow is a bird, a sparrow is an animal. / यदि हर पक्षी जानवर है और गौरैया पक्षी है, तो गौरैया जानवर है।",
            "🧠"
        )
    )
}

private fun practicalGuidesFor(classId: String): List<PracticalGuide> = when {
    isEarlyClass(classId) -> listOf(
        PracticalGuide(
            "guide_colour_hunt_$classId",
            "Colour Hunt / रंग खोजो",
            "Notice and name colours around you. / अपने आसपास के रंग पहचानें।",
            listOf("Large, safe household objects / बड़ी और सुरक्षित घरेलू वस्तुएँ", "Paper and crayons / कागज़ और रंगीन पेंसिल"),
            listOf(
                "Choose one colour with an adult. / किसी बड़े के साथ एक रंग चुनें।",
                "Find three large objects of that colour in the room. / कमरे में उस रंग की तीन बड़ी वस्तुएँ खोजें।",
                "Draw one object and say its colour. / एक वस्तु बनाएँ और उसका रंग बोलें।"
            ),
            "Stay indoors with an adult. Do not pick up small, sharp or unknown objects. / किसी बड़े के साथ कमरे में रहें। छोटी, नुकीली या अनजान चीज़ें न उठाएँ।",
            "🎨"
        ),
        PracticalGuide(
            "guide_shape_sort_$classId",
            "Shape Sort / आकृतियाँ छाँटो",
            "Recognise circles, squares and triangles. / गोला, वर्ग और त्रिभुज पहचानें।",
            listOf("Large toy blocks or picture cards / बड़े खिलौना ब्लॉक या चित्र कार्ड"),
            listOf(
                "Ask an adult to place a few large blocks on a table. / किसी बड़े से मेज़ पर कुछ बड़े ब्लॉक रखने को कहें।",
                "Make one group of circles, one of squares and one of triangles. / गोलों, वर्गों और त्रिभुजों के अलग समूह बनाएँ।",
                "Count how many shapes are in each group. / हर समूह में आकृतियाँ गिनें।"
            ),
            "Use large pieces with an adult nearby; do not put pieces in your mouth. / बड़े टुकड़े लें और पास में किसी बड़े को रखें; टुकड़े मुँह में न डालें।",
            "🔺"
        ),
        PracticalGuide(
            "guide_sound_walk_$classId",
            "Indoor Sound Detective / आवाज़ पहचानो",
            "Listen carefully and describe sounds. / ध्यान से सुनकर आवाज़ों का वर्णन करें।",
            listOf("Paper and crayons / कागज़ और रंगीन पेंसिल"),
            listOf(
                "Sit quietly indoors with an adult. / किसी बड़े के साथ घर के अंदर शांति से बैठें।",
                "Listen for three sounds, such as a fan, birds or a clock. / तीन आवाज़ें सुनें, जैसे पंखा, पक्षी या घड़ी।",
                "Draw one thing that made a sound. / आवाज़ करने वाली एक चीज़ का चित्र बनाएँ।"
            ),
            "Stay inside and away from electrical sockets or appliances. / घर के अंदर रहें और बिजली के सॉकेट या उपकरणों से दूर रहें।",
            "👂"
        )
    )
    isLowerPrimary(classId) -> listOf(
        PracticalGuide(
            "guide_seed_watch_$classId",
            "Seed Watch / बीज की देखभाल",
            "Observe how a seed changes over several days. / कई दिनों में बीज में होने वाला बदलाव देखें।",
            listOf("A transparent cup, cotton and one large bean / पारदर्शी कप, रूई और एक बड़ा बीज", "A little water / थोड़ा पानी"),
            listOf(
                "With an adult, place damp cotton and the bean in the cup. / किसी बड़े के साथ कप में गीली रूई और बीज रखें।",
                "Keep it in a safe, bright place and check it each day. / इसे सुरक्षित, रोशनी वाली जगह रखें और रोज़ देखें।",
                "Draw what changes on day 1, day 3 and day 5. / पहले, तीसरे और पाँचवें दिन का बदलाव बनाएँ।"
            ),
            "An adult should help. Do not eat the seed or cotton; wash hands after the activity. / किसी बड़े की मदद लें। बीज या रूई न खाएँ; गतिविधि के बाद हाथ धोएँ।",
            "🌱"
        ),
        PracticalGuide(
            "guide_paper_bridge_$classId",
            "Paper Bridge / कागज़ का पुल",
            "See how folding can make paper stronger. / देखें कि मोड़ने से कागज़ मज़बूत कैसे बनता है।",
            listOf("Two sturdy books, paper and a few large light blocks / दो मज़बूत किताबें, कागज़ और कुछ बड़े हल्के ब्लॉक"),
            listOf(
                "Place the books a short distance apart on a clear table. / साफ़ मेज़ पर किताबें थोड़ी दूरी पर रखें।",
                "Lay a flat sheet of paper across them and add one light block. / सपाट कागज़ रखें और उस पर एक हल्का ब्लॉक रखें।",
                "Fold another sheet like a fan and compare how many blocks it holds. / दूसरे कागज़ को पंखे की तरह मोड़ें और तुलना करें।"
            ),
            "Use only light blocks, keep the table clear and ask an adult for help. / केवल हल्के ब्लॉक लें, मेज़ साफ़ रखें और किसी बड़े की मदद लें।",
            "🌉"
        ),
        PracticalGuide(
            "guide_shadow_tracker_$classId",
            "Shadow Tracker / छाया का निरीक्षण",
            "Notice how a shadow changes during the day. / दिन में छाया का बदलाव देखें।",
            listOf("Paper, crayons and a large toy / कागज़, रंगीन पेंसिल और बड़ा खिलौना"),
            listOf(
                "With an adult, place the toy near a sunny window. / किसी बड़े के साथ खिलौने को धूप वाली खिड़की के पास रखें।",
                "Draw the shadow in the morning and again later. / सुबह और बाद में छाया का चित्र बनाएँ।",
                "Compare whether the shadow moved or changed size. / देखें कि छाया खिसकी या उसका आकार बदला।"
            ),
            "Never look directly at the Sun. Stay indoors and use crayons, not sharp tools. / सूरज को सीधे कभी न देखें। घर के अंदर रहें और नुकीले औज़ारों की जगह रंगीन पेंसिल लें।",
            "☀️"
        )
    )
    else -> listOf(
        PracticalGuide(
            "guide_absorption_test_$classId",
            "Absorption Test / पानी सोखने की जाँच",
            "Compare which safe material absorbs more water. / तुलना करें कि कौन-सी सुरक्षित चीज़ ज़्यादा पानी सोखती है।",
            listOf("A tray, a little water, paper towel and a clean cloth / ट्रे, थोड़ा पानी, पेपर टॉवल और साफ़ कपड़ा"),
            listOf(
                "Place the tray on a flat surface and add one small spoon of water. / समतल जगह पर ट्रे रखें और एक छोटा चम्मच पानी डालें।",
                "Touch the spill with the paper towel, then repeat with the cloth. / पानी को पहले पेपर टॉवल, फिर कपड़े से सोखें।",
                "Record which material absorbed the water faster. / लिखें कि किस चीज़ ने पानी जल्दी सोखा।"
            ),
            "Ask an adult to help, wipe spills right away and never drink the test water. / किसी बड़े की मदद लें, गिरा पानी तुरंत पोंछें और जाँच का पानी न पिएँ।",
            "💧"
        ),
        PracticalGuide(
            "guide_leaf_compare_$classId",
            "Leaf Shape Study / पत्तियों का अध्ययन",
            "Compare shapes and patterns in fallen leaves. / गिरी हुई पत्तियों के आकार और रेखाओं की तुलना करें।",
            listOf("Two clean fallen leaves, paper and crayons / दो साफ़ गिरी हुई पत्तियाँ, कागज़ और रंगीन पेंसिल"),
            listOf(
                "With an adult, collect two fallen leaves from a clean area. / किसी बड़े के साथ साफ़ जगह से दो गिरी हुई पत्तियाँ लें।",
                "Compare their shapes, sizes and visible veins. / उनके आकार, लंबाई और दिखने वाली नसों की तुलना करें।",
                "Draw each leaf and write one difference. / दोनों पत्तियों का चित्र बनाएँ और एक अंतर लिखें।"
            ),
            "Do not eat leaves or touch unknown plants. Wash hands afterwards. / पत्तियाँ न खाएँ और अनजान पौधों को न छुएँ। बाद में हाथ धोएँ।",
            "🍃"
        ),
        PracticalGuide(
            "guide_weather_graph_$classId",
            "Weather Graph / मौसम का ग्राफ़",
            "Collect simple observations and display them clearly. / सरल निरीक्षण इकट्ठा करके उन्हें साफ़ तरीके से दिखाएँ।",
            listOf("A notebook, pencil and crayons / कॉपी, पेंसिल और रंगीन पेंसिल"),
            listOf(
                "For five days, observe whether the sky is sunny, cloudy or rainy. / पाँच दिन तक देखें कि आसमान धूप वाला, बादल वाला या बरसाती है।",
                "Make one tally mark for each day's weather. / हर दिन के मौसम के लिए एक निशान बनाएँ।",
                "Draw a bar for each type and compare which happened most. / हर मौसम के लिए एक बार बनाएँ और तुलना करें कि कौन-सा सबसे ज़्यादा था।"
            ),
            "Observe from a safe place with an adult. Do not go outside during storms. / किसी बड़े के साथ सुरक्षित जगह से देखें। तूफ़ान के समय बाहर न जाएँ।",
            "📊"
        )
    )
}

@Composable
fun PracticeAndPlayScreen(
    schoolClass: SchoolClass,
    padding: PaddingValues,
    store: ProgressStore,
    onOpenSubject: (String) -> Unit
) {
    var selectedModule by remember { mutableStateOf<String?>(null) }
    var selectedChallenge by remember { mutableStateOf<PracticeChallenge?>(null) }
    var selectedGuide by remember { mutableStateOf<PracticalGuide?>(null) }

    when {
        selectedChallenge != null -> PracticeChallengeDetailScreen(
            challenge = selectedChallenge!!,
            padding = padding,
            store = store,
            onBack = { selectedChallenge = null }
        )
        selectedGuide != null -> PracticalGuideDetailScreen(
            guide = selectedGuide!!,
            padding = padding,
            store = store,
            onBack = { selectedGuide = null }
        )
        selectedModule == "Quiz & Subject Practice" -> Column(
            Modifier.fillMaxSize().padding(padding)
        ) {
            TextButton(onClick = { selectedModule = null }) { Text("← Back to Practice & Play") }
            Box(Modifier.weight(1f)) {
                PracticeScreen(schoolClass, PaddingValues(0.dp), onOpenSubject)
            }
        }
        selectedModule == "Learning Games" -> ChallengeListScreen(
            title = "Learning Games",
            description = "Short, class-level games. Choose an answer and learn from the explanation.",
            emoji = "🎮",
            challenges = gamesFor(schoolClass.id),
            padding = padding,
            store = store,
            onBack = { selectedModule = null },
            onSelect = { selectedChallenge = it }
        )
        selectedModule == "Puzzles" -> ChallengeListScreen(
            title = "Puzzles",
            description = "Try patterns, number puzzles and logic clues.",
            emoji = "🧩",
            challenges = puzzlesFor(schoolClass.id),
            padding = padding,
            store = store,
            onBack = { selectedModule = null },
            onSelect = { selectedChallenge = it }
        )
        selectedModule == "Practical Guides" -> PracticalGuideListScreen(
            schoolClass = schoolClass,
            padding = padding,
            store = store,
            onBack = { selectedModule = null },
            onSelect = { selectedGuide = it }
        )
        else -> PracticeModuleListScreen(
            schoolClass = schoolClass,
            padding = padding,
            onSelect = { selectedModule = it },
            quizSubjectCount = listOf("English", "Mathematics", "Hindi", "EVS / Science", "Computer", "Social Studies")
                .count { subject ->
                    lessonsFor(schoolClass.id, subject).any { !it.title.startsWith("Getting Started:") }
                }
        )
    }
}

@Composable
private fun PracticeModuleListScreen(
    schoolClass: SchoolClass,
    padding: PaddingValues,
    quizSubjectCount: Int,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PremiumHeroCard(
                eyebrow = "PRACTICE, PLAY & EXPLORE",
                title = "Learn by doing!",
                subtitle = "Choose a quiz, game, puzzle or hands-on guide.",
                decoration = "✨"
            ) {
                AccentPill("${schoolClass.title} · 4 WAYS TO LEARN", Color.White.copy(alpha = 0.92f))
            }
        }
        item {
            PracticeModuleCard(
                title = "Quiz & Subject Practice",
                detail = "$quizSubjectCount subject${if (quizSubjectCount == 1) "" else "s"} · answer lessons and check explanations",
                emoji = "✅",
                onClick = { onSelect("Quiz & Subject Practice") }
            )
        }
        item {
            PracticeModuleCard(
                title = "Learning Games",
                detail = "3 quick games for ${schoolClass.title}",
                emoji = "🎮",
                onClick = { onSelect("Learning Games") }
            )
        }
        item {
            PracticeModuleCard(
                title = "Puzzles",
                detail = "Patterns, number puzzles and logic clues",
                emoji = "🧩",
                onClick = { onSelect("Puzzles") }
            )
        }
        item {
            PracticeModuleCard(
                title = "Practical Guides",
                detail = "3 safe, step-by-step activities to try at home or school",
                emoji = "🔬",
                onClick = { onSelect("Practical Guides") }
            )
        }
        item {
            Text(
                "Games, puzzles and guides are built into the app, so they work offline. Practical activities include adult-supervision and safety notes.",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PracticeModuleCard(title: String, detail: String, emoji: String, onClick: () -> Unit) {
    val tint = when (title) {
        "Quiz & Subject Practice" -> LittleGeniusColors.SoftBlue
        "Learning Games" -> LittleGeniusColors.SoftMint
        "Puzzles" -> Color(0xFFF1EAFF)
        else -> LittleGeniusColors.SoftAmber
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).background(tint, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 25.sp) }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = ActivityInk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(detail, color = LittleGeniusColors.Muted, fontSize = 12.sp, lineHeight = 16.sp)
            }
            Text("›", color = ActivityBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChallengeListScreen(
    title: String,
    description: String,
    emoji: String,
    challenges: List<PracticeChallenge>,
    padding: PaddingValues,
    store: ProgressStore,
    onBack: () -> Unit,
    onSelect: (PracticeChallenge) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Back to Practice & Play") }
            PageHeading("CHOOSE AN ACTIVITY", "$emoji  $title", description)
        }
        items(challenges, key = { it.id }) { challenge ->
            val solved = store.isActivityCompleted(challenge.id)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(challenge) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (solved) Color(0xFFEAF8EF) else Color.White)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(challenge.emoji, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(challenge.title, color = ActivityInk, fontWeight = FontWeight.Bold)
                        Text(challenge.subtitle, color = LittleGeniusColors.Muted, fontSize = 12.sp)
                    }
                    Text(if (solved) "Done ✓" else "Play", color = if (solved) Color(0xFF16803C) else ActivityBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PracticeChallengeDetailScreen(
    challenge: PracticeChallenge,
    padding: PaddingValues,
    store: ProgressStore,
    onBack: () -> Unit
) {
    var selectedAnswer by remember(challenge.id) { mutableStateOf<Int?>(null) }
    var submittedAnswer by remember(challenge.id) { mutableStateOf<Boolean?>(null) }

    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back to activities") }
        Text(challenge.title, color = ActivityInk, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(challenge.subtitle, color = Color.Gray)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = ActivitySoftBlue)
        ) {
            Text(
                challenge.prompt,
                Modifier.fillMaxWidth().padding(18.dp),
                color = ActivityInk,
                fontSize = 19.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        challenge.options.forEachIndexed { index, option ->
            OutlinedButton(
                onClick = { if (submittedAnswer == null) selectedAnswer = index },
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selectedAnswer == index) ActivitySoftBlue else Color.Transparent,
                    contentColor = ActivityInk
                )
            ) {
                Text(option, Modifier.fillMaxWidth())
            }
        }
        Button(
            onClick = {
                val correct = selectedAnswer == challenge.correctIndex
                store.recordActivityAnswer(challenge.id, correct)
                submittedAnswer = correct
            },
            enabled = selectedAnswer != null && submittedAnswer == null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check answer / जवाब देखें")
        }
        submittedAnswer?.let { correct ->
            Text(
                if (correct) "Correct! Great work. ⭐ / सही जवाब! बहुत बढ़िया।"
                else "Not quite—try once more. / जवाब सही नहीं है—फिर कोशिश करें।",
                color = if (correct) Color(0xFF16803C) else Color(0xFFB45309),
                fontWeight = FontWeight.Bold
            )
            Text(challenge.explanation, color = ActivityInk)
            if (correct) {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Back to activities")
                }
            } else {
                OutlinedButton(
                    onClick = { selectedAnswer = null; submittedAnswer = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Try again / फिर कोशिश करें")
                }
            }
        }
    }
}

@Composable
private fun PracticalGuideListScreen(
    schoolClass: SchoolClass,
    padding: PaddingValues,
    store: ProgressStore,
    onBack: () -> Unit,
    onSelect: (PracticalGuide) -> Unit
) {
    val guides = remember(schoolClass.id) { practicalGuidesFor(schoolClass.id) }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text("← Back to Practice & Play") }
            PageHeading(
                "HANDS-ON LEARNING",
                "Practical Guides",
                "${schoolClass.title} · Simple activities with materials, steps and safety notes."
            )
        }
        items(guides, key = { it.id }) { guide ->
            val done = store.isPracticalGuideDone(guide.id)
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onSelect(guide) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (done) Color(0xFFEAF8EF) else Color.White)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(guide.emoji, fontSize = 29.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(guide.title, color = ActivityInk, fontWeight = FontWeight.Bold)
                        Text(guide.goal, color = LittleGeniusColors.Muted, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                    Text(if (done) "Done ✓" else "Guide", color = if (done) Color(0xFF16803C) else ActivityBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PracticalGuideDetailScreen(
    guide: PracticalGuide,
    padding: PaddingValues,
    store: ProgressStore,
    onBack: () -> Unit
) {
    var done by remember(guide.id) { mutableStateOf(store.isPracticalGuideDone(guide.id)) }
    Column(
        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back to guides") }
        Text("${guide.emoji}  ${guide.title}", color = ActivityInk, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(guide.goal, color = Color.Gray, fontSize = 15.sp)
        Text("What you need / सामग्री", color = ActivityInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        guide.materials.forEach { material -> Text("•  $material", color = ActivityInk) }
        Text("Steps / चरण", color = ActivityInk, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        guide.steps.forEachIndexed { index, step ->
            Card(
                shape = RoundedCornerShape(15.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("${index + 1}.", color = ActivityBlue, fontWeight = FontWeight.Bold)
                    Text("  $step", color = ActivityInk, modifier = Modifier.weight(1f))
                }
            }
        }
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4D9))
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Safety / सुरक्षा", color = ActivityInk, fontWeight = FontWeight.Bold)
                Text(guide.safety, color = ActivityInk, fontSize = 13.sp)
            }
        }
        Button(
            onClick = {
                done = !done
                store.setPracticalGuideDone(guide.id, done)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (done) "Completed ✓ · Mark not done" else "Mark activity complete")
        }
    }
}
