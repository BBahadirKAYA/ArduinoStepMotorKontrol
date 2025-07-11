package com.bahadirkaya.arduinostepmotorkontrol

import android.content.Context
import android.view.MotionEvent
import android.widget.Toast
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.ui.Alignment
import kotlinx.coroutines.*

@Composable
fun StepMotorKontrolEkrani(onGeriDon: () -> Unit) {
    val context = LocalContext.current
    val flaskBaseUrl = "http://192.168.1.102:5050"
    var adimSayisi by remember { mutableStateOf("100") }
    var hiz by remember { mutableStateOf("0.1") }
    var motorAktif by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text("\uD83D\uDD00 Step Motor Kontrol", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = adimSayisi,
            onValueChange = { adimSayisi = it },
            label = { Text("Adım Sayısı") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = hiz,
            onValueChange = { hiz = it },
            label = { Text("Hız (ms)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Motor Durumu: ", modifier = Modifier.weight(1f))
            Switch(
                checked = motorAktif,
                onCheckedChange = {
                    motorAktif = it
                    val enableUrl = "$flaskBaseUrl/enable?durum=" + if (motorAktif) "ON" else "OFF"
                    gonderKomut(context, enableUrl)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("\uD83D\uDD04 Yön Butonları (basılı tutarak kontrol)")

        Spacer(modifier = Modifier.height(16.dp))

        YonKontrolPaneliBasiliDestekli(
            onYukariBasili = {
                hiz = (hiz.toIntOrNull()?.minus(1)?.coerceAtLeast(1) ?: 1).toString()
            },
            onAsagiBasili = {
                hiz = (hiz.toIntOrNull()?.plus(1) ?: 5).toString()
            },
            onSagaBasili = {
                val url = "$flaskBaseUrl/step?adim=10&yon=sag&hiz=$hiz&enable=${if (motorAktif) "ON" else "OFF"}"
                gonderKomut(context, url)
            },
            onSolaBasili = {
                val url = "$flaskBaseUrl/step?adim=10&yon=sol&hiz=$hiz&enable=${if (motorAktif) "ON" else "OFF"}"
                gonderKomut(context, url)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                val url = "$flaskBaseUrl/step?adim=$adimSayisi&yon=sag&hiz=$hiz&enable=${if (motorAktif) "ON" else "OFF"}"
                gonderKomut(context, url)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("⚙️ Motoru Çalıştır")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onGeriDon,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("⬅ Ana Sayfaya Dön")
        }
    }
}

@Composable
fun YonKontrolPaneliBasiliDestekli(
    onYukariBasili: () -> Unit,
    onAsagiBasili: () -> Unit,
    onSagaBasili: () -> Unit,
    onSolaBasili: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        BasiliButton(Icons.Default.ArrowUpward, "Yukarı", onYukariBasili)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BasiliButton(Icons.Default.ArrowBack, "Sola", onSolaBasili)
            Spacer(modifier = Modifier.width(32.dp))
            BasiliButton(Icons.Default.ArrowForward, "Sağa", onSagaBasili)
        }
        Spacer(modifier = Modifier.height(12.dp))
        BasiliButton(Icons.Default.ArrowDownward, "Aşağı", onAsagiBasili)
    }
}

@Composable
fun BasiliButton(
    icon: ImageVector,
    contentDesc: String,
    onTick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> isPressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> isPressed = false
            }
        }
    }

    LaunchedEffect(isPressed) {
        while (isPressed) {
            onTick()
            delay(10L)
        }
    }

    Button(
        onClick = {},
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
        interactionSource = interactionSource
    ) {
        Icon(icon, contentDescription = contentDesc)
    }
}

fun gonderKomut(context: Context, url: String) {
    val client = okhttp3.OkHttpClient()
    val request = okhttp3.Request.Builder().url(url).build()
    client.newCall(request).enqueue(object : okhttp3.Callback {
        override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            response.body?.string()?.let { body ->
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(context, body, Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}
