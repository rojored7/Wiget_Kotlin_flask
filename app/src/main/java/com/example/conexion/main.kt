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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.instana.android.CustomEvent
import com.instana.android.Instana
import java.util.concurrent.TimeUnit
import androidx.work.Constraints

class main :AppWidgetProvider(){
    private var authToken: String? = null
    private lateinit var apiService: ApiService
    private var counter: Int = 0

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        Log.d("???", "onEnable")

        // Enqueue the delayed worker to keep the WorkManager component enabled
        context?.let {
            enqueueDelayedWorker(it, WidgetKeepAliveWorker::class.java)
        }

    }
    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        val action = intent?.action ?: ""

        if (context != null && action == "increment_counter") {
            val prefs = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)
            val newCount = prefs.getInt("counter", 0) + 1
            prefs.edit().putInt("counter", newCount).apply()

            // Enviar datos a Instana
            Instana.reportEvent(
                CustomEvent(eventName = "CounterIncrement")
            )

            Log.d("main", "Contador incrementado: $newCount y enviado a Instana")

            // Actualizar todos los widgets con el nuevo contador
            updateCounterView(context, newCount)
        }
    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("???", "onUpdate")

        // Enqueue the delayed worker to keep the WorkManager component enabled
        context?.let {
            enqueueDelayedWorker(it, WidgetKeepAliveWorker::class.java)
        }
        // Registrar el receptor para el Broadcast local
        if (context != null) {
            LocalBroadcastManager.getInstance(context).registerReceiver(
                UpdateWidgetReceiver(),

                IntentFilter("com.example.conexion.UPDATE_WIDGET")
            )

            Log.d("onUpdate", "GetButtonReceiver registrado correctamente")
        }

        if (context != null && appWidgetManager != null && appWidgetIds != null) {

            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val lastMessage = sharedPreferences.getString("lastMessage", "Esperando mensaje...")
            counter = sharedPreferences.getInt("counter", 0)

            for (appWidgetId in appWidgetIds) {
                // Crea un RemoteViews que apunte al layout del widget
                val views = RemoteViews(context.packageName, R.layout.ventana_princial)
                // Establecer el último mensaje en el TextView del widget
                views.setTextViewText(R.id.widget_text_view, lastMessage)
                //Contador
                views.setTextViewText(R.id.counter_text_view, "Contador: $counter")

                // Configura un Intent que abrirá LogginActiv   ity
                val intent = Intent(context, Loggin::class.java)
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

                // Asocia el PendingIntent con el botón del widget
                views.setOnClickPendingIntent(R.id.button2, pendingIntent)

                // Configura un Intent para el botón "Obtener Valor Fijo"
                val getIntent = Intent(context, GetButtonReceiver::class.java)
                getIntent.action = "com.example.conexion.GET_BUTTON_CLICKED"
                val getPendingIntent = PendingIntent.getBroadcast(context, 0, getIntent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.button_get, getPendingIntent)



                val incrementIntent = Intent(context, main::class.java)
                incrementIntent.action = "increment_counter"
                val incrementPendingIntent = PendingIntent.getBroadcast(context, 0, incrementIntent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.button_increment, incrementPendingIntent)


                // Notifica al AppWidgetManager para actualizar el widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
        // Registrar el BroadcastReceiver
        //context?.registerReceiver(UpdateWidgetReceiver(), IntentFilter("com.example.conexion.UPDATE_WIDGET"))

    }

    class UpdateWidgetReceiver : BroadcastReceiver() {
        private var lastMessage: String? = null
        private var lastToken: String? = null

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("UpdateWidgetReceiver", "Broadcast recibido main")

            if (context != null && intent != null) {
                val message = intent.getStringExtra("response_message")
                val token = intent.getStringExtra("auth_token")

                Log.d("broadvast main", message ?: "No message received")

                // Verificar y almacenar el token si es nuevo
                if (token != null && token != lastToken) {
                    lastToken = token
                    Log.d("UpdateWidgetReceiver", "Token recibido: $token")
                    val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putString("authToken", token).apply()
                    Log.d("UpdateWidgetReceiver", "Token guardado: $token")
                } else if (token == null) {
                    Log.d("UpdateWidgetReceiver", "Token es nulo o no ha cambiado, no se actualiza.")
                }

                // Verificar y actualizar el mensaje si es nuevo
                if (message != null && message != lastMessage) {
                    lastMessage = message
                    val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                    sharedPreferences.edit().putString("lastMessage", message).apply()

                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.ventana_princial)
                    views.setTextViewText(R.id.widget_text_view, message)

                    val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, main::class.java)
                    )

                    appWidgetManager.updateAppWidget(appWidgetIds, views)
                    Log.d("UpdateWidgetReceiver", "Widget actualizado con el mensaje: $message")
                } else if (message == null) {
                    Log.d("UpdateWidgetReceiver", "Mensaje es nulo o no ha cambiado, no se actualiza.")
                }
            }
        }
    }

    class GetButtonReceiver : BroadcastReceiver() {
        private lateinit var apiService: ApiService

        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("GetButtonReceiver", "onReceive llamado")
            if (context != null) {
                // Inicializar apiService
                apiService = RetrofitClient.instance.create(ApiService::class.java)

                // Obtener authToken de SharedPreferences
                val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                val authToken = sharedPreferences.getString("authToken", null)
                Log.d("GetButtonReceiver", "Token recuperado: $authToken")
                if (authToken != null) {
                    Log.d("GetButtonReceiver", "Token usado: $authToken")
                    val call = apiService.obtenerValor("Bearer $authToken")

                    call.enqueue(object : Callback<ValorFijoResponse> {
                        override fun onResponse(call: Call<ValorFijoResponse>, response: Response<ValorFijoResponse>) {
                            if (response.isSuccessful) {
                                val valor = response.body()?.valor.toString()
                                // Enviar evento a Instana
                                Instana.reportEvent(
                                    CustomEvent(eventName = "GetButtonClick")
                                )
                                MyApp.sendInstanaEvent("GetButtonClick", valor)
                                // Verificar la conectividad con Instana
                                //MyApp.verifyInstanaConnectivity()
                                Log.d("GetButtonReceiver", "Valor recibido: $valor")

                                val intentBroadcast = Intent("com.example.conexion.UPDATE_WIDGET")
                                intentBroadcast.putExtra("response_message", valor)
                                LocalBroadcastManager.getInstance(context).sendBroadcast(intentBroadcast)
                                //context.sendBroadcast(intentBroadcast)
                                Log.d("GetButtonReceiver", "Intent enviado a UpdateWidgetReceiver con mensaje: $valor")
                            } else {
                                Toast.makeText(context, "Error al obtener el valor", Toast.LENGTH_SHORT).show()
                                Log.d("GetButtonReceiver", "Error en la respuesta del servidor")
                            }
                        }

                        override fun onFailure(call: Call<ValorFijoResponse>, t: Throwable) {
                            Toast.makeText(context, "Error de conexión", Toast.LENGTH_SHORT).show()
                            Log.d("GetButtonReceiver", "Error de conexión: ${t.message}")
                        }
                    })
                } else {
                    Toast.makeText(context, "Token no encontrado, inicie sesión", Toast.LENGTH_SHORT).show()
                    Log.d("GetButtonReceiver", "Token no encontrado")
                }
            }
        }
    }
    class IncrementButtonReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("IncrementButtonReceiver", "onReceive called")

            if (context != null) {
                val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
                var counter = sharedPreferences.getInt("counter", 0)
                counter++

                Log.d("IncrementButtonReceiver", "Counter incremented to: $counter")

                sharedPreferences.edit().putInt("counter", counter).apply()

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val views = RemoteViews(context.packageName, R.layout.ventana_princial)
                views.setTextViewText(R.id.counter_text_view, "Contador: $counter")

                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, main::class.java)
                )

                appWidgetManager.updateAppWidget(appWidgetIds, views)
                Log.d("IncrementButtonReceiver", "Widget updated with counter: $counter")
            }
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        Log.d("???", "onDeleted")

    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Log.d("???", "onSisable")
        // Cancel the WorkManager task when the widget is disabled
        context?.let {
            WorkManager.getInstance(it).cancelUniqueWork("appWidgetWorkerKeepEnabled")
        }

        // Anular el registro del receptor cuando el widget se desactiva
        context?.let {
            LocalBroadcastManager.getInstance(it).unregisterReceiver(UpdateWidgetReceiver())
        }
        // Anular el registro del receptor cuando el widget se desactiva
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(UpdateWidgetReceiver())
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

    private fun updateCounterView(context: Context, newCount: Int) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.ventana_princial)
        views.setTextViewText(R.id.counter_text_view, "Contador: $newCount")

        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, main::class.java)
        )

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }



}