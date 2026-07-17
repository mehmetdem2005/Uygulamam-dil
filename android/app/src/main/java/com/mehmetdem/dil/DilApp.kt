package com.mehmetdem.dil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mehmetdem.dil.core.model.LessonSessionConfig
import com.mehmetdem.dil.core.model.SourceKind
import com.mehmetdem.dil.feature.home.HomeScreen
import com.mehmetdem.dil.feature.lesson.LessonWorkspaceScreen
import com.mehmetdem.dil.feature.lesson.NewLessonScreen
import kotlinx.coroutines.launch

private object Routes {
    const val Home = "home"
    const val Lessons = "lessons"
    const val Create = "create"
    const val Downloads = "downloads"
    const val Subscription = "subscription"
    const val Profile = "profile"
    const val Settings = "settings"
    const val Admin = "admin"
    const val Workspace = "workspace"
}

private data class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomDestinations = listOf(
    AppDestination(Routes.Home, "Ana sayfa", Icons.Filled.Home),
    AppDestination(Routes.Lessons, "Dersler", Icons.AutoMirrored.Filled.MenuBook),
    AppDestination(Routes.Create, "Oluştur", Icons.Filled.AddCircle),
    AppDestination(Routes.Downloads, "İndirilen", Icons.Filled.Download),
    AppDestination(Routes.Profile, "Profil", Icons.Filled.Person),
)

private val drawerDestinations = listOf(
    AppDestination(Routes.Home, "Ana sayfa", Icons.Filled.Home),
    AppDestination(Routes.Lessons, "Derslerim", Icons.AutoMirrored.Filled.MenuBook),
    AppDestination(Routes.Create, "Yeni ders", Icons.Filled.AddCircle),
    AppDestination(Routes.Downloads, "İndirilenler", Icons.Filled.Download),
    AppDestination(Routes.Subscription, "Abonelik", Icons.Filled.Payments),
    AppDestination(Routes.Settings, "Ayarlar", Icons.Filled.Settings),
    AppDestination(Routes.Admin, "Yönetici paneli", Icons.Filled.AdminPanelSettings),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DilApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.Home
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var requestedSourceKind by remember { mutableStateOf(SourceKind.YOUTUBE) }
    var activeDraft by remember { mutableStateOf<LessonSessionConfig?>(null) }
    var workspaceRunning by remember { mutableStateOf(false) }

    val isFullScreenRoute = currentRoute == Routes.Create || currentRoute == Routes.Workspace
    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isFullScreenRoute,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text("Uygulamam Dil", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Kaynağından, senin formatında öğren.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 14.dp),
                    )
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    drawerDestinations.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(destination.label) },
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (destination.route == Routes.Create) {
                                    requestedSourceKind = SourceKind.YOUTUBE
                                }
                                navigateTopLevel(destination.route)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                        )
                    }
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                if (!isFullScreenRoute) {
                    TopAppBar(
                        title = { Text(titleFor(currentRoute)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menüyü aç")
                            }
                        },
                        actions = {
                            IconButton(onClick = { }) {
                                Icon(Icons.Filled.NotificationsNone, contentDescription = "Bildirimler")
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (!isFullScreenRoute) {
                    NavigationBar {
                        bottomDestinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    if (destination.route == Routes.Create) {
                                        requestedSourceKind = SourceKind.YOUTUBE
                                    }
                                    navigateTopLevel(destination.route)
                                },
                                icon = { Icon(destination.icon, contentDescription = destination.label) },
                                label = { Text(destination.label) },
                                alwaysShowLabel = false,
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.Home,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Routes.Home) {
                    HomeScreen(
                        onCreateLesson = {
                            requestedSourceKind = SourceKind.YOUTUBE
                            navController.navigate(Routes.Create)
                        },
                        onCreateYouTubeLesson = {
                            requestedSourceKind = SourceKind.YOUTUBE
                            navController.navigate(Routes.Create)
                        },
                        onCreatePdfLesson = {
                            requestedSourceKind = SourceKind.PDF
                            navController.navigate(Routes.Create)
                        },
                    )
                }
                composable(Routes.Create) {
                    NewLessonScreen(
                        initialSourceKind = requestedSourceKind,
                        onBack = { navController.popBackStack() },
                        onCreate = { config ->
                            activeDraft = config
                            workspaceRunning = false
                            navController.navigate(Routes.Workspace)
                        },
                    )
                }
                composable(Routes.Workspace) {
                    val config = activeDraft
                    if (config == null) {
                        EmptyDestination(
                            title = "Ders taslağı bulunamadı",
                            description = "Kaynak ve format seçerek yeni bir ders oluştur.",
                        )
                    } else {
                        LessonWorkspaceScreen(
                            config = config,
                            blocks = emptyList(),
                            isRunning = workspaceRunning,
                            onBack = { navController.popBackStack() },
                            onPause = { workspaceRunning = false },
                            onResume = { workspaceRunning = false },
                            onStop = { workspaceRunning = false },
                            onMore = { },
                        )
                    }
                }
                composable(Routes.Lessons) {
                    EmptyDestination(
                        title = "Derslerim",
                        description = "İşlenen ve çevrimdışı kaydedilen dersler bu bölümde listelenecek.",
                    )
                }
                composable(Routes.Downloads) {
                    EmptyDestination(
                        title = "İndirilenler",
                        description = "Çevrimdışı kartlar, PDF bölümleri ve ses paketleri burada yönetilecek.",
                    )
                }
                composable(Routes.Subscription) {
                    EmptyDestination(
                        title = "Abonelik",
                        description = "Google Play ürünleri bağlandığında plan ve kullanım hakları burada gösterilecek.",
                    )
                }
                composable(Routes.Profile) {
                    EmptyDestination(
                        title = "Profil",
                        description = "Supabase Auth bağlantısı yapıldığında hesap ve cihaz senkronizasyonu burada yönetilecek.",
                    )
                }
                composable(Routes.Settings) {
                    EmptyDestination(
                        title = "Ayarlar",
                        description = "Dil, ses, mikrofon, indirme ve erişilebilirlik tercihleri burada yer alacak.",
                    )
                }
                composable(Routes.Admin) {
                    EmptyDestination(
                        title = "Yönetici paneli",
                        description = "Admin rolü ve analitik API bağlandığında trafik, maliyet ve büyüme panelleri açılacak.",
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDestination(title: String, description: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun titleFor(route: String): String = when (route) {
    Routes.Home -> "Merhaba"
    Routes.Lessons -> "Derslerim"
    Routes.Downloads -> "İndirilenler"
    Routes.Subscription -> "Abonelik"
    Routes.Profile -> "Profil"
    Routes.Settings -> "Ayarlar"
    Routes.Admin -> "Yönetici"
    else -> "Uygulamam Dil"
}
