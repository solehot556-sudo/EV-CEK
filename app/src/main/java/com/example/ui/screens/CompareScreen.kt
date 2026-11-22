package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ElectricMotorEntity
import com.example.ui.components.GlassCard
import com.example.ui.components.MotorStudioImage
import com.example.ui.components.formatRupiahFull
import com.example.ui.components.formatRupiahShort
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalGlassTokens

@Composable
fun CompareScreen(
    allMotors: List<ElectricMotorEntity>,
    compareMotors: List<ElectricMotorEntity>,
    contentEndPadding: Dp,
    bottomInsetPadding: Dp,
    onSelectSlotMotor: (slotIndex: Int, motorId: String) -> Unit,
    onApplyPreset: (String, String) -> Unit,
    onAddThirdSlot: (String) -> Unit,
    onRemoveSlot: (Int) -> Unit,
    onAskGeminiDuel: () -> Unit
) {
    var pickingForSlot by remember { mutableStateOf<Int?>(null) }

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
                    text = "Duel Perbandingan EV",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Bandingkan 2 hingga 3 EV: paling irit, murah, kuat tanjakan & garansi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Preset Duel Cepat
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = compareMotors.map { it.id } == listOf("avand-sc121", "polytron-fox-r"),
                    onClick = { onApplyPreset("avand-sc121", "polytron-fox-r") },
                    label = { Text("Murah vs Terlaris") }
                )
                FilterChip(
                    selected = compareMotors.map { it.id } == listOf("alva-cervo", "maka-cavalry"),
                    onClick = { onApplyPreset("alva-cervo", "maka-cavalry") },
                    label = { Text("Raja Tanjakan") }
                )
                FilterChip(
                    selected = compareMotors.map { it.id } == listOf("yadea-e8s-pro", "smoot-tempur"),
                    onClick = { onApplyPreset("yadea-e8s-pro", "smoot-tempur") },
                    label = { Text("Irit vs Swap 9 Detik") }
                )
                FilterChip(
                    selected = compareMotors.map { it.id } == listOf("honda-icon-e", "alva-n3"),
                    onClick = { onApplyPreset("honda-icon-e", "alva-n3") },
                    label = { Text("Honda vs ALVA N3") }
                )
            }
        }

        // Slot Cards (2 EV Head-to-Head + opsional ke-3)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    compareMotors.take(2).forEachIndexed { index, motor ->
                        CompareSlotGlassCard(
                            slotLabel = "EV ${index + 1}",
                            motor = motor,
                            canRemove = false,
                            onChangeClick = { pickingForSlot = index },
                            onRemoveClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (compareMotors.size >= 3) {
                    CompareSlotGlassCard(
                        slotLabel = "EV 3 (Ekstra)",
                        motor = compareMotors[2],
                        canRemove = true,
                        onChangeClick = { pickingForSlot = 2 },
                        onRemoveClick = { onRemoveSlot(2) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (allMotors.size >= 3) {
                    val nextCandidate = allMotors.firstOrNull { candidate ->
                        compareMotors.none { it.id == candidate.id }
                    }
                    if (nextCandidate != null) {
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_third_ev_button"),
                            shape = RoundedCornerShape(18.dp),
                            elevation = 2.dp,
                            onClick = { onAddThirdSlot(nextCandidate.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Tambah EV ke-3",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tambah EV Ke-3 untuk Perbandingan",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (compareMotors.size >= 2) {
            // Ringkasan Pemenang Duel
            item {
                DuelWinnerSummaryCard(motors = compareMotors)
            }

            // Matriks Perbandingan Mendetail
            item {
                DuelComparisonMatrixCard(motors = compareMotors)
            }

            // Analisis Duel dengan Gemini AI
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("duel_ask_gemini_button"),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 4.dp,
                    onClick = onAskGeminiDuel
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Analisis duel dengan Gemini AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Analisis Duel dengan Gemini AI",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    val activeSlot = pickingForSlot
    if (activeSlot != null) {
        MotorPickerSheet(
            motors = allMotors,
            slotLabel = "Pilih Motor untuk Slot EV ${activeSlot + 1}",
            onDismiss = { pickingForSlot = null },
            onSelect = { selected ->
                onSelectSlotMotor(activeSlot, selected.id)
                pickingForSlot = null
            }
        )
    }
}

@Composable
private fun CompareSlotGlassCard(
    slotLabel: String,
    motor: ElectricMotorEntity,
    canRemove: Boolean,
    onChangeClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.testTag("compare_slot_${motor.id}"),
        shape = RoundedCornerShape(22.dp),
        elevation = 4.dp,
        onClick = onChangeClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = slotLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (canRemove) {
                    IconButton(
                        onClick = onRemoveClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hapus slot EV",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Ganti motor",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                MotorStudioImage(motor = motor, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = motor.brand,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Text(
                text = motor.modelName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatRupiahShort(motor.priceIdr),
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DuelWinnerSummaryCard(motors: List<ElectricMotorEntity>) {
    val mostEfficient = motors.minByOrNull { it.whPerKm }!!
    val cheapest = motors.minByOrNull { it.priceIdr }!!
    val strongestClimb = motors.maxWithOrNull(
        compareBy<ElectricMotorEntity> { it.hillClimbDegrees }.thenBy { it.motorPowerWatt }
    )!!
    val bestWarranty = motors.maxByOrNull { it.warrantyYears }!!
    val longestRange = motors.maxByOrNull { it.rangeKm }!!

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("duel_winner_summary"),
        shape = RoundedCornerShape(24.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = ElectricAmber,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ringkasan Pemenang Duel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            WinnerRow(
                icon = Icons.Rounded.Eco,
                badge = "Paling Irit",
                winnerName = "${mostEfficient.brand} ${mostEfficient.modelName}",
                detail = "${String.format("%.1f", mostEfficient.whPerKm)} Wh/km (~Rp ${mostEfficient.costPerKmIdr}/km)",
                tint = ElectricEmerald
            )
            WinnerRow(
                icon = Icons.Rounded.Payments,
                badge = "Paling Murah",
                winnerName = "${cheapest.brand} ${cheapest.modelName}",
                detail = formatRupiahFull(cheapest.priceIdr),
                tint = MaterialTheme.colorScheme.primary
            )
            WinnerRow(
                icon = Icons.Rounded.Terrain,
                badge = "Kuat Tanjakan",
                winnerName = "${strongestClimb.brand} ${strongestClimb.modelName}",
                detail = "${strongestClimb.hillClimbGrade} • ${strongestClimb.motorPowerWatt}W",
                tint = ElectricAmber
            )
            WinnerRow(
                icon = Icons.Rounded.Shield,
                badge = "Garansi Unggul",
                winnerName = "${bestWarranty.brand} ${bestWarranty.modelName}",
                detail = bestWarranty.warrantyInfo,
                tint = MaterialTheme.colorScheme.secondary
            )
            WinnerRow(
                icon = Icons.Rounded.Route,
                badge = "Jarak Terjauh",
                winnerName = "${longestRange.brand} ${longestRange.modelName}",
                detail = "${longestRange.rangeKm} km per pengisian",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WinnerRow(
    icon: ImageVector,
    badge: String,
    winnerName: String,
    detail: String,
    tint: Color
) {
    val tokens = LocalGlassTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (tokens.isDark) Color(0x1CFFFFFF) else Color(0x66FFFFFF))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = badge,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = tint,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = winnerName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DuelComparisonMatrixCard(motors: List<ElectricMotorEntity>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Matriks Spesifikasi Head-to-Head",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            MetricBarComparison(
                title = "Harga OTR (Lebih Murah Lebih Baik)",
                motors = motors,
                valueProvider = { formatRupiahShort(it.priceIdr) },
                scoreProvider = { m ->
                    val maxP = motors.maxOf { it.priceIdr }.coerceAtLeast(1L).toFloat()
                    (1f - (m.priceIdr / maxP) * 0.65f).coerceIn(0.2f, 1f)
                },
                winnerId = motors.minByOrNull { it.priceIdr }?.id
            )

            MetricBarComparison(
                title = "Efisiensi Energi / Keiritan (Wh/km)",
                motors = motors,
                valueProvider = { "${String.format("%.1f", it.whPerKm)} Wh/km (Rp ${it.costPerKmIdr})" },
                scoreProvider = { m ->
                    val minW = motors.minOf { it.whPerKm }.coerceAtLeast(1.0)
                    (minW / m.whPerKm).toFloat().coerceIn(0.25f, 1f)
                },
                winnerId = motors.minByOrNull { it.whPerKm }?.id
            )

            MetricBarComparison(
                title = "Kemampuan Tanjakan & Daya Motor",
                motors = motors,
                valueProvider = { "${it.hillClimbDegrees}° • ${it.motorPowerWatt}W" },
                scoreProvider = { m ->
                    val maxD = motors.maxOf { it.hillClimbDegrees }.coerceAtLeast(1).toFloat()
                    (m.hillClimbDegrees / maxD).coerceIn(0.2f, 1f)
                },
                winnerId = motors.maxWithOrNull(
                    compareBy<ElectricMotorEntity> { it.hillClimbDegrees }.thenBy { it.motorPowerWatt }
                )?.id
            )

            MetricBarComparison(
                title = "Jarak Tempuh Maksimal",
                motors = motors,
                valueProvider = { "${it.rangeKm} km" },
                scoreProvider = { m ->
                    val maxR = motors.maxOf { it.rangeKm }.coerceAtLeast(1).toFloat()
                    (m.rangeKm / maxR).coerceIn(0.2f, 1f)
                },
                winnerId = motors.maxByOrNull { it.rangeKm }?.id
            )

            MetricBarComparison(
                title = "Top Speed",
                motors = motors,
                valueProvider = { "${it.topSpeedKmh} km/jam" },
                scoreProvider = { m ->
                    val maxS = motors.maxOf { it.topSpeedKmh }.coerceAtLeast(1).toFloat()
                    (m.topSpeedKmh / maxS).coerceIn(0.2f, 1f)
                },
                winnerId = motors.maxByOrNull { it.topSpeedKmh }?.id
            )

            MetricBarComparison(
                title = "Lama Garansi & Perlindungan",
                motors = motors,
                valueProvider = { "${it.warrantyYears} Thn • ${if (it.isSwappable) "Swap" else "Cas"}" },
                scoreProvider = { m ->
                    val maxY = motors.maxOf { it.warrantyYears }.coerceAtLeast(1).toFloat()
                    (m.warrantyYears / maxY).coerceIn(0.2f, 1f)
                },
                winnerId = motors.maxByOrNull { it.warrantyYears }?.id
            )

            // Detail teks garansi masing-masing EV
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Rincian Garansi Resmi",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                motors.forEach { m ->
                    Text(
                        text = "• ${m.brand} ${m.modelName}: ${m.warrantyInfo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBarComparison(
    title: String,
    motors: List<ElectricMotorEntity>,
    valueProvider: (ElectricMotorEntity) -> String,
    scoreProvider: (ElectricMotorEntity) -> Float,
    winnerId: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        motors.forEach { m ->
            val isWinner = m.id == winnerId
            val progress = scoreProvider(m)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWinner) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Unggul",
                                tint = ElectricEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${m.brand} ${m.modelName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isWinner) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = valueProvider(m),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isWinner) ElectricEmerald else MaterialTheme.colorScheme.onSurface
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (isWinner) ElectricEmerald else MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MotorPickerSheet(
    motors: List<ElectricMotorEntity>,
    slotLabel: String,
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
            Text(
                text = slotLabel,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
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
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${motor.motorPowerWatt}W • ${motor.rangeKm} km • Garansi ${motor.warrantyYears} Thn",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = formatRupiahShort(motor.priceIdr),
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}
