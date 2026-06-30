package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun ManualEntryDialog(
    initialDate: String = LocalDate.now().toString(),
    initialStartHour: Int = 10,
    initialStartMin: Int = 0,
    initialEndHour: Int = 18,
    initialEndMin: Int = 0,
    onDismiss: () -> Unit,
    onSave: (date: String, startHour: Int, startMin: Int, endHour: Int, endMin: Int) -> Unit,
    onDelete: ((date: String) -> Unit)? = null
) {
    var dateStr by remember { mutableStateOf(initialDate) }
    
    var startHourStr by remember { mutableStateOf(String.format("%02d", initialStartHour)) }
    var startMinStr by remember { mutableStateOf(String.format("%02d", initialStartMin)) }
    
    var endHourStr by remember { mutableStateOf(String.format("%02d", initialEndHour)) }
    var endMinStr by remember { mutableStateOf(String.format("%02d", initialEndMin)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Edit Daily Baseline", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date input
                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    placeholder = { Text("e.g. 2026-06-26") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Configure the target working hours for this day. Any clocked work starting earlier or ending later will count as overwork, and any starting later or ending earlier will count as owed time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Start Time Inputs
                Text(
                    text = "Target Workday Starts:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startHourStr,
                        onValueChange = { if (it.length <= 2) startHourStr = it },
                        label = { Text("Hour (00-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startMinStr,
                        onValueChange = { if (it.length <= 2) startMinStr = it },
                        label = { Text("Min (00-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // End Time Inputs
                Text(
                    text = "Target Workday Ends:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = endHourStr,
                        onValueChange = { if (it.length <= 2) endHourStr = it },
                        label = { Text("Hour (00-23)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endMinStr,
                        onValueChange = { if (it.length <= 2) endMinStr = it },
                        label = { Text("Min (00-59)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onDelete != null) {
                    TextButton(
                        onClick = {
                            onDelete(dateStr)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                Button(
                    onClick = {
                        // Validate and parse inputs
                        val parsedDate = try {
                            LocalDate.parse(dateStr)
                            dateStr
                        } catch (e: Exception) {
                            LocalDate.now().toString()
                        }

                        val startH = startHourStr.toIntOrNull()?.coerceIn(0, 23) ?: 10
                        val startM = startMinStr.toIntOrNull()?.coerceIn(0, 59) ?: 0
                        val endH = endHourStr.toIntOrNull()?.coerceIn(0, 23) ?: 18
                        val endM = endMinStr.toIntOrNull()?.coerceIn(0, 59) ?: 0

                        onSave(
                            parsedDate,
                            startH,
                            startM,
                            endH,
                            endM
                        )
                        onDismiss()
                    }
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
