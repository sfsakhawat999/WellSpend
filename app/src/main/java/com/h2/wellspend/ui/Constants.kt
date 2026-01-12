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
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.LocalDining
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocalHotel
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.SmokingRooms
import androidx.compose.material.icons.filled.SmokeFree
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocalPizza
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.h2.wellspend.data.SystemCategory

val CategoryColors = mapOf(
    SystemCategory.Shopping to Color(0xFFec4899), // Pink 500
    SystemCategory.Snacks to Color(0xFFf59e0b),   // Amber 500
    SystemCategory.Food to Color(0xFFef4444),     // Red 500
    SystemCategory.Rent to Color(0xFF6366f1),     // Indigo 500
    SystemCategory.Internet to Color(0xFF06b6d4), // Cyan 500
    SystemCategory.Phone to Color(0xFF8b5cf6),    // Violet 500
    SystemCategory.Education to Color(0xFF10b981),// Emerald 500
    SystemCategory.Others to Color(0xFF64748b),   // Slate 500
    SystemCategory.Transport to Color(0xFFf97316), // Orange 500
    SystemCategory.Health to Color(0xFF14b8a6),    // Teal 500
    SystemCategory.Entertainment to Color(0xFFd946ef), // Fuchsia 500
    SystemCategory.Utilities to Color(0xFFeab308), // Yellow 500
    SystemCategory.Insurance to Color(0xFF475569), // Slate 600
    SystemCategory.Savings to Color(0xFF22c55e),   // Green 500
    SystemCategory.Gifts to Color(0xFFdb2777),     // Pink 600
    SystemCategory.Travel to Color(0xFF0ea5e9),    // Sky 500
    SystemCategory.Subscriptions to Color(0xFF818cf8), // Indigo 400
    SystemCategory.Work to Color(0xFF334155),      // Slate 700
    SystemCategory.Pets to Color(0xFFa855f7),      // Purple 500

    SystemCategory.Family to Color(0xFFf43f5e),    // Rose 500
    SystemCategory.Fitness to Color(0xFF16a34a),   // Green 600
    SystemCategory.Beauty to Color(0xFFf472b6),    // Pink 400
    SystemCategory.Donations to Color(0xFFf59e0b), // Amber 500
    SystemCategory.Investments to Color(0xFF0ea5e9),// Sky 500
    SystemCategory.Groceries to Color(0xFF4ade80),  // Green 400
    SystemCategory.Clothing to Color(0xFFc084fc),   // Purple 400
    SystemCategory.Electronics to Color(0xFF60a5fa),// Blue 400
    SystemCategory.Hobbies to Color(0xFFfbbf24),     // Amber 400
    SystemCategory.TransactionFee to Color(0xFF9ca3af), // Gray 400
    SystemCategory.Loan to Color(0xFF7c3aed), // Violet 600
    SystemCategory.BalanceAdjustment to Color(0xFF64748b) // Slate 500 (Neutral)
)

val CategoryIcons = mapOf(
    SystemCategory.Shopping to Icons.Default.ShoppingCart,
    SystemCategory.Snacks to Icons.Default.LocalCafe, // Closest to cookie/snack
    SystemCategory.Food to Icons.Default.Fastfood,
    SystemCategory.Rent to Icons.Default.Home,
    SystemCategory.Internet to Icons.Default.Wifi,
    SystemCategory.Phone to Icons.Default.Phone,
    SystemCategory.Education to Icons.Default.School,
    SystemCategory.Others to Icons.Default.Book, // Generic
    SystemCategory.Transport to Icons.Default.DirectionsCar,
    SystemCategory.Health to Icons.Default.MedicalServices,
    SystemCategory.Entertainment to Icons.Default.Movie,
    SystemCategory.Utilities to Icons.Default.Bolt,
    SystemCategory.Insurance to Icons.Default.Security,
    SystemCategory.Savings to Icons.Default.Savings,
    SystemCategory.Gifts to Icons.Default.CardGiftcard,
    SystemCategory.Travel to Icons.Default.Flight,
    SystemCategory.Subscriptions to Icons.Default.Subscriptions,
    SystemCategory.Work to Icons.Default.Work,
    SystemCategory.Pets to Icons.Default.Pets,

    SystemCategory.Family to Icons.Default.FamilyRestroom,
    SystemCategory.Fitness to Icons.Default.FitnessCenter,
    SystemCategory.Beauty to Icons.Default.Face,
    SystemCategory.Donations to Icons.Default.VolunteerActivism,
    SystemCategory.Investments to Icons.AutoMirrored.Filled.TrendingUp,
    SystemCategory.Groceries to Icons.Default.LocalGroceryStore,
    SystemCategory.Clothing to Icons.Default.Checkroom,
    SystemCategory.Electronics to Icons.Default.Devices,
    SystemCategory.Hobbies to Icons.Default.Palette,
    SystemCategory.TransactionFee to Icons.Default.AttachMoney,
    SystemCategory.Loan to Icons.Default.AccountBalance,
    SystemCategory.BalanceAdjustment to Icons.Default.Tune
)

fun getSystemCategoryColor(category: SystemCategory): Color {
    return CategoryColors[category] ?: Color.Gray
}

