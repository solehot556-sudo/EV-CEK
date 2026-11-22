package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.ElectricMoped
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ElectricMoped
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncState
import com.example.data.model.SyncStatusUi
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.LocalGlassTokens
import com.example.ui.viewmodel.AppTab

/**
 * Latar belakang atmosferik bergaya iOS Liquid Glass dengan pendar cahaya lembut.
 */
@Composable
fun GlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        tokens.backdropTop,
                        tokens.backdropMid,
                        tokens.backdropBottom
                    )
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.accentGlow, Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.14f),
                        radius = size.width * 0.72f
                    ),
                    radius = size.width * 0.72f,
                    center = Offset(size.width * 0.18f, size.height * 0.14f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tokens.secondaryGlow, Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.56f),
                        radius = size.width * 0.65f
                    ),
                    radius = size.width * 0.65f,
                    center = Offset(size.width * 0.88f, size.height * 0.56f)
                )
            },
        content = content
    )
}

/**
 * Kontainer Liquid Glass khas iPhone dengan gradien transparan berlapis & tepian spekular.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevation: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalGlassTokens.current
    val fillBrush = Brush.verticalGradient(
        colors = listOf(tokens.glassFillTop, tokens.glassFillBottom)
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(tokens.specularBorderTop, tokens.specularBorderBottom)
    )

    val interactiveModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = ripple(),
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = if (tokens.isDark) Color.Black else Color(0x2412263F),
                spotColor = if (tokens.isDark) Color.Black else Color(0x2812263F)
            )
            .clip(shape)
            .background(fillBrush)
            .border(BorderStroke(1.dp, borderBrush), shape)
            .then(interactiveModifier),
        content = content
    )
}

/**
 * Seluruh Tab Control di sisi kanan layar bergaya iPhone Duo / iOS Liquid Glass Capsule.
 * Menggunakan kaca ultra-transparan (see-through crystal glass) agar konten layar penuh
 * di belakangnya tetap terlihat jelas tanpa mengurangi ruang tampilan, dilengkapi indikator
 * tetesan kaca animasi pegas saat berpindah tab.
 */
