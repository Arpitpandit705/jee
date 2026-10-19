package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.model.Lecture
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusEmeraldContainer
import com.example.ui.theme.FocusIndigoLight
import com.example.ui.theme.FocusIndigoPrimary
import com.example.util.SpeedAdvisor

@Composable
fun BatchLectureLauncherCard(
    activeLecture: Lecture?,
    subject: String,
    onStartFocusLock: (Lecture) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    val lectureTitle = activeLecture?.title ?: "Batch Lecture ${prefs.batchStartLecture}"
    val speedRec = remember(lectureTitle, subject) {
        SpeedAdvisor.getRecommendation(lectureTitle, subject)
    }

    var selectedSpeed by remember(speedRec) { mutableFloatStateOf(speedRec.speed) }

    val hasApiKey = prefs.batchApiKey.isNotBlank()
    val batchUrl = prefs.batchPortalUrl.ifBlank { "https://youtube.com" }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, FocusIndigoPrimary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1E1B4B).copy(alpha = 0.5f), DarkSurface)
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                // Header with Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(FocusIndigoPrimary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Launch,
                                contentDescription = null,
                                tint = FocusIndigoLight,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Direct Batch Launcher",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (hasApiKey) "Batch API Key Attached • Ready to Stream" else "Batch Portal Attached",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (hasApiKey) FocusEmerald else Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (hasApiKey) FocusEmeraldContainer else DarkSurfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasApiKey) Icons.Default.Key else Icons.Default.School,
                                contentDescription = null,
                                tint = if (hasApiKey) FocusEmerald else Color(0xFF818CF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (hasApiKey) "KEY ATTACHED" else "PORTAL READY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasApiKey) FocusEmerald else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Lecture bookmark info
                Text(
                    text = "Current Bookmark: Lec ${activeLecture?.lectureNumber ?: prefs.batchStartLecture} - $lectureTitle",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Speed Advisor Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = FocusAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Recommended Speed: ${speedRec.speedLabel}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(FocusAmber.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Saves ~${speedRec.minutesSavedOn60m}m",
                                    color = FocusAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = speedRec.explanation,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Interactive speed selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1.0f to "1.0x", 1.25f to "1.25x", 1.50f to "1.50x", 1.75f to "1.75x", 2.0f to "2.0x").forEach { (speedVal, label) ->
                                val isSelected = selectedSpeed == speedVal
                                val isRec = speedRec.speed == speedVal

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) FocusIndigoPrimary else Color(0xFF131A2A),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isRec) FocusAmber else if (isSelected) FocusIndigoLight else DarkCardBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedSpeed = speedVal }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected || isRec) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else if (isRec) FocusAmber else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Launch Lecture Directly Button
                Button(
                    onClick = {
                        val targetLecture = activeLecture ?: Lecture(
                            lectureNumber = prefs.batchStartLecture,
                            title = lectureTitle,
                            durationMinutes = 60L
                        )

                        // 1. Engage focus lock
                        onStartFocusLock(targetLecture)

                        // 2. Build direct URL with start params if applicable.
                        // SECURITY: NEVER append the API key into the URL. Query strings
                        // end up in browser history, target-app logs and crash reports.
                        // The portal URL must carry its own auth (the key stays in PW).

                        val lecParam = "lec=${targetLecture.lectureNumber}"
                        val finalUrl = try {
                            // Only append non-secret params; https-only guard against intent abuse
                            Uri.parse(batchUrl)
                                .buildUpon()
                                .appendQueryParameter("lec", lecParam.substringAfter('='))
                                .appendQueryParameter("speed", selectedSpeed.toString())
                                .build()
                                .toString()
                                .also { if (!it.startsWith("https://")) throw IllegalArgumentException("non-https portal") }
                        } catch (e: Exception) {
                            batchUrl
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            Toast.makeText(
                                context,
                                "Launching batch lecture at ${selectedSpeed}x! Focus lock engaged.",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Focus lock engaged! Open your lecture player.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("launch_batch_lecture_direct_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Launch Lecture directly (${selectedSpeed}x)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
