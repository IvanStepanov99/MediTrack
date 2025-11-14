package com.example.medtracker.data.remote.openfda

import com.example.medtracker.data.db.entities.Drug

internal fun NdcItem.toSuggestion(): DrugSuggestion {
    val rawGeneric = genericName ?: activeIngredients?.firstOrNull()?.name
    val generic = rawGeneric?.trim()?.takeIf { it.isNotEmpty() }
    val brand = brandName?.trim()?.takeIf { it.isNotEmpty() }
    return DrugSuggestion(
        genericName = generic,
        brandName = brand
    )
}

fun DrugSuggestion.toDrug(uid: String) = Drug(
    uid = uid,
    name = (genericName ?: brandName ?: "").trim(),
    brandName = brandName,
    drugbankId = "",
    strength = null,
    unit = null,
    form = null,
    notes = null
)