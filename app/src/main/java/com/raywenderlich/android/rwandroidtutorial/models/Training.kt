package com.raywenderlich.android.rwandroidtutorial.models

data class Training (
    val id: Int,
    val time: Double,
    val distance: Double,
    val speed: Double,
    val userCode: Int,
    val enabled: Int,
    val updateDate: String
)