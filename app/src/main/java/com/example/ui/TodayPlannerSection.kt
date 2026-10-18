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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Course
import com.example.model.DailyPlan
import com.example.model.Lecture
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusEmeraldContainer
import com.example.ui.theme.FocusIndigoDark
import com.example.ui.theme.FocusIndigoPrimary

@Composable
fun TodayPlannerSection(
    courses: List<Course>,
    allLectures: List<Lecture>,
    todayPlan: DailyPlan?,
    onSavePlan: (DailyPlan) -> Unit,
    onStartFocusSession: (lecture: Lecture) -> Unit,
    onOpenVerification: (lecture: Lecture) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // 7 AM Routine Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF312E81).copy(alpha = 0.6f), Color(0xFF1E1B4B))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(FocusAmber.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = FocusAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "7:00 AM Daily Planning Routine",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "Plan today's lectures & durations. Strict blocker auto-starts at 8:00 AM.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "8:00 AM Auto-Blocker: Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA5B4FC)
                                )
                            }

                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("add_lecture_to_plan_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Assign Lecture", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            val plannedLecs = allLectures.filter { todayPlan?.plannedLectureIds?.contains(it.id) == true }
            val nextLec = plannedLecs.firstOrNull { !it.isCompleted } ?: allLectures.firstOrNull { !it.isCompleted } ?: allLectures.firstOrNull()
            val crs = courses.find { it.id == nextLec?.courseId }

            BatchLectureLauncherCard(
                activeLecture = nextLec,
                subject = crs?.subject ?: "Physics",
                onStartFocusLock = onStartFocusSession
            )
        }

        item {
            // Target Summary Pill
            val totalPlannedMins = allLectures.filter { todayPlan?.plannedLectureIds?.contains(it.id) == true }
                .sumOf { it.durationMinutes }

            val completedCount = allLectures.filter {
                todayPlan?.plannedLectureIds?.contains(it.id) == true && it.isCompleted
            }.size

            val plannedCount = todayPlan?.plannedLectureIds?.size ?: 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "Planned Lectures", value = "$plannedCount")
                StatItem(label = "Target Duration", value = "${totalPlannedMins}m")
                StatItem(label = "Completed", value = "$completedCount / $plannedCount")
            }
        }

        item {
            Text(
                text = "Today's Schedule & Targets",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }

        val plannedLectures = allLectures.filter { todayPlan?.plannedLectureIds?.contains(it.id) == true }

        if (plannedLectures.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No lectures scheduled for today yet",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Tap 'Assign Lecture' above to set your 7:00 AM daily schedule.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        } else {
            items(plannedLectures) { lecture ->
                TodayLectureCard(
                    lecture = lecture,
                    onStartFocus = { onStartFocusSession(lecture) },
                    onVerify = { onOpenVerification(lecture) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddDialog) {
        AssignLectureDialog(
            allLectures = allLectures,
            courses = courses,
            currentlyPlannedIds = todayPlan?.plannedLectureIds ?: emptyList(),
            onDismiss = { showAddDialog = false },
            onAssign = { selectedLectureId ->
                val updatedIds = (todayPlan?.plannedLectureIds ?: emptyList()) + selectedLectureId
                val newPlan = DailyPlan(
                    id = todayPlan?.id ?: "",
                    plannedLectureIds = updatedIds.distinct(),
                    totalPlannedMinutes = allLectures.filter { updatedIds.contains(it.id) }.sumOf { it.durationMinutes }
                )
                onSavePlan(newPlan)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun TodayLectureCard(
    lecture: Lecture,
    onStartFocus: () -> Unit,
    onVerify: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lec ${lecture.lectureNumber}: ${lecture.title}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    if (lecture.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = FocusEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${lecture.durationMinutes} mins", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))

                    if (lecture.hasDpp) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = if (lecture.dppSolved) FocusEmerald else FocusAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lecture.dppSolved) "DPP Solved" else "DPP Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (lecture.dppSolved) FocusEmerald else FocusAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            if (!lecture.isCompleted) {
                Button(
                    onClick = onStartFocus,
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("start_lecture_focus_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Focus Lock", fontSize = 12.sp)
                }
            } else if (!lecture.isVerified) {
                Button(
                    onClick = onVerify,
                    colors = ButtonDefaults.buttonColors(containerColor = FocusAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("verify_lecture_btn")
                ) {
                    Text("Verify AI", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .background(FocusEmeraldContainer, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Verified (${lecture.verificationScore}%)", color = FocusEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AssignLectureDialog(
    allLectures: List<Lecture>,
    courses: List<Course>,
    currentlyPlannedIds: List<String>,
    onDismiss: () -> Unit,
    onAssign: (lectureId: String) -> Unit
) {
    val unassignedLectures = allLectures.filter { !currentlyPlannedIds.contains(it.id) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Assign Lecture for Today",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (unassignedLectures.isEmpty()) {
                    Text(
                        text = "No unassigned lectures available. Add courses/lectures in the Courses tab first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(unassignedLectures) { lec ->
                            val course = courses.find { it.id == lec.courseId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Lec ${lec.lectureNumber}: ${lec.title}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${course?.subject ?: "Course"} • ${lec.durationMinutes} mins",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Button(
                                    onClick = { onAssign(lec.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Add", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
    }
}
