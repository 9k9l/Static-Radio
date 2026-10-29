package eg.radio.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import eg.radio.StaticRadioApp
import eg.radio.data.local.StationDao
import eg.radio.data.settings.ImageShape
import eg.radio.data.toResolved
import eg.radio.ui.addstation.AddStationScreen
import eg.radio.ui.common.LocalPlayerBarBottomInset
import eg.radio.ui.common.PlayerBarReservedHeight
import eg.radio.ui.editstation.EditStationScreen
import eg.radio.ui.home.HomeScreen
import eg.radio.ui.home.HomeViewModel
import eg.radio.ui.home.StationViewMode
import eg.radio.ui.mixes.MixFormScreen
import eg.radio.ui.mixes.MixesScreen
import eg.radio.ui.player.PersistentPlayerBar
import eg.radio.ui.player.PlayerViewModel
import eg.radio.ui.settings.GenreVocabularyScreen
import eg.radio.ui.settings.SettingsScreen
import eg.radio.ui.settings.TagVocabularyScreen
import eg.radio.ui.theme.toComposeShape

private sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object Settings : Destination("settings")
    data object AddStation : Destination("add_station")
    data object EditStation : Destination("edit_station/{stationId}") {
        fun createRoute(stationId: String) = "edit_station/$stationId"
    }
    data object GenreVocabulary : Destination("genre_vocabulary")
    data object MoodVocabulary : Destination("mood_vocabulary")
    data object StyleVocabulary : Destination("style_vocabulary")
    data object Mixes : Destination("mixes")
    data object AddMix : Destination("add_mix")
    data object EditMix : Destination("edit_mix/{mixId}") {
        fun createRoute(mixId: String) = "edit_mix/$mixId"
    }
}

/**
 * No bottom nav bar — Home's own top bar (list/grid/map/filter/add/settings
 * icons) is the sole navigation surface. Settings is a regular pushed
 * destinations with their own back affordance, not tabs.
 */
