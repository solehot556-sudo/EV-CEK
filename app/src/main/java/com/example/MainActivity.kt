package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.configureAppCheck
import com.example.data.model.ThemeMode
import com.example.ui.components.GlassBackdrop
import com.example.ui.components.RightSideGlassTabRail
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.CompareScreen
import com.example.ui.screens.MotorDetailSheet
import com.example.ui.screens.SettingsSyncScreen
import com.example.ui.theme.VoltMotorTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.VoltMotorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val viewModel: VoltMotorViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LaunchedEffect(isDark) {
                val transparentArgb = Color.Transparent.toArgb()
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparentArgb)
                    } else {
                        SystemBarStyle.light(transparentArgb, transparentArgb)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(transparentArgb)
                    } else {
                        SystemBarStyle.light(transparentArgb, transparentArgb)
                    }
                )
            }

            VoltMotorTheme(themeMode = themeMode) {
                VoltMotorAppShell(
                    viewModel = viewModel,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
private fun VoltMotorAppShell(
    viewModel: VoltMotorViewModel,
    isDark: Boolean
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val filteredMotors by viewModel.filteredMotors.collectAsStateWithLifecycle()
    val allMotors by viewModel.allMotors.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val compareIds by viewModel.compareIds.collectAsStateWithLifecycle()
    val compareMotors by viewModel.compareMotors.collectAsStateWithLifecycle()
    val calculatorMotorId by viewModel.calculatorMotorId.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val selectedDetail by viewModel.selectedMotorDetail.collectAsStateWithLifecycle()
    val aiUiState by viewModel.aiUiState.collectAsStateWithLifecycle()

    if (selectedTab != AppTab.CATALOG && selectedDetail == null) {
        BackHandler {
            viewModel.selectTab(AppTab.CATALOG)
        }
    }

    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val topSafe = safePadding.calculateTopPadding()
    val bottomSafe = safePadding.calculateBottomPadding()
    val rightSafe = safePadding.calculateRightPadding(layoutDirection)
    val leftSafe = safePadding.calculateLeftPadding(layoutDirection)

    GlassBackdrop {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = leftSafe, top = topSafe)
        ) {
            // Tampilan tetap FULL tanpa dikurangi lebar navigasi;
            // kapsul kaca iPhone melayang ultra-transparan di sisi kanan.
            val fullWidthEndPadding = rightSafe + 16.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 880.dp)
                    .align(Alignment.TopCenter)
            ) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        val goingDown = targetState.ordinal > initialState.ordinal
                        val enterSpec = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { fullHeight ->
                            if (goingDown) (fullHeight * 0.08f).toInt() else -(fullHeight * 0.08f).toInt()
                        } + scaleIn(
                            initialScale = 0.94f,
                            animationSpec = spring(
                                dampingRatio = 0.80f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(240))

                        val exitSpec = slideOutVertically(
                            animationSpec = tween(220)
                        ) { fullHeight ->
                            if (goingDown) -(fullHeight * 0.05f).toInt() else (fullHeight * 0.05f).toInt()
                        } + scaleOut(
                            targetScale = 1.03f,
                            animationSpec = tween(220)
                        ) + fadeOut(animationSpec = tween(180))

                        enterSpec togetherWith exitSpec
                    },
                    label = "iphoneGlassTransition"
                ) { tab ->
                    when (tab) {
                        AppTab.CATALOG -> {
                            val brands = listOf("Semua") + allMotors.map { it.brand }.distinct().sorted()
                            CatalogScreen(
                                motors = filteredMotors,
                                totalCatalogCount = allMotors.size,
                                availableBrands = brands,
                                filterState = filterState,
                                compareIds = compareIds,
                                syncStatus = syncStatus,
                                contentEndPadding = fullWidthEndPadding,
                                bottomInsetPadding = bottomSafe,
                                onQueryChange = viewModel::updateQuery,
                                onSelectType = viewModel::selectType,
                                onSelectBrand = viewModel::selectBrand,
                                onPriceRangeChange = viewModel::updatePriceRange,
                                onMinRangeChange = viewModel::updateMinRangeKm,
                                onSelectSort = viewModel::selectSort,
                                onToggleFavoritesOnly = viewModel::toggleFavoritesOnly,
                                onResetFilters = viewModel::resetFilters,
                                onManualSync = viewModel::syncNow,
                                onToggleFavorite = viewModel::toggleFavorite,
                                onToggleCompare = viewModel::toggleCompareSelection,
                                onOpenDetail = viewModel::openMotorDetail,
                                onGoToCompareTab = { viewModel.selectTab(AppTab.COMPARE) },
                                onGoToCalculatorTab = { viewModel.selectTab(AppTab.CALCULATOR) }
                            )
                        }

                        AppTab.COMPARE -> CompareScreen(
                            allMotors = allMotors,
                            compareMotors = compareMotors,
                            contentEndPadding = fullWidthEndPadding,
                            bottomInsetPadding = bottomSafe,
                            onSelectSlotMotor = viewModel::setCompareSlot,
                            onApplyPreset = viewModel::applyQuickDuelPreset,
                            onAddThirdSlot = viewModel::addThirdCompareSlot,
                            onRemoveSlot = viewModel::removeCompareSlot,
                            onAskGeminiDuel = viewModel::askGeminiAboutDuel
                        )

                        AppTab.CALCULATOR -> CalculatorScreen(
                            allMotors = allMotors,
                            selectedMotorId = calculatorMotorId,
                            contentEndPadding = fullWidthEndPadding,
                            bottomInsetPadding = bottomSafe,
                            onSelectCalculatorMotor = viewModel::selectCalculatorMotor,
                            onOpenMotorDetail = viewModel::openMotorDetail
                        )

                        AppTab.AI_ADVISOR -> AiAdvisorScreen(
                            aiUiState = aiUiState,
                            duelSummaryNames = compareMotors.take(2).joinToString(" vs ") {
                                "${it.brand} ${it.modelName}"
                            },
                            contentEndPadding = fullWidthEndPadding,
                            bottomInsetPadding = bottomSafe,
                            onAskGemini = viewModel::askGemini,
                            onClearAi = viewModel::clearAiResult
                        )

                        AppTab.SETTINGS -> SettingsSyncScreen(
                            themeMode = themeMode,
                            autoSyncEnabled = autoSyncEnabled,
                            syncStatus = syncStatus,
                            syncLogs = syncLogs,
                            favoriteCount = allMotors.count { it.isFavorite },
                            contentEndPadding = fullWidthEndPadding,
                            bottomInsetPadding = bottomSafe,
                            onSelectThemeMode = viewModel::setThemeMode,
                            onToggleAutoSync = viewModel::setAutoSyncEnabled,
                            onManualSync = viewModel::syncNow
                        )
                    }
                }
            }

            // Seluruh Tab Control melayang di sisi kanan dengan efek crystal glass transparan
            RightSideGlassTabRail(
                selectedTab = selectedTab,
                isDark = isDark,
                compareCount = compareIds.size,
                onSelectTab = viewModel::selectTab,
                onToggleDarkMode = viewModel::cycleQuickDarkMode,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = rightSafe + 8.dp, bottom = bottomSafe)
            )
        }
    }

    val activeDetail = selectedDetail
    if (activeDetail != null) {
        MotorDetailSheet(
            motor = activeDetail,
            isInCompare = compareIds.contains(activeDetail.id),
            onDismiss = { viewModel.openMotorDetail(null) },
            onToggleFavorite = { viewModel.toggleFavorite(activeDetail) },
            onAddToCompare = {
                if (!compareIds.contains(activeDetail.id)) {
                    viewModel.toggleCompareSelection(activeDetail.id)
                }
                viewModel.selectTab(AppTab.COMPARE)
            },
            onCalculateSavings = {
                viewModel.openCalculatorForMotor(activeDetail)
            },
            onAskGemini = {
                viewModel.askGeminiAboutMotor(activeDetail)
            }
        )
    }
}
