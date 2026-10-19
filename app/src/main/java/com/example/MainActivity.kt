package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.data.FirestoreRepository
import com.example.data.PreferencesManager
import com.example.data.PwPlanImporter
import com.example.data.PwStudyPlan
import com.example.model.Course
import com.example.model.DailyPlan
import com.example.model.Lecture
import com.example.model.UserProfile
import com.example.service.HardcoreAppBlockerService
import com.example.ui.AuthScreen
import com.example.ui.CoursesTrackerSection
import com.example.ui.HardcoreFocusLockSection
import com.example.ui.LectureVerificationDialog
import com.example.ui.PermissionWizardScreen
import com.example.ui.RevisionPlannerSection
import com.example.ui.StudyOnboardingWizard
import com.example.ui.SyncAndStatsSection
import com.example.ui.TodayPlannerSection
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FocusEmerald
import com.example.ui.theme.FocusIndigoDark
import com.example.ui.theme.FocusIndigoPrimary
import com.example.ui.theme.FocusStudyTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var initialTab: String? = null
    private var showBlockedAlert: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleLaunchIntent(intent)

        setContent {
            FocusStudyTheme {
                AppRootContent(
                    initialTargetTab = initialTab,
                    showInitialBlockedAlert = showBlockedAlert
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        initialTab = intent?.getStringExtra("OPEN_TAB")
        if (intent?.getBooleanExtra("SHOW_LOCK_OVERLAY", false) == true) {
            showBlockedAlert = true
            Toast.makeText(this, "Distraction App Blocked! Return to study focus.", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AppRootContent(
    initialTargetTab: String?,
    showInitialBlockedAlert: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val credentialManager = remember { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var hasCompletedPermissions by remember { mutableStateOf(prefs.isPermissionOnboardingCompleted) }
    var hasCompletedStudyOnboarding by remember { mutableStateOf(prefs.isStudyOnboardingCompleted) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    when {
        currentUser == null -> {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                }
            )
        }
        !hasCompletedPermissions -> {
            PermissionWizardScreen(
                onPermissionsComplete = {
                    prefs.isPermissionOnboardingCompleted = true
                    hasCompletedPermissions = true
                }
            )
        }
        !hasCompletedStudyOnboarding -> {
            StudyOnboardingWizard(
                onComplete = { dailyLecs, dailyHrs, dpps, dailyDppCount, checkIn, blocker, batchKey, batchUrl, startLec ->
                    hasCompletedStudyOnboarding = true
                }
            )
        }
        else -> {
            MainStudyDashboard(
                context = context,
                prefs = prefs,
                credentialManager = credentialManager,
                currentUserId = currentUser!!.uid,
                initialTargetTab = initialTargetTab,
                showInitialBlockedAlert = showInitialBlockedAlert,
                onSignOut = {
                    Firebase.auth.signOut()
                    coroutineScope.launch {
                        try {
                            credentialManager.clearCredentialState(ClearCredentialStateRequest())
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                    currentUser = null
                }
            )
        }
    }
}

@Composable
fun MainStudyDashboard(
    context: Context,
    prefs: PreferencesManager,
    credentialManager: CredentialManager,
    currentUserId: String,
    initialTargetTab: String?,
    showInitialBlockedAlert: Boolean,
    onSignOut: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { FirestoreRepository(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember {
        mutableIntStateOf(
            when (initialTargetTab) {
                "FOCUS_LOCK" -> 1
                "COURSES" -> 2
                "REVISION" -> 3
                "STATS" -> 4
                else -> if (showInitialBlockedAlert) 1 else 0
            }
        )
    }

    // Live Study State
    var isLockActive by remember { mutableStateOf(prefs.isHardcoreLockActive) }
    var remainingSeconds by remember { mutableLongStateOf(prefs.activeFocusRemainingSeconds) }
    var totalSeconds by remember { mutableLongStateOf(prefs.activeFocusTotalSeconds) }
    var blockedCount by remember { mutableLongStateOf(prefs.distractionsBlockedCount) }
    var breaksRemaining by remember { mutableIntStateOf(prefs.emergencyBreaksRemaining) }

    var verifyingLecture by remember { mutableStateOf<Lecture?>(null) }
    var verifyingSubject by remember { mutableStateOf("General Studies") }

    // Firestore Observers
    val userProfile by repository.observeUserProfile().collectAsState(initial = null)
    val courses by repository.observeCourses().collectAsState(initial = emptyList())
    val todayPlan by repository.observeTodayPlan().collectAsState(initial = null)

    val lecturesByCourse = remember { mutableStateMapOf<String, List<Lecture>>() }
    courses.forEach { course ->
        val lecs by repository.observeLectures(course.id).collectAsState(initial = emptyList())
        lecturesByCourse[course.id] = lecs
    }

    // Pw revision plan asset (bundled, secret-free: no API keys inside)
    val pwPlan = remember { PwStudyPlan.fromAssets(context) }

    val allLectures = remember(lecturesByCourse.values.toList()) {
        lecturesByCourse.values.flatten()
    }

    val activeLecture = remember(prefs.activeLectureId, allLectures) {
        allLectures.find { it.id == prefs.activeLectureId }
    }

    // Seed the REAL PW syllabus (plan.json asset) if user has zero courses.
    // Replaces the old demo Physics/Chemistry placeholder courses.
    LaunchedEffect(courses, currentUserId) {
        if (courses.isEmpty() && currentUserId.isNotBlank() && pwPlan != null) {
            try {
                PwPlanImporter(repository.db, currentUserId).importAll(pwPlan, userProfile)
                prefs.isStudyOnboardingCompleted = true
            } catch (e: Exception) {
                android.util.Log.e("PwPlanImport", "PW plan import failed", e)
            }
        }
    }

    // Hardcore Countdown Timer Loop
    LaunchedEffect(isLockActive) {
        while (isLockActive && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            prefs.activeFocusRemainingSeconds = remainingSeconds
            blockedCount = prefs.distractionsBlockedCount

            if (remainingSeconds <= 0) {
                // Timer finished!
                isLockActive = false
                prefs.isHardcoreLockActive = false

                // Stop service
                val stopIntent = Intent(context, HardcoreAppBlockerService::class.java).apply {
                    action = HardcoreAppBlockerService.ACTION_STOP_LOCK
                }
                context.startService(stopIntent)

                // Prompt user to verify with AI
                val completedLec = activeLecture
                if (completedLec != null) {
                    repository.updateLecture(completedLec.copy(isCompleted = true))
                    verifyingLecture = completedLec
                }

                snackbarHostState.showSnackbar("Focus session completed! Verify your lecture to record progress.")
            }
        }
    }

    // Strict No-Exit BackHandler: When lock is active, intercept back press
    BackHandler(enabled = isLockActive) {
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Hardcore Lock Active! Back is disabled until lecture is completed or verified.")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Plan") },
                    label = { Text("7 AM Plan", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FocusIndigoPrimary,
                        selectedTextColor = FocusIndigoPrimary,
                        indicatorColor = Color(0xFF312E81).copy(alpha = 0.5f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Lock") },
                    label = { Text("Focus Lock", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FocusIndigoPrimary,
                        selectedTextColor = FocusIndigoPrimary,
                        indicatorColor = Color(0xFF312E81).copy(alpha = 0.5f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Courses") },
                    label = { Text("Courses/DPP", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FocusIndigoPrimary,
                        selectedTextColor = FocusIndigoPrimary,
                        indicatorColor = Color(0xFF312E81).copy(alpha = 0.5f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Revision") },
                    label = { Text("Revision", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FocusIndigoPrimary,
                        selectedTextColor = FocusIndigoPrimary,
                        indicatorColor = Color(0xFF312E81).copy(alpha = 0.5f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )

                NavigationBarItem(
                    selected = selectedTabIndex == 4,
                    onClick = { selectedTabIndex = 4 },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("Sync & Stats", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = FocusIndigoPrimary,
                        selectedTextColor = FocusIndigoPrimary,
                        indicatorColor = Color(0xFF312E81).copy(alpha = 0.5f),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> TodayPlannerSection(
                    courses = courses,
                    allLectures = allLectures,
                    todayPlan = todayPlan,
                    onSavePlan = { plan ->
                        coroutineScope.launch {
                            repository.saveDailyPlan(plan)
                        }
                    },
                    onStartFocusSession = { lecture ->
                        prefs.activeLectureId = lecture.id
                        prefs.activeLectureTitle = lecture.title
                        prefs.activeFocusTotalSeconds = lecture.durationMinutes * 60
                        prefs.activeFocusRemainingSeconds = lecture.durationMinutes * 60
                        totalSeconds = lecture.durationMinutes * 60
                        remainingSeconds = lecture.durationMinutes * 60
                        isLockActive = true
                        prefs.isHardcoreLockActive = true

                        // Start foreground blocker service
                        val serviceIntent = Intent(context, HardcoreAppBlockerService::class.java).apply {
                            action = HardcoreAppBlockerService.ACTION_START_LOCK
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)

                        selectedTabIndex = 1
                    },
                    onOpenVerification = { lecture ->
                        val crs = courses.find { it.id == lecture.courseId }
                        verifyingSubject = crs?.subject ?: "Subject"
                        verifyingLecture = lecture
                    }
                )

                1 -> HardcoreFocusLockSection(
                    isLockActive = isLockActive,
                    remainingSeconds = remainingSeconds,
                    totalSeconds = totalSeconds,
                    activeLecture = activeLecture,
                    blockedDistractionsCount = blockedCount,
                    emergencyBreaksLeft = breaksRemaining,
                    onStartLock = { durationMins ->
                        val sec = durationMins * 60
                        totalSeconds = sec
                        remainingSeconds = sec
                        prefs.activeFocusTotalSeconds = sec
                        prefs.activeFocusRemainingSeconds = sec
                        isLockActive = true
                        prefs.isHardcoreLockActive = true

                        val serviceIntent = Intent(context, HardcoreAppBlockerService::class.java).apply {
                            action = HardcoreAppBlockerService.ACTION_START_LOCK
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    },
                    onEmergencyBreak = {
                        if (breaksRemaining > 0) {
                            breaksRemaining -= 1
                            prefs.emergencyBreaksRemaining = breaksRemaining

                            val breakIntent = Intent(context, HardcoreAppBlockerService::class.java).apply {
                                action = HardcoreAppBlockerService.ACTION_EMERGENCY_BREAK
                            }
                            context.startService(breakIntent)

                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("5-Minute Emergency Break Active. Blocker will resume soon.")
                            }
                        }
                    },
                    onOpenVerification = {
                        if (activeLecture != null) {
                            val crs = courses.find { it.id == activeLecture.courseId }
                            verifyingSubject = crs?.subject ?: "Subject"
                            verifyingLecture = activeLecture
                        } else {
                            val firstLec = allLectures.firstOrNull()
                            if (firstLec != null) {
                                verifyingLecture = firstLec
                                verifyingSubject = "General"
                            }
                        }
                    }
                )

                2 -> CoursesTrackerSection(
                    courses = courses,
                    lecturesByCourse = lecturesByCourse,
                    onAddCourse = { title, subject, totalLecs, hasDpp ->
                        coroutineScope.launch {
                            val newCourseId = repository.addCourse(
                                Course(
                                    title = title,
                                    subject = subject,
                                    totalLectures = totalLecs,
                                    hasDppDefault = hasDpp
                                )
                            )
                            // Automatically add Lecture 1
                            repository.addLecture(
                                Lecture(
                                    courseId = newCourseId,
                                    lectureNumber = 1L,
                                    title = "Introduction & Chapter 1 Overview",
                                    durationMinutes = 60L,
                                    hasDpp = hasDpp,
                                    revisionDueDays = 3L
                                )
                            )
                        }
                    },
                    onAddLecture = { courseId, title, number, duration, hasDpp ->
                        coroutineScope.launch {
                            repository.addLecture(
                                Lecture(
                                    courseId = courseId,
                                    lectureNumber = number,
                                    title = title,
                                    durationMinutes = duration,
                                    hasDpp = hasDpp,
                                    revisionDueDays = 3L
                                )
                            )
                        }
                    },
                    onToggleDpp = { lec ->
                        coroutineScope.launch {
                            repository.updateLecture(lec.copy(dppSolved = !lec.dppSolved))
                        }
                    },
                    onToggleCompleted = { lec ->
                        coroutineScope.launch {
                            repository.updateLecture(lec.copy(isCompleted = !lec.isCompleted))
                        }
                    },
                    onOpenVerification = { lec, subject ->
                        verifyingSubject = subject
                        verifyingLecture = lec
                    }
                )

                3 -> RevisionPlannerSection(
                    plan = pwPlan,
                    courses = courses,
                    lecturesByCourse = lecturesByCourse
                )

                4 -> SyncAndStatsSection(
                    userProfile = userProfile,
                    distractionsBlockedCount = blockedCount,
                    lastSyncTime = prefs.lastSyncTimestamp,
                    onForceSync = {
                        repository.logSync(
                            studyMinutes = (totalSeconds - remainingSeconds) / 60,
                            blockedAttempts = blockedCount,
                            deviceState = if (isLockActive) "LOCKED" else "STANDBY"
                        )
                        prefs.lastSyncTimestamp = System.currentTimeMillis()
                    },
                    onSignOut = onSignOut
                )
            }

            // Verification Dialog overlay
            if (verifyingLecture != null) {
                LectureVerificationDialog(
                    lecture = verifyingLecture!!,
                    subject = verifyingSubject,
                    onDismiss = { verifyingLecture = null },
                    onVerificationPassed = { proofType, score ->
                        val target = verifyingLecture!!
                        coroutineScope.launch {
                            repository.updateLecture(
                                target.copy(
                                    isCompleted = true,
                                    isVerified = true,
                                    verificationType = proofType,
                                    verificationScore = score
                                )
                            )

                            // If this was the active lecture, unlock hardcore focus mode
                            if (target.id == prefs.activeLectureId || isLockActive) {
                                isLockActive = false
                                prefs.isHardcoreLockActive = false
                                val stopIntent = Intent(context, HardcoreAppBlockerService::class.java).apply {
                                    action = HardcoreAppBlockerService.ACTION_STOP_LOCK
                                }
                                context.startService(stopIntent)
                            }

                            // Update user stats in Firestore
                            val currentCount = userProfile?.completedLecturesCount ?: 0L
                            repository.saveUserProfile(
                                (userProfile ?: UserProfile()).copy(
                                    completedLecturesCount = currentCount + 1,
                                    totalStudyMinutes = (userProfile?.totalStudyMinutes ?: 0L) + target.durationMinutes
                                )
                            )

                            verifyingLecture = null
                            snackbarHostState.showSnackbar("Lecture verified! Device unlocked and progress saved.")
                        }
                    }
                )
            }
        }
    }
}
