package com.seerslab.argear.sample.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ContentsApi {

    @GET("/api/v3/{api_key}")
    fun getContents(@Path("api_key") apiKey: String): Call<ContentsResponse?>
}