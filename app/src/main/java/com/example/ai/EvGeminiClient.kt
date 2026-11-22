package com.example.ai

import com.example.data.model.ElectricMotorEntity
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val prompt: String, val text: String) : AiUiState
    data class Success(val prompt: String, val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class EvGeminiClient {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "Kamu adalah asisten ahli kendaraan listrik (EV) Indonesia di aplikasi VoltMotor. " +
                        "Jawablah dalam bahasa Indonesia yang ringkas, akurat, terstruktur (gunakan poin singkat), " +
                        "dan selalu gunakan data katalog EV lokal yang disertakan dalam konteks jika relevan. " +
                        "Bantu pengguna memilih motor listrik terbaik sesuai anggaran, jarak tempuh, kemampuan tanjakan, " +
                        "efisiensi Wh/km, sistem swap baterai, dan garansi resmi."
                )
            }
        )
    }

    fun streamAdvice(
        userQuestion: String,
        catalogContext: List<ElectricMotorEntity>,
        duelMotors: List<ElectricMotorEntity>
    ): Flow<String> {
        val compactCatalog = catalogContext.take(35).joinToString("\n") { m ->
            "- ${m.brand} ${m.modelName} (${m.vehicleType}): Rp ${m.priceIdr / 1_000_000.0} Jt | " +
                "${m.motorPowerWatt}W | ${m.batteryKwh}kWh | Jarak ${m.rangeKm}km | " +
                "${String.format("%.1f", m.whPerKm)}Wh/km | Tanjakan ${m.hillClimbDegrees}° | " +
                "Garansi ${m.warrantyYears}th | Swap=${m.isSwappable}"
        }

        val duelSummary = if (duelMotors.size >= 2) {
            "\nMotor yang sedang dibandingkan di Duel EV user:\n" +
                duelMotors.joinToString("\n") { m ->
                    "* ${m.brand} ${m.modelName}: Rp ${m.priceIdr}, ${m.motorPowerWatt}W, " +
                        "${m.rangeKm}km, ${String.format("%.1f", m.whPerKm)}Wh/km, " +
                        "Tanjakan ${m.hillClimbGrade}, Garansi: ${m.warrantyInfo}"
                }
        } else {
            ""
        }

        val fullPrompt = """
            Referensi Katalog EV Lokal (VoltMotor):
            $compactCatalog
            $duelSummary

            Pertanyaan Pengguna:
            $userQuestion
        """.trimIndent()

        return model.generateContentStream(fullPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }
}
