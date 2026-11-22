package com.example.data.repository

import com.example.data.local.MotorDao
import com.example.data.local.SeedCatalogData
import com.example.data.local.ThemePreferences
import com.example.data.model.ElectricMotorEntity
import com.example.data.model.SyncLogEntity
import com.example.data.model.ThemeMode
import com.example.data.remote.CekMotorListrikScraper
import com.example.data.remote.NetworkConnectivityObserver
import com.example.data.remote.ScrapeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MotorRepository(
    private val motorDao: MotorDao,
    private val scraper: CekMotorListrikScraper,
    private val connectivityObserver: NetworkConnectivityObserver,
    private val themePreferences: ThemePreferences
) {
    private val syncMutex = Mutex()

    val motorsFlow: Flow<List<ElectricMotorEntity>> = motorDao.observeAllMotors()
    val syncLogsFlow: Flow<List<SyncLogEntity>> = motorDao.observeSyncLogs()
    val isOnlineFlow: Flow<Boolean> = connectivityObserver.observeOnlineState()
    val themeModeFlow: Flow<ThemeMode> = themePreferences.themeModeFlow
    val autoSyncFlow: Flow<Boolean> = themePreferences.autoSyncFlow
    val lastSyncTimestampFlow: Flow<Long> = themePreferences.lastSyncTimestampFlow

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val seed = SeedCatalogData.getInitialMotors(now)
        val existingIds = motorDao.getAllIds().toHashSet()
        val missing = seed.filter { !existingIds.contains(it.id) }

        if (missing.isNotEmpty()) {
            motorDao.insertMissing(missing)
            motorDao.insertSyncLog(
                SyncLogEntity(
                    timestamp = now,
                    status = "OFFLINE_SEED",
                    modelsUpdated = missing.size,
                    sourceEndpoint = "Bundled cekmotorlistrik.com & EV Indonesia",
                    message = "Berhasil menyalin ${missing.size} model EV Indonesia ke database lokal (Total ${existingIds.size + missing.size})."
                )
            )
            themePreferences.updateLastSyncTimestamp(now)
        }
    }

    suspend fun syncWithWeb(isAutoTrigger: Boolean): ScrapeResult = syncMutex.withLock {
        ensureSeeded()
        val currentMotors = motorsFlow.first()
        val now = System.currentTimeMillis()

        if (!connectivityObserver.isCurrentlyOnline()) {
            return ScrapeResult(
                motors = emptyList(),
                endpointUsed = "Local Room DB",
                isLiveSuccess = false,
                message = "Tidak ada koneksi internet. Mengakses database offline (${currentMotors.size} model)."
            )
        }

        val result = scraper.fetchLatestCatalog(currentMotors)
        if (result.isLiveSuccess && result.motors.isNotEmpty()) {
            motorDao.upsertPreservingFavorites(result.motors)
            themePreferences.updateLastSyncTimestamp(now)
            motorDao.insertSyncLog(
                SyncLogEntity(
                    timestamp = now,
                    status = if (isAutoTrigger) "AUTO_SYNC_OK" else "MANUAL_SYNC_OK",
                    modelsUpdated = result.motors.size,
                    sourceEndpoint = result.endpointUsed,
                    message = result.message
                )
            )
        } else {
            motorDao.insertSyncLog(
                SyncLogEntity(
                    timestamp = now,
                    status = "OFFLINE_FALLBACK",
                    modelsUpdated = 0,
                    sourceEndpoint = result.endpointUsed,
                    message = result.message
                )
            )
        }
        result
    }

    suspend fun toggleFavorite(motorId: String, currentFavorite: Boolean) {
        motorDao.setFavorite(motorId, !currentFavorite)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        themePreferences.setThemeMode(mode)
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        themePreferences.setAutoSyncEnabled(enabled)
    }
}
