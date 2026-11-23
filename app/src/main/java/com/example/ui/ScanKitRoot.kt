package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.QrHistoryRepository
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.ScanScreen
import com.example.ui.viewmodel.CreateViewModel
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ScanViewModel
import kotlinx.coroutines.launch

const val ROUTE_SCAN = "scan"
const val ROUTE_CREATE = "create"
const val ROUTE_HISTORY = "history"

private data class ScanKitNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navItems = listOf(
    ScanKitNavItem(
        route = ROUTE_SCAN,
        label = "Scan",
        selectedIcon = Icons.Rounded.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_scan"
    ),
    ScanKitNavItem(
        route = ROUTE_CREATE,
        label = "Create",
        selectedIcon = Icons.Rounded.QrCode2,
        unselectedIcon = Icons.Outlined.QrCode2,
        testTag = "nav_create"
    ),
    ScanKitNavItem(
        route = ROUTE_HISTORY,
        label = "History",
        selectedIcon = Icons.Rounded.History,
        unselectedIcon = Icons.Outlined.History,
        testTag = "nav_history"
    )
)

@Composable
fun ScanKitRoot(
    repository: QrHistoryRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_SCAN

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scanViewModel: ScanViewModel = viewModel(factory = ScanViewModel.factory(repository))
    val createViewModel: CreateViewModel = viewModel(factory = CreateViewModel.factory(repository))
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(repository))

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        if (route != ROUTE_SCAN) {
            scanViewModel.setTorch(false)
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val editInGenerator: (String) -> Unit = { rawPayload ->
        createViewModel.prefillFromRawPayload(rawPayload)
        navigateTo(ROUTE_CREATE)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_SCAN,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(ROUTE_SCAN) {
                            ScanScreen(
                                viewModel = scanViewModel,
                                onEditInGenerator = editInGenerator,
                                onShowMessage = showMessage
                            )
                        }
                        composable(ROUTE_CREATE) {
                            CreateScreen(
                                viewModel = createViewModel,
                                onShowMessage = showMessage
                            )
                        }
                        composable(ROUTE_HISTORY) {
                            HistoryScreen(
                                viewModel = historyViewModel,
                                onNavigateToScan = { navigateTo(ROUTE_SCAN) },
                                onNavigateToCreate = { navigateTo(ROUTE_CREATE) },
                                onEditInGenerator = editInGenerator,
                                onShowMessage = showMessage
                            )
                        }
                    }
                }
            }
        }
    }
}
