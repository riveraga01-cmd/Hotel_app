package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

object NotificationHelper {

    const val CHANNEL_STAYS = "hotel_rivera_stays"
    const val CHANNEL_RESERVATIONS = "hotel_rivera_reservations"
    const val CHANNEL_SYSTEM = "hotel_rivera_system"

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val staysChannel = NotificationChannel(
                CHANNEL_STAYS,
                "Hotel Rivera - Estadías y Tiempo",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de finalización de tiempo y adición de tiempo extra"
                enableVibration(true)
            }

            val resChannel = NotificationChannel(
                CHANNEL_RESERVATIONS,
                "Hotel Rivera - Reservaciones",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios de llegadas y alertas de no presentación"
            }

            val sysChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "Hotel Rivera - Estado de Conexión",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Estado de sincronización y conectividad de red"
            }

            manager.createNotificationChannel(staysChannel)
            manager.createNotificationChannel(resChannel)
            manager.createNotificationChannel(sysChannel)
        }
    }

    fun showStayEndingAlert(context: Context, roomNumber: String, minutesRemaining: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            roomNumber.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STAYS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("HOTEL RIVERA — Habitación $roomNumber")
            .setContentText("Tiempo por finalizar: quedan aprox. $minutesRemaining minutos.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(1000 + roomNumber.hashCode() % 1000, notification)
        } catch (_: SecurityException) {}
    }

    fun showStayEndedAlert(context: Context, roomNumber: String, stayId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            stayId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_STAYS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏰ TIEMPO FINALIZADO — Hab. $roomNumber")
            .setContentText("La estadía en habitación $roomNumber ha finalizado. Ingrese a Recepción para liberar o agregar tiempo extra.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(2000 + roomNumber.hashCode() % 1000, notification)
        } catch (_: SecurityException) {}
    }

    fun showReservationReminder(context: Context, roomNumber: String, clientName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_RESERVATIONS)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("HOTEL RIVERA — Recordatorio de Reservación")
            .setContentText("Llegada prevista en 3 horas: Habitación $roomNumber ($clientName)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(3000 + roomNumber.hashCode() % 1000, notification)
        } catch (_: SecurityException) {}
    }

    fun showReservationNoShowAlert(context: Context, roomNumber: String, clientName: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_RESERVATIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Reservación no presentada — Hab. $roomNumber")
            .setContentText("1 hora de retraso ($clientName). ¿Desea cancelar esta reservación por falta de presentación?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(4000 + roomNumber.hashCode() % 1000, notification)
        } catch (_: SecurityException) {}
    }

    fun showNetworkStatusNotification(context: Context, isConnected: Boolean) {
        val title = if (isConnected) "Conexión Restaurada" else "Sin Conexión a Internet"
        val body = if (isConnected) "Sincronizando operaciones pendientes con la nube..." else "Modo sin conexión activo. Las operaciones se guardan localmente."

        val notification = NotificationCompat.Builder(context, CHANNEL_SYSTEM)
            .setSmallIcon(if (isConnected) android.R.drawable.stat_sys_upload_done else android.R.drawable.ic_dialog_alert)
            .setContentTitle("HOTEL RIVERA: $title")
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(5001, notification)
        } catch (_: SecurityException) {}
    }
}
