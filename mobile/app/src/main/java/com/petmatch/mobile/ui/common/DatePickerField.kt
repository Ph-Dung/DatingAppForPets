package com.petmatch.mobile.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.petmatch.mobile.ui.theme.PrimaryPink
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * Click vào → mở Material3 DatePickerDialog
 * [value] kiểu String "yyyy-MM-dd" hoặc rỗng
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    value: String,          // "yyyy-MM-dd" or ""
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    optional: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    // Parse hiện tại
    val currentMillis = remember(value) {
        if (value.isBlank()) null
        else try {
            val ld = LocalDate.parse(value, fmt)
            ld.toEpochDay() * 86_400_000L
        } catch (_: Exception) { null }
    }

    val displayText = if (value.isBlank()) {
        if (optional) "Không chọn" else "Chọn ngày"
    } else value

    // Clickable field
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { showPicker = true }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, null, tint = PrimaryPink, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    displayText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (value.isBlank()) FontWeight.Normal else FontWeight.SemiBold
                    ),
                    color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = currentMillis)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showPicker = false
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val ld = LocalDate.ofEpochDay(millis / 86_400_000L)
                        onValueChange(ld.format(fmt))
                    } else if (optional) {
                        onValueChange("")
                    }
                }) { Text("OK", color = PrimaryPink) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(
                state = pickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = PrimaryPink,
                    todayDateBorderColor = PrimaryPink
                )
            )
        }
    }
}
