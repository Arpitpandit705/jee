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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Course
import com.example.model.Lecture
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusEmeraldContainer
import com.example.ui.theme.FocusIndigoLight
import com.example.ui.theme.FocusIndigoPrimary

@Composable
fun CoursesTrackerSection(
    courses: List<Course>,
    lecturesByCourse: Map<String, List<Lecture>>,
    onAddCourse: (title: String, subject: String, totalLecs: Long, hasDpp: Boolean) -> Unit,
    onAddLecture: (courseId: String, title: String, number: Long, duration: Long, hasDpp: Boolean) -> Unit,
    onToggleDpp: (Lecture) -> Unit,
    onToggleCompleted: (Lecture) -> Unit,
    onOpenVerification: (Lecture, subject: String) -> Unit
) {
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var selectedCourseForAddLecture by remember { mutableStateOf<Course?>(null) }
    val expandedCourseIds = remember { mutableStateMapOf<String, Boolean>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Header Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Courses & DPP Tracker",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Manage syllabus, DPP sheets, and revision cycles",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Button(
                    onClick = { showAddCourseDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("add_new_course_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Course", fontSize = 12.sp)
                }
            }
        }

        if (courses.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No courses added yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Tap 'Add Course' to define your total lectures and DPP sheets.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        } else {
            items(courses) { course ->
                val lectures = lecturesByCourse[course.id] ?: emptyList()
                val isExpanded = expandedCourseIds[course.id] ?: true

                CourseItemCard(
                    course = course,
                    lectures = lectures,
                    isExpanded = isExpanded,
                    onToggleExpand = { expandedCourseIds[course.id] = !isExpanded },
                    onAddLectureClick = { selectedCourseForAddLecture = course },
                    onToggleDpp = onToggleDpp,
                    onToggleCompleted = onToggleCompleted,
                    onOpenVerification = { lec -> onOpenVerification(lec, course.subject) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddCourseDialog) {
        AddCourseDialog(
            onDismiss = { showAddCourseDialog = false },
            onConfirm = { title, subject, totalLecs, hasDpp ->
                onAddCourse(title, subject, totalLecs, hasDpp)
                showAddCourseDialog = false
            }
        )
    }

    if (selectedCourseForAddLecture != null) {
        val course = selectedCourseForAddLecture!!
        val currentLecs = lecturesByCourse[course.id] ?: emptyList()
        val nextNumber = (currentLecs.maxOfOrNull { it.lectureNumber } ?: 0L) + 1L

        AddLectureDialog(
            courseTitle = course.title,
            suggestedNumber = nextNumber,
            defaultHasDpp = course.hasDppDefault,
            onDismiss = { selectedCourseForAddLecture = null },
            onConfirm = { title, number, duration, hasDpp ->
                onAddLecture(course.id, title, number, duration, hasDpp)
                selectedCourseForAddLecture = null
            }
        )
    }
}

@Composable
private fun CourseItemCard(
    course: Course,
    lectures: List<Lecture>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddLectureClick: () -> Unit,
    onToggleDpp: (Lecture) -> Unit,
    onToggleCompleted: (Lecture) -> Unit,
    onOpenVerification: (Lecture) -> Unit
) {
    val completedCount = lectures.count { it.isCompleted }
    val progress = if (course.totalLectures > 0) {
        (completedCount.toFloat() / course.totalLectures.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val dppsSolved = lectures.count { it.hasDpp && it.dppSolved }
    val totalDpps = lectures.count { it.hasDpp }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Course header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF312E81), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = FocusIndigoLight,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${course.subject} • ${course.totalLectures} Lectures Total",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onAddLectureClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Lecture", tint = FocusIndigoPrimary)
                    }
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                color = FocusIndigoPrimary,
                trackColor = Color(0xFF1E2638),
                modifier = Modifier.fillMaxWidth().height(6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$completedCount / ${course.totalLectures} Lectures (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFCBD5E1)
                )

                if (totalDpps > 0) {
                    Text(
                        text = "DPP Solved: $dppsSolved / $totalDpps",
                        style = MaterialTheme.typography.labelSmall,
                        color = FocusAmber
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(14.dp))

                if (lectures.isEmpty()) {
                    Text(
                        text = "No lectures added yet. Tap '+' above to add lecture 1.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    lectures.forEach { lec ->
                        LectureRowItem(
                            lecture = lec,
                            onToggleDpp = { onToggleDpp(lec) },
                            onToggleCompleted = { onToggleCompleted(lec) },
                            onOpenVerification = { onOpenVerification(lec) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureRowItem(
    lecture: Lecture,
    onToggleDpp: () -> Unit,
    onToggleCompleted: () -> Unit,
    onOpenVerification: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(DarkSurfaceVariant, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            IconButton(onClick = onToggleCompleted, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (lecture.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (lecture.isCompleted) FocusEmerald else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Lec ${lecture.lectureNumber}: ${lecture.title}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (lecture.isCompleted) Color(0xFF94A3B8) else Color.White
                    )
                    if (lecture.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = FocusEmerald, modifier = Modifier.size(14.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${lecture.durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )

                    if (lecture.hasDpp) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = lecture.dppSolved,
                                onCheckedChange = { onToggleDpp() },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = FocusEmerald,
                                    uncheckedColor = Color(0xFF64748B)
                                ),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "DPP",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (lecture.dppSolved) FocusEmerald else FocusAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "Rev: ${lecture.revisionDueDays}d",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF818CF8)
                        )
                    }
                }
            }
        }

        Button(
            onClick = onOpenVerification,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (lecture.isVerified) FocusEmeraldContainer else FocusIndigoPrimary
            ),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = if (lecture.isVerified) "${lecture.verificationScore}%" else "Verify",
                fontSize = 10.sp,
                color = if (lecture.isVerified) FocusEmerald else Color.White
            )
        }
    }
}

@Composable
private fun AddCourseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, subject: String, totalLecs: Long, hasDpp: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var totalLecturesStr by remember { mutableStateOf("30") }
    var hasDppDefault by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add New Course / Subject",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title (e.g. Modern Physics)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject (e.g. Physics, Maths, Coding)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = totalLecturesStr,
                    onValueChange = { totalLecturesStr = it },
                    label = { Text("Total Number of Lectures (e.g. 45)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Include DPP for every lecture",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Switch(
                        checked = hasDppDefault,
                        onCheckedChange = { hasDppDefault = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = FocusIndigoPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val count = totalLecturesStr.toLongOrNull() ?: 30L
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), subject.trim(), count, hasDppDefault)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary)
                    ) {
                        Text("Create Course")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddLectureDialog(
    courseTitle: String,
    suggestedNumber: Long,
    defaultHasDpp: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, number: Long, duration: Long, hasDpp: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var numberStr by remember { mutableStateOf(suggestedNumber.toString()) }
    var durationStr by remember { mutableStateOf("60") }
    var hasDpp by remember { mutableStateOf(defaultHasDpp) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Lecture to $courseTitle",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = numberStr,
                    onValueChange = { numberStr = it },
                    label = { Text("Lecture Number") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Lecture Topic / Title") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = durationStr,
                    onValueChange = { durationStr = it },
                    label = { Text("Duration (Minutes)") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lecture includes DPP (Homework)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Switch(
                        checked = hasDpp,
                        onCheckedChange = { hasDpp = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = FocusIndigoPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val num = numberStr.toLongOrNull() ?: 1L
                            val dur = durationStr.toLongOrNull() ?: 60L
                            if (title.isNotBlank()) {
                                onConfirm(title.trim(), num, dur, hasDpp)
                            }
                        },
                        enabled = title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary)
                    ) {
                        Text("Add Lecture")
                    }
                }
            }
        }
    }
}
