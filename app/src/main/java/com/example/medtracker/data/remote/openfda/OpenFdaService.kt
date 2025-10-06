package com.example.medtracker.data.remote.openfda

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaService {
    @GET("drug/ndc.json")
    suspend fun searchNdc(
        @Query(value = "search", encoded = true) search: String,
        @Query("limit") limit: Int = 10
    ): OpenFdaNdcResponse
}