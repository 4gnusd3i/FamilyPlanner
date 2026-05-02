package io.github.by4gnusd3i.familyplanner.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.os.LocaleListCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import io.github.by4gnusd3i.familyplanner.R
import io.github.by4gnusd3i.familyplanner.data.settings.AppSettings
import io.github.by4gnusd3i.familyplanner.domain.model.BudgetSnapshot
import io.github.by4gnusd3i.familyplanner.domain.model.ExpenseItem
import io.github.by4gnusd3i.familyplanner.domain.model.FamilyMember
import io.github.by4gnusd3i.familyplanner.domain.model.MealPlan
import io.github.by4gnusd3i.familyplanner.domain.model.NoteItem
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerEvent
import io.github.by4gnusd3i.familyplanner.domain.model.ShoppingItem
import io.github.by4gnusd3i.familyplanner.domain.planner.RecurrenceRules
import io.github.by4gnusd3i.familyplanner.ui.theme.FamilyPlannerTheme
import java.math.BigDecimal
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private enum class PlannerDestination(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    Overview(R.string.nav_overview, Icons.Filled.Home),
    Week(R.string.nav_week, Icons.Filled.CalendarMonth),
    Meals(R.string.nav_meals, Icons.Filled.Dining),
    Lists(R.string.nav_lists, Icons.AutoMirrored.Filled.List),
    Budget(R.string.nav_budget, Icons.Filled.Payments),
}

private enum class PlannerQuickAction(@param:StringRes val titleRes: Int) {
    Event(R.string.quick_event),
    Meal(R.string.quick_meal),
    Expense(R.string.quick_expense),
    Note(R.string.quick_note),
    ShoppingItem(R.string.quick_item),
}

private data class PlannerActionCallbacks(
    val addEvent: (title: String, note: String?, eventDate: String, ownerId: Long?) -> Unit,
    val addMeal: (meal: String, note: String?) -> Unit,
    val addExpense: (amount: String, category: String?) -> Unit,
    val addNote: (title: String, content: String?) -> Unit,
    val addShoppingItem: (item: String, quantity: String) -> Unit,
    val saveBudget: (limit: String, income: String, currencyCode: String, month: String) -> Unit,
    val saveEvent: (id: Long?, title: String, note: String?, eventDate: String, ownerId: Long?) -> Unit,
    val saveMeal: (id: Long?, dayOfWeek: Int, meal: String, note: String?) -> Unit,
    val saveExpense: (id: Long?, amount: String, category: String?, expenseDate: LocalDate) -> Unit,
    val saveNote: (id: Long?, title: String, content: String?) -> Unit,
    val saveShoppingItem: (id: Long?, item: String, quantity: String) -> Unit,
    val deleteEvent: (id: Long) -> Unit,
    val deleteMeal: (id: Long) -> Unit,
    val deleteExpense: (id: Long) -> Unit,
    val deleteNote: (id: Long) -> Unit,
    val deleteShoppingItem: (id: Long) -> Unit,
    val saveFamilyMember: (
        id: Long?,
        name: String,
        color: String?,
        birthday: String,
        bio: String?,
        avatarUri: String?,
    ) -> Unit,
    val deleteFamilyMember: (id: Long) -> Unit,
    val setLanguageOverride: (languageId: String?) -> Unit,
    val setCurrencyCode: (currencyCode: String) -> Unit,
    val resetAllData: () -> Unit,
)

private data class EventDraft(
    val eventDate: LocalDate,
    val ownerId: Long?,
)

private data class FamilyDragState(
    val memberId: Long,
    val position: Offset,
)

private sealed interface PlannerSummaryTarget {
    val title: String
    val detail: String?

    data class Event(val event: PlannerEvent) : PlannerSummaryTarget {
        override val title: String = event.title
        override val detail: String? = event.note
    }

    data class Meal(val meal: MealPlan) : PlannerSummaryTarget {
        override val title: String = meal.meal
        override val detail: String? = meal.note
    }

    data class Expense(val expense: ExpenseItem, val currencyCode: String) : PlannerSummaryTarget {
        override val title: String = "${expense.category} ${expense.amount.formatMoney()} $currencyCode"
        override val detail: String? = expense.description
    }

    data class Note(val note: NoteItem) : PlannerSummaryTarget {
        override val title: String = note.title
        override val detail: String? = note.content
    }

    data class Shopping(val item: ShoppingItem) : PlannerSummaryTarget {
        override val title: String = item.item
        override val detail: String? = item.quantity.toString()
    }
}

private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")
private val ShortTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val PlannerInk = Color(0xFF22314D)
private val PlannerMutedInk = Color(0xFF5C6680)
private val PlannerPastelPetal = Color(0xFFFFC8DD)
private val PlannerBabyPink = Color(0xFFFFAFCC)
private val PlannerIcyBlue = Color(0xFFBDE0FE)
private val PlannerSkyBlue = Color(0xFFA2D2FF)
private val PlannerSurface = Color(0xE6FFFDFF)
private val PlannerCardBorder = Color.White.copy(alpha = 0.72f)
private val PlannerSoftBorder = PlannerSkyBlue.copy(alpha = 0.28f)
private val PlannerListGradient = Brush.linearGradient(
    colors = listOf(
        PlannerIcyBlue.copy(alpha = 0.72f),
        Color.White.copy(alpha = 0.58f),
    ),
)
private val PlannerNoteGradient = Brush.linearGradient(
    colors = listOf(
        PlannerPastelPetal.copy(alpha = 0.42f),
        PlannerIcyBlue.copy(alpha = 0.32f),
    ),
)
private val PlannerTodayGradient = Brush.linearGradient(
    colors = listOf(
        PlannerBabyPink.copy(alpha = 0.72f),
        PlannerSkyBlue.copy(alpha = 0.72f),
    ),
)
private val PlannerPageGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFF8FB),
        Color(0xFFF9EFF8),
        Color(0xFFEFF8FF),
    ),
)

@Composable
fun FamilyPlannerApp(viewModel: FamilyPlannerViewModel) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val setupError by viewModel.setupError.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    FamilyPlannerContent(
        dashboard = dashboard,
        settings = settings,
        setupError = setupError,
        actionError = actionError,
        onSetupSubmit = viewModel::initializeHousehold,
        onDismissActionError = viewModel::clearActionError,
        actions = PlannerActionCallbacks(
            addEvent = viewModel::addEvent,
            addMeal = viewModel::addMeal,
            addExpense = viewModel::addExpense,
            addNote = viewModel::addNote,
            addShoppingItem = viewModel::addShoppingItem,
            saveBudget = viewModel::saveBudgetMonth,
            saveEvent = viewModel::saveEvent,
            saveMeal = viewModel::saveMeal,
            saveExpense = viewModel::saveExpense,
            saveNote = viewModel::saveNote,
            saveShoppingItem = viewModel::saveShoppingItem,
            deleteEvent = viewModel::deleteEvent,
            deleteMeal = viewModel::deleteMeal,
            deleteExpense = viewModel::deleteExpense,
            deleteNote = viewModel::deleteNote,
            deleteShoppingItem = viewModel::deleteShoppingItem,
            saveFamilyMember = viewModel::saveFamilyMember,
            deleteFamilyMember = viewModel::deleteFamilyMember,
            setLanguageOverride = viewModel::setLanguageOverride,
            setCurrencyCode = viewModel::setCurrencyCode,
            resetAllData = viewModel::resetAllData,
        ),
    )
}

