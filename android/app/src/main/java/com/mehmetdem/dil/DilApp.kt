package com.mehmetdem.dil

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mehmetdem.dil.core.data.PreferencesLessonLocalRepository
import com.mehmetdem.dil.core.designsystem.DilTeal
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.core.model.StoredLesson
import com.mehmetdem.dil.feature.home.HomeScreen
import com.mehmetdem.dil.feature.lesson.LessonDetailScreen
import com.mehmetdem.dil.feature.lesson.LessonWorkspaceScreen
import com.mehmetdem.dil.feature.lesson.NewLessonScreen
import com.mehmetdem.dil.feature.library.LibraryScreen
import com.mehmetdem.dil.feature.onboarding.PermissionOnboardingScreen
import com.mehmetdem.dil.feature.onboarding.WelcomeOnboardingScreen
import com.mehmetdem.dil.feature.profile.ProfileScreen
import com.mehmetdem.dil.feature.profile.VoiceControlScreen

private object Routes {
    const val Home = "home"
    const val Create = "create"
    const val Library = "library"
    const val Profile = "profile"
    const val Workspace = "workspace/{lessonId}"
    const val Detail = "detail/{lessonId}"
    const val Voice = "voice"

    fun workspace(lessonId: String) = "workspace/$lessonId"
    fun detail(lessonId: String) = "detail/$lessonId"
}

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

private val bottomDestinations = listOf(
    BottomDestination(Routes.Home, "Ana Sayfa", Icons.Filled.Home),
    BottomDestination(Routes.Create, "Oluştur", Icons.Filled.AddCircleOutline),
    BottomDestination(Routes.Library, "Kütüphane", Icons.Outlined.AutoStories),
    BottomDestination(Routes.Profile, "Profil", Icons.Filled.PersonOutline),
)

@Composable
fun DilApp() {
    val context = LocalContext.current
    val onboardingPreferences = remember { context.getSharedPreferences("onboarding", 0) }
    var onboardingPage by rememberSaveable {
        mutableIntStateOf(if (onboardingPreferences.getBoolean("completed", false)) 0 else 1)
    }
    val finishOnboarding = {
        onboardingPage = 0
        onboardingPreferences.edit().putBoolean("completed", true).apply()
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        finishOnboarding()
    }

    if (onboardingPage == 1) {
        WelcomeOnboardingScreen(
            onContinue = { onboardingPage = 2 },
            onSkip = finishOnboarding,
        )
        return
    }
    if (onboardingPage == 2) {
        PermissionOnboardingScreen(
            onRequestPermissions = {
                permissionLauncher.launch(buildList {
                    add(Manifest.permission.RECORD_AUDIO)
                }.toTypedArray())
            },
            onLater = finishOnboarding,
        )
        return
    }

    val repository = remember { PreferencesLessonLocalRepository(context) }
    var lessons by remember { mutableStateOf(repository.all()) }
    var requestedSourceKind by rememberSaveable { mutableStateOf(SourceKind.YOUTUBE) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.Home
    val fullScreen = currentRoute in setOf(Routes.Create, Routes.Workspace, Routes.Detail, Routes.Voice)

    fun refreshLessons() {
        lessons = repository.all()
    }

    fun deleteLesson(id: String) {
        repository.delete(id)
        refreshLessons()
    }

    fun navigateTop(route: String) {
        navController.navigate(route) {
            popUpTo(Routes.Home) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        topBar = {
            if (currentRoute == Routes.Home) {
                Row(
                    Modifier.fillMaxWidth().height(64.dp).background(Color.White).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("▣", color = DilTeal, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "  Uygulamam Dil",
                        color = DilTeal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        bottomBar = {
            if (!fullScreen) {
                Row(Modifier.fillMaxWidth().height(72.dp).background(Color.White)) {
                    bottomDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (destination.route == Routes.Create) requestedSourceKind = SourceKind.YOUTUBE
                                navigateTop(destination.route)
                            },
                            icon = { Icon(destination.icon, destination.label) },
                            label = { Text(destination.label) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = DilTeal,
                                selectedTextColor = DilTeal,
                            ),
                        )
                    }
                }
            }
        },
        containerColor = Color.White,
    ) { padding ->
        NavHost(navController, startDestination = Routes.Home, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(Routes.Home) {
                HomeScreen(
                    lessons = lessons,
                    onCreateYouTubeLesson = {
                        requestedSourceKind = SourceKind.YOUTUBE
                        navController.navigate(Routes.Create)
                    },
                    onCreatePdfLesson = {
                        requestedSourceKind = SourceKind.PDF
                        navController.navigate(Routes.Create)
                    },
                    onOpenLesson = { navController.navigate(Routes.detail(it)) },
                    onDeleteLesson = { deleteLesson(it) },
                    onOpenLibrary = { navigateTop(Routes.Library) },
                )
            }
            composable(Routes.Create) {
                NewLessonScreen(
                    initialSourceKind = requestedSourceKind,
                    onBack = { navController.popBackStack() },
                    onCreate = { config ->
                        val lesson = repository.create(config)
                        refreshLessons()
                        navController.navigate(Routes.workspace(lesson.id))
                    },
                )
            }
            composable(
                route = Routes.Workspace,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val lesson = lessons.findLesson(entry.arguments?.getString("lessonId"))
                if (lesson == null) {
                    MissingLesson(onBack = { navController.popBackStack() })
                } else {
                    LessonWorkspaceScreen(
                        lesson = lesson,
                        blocks = lesson.blocks,
                        onBack = { navController.popBackStack() },
                        onOpenDetails = { navController.navigate(Routes.detail(lesson.id)) },
                        onDelete = {
                            deleteLesson(lesson.id)
                            navController.popBackStack(Routes.Home, inclusive = false)
                        },
                    )
                }
            }
            composable(Routes.Library) {
                LibraryScreen(
                    lessons = lessons,
                    onOpenLesson = { navController.navigate(Routes.detail(it)) },
                    onDeleteLesson = { deleteLesson(it) },
                )
            }
            composable(Routes.Profile) {
                ProfileScreen(
                    lessonCount = lessons.size,
                    onVoiceSettings = { navController.navigate(Routes.Voice) },
                )
            }
            composable(Routes.Voice) { VoiceControlScreen(onBack = { navController.popBackStack() }) }
            composable(
                route = Routes.Detail,
                arguments = listOf(navArgument("lessonId") { type = NavType.StringType }),
            ) { entry ->
                val lesson = lessons.findLesson(entry.arguments?.getString("lessonId"))
                if (lesson == null) {
                    MissingLesson(onBack = { navController.popBackStack() })
                } else {
                    LessonDetailScreen(
                        lesson = lesson,
                        onBack = { navController.popBackStack() },
                        onContinue = { navController.navigate(Routes.workspace(lesson.id)) },
                        onDelete = {
                            deleteLesson(lesson.id)
                            navController.popBackStack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MissingLesson(onBack: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onBack) {
            Text("Ders bulunamadı · Geri dön", modifier = Modifier.padding(12.dp))
        }
    }
}

private fun List<StoredLesson>.findLesson(id: String?): StoredLesson? = firstOrNull { it.id == id }
