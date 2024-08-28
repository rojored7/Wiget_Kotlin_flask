package com.example.conexion

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_APPWIDGET = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.instal_widget_button)

        button.setOnClickListener {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
            pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            startActivityForResult(pickIntent, REQUEST_APPWIDGET)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_APPWIDGET && resultCode == Activity.RESULT_OK) {
            val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != null && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val appWidgetManager = AppWidgetManager.getInstance(this)
                val views = RemoteViews(packageName, R.layout.ventana_princial)

                // Configura la vista inicial del widget
                views.setTextViewText(R.id.widget_text_view, "Bienvenido!")
                appWidgetManager.updateAppWidget(appWidgetId, views)

                // Aquí puedes agregar más configuración del widget si es necesario
            } else {
                Log.e("MainActivity", "Error: appWidgetId no válido.")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}
