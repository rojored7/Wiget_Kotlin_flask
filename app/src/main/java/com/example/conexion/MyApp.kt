package com.example.conexion

import android.app.Application
import android.icu.util.TimeUnit
import android.util.Log
import com.instana.android.CustomEvent
import com.instana.android.Instana
import com.instana.android.Instana.reportEvent
import com.instana.android.core.InstanaConfig


class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = InstanaConfig(
            reportingURL = "url",
            key = "key",
            enableCrashReporting = true,
            //initialSetupTimeoutMs
            //nterval = 60
        )
        Instana.setup(this, config)


        //Metadata Global
        Instana.meta.put("Metadata Global 1","7777")

        Log.d("MyApp", "Setup a Instana")
        // Verificar la conectividad con Instana al iniciar la aplicación
        Instana.reportEvent(
            CustomEvent(eventName = "Setup: exitoso")
        )

        val email = "hjtriana@yahoo.com"
        val maskedEmail = email.replace(Regex("^[^@]+"), "***")
        Instana.userEmail = maskedEmail
        Instana.userName = "ITAC"
        Instana.view = "Login"

        Log.d("MyApp", "Setup Exitoso")

    }

    companion object {
        fun sendInstanaEvent(nombreEvento:String, valorEvento:String) {
            Instana.meta.put("Metadata Individual 1","8888")
            val myMetas: MutableMap<String, String> = HashMap()
            val numeroCuenta = "1357911113151719"
            val maskedCuenta = numeroCuenta.replace(Regex("^.{12}"), "***")
            myMetas["Numero de cuenta"] = "$maskedCuenta"
            myMetas["Saldo"] = "$valorEvento"

            val myEvent = CustomEvent(nombreEvento)
            //myEvent.setDuration(300L, TimeUnit.MILLISECOND)
            myEvent.meta = myMetas
            myEvent.backendTracingID = "1234567890"
            myEvent.customMetric = 98.987
            myEvent.viewName="Consulta Saldo"
            reportEvent(myEvent)
            Instana.view = "Consulta Saldo"



            Log.d("MyApp", "Evento enviado a Instana: $nombreEvento: $valorEvento")


        }
    }
}