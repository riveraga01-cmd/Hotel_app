package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.model.ClientType
import com.example.model.Invoice
import com.example.model.PaymentMethod
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfInvoiceGenerator {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun generatePdfFile(context: Context, invoice: Invoice): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Background Banner (Navy Blue)
        paint.color = Color.rgb(15, 30, 46)
        canvas.drawRect(0f, 0f, 595f, 115f, paint)

        // Gold Accent Line
        paint.color = Color.rgb(217, 119, 6)
        paint.strokeWidth = 3f
        canvas.drawLine(0f, 115f, 595f, 115f, paint)

        // Load Logo (from URI if configured, or default drawable)
        var logoBitmap: Bitmap? = null
        if (!invoice.logoUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(invoice.logoUri)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    logoBitmap = BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {}
        }
        if (logoBitmap == null) {
            try {
                logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.ic_hotel_logo)
            } catch (_: Exception) {}
        }

        var textStartX = 40f
        if (logoBitmap != null) {
            try {
                val scaledLogo = Bitmap.createScaledBitmap(logoBitmap!!, 65, 65, true)
                canvas.drawBitmap(scaledLogo, 40f, 25f, paint)
                textStartX = 118f
            } catch (_: Exception) {}
        }

        // Hotel Name (Dynamic from Config)
        paint.color = Color.WHITE
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.hotelName.ifBlank { "HOTEL RIVERA" }, textStartX, 50f, paint)

        // Hotel Subtitle / Contact (Dynamic from Config)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(220, 230, 240)
        val phoneText = if (invoice.hotelPhone.isNotBlank()) "Tel: ${invoice.hotelPhone} | " else ""
        canvas.drawText("$phoneText${invoice.hotelAddress.ifBlank { "Guatemala" }}", textStartX, 70f, paint)
        canvas.drawText("Comprobante Oficial de Hospedaje y Servicios", textStartX, 88f, paint)

        // Invoice Number Box on top right
        paint.color = Color.rgb(217, 119, 6)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("FACTURA N°", 420f, 48f, paint)
        paint.color = Color.WHITE
        paint.textSize = 20f
        canvas.drawText(invoice.invoiceNumber, 420f, 75f, paint)

        var y = 145f

        // Invoice Meta Info Section
        paint.color = Color.rgb(30, 45, 60)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("Fecha de Emisión:", 40f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(dateFormat.format(Date(invoice.dateEpoch)), 160f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Habitación:", 350f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Hab. ${invoice.roomNumber}", 450f, y, paint)

        y += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Tipo de Cliente:", 40f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val clientTypeLabel = if (invoice.clientType == ClientType.CONSUMIDOR_FINAL) "CONSUMIDOR FINAL" else "CLIENTE"
        canvas.drawText(clientTypeLabel, 160f, y, paint)

        if (invoice.clientType == ClientType.CLIENTE && invoice.clientName.isNotBlank()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Nombre Cliente:", 350f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(invoice.clientName, 450f, y, paint)
        }

        y += 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Hora de Entrada:", 40f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(dateFormat.format(Date(invoice.checkInEpoch)), 160f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Hora Prevista Salida:", 350f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(dateFormat.format(Date(invoice.scheduledCheckOutEpoch)), 470f, y, paint)

        if (invoice.checkOutEpoch != null) {
            y += 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Hora Real Salida:", 40f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(dateFormat.format(Date(invoice.checkOutEpoch)), 160f, y, paint)
        }

        // Table Header
        y += 35f
        paint.color = Color.rgb(240, 243, 246)
        canvas.drawRect(40f, y, 555f, y + 26f, paint)

        paint.color = Color.rgb(15, 30, 46)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText("DESCRIPCIÓN / SERVICIO", 50f, y + 17f, paint)
        canvas.drawText("CANT.", 340f, y + 17f, paint)
        canvas.drawText("PRECIO UNIT.", 400f, y + 17f, paint)
        canvas.drawText("TOTAL", 500f, y + 17f, paint)

        paint.strokeWidth = 1f
        paint.color = Color.rgb(200, 210, 220)
        canvas.drawLine(40f, y + 26f, 555f, y + 26f, paint)

        y += 26f

        // Primary Stay Row
        y += 20f
        paint.color = Color.rgb(30, 40, 50)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 10f
        val stayTitle = "Hospedaje: ${invoice.tariffTitle} (${invoice.durationMinutes} min)"
        canvas.drawText(stayTitle, 50f, y, paint)
        canvas.drawText("1", 350f, y, paint)
        canvas.drawText(String.format(Locale.US, "Q%.2f", invoice.basePrice), 410f, y, paint)
        canvas.drawText(String.format(Locale.US, "Q%.2f", invoice.basePrice), 500f, y, paint)

        // Extra Time Row if any
        if (invoice.extraHours > 0) {
            y += 20f
            val unitExtra = if (invoice.extraHours > 0) invoice.extraChargeTotal / invoice.extraHours else 25.0
            canvas.drawText("+ Tiempo Extra Adicional (${invoice.extraHours} hr)", 50f, y, paint)
            canvas.drawText("${invoice.extraHours}", 350f, y, paint)
            canvas.drawText(String.format(Locale.US, "Q%.2f", unitExtra), 410f, y, paint)
            canvas.drawText(String.format(Locale.US, "Q%.2f", invoice.extraChargeTotal), 500f, y, paint)
        }

        // Extra Charges / Items list if any
        for (item in invoice.extraCharges) {
            y += 20f
            val spec = if (item.specification.isNotBlank()) " - ${item.specification}" else ""
            canvas.drawText("${item.concept}$spec", 50f, y, paint)
            canvas.drawText("${item.quantity}", 350f, y, paint)
            canvas.drawText(String.format(Locale.US, "Q%.2f", item.unitPrice), 410f, y, paint)
            canvas.drawText(String.format(Locale.US, "Q%.2f", item.total), 500f, y, paint)
        }

        // Products Sold list if any
        for (item in invoice.productsSold) {
            y += 20f
            canvas.drawText("Producto: ${item.concept} ${item.specification}", 50f, y, paint)
            canvas.drawText("${item.quantity}", 350f, y, paint)
            canvas.drawText(String.format(Locale.US, "Q%.2f", item.unitPrice), 410f, y, paint)
            canvas.drawText(String.format(Locale.US, "Q%.2f", item.total), 500f, y, paint)
        }

        // Separator line
        y += 18f
        paint.color = Color.rgb(200, 210, 220)
        canvas.drawLine(40f, y, 555f, y, paint)

        // Totals & Payment Info
        y += 28f
        paint.color = Color.rgb(15, 30, 46)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("TOTAL A PAGAR:", 340f, y, paint)
        paint.color = Color.rgb(180, 120, 0)
        canvas.drawText(String.format(Locale.US, "Q%.2f", invoice.total), 480f, y, paint)

        y += 26f
        paint.color = Color.rgb(30, 45, 60)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FORMA DE PAGO:", 40f, y, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val methodLabel = if (invoice.paymentMethod == PaymentMethod.EFECTIVO) "EFECTIVO" else "TRANSFERENCIA"
        canvas.drawText(methodLabel, 150f, y, paint)

        if (invoice.paymentMethod == PaymentMethod.EFECTIVO) {
            y += 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Monto Recibido:", 40f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(String.format(Locale.US, "Q%.2f", invoice.amountReceived), 150f, y, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Cambio Devuelto:", 340f, y, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(String.format(Locale.US, "Q%.2f", invoice.change), 460f, y, paint)
        } else {
            if (invoice.transferReference.isNotBlank()) {
                y += 18f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Ref. Transferencia:", 40f, y, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(invoice.transferReference, 170f, y, paint)
            }
        }

        // Status annotation if canceled or modified
        if (invoice.status != com.example.model.InvoiceStatus.ACTIVA) {
            y += 24f
            paint.color = if (invoice.status == com.example.model.InvoiceStatus.ANULADA) Color.RED else Color.rgb(200, 100, 0)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("ESTADO: ${invoice.status.name} ${if (invoice.cancellationReason.isNotBlank()) "(${invoice.cancellationReason})" else ""}", 40f, y, paint)
        }

        // Farewell Message at bottom (Dynamic from Config)
        paint.color = Color.rgb(80, 95, 110)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        val msg = invoice.farewellMessage.ifBlank { "Gracias por su visita a Hotel Rivera. Esperamos verle pronto." }
        canvas.drawText(msg, 40f, 770f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Registrado por: ${invoice.registeredByDevice.ifBlank { "Terminal Hotel Rivera" }}", 40f, 790f, paint)

        pdfDocument.finishPage(page)

        val dir = File(context.cacheDir, "facturas").apply { mkdirs() }
        val file = File(dir, "Factura_${invoice.invoiceNumber}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.close()
        pdfDocument.close()

        return file
    }

    fun sharePdf(context: Context, invoice: Invoice) {
        val file = generatePdfFile(context, invoice)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Factura ${invoice.invoiceNumber} - ${invoice.hotelName}")
            putExtra(Intent.EXTRA_TEXT, "Adjunto comprobante de hospedaje N° ${invoice.invoiceNumber} de ${invoice.hotelName}.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Compartir Factura")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun savePdfLocally(context: Context, invoice: Invoice): String {
        val sourceFile = generatePdfFile(context, invoice)
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir

        val destFile = File(downloadsDir, "Factura_${invoice.invoiceNumber}.pdf")
        sourceFile.copyTo(destFile, overwrite = true)
        Toast.makeText(context, "Factura guardada en Descargas: ${destFile.name}", Toast.LENGTH_LONG).show()
        return destFile.absolutePath
    }
}
