package com.yourpax.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val subtleText: Color,
    val terminalBackground: Color,
    val terminalText: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val conflictBackground: Color,
    val conflictText: Color,
    val badgeWpa2: Color,
    val badgeOpen: Color,
    val badgeWps: Color,
    val badgeWpa3: Color,
    val badgeEnterprise: Color,
    val categoryHandshake: Color,
    val categoryPmkid: Color,
    val categoryEvilCreds: Color,
    val categoryWps: Color,
    val categoryCracked: Color,
    val categoryStolen: Color,
    val categoryScan: Color,
    val categoryVuln: Color,
    val categoryZombie: Color,
    val categoryNetkb: Color,
    val categoryPortal: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
)

@Composable
fun rememberAppColors(): AppColors {
    val isDark = isSystemInDarkTheme()
    return if (isDark) DarkAppColors else LightAppColors
}

private val LightAppColors = AppColors(
    success = SuccessLight,
    successContainer = SuccessContainerLight,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    info = InfoLight,
    infoContainer = InfoContainerLight,
    subtleText = SubtleTextLight,
    terminalBackground = TerminalBackgroundLight,
    terminalText = TerminalTextLight,
    gradientStart = GradientStartLight,
    gradientEnd = GradientEndLight,
    conflictBackground = ConflictBackground,
    conflictText = ConflictText,
    badgeWpa2 = BadgeWpa2,
    badgeOpen = BadgeOpen,
    badgeWps = BadgeWps,
    badgeWpa3 = BadgeWpa3,
    badgeEnterprise = BadgeEnterprise,
    categoryHandshake = CategoryHandshake,
    categoryPmkid = CategoryPmkid,
    categoryEvilCreds = CategoryEvilCreds,
    categoryWps = CategoryWps,
    categoryCracked = CategoryCracked,
    categoryStolen = CategoryStolen,
    categoryScan = CategoryScan,
    categoryVuln = CategoryVuln,
    categoryZombie = CategoryZombie,
    categoryNetkb = CategoryNetkb,
    categoryPortal = CategoryPortal,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
)

private val DarkAppColors = AppColors(
    success = SuccessDark,
    successContainer = SuccessContainerDark,
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    info = InfoDark,
    infoContainer = InfoContainerDark,
    subtleText = SubtleTextDark,
    terminalBackground = TerminalBackgroundDark,
    terminalText = TerminalTextDark,
    gradientStart = GradientStartDark,
    gradientEnd = GradientEndDark,
    conflictBackground = ConflictBackground,
    conflictText = ConflictText,
    badgeWpa2 = BadgeWpa2,
    badgeOpen = BadgeOpen,
    badgeWps = BadgeWps,
    badgeWpa3 = BadgeWpa3,
    badgeEnterprise = BadgeEnterprise,
    categoryHandshake = CategoryHandshake,
    categoryPmkid = CategoryPmkid,
    categoryEvilCreds = CategoryEvilCreds,
    categoryWps = CategoryWps,
    categoryCracked = CategoryCracked,
    categoryStolen = CategoryStolen,
    categoryScan = CategoryScan,
    categoryVuln = CategoryVuln,
    categoryZombie = CategoryZombie,
    categoryNetkb = CategoryNetkb,
    categoryPortal = CategoryPortal,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
)
