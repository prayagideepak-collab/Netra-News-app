package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.NetraRepository
import com.example.data.local.AppDatabase
import com.example.domain.AppLanguage
import com.example.location.CityLocationEngine
import com.example.power.PowerTier
import com.example.ui.AmbiguousCityConfirmationDialog
import com.example.ui.ArticleDetailProvenanceSheet
import com.example.ui.FeedbackReportModalSheet
import com.example.ui.GovernanceAndSettingsScreen
import com.example.ui.ManualCityPickerDialog
import com.example.ui.NetraFamilyHeader
import com.example.ui.NetraViewModel
import com.example.ui.NewspaperFeedScreen
import com.example.ui.SavedBookmarksScreen
import com.example.ui.WeatherAndEarthquakesScreen
import com.example.ui.theme.NetraAmberPrimary
import com.example.ui.theme.NetraNavyPrimary
import com.example.ui.theme.NetraNewsTheme
import com.example.ui.theme.NewsprintIvory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = NetraRepository(
            context = applicationContext,
            dao = database.netraDao(),
            articleDao = database.articleDao()
        )

        setContent {
            val netraViewModel: NetraViewModel = viewModel(
                factory = NetraViewModel.provideFactory(applicationContext, repository)
            )
            val uiState by netraViewModel.uiState.collectAsStateWithLifecycle()

            NetraNewsTheme(
                survivalLowPowerMode = uiState.powerTelemetry.effectiveTier == PowerTier.SURVIVAL
            ) {
                NetraNewsRootApp(netraViewModel = netraViewModel)
            }
        }
    }
}

