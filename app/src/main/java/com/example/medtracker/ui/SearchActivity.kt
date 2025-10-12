package com.example.medtracker.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.medtracker.R
import com.example.medtracker.data.remote.openfda.OpenFdaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.widget.addTextChangedListener

class SearchActivity : AppCompatActivity(){
    private val repo = OpenFdaRepository()

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

        var suggestJob: Job? = null
        etSearch.addTextChangedListener { text ->
            val q = text?.toString().orEmpty()
            suggestJob?.cancel()
            if (q.length < 2) {
                suggestionAdapter.clear()
                return@addTextChangedListener
            }
            suggestJob = lifecycleScope.launch {
                // debounce
                delay(300)
                val results = repo.suggestByName(q, limit = 10)
                val labels = results.map { s ->
                    val brand = s.brandName?.takeIf { it.isNotBlank() }
                    val generic = s.genericName?.takeIf { it.isNotBlank() }
                    val name = when {
                        !brand.isNullOrBlank() && !generic.isNullOrBlank() -> "$brand — $generic"
                        !brand.isNullOrBlank() -> brand
                        !generic.isNullOrBlank() -> generic
                        else -> "(Unnamed)"
                    }
                    val form = s.form?.takeIf { it.isNotBlank() }
                    val strength = listOfNotNull(
                        s.strengthAmount?.let { amt -> if (amt % 1.0 == 0.0) amt.toInt().toString() else amt.toString() },
                        s.strengthUnit?.takeIf { it.isNotBlank() }
                    ).joinToString("")
                    buildString {
                        append(name)
                        if (!form.isNullOrBlank() || strength.isNotBlank()) {
                            append("  ·  ")
                            append(listOfNotNull(form, strength.ifBlank { null }).joinToString(" "))
                        }
                    }
                }
                suggestionAdapter.clear()
                suggestionAdapter.addAll(labels)
                if (labels.isNotEmpty()) etSearch.showDropDown()
            }
        }

        etSearch.setOnItemClickListener { parent, view, position, id ->
            val chosen = suggestionAdapter.getItem(position)
            if (chosen != null) {
                etSearch.setText(chosen, false)
                etSearch.setSelection(chosen.length)
                // Optionally: kick off a full search or navigate
                // e.g., startActivity(Intent(this, DrugDetailActivity::class.java).putExtra("label", chosen))
            }
        }
    }
}
