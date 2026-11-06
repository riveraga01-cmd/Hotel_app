package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import com.example.model.*
import com.example.util.NotificationHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HotelRepository(private val context: Context) {

    private val localStore = LocalStore(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.CONECTADO)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isAuthorized = MutableStateFlow(localStore.isDeviceAuthorized())
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _hotelConfig = MutableStateFlow(HotelConfig())
    val hotelConfig: StateFlow<HotelConfig> = _hotelConfig.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _tariffs = MutableStateFlow<List<Tariff>>(emptyList())
    val tariffs: StateFlow<List<Tariff>> = _tariffs.asStateFlow()

    private val _stays = MutableStateFlow<List<Stay>>(emptyList())
    val stays: StateFlow<List<Stay>> = _stays.asStateFlow()

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    private val _reservations = MutableStateFlow<List<Reservation>>(emptyList())
    val reservations: StateFlow<List<Reservation>> = _reservations.asStateFlow()

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _inventoryMovements = MutableStateFlow<List<InventoryMovement>>(emptyList())
    val inventoryMovements: StateFlow<List<InventoryMovement>> = _inventoryMovements.asStateFlow()

    private val _purchases = MutableStateFlow<List<InventoryPurchase>>(emptyList())
    val purchases: StateFlow<List<InventoryPurchase>> = _purchases.asStateFlow()

    private val _expenses = MutableStateFlow<List<ExpenseService>>(emptyList())
    val expenses: StateFlow<List<ExpenseService>> = _expenses.asStateFlow()

    private val _extraFeeConcepts = MutableStateFlow<List<ExtraFeeConcept>>(emptyList())
    val extraFeeConcepts: StateFlow<List<ExtraFeeConcept>> = _extraFeeConcepts.asStateFlow()

    private val _devices = MutableStateFlow<List<DeviceAuthorization>>(emptyList())
    val devices: StateFlow<List<DeviceAuthorization>> = _devices.asStateFlow()

    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    private val _linkCodes = MutableStateFlow<List<TempLinkCode>>(emptyList())
    val linkCodes: StateFlow<List<TempLinkCode>> = _linkCodes.asStateFlow()

    private val _pendingSyncOperations = MutableStateFlow(0)
    val pendingSyncOperations: StateFlow<Int> = _pendingSyncOperations.asStateFlow()

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private var isFirebaseAvailable = false
    private var currentDeviceId = localStore.getDeviceId()
    private var currentDeviceName = localStore.getDeviceName()

    init {
        NotificationHelper.initNotificationChannels(context)
        loadInitialOrPersistedData()
        setupNetworkMonitoring()
        initFirebase()
        startStayMonitorLoop()
    }

    fun getDeviceId(): String = currentDeviceId
    fun getDeviceName(): String = currentDeviceName

    private fun loadInitialOrPersistedData() {
        val initialRooms = listOf(
            Room(
                id = "room_101",
                number = "101",
                name = "Habitación 101",
                type = RoomType.DOBLE,
                bedCount = 2,
                features = "2 camas matrimoniales, baño privado, ventilador, Smart TV",
                status = RoomStatus.DISPONIBLE
            ),
            Room(
                id = "room_102",
                number = "102",
                name = "Habitación 102",
                type = RoomType.INDIVIDUAL,
                bedCount = 1,
                features = "1 cama matrimonial, baño privado, ventilador, TV cable",
                status = RoomStatus.DISPONIBLE
            ),
            Room(
                id = "room_103",
                number = "103",
                name = "Habitación 103",
                type = RoomType.INDIVIDUAL,
                bedCount = 1,
                features = "1 cama matrimonial, baño privado, ventilador, TV cable",
                status = RoomStatus.DISPONIBLE
            )
        )

        val stored = localStore.loadSnapshot()
        if (stored != null) {
            _hotelConfig.value = stored.config
            // Ensure default rooms 101, 102, 103 are never wiped out by any previous incomplete save
            val roomMap = stored.rooms.associateBy { it.number }.toMutableMap()
            for (dr in initialRooms) {
                if (!roomMap.containsKey(dr.number)) {
                    roomMap[dr.number] = dr
                }
            }
            _rooms.value = roomMap.values.sortedBy { it.number }
            _tariffs.value = stored.tariffs
            _stays.value = stored.stays
            _invoices.value = stored.invoices
            _reservations.value = stored.reservations
            _inventory.value = stored.inventory
            _inventoryMovements.value = stored.movements
            _purchases.value = stored.purchases
            _expenses.value = stored.expenses
            _extraFeeConcepts.value = stored.extraFees
            _devices.value = stored.devices
            _history.value = stored.history
            _linkCodes.value = stored.linkCodes
        } else {

            // Initial tariffs
            val initialTariffs = listOf(
                Tariff(id = "tariff_1h", title = "1 hora", durationMinutes = 60, price = 45.0, isActive = true),
                Tariff(id = "tariff_3h", title = "3 horas", durationMinutes = 180, price = 75.0, isActive = true),
                Tariff(id = "tariff_noche", title = "Noche completa", durationMinutes = 720, price = 125.0, isActive = true),
                Tariff(id = "tariff_noche_fam", title = "Noche completa familiar", durationMinutes = 720, price = 175.0, isActive = true)
            )

            // Initial Extra Fee Concepts (Requirement 8)
            val initialFees = listOf(
                ExtraFeeConcept(id = "fee_huesped", name = "Huésped adicional", defaultPrice = 50.0),
                ExtraFeeConcept(id = "fee_cama", name = "Cama extra", defaultPrice = 30.0),
                ExtraFeeConcept(id = "fee_1", name = "Daño de mobiliario", defaultPrice = 150.0),
                ExtraFeeConcept(id = "fee_2", name = "Manchas en sábanas / toallas", defaultPrice = 50.0),
                ExtraFeeConcept(id = "fee_3", name = "Daños a objetos / vasos", defaultPrice = 30.0),
                ExtraFeeConcept(id = "fee_4", name = "Pérdida de control remoto", defaultPrice = 75.0),
                ExtraFeeConcept(id = "fee_5", name = "Limpieza especial por desorden", defaultPrice = 60.0)
            )


            // Initial Inventory including Amenities (Jabón, Shampoo, Papel)
            val initialInventory = listOf(
                InventoryItem(id = "inv_jabon", name = "Jabón de Manos", category = "JABON", quantity = 50.0, unit = "pastilla", purchasePrice = 1.5, salePrice = 0.0, isAmenity = true),
                InventoryItem(id = "inv_shampoo", name = "Shampoo Sobrecito", category = "SHAMPOO", quantity = 60.0, unit = "sobre", purchasePrice = 1.0, salePrice = 0.0, isAmenity = true),
                InventoryItem(id = "inv_papel", name = "Papel Sanitario", category = "PAPEL", quantity = 48.0, unit = "rollo", purchasePrice = 2.0, salePrice = 0.0, isAmenity = true),
                InventoryItem(id = "inv_agua", name = "Agua Pura Botella 600ml", category = "BEBIDAS", quantity = 24.0, unit = "botella", purchasePrice = 3.0, salePrice = 6.0, isAmenity = false),
                InventoryItem(id = "inv_gaseosa", name = "Bebida Gaseosa 500ml", category = "BEBIDAS", quantity = 20.0, unit = "botella", purchasePrice = 5.0, salePrice = 10.0, isAmenity = false),
                InventoryItem(id = "inv_snack", name = "Snack / Galletas", category = "PRODUCTOS_VENTA", quantity = 15.0, unit = "paquete", purchasePrice = 4.0, salePrice = 8.0, isAmenity = false),
                InventoryItem(id = "inv_toalla", name = "Toalla de Baño Blanca", category = "TOALLAS", quantity = 12.0, unit = "unidad", purchasePrice = 35.0, salePrice = 0.0, isAmenity = false),
                InventoryItem(id = "inv_cobija", name = "Cobija / Frazada Térmica", category = "COBIJAS", quantity = 8.0, unit = "unidad", purchasePrice = 60.0, salePrice = 0.0, isAmenity = false)
            )

            val initialDevice = DeviceAuthorization(
                id = currentDeviceId,
                deviceId = currentDeviceId,
                deviceName = currentDeviceName,
                linkedAtEpoch = System.currentTimeMillis(),
                lastSyncEpoch = System.currentTimeMillis(),
                isOnline = true,
                isAuthorized = true,
                status = "ACTIVO"
            )

            val initialHistory = listOf(
                HistoryEntry(
                    id = UUID.randomUUID().toString(),
                    dateEpoch = System.currentTimeMillis(),
                    operationType = "VINCULAR_DISPOSITIVO",
                    title = "Terminal Principal Autorizado",
                    description = "Se configuró el terminal principal de Hotel Rivera",
                    userOrRole = "GERENTE",
                    deviceName = currentDeviceName
                )
            )

            _rooms.value = initialRooms
            _tariffs.value = initialTariffs
            _extraFeeConcepts.value = initialFees
            _inventory.value = initialInventory
            _devices.value = listOf(initialDevice)
            _history.value = initialHistory

            saveToLocalStorage()
        }
    }

    private fun setupNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                mainHandler.post {
                    val prevStatus = _connectionStatus.value
                    if (prevStatus == ConnectionStatus.SIN_CONEXION) {
                        _connectionStatus.value = if (_pendingSyncOperations.value > 0) ConnectionStatus.SINCRONIZANDO else ConnectionStatus.CONECTADO
                        if (_hotelConfig.value.notifyInternetRestored) {
                            NotificationHelper.showNetworkStatusNotification(context, true)
                        }
                        syncNow()
                    } else {
                        _connectionStatus.value = ConnectionStatus.CONECTADO
                    }
                }
            }

            override fun onLost(network: Network) {
                mainHandler.post {
                    _connectionStatus.value = ConnectionStatus.SIN_CONEXION
                    if (_hotelConfig.value.notifyInternetDisconnected) {
                        NotificationHelper.showNetworkStatusNotification(context, false)
                    }
                }
            }
        })
    }

    private fun getFirestoreInstance(): FirebaseFirestore {
        return try {
            val dbIdRes = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
            val databaseId = if (dbIdRes != 0) context.getString(dbIdRes) else null
            if (!databaseId.isNullOrBlank() && databaseId != "(default)") {
                FirebaseFirestore.getInstance(databaseId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (_: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            val db = getFirestoreInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                .build()
            db.firestoreSettings = settings
            isFirebaseAvailable = true
            setupFirestoreRealtimeSync()
        } catch (_: Exception) {
            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                isFirebaseAvailable = true
                setupFirestoreRealtimeSync()
            } catch (_: Exception) {
                isFirebaseAvailable = false
            }
        }
    }

    private fun setupFirestoreRealtimeSync() {
        if (!isFirebaseAvailable) return
        try {
            val db = getFirestoreInstance()
            val hotelId = _hotelConfig.value.hotelId
            val hotelRef = db.collection("hotels").document(hotelId)

            // Listen to device authorizations in real-time
            val devSub = hotelRef.collection("devices").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val devList = snapshot.documents.mapNotNull { doc ->
                    try {
                        DeviceAuthorization(
                            id = doc.id,
                            deviceId = doc.getString("deviceId") ?: doc.id,
                            deviceName = doc.getString("deviceName") ?: "Dispositivo",
                            linkedAtEpoch = doc.getLong("linkedAtEpoch") ?: System.currentTimeMillis(),
                            lastSyncEpoch = doc.getLong("lastSyncEpoch") ?: System.currentTimeMillis(),
                            isOnline = doc.getBoolean("isOnline") ?: true,
                            isAuthorized = doc.getBoolean("isAuthorized") ?: true,
                            status = doc.getString("status") ?: "ACTIVO"
                        )
                    } catch (_: Exception) { null }
                }
                if (devList.isNotEmpty()) {
                    _devices.value = devList
                    // Check if current device was revoked from another phone
                    val thisDev = devList.find { it.deviceId == currentDeviceId }
                    if (thisDev != null && thisDev.status == "REVOCADO") {
                        mainHandler.post { revokeThisDeviceLocally() }
                    }
                }
            }
            activeListeners.add(devSub)

            // Push all local rooms to Firestore to ensure complete inventory on cloud
            pushAllRoomsToFirestore()

            // Listen to rooms changes in real-time
            val roomsSub = hotelRef.collection("rooms").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val rList = snapshot.documents.mapNotNull { doc ->
                    try {
                        Room(
                            id = doc.id,
                            number = doc.getString("number") ?: "",
                            name = doc.getString("name") ?: "",
                            type = runCatching { RoomType.valueOf(doc.getString("type") ?: "INDIVIDUAL") }.getOrDefault(RoomType.INDIVIDUAL),
                            bedCount = (doc.getLong("bedCount") ?: 1L).toInt(),
                            features = doc.getString("features") ?: "",
                            status = runCatching { RoomStatus.valueOf(doc.getString("status") ?: "DISPONIBLE") }.getOrDefault(RoomStatus.DISPONIBLE),
                            currentStayId = doc.getString("currentStayId"),
                            hasHistory = doc.getBoolean("hasHistory") ?: false,
                            updatedAtEpoch = doc.getLong("updatedAtEpoch") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) { null }
                }
                if (rList.isNotEmpty()) {
                    // CRITICAL: Merge per room ID and number - NEVER wipe out or overwrite the complete inventory
                    val currentMap = _rooms.value.associateBy { it.id }.toMutableMap()
                    for (remote in rList) {
                        if (remote.number.isNotBlank()) {
                            val existing = currentMap[remote.id] ?: currentMap.values.find { it.number == remote.number }
                            if (existing != null) {
                                currentMap[existing.id] = remote.copy(id = existing.id)
                            } else {
                                currentMap[remote.id] = remote
                            }
                        }
                    }
                    _rooms.value = currentMap.values.sortedBy { it.number }
                    saveToLocalStorage()
                }
            }
            activeListeners.add(roomsSub)

            // Listen to stays changes in real-time
            val staysSub = hotelRef.collection("stays").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val sList = snapshot.documents.mapNotNull { doc ->
                    try {
                        Stay(
                            id = doc.id,
                            roomNumber = doc.getString("roomNumber") ?: "",
                            clientType = runCatching { ClientType.valueOf(doc.getString("clientType") ?: "CONSUMIDOR_FINAL") }.getOrDefault(ClientType.CONSUMIDOR_FINAL),
                            clientName = doc.getString("clientName") ?: "",
                            invoiceNumber = doc.getString("invoiceNumber") ?: "",
                            tariffId = doc.getString("tariffId") ?: "",
                            tariffTitle = doc.getString("tariffTitle") ?: "",
                            basePrice = doc.getDouble("basePrice") ?: 0.0,
                            checkInEpoch = doc.getLong("checkInEpoch") ?: System.currentTimeMillis(),
                            durationMinutes = (doc.getLong("durationMinutes") ?: 60L).toInt(),
                            scheduledCheckOutEpoch = doc.getLong("scheduledCheckOutEpoch") ?: System.currentTimeMillis(),
                            extraHours = (doc.getLong("extraHours") ?: 0L).toInt(),
                            extraChargeTotal = doc.getDouble("extraChargeTotal") ?: 0.0,
                            actualCheckOutEpoch = doc.getLong("actualCheckOutEpoch"),
                            status = runCatching { StayStatus.valueOf(doc.getString("status") ?: "ACTIVE") }.getOrDefault(StayStatus.ACTIVE),
                            isEarlyDeparture = doc.getBoolean("isEarlyDeparture") ?: false,
                            deviceName = doc.getString("deviceName") ?: "",
                            updatedAtEpoch = doc.getLong("updatedAtEpoch") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) { null }
                }
                _stays.value = sList
            }
            activeListeners.add(staysSub)

            // Listen to reservations changes in real-time
            val resSub = hotelRef.collection("reservations").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val resList = snapshot.documents.mapNotNull { doc ->
                    try {
                        Reservation(
                            id = doc.id,
                            reservationNumber = doc.getString("reservationNumber") ?: "",
                            dateEpoch = doc.getLong("dateEpoch") ?: System.currentTimeMillis(),
                            arrivalTimeEpoch = doc.getLong("arrivalTimeEpoch") ?: System.currentTimeMillis(),
                            durationMinutes = (doc.getLong("durationMinutes") ?: 180L).toInt(),
                            tariffTitle = doc.getString("tariffTitle") ?: "3 horas",
                            roomNumber = doc.getString("roomNumber") ?: "",
                            clientType = runCatching { ClientType.valueOf(doc.getString("clientType") ?: "CONSUMIDOR_FINAL") }.getOrDefault(ClientType.CONSUMIDOR_FINAL),
                            clientName = doc.getString("clientName") ?: "",
                            status = runCatching { ReservationStatus.valueOf(doc.getString("status") ?: "PENDIENTE") }.getOrDefault(ReservationStatus.PENDIENTE),
                            notes = doc.getString("notes") ?: "",
                            createdAtEpoch = doc.getLong("createdAtEpoch") ?: System.currentTimeMillis(),
                            updatedAtEpoch = doc.getLong("updatedAtEpoch") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) { null }
                }
                _reservations.value = resList
            }
            activeListeners.add(resSub)

            // Listen to hotel config in real-time
            val configSub = hotelRef.collection("config").document("main").addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) return@addSnapshotListener
                try {
                    _hotelConfig.value = _hotelConfig.value.copy(
                        hotelName = doc.getString("hotelName") ?: _hotelConfig.value.hotelName,
                        phone = doc.getString("phone") ?: _hotelConfig.value.phone,
                        address = doc.getString("address") ?: _hotelConfig.value.address,
                        logoUri = doc.getString("logoUri"),
                        departureMessage = doc.getString("departureMessage") ?: _hotelConfig.value.departureMessage,
                        gerentePin = doc.getString("gerentePin") ?: _hotelConfig.value.gerentePin,
                        extraTimePricePerHour = doc.getDouble("extraTimePricePerHour") ?: _hotelConfig.value.extraTimePricePerHour
                    )
                } catch (_: Exception) {}
            }
            activeListeners.add(configSub)

            // Listen to invoices in real-time
            val invoicesSub = hotelRef.collection("invoices").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val invList = snapshot.documents.mapNotNull { doc ->
                    try {
                        Invoice(
                            id = doc.id,
                            invoiceNumber = doc.getString("invoiceNumber") ?: "",
                            dateEpoch = doc.getLong("dateEpoch") ?: System.currentTimeMillis(),
                            checkInEpoch = doc.getLong("checkInEpoch") ?: System.currentTimeMillis(),
                            scheduledCheckOutEpoch = doc.getLong("scheduledCheckOutEpoch") ?: System.currentTimeMillis(),
                            clientType = runCatching { ClientType.valueOf(doc.getString("clientType") ?: "CONSUMIDOR_FINAL") }.getOrDefault(ClientType.CONSUMIDOR_FINAL),
                            clientName = doc.getString("clientName") ?: "",
                            roomNumber = doc.getString("roomNumber") ?: "",
                            tariffTitle = doc.getString("tariffTitle") ?: "",
                            durationMinutes = (doc.getLong("durationMinutes") ?: 60L).toInt(),
                            basePrice = doc.getDouble("basePrice") ?: 0.0,
                            extraHours = (doc.getLong("extraHours") ?: 0L).toInt(),
                            extraChargeTotal = doc.getDouble("extraChargeTotal") ?: 0.0,
                            total = doc.getDouble("total") ?: 0.0,
                            paymentMethod = runCatching { PaymentMethod.valueOf(doc.getString("paymentMethod") ?: "EFECTIVO") }.getOrDefault(PaymentMethod.EFECTIVO),
                            amountReceived = doc.getDouble("amountReceived") ?: 0.0,
                            change = doc.getDouble("change") ?: 0.0,
                            transferReference = doc.getString("transferReference") ?: "",
                            status = runCatching { InvoiceStatus.valueOf(doc.getString("status") ?: "ACTIVA") }.getOrDefault(InvoiceStatus.ACTIVA),
                            cancellationReason = doc.getString("cancellationReason") ?: "",
                            modificationNotes = doc.getString("modificationNotes") ?: "",
                            previousTotal = doc.getDouble("previousTotal"),
                            registeredByDevice = doc.getString("registeredByDevice") ?: "",
                            updatedAtEpoch = doc.getLong("updatedAtEpoch") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) { null }
                }
                if (invList.isNotEmpty()) {
                    _invoices.value = invList
                }
            }
            activeListeners.add(invoicesSub)

            // Listen to link_codes in real-time
            val codesSub = hotelRef.collection("link_codes").addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val codeList = snapshot.documents.mapNotNull { doc ->
                    try {
                        TempLinkCode(
                            code = doc.getString("code") ?: doc.id,
                            hotelId = doc.getString("hotelId") ?: hotelId,
                            expiresAtEpoch = doc.getLong("expiresAtEpoch") ?: 0L,
                            isUsed = doc.getBoolean("isUsed") ?: false,
                            createdAtEpoch = doc.getLong("createdAtEpoch") ?: 0L
                        )
                    } catch (_: Exception) { null }
                }
                if (codeList.isNotEmpty()) {
                    _linkCodes.value = codeList
                }
            }
            activeListeners.add(codesSub)

        } catch (_: Exception) {}
    }

    private fun saveToLocalStorage() {
        localStore.saveSnapshot(
            config = _hotelConfig.value,
            rooms = _rooms.value,
            tariffs = _tariffs.value,
            stays = _stays.value,
            invoices = _invoices.value,
            reservations = _reservations.value,
            inventory = _inventory.value,
            movements = _inventoryMovements.value,
            purchases = _purchases.value,
            expenses = _expenses.value,
            extraFees = _extraFeeConcepts.value,
            devices = _devices.value,
            history = _history.value,
            linkCodes = _linkCodes.value
        )
    }

    fun syncNow() {
        coroutineScope.launch {
            _connectionStatus.value = ConnectionStatus.SINCRONIZANDO
            delay(350)

            val thisDev = _devices.value.find { it.deviceId == currentDeviceId }
            if (thisDev != null && thisDev.status == "REVOCADO") {
                withContext(Dispatchers.Main) {
                    revokeThisDeviceLocally()
                }
                return@launch
            }

            if (isFirebaseAvailable) {
                try {
                    val db = getFirestoreInstance()
                    val hotelId = _hotelConfig.value.hotelId
                    val hotelRef = db.collection("hotels").document(hotelId)

                    // Push config
                    val configMap = hashMapOf(
                        "hotelName" to _hotelConfig.value.hotelName,
                        "phone" to _hotelConfig.value.phone,
                        "address" to _hotelConfig.value.address,
                        "logoUri" to (_hotelConfig.value.logoUri ?: ""),
                        "departureMessage" to _hotelConfig.value.departureMessage,
                        "gerentePin" to _hotelConfig.value.gerentePin,
                        "extraTimePricePerHour" to _hotelConfig.value.extraTimePricePerHour,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    hotelRef.collection("config").document("main").set(configMap, SetOptions.merge())

                    // Push current device ping
                    val devMap = hashMapOf(
                        "deviceId" to currentDeviceId,
                        "deviceName" to currentDeviceName,
                        "lastSyncEpoch" to System.currentTimeMillis(),
                        "isOnline" to true,
                        "isAuthorized" to true,
                        "status" to "ACTIVO"
                    )
                    hotelRef.collection("devices").document(currentDeviceId).set(devMap, SetOptions.merge())

                    // Ensure all rooms exist in Firestore
                    pushAllRoomsToFirestore()
                } catch (_: Exception) {}
            }

            _pendingSyncOperations.value = 0
            _connectionStatus.value = ConnectionStatus.CONECTADO

            _devices.value = _devices.value.map {
                if (it.deviceId == currentDeviceId) it.copy(lastSyncEpoch = System.currentTimeMillis(), isOnline = true)
                else it
            }
            saveToLocalStorage()
        }
    }

    // --- Device Authorization & Real Firestore Linking ---

    fun generateTempLinkCode(): TempLinkCode {
        val randomNum = (1000..9999).random()
        val codeStr = "HR-$randomNum"
        val tempCode = TempLinkCode(
            code = codeStr,
            hotelId = _hotelConfig.value.hotelId,
            expiresAtEpoch = System.currentTimeMillis() + (15 * 60 * 1000L),
            isUsed = false
        )
        _linkCodes.value = _linkCodes.value + tempCode

        addHistoryEntry(
            operationType = "VINCULAR_DISPOSITIVO",
            title = "Código de Vinculación Generado",
            description = "Se generó código temporal $codeStr válido por 15 minutos",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()

        // Push code to Firestore
        if (isFirebaseAvailable) {
            coroutineScope.launch {
                try {
                    val db = getFirestoreInstance()
                    val hotelId = _hotelConfig.value.hotelId
                    val codeMap = hashMapOf(
                        "code" to codeStr,
                        "hotelId" to hotelId,
                        "expiresAtEpoch" to tempCode.expiresAtEpoch,
                        "isUsed" to false,
                        "createdAtEpoch" to tempCode.createdAtEpoch
                    )
                    db.collection("hotels").document(hotelId).collection("link_codes").document(codeStr).set(codeMap)
                } catch (_: Exception) {}
            }
        }

        syncNow()
        return tempCode
    }

    fun linkDeviceWithCode(enteredCode: String, newDeviceName: String): Result<String> {
        val cleanCode = enteredCode.trim().uppercase()

        // 1. Check in local link codes or Firestore
        var foundCode = _linkCodes.value.find { it.code.uppercase() == cleanCode }

        if (foundCode == null && isFirebaseAvailable) {
            try {
                val db = getFirestoreInstance()
                val hotelId = _hotelConfig.value.hotelId
                // Synchronous task wait on IO thread
                runBlocking(Dispatchers.IO) {
                    try {
                        val task = db.collection("hotels").document(hotelId).collection("link_codes").document(cleanCode).get()
                        val doc = com.google.android.gms.tasks.Tasks.await(task, 4, java.util.concurrent.TimeUnit.SECONDS)
                        if (doc != null && doc.exists()) {
                            foundCode = TempLinkCode(
                                code = cleanCode,
                                hotelId = hotelId,
                                expiresAtEpoch = doc.getLong("expiresAtEpoch") ?: 0L,
                                isUsed = doc.getBoolean("isUsed") ?: false,
                                createdAtEpoch = doc.getLong("createdAtEpoch") ?: 0L
                            )
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        if (foundCode == null) {
            return Result.failure(Exception("El código no existe o no es válido para Hotel Rivera."))
        }
        if (foundCode!!.isUsed) {
            return Result.failure(Exception("Este código ya fue utilizado previamente."))
        }
        if (System.currentTimeMillis() > foundCode!!.expiresAtEpoch) {
            return Result.failure(Exception("El código ha expirado. Por favor genera un nuevo código."))
        }

        // Mark code as used locally and in Firestore
        _linkCodes.value = _linkCodes.value.map {
            if (it.code == foundCode!!.code) it.copy(isUsed = true) else it
        }

        localStore.setDeviceAuthorized(true)
        localStore.setDeviceName(newDeviceName.ifBlank { "Teléfono Recepción 2" })
        currentDeviceName = localStore.getDeviceName()
        _isAuthorized.value = true

        val newAuth = DeviceAuthorization(
            id = currentDeviceId,
            deviceId = currentDeviceId,
            deviceName = currentDeviceName,
            linkedAtEpoch = System.currentTimeMillis(),
            lastSyncEpoch = System.currentTimeMillis(),
            isOnline = true,
            isAuthorized = true,
            status = "ACTIVO"
        )
        _devices.value = _devices.value.filter { it.deviceId != currentDeviceId } + newAuth

        addHistoryEntry(
            operationType = "VINCULAR_DISPOSITIVO",
            title = "Dispositivo Vinculado Correctamente",
            description = "Dispositivo '$currentDeviceName' autorizado mediante código $cleanCode",
            userOrRole = "RECEPCIÓN"
        )

        saveToLocalStorage()

        // Sync to Firestore
        if (isFirebaseAvailable) {
            coroutineScope.launch {
                try {
                    val db = getFirestoreInstance()
                    val hotelId = _hotelConfig.value.hotelId
                    val hotelRef = db.collection("hotels").document(hotelId)

                    // Mark code as used in Firestore
                    hotelRef.collection("link_codes").document(cleanCode).update("isUsed", true)

                    // Register new device in Firestore
                    val devData = hashMapOf(
                        "deviceId" to currentDeviceId,
                        "deviceName" to currentDeviceName,
                        "linkedAtEpoch" to newAuth.linkedAtEpoch,
                        "lastSyncEpoch" to newAuth.lastSyncEpoch,
                        "isOnline" to true,
                        "isAuthorized" to true,
                        "status" to "ACTIVO"
                    )
                    hotelRef.collection("devices").document(currentDeviceId).set(devData, SetOptions.merge())
                } catch (_: Exception) {}
            }
        }

        syncNow()
        return Result.success("Dispositivo vinculado correctamente")
    }

    fun renameDevice(deviceId: String, newName: String) {
        _devices.value = _devices.value.map {
            if (it.deviceId == deviceId) it.copy(deviceName = newName) else it
        }
        if (deviceId == currentDeviceId) {
            currentDeviceName = newName
            localStore.setDeviceName(newName)
        }
        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = "Nombre de Dispositivo Modificado",
            description = "Dispositivo $deviceId renombrado a '$newName'",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()

        if (isFirebaseAvailable) {
            coroutineScope.launch {
                try {
                    val db = getFirestoreInstance()
                    val hotelId = _hotelConfig.value.hotelId
                    db.collection("hotels").document(hotelId).collection("devices").document(deviceId).update("deviceName", newName)
                } catch (_: Exception) {}
            }
        }
        syncNow()
    }

    fun revokeDevice(deviceId: String) {
        _devices.value = _devices.value.map {
            if (it.deviceId == deviceId) it.copy(status = "REVOCADO", isAuthorized = false) else it
        }
        addHistoryEntry(
            operationType = "REVOCAR_DISPOSITIVO",
            title = "Dispositivo Revocado",
            description = "Acceso revocado para el dispositivo ID $deviceId",
            userOrRole = "GERENTE"
        )

        // Update in Firestore so the other device loses access immediately
        if (isFirebaseAvailable) {
            coroutineScope.launch {
                try {
                    val db = getFirestoreInstance()
                    val hotelId = _hotelConfig.value.hotelId
                    db.collection("hotels").document(hotelId).collection("devices").document(deviceId)
                        .update("status", "REVOCADO", "isAuthorized", false)
                } catch (_: Exception) {}
            }
        }

        if (deviceId == currentDeviceId) {
            revokeThisDeviceLocally()
        } else {
            saveToLocalStorage()
            syncNow()
        }
    }

    private fun revokeThisDeviceLocally() {
        localStore.clearLocalHotelData()
        _isAuthorized.value = false
    }

    // --- Rooms Administration ---

    fun addRoom(number: String, name: String, type: RoomType, bedCount: Int, features: String) {
        val newRoom = Room(
            id = "room_${UUID.randomUUID()}",
            number = number.trim(),
            name = name.ifBlank { "Habitación $number" }.trim(),
            type = type,
            bedCount = bedCount,
            features = features.trim(),
            status = RoomStatus.DISPONIBLE,
            updatedAtEpoch = System.currentTimeMillis()
        )
        _rooms.value = _rooms.value + newRoom
        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = "Nueva Habitación Agregada",
            description = "Habitación ${newRoom.number} ($type, $bedCount camas)",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()
        pushRoomToFirestore(newRoom)
        syncNow()
    }

    fun updateRoom(roomId: String, number: String, name: String, type: RoomType, bedCount: Int, features: String) {
        var updatedRoom: Room? = null
        _rooms.value = _rooms.value.map { r ->
            if (r.id == roomId) {
                val ur = r.copy(
                    number = number.trim(),
                    name = name.trim(),
                    type = type,
                    bedCount = bedCount,
                    features = features.trim(),
                    updatedAtEpoch = System.currentTimeMillis()
                )
                updatedRoom = ur
                ur
            } else r
        }
        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = "Habitación Modificada",
            description = "Habitación $number actualizada por Gerente",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()
        if (updatedRoom != null) pushRoomToFirestore(updatedRoom!!)
        syncNow()
    }

    fun toggleRoomDisabled(roomId: String, disable: Boolean) {
        var target: Room? = null
        _rooms.value = _rooms.value.map { r ->
            if (r.id == roomId) {
                val ur = r.copy(
                    status = if (disable) RoomStatus.DESHABILITADA else RoomStatus.DISPONIBLE,
                    updatedAtEpoch = System.currentTimeMillis()
                )
                target = ur
                ur
            } else r
        }
        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = if (disable) "Habitación Deshabilitada" else "Habitación Reactivada",
            description = "Habitación ${target?.number ?: roomId} ${if (disable) "deshabilitada" else "reactivada"}",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()
        if (target != null) pushRoomToFirestore(target!!)
        syncNow()
    }

    private fun pushRoomToFirestore(room: Room) {
        if (!isFirebaseAvailable) return
        coroutineScope.launch {
            try {
                val db = getFirestoreInstance()
                val hotelId = _hotelConfig.value.hotelId
                val data = hashMapOf(
                    "number" to room.number,
                    "name" to room.name,
                    "type" to room.type.name,
                    "bedCount" to room.bedCount,
                    "features" to room.features,
                    "status" to room.status.name,
                    "currentStayId" to (room.currentStayId ?: ""),
                    "hasHistory" to room.hasHistory,
                    "updatedAtEpoch" to room.updatedAtEpoch
                )
                db.collection("hotels").document(hotelId).collection("rooms").document(room.id).set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    fun pushAllRoomsToFirestore() {
        if (!isFirebaseAvailable) return
        coroutineScope.launch {
            try {
                val db = getFirestoreInstance()
                val hotelId = _hotelConfig.value.hotelId
                val batch = db.batch()
                for (room in _rooms.value) {
                    val docRef = db.collection("hotels").document(hotelId).collection("rooms").document(room.id)
                    val data = hashMapOf(
                        "number" to room.number,
                        "name" to room.name,
                        "type" to room.type.name,
                        "bedCount" to room.bedCount,
                        "features" to room.features,
                        "status" to room.status.name,
                        "currentStayId" to (room.currentStayId ?: ""),
                        "hasHistory" to room.hasHistory,
                        "updatedAtEpoch" to room.updatedAtEpoch
                    )
                    batch.set(docRef, data, SetOptions.merge())
                }
                batch.commit()
            } catch (_: Exception) {}
        }
    }

    // --- Tariffs Administration ---

    fun addTariff(title: String, durationMinutes: Int, price: Double) {
        val newTariff = Tariff(
            id = "tariff_${UUID.randomUUID()}",
            title = title.trim(),
            durationMinutes = durationMinutes,
            price = price,
            isActive = true
        )
        _tariffs.value = _tariffs.value + newTariff
        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = "Nueva Tarifa Creada",
            description = "Tarifa '$title' ($durationMinutes min) - Q$price",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()
        syncNow()
    }

    fun updateTariff(id: String, title: String, durationMinutes: Int, price: Double, isActive: Boolean) {
        _tariffs.value = _tariffs.value.map { t ->
            if (t.id == id) {
                t.copy(title = title.trim(), durationMinutes = durationMinutes, price = price, isActive = isActive, updatedAtEpoch = System.currentTimeMillis())
            } else t
        }
        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = "Tarifa Modificada",
            description = "Tarifa '$title' actualizada a Q$price (${if (isActive) "Activa" else "Inactiva"})",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()
        syncNow()
    }

    // --- Reception Operations (Stays, Invoices, Extra Time, Room Change, Checkout) ---

    fun getNextInvoiceNumber(): String {
        val nextNum = _hotelConfig.value.nextInvoiceCorrelative
        _hotelConfig.value = _hotelConfig.value.copy(nextInvoiceCorrelative = nextNum + 1)
        saveToLocalStorage()
        return String.format("%06d", nextNum)
    }

    /**
     * Requirement 7: Automatic Amenity Consumption upon Check-in
     * Every time a new check-in/stay is registered:
     * - 1 jabón
     * - 1 shampoo
     * - 1 papel sanitario
     * Descontados automáticamente del inventario.
     * Sin cargo al cliente en la factura (gratuitos).
     * Registrado internamente como CONSUMO DE AMENITIES.
     * Si no hay existencias, no pasa a negativos y registra alerta sin bloquear al huésped.
     */
    private fun applyAutomaticAmenitiesConsumption(roomNumber: String, stayId: String, invoiceNum: String) {
        val amenityCategories = listOf("JABON" to "Jabón", "SHAMPOO" to "Shampoo", "PAPEL" to "Papel Sanitario")

        for ((category, defaultName) in amenityCategories) {
            val item = _inventory.value.find {
                it.isAmenity && (it.category.equals(category, ignoreCase = true) || it.name.contains(defaultName, ignoreCase = true))
            }

            if (item != null) {
                if (item.quantity > 0) {
                    val newQty = (item.quantity - 1.0).coerceAtLeast(0.0)
                    _inventory.value = _inventory.value.map {
                        if (it.id == item.id) it.copy(quantity = newQty, updatedAtEpoch = System.currentTimeMillis()) else it
                    }
                    recordInventoryMovement(
                        itemId = item.id,
                        itemName = item.name,
                        type = "CONSUMO_AMENITY",
                        delta = -1.0,
                        notes = "Consumo automático: 1x ${item.name} para Hab. $roomNumber (Estadía $stayId, Factura $invoiceNum)"
                    )
                    addHistoryEntry(
                        operationType = "CONSUMO_AMENITY",
                        title = "Consumo de Amenities — Hab. $roomNumber",
                        description = "1x ${item.name} entregado automáticamente (gratuito al huésped). Factura $invoiceNum",
                        userOrRole = "RECEPCIÓN",
                        relatedId = invoiceNum
                    )
                } else {
                    // Insufficient stock alert - does NOT block guest check-in
                    recordInventoryMovement(
                        itemId = item.id,
                        itemName = item.name,
                        type = "CONSUMO_AMENITY",
                        delta = 0.0,
                        notes = "ALERTA: Inventario insuficiente de ${item.name}. Faltante para Hab. $roomNumber."
                    )
                    addHistoryEntry(
                        operationType = "INVENTARIO_ALERTA",
                        title = "⚠️ Inventario insuficiente: ${item.name}",
                        description = "Se registró entrada en Hab. $roomNumber pero existencias de ${item.name} estaban en 0. Consumo pendiente/faltante.",
                        userOrRole = "SISTEMA",
                        relatedId = invoiceNum
                    )
                }
            }
        }
    }

    fun startStay(
        roomNumber: String,
        clientType: ClientType,
        clientName: String,
        tariff: Tariff,
        paymentMethod: PaymentMethod,
        amountReceived: Double,
        change: Double,
        transferReference: String,
        extraCharges: List<ExtraChargeItem>,
        productsSold: List<ExtraChargeItem>
    ): Invoice {
        val invoiceNum = getNextInvoiceNumber()
        val now = System.currentTimeMillis()
        val scheduledEnd = now + (tariff.durationMinutes * 60 * 1000L)

        val extrasSum = extraCharges.sumOf { it.total }
        val prodsSum = productsSold.sumOf { it.total }
        val grandTotal = tariff.price + extrasSum + prodsSum

        val stayId = UUID.randomUUID().toString()

        val stay = Stay(
            id = stayId,
            roomNumber = roomNumber,
            clientType = clientType,
            clientName = if (clientType == ClientType.CLIENTE) clientName.trim() else "Consumidor Final",
            invoiceNumber = invoiceNum,
            tariffId = tariff.id,
            tariffTitle = tariff.title,
            basePrice = tariff.price,
            checkInEpoch = now,
            durationMinutes = tariff.durationMinutes,
            scheduledCheckOutEpoch = scheduledEnd,
            extraHours = 0,
            extraChargeTotal = 0.0,
            status = StayStatus.ACTIVE,
            deviceName = currentDeviceName
        )

        // Invoice dynamically takes hotel config (Requirement 5)
        val invoice = Invoice(
            id = UUID.randomUUID().toString(),
            invoiceNumber = invoiceNum,
            dateEpoch = now,
            checkInEpoch = now,
            scheduledCheckOutEpoch = scheduledEnd,
            clientType = clientType,
            clientName = if (clientType == ClientType.CLIENTE) clientName.trim() else "",
            roomNumber = roomNumber,
            tariffTitle = tariff.title,
            durationMinutes = tariff.durationMinutes,
            basePrice = tariff.price,
            extraHours = 0,
            extraChargeTotal = 0.0,
            extraCharges = extraCharges,
            productsSold = productsSold,
            total = grandTotal,
            paymentMethod = paymentMethod,
            amountReceived = amountReceived,
            change = change,
            transferReference = transferReference.trim(),
            status = InvoiceStatus.ACTIVA,
            hotelName = _hotelConfig.value.hotelName,
            hotelPhone = _hotelConfig.value.phone,
            hotelAddress = _hotelConfig.value.address,
            farewellMessage = _hotelConfig.value.departureMessage,
            logoUri = _hotelConfig.value.logoUri,
            registeredByDevice = currentDeviceName
        )

        // Update Room to OCUPADA
        var targetRoom: Room? = null
        _rooms.value = _rooms.value.map { r ->
            if (r.number == roomNumber) {
                val ur = r.copy(status = RoomStatus.OCUPADA, currentStayId = stayId, hasHistory = true, updatedAtEpoch = now)
                targetRoom = ur
                ur
            } else r
        }

        _stays.value = _stays.value + stay
        _invoices.value = _invoices.value + invoice

        // Deduct sold products from inventory
        for (prod in productsSold) {
            if (prod.inventoryItemId != null) {
                deductInventoryItem(prod.inventoryItemId, prod.quantity.toDouble(), "Venta en Factura $invoiceNum")
            }
        }

        // Requirement 7: Automatic deduction of the 3 amenities
        applyAutomaticAmenitiesConsumption(roomNumber, stayId, invoiceNum)

        addHistoryEntry(
            operationType = "CHECK_IN",
            title = "Entrada Registrada — Hab. $roomNumber",
            description = "Factura $invoiceNum | Tarifa: ${tariff.title} (Q${tariff.price}) | Cliente: ${stay.clientName}",
            userOrRole = "RECEPCIÓN",
            relatedId = invoiceNum
        )

        // If there was an active or pending reservation for this room, complete it
        val matchingRes = _reservations.value.find {
            it.roomNumber == roomNumber && (it.status == ReservationStatus.PENDIENTE || it.status == ReservationStatus.CONFIRMADA)
        }
        if (matchingRes != null) {
            _reservations.value = _reservations.value.map {
                if (it.id == matchingRes.id) it.copy(status = ReservationStatus.COMPLETADA, updatedAtEpoch = now) else it
            }
            pushReservationToFirestore(matchingRes.copy(status = ReservationStatus.COMPLETADA, updatedAtEpoch = now))
        }

        saveToLocalStorage()
        if (targetRoom != null) pushRoomToFirestore(targetRoom!!)
        pushStayToFirestore(stay)
        pushInvoiceToFirestore(invoice)
        syncNow()
        return invoice
    }

    private fun pushInvoiceToFirestore(invoice: Invoice) {
        if (!isFirebaseAvailable) return
        coroutineScope.launch {
            try {
                val db = getFirestoreInstance()
                val hotelId = _hotelConfig.value.hotelId
                val data = hashMapOf(
                    "invoiceNumber" to invoice.invoiceNumber,
                    "dateEpoch" to invoice.dateEpoch,
                    "checkInEpoch" to invoice.checkInEpoch,
                    "scheduledCheckOutEpoch" to invoice.scheduledCheckOutEpoch,
                    "clientType" to invoice.clientType.name,
                    "clientName" to invoice.clientName,
                    "roomNumber" to invoice.roomNumber,
                    "tariffTitle" to invoice.tariffTitle,
                    "durationMinutes" to invoice.durationMinutes,
                    "basePrice" to invoice.basePrice,
                    "extraHours" to invoice.extraHours,
                    "extraChargeTotal" to invoice.extraChargeTotal,
                    "total" to invoice.total,
                    "paymentMethod" to invoice.paymentMethod.name,
                    "amountReceived" to invoice.amountReceived,
                    "change" to invoice.change,
                    "transferReference" to invoice.transferReference,
                    "status" to invoice.status.name,
                    "cancellationReason" to invoice.cancellationReason,
                    "modificationNotes" to invoice.modificationNotes,
                    "previousTotal" to (invoice.previousTotal ?: 0.0),
                    "registeredByDevice" to invoice.registeredByDevice,
                    "updatedAtEpoch" to invoice.updatedAtEpoch
                )
                db.collection("hotels").document(hotelId).collection("invoices").document(invoice.id).set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    private fun pushStayToFirestore(stay: Stay) {
        if (!isFirebaseAvailable) return
        coroutineScope.launch {
            try {
                val db = getFirestoreInstance()
                val hotelId = _hotelConfig.value.hotelId
                val data = hashMapOf(
                    "roomNumber" to stay.roomNumber,
                    "clientType" to stay.clientType.name,
                    "clientName" to stay.clientName,
                    "invoiceNumber" to stay.invoiceNumber,
                    "tariffId" to stay.tariffId,
                    "tariffTitle" to stay.tariffTitle,
                    "basePrice" to stay.basePrice,
                    "checkInEpoch" to stay.checkInEpoch,
                    "durationMinutes" to stay.durationMinutes,
                    "scheduledCheckOutEpoch" to stay.scheduledCheckOutEpoch,
                    "extraHours" to stay.extraHours,
                    "extraChargeTotal" to stay.extraChargeTotal,
                    "status" to stay.status.name,
                    "deviceName" to stay.deviceName,
                    "updatedAtEpoch" to stay.updatedAtEpoch
                )
                db.collection("hotels").document(hotelId).collection("stays").document(stay.id).set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    fun addExtraTime(stayId: String, hours: Int = 1) {
        val targetStay = _stays.value.find { it.id == stayId } ?: return
        val pricePerHour = _hotelConfig.value.extraTimePricePerHour
        val additionalCost = pricePerHour * hours
        val additionalMs = hours * 60 * 60 * 1000L

        val newExtraHours = targetStay.extraHours + hours
        val newExtraChargeTotal = targetStay.extraChargeTotal + additionalCost
        val newScheduledEnd = targetStay.scheduledCheckOutEpoch + additionalMs

        var updatedStay: Stay? = null
        _stays.value = _stays.value.map { s ->
            if (s.id == stayId) {
                val us = s.copy(
                    extraHours = newExtraHours,
                    extraChargeTotal = newExtraChargeTotal,
                    scheduledCheckOutEpoch = newScheduledEnd,
                    updatedAtEpoch = System.currentTimeMillis()
                )
                updatedStay = us
                us
            } else s
        }

        addHistoryEntry(
            operationType = "TIEMPO_EXTRA",
            title = "+ Tiempo Extra — Hab. ${targetStay.roomNumber}",
            description = "Se agregaron $hours hora(s) (Q$additionalCost). Tiempo extra acumulado: $newExtraHours hr(s). Factura ref: ${targetStay.invoiceNumber}",
            userOrRole = "RECEPCIÓN",
            relatedId = targetStay.invoiceNumber
        )

        saveToLocalStorage()
        if (updatedStay != null) pushStayToFirestore(updatedStay!!)
        syncNow()
    }

    fun changeRoom(stayId: String, newRoomNumber: String) {
        val stay = _stays.value.find { it.id == stayId } ?: return
        val oldRoomNumber = stay.roomNumber
        if (oldRoomNumber == newRoomNumber) return

        val now = System.currentTimeMillis()

        var oldR: Room? = null
        var newR: Room? = null

        _rooms.value = _rooms.value.map { r ->
            when (r.number) {
                oldRoomNumber -> {
                    val ur = r.copy(status = RoomStatus.DISPONIBLE, currentStayId = null, updatedAtEpoch = now)
                    oldR = ur
                    ur
                }
                newRoomNumber -> {
                    val ur = r.copy(status = RoomStatus.OCUPADA, currentStayId = stayId, hasHistory = true, updatedAtEpoch = now)
                    newR = ur
                    ur
                }
                else -> r
            }
        }

        var updatedStay: Stay? = null
        _stays.value = _stays.value.map { s ->
            if (s.id == stayId) {
                val us = s.copy(roomNumber = newRoomNumber, updatedAtEpoch = now)
                updatedStay = us
                us
            } else s
        }

        var updatedInvoice: Invoice? = null
        _invoices.value = _invoices.value.map { inv ->
            if (inv.invoiceNumber == stay.invoiceNumber) {
                val ui = inv.copy(roomNumber = newRoomNumber, updatedAtEpoch = now)
                updatedInvoice = ui
                ui
            } else inv
        }

        addHistoryEntry(
            operationType = "CAMBIO_HABITACION",
            title = "Cambio de Habitación",
            description = "Estadía ${stay.invoiceNumber} trasladada de Hab. $oldRoomNumber a Hab. $newRoomNumber",
            userOrRole = "RECEPCIÓN",
            relatedId = stay.invoiceNumber
        )

        saveToLocalStorage()
        if (oldR != null) pushRoomToFirestore(oldR!!)
        if (newR != null) pushRoomToFirestore(newR!!)
        if (updatedStay != null) pushStayToFirestore(updatedStay!!)
        if (updatedInvoice != null) pushInvoiceToFirestore(updatedInvoice!!)
        syncNow()
    }

    fun releaseRoom(stayId: String, isEarlyDeparture: Boolean = false) {
        val stay = _stays.value.find { it.id == stayId } ?: return
        val now = System.currentTimeMillis()

        val hasActiveRes = _reservations.value.any { it.roomNumber == stay.roomNumber && (it.status == ReservationStatus.PENDIENTE || it.status == ReservationStatus.CONFIRMADA) }
        val targetStatus = if (hasActiveRes) RoomStatus.RESERVADA else RoomStatus.DISPONIBLE

        var releasedRoom: Room? = null
        _rooms.value = _rooms.value.map { r ->
            if (r.number == stay.roomNumber) {
                val ur = r.copy(status = targetStatus, currentStayId = null, updatedAtEpoch = now)
                releasedRoom = ur
                ur
            } else r
        }

        var completedStay: Stay? = null
        _stays.value = _stays.value.map { s ->
            if (s.id == stayId) {
                val us = s.copy(
                    actualCheckOutEpoch = now,
                    status = if (isEarlyDeparture) StayStatus.EARLY_CHECKOUT else StayStatus.COMPLETED,
                    isEarlyDeparture = isEarlyDeparture,
                    updatedAtEpoch = now
                )
                completedStay = us
                us
            } else s
        }

        var updatedInvoice: Invoice? = null
        _invoices.value = _invoices.value.map { inv ->
            if (inv.invoiceNumber == stay.invoiceNumber) {
                val ui = inv.copy(checkOutEpoch = now, updatedAtEpoch = now)
                updatedInvoice = ui
                ui
            } else inv
        }

        val opType = if (isEarlyDeparture) "SALIDA_ANTICIPADA" else "CHECK_OUT"
        val opTitle = if (isEarlyDeparture) "Salida Anticipada — Hab. ${stay.roomNumber}" else "Habitación Liberada — Hab. ${stay.roomNumber}"
        val opDesc = if (isEarlyDeparture) {
            "El cliente se retiró antes de finalizar el tiempo contratado. Factura ${stay.invoiceNumber} sin reembolso."
        } else {
            "Estadía finalizada normalmente. Habitación ${stay.roomNumber} disponible. Factura ${stay.invoiceNumber}."
        }

        addHistoryEntry(
            operationType = opType,
            title = opTitle,
            description = opDesc,
            userOrRole = "RECEPCIÓN",
            relatedId = stay.invoiceNumber
        )

        saveToLocalStorage()
        if (releasedRoom != null) pushRoomToFirestore(releasedRoom!!)
        if (completedStay != null) pushStayToFirestore(completedStay!!)
        if (updatedInvoice != null) pushInvoiceToFirestore(updatedInvoice!!)
        syncNow()
    }

    // --- Invoices Administration ---

    fun cancelInvoice(invoiceId: String, reason: String) {
        val invoice = _invoices.value.find { it.id == invoiceId } ?: return
        var cancelledInv: Invoice? = null
        _invoices.value = _invoices.value.map { inv ->
            if (inv.id == invoiceId) {
                val ui = inv.copy(status = InvoiceStatus.ANULADA, cancellationReason = reason, updatedAtEpoch = System.currentTimeMillis())
                cancelledInv = ui
                ui
            } else inv
        }

        addHistoryEntry(
            operationType = "FACTURA_ANULADA",
            title = "Factura Anulada N° ${invoice.invoiceNumber}",
            description = "Motivo: $reason | Total anulado: Q${invoice.total}",
            userOrRole = "GERENTE",
            relatedId = invoice.invoiceNumber
        )

        saveToLocalStorage()
        if (cancelledInv != null) pushInvoiceToFirestore(cancelledInv!!)
        syncNow()
    }

    fun modifyInvoice(invoiceId: String, newClientName: String, notes: String) {
        val oldInv = _invoices.value.find { it.id == invoiceId } ?: return
        var modifiedInv: Invoice? = null
        _invoices.value = _invoices.value.map { inv ->
            if (inv.id == invoiceId) {
                val ui = inv.copy(
                    clientName = newClientName.trim(),
                    status = InvoiceStatus.MODIFICADA,
                    modificationNotes = notes,
                    previousTotal = inv.total,
                    updatedAtEpoch = System.currentTimeMillis()
                )
                modifiedInv = ui
                ui
            } else inv
        }

        addHistoryEntry(
            operationType = "FACTURA_MODIFICADA",
            title = "Factura Modificada N° ${oldInv.invoiceNumber}",
            description = "Cliente previo: '${oldInv.clientName}' -> '$newClientName'. Notas: $notes",
            userOrRole = "GERENTE",
            relatedId = oldInv.invoiceNumber
        )

        saveToLocalStorage()
        if (modifiedInv != null) pushInvoiceToFirestore(modifiedInv!!)
        syncNow()
    }

    // --- Reservations (Requirement 6 & Calendar Module) ---

    fun createReservation(
        dateEpoch: Long,
        arrivalTimeEpoch: Long,
        durationMinutes: Int,
        roomNumber: String,
        clientName: String,
        notes: String
    ): Result<Reservation> = createReservation(
        dateEpoch = dateEpoch,
        arrivalTimeEpoch = arrivalTimeEpoch,
        durationMinutes = durationMinutes,
        tariffTitle = "Estadía Reservada (${durationMinutes / 60}h ${durationMinutes % 60}m)",
        roomNumber = roomNumber,
        clientType = if (clientName.isNotBlank() && clientName != "Consumidor Final") ClientType.CLIENTE else ClientType.CONSUMIDOR_FINAL,
        clientName = clientName,
        notes = notes
    )

    fun createReservation(
        dateEpoch: Long,
        arrivalTimeEpoch: Long,
        durationMinutes: Int,
        tariffTitle: String,
        roomNumber: String,
        clientType: ClientType,
        clientName: String,
        notes: String
    ): Result<Reservation> {
        val scheduledEnd = arrivalTimeEpoch + (durationMinutes * 60 * 1000L)
        val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // 1. Strict Overlap Check against other active reservations for the same room
        val conflictingReservation = _reservations.value.find { res ->
            if (res.roomNumber != roomNumber || res.status == ReservationStatus.CANCELADA || res.status == ReservationStatus.COMPLETADA) {
                false
            } else {
                val resEnd = res.arrivalTimeEpoch + (res.durationMinutes * 60 * 1000L)
                arrivalTimeEpoch < resEnd && scheduledEnd > res.arrivalTimeEpoch
            }
        }
        if (conflictingReservation != null) {
            val confStart = timeFormat.format(Date(conflictingReservation.arrivalTimeEpoch))
            val confEnd = timeFormat.format(Date(conflictingReservation.arrivalTimeEpoch + conflictingReservation.durationMinutes * 60_000L))
            return Result.failure(Exception("Horario ocupado: la habitación $roomNumber ya tiene la reservación ${conflictingReservation.reservationNumber} ($confStart a $confEnd)."))
        }

        // 2. Strict Overlap Check against active stays for the same room
        val conflictingStay = _stays.value.find { stay ->
            if (stay.roomNumber != roomNumber || stay.status != StayStatus.ACTIVE) {
                false
            } else {
                arrivalTimeEpoch < stay.scheduledCheckOutEpoch && scheduledEnd > stay.checkInEpoch
            }
        }
        if (conflictingStay != null) {
            val stayStart = timeFormat.format(Date(conflictingStay.checkInEpoch))
            val stayEnd = timeFormat.format(Date(conflictingStay.scheduledCheckOutEpoch))
            return Result.failure(Exception("Horario ocupado: la habitación $roomNumber tiene una estadía activa ($stayStart a $stayEnd)."))
        }

        val resNumber = "RES-${(1000..9999).random()}"
        val reservation = Reservation(
            id = UUID.randomUUID().toString(),
            reservationNumber = resNumber,
            dateEpoch = dateEpoch,
            arrivalTimeEpoch = arrivalTimeEpoch,
            durationMinutes = durationMinutes,
            tariffTitle = tariffTitle,
            roomNumber = roomNumber,
            clientType = clientType,
            clientName = if (clientType == ClientType.CLIENTE) clientName.trim() else "Consumidor Final",
            status = ReservationStatus.PENDIENTE,
            notes = notes.trim(),
            createdAtEpoch = System.currentTimeMillis(),
            updatedAtEpoch = System.currentTimeMillis()
        )

        _reservations.value = _reservations.value + reservation

        // Requirement: La reservación no debe cambiar la habitación a Ocupada. Su estado debe ser Reservada hasta que se registre la llegada real del cliente.
        var targetRoom: Room? = null
        _rooms.value = _rooms.value.map { r ->
            if (r.number == roomNumber && r.status == RoomStatus.DISPONIBLE) {
                val ur = r.copy(status = RoomStatus.RESERVADA, updatedAtEpoch = System.currentTimeMillis())
                targetRoom = ur
                ur
            } else r
        }

        addHistoryEntry(
            operationType = "RESERVA_CREADA",
            title = "Reservación Creada $resNumber",
            description = "Hab. $roomNumber para '${reservation.clientName}' ($tariffTitle)",
            userOrRole = "RECEPCIÓN",
            relatedId = resNumber
        )

        saveToLocalStorage()
        if (targetRoom != null) pushRoomToFirestore(targetRoom!!)
        pushReservationToFirestore(reservation)
        syncNow()
        return Result.success(reservation)
    }

    fun updateReservationStatus(reservationId: String, newStatus: ReservationStatus) {
        val res = _reservations.value.find { it.id == reservationId } ?: return
        var updatedRes: Reservation? = null
        _reservations.value = _reservations.value.map {
            if (it.id == reservationId) {
                val ur = it.copy(status = newStatus, updatedAtEpoch = System.currentTimeMillis())
                updatedRes = ur
                ur
            } else it
        }

        var roomToUpdate: Room? = null
        if (newStatus == ReservationStatus.CANCELADA) {
            val remainingRes = _reservations.value.any { it.id != reservationId && it.roomNumber == res.roomNumber && (it.status == ReservationStatus.PENDIENTE || it.status == ReservationStatus.CONFIRMADA) }
            val activeStay = _stays.value.any { it.roomNumber == res.roomNumber && it.status == StayStatus.ACTIVE }
            if (!remainingRes && !activeStay) {
                _rooms.value = _rooms.value.map { r ->
                    if (r.number == res.roomNumber && r.status == RoomStatus.RESERVADA) {
                        val ur = r.copy(status = RoomStatus.DISPONIBLE, updatedAtEpoch = System.currentTimeMillis())
                        roomToUpdate = ur
                        ur
                    } else r
                }
            }
        } else if (newStatus == ReservationStatus.CONFIRMADA) {
            _rooms.value = _rooms.value.map { r ->
                if (r.number == res.roomNumber && r.status == RoomStatus.DISPONIBLE) {
                    val ur = r.copy(status = RoomStatus.RESERVADA, updatedAtEpoch = System.currentTimeMillis())
                    roomToUpdate = ur
                    ur
                } else r
            }
        }

        addHistoryEntry(
            operationType = if (newStatus == ReservationStatus.CANCELADA) "RESERVA_CANCELADA" else "RESERVA_MODIFICADA",
            title = "Reservación ${res.reservationNumber} -> $newStatus",
            description = "Estado de reservación cambiado a $newStatus",
            userOrRole = "RECEPCIÓN",
            relatedId = res.reservationNumber
        )

        saveToLocalStorage()
        if (roomToUpdate != null) pushRoomToFirestore(roomToUpdate!!)
        if (updatedRes != null) pushReservationToFirestore(updatedRes!!)
        syncNow()
    }

    fun modifyReservationDetails(
        reservationId: String,
        roomNumber: String,
        dateEpoch: Long,
        arrivalTimeEpoch: Long,
        durationMinutes: Int,
        tariffTitle: String,
        clientName: String,
        notes: String
    ): Result<Unit> {
        val res = _reservations.value.find { it.id == reservationId } ?: return Result.failure(Exception("Reservación no encontrada."))
        val scheduledEnd = arrivalTimeEpoch + (durationMinutes * 60 * 1000L)
        val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val conflictingReservation = _reservations.value.find { r ->
            if (r.id == reservationId || r.roomNumber != roomNumber || r.status == ReservationStatus.CANCELADA || r.status == ReservationStatus.COMPLETADA) {
                false
            } else {
                val rEnd = r.arrivalTimeEpoch + (r.durationMinutes * 60 * 1000L)
                arrivalTimeEpoch < rEnd && scheduledEnd > r.arrivalTimeEpoch
            }
        }
        if (conflictingReservation != null) {
            val confStart = timeFormat.format(Date(conflictingReservation.arrivalTimeEpoch))
            val confEnd = timeFormat.format(Date(conflictingReservation.arrivalTimeEpoch + conflictingReservation.durationMinutes * 60_000L))
            return Result.failure(Exception("Conflicto de horario: la habitación $roomNumber ya tiene la reservación ${conflictingReservation.reservationNumber} ($confStart a $confEnd)."))
        }

        val conflictingStay = _stays.value.find { stay ->
            if (stay.roomNumber != roomNumber || stay.status != StayStatus.ACTIVE) {
                false
            } else {
                arrivalTimeEpoch < stay.scheduledCheckOutEpoch && scheduledEnd > stay.checkInEpoch
            }
        }
        if (conflictingStay != null) {
            val stayStart = timeFormat.format(Date(conflictingStay.checkInEpoch))
            val stayEnd = timeFormat.format(Date(conflictingStay.scheduledCheckOutEpoch))
            return Result.failure(Exception("Conflicto de horario: la habitación $roomNumber tiene una estadía activa ($stayStart a $stayEnd)."))
        }

        val oldRoomNumber = res.roomNumber
        var updatedRes: Reservation? = null
        _reservations.value = _reservations.value.map {
            if (it.id == reservationId) {
                val ur = it.copy(
                    roomNumber = roomNumber,
                    dateEpoch = dateEpoch,
                    arrivalTimeEpoch = arrivalTimeEpoch,
                    durationMinutes = durationMinutes,
                    tariffTitle = tariffTitle,
                    clientName = if (clientName.isNotBlank() && clientName != "Consumidor Final") clientName.trim() else "Consumidor Final",
                    clientType = if (clientName.isNotBlank() && clientName != "Consumidor Final") ClientType.CLIENTE else ClientType.CONSUMIDOR_FINAL,
                    notes = notes.trim(),
                    updatedAtEpoch = System.currentTimeMillis()
                )
                updatedRes = ur
                ur
            } else it
        }

        var oldR: Room? = null
        var newR: Room? = null
        if (oldRoomNumber != roomNumber) {
            _rooms.value = _rooms.value.map { r ->
                if (r.number == oldRoomNumber && r.status == RoomStatus.RESERVADA) {
                    val otherRes = _reservations.value.any { it.id != reservationId && it.roomNumber == oldRoomNumber && (it.status == ReservationStatus.PENDIENTE || it.status == ReservationStatus.CONFIRMADA) }
                    val activeStay = _stays.value.any { it.roomNumber == oldRoomNumber && it.status == StayStatus.ACTIVE }
                    if (!otherRes && !activeStay) {
                        val ur = r.copy(status = RoomStatus.DISPONIBLE, updatedAtEpoch = System.currentTimeMillis())
                        oldR = ur
                        ur
                    } else r
                } else if (r.number == roomNumber && r.status == RoomStatus.DISPONIBLE) {
                    val ur = r.copy(status = RoomStatus.RESERVADA, updatedAtEpoch = System.currentTimeMillis())
                    newR = ur
                    ur
                } else r
            }
        }

        addHistoryEntry(
            operationType = "RESERVA_MODIFICADA",
            title = "Reservación Modificada ${res.reservationNumber}",
            description = "Horario/datos modificados para Hab. $roomNumber ($tariffTitle)",
            userOrRole = "RECEPCIÓN",
            relatedId = res.reservationNumber
        )

        saveToLocalStorage()
        if (oldR != null) pushRoomToFirestore(oldR!!)
        if (newR != null) pushRoomToFirestore(newR!!)
        if (updatedRes != null) pushReservationToFirestore(updatedRes!!)
        syncNow()
        return Result.success(Unit)
    }

    private fun pushReservationToFirestore(res: Reservation) {
        if (!isFirebaseAvailable) return
        coroutineScope.launch {
            try {
                val db = getFirestoreInstance()
                val hotelId = _hotelConfig.value.hotelId
                val data = hashMapOf(
                    "reservationNumber" to res.reservationNumber,
                    "dateEpoch" to res.dateEpoch,
                    "arrivalTimeEpoch" to res.arrivalTimeEpoch,
                    "durationMinutes" to res.durationMinutes,
                    "tariffTitle" to res.tariffTitle,
                    "roomNumber" to res.roomNumber,
                    "clientType" to res.clientType.name,
                    "clientName" to res.clientName,
                    "status" to res.status.name,
                    "notes" to res.notes,
                    "createdAtEpoch" to res.createdAtEpoch,
                    "updatedAtEpoch" to res.updatedAtEpoch
                )
                db.collection("hotels").document(hotelId).collection("reservations").document(res.id).set(data, SetOptions.merge())
            } catch (_: Exception) {}
        }
    }

    // --- Inventory, Purchases & Movements ---

    fun addInventoryItem(name: String, category: String, quantity: Double, unit: String, purchasePrice: Double, salePrice: Double, isAmenity: Boolean) {
        val item = InventoryItem(
            id = "inv_${UUID.randomUUID()}",
            name = name.trim(),
            category = category,
            quantity = quantity,
            unit = unit.trim(),
            purchasePrice = purchasePrice,
            salePrice = if (isAmenity) 0.0 else salePrice,
            isAmenity = isAmenity,
            isActive = true
        )
        _inventory.value = _inventory.value + item

        if (quantity > 0) {
            recordInventoryMovement(item.id, item.name, "ENTRADA", quantity, "Inventario inicial")
        }

        addHistoryEntry(
            operationType = "INVENTARIO_MOVIMIENTO",
            title = "Producto de Inventario Creado",
            description = "${item.name} (${item.quantity} ${item.unit})",
            userOrRole = "GERENTE"
        )

        saveToLocalStorage()
        syncNow()
    }

    /**
     * Requirement 7: Inventory item full editing
     * Allows editing name, available quantity, unit, purchase price, sale price, active status, isAmenity.
     */
    fun updateInventoryItem(
        itemId: String,
        name: String,
        quantity: Double,
        unit: String,
        purchasePrice: Double,
        salePrice: Double,
        isActive: Boolean,
        isAmenity: Boolean
    ) {
        _inventory.value = _inventory.value.map { item ->
            if (item.id == itemId) {
                item.copy(
                    name = name.trim(),
                    quantity = quantity,
                    unit = unit.trim(),
                    purchasePrice = purchasePrice,
                    salePrice = if (isAmenity) 0.0 else salePrice,
                    isActive = isActive,
                    isAmenity = isAmenity,
                    updatedAtEpoch = System.currentTimeMillis()
                )
            } else item
        }
        addHistoryEntry(
            operationType = "INVENTARIO_MODIFICADO",
            title = "Producto de Inventario Modificado",
            description = "$name ($quantity $unit) - Compra: Q$purchasePrice, Venta: Q$salePrice (${if (isActive) "Activo" else "Inactivo"})",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()
        syncNow()
    }


    fun consumeAmenity(itemId: String, qty: Double, roomNumber: String) {
        val item = _inventory.value.find { it.id == itemId } ?: return
        val newQty = (item.quantity - qty).coerceAtLeast(0.0)
        _inventory.value = _inventory.value.map {
            if (it.id == itemId) it.copy(quantity = newQty, updatedAtEpoch = System.currentTimeMillis()) else it
        }
        recordInventoryMovement(
            itemId = item.id,
            itemName = item.name,
            type = "CONSUMO_AMENITY",
            delta = -qty,
            notes = "Consumo manual: $qty ${item.unit} de ${item.name} entregado a Hab. $roomNumber"
        )
        addHistoryEntry(
            operationType = "CONSUMO_AMENITY",
            title = "Consumo de Amenity — Hab. $roomNumber",
            description = "$qty ${item.unit} de ${item.name} para Hab. $roomNumber",
            userOrRole = "RECEPCIÓN"
        )
        saveToLocalStorage()
        syncNow()
    }

    fun recordPurchase(
        itemId: String,
        quantity: Double,
        unitPrice: Double,
        supplier: String,
        invoiceNumber: String,
        hasInvoice: Boolean,
        photoUri: String?,
        notes: String
    ) {
        val item = _inventory.value.find { it.id == itemId } ?: return
        val total = quantity * unitPrice
        val now = System.currentTimeMillis()

        val purchase = InventoryPurchase(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            itemName = item.name,
            quantity = quantity,
            unit = item.unit,
            unitPrice = unitPrice,
            total = total,
            supplier = supplier.trim(),
            dateEpoch = now,
            invoiceNumber = invoiceNumber.trim(),
            hasInvoice = hasInvoice,
            photoUri = photoUri,
            notes = notes.trim(),
            deviceName = currentDeviceName,
            updatedAtEpoch = now
        )

        _inventory.value = _inventory.value.map {
            if (it.id == itemId) it.copy(quantity = it.quantity + quantity, updatedAtEpoch = now) else it
        }

        _purchases.value = _purchases.value + purchase

        recordInventoryMovement(itemId, item.name, "COMPRA", quantity, "Compra a proveedor $supplier (Total Q$total)")

        addHistoryEntry(
            operationType = "COMPRA_INVENTARIO",
            title = "Compra de Inventario — ${item.name}",
            description = "+$quantity ${item.unit} por Q$total | Proveedor: $supplier",
            userOrRole = "GERENTE"
        )

        saveToLocalStorage()
        syncNow()
    }

    private fun deductInventoryItem(itemId: String, quantity: Double, notes: String) {
        val item = _inventory.value.find { it.id == itemId } ?: return
        _inventory.value = _inventory.value.map {
            if (it.id == itemId) it.copy(quantity = (it.quantity - quantity).coerceAtLeast(0.0), updatedAtEpoch = System.currentTimeMillis()) else it
        }
        recordInventoryMovement(itemId, item.name, "VENTA", -quantity, notes)
    }

    private fun recordInventoryMovement(itemId: String, itemName: String, type: String, delta: Double, notes: String) {
        val mov = InventoryMovement(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            itemName = itemName,
            movementType = type,
            quantityDelta = delta,
            dateEpoch = System.currentTimeMillis(),
            deviceName = currentDeviceName,
            notes = notes
        )
        _inventoryMovements.value = _inventoryMovements.value + mov
    }

    // --- Expenses and Services ---

    fun recordExpenseService(
        type: String,
        specification: String,
        quantity: Int,
        unitPrice: Double,
        providerName: String,
        dateEpoch: Long,
        frequency: String,
        notes: String,
        photoUri: String?
    ) {
        val total = quantity * unitPrice
        val expense = ExpenseService(
            id = UUID.randomUUID().toString(),
            type = type,
            specification = specification.trim(),
            quantity = quantity,
            unitPrice = unitPrice,
            total = total,
            providerName = providerName.trim(),
            dateEpoch = dateEpoch,
            frequency = frequency,
            notes = notes.trim(),
            photoUri = photoUri,
            deviceName = currentDeviceName,
            updatedAtEpoch = System.currentTimeMillis()
        )

        _expenses.value = _expenses.value + expense

        addHistoryEntry(
            operationType = "SERVICIO_GASTO",
            title = "Servicio/Gasto: $type — Q$total",
            description = "$specification | Proveedor: $providerName ($frequency)",
            userOrRole = "GERENTE"
        )

        saveToLocalStorage()
        syncNow()
    }

    // --- Extra Fee Concepts ---

    fun addExtraFeeConcept(name: String, defaultPrice: Double) {
        val concept = ExtraFeeConcept(
            id = "fee_${UUID.randomUUID()}",
            name = name.trim(),
            defaultPrice = defaultPrice,
            isActive = true
        )
        _extraFeeConcepts.value = _extraFeeConcepts.value + concept
        saveToLocalStorage()
        syncNow()
    }

    fun updateExtraFeeConcept(id: String, name: String, defaultPrice: Double, isActive: Boolean) {
        _extraFeeConcepts.value = _extraFeeConcepts.value.map {
            if (it.id == id) it.copy(name = name.trim(), defaultPrice = defaultPrice, isActive = isActive) else it
        }
        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = "Cobro Adicional Modificado",
            description = "$name -> Q$defaultPrice (${if (isActive) "Activo" else "Inactivo"})",
            userOrRole = "GERENTE"
        )
        saveToLocalStorage()
        syncNow()
    }

    fun toggleExtraFeeConcept(id: String, isActive: Boolean) {
        _extraFeeConcepts.value = _extraFeeConcepts.value.map {
            if (it.id == id) it.copy(isActive = isActive) else it
        }
        saveToLocalStorage()
        syncNow()
    }

    // --- Hotel Config Administration (Requirement 5 & 9) ---

    fun updateHotelConfig(
        name: String,
        phone: String,
        address: String,
        logoUri: String?,
        departureMessage: String,
        gerentePin: String,
        extraTimePricePerHour: Double,
        stayEndingAlertMinutesBefore: Int,
        notificationsMap: Map<String, Boolean>,
        managerInCharge: String = _hotelConfig.value.managerInCharge,
        nextInvoiceCorrelative: Long? = null
    ) {
        _hotelConfig.value = _hotelConfig.value.copy(
            hotelName = name.trim().ifBlank { "HOTEL RIVERA" },
            phone = phone.trim(),
            address = address.trim(),
            logoUri = logoUri ?: _hotelConfig.value.logoUri,
            departureMessage = departureMessage.trim().ifBlank { "Gracias por su visita a Hotel Rivera. Esperamos verle pronto." },
            gerentePin = gerentePin.trim().ifBlank { "1234" },
            extraTimePricePerHour = extraTimePricePerHour,
            stayEndingAlertMinutesBefore = stayEndingAlertMinutesBefore,
            managerInCharge = managerInCharge.trim().ifBlank { "Administrador en Turno" },
            nextInvoiceCorrelative = nextInvoiceCorrelative ?: _hotelConfig.value.nextInvoiceCorrelative,
            notifyStayEnding = notificationsMap["notifyStayEnding"] ?: true,
            notifyStayEnded = notificationsMap["notifyStayEnded"] ?: true,
            notifyAllowExtraTime = notificationsMap["notifyAllowExtraTime"] ?: true,
            notifyReservation3h = notificationsMap["notifyReservation3h"] ?: true,
            notifyReservationModified = notificationsMap["notifyReservationModified"] ?: true,
            notifyReservationCancelled = notificationsMap["notifyReservationCancelled"] ?: true,
            notifyReservationNoShow1h = notificationsMap["notifyReservationNoShow1h"] ?: true,
            notifyInternetDisconnected = notificationsMap["notifyInternetDisconnected"] ?: true,
            notifyInternetRestored = notificationsMap["notifyInternetRestored"] ?: true,
            notifySyncPending = notificationsMap["notifySyncPending"] ?: true
        )


        addHistoryEntry(
            operationType = "CAMBIO_CONFIG",
            title = "Configuración del Hotel Actualizada",
            description = "Datos generales, logo y parámetros modificados por Gerente",
            userOrRole = "GERENTE"
        )

        saveToLocalStorage()

        // Sync config directly to Firestore
        if (isFirebaseAvailable) {
            coroutineScope.launch {
                try {
                    val db = getFirestoreInstance()
                    val hotelId = _hotelConfig.value.hotelId
                    val configMap = hashMapOf(
                        "hotelName" to _hotelConfig.value.hotelName,
                        "phone" to _hotelConfig.value.phone,
                        "address" to _hotelConfig.value.address,
                        "logoUri" to (_hotelConfig.value.logoUri ?: ""),
                        "departureMessage" to _hotelConfig.value.departureMessage,
                        "gerentePin" to _hotelConfig.value.gerentePin,
                        "extraTimePricePerHour" to _hotelConfig.value.extraTimePricePerHour,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("hotels").document(hotelId).collection("config").document("main").set(configMap, SetOptions.merge())
                } catch (_: Exception) {}
            }
        }
        syncNow()
    }

    // --- History Helper ---

    fun addHistoryEntry(operationType: String, title: String, description: String, userOrRole: String, relatedId: String = "") {
        val entry = HistoryEntry(
            id = UUID.randomUUID().toString(),
            dateEpoch = System.currentTimeMillis(),
            operationType = operationType,
            title = title,
            description = description,
            userOrRole = userOrRole,
            deviceName = currentDeviceName,
            relatedId = relatedId
        )
        _history.value = listOf(entry) + _history.value
    }

    // --- Background Stay & Reservation Monitor Loop ---

    private fun startStayMonitorLoop() {
        coroutineScope.launch {
            while (isActive) {
                try {
                    val now = System.currentTimeMillis()
                    val config = _hotelConfig.value
                    val alertMs = config.stayEndingAlertMinutesBefore * 60 * 1000L

                    for (stay in _stays.value.filter { it.status == StayStatus.ACTIVE }) {
                        val diff = stay.scheduledCheckOutEpoch - now
                        if (diff in 1..alertMs) {
                            if (config.notifyStayEnding) {
                                withContext(Dispatchers.Main) {
                                    NotificationHelper.showStayEndingAlert(context, stay.roomNumber, diff / (60 * 1000L))
                                }
                            }
                        } else if (diff <= 0) {
                            if (config.notifyStayEnded) {
                                withContext(Dispatchers.Main) {
                                    NotificationHelper.showStayEndedAlert(context, stay.roomNumber, stay.id)
                                }
                            }
                        }
                    }

                    for (res in _reservations.value.filter { it.status == ReservationStatus.CONFIRMADA || it.status == ReservationStatus.PENDIENTE }) {
                        val arrivalDiff = res.arrivalTimeEpoch - now
                        if (arrivalDiff in (175 * 60 * 1000L)..(185 * 60 * 1000L) && config.notifyReservation3h) {
                            withContext(Dispatchers.Main) {
                                NotificationHelper.showReservationReminder(context, res.roomNumber, res.clientName)
                            }
                        }
                        if (-arrivalDiff >= (60 * 60 * 1000L) && config.notifyReservationNoShow1h) {
                            withContext(Dispatchers.Main) {
                                NotificationHelper.showReservationNoShowAlert(context, res.roomNumber, res.clientName)
                            }
                        }
                    }
                } catch (_: Exception) {}

                delay(30_000L)
            }
        }
    }
}
