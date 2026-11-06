package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.ConnectionStatusBar
import com.example.ui.components.RoomStatusBadge
import com.example.ui.theme.*
import com.example.viewmodel.GerenteSection
import java.text.SimpleDateFormat
import java.util.Calendar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerenteScreen(
    currentSection: GerenteSection,
    onSectionSelected: (GerenteSection) -> Unit,
    connectionStatus: ConnectionStatus,
    pendingSyncCount: Int,
    onSyncClick: () -> Unit,
    hotelConfig: HotelConfig,
    rooms: List<Room>,
    tariffs: List<Tariff>,
    stays: List<Stay>,
    invoices: List<Invoice>,
    reservations: List<Reservation>,
    inventory: List<InventoryItem>,
    inventoryMovements: List<InventoryMovement>,
    purchases: List<InventoryPurchase>,
    expenses: List<ExpenseService>,
    extraFeeConcepts: List<ExtraFeeConcept>,
    devices: List<DeviceAuthorization>,
    history: List<HistoryEntry>,
    onAddRoom: (number: String, name: String, type: RoomType, beds: Int, features: String) -> Unit,
    onUpdateRoom: (id: String, number: String, name: String, type: RoomType, beds: Int, features: String) -> Unit,
    onToggleRoomDisabled: (id: String, disable: Boolean) -> Unit,
    onAddTariff: (title: String, durationMin: Int, price: Double) -> Unit,
    onUpdateTariff: (id: String, title: String, durationMin: Int, price: Double, active: Boolean) -> Unit,
    onCancelInvoice: (invoiceId: String, reason: String) -> Unit,
    onModifyInvoice: (invoiceId: String, newClientName: String, notes: String) -> Unit,
    onOpenInvoicePdf: (Invoice) -> Unit,
    onAddInventoryItem: (name: String, category: String, qty: Double, unit: String, buyPrice: Double, sellPrice: Double, isAmenity: Boolean) -> Unit,
    onUpdateInventoryItem: (id: String, name: String, qty: Double, unit: String, buyPrice: Double, sellPrice: Double, isActive: Boolean, isAmenity: Boolean) -> Unit,
    onRecordPurchase: (itemId: String, qty: Double, unitPrice: Double, supplier: String, invoiceNum: String, hasInv: Boolean, photoUri: String?, notes: String) -> Unit,
    onRecordExpense: (type: String, spec: String, qty: Int, unitPrice: Double, provider: String, date: Long, freq: String, notes: String, photo: String?) -> Unit,
    onAddExtraFeeConcept: (name: String, defaultPrice: Double) -> Unit,
    onUpdateExtraFeeConcept: (id: String, name: String, defaultPrice: Double, isActive: Boolean) -> Unit,
    onToggleExtraFeeConcept: (id: String, active: Boolean) -> Unit,
    onRenameDevice: (deviceId: String, newName: String) -> Unit,
    onRevokeDevice: (deviceId: String) -> Unit,
    onUpdateConfig: (name: String, phone: String, address: String, logo: String?, msg: String, pin: String, extraPrice: Double, alertMin: Int, notifs: Map<String, Boolean>, managerInCharge: String, nextInvoiceCorrelative: Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackClick() }

    var showNavDrawerMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "GERENTE 🔐",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = currentSection.title,
                                fontSize = 12.sp,
                                color = HotelGoldLight
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.testTag("btn_back_gerente")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Regresar al Menú Principal (Bloquea Gerente)",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showNavDrawerMenu = true },
                            modifier = Modifier.testTag("btn_gerente_menu")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Módulos de Gerente", tint = Color.White)
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

                // Quick Horizontal Selector of 10 Gerente Modules
                ScrollableTabRow(
                    selectedTabIndex = currentSection.ordinal,
                    containerColor = HotelNavyLight,
                    contentColor = HotelGold,
                    edgePadding = 12.dp
                ) {
                    GerenteSection.values().forEach { sec ->
                        Tab(
                            selected = currentSection == sec,
                            onClick = { onSectionSelected(sec) },
                            text = {
                                Text(
                                    text = sec.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (currentSection == sec) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentSection == sec) HotelGold else Color.White.copy(alpha = 0.8f)
                                )
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
            when (currentSection) {
                GerenteSection.CONTROL_GENERAL -> {
                    GerenteControlGeneralView(
                        rooms = rooms,
                        stays = stays,
                        reservations = reservations,
                        invoices = invoices,
                        purchases = purchases,
                        expenses = expenses
                    )
                }

                GerenteSection.HABITACIONES -> {
                    GerenteHabitacionesView(
                        rooms = rooms,
                        onAddRoom = onAddRoom,
                        onUpdateRoom = onUpdateRoom,
                        onToggleRoomDisabled = onToggleRoomDisabled
                    )
                }

                GerenteSection.PRECIOS -> {
                    GerentePreciosView(
                        tariffs = tariffs,
                        extraTimePricePerHour = hotelConfig.extraTimePricePerHour,
                        onAddTariff = onAddTariff,
                        onUpdateTariff = onUpdateTariff
                    )
                }

                GerenteSection.FACTURAS -> {
                    GerenteFacturasView(
                        invoices = invoices,
                        onCancelInvoice = onCancelInvoice,
                        onModifyInvoice = onModifyInvoice,
                        onOpenPdf = onOpenInvoicePdf
                    )
                }

                GerenteSection.HISTORIAL -> {
                    GerenteHistorialView(history = history)
                }

                GerenteSection.INVENTARIO -> {
                    GerenteInventarioView(
                        inventory = inventory,
                        movements = inventoryMovements,
                        onAddItem = onAddInventoryItem,
                        onUpdateItem = onUpdateInventoryItem,
                        onRecordPurchase = onRecordPurchase
                    )
                }

                GerenteSection.COBROS_EXTRAS -> {
                    GerenteCobrosExtrasView(
                        concepts = extraFeeConcepts,
                        onAddConcept = onAddExtraFeeConcept,
                        onUpdateConcept = onUpdateExtraFeeConcept,
                        onToggleConcept = onToggleExtraFeeConcept
                    )
                }

                GerenteSection.REGISTRO_ECONOMICO -> {
                    GerenteRegistroEconomicoView(
                        invoices = invoices,
                        purchases = purchases,
                        expenses = expenses,
                        onRecordExpense = onRecordExpense
                    )
                }

                GerenteSection.DISPOSITIVOS -> {
                    GerenteDispositivosView(
                        devices = devices,
                        onRenameDevice = onRenameDevice,
                        onRevokeDevice = onRevokeDevice
                    )
                }

                GerenteSection.CONFIGURACION -> {
                    GerenteConfiguracionView(
                        config = hotelConfig,
                        onUpdateConfig = onUpdateConfig
                    )
                }
            }
        }
    }

    // Module Selection BottomSheet
    if (showNavDrawerMenu) {
        ModalBottomSheet(
            onDismissRequest = { showNavDrawerMenu = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "MÓDULOS DE GERENCIA",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = HotelNavy,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                GerenteSection.values().forEach { sec ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSectionSelected(sec)
                                showNavDrawerMenu = false
                            },
                        color = if (currentSection == sec) HotelNavyLight else Color(0xFFF7F7F7)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sec.title,
                                fontWeight = FontWeight.Bold,
                                color = if (currentSection == sec) Color.White else TextDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 1. CONTROL GENERAL
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteControlGeneralView(
    rooms: List<Room>,
    stays: List<Stay>,
    reservations: List<Reservation>,
    invoices: List<Invoice>,
    purchases: List<InventoryPurchase>,
    expenses: List<ExpenseService>
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var selectedFilter by remember { mutableStateOf("Hoy") }

    val activeStays = remember(stays) { stays.filter { it.status == StayStatus.ACTIVE } }

    val availableCount = rooms.count { it.status == RoomStatus.DISPONIBLE }
    val occupiedCount = rooms.count { it.status == RoomStatus.OCUPADA }
    val reservedCount = rooms.count { it.status == RoomStatus.RESERVADA }
    val disabledCount = rooms.count { it.status == RoomStatus.DESHABILITADA }

    // Date range filtering
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = cal.timeInMillis

    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
    val startOfWeek = cal.timeInMillis

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val startOfMonth = cal.timeInMillis

    val filterStartTime = when (selectedFilter) {
        "Hoy" -> startOfToday
        "Esta semana" -> startOfWeek
        "Este mes" -> startOfMonth
        else -> 0L
    }

    // Filter records
    val filteredInvoices = remember(invoices, filterStartTime) {
        invoices.filter { it.dateEpoch >= filterStartTime && it.status != InvoiceStatus.ANULADA }
    }
    val filteredPurchases = remember(purchases, filterStartTime) {
        purchases.filter { it.dateEpoch >= filterStartTime }
    }
    val filteredExpenses = remember(expenses, filterStartTime) {
        expenses.filter { it.dateEpoch >= filterStartTime }
    }

    val totalVentas = filteredInvoices.sumOf { it.total }
    val totalCompras = filteredPurchases.sumOf { it.total }
    val totalServicios = filteredExpenses.sumOf { it.total }

    val totalIngresos = totalVentas
    val totalEgresos = totalCompras + totalServicios
    val balance = totalIngresos - totalEgresos

    var detailDialogTitle by remember { mutableStateOf<String?>(null) }
    var detailDialogContent by remember { mutableStateOf<List<String>>(emptyList()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Resumen de Habitaciones
        item {
            Text("ESTADO DE HABITACIONES", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ResumenMiniCard("DISPONIBLES", "$availableCount", StatusAvailableGreen, StatusAvailableBg, Modifier.weight(1f))
                ResumenMiniCard("OCUPADAS", "$occupiedCount", StatusOccupiedRed, StatusOccupiedBg, Modifier.weight(1f))
                ResumenMiniCard("RESERVADAS", "$reservedCount", StatusReservedAmber, StatusReservedBg, Modifier.weight(1f))
                ResumenMiniCard("DESHABILITADAS", "$disabledCount", StatusDisabledGray, StatusDisabledBg, Modifier.weight(1f))
            }
        }

        // Section: Estadía Actual
        item {
            Text("ESTADÍAS ACTIVAS (${activeStays.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
        }

        if (activeStays.isEmpty()) {
            item {
                Text("No hay estadías activas en este momento.", fontSize = 13.sp, color = TextMuted)
            }
        } else {
            items(activeStays) { stay ->
                val remainingMs = stay.scheduledCheckOutEpoch - now
                val remMin = (remainingMs / (60 * 1000L)).coerceAtLeast(0)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Habitación ${stay.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                            Text("${stay.tariffTitle}", fontWeight = FontWeight.Bold, color = HotelGoldDark, fontSize = 12.sp)
                        }
                        Text("Cliente: ${stay.clientName}", fontSize = 13.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entrada: ${timeFormat.format(Date(stay.checkInEpoch))}", fontSize = 12.sp, color = TextMuted)
                            Text("Prevista: ${timeFormat.format(Date(stay.scheduledCheckOutEpoch))}", fontSize = 12.sp, color = TextMuted)
                            Text("Resta: ${remMin}m", fontWeight = FontWeight.Bold, color = if (remMin < 15) StatusOccupiedRed else StatusAvailableGreen, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Section: Reservaciones Próximas
        item {
            Text("PRÓXIMAS RESERVACIONES (${reservations.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
        }
        val upcoming = reservations.filter { it.status == ReservationStatus.CONFIRMADA || it.status == ReservationStatus.PENDIENTE }.take(3)
        if (upcoming.isEmpty()) {
            item {
                Text("No hay reservaciones próximas registradas.", fontSize = 13.sp, color = TextMuted)
            }
        } else {
            items(upcoming) { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Hab. ${res.roomNumber} — ${res.clientName.ifBlank { "Sin nombre" }}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Llegada: ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(res.arrivalTimeEpoch))}", fontSize = 11.sp, color = TextMuted)
                        }
                        Text(res.status.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HotelNavy)
                    }
                }
            }
        }

        // Section: Resumen Económico
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RESUMEN ECONÓMICO", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)

                    // Period Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Hoy", "Esta semana", "Este mes", "Todo").forEach { period ->
                            FilterChip(
                                selected = selectedFilter == period,
                                onClick = { selectedFilter = period },
                                label = { Text(period, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown Cards
                EconomicItemCard(
                    label = "VENTAS",
                    amount = totalVentas,
                    color = StatusAvailableGreen,
                    onClick = {
                        detailDialogTitle = "Detalle de Ventas ($selectedFilter)"
                        detailDialogContent = filteredInvoices.map { "Factura ${it.invoiceNumber}: Q${it.total} (${it.clientName.ifBlank { "Consumidor Final" }})" }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                EconomicItemCard(
                    label = "COMPRAS",
                    amount = totalCompras,
                    color = StatusOccupiedRed,
                    onClick = {
                        detailDialogTitle = "Detalle de Compras ($selectedFilter)"
                        detailDialogContent = filteredPurchases.map { "${it.itemName} (${it.quantity} ${it.unit}): Q${it.total} - Prov: ${it.supplier}" }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                EconomicItemCard(
                    label = "SERVICIOS / GASTOS",
                    amount = totalServicios,
                    color = StatusOccupiedRed,
                    onClick = {
                        detailDialogTitle = "Detalle de Servicios y Gastos ($selectedFilter)"
                        detailDialogContent = filteredExpenses.map { "${it.type}: Q${it.total} - ${it.specification} (${it.providerName})" }
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                // Totals & Balance
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ResumenMiniCard("TOTAL INGRESOS", String.format(Locale.US, "Q%.2f", totalIngresos), StatusAvailableGreen, StatusAvailableBg, Modifier.weight(1f))
                    ResumenMiniCard("TOTAL EGRESOS", String.format(Locale.US, "Q%.2f", totalEgresos), StatusOccupiedRed, StatusOccupiedBg, Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (balance >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BALANCE NETO:", fontWeight = FontWeight.Black, fontSize = 16.sp, color = HotelNavy)
                        Text(
                            text = String.format(Locale.US, "Q%.2f", balance),
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = if (balance >= 0) StatusAvailableGreen else StatusOccupiedRed
                        )
                    }
                }
            }
        }
    }

    if (detailDialogTitle != null) {
        AlertDialog(
            onDismissRequest = { detailDialogTitle = null },
            title = { Text(detailDialogTitle!!, fontWeight = FontWeight.Bold, color = HotelNavy) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (detailDialogContent.isEmpty()) {
                        Text("No hay registros en este periodo.")
                    } else {
                        detailDialogContent.forEach { line ->
                            Text("• $line", fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailDialogTitle = null }) { Text("CERRAR") }
            }
        )
    }
}

@Composable
fun EconomicItemCard(label: String, amount: Double, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Text("Tocar para ver detalle", fontSize = 10.sp, color = TextMuted)
            }
            Text(String.format(Locale.US, "Q%.2f", amount), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
        }
    }
}

@Composable
fun ResumenMiniCard(title: String, value: String, color: Color, bg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. HABITACIONES
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteHabitacionesView(
    rooms: List<Room>,
    onAddRoom: (number: String, name: String, type: RoomType, beds: Int, features: String) -> Unit,
    onUpdateRoom: (id: String, number: String, name: String, type: RoomType, beds: Int, features: String) -> Unit,
    onToggleRoomDisabled: (id: String, disable: Boolean) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<Room?>(null) }
    var roomToDisableWarning by remember { mutableStateOf<Room?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ADMINISTRACIÓN DE HABITACIONES", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AGREGAR", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(rooms) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Habitación ${r.number} — ${r.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                            RoomStatusBadge(status = r.status)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tipo: ${r.type.name} • ${r.bedCount} cama(s)", fontSize = 13.sp)
                        if (r.features.isNotBlank()) {
                            Text("Características: ${r.features}", fontSize = 12.sp, color = TextMuted)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = { editingRoom = r },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Editar", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            if (r.status == RoomStatus.DESHABILITADA) {
                                Button(
                                    onClick = { onToggleRoomDisabled(r.id, false) },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusAvailableGreen),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Reactivar", fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        if (r.status == RoomStatus.OCUPADA) {
                                            roomToDisableWarning = r
                                        } else {
                                            onToggleRoomDisabled(r.id, true)
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOccupiedRed),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Deshabilitar", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RoomFormDialog(
            title = "Agregar Habitación",
            initialNumber = "",
            initialName = "",
            initialType = RoomType.INDIVIDUAL,
            initialBeds = 1,
            initialFeatures = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { num, name, type, beds, feat ->
                onAddRoom(num, name, type, beds, feat)
                showAddDialog = false
            }
        )
    }

    if (editingRoom != null) {
        RoomFormDialog(
            title = "Editar Habitación ${editingRoom!!.number}",
            initialNumber = editingRoom!!.number,
            initialName = editingRoom!!.name,
            initialType = editingRoom!!.type,
            initialBeds = editingRoom!!.bedCount,
            initialFeatures = editingRoom!!.features,
            onDismiss = { editingRoom = null },
            onConfirm = { num, name, type, beds, feat ->
                onUpdateRoom(editingRoom!!.id, num, name, type, beds, feat)
                editingRoom = null
            }
        )
    }

    // Warning if disabling an occupied room (Rule 9)
    if (roomToDisableWarning != null) {
        AlertDialog(
            onDismissRequest = { roomToDisableWarning = null },
            title = { Text("⚠️ Advertencia: Habitación Ocupada", fontWeight = FontWeight.Bold, color = StatusOccupiedRed) },
            text = {
                Text("La habitación ${roomToDisableWarning!!.number} está actualmente OCUPADA. ¿Deseas deshabilitarla de todos modos? Se conservará la estadía activa.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleRoomDisabled(roomToDisableWarning!!.id, true)
                        roomToDisableWarning = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOccupiedRed)
                ) {
                    Text("DESHABILITAR")
                }
            },
            dismissButton = {
                TextButton(onClick = { roomToDisableWarning = null }) { Text("CANCELAR") }
            }
        )
    }
}

@Composable
fun RoomFormDialog(
    title: String,
    initialNumber: String,
    initialName: String,
    initialType: RoomType,
    initialBeds: Int,
    initialFeatures: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, RoomType, Int, String) -> Unit
) {
    var number by remember { mutableStateOf(initialNumber) }
    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }
    var beds by remember { mutableStateOf(initialBeds.toString()) }
    var features by remember { mutableStateOf(initialFeatures) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = number, onValueChange = { number = it }, label = { Text("Número de Habitación") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre descriptivo") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tipo de Habitación:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                RoomType.values().forEach { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { type = t }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(t.name)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = beds, onValueChange = { beds = it }, label = { Text("Número de camas") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = features, onValueChange = { features = it }, label = { Text("Características (baño, tv, etc.)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(number, name, type, beds.toIntOrNull() ?: 1, features)
                },
                enabled = number.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("GUARDAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// 3. PRECIOS
// -------------------------------------------------------------------------------------------------
@Composable
fun GerentePreciosView(
    tariffs: List<Tariff>,
    extraTimePricePerHour: Double,
    onAddTariff: (String, Int, Double) -> Unit,
    onUpdateTariff: (String, String, Int, Double, Boolean) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTariff by remember { mutableStateOf<Tariff?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TARIFAS Y PRECIOS", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("NUEVA TARIFA", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tiempo extra configurado: Q${extraTimePricePerHour} por cada hora adicional. Los cambios de precio solo aplican a nuevas facturas.",
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(tariffs) { tf ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tf.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                                if (!tf.isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("(Inactiva)", color = StatusOccupiedRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Duración: ${tf.durationMinutes} min", fontSize = 12.sp, color = TextMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(String.format(Locale.US, "Q%.2f", tf.price), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = HotelGoldDark)
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(onClick = { editingTariff = tf }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = HotelNavy)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TariffFormDialog(
            title = "Nueva Tarifa",
            initialTitle = "",
            initialDuration = 60,
            initialPrice = 50.0,
            initialActive = true,
            onDismiss = { showAddDialog = false },
            onConfirm = { t, d, p, _ ->
                onAddTariff(t, d, p)
                showAddDialog = false
            }
        )
    }

    if (editingTariff != null) {
        TariffFormDialog(
            title = "Editar Tarifa",
            initialTitle = editingTariff!!.title,
            initialDuration = editingTariff!!.durationMinutes,
            initialPrice = editingTariff!!.price,
            initialActive = editingTariff!!.isActive,
            onDismiss = { editingTariff = null },
            onConfirm = { t, d, p, act ->
                onUpdateTariff(editingTariff!!.id, t, d, p, act)
                editingTariff = null
            }
        )
    }
}

@Composable
fun TariffFormDialog(
    title: String,
    initialTitle: String,
    initialDuration: Int,
    initialPrice: Double,
    initialActive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Double, Boolean) -> Unit
) {
    var titleInput by remember { mutableStateOf(initialTitle) }
    var durationInput by remember { mutableStateOf(initialDuration.toString()) }
    var priceInput by remember { mutableStateOf(initialPrice.toString()) }
    var isActive by remember { mutableStateOf(initialActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column {
                OutlinedTextField(value = titleInput, onValueChange = { titleInput = it }, label = { Text("Nombre de la tarifa (ej. 3 horas)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = durationInput, onValueChange = { durationInput = it }, label = { Text("Duración en minutos") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = priceInput, onValueChange = { priceInput = it }, label = { Text("Precio en Quetzales (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isActive, onCheckedChange = { isActive = it })
                    Text("Tarifa activa")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(titleInput, durationInput.toIntOrNull() ?: 60, priceInput.toDoubleOrNull() ?: 45.0, isActive)
                },
                enabled = titleInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("GUARDAR")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

// -------------------------------------------------------------------------------------------------
// 4. FACTURAS
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteFacturasView(
    invoices: List<Invoice>,
    onCancelInvoice: (String, String) -> Unit,
    onModifyInvoice: (String, String, String) -> Unit,
    onOpenPdf: (Invoice) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var cancelingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var cancelReason by remember { mutableStateOf("") }

    var modifyingInvoice by remember { mutableStateOf<Invoice?>(null) }
    var newClientName by remember { mutableStateOf("") }
    var modNotes by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("FACTURAS REGISTRADAS (${invoices.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
        Spacer(modifier = Modifier.height(10.dp))

        if (invoices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay facturas registradas.", color = TextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(invoices.sortedByDescending { it.invoiceNumber }) { inv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Factura N° ${inv.invoiceNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = HotelNavy)
                                Text(
                                    text = inv.status.name,
                                    color = when (inv.status) {
                                        InvoiceStatus.ACTIVA -> StatusAvailableGreen
                                        InvoiceStatus.ANULADA -> StatusOccupiedRed
                                        InvoiceStatus.MODIFICADA -> HotelGoldDark
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Habitación ${inv.roomNumber} • ${inv.tariffTitle}", fontSize = 13.sp)
                            Text("Cliente: ${inv.clientName.ifBlank { "Consumidor Final" }}", fontSize = 13.sp)
                            Text("Fecha: ${dateFormat.format(Date(inv.dateEpoch))}", fontSize = 11.sp, color = TextMuted)
                            Text("Forma de pago: ${inv.paymentMethod.name}", fontSize = 11.sp, color = TextMuted)

                            if (inv.status == InvoiceStatus.ANULADA) {
                                Text("Motivo anulación: ${inv.cancellationReason}", fontSize = 11.sp, color = StatusOccupiedRed)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(String.format(Locale.US, "TOTAL: Q%.2f", inv.total), fontWeight = FontWeight.Black, fontSize = 16.sp, color = HotelGoldDark)

                                Row {
                                    Button(
                                        onClick = { onOpenPdf(inv) },
                                        modifier = Modifier.height(34.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PDF", fontSize = 11.sp)
                                    }

                                    if (inv.status != InvoiceStatus.ANULADA) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                modifyingInvoice = inv
                                                newClientName = inv.clientName
                                                modNotes = ""
                                            },
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Modificar", fontSize = 11.sp)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                cancelingInvoice = inv
                                                cancelReason = ""
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusOccupiedRed),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Anular", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (cancelingInvoice != null) {
        AlertDialog(
            onDismissRequest = { cancelingInvoice = null },
            title = { Text("Anular Factura N° ${cancelingInvoice!!.invoiceNumber}", fontWeight = FontWeight.Bold, color = StatusOccupiedRed) },
            text = {
                Column {
                    Text("Las facturas anuladas se conservan en el historial y no se borran físicamente.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Motivo de la anulación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cancelReason.isNotBlank()) {
                            onCancelInvoice(cancelingInvoice!!.id, cancelReason)
                            cancelingInvoice = null
                        }
                    },
                    enabled = cancelReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOccupiedRed)
                ) {
                    Text("CONFIRMAR ANULACIÓN")
                }
            },
            dismissButton = { TextButton(onClick = { cancelingInvoice = null }) { Text("CANCELAR") } }
        )
    }

    if (modifyingInvoice != null) {
        AlertDialog(
            onDismissRequest = { modifyingInvoice = null },
            title = { Text("Modificar Factura N° ${modifyingInvoice!!.invoiceNumber}", fontWeight = FontWeight.Bold, color = HotelNavy) },
            text = {
                Column {
                    Text("Los datos modificados se guardan conservando los valores anteriores en el historial.")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newClientName,
                        onValueChange = { newClientName = it },
                        label = { Text("Nombre de cliente corregido") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = modNotes,
                        onValueChange = { modNotes = it },
                        label = { Text("Motivo / Observaciones del cambio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onModifyInvoice(modifyingInvoice!!.id, newClientName, modNotes)
                        modifyingInvoice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("GUARDAR MODIFICACIÓN")
                }
            },
            dismissButton = { TextButton(onClick = { modifyingInvoice = null }) { Text("CANCELAR") } }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 5. HISTORIAL GENERAL
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteHistorialView(history: List<HistoryEntry>) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }
    var filterType by remember { mutableStateOf("TODOS") }

    val distinctTypes = remember(history) {
        listOf("TODOS") + history.map { it.operationType }.distinct()
    }

    val filteredList = remember(history, filterType) {
        if (filterType == "TODOS") history else history.filter { it.operationType == filterType }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("HISTORIAL GENERAL DE OPERACIONES", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal filter chips
        ScrollableTabRow(
            selectedTabIndex = distinctTypes.indexOf(filterType).coerceAtLeast(0),
            containerColor = Color.Transparent,
            edgePadding = 0.dp
        ) {
            distinctTypes.forEach { type ->
                Tab(
                    selected = filterType == type,
                    onClick = { filterType = type },
                    text = { Text(type, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredList) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                            Text(entry.operationType, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HotelGoldDark)
                        }
                        Text(entry.description, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(dateFormat.format(Date(entry.dateEpoch)), fontSize = 10.sp, color = TextMuted)
                            Text("${entry.userOrRole} • ${entry.deviceName}", fontSize = 10.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 6. INVENTARIO & AMENITIES
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteInventarioView(
    inventory: List<InventoryItem>,
    movements: List<InventoryMovement>,
    onAddItem: (String, String, Double, String, Double, Double, Boolean) -> Unit,
    onUpdateItem: (String, String, Double, String, Double, Double, Boolean, Boolean) -> Unit,
    onRecordPurchase: (String, Double, Double, String, String, Boolean, String?, String) -> Unit
) {
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var purchasingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showMovementsDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("INVENTARIO Y AMENITIES", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
            Row {
                OutlinedButton(onClick = { showMovementsDialog = true }, modifier = Modifier.height(36.dp)) {
                    Text("Movimientos", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Button(
                    onClick = { showAddItemDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nuevo Item", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(inventory) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingItem = item },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HotelNavy)
                                if (item.isAmenity) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("AMENITY", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = HotelTeal, modifier = Modifier.background(HotelTealLight, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                                if (!item.isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("INACTIVO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StatusDisabledGray, modifier = Modifier.background(StatusDisabledBg, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Existencias: ${item.quantity.toInt()} ${item.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (item.quantity <= 3) StatusOccupiedRed else StatusAvailableGreen)
                            Text("Compra: Q${item.purchasePrice} ${if (!item.isAmenity) "• Venta: Q${item.salePrice}" else ""}", fontSize = 11.sp, color = TextMuted)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingItem = item },
                                modifier = Modifier.size(36.dp).testTag("btn_edit_item_${item.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar ${item.name}", tint = HotelNavy, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = { purchasingItem = item },
                                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("+ COMPRAR", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddItemDialog) {
        InventoryItemFormDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, cat, qty, unit, buy, sell, isAmenity ->
                onAddItem(name, cat, qty, unit, buy, sell, isAmenity)
                showAddItemDialog = false
            }
        )
    }

    if (editingItem != null) {
        EditInventoryItemDialog(
            item = editingItem!!,
            onDismiss = { editingItem = null },
            onConfirm = { id, name, qty, unit, buy, sell, isActive, isAmenity ->
                onUpdateItem(id, name, qty, unit, buy, sell, isActive, isAmenity)
                editingItem = null
            }
        )
    }

    if (purchasingItem != null) {
        RecordPurchaseDialog(
            item = purchasingItem!!,
            onDismiss = { purchasingItem = null },
            onConfirm = { qty, price, sup, invNum, hasInv, notes ->
                onRecordPurchase(purchasingItem!!.id, qty, price, sup, invNum, hasInv, null, notes)
                purchasingItem = null
            }
        )
    }

    if (showMovementsDialog) {
        AlertDialog(
            onDismissRequest = { showMovementsDialog = false },
            title = { Text("Historial de Movimientos de Inventario", fontWeight = FontWeight.Bold, color = HotelNavy) },
            text = {
                val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (movements.isEmpty()) {
                        Text("No hay movimientos registrados.")
                    } else {
                        movements.take(25).forEach { m ->
                            Text("• ${dateFormat.format(Date(m.dateEpoch))} | ${m.movementType}: ${m.itemName} (${m.quantityDelta}) - ${m.notes}", fontSize = 12.sp, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMovementsDialog = false }) { Text("CERRAR") } }
        )
    }
}

@Composable
fun EditInventoryItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (id: String, name: String, qty: Double, unit: String, buyPrice: Double, sellPrice: Double, isActive: Boolean, isAmenity: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    var unit by remember { mutableStateOf(item.unit) }
    var purchasePrice by remember { mutableStateOf(item.purchasePrice.toString()) }
    var salePrice by remember { mutableStateOf(item.salePrice.toString()) }
    var isActive by remember { mutableStateOf(item.isActive) }
    var isAmenity by remember { mutableStateOf(item.isAmenity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Producto: ${item.name}", fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del producto") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_item_name")
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad actual en existencias") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_item_qty")
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unidad de medida (unidad, botella, rollo)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purchasePrice,
                    onValueChange = { purchasePrice = it },
                    label = { Text("Precio de compra (Q)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = salePrice,
                    onValueChange = { salePrice = it },
                    label = { Text("Precio de venta al cliente (Q)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Producto Activo en Sistema", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAmenity, onCheckedChange = { isAmenity = it })
                    Text("Es Amenity (Habitación)", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        item.id,
                        name,
                        quantity.toDoubleOrNull() ?: item.quantity,
                        unit,
                        purchasePrice.toDoubleOrNull() ?: item.purchasePrice,
                        salePrice.toDoubleOrNull() ?: item.salePrice,
                        isActive,
                        isAmenity
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                modifier = Modifier.testTag("btn_save_item_edit")
            ) {
                Text("GUARDAR CAMBIOS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}

@Composable
fun InventoryItemFormDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String, qty: Double, unit: String, buyPrice: Double, sellPrice: Double, isAmenity: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("BEBIDAS") }
    var quantity by remember { mutableStateOf("10") }
    var unit by remember { mutableStateOf("unidad") }
    var purchasePrice by remember { mutableStateOf("5") }
    var salePrice by remember { mutableStateOf("10") }
    var isAmenity by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Producto de Inventario", fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre del producto") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unidad (unidad, botella, rollo)") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Cantidad inicial") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = purchasePrice, onValueChange = { purchasePrice = it }, label = { Text("Precio de compra (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAmenity, onCheckedChange = { isAmenity = it })
                    Text("Es Amenity (Gratuito para el huésped)")
                }

                if (!isAmenity) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = salePrice, onValueChange = { salePrice = it }, label = { Text("Precio de venta al cliente (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        name,
                        category,
                        quantity.toDoubleOrNull() ?: 0.0,
                        unit,
                        purchasePrice.toDoubleOrNull() ?: 0.0,
                        salePrice.toDoubleOrNull() ?: 0.0,
                        isAmenity
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("GUARDAR")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

@Composable
fun RecordPurchaseDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (qty: Double, price: Double, supplier: String, invNum: String, hasInv: Boolean, notes: String) -> Unit
) {
    var quantity by remember { mutableStateOf("10") }
    var unitPrice by remember { mutableStateOf(item.purchasePrice.toString()) }
    var supplier by remember { mutableStateOf("Distribuidora Local") }
    var invoiceNumber by remember { mutableStateOf("") }
    var hasInvoice by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    val total = (quantity.toDoubleOrNull() ?: 0.0) * (unitPrice.toDoubleOrNull() ?: 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Compra: ${item.name}", fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Cantidad (${item.unit})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Precio Unitario (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total calculado: Q$total", fontWeight = FontWeight.ExtraBold, color = StatusOccupiedRed, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = supplier, onValueChange = { supplier = it }, label = { Text("Proveedor") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasInvoice, onCheckedChange = { hasInvoice = it })
                    Text("Compra con factura")
                }
                if (hasInvoice) {
                    OutlinedTextField(value = invoiceNumber, onValueChange = { invoiceNumber = it }, label = { Text("Número de Factura de Compra") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        quantity.toDoubleOrNull() ?: 1.0,
                        unitPrice.toDoubleOrNull() ?: item.purchasePrice,
                        supplier,
                        invoiceNumber,
                        hasInvoice,
                        notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("REGISTRAR COMPRA")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

// -------------------------------------------------------------------------------------------------
// 7. COBROS EXTRAS
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteCobrosExtrasView(
    concepts: List<ExtraFeeConcept>,
    onAddConcept: (String, Double) -> Unit,
    onUpdateConcept: (String, String, Double, Boolean) -> Unit,
    onToggleConcept: (String, Boolean) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingConcept by remember { mutableStateOf<ExtraFeeConcept?>(null) }
    var conceptName by remember { mutableStateOf("") }
    var defaultPrice by remember { mutableStateOf("50") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("CONCEPTOS DE COBROS EXTRAS", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("NUEVO CONCEPTO", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Conceptos configurables (ej. Huésped adicional Q50, Cama extra Q30, daños). Pulsa sobre un concepto para editar su precio.",
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(concepts) { c ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingConcept = c },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(c.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HotelNavy)
                            Text("Precio sugerido: Q${c.defaultPrice}", fontSize = 12.sp, color = HotelGoldDark, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { editingConcept = c },
                                modifier = Modifier.size(36.dp).testTag("btn_edit_fee_${c.id}")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar ${c.name}", tint = HotelNavy, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(if (c.isActive) "Activo" else "Inactivo", fontSize = 12.sp, color = if (c.isActive) StatusAvailableGreen else TextMuted)
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = c.isActive,
                                onCheckedChange = { onToggleConcept(c.id, it) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Concepto de Cobro Extra", fontWeight = FontWeight.Bold, color = HotelNavy) },
            text = {
                Column {
                    OutlinedTextField(value = conceptName, onValueChange = { conceptName = it }, label = { Text("Concepto (ej. Daño de control TV)") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = defaultPrice, onValueChange = { defaultPrice = it }, label = { Text("Precio sugerido (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (conceptName.isNotBlank()) {
                            onAddConcept(conceptName, defaultPrice.toDoubleOrNull() ?: 50.0)
                            showAddDialog = false
                            conceptName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("GUARDAR")
                }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (editingConcept != null) {
        EditExtraFeeConceptDialog(
            concept = editingConcept!!,
            onDismiss = { editingConcept = null },
            onConfirm = { id, name, price, active ->
                onUpdateConcept(id, name, price, active)
                editingConcept = null
            }
        )
    }
}

@Composable
fun EditExtraFeeConceptDialog(
    concept: ExtraFeeConcept,
    onDismiss: () -> Unit,
    onConfirm: (id: String, name: String, price: Double, active: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(concept.name) }
    var priceStr by remember { mutableStateOf(concept.defaultPrice.toString()) }
    var active by remember { mutableStateOf(concept.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Concepto de Cobro Extra", fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del concepto") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_fee_name")
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Precio sugerido (Q)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_fee_price")
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Concepto Activo en Recepción", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        concept.id,
                        name,
                        priceStr.toDoubleOrNull() ?: concept.defaultPrice,
                        active
                    )
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                modifier = Modifier.testTag("btn_save_fee_edit")
            ) {
                Text("GUARDAR CAMBIOS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// 8. REGISTRO ECONÓMICO
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteRegistroEconomicoView(
    invoices: List<Invoice>,
    purchases: List<InventoryPurchase>,
    expenses: List<ExpenseService>,
    onRecordExpense: (type: String, spec: String, qty: Int, unitPrice: Double, provider: String, date: Long, freq: String, notes: String, photo: String?) -> Unit
) {
    var selectedPeriod by remember { mutableStateOf("TODOS") }
    var selectedTab by remember { mutableStateOf("VENTAS") }
    var showExpenseDialog by remember { mutableStateOf(false) }

    val periodStartEpoch = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "HOY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "SEMANA" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "MES" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }
    }

    val filteredInvoices = remember(invoices, periodStartEpoch) {
        invoices.filter { it.status != InvoiceStatus.ANULADA && it.dateEpoch >= periodStartEpoch }
    }
    val filteredPurchases = remember(purchases, periodStartEpoch) {
        purchases.filter { it.dateEpoch >= periodStartEpoch }
    }
    val filteredExpenses = remember(expenses, periodStartEpoch) {
        expenses.filter { it.dateEpoch >= periodStartEpoch }
    }

    val totalIngresos = remember(filteredInvoices) { filteredInvoices.sumOf { it.total } }
    val totalCompras = remember(filteredPurchases) { filteredPurchases.sumOf { it.total } }
    val totalGastos = remember(filteredExpenses) { filteredExpenses.sumOf { it.total } }
    val totalEgresos = totalCompras + totalGastos
    val balanceNeto = totalIngresos - totalEgresos

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("REGISTRO ECONÓMICO Y BALANCE", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
            Button(
                onClick = { showExpenseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("REGISTRAR GASTO", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tarjeta destacada de Balance
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = HotelNavy),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RESUMEN ECONÓMICO", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HotelGold)
                    Text("Período: $selectedPeriod", fontSize = 11.sp, color = TextLightSecondary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Ingresos", fontSize = 11.sp, color = TextLightSecondary)
                        Text(String.format(Locale.US, "+Q%.2f", totalIngresos), fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF81C784))
                    }
                    Column {
                        Text("Egresos", fontSize = 11.sp, color = TextLightSecondary)
                        Text(String.format(Locale.US, "-Q%.2f", totalEgresos), fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFE57373))
                    }
                    Column {
                        Text("Balance Neto", fontSize = 11.sp, color = TextLightSecondary)
                        Text(
                            String.format(Locale.US, "Q%.2f", balanceNeto),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (balanceNeto >= 0) HotelGold else Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filtro por Período
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("HOY", "SEMANA", "MES", "TODOS").forEach { period ->
                val isSel = selectedPeriod == period
                Button(
                    onClick = { selectedPeriod = period },
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) HotelGold else Color.White,
                        contentColor = if (isSel) TextDark else TextDarkSecondary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(period, fontSize = 10.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tabs: VENTAS, COMPRAS, SERVICIOS/GASTOS
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("VENTAS", "COMPRAS", "SERVICIOS/GASTOS").forEach { tab ->
                val isSel = selectedTab == tab
                Button(
                    onClick = { selectedTab = tab },
                    modifier = Modifier.weight(1f).height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) HotelNavy else Color.White,
                        contentColor = if (isSel) Color.White else TextDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(tab, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedTab) {
            "VENTAS" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filteredInvoices.isEmpty()) {
                        item {
                            Text("No hay ventas registradas en este período.", fontSize = 12.sp, color = TextLightSecondary, modifier = Modifier.padding(8.dp))
                        }
                    }
                    items(filteredInvoices) { inv ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Factura ${inv.invoiceNumber} — Hab. ${inv.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                    Text("Cliente: ${inv.clientName.ifBlank { "Consumidor Final" }}", fontSize = 11.sp, color = TextDarkSecondary)
                                    Text("Pago: ${inv.paymentMethod.name}", fontSize = 11.sp, color = TextDarkSecondary)
                                }
                                Text(String.format(Locale.US, "+Q%.2f", inv.total), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = StatusAvailableGreen)
                            }
                        }
                    }
                }
            }

            "COMPRAS" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filteredPurchases.isEmpty()) {
                        item {
                            Text("No hay compras registradas en este período.", fontSize = 12.sp, color = TextLightSecondary, modifier = Modifier.padding(8.dp))
                        }
                    }
                    items(filteredPurchases) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${p.itemName} (${p.quantity} ${p.unit})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                    Text("Proveedor: ${p.supplier}", fontSize = 11.sp, color = TextDarkSecondary)
                                    if (p.hasInvoice && p.invoiceNumber.isNotBlank()) {
                                        Text("Factura N°: ${p.invoiceNumber}", fontSize = 11.sp, color = TextDarkSecondary)
                                    }
                                }
                                Text(String.format(Locale.US, "-Q%.2f", p.total), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = StatusOccupiedRed)
                            }
                        }
                    }
                }
            }

            "SERVICIOS/GASTOS" -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (filteredExpenses.isEmpty()) {
                        item {
                            Text("No hay gastos registrados en este período.", fontSize = 12.sp, color = TextLightSecondary, modifier = Modifier.padding(8.dp))
                        }
                    }
                    items(filteredExpenses) { exp ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${exp.type} — ${exp.specification}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                    Text("Proveedor: ${exp.providerName} (${exp.frequency})", fontSize = 11.sp, color = TextDarkSecondary)
                                    if (exp.notes.isNotBlank()) {
                                        Text("Notas: ${exp.notes}", fontSize = 11.sp, color = TextDarkSecondary)
                                    }
                                }
                                Text(String.format(Locale.US, "-Q%.2f", exp.total), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = StatusOccupiedRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExpenseDialog) {
        ExpenseFormDialog(
            onDismiss = { showExpenseDialog = false },
            onConfirm = { type, spec, qty, price, prov, freq, notes ->
                onRecordExpense(type, spec, qty, price, prov, System.currentTimeMillis(), freq, notes, null)
                showExpenseDialog = false
            }
        )
    }
}

@Composable
fun ExpenseFormDialog(
    onDismiss: () -> Unit,
    onConfirm: (type: String, spec: String, qty: Int, unitPrice: Double, provider: String, freq: String, notes: String) -> Unit
) {
    var type by remember { mutableStateOf("REPARACION") }
    var spec by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("150") }
    var provider by remember { mutableStateOf("") }
    var freq by remember { mutableStateOf("UNICA") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Servicio / Gasto", fontWeight = FontWeight.Bold, color = HotelNavy) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Tipo de Servicio:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                listOf("PLOMERO", "ELECTRICISTA", "REPARACION", "MANTENIMIENTO", "OTRO").forEach { t ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { type = t }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(t, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Especificación del trabajo") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = provider, onValueChange = { provider = it }, label = { Text("Proveedor / Persona contratada") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = unitPrice, onValueChange = { unitPrice = it }, label = { Text("Total / Costo (Q)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))

                Text("Frecuencia:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { freq = "UNICA" }) {
                        RadioButton(selected = freq == "UNICA", onClick = { freq = "UNICA" })
                        Text("Única", fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { freq = "MENSUAL" }) {
                        RadioButton(selected = freq == "MENSUAL", onClick = { freq = "MENSUAL" })
                        Text("Mensual", fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Observaciones") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(type, spec, qty.toIntOrNull() ?: 1, unitPrice.toDoubleOrNull() ?: 0.0, provider, freq, notes)
                },
                enabled = spec.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
            ) {
                Text("REGISTRAR")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } }
    )
}

// -------------------------------------------------------------------------------------------------
// 9. DISPOSITIVOS
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteDispositivosView(
    devices: List<DeviceAuthorization>,
    onRenameDevice: (String, String) -> Unit,
    onRevokeDevice: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var renamingDevice by remember { mutableStateOf<DeviceAuthorization?>(null) }
    var newNameInput by remember { mutableStateOf("") }
    var revokingDevice by remember { mutableStateOf<DeviceAuthorization?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("DISPOSITIVOS AUTORIZADOS (${devices.size})", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = HotelNavy)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Al revocar un dispositivo, ese teléfono pierde acceso y se eliminan sus datos locales del hotel.",
            fontSize = 12.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(devices) { dev ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dev.deviceName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = HotelNavy)
                            Text(
                                text = dev.status,
                                color = if (dev.status == "ACTIVO") StatusAvailableGreen else StatusOccupiedRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ID Dispositivo: ${dev.deviceId}", fontSize = 11.sp, color = TextMuted)
                        Text("Fecha de vinculación: ${dateFormat.format(Date(dev.linkedAtEpoch))}", fontSize = 11.sp, color = TextMuted)
                        Text("Última sincronización: ${dateFormat.format(Date(dev.lastSyncEpoch))}", fontSize = 11.sp, color = TextMuted)
                        Text("Estado de conexión: ${if (dev.isOnline) "Conectado" else "Desconectado"}", fontSize = 11.sp, color = if (dev.isOnline) StatusAvailableGreen else TextMuted)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = {
                                    renamingDevice = dev
                                    newNameInput = dev.deviceName
                                },
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Renombrar", fontSize = 11.sp)
                            }

                            if (dev.status == "ACTIVO") {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { revokingDevice = dev },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusOccupiedRed),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("REVOCAR", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (renamingDevice != null) {
        AlertDialog(
            onDismissRequest = { renamingDevice = null },
            title = { Text("Renombrar Dispositivo", fontWeight = FontWeight.Bold, color = HotelNavy) },
            text = {
                OutlinedTextField(
                    value = newNameInput,
                    onValueChange = { newNameInput = it },
                    label = { Text("Nombre del dispositivo") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNameInput.isNotBlank()) {
                            onRenameDevice(renamingDevice!!.deviceId, newNameInput)
                            renamingDevice = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
                ) {
                    Text("GUARDAR")
                }
            },
            dismissButton = { TextButton(onClick = { renamingDevice = null }) { Text("CANCELAR") } }
        )
    }

    if (revokingDevice != null) {
        AlertDialog(
            onDismissRequest = { revokingDevice = null },
            title = { Text("⚠️ Revocar Dispositivo", fontWeight = FontWeight.Bold, color = StatusOccupiedRed) },
            text = {
                Text("¿Estás seguro de revocar el acceso a '${revokingDevice!!.deviceName}'? Ese teléfono perderá acceso a los datos del hotel inmediatamente. La información en Firestore se conservará intacta.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRevokeDevice(revokingDevice!!.deviceId)
                        revokingDevice = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusOccupiedRed)
                ) {
                    Text("REVOCAR ACCESO")
                }
            },
            dismissButton = { TextButton(onClick = { revokingDevice = null }) { Text("CANCELAR") } }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 10. CONFIGURACIÓN
// -------------------------------------------------------------------------------------------------
@Composable
fun GerenteConfiguracionView(
    config: HotelConfig,
    onUpdateConfig: (name: String, phone: String, address: String, logo: String?, msg: String, pin: String, extraPrice: Double, alertMin: Int, notifs: Map<String, Boolean>, managerInCharge: String, nextInvoiceCorrelative: Int) -> Unit
) {
    var hotelName by remember { mutableStateOf(config.hotelName) }
    var phone by remember { mutableStateOf(config.phone) }
    var address by remember { mutableStateOf(config.address) }
    var managerInCharge by remember { mutableStateOf(config.managerInCharge) }
    var nextInvoiceCorrelative by remember { mutableStateOf(config.nextInvoiceCorrelative.toString()) }
    var departureMessage by remember { mutableStateOf(config.departureMessage) }
    var pin by remember { mutableStateOf(config.gerentePin) }
    var extraTimePrice by remember { mutableStateOf(config.extraTimePricePerHour.toString()) }
    var alertMinutes by remember { mutableStateOf(config.stayEndingAlertMinutesBefore.toString()) }
    var logoUri by remember { mutableStateOf(config.logoUri) }

    var notifStayEnding by remember { mutableStateOf(config.notifyStayEnding) }
    var notifStayEnded by remember { mutableStateOf(config.notifyStayEnded) }
    var notifAllowExtra by remember { mutableStateOf(config.notifyAllowExtraTime) }
    var notifRes3h by remember { mutableStateOf(config.notifyReservation3h) }
    var notifResMod by remember { mutableStateOf(config.notifyReservationModified) }
    var notifResCancel by remember { mutableStateOf(config.notifyReservationCancelled) }
    var notifResNoShow by remember { mutableStateOf(config.notifyReservationNoShow1h) }
    var notifNetDrop by remember { mutableStateOf(config.notifyInternetDisconnected) }
    var notifNetRestore by remember { mutableStateOf(config.notifyInternetRestored) }
    var notifSyncPending by remember { mutableStateOf(config.notifySyncPending) }

    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            logoUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("CONFIGURACIÓN GENERAL DEL HOTEL", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = HotelNavy)

        // Logo del Hotel
        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Logotipo del Hotel", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (logoUri != null) {
                        AsyncImage(
                            model = logoUri,
                            contentDescription = "Logo Hotel",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, HotelGold, RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(HotelNavyLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Apartment, contentDescription = null, tint = HotelGold, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (logoUri != null) "Cambiar Logo" else "Seleccionar Logo", fontSize = 11.sp)
                        }

                        if (logoUri != null) {
                            TextButton(onClick = { logoUri = null }) {
                                Text("Quitar logotipo", fontSize = 11.sp, color = StatusOccupiedRed)
                            }
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Datos del Hotel e Información en Factura", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(value = hotelName, onValueChange = { hotelName = it }, label = { Text("Nombre del Hotel") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Teléfono de Contacto") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Dirección del Hotel") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = managerInCharge,
                    onValueChange = { managerInCharge = it },
                    label = { Text("Encargado en turno") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = nextInvoiceCorrelative,
                    onValueChange = { nextInvoiceCorrelative = it },
                    label = { Text("Próximo correlativo de factura") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = departureMessage, onValueChange = { departureMessage = it }, label = { Text("Mensaje de Despedida en Factura") }, modifier = Modifier.fillMaxWidth())
            }
        }

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Seguridad y Operación", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN de Acceso a Gerente") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth().testTag("input_config_pin")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = extraTimePrice,
                    onValueChange = { extraTimePrice = it },
                    label = { Text("Precio por hora extra (Q)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = alertMinutes,
                    onValueChange = { alertMinutes = it },
                    label = { Text("Anticipación para alerta de finalización (minutos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Notificaciones del Sistema", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HotelNavy)
                Spacer(modifier = Modifier.height(8.dp))

                NotifSwitchItem("Estadía próxima a finalizar", notifStayEnding) { notifStayEnding = it }
                NotifSwitchItem("Estadía finalizada", notifStayEnded) { notifStayEnded = it }
                NotifSwitchItem("Opción de agregar tiempo extra", notifAllowExtra) { notifAllowExtra = it }
                NotifSwitchItem("Recordatorio reservación 3 horas antes", notifRes3h) { notifRes3h = it }
                NotifSwitchItem("Reservación modificada", notifResMod) { notifResMod = it }
                NotifSwitchItem("Reservación cancelada", notifResCancel) { notifResCancel = it }
                NotifSwitchItem("Cliente no presentado (1 hora de retraso)", notifResNoShow) { notifResNoShow = it }
                NotifSwitchItem("Internet desconectado", notifNetDrop) { notifNetDrop = it }
                NotifSwitchItem("Internet restaurado", notifNetRestore) { notifNetRestore = it }
                NotifSwitchItem("Sincronización pendiente / completada", notifSyncPending) { notifSyncPending = it }
            }
        }

        Button(
            onClick = {
                val notifsMap = mapOf(
                    "notifyStayEnding" to notifStayEnding,
                    "notifyStayEnded" to notifStayEnded,
                    "notifyAllowExtraTime" to notifAllowExtra,
                    "notifyReservation3h" to notifRes3h,
                    "notifyReservationModified" to notifResMod,
                    "notifyReservationCancelled" to notifResCancel,
                    "notifyReservationNoShow1h" to notifResNoShow,
                    "notifyInternetDisconnected" to notifNetDrop,
                    "notifyInternetRestored" to notifNetRestore,
                    "notifySyncPending" to notifSyncPending
                )

                onUpdateConfig(
                    hotelName,
                    phone,
                    address,
                    logoUri,
                    departureMessage,
                    pin,
                    extraTimePrice.toDoubleOrNull() ?: 25.0,
                    alertMinutes.toIntOrNull() ?: 15,
                    notifsMap,
                    managerInCharge,
                    nextInvoiceCorrelative.toIntOrNull() ?: config.nextInvoiceCorrelative.toInt()
                )
                Toast.makeText(context, "Configuración guardada exitosamente.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("btn_save_config"),
            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy)
        ) {
            Text("GUARDAR CONFIGURACIÓN", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun NotifSwitchItem(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// -------------------------------------------------------------------------------------------------
// PIN DIALOG / SCREEN FOR GERENTE
// -------------------------------------------------------------------------------------------------
@Composable
fun GerentePinDialog(
    onDismiss: () -> Unit,
    onPinSubmit: (String) -> Boolean
) {
    var pinInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = HotelGoldDark)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Acceso a Gerente 🔐", fontWeight = FontWeight.Bold, color = HotelNavy)
            }
        },
        text = {
            Column {
                Text(
                    text = "El módulo Gerente está protegido mediante PIN de seguridad.",
                    fontSize = 13.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        isError = false
                    },
                    label = { Text("Ingresa el PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_gerente_pin")
                )
                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("PIN incorrecto. Intenta de nuevo.", color = StatusOccupiedRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val success = onPinSubmit(pinInput)
                    if (!success) {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                modifier = Modifier.testTag("btn_submit_gerente_pin")
            ) {
                Text("ENTRAR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR") }
        }
    )
}
