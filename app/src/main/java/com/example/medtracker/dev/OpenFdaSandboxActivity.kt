package com.example.medtracker.dev

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
import kotlinx.coroutines.launch

class OpenFdaSandboxActivity : ComponentActivity() {
    private val repo = OpenFdaRepository() // Retrofit wrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SandboxScreen(repo) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SandboxScreen(repo: OpenFdaRepository) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("apix") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<DrugSuggestion>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun runSearch() {
        scope.launch {
            loading = true
            error = null
            try {
                // space-OR is robust; Retrofit encodes spaces automatically
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

    Scaffold(topBar = { TopAppBar(title = { Text("OpenFDA Sandbox (debug)") }) }) { padding ->
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
                        try {
                            val svc = OpenFdaClient.service
                            val res = svc.searchNdc("generic_name:ibuprofen", 2)
                            val n = res.results?.size ?: 0
                            Toast.makeText(ctx, "Ping NDC OK: $n rows", Toast.LENGTH_SHORT).show()
                        } catch (t: Throwable) {
                            Toast.makeText(ctx, "Ping NDC FAIL: ${t.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Ping NDC") }
            }

            when {
                error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
                !loading && results.isEmpty() && query.isNotBlank() ->
                    Text("No results. Try: apix, ibup, metfo")
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
                                    val testUid = "local"
                                    val user = userDao.get(testUid)
                                    if (user == null) {
                                        userDao.upsert(com.example.medtracker.data.db.entities.UserProfile(uid = testUid, firstName = "Test", lastName = "User"))
                                    }
                                    val id = db.drugDao().insert(s.toDrug(uid = testUid))
                                    Toast.makeText(ctx, "Inserted drugId=$id", Toast.LENGTH_SHORT).show()
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
