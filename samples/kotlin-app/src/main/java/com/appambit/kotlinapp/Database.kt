package com.appambit.kotlinapp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.appambit.sdk.AppAmbitDb
import com.appambit.sdk.models.db.DbStatement

@Composable
fun Database() {
    var sql by remember { mutableStateOf("SELECT * FROM users LIMIT 10") }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var rows by remember { mutableStateOf<List<List<Any?>>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun resetOutput() {
        columns = emptyList()
        rows = emptyList()
        statusMessage = null
        errorMessage = null
    }

    fun runSql(query: String) {
        resetOutput()
        isLoading = true
        AppAmbitDb.execute(query)
            .then { result ->
                isLoading = false
                if (result.hasError()) {
                    errorMessage = result.error
                } else {
                    columns = result.columns
                    rows = result.rows
                    statusMessage = "rows_read=${result.rowsRead}  rows_written=${result.rowsWritten}"
                }
            }
            .onError { error ->
                isLoading = false
                errorMessage = error.message ?: "Unknown error"
            }
    }

    fun runBatchDemo() {
        resetOutput()
        isLoading = true
        AppAmbitDb.batchInTransaction(
            DbStatement.of("INSERT INTO demo_log (event) VALUES (?)", "batch_start"),
            DbStatement.of("INSERT INTO demo_log (event) VALUES (?)", "batch_end")
        ).then { results ->
            isLoading = false
            val written = results.sumOf { it.rowsWritten }
            statusMessage = "Batch complete — $written row(s) written across ${results.size} statements"
        }.onError { error ->
            isLoading = false
            errorMessage = error.message ?: "Batch failed"
        }
    }

    fun runFluentDemo(table: String) {
        resetOutput()
        isLoading = true
        AppAmbitDb.from(table)
            .limit(10)
            .get()
            .then { maps ->
                isLoading = false
                if (maps.isEmpty()) {
                    statusMessage = "No rows found in \"$table\""
                } else {
                    columns = maps.first().keys.toList()
                    rows = maps.map { it.values.toList() }
                    statusMessage = "${maps.size} row(s) via fluent builder"
                }
            }
            .onError { error ->
                isLoading = false
                errorMessage = error.message ?: "Query failed"
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Database", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(12.dp))

        // SQL editor
        OutlinedTextField(
            value = sql,
            onValueChange = { sql = it },
            label = { Text("SQL") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { runSql(sql) },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) { Text("Execute") }

            OutlinedButton(
                onClick = { runBatchDemo() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) { Text("Batch demo") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset queries
        Text("Preset queries", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetChip("List tables") {
                sql = "SELECT name FROM sqlite_master WHERE type = 'table'"
                runSql(sql)
            }
            PresetChip("users (fluent)") { runFluentDemo("users") }
            PresetChip("products (fluent)") { runFluentDemo("products") }
            PresetChip("SELECT 1") { runSql("SELECT 1 AS result") }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status / error banner
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        statusMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    msg,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        errorMessage?.let { msg ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Results table
        if (columns.isNotEmpty()) {
            ResultTable(columns = columns, rows = rows)
        }
    }
}

@Composable
private fun PresetChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}

@Composable
private fun ResultTable(columns: List<String>, rows: List<List<Any?>>) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
    ) {
        Column {
            // Header row
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                columns.forEach { col ->
                    Text(
                        text = col,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(rows) { row ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        row.forEachIndexed { idx, cell ->
                            Text(
                                text = cell?.toString() ?: "null",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = if (cell == null)
                                        MaterialTheme.colorScheme.outline
                                    else
                                        Color.Unspecified
                                ),
                                modifier = Modifier
                                    .width(120.dp)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                maxLines = 2
                            )
                        }
                    }
                    if (rows.indexOf(row) < rows.size - 1) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
