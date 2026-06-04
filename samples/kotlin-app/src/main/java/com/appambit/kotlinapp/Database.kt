package com.appambit.kotlinapp

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
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

@Composable
fun Database() {
    var sql by remember { mutableStateOf("SELECT * FROM tasks LIMIT 10") }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var rows by remember { mutableStateOf<List<List<Any?>>>(emptyList()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    fun reset() {
        columns = emptyList(); rows = emptyList()
        statusMessage = null; errorMessage = null
    }

    fun ok(msg: String) { isLoading = false; statusMessage = msg; errorMessage = null }
    fun err(msg: String) { isLoading = false; errorMessage = msg; statusMessage = null }

    fun demoExecute() {
        reset(); isLoading = true
        AppAmbitDb.execute(sql)
            .then { result ->
                if (result.hasError()) err(result.error)
                else {
                    columns = result.columns
                    rows = result.rows
                    ok("execute(sql) — rows_read=${result.rowsRead}  rows_written=${result.rowsWritten}")
                }
            }
            .onError { e -> err(e.message ?: "Unknown error") }
    }

    fun demoExecuteParams() {
        reset(); isLoading = true
        AppAmbitDb.execute("SELECT * FROM tasks WHERE is_completed = ? LIMIT ?", 0, 10)
            .then { result ->
                if (result.hasError()) err(result.error)
                else {
                    columns = result.columns
                    rows = result.rows
                    ok("execute(sql, 0, 10) — tareas pendientes, rows_read=${result.rowsRead}")
                }
            }
            .onError { e -> err(e.message ?: "Error") }
    }

    fun demoBatch() {
        reset(); isLoading = true
        AppAmbitDb.batch(
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Comprar café", 0, "low", "2026-06-10"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Revisar PR", 0, "high", "2026-06-05"),
            DbStatement.of("SELECT COUNT(*) AS total FROM tasks")
        )
            .then { results ->
                val written = results.sumOf { it.rowsWritten }
                columns = listOf("statement", "rows_written")
                rows = results.mapIndexed { i, r -> listOf(i + 1, r.rowsWritten) }
                ok("batch() — $written row(s) written across ${results.size} statements (no transaction)")
            }
            .onError { e -> err(e.message ?: "Batch error") }
    }

    fun demoBatchInTransaction() {
        reset(); isLoading = true
        AppAmbitDb.batchInTransaction(
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Reunión de equipo", 0, "high", "2026-06-06"),
            DbStatement.of("INSERT INTO tasks (title, is_completed, priority, due_date) VALUES (?, ?, ?, ?)", "Preparar agenda", 0, "medium", "2026-06-06")
        )
            .then { results ->
                val written = results.sumOf { it.rowsWritten }
                ok("batchInTransaction() — $written row(s) written, rolled back on any failure")
            }
            .onError { e -> err(e.message ?: "Transaction error") }
    }

    fun demoFluentSelect() {
        reset(); isLoading = true
        AppAmbitDb.from("tasks")
            .select("id", "title", "priority", "due_date")
            .where("is_completed", "=", 0)
            .orderByDesc("due_date")
            .limit(5)
            .get()
            .then { maps ->
                if (maps.isEmpty()) ok("No hay tareas pendientes")
                else {
                    columns = maps.first().keys.toList()
                    rows = maps.map { it.values.toList() }
                    ok("tareas pendientes por vencer — ${maps.size} row(s)")
                }
            }
            .onError { e -> err(e.message ?: "Error") }
    }

    fun demoWhereEquality() {
        reset(); isLoading = true
        AppAmbitDb.from("tasks")
            .where("is_completed", 0)
            .get()
            .then { maps ->
                if (maps.isEmpty()) ok("No hay tareas pendientes")
                else {
                    columns = maps.first().keys.toList()
                    rows = maps.map { it.values.toList() }
                    ok("where(is_completed, 0) — ${maps.size} tarea(s) pendiente(s)")
                }
            }
            .onError { e -> err(e.message ?: "Error") }
    }

    fun demoWhereIn() {
        reset(); isLoading = true
        AppAmbitDb.from("tasks")
            .whereIn("priority", listOf("high", "medium"))
            .orderBy("due_date")
            .get()
            .then { maps ->
                if (maps.isEmpty()) ok("No hay tareas high/medium")
                else {
                    columns = maps.first().keys.toList()
                    rows = maps.map { it.values.toList() }
                    ok("whereIn(priority, [high, medium]) — ${maps.size} row(s)")
                }
            }
            .onError { e -> err(e.message ?: "Error") }
    }

    fun demoOffset() {
        reset(); isLoading = true
        AppAmbitDb.from("tasks")
            .orderBy("due_date")
            .limit(5)
            .offset(0)
            .get()
            .then { maps ->
                if (maps.isEmpty()) ok("No hay tareas")
                else {
                    columns = maps.first().keys.toList()
                    rows = maps.map { it.values.toList() }
                    ok("limit(5).offset(0) — página 1, ${maps.size} row(s)")
                }
            }
            .onError { e -> err(e.message ?: "Error") }
    }

    fun demoFirst() {
        reset(); isLoading = true
        AppAmbitDb.from("tasks")
            .where("is_completed", "=", 0)
            .orderBy("due_date")
            .first()
            .then { item ->
                if (item == null) ok("first() — no hay tareas pendientes")
                else {
                    columns = item.keys.toList()
                    rows = listOf(item.values.toList())
                    ok("first() — próxima tarea a vencer")
                }
            }
            .onError { e -> err(e.message ?: "Error") }
    }

    fun demoCount() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks").where("is_completed", 0).count()
        future.then { count ->
            columns = listOf("tareas_pendientes")
            rows = listOf(listOf(count))
            ok("count() — $count tarea(s) pendiente(s)")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoInsert() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks")
            .insert(mapOf(
                "title" to "Nueva tarea",
                "is_completed" to 0,
                "priority" to "medium",
                "due_date" to "2026-06-10"
            ))
        future.then { result ->
            if (result.hasError()) err(result.error)
            else ok("insert() — tarea creada, rows_written=${result.rowsWritten}")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoUpdate() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks")
            .where("title", "Nueva tarea")
            .update(mapOf("is_completed" to 1))
        future.then { result ->
            if (result.hasError()) err(result.error)
            else ok("update() — tarea marcada como completada, rows_written=${result.rowsWritten}")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    fun demoDelete() {
        reset(); isLoading = true
        val future = AppAmbitDb.from("tasks")
            .where("is_completed", 1)
            .delete()
        future.then { result ->
            if (result.hasError()) err(result.error)
            else ok("delete() — tareas completadas eliminadas, rows_written=${result.rowsWritten}")
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
            columns = listOf("id", "title", "isCompleted", "priority", "dueDate")
            rows = tasks.map { listOf(it.id, it.title, it.isCompleted, it.priority, it.dueDate) }
            ok("from(tasks, TaskModel::class.java) — ${tasks.size} typed row(s)")
        }
        future.onError { e -> err(e.message ?: "Error") }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Database", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            DbSection("Raw SQL")
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
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DbButton("execute(sql)", Modifier.weight(1f), !isLoading) { demoExecute() }
                DbButton("execute(sql, params)", Modifier.weight(1f), !isLoading) { demoExecuteParams() }
            }

            Spacer(Modifier.height(16.dp))

            DbSection("Batch")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DbButton("batch()", Modifier.weight(1f), !isLoading) { demoBatch() }
                DbButton("batchInTransaction()", Modifier.weight(1f), !isLoading) { demoBatchInTransaction() }
            }

            Spacer(Modifier.height(16.dp))

            DbSection("Fluent Builder — SELECT")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip({ demoFluentSelect() }, { Text("select+where+orderByDesc+limit") }, enabled = !isLoading)
                AssistChip({ demoWhereEquality() }, { Text("where(col, val)") }, enabled = !isLoading)
                AssistChip({ demoWhereIn() }, { Text("whereIn()") }, enabled = !isLoading)
                AssistChip({ demoOffset() }, { Text("limit+offset") }, enabled = !isLoading)
                AssistChip({ demoFirst() }, { Text("first()") }, enabled = !isLoading)
                AssistChip({ demoCount() }, { Text("count()") }, enabled = !isLoading)
            }

            Spacer(Modifier.height(16.dp))

            DbSection("Fluent Builder — WRITE")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip({ demoInsert() }, { Text("insert()") }, enabled = !isLoading)
                AssistChip({ demoUpdate() }, { Text("update()") }, enabled = !isLoading)
                AssistChip({ demoDelete() }, { Text("delete()") }, enabled = !isLoading)
            }

            Spacer(Modifier.height(16.dp))

            DbSection("Typed Model Mapping")
            DbButton(
                "from(\"tasks\", TaskModel::class.java)",
                Modifier.fillMaxWidth(),
                !isLoading
            ) { demoTypedModel() }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(4.dp))
            }
            statusMessage?.let {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
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
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
            if (columns.isNotEmpty()) ResultTable(columns, rows)
        }
    }
}

@Composable
private fun DbSection(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun DbButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = modifier, enabled = enabled) {
        Text(label, maxLines = 2, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ResultTable(columns: List<String>, rows: List<List<Any?>>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                columns.forEach { col ->
                    Text(
                        col,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .width(120.dp)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        maxLines = 1
                    )
                }
            }
            HorizontalDivider()
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(rows) { row ->
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        row.forEach { cell ->
                            Text(
                                cell?.toString() ?: "null",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    color = if (cell == null) MaterialTheme.colorScheme.outline else Color.Unspecified
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