@Composable
private fun FamilyPlannerContent(
    dashboard: PlannerDashboard,
    settings: AppSettings,
    setupError: String?,
    actionError: String?,
    onSetupSubmit: (familyName: String, firstMemberName: String) -> Unit,
    onDismissActionError: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    if (!dashboard.isSetupComplete) {
        SetupScreen(
            setupError = setupError,
            onSetupSubmit = onSetupSubmit,
        )
        return
    }

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val isTablet = windowWidth >= 840.dp
    var activeAction by rememberSaveable { mutableStateOf<PlannerQuickAction?>(null) }
    var eventDraft by remember { mutableStateOf<EventDraft?>(null) }
    var summaryTarget by remember { mutableStateOf<PlannerSummaryTarget?>(null) }
    var editTarget by remember { mutableStateOf<PlannerSummaryTarget?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val openQuickAction: (PlannerQuickAction) -> Unit = { action ->
        if (action == PlannerQuickAction.Event) {
            eventDraft = EventDraft(eventDate = LocalDate.now(), ownerId = null)
        } else {
            activeAction = action
        }
    }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            familyMembers = dashboard.familyMembers,
            onDismiss = { showSettings = false },
            actions = actions,
        )
    }

    eventDraft?.let { draft ->
        EventActionDialog(
            familyMembers = dashboard.familyMembers,
            defaultDate = draft.eventDate,
            defaultOwnerId = draft.ownerId,
            onDismiss = { eventDraft = null },
            actions = actions,
        )
    }

    activeAction?.let { action ->
        QuickActionDialog(
            action = action,
            onDismiss = { activeAction = null },
            actions = actions,
        )
    }

    summaryTarget?.let { target ->
        SummaryDialog(
            target = target,
            familyMembers = dashboard.familyMembers,
            onDismiss = { summaryTarget = null },
            onEdit = {
                summaryTarget = null
                editTarget = target
            },
            actions = actions,
        )
    }

    editTarget?.let { target ->
        EditEntryDialog(
            target = target,
            onDismiss = { editTarget = null },
            actions = actions,
        )
    }

    if (!actionError.isNullOrBlank()) {
        ActionErrorDialog(
            message = actionError,
            onDismiss = onDismissActionError,
        )
    }

    if (isTablet) {
        TabletDashboard(
            dashboard = dashboard,
            actions = actions,
            onQuickActionSelected = openQuickAction,
            onEntrySelected = { summaryTarget = it },
            onEventDateSelected = { eventDraft = EventDraft(eventDate = it, ownerId = null) },
            onFamilyDateSelected = { memberId, eventDate ->
                eventDraft = EventDraft(eventDate = eventDate, ownerId = memberId)
            },
            onSettingsClick = { showSettings = true },
        )
    } else {
        PhoneShell(
            dashboard = dashboard,
            actions = actions,
            onQuickActionSelected = openQuickAction,
            onEntrySelected = { summaryTarget = it },
            onEventDateSelected = { eventDraft = EventDraft(eventDate = it, ownerId = null) },
            onSettingsClick = { showSettings = true },
        )
    }
}

