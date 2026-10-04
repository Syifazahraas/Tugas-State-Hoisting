package com.example.tugasstatehoisting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Memanggil Parent Composable
                TicketScreenParent()
            }
        }
    }
}

// 1. PARENT COMPOSABLE (Menyimpan State / Single Source of Truth)
@Composable
fun TicketScreenParent() {
    // State disimpan dengan rememberSaveable agar tidak hilang saat rotasi layar (Configuration Change)
    var namaPembeli by rememberSaveable { mutableStateOf("") }
    var jumlahTiket by rememberSaveable { mutableIntStateOf(1) }
    val hargaTiket = 50000 // Sesuai instruksi, harga tiket dikelola parent

    // Status State: 0 = Awal, 1 = Loading/Proses, 2 = Berhasil, 3 = Error (Nama Kosong)
    var statusPesanan by rememberSaveable { mutableIntStateOf(0) }

    // Menggunakan LaunchedEffect untuk side-effect asinkron
    // Dijalankan setiap kali nilai 'statusPesanan' berubah
    LaunchedEffect(statusPesanan) {
        if (statusPesanan == 1) { // Jika status sedang memproses
            delay(5000) // Delay 5 detik sesuai teks instruksi di gambar
            statusPesanan = 2 // Ubah status menjadi berhasil
        }
    }

    // Memanggil Child Composable dan melakukan State Hoisting (Passing data ke bawah, event ke atas)
    TicketScreen(
        namaPembeli = namaPembeli,
        onNamaChange = {
            namaPembeli = it
            // Hilangkan error kalau user mulai mengetik ulang
            if (statusPesanan == 3) statusPesanan = 0
        },
        jumlahTiket = jumlahTiket,
        onJumlahChange = { newValue ->
            if (newValue >= 1) jumlahTiket = newValue // Minimal beli 1 tiket
        },
        hargaTiket = hargaTiket,
        statusPesanan = statusPesanan,
        onPesanClick = {
            if (namaPembeli.isBlank()) {
                statusPesanan = 3 // Validasi: Nama kosong
            } else {
                statusPesanan = 1 // Nama terisi, mulai proses (Loading)
            }
        }
    )
}

// 2. CHILD COMPOSABLE (Stateless - Hanya mengurus UI/Tampilan)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    namaPembeli: String,
    onNamaChange: (String) -> Unit,
    jumlahTiket: Int,
    onJumlahChange: (Int) -> Unit,
    hargaTiket: Int,
    statusPesanan: Int,
    onPesanClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pemesanan Tiket", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1565C0))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Field Input Nama
            Text(text = "Nama", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            OutlinedTextField(
                value = namaPembeli,
                onValueChange = onNamaChange,
                placeholder = { Text("Masukkan nama Anda") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = statusPesanan == 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Field Jumlah Tiket
            Text(text = "Jumlah Tiket", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                OutlinedButton(onClick = { onJumlahChange(jumlahTiket - 1) }) {
                    Text("-", fontSize = 20.sp)
                }
                Text(text = jumlahTiket.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { onJumlahChange(jumlahTiket + 1) }) {
                    Text("+", fontSize = 20.sp)
                }
            }

            // Opsional: Teks Total Harga (Menyesuaikan instruksi parent mengelola harga tiket)
            Text(text = "Total: Rp ${jumlahTiket * hargaTiket}", color = Color.Gray, modifier = Modifier.padding(bottom = 24.dp))

            // Tombol Pesan
            Button(
                onClick = onPesanClick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = statusPesanan != 1, // Disable tombol saat loading
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("Pesan Tiket", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Container Status Pesanan
            StatusCard(statusPesanan)
        }
    }
}

// 3. KOMPONEN UI TAMBAHAN (Untuk merapikan status di bagian bawah layar)
@Composable
fun StatusCard(statusPesanan: Int) {
    val statusData = when (statusPesanan) {
        1 -> Triple("Status: Memproses pesanan...", Color.Gray, null)
        2 -> Triple("Status: Tiket berhasil dipesan!", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        3 -> Triple("Status: Nama harus diisi", Color(0xFFF44336), Icons.Default.Warning)
        else -> Triple("Status: Silakan pesan tiket", Color.Gray, null)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, statusData.second.copy(alpha = 0.5f)),
        color = statusData.second.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            // Icon atau Loading Indicator
            if (statusPesanan == 1) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            } else if (statusData.third != null) {
                Icon(imageVector = statusData.third!!, contentDescription = null, tint = statusData.second)
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Teks Status
            Text(text = statusData.first, color = statusData.second, fontWeight = FontWeight.Medium)
        }
    }
}