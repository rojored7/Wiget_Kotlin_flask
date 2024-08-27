package com.example.conexion

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.widget.RemoteViews
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.example.conexion.api.ApiService
import com.example.conexion.api.ValorFijoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Toast
import com.instana.android.CustomEvent
import com.instana.android.Instana
import java.util.concurrent.TimeUnit

class main1 : AppWidgetProvider() {
    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        Log.d("main", "onEnabled llamado")

        // Enqueue the delayed worker to keep the WorkManager component enabled
        context?.let {
            enqueueDelayedWorker(it, WidgetKeepAliveWorker::class.java)
        }
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("main", "onUpdate llamado")

        // Enqueue the delayed worker to keep the WorkManager component enabled
        context?.let {
            enqueueDelayedWorker(it, WidgetKeepAliveWorker::class.java)
        }

        if (context != null && appWidgetManager != null && appWidgetIds != null) {
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val lastMessage = sharedPreferences.getString("lastMessage", "Esperando mensaje...")

            // Registrar el BroadcastReceiver si no se ha registrado aún
            val intentFilter = IntentFilter("com.example.conexion.UPDATE_WIDGET")
            LocalBroadcastManager.getInstance(context).registerReceiver(UpdateWidgetReceiver(), intentFilter)
            Log.d("main", "UpdateWidgetReceiver registrado")

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.ventana_princial)
                views.setTextViewText(R.id.widget_text_view, lastMessage)

                val intent = Intent(context, Loggin::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.button2, pendingIntent)

                val getIntent = Intent(context, GetButtonReceiver::class.java)
                getIntent.action = "com.example.conexion.GET_BUTTON_CLICKED"
                val getPendingIntent = PendingIntent.getBroadcast(context, 0, getIntent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.button_get, getPendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Log.d("main", "onDisabled llamado")

        // Cancel the WorkManager task when the widget is disabled
        context?.let {
            WorkManager.getInstance(it).cancelUniqueWork("appWidgetWorkerKeepEnabled")
        }

        // Anular el registro del receptor cuando el widget se desactiva
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(UpdateWidgetReceiver())
        }
    }

    class GetButtonReceiver : BroadcastReceiver() {
        private lateinit var apiService: ApiService

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("GetButtonReceiver", "onReceive llamado")
            if (context != null) {
                apiService = RetrofitClient.instance.create(ApiService::class.java)
                val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val authToken = sharedPreferences.getString("authToken", null)

                if (authToken != null) {
                    val call = apiService.obtenerValor("Bearer $authToken")
                    call.enqueue(object : Callback<ValorFijoResponse> {
                        override fun onResponse(call: Call<ValorFijoResponse>, response: Response<ValorFijoResponse>) {
                            if (response.isSuccessful) {
                                val valor = response.body()?.valor.toString()
                                // Instana.reportEvent(CustomEvent(eventName = "GetButtonClick"))
                                MyApp.sendInstanaEvent("GetButtonClick",valor)

                                val intentBroadcast = Intent("com.example.conexion.UPDATE_WIDGET")
                                intentBroadcast.putExtra("response_message", valor)
                                Log.d("GetButtonReceiver", "Intent enviado a UpdateWidgetReceiver con mensaje: $valor")
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)
                            }
                        }

                        override fun onFailure(call: Call<ValorFijoResponse>, t: Throwable) {
                            //Envair un cash a proposito si el backend esta abajo. se debe re-run para generar la exception
                            //throw RuntimeException("Crash for Backend no disponible")
                            Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                            try {
                                // Lanza la excepción intencionalmente
                                throw RuntimeException("Crash for Backend no disponible")
                            } catch (e: RuntimeException) {
                                // Reporta el error a Instana y maneja la excepción sin bloquear la app
                                // Puedes manejar cualquier otra lógica aquí sin detener la aplicación
                                Log.e("Error", "Se ha capturado una excepción: ${e.message}")
                            }




                            //

                        }
                    })
                } else {
                    Toast.makeText(context, "Token no encontrado, inicie sesión", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class UpdateWidgetReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("UpdateWidgetReceiver", "onReceive llamado")
            if (context != null && intent != null) {
                val message = intent.getStringExtra("response_message")
                Log.d("UpdateWidgetReceiver", "Mensaje recibido: $message")
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val views = RemoteViews(context.packageName, R.layout.ventana_princial)
                views.setTextViewText(R.id.widget_text_view, message)

                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, main::class.java)
                )
                appWidgetManager.updateAppWidget(appWidgetIds, views)
            }
        }
    }

    // Worker class for keeping WorkManager alive
    class WidgetKeepAliveWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            Log.d("WidgetKeepAliveWorker", "Running widget keep-alive worker")
            return Result.success()
        }
    }

    // Function to enqueue the delayed worker
    fun enqueueDelayedWorker(context: Context, workerClass: Class<out ListenableWorker>) {
        val workRequest = OneTimeWorkRequest.Builder(workerClass)
            .setInitialDelay(10 * 365, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresCharging(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "appWidgetWorkerKeepEnabled",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}