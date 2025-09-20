package com.example.medtracker.data.remote.openfda

import com.google.gson.annotations.SerializedName

data class OpenFdaNdcResponse(
    @SerializedName("results") val results: List<NdcItem>?
)

data class NdcItem(
    @SerializedName("brand_name")   val brandName: String?,
    @SerializedName("generic_name") val genericName: String?,
    @SerializedName("dosage_form")  val dosageForm: String?,
    @SerializedName("product_ndc")  val productNdc: String?,
    @SerializedName("active_ingredients") val activeIngredients: List<ActiveIngredient>?
)

data class ActiveIngredient(
    @SerializedName("name")     val name: String?,
    @SerializedName("strength") val strength: String?
)

