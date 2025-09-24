package com.example.medtracker.data.remote.openfda

import com.example.medtracker.data.db.entities.Drug

internal fun NdcItem.toSuggestion(): DrugSuggestion {
    val (amt, unit) = parseStrength(activeIngredients?.firstOrNull()?.strength)
    val formNorm = dosageForm?.lowercase()?.replace('_',' ')
    val generic = genericName ?: activeIngredients?.firstOrNull()?.name
    return DrugSuggestion(
        genericName = generic,
        brandName = brandName,
        strengthAmount = amt,
        strengthUnit = unit,
        form = formNorm
    )
}

private fun parseStrength(s: String?): Pair<Double?, String?> {
    if (s.isNullOrBlank()) return null to null
    val m = Regex("""([\d.]+)\s*([A-Za-zμµ]+)""").find(s) ?: return null to null
    val amount = m.groupValues.getOrNull(1)?.toDoubleOrNull()
    val unit   = m.groupValues.getOrNull(2)
    return amount to unit
}

fun DrugSuggestion.toDrug(uid: String) = Drug(
    uid = uid,
    name = (genericName ?: brandName ?: "").trim(),
    brandName = brandName,
    drugbankId = "",
    strength = strengthAmount,
    unit = strengthUnit,
    form = form,
    notes = null
)