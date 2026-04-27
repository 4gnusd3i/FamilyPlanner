package io.github.fourgnusd3i.familyplanner.ui

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import io.github.fourgnusd3i.familyplanner.R
import io.github.fourgnusd3i.familyplanner.ui.theme.FamilyPlannerTheme

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
fun FamilyPlannerApp() {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val windowWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val isTablet = windowWidth >= 840.dp

    if (isTablet) {
        TabletDashboard()
    } else {
        PhoneShell()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneShell() {
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TabletDashboard() {
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
                    PlannerPanel(R.string.shopping_title, Modifier.weight(1f))
                    PlannerPanel(R.string.notes_title, Modifier.weight(1f))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    PlannerPanel(R.string.week_title, Modifier.weight(1.6f))
                    PlannerPanel(R.string.meals_title, Modifier.weight(0.7f))
                }
                PlannerPanel(R.string.overview_title, Modifier.width(280.dp))
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
                repeat(6) { index ->
                    FamilyChip(name = "Member ${index + 1}")
                }
            }
        }
    }
}

@Composable
private fun DestinationContent(destination: PlannerDestination, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        when (destination) {
            PlannerDestination.Overview -> {
                item { PlannerPanel(R.string.overview_title, Modifier.fillMaxWidth().height(280.dp)) }
            }
            PlannerDestination.Week -> {
                item { PlannerPanel(R.string.week_title, Modifier.fillMaxWidth().height(520.dp)) }
            }
            PlannerDestination.Meals -> {
                item { PlannerPanel(R.string.meals_title, Modifier.fillMaxWidth().height(420.dp)) }
            }
            PlannerDestination.Lists -> {
                item { PlannerPanel(R.string.shopping_title, Modifier.fillMaxWidth().height(260.dp)) }
                item { PlannerPanel(R.string.notes_title, Modifier.fillMaxWidth().height(260.dp)) }
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
private fun PlannerPanel(@StringRes title: Int, modifier: Modifier = Modifier) {
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
                    text = "${stringResource(R.string.empty_state)}. ${stringResource(R.string.coming_next)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FamilyChip(name: String) {
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
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun PhonePreview() {
    FamilyPlannerTheme {
        PhoneShell()
    }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
private fun TabletPreview() {
    FamilyPlannerTheme {
        TabletDashboard()
    }
}
