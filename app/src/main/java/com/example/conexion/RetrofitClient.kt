/*package com.example.conexion

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

object RetrofitClient{
    private const val BASE_URL = "http://10.0.2.2:5000/"

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
*/
package com.example.conexion

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import com.instana.android.instrumentation.okhttp3.OkHttp3GlobalInterceptor

object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:5000/"

    // Configuración del cliente OkHttp con el interceptor de Instana
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(OkHttp3GlobalInterceptor)  // Añade el interceptor de Instana
            .build()
    }

    // Configuración de Retrofit con el cliente OkHttp instrumentado
    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)  // Usar el cliente OkHttp configurado
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}