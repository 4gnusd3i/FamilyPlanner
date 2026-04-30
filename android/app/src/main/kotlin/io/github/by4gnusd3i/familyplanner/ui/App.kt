package io.github.by4gnusd3i.familyplanner.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.os.LocaleListCompat
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    val addEvent: (title: String, note: String?) -> Unit,
    val addMeal: (meal: String, note: String?) -> Unit,
    val addExpense: (amount: String, category: String?) -> Unit,
    val addNote: (title: String, content: String?) -> Unit,
    val addShoppingItem: (item: String, quantity: String) -> Unit,
    val saveBudget: (limit: String, income: String, currencyCode: String, month: String) -> Unit,
    val saveEvent: (id: Long?, title: String, note: String?, eventDate: LocalDate) -> Unit,
    val saveMeal: (id: Long?, dayOfWeek: Int, meal: String, note: String?) -> Unit,
    val saveExpense: (id: Long?, amount: String, category: String?, expenseDate: LocalDate) -> Unit,
    val saveNote: (id: Long?, title: String, content: String?) -> Unit,
    val saveShoppingItem: (id: Long?, item: String, quantity: String) -> Unit,
    val deleteEvent: (id: Long) -> Unit,
    val deleteMeal: (id: Long) -> Unit,
    val deleteExpense: (id: Long) -> Unit,
    val deleteNote: (id: Long) -> Unit,
    val deleteShoppingItem: (id: Long) -> Unit,
    val saveFamilyMember: (id: Long?, name: String, color: String?, birthday: String, bio: String?) -> Unit,
    val deleteFamilyMember: (id: Long) -> Unit,
    val setLanguageOverride: (languageId: String?) -> Unit,
    val setCurrencyCode: (currencyCode: String) -> Unit,
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
    var summaryTarget by remember { mutableStateOf<PlannerSummaryTarget?>(null) }
    var editTarget by remember { mutableStateOf<PlannerSummaryTarget?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        SettingsDialog(
            settings = settings,
            familyMembers = dashboard.familyMembers,
            onDismiss = { showSettings = false },
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
            onQuickActionSelected = { activeAction = it },
            onEntrySelected = { summaryTarget = it },
            onSettingsClick = { showSettings = true },
        )
    } else {
        PhoneShell(
            dashboard = dashboard,
            actions = actions,
            onQuickActionSelected = { activeAction = it },
            onEntrySelected = { summaryTarget = it },
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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneShell(
    dashboard: PlannerDashboard,
    actions: PlannerActionCallbacks,
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(selected.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
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
            FloatingActionButton(onClick = { showQuickActions = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
        },
    ) { padding ->
        DestinationContent(
            destination = selected,
            dashboard = dashboard,
            actions = actions,
            onEntrySelected = onEntrySelected,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabletDashboard(
    dashboard: PlannerDashboard,
    actions: PlannerActionCallbacks,
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
    onSettingsClick: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuickActionRow(
                    modifier = Modifier.weight(1f),
                    onQuickActionSelected = onQuickActionSelected,
                )
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings_title))
                }
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.width(280.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlannerPanel(
                        title = R.string.budget_title,
                        modifier = Modifier.weight(0.8f),
                    ) {
                        BudgetSummary(dashboard.budget, onEntrySelected, actions)
                    }
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.weight(1f),
                    ) {
                        ShoppingList(dashboard.shoppingItems, onEntrySelected)
                    }
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.weight(1f),
                    ) {
                        NotesList(dashboard.notes, onEntrySelected)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlannerPanel(R.string.week_title, Modifier.weight(1.6f)) {
                        UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers, onEntrySelected)
                    }
                    PlannerPanel(R.string.meals_title, Modifier.weight(0.7f)) {
                        MealsList(dashboard.meals, onEntrySelected)
                    }
                }
                PlannerPanel(
                    title = R.string.overview_title,
                    modifier = Modifier.width(280.dp),
                ) {
                    UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers, onEntrySelected)
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.family_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                dashboard.familyMembers.forEach { member ->
                    FamilyChip(member)
                }
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
                        UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers, onEntrySelected)
                    }
                }
            }
            PlannerDestination.Meals -> {
                item {
                    PlannerPanel(R.string.meals_title, Modifier.fillMaxWidth().height(420.dp)) {
                        MealsList(dashboard.meals, onEntrySelected)
                    }
                }
            }
            PlannerDestination.Lists -> {
                item {
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    ) {
                        ShoppingList(dashboard.shoppingItems, onEntrySelected)
                    }
                }
                item {
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    ) {
                        NotesList(dashboard.notes, onEntrySelected)
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
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        PlannerQuickAction.entries.forEach { action ->
            item {
                QuickAction(
                    label = action.titleRes,
                    onClick = { onQuickActionSelected(action) },
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    @StringRes label: Int,
    onClick: () -> Unit,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(label), maxLines = 1) },
    )
}

