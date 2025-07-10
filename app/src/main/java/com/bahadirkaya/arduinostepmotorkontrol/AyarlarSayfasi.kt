package com.bahadirkaya.arduinostepmotorkontrol

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
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

class AyarlarSayfasi : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            arduinostepmotorkontrolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PinAyarEkrani()
                }
            }
        }
    }
}

@Composable
fun PinAyarEkrani() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("pin_ayarlar", Context.MODE_PRIVATE)

    val pinList = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14, 15, 17, 18, 22, 23, 24, 25, 27)

    val pin1 = remember { mutableStateOf(prefs.getInt("rele1_pin", 17)) }
    val pin2 = remember { mutableStateOf(prefs.getInt("rele2_pin", 18)) }
    val pin3 = remember { mutableStateOf(prefs.getInt("rele3_pin", 23)) }
    val pin4 = remember { mutableStateOf(prefs.getInt("rele4_pin", 24)) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("🔧 Röle Pin Ayarları", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        PinDropdown("Röle 1 Pin", pinList, pin1)
        PinDropdown("Röle 2 Pin", pinList, pin2)
        PinDropdown("Röle 3 Pin", pinList, pin3)
        PinDropdown("Röle 4 Pin", pinList, pin4)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            prefs.edit()
                .putInt("rele1_pin", pin1.value)
                .putInt("rele2_pin", pin2.value)
                .putInt("rele3_pin", pin3.value)
                .putInt("rele4_pin", pin4.value)
                .apply()
            Toast.makeText(context, "Pin ayarları kaydedildi", Toast.LENGTH_SHORT).show()
        }) {
            Text("Kaydet")
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinDropdown(label: String, options: List<Int>, selectedValue: MutableState<Int>) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            readOnly = true,
            value = "GPIO ${selectedValue.value}",
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { pin ->
                DropdownMenuItem(
                    text = { Text("GPIO $pin") },
                    onClick = {
                        selectedValue.value = pin
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun PinInput(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
