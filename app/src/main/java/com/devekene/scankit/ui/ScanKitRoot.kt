package com.devekene.scankit.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devekene.scankit.R
import com.devekene.scankit.data.model.BarcodeFormatType
import com.devekene.scankit.data.model.ScanMode
import com.devekene.scankit.data.repository.QrHistoryRepository
import com.devekene.scankit.ui.components.qrTypeBadgeStyle
import com.devekene.scankit.ui.screens.CreateScreen
import com.devekene.scankit.ui.screens.HistoryScreen
import com.devekene.scankit.ui.screens.ScanScreen
import com.devekene.scankit.ui.viewmodel.CreateViewModel
import com.devekene.scankit.ui.viewmodel.HistoryViewModel
import com.devekene.scankit.ui.viewmodel.ScanViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val ROUTE_QR_SCAN = "qr_scan"
const val ROUTE_CREATE = "create"
const val ROUTE_BARCODE_SCAN = "barcode_scan"
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
        route = ROUTE_QR_SCAN,
        label = "QR Scan",
        selectedIcon = Icons.Rounded.QrCodeScanner,
        unselectedIcon = Icons.Outlined.QrCodeScanner,
        testTag = "nav_qr_scan"
    ),
    ScanKitNavItem(
        route = ROUTE_CREATE,
        label = "Create",
        selectedIcon = Icons.Rounded.QrCode2,
        unselectedIcon = Icons.Outlined.QrCode2,
        testTag = "nav_create"
    ),
    ScanKitNavItem(
        route = ROUTE_BARCODE_SCAN,
        label = "Barcode Scan",
        selectedIcon = Icons.Rounded.ViewWeek,
        unselectedIcon = Icons.Outlined.ViewWeek,
        testTag = "nav_barcode_scan"
    )
)

@Composable
fun ScanKitRoot(
    repository: QrHistoryRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_QR_SCAN

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val scanViewModel: ScanViewModel = viewModel(factory = ScanViewModel.factory(repository))
    val createViewModel: CreateViewModel = viewModel(factory = CreateViewModel.factory(repository))
    val historyViewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(repository))
    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()

    var showSplash by rememberSaveable { mutableStateOf(true) }

    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    val navigateTo: (String) -> Unit = { route ->
        if (route != currentRoute) {
            scanViewModel.setTorch(false)
            if (currentRoute == ROUTE_QR_SCAN || currentRoute == ROUTE_BARCODE_SCAN) {
                scanViewModel.dismissResult()
            }
        }
        if (route == ROUTE_HISTORY) {
            navController.navigate(ROUTE_HISTORY) {
                launchSingleTop = true
            }
        } else {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val openDrawer: () -> Unit = {
        scope.launch { drawerState.open() }
    }

    val closeDrawerAndNavigate: (String) -> Unit = { route ->
        scope.launch {
            drawerState.close()
            navigateTo(route)
        }
    }

    val editInGenerator: (String, BarcodeFormatType?) -> Unit = { rawPayload, barcodeFormat ->
        createViewModel.prefillFromRawPayload(rawPayload, barcodeFormat)
        navigateTo(ROUTE_CREATE)
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen ||
                currentRoute == ROUTE_QR_SCAN ||
                currentRoute == ROUTE_BARCODE_SCAN,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier
                        .width(304.dp)
                        .testTag("left_navigation_drawer")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 20.dp)
                    ) {
                        // Drawer Brand Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.scankit_app_icon_1795000689141),
                                contentDescription = "ScanKit App Icon",
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Column {
                                Text(
                                    text = "ScanKit",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "QR & Barcode Studio",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Primary History destination moved into Hamburger Menu
                        NavigationDrawerItem(
                            selected = currentRoute == ROUTE_HISTORY,
                            onClick = { closeDrawerAndNavigate(ROUTE_HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.History,
                                    contentDescription = "History"
                                )
                            },
                            label = { Text("History") },
                            badge = {
                                if (historyState.totalCount > 0) {
                                    Badge {
                                        Text(historyState.totalCount.toString())
                                    }
                                }
                            },
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .testTag("drawer_history_item")
                        )

                        // Quick Recent History preview inside the left drawer
                        if (historyState.entries.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Recent Scans & Codes",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                            historyState.entries.take(4).forEach { entry ->
                                val badge = qrTypeBadgeStyle(entry.parsedType())
                                val subtitleLabel = entry.parsedBarcodeFormat()?.label ?: badge.label
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            historyViewModel.selectEntry(entry)
                                            closeDrawerAndNavigate(ROUTE_HISTORY)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("drawer_recent_item_${entry.id}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        color = badge.containerColor,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = badge.icon,
                                                contentDescription = subtitleLabel,
                                                tint = badge.contentColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.displayTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$subtitleLabel • ${entry.displaySubtitle}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Tools",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )

                        navItems.forEach { item ->
                            NavigationDrawerItem(
                                selected = currentRoute == item.route,
                                onClick = { closeDrawerAndNavigate(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = item.selectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier
                                    .padding(NavigationDrawerItemDefaults.ItemPadding)
                                    .testTag("drawer_${item.testTag}")
                            )
                        }
                    }
                }
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 600.dp
                val showTopLevelNav = currentRoute != ROUTE_HISTORY

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useNavRail && showTopLevelNav) {
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
                        if (useNavRail && showTopLevelNav) {
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
                                startDestination = ROUTE_QR_SCAN,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(ROUTE_QR_SCAN) {
                                    ScanScreen(
                                        viewModel = scanViewModel,
                                        scanMode = ScanMode.QR,
                                        onOpenDrawer = openDrawer,
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
                                composable(ROUTE_BARCODE_SCAN) {
                                    ScanScreen(
                                        viewModel = scanViewModel,
                                        scanMode = ScanMode.BARCODE,
                                        onOpenDrawer = openDrawer,
                                        onEditInGenerator = editInGenerator,
                                        onShowMessage = showMessage
                                    )
                                }
                                composable(ROUTE_HISTORY) {
                                    HistoryScreen(
                                        viewModel = historyViewModel,
                                        onBackClick = { navController.popBackStack() },
                                        onNavigateToScan = { navigateTo(ROUTE_QR_SCAN) },
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

        AnimatedVisibility(
            visible = showSplash,
            exit = fadeOut(animationSpec = tween(durationMillis = 320))
        ) {
            ScanKitSplashOverlay(
                onSplashFinished = { showSplash = false }
            )
        }
    }
}

@Composable
private fun ScanKitSplashOverlay(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.86f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            alpha.animateTo(1f, animationSpec = tween(durationMillis = 280))
        }
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
            )
        }
        delay(850)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1A34))
            .testTag("splash_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Image(
                painter = painterResource(id = R.drawable.scankit_app_icon_1795000689141),
                contentDescription = "ScanKit Splash Icon",
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "ScanKit",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "QR & Barcode Studio",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
