package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.roundToInt

@Entity(tableName = "electric_motors")
data class ElectricMotorEntity(
    @PrimaryKey val id: String,
    val brand: String,
    val modelName: String,
    val vehicleType: String,
    val priceIdr: Long,
    val priceNote: String,
    val motorPowerWatt: Int,
    val batteryKwh: Double,
    val batterySpec: String,
    val isSwappable: Boolean,
    val rangeKm: Int,
    val topSpeedKmh: Int,
    val chargingHours: Double,
    val hillClimbGrade: String,
    val hillClimbDegrees: Int,
    val weightKg: Int,
    val warrantyInfo: String,
    val warrantyYears: Int,
    val subsidyEligible: Boolean,
    val imageUrl: String?,
    val sourceUrl: String,
    val highlights: String,
    val isFavorite: Boolean = false,
    val lastSyncedAt: Long = System.currentTimeMillis()
) {
    /** Konsumsi energi spesifik (Wh/km). Semakin kecil semakin irit. */
    val whPerKm: Double
        get() = if (rangeKm > 0) (batteryKwh * 1000.0) / rangeKm else 999.0

    /** Estimasi biaya listrik per km (asumsi tarif PLN R-1 Rp 1.445/kWh). */
    val costPerKmIdr: Int
        get() = ((whPerKm / 1000.0) * 1445.0).roundToInt().coerceAtLeast(12)
}

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val status: String,
    val modelsUpdated: Int,
    val sourceEndpoint: String,
    val message: String
)

enum class ThemeMode(val label: String) {
    SYSTEM("Ikuti Sistem"),
    LIGHT("Mode Terang"),
    DARK("Gelap Elegan")
}

enum class SortOption(val label: String) {
    PRICE_ASC("Harga Termurah"),
    PRICE_DESC("Harga Tertinggi"),
    RANGE_DESC("Jarak Terjauh"),
    POWER_DESC("Daya Terbesar"),
    EFFICIENCY_ASC("Paling Irit (Wh/km)"),
    SPEED_DESC("Top Speed")
}

enum class SyncState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

data class SyncStatusUi(
    val state: SyncState = SyncState.IDLE,
    val isOnline: Boolean = false,
    val lastSyncedAt: Long = 0L,
    val statusMessage: String = "Menunggu koneksi...",
    val totalCachedModels: Int = 0
)
