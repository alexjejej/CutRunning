package com.cut.android.running.provider.services

import com.cut.android.running.common.response.IResponse
import com.cut.android.running.models.Role
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface RoleService {
    @GET("role")
    suspend fun getRoles(): Response<IResponse<List<Role>>>

    @GET("role/{id}")
    suspend fun getRoleById(@Path("id") id: Int): Response<IResponse<Role>>

    @POST("role")
    suspend fun addRole(@Body role: Role): Response<IResponse<Boolean>>

    @PUT("role")
    suspend fun updateRole(@Body role: Role): Response<IResponse<Boolean>>

    @DELETE("role/{id}")
    suspend fun deleteRole(@Path("id") id: Int): Response<IResponse<Boolean>>
}