@Composable
private fun SetupScreen(
    setupError: String?,
    onSetupSubmit: (familyName: String, firstMemberName: String) -> Unit,
) {
    var familyName by rememberSaveable { mutableStateOf("") }
    var firstMemberName by rememberSaveable { mutableStateOf("") }

    PlannerBackground(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = plannerCardColors(),
                border = plannerCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setup_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.setup_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text(stringResource(R.string.setup_family_name)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = firstMemberName,
                        onValueChange = { firstMemberName = it },
                        label = { Text(stringResource(R.string.setup_first_member)) },
                        singleLine = true,
                    )
                    if (!setupError.isNullOrBlank()) {
                        Text(
                            text = setupError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSetupSubmit(familyName, firstMemberName) },
                    ) {
                        Text(stringResource(R.string.setup_submit))
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannerBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(PlannerPageGradient),
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneShell(
    dashboard: PlannerDashboard,
    actions: PlannerActionCallbacks,
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
    onEventDateSelected: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(PlannerDestination.Overview) }
    var showQuickActions by rememberSaveable { mutableStateOf(false) }

    if (showQuickActions) {
        QuickActionChooserDialog(
            onDismiss = { showQuickActions = false },
            onQuickActionSelected = {
                showQuickActions = false
                onQuickActionSelected(it)
            },
        )
    }

    PlannerBackground(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(selected.titleRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = PlannerInk,
                        actionIconContentColor = PlannerInk,
                    ),
                )
            },
            bottomBar = {
                NavigationBar(containerColor = PlannerSurface) {
                    PlannerDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            selected = selected == destination,
                            onClick = { selected = destination },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(stringResource(destination.titleRes), maxLines = 1) },
                        )
                    }
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showQuickActions = true },
                    containerColor = PlannerPastelPetal,
                    contentColor = PlannerInk,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_choose_title))
                }
            },
        ) { padding ->
            DestinationContent(
                destination = selected,
                dashboard = dashboard,
                actions = actions,
                onEntrySelected = onEntrySelected,
                onEventDateSelected = onEventDateSelected,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabletDashboard(
    dashboard: PlannerDashboard,
    actions: PlannerActionCallbacks,
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
    onEventDateSelected: (LocalDate) -> Unit,
    onFamilyDateSelected: (Long, LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
) {
    val dayDropBounds = remember(dashboard.weekStart) { mutableStateMapOf<LocalDate, Rect>() }
    var familyDragState by remember { mutableStateOf<FamilyDragState?>(null) }
    val highlightedDropDate = familyDragState?.position?.let { position ->
        dayDropBounds.entries.firstOrNull { (_, bounds) -> bounds.contains(position) }?.key
    }

    PlannerBackground(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickActionRow(
                modifier = Modifier.fillMaxWidth(),
                onQuickActionSelected = onQuickActionSelected,
            )
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(
                    modifier = Modifier.width(292.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PlannerPanel(
                        title = R.string.budget_title,
                        modifier = Modifier.weight(0.86f),
                    ) {
                        BudgetSummary(dashboard.budget, onEntrySelected, actions)
                    }
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.weight(1.24f),
                    ) {
                        ShoppingList(dashboard.shoppingItems, dashboard.familyMembers, onEntrySelected)
                    }
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.weight(0.9f),
                    ) {
                        NotesList(dashboard.notes, dashboard.familyMembers, onEntrySelected)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PlannerPanel(R.string.week_title, Modifier.weight(1.7f)) {
                        WeekCalendar(
                            weekStart = dashboard.weekStart,
                            events = dashboard.weekEvents,
                            familyMembers = dashboard.familyMembers,
                            onEntrySelected = onEntrySelected,
                            onDateSelected = onEventDateSelected,
                            onDayBoundsChanged = { day, bounds -> dayDropBounds[day] = bounds },
                            highlightedDate = highlightedDropDate,
                        )
                    }
                    PlannerPanel(R.string.meals_title, Modifier.weight(0.7f)) {
                        MealPlannerGrid(
                            weekStart = dashboard.weekStart,
                            meals = dashboard.meals,
                            familyMembers = dashboard.familyMembers,
                            onEntrySelected = onEntrySelected,
                        )
                    }
                }
                PlannerPanel(
                    title = R.string.overview_title,
                    modifier = Modifier.width(292.dp),
                ) {
                    UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers, onEntrySelected)
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(44.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dashboard.familyMembers.forEach { member ->
                    FamilyFooterTile(
                        member = member,
                        onLongPress = { onFamilyDateSelected(member.id, LocalDate.now()) },
                        onDragStarted = { position ->
                            familyDragState = FamilyDragState(member.id, position)
                        },
                        onDragged = { position ->
                            familyDragState = FamilyDragState(member.id, position)
                        },
                        onDragEnded = { position ->
                            val dropDate = dayDropBounds.entries
                                .firstOrNull { (_, bounds) -> bounds.contains(position) }
                                ?.key
                            familyDragState = null
                            onFamilyDateSelected(member.id, dropDate ?: LocalDate.now())
                        },
                        onDragCancelled = {
                            familyDragState = null
                        },
                    )
                }
                AddFamilyFooterTile(onClick = onSettingsClick)
            }
        }
    }
}

@Composable
private fun DestinationContent(
    destination: PlannerDestination,
    dashboard: PlannerDashboard,
    actions: PlannerActionCallbacks,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
    onEventDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        when (destination) {
            PlannerDestination.Overview -> {
                item {
                    PlannerPanel(
                        title = R.string.overview_title,
                        modifier = Modifier.fillMaxWidth().height(280.dp),
                    ) {
                        UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers, onEntrySelected)
                    }
                }
            }
            PlannerDestination.Week -> {
                item {
                    PlannerPanel(R.string.week_title, Modifier.fillMaxWidth().height(520.dp)) {
                        WeekCalendar(
                            weekStart = dashboard.weekStart,
                            events = dashboard.weekEvents,
                            familyMembers = dashboard.familyMembers,
                            onEntrySelected = onEntrySelected,
                            onDateSelected = onEventDateSelected,
                        )
                    }
                }
            }
            PlannerDestination.Meals -> {
                item {
                    PlannerPanel(R.string.meals_title, Modifier.fillMaxWidth().height(420.dp)) {
                        MealPlannerGrid(
                            weekStart = dashboard.weekStart,
                            meals = dashboard.meals,
                            familyMembers = dashboard.familyMembers,
                            onEntrySelected = onEntrySelected,
                        )
                    }
                }
            }
            PlannerDestination.Lists -> {
                item {
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    ) {
                        ShoppingList(dashboard.shoppingItems, dashboard.familyMembers, onEntrySelected)
                    }
                }
                item {
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    ) {
                        NotesList(dashboard.notes, dashboard.familyMembers, onEntrySelected)
                    }
                }
            }
            PlannerDestination.Budget -> {
                item {
                    PlannerPanel(R.string.budget_title, Modifier.fillMaxWidth().height(420.dp)) {
                        BudgetSummary(dashboard.budget, onEntrySelected, actions)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(
    modifier: Modifier = Modifier,
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        if (maxWidth >= 520.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlannerQuickAction.entries.forEach { action ->
                    QuickAction(
                        modifier = Modifier.weight(1f),
                        label = action.titleRes,
                        onClick = { onQuickActionSelected(action) },
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                PlannerQuickAction.entries.forEach { action ->
                    item {
                        QuickAction(
                            modifier = Modifier.width(140.dp),
                            label = action.titleRes,
                            onClick = { onQuickActionSelected(action) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAction(
    modifier: Modifier = Modifier,
    @StringRes label: Int,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(48.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.6f),
            contentColor = PlannerInk,
        ),
        border = BorderStroke(1.dp, PlannerSoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+ ${stringResource(label)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun plannerCardColors() = CardDefaults.cardColors(
    containerColor = PlannerSurface,
    contentColor = PlannerInk,
)

private fun plannerCardBorder() = BorderStroke(1.dp, PlannerCardBorder)

@Composable
private fun PlannerPanel(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    body: String? = null,
    content: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = plannerCardColors(),
        border = plannerCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = if (content != null) Alignment.TopStart else Alignment.Center,
            ) {
                if (content != null) {
                    content()
                } else {
                    Text(
                        text = body ?: "${stringResource(R.string.empty_state)}. ${stringResource(R.string.coming_next)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekCalendar(
    weekStart: LocalDate,
    events: List<PlannerEvent>,
    familyMembers: List<FamilyMember>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onDayBoundsChanged: (LocalDate, Rect) -> Unit = { _, _ -> },
    highlightedDate: LocalDate? = null,
) {
    val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    val locale = LocalLocale.current.platformLocale
    val weekNumber = remember(weekStart, locale) {
        weekStart.get(WeekFields.of(locale).weekOfWeekBasedYear())
    }
    val weekRange = remember(weekStart) {
        "${weekStart.format(ShortDateFormatter)} - ${weekStart.plusDays(6).format(ShortDateFormatter)}"
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = weekNumber.toString(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.week_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = weekRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlannerMutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxWidth >= 520.dp) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    weekDays.forEach { day ->
                        WeekDayCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            day = day,
                            events = events.filter { it.eventDate == day },
                            familyMembers = familyMembers,
                            onEntrySelected = onEntrySelected,
                            onDateSelected = onDateSelected,
                            onBoundsChanged = onDayBoundsChanged,
                            isDropHighlighted = highlightedDate == day,
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    weekDays.forEach { day ->
                        item {
                            WeekDayCard(
                                modifier = Modifier
                                    .width(150.dp)
                                    .fillMaxHeight(),
                                day = day,
                                events = events.filter { it.eventDate == day },
                                familyMembers = familyMembers,
                                onEntrySelected = onEntrySelected,
                                onDateSelected = onDateSelected,
                                onBoundsChanged = onDayBoundsChanged,
                                isDropHighlighted = highlightedDate == day,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekDayCard(
    modifier: Modifier,
    day: LocalDate,
    events: List<PlannerEvent>,
    familyMembers: List<FamilyMember>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onBoundsChanged: (LocalDate, Rect) -> Unit = { _, _ -> },
    isDropHighlighted: Boolean = false,
) {
    val isToday = day == LocalDate.now()
    Card(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(day, coordinates.boundsInWindow())
            }
            .clickable { onDateSelected(day) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.50f),
            contentColor = PlannerInk,
        ),
        border = if (isDropHighlighted || isToday) {
            BorderStroke(1.5.dp, PlannerBabyPink.copy(alpha = 0.72f))
        } else {
            BorderStroke(1.dp, PlannerSoftBorder)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DayHeader(day = day, isToday = isToday)
            if (events.isEmpty()) {
                EmptyPanelText()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    events.forEach { event ->
                        EventCalendarCard(
                            event = event,
                            familyMembers = familyMembers,
                            onClick = { onEntrySelected(PlannerSummaryTarget.Event(event)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate, isToday: Boolean) {
    val locale = LocalLocale.current.platformLocale
    val headerBrush = if (isToday) {
        PlannerTodayGradient
    } else {
        Brush.linearGradient(
            colors = listOf(
                PlannerPastelPetal.copy(alpha = 0.20f),
                PlannerPastelPetal.copy(alpha = 0.20f),
            ),
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(headerBrush),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale).uppercase(locale),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = PlannerMutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${day.dayOfMonth}.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EventCalendarCard(
    event: PlannerEvent,
    familyMembers: List<FamilyMember>,
    onClick: () -> Unit,
) {
    val owner = eventOwner(event, familyMembers)
    val accent = (event.color ?: owner?.color ?: "#A2D2FF").toComposeColor()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.78f),
            contentColor = PlannerInk,
        ),
        border = BorderStroke(1.dp, PlannerSoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.70f),
                        ),
                    ),
                )
                .padding(start = 8.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            MiniAvatar(member = owner, fallbackColor = accent, size = 22.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (owner != null && !event.isBirthdayEvent()) {
                    Text(
                        text = owner.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = eventDisplayTitle(event, familyMembers),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                event.startTime?.let { TimePill(it.format(ShortTimeFormatter)) }
                if (!event.note.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.action_show_more),
                        style = MaterialTheme.typography.bodySmall,
                        color = PlannerMutedInk,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                event.endTime?.let { TimePill(it.format(ShortTimeFormatter)) }
            }
        }
    }
}

@Composable
private fun TimePill(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = PlannerBabyPink.copy(alpha = 0.90f),
        contentColor = PlannerInk,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UpcomingList(
    events: List<PlannerEvent>,
    familyMembers: List<FamilyMember>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    if (events.isEmpty()) {
        EmptyPanelText()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        events.forEach { event ->
            UpcomingEventCard(
                event = event,
                familyMembers = familyMembers,
                onClick = { onEntrySelected(PlannerSummaryTarget.Event(event)) },
            )
        }
    }
}

@Composable
private fun UpcomingEventCard(
    event: PlannerEvent,
    familyMembers: List<FamilyMember>,
    onClick: () -> Unit,
) {
    val owner = eventOwner(event, familyMembers)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 66.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = PlannerIcyBlue.copy(alpha = 0.58f),
            contentColor = PlannerInk,
        ),
        border = BorderStroke(1.dp, PlannerSoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlannerListGradient)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DateTile(event.eventDate)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                owner?.let { member ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiniAvatar(member = member, size = 20.dp)
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = member.color.toComposeColor(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Text(
                    text = eventDisplayTitle(event, familyMembers),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                eventTimeText(event)?.let { time ->
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = PlannerMutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateTile(date: LocalDate) {
    val locale = LocalLocale.current.platformLocale
    Surface(
        modifier = Modifier
            .width(54.dp)
            .height(54.dp),
        shape = RoundedCornerShape(10.dp),
        color = PlannerSkyBlue.copy(alpha = 0.66f),
        contentColor = PlannerInk,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = date.month.getDisplayName(JavaTextStyle.SHORT, locale),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MealPlannerGrid(
    weekStart: LocalDate,
    meals: List<MealPlan>,
    familyMembers: List<FamilyMember>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= 520.dp) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                weekDays.forEachIndexed { index, day ->
                    MealDayCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        day = day,
                        meals = meals.filter { it.dayOfWeek == index },
                        familyMembers = familyMembers,
                        onEntrySelected = onEntrySelected,
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp),
            ) {
                weekDays.forEachIndexed { index, day ->
                    item {
                        MealDayCard(
                            modifier = Modifier
                                .width(150.dp)
                                .fillMaxHeight(),
                            day = day,
                            meals = meals.filter { it.dayOfWeek == index },
                            familyMembers = familyMembers,
                            onEntrySelected = onEntrySelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealDayCard(
    modifier: Modifier,
    day: LocalDate,
    meals: List<MealPlan>,
    familyMembers: List<FamilyMember>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.48f),
            contentColor = PlannerInk,
        ),
        border = BorderStroke(1.dp, PlannerSoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = day.format(ShortDateFormatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meals.isEmpty()) {
                EmptyPanelText()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    meals.forEach { meal ->
                        MealCompactCard(
                            meal = meal,
                            owner = familyMembers.firstOrNull { it.id == meal.ownerId },
                            onClick = { onEntrySelected(PlannerSummaryTarget.Meal(meal)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MealCompactCard(
    meal: MealPlan,
    owner: FamilyMember?,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.70f),
            contentColor = PlannerInk,
        ),
        border = BorderStroke(1.dp, PlannerSoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniAvatar(member = owner, size = 18.dp)
            Text(
                text = meal.meal,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ShoppingList(
    items: List<ShoppingItem>,
    familyMembers: List<FamilyMember>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyPanelText()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            ShoppingItemCard(
                item = item,
                owner = familyMembers.firstOrNull { it.id == item.ownerId },
                onClick = { onEntrySelected(PlannerSummaryTarget.Shopping(item)) },
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<NoteItem>,
    familyMembers: List<FamilyMember>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    if (notes.isEmpty()) {
        EmptyPanelText()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        notes.forEach { note ->
            NoteCard(
                note = note,
                owner = familyMembers.firstOrNull { it.id == note.ownerId },
                onClick = { onEntrySelected(PlannerSummaryTarget.Note(note)) },
            )
        }
    }
}

@Composable
private fun ShoppingItemCard(
    item: ShoppingItem,
    owner: FamilyMember?,
    onClick: () -> Unit,
) {
    ReleaseListCard(
        modifier = Modifier.clickable { onClick() },
        gradient = PlannerListGradient,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(2.dp, PlannerBabyPink),
            ) {}
            MiniAvatar(member = owner, size = 22.dp)
            Text(
                text = item.item,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = CircleShape,
                color = PlannerBabyPink,
                contentColor = PlannerInk,
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteItem,
    owner: FamilyMember?,
    onClick: () -> Unit,
) {
    ReleaseListCard(
        modifier = Modifier.clickable { onClick() },
        gradient = PlannerNoteGradient,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MiniAvatar(member = owner, size = 22.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!note.content.isNullOrBlank()) {
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = PlannerMutedInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReleaseListCard(
    modifier: Modifier = Modifier,
    gradient: Brush = PlannerListGradient,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.62f),
            contentColor = PlannerInk,
        ),
        border = BorderStroke(1.dp, PlannerSoftBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(8.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun BudgetSummary(
    budget: BudgetSnapshot,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
    actions: PlannerActionCallbacks,
) {
    var editingBudget by rememberSaveable(budget.month) { mutableStateOf(false) }

    if (editingBudget) {
        BudgetDialog(
            budget = budget,
            onDismiss = { editingBudget = false },
            actions = actions,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val spentRatio = if (budget.limit.signum() > 0) {
            budget.spent.divide(budget.limit, 4, java.math.RoundingMode.HALF_UP)
                .toFloat()
                .coerceIn(0f, 1f)
        } else {
            0f
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PanelLine(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.budget_spent, budget.spent.formatMoney(), budget.currencyCode),
                detail = stringResource(R.string.budget_remaining, budget.remaining.formatMoney(), budget.currencyCode),
            )
            PanelLine(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.budget_income, budget.income.formatMoney(), budget.currencyCode),
                detail = stringResource(R.string.budget_limit, budget.limit.formatMoney(), budget.currencyCode),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PlannerPastelPetal.copy(alpha = 0.22f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(spentRatio)
                    .background(PlannerSkyBlue.copy(alpha = 0.88f)),
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.budget_remaining, budget.remaining.formatMoney(), budget.currencyCode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2B6790),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { editingBudget = true },
        ) {
            Text(stringResource(R.string.action_edit_budget), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (budget.expenses.isNotEmpty()) {
            budget.expenses.take(3).forEach { expense ->
                PanelLine(
                    modifier = Modifier.clickable {
                        onEntrySelected(PlannerSummaryTarget.Expense(expense, budget.currencyCode))
                    },
                    title = "${expense.category} ${expense.amount.formatMoney()} ${budget.currencyCode}",
                    detail = expense.description,
                )
            }
        }
    }
}

@Composable
private fun BudgetDialog(
    budget: BudgetSnapshot,
    onDismiss: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    var limit by rememberSaveable(budget.month) { mutableStateOf(budget.limit.formatMoney()) }
    var income by rememberSaveable(budget.month) { mutableStateOf(budget.income.formatMoney()) }
    var currencyCode by rememberSaveable(budget.month) { mutableStateOf(budget.currencyCode) }
    var month by rememberSaveable(budget.month) { mutableStateOf(budget.month) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_edit_budget)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = month,
                    onValueChange = { month = it.take(7) },
                    label = { Text(stringResource(R.string.field_month)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = income,
                    onValueChange = { income = it },
                    label = { Text(stringResource(R.string.field_income)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = limit,
                    onValueChange = { limit = it },
                    label = { Text(stringResource(R.string.field_budget_limit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = currencyCode,
                    onValueChange = { currencyCode = it.take(3).uppercase(Locale.ROOT) },
                    label = { Text(stringResource(R.string.settings_currency)) },
                    singleLine = true,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    actions.saveBudget(limit, income, currencyCode, month)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
    )
}

@Composable
private fun PanelLine(
    modifier: Modifier = Modifier,
    title: String,
    detail: String?,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!detail.isNullOrBlank()) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = PlannerMutedInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyPanelText() {
    Text(
        text = stringResource(R.string.empty_state),
        style = MaterialTheme.typography.bodyMedium,
        color = PlannerMutedInk,
    )
}

@Composable
private fun QuickActionChooserDialog(
    onDismiss: () -> Unit,
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_choose_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlannerQuickAction.entries.forEach { action ->
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onQuickActionSelected(action) },
                    ) {
                        Text(stringResource(action.titleRes), maxLines = 1)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun EventActionDialog(
    familyMembers: List<FamilyMember>,
    defaultDate: LocalDate,
    defaultOwnerId: Long?,
    onDismiss: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    var title by rememberSaveable(defaultDate, defaultOwnerId) { mutableStateOf("") }
    var note by rememberSaveable(defaultDate, defaultOwnerId) { mutableStateOf("") }
    var eventDate by rememberSaveable(defaultDate, defaultOwnerId) { mutableStateOf(defaultDate.toString()) }
    var ownerId by rememberSaveable(defaultDate, defaultOwnerId) { mutableStateOf(defaultOwnerId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_event)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.field_title)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = eventDate,
                    onValueChange = { eventDate = it.take(10) },
                    label = { Text(stringResource(R.string.field_event_date)) },
                    singleLine = true,
                )
                OwnerSelector(
                    familyMembers = familyMembers,
                    selectedOwnerId = ownerId,
                    onOwnerSelected = { ownerId = it },
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.field_note)) },
                    maxLines = 3,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    actions.addEvent(title, note, eventDate, ownerId)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OwnerSelector(
    familyMembers: List<FamilyMember>,
    selectedOwnerId: Long?,
    onOwnerSelected: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.field_owner),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OwnerButton(
                label = stringResource(R.string.owner_none),
                selected = selectedOwnerId == null,
                onClick = { onOwnerSelected(null) },
            )
            familyMembers.forEach { member ->
                OwnerButton(
                    label = member.name,
                    selected = selectedOwnerId == member.id,
                    onClick = { onOwnerSelected(member.id) },
                )
            }
        }
    }
}

@Composable
private fun OwnerButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(onClick = onClick) {
        Text(
            text = if (selected) "$label *" else label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QuickActionDialog(
    action: PlannerQuickAction,
    onDismiss: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    var primary by rememberSaveable(action) { mutableStateOf("") }
    var secondary by rememberSaveable(action) { mutableStateOf("") }
    val titleLabel = when (action) {
        PlannerQuickAction.Event -> R.string.field_title
        PlannerQuickAction.Meal -> R.string.field_meal
        PlannerQuickAction.Expense -> R.string.field_amount
        PlannerQuickAction.Note -> R.string.field_title
        PlannerQuickAction.ShoppingItem -> R.string.field_item
    }
    val secondaryLabel = when (action) {
        PlannerQuickAction.Event -> R.string.field_note
        PlannerQuickAction.Meal -> R.string.field_note
        PlannerQuickAction.Expense -> R.string.field_category
        PlannerQuickAction.Note -> R.string.field_content
        PlannerQuickAction.ShoppingItem -> R.string.field_quantity
    }
    val keyboardType = when (action) {
        PlannerQuickAction.Expense -> KeyboardType.Decimal
        PlannerQuickAction.ShoppingItem -> KeyboardType.Number
        else -> KeyboardType.Text
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(action.titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = primary,
                    onValueChange = { primary = it },
                    label = { Text(stringResource(titleLabel)) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = secondary,
                    onValueChange = { secondary = it },
                    label = { Text(stringResource(secondaryLabel)) },
                    singleLine = action == PlannerQuickAction.Expense || action == PlannerQuickAction.ShoppingItem,
                    maxLines = if (action == PlannerQuickAction.Expense || action == PlannerQuickAction.ShoppingItem) 1 else 3,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (action) {
                        PlannerQuickAction.Event -> actions.addEvent(primary, secondary, LocalDate.now().toString(), null)
                        PlannerQuickAction.Meal -> actions.addMeal(primary, secondary)
                        PlannerQuickAction.Expense -> actions.addExpense(primary, secondary)
                        PlannerQuickAction.Note -> actions.addNote(primary, secondary)
                        PlannerQuickAction.ShoppingItem -> actions.addShoppingItem(primary, secondary)
                    }
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
    )
}

@Composable
private fun SummaryDialog(
    target: PlannerSummaryTarget,
    familyMembers: List<FamilyMember>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    val isReadOnly = target.isReadOnlySystemEntry()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = summaryTypeLabel(target),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        text = {
            val detail = target.detail
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = summaryTitle(target, familyMembers),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                summaryMeta(target)?.let { meta ->
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (!detail.isNullOrBlank()) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
        confirmButton = {
            if (!isReadOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            deleteTarget(target, actions)
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                    TextButton(onClick = onEdit) {
                        Text(stringResource(R.string.action_edit))
                    }
                }
            }
        },
    )
}

@Composable
private fun EditEntryDialog(
    target: PlannerSummaryTarget,
    onDismiss: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    var primary by rememberSaveable(target.title) { mutableStateOf(editPrimaryValue(target)) }
    var secondary by rememberSaveable(target.detail) { mutableStateOf(editSecondaryValue(target)) }
    val primaryLabel = when (target) {
        is PlannerSummaryTarget.Event -> R.string.field_title
        is PlannerSummaryTarget.Meal -> R.string.field_meal
        is PlannerSummaryTarget.Expense -> R.string.field_amount
        is PlannerSummaryTarget.Note -> R.string.field_title
        is PlannerSummaryTarget.Shopping -> R.string.field_item
    }
    val secondaryLabel = when (target) {
        is PlannerSummaryTarget.Event -> R.string.field_note
        is PlannerSummaryTarget.Meal -> R.string.field_note
        is PlannerSummaryTarget.Expense -> R.string.field_category
        is PlannerSummaryTarget.Note -> R.string.field_content
        is PlannerSummaryTarget.Shopping -> R.string.field_quantity
    }
    val keyboardType = when (target) {
        is PlannerSummaryTarget.Expense -> KeyboardType.Decimal
        is PlannerSummaryTarget.Shopping -> KeyboardType.Number
        else -> KeyboardType.Text
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = primary,
                    onValueChange = { primary = it },
                    label = { Text(stringResource(primaryLabel)) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = secondary,
                    onValueChange = { secondary = it },
                    label = { Text(stringResource(secondaryLabel)) },
                    singleLine = target is PlannerSummaryTarget.Expense || target is PlannerSummaryTarget.Shopping,
                    maxLines = if (target is PlannerSummaryTarget.Expense || target is PlannerSummaryTarget.Shopping) 1 else 3,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    saveTarget(target, primary, secondary, actions)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
    )
}

@Composable
private fun SettingsDialog(
    settings: AppSettings,
    familyMembers: List<FamilyMember>,
    onDismiss: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    var currencyCode by rememberSaveable(settings.currencyCode) { mutableStateOf(settings.currencyCode) }
    var editedMember by remember { mutableStateOf<FamilyMember?>(null) }
    var addingMember by rememberSaveable { mutableStateOf(false) }

    if (addingMember || editedMember != null) {
        FamilyMemberDialog(
            member = editedMember,
            onDismiss = {
                addingMember = false
                editedMember = null
            },
            actions = actions,
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LanguageButton(
                            label = stringResource(R.string.settings_language_system),
                            selected = settings.languageOverride == null,
                            onClick = {
                                applyLanguageOverride(null)
                                actions.setLanguageOverride(null)
                            },
                        )
                        LanguageButton(
                            label = stringResource(R.string.settings_language_norwegian),
                            selected = settings.languageOverride == "nb",
                            onClick = {
                                applyLanguageOverride("nb")
                                actions.setLanguageOverride("nb")
                            },
                        )
                        LanguageButton(
                            label = stringResource(R.string.settings_language_english),
                            selected = settings.languageOverride == "en",
                            onClick = {
                                applyLanguageOverride("en")
                                actions.setLanguageOverride("en")
                            },
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.settings_currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = currencyCode,
                            onValueChange = { currencyCode = it.take(3).uppercase(Locale.ROOT) },
                            label = { Text(stringResource(R.string.settings_currency)) },
                            singleLine = true,
                        )
                        Button(onClick = { actions.setCurrencyCode(currencyCode) }) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.settings_family),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { addingMember = true },
                    ) {
                        Text(stringResource(R.string.settings_add_member))
                    }
                }
                familyMembers.forEach { member ->
                    item {
                        PanelLine(
                            modifier = Modifier.clickable { editedMember = member },
                            title = member.name,
                            detail = memberDetail(member),
                        )
                    }
                }
                item {
                    Text(
                        text = stringResource(R.string.settings_data),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_reset_data_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = actions.resetAllData,
                    ) {
                        Text(stringResource(R.string.action_reset_data))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

@Composable
private fun LanguageButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            text = if (selected) "$label *" else label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FamilyMemberDialog(
    member: FamilyMember?,
    onDismiss: () -> Unit,
    actions: PlannerActionCallbacks,
) {
    var name by rememberSaveable(member?.id) { mutableStateOf(member?.name.orEmpty()) }
    var color by rememberSaveable(member?.id) { mutableStateOf(member?.color ?: "#3b82f6") }
    var birthday by rememberSaveable(member?.id) { mutableStateOf(member?.birthday?.toString().orEmpty()) }
    var bio by rememberSaveable(member?.id) { mutableStateOf(member?.bio.orEmpty()) }
    var avatarUri by rememberSaveable(member?.id) { mutableStateOf(member?.avatarUri.orEmpty()) }
    val avatarPicker = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }
    val title = if (member == null) R.string.settings_add_member else R.string.settings_edit_member

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AvatarSurface(
                        member = member?.copy(avatarUri = avatarUri.takeIf { it.isNotBlank() })
                            ?: FamilyMember(
                                id = 0,
                                name = name,
                                color = color,
                                avatarUri = avatarUri.takeIf { it.isNotBlank() },
                                birthday = null,
                                bio = null,
                            ),
                    )
                    Button(
                        onClick = {
                            avatarPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                        },
                    ) {
                        Text(stringResource(R.string.action_choose_avatar))
                    }
                }
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = color,
                    onValueChange = { color = it },
                    label = { Text(stringResource(R.string.field_color)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = birthday,
                    onValueChange = { birthday = it },
                    label = { Text(stringResource(R.string.field_birthday)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text(stringResource(R.string.field_bio)) },
                    minLines = 2,
                    maxLines = 4,
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (member != null) {
                    TextButton(
                        onClick = {
                            actions.deleteFamilyMember(member.id)
                            onDismiss()
                        },
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    actions.saveFamilyMember(member?.id, name, color, birthday, bio, avatarUri)
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
    )
}

@Composable
private fun ActionErrorDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_error_title)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        },
    )
}

@Composable
private fun summaryTypeLabel(target: PlannerSummaryTarget): String =
    stringResource(
        when (target) {
            is PlannerSummaryTarget.Event -> R.string.quick_event
            is PlannerSummaryTarget.Meal -> R.string.quick_meal
            is PlannerSummaryTarget.Expense -> R.string.quick_expense
            is PlannerSummaryTarget.Note -> R.string.quick_note
            is PlannerSummaryTarget.Shopping -> R.string.quick_item
        },
    )

@Composable
private fun summaryTitle(
    target: PlannerSummaryTarget,
    familyMembers: List<FamilyMember>,
): String =
    when (target) {
        is PlannerSummaryTarget.Event -> eventDisplayTitle(target.event, familyMembers)
        else -> target.title
    }

@Composable
private fun eventDisplayTitle(
    event: PlannerEvent,
    familyMembers: List<FamilyMember>,
): String {
    if (!event.isBirthdayEvent()) return event.title

    val memberName = familyMembers
        .firstOrNull { it.id == event.sourceMemberId || it.id == event.ownerId }
        ?.name
        ?: event.title
    return stringResource(R.string.birthday_event_title, memberName)
}

private fun PlannerEvent.isBirthdayEvent(): Boolean =
    sourceType == RecurrenceRules.BIRTHDAY_SOURCE_TYPE

private fun eventTimeText(event: PlannerEvent): String? =
    listOfNotNull(
        event.startTime?.format(ShortTimeFormatter),
        event.endTime?.format(ShortTimeFormatter),
    ).joinToString(" - ").takeIf { it.isNotBlank() }

private fun PlannerSummaryTarget.isReadOnlySystemEntry(): Boolean =
    this is PlannerSummaryTarget.Event && event.isBirthdayEvent()

@Composable
private fun summaryMeta(target: PlannerSummaryTarget): String? =
    when (target) {
        is PlannerSummaryTarget.Event -> target.event.eventDate.format(ShortDateFormatter)
        is PlannerSummaryTarget.Meal -> stringResource(R.string.day_index_value, target.meal.dayOfWeek + 1)
        is PlannerSummaryTarget.Expense -> target.expense.expenseDate.format(ShortDateFormatter)
        is PlannerSummaryTarget.Note -> null
        is PlannerSummaryTarget.Shopping -> stringResource(R.string.quantity_value, target.item.quantity)
    }

private fun editPrimaryValue(target: PlannerSummaryTarget): String =
    when (target) {
        is PlannerSummaryTarget.Event -> target.event.title
        is PlannerSummaryTarget.Meal -> target.meal.meal
        is PlannerSummaryTarget.Expense -> target.expense.amount.formatMoney()
        is PlannerSummaryTarget.Note -> target.note.title
        is PlannerSummaryTarget.Shopping -> target.item.item
    }

private fun editSecondaryValue(target: PlannerSummaryTarget): String =
    when (target) {
        is PlannerSummaryTarget.Event -> target.event.note.orEmpty()
        is PlannerSummaryTarget.Meal -> target.meal.note.orEmpty()
        is PlannerSummaryTarget.Expense -> target.expense.category
        is PlannerSummaryTarget.Note -> target.note.content.orEmpty()
        is PlannerSummaryTarget.Shopping -> target.item.quantity.toString()
    }

private fun saveTarget(
    target: PlannerSummaryTarget,
    primary: String,
    secondary: String,
    actions: PlannerActionCallbacks,
) {
    when (target) {
        is PlannerSummaryTarget.Event -> actions.saveEvent(
            target.event.id,
            primary,
            secondary,
            target.event.eventDate.toString(),
            target.event.ownerId,
        )
        is PlannerSummaryTarget.Meal -> actions.saveMeal(
            target.meal.id,
            target.meal.dayOfWeek,
            primary,
            secondary,
        )
        is PlannerSummaryTarget.Expense -> actions.saveExpense(
            target.expense.id,
            primary,
            secondary,
            target.expense.expenseDate,
        )
        is PlannerSummaryTarget.Note -> actions.saveNote(
            target.note.id,
            primary,
            secondary,
        )
        is PlannerSummaryTarget.Shopping -> actions.saveShoppingItem(
            target.item.id,
            primary,
            secondary,
        )
    }
}

private fun deleteTarget(target: PlannerSummaryTarget, actions: PlannerActionCallbacks) {
    when (target) {
        is PlannerSummaryTarget.Event -> actions.deleteEvent(target.event.id)
        is PlannerSummaryTarget.Meal -> actions.deleteMeal(target.meal.id)
        is PlannerSummaryTarget.Expense -> actions.deleteExpense(target.expense.id)
        is PlannerSummaryTarget.Note -> actions.deleteNote(target.note.id)
        is PlannerSummaryTarget.Shopping -> actions.deleteShoppingItem(target.item.id)
    }
}

private fun applyLanguageOverride(languageId: String?) {
    val languageTags = when (languageId) {
        "nb" -> "nb-NO"
        "en" -> "en-US"
        else -> ""
    }
    val currentTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    if (currentTags != languageTags) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTags))
    }
}

private fun BigDecimal.formatMoney(): String =
    stripTrailingZeros().toPlainString()

private fun memberDetail(member: FamilyMember): String =
    listOfNotNull(
        member.birthday?.toString(),
        member.bio?.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { member.color }

@Composable
private fun FamilyFooterTile(
    member: FamilyMember,
    onLongPress: (() -> Unit)? = null,
    onDragStarted: ((Offset) -> Unit)? = null,
    onDragged: ((Offset) -> Unit)? = null,
    onDragEnded: ((Offset) -> Unit)? = null,
    onDragCancelled: (() -> Unit)? = null,
) {
    var chipBounds by remember { mutableStateOf<Rect?>(null) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    Column(
        modifier = Modifier
            .width(72.dp)
            .onGloballyPositioned { coordinates ->
                chipBounds = coordinates.boundsInWindow()
            }
            .pointerInput(onLongPress, onDragStarted, onDragged, onDragEnded, onDragCancelled) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        val start = chipBounds?.topLeft?.plus(localOffset) ?: localOffset
                        dragPosition = start
                        onDragStarted?.invoke(start)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val next = (dragPosition ?: chipBounds?.center ?: Offset.Zero) + dragAmount
                        dragPosition = next
                        onDragged?.invoke(next)
                    },
                    onDragEnd = {
                        val finalPosition = dragPosition ?: chipBounds?.center
                        dragPosition = null
                        if (finalPosition != null) {
                            onDragEnded?.invoke(finalPosition)
                        } else {
                            onLongPress?.invoke()
                        }
                    },
                    onDragCancel = {
                        dragPosition = null
                        onDragCancelled?.invoke()
                    },
                )
            }
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AvatarSurface(member)
        Text(
            member.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = PlannerInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AddFamilyFooterTile(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable { onClick() }
            .padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            color = PlannerPastelPetal.copy(alpha = 0.72f),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, PlannerSoftBorder),
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        colors = listOf(
                            PlannerPastelPetal.copy(alpha = 0.82f),
                            PlannerSkyBlue.copy(alpha = 0.72f),
                        ),
                    ),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.settings_add_member))
            }
        }
        Text(
            stringResource(R.string.settings_add_member),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = PlannerInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AvatarSurface(member: FamilyMember) {
    val avatarBitmap = rememberAvatarBitmap(member.avatarUri)
    Surface(
        modifier = Modifier.size(52.dp),
        color = member.color.toComposeColor(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PlannerSoftBorder),
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = member.name.take(1).ifBlank { "?" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun MiniAvatar(
    member: FamilyMember?,
    fallbackColor: Color = PlannerSkyBlue,
    size: Dp,
) {
    val avatarBitmap = rememberAvatarBitmap(member?.avatarUri)
    Surface(
        modifier = Modifier.size(size),
        color = member?.color?.toComposeColor() ?: fallbackColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.88f)),
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = member?.name?.take(1)?.ifBlank { "?" } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = PlannerInk,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun eventOwner(event: PlannerEvent, familyMembers: List<FamilyMember>): FamilyMember? =
    familyMembers.firstOrNull { member ->
        member.id == event.sourceMemberId || member.id == event.ownerId
    }

@Composable
private fun rememberAvatarBitmap(avatarUri: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(avatarUri) {
        avatarUri
            ?.takeIf { it.isNotBlank() }
            ?.let { loadAvatarBitmap(context, it) }
    }
}

private fun loadAvatarBitmap(context: Context, avatarUri: String): ImageBitmap? =
    runCatching {
        val uri = avatarUri.toUri()
        val bitmap = if (uri.scheme == "file") {
            BitmapFactory.decodeFile(File(requireNotNull(uri.path)).absolutePath)
        } else {
            context.contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
        bitmap?.asImageBitmap()
    }.getOrNull()

private fun String.toComposeColor(): androidx.compose.ui.graphics.Color =
    runCatching {
        androidx.compose.ui.graphics.Color(toColorInt())
    }.getOrDefault(androidx.compose.ui.graphics.Color(0xFF3B82F6))

private fun previewDashboard(): PlannerDashboard =
    PlannerDashboard(
        isSetupComplete = true,
        familyMembers = listOf(
            FamilyMember(
                id = 1,
                name = "Anna",
                color = "#3b82f6",
                avatarUri = null,
                birthday = null,
                bio = null,
            ),
        ),
        weekStart = LocalDate.of(2026, 4, 20),
        weekEvents = emptyList(),
        upcomingEvents = emptyList(),
        meals = emptyList(),
        budget = BudgetSnapshot(
            month = "2026-04",
            limit = BigDecimal.ZERO,
            income = BigDecimal.ZERO,
            spent = BigDecimal.ZERO,
            remaining = BigDecimal.ZERO,
            available = BigDecimal.ZERO,
            currencyCode = "NOK",
            expenses = emptyList(),
        ),
        shoppingItems = emptyList(),
        notes = emptyList(),
    )

private fun previewActions(): PlannerActionCallbacks =
    PlannerActionCallbacks(
        addEvent = { _, _, _, _ -> },
        addMeal = { _, _ -> },
        addExpense = { _, _ -> },
        addNote = { _, _ -> },
        addShoppingItem = { _, _ -> },
        saveBudget = { _, _, _, _ -> },
        saveEvent = { _, _, _, _, _ -> },
        saveMeal = { _, _, _, _ -> },
        saveExpense = { _, _, _, _ -> },
        saveNote = { _, _, _ -> },
        saveShoppingItem = { _, _, _ -> },
        deleteEvent = { _ -> },
        deleteMeal = { _ -> },
        deleteExpense = { _ -> },
        deleteNote = { _ -> },
        deleteShoppingItem = { _ -> },
        saveFamilyMember = { _, _, _, _, _, _ -> },
        deleteFamilyMember = { _ -> },
        setLanguageOverride = { _ -> },
        setCurrencyCode = { _ -> },
        resetAllData = {},
    )

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PhonePreview() {
    FamilyPlannerPhoneShellTestContent()
}

@Composable
internal fun FamilyPlannerPhoneShellTestContent() {
    FamilyPlannerTheme {
        PhoneShell(
            dashboard = previewDashboard(),
            actions = previewActions(),
            onQuickActionSelected = {},
            onEntrySelected = {},
            onEventDateSelected = {},
            onSettingsClick = {},
        )
    }
}

@Composable
internal fun FamilyPlannerTabletDashboardTestContent() {
    FamilyPlannerTheme {
        TabletDashboard(
            dashboard = previewDashboard(),
            actions = previewActions(),
            onQuickActionSelected = {},
            onEntrySelected = {},
            onEventDateSelected = {},
            onFamilyDateSelected = { _, _ -> },
            onSettingsClick = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun TabletPreview() {
    FamilyPlannerTabletDashboardTestContent()
}
