package com.shrey.reelize.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shrey.reelize.data.ReelDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Seven bars, one per day, ending on the selected day. Tapping a bar selects that day. */
@Composable
fun WeekChart(week: List<ReelDay>, selected: LocalDate, onSelect: (LocalDate) -> Unit) {
    val max = (week.maxOfOrNull { it.totalReels } ?: 0).coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().height(170.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        week.forEach { day ->
            val isSelected = day.day == selected
            val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().clickable { onSelect(day.day) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    day.totalReels.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    val fraction = day.totalReels.toFloat() / max
                    Box(
                        Modifier.fillMaxWidth()
                            .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                            .background(color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    day.day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}