fun getSystemCategoryIcon(category: SystemCategory): ImageVector {
    return CategoryIcons[category] ?: Icons.Default.Book
}

// Helper to get ALL icons by name for custom categories
val AllIcons: Map<String, ImageVector> = mapOf(
    "Label" to Icons.Default.Label,
    "Shopping Cart" to Icons.Default.ShoppingCart,
    "Cafe" to Icons.Default.LocalCafe,
    "Fastfood" to Icons.Default.Fastfood,
    "Home" to Icons.Default.Home,
    "Wifi" to Icons.Default.Wifi,
    "Phone" to Icons.Default.Phone,
    "School" to Icons.Default.School,
    "Book" to Icons.Default.Book,
    "Car" to Icons.Default.DirectionsCar,
    "Medical" to Icons.Default.MedicalServices,
    "Movie" to Icons.Default.Movie,
    "Bolt" to Icons.Default.Bolt,
    "Security" to Icons.Default.Security,
    "Savings" to Icons.Default.Savings,
    "Gift" to Icons.Default.CardGiftcard,
    "Flight" to Icons.Default.Flight,
    "Subscriptions" to Icons.Default.Subscriptions,
    "Work" to Icons.Default.Work,
    "Pets" to Icons.Default.Pets,
    "Family" to Icons.Default.FamilyRestroom,
    "Fitness" to Icons.Default.FitnessCenter,
    "Face" to Icons.Default.Face,
    "Volunteer" to Icons.Default.VolunteerActivism,
    "Trending Up" to Icons.AutoMirrored.Filled.TrendingUp,
    "Grocery" to Icons.Default.LocalGroceryStore,
    "Clothing" to Icons.Default.Checkroom,
    "Devices" to Icons.Default.Devices,
    "Palette" to Icons.Default.Palette,
    "Money" to Icons.Default.AttachMoney,
    "Bank" to Icons.Default.AccountBalance,
    "Tune" to Icons.Default.Tune,
    "Star" to Icons.Default.Star,
    "Heart" to Icons.Default.Favorite,
    "Person" to Icons.Default.Person,
    "Place" to Icons.Default.Place,
    "Email" to Icons.Default.Email,
    "Call" to Icons.Default.Call,
    "Camera" to Icons.Default.Camera,
    "Map" to Icons.Default.Map,
    "Dining" to Icons.Default.LocalDining,
    "Gas" to Icons.Default.LocalGasStation,
    "Pharmacy" to Icons.Default.LocalPharmacy,
    "Hotel" to Icons.Default.LocalHotel,
    "Taxi" to Icons.Default.LocalTaxi,
    "Bus" to Icons.Default.DirectionsBus,
    "Train" to Icons.Default.Train,
    "Motorcycle" to Icons.Default.TwoWheeler,
    "Bike" to Icons.Default.PedalBike,
    "Walk" to Icons.Default.DirectionsWalk,
    "Run" to Icons.Default.DirectionsRun,
    "Pool" to Icons.Default.Pool,
    "Park" to Icons.Default.Park,
    "Beach" to Icons.Default.BeachAccess,
    "Casino" to Icons.Default.Casino,
    "Spa" to Icons.Default.Spa,
    "Bar" to Icons.Default.LocalBar,
    "Kitchen" to Icons.Default.Kitchen,
    "Child" to Icons.Default.ChildCare,
    "Smoke" to Icons.Default.SmokingRooms,
    "No Smoke" to Icons.Default.SmokeFree,
    "AC" to Icons.Default.AcUnit,
    "Power" to Icons.Default.Power,
    "Sunny" to Icons.Default.WbSunny,
    "Night" to Icons.Default.Nightlight,
    "Landscape" to Icons.Default.Landscape,
    "Brush" to Icons.Default.Brush,
    "Color" to Icons.Default.ColorLens,
    "Music" to Icons.Default.MusicNote,
    "Headphones" to Icons.Default.Headphones,
    "Mic" to Icons.Default.Mic,
    "Radio" to Icons.Default.Radio,
    "Game" to Icons.Default.VideogameAsset,
    "Toys" to Icons.Default.Toys,
    "Soccer" to Icons.Default.SportsSoccer,
    "Basketball" to Icons.Default.SportsBasketball,
    "Tennis" to Icons.Default.SportsTennis,
    "Golf" to Icons.Default.SportsGolf,
    "Construction" to Icons.Default.Construction,
    "Tools" to Icons.Default.Build,
    "Commute" to Icons.Default.Commute,
    "Farming" to Icons.Default.Agriculture,
    "Business" to Icons.Default.Business,
    "Party" to Icons.Default.Celebration,
    "Shipping" to Icons.Default.LocalShipping,
    "Pizza" to Icons.Default.LocalPizza,
    
    // Add more icons as valid choices
    "Add" to Icons.Default.Add,
    "List" to Icons.Default.List,
    "Settings" to Icons.Default.Settings,
    "Repeats" to Icons.Default.Repeat,
    "Download" to Icons.Default.Download
)

fun getIconByName(name: String): ImageVector {
    // Try to find in AllIcons, or check if it matches a SystemCategory name
    return AllIcons[name] 
        ?: try { CategoryIcons[SystemCategory.valueOf(name)] } catch(e: Exception) { null }
        ?: Icons.Default.Label
}
