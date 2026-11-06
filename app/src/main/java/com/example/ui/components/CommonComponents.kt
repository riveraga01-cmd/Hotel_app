package com.example.ui.components

import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.QrCodeGenerator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConnectionStatusBar(
    status: ConnectionStatus,
    pendingSyncCount: Int = 0,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        ConnectionStatus.CONECTADO -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), Icons.Default.CloudDone)
        ConnectionStatus.SIN_CONEXION -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.CloudOff)
        ConnectionStatus.SINCRONIZANDO -> Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Icons.Default.Sync)
        ConnectionStatus.SINCRONIZACION_PENDIENTE -> Triple(Color(0xFFFFF8E1), Color(0xFFF57F17), Icons.Default.CloudQueue)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = status.label,
                    tint = textColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = status.label,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    if (pendingSyncCount > 0 && status != ConnectionStatus.CONECTADO) {
                        Text(
                            text = "$pendingSyncCount cambios pendientes de sincronizar",
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = textColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("sync_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ACTUALIZAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RoomStatusBadge(status: RoomStatus, modifier: Modifier = Modifier) {
    val (bg, fg, label) = when (status) {
        RoomStatus.DISPONIBLE -> Triple(StatusAvailableBg, StatusAvailableGreen, "DISPONIBLE")
        RoomStatus.OCUPADA -> Triple(StatusOccupiedBg, StatusOccupiedRed, "OCUPADA")
        RoomStatus.RESERVADA -> Triple(StatusReservedBg, StatusReservedAmber, "RESERVADA")
        RoomStatus.DESHABILITADA -> Triple(StatusDisabledBg, StatusDisabledGray, "DESHABILITADA")
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun CompactRoomCard(
    room: Room,
    stay: Stay? = null,
    onStartStay: () -> Unit = {},
    onAddExtraTime: () -> Unit = {},
    onReleaseRoom: () -> Unit = {},
    onEarlyCheckout: () -> Unit = {},
    onChangeRoom: () -> Unit = {},
    onDeliverAmenity: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isOccupied = room.status == RoomStatus.OCUPADA && stay != null
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(stay?.id) {
        if (isOccupied) {
            while (true) {
                currentTime = System.currentTimeMillis()
                delay(1000L)
            }
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val remainingMillis = if (stay != null) stay.scheduledCheckOutEpoch - currentTime else 0L
    val isTimeEnded = isOccupied && remainingMillis <= 0L

    val remAbs = kotlin.math.abs(remainingMillis)
    val remHours = remAbs / (1000 * 60 * 60)
    val remMinutes = (remAbs / (1000 * 60)) % 60
    val remSeconds = (remAbs / 1000) % 60

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("room_card_${room.number}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isTimeEnded) androidx.compose.foundation.BorderStroke(1.5.dp, StatusOccupiedRed)
                 else androidx.compose.foundation.BorderStroke(1.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            // Header: Number + Type + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HotelHeaderNav),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.number,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Column {
                        Text(
                            text = room.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Text(
                            text = "${room.type.name} • ${room.bedCount} cama(s)",
                            fontSize = 12.sp,
                            color = TextDarkSecondary
                        )
                    }
                }

                RoomStatusBadge(status = room.status)
            }

            // Room status dependent section
            when (room.status) {
                RoomStatus.DISPONIBLE -> {
                    if (room.features.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = room.features,
                            fontSize = 11.sp,
                            color = TextDarkSecondary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onStartStay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_start_stay_${room.number}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary,
                            contentColor = ButtonPrimaryText
                        )
                    ) {
                        Icon(imageVector = Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("OCUPAR / HOSPEDAR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                RoomStatus.OCUPADA -> {
                    if (stay != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        // Compact Guest & Factura line
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CardLightSubtle, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Huésped: ${stay.clientName.ifBlank { "Consumidor Final" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                            Text(
                                text = "Fac. #${stay.invoiceNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotelGoldDark
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Compact Countdown & Scheduled End Time
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isTimeEnded) StatusOccupiedBg else CardLightSubtle,
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isTimeEnded) Icons.Default.Warning else Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isTimeEnded) StatusOccupiedRed else TextDarkSecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTimeEnded) "VENCIDO: +${String.format(Locale.US, "%02d:%02d:%02d", remHours, remMinutes, remSeconds)}"
                                           else "Restante: ${String.format(Locale.US, "%02d:%02d:%02d", remHours, remMinutes, remSeconds)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTimeEnded) StatusOccupiedRed else TextDark
                                )
                            }

                            Text(
                                text = "Salida: ${timeFormat.format(Date(stay.scheduledCheckOutEpoch))}${if (stay.extraHours > 0) " (+${stay.extraHours}h)" else ""}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDarkSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Actions Row 1: + Tiempo Extra and Liberar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = onAddExtraTime,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_extra_time_${room.number}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonAccent,
                                    contentColor = ButtonAccentText
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AddAlarm, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Tiempo Extra", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onReleaseRoom,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("btn_release_${room.number}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ButtonConfirm,
                                    contentColor = ButtonConfirmText
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Liberar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Actions Row 2: Secondary compact actions (white bg, dark text, visible border)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = onEarlyCheckout,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("btn_early_checkout_${room.number}"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ButtonSecondaryBg,
                                    contentColor = ButtonSecondaryText
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Salida Anticipada", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = onChangeRoom,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .testTag("btn_change_room_${room.number}"),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = ButtonSecondaryBg,
                                    contentColor = ButtonSecondaryText
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Cambiar Hab.", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                RoomStatus.RESERVADA -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Habitación con reservación programada",
                        fontSize = 11.sp,
                        color = StatusReservedAmber,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStartStay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonPrimary,
                            contentColor = ButtonPrimaryText
                        )
                    ) {
                        Text("OCUPAR CON RESERVACIÓN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                RoomStatus.DESHABILITADA -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Habitación fuera de servicio / mantenimiento",
                        fontSize = 11.sp,
                        color = StatusDisabledGray
                    )
                }
            }
        }
    }
}


@Composable
fun QrCodeView(
    qrText: String,
    modifier: Modifier = Modifier,
    dimensionDp: Int = 220
) {
    val bitmap = remember(qrText) {
        QrCodeGenerator.generateQrBitmap(qrText, 400)
    }

    Box(
        modifier = modifier
            .size(dimensionDp.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, HotelNavyLight, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Código QR Hotel Rivera: $qrText",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun CameraQrScanner(
    onCodeDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            onRelease = { previewView ->
                try {
                    val cameraProvider = ProcessCameraProvider.getInstance(previewView.context).get()
                    cameraProvider.unbindAll()
                } catch (_: Exception) {}
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay scanning reticle
        Box(
            modifier = Modifier
                .size(180.dp)
                .border(2.dp, HotelGold, RoundedCornerShape(12.dp))
        )

        Text(
            text = "Apunta la cámara al código QR",
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .background(Color(0x88000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
