package com.yourpax.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourpax.app.data.comm.ConnectionStatus

@Composable
fun ConnectionStatusIndicator(
    status: ConnectionStatus,
    commType: String,
    modifier: Modifier = Modifier
) {
    val dotColor by animateColorAsState(
        targetValue = when (status) {
            ConnectionStatus.CONNECTED -> if (commType == "BT") Color(0xFF4CAF50) else Color(0xFF2196F3)
            ConnectionStatus.DISCONNECTED -> Color(0xFFF44336)
            ConnectionStatus.RECONNECTING -> Color(0xFFFFC107)
        },
        label = "dotColor"
    )

    val label = when (status) {
        ConnectionStatus.CONNECTED -> commType
        ConnectionStatus.DISCONNECTED -> "OFF"
        ConnectionStatus.RECONNECTING -> "..."
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = dotColor
        )
    }
}

@Composable
fun ReconnectOverlay(
    status: ConnectionStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (status == ConnectionStatus.CONNECTED) return

    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = when (status) {
                    ConnectionStatus.RECONNECTING -> "Reconnecting\u2026"
                    ConnectionStatus.DISCONNECTED -> "Connection Lost"
                    else -> ""
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (status == ConnectionStatus.DISCONNECTED) {
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Reconnect")
                }
            }
        }
    }
}
