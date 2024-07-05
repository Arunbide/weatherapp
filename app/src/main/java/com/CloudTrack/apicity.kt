package com.CloudTrack
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface apicity {
        @GET("reverse")
        fun getCityData(
            @Query("lat") lat: String,
            @Query("lon") lon: String,
            @Query("appid") apiKey: String
        ): Call<List<GetCity>>
    }