@Composable
fun NetraNewsRootApp(netraViewModel: NetraViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by netraViewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks by netraViewModel.bookmarksState.collectAsStateWithLifecycle()
    val feedbackDrafts by netraViewModel.feedbackDraftsState.collectAsStateWithLifecycle()
    val healthLogs by netraViewModel.healthLogsState.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> {
                    netraViewModel.onForegroundLifecycleChanged(true)
                }
                Lifecycle.Event.ON_STOP -> {
                    netraViewModel.onForegroundLifecycleChanged(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val coarsePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        netraViewModel.detectApproximateCityInForeground()
    }

    val requestCoarseCityAction = {
        if (CityLocationEngine.hasCoarseLocationPermission(context)) {
            netraViewModel.detectApproximateCityInForeground()
        } else {
            coarsePermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    var currentBottomTab by rememberSaveable { mutableIntStateOf(0) }
    val isHi = uiState.language == AppLanguage.HINDI
    val savedIds = bookmarks.map { it.articleId }.toSet()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            NetraFamilyHeader(
                uiState = uiState,
                onToggleLanguage = { netraViewModel.toggleLanguage() },
                onRefresh = { netraViewModel.refreshAllFeeds(isUserInitiated = true) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NetraNavyPrimary,
                contentColor = NewsprintIvory
            ) {
                NavigationBarItem(
                    selected = currentBottomTab == 0,
                    onClick = { currentBottomTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentBottomTab == 0) Icons.Filled.Newspaper else Icons.Outlined.Newspaper,
                            contentDescription = if (isHi) "समाचार" else "News"
                        )
                    },
                    label = { Text(if (isHi) "समाचार" else "Newspaper") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetraNavyPrimary,
                        selectedTextColor = NetraAmberPrimary,
                        indicatorColor = NetraAmberPrimary,
                        unselectedIconColor = NewsprintIvory.copy(alpha = 0.7f),
                        unselectedTextColor = NewsprintIvory.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_tab_newspaper")
                )

                NavigationBarItem(
                    selected = currentBottomTab == 1,
                    onClick = { currentBottomTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (bookmarks.isNotEmpty()) {
                                    Badge { Text("${bookmarks.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (currentBottomTab == 1) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isHi) "सहेजे गए" else "Saved"
                            )
                        }
                    },
                    label = { Text(if (isHi) "सहेजे गए" else "Saved") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetraNavyPrimary,
                        selectedTextColor = NetraAmberPrimary,
                        indicatorColor = NetraAmberPrimary,
                        unselectedIconColor = NewsprintIvory.copy(alpha = 0.7f),
                        unselectedTextColor = NewsprintIvory.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_tab_saved")
                )

                NavigationBarItem(
                    selected = currentBottomTab == 2,
                    onClick = { currentBottomTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentBottomTab == 2) Icons.Filled.Thunderstorm else Icons.Outlined.Thunderstorm,
                            contentDescription = if (isHi) "मौसम व भूकंप" else "Bulletins"
                        )
                    },
                    label = { Text(if (isHi) "मौसम/भूकंप" else "Bulletins") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetraNavyPrimary,
                        selectedTextColor = NetraAmberPrimary,
                        indicatorColor = NetraAmberPrimary,
                        unselectedIconColor = NewsprintIvory.copy(alpha = 0.7f),
                        unselectedTextColor = NewsprintIvory.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_tab_bulletins")
                )

                NavigationBarItem(
                    selected = currentBottomTab == 3,
                    onClick = { currentBottomTab = 3 },
                    icon = {
                        Icon(
                            imageVector = if (currentBottomTab == 3) Icons.Filled.Policy else Icons.Outlined.Policy,
                            contentDescription = if (isHi) "अनुबंध व सेटिंग्स" else "Governance"
                        )
                    },
                    label = { Text(if (isHi) "अनुबंध/पावर" else "Governance") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NetraNavyPrimary,
                        selectedTextColor = NetraAmberPrimary,
                        indicatorColor = NetraAmberPrimary,
                        unselectedIconColor = NewsprintIvory.copy(alpha = 0.7f),
                        unselectedTextColor = NewsprintIvory.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.testTag("nav_tab_governance")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentBottomTab) {
                0 -> NewspaperFeedScreen(
                    uiState = uiState,
                    savedArticleIds = savedIds,
                    onSelectScope = { netraViewModel.selectScope(it) },
                    onOpenCityPicker = { netraViewModel.setShowCityPickerDialog(true) },
                    onRequestCoarseLocation = requestCoarseCityAction,
                    onOpenArticleDetail = { netraViewModel.openArticleDetail(it) },
                    onReportArticleIssue = { netraViewModel.openFeedbackModal(it) },
                    onOpenGeneralFeedback = { netraViewModel.openFeedbackModal(null) },
                    onToggleBookmark = { netraViewModel.toggleBookmark(it) },
                    onOpenExternalUrl = { url ->
                        netraViewModel.openExternalSourceUrlSafely(context, url)
                    },
                    onRefresh = { netraViewModel.refreshAllFeeds(isUserInitiated = true) },
                    onSelectDiagnosticMode = { netraViewModel.setDiagnosticContractMode(it) },
                    onToggleWarningRecordsOnHome = { netraViewModel.toggleShowWarningStatusRecordsOnHome() },
                    onDismissLocationBanner = { netraViewModel.clearLocationBanner() }
                )

                1 -> SavedBookmarksScreen(
                    bookmarks = bookmarks,
                    language = uiState.language,
                    onRemoveBookmark = { netraViewModel.removeBookmark(it) },
                    onClearAllBookmarks = { netraViewModel.clearAllBookmarks() },
                    onOpenExternalUrl = { url ->
                        netraViewModel.openExternalSourceUrlSafely(context, url)
                    },
                    onNavigateBackHome = { currentBottomTab = 0 }
                )

                2 -> WeatherAndEarthquakesScreen(
                    uiState = uiState,
                    onOpenExternalUrl = { url ->
                        netraViewModel.openExternalSourceUrlSafely(context, url)
                    },
                    onNavigateBackHome = { currentBottomTab = 0 }
                )

                3 -> GovernanceAndSettingsScreen(
                    uiState = uiState,
                    healthLogs = healthLogs,
                    onOpenCityPicker = { netraViewModel.setShowCityPickerDialog(true) },
                    onRequestCoarseLocation = requestCoarseCityAction,
                    onResetSavedCity = { netraViewModel.resetSavedCityPreference() },
                    onClearHealthLogs = { netraViewModel.clearHealthLogs() },
                    onOpenGeneralFeedback = { netraViewModel.openFeedbackModal(null) },
                    onNavigateBackHome = { currentBottomTab = 0 }
                )
            }
        }

        // Provenance Detail Bottom Sheet
        val detailArticle = uiState.selectedArticleDetail
        if (detailArticle != null) {
            ArticleDetailProvenanceSheet(
                article = detailArticle,
                isHi = isHi,
                onDismiss = { netraViewModel.openArticleDetail(null) },
                onReportIssue = {
                    netraViewModel.openArticleDetail(null)
                    netraViewModel.openFeedbackModal(detailArticle)
                },
                onOpenExternalUrl = { url ->
                    netraViewModel.openExternalSourceUrlSafely(context, url)
                }
            )
        }

        // Section 8 Feedback & Issue Report Modal Sheet
        if (uiState.showFeedbackModal) {
            FeedbackReportModalSheet(
                prefilledArticleId = uiState.feedbackPrefilledArticleId,
                prefilledArticleTitle = uiState.feedbackPrefilledArticleTitle,
                initialIssueType = uiState.feedbackInitialIssueType,
                feedbackBannerMessage = uiState.feedbackStatusMessage,
                feedbackBannerIsError = uiState.feedbackStatusIsError,
                savedDrafts = feedbackDrafts,
                isHi = isHi,
                onDismiss = { netraViewModel.closeFeedbackModal() },
                onSaveLocalDraft = { artId, artTitle, type, desc, comments, contact ->
                    netraViewModel.validateAndSaveLocalFeedbackDraft(
                        articleId = artId,
                        articleTitle = artTitle,
                        issueType = type,
                        description = desc,
                        optionalComments = comments,
                        optionalContact = contact
                    )
                },
                onRemoveContactFromDraft = { netraViewModel.removeContactFromDraft(it) },
                onDeleteDraft = { netraViewModel.deleteFeedbackDraft(it) }
            )
        }

        // Manual City Picker Dialog
        if (uiState.showCityPickerDialog) {
            ManualCityPickerDialog(
                currentCityId = uiState.locationPreference.cityId,
                isHi = isHi,
                onSelectCity = { netraViewModel.selectCityManually(it) },
                onDismiss = { netraViewModel.setShowCityPickerDialog(false) }
            )
        }

        // Nearest-City Ambiguity Confirmation Dialog (Section 9.4)
        val ambiguous = uiState.ambiguousCityPrompt
        if (ambiguous != null) {
            AmbiguousCityConfirmationDialog(
                primary = ambiguous.primaryCandidate,
                secondary = ambiguous.secondaryCandidate,
                isHi = isHi,
                onConfirmCity = { netraViewModel.confirmAmbiguousCityChoice(it) },
                onOpenManualPicker = {
                    netraViewModel.clearLocationBanner()
                    netraViewModel.setShowCityPickerDialog(true)
                }
            )
        }
    }
}
