package com.shrey.reelize.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shrey.reelize.detect.ReelApp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(state: DashboardState, vm: DashboardViewModel, onOpenLog: () -> Unit, onOpenBubble: () -> Unit) {
    var picking by remember { mutableStateOf(false) }
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reelize") },
                actions = {
                    IconButton(onClick = onOpenLog) { Icon(Icons.Filled.BugReport, "Detection log") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding() + 4.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                !state.counterOn && state.counterEverOn -> item { CounterOffCard() }
                !state.counterOn -> item { CounterSetupCard() }
                !state.batteryUnrestricted && !state.keepRunningDismissed -> item {
                    KeepRunningPromptCard(onDismiss = vm::dismissKeepRunning)
                }
            }
            if (!state.usageAccess) item { UsageSetupCard() }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { vm.select(state.selected.minusDays(1)) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day")
                    }
                    TextButton(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.CalendarMonth, null)
                        Spacer(Modifier.width(8.dp))
                        Text(dayTitle(state.selected), style = MaterialTheme.typography.titleMedium)
                    }
                    IconButton(onClick = { vm.select(state.selected.plusDays(1)) }, enabled = state.selected < today) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day")
                    }
                }
            }

            item { HeroCard(state) }

            item {
                SectionCard("Last 7 days") {
                    WeekChart(state.week, state.selected, vm::select)
                    val total = state.week.sumOf { it.totalReels }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "$total reels · ${total / 7} a day on average · ${duration(state.week.sumOf { it.totalMillis })} in feeds",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item { SocialCard(state) }

            item {
                SectionCard("Floating counter") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Show the bubble while Reels or Shorts are open",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(checked = state.bubbleEnabled, onCheckedChange = vm::setBubble)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenBubble) { Text("Customize bubble") }
                }
            }

            item { KeepRunningSection(state.batteryUnrestricted) }
        }
    }

    if (picking) {
        val zone = ZoneOffset.UTC
        val todayUtcMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selected.atStartOfDay(zone).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMs
                override fun isSelectableYear(year: Int) = year <= today.year
            },
        )
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        vm.select(Instant.ofEpochMilli(it).atZone(zone).toLocalDate())
                    }
                    picking = false
                }) { Text("Show") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
        ) { DatePicker(pickerState) }
    }
}

@Composable
private fun HeroCard(state: DashboardState) {
    val day = state.day
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                (day?.totalReels ?: 0).toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "reels & shorts watched",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReelApp.entries.forEach { app ->
                    Stat(
                        "${app.label} ${app.feedName}",
                        (day?.reels?.get(app) ?: 0).toString(),
                        duration(day?.millis?.get(app) ?: 0),
                        Modifier.weight(1f),
                    )
                }
                Stat("Time in feeds", duration(day?.totalMillis ?: 0), null, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, sub: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        if (sub != null) Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f))
    }
}

@Composable
private fun SocialCard(state: DashboardState) {
    SectionCard("Social media time") {
        if (!state.usageAccess) {
            Text("Needs usage access - see above.", style = MaterialTheme.typography.bodyMedium)
            return@SectionCard
        }
        Text(duration(state.socialTotal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (state.social.isEmpty()) {
            Text("No social apps used this day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val max = state.social.maxOfOrNull { it.millis }?.coerceAtLeast(1) ?: 1
        state.social.forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(row.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
                LinearProgressIndicator(
                    progress = { row.millis.toFloat() / max },
                    modifier = Modifier.weight(1f).height(8.dp),
                    drawStopIndicator = {},
                    gapSize = 0.dp,
                )
                Text(duration(row.millis), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(64.dp).padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun CounterSetupCard() {
    val context = LocalContext.current
    SetupCard(
        title = "Turn on the reel counter",
        body = "Reelize counts reels through Android's accessibility settings. Open them, tap " +
            "Installed apps (or Downloaded apps), choose Reelize and switch it on.\n\n" +
            "If the switch is greyed out as a \"Restricted setting\", that's Android being careful " +
            "with apps installed from a file: open App info, tap the ⋮ menu, choose " +
            "\"Allow restricted settings\", then try again.",
    ) {
        Button(onClick = { Access.openAccessibility(context) }) { Text("Open settings") }
        OutlinedButton(onClick = { Access.openAppInfo(context) }) { Text("App info") }
    }
}

/**
 * The counter ran before and is off now. That is almost always a force-stop - "Clear all" in
 * Recents on many phones, or a battery cleaner - which makes Android switch it off.
 */
@Composable
private fun CounterOffCard() {
    val context = LocalContext.current
    SetupCard(
        title = "The reel counter was switched off",
        body = "Android turns the counter off when Reelize is force-stopped, which on many phones " +
            "happens when you clear all apps from Recents or a battery cleaner runs. Nothing is " +
            "counted until it's back on. Switch it on again, then follow the steps below so it stays on.",
    ) {
        Button(onClick = { Access.openAccessibility(context) }) { Text("Turn on again") }
    }
}

/** Shown once the counter is working, until battery is unrestricted or the user dismisses it. */
@Composable
private fun KeepRunningPromptCard(onDismiss: () -> Unit) {
    val context = LocalContext.current
    SetupCard(
        title = "Keep the counter running",
        body = "Some phones close background apps to save battery, and that switches the counter " +
            "off. Let Reelize run in the background so it keeps counting. The steps for your phone " +
            "are at the bottom of this screen.",
    ) {
        Button(onClick = { Access.requestBatteryUnrestricted(context) }) { Text("Allow") }
        TextButton(onClick = onDismiss) { Text("Not now") }
    }
}

@Composable
private fun KeepRunningSection(batteryUnrestricted: Boolean) {
    val context = LocalContext.current
    val tips = remember { keepRunningTips() }
    SectionCard("Keep the counter running") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (batteryUnrestricted) "Battery: unrestricted ✓" else "Battery: optimized - Android may stop Reelize",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            if (!batteryUnrestricted) {
                Button(onClick = { Access.requestBatteryUnrestricted(context) }) { Text("Allow") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("On ${tips.brand} phones", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        tips.steps.forEachIndexed { i, step ->
            Text("${i + 1}. $step", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { Access.openAppInfo(context) }) { Text("App info") }
            tips.extraScreen?.let { screen ->
                OutlinedButton(onClick = {
                    val intent = android.content.Intent().setClassName(screen.packageName, screen.className)
                    if (!Access.tryStart(context, intent)) Access.openAppInfo(context)
                }) { Text(screen.label) }
            }
        }
    }
}

@Composable
private fun UsageSetupCard() {
    val context = LocalContext.current
    SetupCard(
        title = "Allow usage access",
        body = "For total time in social apps, Reelize reads Android's own app-usage records. Find Reelize in the list and allow it.",
    ) {
        Button(onClick = { Access.openUsageAccess(context) }) { Text("Open settings") }
    }
}

@Composable
private fun SetupCard(title: String, body: String, actions: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { actions() }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}
