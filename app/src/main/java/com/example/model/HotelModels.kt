package com.example.model

enum class RoomStatus {
    DISPONIBLE,
    OCUPADA,
    RESERVADA,
    DESHABILITADA
}

enum class RoomType {
    INDIVIDUAL,
    DOBLE,
    FAMILIAR,
    SUITE
}

data class Room(
    val id: String = "",
    val number: String = "",
    val name: String = "",
    val type: RoomType = RoomType.INDIVIDUAL,
    val bedCount: Int = 1,
    val features: String = "",
    val status: RoomStatus = RoomStatus.DISPONIBLE,
    val currentStayId: String? = null,
    val hasHistory: Boolean = false,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class Tariff(
    val id: String = "",
    val title: String = "",
    val durationMinutes: Int = 60,
    val price: Double = 45.0,
    val isActive: Boolean = true,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

enum class ClientType {
    CONSUMIDOR_FINAL,
    CLIENTE
}

enum class PaymentMethod {
    EFECTIVO,
    TRANSFERENCIA
}

enum class StayStatus {
    ACTIVE,
    COMPLETED,
    EARLY_CHECKOUT,
    ROOM_CHANGED,
    CANCELLED
}

data class ExtraChargeItem(
    val id: String = "",
    val concept: String = "",
    val quantity: Int = 1,
    val specification: String = "",
    val unitPrice: Double = 0.0,
    val total: Double = 0.0,
    val inventoryItemId: String? = null
)

data class Stay(
    val id: String = "",
    val roomNumber: String = "",
    val clientType: ClientType = ClientType.CONSUMIDOR_FINAL,
    val clientName: String = "",
    val invoiceNumber: String = "",
    val tariffId: String = "",
    val tariffTitle: String = "",
    val basePrice: Double = 0.0,
    val checkInEpoch: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 60,
    val scheduledCheckOutEpoch: Long = System.currentTimeMillis() + (60 * 60 * 1000L),
    val extraHours: Int = 0,
    val extraChargeTotal: Double = 0.0,
    val actualCheckOutEpoch: Long? = null,
    val status: StayStatus = StayStatus.ACTIVE,
    val isEarlyDeparture: Boolean = false,
    val deviceName: String = "",
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

enum class InvoiceStatus {
    ACTIVA,
    ANULADA,
    MODIFICADA
}

data class Invoice(
    val id: String = "",
    val invoiceNumber: String = "", // Formatted as "000001", "000002"
    val dateEpoch: Long = System.currentTimeMillis(),
    val checkInEpoch: Long = System.currentTimeMillis(),
    val checkOutEpoch: Long? = null,
    val scheduledCheckOutEpoch: Long = System.currentTimeMillis(),
    val clientType: ClientType = ClientType.CONSUMIDOR_FINAL,
    val clientName: String = "",
    val roomNumber: String = "",
    val tariffTitle: String = "",
    val durationMinutes: Int = 60,
    val basePrice: Double = 0.0,
    val extraHours: Int = 0,
    val extraChargeTotal: Double = 0.0,
    val extraCharges: List<ExtraChargeItem> = emptyList(),
    val productsSold: List<ExtraChargeItem> = emptyList(),
    val total: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val amountReceived: Double = 0.0,
    val change: Double = 0.0,
    val transferReference: String = "",
    val status: InvoiceStatus = InvoiceStatus.ACTIVA,
    val cancellationReason: String = "",
    val modificationNotes: String = "",
    val previousTotal: Double? = null,
    val hotelName: String = "HOTEL RIVERA",
    val hotelPhone: String = "",
    val hotelAddress: String = "",
    val farewellMessage: String = "Gracias por su visita a Hotel Rivera. Esperamos verle pronto.",
    val logoUri: String? = null,
    val registeredByDevice: String = "",
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

enum class ReservationStatus {
    PENDIENTE,
    CONFIRMADA,
    CANCELADA,
    COMPLETADA,
    NO_SE_PRESENTO
}

data class Reservation(
    val id: String = "",
    val reservationNumber: String = "",
    val dateEpoch: Long = System.currentTimeMillis(),
    val arrivalTimeEpoch: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 180,
    val tariffTitle: String = "3 horas",
    val roomNumber: String = "",
    val clientType: ClientType = ClientType.CONSUMIDOR_FINAL,
    val clientName: String = "",
    val status: ReservationStatus = ReservationStatus.PENDIENTE,
    val notes: String = "",
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val category: String = "BEBIDAS", // BEBIDAS, PRODUCTOS_VENTA, PAPEL, JABON, SHAMPOO, TOALLAS, COBIJAS, MATERIALES, SUMINISTROS
    val quantity: Double = 0.0,
    val unit: String = "unidad",
    val purchasePrice: Double = 0.0,
    val salePrice: Double = 0.0,
    val isAmenity: Boolean = false,
    val isActive: Boolean = true,
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class InventoryMovement(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val movementType: String = "COMPRA", // COMPRA, VENTA, CONSUMO_AMENITY, ENTRADA, SALIDA, AJUSTE
    val quantityDelta: Double = 0.0,
    val dateEpoch: Long = System.currentTimeMillis(),
    val deviceName: String = "",
    val notes: String = ""
)

data class InventoryPurchase(
    val id: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val quantity: Double = 1.0,
    val unit: String = "unidad",
    val unitPrice: Double = 0.0,
    val total: Double = 0.0,
    val supplier: String = "",
    val dateEpoch: Long = System.currentTimeMillis(),
    val invoiceNumber: String = "",
    val hasInvoice: Boolean = true,
    val photoUri: String? = null,
    val notes: String = "",
    val deviceName: String = "",
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class ExpenseService(
    val id: String = "",
    val type: String = "REPARACION", // PLOMERO, ELECTRICISTA, REPARACION, MANTENIMIENTO, OTRO
    val specification: String = "",
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val total: Double = 0.0,
    val providerName: String = "",
    val dateEpoch: Long = System.currentTimeMillis(),
    val frequency: String = "UNICA", // UNICA, MENSUAL
    val notes: String = "",
    val photoUri: String? = null,
    val deviceName: String = "",
    val updatedAtEpoch: Long = System.currentTimeMillis()
)

data class ExtraFeeConcept(
    val id: String = "",
    val name: String = "",
    val defaultPrice: Double = 25.0,
    val isActive: Boolean = true
)

data class DeviceAuthorization(
    val id: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val linkedAtEpoch: Long = System.currentTimeMillis(),
    val lastSyncEpoch: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val isAuthorized: Boolean = true,
    val status: String = "ACTIVO" // ACTIVO, REVOCADO
)

data class TempLinkCode(
    val code: String = "",
    val hotelId: String = "",
    val expiresAtEpoch: Long = System.currentTimeMillis() + (15 * 60 * 1000L),
    val isUsed: Boolean = false,
    val createdAtEpoch: Long = System.currentTimeMillis()
)

data class HistoryEntry(
    val id: String = "",
    val dateEpoch: Long = System.currentTimeMillis(),
    val operationType: String = "", // CHECK_IN, CHECK_OUT, SALIDA_ANTICIPADA, TIEMPO_EXTRA, CAMBIO_HABITACION, NUEVA_FACTURA, FACTURA_ANULADA, FACTURA_MODIFICADA, RESERVA_CREADA, RESERVA_MODIFICADA, RESERVA_CANCELADA, COMPRA_INVENTARIO, SERVICIO_GASTO, COBRO_EXTRA, CONSUMO_AMENITY, INVENTARIO_MOVIMIENTO, INVENTARIO_ALERTA, VINCULAR_DISPOSITIVO, REVOCAR_DISPOSITIVO, CAMBIO_CONFIG
    val title: String = "",
    val description: String = "",
    val userOrRole: String = "RECEPCIÓN", // GERENTE, RECEPCIÓN
    val deviceName: String = "",
    val relatedId: String = ""
)

data class HotelConfig(
    val hotelId: String = "hotel_rivera_main",
    val hotelName: String = "HOTEL RIVERA",
    val phone: String = "+502 7765-4321",
    val address: String = "Calzada Principal 5-30, Zona 2",
    val logoUri: String? = null,
    val departureMessage: String = "Gracias por su visita a Hotel Rivera. Esperamos verle pronto.",
    val gerentePin: String = "1234",
    val extraTimePricePerHour: Double = 25.0,
    val stayEndingAlertMinutesBefore: Int = 15,
    val managerInCharge: String = "Administrador en Turno",
    val notifyStayEnding: Boolean = true,
    val notifyStayEnded: Boolean = true,
    val notifyAllowExtraTime: Boolean = true,
    val notifyReservation3h: Boolean = true,
    val notifyReservationModified: Boolean = true,
    val notifyReservationCancelled: Boolean = true,
    val notifyReservationNoShow1h: Boolean = true,
    val notifyInternetDisconnected: Boolean = true,
    val notifyInternetRestored: Boolean = true,
    val notifySyncPending: Boolean = true,
    val nextInvoiceCorrelative: Long = 1L
)


enum class ConnectionStatus(val label: String) {
    CONECTADO("Conectado"),
    SIN_CONEXION("Sin conexión"),
    SINCRONIZANDO("Sincronizando"),
    SINCRONIZACION_PENDIENTE("Sincronización pendiente")
}
