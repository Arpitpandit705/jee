package com.example.util

data class SpeedRecommendation(
    val speed: Float,
    val speedLabel: String,
    val category: String,
    val explanation: String,
    val minutesSavedOn60m: Int
)

object SpeedAdvisor {

    fun getRecommendation(lectureTitle: String, subject: String): SpeedRecommendation {
        val lowerTitle = lectureTitle.lowercase()
        val lowerSub = subject.lowercase()

        return when {
            // High conceptual complexity or derivations -> 1.25x
            lowerTitle.contains("derivation") ||
            lowerTitle.contains("quantum") ||
            lowerTitle.contains("mechanism") ||
            lowerTitle.contains("rotational") ||
            lowerTitle.contains("calculus") ||
            lowerTitle.contains("integration") ||
            lowerTitle.contains("thermodynamics") ||
            lowerTitle.contains("electrodynamics") -> {
                SpeedRecommendation(
                    speed = 1.25f,
                    speedLabel = "1.25x",
                    category = "Deep Conceptual / Derivation",
                    explanation = "Complex topic. 1.25x maintains maximum neural retention while saving ~12 minutes.",
                    minutesSavedOn60m = 12
                )
            }

            // Practice, DPP, revision, or numerical exercise -> 1.75x
            lowerTitle.contains("dpp") ||
            lowerTitle.contains("revision") ||
            lowerTitle.contains("question") ||
            lowerTitle.contains("exercise") ||
            lowerTitle.contains("problem") ||
            lowerTitle.contains("pyq") ||
            lowerTitle.contains("summary") -> {
                SpeedRecommendation(
                    speed = 1.75f,
                    speedLabel = "1.75x",
                    category = "Problem Solving / Rapid Review",
                    explanation = "Practice session. 1.75x sharpens active problem recognition and saves ~26 minutes.",
                    minutesSavedOn60m = 26
                )
            }

            // Standard syllabus lecture -> 1.50x
            else -> {
                SpeedRecommendation(
                    speed = 1.50f,
                    speedLabel = "1.50x",
                    category = "Standard Concept Delivery",
                    explanation = "Ideal balance: 1.50x cuts passive spacing out and saves 20 minutes on a 60m lecture.",
                    minutesSavedOn60m = 20
                )
            }
        }
    }
}
