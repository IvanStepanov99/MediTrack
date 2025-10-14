package com.example.medtracker.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.medtracker.R
import com.example.medtracker.data.db.DbBuilder
import com.example.medtracker.data.db.entities.Drug
import com.example.medtracker.data.session.SessionManager
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast

class SearchActivity : AppCompatActivity() {
    private var allMeds: List<Drug> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        BottomBarHelper.setup(this, R.id.btnSearch)

        val etSearch = findViewById<MaterialAutoCompleteTextView>(R.id.etSearch)
        val suggestionAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            mutableListOf()
        )
        etSearch.setAdapter(suggestionAdapter)

        val db = DbBuilder.getDatabase(applicationContext)
        val drugDao = db.drugDao()

        val uidFromIntent = intent.getStringExtra("uid")
        val uid = uidFromIntent ?: SessionManager.getCurrentUid(applicationContext)
        if (uid == null) {
            Toast.makeText(this, "No user id. Please log in.", Toast.LENGTH_LONG).show()
            return
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                drugDao.observeByUser(uid).collect { list ->
                    allMeds = list
                    val q = etSearch.text?.toString().orEmpty()
                    updateSuggestions(q, allMeds, suggestionAdapter, etSearch)
                }
            }
        }

        var suggestJob: Job? = null
        etSearch.addTextChangedListener { text ->
            val q = text?.toString().orEmpty()
            suggestJob?.cancel()
            suggestJob = lifecycleScope.launch {
                delay(120) // debounce time
                updateSuggestions(q, allMeds, suggestionAdapter, etSearch)
            }
        }

        etSearch.setOnItemClickListener { _, _, position, _ ->
            val chosen = suggestionAdapter.getItem(position)
            if (chosen != null) {
                etSearch.setText(chosen, false)
                etSearch.setSelection(chosen.length)
            }
        }
    }

    private fun updateSuggestions(
        query: String,
        meds: List<Drug>,
        adapter: ArrayAdapter<String>,
        input: MaterialAutoCompleteTextView
    ) {
        val q = query.trim()
        if (q.length < 1) {
            adapter.clear()
            input.dismissDropDown()
            return
        }
        val lower = q.lowercase()
        val filtered = meds.asSequence()
            .filter { d ->
                d.name.contains(lower, ignoreCase = true) ||
                (d.brandName?.contains(lower, ignoreCase = true) == true)
            }
            .take(12)
            .toList()

        val labels = filtered.map { d ->
            val brand = d.brandName?.takeIf { it.isNotBlank() }
            val generic = d.name.takeIf { it.isNotBlank() }
            val name = when {
                brand != null && generic != null -> "$brand — $generic"
                brand != null -> brand
                generic != null -> generic
                else -> "(Unnamed)"
            }
            val form = d.form?.takeIf { it.isNotBlank() }
            val strength = buildString {
                d.strength?.let { amt ->
                    append(if (amt % 1.0 == 0.0) amt.toInt().toString() else amt.toString())
                }
                d.unit?.takeIf { it.isNotBlank() }?.let { u ->
                    if (isNotEmpty()) append("")
                    append(u)
                }
            }
            buildString {
                append(name)
                if (!form.isNullOrBlank() || strength.isNotBlank()) {
                    append("  ·  ")
                    append(listOfNotNull(form, strength.ifBlank { null }).joinToString(" "))
                }
            }
        }
        adapter.clear()
        adapter.addAll(labels)
        if (labels.isNotEmpty()) input.showDropDown() else input.dismissDropDown()
    }
}
