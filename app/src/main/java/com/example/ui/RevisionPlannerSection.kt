package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.GeminiHintEngine
import com.example.data.PwStudyPlan
import com.example.model.Course
import com.example.model.Lecture
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FocusAmber
import com.example.ui.theme.FocusCrimson
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigoPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/** Only https links may ever be opened from the plan asset. */
private fun openUrl(context: android.content.Context, rawUrl: String): Boolean {
    return try {
        val uri = Uri.parse(rawUrl)
        if (uri.scheme == "https" && !rawUrl.contains(" ")) {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } else false
    } catch (e: Exception) {
        false
    }
}

private const val STUCK_THRESHOLD_SECONDS = 180 // 3 minutes stuck => hint unlocked

@Composable
fun RevisionPlannerSection(
    plan: PwStudyPlan?,
    courses: List<Course>,
    lecturesByCourse: Map<String, List<Lecture>>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hintEngine = remember { GeminiHintEngine() }

    var selectedCourseIdx by remember { mutableIntStateOf(0) }
    var hintTarget by remember { mutableStateOf<PwChapterEntryLite?>(null) }

    // Courses that carry chapter/DPP data from the plan
    val planCourses = plan?.courses?.filter { !it.fullyComplete && it.chapters.isNotEmpty() } ?: emptyList()
    val selectedCourse = planCourses.getOrNull(selectedCourseIdx)

    // Due-for-revision list: completed lectures whose revision window elapsed
    val dueLectures = remember(courses, lecturesByCourse) {
        val now = System.currentTimeMillis()
        val byId = courses.associateBy { it.id }
        lecturesByCourse.values.flatten()
            .filter { lec ->
                lec.isCompleted && lec.revisionDueDays > 0 && lec.updatedAt != null &&
                    (lec.updatedAt.toDate().time + lec.revisionDueDays * 86_400_000L) <= now
            }
            .sortedBy { lec ->
                lec.updatedAt!!.toDate().time + lec.revisionDueDays * 86_400_000L
            }
            .take(12)
            .map { lec -> (byId[lec.courseId]?.subject ?: "") to lec }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Revision Planner", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            "DPP direct-open + 3-minute stuck hint + spaced revision",
            fontSize = 12.sp,
            color = TextSecondary
        )

        // ---------- Due for revision ----------
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🔄 Due for Revision", fontWeight = FontWeight.Bold, color = FocusAmber)
                if (dueLectures.isEmpty()) {
                    Text("Kuch due nahi — spaced revision schedule is on track.", color = TextSecondary, fontSize = 13.sp)
                } else {
                    dueLectures.forEach { (subject, lec) ->
                        Text(
                            "• $subject — ${lec.title.take(48)}",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // ---------- YouTube playlists ----------
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("📺 Revision Playlists (YouTube)", fontWeight = FontWeight.Bold, color = FocusEmerald)
                plan?.courses?.map { it.subjectGroup to it.playlistUrl }
                    ?.distinct()
                    ?.take(3)
                    ?.forEach { (group, url) ->
                        OutlinedButton(onClick = { openUrl(context, url) }) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open ${group.replaceFirstChar { it.uppercase() }} playlist")
                        }
                    }
                if (plan == null) {
                    Text("Plan asset missing — redownload app build.", color = FocusCrimson, fontSize = 12.sp)
                }
            }
        }

        // ---------- DPP Practice + hints ----------
        if (planCourses.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📝 DPP Practice (PW direct-open)", fontWeight = FontWeight.Bold, color = FocusIndigoPrimary)

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        planCourses.forEachIndexed { idx, c ->
                            FilterChip(
                                selected = idx == selectedCourseIdx,
                                onClick = { selectedCourseIdx = idx },
                                label = { Text(PwStudyPlan.run { c.subjectTitle() }, fontSize = 11.sp) }
                            )
                        }
                    }

                    selectedCourse?.chapters?.forEach { chapter ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(chapter.name.take(40), fontSize = 13.sp, color = TextPrimary, maxLines = 1)
                                Text(
                                    "${chapter.lectures.size} lec • ${chapter.dpps.size} DPP PDFs",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            if (chapter.dpps.firstOrNull()?.url?.startsWith("https://") == true) {
                                TextButton(onClick = {
                                    openUrl(context, chapter.dpps.first().url)
                                    // Start the stuck-watch: after 3 minutes the hint button unlocks
                                    hintTarget = PwChapterEntryLite(
                                        courseKey = selectedCourse.key,
                                        chapterName = chapter.name,
                                        dppFirst = chapter.dpps.first().name
                                    )
                                }) { Text("Open DPP") }
                                TextButton(onClick = {
                                    hintTarget = PwChapterEntryLite(
                                        courseKey = selectedCourse.key,
                                        chapterName = chapter.name,
                                        dppFirst = chapter.dpps.firstOrNull()?.name.orEmpty()
                                    )
                                }) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = FocusEmerald, modifier = Modifier.height(16.dp))
                                    Text("Stuck? Hint")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------- 3-minute AI hint dialog ----------
    hintTarget?.let { target ->
        var stuckSeconds by remember(target) { mutableIntStateOf(0) }
        var question by remember(target) { mutableStateOf("") }
        var hintResult by remember(target) { mutableStateOf<String?>(null) }
        var hintError by remember(target) { mutableStateOf<String?>(null) }
        var loading by remember(target) { mutableStateOf(false) }

        LaunchedEffect(target) {
            while (stuckSeconds < STUCK_THRESHOLD_SECONDS) {
                delay(1_000L)
                stuckSeconds += 1
            }
        }

        AlertDialog(
            onDismissRequest = { hintTarget = null },
            containerColor = DarkSurface,
            title = { Text("🧠 AI Hint", fontSize = 18.sp, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${target.courseKey} • ${target.chapterName.take(44)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    if (stuckSeconds < STUCK_THRESHOLD_SECONDS) {
                        val remain = STUCK_THRESHOLD_SECONDS - stuckSeconds
                        Text(
                            "Pehle ${TimeUnit.SECONDS.toMinutes(remain.toLong())}m ${remain % 60}s aur khud try karo 💪 (3-min rule)",
                            fontSize = 12.sp,
                            color = FocusAmber
                        )
                    } else {
                        Text(
                            "✅ 3 minutes up — hint unlocked!",
                            fontSize = 12.sp,
                            color = FocusEmerald
                        )
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { if (it.length <= 600) question = it },
                        label = { Text("Question kya hai? (type the problem)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4
                    )
                    hintResult?.let {
                        Text(it, fontSize = 13.sp, color = TextPrimary)
                    }
                    hintError?.let {
                        Text(it, fontSize = 12.sp, color = FocusCrimson)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !loading && stuckSeconds >= STUCK_THRESHOLD_SECONDS &&
                        question.trim().length >= 5,
                    onClick = {
                        loading = true
                        coroutineScope.launch {
                            val res = hintEngine.generateHint(
                                chapterName = target.chapterName,
                                lectureTitle = target.dppFirst,
                                questionText = question
                            )
                            loading = false
                            hintResult = res.hint
                            hintError = res.failureReason
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FocusIndigoPrimary)
                ) { Text(if (loading) "Hint aa raha..." else "Get Hint") }
            },
            dismissButton = {
                TextButton(onClick = { hintTarget = null }) { Text("Later") }
            }
        )
    }
}

/** Lightweight reference passed into the hint dialog. */
private data class PwChapterEntryLite(
    val courseKey: String,
    val chapterName: String,
    val dppFirst: String
)

