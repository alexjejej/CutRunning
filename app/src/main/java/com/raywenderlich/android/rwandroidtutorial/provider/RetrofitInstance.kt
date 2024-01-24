package com.raywenderlich.android.rwandroidtutorial.provider

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitInstance {
    companion object {
        // Creacion de instancia Retrofit 2
        fun getRetrofit(): Retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.100.11:8008/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}