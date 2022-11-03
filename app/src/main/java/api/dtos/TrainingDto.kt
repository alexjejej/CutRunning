package api.dtos

import com.google.gson.annotations.SerializedName

data class TrainingDto (
    // @SerializedName("id") var id: Int,
    @SerializedName("steps") var steps: Int,
    @SerializedName("averagespeed") var averagespeed: Double,
    @SerializedName("distance") val distance: Double
        )