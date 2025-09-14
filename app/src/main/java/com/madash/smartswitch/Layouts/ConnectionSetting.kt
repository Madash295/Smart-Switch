package com.madash.smartswitch.Layouts

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.madash.smartswitch.util.ConnectionMode
import com.madash.smartswitch.util.WiFiBand

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiverConnectionSettingsDialog(
    currentMode: ConnectionMode,
    currentBand: WiFiBand,
    context: Context,
    onModeChange: (ConnectionMode, WiFiBand) -> Unit,
    onDismiss: () -> Unit
) {

    var selectedMode by remember { mutableStateOf(currentMode) }
    var selectedBand by remember { mutableStateOf(currentBand ?: WiFiBand.BAND_2_4_GHZ) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .width(400.dp)
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Connection Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Connection Mode Section
                Text(
                    text = "Connection Method",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Choose how sender devices will connect to this receiver",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(modifier = Modifier.selectableGroup()) {
                    ConnectionModeOption(
                        mode = ConnectionMode.AUTOMATIC,
                        icon = Icons.Default.AutoMode,
                        title = "Automatic (Recommended)",
                        description = "Try WiFi Direct first, fallback to Local Network",
                        isSelected = selectedMode == ConnectionMode.AUTOMATIC,
                        isEnabled = true,
                        onSelect = { selectedMode = ConnectionMode.AUTOMATIC }
                    )

                    ConnectionModeOption(
                        mode = ConnectionMode.LOCAL_NETWORK,
                        icon = Icons.Default.Router,
                        title = "Local Network Only",
                        description = "Use existing WiFi router connection",
                        isSelected = selectedMode == ConnectionMode.LOCAL_NETWORK,
                        isEnabled = true,
                        onSelect = { selectedMode = ConnectionMode.LOCAL_NETWORK }
                    )

                    ConnectionModeOption(
                        mode = ConnectionMode.WIFI_DIRECT,
                        icon = Icons.Default.WifiTethering,
                        title = "WiFi Direct Only",
                        description = "Direct peer-to-peer connection",
                        isSelected = selectedMode == ConnectionMode.WIFI_DIRECT,
                        isEnabled = isWifiDirectSupported(context),
                        onSelect = { selectedMode = ConnectionMode.WIFI_DIRECT }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // WiFi Band Selection (only show for WiFi Direct and Automatic)
                if (selectedMode == ConnectionMode.WIFI_DIRECT ||
                    selectedMode == ConnectionMode.AUTOMATIC) {

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "WiFi Band Selection",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when (selectedMode) {
                            ConnectionMode.WIFI_DIRECT -> "Choose frequency band for WiFi Direct connection"
                            ConnectionMode.AUTOMATIC -> "Choose preferred frequency band"
                            else -> "Choose frequency band"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(modifier = Modifier.selectableGroup()) {
                        WiFiBandOption(
                            band = WiFiBand.BAND_2_4_GHZ,
                            title = "2.4 GHz",
                            description = "Better range, more compatibility",
                            isSelected = selectedBand == WiFiBand.BAND_2_4_GHZ,
                            isEnabled = WiFiBand.BAND_2_4_GHZ.isSupported(context),
                            onSelect = { selectedBand = WiFiBand.BAND_2_4_GHZ }
                        )

                        WiFiBandOption(
                            band = WiFiBand.BAND_5_GHZ,
                            title = "5 GHz",
                            description = "Faster speeds, less interference",
                            isSelected = selectedBand == WiFiBand.BAND_5_GHZ,
                            isEnabled = WiFiBand.BAND_5_GHZ.isSupported(context),
                            onSelect = { selectedBand = WiFiBand.BAND_5_GHZ }
                        )

                        // Add 6GHz support for newer devices
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            WiFiBandOption(
                                band = WiFiBand.BAND_6_GHZ,
                                title = "6 GHz",
                                description = "Latest standard, highest speeds",
                                isSelected = selectedBand == WiFiBand.BAND_6_GHZ,
                                isEnabled = WiFiBand.BAND_6_GHZ.isSupported(context),
                                onSelect = { selectedBand = WiFiBand.BAND_6_GHZ }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mode Information Card
                ModeInformationCard(selectedMode, selectedBand, context)

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onModeChange(selectedMode, selectedBand)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionModeOption(
    mode: ConnectionMode,
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .selectable(
                selected = isSelected,
                onClick = { if (isEnabled) onSelect() },
                role = Role.RadioButton,
                enabled = isEnabled
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isEnabled) {
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )

                if (!isEnabled) {
                    Text(
                        text = "Not supported on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isEnabled,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun WiFiBandOption(
    band: WiFiBand,
    title: String,
    description: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .selectable(
                selected = isSelected,
                onClick = { if (isEnabled) onSelect() },
                role = Role.RadioButton,
                enabled = isEnabled
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = if (isEnabled) {
                    if (isSelected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                } else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )

                if (!isEnabled) {
                    Text(
                        text = "Not supported",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = isEnabled,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun ModeInformationCard(selectedMode: ConnectionMode, selectedBand: WiFiBand, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "${getModeDisplayName(selectedMode)} - ${selectedBand.frequency}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            val infoText = when (selectedMode) {
                ConnectionMode.AUTOMATIC -> {
                    buildString {
                        append("• Tries WiFi Direct first, then Local Network\n")
                        append("• Maximum compatibility with all devices\n")
                        append("• Uses ${selectedBand.frequency} for wireless connections\n")
                        append("• Recommended for most users")
                    }
                }
                ConnectionMode.LOCAL_NETWORK -> {
                    buildString {
                        append("• Uses your existing WiFi router\n")
                        append("• Both devices must be on the same network\n")
                        append("• More stable and faster transfers\n")
                        append("• Lower battery consumption")
                    }
                }
                ConnectionMode.WIFI_DIRECT -> {
                    buildString {
                        append("• Creates direct peer-to-peer connection\n")
                        append("• Works without existing WiFi network\n")
                        append("• Uses ${selectedBand.frequency} frequency band\n")
                        if (!selectedBand.isSupported(context)) {
                            append("• Warning: ${selectedBand.frequency} not supported, will use 2.4GHz")
                        } else {
                            append("• ${if (selectedBand == WiFiBand.BAND_5_GHZ) "Faster speeds, shorter range" else "Better range, more compatible"}")
                        }
                    }
                }
            }

            Text(
                text = infoText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getModeDisplayName(mode: ConnectionMode): String {
    return when (mode) {
        ConnectionMode.AUTOMATIC -> "Automatic Mode"
        ConnectionMode.LOCAL_NETWORK -> "Local Network Mode"
        ConnectionMode.WIFI_DIRECT -> "WiFi Direct Mode"
    }
}

// Helper function to check device capabilities
private fun isWifiDirectSupported(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_WIFI_DIRECT)
}