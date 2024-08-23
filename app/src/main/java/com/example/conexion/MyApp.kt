package com.example.conexion

import android.app.Application
import android.util.Log
import com.instana.android.CustomEvent
import com.instana.android.Instana
import com.instana.android.core.InstanaConfig
import com.instana.android.core.SuspendReportingType

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = InstanaConfig(
            reportingURL = "URL_",
            key = "Key",
            //suspendReporting = SuspendReportingType.LOW_BATTERY,
            //initialBeaconDelayMs = 5000,
            //slowSendIntervalMillis = 10000,
            collectionEnabled = false
            //usiRefreshTimeIntervalInHrs = 24,
            //autoCaptureScreenNames = false
        )
        Instana.setup(this, config)

        // Verificar la conectividad con Instana al iniciar la aplicación
        verifyInstanaConnectivity()
    }

    companion object {
        fun verifyInstanaConnectivity() {
            Instana.reportEvent(
                CustomEvent(eventName = "TestEvent")
            )

            Instana.reportEvent(
                CustomEvent(eventName = "ConnectionSuccess")
            )

            Log.d("MyApp", "Instana connectivity test event sent: TestEvent")
            Log.d("MyApp", "Instana connection success event sent: ConnectionSuccess")
        }
    }
}
