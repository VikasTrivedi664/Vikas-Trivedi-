package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CleaningType
import com.example.model.ColumnType
import com.example.ui.components.DataHealthScoreCard
import com.example.viewmodel.AnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCleaningScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val dataset by viewModel.dataset.collectAsStateWithLifecycle()
    var selectedColIndex by remember { mutableIntStateOf(0) }
    var customFillValue by remember { mutableStateOf("N/A") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("data_cleaning_screen")
    ) {
        // Health Banner
        DataHealthScoreCard(
            profile = dataset.profile,
            onAutoCleanClick = { viewModel.autoCleanDataset() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cleaning_health_banner")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Auto Clean & Reset Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.autoCleanDataset() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_auto_clean")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "One-Click Auto Clean",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { viewModel.resetDatasetToOriginal() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_reset_dataset")
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Reset", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tool 1: Duplicate Rows
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clean_tool_duplicates"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF43F5E).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Duplicate Records",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (dataset.profile.duplicateRowCount > 0)
                                "${dataset.profile.duplicateRowCount} duplicate rows found"
                            else "No duplicates detected (Clean)",
                            fontSize = 12.sp,
                            color = if (dataset.profile.duplicateRowCount > 0) Color(0xFFF43F5E) else Color(0xFF10B981)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.cleanRemoveDuplicates() },
                    enabled = dataset.profile.duplicateRowCount > 0,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF43F5E),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("btn_remove_duplicates")
                ) {
                    Text(text = "Deduplicate", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tool 2: Missing Values & Imputation Studio
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clean_tool_imputation"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Null & Missing Data Imputation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dataset.profile.missingCellsCount} empty cell(s) across dataset",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Select Target Column
                var colExpanded by remember { mutableStateOf(false) }
                val activeCol = dataset.columns.getOrNull(selectedColIndex) ?: dataset.columns.firstOrNull()

                ExposedDropdownMenuBox(
                    expanded = colExpanded,
                    onExpandedChange = { colExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${activeCol?.name ?: ""} (${activeCol?.nullCount ?: 0} nulls)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Target Column for Cleansing") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = colExpanded,
                        onDismissRequest = { colExpanded = false }
                    ) {
                        dataset.columns.forEachIndexed { idx, col ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${col.name} [${col.type.name}]")
                                        Text(
                                            "${col.nullCount} nulls",
                                            color = if (col.nullCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedColIndex = idx
                                    colExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Imputation Actions for Active Column
                Text(
                    text = "IMPUTATION STRATEGIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeCol?.type == ColumnType.NUMERIC) {
                        OutlinedButton(
                            onClick = { viewModel.cleanFillNullsMean(selectedColIndex) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fill Mean", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.cleanFillNullsMedian(selectedColIndex) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fill Median", fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.cleanFillNullsMode(selectedColIndex) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Fill Mode", fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.cleanDropNulls(selectedColIndex) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Drop Rows", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Fill row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customFillValue,
                        onValueChange = { customFillValue = it },
                        label = { Text("Custom Value") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.cleanFillNullsCustom(selectedColIndex, customFillValue) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tool 3: Outlier Trimming & Normalization
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clean_tool_outliers"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Outlier Detection & Text Hygiene",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Statistical IQR filter and string case standardization",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cleanTrimWhitespace() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Rule, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Trim Spaces", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.cleanRemoveOutliers(selectedColIndex) },
                        enabled = dataset.columns.getOrNull(selectedColIndex)?.type == ColumnType.NUMERIC,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Trim Outliers", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Text Case Normalization
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cleanConvertCase(selectedColIndex, CleaningType.TO_TITLECASE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Title Case", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.cleanConvertCase(selectedColIndex, CleaningType.TO_UPPERCASE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("UPPER", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { viewModel.cleanConvertCase(selectedColIndex, CleaningType.TO_LOWERCASE) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("lower", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cleaning Audit Trail
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cleaning_audit_trail_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cleansing Transformation Log (${dataset.cleaningHistory.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (dataset.cleaningHistory.isEmpty()) {
                    Text(
                        text = "No transformation steps recorded yet. Apply operations above to refine the data.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    dataset.cleaningHistory.reversed().forEachIndexed { i, step ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = step.type.label,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = step.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${step.rowsAffected} rows",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
