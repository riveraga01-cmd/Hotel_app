package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ClientType
import com.example.model.Invoice
import com.example.model.PaymentMethod
import com.example.ui.theme.*
import com.example.util.PdfInvoiceGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoicePdfViewerDialog(
    invoice: Invoice,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HotelNavy)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = HotelGoldLight,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FACTURA N° ${invoice.invoiceNumber}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White
                        )
                    }
                }

                // Printable Invoice Preview Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Document Sheet Representation
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderLight, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFDFD)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Banner
                            Text(
                                text = invoice.hotelName.ifBlank { "HOTEL RIVERA" },
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HotelNavy,
                                letterSpacing = 2.sp
                            )
                            if (invoice.hotelPhone.isNotBlank() || invoice.hotelAddress.isNotBlank()) {
                                Text(
                                    text = "${invoice.hotelAddress} • Tel: ${invoice.hotelPhone}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = "Comprobante de Hospedaje y Servicios",
                                fontSize = 11.sp,
                                color = HotelGoldDark,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = HotelGold, thickness = 2.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // Metadata Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fecha de Emisión: ${dateFormat.format(Date(invoice.dateEpoch))}", fontSize = 12.sp)
                                Text("Habitación: ${invoice.roomNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HotelNavy)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val clientTypeStr = if (invoice.clientType == ClientType.CONSUMIDOR_FINAL) "CONSUMIDOR FINAL" else "CLIENTE"
                                Text("Tipo de Cliente: $clientTypeStr", fontSize = 12.sp)
                                if (invoice.clientName.isNotBlank()) {
                                    Text("Nombre: ${invoice.clientName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Entrada: ${dateFormat.format(Date(invoice.checkInEpoch))}", fontSize = 11.sp, color = TextMuted)
                                Text("Prevista: ${dateFormat.format(Date(invoice.scheduledCheckOutEpoch))}", fontSize = 11.sp, color = TextMuted)
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF0F4F8), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Concepto", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Cant.", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Precio", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text("Total", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            // Primary stay row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Hospedaje: ${invoice.tariffTitle}", fontSize = 12.sp, modifier = Modifier.weight(2f))
                                Text("1", fontSize = 12.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                Text(String.format(Locale.US, "Q%.2f", invoice.basePrice), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text(String.format(Locale.US, "Q%.2f", invoice.basePrice), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            // Extra time row if any
                            if (invoice.extraHours > 0) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("+ Tiempo Extra (${invoice.extraHours}h)", fontSize = 12.sp, modifier = Modifier.weight(2f))
                                    Text("${invoice.extraHours}", fontSize = 12.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                    Text(String.format(Locale.US, "Q%.2f", invoice.extraChargeTotal / invoice.extraHours), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text(String.format(Locale.US, "Q%.2f", invoice.extraChargeTotal), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }

                            // Products sold
                            invoice.productsSold.forEach { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(p.concept, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                    Text("${p.quantity}", fontSize = 12.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                    Text(String.format(Locale.US, "Q%.2f", p.unitPrice), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text(String.format(Locale.US, "Q%.2f", p.total), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }

                            // Extra charges
                            invoice.extraCharges.forEach { ec ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${ec.concept} ${ec.specification}", fontSize = 12.sp, modifier = Modifier.weight(2f))
                                    Text("${ec.quantity}", fontSize = 12.sp, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                    Text(String.format(Locale.US, "Q%.2f", ec.unitPrice), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text(String.format(Locale.US, "Q%.2f", ec.total), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(10.dp))

                            // Total
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TOTAL A PAGAR:", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = HotelNavy)
                                Text(
                                    String.format(Locale.US, "Q%.2f", invoice.total),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = HotelGoldDark
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Payment details
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Forma de Pago: ${invoice.paymentMethod.name}", fontSize = 12.sp)
                                if (invoice.paymentMethod == PaymentMethod.EFECTIVO) {
                                    Text(
                                        "Recibido: Q${String.format(Locale.US, "%.2f", invoice.amountReceived)} | Cambio: Q${String.format(Locale.US, "%.2f", invoice.change)}",
                                        fontSize = 12.sp
                                    )
                                } else if (invoice.transferReference.isNotBlank()) {
                                    Text("Ref: ${invoice.transferReference}", fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Farewell
                            Text(
                                text = invoice.farewellMessage,
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom Action Buttons: PDF, COMPARTIR, GUARDAR (Rule 28)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // PDF Button
                        OutlinedButton(
                            onClick = {
                                PdfInvoiceGenerator.generatePdfFile(context, invoice)
                                android.widget.Toast.makeText(context, "Documento PDF generado con éxito.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_pdf_generate"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // COMPARTIR Button
                        Button(
                            onClick = {
                                PdfInvoiceGenerator.sharePdf(context, invoice)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_pdf_share"),
                            colors = ButtonDefaults.buttonColors(containerColor = HotelNavy),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = HotelGold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("COMPARTIR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        // GUARDAR Button
                        Button(
                            onClick = {
                                PdfInvoiceGenerator.savePdfLocally(context, invoice)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_pdf_save"),
                            colors = ButtonDefaults.buttonColors(containerColor = HotelGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = HotelNavyDark)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GUARDAR", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = HotelNavyDark)
                        }
                    }
                }
            }
        }
    }
}
