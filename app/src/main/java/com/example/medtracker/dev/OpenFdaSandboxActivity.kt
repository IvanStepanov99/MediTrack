package com.example.medtracker.dev

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import android.content.pm.ApplicationInfo
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.medtracker.data.db.DbBuilder
import com.example.medtracker.data.db.AppDatabase
import com.example.medtracker.data.remote.openfda.DrugSuggestion
import com.example.medtracker.data.remote.openfda.OpenFdaClient
import com.example.medtracker.data.remote.openfda.OpenFdaRepository
import com.example.medtracker.data.remote.openfda.toDrug
import com.example.medtracker.data.session.SessionManager
import com.example.medtracker.data.db.entities.DoseSchedule
import java.util.TimeZone
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OpenFdaSandboxActivity : ComponentActivity() {
    private val repo = OpenFdaRepository() // Retrofit wrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SandboxScreen(repo) }
    }
}

private fun hasInternet(cm: ConnectivityManager?): Boolean {
    return try {
        cm ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (se: SecurityException) {
        true
    }
}

suspend fun saveSandboxScheduleForDrug(
    db: AppDatabase,
    drugId: Long,
    timesText: String,
    doseAmount: Double,
    doseUnit: String,
    freqType: String,
    everyNDays: Int?,
    intervalHours: Int?
): Long = withContext(Dispatchers.IO) {
    val rawParts = timesText.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val formattedTimes: List<String>? = rawParts.mapNotNull { formatTo12Hour(it) }.ifEmpty { null }
    val minutesList: List<Int> = rawParts.mapNotNull { parseTimeToMinutes(it) }

    val schedule = DoseSchedule(
        doseScheduleId = 0L,
        drugId = drugId,
        prn = (freqType == "PRN"),
        doseAmount = doseAmount,
        doseUnit = doseUnit,
        freqType = freqType,
        intervalHours = intervalHours,
        everyNDays = everyNDays,
        byWeekDay = null,
        timesOfDay = formattedTimes,
        timeZone = TimeZone.getDefault().id
    )

    val timesPairs: List<Pair<Int, Double>> = minutesList.map { minutes -> minutes to doseAmount }

    val schedDao = db.doseScheduleDao()
    val newId = schedDao.saveOrReplaceForDrug(schedule, timesPairs)

    // update drug strength/unit to match schedule
    val drugDao = db.drugDao()
    val d = drugDao.getById(drugId)
    if (d != null) {
        drugDao.update(d.copy(strength = doseAmount, unit = doseUnit))
    }

    Log.d("OpenFdaSandbox", "helper saved schedule for drugId=$drugId, scheduleId=$newId, times=${timesPairs.size}")
    newId
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SandboxScreen(repo: OpenFdaRepository) {
    val ctx = LocalContext.current
    val cm = ctx.getSystemService(ConnectivityManager::class.java)
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<DrugSuggestion>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // State for scheduling dialog after insert
    var pendingDrugId by remember { mutableStateOf<Long?>(null) }
    var doseAmountText by remember { mutableStateOf("") }
    var doseUnitText by remember { mutableStateOf("mg") }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var freqType by remember { mutableStateOf("PRN") }
    var timesText by remember { mutableStateOf("") } // comma-separated HH:mm or 12h
    var everyNDaysText by remember { mutableStateOf("") }
    var intervalHoursText by remember { mutableStateOf("") }

    fun runSearch() {
        scope.launch {
            if (!hasInternet(cm)) {
                Toast.makeText(ctx, "No internet connection", Toast.LENGTH_LONG).show()
                return@launch
            }
            loading = true
            error = null
            try {
                results = repo.suggestByName(query, limit = 10)
                Toast.makeText(ctx, "Results: ${results.size}", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                error = t.message ?: "Unknown error"
                Toast.makeText(ctx, "Error: $error", Toast.LENGTH_LONG).show()
            } finally {
                loading = false
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("OpenFDA Sandbox") }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search (brand or generic)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { runSearch() }) { Text("Search") }
                if (loading) CircularProgressIndicator(modifier = Modifier.size(24.dp))


                Button(onClick = {
                    scope.launch {
                        if (!hasInternet(cm)) {
                            Toast.makeText(ctx, "No internet connection", Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        try {
                            val svc = OpenFdaClient.service
                            // Use a known-good wildcard query with quotes and OR between fields
                            val q1 = "generic_name:\"ibup*\" OR brand_name:\"ibup*\""
                            val res1 = svc.searchNdc(q1, 2)
                            val n1 = res1.results?.size ?: 0
                            if (n1 > 0) {
                                Toast.makeText(ctx, "Ping NDC #1 OK: $n1 rows", Toast.LENGTH_SHORT).show()
                            } else {

                                val q2 = "active_ingredients.name:\"IBUPROFEN\""
                                val res2 = svc.searchNdc(q2, 2)
                                Toast.makeText(ctx, "Ping NDC #2 ($q2): ${res2.results?.size ?: 0} rows", Toast.LENGTH_SHORT).show()
                            }
                        } catch (t: Throwable) {
                            if (t is HttpException) {
                                val body = try { t.response()?.errorBody()?.string() } catch (e: Exception) { null }
                                Toast.makeText(ctx, "Ping NDC FAIL: HTTP ${t.code()} ${t.message()} ${body ?: ""}", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(ctx, "Ping NDC FAIL: ${t.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }) { Text("Ping NDC") }
            }

            when {
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                !loading && results.isEmpty() && query.isNotBlank() ->
                    Text("No results")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { s ->
                    SuggestionRow(
                        s = s,
                        onInsert = {
                            val db = DbBuilder.getDatabase(ctx)
                            scope.launch {
                                try {
                                    val userDao = db.userProfileDao()
                                    val uidPref = SessionManager.getCurrentUid(ctx)
                                    val current = if (uidPref != null) userDao.get(uidPref) else userDao.getAny()
                                    var realCurrent = current
                                    if (realCurrent == null) {
                                        // Detect debug mode at runtime via applicationInfo
                                        val isDebuggable = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                                        if (isDebuggable) {
                                            // create a quick debug user so sandbox testing is frictionless
                                            val newUid = UUID.randomUUID().toString()
                                            val now = System.currentTimeMillis()
                                            val u = com.example.medtracker.data.db.entities.UserProfile(uid = newUid, firstName = "Debug", lastName = "User", dob = "01/01/1970", createdAt = now, lastSignAt = now)
                                            userDao.upsert(u)
                                            realCurrent = userDao.get(newUid)
                                            SessionManager.setCurrentUid(ctx, newUid)
                                            Toast.makeText(ctx, "Created debug user ${u.firstName}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(ctx, "No local user. Please log in first.", Toast.LENGTH_LONG).show()
                                            return@launch
                                        }
                                    }
                                    // ensure realCurrent is non-null and use its uid for insertion
                                    val realUid = realCurrent!!.uid
                                    val drugId = db.drugDao().insert(s.toDrug(uid = realUid))
                                     // Prepare schedule dialog defaults
                                    pendingDrugId = drugId
                                    doseAmountText = s.strengthAmount?.toString() ?: ""
                                    doseUnitText = s.strengthUnit ?: "mg"
                                    showScheduleDialog = true
                                    Toast.makeText(ctx, "Inserted drugId=$drugId for uid=$realUid", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Insert failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showScheduleDialog && pendingDrugId != null) {
        AlertDialog(
            onDismissRequest = { showScheduleDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val amt = doseAmountText.trim().toDoubleOrNull()
                    val unit = doseUnitText.trim().ifBlank { "mg" }
                    if (amt == null || amt <= 0) {
                        Toast.makeText(ctx, "Enter valid dose amount", Toast.LENGTH_LONG).show()
                        return@TextButton
                    }
                    val drugId = pendingDrugId!!
                    val db = DbBuilder.getDatabase(ctx)
                    scope.launch {
                        try {
                            val parsedEveryN = everyNDaysText.trim().toIntOrNull()
                            val parsedInterval = intervalHoursText.trim().toIntOrNull()
                            val schedId = saveSandboxScheduleForDrug(
                                db = db,
                                drugId = drugId,
                                timesText = timesText,
                                doseAmount = amt,
                                doseUnit = unit,
                                freqType = freqType,
                                everyNDays = parsedEveryN,
                                intervalHours = parsedInterval
                            )
                            Log.d("OpenFdaSandbox", "saved schedule id=$schedId for drugId=$drugId")
                            Toast.makeText(ctx, "Schedule saved", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Log.e("OpenFdaSandbox", "schedule save failed: ${e.message}")
                            Toast.makeText(ctx, "Schedule failed: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            showScheduleDialog = false
                            pendingDrugId = null
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showScheduleDialog = false
                    pendingDrugId = null
                }) { Text("Cancel") }
            },
            title = { Text("Set dose for this med") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = doseAmountText,
                        onValueChange = { doseAmountText = it },
                        label = { Text("Dose amount") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = doseUnitText,
                        onValueChange = { doseUnitText = it },
                        label = { Text("Dose unit (e.g., mg, ml)") },
                        singleLine = true
                    )
                    // Frequency selector (basic)
                    Column {
                        Text("Frequency")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val options = listOf("PRN", "DAILY", "EVERY_N_DAYS", "EVERY_N_HOURS", "WEEKLY")
                            options.forEach { opt ->
                                val selected = (freqType == opt)
                                Button(onClick = { freqType = opt }) {
                                    Text(opt + if (selected) " ✓" else "")
                                }
                            }
                        }
                    }

                    // Times input (comma-separated HH:mm)
                    OutlinedTextField(
                        value = timesText,
                        onValueChange = { timesText = it },
                        label = { Text("Times (comma-separated HH:mm), e.g. 08:00,20:00") },
                        singleLine = true
                    )

                    // Every N days / Interval hours inputs (optional)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = everyNDaysText,
                            onValueChange = { everyNDaysText = it },
                            label = { Text("Every N days (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = intervalHoursText,
                            onValueChange = { intervalHoursText = it },
                            label = { Text("Interval hours (optional)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("Note: This saves a PRN schedule (no times)." , style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }
}

@Composable
private fun SuggestionRow(s: DrugSuggestion, onInsert: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInsert() }
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(s.genericName ?: s.brandName ?: "Unknown")
            val sub = listOfNotNull(
                s.brandName?.let { "brand: $it" },
                listOfNotNull(s.strengthAmount?.toString(), s.strengthUnit)
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() },
                s.form
            ).joinToString(" • ")
            if (sub.isNotBlank()) Text(sub, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// Helper to parse time strings (accepts 24h or 12h with/without AM/PM) to minutes since midnight
private fun parseTimeToMinutes(time: String): Int? {
    val trimmed = time.trim()
    if (trimmed.isEmpty()) return null
    val patterns = listOf("H:mm", "HH:mm", "h:mma", "hh:mma", "h:mm a", "hh:mm a")
    for (pat in patterns) {
        try {
            val sdf = SimpleDateFormat(pat, Locale.US)
            sdf.isLenient = false
            // Try parsing as-is
            val d = try {
                sdf.parse(trimmed)
            } catch (_: Exception) {
                // Try normalizing am/pm spacing/casing and parse again
                val norm = trimmed.replace(Regex("(?i)(am|pm)"), { m -> " ${m.value.uppercase(Locale.US)}" }).trim()
                try { sdf.parse(norm) } catch (_: Exception) { null }
            } ?: continue
            val cal = java.util.Calendar.getInstance().apply { this.time = d }
            return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        } catch (_: Exception) {

        }
    }
    return null
}

private fun formatTo12Hour(time: String): String? {
    val minutes = parseTimeToMinutes(time) ?: return null
    val cal = java.util.Calendar.getInstance().apply {
        timeInMillis = 0
        set(java.util.Calendar.HOUR_OF_DAY, (minutes / 60) % 24)
        set(java.util.Calendar.MINUTE, minutes % 60)
    }
    val out = SimpleDateFormat("h:mma", Locale.US)
    return out.format(cal.time).lowercase(Locale.US)
}
