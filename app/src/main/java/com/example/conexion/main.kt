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
import com.example.conexion.api.ApiService
import com.example.conexion.api.ValorFijoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.widget.Toast
import com.instana.android.CustomEvent
import com.instana.android.Instana

class main :AppWidgetProvider(){
    private var authToken: String? = null
    private lateinit var apiService: ApiService

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        Log.d("???", "onEnable")

    }

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        Log.d("???", "onUpdate")

        //aqui quite lo de blog 1
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


            for (appWidgetId in appWidgetIds) {
                // Crea un RemoteViews que apunte al layout del widget
                val views = RemoteViews(context.packageName, R.layout.ventana_princial)
                // Establecer el último mensaje en el TextView del widget
                views.setTextViewText(R.id.widget_text_view, lastMessage)

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
                                // Verificar la conectividad con Instana
                                MyApp.verifyInstanaConnectivity()
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

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        Log.d("???", "onDeleted")

    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)
        Log.d("???", "onSisable")
        // Anular el registro del receptor cuando el widget se desactiva
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(UpdateWidgetReceiver())
        }

    }
}