package com.example.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.GeminiVerificationEngine
import com.example.model.Lecture
import com.example.model.QuizQuestion
import com.example.model.VerificationResult
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusEmeraldContainer
import com.example.ui.theme.FocusIndigoLight
import com.example.ui.theme.FocusIndigoPrimary
import kotlinx.coroutines.launch

@Composable
fun LectureVerificationDialog(
    lecture: Lecture,
    subject: String,
    onDismiss: () -> Unit,
    onVerificationPassed: (proofType: String, score: Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiEngine = remember { GeminiVerificationEngine() }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Notes/DPP Photo, 1: AI Quiz, 2: Active Recall

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground.copy(alpha = 0.95f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = FocusIndigoLight,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Lecture Verification",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Lec ${lecture.lectureNumber}: ${lecture.title} ($subject)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Verification method tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DarkSurfaceVariant,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = FocusIndigoPrimary
                            )
                        },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Notes & DPP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("AI Quiz", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Active Recall", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    when (selectedTab) {
                        0 -> NotesAndDppVerificationTab(
                            lecture = lecture,
                            subject = subject,
                            aiEngine = aiEngine,
                            onPassed = { score -> onVerificationPassed("NOTES_PHOTO", score) }
                        )
                        1 -> AiQuizVerificationTab(
                            lecture = lecture,
                            subject = subject,
                            aiEngine = aiEngine,
                            onPassed = { score -> onVerificationPassed("QUIZ", score) }
                        )
                        2 -> ActiveRecallVerificationTab(
                            lecture = lecture,
                            subject = subject,
                            aiEngine = aiEngine,
                            onPassed = { score -> onVerificationPassed("ACTIVE_RECALL", score) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesAndDppVerificationTab(
    lecture: Lecture,
    subject: String,
    aiEngine: GeminiVerificationEngine,
    onPassed: (Long) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VerificationResult?>(null) }

    // Android Zero-permission Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                selectedBitmap = BitmapFactory.decodeStream(inputStream)
                result = null
            } catch (e: Exception) {
                // Handle read error
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Upload a clear photo of your handwritten lecture notes and solved DPP problems to verify authentic completion.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, FocusIndigoPrimary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = selectedBitmap!!.asImageBitmap(),
                    contentDescription = "Selected notes photo",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Upload photo",
                        tint = FocusIndigoPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to Select Notes / DPP Photo",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Text(
                        text = "Supports JPEG, PNG",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedBitmap != null && result == null) {
            Button(
                onClick = {
                    isAnalyzing = true
                    coroutineScope.launch {
                        val res = aiEngine.verifyLectureNotesAndDpp(lecture.title, subject, selectedBitmap!!)
                        result = res
                        isAnalyzing = false
                    }
                },
                enabled = !isAnalyzing,
                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isAnalyzing) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini Vision Inspecting Notes...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify with Gemini AI")
                }
            }
        }

        if (result != null) {
            VerificationResultCard(
                result = result!!,
                onConfirmUnlock = { onPassed(result!!.score.toLong()) }
            )
        }
    }
}

