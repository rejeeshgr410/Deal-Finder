package com.example.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
