package com.example.myapplication

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.ui.screens.BadgeRoomScreen
import com.example.myapplication.ui.screens.DistrictMapScreen
import com.example.myapplication.ui.screens.HeroListScreen
import com.example.myapplication.ui.screens.QuizScreen
import com.example.myapplication.ui.screens.StoryViewerScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.Language as AppLanguage
import com.example.myapplication.viewmodel.StoryViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: StoryViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!NammaKatheyApplication.userSession(this).canAccessMainContent()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsState()
            val navController = rememberNavController()
            val context = LocalContext.current
            var accountMenuOpen by remember { mutableStateOf(false) }
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            LaunchedEffect(currentLanguage) {
                val locale = when (currentLanguage) {
                    AppLanguage.EN -> Locale.ENGLISH
                    AppLanguage.KN -> Locale("kn", "IN")
                }
                val currentLocale = context.resources.configuration.locales[0]
                if (currentLocale.language != locale.language) {
                    val config = Configuration(context.resources.configuration)
                    config.setLocale(locale)
                    Locale.setDefault(locale)
                    context.resources.updateConfiguration(config, context.resources.displayMetrics)
                    recreate()
                }
            }

            MyApplicationTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.app_name)) },
                            actions = {
                                Box {
                                    IconButton(onClick = { accountMenuOpen = true }) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.sign_out),
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = accountMenuOpen,
                                        onDismissRequest = { accountMenuOpen = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.sign_out))
                                            },
                                            onClick = {
                                                accountMenuOpen = false
                                                viewModel.signOutToAuth(context as ComponentActivity)
                                            },
                                        )
                                    }
                                }
                                IconButton(onClick = { viewModel.toggleLanguage() }) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = stringResource(R.string.toggle_language),
                                    )
                                }
                                TextButton(onClick = { viewModel.toggleLanguage() }) {
                                    val label = when (currentLanguage) {
                                        AppLanguage.EN -> stringResource(R.string.language_label_kn)
                                        AppLanguage.KN -> stringResource(R.string.language_label_en)
                                    }
                                    Text(label)
                                }
                            },
                        )
                    },
                    bottomBar = {
                        val route = currentDestination?.route.orEmpty()
                        val showBottomBar = route == "map" ||
                            route.startsWith("heroes") ||
                            route == "badges"

                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                                    label = {
                                        Text(
                                            if (currentLanguage == AppLanguage.EN) {
                                                stringResource(R.string.nav_map_en)
                                            } else {
                                                stringResource(R.string.nav_map_kn)
                                            },
                                        )
                                    },
                                    selected = route == "map",
                                    onClick = {
                                        navController.navigate("map") {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                                    label = {
                                        Text(
                                            if (currentLanguage == AppLanguage.EN) {
                                                stringResource(R.string.nav_heroes_en)
                                            } else {
                                                stringResource(R.string.nav_heroes_kn)
                                            },
                                        )
                                    },
                                    selected = route.startsWith("heroes"),
                                    onClick = {
                                        navController.navigate("heroes/all") {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.MilitaryTech, contentDescription = null) },
                                    label = {
                                        Text(
                                            if (currentLanguage == AppLanguage.EN) {
                                                stringResource(R.string.nav_badges_en)
                                            } else {
                                                stringResource(R.string.nav_badges_kn)
                                            },
                                        )
                                    },
                                    selected = route == "badges",
                                    onClick = {
                                        navController.navigate("badges") {
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "map",
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable("map") {
                            DistrictMapScreen(
                                viewModel = viewModel,
                                onDistrictSelected = { districtKey ->
                                    navController.navigate("heroes/${Uri.encode(districtKey)}") {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                        composable(
                            route = "heroes/{district}",
                            arguments = listOf(
                                navArgument("district") {
                                    type = NavType.StringType
                                    defaultValue = "all"
                                },
                            ),
                        ) { backStackEntry ->
                            val raw = backStackEntry.arguments?.getString("district").orEmpty()
                            val decoded = Uri.decode(raw)
                            HeroListScreen(
                                viewModel = viewModel,
                                districtFilterEnglish = if (decoded.equals("all", ignoreCase = true)) null else decoded,
                                onHeroClick = { id -> navController.navigate("story/$id") },
                            )
                        }
                        composable("badges") {
                            BadgeRoomScreen(viewModel = viewModel)
                        }
                        composable(
                            route = "story/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType }),
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: 0
                            StoryViewerScreen(
                                viewModel = viewModel,
                                initialStoryId = id,
                                onStartQuiz = {
                                    viewModel.startQuiz(it)
                                    navController.navigate("quiz")
                                },
                            )
                        }
                        composable("quiz") {
                            QuizScreen(
                                viewModel = viewModel,
                                onFinish = {
                                    navController.popBackStack()
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
