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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.by4gnusd3i.familyplanner.R
import io.github.by4gnusd3i.familyplanner.domain.model.FamilyMember
import io.github.by4gnusd3i.familyplanner.domain.model.PlannerDashboard
import io.github.by4gnusd3i.familyplanner.ui.theme.FamilyPlannerTheme

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

@Composable
fun FamilyPlannerApp(viewModel: FamilyPlannerViewModel) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val setupError by viewModel.setupError.collectAsStateWithLifecycle()

    FamilyPlannerContent(
        dashboard = dashboard,
        setupError = setupError,
        onSetupSubmit = viewModel::initializeHousehold,
    )
}

@Composable
private fun FamilyPlannerContent(
    dashboard: PlannerDashboard,
    setupError: String?,
    onSetupSubmit: (familyName: String, firstMemberName: String) -> Unit,
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

    if (isTablet) {
        TabletDashboard(dashboard)
    } else {
        PhoneShell(dashboard)
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
private fun PhoneShell(dashboard: PlannerDashboard) {
    var selected by rememberSaveable { mutableStateOf(PlannerDestination.Overview) }

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
            FloatingActionButton(onClick = { }) {
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
private fun TabletDashboard(dashboard: PlannerDashboard) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            QuickActionRow()
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.width(280.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlannerPanel(R.string.budget_title, Modifier.weight(0.8f))
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.weight(1f),
                        body = stringResource(R.string.count_shopping, dashboard.shoppingItems.size),
                    )
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.weight(1f),
                        body = stringResource(R.string.count_notes, dashboard.notes.size),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlannerPanel(R.string.week_title, Modifier.weight(1.6f))
                    PlannerPanel(R.string.meals_title, Modifier.weight(0.7f))
                }
                PlannerPanel(
                    title = R.string.overview_title,
                    modifier = Modifier.width(280.dp),
                    body = stringResource(R.string.count_upcoming, dashboard.upcomingEvents.size),
                )
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
                        body = stringResource(R.string.count_upcoming, dashboard.upcomingEvents.size),
                    )
                }
            }
            PlannerDestination.Week -> {
                item { PlannerPanel(R.string.week_title, Modifier.fillMaxWidth().height(520.dp)) }
            }
            PlannerDestination.Meals -> {
                item { PlannerPanel(R.string.meals_title, Modifier.fillMaxWidth().height(420.dp)) }
            }
            PlannerDestination.Lists -> {
                item {
                    PlannerPanel(
                        title = R.string.shopping_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        body = stringResource(R.string.count_shopping, dashboard.shoppingItems.size),
                    )
                }
                item {
                    PlannerPanel(
                        title = R.string.notes_title,
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        body = stringResource(R.string.count_notes, dashboard.notes.size),
                    )
                }
            }
            PlannerDestination.Budget -> {
                item { PlannerPanel(R.string.budget_title, Modifier.fillMaxWidth().height(420.dp)) }
            }
        }
    }
}

@Composable
private fun QuickActionRow() {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        item { QuickAction(R.string.quick_event) }
        item { QuickAction(R.string.quick_meal) }
        item { QuickAction(R.string.quick_expense) }
        item { QuickAction(R.string.quick_note) }
        item { QuickAction(R.string.quick_item) }
    }
}

@Composable
private fun QuickAction(@StringRes label: Int) {
    ExtendedFloatingActionButton(
        onClick = { },
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text(stringResource(label), maxLines = 1) },
    )
}

@Composable
private fun PlannerPanel(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    body: String? = null,
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
                Text(
                    text = body ?: "${stringResource(R.string.empty_state)}. ${stringResource(R.string.coming_next)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

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
        shoppingItems = emptyList(),
        notes = emptyList(),
    )

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PhonePreview() {
    FamilyPlannerTheme {
        PhoneShell(previewDashboard())
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun TabletPreview() {
    FamilyPlannerTheme {
        TabletDashboard(previewDashboard())
    }
}
