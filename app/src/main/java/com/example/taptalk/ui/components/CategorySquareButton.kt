package com.example.taptalk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun CategorySquareButton(
    icon: ImageVector,
    contentDesc: String,
    enabled: Boolean,
    width: Dp = 80.dp,
    height: Dp = 90.dp,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val borderColor = if (enabled) Color.Black else Color.Gray
    val bg = if (enabled) Color(0xFFD9D9D9) else Color(0xFFE8E8E8)

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .border(2.dp, borderColor, shape)
            .background(bg, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDesc, tint = borderColor)
    }
}
