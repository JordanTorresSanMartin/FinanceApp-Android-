package com.example.financeapp.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Las categorías guardan en BD nombres de icono al estilo Ionicons. Aquí los
 * homologamos a Material Icons buscando por palabra clave, para acercarnos al
 * look de la app RN sin depender del set exacto de glifos.
 */
fun categoryIcon(name: String?): ImageVector {
    val n = name?.lowercase().orEmpty()
    return when {
        n.contains("fast") || n.contains("food") || n.contains("restaurant") ||
            n.contains("pizza") || n.contains("nutrition") || n.contains("cafe") -> Icons.Filled.Restaurant
        n.contains("gas") || n.contains("fuel") || n.contains("bencina") -> Icons.Filled.LocalGasStation
        n.contains("bus") || n.contains("train") || n.contains("subway") -> Icons.Filled.DirectionsBus
        n.contains("car") || n.contains("transport") -> Icons.Filled.DirectionsCar
        n.contains("home") || n.contains("house") || n.contains("vivienda") || n.contains("hogar") -> Icons.Filled.Home
        n.contains("medkit") || n.contains("medical") || n.contains("health") ||
            n.contains("salud") || n.contains("fitness") -> Icons.Filled.LocalHospital
        n.contains("cart") || n.contains("bag") || n.contains("compras") || n.contains("pricetag") -> Icons.Filled.ShoppingCart
        n.contains("shirt") || n.contains("ropa") -> Icons.Filled.Checkroom
        n.contains("game") || n.contains("entreten") || n.contains("film") || n.contains("tv") -> Icons.Filled.SportsEsports
        n.contains("school") || n.contains("educa") || n.contains("book") -> Icons.Filled.School
        n.contains("phone") || n.contains("call") -> Icons.Filled.Phone
        n.contains("wifi") || n.contains("telecom") || n.contains("globe") -> Icons.Filled.Wifi
        n.contains("flash") || n.contains("bolt") || n.contains("power") || n.contains("servicio") -> Icons.Filled.Bolt
        n.contains("subscri") || n.contains("repeat") -> Icons.Filled.Subscriptions
        n.contains("card") || n.contains("credit") -> Icons.Filled.CreditCard
        n.contains("build") || n.contains("wrench") || n.contains("construct") -> Icons.Filled.Build
        n.contains("flight") || n.contains("airplane") || n.contains("travel") -> Icons.Filled.Flight
        n.contains("paw") || n.contains("pet") -> Icons.Filled.Pets
        n.contains("gift") || n.contains("regalo") -> Icons.Filled.CardGiftcard
        n.contains("cake") || n.contains("birthday") -> Icons.Filled.Cake
        n.contains("save") || n.contains("ahorro") || n.contains("piggy") -> Icons.Filled.Savings
        n.contains("swap") || n.contains("transfer") -> Icons.Filled.SwapHoriz
        n.contains("cash") || n.contains("wallet") || n.contains("money") ||
            n.contains("ingreso") || n.contains("trending") -> Icons.Filled.AttachMoney
        n.contains("bank") || n.contains("business") || n.contains("deuda") || n.contains("seguro") -> Icons.Filled.AccountBalance
        n.contains("dots") || n.contains("ellipsis") || n.contains("otros") -> Icons.Filled.MoreHoriz
        else -> Icons.Filled.Category
    }
}

/** Convierte un color hex de BD ("#RRGGBB") a Color de Compose, con fallback seguro. */
fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleaned = if (hex.startsWith("#")) hex else "#$hex"
        Color(android.graphics.Color.parseColor(cleaned))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}
