package com.example.medtracker.dev

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.Toast
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
import com.example.medtracker.data.remote.openfda.DrugSuggestion
import com.example.medtracker.data.remote.openfda.OpenFdaClient
import com.example.medtracker.data.remote.openfda.OpenFdaRepository
import com.example.medtracker.data.remote.openfda.toDrug
import com.example.medtracker.data.session.SessionManager
import kotlinx.coroutines.launch
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
                Toast.makeText(ctx, "Results: ${'$'}{results.size}", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                error = t.message ?: "Unknown error"
                Toast.makeText(ctx, "Error: ${'$'}error", Toast.LENGTH_LONG).show()
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
                                Toast.makeText(ctx, "Ping NDC #1 OK: ${'$'}n1 rows", Toast.LENGTH_SHORT).show()
                            } else {

                                val q2 = "active_ingredients.name:\"IBUPROFEN\""
                                val res2 = svc.searchNdc(q2, 2)
                                val n2 = res2.results?.size ?: 0
                                Toast.makeText(ctx, "Ping NDC #2 (${ '$' }q2): ${'$'}n2 rows", Toast.LENGTH_SHORT).show()
                            }
                        } catch (t: Throwable) {
                            val msg = if (t is HttpException) {
                                val body = try { t.response()?.errorBody()?.string() } catch (e: Exception) { null }
                                "HTTP ${'$'}{t.code()} ${'$'}{t.message()} ${'$'}body"
                            } else t.message ?: t.toString()
                            Toast.makeText(ctx, "Ping NDC FAIL: ${'$'}msg", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Ping NDC") }
            }

            when {
                error != null -> Text("Error: ${'$'}error", color = MaterialTheme.colorScheme.error)
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
                                    if (current == null) {
                                        Toast.makeText(ctx, "No local user. Please log in first.", Toast.LENGTH_LONG).show()
                                        return@launch
                                    }
                                    val id = db.drugDao().insert(s.toDrug(uid = current.uid))
                                    Toast.makeText(ctx, "Inserted for uid=${'$'}{current.uid} (drugId=${'$'}id)", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Insert failed: ${'$'}{e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
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
