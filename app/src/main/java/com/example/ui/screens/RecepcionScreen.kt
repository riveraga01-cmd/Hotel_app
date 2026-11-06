package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.RecepcionTab
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecepcionScreen(
    currentTab: RecepcionTab,
    onTabSelected: (RecepcionTab) -> Unit,
    connectionStatus: ConnectionStatus,
    pendingSyncCount: Int,
    onSyncClick: () -> Unit,
    rooms: List<Room>,
    tariffs: List<Tariff>,
    stays: List<Stay>,
    reservations: List<Reservation>,
    inventory: List<InventoryItem>,
    extraFeeConcepts: List<ExtraFeeConcept>,
    selectedRoomForStay: Room?,
    onClearSelectedRoom: () -> Unit,
    onStartStay: (
        roomNumber: String,
        clientType: ClientType,
        clientName: String,
        tariff: Tariff,
        paymentMethod: PaymentMethod,
        amountReceived: Double,
        change: Double,
        transferRef: String,
        extraCharges: List<ExtraChargeItem>,
        productsSold: List<ExtraChargeItem>
    ) -> Invoice,
    onAddExtraTime: (stayId: String) -> Unit,
    onChangeRoom: (stayId: String, newRoomNumber: String) -> Unit,
    onReleaseRoom: (stayId: String, isEarly: Boolean) -> Unit,
    onConsumeAmenity: (itemId: String, qty: Double, roomNumber: String) -> Unit,
    onCreateReservation: (dateEpoch: Long, arrivalEpoch: Long, durationMin: Int, roomNumber: String, clientName: String, notes: String) -> Result<Reservation>,
    onModifyReservation: (reservationId: String, roomNumber: String, dateEpoch: Long, arrivalEpoch: Long, durationMin: Int, tariffTitle: String, clientName: String, notes: String) -> Result<Unit>,
    onUpdateReservationStatus: (reservationId: String, status: ReservationStatus) -> Unit,
    onOpenInvoicePdf: (Invoice) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current

    var activeCheckInRoom by remember(selectedRoomForStay) { mutableStateOf(selectedRoomForStay) }
    var prefilledClientName by remember { mutableStateOf("") }

    // Dialog states
    var stayForRoomChange by remember { mutableStateOf<Stay?>(null) }
    var stayForAmenityDelivery by remember { mutableStateOf<Stay?>(null) }
    var showCreateReservationDialog by remember { mutableStateOf(false) }
    var reservationToModify by remember { mutableStateOf<Reservation?>(null) }
    var reservationToCancel by remember { mutableStateOf<Reservation?>(null) }

    if (activeCheckInRoom != null) {
        BackHandler {
            activeCheckInRoom = null
            onClearSelectedRoom()
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "NUEVA ESTADÍA Y FACTURA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Habitación ${activeCheckInRoom!!.number} (${activeCheckInRoom!!.type.name})",
                                fontSize = 12.sp,
                                color = ButtonAccent
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                activeCheckInRoom = null
                                onClearSelectedRoom()
                            },
                            modifier = Modifier.testTag("btn_back_factura_to_habitaciones")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar a Habitaciones",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = HotelHeaderNav,
                        titleContentColor = Color.White
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                RecepcionFacturacionContent(
                    rooms = rooms,
                    tariffs = tariffs.filter { it.isActive },
                    inventory = inventory,
                    extraFeeConcepts = extraFeeConcepts.filter { it.isActive },
                    preselectedRoom = activeCheckInRoom,
                    initialClientName = prefilledClientName,
                    onClearPreselectedRoom = {
                        activeCheckInRoom = null
                        onClearSelectedRoom()
                    },
                    onSubmitStayAndInvoice = { roomNumber, clientType, clientName, tariff, payMethod, received, change, ref, extras, prods ->
                        val inv = onStartStay(roomNumber, clientType, clientName, tariff, payMethod, received, change, ref, extras, prods)
                        activeCheckInRoom = null
                        onClearSelectedRoom()
                        onOpenInvoicePdf(inv)
                        Toast.makeText(context, "Estadía y Factura N° ${inv.invoiceNumber} registradas.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                text = "RECEPCIÓN — HOTEL RIVERA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier.testTag("btn_back_recepcion")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Regresar al Menú Principal",
                                    tint = Color.White
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = HotelHeaderNav,
                            titleContentColor = Color.White
                        )
                    )

                    ConnectionStatusBar(
                        status = connectionStatus,
                        pendingSyncCount = pendingSyncCount,
                        onSyncClick = onSyncClick
                    )

                    // 2 Section Tabs ONLY: Habitaciones, Reservaciones (Requirement 4)
                    TabRow(
                        selectedTabIndex = currentTab.ordinal,
                        containerColor = HotelNavyLight,
                        contentColor = ButtonAccent
                    ) {
                        RecepcionTab.values().forEach { tab ->
                            Tab(
                                selected = currentTab == tab,
                                onClick = { onTabSelected(tab) },
                                text = {
                                    Text(
                                        text = tab.title.uppercase(),
                                        fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (currentTab == tab) ButtonAccent else TextOnDarkSecondary
                                    )
                                },
                                icon = {
                                    when (tab) {
                                        RecepcionTab.HABITACIONES -> Icon(Icons.Default.Hotel, contentDescription = null, modifier = Modifier.size(18.dp))
                                        RecepcionTab.RESERVACIONES -> Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    RecepcionTab.HABITACIONES -> {
                        RecepcionHabitacionesContent(
                            rooms = rooms,
                            stays = stays,
                            onRoomClick = { room ->
                                if (room.status == RoomStatus.DISPONIBLE || room.status == RoomStatus.RESERVADA) {
                                    prefilledClientName = ""
                                    activeCheckInRoom = room
                                }
                            },
                            onAddExtraTime = onAddExtraTime,
                            onChangeRoomClick = { stay -> stayForRoomChange = stay },
                            onReleaseRoom = { stay ->
                                onReleaseRoom(stay.id, false)
                                Toast.makeText(context, "Habitación ${stay.roomNumber} liberada. Estado: Disponible.", Toast.LENGTH_SHORT).show()
                            },
                            onEarlyCheckout = { stay ->
                                onReleaseRoom(stay.id, true)
                                Toast.makeText(context, "Salida anticipada registrada. Habitación ${stay.roomNumber} liberada.", Toast.LENGTH_SHORT).show()
                            },
                            onDeliverAmenityClick = { stay -> stayForAmenityDelivery = stay }
                        )
                    }

                    RecepcionTab.RESERVACIONES -> {
                        RecepcionReservacionesContent(
                            reservations = reservations,
                            rooms = rooms,
                            stays = stays,
                            onCreateClick = { showCreateReservationDialog = true },
                            onModifyClick = { reservationToModify = it },
                            onCancelClick = { reservationToCancel = it },
                            onUpdateStatus = onUpdateReservationStatus,
                            onCheckInFromReservation = { reservation ->
                                val targetRoom = rooms.find { it.number == reservation.roomNumber }
                                if (targetRoom != null && (targetRoom.status == RoomStatus.DISPONIBLE || targetRoom.status == RoomStatus.RESERVADA)) {
                                    prefilledClientName = reservation.clientName
                                    activeCheckInRoom = targetRoom
                                } else {
                                    Toast.makeText(context, "La habitación ${reservation.roomNumber} no está disponible actualmente.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }


    // Modal: Cambiar Habitación
    if (stayForRoomChange != null) {
        val availableRooms = rooms.filter { it.status == RoomStatus.DISPONIBLE }
        var selectedTargetRoom by remember { mutableStateOf(availableRooms.firstOrNull()?.number ?: "") }

        AlertDialog(
            onDismissRequest = { stayForRoomChange = null },
            title = { Text("Cambiar Habitación", fontWeight = FontWeight.Bold, color = HotelNavy) },
            text = {
                Column {
                    Text(
                        text = "Estadía actual en Hab. ${stayForRoomChange!!.roomNumber} (Factura ${stayForRoomChange!!.invoiceNumber}).",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Selecciona la nueva habitación disponible:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (availableRooms.isEmpty()) {
                        Text(
                            text = "No hay otras habitaciones disponibles en este momento.",
                            color = StatusOccupiedRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        availableRooms.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTargetRoom = r.number }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedTargetRoom == r.number,
                                    onClick = { selectedTargetRoom = r.number }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Habitación ${r.number} (${r.type}, ${r.bedCount} camas)")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedTargetRoom.isNotBlank()) {
                            onChangeRoom(stayForRoomChange!!.id, selectedTargetRoom)
                            stayForRoomChange = null
                            Toast.makeText(context, "Habitación cambiada exitosamente.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = availableRooms.isNotEmpty() && selectedTargetRoom.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("CONFIRMAR CAMBIO")
                }
            },
            dismissButton = {
                TextButton(onClick = { stayForRoomChange = null }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    // Modal: Entregar Amenities Gratuitos a Habitación
    if (stayForAmenityDelivery != null) {
        val amenitiesList = inventory.filter { it.isAmenity && it.isActive && it.quantity > 0 }
        var selectedAmenityId by remember { mutableStateOf(amenitiesList.firstOrNull()?.id ?: "") }
        var amenityQty by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { stayForAmenityDelivery = null },
            title = { Text("Entregar Amenities a Hab. ${stayForAmenityDelivery!!.roomNumber}", fontWeight = FontWeight.Bold, color = HotelNavy) },
            text = {
                Column {
                    Text(
                        text = "Los amenities son productos gratuitos entregados al huésped. No se cobran en la factura y reducen existencias del inventario.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (amenitiesList.isEmpty()) {
                        Text("No hay amenities con existencias en el inventario.", color = StatusOccupiedRed, fontSize = 12.sp)
                    } else {
                        amenitiesList.forEach { am ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAmenityId = am.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedAmenityId == am.id, onClick = { selectedAmenityId = am.id })
                                Text("${am.name} (Disponibles: ${am.quantity.toInt()} ${am.unit})", fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = amenityQty,
                            onValueChange = { amenityQty = it },
                            label = { Text("Cantidad a entregar") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = amenityQty.toDoubleOrNull() ?: 1.0
                        if (selectedAmenityId.isNotBlank()) {
                            onConsumeAmenity(selectedAmenityId, qty, stayForAmenityDelivery!!.roomNumber)
                            stayForAmenityDelivery = null
                            Toast.makeText(context, "Amenity entregado y descontado de inventario.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = amenitiesList.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("REGISTRAR ENTREGA")
                }
            },
            dismissButton = {
                TextButton(onClick = { stayForAmenityDelivery = null }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    // Modal: Crear Reservación con Calendario y Selector de Hora
    if (showCreateReservationDialog) {
        CreateReservationDialog(
            rooms = rooms,
            reservations = reservations,
            stays = stays,
            initialDateEpoch = System.currentTimeMillis(),
            onDismiss = { showCreateReservationDialog = false },
            onConfirm = { dateEpoch, arrivalEpoch, durationMin, roomNum, clientName, notes ->
                val res = onCreateReservation(dateEpoch, arrivalEpoch, durationMin, roomNum, clientName, notes)
                if (res.isSuccess) {
                    showCreateReservationDialog = false
                    Toast.makeText(context, "Reservación creada exitosamente para Hab. $roomNum.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Error al crear reservación", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Modal: Modificar Reservación
    if (reservationToModify != null) {
        ModifyReservationDialog(
            reservation = reservationToModify!!,
            rooms = rooms,
            reservations = reservations,
            stays = stays,
            onDismiss = { reservationToModify = null },
            onConfirm = { resId, roomNum, dateEpoch, arrivalEpoch, durationMin, title, clientName, notes ->
                val res = onModifyReservation(resId, roomNum, dateEpoch, arrivalEpoch, durationMin, title, clientName, notes)
                if (res.isSuccess) {
                    reservationToModify = null
                    Toast.makeText(context, "Reservación modificada exitosamente.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Error al modificar reservación", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    // Modal: Confirmar Cancelación de Reservación
    if (reservationToCancel != null) {
        AlertDialog(
            onDismissRequest = { reservationToCancel = null },
            title = { Text("Cancelar Reservación", fontWeight = FontWeight.Bold, color = StatusOccupiedRed) },
            text = {
                Text(
                    text = "¿Estás seguro de cancelar la reservación ${reservationToCancel!!.reservationNumber} de la Habitación ${reservationToCancel!!.roomNumber}?\n\nLa habitación volverá a estar Disponible si no tiene otras reservaciones o estadías activas.",
                    color = TextDark,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = reservationToCancel!!.roomNumber
                        onUpdateReservationStatus(reservationToCancel!!.id, ReservationStatus.CANCELADA)
                        reservationToCancel = null
                        Toast.makeText(context, "Reservación cancelada. Habitación $num liberada.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOccupiedRed)
                ) {
                    Text("SÍ, CANCELAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { reservationToCancel = null }) {
                    Text("NO, VOLVER", color = TextDark)
                }
            }
        )
    }
}

@Composable
fun RecepcionHabitacionesContent(
    rooms: List<Room>,
    stays: List<Stay>,
    onRoomClick: (Room) -> Unit,
    onAddExtraTime: (stayId: String) -> Unit,
    onChangeRoomClick: (Stay) -> Unit,
    onReleaseRoom: (Stay) -> Unit,
    onEarlyCheckout: (Stay) -> Unit,
    onDeliverAmenityClick: (Stay) -> Unit
) {
    val activeStaysByRoom = remember(stays) {
        stays.filter { it.status == StayStatus.ACTIVE }.associateBy { it.roomNumber }
    }

    val availableRooms = remember(rooms) { rooms.filter { it.status == RoomStatus.DISPONIBLE } }
    val occupiedRooms = remember(rooms) { rooms.filter { it.status == RoomStatus.OCUPADA } }
    val reservedRooms = remember(rooms) { rooms.filter { it.status == RoomStatus.RESERVADA } }
    val disabledRooms = remember(rooms) { rooms.filter { it.status == RoomStatus.DESHABILITADA } }

    var filterState by remember { mutableStateOf("TODAS") }

    val filteredRooms = remember(rooms, filterState) {
        when (filterState) {
            "DISPONIBLES" -> availableRooms
            "OCUPADAS" -> occupiedRooms
            "RESERVADAS" -> reservedRooms
            "DESHABILITADAS" -> disabledRooms
            else -> rooms
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val minCardWidth = if (maxWidth >= 950.dp) 320.dp else 290.dp

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCardWidth),
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary Counter Header (Full width across all grid columns)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusMiniChip(
                        label = "DISPONIBLES",
                        count = availableRooms.size,
                        color = StatusAvailableGreen,
                        bg = StatusAvailableBg,
                        modifier = Modifier.weight(1f).clickable { filterState = if (filterState == "DISPONIBLES") "TODAS" else "DISPONIBLES" }
                    )
                    StatusMiniChip(
                        label = "OCUPADAS",
                        count = occupiedRooms.size,
                        color = StatusOccupiedRed,
                        bg = StatusOccupiedBg,
                        modifier = Modifier.weight(1f).clickable { filterState = if (filterState == "OCUPADAS") "TODAS" else "OCUPADAS" }
                    )
                    StatusMiniChip(
                        label = "RESERVADAS",
                        count = reservedRooms.size,
                        color = StatusReservedAmber,
                        bg = StatusReservedBg,
                        modifier = Modifier.weight(1f).clickable { filterState = if (filterState == "RESERVADAS") "TODAS" else "RESERVADAS" }
                    )
                    StatusMiniChip(
                        label = "DESHABILITADAS",
                        count = disabledRooms.size,
                        color = StatusDisabledGray,
                        bg = StatusDisabledBg,
                        modifier = Modifier.weight(1f).clickable { filterState = if (filterState == "DESHABILITADAS") "TODAS" else "DESHABILITADAS" }
                    )
                }
            }

            if (filterState != "TODAS") {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtro activo: $filterState (${filteredRooms.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ButtonAccent
                        )
                        TextButton(onClick = { filterState = "TODAS" }) {
                            Text("Ver todas", fontSize = 11.sp, color = TextLightSecondary)
                        }
                    }
                }
            }

            if (filteredRooms.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardLight)
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No hay habitaciones en esta categoría.", fontSize = 13.sp, color = TextDarkSecondary)
                        }
                    }
                }
            } else {
                items(filteredRooms) { room ->
                    val activeStay = activeStaysByRoom[room.number]
                    CompactRoomCard(
                        room = room,
                        stay = activeStay,
                        onStartStay = { onRoomClick(room) },
                        onAddExtraTime = { if (activeStay != null) onAddExtraTime(activeStay.id) },
                        onChangeRoom = { if (activeStay != null) onChangeRoomClick(activeStay) },
                        onReleaseRoom = { if (activeStay != null) onReleaseRoom(activeStay) },
                        onEarlyCheckout = { if (activeStay != null) onEarlyCheckout(activeStay) },
                        onDeliverAmenity = { if (activeStay != null) onDeliverAmenityClick(activeStay) }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusMiniChip(label: String, count: Int, color: Color, bg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$count", fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = label, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}


@Composable
fun RecepcionFacturacionContent(
    rooms: List<Room>,
    tariffs: List<Tariff>,
    inventory: List<InventoryItem>,
    extraFeeConcepts: List<ExtraFeeConcept>,
    preselectedRoom: Room?,
    initialClientName: String = "",
    onClearPreselectedRoom: () -> Unit,
    onSubmitStayAndInvoice: (
        roomNumber: String,
        clientType: ClientType,
        clientName: String,
        tariff: Tariff,
        paymentMethod: PaymentMethod,
        amountReceived: Double,
        change: Double,
        transferRef: String,
        extraCharges: List<ExtraChargeItem>,
        productsSold: List<ExtraChargeItem>
    ) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val availableRooms = remember(rooms) { rooms.filter { it.status == RoomStatus.DISPONIBLE } }

    var selectedRoomNumber by remember(preselectedRoom) {
        mutableStateOf(preselectedRoom?.number ?: availableRooms.firstOrNull()?.number ?: "")
    }

    var clientType by remember(initialClientName) {
        mutableStateOf(if (initialClientName.isNotBlank()) ClientType.CLIENTE else ClientType.CONSUMIDOR_FINAL)
    }
    var clientName by remember(initialClientName) {
        mutableStateOf(initialClientName)
    }

    var selectedTariff by remember(tariffs) { mutableStateOf(tariffs.firstOrNull()) }


    var paymentMethod by remember { mutableStateOf(PaymentMethod.EFECTIVO) }
    var amountReceivedText by remember { mutableStateOf("") }
    var transferRefText by remember { mutableStateOf("") }

    // Extras & products lists
    var extraChargesList by remember { mutableStateOf(listOf<ExtraChargeItem>()) }
    var productsSoldList by remember { mutableStateOf(listOf<ExtraChargeItem>()) }

    // Dialog state for adding extra charge or product
    var showAddExtraFeeDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }

    val basePrice = selectedTariff?.price ?: 0.0
    val extrasSum = extraChargesList.sumOf { it.total }
    val prodsSum = productsSoldList.sumOf { it.total }
    val grandTotal = basePrice + extrasSum + prodsSum

    val amountReceived = amountReceivedText.toDoubleOrNull() ?: grandTotal
    val changeCalculated = (amountReceived - grandTotal).coerceAtLeast(0.0)

    val currentNow = remember { System.currentTimeMillis() }
    val scheduledCheckout = remember(selectedTariff, currentNow) {
        currentNow + ((selectedTariff?.durationMinutes ?: 60) * 60 * 1000L)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 840.dp)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Section Title
        Text(
            text = "REGISTRAR NUEVA ESTADÍA Y FACTURA",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HotelNavy,
            letterSpacing = 1.sp
        )

        // Room selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. SELECCIONAR HABITACIÓN DISPONIBLE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(10.dp))

                if (availableRooms.isEmpty()) {
                    Text("No hay habitaciones disponibles.", color = StatusOccupiedRed, fontWeight = FontWeight.Bold)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        availableRooms.forEach { r ->
                            val isSel = selectedRoomNumber == r.number
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) HotelNavy else Color(0xFFF4F6F8))
                                    .border(if (isSel) 2.dp else 1.dp, if (isSel) HotelGold else Color.LightGray, RoundedCornerShape(10.dp))
                                    .clickable { selectedRoomNumber = r.number }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Hab. ${r.number}",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextDark,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = r.type.name,
                                        fontSize = 10.sp,
                                        color = if (isSel) HotelGoldLight else TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Client Type Selector
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("2. TIPO DE CLIENTE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { clientType = ClientType.CONSUMIDOR_FINAL },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_client_consumidor_final"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (clientType == ClientType.CONSUMIDOR_FINAL) HotelNavy else Color(0xFFE6EBF0),
                            contentColor = if (clientType == ClientType.CONSUMIDOR_FINAL) Color.White else TextDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("CONSUMIDOR FINAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { clientType = ClientType.CLIENTE },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_client_nominado"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (clientType == ClientType.CLIENTE) HotelNavy else Color(0xFFE6EBF0),
                            contentColor = if (clientType == ClientType.CLIENTE) Color.White else TextDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("CLIENTE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (clientType == ClientType.CLIENTE) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre del cliente") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_client_name"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Tariff / Duration Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("3. DURACIÓN / TARIFA", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(10.dp))

                tariffs.forEach { tf ->
                    val isSel = selectedTariff?.id == tf.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFFFFF9E6) else Color.Transparent)
                            .border(1.dp, if (isSel) HotelGold else Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                            .clickable { selectedTariff = tf }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("tariff_option_${tf.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = isSel, onClick = { selectedTariff = tf })
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(tf.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HotelNavy)
                                Text("${tf.durationMinutes} minutos", fontSize = 11.sp, color = TextMuted)
                            }
                        }
                        Text("Q${tf.price}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HotelGoldDark)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Automatic time calculated
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF0F4F8), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hora entrada (actual): ${timeFormat.format(Date(currentNow))}", fontSize = 12.sp, color = HotelNavy)
                    Text("Salida prevista: ${timeFormat.format(Date(scheduledCheckout))}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HotelGoldDark)
                }
            }
        }

        // Extras / Products Itemization (Optional)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4. EXTRAS Y PRODUCTOS (OPCIONAL)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showAddProductDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Venta Producto", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showAddExtraFeeDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Cobro Extra", fontSize = 11.sp)
                    }
                }

                // List added products
                if (productsSoldList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Productos a cobrar:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    productsSoldList.forEachIndexed { idx, p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${p.quantity}x ${p.concept}", fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Q${p.total}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Quitar",
                                    tint = StatusOccupiedRed,
                                    modifier = Modifier.size(16.dp).clickable {
                                        productsSoldList = productsSoldList.filterIndexed { i, _ -> i != idx }
                                    }
                                )
                            }
                        }
                    }
                }

                // List added extra fees
                if (extraChargesList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cobros extras / daños:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    extraChargesList.forEachIndexed { idx, e ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${e.concept} (${e.specification})", fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Q${e.total}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Quitar",
                                    tint = StatusOccupiedRed,
                                    modifier = Modifier.size(16.dp).clickable {
                                        extraChargesList = extraChargesList.filterIndexed { i, _ -> i != idx }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Payment Method & Total
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("5. FORMA DE PAGO Y TOTAL", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { paymentMethod = PaymentMethod.EFECTIVO },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == PaymentMethod.EFECTIVO) HotelGoldDark else Color(0xFFE6EBF0),
                            contentColor = if (paymentMethod == PaymentMethod.EFECTIVO) Color.White else TextDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("EFECTIVO", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { paymentMethod = PaymentMethod.TRANSFERENCIA },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentMethod == PaymentMethod.TRANSFERENCIA) HotelNavy else Color(0xFFE6EBF0),
                            contentColor = if (paymentMethod == PaymentMethod.TRANSFERENCIA) Color.White else TextDark
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("TRANSFERENCIA", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (paymentMethod == PaymentMethod.EFECTIVO) {
                    OutlinedTextField(
                        value = amountReceivedText,
                        onValueChange = { amountReceivedText = it },
                        label = { Text("Monto Recibido (Q)") },
                        placeholder = { Text(String.format(Locale.US, "%.2f", grandTotal)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("input_amount_received"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cambio a devolver:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = String.format(Locale.US, "Q%.2f", changeCalculated),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = StatusAvailableGreen
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = transferRefText,
                        onValueChange = { transferRefText = it },
                        label = { Text("Referencia de Transferencia (Opcional)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_transfer_reference"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL A COBRAR:", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HotelNavy)
                    Text(
                        text = String.format(Locale.US, "Q%.2f", grandTotal),
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = HotelGoldDark
                    )
                }
            }
        }

        // Submit Button
        Button(
            onClick = {
                val tariff = selectedTariff
                if (tariff == null || selectedRoomNumber.isBlank()) return@Button

                val rec = if (paymentMethod == PaymentMethod.EFECTIVO) {
                    amountReceivedText.toDoubleOrNull() ?: grandTotal
                } else grandTotal

                val ch = if (paymentMethod == PaymentMethod.EFECTIVO) (rec - grandTotal).coerceAtLeast(0.0) else 0.0

                onSubmitStayAndInvoice(
                    selectedRoomNumber,
                    clientType,
                    clientName,
                    tariff,
                    paymentMethod,
                    rec,
                    ch,
                    transferRefText,
                    extraChargesList,
                    productsSoldList
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("btn_submit_stay_and_invoice"),
            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            enabled = availableRooms.isNotEmpty() && selectedTariff != null
        ) {
            Icon(Icons.Default.Receipt, contentDescription = null, tint = HotelGold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("REGISTRAR ESTADÍA Y GENERAR FACTURA", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

    // Modal: Add Product to Invoice
    if (showAddProductDialog) {
        val saleProducts = inventory.filter { !it.isAmenity && it.isActive && it.quantity > 0 }
        var selectedProduct by remember { mutableStateOf(saleProducts.firstOrNull()) }
        var productQty by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Agregar Producto a Factura", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (saleProducts.isEmpty()) {
                        Text("No hay productos para venta con existencias en inventario.")
                    } else {
                        saleProducts.forEach { p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedProduct = p }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedProduct?.id == p.id, onClick = { selectedProduct = p })
                                Text("${p.name} — Q${p.salePrice} (Disp: ${p.quantity.toInt()})", fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = productQty,
                            onValueChange = { productQty = it },
                            label = { Text("Cantidad") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val prod = selectedProduct
                        val q = productQty.toIntOrNull() ?: 1
                        if (prod != null && q > 0) {
                            productsSoldList = productsSoldList + ExtraChargeItem(
                                id = "prod_${java.util.UUID.randomUUID()}",
                                concept = prod.name,
                                quantity = q,
                                specification = "${prod.quantity.toInt()} en stock",
                                unitPrice = prod.salePrice,
                                total = prod.salePrice * q,
                                inventoryItemId = prod.id
                            )
                            showAddProductDialog = false
                        }
                    },
                    enabled = saleProducts.isNotEmpty()
                ) {
                    Text("AGREGAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    // Modal: Add Extra Fee / Damage Concept
    if (showAddExtraFeeDialog) {
        var selectedConcept by remember { mutableStateOf(extraFeeConcepts.firstOrNull()) }
        var feeQty by remember { mutableStateOf("1") }
        var feeSpec by remember { mutableStateOf("") }
        var customPrice by remember { mutableStateOf(selectedConcept?.defaultPrice?.toString() ?: "50") }

        AlertDialog(
            onDismissRequest = { showAddExtraFeeDialog = false },
            title = { Text("Cobro Extra / Daño", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    extraFeeConcepts.forEach { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedConcept = c
                                    customPrice = c.defaultPrice.toString()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedConcept?.id == c.id, onClick = {
                                selectedConcept = c
                                customPrice = c.defaultPrice.toString()
                            })
                            Text("${c.name} (Q${c.defaultPrice})", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feeSpec,
                        onValueChange = { feeSpec = it },
                        label = { Text("Especificación / Detalle (ej. Toalla rota)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        label = { Text("Precio Unitario (Q)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val c = selectedConcept
                        val q = feeQty.toIntOrNull() ?: 1
                        val p = customPrice.toDoubleOrNull() ?: c?.defaultPrice ?: 0.0
                        if (c != null) {
                            extraChargesList = extraChargesList + ExtraChargeItem(
                                id = "fee_${java.util.UUID.randomUUID()}",
                                concept = c.name,
                                quantity = q,
                                specification = feeSpec,
                                unitPrice = p,
                                total = p * q
                            )
                            showAddExtraFeeDialog = false
                        }
                    }
                ) {
                    Text("AGREGAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExtraFeeDialog = false }) { Text("CANCELAR") }
            }
        )
    }
}

// --- RESERVACIONES & CALENDARIO INTERACTIVO ---

enum class CalendarViewMode(val label: String) {
    DIA("Día"),
    SEMANA("Semana"),
    MES("Mes")
}

@Composable
fun RecepcionReservacionesContent(
    reservations: List<Reservation>,
    rooms: List<Room>,
    stays: List<Stay>,
    onCreateClick: () -> Unit,
    onModifyClick: (Reservation) -> Unit,
    onCancelClick: (Reservation) -> Unit,
    onUpdateStatus: (String, ReservationStatus) -> Unit,
    onCheckInFromReservation: (Reservation) -> Unit = {}
) {
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
    val timeOnlyFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dayHeaderFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")) }
    val shortDayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    var selectedCalendarDateEpoch by remember { mutableStateOf(System.currentTimeMillis()) }
    var calendarViewMode by remember { mutableStateOf(CalendarViewMode.MES) }
    var showOnlySelectedDate by remember { mutableStateOf(true) }
    var filterStatus by remember { mutableStateOf("TODAS") }

    val selCal = remember(selectedCalendarDateEpoch) {
        Calendar.getInstance().apply { timeInMillis = selectedCalendarDateEpoch }
    }
    val selYear = selCal.get(Calendar.YEAR)
    val selDayOfYear = selCal.get(Calendar.DAY_OF_YEAR)

    // Filter reservations by selected date or all dates, plus status filter
    val filteredList = remember(reservations, selectedCalendarDateEpoch, showOnlySelectedDate, filterStatus) {
        val baseList = if (showOnlySelectedDate) {
            reservations.filter { res ->
                val resCal = Calendar.getInstance().apply { timeInMillis = res.arrivalTimeEpoch }
                resCal.get(Calendar.YEAR) == selYear && resCal.get(Calendar.DAY_OF_YEAR) == selDayOfYear
            }
        } else {
            reservations
        }
        if (filterStatus == "TODAS") baseList.sortedBy { it.arrivalTimeEpoch }
        else baseList.filter { it.status.name == filterStatus }.sortedBy { it.arrivalTimeEpoch }
    }

    val totalActiveCount = remember(reservations) {
        reservations.count { it.status == ReservationStatus.PENDIENTE || it.status == ReservationStatus.CONFIRMADA }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth >= 720.dp

        if (isTabletLandscape) {
            // TABLET LANDSCAPE: 2-PANE HORIZONTAL LAYOUT
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // LEFT PANE: Calendar & Header (Scrollable)
                Column(
                    modifier = Modifier
                        .weight(0.48f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header with Title & Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RESERVACIONES",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HotelNavy
                            )
                            Text(
                                text = "$totalActiveCount activas • ${reservations.size} en total",
                                fontSize = 12.sp,
                                color = TextDarkSecondary
                            )
                        }

                        Button(
                            onClick = onCreateClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonAccent,
                                contentColor = ButtonAccentText
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_new_reservation_tablet")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NUEVA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Interactive Calendar
                    ReservationCalendarView(
                        selectedDateEpoch = selectedCalendarDateEpoch,
                        onDateSelected = { newEpoch ->
                            selectedCalendarDateEpoch = newEpoch
                            showOnlySelectedDate = true
                        },
                        reservations = reservations,
                        viewMode = calendarViewMode,
                        onViewModeChanged = { calendarViewMode = it }
                    )
                }

                // RIGHT PANE: Selected Day Filter & Reservation Cards
                Column(
                    modifier = Modifier
                        .weight(0.52f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardLightSubtle),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (showOnlySelectedDate) "Fecha: ${dayHeaderFormat.format(Date(selectedCalendarDateEpoch)).replaceFirstChar { it.uppercase() }}"
                                           else "Mostrando: Todas las fechas (${reservations.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelNavy
                                )
                                Text(
                                    text = "${filteredList.size} reservación(es) encontrada(s)",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                            }

                            OutlinedButton(
                                onClick = { showOnlySelectedDate = !showOnlySelectedDate },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!showOnlySelectedDate) HotelNavy else ButtonSecondaryBg,
                                    contentColor = if (!showOnlySelectedDate) Color.White else TextDark
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                            ) {
                                Icon(
                                    imageVector = if (showOnlySelectedDate) Icons.AutoMirrored.Filled.EventNote else Icons.Default.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showOnlySelectedDate) "Ver todas" else "Ver solo este día",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Filter chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("TODAS", "PENDIENTE", "CONFIRMADA", "COMPLETADA", "CANCELADA").forEach { st ->
                            val isSel = filterStatus == st
                            val count = if (showOnlySelectedDate) {
                                reservations.count { res ->
                                    val rCal = Calendar.getInstance().apply { timeInMillis = res.arrivalTimeEpoch }
                                    val onDay = rCal.get(Calendar.YEAR) == selYear && rCal.get(Calendar.DAY_OF_YEAR) == selDayOfYear
                                    onDay && (st == "TODAS" || res.status.name == st)
                                }
                            } else {
                                if (st == "TODAS") reservations.size else reservations.count { it.status.name == st }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ButtonPrimary else CardLight)
                                    .border(1.dp, if (isSel) ButtonPrimary else BorderMedium, RoundedCornerShape(8.dp))
                                    .clickable { filterStatus = st }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (count > 0) "$st ($count)" else st,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else TextDark
                                )
                            }
                        }
                    }

                    // Cards list in right pane
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filteredList.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = CardLight),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                                ) {
                                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextDarkSecondary, modifier = Modifier.size(36.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = if (showOnlySelectedDate) "No hay reservaciones para ${shortDayFormat.format(Date(selectedCalendarDateEpoch))}."
                                                       else "No hay reservaciones registradas con el filtro seleccionado.",
                                                fontSize = 13.sp,
                                                color = TextDarkSecondary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(filteredList) { res ->
                                ReservationCardItem(
                                    res = res,
                                    targetRoom = rooms.find { it.number == res.roomNumber },
                                    onCheckInFromReservation = onCheckInFromReservation,
                                    onModifyClick = onModifyClick,
                                    onCancelClick = onCancelClick,
                                    onUpdateStatus = onUpdateStatus
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // PHONE PORTRAIT: SINGLE VERTICAL FLOW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Top Section Header with Title and Create Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RESERVACIONES",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HotelNavy
                            )
                            Text(
                                text = "$totalActiveCount activas • ${reservations.size} en total",
                                fontSize = 12.sp,
                                color = TextDarkSecondary
                            )
                        }

                        Button(
                            onClick = onCreateClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ButtonAccent,
                                contentColor = ButtonAccentText
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_new_reservation")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NUEVA RESERVACIÓN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 2. Functional Interactive Calendar
                item {
                    ReservationCalendarView(
                        selectedDateEpoch = selectedCalendarDateEpoch,
                        onDateSelected = { newEpoch ->
                            selectedCalendarDateEpoch = newEpoch
                            showOnlySelectedDate = true
                        },
                        reservations = reservations,
                        viewMode = calendarViewMode,
                        onViewModeChanged = { calendarViewMode = it }
                    )
                }

                // 3. Date Selection Bar & View Mode Toggle
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardLightSubtle),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (showOnlySelectedDate) "Fecha: ${dayHeaderFormat.format(Date(selectedCalendarDateEpoch)).replaceFirstChar { it.uppercase() }}"
                                           else "Mostrando: Todas las fechas (${reservations.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HotelNavy
                                )
                                Text(
                                    text = "${filteredList.size} reservación(es) encontrada(s)",
                                    fontSize = 11.sp,
                                    color = TextDarkSecondary
                                )
                            }

                            OutlinedButton(
                                onClick = { showOnlySelectedDate = !showOnlySelectedDate },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!showOnlySelectedDate) HotelNavy else ButtonSecondaryBg,
                                    contentColor = if (!showOnlySelectedDate) Color.White else TextDark
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                            ) {
                                Icon(
                                    imageVector = if (showOnlySelectedDate) Icons.AutoMirrored.Filled.EventNote else Icons.Default.Today,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (showOnlySelectedDate) "Ver todas" else "Ver solo este día",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // 4. Status Filter Chips
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("TODAS", "PENDIENTE", "CONFIRMADA", "COMPLETADA", "CANCELADA").forEach { st ->
                            val isSel = filterStatus == st
                            val count = if (showOnlySelectedDate) {
                                reservations.count { res ->
                                    val rCal = Calendar.getInstance().apply { timeInMillis = res.arrivalTimeEpoch }
                                    val onDay = rCal.get(Calendar.YEAR) == selYear && rCal.get(Calendar.DAY_OF_YEAR) == selDayOfYear
                                    onDay && (st == "TODAS" || res.status.name == st)
                                }
                            } else {
                                if (st == "TODAS") reservations.size else reservations.count { it.status.name == st }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ButtonPrimary else CardLight)
                                    .border(1.dp, if (isSel) ButtonPrimary else BorderMedium, RoundedCornerShape(8.dp))
                                    .clickable { filterStatus = st }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (count > 0) "$st ($count)" else st,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else TextDark
                                )
                            }
                        }
                    }
                }

                // 5. Reservation Cards
                if (filteredList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardLight),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                        ) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = TextDarkSecondary, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (showOnlySelectedDate) "No hay reservaciones para ${shortDayFormat.format(Date(selectedCalendarDateEpoch))}."
                                               else "No hay reservaciones registradas con el filtro seleccionado.",
                                        fontSize = 13.sp,
                                        color = TextDarkSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredList) { res ->
                        ReservationCardItem(
                            res = res,
                            targetRoom = rooms.find { it.number == res.roomNumber },
                            onCheckInFromReservation = onCheckInFromReservation,
                            onModifyClick = onModifyClick,
                            onCancelClick = onCancelClick,
                            onUpdateStatus = onUpdateStatus
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationCardItem(
    res: Reservation,
    targetRoom: Room?,
    onCheckInFromReservation: (Reservation) -> Unit,
    onModifyClick: (Reservation) -> Unit,
    onCancelClick: (Reservation) -> Unit,
    onUpdateStatus: (String, ReservationStatus) -> Unit
) {
    val dateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }
    val timeOnlyFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val departureEpoch = res.arrivalTimeEpoch + (res.durationMinutes * 60 * 1000L)
    val isUpcomingOrPending = res.status == ReservationStatus.PENDIENTE || res.status == ReservationStatus.CONFIRMADA

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card Header: Room Number + Type + Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(HotelNavy),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = res.roomNumber,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Habitación ${res.roomNumber}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        if (targetRoom != null) {
                            Text(
                                text = "${targetRoom.type.name} • ${targetRoom.bedCount} cama(s)",
                                fontSize = 11.sp,
                                color = TextDarkSecondary
                            )
                        }
                    }
                }

                ReservationBadge(status = res.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Guest name and Reservation number
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardLightSubtle, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = TextDarkSecondary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = res.clientName.ifBlank { "Consumidor Final" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
                Text(
                    text = res.reservationNumber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HotelGoldDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arrival, Duration and Calculated Departure
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardLightSubtle, RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = HotelNavy)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Llegada: ${dateTimeFormat.format(Date(res.arrivalTimeEpoch))}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextDarkSecondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Duración: ${res.durationMinutes / 60}h ${res.durationMinutes % 60}m (${res.tariffTitle})",
                            fontSize = 12.sp,
                            color = TextDarkSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(14.dp), tint = StatusOccupiedRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Salida: ${timeOnlyFormat.format(Date(departureEpoch))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusOccupiedRed
                        )
                    }
                }
            }

            if (res.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Notas: ${res.notes}",
                    fontSize = 11.sp,
                    color = TextDarkSecondary,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: HOSPEDAR, MODIFICAR, CONFIRMAR, CANCELAR
            if (isUpcomingOrPending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // [ HOSPEDAR ]
                    Button(
                        onClick = { onCheckInFromReservation(res) },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ButtonAccent,
                            contentColor = ButtonAccentText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("HOSPEDAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // [ MODIFICAR ]
                    OutlinedButton(
                        onClick = { onModifyClick(res) },
                        modifier = Modifier
                            .weight(1.1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ButtonSecondaryBg,
                            contentColor = ButtonSecondaryText
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Modificar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    if (res.status == ReservationStatus.PENDIENTE) {
                        // [ CONFIRMAR ]
                        OutlinedButton(
                            onClick = { onUpdateStatus(res.id, ReservationStatus.CONFIRMADA) },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusAvailableGreen),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = StatusAvailableBg,
                                contentColor = StatusAvailableGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Confirmar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // [ CANCELAR ]
                    OutlinedButton(
                        onClick = { onCancelClick(res) },
                        modifier = Modifier
                            .weight(1.0f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StatusOccupiedRed),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StatusOccupiedBg,
                            contentColor = StatusOccupiedRed
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Cancelar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Functional interactive calendar supporting Month, Week, and Day views.
 */
@Composable
fun ReservationCalendarView(
    selectedDateEpoch: Long,
    onDateSelected: (Long) -> Unit,
    reservations: List<Reservation>,
    viewMode: CalendarViewMode,
    onViewModeChanged: (CalendarViewMode) -> Unit
) {
    var viewingMonthCal by remember(selectedDateEpoch) {
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = selectedDateEpoch })
    }

    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-ES")) }
    val dayOnlyFormat = remember { SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("es-ES")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardLight),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // View Mode Selector: [ DÍA ] [ SEMANA ] [ MES ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardLightSubtle)
                    .border(1.dp, BorderLight, RoundedCornerShape(8.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CalendarViewMode.values().forEach { mode ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) HotelNavy else Color.Transparent)
                            .clickable { onViewModeChanged(mode) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label.uppercase(),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextDarkSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Bar: Prev, Header Label, Today, Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val newC = (viewingMonthCal.clone() as Calendar).apply {
                            when (viewMode) {
                                CalendarViewMode.MES -> add(Calendar.MONTH, -1)
                                CalendarViewMode.SEMANA -> add(Calendar.WEEK_OF_YEAR, -1)
                                CalendarViewMode.DIA -> add(Calendar.DAY_OF_YEAR, -1)
                            }
                        }
                        viewingMonthCal = newC
                        onDateSelected(newC.timeInMillis)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Anterior", tint = HotelNavy)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when (viewMode) {
                            CalendarViewMode.MES -> monthYearFormat.format(viewingMonthCal.time).replaceFirstChar { it.uppercase() }
                            CalendarViewMode.SEMANA -> "Semana ${viewingMonthCal.get(Calendar.WEEK_OF_YEAR)} • ${monthYearFormat.format(viewingMonthCal.time)}"
                            CalendarViewMode.DIA -> dayOnlyFormat.format(viewingMonthCal.time).replaceFirstChar { it.uppercase() }
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HotelNavy
                    )

                    // [ HOY ] button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(StatusAvailableBg)
                            .border(1.dp, StatusAvailableBorder, RoundedCornerShape(6.dp))
                            .clickable {
                                val today = Calendar.getInstance()
                                viewingMonthCal = today
                                onDateSelected(today.timeInMillis)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("HOY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StatusAvailableGreen)
                    }
                }

                IconButton(
                    onClick = {
                        val newC = (viewingMonthCal.clone() as Calendar).apply {
                            when (viewMode) {
                                CalendarViewMode.MES -> add(Calendar.MONTH, 1)
                                CalendarViewMode.SEMANA -> add(Calendar.WEEK_OF_YEAR, 1)
                                CalendarViewMode.DIA -> add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }
                        viewingMonthCal = newC
                        onDateSelected(newC.timeInMillis)
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Siguiente", tint = HotelNavy)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (viewMode) {
                CalendarViewMode.MES -> {
                    // Month Grid with Day headers
                    val daysOfWeek = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        daysOfWeek.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val daysInMonth = viewingMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val firstDayCal = (viewingMonthCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
                    val rawDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                    val offset = if (rawDayOfWeek == Calendar.SUNDAY) 6 else rawDayOfWeek - 2

                    val totalCells = ((offset + daysInMonth + 6) / 7) * 7

                    val todayCal = Calendar.getInstance()
                    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateEpoch }

                    for (row in 0 until (totalCells / 7)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - offset + 1

                                if (dayNum in 1..daysInMonth) {
                                    val cellCal = (viewingMonthCal.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, dayNum)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    val startEpoch = cellCal.timeInMillis
                                    val endEpoch = startEpoch + 24 * 3600 * 1000L - 1

                                    val isSelected = selectedCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                                     selectedCal.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                                                     selectedCal.get(Calendar.DAY_OF_MONTH) == dayNum

                                    val isToday = todayCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                                  todayCal.get(Calendar.MONTH) == cellCal.get(Calendar.MONTH) &&
                                                  todayCal.get(Calendar.DAY_OF_MONTH) == dayNum

                                    val resCountOnDay = reservations.count {
                                        it.status != ReservationStatus.CANCELADA &&
                                        it.arrivalTimeEpoch in startEpoch..endEpoch
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> HotelNavy
                                                    isToday -> StatusAvailableBg
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                color = if (isToday && !isSelected) StatusAvailableGreen else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                onDateSelected(cellCal.timeInMillis)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dayNum",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                color = when {
                                                    isSelected -> Color.White
                                                    isToday -> StatusAvailableGreen
                                                    else -> TextDark
                                                }
                                            )
                                            if (resCountOnDay > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) ButtonAccent else HotelGoldDark)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                CalendarViewMode.SEMANA -> {
                    // Week View: 7 days in horizontal strip
                    val weekCal = (viewingMonthCal.clone() as Calendar).apply {
                        val dow = get(Calendar.DAY_OF_WEEK)
                        val diff = if (dow == Calendar.SUNDAY) -6 else -(dow - 2)
                        add(Calendar.DAY_OF_MONTH, diff)
                    }

                    val daysOfWeekNames = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
                    val selectedCal = Calendar.getInstance().apply { timeInMillis = selectedDateEpoch }
                    val todayCal = Calendar.getInstance()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 0..6) {
                            val dayCal = (weekCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
                            val dayNum = dayCal.get(Calendar.DAY_OF_MONTH)
                            val startEpoch = (dayCal.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val endEpoch = startEpoch + 24 * 3600 * 1000L - 1

                            val isSelected = selectedCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                             selectedCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)

                            val isToday = todayCal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR) &&
                                          todayCal.get(Calendar.DAY_OF_YEAR) == dayCal.get(Calendar.DAY_OF_YEAR)

                            val resCountOnDay = reservations.count {
                                it.status != ReservationStatus.CANCELADA &&
                                it.arrivalTimeEpoch in startEpoch..endEpoch
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onDateSelected(dayCal.timeInMillis) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) HotelNavy else CardLightSubtle
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isToday) StatusAvailableGreen else BorderLight
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = daysOfWeekNames[i],
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextDarkSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextDark
                                    )
                                    if (resCountOnDay > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isSelected) ButtonAccent else HotelGoldDark)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "$resCountOnDay",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) HotelNavy else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CalendarViewMode.DIA -> {
                    // Day View: detail card
                    val dayResCount = remember(selectedDateEpoch, reservations) {
                        val c = Calendar.getInstance().apply { timeInMillis = selectedDateEpoch }
                        val y = c.get(Calendar.YEAR)
                        val doy = c.get(Calendar.DAY_OF_YEAR)
                        reservations.count {
                            val rC = Calendar.getInstance().apply { timeInMillis = it.arrivalTimeEpoch }
                            rC.get(Calendar.YEAR) == y && rC.get(Calendar.DAY_OF_YEAR) == doy && it.status != ReservationStatus.CANCELADA
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardLightSubtle, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = dayOnlyFormat.format(Date(selectedDateEpoch)).replaceFirstChar { it.uppercase() },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                            Text(
                                text = "$dayResCount reservaciones activas para este día",
                                fontSize = 11.sp,
                                color = if (dayResCount > 0) StatusAvailableGreen else TextDarkSecondary,
                                fontWeight = if (dayResCount > 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply {
                                    timeInMillis = selectedDateEpoch
                                    add(Calendar.DAY_OF_YEAR, 1)
                                }
                                onDateSelected(c.timeInMillis)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+1 Día", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReservationBadge(status: ReservationStatus) {
    val (bg, fg) = when (status) {
        ReservationStatus.PENDIENTE -> Pair(StatusReservedBg, StatusReservedAmber)
        ReservationStatus.CONFIRMADA -> Pair(StatusAvailableBg, StatusAvailableGreen)
        ReservationStatus.COMPLETADA -> Pair(Color(0xFFE3F2FD), Color(0xFF1565C0))
        ReservationStatus.CANCELADA -> Pair(StatusOccupiedBg, StatusOccupiedRed)
        ReservationStatus.NO_SE_PRESENTO -> Pair(StatusDisabledBg, StatusDisabledGray)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = status.name, color = fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Dialog for creating a new reservation with functional DatePicker and TimePicker,
 * duration inputs, calculated expected departure, and strict conflict verification.
 */
@Composable
fun CreateReservationDialog(
    rooms: List<Room>,
    reservations: List<Reservation>,
    stays: List<Stay>,
    initialDateEpoch: Long,
    onDismiss: () -> Unit,
    onConfirm: (dateEpoch: Long, arrivalEpoch: Long, durationMin: Int, roomNumber: String, clientName: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    val eligibleRooms = remember(rooms) { rooms.filter { it.status != RoomStatus.DESHABILITADA } }
    var selectedRoomNumber by remember { mutableStateOf(eligibleRooms.firstOrNull()?.number ?: "") }
    var clientName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Real Date and Time initialized to rounded hour
    val initialCal = remember(initialDateEpoch) {
        Calendar.getInstance().apply {
            timeInMillis = initialDateEpoch
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 2)
        }
    }
    var arrivalEpoch by remember { mutableStateOf(initialCal.timeInMillis) }

    // Duration state: hours and minutes
    var durationHours by remember { mutableStateOf("3") }
    var durationMinutesPart by remember { mutableStateOf("0") }

    val totalDurationMinutes = remember(durationHours, durationMinutesPart) {
        val h = durationHours.toIntOrNull() ?: 0
        val m = durationMinutesPart.toIntOrNull() ?: 0
        ((h * 60) + m).coerceAtLeast(15)
    }

    val scheduledEndEpoch = remember(arrivalEpoch, totalDurationMinutes) {
        arrivalEpoch + (totalDurationMinutes * 60 * 1000L)
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a (HH:mm)", Locale.getDefault()) }
    val fullDateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    // Overlap verification with other reservations and stays
    val hasConflict = remember(selectedRoomNumber, arrivalEpoch, scheduledEndEpoch, reservations, stays) {
        val conflictRes = reservations.any { r ->
            if (r.roomNumber != selectedRoomNumber || r.status == ReservationStatus.CANCELADA || r.status == ReservationStatus.COMPLETADA) {
                false
            } else {
                val rEnd = r.arrivalTimeEpoch + (r.durationMinutes * 60 * 1000L)
                arrivalEpoch < rEnd && scheduledEndEpoch > r.arrivalTimeEpoch
            }
        }
        val conflictStay = stays.any { s ->
            if (s.roomNumber != selectedRoomNumber || s.status != StayStatus.ACTIVE) {
                false
            } else {
                arrivalEpoch < s.scheduledCheckOutEpoch && scheduledEndEpoch > s.checkInEpoch
            }
        }
        conflictRes || conflictStay
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, contentDescription = null, tint = HotelNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nueva Reservación", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Room Selection
                Text("1. Habitación:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    eligibleRooms.forEach { r ->
                        val isSel = selectedRoomNumber == r.number
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) CardLightSubtle else Color.Transparent)
                                .border(1.dp, if (isSel) HotelNavy else BorderLight, RoundedCornerShape(8.dp))
                                .clickable { selectedRoomNumber = r.number }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSel,
                                onClick = { selectedRoomNumber = r.number },
                                colors = RadioButtonDefaults.colors(selectedColor = HotelNavy)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Habitación ${r.number}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                Text("${r.type.name} • ${r.bedCount} cama(s) • Estado actual: ${r.status.name}", fontSize = 11.sp, color = TextDarkSecondary)
                            }
                        }
                    }
                }

                // 2. Client Name (Optional)
                Text("2. Huésped:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nombre del cliente (opcional)") },
                    placeholder = { Text("Consumidor Final") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. Arrival Date Picker (Functional Calendar)
                Text("3. Fecha de llegada:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardLightSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Fecha seleccionada:", fontSize = 10.sp, color = TextDarkSecondary)
                            Text(
                                text = dateFormat.format(Date(arrivalEpoch)).replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }

                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = arrivalEpoch }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = arrivalEpoch
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        arrivalEpoch = newCal.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cambiar Fecha", fontSize = 11.sp)
                        }
                    }
                }

                // 4. Arrival Time Picker (Functional 12/24 hour picker)
                Text("4. Hora de llegada:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardLightSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hora seleccionada:", fontSize = 10.sp, color = TextDarkSecondary)
                            Text(
                                text = timeFormat.format(Date(arrivalEpoch)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }

                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = arrivalEpoch }
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = arrivalEpoch
                                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            set(Calendar.MINUTE, minute)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        arrivalEpoch = newCal.timeInMillis
                                    },
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE),
                                    false // 12-hour AM/PM format
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cambiar Hora", fontSize = 11.sp)
                        }
                    }
                }

                // 5. Duration Inputs & Quick chips
                Text("5. Duración prevista:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(1 to "1h", 3 to "3h", 6 to "6h", 12 to "12h (Noche)", 24 to "24h (Día)").forEach { (h, lbl) ->
                        val isSel = durationHours == "$h" && durationMinutesPart == "0"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) ButtonAccent else CardLightSubtle)
                                .border(1.dp, if (isSel) ButtonAccent else BorderMedium, RoundedCornerShape(6.dp))
                                .clickable {
                                    durationHours = "$h"
                                    durationMinutesPart = "0"
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) ButtonAccentText else TextDark
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Horas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationMinutesPart,
                        onValueChange = { durationMinutesPart = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 6. Notes (Optional)
                Text("6. Notas u observaciones:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Instrucciones o requerimientos especiales") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 7. Automatic Calculated Summary Card with Scheduled End & Conflict Warning
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasConflict) ErrorRedBg else StatusAvailableBg
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasConflict) ErrorRed else StatusAvailableBorder
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "RESUMEN DE RESERVACIÓN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasConflict) ErrorRed else StatusAvailableGreen
                        )
                        Text(
                            text = "Habitación $selectedRoomNumber • Huésped: ${clientName.ifBlank { "Consumidor Final" }}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Llegada: ${fullDateTimeFormat.format(Date(arrivalEpoch))}",
                            fontSize = 11.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Duración: ${totalDurationMinutes / 60}h ${totalDurationMinutes % 60}m ($totalDurationMinutes minutos)",
                            fontSize = 11.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Salida Prevista Calculada: ${fullDateTimeFormat.format(Date(scheduledEndEpoch))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hasConflict) ErrorRed else HotelNavy
                        )

                        if (hasConflict) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "⚠️ CONFLICTO DE HORARIO: La habitación $selectedRoomNumber ya tiene una reservación o estadía que se cruza con este horario. Selecciona otra habitación u otro horario.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        } else {
                            Text(
                                text = "✅ Horario disponible sin cruces.",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusAvailableGreen
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        System.currentTimeMillis(),
                        arrivalEpoch,
                        totalDurationMinutes,
                        selectedRoomNumber,
                        clientName,
                        notes
                    )
                },
                enabled = selectedRoomNumber.isNotBlank() && !hasConflict && totalDurationMinutes >= 15,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HotelNavy,
                    contentColor = Color.White
                )
            ) {
                Text("CREAR RESERVACIÓN", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR", color = TextDark)
            }
        }
    )
}

/**
 * Dialog for modifying an existing reservation.
 */
@Composable
fun ModifyReservationDialog(
    reservation: Reservation,
    rooms: List<Room>,
    reservations: List<Reservation>,
    stays: List<Stay>,
    onDismiss: () -> Unit,
    onConfirm: (resId: String, roomNumber: String, dateEpoch: Long, arrivalEpoch: Long, durationMin: Int, tariffTitle: String, clientName: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    val eligibleRooms = remember(rooms) { rooms.filter { it.status != RoomStatus.DESHABILITADA } }

    var selectedRoomNumber by remember { mutableStateOf(reservation.roomNumber) }
    var clientName by remember { mutableStateOf(reservation.clientName) }
    var notes by remember { mutableStateOf(reservation.notes) }
    var arrivalEpoch by remember { mutableStateOf(reservation.arrivalTimeEpoch) }

    var durationHours by remember { mutableStateOf("${reservation.durationMinutes / 60}") }
    var durationMinutesPart by remember { mutableStateOf("${reservation.durationMinutes % 60}") }

    val totalDurationMinutes = remember(durationHours, durationMinutesPart) {
        val h = durationHours.toIntOrNull() ?: 0
        val m = durationMinutesPart.toIntOrNull() ?: 0
        ((h * 60) + m).coerceAtLeast(15)
    }

    val scheduledEndEpoch = remember(arrivalEpoch, totalDurationMinutes) {
        arrivalEpoch + (totalDurationMinutes * 60 * 1000L)
    }

    val dateFormat = remember { SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a (HH:mm)", Locale.getDefault()) }
    val fullDateTimeFormat = remember { SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()) }

    // Overlap verification ignoring this reservation
    val hasConflict = remember(selectedRoomNumber, arrivalEpoch, scheduledEndEpoch, reservations, stays) {
        val conflictRes = reservations.any { r ->
            if (r.id == reservation.id || r.roomNumber != selectedRoomNumber || r.status == ReservationStatus.CANCELADA || r.status == ReservationStatus.COMPLETADA) {
                false
            } else {
                val rEnd = r.arrivalTimeEpoch + (r.durationMinutes * 60 * 1000L)
                arrivalEpoch < rEnd && scheduledEndEpoch > r.arrivalTimeEpoch
            }
        }
        val conflictStay = stays.any { s ->
            if (s.roomNumber != selectedRoomNumber || s.status != StayStatus.ACTIVE) {
                false
            } else {
                arrivalEpoch < s.scheduledCheckOutEpoch && scheduledEndEpoch > s.checkInEpoch
            }
        }
        conflictRes || conflictStay
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 560.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EditCalendar, contentDescription = null, tint = HotelNavy)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Modificar Reservación ${reservation.reservationNumber}", fontWeight = FontWeight.Bold, color = HotelNavy, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Room
                Text("Habitación:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    eligibleRooms.forEach { r ->
                        val isSel = selectedRoomNumber == r.number
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) HotelNavy else CardLightSubtle)
                                .border(1.dp, if (isSel) HotelNavy else BorderMedium, RoundedCornerShape(8.dp))
                                .clickable { selectedRoomNumber = r.number }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hab. ${r.number}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else TextDark
                            )
                        }
                    }
                }

                // Client name
                Text("Nombre del Huésped:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Picker
                Text("Fecha de llegada:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardLightSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = dateFormat.format(Date(arrivalEpoch)).replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }
                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = arrivalEpoch }
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = arrivalEpoch
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, month)
                                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        }
                                        arrivalEpoch = newCal.timeInMillis
                                    },
                                    c.get(Calendar.YEAR),
                                    c.get(Calendar.MONTH),
                                    c.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cambiar", fontSize = 11.sp)
                        }
                    }
                }

                // Time Picker
                Text("Hora de llegada:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = CardLightSubtle),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = timeFormat.format(Date(arrivalEpoch)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HotelNavy
                            )
                        }
                        Button(
                            onClick = {
                                val c = Calendar.getInstance().apply { timeInMillis = arrivalEpoch }
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val newCal = Calendar.getInstance().apply {
                                            timeInMillis = arrivalEpoch
                                            set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            set(Calendar.MINUTE, minute)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }
                                        arrivalEpoch = newCal.timeInMillis
                                    },
                                    c.get(Calendar.HOUR_OF_DAY),
                                    c.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cambiar", fontSize = 11.sp)
                        }
                    }
                }

                // Duration
                Text("Duración prevista:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Horas") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationMinutesPart,
                        onValueChange = { durationMinutesPart = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutos") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Notes
                Text("Notas / Observaciones:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth()
                )

                // Calculated summary & Conflict
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasConflict) ErrorRedBg else StatusAvailableBg
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasConflict) ErrorRed else StatusAvailableBorder
                    )
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "NUEVO HORARIO PREVISTO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasConflict) ErrorRed else StatusAvailableGreen
                        )
                        Text(
                            text = "Llegada: ${fullDateTimeFormat.format(Date(arrivalEpoch))}",
                            fontSize = 11.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Salida Prevista Calculada: ${fullDateTimeFormat.format(Date(scheduledEndEpoch))}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasConflict) ErrorRed else HotelNavy
                        )
                        if (hasConflict) {
                            Text(
                                text = "⚠️ Conflicto con otra reservación o estadía activa en Hab. $selectedRoomNumber.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        reservation.id,
                        selectedRoomNumber,
                        reservation.dateEpoch,
                        arrivalEpoch,
                        totalDurationMinutes,
                        "Estadía Reservada (${totalDurationMinutes / 60}h ${totalDurationMinutes % 60}m)",
                        clientName,
                        notes
                    )
                },
                enabled = selectedRoomNumber.isNotBlank() && !hasConflict && totalDurationMinutes >= 15,
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("GUARDAR CAMBIOS", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = TextDark) }
        }
    )
}

