package com.witte.lozify.presentation.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.witte.lozify.R

/**
 * Google Fonts Provider configuration for downloadable fonts.
 */
val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val NunitoFont = GoogleFont("Nunito")

val NunitoFontFamily = FontFamily(
    Font(googleFont = NunitoFont, fontProvider = GoogleFontProvider, weight = FontWeight.Black),
    Font(googleFont = NunitoFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = NunitoFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal)
)

/**
 * LozifyLogo - Anti-distortion rounded brand logo component (Flomo-styled).
 *
 * Features:
 * 1. Font: Google Font "Nunito" with extra heavy weight (FontWeight.Black).
 * 2. Physical Size Lock: Uses `with(LocalDensity.current) { sizeDp.toSp() }`
 *    to prevent system font-scaling distortion while maintaining crisp rendering.
 * 3. Color & Kerning: Deep charcoal (#111111) with tightened letter spacing (-0.5sp).
 *
 * @param modifier Optional modifier
 * @param sizeDp Target physical logo size in DP (default 24.dp)
 * @param color Text color (default #111111)
 */
@Composable
fun LozifyLogo(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 24.dp,
    color: Color = Color(0xFF111111)
) {
    // 强制将 dp 转换为固定物理像素换算的 sp，避免系统字体缩放撑爆 Logo 布局
    val fixedFontSize = with(LocalDensity.current) {
        sizeDp.toSp()
    }

    Text(
        text = "Lozify",
        modifier = modifier,
        fontFamily = NunitoFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = fixedFontSize,
        color = color,
        letterSpacing = (-0.5).sp
    )
}
