package com.example.ardogs

data class DogBreedInfo(
    val breedName: String,
    val funFacts: List<String>,
    val origin: String,
    val temperament: String,
    val size: String
)

object DogBreedDatabase {
    private val breedInfoMap = mapOf(
        "Beagle" to DogBreedInfo(
            breedName = "Beagle",
            funFacts = listOf(
                "🐾 Beagles have been depicted in popular culture since Elizabethan times",
                "👃 They have 220 million scent receptors - perfect for tracking!",
                "🎵 Beagles are known for their unique howl called 'baying'",
                "⚡ Snoopy from Peanuts is the world's most famous Beagle!"
            ),
            origin = "England",
            temperament = "Friendly, Curious, Merry",
            size = "Small to Medium (13-15 inches)"
        ),

        "Chihuahua" to DogBreedInfo(
            breedName = "Chihuahua",
            funFacts = listOf(
                "🏆 World's smallest dog breed!",
                "🧠 They have the largest brain-to-body ratio of all dog breeds",
                "🌡️ Chihuahuas tend to seek warmth and often burrow under blankets",
                "👑 Named after the Mexican state of Chihuahua"
            ),
            origin = "Mexico",
            temperament = "Alert, Quick, Devoted",
            size = "Tiny (5-8 inches)"
        ),

        "Doberman" to DogBreedInfo(
            breedName = "Doberman Pinscher",
            funFacts = listOf(
                "🚔 Originally bred as guard dogs in the 1890s",
                "💪 5th smartest dog breed - highly trainable!",
                "⚡ Can run up to 32 mph (51 km/h)",
                "🎖️ Served as war dogs in WWII with distinction"
            ),
            origin = "Germany",
            temperament = "Loyal, Fearless, Alert",
            size = "Large (24-28 inches)"
        ),

        "French_bulldog" to DogBreedInfo(
            breedName = "French Bulldog",
            funFacts = listOf(
                "🇫🇷 Most popular breed in Paris and New York!",
                "🦇 Their distinctive 'bat ears' are a breed trademark",
                "😴 They snore loudly due to their flat faces",
                "🏊 Cannot swim well - their heavy front makes them sink!"
            ),
            origin = "France/England",
            temperament = "Playful, Adaptable, Smart",
            size = "Small (11-13 inches)"
        ),

        "German_shepherd" to DogBreedInfo(
            breedName = "German Shepherd",
            funFacts = listOf(
                "🚓 Top choice for police and military work worldwide",
                "🧠 3rd most intelligent dog breed!",
                "🎬 Rin Tin Tin was a famous German Shepherd movie star",
                "👃 Can detect substances as small as a few parts per trillion"
            ),
            origin = "Germany",
            temperament = "Confident, Courageous, Smart",
            size = "Large (22-26 inches)"
        ),

        "Golden_retriever" to DogBreedInfo(
            breedName = "Golden Retriever",
            funFacts = listOf(
                "🏅 4th most popular breed in the USA!",
                "🦆 Originally bred to retrieve waterfowl during hunting",
                "😊 Their smile is genuine - they're always happy!",
                "🌊 They LOVE water and are excellent swimmers"
            ),
            origin = "Scotland",
            temperament = "Friendly, Intelligent, Devoted",
            size = "Large (21-24 inches)"
        ),

        "Labrador_retriever" to DogBreedInfo(
            breedName = "Labrador Retriever",
            funFacts = listOf(
                "👑 Most popular breed in USA for 31 consecutive years!",
                "🦴 They have 'soft mouths' - can carry eggs without breaking them",
                "🌊 Webbed paws make them excellent swimmers",
                "🐕‍🦺 Most commonly used as guide dogs and service dogs"
            ),
            origin = "Canada (Newfoundland)",
            temperament = "Outgoing, Even-Tempered, Gentle",
            size = "Large (21-24 inches)"
        ),

        "Maltese_dog" to DogBreedInfo(
            breedName = "Maltese",
            funFacts = listOf(
                "👸 Ancient breed - beloved by royalty for over 2,800 years!",
                "🎀 Their silky white coat doesn't shed but grows like human hair",
                "💎 Often called 'living jewels' by ancient Greeks",
                "🏝️ Named after the Mediterranean island of Malta"
            ),
            origin = "Malta/Italy",
            temperament = "Gentle, Playful, Charming",
            size = "Tiny (7-9 inches)"
        ),

        "Pomeranian" to DogBreedInfo(
            breedName = "Pomeranian",
            funFacts = listOf(
                "👑 Queen Victoria's favorite breed - she had 35 Pomeranians!",
                "🦊 Descended from large sled-pulling Spitz-type dogs",
                "🎭 Extremely expressive face with fox-like features",
                "⚡ Despite tiny size, they think they're HUGE guard dogs!"
            ),
            origin = "Germany/Poland",
            temperament = "Inquisitive, Bold, Lively",
            size = "Tiny (6-7 inches)"
        ),

        "Pug" to DogBreedInfo(
            breedName = "Pug",
            funFacts = listOf(
                "🇨🇳 Ancient breed from China - over 2,000 years old!",
                "👑 Motto: 'Multum in Parvo' (a lot in a little)",
                "😤 Their flat face makes them snore adorably",
                "🎨 Featured in paintings by Goya and other famous artists"
            ),
            origin = "China",
            temperament = "Charming, Mischievous, Loving",
            size = "Small (10-13 inches)"
        ),

        "Rottweiler" to DogBreedInfo(
            breedName = "Rottweiler",
            funFacts = listOf(
                "🐄 Originally used to herd cattle and pull carts to market",
                "💪 One of the oldest herding breeds from Roman times",
                "❤️ Despite tough looks, they're gentle 'velcro dogs' with family",
                "🎖️ Excellent police, military, and therapy dogs"
            ),
            origin = "Germany",
            temperament = "Loyal, Loving, Confident Guardian",
            size = "Large (22-27 inches)"
        ),

        "Samoyed" to DogBreedInfo(
            breedName = "Samoyed",
            funFacts = listOf(
                "😊 Famous 'Sammy smile' prevents drooling in cold weather!",
                "❄️ Bred to herd reindeer in Siberia - loves cold weather",
                "☁️ Their fluffy white coat is hypoallergenic and can be spun into yarn",
                "🗣️ Very vocal breed - they 'talk' with unique sounds"
            ),
            origin = "Siberia",
            temperament = "Friendly, Gentle, Adaptable",
            size = "Medium to Large (19-24 inches)"
        ),

        "Shih-Tzu" to DogBreedInfo(
            breedName = "Shih Tzu",
            funFacts = listOf(
                "🦁 Name means 'Little Lion' in Mandarin Chinese",
                "🏯 Bred as royal palace dogs in Tibet and China",
                "🎀 Their hair can grow very long if not trimmed regularly",
                "💝 Bred solely to be companions - they excel at cuddling!"
            ),
            origin = "Tibet/China",
            temperament = "Affectionate, Playful, Outgoing",
            size = "Small (9-10 inches)"
        ),

        "Siberian_husky" to DogBreedInfo(
            breedName = "Siberian Husky",
            funFacts = listOf(
                "🛷 Can run 100+ miles per day pulling sleds in freezing temperatures!",
                "👁️ Often have stunning blue eyes or one blue, one brown (heterochromia)",
                "🎭 Very vocal - they 'talk', howl, and rarely bark",
                "🦸 Balto the Husky delivered life-saving medicine in Alaska, 1925"
            ),
            origin = "Siberia",
            temperament = "Outgoing, Mischievous, Loyal",
            size = "Medium (20-23 inches)"
        ),

        "Standard_poodle" to DogBreedInfo(
            breedName = "Standard Poodle",
            funFacts = listOf(
                "🧠 2nd most intelligent dog breed in the world!",
                "🦆 Originally bred as water retrievers in Germany",
                "✂️ Their fancy haircut was designed to protect joints in cold water",
                "🏊 Name comes from German 'Pudelhund' meaning 'splash dog'"
            ),
            origin = "Germany/France",
            temperament = "Intelligent, Active, Elegant",
            size = "Large (over 15 inches)"
        )
    )

    fun getBreedInfo(breedName: String): DogBreedInfo? {
        return breedInfoMap[breedName]
    }

    fun getRandomFunFact(breedName: String): String? {
        return breedInfoMap[breedName]?.funFacts?.random()
    }

    fun getAllFunFacts(breedName: String): List<String> {
        return breedInfoMap[breedName]?.funFacts ?: emptyList()
    }
}

