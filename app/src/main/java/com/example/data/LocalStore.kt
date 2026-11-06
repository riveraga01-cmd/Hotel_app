package com.example.data

import android.content.Context
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Handles rock-solid local storage persistence in app private storage.
 * Ensures the app works 100% offline, across app restarts, reboots, and network loss.
 */
class LocalStore(private val context: Context) {

    private val dataFile = File(context.filesDir, "hotel_rivera_local_data.json")
    private val prefs = context.getSharedPreferences("hotel_rivera_prefs", Context.MODE_PRIVATE)

    fun getDeviceId(): String {
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = "DEV-" + java.util.UUID.randomUUID().toString().take(8).uppercase()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }

    fun getDeviceName(): String {
        return prefs.getString("device_name", "Teléfono Hotel Rivera") ?: "Teléfono Hotel Rivera"
    }

    fun setDeviceName(name: String) {
        prefs.edit().putString("device_name", name).apply()
    }

    fun isDeviceAuthorized(): Boolean {
        // If not set yet, defaults to true on the first setup
        return prefs.getBoolean("device_authorized", true)
    }

    fun setDeviceAuthorized(auth: Boolean) {
        prefs.edit().putBoolean("device_authorized", auth).apply()
    }

    fun getHotelId(): String {
        return prefs.getString("hotel_id", "hotel_rivera_main") ?: "hotel_rivera_main"
    }

    fun setHotelId(id: String) {
        prefs.edit().putString("hotel_id", id).apply()
    }

    fun clearLocalHotelData() {
        if (dataFile.exists()) {
            dataFile.delete()
        }
        setDeviceAuthorized(false)
    }

