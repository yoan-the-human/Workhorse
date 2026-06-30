package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.WorkDay
import com.example.ui.*
import java.time.LocalDate
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: WorkDayViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val allDaysAsc by viewModel.allDaysAsc.collectAsState()
    val allDaysDesc by viewModel.allDaysDesc.collectAsState()
    val todayDay by viewModel.todayWorkDay.collectAsState()

    var showManualEntryDialog by remember { mutableStateOf(false) }
    var showPasteImportDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var isGridExpanded by remember { mutableStateOf(false) }

    val stats = remember(allDaysAsc) { viewModel.calculateStats(allDaysAsc) }
    val gridSquares = remember(allDaysAsc) { viewModel.getGridSquares(allDaysAsc) }

    // Calculate cumulative debt
    val totalCumulativeDebt = remember(allDaysAsc) {
        allDaysAsc.filter { !it.checkIfNonWorkDay() }.sumOf { it.getTotalDebt() }
    }

    // Active break duration ticker
    var breakTickerMins by remember { mutableStateOf(0L) }
    LaunchedEffect(todayDay?.activeBreakStartMillis) {
        val start = todayDay?.activeBreakStartMillis
        if (start != null) {
            while (true) {
                val elapsedMs = System.currentTimeMillis() - start
                breakTickerMins = elapsedMs / 60000
                kotlinx.coroutines.delay(10000) // update every 10 seconds
            }
        } else {
            breakTickerMins = 0L
        }
    }

    // File Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val jsonStr = stream.bufferedReader().use { it.readText() }
                    coroutineScope.launch {
                        val success = viewModel.importBackup(jsonStr)
                        if (success) {
                            Toast.makeText(context, "Backup imported successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid backup data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load backup file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            // High-end custom header to perfectly mimic the Sophisticated Dark style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Workhorse",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = (-0.5).sp
                            )
                        )
                        Text(
                            text = "TIME BALANCE TRACKER",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                letterSpacing = 1.5.sp
                            )
                        )
                    }

                    // Rounded buttons representing export, import, and custom plus
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Export button
                        IconButton(
                            onClick = { viewModel.shareBackup(context) },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Backup",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }


                        // Plus button for manual entries
                        IconButton(
                            onClick = { showManualEntryDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Edit Manual Entry",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // --- MAIN BALANCE DISPLAY / CUMULATIVE DEBT GAUGE ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                // Subtle decorative top-right glowing circle decoration (10% opacity)
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .offset(x = 65.dp, y = (-25).dp)
                        .border(10.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                        .align(Alignment.TopEnd)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CURRENT BALANCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    // Large glowing balance text
                    val gaugeColor = if (totalCumulativeDebt <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    val absMin = Math.abs(totalCumulativeDebt)
                    val hrs = absMin / 60
                    val mns = absMin % 60
                    val sign = if (totalCumulativeDebt < 0) "-" else if (totalCumulativeDebt > 0) "+" else ""
                    val formattedBalance = String.format("%s%02d:%02d", sign, hrs, mns)

                    Text(
                        text = formattedBalance,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            color = gaugeColor,
                            letterSpacing = (-1.5).sp
                        )
                    )
                }
            }

            // --- PRIMARY CONTROLS (3-COLUMN BUTTON PANEL) ---
            val hasStarted = todayDay?.actualStartMillis != null
            val hasEnded = todayDay?.actualEndMillis != null
            val isOutside = todayDay?.activeBreakStartMillis != null

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. START WORKDAY BUTTON
                val startEnabled = !hasStarted
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (startEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (startEnabled) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = startEnabled) { viewModel.startWorkday() }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (startEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Rotated cyan-400 square mimicking the play icon rotated design
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(
                                    if (startEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                        )
                    }
                    Text(
                        text = "START",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (startEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = TimeUtils.formatTime(todayDay?.actualStartMillis),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (hasStarted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // 2. AWAY / OUTSIDE BREAK BUTTON
                val breakEnabled = hasStarted && !hasEnded
                val awayBgColor = if (isOutside) MaterialTheme.colorScheme.primary
                else if (breakEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                val awayContentColor = if (isOutside) Color.Black
                else if (breakEnabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(awayBgColor)
                        .border(
                            1.dp,
                            if (isOutside) Color.Transparent
                            else if (breakEnabled) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = breakEnabled) { viewModel.toggleBreak() }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOutside) Color.White.copy(alpha = 0.3f)
                                else if (breakEnabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isOutside) {
                            // Circular spinner animating to represent the active break
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Go Outside",
                                tint = awayContentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isOutside) "AWAY" else "AWAY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = awayContentColor
                    )
                    val totalBreaks = if (isOutside) {
                        (todayDay?.totalBreakMinutes ?: 0) + breakTickerMins
                    } else {
                        (todayDay?.totalBreakMinutes ?: 0)
                    }
                    val breakDebtVal = totalBreaks.toInt() - 60
                    val breakLabelText = if (!hasStarted) {
                        "0m"
                    } else if (breakDebtVal >= 0) {
                        "+${breakDebtVal}m"
                    } else {
                        "${breakDebtVal}m"
                    }
                    Text(
                        text = breakLabelText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isOutside) Color.Black else if (breakEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // 3. END WORKDAY BUTTON
                val endEnabled = hasStarted && !hasEnded
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (endEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                        )
                        .border(
                            1.dp,
                            if (endEnabled) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = endEnabled) { viewModel.endWorkday() }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (endEnabled) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (endEnabled) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                        )
                    }
                    Text(
                        text = "END",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (endEnabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Text(
                        text = TimeUtils.formatTime(todayDay?.actualEndMillis),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (hasEnded) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }


            // --- ANALYTICS GRID (4 KPI CARDS) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KPI 1: Morning Shift Start Offset
                AnalyticsKPICard(
                    label = "Morning",
                    value = formatMinutesToShortOffset(stats.avgMorning),
                    isGood = stats.avgMorning <= 0,
                    diffText = "(${formatMinutesToShortOffset(stats.diffMorning)})",
                    modifier = Modifier.weight(1f)
                )
                // KPI 2: Evening Shift End Offset
                AnalyticsKPICard(
                    label = "Evening",
                    value = formatMinutesToShortOffset(stats.avgEvening),
                    isGood = stats.avgEvening <= 0,
                    diffText = "(${formatMinutesToShortOffset(stats.diffEvening)})",
                    modifier = Modifier.weight(1f)
                )
                // KPI 3: Middle Break Offset
                AnalyticsKPICard(
                    label = "Middle",
                    value = formatMinutesToShortOffset(stats.avgMiddle),
                    isGood = stats.avgMiddle <= 0,
                    diffText = "(${formatMinutesToShortOffset(stats.diffMiddle)})",
                    modifier = Modifier.weight(1f)
                )
                // KPI 4: Daily Avg Trend
                AnalyticsKPICard(
                    label = "Daily Avg",
                    value = formatMinutesToShortOffset(stats.avgTotal),
                    isGood = stats.avgTotal <= 0,
                    diffText = "(${formatMinutesToShortOffset(stats.diffTotal)})",
                    modifier = Modifier.weight(1f)
                )
            }

            // --- 228 DAYS CONSISTENCY GRID MAP ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111111))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGridExpanded = !isGridExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "228-DAY CONSISTENCY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = if (isGridExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(if (isGridExpanded) 180f else 0f)
                            )
                        }

                        // Inline Mini Legend
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MiniLegendItem("Win", MaterialTheme.colorScheme.primary)
                            MiniLegendItem("Owe", MaterialTheme.colorScheme.error)
                        }
                    }

                    // Heatmap Grid: 19 columns, non-scrollable, viewed as a whole
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val columnsCount = 19
                        val rows = gridSquares.chunked(columnsCount)
                        val displayedRows = if (isGridExpanded) rows else rows.take(1)
                        for (rowItems in displayedRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                for (item in rowItems) {
                                    val color = when (item.status) {
                                        SquareStatus.EMPTY -> Color(0xFF1E293B)
                                        SquareStatus.NON_WORK -> Color(0xFF0F172A).copy(alpha = 0.5f)
                                        SquareStatus.OVERWORKED -> MaterialTheme.colorScheme.primary // cyan-400
                                        SquareStatus.OWES_TIME -> MaterialTheme.colorScheme.error // red-500
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(color)
                                            .clickable {
                                                Toast.makeText(
                                                    context,
                                                    "${item.date}: Debt: ${item.debtMinutes}m",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    )
                                }
                                // Fill out remaining empty space in the last row to maintain consistent cell size
                                if (rowItems.size < columnsCount) {
                                    val missing = columnsCount - rowItems.size
                                    repeat(missing) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                    // Streaks grid 2x2 style list
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Standing out Current Streak Card
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Current Streak",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Current Streak",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${stats.currentLightBlueStreak} Days",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // The other 4 in a 2x2 grid
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StreakItem(
                                label = "Streak Record",
                                value = "${stats.maxLightBlueStreak} Days",
                                modifier = Modifier.weight(1f)
                            )
                            StreakItem(
                                label = "Early Start Best",
                                value = "${stats.maxStartStreak} Days",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            StreakItem(
                                label = "Late End Best",
                                value = "${stats.maxEndStreak} Days",
                                modifier = Modifier.weight(1f)
                            )
                            StreakItem(
                                label = "Middle Break Best",
                                value = "${stats.maxMiddleStreak} Days",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- DYNAMIC TREND CHART ---
            WorkhorseChart(
                workDays = allDaysAsc,
                modifier = Modifier.fillMaxWidth()
            )

            // --- DATA MANAGEMENT AND OPERATIONS PANEL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "BACKUP OPERATIONS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { importLauncher.launch("*/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Import JSON File",
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import File", fontSize = 11.sp, color = Color.Black)
                        }

                        Button(
                            onClick = { showPasteImportDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = Color(0xFF00E5FF)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Create,
                                contentDescription = "Paste Backup",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste JSON", fontSize = 11.sp, color = Color(0xFF00E5FF))
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showManualEntryDialog) {
        ManualEntryDialog(
            initialDate = LocalDate.now().toString(),
            initialStartHour = (todayDay?.defaultStartMinutes ?: (10 * 60)) / 60,
            initialStartMin = (todayDay?.defaultStartMinutes ?: (10 * 60)) % 60,
            initialEndHour = (todayDay?.defaultEndMinutes ?: (18 * 60)) / 60,
            initialEndMin = (todayDay?.defaultEndMinutes ?: (18 * 60)) % 60,
            onDismiss = { showManualEntryDialog = false },
            onSave = { date, startH, startM, endH, endM ->
                viewModel.saveDailyTargets(date, startH, startM, endH, endM)
                Toast.makeText(context, "Baseline targets saved!", Toast.LENGTH_SHORT).show()
            },
            onDelete = { date ->
                viewModel.deleteDayRecord(date)
                Toast.makeText(context, "Record deleted!", Toast.LENGTH_SHORT).show()
            }
        )
    }


    if (showPasteImportDialog) {
        var pasteText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteImportDialog = false },
            title = { Text("Paste JSON Backup") },
            text = {
                Column {
                    Text("Paste your exported JSON backup text below:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pasteText,
                        onValueChange = { pasteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("[{...}]") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val success = viewModel.importBackup(pasteText)
                            if (success) {
                                Toast.makeText(context, "Backup imported successfully!", Toast.LENGTH_SHORT).show()
                                showPasteImportDialog = false
                            } else {
                                Toast.makeText(context, "Failed to parse backup JSON", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Import Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TodayMetricItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    val highlightColor = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = highlightColor
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun LegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StreakLine(
    label: String,
    value: Any
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AnalyticsKPICard(
    label: String,
    value: String,
    isGood: Boolean,
    diffText: String = "",
    modifier: Modifier = Modifier
) {
    val highlightColor = if (isGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = highlightColor
            )
            if (diffText.isNotEmpty()) {
                Text(
                    text = diffText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MiniLegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun StreakItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(vertical = 2.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

fun formatMinutesToShortOffset(minutes: Int): String {
    val sign = if (minutes > 0) "+" else if (minutes < 0) "-" else ""
    val absMins = Math.abs(minutes)
    val hrs = absMins / 60
    val mins = absMins % 60
    return when {
        hrs > 0 -> "$sign${hrs}h${mins}m"
        else -> "$sign${mins}m"
    }
}

