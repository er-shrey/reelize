package com.shrey.reelize.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shrey.reelize.Prefs
import com.shrey.reelize.data.ReelDay
import com.shrey.reelize.data.SOCIAL_APPS
import com.shrey.reelize.data.SocialUsage
import com.shrey.reelize.data.Stats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class SocialRow(val label: String, val millis: Long)

data class DashboardState(
    val selected: LocalDate = LocalDate.now(),
    val week: List<ReelDay> = emptyList(),
    val social: List<SocialRow> = emptyList(),
    val counterOn: Boolean = true,
    val usageAccess: Boolean = true,
    val bubbleEnabled: Boolean = true,
) {
    val day: ReelDay? get() = week.lastOrNull()
    val socialTotal: Long get() = social.sumOf { it.millis }
}

@OptIn(FlowPreview::class)
class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    private val selected = MutableStateFlow(LocalDate.now())
    private val _state = MutableStateFlow(DashboardState(bubbleEnabled = prefs.bubbleEnabled))
    val state: StateFlow<DashboardState> = _state

    init {
        Stats.init(app)
        viewModelScope.launch {
            combine(selected, Stats.version.debounce(300)) { day, _ -> day }.collect { load(it) }
        }
    }

    fun select(day: LocalDate) {
        selected.value = minOf(day, LocalDate.now())
    }

    fun setBubble(enabled: Boolean) {
        prefs.bubbleEnabled = enabled
        _state.update { it.copy(bubbleEnabled = enabled) }
    }

    /** Called on resume: permissions may have changed in Settings, and the day may have rolled over. */
    fun refresh() {
        val ctx = getApplication<Application>()
        _state.update {
            it.copy(counterOn = Access.counterOn(ctx), usageAccess = SocialUsage.hasAccess(ctx), bubbleEnabled = prefs.bubbleEnabled)
        }
        viewModelScope.launch { load(selected.value) }
    }

    private suspend fun load(day: LocalDate) {
        val ctx = getApplication<Application>()
        val (week, social) = withContext(Dispatchers.IO) {
            val week = Stats.reelDays(day.minusDays(6), day)
            val social = SocialUsage.forDay(ctx, day)
                .entries.groupBy({ SOCIAL_APPS[it.key] ?: it.key }, { it.value })
                .map { (label, ms) -> SocialRow(label, ms.sum()) }
                .sortedByDescending { it.millis }
            week to social
        }
        _state.update { it.copy(selected = day, week = week, social = social) }
    }
}
