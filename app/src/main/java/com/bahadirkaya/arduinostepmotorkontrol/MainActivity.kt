package com.bahadirkaya.arduinostepmotorkontrol

import android.os.Bundle
import android.os.Environment
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahadirkaya.arduinostepmotorkontrol.ui.theme.arduinostepmotorkontrolTheme
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import androidx.compose.ui.Alignment


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            arduinostepmotorkontrolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
        val url = "https://script.google.com/macros/s/AKfycbyBhKdr4AeUGKmV8u1xzMFZ9Q5qFQkL8rGVFYtMngTLWXGH_eEwE1EacTlk0dULVNto/exec"
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

    // Tüm içerik dış sarmalayıcısı
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {

        // Güncelleme varsa göster
        if (updateAvailable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
                Text("Sunucu: $versionName", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { indirVeYukle(context, apkUrl) }) {
                    Text("Güncelle")
                }
            }
        }

        // Bağlantı butonu
        Button(
            onClick = { gonderKomut(context, "$flaskBaseUrl/hello") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("🔗 Raspberry Pi'ye Bağlan")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Röle paneli
        CokluRoleKontrolPaneli()
    }
}


@Composable
fun CokluRoleKontrolPaneli() {
    val context = LocalContext.current
    val gpioPinList = listOf(17, 18, 23, 24)
    val durumlar = remember { mutableStateMapOf<Int, String>() }
    val flaskBaseUrl = "http://192.168.1.102:5050"

    Column {
        Text(
            "🔌 Röle Kontrol Paneli",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        gpioPinList.forEach { pin ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {

                    // GPIO başlık + durum
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "GPIO $pin",
                                tint = getDurumRenk(durumlar[pin])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GPIO $pin", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = durumlar[pin] ?: "Bilinmiyor",
                            color = getDurumRenk(durumlar[pin]),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Aç - Kapat butonları
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                gonderKomut(context, "$flaskBaseUrl/relay?pin=$pin&durum=ON") {
                                    durumlar[pin] = it
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aç")
                        }
                        Button(
                            onClick = {
                                gonderKomut(context, "$flaskBaseUrl/relay?pin=$pin&durum=OFF") {
                                    durumlar[pin] = it
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kapat")
                        }
                    }
                }
            }
        }
    }
}


fun getDurumRenk(durum: String?): Color {
    return when (durum) {
        "AÇIK" -> Color(0xFF4CAF50)
        "KAPALI" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

fun gonderKomut(context: Context, url: String, onResult: ((String) -> Unit)? = null) {
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Bağlantı hatası: ${e.message}", Toast.LENGTH_SHORT).show()
                onResult?.invoke("HATA")
            }
        }
        override fun onResponse(call: Call, response: Response) {
            response.body?.string()?.let { body ->
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, body, Toast.LENGTH_SHORT).show()
                    if (body.contains("AÇILDI")) onResult?.invoke("AÇIK")
                    else if (body.contains("KAPANDI")) onResult?.invoke("KAPALI")
                    else onResult?.invoke("Bilinmiyor")
                }
            }
        }
    })
}

fun indirVeYukle(context: Context, apkUrl: String) {
    val fileName = "arduinostepmotorkontrol.apk"
    val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
        setTitle("Yeni güncelleme indiriliyor")
        setDescription("Lütfen bekleyin...")
        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        setMimeType("application/vnd.android.package-archive")
    }

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val downloadId = manager.enqueue(request)

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val action = intent?.action
            val receivedDownloadId = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action && receivedDownloadId == downloadId) {
                val apkUri = Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/$fileName")
                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(installIntent)
            }
        }
    }}