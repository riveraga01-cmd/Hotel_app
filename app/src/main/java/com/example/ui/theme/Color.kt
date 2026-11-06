package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Centralized Brand Palette for HOTEL RIVERA — TEMA CLARO UNIFORME
// 1. Backgrounds & Navigation
val HotelBackgroundLight = Color(0xFFFFFFFF) // Fondo general: blanco #FFFFFF
val HotelBackgroundDark = Color(0xFFFFFFFF)  // Fondo general uniforme #FFFFFF
val HotelHeaderNav = Color(0xFF0B2239)       // Encabezados y barras de navegación: fondo azul marino #0B2239 con texto blanco
val HotelNavy = Color(0xFF0B2239)            // Azul marino para headers y botones principales
val HotelNavyDark = Color(0xFF071521)
val HotelNavyLight = Color(0xFF13324E)       // Tabs / subheader navy

// 2. Cards & Forms
val CardLight = Color(0xFFFFFFFF)            // Tarjetas, ventanas y formularios principales: blanco #FFFFFF
val CardLightSubtle = Color(0xFFF8FAFC)      // Contenedor interior sutil
val DialogBackground = Color(0xFFFFFFFF)     // Diálogos fondo blanco #FFFFFF

// 3. Typography
val TextDark = Color(0xFF111111)             // Texto principal: negro #111111
val TextDarkPrimary = Color(0xFF111111)
val TextDarkSecondary = Color(0xFF424242)    // Texto secundario: gris oscuro legible #424242
val TextMuted = Color(0xFF424242)
val TextLightSecondary = Color(0xFF424242)   // Reemplazado por gris oscuro para legibilidad garantizada
val TextWhite = Color(0xFFFFFFFF)            // Texto sobre fondos oscuros (headers, botones): blanco #FFFFFF
val TextOnDarkSecondary = Color(0xFFC5D0DC)  // Texto secundario sobre barras oscuras

// 4. Buttons & Actions
val ButtonPrimary = Color(0xFF0B2239)        // Botones principales: azul marino #0B2239
val ButtonPrimaryText = Color(0xFFFFFFFF)    // Con texto blanco
val ButtonConfirm = Color(0xFF1B5E20)        // Acciones de confirmación (liberar habitación): verde oscuro con texto blanco
val ButtonConfirmText = Color(0xFFFFFFFF)
val ButtonSecondaryBg = Color(0xFFFFFFFF)    // Acciones secundarias: fondo blanco, texto oscuro y borde visible
val ButtonSecondaryText = Color(0xFF111111)
val ButtonSecondaryBorder = Color(0xFFBDBDBD)
val ButtonAccent = Color(0xFFD4AF37)         // Botones de acción destacada: dorado #D4AF37
val ButtonAccentText = Color(0xFF111111)     // Con texto oscuro

// 5. Hotel Accents
val HotelGold = Color(0xFFD4AF37)            // Acentos del hotel: dorado
val HotelGoldDark = Color(0xFFB78103)        // Dorado alto contraste para texto sobre blanco
val HotelGoldLight = Color(0xFFF7D774)       // Dorado para acento sobre azul marino
val HotelTeal = Color(0xFF0D9488)
val HotelTealLight = Color(0xFFCCFBF1)

val BorderLight = Color(0xFFE0E0E0)          // Bordes grises legibles
val BorderMedium = Color(0xFFBDBDBD)

// 6. Room & Operation Statuses (with high contrast)
// Estado disponible: verde con fondo y texto de contraste suficiente
val StatusAvailableGreen = Color(0xFF1B5E20)
val StatusAvailableBg = Color(0xFFE8F5E9)
val StatusAvailableBorder = Color(0xFF2E7D32)

// Estado ocupado: rojo con fondo y texto de contraste suficiente
val StatusOccupiedRed = Color(0xFFB71C1C)
val StatusOccupiedBg = Color(0xFFFFEBEE)
val StatusOccupiedBorder = Color(0xFFC62828)

// Estado reservado: ámbar con fondo y texto de contraste suficiente
val StatusReservedAmber = Color(0xFFB78103)
val StatusReservedBg = Color(0xFFFFF8E1)
val StatusReservedBorder = Color(0xFFE65100)

// Estado deshabilitado: gris con fondo y texto de contraste suficiente
val StatusDisabledGray = Color(0xFF455A64)
val StatusDisabledBg = Color(0xFFECEFF1)
val StatusDisabledBorder = Color(0xFF78909C)

// Errores: rojo legible con fondo apropiado
val ErrorRed = Color(0xFFD32F2F)
val ErrorRedBg = Color(0xFFFFEBEE)
