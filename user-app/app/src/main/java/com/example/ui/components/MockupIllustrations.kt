package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MockupYellow
import com.example.ui.theme.MockupYellowDark

@Composable
fun BasketIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Soft purple basket container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF8B5CF6).copy(alpha = 0.85f),
            modifier = Modifier
                .width(size * 0.85f)
                .height(size * 0.65f)
                .align(Alignment.BottomCenter)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ShoppingBasket,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(size * 0.45f)
                )
            }
        }

        // Groceries peeking out
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = size * 0.08f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.22f)
                    .clip(CircleShape)
                    .background(Color(0xFF38BDF8))
            )
            Box(
                modifier = Modifier
                    .size(size * 0.26f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFBBF24))
            )
            Box(
                modifier = Modifier
                    .size(size * 0.2f)
                    .clip(CircleShape)
                    .background(Color(0xFFEF4444))
            )
        }

        // Gold % Coin Badge in corner
        Surface(
            shape = CircleShape,
            color = MockupYellow,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White),
            modifier = Modifier
                .size(size * 0.32f)
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "%",
                    color = MockupYellowDark,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun BellsIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Primary 3D Bell
        Surface(
            shape = CircleShape,
            color = Color(0xFF818CF8).copy(alpha = 0.35f),
            modifier = Modifier.size(size * 0.85f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier
                        .size(size * 0.55f)
                        .rotate(12f)
                )
            }
        }

        // Smaller accent bell
        Surface(
            shape = CircleShape,
            color = Color(0xFFA78BFA).copy(alpha = 0.3f),
            modifier = Modifier
                .size(size * 0.45f)
                .align(Alignment.BottomStart)
                .offset(x = (-4).dp, y = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color(0xFF8B5CF6),
                    modifier = Modifier.size(size * 0.3f)
                )
            }
        }

        // Discount % badge
        Surface(
            shape = CircleShape,
            color = Color(0xFFF97316),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
            modifier = Modifier
                .size(size * 0.28f)
                .align(Alignment.TopEnd)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "%",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun HomeTopBackgroundWash(modifier: Modifier = Modifier) {
    val isDark = com.example.ui.theme.LocalAppPalette.current.isDark
    val radialOuter = if (isDark) Color(0x389D174D) else Color(0xAAF8E4E9)
    val radialInner = if (isDark) Color(0x40BE185D) else Color(0x90F9CBD3)
    val ribbonStop0 = if (isDark) Color(0x30881337) else Color(0x50FFDFE6)
    val ribbonStop1 = if (isDark) Color(0x28E11D48) else Color(0x42F9849A)
    val ribbonStop2 = if (isDark) Color(0x18F43F5E) else Color(0x30F9B6C3)
    val ribbonStop3 = if (isDark) Color(0x0A881337) else Color(0x14F9DAE0)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(radialOuter, Color.Transparent),
                center = Offset(w * 0.68f, h * 0.16f),
                radius = w * 0.85f
            ),
            radius = w * 0.85f,
            center = Offset(w * 0.68f, h * 0.16f)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(radialInner, Color.Transparent),
                center = Offset(w * 0.82f, h * 0.08f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.82f, h * 0.08f)
        )

        rotate(degrees = -14f, pivot = Offset(w * 0.78f, 0f)) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0.0f to ribbonStop0,
                    0.45f to ribbonStop1,
                    0.68f to ribbonStop2,
                    0.88f to ribbonStop3,
                    1.0f to Color.Transparent,
                    startY = -h * 0.38f,
                    endY = h * 0.38f
                ),
                topLeft = Offset(w * 0.38f, -h * 0.38f),
                size = Size(w * 0.85f, h * 0.76f),
                cornerRadius = CornerRadius(w * 0.32f, w * 0.32f)
            )
        }
    }
}
