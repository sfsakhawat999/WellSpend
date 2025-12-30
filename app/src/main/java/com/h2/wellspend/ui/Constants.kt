package com.h2.wellspend.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.h2.wellspend.data.Category

val CategoryColors = mapOf(
    Category.Shopping to Color(0xFFec4899), // Pink 500
    Category.Snacks to Color(0xFFf59e0b),   // Amber 500
    Category.Food to Color(0xFFef4444),     // Red 500
    Category.Rent to Color(0xFF6366f1),     // Indigo 500
    Category.Internet to Color(0xFF06b6d4), // Cyan 500
    Category.Phone to Color(0xFF8b5cf6),    // Violet 500
    Category.Education to Color(0xFF10b981),// Emerald 500
    Category.Others to Color(0xFF64748b),   // Slate 500
    Category.Transport to Color(0xFFf97316), // Orange 500
    Category.Health to Color(0xFF14b8a6),    // Teal 500
    Category.Entertainment to Color(0xFFd946ef), // Fuchsia 500
    Category.Utilities to Color(0xFFeab308), // Yellow 500
    Category.Insurance to Color(0xFF475569), // Slate 600
    Category.Savings to Color(0xFF22c55e),   // Green 500
    Category.Gifts to Color(0xFFdb2777),     // Pink 600
    Category.Travel to Color(0xFF0ea5e9),    // Sky 500
    Category.Subscriptions to Color(0xFF818cf8), // Indigo 400
    Category.Work to Color(0xFF334155),      // Slate 700
    Category.Pets to Color(0xFFa855f7),      // Purple 500

    Category.Family to Color(0xFFf43f5e),    // Rose 500
    Category.Fitness to Color(0xFF16a34a),   // Green 600
    Category.Beauty to Color(0xFFf472b6),    // Pink 400
    Category.Donations to Color(0xFFf59e0b), // Amber 500
    Category.Investments to Color(0xFF0ea5e9),// Sky 500
    Category.Groceries to Color(0xFF4ade80),  // Green 400
    Category.Clothing to Color(0xFFc084fc),   // Purple 400
    Category.Electronics to Color(0xFF60a5fa),// Blue 400
    Category.Electronics to Color(0xFF60a5fa),// Blue 400
    Category.Hobbies to Color(0xFFfbbf24),     // Amber 400
    Category.TransactionFee to Color(0xFF9ca3af) // Gray 400
)

val CategoryIcons = mapOf(
    Category.Shopping to Icons.Default.ShoppingCart,
    Category.Snacks to Icons.Default.LocalCafe, // Closest to cookie/snack
    Category.Food to Icons.Default.Fastfood,
    Category.Rent to Icons.Default.Home,
    Category.Internet to Icons.Default.Wifi,
    Category.Phone to Icons.Default.Phone,
    Category.Education to Icons.Default.School,
    Category.Others to Icons.Default.Book, // Generic
    Category.Transport to Icons.Default.DirectionsCar,
    Category.Health to Icons.Default.MedicalServices,
    Category.Entertainment to Icons.Default.Movie,
    Category.Utilities to Icons.Default.Bolt,
    Category.Insurance to Icons.Default.Security,
    Category.Savings to Icons.Default.Savings,
    Category.Gifts to Icons.Default.CardGiftcard,
    Category.Travel to Icons.Default.Flight,
    Category.Subscriptions to Icons.Default.Subscriptions,
    Category.Work to Icons.Default.Work,
    Category.Pets to Icons.Default.Pets,

    Category.Family to Icons.Default.FamilyRestroom,
    Category.Fitness to Icons.Default.FitnessCenter,
    Category.Beauty to Icons.Default.Face,
    Category.Donations to Icons.Default.VolunteerActivism,
    Category.Investments to Icons.AutoMirrored.Filled.TrendingUp,
    Category.Groceries to Icons.Default.LocalGroceryStore,
    Category.Clothing to Icons.Default.Checkroom,
    Category.Electronics to Icons.Default.Devices,
    Category.Electronics to Icons.Default.Devices,
    Category.Hobbies to Icons.Default.Palette,
    Category.TransactionFee to Icons.Default.AttachMoney
)

fun getCategoryColor(category: Category): Color {
    return CategoryColors[category] ?: Color.Gray
}

fun getCategoryIcon(category: Category): ImageVector {
    return CategoryIcons[category] ?: Icons.Default.Book
}
