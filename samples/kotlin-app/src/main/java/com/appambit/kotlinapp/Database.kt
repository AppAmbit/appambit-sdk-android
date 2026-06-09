package com.appambit.kotlinapp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.appambit.sdk.AppAmbitDb
import com.appambit.sdk.annotations.DbColumn
import com.appambit.sdk.models.db.DbStatement

class TaskModel {
    var id: Int = 0
    var title: String = ""

    @field:DbColumn("is_completed")
    var isCompleted: Int = 0

    var priority: String = ""

    @field:DbColumn("due_date")
    var dueDate: String = ""
}

private data class DemoItem(val label: String, val action: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Database() {
    var sql by remember { mutableStateOf("SELECT * FROM tasks LIMIT 10") }
    var tableColumns by remember { mutableStateOf<List<String>>(emptyList()) }
    var tableRows by remember { mutableStateOf<List<List<Any?>>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }

    fun reset() {
        tableColumns = emptyList(); tableRows = emptyList()
        statusMessage = null; errorMessage = null
    }
    fun ok(msg: String) { isLoading = false; statusMessage = msg; errorMessage = null }
    fun err(msg: String) { isLoading = false; errorMessage = msg; statusMessage = null }

    fun showRows(columns: List<String>, rows: List<List<Any?>>) {
        tableColumns = columns; tableRows = rows
    }

    fun showMaps(maps: List<Map<String, Any?>>) {
        if (maps.isEmpty()) { tableColumns = emptyList(); tableRows = emptyList(); return }
        val cols = maps.first().keys.toList()
        tableColumns = cols
        tableRows = maps.map { row -> cols.map { col -> row[col] } }
    }

    fun showSingle(col: String, value: Any?) {
        tableColumns = listOf(col); tableRows = listOf(listOf(value))
    }

    fun demoExecute() {
        reset(); isLoading = true
        val future = AppAmbitDb.execute(sql)
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showRows(result.columns, result.rows)
                ok("execute(sql) — rows_read=${result.rowsRead}  rows_written=${result.rowsWritten}")
            }
        }
        future.onError { e -> err(e.message ?: "Unknown error") }
    }

    fun demoExecuteParams() {
        reset(); isLoading = true
        val future = AppAmbitDb.execute("SELECT * FROM tasks WHERE is_completed = ? LIMIT ?", 0, 10)
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showRows(result.columns, result.rows)
                ok("execute(sql, 0, 10) — pending tasks, rows_read=${result.rowsRead}")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoCreateTable() {
        reset(); isLoading = true
        val future = AppAmbitDb.execute(
            "CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, is_completed INTEGER DEFAULT 0, priority TEXT, due_date TEXT)"
        )
        future.then { result ->
            if (result.hasError()) err(result.error)
            else ok("CREATE TABLE OK — rows_read=${result.rowsRead}  rows_written=${result.rowsWritten}")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoDropTable() {
        reset(); isLoading = true
        val future = AppAmbitDb.execute("DROP TABLE IF EXISTS tasks")
        future.then { result ->
            if (result.hasError()) err(result.error)
            else ok("DROP TABLE OK — rows_read=${result.rowsRead}  rows_written=${result.rowsWritten}")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoPresetTables() {
        reset(); isLoading = true
        sql = "SELECT name FROM sqlite_master WHERE type = 'table'"
        val future = AppAmbitDb.execute(sql)
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showRows(result.columns, result.rows)
                ok("sqlite_master tables — ${result.rowsRead} row(s)")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoPresetHighPriority() {
        reset(); isLoading = true
        sql = "SELECT * FROM tasks WHERE priority = 'high'"
        val future = AppAmbitDb.execute(sql)
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showRows(result.columns, result.rows)
                ok("tasks WHERE priority='high' — ${result.rowsRead} row(s)")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoBatch() {
        reset(); isLoading = true
        val future = AppAmbitDb.batch(
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Buy coffee", 0, "low", "2026-06-10"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Review PR", 0, "high", "2026-06-05"),
            DbStatement.of("SELECT COUNT(*) AS total FROM tasks")
        )
        future.then { results ->
            val written = results.sumOf { it.rowsWritten }
            tableColumns = listOf("statement", "rows_written", "rows_read")
            tableRows = results.mapIndexed { i, r -> listOf(i + 1, r.rowsWritten, r.rowsRead) }
            ok("batch() — $written row(s) written across ${results.size} statements (no transaction)")
        }
        future.onError { e -> err(e.message ?: "Batch error") }
    }

    fun demoBatchInTransaction() {
        reset(); isLoading = true
        val future = AppAmbitDb.batchInTransaction(
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Team meeting", 0, "high", "2026-06-06"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Prepare agenda", 0, "medium", "2026-06-06")
        )
        future.then { results ->
            val written = results.sumOf { it.rowsWritten }
            tableColumns = listOf("statement", "rows_written")
            tableRows = results.mapIndexed { i, r -> listOf(i + 1, r.rowsWritten) }
            ok("batchInTransaction() — $written row(s) written, rolled back on any failure")
        }
        future.onError { e -> err(e.message ?: "Transaction error") }
    }

    fun demoFluentSelect() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks")
            .select("id", "title", "priority", "due_date")
            .where("is_completed", "=", 0)
            .orderByDesc("due_date")
            .limit(5)
            .get()
        future.then { maps ->
            if (maps.isEmpty()) ok("No pending tasks")
            else {
                showMaps(maps)
                ok("from().select().where().orderByDesc().limit(5) — ${maps.size} row(s)")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoWhereEquality() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").where("is_completed", 0).get()
        future.then { maps ->
            if (maps.isEmpty()) ok("No pending tasks")
            else {
                showMaps(maps)
                ok("where(is_completed, 0) — ${maps.size} pending task(s)")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoWhereIn() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks")
            .whereIn("priority", listOf("high", "medium"))
            .orderBy("due_date")
            .get()
        future.then { maps ->
            if (maps.isEmpty()) ok("No high/medium tasks")
            else {
                showMaps(maps)
                ok("whereIn(priority, [high, medium]) — ${maps.size} row(s)")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoOffset() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").orderBy("due_date").limit(5).offset(0).get()
        future.then { maps ->
            if (maps.isEmpty()) ok("No tasks")
            else {
                showMaps(maps)
                ok("limit(5).offset(0) — page 1, ${maps.size} row(s)")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoFirst() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").where("is_completed", "=", 0).orderBy("due_date").first()
        future.then { item ->
            if (item == null) ok("first() — No pending tasks")
            else {
                showMaps(listOf(item))
                ok("first() — next task due")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoCount() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").where("is_completed", 0).count()
        future.then { count ->
            showSingle("pending_tasks", count)
            ok("count() — $count pending task(s)")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoInsert() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").insert(mapOf(
            "title" to "New task", "is_completed" to 0, "priority" to "medium", "due_date" to "2026-06-10"
        ))
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showSingle("rows_written", result.rowsWritten)
                ok("insert() — task created, rows_written=${result.rowsWritten}")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoInsertHigh() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").insert(mapOf(
            "title" to "Fix critical bug", "is_completed" to 0, "priority" to "high", "due_date" to "2026-06-05"
        ))
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showSingle("rows_written", result.rowsWritten)
                ok("insert() high priority — task created, rows_written=${result.rowsWritten}")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoInsertRawSQL() {
        reset(); isLoading = true
        val future = AppAmbitDb.execute(
            "INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)",
            "Raw SQL insert", 0, "medium", "2026-06-12"
        )
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showSingle("rows_written", result.rowsWritten)
                ok("execute() INSERT OK — rows_written=${result.rowsWritten}")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoInsertMany() {
        reset(); isLoading = true
        val future = AppAmbitDb.batchInTransaction(
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Write unit tests", 0, "high", "2026-06-07"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Update documentation", 0, "low", "2026-06-15"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Code review", 0, "medium", "2026-06-08"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Deploy to staging", 0, "high", "2026-06-09"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Monitor metrics", 0, "low", "2026-06-20")
        )
        future.then { results ->
            val written = results.sumOf { it.rowsWritten }
            showSingle("rows_inserted", written)
            ok("insert many — $written rows inserted via batch")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoUpdate() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").where("title", "New task").update(mapOf("is_completed" to 1))
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showSingle("rows_written", result.rowsWritten)
                ok("update() — task marked as completed, rows_written=${result.rowsWritten}")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoDelete() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").where("is_completed", 1).delete()
        future.then { result ->
            if (result.hasError()) err(result.error)
            else {
                showSingle("rows_written", result.rowsWritten)
                ok("delete() — completed tasks deleted, rows_written=${result.rowsWritten}")
            }
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoTypedModel() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks", TaskModel::class.java)
            .select("id", "title", "is_completed", "priority", "due_date")
            .limit(5)
            .get()
        future.then { tasks ->
            tableColumns = listOf("id", "title", "isCompleted", "priority", "dueDate")
            tableRows = tasks.map { t -> listOf(t.id, t.title, t.isCompleted, t.priority, t.dueDate) }
            ok("from(tasks, TaskModel::class.java) — ${tasks.size} typed row(s)")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    val demos = listOf(
        DemoItem("Raw SQL → execute(sql)")                          { demoExecute() },
        DemoItem("Raw SQL → execute(sql, params)")                  { demoExecuteParams() },
        DemoItem("Schema → CREATE TABLE tasks")                     { demoCreateTable() },
        DemoItem("Schema → DROP TABLE tasks")                       { demoDropTable() },
        DemoItem("Batch → batch()")                                 { demoBatch() },
        DemoItem("Batch → batchInTransaction()")                    { demoBatchInTransaction() },
        DemoItem("Fluent SELECT → select+where+orderByDesc+limit")  { demoFluentSelect() },
        DemoItem("Fluent SELECT → where(col, val)")                 { demoWhereEquality() },
        DemoItem("Fluent SELECT → whereIn()")                       { demoWhereIn() },
        DemoItem("Fluent SELECT → limit+offset")                    { demoOffset() },
        DemoItem("Fluent SELECT → first()")                         { demoFirst() },
        DemoItem("Fluent SELECT → count()")                         { demoCount() },
        DemoItem("Fluent WRITE → insert()")                         { demoInsert() },
        DemoItem("Fluent WRITE → insert() high priority")           { demoInsertHigh() },
        DemoItem("Fluent WRITE → insert() raw SQL")                 { demoInsertRawSQL() },
        DemoItem("Fluent WRITE → insert many (batch)")              { demoInsertMany() },
        DemoItem("Fluent WRITE → update()")                         { demoUpdate() },
        DemoItem("Fluent WRITE → delete()")                         { demoDelete() },
        DemoItem("Typed Model → from(tasks, TaskModel::class.java)") { demoTypedModel() },
        DemoItem("Preset → List tables")                            { demoPresetTables() },
        DemoItem("Preset → SELECT * WHERE priority='high'")         { demoPresetHighPriority() },
    )

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        OutlinedTextField(
            value = sql,
            onValueChange = { sql = it },
            label = { Text("SQL") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            maxLines = 5
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = demos[selectedIndex].label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall,
                    singleLine = true
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    demos.forEachIndexed { i, demo ->
                        DropdownMenuItem(
                            text = { Text(demo.label, style = MaterialTheme.typography.bodySmall) },
                            onClick = { selectedIndex = i; expanded = false }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    reset()
                    isLoading = true
                    demos[selectedIndex].action()
                },
                enabled = !isLoading
            ) {
                Text("▶  Run", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(8.dp))

        statusMessage?.let {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.height(4.dp))
        }

        errorMessage?.let {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(4.dp))
        }

        if (tableColumns.isNotEmpty()) {
            ResultTable(columns = tableColumns, rows = tableRows)
        }
    }
}

@Composable
private fun ResultTable(columns: List<String>, rows: List<List<Any?>>) {
    val colWidth = 140.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Header
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(vertical = 2.dp)
                ) {
                    columns.forEach { col ->
                        Text(
                            text = col,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .width(colWidth)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                thickness = 1.dp
            )

            if (rows.isEmpty()) {
                Text(
                    text = "(no rows)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                )
            } else {
                val scrollState = rememberScrollState()
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    itemsIndexed(rows) { index, row ->
                        Box(modifier = Modifier.horizontalScroll(scrollState)) {
                            Row(
                                modifier = Modifier.background(
                                    if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                            ) {
                                columns.forEachIndexed { colIndex, _ ->
                                    val cell = row.getOrNull(colIndex)
                                    Text(
                                        text = cell?.toString() ?: "null",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = if (cell == null) MaterialTheme.colorScheme.outline
                                               else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .width(colWidth)
                                            .padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                        if (index < rows.size - 1) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "${rows.size} row${if (rows.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
