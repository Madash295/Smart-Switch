package com.madash.smartswitch

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen() {

    Surface(

        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),

        color = MaterialTheme.colorScheme.background // 👈 dynamic background color
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f).padding(16.dp)
            ) {
                /* -------- App name row -------- */
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dynamic = LocalDynamicColour.current
                    val primary = MaterialTheme.colorScheme.primary
                    val secondary = MaterialTheme.colorScheme.primaryContainer
                    val iconTint = if (dynamic) primary else Color.Unspecified
                    val iconTint2 = if (dynamic) secondary else Color.Unspecified
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = primary, // ← always use primary color
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.whitearr),
                            contentDescription = null,
                            tint = iconTint2, // ← dynamic = primary tint, else original
                            modifier = Modifier.size(50.dp)
                        )
                    }

                    Text(
                        "Smart Transfer",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                StorageCard()
                Spacer(Modifier.height(24.dp))
                FeatureGrid()
                Spacer(Modifier.height(20.dp))
                NativeAdBlock()
                Spacer(Modifier.weight(1f))

            }

            SmartBottomBar()
        }
    }
}
/* ───────── Storage card ───────── */
// Helper to choose the storage card background
private fun storageCardColor(dynamic: Boolean, c: ColorScheme): Color =
    if (dynamic) c.primaryContainer else c.surfaceVariant

@Composable
fun StorageCard() {
    val context = LocalContext.current
    // Load real storage in a blocking recall (you can swap to produceState if needed)
    val storageInfo = remember {
        StorageUtils.getStorageInfo(context) // returns totalGB, usedGB, usedPercentage
    }

    // Animate the progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = storageInfo.usedPercentage,
        animationSpec = tween(durationMillis = 1000),
        label = "storageProgress"
    )

    // Theme‑aware text colors:
    val textColor    = MaterialTheme.colorScheme.onSurface
    val subTextColor = textColor.copy(alpha = 0.7f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = storageCardColor(
                dynamic = LocalDynamicColour.current,
                c       = MaterialTheme.colorScheme
            )
        )
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = rememberVectorPainter(image = ImageVector.vectorResource(R.drawable.phone)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Device Storage",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = textColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${storageInfo.totalGB} GB total",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${storageInfo.usedGB} GB used",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${storageInfo.usedGB} GB",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = textColor
                )
                Text(
                    "used",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor
                )
            }
        }
    }
}

/* ───────── Feature grid 2×2 ───────── */
@Composable
private fun FeatureGrid() {
    val dynamic = LocalDynamicColour.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FeatureCard(
            modifier = Modifier.weight(1f),
            title = "File Transfer",
            subtitle = "Send & Receive Data",
            icon = ImageVector.vectorResource(R.drawable.green_arrow),
            gradientColors = if (dynamic) null else listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))
        )
        FeatureCard(
            modifier = Modifier.weight(1f),
            title = "Phone Clone",
            subtitle = "Transfer to new phone",
            icon = ImageVector.vectorResource(R.drawable.clone),
            gradientColors = if (dynamic) null else listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
        )
    }

    Spacer(Modifier.height(16.dp))

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FeatureCard(
            modifier = Modifier.weight(1f),
            title = "Mobile to PC",
            subtitle = "Share files with laptop",
            icon = ImageVector.vectorResource(R.drawable.pc),
            gradientColors = if (dynamic) null else listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF))
        )
        FeatureCard(
            modifier = Modifier.weight(1f),
            title = "AI Assistant",
            subtitle = "Smart data transfer",
            icon = ImageVector.vectorResource(R.drawable.ai_asistant),
            gradientColors = if (dynamic) null else listOf(Color(0xFFFDF2F8), Color(0xFFFCE7F3))
        )
    }
}

