package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.ElectricMoped
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ElectricMotorEntity
import com.example.data.model.SortOption
import com.example.data.model.SyncStatusUi
import com.example.ui.components.GlassCard
import com.example.ui.components.MotorStudioImage
import com.example.ui.components.SyncStatusGlassPill
import com.example.ui.components.formatRupiahFull
import com.example.ui.components.formatRupiahShort
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.CatalogFilterState
import kotlin.math.roundToInt

private val VEHICLE_TYPES = listOf(
    "Semua",
    "Skuter Listrik",
    "Maxi EV",
    "Sport EV",
    "Swap Battery",
    "Sepeda Listrik"
)

@Composable
fun CatalogScreen(
    motors: List<ElectricMotorEntity>,
    totalCatalogCount: Int,
    availableBrands: List<String>,
    filterState: CatalogFilterState,
    compareIds: List<String>,
    syncStatus: SyncStatusUi,
    contentEndPadding: Dp,
    bottomInsetPadding: Dp,
    onQueryChange: (String) -> Unit,
    onSelectType: (String) -> Unit,
    onSelectBrand: (String) -> Unit,
    onPriceRangeChange: (Float, Float) -> Unit,
    onMinRangeChange: (Int) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onResetFilters: () -> Unit,
    onManualSync: () -> Unit,
    onToggleFavorite: (ElectricMotorEntity) -> Unit,
    onToggleCompare: (String) -> Unit,
    onOpenDetail: (ElectricMotorEntity) -> Unit,
    onGoToCompareTab: () -> Unit,
    onGoToCalculatorTab: () -> Unit
) {
    var showFilterPanel by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = contentEndPadding,
            top = 8.dp,
            bottom = bottomInsetPadding + 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Sync Pill
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "VoltMotor",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Katalog EV Indonesia • cekmotorlistrik.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                SyncStatusGlassPill(
                    syncStatus = syncStatus,
                    onManualSync = onManualSync
                )
            }
        }

        // Hero Glass Banner
        item {
            HeroBannerGlassCard(
                totalModels = totalCatalogCount,
                filteredCount = motors.size,
                compareCount = compareIds.size,
                onGoToCompare = onGoToCompareTab,
                onGoToCalculator = onGoToCalculatorTab
            )
        }

        // Search & Filter Toggle Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    elevation = 3.dp
                ) {
                    TextField(
                        value = filterState.query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_motor_input"),
                        placeholder = {
                            Text(
                                text = "Cari Polytron, ALVA, Honda...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Cari motor listrik",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            if (filterState.query.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Hapus pencarian"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                val hasActiveRangeOrPrice = filterState.minPriceMillion > 5f ||
                    filterState.maxPriceMillion < 65f ||
                    filterState.minRangeKm > 0 ||
                    filterState.onlyFavorites

                GlassCard(
                    modifier = Modifier
                        .size(54.dp)
                        .testTag("toggle_filter_panel_button"),
                    shape = RoundedCornerShape(18.dp),
                    elevation = 3.dp,
                    onClick = { showFilterPanel = !showFilterPanel }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (showFilterPanel || hasActiveRangeOrPrice)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FilterList,
                            contentDescription = "Filter harga dan jarak tempuh",
                            tint = if (showFilterPanel || hasActiveRangeOrPrice)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Quick Vehicle Type Pills (Tipe Kendaraan)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VEHICLE_TYPES.forEach { type ->
                    val selected = filterState.selectedType == type
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectType(type) },
                        label = {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("type_chip_${type.lowercase().replace(" ", "_")}"),
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Quick Brand Pills (Merek EV Indonesia)
        if (availableBrands.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableBrands.forEach { brand ->
                        val selected = filterState.selectedBrand == brand
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectBrand(brand) },
                            label = {
                                Text(
                                    text = if (brand == "Semua") "Semua Merek" else brand,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            shape = RoundedCornerShape(50),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.22f),
                                selectedLabelColor = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }

        // Expandable Glass Filter Panel (Harga, Jarak Tempuh, Urutan, Favorit)
        item {
            AnimatedVisibility(
                visible = showFilterPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AdvancedGlassFilterPanel(
                    filterState = filterState,
                    onPriceRangeChange = onPriceRangeChange,
                    onMinRangeChange = onMinRangeChange,
                    onSelectSort = onSelectSort,
                    onToggleFavoritesOnly = onToggleFavoritesOnly,
                    onResetFilters = onResetFilters
                )
            }
        }

        // Result Summary Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menampilkan ${motors.size} dari $totalCatalogCount unit",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = filterState.sortOption.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showFilterPanel = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Empty State or Motor Cards
        if (motors.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ElectricMoped,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada motor listrik yang cocok",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Coba perluas rentang harga atau turunkan batas jarak tempuh minimum.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onResetFilters,
                            modifier = Modifier.testTag("reset_filters_button")
                        ) {
                            Text("Reset Semua Filter")
                        }
                    }
                }
            }
        } else {
            items(motors, key = { it.id }) { motor ->
                MotorGlassCard(
                    motor = motor,
                    isInCompare = compareIds.contains(motor.id),
                    onClick = { onOpenDetail(motor) },
                    onToggleFavorite = { onToggleFavorite(motor) },
                    onToggleCompare = { onToggleCompare(motor.id) }
                )
            }
        }
    }
}

@Composable
private fun HeroBannerGlassCard(
    totalModels: Int,
    filteredCount: Int,
    compareCount: Int,
    onGoToCompare: () -> Unit,
    onGoToCalculator: () -> Unit
) {
    val tokens = LocalGlassTokens.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_ev_banner),
                contentDescription = "Banner Katalog Motor Listrik Indonesia",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xE6070D19),
                                Color(0xB80A1426),
                                Color(0x550A1426)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x382CEAA3))
                            .border(0.8.dp, Color(0x702CEAA3), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "OFFLINE + AUTO SYNC",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF8CFFD6)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x3810B981))
                                .border(0.8.dp, Color(0x7010B981), RoundedCornerShape(50))
                                .clickable(onClick = onGoToCalculator)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("hero_calculator_shortcut")
                        ) {
                            Text(
                                text = "Hitung Hemat",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0x45FFFFFF))
                                .clickable(onClick = onGoToCompare)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .testTag("hero_compare_shortcut")
                        ) {
                            Text(
                                text = "Duel $compareCount EV →",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Column {
                    Text(
                        text = "Temukan Motor Listrik\nTerbaik di Indonesia",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "$totalModels model tersimpan di Room DB • $filteredCount tampil • Filter Tipe, Harga & Jarak",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFD5E2F5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedGlassFilterPanel(
    filterState: CatalogFilterState,
    onPriceRangeChange: (Float, Float) -> Unit,
    onMinRangeChange: (Int) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onResetFilters: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("advanced_filter_panel"),
        shape = RoundedCornerShape(24.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Spesifikasi & Harga",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Reset",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onResetFilters)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Harga Range Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Rentang Harga",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rp ${filterState.minPriceMillion.roundToInt()} Jt – Rp ${filterState.maxPriceMillion.roundToInt()} Jt",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                RangeSlider(
                    value = filterState.minPriceMillion..filterState.maxPriceMillion,
                    onValueChange = { range ->
                        onPriceRangeChange(range.start, range.endInclusive)
                    },
                    valueRange = 5f..65f,
                    steps = 11,
                    modifier = Modifier.testTag("price_range_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Jarak Tempuh Minimum Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Jarak Tempuh Minimum",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (filterState.minRangeKm == 0) "Semua Jarak" else "≥ ${filterState.minRangeKm} km",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Slider(
                    value = filterState.minRangeKm.toFloat(),
                    onValueChange = { onMinRangeChange((it / 10f).roundToInt() * 10) },
                    valueRange = 0f..150f,
                    steps = 14,
                    modifier = Modifier.testTag("min_range_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            // Sort Options
            Text(
                text = "Urutkan Berdasarkan",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SortOption.entries.forEach { sort ->
                    FilterChip(
                        selected = filterState.sortOption == sort,
                        onClick = { onSelectSort(sort) },
                        label = { Text(sort.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
                FilterChip(
                    selected = filterState.onlyFavorites,
                    onClick = onToggleFavoritesOnly,
                    leadingIcon = {
                        Icon(
                            imageVector = if (filterState.onlyFavorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Favorit Saja", style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}

@Composable
private fun MotorGlassCard(
    motor: ElectricMotorEntity,
    isInCompare: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompare: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("motor_card_${motor.id}"),
        shape = RoundedCornerShape(24.dp),
        elevation = 5.dp,
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 108.dp, height = 84.dp)
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    MotorStudioImage(
                        motor = motor,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (motor.subsidyEligible) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricEmerald.copy(alpha = 0.88f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "TKDN",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = motor.vehicleType.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Rp ${motor.costPerKmIdr}/km",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricEmerald
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${motor.brand} ${motor.modelName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRupiahFull(motor.priceIdr),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = motor.priceNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Spec Telemetry Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SpecMiniPill(
                    icon = Icons.Rounded.Bolt,
                    label = "${motor.motorPowerWatt}W",
                    modifier = Modifier.weight(1f)
                )
                SpecMiniPill(
                    icon = Icons.Rounded.Route,
                    label = "${motor.rangeKm} km",
                    modifier = Modifier.weight(1f)
                )
                SpecMiniPill(
                    icon = Icons.Rounded.Speed,
                    label = "${motor.topSpeedKmh} km/h",
                    modifier = Modifier.weight(1f)
                )
                SpecMiniPill(
                    icon = Icons.Rounded.Terrain,
                    label = "${motor.hillClimbDegrees}°",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions: Garansi preview + Favorite + Compare toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Garansi",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Garansi ${motor.warrantyYears} Thn • ${motor.batteryKwh} kWh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("fav_btn_${motor.id}")
                    ) {
                        Icon(
                            imageVector = if (motor.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Simpan favorit",
                            tint = if (motor.isFavorite) Color(0xFFF43F5E) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isInCompare) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                            .clickable(onClick = onToggleCompare)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("compare_btn_${motor.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CompareArrows,
                                contentDescription = "Tambahkan ke perbandingan",
                                tint = if (isInCompare) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isInCompare) "Di Duel" else "Duel",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isInCompare) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecMiniPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (tokens.isDark) Color(0x26FFFFFF) else Color(0x55FFFFFF)
            )
            .border(0.6.dp, tokens.subtleDivider, RoundedCornerShape(12.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MotorDetailSheet(
    motor: ElectricMotorEntity,
    isInCompare: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToCompare: () -> Unit,
    onCalculateSavings: () -> Unit,
    onAskGemini: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                MotorStudioImage(motor = motor, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xBB0A1120))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${motor.vehicleType} • ${if (motor.isSwappable) "Bisa Swap Baterai" else "Cas Rumahan"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = motor.brand,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = motor.modelName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatRupiahFull(motor.priceIdr),
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = motor.priceNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Highlights chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                motor.highlights.split(",").filter { it.isNotBlank() }.forEach { badge ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Verified,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badge.trim(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Full Specification Table
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Spesifikasi Lengkap",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    DetailSpecRow("Daya Motor", "${motor.motorPowerWatt} Watt")
                    DetailSpecRow("Kapasitas Baterai", "${motor.batteryKwh} kWh (${motor.batterySpec})")
                    DetailSpecRow("Jarak Tempuh", "${motor.rangeKm} km / pengisian")
                    DetailSpecRow("Efisiensi Energi", "${String.format("%.1f", motor.whPerKm)} Wh/km (~Rp ${motor.costPerKmIdr}/km)")
                    DetailSpecRow("Kecepatan Maksimal", "${motor.topSpeedKmh} km/jam")
                    DetailSpecRow(
                        "Waktu Pengisian",
                        if (motor.chargingHours < 0.5) "Swap Baterai (±9 detik)" else "${motor.chargingHours} jam"
                    )
                    DetailSpecRow("Kemampuan Tanjakan", motor.hillClimbGrade)
                    DetailSpecRow("Bobot Kendaraan", "${motor.weightKg} kg")
                    DetailSpecRow("Garansi Resmi", motor.warrantyInfo)
                    DetailSpecRow(
                        "Subsidi / TKDN",
                        if (motor.subsidyEligible) "Memenuhi Syarat TKDN" else "Harga Non-Subsidi"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = if (motor.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (motor.isFavorite) "Difavoritkan" else "Favoritkan")
                }

                Button(
                    onClick = {
                        onAddToCompare()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Rounded.CompareArrows, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isInCompare) "Lihat Duel" else "Bandingkan")
                }
            }

            OutlinedButton(
                onClick = onCalculateSavings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hitung Penghematan EV Ini di Kalkulator Hemat")
            }

            Button(
                onClick = onAskGemini,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detail_ask_gemini_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tanya Gemini tentang EV Ini")
            }
        }
    }
}

@Composable
private fun DetailSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.44f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.56f)
        )
    }
}
