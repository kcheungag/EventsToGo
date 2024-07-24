package com.example.eventstogo_group6.api

import com.example.eventstogo_group6.models.City
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface CityInterface {
    @Headers("X-Api-Key: Ip5V6zGrztqZF21sStXRztlu1hzlpKDraYOBT2C3")
    @GET("/v1/city?limit=30")
    suspend fun getCities(@Query("country") countryCode:String): List<City>
}