/* ───────── One feature card ───────── */
@Composable
fun FeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>?
) {
    val dynamic    = LocalDynamicColour.current
    val iconColor  = if (dynamic) MaterialTheme.colorScheme.primary else Color.Unspecified
    val textColor  = MaterialTheme.colorScheme.onBackground
    val subColor   = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)

    Card(
        modifier = modifier.height(120.dp),
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = featureColor(dynamic, MaterialTheme.colorScheme)
        )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp).let {
                        if (gradientColors != null) {
                            it.background(
                                brush = Brush.linearGradient(gradientColors),
                                shape = RoundedCornerShape(24.dp))
                        } else it
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(7.dp))
            Text(title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Spacer(Modifier.height(4.dp))
            Text(subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                lineHeight = 14.sp
            )
        }
    }
}


/* ───────── Bottom nav bar ───────── */
@Composable
private fun SmartBottomBar() {
    val dynamic   = LocalDynamicColour.current
    val barColor  = bottomBarColor(dynamic, MaterialTheme.colorScheme)

    NavigationBar(
        containerColor = barColor,     // 👈 adaptive
        tonalElevation = 3.dp
    ) {
        NavItem(Icons.Default.Home,       "Home",     true )
        NavItem(Icons.Default.SwapHoriz,  "Transfer", false)
        NavItem(Icons.Default.History,    "Files",    false)
        NavItem(Icons.Default.Person,     "Profile",  false)
    }
}


@Composable
private fun RowScope.NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean
) = NavigationBarItem(
    selected = selected,
    onClick  = { /* TODO */ },
    icon     = { Icon(icon, contentDescription = label) },
    label    = { Text(label) },
    colors   = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = Color.Transparent
    )
)




/* adaptive container colours */

private fun featureColor   (dyn: Boolean, c: ColorScheme) = if (dyn) c.secondaryContainer   else c.surfaceVariant
private fun bottomBarColor (dyn: Boolean, c: ColorScheme) = if (dyn) c.background    else c.surfaceVariant
private fun iconBgColor(dynamic: Boolean, c: ColorScheme): Color =
    if (dynamic) c.primaryContainer else Color(0xFFE5F5FF)

/* ───────── Previews ───────── */
@Preview(name = "Light", showBackground = true, showSystemUi = true)
@Composable fun LightPreview() =
    SmartSwitchTheme(useDarkTheme = false, dynamicColour = false) { MainScreen() }

@Preview(name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, showBackground = true,
    showSystemUi = true
)
@Composable fun DarkPreview()  =
    SmartSwitchTheme(useDarkTheme = true , dynamicColour = false) { MainScreen() }

@Preview(name = "Dynamic Light", showBackground = true, showSystemUi = false)
@Composable fun DynamicLightPreview() =
    SmartSwitchTheme(useDarkTheme = false, dynamicColour = true) { MainScreen() }

@Preview(name = "Dynamic Dark", showBackground = true, showSystemUi = true)
@Composable fun DynamicDarkPreview() =
    SmartSwitchTheme(useDarkTheme = true, dynamicColour = true) { MainScreen() }

@Composable
private fun NativeAdBlock() {


    val c = MaterialTheme.colorScheme
    val cardBg      = c.surface
    val borderCol   = c.outlineVariant
    val skeletonCol = c.surfaceVariant
    val textCol     = c.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderCol),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {

            /* --- top skeleton row --- */
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(skeletonCol, RoundedCornerShape(6.dp))
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .height(10.dp)
                            .fillMaxWidth(0.7f)
                            .background(skeletonCol, RoundedCornerShape(4.dp))
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .height(10.dp)
                            .fillMaxWidth(0.5f)
                            .background(skeletonCol, RoundedCornerShape(4.dp))
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            /* --- additional skeleton lines --- */
            Box(
                Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.9f)
                    .background(skeletonCol, RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .height(10.dp)
                    .fillMaxWidth(0.6f)
                    .background(skeletonCol, RoundedCornerShape(4.dp))
            )

            Spacer(Modifier.height(12.dp))

            /* --- ad rectangle --- */
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(skeletonCol, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Native AD",
                    color = textCol,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

