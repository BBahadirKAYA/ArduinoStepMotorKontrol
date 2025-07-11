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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    val intent = Intent(context, AyarlarSayfasi::class.java)
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .widthIn(min = 180.dp, max = 240.dp)
                    .height(48.dp)
            ) {
                Text("Ayarlar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Röle paneli
        CokluRoleKontrolPaneli()
    }
}


@Composable
fun CokluRoleKontrolPaneli() {
    val context = LocalContext.current
    val durumlar = remember { mutableStateMapOf<Int, String>() }
    val flaskBaseUrl = "http://192.168.1.102:5050"

    // SharedPreferences'tan pin numaralarını oku
    val pinList = remember {
        val prefs = context.getSharedPreferences("ayarlar", Context.MODE_PRIVATE)
        listOf(
            prefs.getInt("pin1", 17),
            prefs.getInt("pin2", 18),
            prefs.getInt("pin3", 23),
            prefs.getInt("pin4", 24)
        )
    }
    // 🟡 Başlangıçta tüm pinlerin durumlarını sorgula
    LaunchedEffect(Unit) {
        val client = OkHttpClient()
        pinList.forEach { pin ->
            val request = Request.Builder()
                .url("$flaskBaseUrl/relay/status?pin=$pin")
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    durumlar[pin] = "Bilinmiyor"
                }

                override fun onResponse(call: Call, response: Response) {
                    response.body?.string()?.let { body ->
                        try {
                            val json = JSONObject(body)
                            val durum = json.getString("durum") // "AÇIK" veya "KAPALI"
                            durumlar[pin] = durum
                        } catch (e: Exception) {
                            durumlar[pin] = "Bilinmiyor"
                        }
                    }
                }
            })
        }
    }
    Column(modifier = Modifier.padding(16.dp)) {
        Text("🔌 Röle Kontrol Paneli", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        pinList.forEach { pin ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Role",
                                tint = getDurumRenk(durumlar[pin])
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GPIO $pin", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = durumlar[pin] ?: "Bilinmiyor",
                            color = getDurumRenk(durumlar[pin]),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Button(onClick = {
                            gonderKomut(context, "$flaskBaseUrl/relay?pin=$pin&durum=ON") {
                                durumlar[pin] = it
                            }
                        }) { Text("Aç") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            gonderKomut(context, "$flaskBaseUrl/relay?pin=$pin&durum=OFF") {
                                durumlar[pin] = it
                            }
                        }) { Text("Kapat") }
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
                    try {
                        val json = JSONObject(body)
                        val status = json.getString("status")
                        Toast.makeText(context, status, Toast.LENGTH_SHORT).show()

                        when {
                            status.contains("AÇILDI") -> onResult?.invoke("AÇIK")
                            status.contains("KAPANDI") -> onResult?.invoke("KAPALI")
                            else -> onResult?.invoke("Bilinmiyor")
                        }

                    } catch (e: Exception) {
                        Toast.makeText(context, "Yanıt çözümlemesi hatası", Toast.LENGTH_SHORT).show()
                        onResult?.invoke("Bilinmiyor")
                    }
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