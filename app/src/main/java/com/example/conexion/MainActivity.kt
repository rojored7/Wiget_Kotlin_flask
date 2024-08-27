package com.example.conexion

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.instal_widget_button)

        button.setOnClickListener {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(this, main::class.java))

            if (appWidgetIds.isNotEmpty()) {
                val appWidgetId = appWidgetIds[0]  // Usando el primer ID de la lista

                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                startActivityForResult(intent, 1)
            } else {
                // Manejar el caso donde no hay widgets disponibles
                Log.d("MainActivity", "No hay IDs de AppWidget disponibles.")
            }
        }
    }
}
