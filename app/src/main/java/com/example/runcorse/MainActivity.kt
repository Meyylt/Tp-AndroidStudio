package com.example.runcorse

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- 1. GESTION DES DEEP LINKS (Lien reçu par SMS/Web) ---
        val uri = intent.data
        var messagePartage = ""

        if (uri != null && uri.scheme == "runcorse") {
            // Si on clique sur runcorse://partage?course=LeTexte
            val texteRecu = uri.getQueryParameter("course")
            if (texteRecu != null) {
                messagePartage = "🔗 Reçu d'un ami : $texteRecu"
            }
        }

        val app = application as RunApplication
        val factory = RunViewModelFactory(app.repository)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // On passe le texte du lien à l'interface
                    RunScreen(factory, messagePartage)
                }
            }
        }
    }
}

// --- FONCTION GPS (Reverse Geocoding) ---
fun getCityName(context: Context, lat: Double, lng: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) addresses[0].locality ?: "Ville inconnue" else "Coordonnées: $lat, $lng"
    } catch (e: Exception) {
        "Lat: ${lat.toString().take(6)}, Lng: ${lng.toString().take(6)}"
    }
}

// --- FONCTION NOTIFICATION NATIVE ---
fun showNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            "run_channel",
            "Rappels Course",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, "run_channel")
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle("RunCorse 🏃")
        .setContentText("Il fait beau aujourd'hui, c'est l'heure d'aller courir !")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    notificationManager.notify(1, notification)
}

@SuppressLint("MissingPermission")
@Composable
fun RunScreen(
    factory: RunViewModelFactory,
    initialText: String = "",
    viewModel: RunViewModel = viewModel(factory = factory)
) {
    val runs by viewModel.runs.collectAsState()

    // Le champ texte prend la valeur du Deep Link s'il y en a un
    var text by remember { mutableStateOf(initialText) }

    val context = LocalContext.current

    // GPS
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Podomètre
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) }
    var isTrackingSteps by remember { mutableStateOf(false) }
    var currentSteps by remember { mutableStateOf(0) }
    var initialSteps by remember { mutableStateOf(-1f) }

    // --- POPUPS DE PERMISSIONS ---
    val gpsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                if (location != null) text = "Course à " + getCityName(context, location.latitude, location.longitude)
            }
        }
    }

    val stepPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) isTrackingSteps = true
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) showNotification(context)
    }

    // --- ÉCOUTEUR DU CAPTEUR DE PAS ---
    DisposableEffect(isTrackingSteps) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                    if (initialSteps == -1f) initialSteps = event.values[0]
                    currentSteps = (event.values[0] - initialSteps).toInt()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (isTrackingSteps && stepSensor != null) sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // --- INTERFACE UTILISATEUR ---
    Column(modifier = Modifier.padding(16.dp)) {
        Text("RunCorse", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏃 Podomètre", fontSize = 18.sp)
                Text("$currentSteps pas", fontSize = 36.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(8.dp))

                // BOUTON DE TEST POUR L'ÉMULATEUR
                Button(onClick = { currentSteps += 15 }) {
                    Text("👟 +15 pas (Simulateur)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    if (isTrackingSteps) {
                        isTrackingSteps = false
                        initialSteps = -1f
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                            if (isGranted) isTrackingSteps = true else stepPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            isTrackingSteps = true
                        }
                    }
                }) {
                    Text(if (isTrackingSteps) "Arrêter la course" else "Démarrer la course")
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = {
                    val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (isGranted) {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                            if (location != null) text = "Course à " + getCityName(context, location.latitude, location.longitude)
                        }
                    } else {
                        gpsPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("📍 GPS")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                        if (isGranted) {
                            showNotification(context)
                        } else {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        showNotification(context)
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("🔔 Rappel")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ex: Footing 10km") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                val finalText = if (currentSteps > 0) "$text ($currentSteps pas)" else text
                viewModel.addRun(finalText)
                text = ""
                currentSteps = 0
                initialSteps = -1f
                isTrackingSteps = false
            }) {
                Text("Ajouter")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(runs) { run ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(text = run.text, modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}