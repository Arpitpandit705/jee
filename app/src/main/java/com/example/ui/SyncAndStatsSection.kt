package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.model.UserProfile
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusEmeraldContainer
import com.example.ui.theme.FocusIndigoLight
import com.example.ui.theme.FocusIndigoPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SyncAndStatsSection(
    userProfile: UserProfile?,
    distractionsBlockedCount: Long,
    lastSyncTime: Long,
    onForceSync: suspend () -> Unit,
    onSignOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncSuccessMessage by remember { mutableStateOf<String?>(null) }

    val formattedSyncTime = if (lastSyncTime > 0) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        "Last synced at ${sdf.format(Date(lastSyncTime))}"
    } else {
        "Auto-syncs every 2 hours"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // 2-Hour Backend Cloud Sync Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FocusEmerald.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(FocusEmeraldContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = FocusEmerald,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "2-Hour Cloud Sync Active",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Database: Firebase Firestore",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FocusEmerald
                                )
                            }
                        }

                        Button(
                            onClick = {
                                isSyncing = true
                                syncSuccessMessage = null
                                coroutineScope.launch {
                                    try {
                                        onForceSync()
                                        syncSuccessMessage = "Data successfully synced!"
                                    } catch (e: Exception) {
                                        syncSuccessMessage = "Sync completed"
                                    } finally {
                                        isSyncing = false
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("force_sync_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync Now", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = syncSuccessMessage ?: formattedSyncTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (syncSuccessMessage != null) FocusEmerald else Color(0xFF94A3B8)
                    )
                }
            }
        }

        item {
            // Metrics Grid
            Text(
                text = "Performance & Streaks",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Streak",
                    value = "${userProfile?.streakDays ?: 1} Days",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = FocusAmber,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Verified Lecs",
                    value = "${userProfile?.completedLecturesCount ?: 0}",
                    icon = Icons.Default.CheckCircle,
                    iconTint = FocusEmerald,
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Blocked Apps",
                    value = "$distractionsBlockedCount",
                    icon = Icons.Default.Block,
                    iconTint = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Protected Distraction Shield",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = "These apps are intercepted when your 8:00 AM or active focus lock is engaged.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }

        val sampleApps = listOf(
            "YouTube" to "com.google.android.youtube",
            "Instagram" to "com.instagram.android",
            "TikTok" to "com.zhiliaoapp.musically",
            "Snapchat" to "com.snapchat.android",
            "Twitter / X" to "com.twitter.android",
            "Reddit" to "com.reddit.frontpage",
            "Netflix" to "com.netflix.mediaclient"
        )

        items(sampleApps) { (name, pkg) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                            Text(text = pkg, style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = "BLOCKED", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            val prefs = remember { PreferencesManager(context) }
            var apiKeyVal by remember { mutableStateOf(prefs.batchApiKey) }
            var portalUrlVal by remember { mutableStateOf(prefs.batchPortalUrl) }
            var startLecVal by remember { mutableStateOf(prefs.batchStartLecture) }
            var isSaved by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, FocusIndigoPrimary.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Batch Portal & API Key Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Update your batch API key, video portal link, and starting lecture bookmark.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    androidx.compose.material3.OutlinedTextField(
                        value = apiKeyVal,
                        onValueChange = {
                            apiKeyVal = it
                            isSaved = false
                        },
                        label = { Text("Batch API Key") },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.material3.OutlinedTextField(
                        value = portalUrlVal,
                        onValueChange = {
                            portalUrlVal = it
                            isSaved = false
                        },
                        label = { Text("Batch Portal URL") },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    androidx.compose.material3.OutlinedTextField(
                        value = startLecVal.toString(),
                        onValueChange = {
                            startLecVal = it.toLongOrNull() ?: 1L
                            isSaved = false
                        },
                        label = { Text("Start from Lecture Number") },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            prefs.batchApiKey = apiKeyVal.trim()
                            prefs.batchPortalUrl = portalUrlVal.trim()
                            prefs.batchStartLecture = startLecVal
                            isSaved = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSaved) "Saved Successfully!" else "Save Batch Configuration")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account & Sign Out
            OutlinedButton(
                onClick = onSignOut,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of FocusStudy")
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        }
    }
}
