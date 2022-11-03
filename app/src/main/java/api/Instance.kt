package api

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Instance {

    companion object {

        /** Genera una instancia de Retrofit apuntando a la url base de la api
         * @return Retrofit
         * **/
        fun getRetrofitInstance(): Retrofit {
            return Retrofit.Builder()
                .baseUrl( "https://cutrunningapi-production.up.railway.app/api/" )
                .addConverterFactory(GsonConverterFactory.create())
                    // implementar el http client
                .client( getClient() )
                .build()
        }

        /** Definicion de un interceptor para los headers de las consultas **/
        private fun getClient(): OkHttpClient {
            val client = OkHttpClient.Builder()
                .addInterceptor( HeaderInterceptor() )
                .build()
            return  client
        }
    }
}