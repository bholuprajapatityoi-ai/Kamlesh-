package com.example.presentation.screens.v2

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.presentation.components.GoluGlassCard
import com.example.presentation.components.GoluLogoBadge
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluIndigoAccent
import com.example.ui.theme.GoluNeonCyan

data class Golu2TileSpec(
    val id: String,
    val title: String,
    val subtitle: String,
    val categoryLabel: String,
    val route: String,
    val icon: ImageVector,
    val tint: Color,
)

val golu2AllTiles = listOf(
    // Vision & Perception
    Golu2TileSpec("vision", "AI Vision", "Screen Understanding", "Vision & AI", "v2_vision/0", Icons.Filled.ScreenSearchDesktop, GoluNeonCyan),
    Golu2TileSpec("camera", "Camera AI", "Live Scene & OCR", "Vision & AI", "v2_vision/1", Icons.Filled.CameraAlt, GoluElectricBlue),
    Golu2TileSpec("documents", "Document Intelligence", "PDF & Q&A Index", "Vision & AI", "v2_vision/2", Icons.Filled.Description, GoluIndigoAccent),
    Golu2TileSpec("notifications", "Smart Notifications", "Priority & Summary", "Vision & AI", "v2_vision/3", Icons.Filled.NotificationsActive, GoluAmber),
    // Brain & Knowledge
    Golu2TileSpec("knowledge", "Knowledge Brain", "Unified Notes & Memory", "Knowledge", "v2_brain/0", Icons.Filled.Psychology, GoluNeonCyan),
    Golu2TileSpec("translator", "Translator", "Live Voice & Text", "Knowledge", "v2_brain/1", Icons.Filled.Translate, GoluEmerald),
    Golu2TileSpec("research", "Deep Research", "Citations & Facts", "Knowledge", "v2_brain/2", Icons.Filled.TravelExplore, GoluElectricBlue),
    Golu2TileSpec("offline_brain", "Offline Brain", "Math, Units & Phrases", "Knowledge", "v2_brain/3", Icons.Filled.OfflineBolt, GoluAmber),
    // Productivity & Creative
    Golu2TileSpec("planner", "Life Planner", "Approve Daily Plans", "Productivity", "v2_productivity/0", Icons.Filled.CalendarMonth, GoluEmerald),
    Golu2TileSpec("coding", "Coding Assistant", "Code, Debug & Review", "Productivity", "v2_productivity/1", Icons.Filled.Code, GoluNeonCyan),
    Golu2TileSpec("creative", "Creative Studio", "AI Art & Concepts", "Productivity", "v2_productivity/2", Icons.Filled.Brush, GoluIndigoAccent),
    Golu2TileSpec("recorder", "Voice Recorder", "Audio & Summaries", "Productivity", "v2_productivity/3", Icons.Filled.Mic, Color(0xFFEC4899)),
    // Smart Environment & Personalization
    Golu2TileSpec("smart_home", "Smart Home", "Authorized Hub Control", "Environment", "v2_environment/0", Icons.Filled.Home, GoluNeonCyan),
    Golu2TileSpec("expenses", "Expense Tracker", "Budgets, Charts & CSV", "Environment", "v2_environment/1", Icons.Filled.AccountBalanceWallet, GoluEmerald),
    Golu2TileSpec("personality", "Custom Personality", "Humor, Length & Style", "Environment", "v2_environment/2", Icons.Filled.Face, GoluElectricBlue),
    Golu2TileSpec("smart_apps", "App Intelligence", "Launch & Shortcuts", "Environment", "v2_environment/3", Icons.Filled.Apps, GoluAmber),
    // Learning, Mobility & System
    Golu2TileSpec("study", "Study Tutor", "Hindi/English Mastery", "Mobility & System", "v2_mobility/0", Icons.Filled.School, GoluIndigoAccent),
    Golu2TileSpec("travel", "Travel Intelligence", "Places & Itineraries", "Mobility & System", "v2_mobility/1", Icons.Filled.Explore, GoluNeonCyan),
    Golu2TileSpec("multi_device", "Multi-Device", "Account Sync & Devices", "Mobility & System", "v2_mobility/2", Icons.Filled.Devices, GoluElectricBlue),
    Golu2TileSpec("diagnostics", "System Diagnostics", "Real Health & Checks", "Mobility & System", "v2_mobility/3", Icons.Filled.HealthAndSafety, GoluEmerald),
)

@Composable
fun Golu2DashboardScreen(
    onNavigateRoute: (String) -> Unit,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("golu2_dashboard_list"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Golu2HeaderBar(
                    title = "GOLU 2.0 Hub",
                    subtitle = "20 Integrated AI & System Modules",
                    onBack = onBack,
                )

                GoluGlassCard(accentBorder = GoluNeonCyan.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GoluLogoBadge()
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = GoluNeonCyan.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, GoluNeonCyan.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoluNeonCyan, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("v2.0 READY", style = MaterialTheme.typography.labelSmall, color = GoluNeonCyan)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "All core Boss workflows preserved. Tap any GOLU 2.0 capability below to launch it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                golu2AllTiles.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        pair.forEach { tile ->
                            Golu2DashboardTile(
                                tile = tile,
                                onClick = { onNavigateRoute(tile.route) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Golu2DashboardTile(
    tile: Golu2TileSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, tile.tint.copy(alpha = 0.42f)),
        modifier = modifier.height(92.dp).testTag("v2_tile_${tile.id}"),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(tile.tint.copy(alpha = 0.28f), tile.tint.copy(alpha = 0.10f))
                            )
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.title,
                    tint = tile.tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tile.categoryLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = tile.tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tile.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tile.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