@Composable
fun StaticApp(stationDao: StationDao, sharedMixUrl: String? = null) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as StaticRadioApp
    val mixDao = app.database.mixDao()

    var pendingSharedMixUrl by remember { mutableStateOf(sharedMixUrl) }
    LaunchedEffect(sharedMixUrl) {
        if (sharedMixUrl != null) {
            pendingSharedMixUrl = sharedMixUrl
            navController.navigate(Destination.AddMix.route)
        }
    }

    // One shared MediaController connection for the whole app lifetime — see
    // RadioController's own note on why this shouldn't be reconnected per-screen.
    LaunchedEffect(Unit) { app.radioController.connect() }

    val playerViewModel: PlayerViewModel = viewModel(
        factory = PlayerViewModel.Factory(
            repository = app.playbackRepository,
            controller = app.radioController,
            stationMetadataProvider = { stationId ->
                stationDao.getStationWithTags(stationId)?.toResolved()?.let { station ->
                    PlayerViewModel.StationBarInfo(
                        title = station.name,
                        imageUrl = station.imageUrl,
                        countryCode = station.countryCode,
                        genres = station.genres,
                        bitrate = station.bitrate,
                        websiteUrl = station.websiteUrl
                    )
                }
            }
        )
    )
    val barState = playerViewModel.barState

    // Recording saves into the public Download folder. On Android 10+ that
    // needs no permission; on 8/9 it needs WRITE_EXTERNAL_STORAGE at runtime.
    val recordContext = LocalContext.current
    val storagePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) playerViewModel.toggleRecording() }
    val onToggleRecording: () -> Unit = {
        val needsPermission = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
            !playerViewModel.isRecording &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                recordContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            playerViewModel.toggleRecording()
        }
    }
    LaunchedEffect(Unit) {
        app.playbackRepository.recordingMessages.collect { msg ->
            android.widget.Toast.makeText(recordContext, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val imageShape by app.settingsRepository.imageShape.collectAsState(initial = ImageShape.SQUARE)
    val showBackgroundGrid by app.settingsRepository.showBackgroundGrid.collectAsState(initial = true)
    val gridSpacingDp by app.settingsRepository.gridSpacingDp.collectAsState(initial = eg.radio.data.settings.DEFAULT_GRID_SPACING_DP)
    val gridLineWidthDp by app.settingsRepository.gridLineWidthDp.collectAsState(initial = eg.radio.data.settings.DEFAULT_GRID_LINE_WIDTH_DP)
    val gridOpacity by app.settingsRepository.gridOpacity.collectAsState(initial = eg.radio.data.settings.DEFAULT_GRID_OPACITY)
    val castEnabled by app.settingsRepository.castEnabled.collectAsState(initial = false)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            val playerBarInset = if (barState.isVisible) PlayerBarReservedHeight else 0.dp
            CompositionLocalProvider(LocalPlayerBarBottomInset provides playerBarInset) {
                NavHost(
                    navController = navController,
                    startDestination = Destination.Home.route,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable(Destination.Home.route) {
                        HomeScreen(
                            stationDao = stationDao,
                            onStationClick = { item ->
                                app.radioController.playStation(
                                    stationId = item.station.id,
                                    streamUrl = item.station.streamUrl,
                                    title = item.station.name,
                                    imageUrl = item.station.imageUrl
                                )
                            },
                            onEditStation = { item -> navController.navigate(Destination.EditStation.createRoute(item.station.id)) },
                            onAddStation = { navController.navigate(Destination.AddStation.route) },
                            onMixesClick = { navController.navigate(Destination.Mixes.route) },
                            onSettingsClick = { navController.navigate(Destination.Settings.route) },
                            nowPlayingStationId = barState.stationId,
                            isPlaying = barState.isPlaying,
                            gridImageShape = imageShape.toComposeShape(),
                            showBackgroundGrid = showBackgroundGrid,
                            gridSpacing = gridSpacingDp.dp,
                            gridLineWidth = gridLineWidthDp.dp,
                            gridOpacity = gridOpacity
                        )
                    }
                    composable(Destination.Settings.route) {
                        SettingsScreen(
                            settingsRepository = app.settingsRepository,
                            stationDao = stationDao,
                            mixDao = mixDao,
                            radioController = app.radioController,
                            playbackRepository = app.playbackRepository,
                            onManageGenres = { navController.navigate(Destination.GenreVocabulary.route) },
                            onManageMoods = { navController.navigate(Destination.MoodVocabulary.route) },
                            onManageStyles = { navController.navigate(Destination.StyleVocabulary.route) },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Destination.GenreVocabulary.route) {
                        GenreVocabularyScreen(
                            settingsRepository = app.settingsRepository,
                            stationDao = stationDao,
                            mixDao = mixDao,
                            radioController = app.radioController,
                            playbackRepository = app.playbackRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Destination.MoodVocabulary.route) {
                        TagVocabularyScreen(
                            stationDao = stationDao,
                            tagType = eg.radio.data.local.TagType.MOOD,
                            title = "المزاج",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Destination.StyleVocabulary.route) {
                        TagVocabularyScreen(
                            stationDao = stationDao,
                            tagType = eg.radio.data.local.TagType.STYLE,
                            title = "النمط",
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Destination.AddStation.route) {
                        AddStationScreen(
                            stationDao = stationDao,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Destination.EditStation.route,
                        arguments = listOf(navArgument("stationId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val stationId = backStackEntry.arguments?.getString("stationId")
                        if (stationId != null) {
                            EditStationScreen(
                                stationId = stationId,
                                stationDao = stationDao,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable(Destination.Mixes.route) {
                        MixesScreen(
                            mixDao = mixDao,
                            onEditMix = { mixId -> navController.navigate(Destination.EditMix.createRoute(mixId)) },
                            onAddMix = { navController.navigate(Destination.AddMix.route) },
                            onSettingsClick = { navController.navigate(Destination.Settings.route) },
                            onBack = { navController.popBackStack() },
                            showBackgroundGrid = showBackgroundGrid,
                            gridSpacing = gridSpacingDp.dp,
                            gridLineWidth = gridLineWidthDp.dp,
                            gridOpacity = gridOpacity
                        )
                    }
                    composable(Destination.AddMix.route) {
                        MixFormScreen(
                            mixId = null,
                            mixDao = mixDao,
                            stationDao = stationDao,
                            prefillUrl = pendingSharedMixUrl,
                            onBack = { pendingSharedMixUrl = null; navController.popBackStack() }
                        )
                    }
                    composable(
                        route = Destination.EditMix.route,
                        arguments = listOf(navArgument("mixId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val mixId = backStackEntry.arguments?.getString("mixId")
                        if (mixId != null) {
                            MixFormScreen(
                                mixId = mixId,
                                mixDao = mixDao,
                                stationDao = stationDao,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }

            PersistentPlayerBar(
                state = barState,
                onTogglePlayPause = playerViewModel::togglePlayPause,
                isRecording = playerViewModel.isRecording,
                recordingSeconds = playerViewModel.recordingSeconds,
                onToggleRecording = onToggleRecording,
                castEnabled = castEnabled,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
    }
}
