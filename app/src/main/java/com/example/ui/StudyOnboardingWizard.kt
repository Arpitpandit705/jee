package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PreferencesManager
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusEmeraldContainer
import com.example.ui.theme.FocusIndigoLight
import com.example.ui.theme.FocusIndigoPrimary

@Composable
fun StudyOnboardingWizard(
    onComplete: (
        dailyLectures: Int,
        dailyHours: Int,
        dppsEnabled: Boolean,
        dailyDppQuestions: Int,
        checkIn7Am: String,
        blocker8Am: String,
        batchKey: String,
        batchUrl: String,
        startLecture: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, 3

    // Step 1 State: Lectures & Quota
    var dailyLecturesCount by remember { mutableIntStateOf(prefs.dailyLectureQuota) }
    var dailyStudyHours by remember { mutableIntStateOf(prefs.dailyStudyHoursTarget) }
    val selectedSubjects = remember { mutableStateListOf("Physics", "Chemistry", "Mathematics") }

    // Step 2 State: DPP & Revision Mapping
    var dppEnabled by remember { mutableStateOf(prefs.dppTrackingEnabled) }
    var dailyDppQuestionsCount by remember { mutableIntStateOf(prefs.dailyDppQuota) }
    var revisionCycleDays by remember { mutableIntStateOf(3) }

    // Step 3 State: Reminders & Batch API Setup
    var morningCheckInTime by remember { mutableStateOf(prefs.checkInTime7Am) }
    var autoLockdownTime by remember { mutableStateOf(prefs.blockerTime8Am) }
    var batchApiKeyInput by remember { mutableStateOf(prefs.batchApiKey) }
    var batchPortalUrlInput by remember { mutableStateOf(prefs.batchPortalUrl) }
    var batchStartLectureInput by remember { mutableLongStateOf(prefs.batchStartLecture) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Wizard Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentStep > 1) {
                    IconButton(onClick = { currentStep -= 1 }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }

                Text(
                    text = "Study Setup: Step $currentStep of 3",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Text(
                    text = "${(currentStep * 33.3f).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = FocusIndigoLight
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { currentStep / 3f },
                color = FocusIndigoPrimary,
                trackColor = Color(0xFF1E2638),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } togetherWith slideOutHorizontally { width -> -width }
                    } else {
                        slideInHorizontally { width -> -width } togetherWith slideOutHorizontally { width -> width }
                    }
                },
                label = "wizard_step"
            ) { step ->
                when (step) {
                    1 -> Step1DailyLectures(
                        dailyLectures = dailyLecturesCount,
                        onDailyLecturesChange = { dailyLecturesCount = it },
                        dailyHours = dailyStudyHours,
                        onDailyHoursChange = { dailyStudyHours = it },
                        selectedSubjects = selectedSubjects,
                        onToggleSubject = { sub ->
                            if (selectedSubjects.contains(sub)) selectedSubjects.remove(sub)
                            else selectedSubjects.add(sub)
                        }
                    )
                    2 -> Step2DppMapping(
                        dppEnabled = dppEnabled,
                        onDppEnabledChange = { dppEnabled = it },
                        dailyDppQuestions = dailyDppQuestionsCount,
                        onDailyDppQuestionsChange = { dailyDppQuestionsCount = it },
                        revisionDays = revisionCycleDays,
                        onRevisionDaysChange = { revisionCycleDays = it }
                    )
                    3 -> Step3RemindersAndBatch(
                        checkInTime = morningCheckInTime,
                        onCheckInTimeChange = { morningCheckInTime = it },
                        lockdownTime = autoLockdownTime,
                        onLockdownTimeChange = { autoLockdownTime = it },
                        batchApiKey = batchApiKeyInput,
                        onBatchApiKeyChange = { batchApiKeyInput = it },
                        batchUrl = batchPortalUrlInput,
                        onBatchUrlChange = { batchPortalUrlInput = it },
                        startLecture = batchStartLectureInput,
                        onStartLectureChange = { batchStartLectureInput = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (currentStep < 3) {
                        currentStep += 1
                    } else {
                        // Persist preferences and complete onboarding
                        prefs.dailyLectureQuota = dailyLecturesCount
                        prefs.dailyStudyHoursTarget = dailyStudyHours
                        prefs.dppTrackingEnabled = dppEnabled
                        prefs.dailyDppQuota = dailyDppQuestionsCount
                        prefs.checkInTime7Am = morningCheckInTime
                        prefs.blockerTime8Am = autoLockdownTime
                        prefs.batchApiKey = batchApiKeyInput.trim()
                        prefs.batchPortalUrl = batchPortalUrlInput.trim().ifBlank { "https://youtube.com" }
                        prefs.batchStartLecture = batchStartLectureInput
                        prefs.isStudyOnboardingCompleted = true

                        onComplete(
                            dailyLecturesCount,
                            dailyStudyHours,
                            dppEnabled,
                            dailyDppQuestionsCount,
                            morningCheckInTime,
                            autoLockdownTime,
                            batchApiKeyInput.trim(),
                            batchPortalUrlInput.trim(),
                            batchStartLectureInput
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("onboarding_continue_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (currentStep < 3) "Next Step" else "Complete Setup & Enter Focus Hub",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (currentStep < 3) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step1DailyLectures(
    dailyLectures: Int,
    onDailyLecturesChange: (Int) -> Unit,
    dailyHours: Int,
    onDailyHoursChange: (Int) -> Unit,
    selectedSubjects: List<String>,
    onToggleSubject: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Step 1: Daily Lecture Quota",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "How many lectures do you plan to watch each day in your batch?",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Daily Target: $dailyLectures Lectures / Day",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2, 3, 4, 5, 6).forEach { num ->
                        val isSelected = dailyLectures == num
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) FocusIndigoPrimary else DarkSurfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) FocusIndigoLight else DarkCardBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onDailyLecturesChange(num) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$num",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Daily Study Target: $dailyHours Hours",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Recommended for optimal retention and DPP solving",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(8.dp))

                var sliderVal by remember { mutableFloatStateOf(dailyHours.toFloat()) }
                Slider(
                    value = sliderVal,
                    onValueChange = {
                        sliderVal = it
                        onDailyHoursChange(it.toInt())
                    },
                    valueRange = 2f..10f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = FocusIndigoPrimary,
                        activeTrackColor = FocusIndigoPrimary,
                        inactiveTrackColor = Color(0xFF1E2638)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Your Subjects:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                val availableSubjects = listOf("Physics", "Chemistry", "Mathematics", "Biology", "Coding", "General Studies")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableSubjects.forEach { sub ->
                        val isSel = selectedSubjects.contains(sub)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSel) Color(0xFF312E81) else DarkSurfaceVariant,
                                    RoundedCornerShape(20.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSel) FocusIndigoLight else DarkCardBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { onToggleSubject(sub) }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = sub,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step2DppMapping(
    dppEnabled: Boolean,
    onDppEnabledChange: (Boolean) -> Unit,
    dailyDppQuestions: Int,
    onDailyDppQuestionsChange: (Int) -> Unit,
    revisionDays: Int,
    onRevisionDaysChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Step 2: DPP & Revision Mapping",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Connect homework DPPs (Daily Practice Problems) and schedule spaced revisions.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Track DPPs For Lectures",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Require problem sheet solving before marking lecture complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = dppEnabled,
                        onCheckedChange = onDppEnabledChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = FocusIndigoPrimary)
                    )
                }

                if (dppEnabled) {
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Daily Target DPP Questions: $dailyDppQuestions Questions",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(10, 15, 20, 30).forEach { count ->
                            val isSelected = dailyDppQuestions == count
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) FocusIndigoPrimary else DarkSurfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) FocusIndigoLight else DarkCardBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDailyDppQuestionsChange(count) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$count Qs",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Spaced Repetition Frequency: Every $revisionDays Days",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 7).forEach { days ->
                            val isSelected = revisionDays == days
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) FocusIndigoPrimary else DarkSurfaceVariant,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onRevisionDaysChange(days) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Day $days Revision",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step3RemindersAndBatch(
    checkInTime: String,
    onCheckInTimeChange: (String) -> Unit,
    lockdownTime: String,
    onLockdownTimeChange: (String) -> Unit,
    batchApiKey: String,
    onBatchApiKeyChange: (String) -> Unit,
    batchUrl: String,
    onBatchUrlChange: (String) -> Unit,
    startLecture: Long,
    onStartLectureChange: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Step 3: Reminders & Batch Setup",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Text(
            text = "Schedule morning check-in and connect your batch lecture key & starting point.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Morning schedule
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "7:00 AM Check-In",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = checkInTime,
                            onValueChange = onCheckInTimeChange,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "8:00 AM Auto-Lock",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = lockdownTime,
                            onValueChange = onLockdownTimeChange,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = DarkSurfaceVariant,
                                unfocusedContainerColor = DarkSurfaceVariant,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Batch API & Starting point section
                Text(
                    text = "Batch Lecture Portal Integration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Enter your batch API key or lecture URL. Clicking start will launch directly to your lecture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = batchApiKey,
                    onValueChange = onBatchApiKeyChange,
                    label = { Text("Batch API Key / Access Token") },
                    placeholder = { Text("e.g. batch_live_token_982x...", color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = FocusIndigoPrimary) },
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
                    value = batchUrl,
                    onValueChange = onBatchUrlChange,
                    label = { Text("Batch Lecture URL / Portal Link") },
                    placeholder = { Text("https://batch.example.com/lectures", color = Color(0xFF64748B)) },
                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = FocusIndigoPrimary) },
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
                    value = startLecture.toString(),
                    onValueChange = { onStartLectureChange(it.toLongOrNull() ?: 1L) },
                    label = { Text("Start from Lecture Number (Bookmark)") },
                    leadingIcon = { Icon(Icons.Default.School, contentDescription = null, tint = FocusAmber) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
