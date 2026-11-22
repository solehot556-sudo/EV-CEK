package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiUiState
import com.example.ai.EvGeminiClient
import com.example.data.local.MotorDatabase
import com.example.data.local.ThemePreferences
import com.example.data.model.ElectricMotorEntity
import com.example.data.model.SortOption
import com.example.data.model.SyncLogEntity
import com.example.data.model.SyncState
import com.example.data.model.SyncStatusUi
import com.example.data.model.ThemeMode
import com.example.data.remote.CekMotorListrikScraper
import com.example.data.remote.NetworkConnectivityObserver
import com.example.data.repository.MotorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "VoltMotorViewModel"

enum class AppTab(val label: String) {
    CATALOG("Katalog"),
    COMPARE("Duel EV"),
    CALCULATOR("Hitung"),
    AI_ADVISOR("Tanya AI"),
    SETTINGS("Setelan")
}

data class CatalogFilterState(
    val query: String = "",
    val selectedType: String = "Semua",
    val selectedBrand: String = "Semua",
    val minPriceMillion: Float = 5f,
    val maxPriceMillion: Float = 65f,
    val minRangeKm: Int = 0,
    val onlyFavorites: Boolean = false,
    val sortOption: SortOption = SortOption.PRICE_ASC
)

class VoltMotorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MotorRepository

    init {
        val db = MotorDatabase.getInstance(application)
        repository = MotorRepository(
            motorDao = db.motorDao(),
            scraper = CekMotorListrikScraper(),
            connectivityObserver = NetworkConnectivityObserver(application),
            themePreferences = ThemePreferences(application)
        )
    }

    private val _selectedTab = MutableStateFlow(AppTab.CATALOG)
    val selectedTab: StateFlow<AppTab> = _selectedTab

    private val _filterState = MutableStateFlow(CatalogFilterState())
    val filterState: StateFlow<CatalogFilterState> = _filterState

    private val _selectedMotorDetail = MutableStateFlow<ElectricMotorEntity?>(null)
    val selectedMotorDetail: StateFlow<ElectricMotorEntity?> = _selectedMotorDetail

    // Dua slot utama perbandingan EV (Head-to-Head Duel) + opsional slot ke-3
    private val _compareIds = MutableStateFlow(listOf("polytron-fox-r", "alva-cervo"))
    val compareIds: StateFlow<List<String>> = _compareIds

    private val geminiClient = EvGeminiClient()

    private val _calculatorMotorId = MutableStateFlow("polytron-fox-r")
    val calculatorMotorId: StateFlow<String> = _calculatorMotorId

    private val _aiUiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiUiState: StateFlow<AiUiState> = _aiUiState

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    private val _syncMessage = MutableStateFlow("Database offline siap digunakan")

    val themeMode: StateFlow<ThemeMode> = repository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.DARK)

    val autoSyncEnabled: StateFlow<Boolean> = repository.autoSyncFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isOnline: StateFlow<Boolean> = repository.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val allMotors: StateFlow<List<ElectricMotorEntity>> = repository.motorsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.syncLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncStatus: StateFlow<SyncStatusUi> = combine(
        _syncState,
        isOnline,
        repository.lastSyncTimestampFlow,
        _syncMessage,
        allMotors
    ) { state, online, lastTs, msg, motors ->
        val effectiveState = when {
            state == SyncState.SYNCING -> SyncState.SYNCING
            !online -> SyncState.OFFLINE
            else -> state
        }
        SyncStatusUi(
            state = effectiveState,
            isOnline = online,
            lastSyncedAt = lastTs,
            statusMessage = if (!online && state != SyncState.SYNCING) {
                "Mode Offline • ${motors.size} model tersimpan lokal"
            } else {
                msg
            },
            totalCachedModels = motors.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyncStatusUi())

    val filteredMotors: StateFlow<List<ElectricMotorEntity>> = combine(
        allMotors,
        _filterState
    ) { motors, filter ->
        val q = filter.query.trim().lowercase()
        val minPrice = (filter.minPriceMillion * 1_000_000f).toLong()
        val maxPrice = (filter.maxPriceMillion * 1_000_000f).toLong()

        val filtered = motors.filter { m ->
            val matchesQuery = q.isEmpty() ||
                m.brand.lowercase().contains(q) ||
                m.modelName.lowercase().contains(q) ||
                m.vehicleType.lowercase().contains(q) ||
                m.highlights.lowercase().contains(q)

            val matchesType = filter.selectedType == "Semua" ||
                m.vehicleType.equals(filter.selectedType, ignoreCase = true)

            val matchesBrand = filter.selectedBrand == "Semua" ||
                m.brand.equals(filter.selectedBrand, ignoreCase = true)

            val matchesPrice = m.priceIdr in minPrice..maxPrice
            val matchesRange = m.rangeKm >= filter.minRangeKm
            val matchesFav = !filter.onlyFavorites || m.isFavorite

            matchesQuery && matchesType && matchesBrand && matchesPrice && matchesRange && matchesFav
        }

        when (filter.sortOption) {
            SortOption.PRICE_ASC -> filtered.sortedBy { it.priceIdr }
            SortOption.PRICE_DESC -> filtered.sortedByDescending { it.priceIdr }
            SortOption.RANGE_DESC -> filtered.sortedByDescending { it.rangeKm }
            SortOption.POWER_DESC -> filtered.sortedByDescending { it.motorPowerWatt }
            SortOption.EFFICIENCY_ASC -> filtered.sortedBy { it.whPerKm }
            SortOption.SPEED_DESC -> filtered.sortedByDescending { it.topSpeedKmh }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val compareMotors: StateFlow<List<ElectricMotorEntity>> = combine(
        allMotors,
        _compareIds
    ) { motors, ids ->
        ids.mapNotNull { id -> motors.firstOrNull { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var lastAutoSyncAtMs = 0L

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            // Pantau koneksi internet & sinkronisasi otomatis setiap kali internet terdeteksi
            repository.isOnlineFlow.collect { online ->
                if (online && repository.autoSyncFlow.first()) {
                    val now = System.currentTimeMillis()
                    if (now - lastAutoSyncAtMs > 20_000L) {
                        lastAutoSyncAtMs = now
                        performSync(isAutoTrigger = true)
                    }
                } else if (!online) {
                    _syncState.value = SyncState.OFFLINE
                    _syncMessage.value = "Koneksi terputus • Menggunakan cache lokal"
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun openMotorDetail(motor: ElectricMotorEntity?) {
        _selectedMotorDetail.value = motor
    }

    fun updateQuery(query: String) {
        _filterState.update { it.copy(query = query) }
    }

    fun selectType(type: String) {
        _filterState.update { it.copy(selectedType = type) }
    }

    fun selectBrand(brand: String) {
        _filterState.update { it.copy(selectedBrand = brand) }
    }

    fun updatePriceRange(minMillion: Float, maxMillion: Float) {
        _filterState.update {
            it.copy(
                minPriceMillion = minMillion.coerceIn(5f, 65f),
                maxPriceMillion = maxMillion.coerceIn(minMillion, 65f)
            )
        }
    }

    fun updateMinRangeKm(minKm: Int) {
        _filterState.update { it.copy(minRangeKm = minKm) }
    }

    fun toggleFavoritesOnly() {
        _filterState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    fun selectSort(sortOption: SortOption) {
        _filterState.update { it.copy(sortOption = sortOption) }
    }

    fun resetFilters() {
        _filterState.value = CatalogFilterState()
    }

    fun toggleFavorite(motor: ElectricMotorEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(motor.id, motor.isFavorite)
            if (_selectedMotorDetail.value?.id == motor.id) {
                _selectedMotorDetail.value = motor.copy(isFavorite = !motor.isFavorite)
            }
        }
    }

    fun setCompareSlot(slotIndex: Int, motorId: String) {
        _compareIds.update { current ->
            val mutable = current.toMutableList()
            while (mutable.size <= slotIndex) {
                mutable.add(motorId)
            }
            mutable[slotIndex] = motorId
            mutable.distinct()
        }
    }

    fun toggleCompareSelection(motorId: String) {
        _compareIds.update { current ->
            when {
                current.contains(motorId) && current.size > 1 -> current - motorId
                current.contains(motorId) -> current
                current.size < 3 -> current + motorId
                else -> listOf(current[1], current[2], motorId)
            }
        }
    }

    fun applyQuickDuelPreset(firstId: String, secondId: String) {
        _compareIds.value = listOf(firstId, secondId)
    }

    fun addThirdCompareSlot(motorId: String) {
        _compareIds.update { current ->
            if (current.size < 3 && !current.contains(motorId)) current + motorId else current
        }
    }

    fun removeCompareSlot(slotIndex: Int) {
        _compareIds.update { current ->
            if (current.size > 2 && slotIndex in current.indices) {
                current.toMutableList().also { it.removeAt(slotIndex) }
            } else {
                current
            }
        }
    }

    fun selectCalculatorMotor(motorId: String) {
        _calculatorMotorId.value = motorId
    }

    fun openCalculatorForMotor(motor: ElectricMotorEntity) {
        _calculatorMotorId.value = motor.id
        _selectedMotorDetail.value = null
        _selectedTab.value = AppTab.CALCULATOR
    }

    fun askGemini(question: String) {
        val cleanQuestion = question.trim()
        if (cleanQuestion.isEmpty()) return

        viewModelScope.launch {
            _aiUiState.value = AiUiState.Loading
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiClient.streamAdvice(
                        userQuestion = cleanQuestion,
                        catalogContext = allMotors.value,
                        duelMotors = compareMotors.value
                    ).collect { chunk ->
                        accumulated.append(chunk)
                        _aiUiState.value = AiUiState.Streaming(
                            prompt = cleanQuestion,
                            text = accumulated.toString()
                        )
                    }
                }
                if (accumulated.isNotBlank()) {
                    _aiUiState.value = AiUiState.Success(
                        prompt = cleanQuestion,
                        text = accumulated.toString()
                    )
                } else {
                    _aiUiState.value = AiUiState.Error("Gemini mengembalikan respons kosong.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini AI generation timed out", e)
                _aiUiState.value = AiUiState.Error(
                    "Permintaan melebihi batas waktu 60 detik. Silakan coba lagi."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _aiUiState.value = AiUiState.Error(
                    e.localizedMessage ?: "Gagal menghubungi Gemini AI."
                )
            }
        }
    }

    fun askGeminiAboutMotor(motor: ElectricMotorEntity) {
        _selectedMotorDetail.value = null
        _selectedTab.value = AppTab.AI_ADVISOR
        askGemini(
            "Berikan ulasan mendalam, kelebihan, kekurangan, serta kecocokan penggunaan harian untuk ${motor.brand} ${motor.modelName} (harga ${motor.priceIdr}, daya ${motor.motorPowerWatt}W, jarak ${motor.rangeKm} km, garansi ${motor.warrantyInfo})."
        )
    }

    fun askGeminiAboutDuel() {
        _selectedTab.value = AppTab.AI_ADVISOR
        askGemini(
            "Analisis perbandingan head-to-head antara motor EV yang sedang saya duelkan sekarang dari sisi keiritan biaya per km, harga, kemampuan tanjakan, dan garansinya. Mana yang paling worth it untuk dibeli?"
        )
    }

    fun clearAiResult() {
        _aiUiState.value = AiUiState.Idle
    }

    fun syncNow() {
        viewModelScope.launch {
            performSync(isAutoTrigger = false)
        }
    }

    private suspend fun performSync(isAutoTrigger: Boolean) {
        _syncState.value = SyncState.SYNCING
        _syncMessage.value = "Menyinkronkan data dari cekmotorlistrik.com..."
        val result = repository.syncWithWeb(isAutoTrigger)
        if (result.isLiveSuccess) {
            _syncState.value = SyncState.SYNCED
            _syncMessage.value = result.message
        } else {
            _syncState.value = if (isOnline.value) SyncState.ERROR else SyncState.OFFLINE
            _syncMessage.value = result.message
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.setThemeMode(mode)
        }
    }

    fun cycleQuickDarkMode() {
        viewModelScope.launch {
            val next = if (themeMode.value == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
            repository.setThemeMode(next)
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
        }
    }
}
