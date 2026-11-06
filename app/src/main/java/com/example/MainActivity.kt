package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ClientType
import com.example.model.PaymentMethod
import com.example.model.ReservationStatus
import com.example.model.RoomType
import com.example.model.Tariff
import com.example.ui.screens.*
import com.example.ui.theme.HotelRiveraTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.HotelViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {

    private val viewModel: HotelViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            HotelRiveraTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val auth = remember { FirebaseAuth.getInstance() }
                    var currentUser by remember { mutableStateOf(auth.currentUser) }

                    DisposableEffect(auth) {
                        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
                            currentUser = fbAuth.currentUser
                        }
                        auth.addAuthStateListener(listener)
                        onDispose {
                            auth.removeAuthStateListener(listener)
                        }
                    }

                    if (currentUser == null) {
                        SignInScreen(
                            onSignInSuccess = { user ->
                                currentUser = user
                            }
                        )
                    } else {
                        HotelRiveraApp(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onSignOut = {
                                auth.signOut()
                                currentUser = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HotelRiveraApp(
    viewModel: HotelViewModel,
    currentUser: FirebaseUser? = null,
    onSignOut: () -> Unit = {}
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val gerenteSection by viewModel.gerenteSection.collectAsStateWithLifecycle()
    val recepcionTab by viewModel.recepcionTab.collectAsStateWithLifecycle()

    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val pendingSyncCount by viewModel.pendingSyncOperations.collectAsStateWithLifecycle()
    val isAuthorized by viewModel.isAuthorized.collectAsStateWithLifecycle()

    val hotelConfig by viewModel.hotelConfig.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val tariffs by viewModel.tariffs.collectAsStateWithLifecycle()
    val stays by viewModel.stays.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val reservations by viewModel.reservations.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val inventoryMovements by viewModel.inventoryMovements.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val extraFeeConcepts by viewModel.extraFeeConcepts.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val activeGeneratedCode by viewModel.activeGeneratedCode.collectAsStateWithLifecycle()
    val activePdfInvoice by viewModel.activePdfInvoice.collectAsStateWithLifecycle()
    val selectedRoomForStay by viewModel.selectedRoomForStay.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showGerentePinModal by remember { mutableStateOf(false) }

    LaunchedEffect(isAuthorized) {
        if (!isAuthorized && (currentScreen == AppScreen.GERENTE_DASHBOARD || currentScreen == AppScreen.RECEPCION || currentScreen == AppScreen.GERENTE_LOGIN)) {
            viewModel.navigateTo(AppScreen.MAIN_HUB)
            Toast.makeText(context, "Acceso revocado para este dispositivo.", Toast.LENGTH_LONG).show()
        }
    }

    when (currentScreen) {
        AppScreen.MAIN_HUB -> {
            MainHubScreen(
                connectionStatus = connectionStatus,
                pendingSyncCount = pendingSyncCount,
                currentUserEmail = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: currentUser?.email,
                onSignOutClick = onSignOut,
                onSyncClick = { viewModel.requestSync() },
                onVincularDispositivoClick = { viewModel.navigateTo(AppScreen.DEVICE_LINK) },
                onGerenteClick = {
                    if (!isAuthorized) {
                        Toast.makeText(context, "Dispositivo no autorizado. Debes vincular este teléfono primero.", Toast.LENGTH_LONG).show()
                    } else if (viewModel.isGerenteUnlocked.value) {
                        viewModel.navigateTo(AppScreen.GERENTE_DASHBOARD)
                    } else {
                        showGerentePinModal = true
                    }
                },
                onRecepcionClick = {
                    if (!isAuthorized) {
                        Toast.makeText(context, "Dispositivo no autorizado. Debes vincular este teléfono primero.", Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.navigateTo(AppScreen.RECEPCION)
                    }
                }
            )
        }

        AppScreen.DEVICE_LINK -> {
            DeviceLinkScreen(
                connectionStatus = connectionStatus,
                pendingSyncCount = pendingSyncCount,
                onSyncClick = { viewModel.requestSync() },
                isDeviceAuthorized = isAuthorized,
                activeGeneratedCode = activeGeneratedCode,
                onGenerateCodeClick = { viewModel.generateNewLinkCode() },
                onLinkDeviceWithCode = { code, name -> viewModel.linkDeviceWithCode(code, name) },
                onBackClick = {
                    viewModel.clearGeneratedCode()
                    viewModel.navigateTo(AppScreen.MAIN_HUB)
                }
            )
        }

        AppScreen.GERENTE_LOGIN -> {
            showGerentePinModal = true
            MainHubScreen(
                connectionStatus = connectionStatus,
                pendingSyncCount = pendingSyncCount,
                onSyncClick = { viewModel.requestSync() },
                onVincularDispositivoClick = { viewModel.navigateTo(AppScreen.DEVICE_LINK) },
                onGerenteClick = { showGerentePinModal = true },
                onRecepcionClick = { viewModel.navigateTo(AppScreen.RECEPCION) }
            )
        }

        AppScreen.GERENTE_DASHBOARD -> {
            GerenteScreen(
                currentSection = gerenteSection,
                onSectionSelected = { viewModel.setGerenteSection(it) },
                connectionStatus = connectionStatus,
                pendingSyncCount = pendingSyncCount,
                onSyncClick = { viewModel.requestSync() },
                hotelConfig = hotelConfig,
                rooms = rooms,
                tariffs = tariffs,
                stays = stays,
                invoices = invoices,
                reservations = reservations,
                inventory = inventory,
                inventoryMovements = inventoryMovements,
                purchases = purchases,
                expenses = expenses,
                extraFeeConcepts = extraFeeConcepts,
                devices = devices,
                history = history,
                onAddRoom = { num, name, type, beds, feat -> viewModel.repository.addRoom(num, name, type, beds, feat) },
                onUpdateRoom = { id, num, name, type, beds, feat -> viewModel.repository.updateRoom(id, num, name, type, beds, feat) },
                onToggleRoomDisabled = { id, dis -> viewModel.repository.toggleRoomDisabled(id, dis) },
                onAddTariff = { title, dur, price -> viewModel.repository.addTariff(title, dur, price) },
                onUpdateTariff = { id, title, dur, price, act -> viewModel.repository.updateTariff(id, title, dur, price, act) },
                onCancelInvoice = { id, reason -> viewModel.repository.cancelInvoice(id, reason) },
                onModifyInvoice = { id, newName, notes -> viewModel.repository.modifyInvoice(id, newName, notes) },
                onOpenInvoicePdf = { inv -> viewModel.openInvoicePdf(inv) },
                onAddInventoryItem = { name, cat, qty, unit, buy, sell, amenity ->
                    viewModel.repository.addInventoryItem(name, cat, qty, unit, buy, sell, amenity)
                },
                onUpdateInventoryItem = { id, name, qty, unit, buy, sell, act, am ->
                    viewModel.repository.updateInventoryItem(id, name, qty, unit, buy, sell, act, am)
                },
                onRecordPurchase = { itemId, qty, price, sup, invNum, hasInv, photo, notes ->
                    viewModel.repository.recordPurchase(itemId, qty, price, sup, invNum, hasInv, photo, notes)
                },
                onRecordExpense = { type, spec, qty, price, prov, date, freq, notes, photo ->
                    viewModel.repository.recordExpenseService(type, spec, qty, price, prov, date, freq, notes, photo)
                },
                onAddExtraFeeConcept = { name, price -> viewModel.repository.addExtraFeeConcept(name, price) },
                onUpdateExtraFeeConcept = { id, name, price, act -> viewModel.repository.updateExtraFeeConcept(id, name, price, act) },
                onToggleExtraFeeConcept = { id, act -> viewModel.repository.toggleExtraFeeConcept(id, act) },
                onRenameDevice = { devId, newName -> viewModel.repository.renameDevice(devId, newName) },
                onRevokeDevice = { devId -> viewModel.repository.revokeDevice(devId) },
                onUpdateConfig = { name, phone, addr, logo, msg, pin, extraPrice, alertMin, notifs, mgr, corr ->
                    viewModel.repository.updateHotelConfig(name, phone, addr, logo, msg, pin, extraPrice, alertMin, notifs, mgr, corr.toLong())
                },
                onBackClick = {
                    // Locks Gerente and returns to Main Hub
                    viewModel.lockGerente()
                }
            )
        }

        AppScreen.RECEPCION -> {
            RecepcionScreen(
                currentTab = recepcionTab,
                onTabSelected = { viewModel.setRecepcionTab(it) },
                connectionStatus = connectionStatus,
                pendingSyncCount = pendingSyncCount,
                onSyncClick = { viewModel.requestSync() },
                rooms = rooms,
                tariffs = tariffs,
                stays = stays,
                reservations = reservations,
                inventory = inventory,
                extraFeeConcepts = extraFeeConcepts,
                selectedRoomForStay = selectedRoomForStay,
                onClearSelectedRoom = { viewModel.clearSelectedRoomForStay() },
                onStartStay = { roomNum, clientType, clientName, tariff, payMethod, rec, ch, ref, extras, prods ->
                    viewModel.repository.startStay(roomNum, clientType, clientName, tariff, payMethod, rec, ch, ref, extras, prods)
                },
                onAddExtraTime = { stayId -> viewModel.repository.addExtraTime(stayId, 1) },
                onChangeRoom = { stayId, newRoom -> viewModel.repository.changeRoom(stayId, newRoom) },
                onReleaseRoom = { stayId, isEarly -> viewModel.repository.releaseRoom(stayId, isEarly) },
                onConsumeAmenity = { itemId, qty, roomNum -> viewModel.repository.consumeAmenity(itemId, qty, roomNum) },
                onCreateReservation = { date, arrival, dur, roomNum, clientName, notes ->
                    viewModel.repository.createReservation(date, arrival, dur, roomNum, clientName, notes)
                },
                onModifyReservation = { id, roomNum, date, arrival, dur, title, clientName, notes ->
                    viewModel.repository.modifyReservationDetails(id, roomNum, date, arrival, dur, title, clientName, notes)
                },
                onUpdateReservationStatus = { id, status -> viewModel.repository.updateReservationStatus(id, status) },
                onOpenInvoicePdf = { inv -> viewModel.openInvoicePdf(inv) },
                onBackClick = { viewModel.navigateTo(AppScreen.MAIN_HUB) }
            )
        }
    }

    // Modal: PIN de Gerente
    if (showGerentePinModal) {
        GerentePinDialog(
            onDismiss = {
                showGerentePinModal = false
                if (currentScreen == AppScreen.GERENTE_LOGIN) {
                    viewModel.navigateTo(AppScreen.MAIN_HUB)
                }
            },
            onPinSubmit = { pin ->
                val ok = viewModel.unlockGerente(pin)
                if (ok) {
                    showGerentePinModal = false
                }
                ok
            }
        )
    }

    // Modal: PDF de Factura
    if (activePdfInvoice != null) {
        InvoicePdfViewerDialog(
            invoice = activePdfInvoice!!,
            onDismiss = { viewModel.closeInvoicePdf() }
        )
    }
}
