package api.request

import api.dtos.TrainingDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ITrainingRequest {
    @GET("trainings")
    suspend fun getAllTrainings(): Response<TrainingDto>

    @POST("trainings")
    suspend fun addNewTraining( @Body training: TrainingDto ): Response<String>
}