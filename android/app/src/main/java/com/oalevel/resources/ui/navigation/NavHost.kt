package com.oalevel.resources.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.oalevel.resources.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Browse : Screen("browse/{nodeId}") {
        fun withId(nodeId: String) = "browse/$nodeId"
    }
    object PdfViewer : Screen("pdf/{nodeId}/{name}") {
        fun withId(nodeId: String, name: String) =
            "pdf/${java.net.URLEncoder.encode(nodeId, "UTF-8")}/${java.net.URLEncoder.encode(name, "UTF-8")}"
    }
    object Search : Screen("search")
    object Downloads : Screen("downloads")
    object Favourites : Screen("favourites")
    object Recent : Screen("recent")
    object ContinueReading : Screen("continue_reading")
    object Settings : Screen("settings")
    object AiChat : Screen("ai_chat")
}

@Composable
fun OALevelNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onLevelClick = { node -> navController.navigate(Screen.Browse.withId(node.id)) },
                onSearchClick = { navController.navigate(Screen.Search.route) },
                onDownloadsClick = { navController.navigate(Screen.Downloads.route) },
                onFavouritesClick = { navController.navigate(Screen.Favourites.route) },
                onRecentClick = { navController.navigate(Screen.Recent.route) },
                onContinueReadingClick = { navController.navigate(Screen.ContinueReading.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                onAiChatClick = { navController.navigate(Screen.AiChat.route) }
            )
        }

        composable(
            Screen.Browse.route,
            arguments = listOf(navArgument("nodeId") { type = NavType.StringType })
        ) { back ->
            val nodeId = back.arguments?.getString("nodeId") ?: return@composable
            BrowseScreen(
                nodeId = nodeId,
                onNodeClick = { node ->
                    if (node.type == "folder") {
                        navController.navigate(Screen.Browse.withId(node.id))
                    } else {
                        navController.navigate(Screen.PdfViewer.withId(node.id, node.name))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            Screen.PdfViewer.route,
            arguments = listOf(
                navArgument("nodeId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { back ->
            val nodeId = java.net.URLDecoder.decode(back.arguments?.getString("nodeId") ?: "", "UTF-8")
            val name = java.net.URLDecoder.decode(back.arguments?.getString("name") ?: "", "UTF-8")
            PdfViewerScreen(
                nodeId = nodeId,
                displayName = name,
                onBack = { navController.popBackStack() },
                onOpenAnother = { id, n ->
                    navController.navigate(Screen.PdfViewer.withId(id, n)) {
                        popUpTo(Screen.PdfViewer.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onResultClick = { result ->
                    if (result.type == "folder") {
                        navController.navigate(Screen.Browse.withId(result.id))
                    } else {
                        navController.navigate(Screen.PdfViewer.withId(result.id, result.name))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onOpenPdf = { download ->
                    navController.navigate(Screen.PdfViewer.withId(download.resourceId, download.name))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Favourites.route) {
            FavouritesScreen(
                onItemClick = { fav ->
                    if (fav.type == "folder") {
                        navController.navigate(Screen.Browse.withId(fav.resourceId))
                    } else {
                        navController.navigate(Screen.PdfViewer.withId(fav.resourceId, fav.name))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Recent.route) {
            RecentScreen(
                onItemClick = { item ->
                    if (item.type == "folder") {
                        navController.navigate(Screen.Browse.withId(item.resourceId))
                    } else {
                        navController.navigate(Screen.PdfViewer.withId(item.resourceId, item.name))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ContinueReading.route) {
            ContinueReadingScreen(
                onPdfClick = { progress ->
                    navController.navigate(Screen.PdfViewer.withId(progress.resourceId, progress.name))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AiChat.route) {
            AiChatScreen(onBack = { navController.popBackStack() })
        }
    }
}
