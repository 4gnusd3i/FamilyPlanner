package io.github.by4gnusd3i.familyplanner.ui

import androidx.annotation.StringRes
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
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import io.github.by4gnusd3i.familyplanner.R
import io.github.by4gnusd3i.familyplanner.domain.model.BudgetSnapshot
import io.github.by4gnusd3i.familyplanner.domain.model.FamilyMember
import io.github.by4gnusd3i.familyplanner.domain.model.MealPlan
import io.github.by4gnusd3i.familyplanner.domain.model.NoteItem
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerEvent
import io.github.by4gnusd3i.familyplanner.domain.model.ShoppingItem
import io.github.by4gnusd3i.familyplanner.ui.theme.FamilyPlannerTheme
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

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
)

private val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.")

@Composable
fun FamilyPlannerApp(viewModel: FamilyPlannerViewModel) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val setupError by viewModel.setupError.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()

    FamilyPlannerContent(
        dashboard = dashboard,
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
        ),
    )
}

@Composable
private fun FamilyPlannerContent(
    dashboard: PlannerDashboard,
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

    activeAction?.let { action ->
        QuickActionDialog(
            action = action,
            onDismiss = { activeAction = null },
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
            onQuickActionSelected = { activeAction = it },
        )
    } else {
        PhoneShell(
            dashboard = dashboard,
            onQuickActionSelected = { activeAction = it },
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
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
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
            TopAppBar(title = { Text(stringResource(selected.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) })
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
    onQuickActionSelected: (PlannerQuickAction) -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            QuickActionRow(onQuickActionSelected)
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
                        BudgetSummary(dashboard.budget)
                    }
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.weight(1f),
                    ) {
                        ShoppingList(dashboard.shoppingItems)
                    }
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.weight(1f),
                    ) {
                        NotesList(dashboard.notes)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlannerPanel(R.string.week_title, Modifier.weight(1.6f)) {
                        UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers)
                    }
                    PlannerPanel(R.string.meals_title, Modifier.weight(0.7f)) {
                        MealsList(dashboard.meals)
                    }
                }
                PlannerPanel(
                    title = R.string.overview_title,
                    modifier = Modifier.width(280.dp),
                ) {
                    UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers)
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
                        UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers)
                    }
                }
            }
            PlannerDestination.Week -> {
                item {
                    PlannerPanel(R.string.week_title, Modifier.fillMaxWidth().height(520.dp)) {
                        UpcomingList(dashboard.upcomingEvents, dashboard.familyMembers)
                    }
                }
            }
            PlannerDestination.Meals -> {
                item {
                    PlannerPanel(R.string.meals_title, Modifier.fillMaxWidth().height(420.dp)) {
                        MealsList(dashboard.meals)
                    }
                }
            }
            PlannerDestination.Lists -> {
                item {
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    ) {
                        ShoppingList(dashboard.shoppingItems)
                    }
                }
                item {
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                    ) {
                        NotesList(dashboard.notes)
                    }
                }
            }
            PlannerDestination.Budget -> {
                item {
                    PlannerPanel(R.string.budget_title, Modifier.fillMaxWidth().height(420.dp)) {
                        BudgetSummary(dashboard.budget)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionRow(onQuickActionSelected: (PlannerQuickAction) -> Unit) {
    LazyRow(
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
            val owner = familyMembers.firstOrNull { it.id == event.ownerId }?.name
            PanelLine(
                title = listOfNotNull(
                    event.eventDate.format(ShortDateFormatter),
                    owner,
                    event.title,
                ).joinToString(" "),
                detail = event.note,
            )
        }
    }
}

@Composable
private fun MealsList(meals: List<MealPlan>) {
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
                title = meal.meal,
                detail = meal.note,
            )
        }
    }
}

@Composable
private fun ShoppingList(items: List<ShoppingItem>) {
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
                title = item.item,
                detail = stringResource(R.string.quantity_value, item.quantity),
            )
        }
    }
}

@Composable
private fun NotesList(notes: List<NoteItem>) {
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
                title = note.title,
                detail = note.content,
            )
        }
    }
}

@Composable
private fun BudgetSummary(budget: BudgetSnapshot) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PanelLine(
            title = stringResource(R.string.budget_spent, budget.spent.formatMoney(), budget.currencyCode),
            detail = stringResource(R.string.budget_remaining, budget.remaining.formatMoney(), budget.currencyCode),
        )
        if (budget.expenses.isNotEmpty()) {
            budget.expenses.take(3).forEach { expense ->
                PanelLine(
                    title = "${expense.category} ${expense.amount.formatMoney()} ${budget.currencyCode}",
                    detail = expense.description,
                )
            }
        }
    }
}

@Composable
private fun PanelLine(
    title: String,
    detail: String?,
) {
    Column(Modifier.fillMaxWidth()) {
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

private fun BigDecimal.formatMoney(): String =
    stripTrailingZeros().toPlainString()

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

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PhonePreview() {
    FamilyPlannerTheme {
        PhoneShell(previewDashboard(), onQuickActionSelected = {})
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun TabletPreview() {
    FamilyPlannerTheme {
        TabletDashboard(previewDashboard(), onQuickActionSelected = {})
    }
}
