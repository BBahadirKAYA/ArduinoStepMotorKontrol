package com.bahadirkaya.arduinostepmotorkontrol

import android.os.Bundle
import android.os.Environment
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bahadirkaya.arduinostepmotorkontrol.ui.theme.arduinostepmotorkontrolTheme
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            arduinostepmotorkontrolTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SorguSonucuEkrani()
                }
            }
        }
    }
}

@Composable
fun SorguSonucuEkrani() {
    val context = LocalContext.current
    var versionCodeFromServer by remember { mutableStateOf(-1) }
    var versionName by remember { mutableStateOf("") }
    var mesaj by remember { mutableStateOf("") }
    var updateAvailable by remember { mutableStateOf(false) }
    var apkUrl by remember { mutableStateOf("") }

    val flaskBaseUrl = "http://192.168.1.102:5050"

    // Sürüm kontrolü
    LaunchedEffect(Unit) {
        val url =
            "https://script.google.com/macros/s/AKfycbyBhKdr4AeUGKmV8u1xzMFZ9Q5qFQkL8rGVFYtMngTLWXGH_eEwE1EacTlk0dULVNto/exec"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                mesaj = "Hata: ${e.message}"
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string() ?: ""
                try {
                    val json = JSONObject(responseString)
                    val dataArray = json.getJSONArray("data")
                    val item = dataArray.getJSONObject(0)

                    versionCodeFromServer = item.getInt("versionCode")
                    versionName = item.getString("versionName")
                    mesaj = item.getString("mesaj")
                    apkUrl = item.getString("apkUrl")

                    val currentVersion = BuildConfig.VERSION_CODE
                    updateAvailable = versionCodeFromServer > currentVersion
                } catch (e: Exception) {
                    mesaj = "JSON ayrıştırma hatası: ${e.message}"
                }
            }
        })
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Uygulama Sürüm: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
        Text("Sunucu Sürüm: $versionName", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Mesaj: $mesaj", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(12.dp))

        if (updateAvailable) {
            Text("🔔 Yeni güncelleme mevcut!", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = {
                indirVeYukle(context, apkUrl)
            }) {
                Text("Güncellemeyi İndir")
            }
        } else {
            Text("✅ Uygulama güncel.", color = MaterialTheme.colorScheme.secondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔌 Röle kontrolü
        Text("🔧 Röle Kontrolü", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = {
                gonderKomut(context, "$flaskBaseUrl/relay?durum=ON")
            }) {
                Text("Röle AÇ")
            }

            Button(onClick = {
                gonderKomut(context, "$flaskBaseUrl/relay?durum=OFF")
            }) {
                Text("Röle KAPA")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🔎 Bağlantı testi
        Button(onClick = {
            gonderKomut(context, "$flaskBaseUrl/hello")
        }) {
            Text("Raspberry Pi'ye Bağlan")
        }
    }
}

fun gonderKomut(context: Context, url: String) {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { body ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, body, Toast.LENGTH_SHORT).show()
                }
            }
        }
    })
}

fun indirVeYukle(context: Context, apkUrl: String) {
    val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
        setTitle("Yeni güncelleme indiriliyor")
        setDescription("Lütfen bekleyin...")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "arduinostepmotorkontrol.apk")
        setMimeType("application/vnd.android.package-archive")
    }

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}
