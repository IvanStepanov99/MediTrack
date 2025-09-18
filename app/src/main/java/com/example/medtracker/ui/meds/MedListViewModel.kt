package com.example.medtracker.ui.meds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medtracker.data.repo.MedicationRepository
import com.example.medtracker.domain.model.Medication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MedListViewModel(
    repo: MedicationRepository,
    uid: String = "local"
) : ViewModel() {
    val items: StateFlow<List<Medication>> =
        repo.observeAll(uid)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