    fun saveSnapshot(
        config: HotelConfig,
        rooms: List<Room>,
        tariffs: List<Tariff>,
        stays: List<Stay>,
        invoices: List<Invoice>,
        reservations: List<Reservation>,
        inventory: List<InventoryItem>,
        movements: List<InventoryMovement>,
        purchases: List<InventoryPurchase>,
        expenses: List<ExpenseService>,
        extraFees: List<ExtraFeeConcept>,
        devices: List<DeviceAuthorization>,
        history: List<HistoryEntry>,
        linkCodes: List<TempLinkCode>
    ) {
        try {
            val root = JSONObject()

            // Config
            val configObj = JSONObject().apply {
                put("hotelId", config.hotelId)
                put("hotelName", config.hotelName)
                put("phone", config.phone)
                put("address", config.address)
                put("logoUri", config.logoUri ?: "")
                put("departureMessage", config.departureMessage)
                put("gerentePin", config.gerentePin)
                put("extraTimePricePerHour", config.extraTimePricePerHour)
                put("stayEndingAlertMinutesBefore", config.stayEndingAlertMinutesBefore)
                put("nextInvoiceCorrelative", config.nextInvoiceCorrelative)
                put("notifyStayEnding", config.notifyStayEnding)
                put("notifyStayEnded", config.notifyStayEnded)
                put("notifyAllowExtraTime", config.notifyAllowExtraTime)
                put("notifyReservation3h", config.notifyReservation3h)
                put("notifyReservationModified", config.notifyReservationModified)
                put("notifyReservationCancelled", config.notifyReservationCancelled)
                put("notifyReservationNoShow1h", config.notifyReservationNoShow1h)
                put("notifyInternetDisconnected", config.notifyInternetDisconnected)
                put("notifyInternetRestored", config.notifyInternetRestored)
                put("notifySyncPending", config.notifySyncPending)
            }
            root.put("config", configObj)

            // Rooms
            val roomsArray = JSONArray()
            rooms.forEach { r ->
                roomsArray.put(JSONObject().apply {
                    put("id", r.id)
                    put("number", r.number)
                    put("name", r.name)
                    put("type", r.type.name)
                    put("bedCount", r.bedCount)
                    put("features", r.features)
                    put("status", r.status.name)
                    put("currentStayId", r.currentStayId ?: "")
                    put("hasHistory", r.hasHistory)
                    put("updatedAtEpoch", r.updatedAtEpoch)
                })
            }
            root.put("rooms", roomsArray)

            // Tariffs
            val tariffsArray = JSONArray()
            tariffs.forEach { t ->
                tariffsArray.put(JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("durationMinutes", t.durationMinutes)
                    put("price", t.price)
                    put("isActive", t.isActive)
                    put("updatedAtEpoch", t.updatedAtEpoch)
                })
            }
            root.put("tariffs", tariffsArray)

            // Stays
            val staysArray = JSONArray()
            stays.forEach { s ->
                staysArray.put(JSONObject().apply {
                    put("id", s.id)
                    put("roomNumber", s.roomNumber)
                    put("clientType", s.clientType.name)
                    put("clientName", s.clientName)
                    put("invoiceNumber", s.invoiceNumber)
                    put("tariffId", s.tariffId)
                    put("tariffTitle", s.tariffTitle)
                    put("basePrice", s.basePrice)
                    put("checkInEpoch", s.checkInEpoch)
                    put("durationMinutes", s.durationMinutes)
                    put("scheduledCheckOutEpoch", s.scheduledCheckOutEpoch)
                    put("extraHours", s.extraHours)
                    put("extraChargeTotal", s.extraChargeTotal)
                    put("actualCheckOutEpoch", s.actualCheckOutEpoch ?: -1L)
                    put("status", s.status.name)
                    put("isEarlyDeparture", s.isEarlyDeparture)
                    put("deviceName", s.deviceName)
                    put("updatedAtEpoch", s.updatedAtEpoch)
                })
            }
            root.put("stays", staysArray)

            // Invoices
            val invoicesArray = JSONArray()
            invoices.forEach { inv ->
                val invObj = JSONObject().apply {
                    put("id", inv.id)
                    put("invoiceNumber", inv.invoiceNumber)
                    put("dateEpoch", inv.dateEpoch)
                    put("checkInEpoch", inv.checkInEpoch)
                    put("checkOutEpoch", inv.checkOutEpoch ?: -1L)
                    put("scheduledCheckOutEpoch", inv.scheduledCheckOutEpoch)
                    put("clientType", inv.clientType.name)
                    put("clientName", inv.clientName)
                    put("roomNumber", inv.roomNumber)
                    put("tariffTitle", inv.tariffTitle)
                    put("durationMinutes", inv.durationMinutes)
                    put("basePrice", inv.basePrice)
                    put("extraHours", inv.extraHours)
                    put("extraChargeTotal", inv.extraChargeTotal)
                    put("total", inv.total)
                    put("paymentMethod", inv.paymentMethod.name)
                    put("amountReceived", inv.amountReceived)
                    put("change", inv.change)
                    put("transferReference", inv.transferReference)
                    put("status", inv.status.name)
                    put("cancellationReason", inv.cancellationReason)
                    put("modificationNotes", inv.modificationNotes)
                    put("previousTotal", inv.previousTotal ?: -1.0)
                    put("hotelName", inv.hotelName)
                    put("hotelPhone", inv.hotelPhone)
                    put("hotelAddress", inv.hotelAddress)
                    put("farewellMessage", inv.farewellMessage)
                    put("registeredByDevice", inv.registeredByDevice)
                    put("updatedAtEpoch", inv.updatedAtEpoch)

                    // extraCharges
                    val extrasArr = JSONArray()
                    inv.extraCharges.forEach { ec ->
                        extrasArr.put(JSONObject().apply {
                            put("id", ec.id)
                            put("concept", ec.concept)
                            put("quantity", ec.quantity)
                            put("specification", ec.specification)
                            put("unitPrice", ec.unitPrice)
                            put("total", ec.total)
                        })
                    }
                    put("extraCharges", extrasArr)

                    // productsSold
                    val prodsArr = JSONArray()
                    inv.productsSold.forEach { p ->
                        prodsArr.put(JSONObject().apply {
                            put("id", p.id)
                            put("concept", p.concept)
                            put("quantity", p.quantity)
                            put("specification", p.specification)
                            put("unitPrice", p.unitPrice)
                            put("total", p.total)
                            put("inventoryItemId", p.inventoryItemId ?: "")
                        })
                    }
                    put("productsSold", prodsArr)
                }
                invoicesArray.put(invObj)
            }
            root.put("invoices", invoicesArray)

            // Reservations
            val resArray = JSONArray()
            reservations.forEach { res ->
                resArray.put(JSONObject().apply {
                    put("id", res.id)
                    put("reservationNumber", res.reservationNumber)
                    put("dateEpoch", res.dateEpoch)
                    put("arrivalTimeEpoch", res.arrivalTimeEpoch)
                    put("durationMinutes", res.durationMinutes)
                    put("roomNumber", res.roomNumber)
                    put("clientName", res.clientName)
                    put("status", res.status.name)
                    put("notes", res.notes)
                    put("createdAtEpoch", res.createdAtEpoch)
                    put("updatedAtEpoch", res.updatedAtEpoch)
                })
            }
            root.put("reservations", resArray)

            // Inventory
            val invItemsArray = JSONArray()
            inventory.forEach { item ->
                invItemsArray.put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("category", item.category)
                    put("quantity", item.quantity)
                    put("unit", item.unit)
                    put("purchasePrice", item.purchasePrice)
                    put("salePrice", item.salePrice)
                    put("isAmenity", item.isAmenity)
                    put("isActive", item.isActive)
                    put("updatedAtEpoch", item.updatedAtEpoch)
                })
            }
            root.put("inventory", invItemsArray)

            // Movements
            val movsArray = JSONArray()
            movements.forEach { m ->
                movsArray.put(JSONObject().apply {
                    put("id", m.id)
                    put("itemId", m.itemId)
                    put("itemName", m.itemName)
                    put("movementType", m.movementType)
                    put("quantityDelta", m.quantityDelta)
                    put("dateEpoch", m.dateEpoch)
                    put("deviceName", m.deviceName)
                    put("notes", m.notes)
                })
            }
            root.put("movements", movsArray)

            // Purchases
            val purchasesArray = JSONArray()
            purchases.forEach { p ->
                purchasesArray.put(JSONObject().apply {
                    put("id", p.id)
                    put("itemId", p.itemId)
                    put("itemName", p.itemName)
                    put("quantity", p.quantity)
                    put("unit", p.unit)
                    put("unitPrice", p.unitPrice)
                    put("total", p.total)
                    put("supplier", p.supplier)
                    put("dateEpoch", p.dateEpoch)
                    put("invoiceNumber", p.invoiceNumber)
                    put("hasInvoice", p.hasInvoice)
                    put("photoUri", p.photoUri ?: "")
                    put("notes", p.notes)
                    put("deviceName", p.deviceName)
                    put("updatedAtEpoch", p.updatedAtEpoch)
                })
            }
            root.put("purchases", purchasesArray)

            // Expenses
            val expensesArray = JSONArray()
            expenses.forEach { e ->
                expensesArray.put(JSONObject().apply {
                    put("id", e.id)
                    put("type", e.type)
                    put("specification", e.specification)
                    put("quantity", e.quantity)
                    put("unitPrice", e.unitPrice)
                    put("total", e.total)
                    put("providerName", e.providerName)
                    put("dateEpoch", e.dateEpoch)
                    put("frequency", e.frequency)
                    put("notes", e.notes)
                    put("photoUri", e.photoUri ?: "")
                    put("deviceName", e.deviceName)
                    put("updatedAtEpoch", e.updatedAtEpoch)
                })
            }
            root.put("expenses", expensesArray)

            // Extra fee concepts
            val extraFeesArray = JSONArray()
            extraFees.forEach { ef ->
                extraFeesArray.put(JSONObject().apply {
                    put("id", ef.id)
                    put("name", ef.name)
                    put("defaultPrice", ef.defaultPrice)
                    put("isActive", ef.isActive)
                })
            }
            root.put("extraFees", extraFeesArray)

            // Devices
            val devicesArray = JSONArray()
            devices.forEach { d ->
                devicesArray.put(JSONObject().apply {
                    put("id", d.id)
                    put("deviceId", d.deviceId)
                    put("deviceName", d.deviceName)
                    put("linkedAtEpoch", d.linkedAtEpoch)
                    put("lastSyncEpoch", d.lastSyncEpoch)
                    put("isOnline", d.isOnline)
                    put("isAuthorized", d.isAuthorized)
                    put("status", d.status)
                })
            }
            root.put("devices", devicesArray)

            // History
            val historyArray = JSONArray()
            history.forEach { h ->
                historyArray.put(JSONObject().apply {
                    put("id", h.id)
                    put("dateEpoch", h.dateEpoch)
                    put("operationType", h.operationType)
                    put("title", h.title)
                    put("description", h.description)
                    put("userOrRole", h.userOrRole)
                    put("deviceName", h.deviceName)
                    put("relatedId", h.relatedId)
                })
            }
            root.put("history", historyArray)

            // Link Codes
            val codesArray = JSONArray()
            linkCodes.forEach { lc ->
                codesArray.put(JSONObject().apply {
                    put("code", lc.code)
                    put("hotelId", lc.hotelId)
                    put("expiresAtEpoch", lc.expiresAtEpoch)
                    put("isUsed", lc.isUsed)
                    put("createdAtEpoch", lc.createdAtEpoch)
                })
            }
            root.put("linkCodes", codesArray)

            dataFile.writeText(root.toString())
        } catch (_: Exception) {}
    }

    fun loadSnapshot(): StoredData? {
        if (!dataFile.exists()) return null
        return try {
            val text = dataFile.readText()
            val root = JSONObject(text)

            // Config
            val cObj = root.optJSONObject("config")
            val config = if (cObj != null) {
                HotelConfig(
                    hotelId = cObj.optString("hotelId", "hotel_rivera_main"),
                    hotelName = cObj.optString("hotelName", "HOTEL RIVERA"),
                    phone = cObj.optString("phone", "+502 7765-4321"),
                    address = cObj.optString("address", "Calzada Principal 5-30, Zona 2"),
                    logoUri = cObj.optString("logoUri").ifBlank { null },
                    departureMessage = cObj.optString("departureMessage", "¡Gracias por su visita a Hotel Rivera! Esperamos verle pronto."),
                    gerentePin = cObj.optString("gerentePin", "1234"),
                    extraTimePricePerHour = cObj.optDouble("extraTimePricePerHour", 25.0),
                    stayEndingAlertMinutesBefore = cObj.optInt("stayEndingAlertMinutesBefore", 15),
                    nextInvoiceCorrelative = cObj.optLong("nextInvoiceCorrelative", 1L),
                    notifyStayEnding = cObj.optBoolean("notifyStayEnding", true),
                    notifyStayEnded = cObj.optBoolean("notifyStayEnded", true),
                    notifyAllowExtraTime = cObj.optBoolean("notifyAllowExtraTime", true),
                    notifyReservation3h = cObj.optBoolean("notifyReservation3h", true),
                    notifyReservationModified = cObj.optBoolean("notifyReservationModified", true),
                    notifyReservationCancelled = cObj.optBoolean("notifyReservationCancelled", true),
                    notifyReservationNoShow1h = cObj.optBoolean("notifyReservationNoShow1h", true),
                    notifyInternetDisconnected = cObj.optBoolean("notifyInternetDisconnected", true),
                    notifyInternetRestored = cObj.optBoolean("notifyInternetRestored", true),
                    notifySyncPending = cObj.optBoolean("notifySyncPending", true)
                )
            } else HotelConfig()

            // Rooms
            val rooms = mutableListOf<Room>()
            val rArray = root.optJSONArray("rooms") ?: JSONArray()
            for (i in 0 until rArray.length()) {
                val o = rArray.getJSONObject(i)
                rooms.add(Room(
                    id = o.optString("id"),
                    number = o.optString("number"),
                    name = o.optString("name"),
                    type = runCatching { RoomType.valueOf(o.optString("type")) }.getOrDefault(RoomType.INDIVIDUAL),
                    bedCount = o.optInt("bedCount", 1),
                    features = o.optString("features"),
                    status = runCatching { RoomStatus.valueOf(o.optString("status")) }.getOrDefault(RoomStatus.DISPONIBLE),
                    currentStayId = o.optString("currentStayId").ifBlank { null },
                    hasHistory = o.optBoolean("hasHistory", false),
                    updatedAtEpoch = o.optLong("updatedAtEpoch", System.currentTimeMillis())
                ))
            }

            // Tariffs
            val tariffs = mutableListOf<Tariff>()
            val tArray = root.optJSONArray("tariffs") ?: JSONArray()
            for (i in 0 until tArray.length()) {
                val o = tArray.getJSONObject(i)
                tariffs.add(Tariff(
                    id = o.optString("id"),
                    title = o.optString("title"),
                    durationMinutes = o.optInt("durationMinutes", 60),
                    price = o.optDouble("price", 45.0),
                    isActive = o.optBoolean("isActive", true),
                    updatedAtEpoch = o.optLong("updatedAtEpoch", System.currentTimeMillis())
                ))
            }

            // Stays
            val stays = mutableListOf<Stay>()
            val sArray = root.optJSONArray("stays") ?: JSONArray()
            for (i in 0 until sArray.length()) {
                val o = sArray.getJSONObject(i)
                stays.add(Stay(
                    id = o.optString("id"),
                    roomNumber = o.optString("roomNumber"),
                    clientType = runCatching { ClientType.valueOf(o.optString("clientType")) }.getOrDefault(ClientType.CONSUMIDOR_FINAL),
                    clientName = o.optString("clientName"),
                    invoiceNumber = o.optString("invoiceNumber"),
                    tariffId = o.optString("tariffId"),
                    tariffTitle = o.optString("tariffTitle"),
                    basePrice = o.optDouble("basePrice", 0.0),
                    checkInEpoch = o.optLong("checkInEpoch"),
                    durationMinutes = o.optInt("durationMinutes"),
                    scheduledCheckOutEpoch = o.optLong("scheduledCheckOutEpoch"),
                    extraHours = o.optInt("extraHours", 0),
                    extraChargeTotal = o.optDouble("extraChargeTotal", 0.0),
                    actualCheckOutEpoch = o.optLong("actualCheckOutEpoch").takeIf { it > 0 },
                    status = runCatching { StayStatus.valueOf(o.optString("status")) }.getOrDefault(StayStatus.ACTIVE),
                    isEarlyDeparture = o.optBoolean("isEarlyDeparture", false),
                    deviceName = o.optString("deviceName"),
                    updatedAtEpoch = o.optLong("updatedAtEpoch", System.currentTimeMillis())
                ))
            }

            // Invoices
            val invoices = mutableListOf<Invoice>()
            val invArray = root.optJSONArray("invoices") ?: JSONArray()
            for (i in 0 until invArray.length()) {
                val o = invArray.getJSONObject(i)
                val extraChargesList = mutableListOf<ExtraChargeItem>()
                val ecArr = o.optJSONArray("extraCharges") ?: JSONArray()
                for (j in 0 until ecArr.length()) {
                    val eco = ecArr.getJSONObject(j)
                    extraChargesList.add(ExtraChargeItem(
                        id = eco.optString("id"),
                        concept = eco.optString("concept"),
                        quantity = eco.optInt("quantity", 1),
                        specification = eco.optString("specification"),
                        unitPrice = eco.optDouble("unitPrice"),
                        total = eco.optDouble("total")
                    ))
                }

                val prodsList = mutableListOf<ExtraChargeItem>()
                val pArr = o.optJSONArray("productsSold") ?: JSONArray()
                for (j in 0 until pArr.length()) {
                    val po = pArr.getJSONObject(j)
                    prodsList.add(ExtraChargeItem(
                        id = po.optString("id"),
                        concept = po.optString("concept"),
                        quantity = po.optInt("quantity", 1),
                        specification = po.optString("specification"),
                        unitPrice = po.optDouble("unitPrice"),
                        total = po.optDouble("total"),
                        inventoryItemId = po.optString("inventoryItemId").ifBlank { null }
                    ))
                }

                invoices.add(Invoice(
                    id = o.optString("id"),
                    invoiceNumber = o.optString("invoiceNumber"),
                    dateEpoch = o.optLong("dateEpoch"),
                    checkInEpoch = o.optLong("checkInEpoch"),
                    checkOutEpoch = o.optLong("checkOutEpoch").takeIf { it > 0 },
                    scheduledCheckOutEpoch = o.optLong("scheduledCheckOutEpoch"),
                    clientType = runCatching { ClientType.valueOf(o.optString("clientType")) }.getOrDefault(ClientType.CONSUMIDOR_FINAL),
                    clientName = o.optString("clientName"),
                    roomNumber = o.optString("roomNumber"),
                    tariffTitle = o.optString("tariffTitle"),
                    durationMinutes = o.optInt("durationMinutes"),
                    basePrice = o.optDouble("basePrice"),
                    extraHours = o.optInt("extraHours"),
                    extraChargeTotal = o.optDouble("extraChargeTotal"),
                    extraCharges = extraChargesList,
                    productsSold = prodsList,
                    total = o.optDouble("total"),
                    paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod")) }.getOrDefault(PaymentMethod.EFECTIVO),
                    amountReceived = o.optDouble("amountReceived"),
                    change = o.optDouble("change"),
                    transferReference = o.optString("transferReference"),
                    status = runCatching { InvoiceStatus.valueOf(o.optString("status")) }.getOrDefault(InvoiceStatus.ACTIVA),
                    cancellationReason = o.optString("cancellationReason"),
                    modificationNotes = o.optString("modificationNotes"),
                    previousTotal = o.optDouble("previousTotal").takeIf { it >= 0 },
                    hotelName = o.optString("hotelName", "HOTEL RIVERA"),
                    hotelPhone = o.optString("hotelPhone"),
                    hotelAddress = o.optString("hotelAddress"),
                    farewellMessage = o.optString("farewellMessage"),
                    registeredByDevice = o.optString("registeredByDevice"),
                    updatedAtEpoch = o.optLong("updatedAtEpoch")
                ))
            }

            // Reservations
            val reservations = mutableListOf<Reservation>()
            val resArr = root.optJSONArray("reservations") ?: JSONArray()
            for (i in 0 until resArr.length()) {
                val o = resArr.getJSONObject(i)
                reservations.add(Reservation(
                    id = o.optString("id"),
                    reservationNumber = o.optString("reservationNumber"),
                    dateEpoch = o.optLong("dateEpoch"),
                    arrivalTimeEpoch = o.optLong("arrivalTimeEpoch"),
                    durationMinutes = o.optInt("durationMinutes"),
                    roomNumber = o.optString("roomNumber"),
                    clientName = o.optString("clientName"),
                    status = runCatching { ReservationStatus.valueOf(o.optString("status")) }.getOrDefault(ReservationStatus.PENDIENTE),
                    notes = o.optString("notes"),
                    createdAtEpoch = o.optLong("createdAtEpoch"),
                    updatedAtEpoch = o.optLong("updatedAtEpoch")
                ))
            }

            // Inventory
            val inventory = mutableListOf<InventoryItem>()
            val invItemsArr = root.optJSONArray("inventory") ?: JSONArray()
            for (i in 0 until invItemsArr.length()) {
                val o = invItemsArr.getJSONObject(i)
                inventory.add(InventoryItem(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    category = o.optString("category"),
                    quantity = o.optDouble("quantity"),
                    unit = o.optString("unit", "unidad"),
                    purchasePrice = o.optDouble("purchasePrice"),
                    salePrice = o.optDouble("salePrice"),
                    isAmenity = o.optBoolean("isAmenity"),
                    isActive = o.optBoolean("isActive", true),
                    updatedAtEpoch = o.optLong("updatedAtEpoch")
                ))
            }

            // Movements
            val movements = mutableListOf<InventoryMovement>()
            val mArr = root.optJSONArray("movements") ?: JSONArray()
            for (i in 0 until mArr.length()) {
                val o = mArr.getJSONObject(i)
                movements.add(InventoryMovement(
                    id = o.optString("id"),
                    itemId = o.optString("itemId"),
                    itemName = o.optString("itemName"),
                    movementType = o.optString("movementType"),
                    quantityDelta = o.optDouble("quantityDelta"),
                    dateEpoch = o.optLong("dateEpoch"),
                    deviceName = o.optString("deviceName"),
                    notes = o.optString("notes")
                ))
            }

            // Purchases
            val purchases = mutableListOf<InventoryPurchase>()
            val pArr = root.optJSONArray("purchases") ?: JSONArray()
            for (i in 0 until pArr.length()) {
                val o = pArr.getJSONObject(i)
                purchases.add(InventoryPurchase(
                    id = o.optString("id"),
                    itemId = o.optString("itemId"),
                    itemName = o.optString("itemName"),
                    quantity = o.optDouble("quantity"),
                    unit = o.optString("unit"),
                    unitPrice = o.optDouble("unitPrice"),
                    total = o.optDouble("total"),
                    supplier = o.optString("supplier"),
                    dateEpoch = o.optLong("dateEpoch"),
                    invoiceNumber = o.optString("invoiceNumber"),
                    hasInvoice = o.optBoolean("hasInvoice", true),
                    photoUri = o.optString("photoUri").ifBlank { null },
                    notes = o.optString("notes"),
                    deviceName = o.optString("deviceName"),
                    updatedAtEpoch = o.optLong("updatedAtEpoch")
                ))
            }

            // Expenses
            val expenses = mutableListOf<ExpenseService>()
            val expArr = root.optJSONArray("expenses") ?: JSONArray()
            for (i in 0 until expArr.length()) {
                val o = expArr.getJSONObject(i)
                expenses.add(ExpenseService(
                    id = o.optString("id"),
                    type = o.optString("type"),
                    specification = o.optString("specification"),
                    quantity = o.optInt("quantity", 1),
                    unitPrice = o.optDouble("unitPrice"),
                    total = o.optDouble("total"),
                    providerName = o.optString("providerName"),
                    dateEpoch = o.optLong("dateEpoch"),
                    frequency = o.optString("frequency", "UNICA"),
                    notes = o.optString("notes"),
                    photoUri = o.optString("photoUri").ifBlank { null },
                    deviceName = o.optString("deviceName"),
                    updatedAtEpoch = o.optLong("updatedAtEpoch")
                ))
            }

            // Extra fee concepts
            val extraFees = mutableListOf<ExtraFeeConcept>()
            val efArr = root.optJSONArray("extraFees") ?: JSONArray()
            for (i in 0 until efArr.length()) {
                val o = efArr.getJSONObject(i)
                extraFees.add(ExtraFeeConcept(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    defaultPrice = o.optDouble("defaultPrice"),
                    isActive = o.optBoolean("isActive", true)
                ))
            }

            // Devices
            val devices = mutableListOf<DeviceAuthorization>()
            val devArr = root.optJSONArray("devices") ?: JSONArray()
            for (i in 0 until devArr.length()) {
                val o = devArr.getJSONObject(i)
                devices.add(DeviceAuthorization(
                    id = o.optString("id"),
                    deviceId = o.optString("deviceId"),
                    deviceName = o.optString("deviceName"),
                    linkedAtEpoch = o.optLong("linkedAtEpoch"),
                    lastSyncEpoch = o.optLong("lastSyncEpoch"),
                    isOnline = o.optBoolean("isOnline", true),
                    isAuthorized = o.optBoolean("isAuthorized", true),
                    status = o.optString("status", "ACTIVO")
                ))
            }

            // History
            val history = mutableListOf<HistoryEntry>()
            val hArr = root.optJSONArray("history") ?: JSONArray()
            for (i in 0 until hArr.length()) {
                val o = hArr.getJSONObject(i)
                history.add(HistoryEntry(
                    id = o.optString("id"),
                    dateEpoch = o.optLong("dateEpoch"),
                    operationType = o.optString("operationType"),
                    title = o.optString("title"),
                    description = o.optString("description"),
                    userOrRole = o.optString("userOrRole", "RECEPCIÓN"),
                    deviceName = o.optString("deviceName"),
                    relatedId = o.optString("relatedId")
                ))
            }

            // Link codes
            val linkCodes = mutableListOf<TempLinkCode>()
            val lcArr = root.optJSONArray("linkCodes") ?: JSONArray()
            for (i in 0 until lcArr.length()) {
                val o = lcArr.getJSONObject(i)
                linkCodes.add(TempLinkCode(
                    code = o.optString("code"),
                    hotelId = o.optString("hotelId"),
                    expiresAtEpoch = o.optLong("expiresAtEpoch"),
                    isUsed = o.optBoolean("isUsed"),
                    createdAtEpoch = o.optLong("createdAtEpoch")
                ))
            }

            StoredData(
                config = config,
                rooms = rooms,
                tariffs = tariffs,
                stays = stays,
                invoices = invoices,
                reservations = reservations,
                inventory = inventory,
                movements = movements,
                purchases = purchases,
                expenses = expenses,
                extraFees = extraFees,
                devices = devices,
                history = history,
                linkCodes = linkCodes
            )
        } catch (_: Exception) {
            null
        }
    }

    data class StoredData(
        val config: HotelConfig,
        val rooms: List<Room>,
        val tariffs: List<Tariff>,
        val stays: List<Stay>,
        val invoices: List<Invoice>,
        val reservations: List<Reservation>,
        val inventory: List<InventoryItem>,
        val movements: List<InventoryMovement>,
        val purchases: List<InventoryPurchase>,
        val expenses: List<ExpenseService>,
        val extraFees: List<ExtraFeeConcept>,
        val devices: List<DeviceAuthorization>,
        val history: List<HistoryEntry>,
        val linkCodes: List<TempLinkCode>
    )
}
