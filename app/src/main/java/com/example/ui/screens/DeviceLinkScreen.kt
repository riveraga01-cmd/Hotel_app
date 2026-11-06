package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.ConnectionStatus
import com.example.model.TempLinkCode
import com.example.ui.components.CameraQrScanner
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.QrCodeView
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceLinkScreen(
    connectionStatus: ConnectionStatus,
    pendingSyncCount: Int,
    onSyncClick: () -> Unit,
    isDeviceAuthorized: Boolean,
    activeGeneratedCode: TempLinkCode?,
    onGenerateCodeClick: () -> Unit,
    onLinkDeviceWithCode: (String, String) -> Result<String>,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    var isCameraScanning by remember { mutableStateOf(false) }
    var manualCodeInput by remember { mutableStateOf("") }
    var deviceNameInput by remember { mutableStateOf("Teléfono Recepción 2") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }

    // Camera permission launcher
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            isCameraScanning = true
        } else {
            Toast.makeText(context, "Se requiere permiso de cámara para escanear código QR", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "VINCULAR DISPOSITIVO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("btn_back_link_screen")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HotelNavy,
                        titleContentColor = Color.White
                    )
                )
                ConnectionStatusBar(
                    status = connectionStatus,
                    pendingSyncCount = pendingSyncCount,
                    onSyncClick = onSyncClick
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Notice Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDeviceAuthorized) StatusAvailableBg else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isDeviceAuthorized) Icons.Default.CheckCircle else Icons.Default.Devices,
                        contentDescription = null,
                        tint = if (isDeviceAuthorized) StatusAvailableGreen else HotelNavy,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isDeviceAuthorized) "Dispositivo Autorizado — Hotel Rivera" else "Sistema Multi-Dispositivo Hotel Rivera",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDeviceAuthorized) StatusAvailableGreen else HotelNavy
                        )
                        Text(
                            text = if (isDeviceAuthorized)
                                "Este teléfono está activo y comparte información con los demás dispositivos autorizados."
                            else
                                "Permite compartir habitaciones, facturas y cobros en tiempo real entre múltiples teléfonos autorizados.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section A: GENERAR CÓDIGO (Only when user taps button)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DESDE UN DISPOSITIVO AUTORIZADO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HotelNavy,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Genera un código temporal de un solo uso para autorizar otro teléfono.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onGenerateCodeClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_generate_link_code"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HotelNavy,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GENERAR CÓDIGO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    if (activeGeneratedCode != null) {
                        Spacer(modifier = Modifier.height(20.dp))

                        var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
                        LaunchedEffect(activeGeneratedCode.code) {
                            while (true) {
                                currentTime = System.currentTimeMillis()
                                delay(1000L)
                            }
                        }

                        val remainingSeconds = ((activeGeneratedCode.expiresAtEpoch - currentTime) / 1000).coerceAtLeast(0L)
                        val isExpired = remainingSeconds <= 0L || activeGeneratedCode.isUsed

                        if (isExpired) {
                            Text(
                                text = if (activeGeneratedCode.isUsed) "✓ Código utilizado con éxito" else "⚠️ Este código ha expirado",
                                color = if (activeGeneratedCode.isUsed) StatusAvailableGreen else StatusOccupiedRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        } else {
                            // Display QR code
                            QrCodeView(
                                qrText = activeGeneratedCode.code,
                                dimensionDp = 200
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = activeGeneratedCode.code,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HotelNavy,
                                letterSpacing = 4.sp
                            )

                            Text(
                                text = "Expira en: ${remainingSeconds / 60}m ${remainingSeconds % 60}s • De un solo uso",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusReservedAmber
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section B: VINCULAR ESTE NUEVO DISPOSITIVO
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "EN ESTE NUEVO DISPOSITIVO",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HotelGoldDark,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons: ESCANEAR CÓDIGO QR & INGRESAR CÓDIGO MANUALMENTE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    isCameraScanning = !isCameraScanning
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_scan_qr_code"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCameraScanning) StatusOccupiedRed else HotelNavyLight,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isCameraScanning) Icons.Default.Close else Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isCameraScanning) "CERRAR CÁMARA" else "ESCANEAR QR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isCameraScanning) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CameraQrScanner(
                            onCodeDetected = { code ->
                                manualCodeInput = code
                                isCameraScanning = false
                            }
                        )
                        if (activeGeneratedCode != null && !activeGeneratedCode.isUsed && System.currentTimeMillis() <= activeGeneratedCode.expiresAtEpoch) {
                            TextButton(
                                onClick = {
                                    manualCodeInput = activeGeneratedCode.code
                                    isCameraScanning = false
                                },
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                Text(
                                    text = "Detectar QR generado (${activeGeneratedCode.code})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "INGRESAR CÓDIGO MANUALMENTE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it.uppercase() },
                        label = { Text("Código de Vinculación (ej. HR-1234)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_link_code"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = deviceNameInput,
                        onValueChange = { deviceNameInput = it },
                        label = { Text("Nombre de este dispositivo (ej. Teléfono 2)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_device_name"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Smartphone, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (manualCodeInput.isBlank()) {
                                statusMessage = "Por favor ingresa un código de vinculación válido."
                                isErrorMessage = true
                                return@Button
                            }
                            val res = onLinkDeviceWithCode(manualCodeInput, deviceNameInput)
                            if (res.isSuccess) {
                                statusMessage = "Dispositivo vinculado correctamente"
                                isErrorMessage = false
                                Toast.makeText(context, "Dispositivo vinculado correctamente", Toast.LENGTH_LONG).show()
                            } else {
                                statusMessage = res.exceptionOrNull()?.message ?: "Error al vincular el dispositivo."
                                isErrorMessage = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_confirm_link_device"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HotelGold,
                            contentColor = HotelNavyDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "VINCULAR ESTE TELÉFONO",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        if (!isErrorMessage) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusAvailableBg,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusAvailableBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusAvailableGreen,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Dispositivo vinculado correctamente",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = StatusAvailableGreen
                                        )
                                        Text(
                                            text = "Asociado a Hotel Rivera. Datos sincronizados en tiempo real mediante Firestore.",
                                            fontSize = 12.sp,
                                            color = TextDarkSecondary
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusOccupiedBg,
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, StatusOccupiedBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = StatusOccupiedRed,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = statusMessage!!,
                                        color = StatusOccupiedRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
