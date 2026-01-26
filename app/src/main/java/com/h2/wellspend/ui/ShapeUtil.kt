package com.h2.wellspend.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

fun getGroupedItemShape(index: Int, size: Int): RoundedCornerShape {
    return when {
        size == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
        index == size - 1 -> RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(3.dp)
    }
}

fun getGroupedItemBackgroundShape(index: Int, size: Int): RoundedCornerShape {
    return when {
        size == 1 -> RoundedCornerShape(17.dp)
        index == 0 -> RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == size - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 17.dp, bottomEnd = 17.dp)
        else -> RoundedCornerShape(4.dp)
    }
}
