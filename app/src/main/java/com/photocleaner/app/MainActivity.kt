package com.photocleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.photocleaner.app.ui.screens.AlbumsScreen
import com.photocleaner.app.ui.screens.HomeScreen
import com.photocleaner.app.ui.screens.ScanScreen
import com.photocleaner.app.ui.screens.SettingsScreen
import com.photocleaner.app.ui.screens.StatsScreen
import com.photocleaner.app.ui.screens.TrashScreen
import com.photocleaner.app.ui.theme.PhotoCleanerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoCleanerTheme {
                PhotoCleanerNav()
            }
        }
    }
}

sealed class Screen(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    data object Home : Screen("home", R.string.home, Icons.Default.Home)
    data object Scan : Screen("scan", R.string.scan, Icons.Default.Search)
    data object Albums : Screen("albums", R.string.albums, Icons.Default.PhotoLibrary)
    data object Trash : Screen("trash", R.string.trash, Icons.Default.Delete)
    data object Stats : Screen("stats", R.string.stats, Icons.Default.Analytics)
    data object Settings : Screen("settings", R.string.settings, Icons.Default.Settings)
}

@Composable
fun PhotoCleanerNav() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Home, Screen.Scan, Screen.Albums,
        Screen.Trash, Screen.Stats, Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.labelRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Scan.route) { ScanScreen() }
            composable(Screen.Albums.route) { AlbumsScreen() }
            composable(Screen.Trash.route) { TrashScreen() }
            composable(Screen.Stats.route) { StatsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
