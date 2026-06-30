package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WorkDay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageDaysDialog(
    allDaysDesc: List<WorkDay>,
    onDismiss: () -> Unit,
    onSave: (WorkDay) -> Unit,
    onDelete: (String) -> Unit
) {
    var editingDay by remember { mutableStateOf<WorkDay?>(null) }
    var showModifyDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "MANAGE CLOCKED DAYS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = {
                            editingDay = null // null means create new
                            showModifyDialog = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Record",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of Days
                if (allDaysDesc.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No days recorded yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(allDaysDesc, key = { it.date }) { day ->
                            DayRowItem(
                                day = day,
                                onEdit = {
                                    editingDay = day
                                    showModifyDialog = true
                                },
                                onDelete = {
                                    onDelete(day.date)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModifyDialog) {
        ModifyDayDialog(
            initialWorkDay = editingDay,
            onDismiss = { showModifyDialog = false },
            onSave = { updatedDay ->
                onSave(updatedDay)
                showModifyDialog = false
            }
        )
    }
}

@Composable
fun DayRowItem(
    day: WorkDay,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val totalDebt = day.getTotalDebt()
    val isOverwork = totalDebt <= 0
    val highlightColor = if (isOverwork) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val absMin = Math.abs(totalDebt)
    val hrs = absMin / 60
    val mns = absMin % 60
    val sign = if (totalDebt < 0) "-" else if (totalDebt > 0) "+" else ""
    val formattedDebt = if (day.checkIfNonWorkDay()) "REST" else String.format("%s%02d:%02d", sign, hrs, mns)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Date
                val formattedDate = try {
                    val ld = LocalDate.parse(day.date)
                    ld.format(DateTimeFormatter.ofPattern("EEE, MMM dd, yyyy"))
                } catch (e: Exception) {
                    day.date
                }
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (day.checkIfNonWorkDay()) {
                    Text(
                        text = "Rest Day (Non-work Day)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else {
                    val startStr = if (day.actualStartMillis != null) {
                        val t = LocalTime.ofInstant(Instant.ofEpochMilli(day.actualStartMillis), ZoneId.systemDefault())
                        String.format("%02d:%02d", t.hour, t.minute)
                    } else {
                        "--:--"
                    }
                    val endStr = if (day.actualEndMillis != null) {
                        val t = LocalTime.ofInstant(Instant.ofEpochMilli(day.actualEndMillis), ZoneId.systemDefault())
                        String.format("%02d:%02d", t.hour, t.minute)
                    } else {
                        "--:--"
                    }
                    val breakStr = "${day.totalBreakMinutes}m break"

                    Text(
                        text = "Clocked: $startStr - $endStr  |  $breakStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Target range
                    val targetStartStr = String.format("%02d:%02d", day.defaultStartMinutes / 60, day.defaultStartMinutes % 60)
                    val targetEndStr = String.format("%02d:%02d", day.defaultEndMinutes / 60, day.defaultEndMinutes % 60)
                    Text(
                        text = "Target: $targetStartStr - $targetEndStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Debt indicator tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(highlightColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formattedDebt,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = highlightColor
                    )
                }

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Day",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Day",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModifyDayDialog(
    initialWorkDay: WorkDay?,
    onDismiss: () -> Unit,
    onSave: (WorkDay) -> Unit
) {
    val isEditing = initialWorkDay != null
    var dateStr by remember { mutableStateOf(initialWorkDay?.date ?: LocalDate.now().toString()) }
    var isNonWorkDay by remember { mutableStateOf(initialWorkDay?.isNonWorkDay ?: false) }

    // Targets defaults
    var targetStartHour by remember { mutableStateOf((initialWorkDay?.defaultStartMinutes ?: (9 * 60)) / 60) }
    var targetStartMin by remember { mutableStateOf((initialWorkDay?.defaultStartMinutes ?: (9 * 60)) % 60) }
    var targetEndHour by remember { mutableStateOf((initialWorkDay?.defaultEndMinutes ?: (18 * 60)) / 60) }
    var targetEndMin by remember { mutableStateOf((initialWorkDay?.defaultEndMinutes ?: (18 * 60)) % 60) }

    // Clocked times
    var hasStart by remember { mutableStateOf(initialWorkDay?.actualStartMillis != null) }
    var startHour by remember {
        mutableStateOf(
            if (initialWorkDay?.actualStartMillis != null) {
                val t = LocalTime.ofInstant(Instant.ofEpochMilli(initialWorkDay.actualStartMillis), ZoneId.systemDefault())
                t.hour
            } else 9
        )
    }
    var startMin by remember {
        mutableStateOf(
            if (initialWorkDay?.actualStartMillis != null) {
                val t = LocalTime.ofInstant(Instant.ofEpochMilli(initialWorkDay.actualStartMillis), ZoneId.systemDefault())
                t.minute
            } else 0
        )
    }

    var hasEnd by remember { mutableStateOf(initialWorkDay?.actualEndMillis != null) }
    var endHour by remember {
        mutableStateOf(
            if (initialWorkDay?.actualEndMillis != null) {
                val t = LocalTime.ofInstant(Instant.ofEpochMilli(initialWorkDay.actualEndMillis), ZoneId.systemDefault())
                t.hour
            } else 18
        )
    }
    var endMin by remember {
        mutableStateOf(
            if (initialWorkDay?.actualEndMillis != null) {
                val t = LocalTime.ofInstant(Instant.ofEpochMilli(initialWorkDay.actualEndMillis), ZoneId.systemDefault())
                t.minute
            } else 0
        )
    }

    var breakMins by remember { mutableStateOf(initialWorkDay?.totalBreakMinutes ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Clocked Record" else "Add Clocked Record",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date Text Field
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    placeholder = { Text("e.g. 2026-06-26") },
                    singleLine = true,
                    enabled = !isEditing, // date cannot be edited directly if modifying to maintain primary key integrity
                    modifier = Modifier.fillMaxWidth()
                )

                // Non-workday switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Is Rest / Non-work Day?", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isNonWorkDay,
                        onCheckedChange = { isNonWorkDay = it }
                    )
                }

                if (!isNonWorkDay) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Clocked times section
                    Text(
                        text = "CLOCKING DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Start Time Checkbox & Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = hasStart, onCheckedChange = { hasStart = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clocked Start Time:", style = MaterialTheme.typography.bodySmall)
                    }

                    if (hasStart) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (startHour == 0 && startHour.toString().length == 1) "00" else String.format("%02d", startHour),
                                onValueChange = { startHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 9 },
                                label = { Text("Hour") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = if (startMin == 0 && startMin.toString().length == 1) "00" else String.format("%02d", startMin),
                                onValueChange = { startMin = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                label = { Text("Minute") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // End Time Checkbox & Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = hasEnd, onCheckedChange = { hasEnd = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clocked End Time:", style = MaterialTheme.typography.bodySmall)
                    }

                    if (hasEnd) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = if (endHour == 0 && endHour.toString().length == 1) "00" else String.format("%02d", endHour),
                                onValueChange = { endHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 18 },
                                label = { Text("Hour") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = if (endMin == 0 && endMin.toString().length == 1) "00" else String.format("%02d", endMin),
                                onValueChange = { endMin = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                                label = { Text("Minute") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Break minutes
                    OutlinedTextField(
                        value = breakMins.toString(),
                        onValueChange = { breakMins = it.toIntOrNull()?.coerceAtLeast(0) ?: 0 },
                        label = { Text("Break Duration (minutes)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Targets section
                    Text(
                        text = "BASELINE TARGETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Target Start
                    Text("Target Start Hour/Minute:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", targetStartHour),
                            onValueChange = { targetStartHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 9 },
                            label = { Text("Hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = String.format("%02d", targetStartMin),
                            onValueChange = { targetStartMin = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Target End
                    Text("Target End Hour/Minute:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = String.format("%02d", targetEndHour),
                            onValueChange = { targetEndHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 18 },
                            label = { Text("Hour") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = String.format("%02d", targetEndMin),
                            onValueChange = { targetEndMin = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
                            label = { Text("Minute") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDate = try {
                        LocalDate.parse(dateStr)
                        dateStr
                    } catch (e: Exception) {
                        LocalDate.now().toString()
                    }

                    val zoneId = ZoneId.systemDefault()

                    val startMillis = if (hasStart && !isNonWorkDay) {
                        try {
                            LocalDate.parse(parsedDate)
                                .atTime(LocalTime.of(startHour, startMin))
                                .atZone(zoneId)
                                .toInstant()
                                .toEpochMilli()
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    val endMillis = if (hasEnd && !isNonWorkDay) {
                        try {
                            LocalDate.parse(parsedDate)
                                .atTime(LocalTime.of(endHour, endMin))
                                .atZone(zoneId)
                                .toInstant()
                                .toEpochMilli()
                        } catch (e: Exception) {
                            null
                        }
                    } else {
                        null
                    }

                    val dayRecord = WorkDay(
                        date = parsedDate,
                        actualStartMillis = startMillis,
                        actualEndMillis = endMillis,
                        defaultStartMinutes = if (isNonWorkDay) (9 * 60) else (targetStartHour * 60 + targetStartMin),
                        defaultEndMinutes = if (isNonWorkDay) (18 * 60) else (targetEndHour * 60 + targetEndMin),
                        totalBreakMinutes = if (isNonWorkDay) 0 else breakMins,
                        isNonWorkDay = isNonWorkDay
                    )

                    onSave(dayRecord)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