@Composable
private fun AiQuizVerificationTab(
    lecture: Lecture,
    subject: String,
    aiEngine: GeminiVerificationEngine,
    onPassed: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var isLoadingQuiz by remember { mutableStateOf(false) }
    val userAnswers = remember { mutableStateListOf<Int>() }
    var quizSubmitted by remember { mutableStateOf(false) }
    var calculatedScore by remember { mutableIntStateOf(0) }

    LaunchedEffect(lecture.id) {
        isLoadingQuiz = true
        questions = aiEngine.generateLectureQuiz(lecture.title, subject)
        userAnswers.clear()
        repeat(questions.size) { userAnswers.add(-1) }
        isLoadingQuiz = false
    }

    if (isLoadingQuiz) {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = FocusIndigoPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Generating 3 Concept Questions...", color = Color(0xFF94A3B8), fontSize = 13.sp)
            }
        }
    } else if (questions.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            questions.forEachIndexed { qIndex, question ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Q${qIndex + 1}. ${question.question}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        question.options.forEachIndexed { optIndex, optText ->
                            val isSelected = userAnswers.getOrNull(qIndex) == optIndex
                            val isCorrect = question.correctIndex == optIndex

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .background(
                                        when {
                                            quizSubmitted && isCorrect -> FocusEmeraldContainer.copy(alpha = 0.5f)
                                            quizSubmitted && isSelected && !isCorrect -> Color(0xFF7F1D1D).copy(alpha = 0.5f)
                                            isSelected -> FocusIndigoPrimary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable(enabled = !quizSubmitted) {
                                        if (qIndex < userAnswers.size) {
                                            userAnswers[qIndex] = optIndex
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (!quizSubmitted && qIndex < userAnswers.size) {
                                            userAnswers[qIndex] = optIndex
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = FocusIndigoPrimary,
                                        unselectedColor = Color(0xFF64748B)
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = optText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (quizSubmitted && isCorrect) FocusEmerald else Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val allAnswered = userAnswers.none { it == -1 }

            if (!quizSubmitted) {
                Button(
                    onClick = {
                        var correctCount = 0
                        questions.forEachIndexed { index, q ->
                            if (userAnswers.getOrNull(index) == q.correctIndex) {
                                correctCount++
                            }
                        }
                        calculatedScore = ((correctCount.toDouble() / questions.size) * 100).toInt()
                        quizSubmitted = true
                    },
                    enabled = allAnswered,
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(if (allAnswered) "Submit Quiz for Verification" else "Answer All 3 Questions")
                }
            } else {
                val passed = calculatedScore >= 66
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (passed) FocusEmeraldContainer else Color(0xFF7F1D1D)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (passed) "Score: $calculatedScore% - Passed!" else "Score: $calculatedScore% - Try Again",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (passed) {
                            Button(
                                onClick = { onPassed(calculatedScore.toLong()) },
                                colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald)
                            ) {
                                Text("Complete Verification & Unlock")
                            }
                        } else {
                            Button(
                                onClick = {
                                    quizSubmitted = false
                                    userAnswers.clear()
                                    repeat(questions.size) { userAnswers.add(-1) }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary)
                            ) {
                                Text("Retry Quiz")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveRecallVerificationTab(
    lecture: Lecture,
    subject: String,
    aiEngine: GeminiVerificationEngine,
    onPassed: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var summaryText by remember { mutableStateOf("") }
    var isEvaluating by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<VerificationResult?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Active Recall: Explain in your own words the key concepts, equations, and problem steps from this lecture.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = summaryText,
            onValueChange = { summaryText = it },
            placeholder = { Text("Example: Today's lecture covered Angular Momentum conservation. In the DPP we solved problem 4 by equating initial I1*w1 to final I2*w2...", color = Color(0xFF64748B)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = FocusIndigoPrimary,
                unfocusedIndicatorColor = DarkCardBorder
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(150.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (result == null) {
            Button(
                onClick = {
                    isEvaluating = true
                    coroutineScope.launch {
                        result = aiEngine.evaluateActiveRecall(lecture.title, subject, summaryText)
                        isEvaluating = false
                    }
                },
                enabled = summaryText.trim().length >= 30 && !isEvaluating,
                colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (isEvaluating) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Assessing Comprehension...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify Active Recall")
                }
            }
        } else {
            VerificationResultCard(
                result = result!!,
                onConfirmUnlock = { onPassed(result!!.score.toLong()) }
            )
        }
    }
}

@Composable
private fun VerificationResultCard(
    result: VerificationResult,
    onConfirmUnlock: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (result.isVerified) FocusEmeraldContainer.copy(alpha = 0.8f) else Color(0xFF7F1D1D)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (result.isVerified) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.isVerified) FocusEmerald else Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (result.isVerified) "Verified! Comprehension Score: ${result.score}%" else "Verification Incomplete (${result.score}%)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.feedback,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
            )

            if (result.identifiedConcepts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    result.identifiedConcepts.take(3).forEach { concept ->
                        Box(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = concept, color = Color(0xFFA7F3D0), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (result.isVerified) {
                Button(
                    onClick = onConfirmUnlock,
                    colors = ButtonDefaults.buttonColors(containerColor = FocusEmerald),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Record Verified Completion & Unlock", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
