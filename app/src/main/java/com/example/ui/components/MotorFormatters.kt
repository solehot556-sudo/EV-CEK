package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ElectricMotorEntity
import com.example.ui.theme.LocalGlassTokens
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val idLocale = Locale("in", "ID")

fun formatRupiahFull(amount: Long): String {
    val formatter = NumberFormat.getNumberInstance(idLocale)
    return "Rp ${formatter.format(amount)}"
}

fun formatRupiahShort(amount: Long): String {
    val millions = amount / 1_000_000.0
    return if (millions % 1.0 == 0.0) {
        "Rp ${millions.toInt()} Jt"
    } else {
        String.format(idLocale, "Rp %.1f Jt", millions)
    }
}

fun formatTimestamp(ts: Long): String {
    if (ts <= 0L) return "Belum pernah"
    val sdf = SimpleDateFormat("dd MMM HH:mm", idLocale)
    return sdf.format(Date(ts))
}

@DrawableRes
fun fallbackDrawableForType(vehicleType: String): Int {
    return when {
        vehicleType.contains("Sepeda", ignoreCase = true) -> R.drawable.img_ev_ebike_studio
        vehicleType.contains("Maxi", ignoreCase = true) ||
            vehicleType.contains("Sport", ignoreCase = true) -> R.drawable.img_ev_maxi_sport
        else -> R.drawable.img_ev_scooter_studio
    }
}

@Composable
fun MotorStudioImage(
    motor: ElectricMotorEntity,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val fallbackRes = fallbackDrawableForType(motor.vehicleType)
    val tokens = LocalGlassTokens.current

    Box(
        modifier = modifier.background(
            tokens.glassFillBottom.copy(alpha = 0.7f)
        )
    ) {
        if (!motor.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = motor.imageUrl,
                contentDescription = "${motor.brand} ${motor.modelName}",
                placeholder = painterResource(id = fallbackRes),
                error = painterResource(id = fallbackRes),
                fallback = painterResource(id = fallbackRes),
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = fallbackRes),
                contentDescription = "${motor.brand} ${motor.modelName}",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
