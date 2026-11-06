package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ConnectionStatus
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.theme.*

@Composable
fun MainHubScreen(
    connectionStatus: ConnectionStatus,
    pendingSyncCount: Int,
    onSyncClick: () -> Unit,
    onVincularDispositivoClick: () -> Unit,
    onGerenteClick: () -> Unit,
    onRecepcionClick: () -> Unit,
    currentUserEmail: String? = null,
    onSignOutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showApkDownloadDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ConnectionStatusBar(
                status = connectionStatus,
                pendingSyncCount = pendingSyncCount,
                onSyncClick = onSyncClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Branding Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hotel Logo / Emblem
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(HotelNavyDark, HotelNavyLight)
                            )
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_hotel_logo),
                        contentDescription = "Hotel Rivera Logo",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "HOTEL RIVERA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp,
                    color = HotelNavy,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Sistema de Gestión y Operaciones",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelGoldDark,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "Sincronización Multi-dispositivo en Tiempo Real",
                    fontSize = 12.sp,
                    color = TextDarkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Staff Authenticated Info & Sign Out
            if (!currentUserEmail.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 440.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CardLightSubtle,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = HotelNavy,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Personal Autenticado",
                                    fontSize = 10.sp,
                                    color = TextDarkSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = currentUserEmail,
                                    fontSize = 12.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        if (onSignOutClick != null) {
                            OutlinedButton(
                                onClick = onSignOutClick,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOccupiedRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, StatusOccupiedRed.copy(alpha = 0.5f)),
                                modifier = Modifier.testTag("btn_sign_out")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salir", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Main Primary Buttons Section (Only these 3 buttons per section 1)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // [ VINCULAR DISPOSITIVO ]
                Button(
                    onClick = onVincularDispositivoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("btn_vincular_dispositivo"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimary,
                        contentColor = ButtonPrimaryText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = ButtonAccent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "VINCULAR DISPOSITIVO",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // [ GERENTE 🔐 ]
                Button(
                    onClick = onGerenteClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("btn_gerente"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimary,
                        contentColor = ButtonPrimaryText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = ButtonAccent
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "GERENTE 🔐",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // [ RECEPCIÓN ]
                Button(
                    onClick = onRecepcionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("btn_recepcion"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonAccent,
                        contentColor = ButtonAccentText
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MeetingRoom,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint = ButtonAccentText
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "RECEPCIÓN",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Footer & APK Download Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                OutlinedButton(
                    onClick = { showApkDownloadDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = CardLightSubtle,
                        contentColor = HotelNavy
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_apk_download_info")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = HotelGoldDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Descargar APK en GitHub",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Hotel Rivera v1.0 • Todos los datos protegidos",
                    fontSize = 11.sp,
                    color = TextDarkSecondary
                )
            }
        }
    }

    if (showApkDownloadDialog) {
        val uriHandler = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { showApkDownloadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = HotelNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Descargar APK en GitHub", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "El archivo instalador APK se compila y publica automáticamente en el repositorio de GitHub:",
                        fontSize = 13.sp,
                        color = TextDark
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardLightSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("1. Sección Releases:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HotelNavy)
                            Text("En la barra lateral derecha del repositorio en GitHub, entra a 'Releases' y descarga directamente HotelRivera-v1.0.apk.", fontSize = 11.sp, color = TextDarkSecondary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardLightSubtle,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("2. Pestaña Actions:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HotelNavy)
                            Text("En la pestaña 'Actions', abre la última ejecución del flujo 'Build & Release Android APK' y descarga el APK desde 'Artifacts'.", fontSize = 11.sp, color = TextDarkSecondary)
                        }
                    }

                    Text(
                        text = "También puedes descargar el APK directamente desde el menú de opciones de Google AI Studio (Export / Download APK).",
                        fontSize = 11.sp,
                        color = TextDarkSecondary,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApkDownloadDialog = false
                        try {
                            uriHandler.openUri("https://github.com")
                        } catch (_: Exception) {}
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("ABRIR GITHUB")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApkDownloadDialog = false }) {
                    Text("CERRAR", color = TextDark)
                }
            }
        )
    }
}

