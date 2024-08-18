package com.example.conexion.api

import android.telecom.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService{
    @GET("/")
    fun getHomeMessage():retrofit2.Call<String>

    @POST("/login")
    fun login(@Body loginData: LoginData): retrofit2.Call<LoginResponse>

    @GET("/valor")
    fun obtenerValor(@Header("Authorization") token: String): retrofit2.Call<ValorFijoResponse>

    @POST("/valor")
    fun actualizarValor(@Header("Authorization") token: String, @Body nuevoValor: NuevoValorRequest): retrofit2.Call<ActualizarValorResponse>
}

data class LoginData(val usuario: String, val password: String)
data class LoginResponse(val mensaje: String, val token: String, val status: String)
data class ValorFijoResponse(val valor: Int)
data class NuevoValorRequest(val valor: Int)
data class ActualizarValorResponse(val mensaje: String, val nuevo_valor: Int)