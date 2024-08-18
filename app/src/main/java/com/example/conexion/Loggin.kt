package com.example.conexion

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.conexion.api.ApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import android.widget.EditText
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.conexion.api.LoginData
import com.example.conexion.api.LoginResponse
import com.example.conexion.api.ValorFijoResponse

class Loggin : AppCompatActivity() {
    private lateinit var apiService: ApiService
    private lateinit var usuarioField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginButton: Button
    private lateinit var back2WidgetButton: Button
    private lateinit var conexionButton: Button
    private var authToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LogginActivity", "onCreate iniciado")

        enableEdgeToEdge()
        setContentView(R.layout.activity_loggin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inicializar ApiService
        apiService = RetrofitClient.instance.create(ApiService::class.java)
        Log.d("LogginActivity", "ApiService creado")

        // Inicializar las vistas
        usuarioField = findViewById(R.id.usuario)
        passwordField = findViewById(R.id.password)
        loginButton = findViewById(R.id.btn_login)
        back2WidgetButton = findViewById(R.id.button)
        conexionButton = findViewById(R.id.button3)

        Log.d("LogginActivity", "Vistas inicializadas")

        // Configura el listener del botón para cerrar la actividad
        back2WidgetButton.setOnClickListener {
            Log.d("LogginActivity", "Botón de retorno clicado")
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_HOME)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()  // Cierra la actividad actual y regresa al widget
            Log.d("LogginActivity", "Actividad cerrada y regreso al widget")
        }

        loginButton.setOnClickListener {
            val usuario = usuarioField.text.toString()
            val password = passwordField.text.toString()
            Log.d("LogginActivity", "Botón de login clicado con usuario: $usuario")

            if (usuario.isNotEmpty() && password.isNotEmpty()) {
                Log.d("LogginActivity", "Campos de usuario y contraseña no están vacíos")
                realizarLogin(usuario, password)
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
                Log.d("LogginActivity", "Campos de usuario o contraseña vacíos")
            }
        }

        // Configura el listener para el botón de conexión
        conexionButton.setOnClickListener {
            Log.d("LogginActivity", "Botón de conexión clicado")
            // Llamar al endpoint '/'
            val call = apiService.getHomeMessage()

            call.enqueue(object : Callback<String> {
                override fun onResponse(call: retrofit2.Call<String>, response: Response<String>) {
                    if (response.isSuccessful) {
                        val message = response.body() ?: "Sin respuesta"
                        Log.d("LogginActivity", "Conexión exitosa, mensaje recibido: $message")

                        // Envía un broadcast para actualizar el widget con el mensaje del servidor
                        val intentBroadcast  = Intent("com.example.conexion.UPDATE_WIDGET")
                        intentBroadcast.putExtra("response_message", message)
                        LocalBroadcastManager.getInstance(this@Loggin).sendBroadcast(intentBroadcast)
                        Log.d("LogginActivity", "Broadcast enviado con mensaje: $message")

                        // Cierra LogginActivity y regresa a la pantalla de inicio
                        val intent = Intent(Intent.ACTION_MAIN)
                        intent.addCategory(Intent.CATEGORY_HOME)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        Log.d("LogginActivity", "Regresando a la pantalla de inicio")
                        finish()
                    } else {
                        Toast.makeText(this@Loggin, "Error en la respuesta del servidor", Toast.LENGTH_SHORT).show()
                        Log.d("LogginActivity", "Error en la respuesta del servidor: ${response.code()}")
                    }
                }
                override fun onFailure(call: Call<String>, t: Throwable) {
                    Toast.makeText(this@Loggin, "Error de conexión", Toast.LENGTH_SHORT).show()
                    Log.d("LogginActivity", "Error de conexión: ${t.message}")
                }
            })
        }
    }

    private fun realizarLogin(usuario: String, password: String) {
        Log.d("LogginActivity", "Iniciando login con usuario: $usuario")
        val loginData = LoginData(usuario, password)
        val call = apiService.login(loginData)

        call.enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body()?.status == "success") {
                    authToken = response.body()?.token
                    Toast.makeText(this@Loggin, "Login exitoso", Toast.LENGTH_SHORT).show()
                    Log.d("LogginActivity", "Login exitoso, token recibido: $authToken")
                    // Envía el token de regreso al widget usando LocalBroadcastManager
                    val intentBroadcast = Intent("com.example.conexion.UPDATE_WIDGET")
                    intentBroadcast.putExtra("auth_token", authToken)
                    LocalBroadcastManager.getInstance(this@Loggin).sendBroadcast(intentBroadcast)
                    Log.d("LogginActivity", "Broadcast enviado con token: $authToken")

                    // Cierra la actividad y regresa al widget
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@Loggin, "Login fallido", Toast.LENGTH_SHORT).show()
                    Log.d("LogginActivity", "Login fallido: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@Loggin, "Error de conexión", Toast.LENGTH_SHORT).show()
                Log.d("LogginActivity", "Error de conexión: ${t.message}")
            }
        })
    }

    private fun obtenerValorProtegido(token: String) {
        Log.d("LogginActivity", "Iniciando obtención de valor protegido con token: $token")
        val call = apiService.obtenerValor("Bearer $token")

        call.enqueue(object : Callback<ValorFijoResponse> {
            override fun onResponse(call: Call<ValorFijoResponse>, response: Response<ValorFijoResponse>) {
                if (response.isSuccessful) {
                    val valor = response.body()?.valor.toString()
                    Log.d("LogginActivity", "Valor protegido recibido: $valor")

                    // Envía un broadcast para actualizar el widget con el valor fijo
                    val intentBroadcast  = Intent("com.example.conexion.UPDATE_WIDGET")
                    intentBroadcast.putExtra("response_message", valor)
                    LocalBroadcastManager.getInstance(this@Loggin).sendBroadcast(intentBroadcast)
                    Log.d("LogginActivity", "Broadcast enviado con valor protegido: $valor")

                    // Cierra LogginActivity y regresa a la pantalla de inicio
                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_HOME)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@Loggin, "Error al obtener el valor", Toast.LENGTH_SHORT).show()
                    Log.d("LogginActivity", "Error al obtener el valor: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ValorFijoResponse>, t: Throwable) {
                Toast.makeText(this@Loggin, "Error de conexión", Toast.LENGTH_SHORT).show()
                Log.d("LogginActivity", "Error de conexión: ${t.message}")
            }
        })
    }
}