@Composable
private fun PlannerPanel(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    body: String? = null,
    content: @Composable (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        events.take(5).forEach { event ->
            val owner = if (event.isBirthdayEvent()) {
                null
            } else {
                familyMembers.firstOrNull { it.id == event.ownerId }?.name
            }
            val title = eventDisplayTitle(event, familyMembers)
            PanelLine(
                modifier = Modifier.clickable { onEntrySelected(PlannerSummaryTarget.Event(event)) },
                title = listOfNotNull(
                    event.eventDate.format(ShortDateFormatter),
                    owner,
                    title,
                ).joinToString(" "),
                detail = event.note,
            )
        }
    }
}

@Composable
private fun MealsList(
    meals: List<MealPlan>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    if (meals.isEmpty()) {
        EmptyPanelText()
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        meals.take(5).forEach { meal ->
            PanelLine(
                modifier = Modifier.clickable { onEntrySelected(PlannerSummaryTarget.Meal(meal)) },
                title = meal.meal,
                detail = meal.note,
            )
        }
    }
}

@Composable
private fun ShoppingList(
    items: List<ShoppingItem>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    if (items.isEmpty()) {
        EmptyPanelText()
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.take(5).forEach { item ->
            PanelLine(
                modifier = Modifier.clickable { onEntrySelected(PlannerSummaryTarget.Shopping(item)) },
                title = item.item,
                detail = stringResource(R.string.quantity_value, item.quantity),
            )
        }
    }
}

@Composable
private fun NotesList(
    notes: List<NoteItem>,
    onEntrySelected: (PlannerSummaryTarget) -> Unit,
) {
    if (notes.isEmpty()) {
        EmptyPanelText()
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        notes.take(5).forEach { note ->
            PanelLine(
                modifier = Modifier.clickable { onEntrySelected(PlannerSummaryTarget.Note(note)) },
                title = note.title,
                detail = note.content,
            )
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
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PanelLine(
            title = stringResource(R.string.budget_spent, budget.spent.formatMoney(), budget.currencyCode),
            detail = stringResource(R.string.budget_remaining, budget.remaining.formatMoney(), budget.currencyCode),
        )
        PanelLine(
            title = stringResource(R.string.budget_income, budget.income.formatMoney(), budget.currencyCode),
            detail = stringResource(R.string.budget_limit, budget.limit.formatMoney(), budget.currencyCode),
        )
        Button(
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        PlannerQuickAction.Event -> actions.addEvent(primary, secondary)
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
    val title = if (member == null) R.string.settings_add_member else R.string.settings_edit_member

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    actions.saveFamilyMember(member?.id, name, color, birthday, bio)
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
            target.event.eventDate,
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
private fun FamilyChip(member: FamilyMember) {
    Card(shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
            ) {}
            Spacer(Modifier.width(10.dp))
            Text(member.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

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
        addEvent = { _, _ -> },
        addMeal = { _, _ -> },
        addExpense = { _, _ -> },
        addNote = { _, _ -> },
        addShoppingItem = { _, _ -> },
        saveBudget = { _, _, _, _ -> },
        saveEvent = { _, _, _, _ -> },
        saveMeal = { _, _, _, _ -> },
        saveExpense = { _, _, _, _ -> },
        saveNote = { _, _, _ -> },
        saveShoppingItem = { _, _, _ -> },
        deleteEvent = { _ -> },
        deleteMeal = { _ -> },
        deleteExpense = { _ -> },
        deleteNote = { _ -> },
        deleteShoppingItem = { _ -> },
        saveFamilyMember = { _, _, _, _, _ -> },
        deleteFamilyMember = { _ -> },
        setLanguageOverride = { _ -> },
        setCurrencyCode = { _ -> },
    )

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PhonePreview() {
    FamilyPlannerTheme {
        PhoneShell(
            dashboard = previewDashboard(),
            actions = previewActions(),
            onQuickActionSelected = {},
            onEntrySelected = {},
            onSettingsClick = {},
        )
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun TabletPreview() {
    FamilyPlannerTheme {
        TabletDashboard(
            dashboard = previewDashboard(),
            actions = previewActions(),
            onQuickActionSelected = {},
            onEntrySelected = {},
            onSettingsClick = {},
        )
    }
}
