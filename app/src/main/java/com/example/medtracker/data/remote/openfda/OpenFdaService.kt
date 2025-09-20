package com.example.medtracker.data.remote.openfda

import android.icu.text.StringSearch
import android.media.audiofx.DynamicsProcessing.Limiter
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenFdaService {
    @GET("drug/ndc.json")
    suspend fun searchNdc(
        @Query("search") search: String,
        @Query("limit") limiter: Int = 10
    ): OpenFdaNdcResponse
}