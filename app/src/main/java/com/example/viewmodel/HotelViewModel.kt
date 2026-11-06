package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HotelRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_HUB,
    DEVICE_LINK,
    GERENTE_LOGIN,
    GERENTE_DASHBOARD,
    RECEPCION
}

enum class GerenteSection(val title: String) {
    CONTROL_GENERAL("Control General"),
    HABITACIONES("Habitaciones"),
    PRECIOS("Precios"),
    FACTURAS("Facturas"),
    HISTORIAL("Historial"),
    INVENTARIO("Inventario"),
    COBROS_EXTRAS("Cobros Extras"),
    REGISTRO_ECONOMICO("Registro Económico"),
    DISPOSITIVOS("Dispositivos"),
    CONFIGURACION("Configuración")
}

enum class RecepcionTab(val title: String) {
    HABITACIONES("Habitaciones"),
    RESERVACIONES("Reservaciones")
}


class HotelViewModel(application: Application) : AndroidViewModel(application) {

    val repository = HotelRepository(application.applicationContext)

    val connectionStatus = repository.connectionStatus
    val isAuthorized = repository.isAuthorized
    val hotelConfig = repository.hotelConfig
    val rooms = repository.rooms
    val tariffs = repository.tariffs
    val stays = repository.stays
    val invoices = repository.invoices
    val reservations = repository.reservations
    val inventory = repository.inventory
    val inventoryMovements = repository.inventoryMovements
    val purchases = repository.purchases
    val expenses = repository.expenses
    val extraFeeConcepts = repository.extraFeeConcepts
    val devices = repository.devices
    val history = repository.history
    val linkCodes = repository.linkCodes
    val pendingSyncOperations = repository.pendingSyncOperations

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_HUB)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _gerenteSection = MutableStateFlow(GerenteSection.CONTROL_GENERAL)
    val gerenteSection: StateFlow<GerenteSection> = _gerenteSection.asStateFlow()

    private val _recepcionTab = MutableStateFlow(RecepcionTab.HABITACIONES)
    val recepcionTab: StateFlow<RecepcionTab> = _recepcionTab.asStateFlow()

    private val _isGerenteUnlocked = MutableStateFlow(false)
    val isGerenteUnlocked: StateFlow<Boolean> = _isGerenteUnlocked.asStateFlow()

    // Selected Invoice for PDF display / preview
    private val _activePdfInvoice = MutableStateFlow<Invoice?>(null)
    val activePdfInvoice: StateFlow<Invoice?> = _activePdfInvoice.asStateFlow()

    // Preselected room for starting stay from reception room tap
    private val _selectedRoomForStay = MutableStateFlow<Room?>(null)
    val selectedRoomForStay: StateFlow<Room?> = _selectedRoomForStay.asStateFlow()

    // Active Temp Link code generated
    private val _activeGeneratedCode = MutableStateFlow<TempLinkCode?>(null)
    val activeGeneratedCode: StateFlow<TempLinkCode?> = _activeGeneratedCode.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        if (screen == AppScreen.GERENTE_DASHBOARD && !_isGerenteUnlocked.value) {
            _currentScreen.value = AppScreen.GERENTE_LOGIN
            return
        }
        if (screen == AppScreen.MAIN_HUB && _currentScreen.value == AppScreen.GERENTE_DASHBOARD) {
            // Lock Gerente on exit as per Rule 18/5: "Al salir de Gerente, al volver a entrar debe solicitar nuevamente PIN"
            _isGerenteUnlocked.value = false
        }
        _currentScreen.value = screen
    }

    fun setGerenteSection(section: GerenteSection) {
        _gerenteSection.value = section
    }

    fun setRecepcionTab(tab: RecepcionTab) {
        _recepcionTab.value = tab
    }

    fun unlockGerente(pin: String): Boolean {
        if (pin.trim() == hotelConfig.value.gerentePin.trim()) {
            _isGerenteUnlocked.value = true
            _currentScreen.value = AppScreen.GERENTE_DASHBOARD
            return true
        }
        return false
    }

    fun lockGerente() {
        _isGerenteUnlocked.value = false
        _currentScreen.value = AppScreen.MAIN_HUB
    }

    fun requestSync() {
        repository.syncNow()
    }

    fun selectRoomForNewStay(room: Room) {
        _selectedRoomForStay.value = room
        _recepcionTab.value = RecepcionTab.HABITACIONES
        _currentScreen.value = AppScreen.RECEPCION
    }


    fun clearSelectedRoomForStay() {
        _selectedRoomForStay.value = null
    }

    fun openInvoicePdf(invoice: Invoice) {
        _activePdfInvoice.value = invoice
    }

    fun closeInvoicePdf() {
        _activePdfInvoice.value = null
    }

    fun generateNewLinkCode() {
        viewModelScope.launch {
            val code = repository.generateTempLinkCode()
            _activeGeneratedCode.value = code
        }
    }

    fun clearGeneratedCode() {
        _activeGeneratedCode.value = null
    }

    fun linkDeviceWithCode(code: String, deviceName: String): Result<String> {
        val result = repository.linkDeviceWithCode(code, deviceName)
        if (result.isSuccess) {
            _currentScreen.value = AppScreen.MAIN_HUB
        }
        return result
    }
}