@Composable
fun RightSideGlassTabRail(
    selectedTab: AppTab,
    isDark: Boolean,
    compareCount: Int,
    onSelectTab: (AppTab) -> Unit,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalGlassTokens.current
    val railShape = RoundedCornerShape(30.dp)
    val outerRimBrush = Brush.linearGradient(
        colors = listOf(
            tokens.specularBorderTop,
            tokens.specularBorderBottom,
            tokens.specularBorderTop.copy(alpha = 0.55f)
        )
    )
    val crystalGlassBrush = Brush.verticalGradient(
        colors = listOf(
            if (tokens.isDark) Color(0x4D1E2E4A) else Color(0x66FFFFFF),
            tokens.glassRailFill,
            if (tokens.isDark) Color(0x3D0B1324) else Color(0x47E6F0FA)
        )
    )

    val itemHeight = 46.dp
    val itemSpacing = 4.dp
    val selectedIndex = AppTab.entries.indexOf(selectedTab).coerceAtLeast(0)
    val indicatorOffsetY by animateDpAsState(
        targetValue = (itemHeight + itemSpacing) * selectedIndex,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glassDropletOffset"
    )

    Column(
        modifier = modifier
            .width(58.dp)
            .shadow(
                elevation = 14.dp,
                shape = railShape,
                ambientColor = if (tokens.isDark) Color(0x88000000) else Color(0x28102542),
                spotColor = if (tokens.isDark) Color(0x88000000) else Color(0x28102542)
            )
            .clip(railShape)
            .background(crystalGlassBrush)
            .border(BorderStroke(1.2.dp, outerRimBrush), railShape)
            .drawBehind {
                // Kilau spekular halus di tepi atas kapsul kaca (efek refraksi iOS Liquid Glass)
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = if (tokens.isDark) 0.38f else 0.75f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(12f, 2f),
                    end = Offset(size.width - 12f, 2f),
                    strokeWidth = 2.5f
                )
            }
            .padding(vertical = 8.dp, horizontal = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Tombol cepat Mode Gelap Elegan
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                )
                .border(
                    width = 0.9.dp,
                    color = tokens.specularBorderTop.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onToggleDarkMode
                )
                .testTag("quick_dark_mode_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = if (isDark) "Aktifkan Mode Terang" else "Aktifkan Mode Gelap Elegan",
                tint = if (isDark) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(19.dp)
            )
        }

        Box(
            modifier = Modifier
                .width(30.dp)
                .height(1.dp)
                .background(tokens.subtleDivider)
        )

        // Container untuk 4 tab dengan indikator Liquid Glass meluncur mulus
        Box(modifier = Modifier.fillMaxWidth()) {
            // Sliding Liquid Glass Droplet Indicator
            Box(
                modifier = Modifier
                    .offset(y = indicatorOffsetY)
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (tokens.isDark) 0.34f else 0.28f),
                                MaterialTheme.colorScheme.primary.copy(alpha = if (tokens.isDark) 0.16f else 0.12f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                tokens.specularBorderTop.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(itemSpacing)
            ) {
                AppTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 1f,
                        animationSpec = spring(
                            dampingRatio = 0.68f,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "tabScale"
                    )
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                        animationSpec = tween(220),
                        label = "tabIconTint"
                    )

                    val iconVector = when (tab) {
                        AppTab.CATALOG -> if (isSelected) Icons.Rounded.ElectricMoped else Icons.Outlined.ElectricMoped
                        AppTab.COMPARE -> if (isSelected) Icons.Rounded.CompareArrows else Icons.Outlined.CompareArrows
                        AppTab.CALCULATOR -> if (isSelected) Icons.Rounded.Calculate else Icons.Outlined.Calculate
                        AppTab.AI_ADVISOR -> if (isSelected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
                        AppTab.SETTINGS -> if (isSelected) Icons.Rounded.Tune else Icons.Outlined.Tune
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .minimumInteractiveComponentSize()
                            .scale(scale)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                onClick = { onSelectTab(tab) }
                            )
                            .padding(vertical = 5.dp, horizontal = 2.dp)
                            .testTag("right_tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = tab.label,
                                tint = iconTint,
                                modifier = Modifier.size(21.dp)
                            )
                            if (tab == AppTab.COMPARE && compareCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(13.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = compareCount.toString(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            color = iconTint,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pill status sinkronisasi real-time & deteksi koneksi internet.
 */
@Composable
fun SyncStatusGlassPill(
    syncStatus: SyncStatusUi,
    onManualSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "syncSpin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val (dotColor, statusText, icon) = when (syncStatus.state) {
        SyncState.SYNCING -> Triple(
            MaterialTheme.colorScheme.secondary,
            "Menyinkronkan...",
            Icons.Rounded.Sync
        )
        SyncState.SYNCED -> Triple(
            ElectricEmerald,
            "Live • ${syncStatus.totalCachedModels} EV",
            Icons.Rounded.CloudDone
        )
        SyncState.OFFLINE -> Triple(
            ElectricAmber,
            "Offline • ${syncStatus.totalCachedModels} EV",
            Icons.Rounded.CloudOff
        )
        SyncState.ERROR -> Triple(
            ElectricAmber,
            "Cache Lokal • ${syncStatus.totalCachedModels} EV",
            Icons.Rounded.CloudDone
        )
        SyncState.IDLE -> Triple(
            if (syncStatus.isOnline) ElectricEmerald else ElectricAmber,
            "${syncStatus.totalCachedModels} EV Lokal",
            if (syncStatus.isOnline) Icons.Rounded.CloudDone else Icons.Rounded.CloudOff
        )
    }

    GlassCard(
        modifier = modifier.testTag("sync_status_pill"),
        shape = RoundedCornerShape(50),
        elevation = 2.dp,
        onClick = onManualSync
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = icon,
                contentDescription = statusText,
                tint = dotColor,
                modifier = Modifier
                    .size(15.dp)
                    .then(
                        if (syncStatus.state == SyncState.SYNCING) Modifier.rotate(spinAngle)
                        else Modifier
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
