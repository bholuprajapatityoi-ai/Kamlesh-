package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.BackendDiagnostics
import com.example.domain.model.ConnectionPhase
import com.example.ui.theme.GoluAmber
import com.example.ui.theme.GoluCoralRed
import com.example.ui.theme.GoluElectricBlue
import com.example.ui.theme.GoluEmerald
import com.example.ui.theme.GoluNeonCyan

@Composable
fun GoluLogoBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(GoluElectricBlue, GoluNeonCyan))
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "G",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF050B18),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "GOLU",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "BOSS AI ASSISTANT",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
fun GoluGlassCard(
    modifier: Modifier = Modifier,
    accentBorder: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.88f)
            ),
        border = BorderStroke(1.dp, accentBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = tint.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = tint,
            )
        }
    }
}

@Composable
fun BackendStatusBanner(
    diagnostics: BackendDiagnostics,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint =
        when (diagnostics.connectionPhase) {
            ConnectionPhase.CONNECTED -> GoluEmerald
            ConnectionPhase.CONNECTING -> GoluNeonCyan
            ConnectionPhase.OFFLINE,
            ConnectionPhase.NOT_CONFIGURED -> GoluAmber
            ConnectionPhase.ERROR -> GoluCoralRed
        }
    val icon =
        when (diagnostics.connectionPhase) {
            ConnectionPhase.CONNECTED -> Icons.Filled.CloudDone
            ConnectionPhase.CONNECTING -> Icons.Filled.CloudSync
            else -> Icons.Filled.CloudOff
        }
    Surface(
        onClick = onActionClick,
        modifier = modifier.fillMaxWidth().border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = tint.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = diagnostics.statusSummary,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = diagnostics.statusSummary,
                    style = MaterialTheme.typography.labelLarge,
                    color = tint,
                )
                if (diagnostics.rootCauseDetail.isNotBlank()) {
                    Text(
                        text = diagnostics.rootCauseDetail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Composable
fun AutomaticBackendConnectionCard(
    diagnostics: BackendDiagnostics,
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusTint =
        when (diagnostics.connectionPhase) {
            ConnectionPhase.CONNECTED -> GoluEmerald
            ConnectionPhase.CONNECTING -> GoluNeonCyan
            ConnectionPhase.OFFLINE,
            ConnectionPhase.NOT_CONFIGURED -> GoluAmber
            ConnectionPhase.ERROR -> GoluCoralRed
        }
    val statusIcon =
        when (diagnostics.connectionPhase) {
            ConnectionPhase.CONNECTED -> Icons.Filled.CloudDone
            ConnectionPhase.CONNECTING -> Icons.Filled.CloudSync
            else -> Icons.Filled.CloudOff
        }

    GoluGlassCard(
        modifier = modifier.testTag("automatic_backend_card"),
        accentBorder = statusTint.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudSync,
                    contentDescription = "Automatic Backend Connection",
                    tint = GoluNeonCyan,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Automatic Backend Connection",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            StatusPill(
                label = diagnostics.connectionPhase.label,
                icon = statusIcon,
                tint = statusTint,
                modifier = Modifier.testTag("backend_status_pill"),
            )
        }

        Spacer(Modifier.height(12.dp))
        BackendStatusBanner(
            diagnostics = diagnostics,
            onActionClick = onRetryConnection,
        )

        Spacer(Modifier.height(12.dp))
        DiagnosticMetricRow(
            label = "API Health",
            value =
                when {
                    diagnostics.connectionPhase == ConnectionPhase.CONNECTING -> "Checking..."
                    diagnostics.isBackendReachable -> "Healthy"
                    else -> "Unreachable"
                },
            valueTint =
                when {
                    diagnostics.isBackendReachable -> GoluEmerald
                    diagnostics.connectionPhase == ConnectionPhase.CONNECTING -> GoluNeonCyan
                    else -> GoluAmber
                },
        )
        Spacer(Modifier.height(6.dp))
        DiagnosticMetricRow(
            label = "Database Health",
            value =
                when {
                    diagnostics.connectionPhase == ConnectionPhase.CONNECTING -> "Checking..."
                    diagnostics.isDatabaseConnected -> "PostgreSQL Connected"
                    else -> "Unavailable"
                },
            valueTint =
                when {
                    diagnostics.isDatabaseConnected -> GoluEmerald
                    diagnostics.connectionPhase == ConnectionPhase.CONNECTING -> GoluNeonCyan
                    else -> GoluCoralRed
                },
        )
        Spacer(Modifier.height(6.dp))
        DiagnosticMetricRow(
            label = "Last Connected",
            value = diagnostics.lastConnectedAt.ifBlank { "Not yet connected" },
            valueTint = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRetryConnection,
            enabled = diagnostics.connectionPhase != ConnectionPhase.CONNECTING,
            modifier = Modifier.fillMaxWidth().testTag("retry_connection_button"),
            shape = RoundedCornerShape(14.dp),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = GoluElectricBlue,
                    contentColor = Color.White,
                ),
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Retry Connection")
            Spacer(Modifier.width(8.dp))
            Text(
                text =
                    if (diagnostics.connectionPhase == ConnectionPhase.CONNECTING) "Connecting..."
                    else "Retry Connection",
            )
        }
    }
}

@Composable
private fun DiagnosticMetricRow(
    label: String,
    value: String,
    valueTint: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueTint,
        )
    }
}
