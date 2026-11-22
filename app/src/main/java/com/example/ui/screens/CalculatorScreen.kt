package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.ElectricMoped
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ElectricMotorEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.MotorStudioImage
import com.example.ui.components.formatRupiahFull
import com.example.ui.components.formatRupiahShort
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.roundToInt

@Composable
fun CalculatorScreen(
    allMotors: List<ElectricMotorEntity>,
    selectedMotorId: String,
    contentEndPadding: Dp,
    bottomInsetPadding: Dp,
    onSelectCalculatorMotor: (String) -> Unit,
    onOpenMotorDetail: (ElectricMotorEntity) -> Unit
) {
    var modeIndex by remember { mutableIntStateOf(0) } // 0 = Kalkulator Hemat, 1 = Cek Tanjakan
    var showMotorPicker by remember { mutableStateOf(false) }

    // State Kalkulator Hemat
    var dailyKm by remember { mutableFloatStateOf(30f) }
    var activeDaysPerMonth by remember { mutableFloatStateOf(26f) }
    var fuelPricePerLiter by remember { mutableFloatStateOf(10000f) }
    var fuelKmPerLiter by remember { mutableFloatStateOf(42f) }
    var electricityRatePerKwh by remember { mutableFloatStateOf(1445f) }
    var includeMaintenanceCost by remember { mutableStateOf(true) }

    // State Cek Tanjakan
    var totalRiderWeightKg by remember { mutableFloatStateOf(85f) }
    var slopeDegrees by remember { mutableFloatStateOf(18f) }

    val selectedMotor = remember(allMotors, selectedMotorId) {
        allMotors.firstOrNull { it.id == selectedMotorId } ?: allMotors.firstOrNull()
    }

    val monthlyKm = dailyKm * activeDaysPerMonth
    val monthlyFuelOnlyCost = ((monthlyKm / fuelKmPerLiter) * fuelPricePerLiter).roundToInt()
    val monthlyBbmMaintenance = if (includeMaintenanceCost) 85_000 else 0
    val totalMonthlyBbmCost = monthlyFuelOnlyCost + monthlyBbmMaintenance

    val evWhPerKm = selectedMotor?.whPerKm ?: 28.0
    val monthlyEvEnergyCost = ((monthlyKm * (evWhPerKm / 1000.0)) * electricityRatePerKwh).roundToInt()
    val monthlyEvMaintenance = if (includeMaintenanceCost) 15_000 else 0
    val totalMonthlyEvCost = monthlyEvEnergyCost + monthlyEvMaintenance

    val monthlySavings = (totalMonthlyBbmCost - totalMonthlyEvCost).coerceAtLeast(0)
    val yearlySavings = monthlySavings * 12L
    val fiveYearSavings = yearlySavings * 5L
    val breakEvenMonths = if (monthlySavings > 0 && selectedMotor != null) {
        (selectedMotor.priceIdr / monthlySavings).toInt().coerceAtLeast(1)
    } else {
        0
    }
    // Rata-rata emisi motor bensin ~0.072 kg CO2/km
    val yearlyCo2SavedKg = (monthlyKm * 12f * 0.072f).roundToInt()

    val requiredDegrees = slopeDegrees.roundToInt()
    val recommendedWatt = ((totalRiderWeightKg / 75f) * (requiredDegrees * 120f)).roundToInt().coerceAtLeast(1000)

    val capableClimbMotors = remember(allMotors, requiredDegrees, recommendedWatt) {
        allMotors.filter {
            it.hillClimbDegrees >= requiredDegrees && it.motorPowerWatt >= recommendedWatt
        }.sortedBy { it.priceIdr }
    }

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
        item {
            Column {
                Text(
                    text = "Kalkulator Hemat EV",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hitung penghematan BBM vs Listrik, Balik Modal & Cek Tanjakan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculator_mode_switch")
            ) {
                listOf("Kalkulator Hemat", "Cek Tanjakan").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = modeIndex == index,
                        onClick = { modeIndex = index },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2)
                    ) {
                        Text(label)
                    }
                }
            }
        }

        if (modeIndex == 0) {
            // Pilih EV Acuan dari Database Lokal
            if (selectedMotor != null) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calculator_motor_selector"),
                        shape = RoundedCornerShape(22.dp),
                        elevation = 4.dp,
                        onClick = { showMotorPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 74.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                MotorStudioImage(motor = selectedMotor, modifier = Modifier.fillMaxSize())
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EV Acuan Simulasi",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${selectedMotor.brand} ${selectedMotor.modelName}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${String.format("%.1f", selectedMotor.whPerKm)} Wh/km • ${formatRupiahShort(selectedMotor.priceIdr)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ElectricEmerald
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = "Ganti EV acuan",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Ganti",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Parameter Input Simulasi
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Savings,
                                contentDescription = null,
                                tint = ElectricEmerald
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Parameter Mobilitas Harian",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        SliderLabelRow("Jarak Harian", "${dailyKm.roundToInt()} km/hari")
                        Slider(
                            value = dailyKm,
                            onValueChange = { dailyKm = it },
                            valueRange = 5f..120f
                        )

                        SliderLabelRow("Hari Berkendara / Bulan", "${activeDaysPerMonth.roundToInt()} hari")
                        Slider(
                            value = activeDaysPerMonth,
                            onValueChange = { activeDaysPerMonth = it },
                            valueRange = 15f..30f
                        )

                        SliderLabelRow("Harga BBM", "Rp ${fuelPricePerLiter.roundToInt()}/L")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Pertalite (10rb)" to 10000f,
                                "Pertamax (12,9rb)" to 12950f,
                                "Turbo (13,5rb)" to 13500f
                            ).forEach { (label, price) ->
                                FilterChip(
                                    selected = fuelPricePerLiter.roundToInt() == price.roundToInt(),
                                    onClick = { fuelPricePerLiter = price },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        SliderLabelRow("Irit Motor Bensin", "${fuelKmPerLiter.roundToInt()} km/L")
                        Slider(
                            value = fuelKmPerLiter,
                            onValueChange = { fuelKmPerLiter = it },
                            valueRange = 25f..60f
                        )

                        SliderLabelRow("Tarif Listrik PLN", "Rp ${electricityRatePerKwh.roundToInt()}/kWh")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Subsidi (900)" to 900f,
                                "Rumah 1300VA (1.445)" to 1445f,
                                "SPKLU (2.466)" to 2466f
                            ).forEach { (label, rate) ->
                                FilterChip(
                                    selected = electricityRatePerKwh.roundToInt() == rate.roundToInt(),
                                    onClick = { electricityRatePerKwh = rate },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sertakan Oli & Servis Rutin",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "BBM ~Rp85rb/bln vs EV ~Rp15rb/bln",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = includeMaintenanceCost,
                                onCheckedChange = { includeMaintenanceCost = it }
                            )
                        }
                    }
                }
            }

            // Hasil Penghematan Utama + 5 Tahun + Break Even + CO2
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("savings_result_card"),
                    shape = RoundedCornerShape(24.dp),
                    elevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Total Penghematan (${monthlyKm.roundToInt()} km/bln)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatRupiahFull(monthlySavings.toLong())} / bulan",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = ElectricEmerald
                            )
                        )
                        Text(
                            text = "1 Tahun: ${formatRupiahFull(yearlySavings)} • 5 Tahun: ${formatRupiahFull(fiveYearSavings)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CostBox(
                                label = "Biaya Motor Bensin",
                                value = formatRupiahFull(totalMonthlyBbmCost.toLong()),
                                tint = ElectricAmber,
                                modifier = Modifier.weight(1f)
                            )
                            CostBox(
                                label = "Biaya Motor Listrik",
                                value = formatRupiahFull(totalMonthlyEvCost.toLong()),
                                tint = ElectricEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CostBox(
                                label = "Estimasi Balik Modal",
                                value = if (breakEvenMonths > 0) "$breakEvenMonths Bulan" else "-",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            CostBox(
                                label = "Reduksi Emisi CO₂",
                                value = "$yearlyCo2SavedKg kg/thn",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Eco,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rekomendasi EV Paling Irit (Jarak ≥ ${dailyKm.roundToInt()} km)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            val suitableRangeMotors = allMotors
                .filter { it.rangeKm >= dailyKm.roundToInt() }
                .sortedBy { it.whPerKm }
                .take(5)

            items(suitableRangeMotors, key = { it.id }) { motor ->
                RecommendedCalcMotorCard(
                    motor = motor,
                    badge = "${String.format("%.1f", motor.whPerKm)} Wh/km • Rp ${motor.costPerKmIdr}/km",
                    onClick = { onOpenMotorDetail(motor) }
                )
            }
        } else {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Terrain,
                                contentDescription = null,
                                tint = ElectricAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulasi Rute Tanjakan",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        SliderLabelRow("Total Beban (Pengendara + Barang)", "${totalRiderWeightKg.roundToInt()} kg")
                        Slider(
                            value = totalRiderWeightKg,
                            onValueChange = { totalRiderWeightKg = it },
                            valueRange = 45f..180f
                        )

                        SliderLabelRow("Kemiringan Tanjakan Rute", "${requiredDegrees}°")
                        Slider(
                            value = slopeDegrees,
                            onValueChange = { slopeDegrees = it },
                            valueRange = 8f..30f
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Disarankan minimal motor $recommendedWatt Watt (${capableClimbMotors.size} model lulus)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            items(capableClimbMotors, key = { it.id }) { motor ->
                RecommendedCalcMotorCard(
                    motor = motor,
                    badge = "${motor.hillClimbGrade} • ${motor.motorPowerWatt}W",
                    onClick = { onOpenMotorDetail(motor) }
                )
            }
        }
    }

    if (showMotorPicker) {
        CalculatorMotorPickerSheet(
            motors = allMotors,
            onDismiss = { showMotorPicker = false },
            onSelect = {
                onSelectCalculatorMotor(it.id)
                showMotorPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorMotorPickerSheet(
    motors: List<ElectricMotorEntity>,
    onDismiss: () -> Unit,
    onSelect: (ElectricMotorEntity) -> Unit
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
                .padding(horizontal = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ElectricMoped,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pilih EV untuk Kalkulator Hemat",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(motors, key = { it.id }) { motor ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        elevation = 2.dp,
                        onClick = { onSelect(motor) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${motor.brand} ${motor.modelName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${String.format("%.1f", motor.whPerKm)} Wh/km • ${motor.rangeKm} km • ${motor.motorPowerWatt}W",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatRupiahShort(motor.priceIdr),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SliderLabelRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CostBox(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = tint
            )
        }
    }
}

@Composable
private fun RecommendedCalcMotorCard(
    motor: ElectricMotorEntity,
    badge: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = 3.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${motor.brand} ${motor.modelName}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = badge,
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricEmerald
                )
            }
            Text(
                text = formatRupiahShort(motor.priceIdr),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
