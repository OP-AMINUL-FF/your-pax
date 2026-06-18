package com.yourpax.app.ui.screens.epd

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.yourpax.app.data.repository.SystemRepository
import com.yourpax.app.ui.components.DemoModeBanner
import com.yourpax.app.ui.components.EmptyState
import com.yourpax.app.ui.components.ModernCard
import com.yourpax.app.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EPDScreen(baseUrl: String = "http://192.168.4.1:8000") {
    val repo = remember { SystemRepository() }
    var isLive by remember { mutableStateOf(false) }
    var delayMs by remember { mutableIntStateOf(5000) }
    var imageScale by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableIntStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        delayMs = repo.getWebDelay().getOrDefault(5000)
        isLoading = false
    }

    LaunchedEffect(isLive) {
        while (isLive) {
            delay(delayMs.toLong())
            timestamp = System.currentTimeMillis()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!isFullscreen) {
            DemoModeBanner()
            TopAppBar(
                title = { Text("Live EPD", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) 0.dp else 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isFullscreen) {
                Text(
                    text = "Live e-paper screen view",
                    style = MaterialTheme.typography.bodySmall,
                    color = SubtleText
                )

                // Controls
                ModernCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Start/Stop toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    isLive = !isLive
                                    if (isLive) timestamp = System.currentTimeMillis()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLive) Error else Success
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    if (isLive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isLive) "Stop" else "Start Live View")
                            }
                            OutlinedButton(
                                onClick = {
                                    if (!isLive) timestamp = System.currentTimeMillis()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Refresh")
                            }
                        }

                        // Delay slider
                        Text("Update Interval: ${delayMs}ms", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Slider(
                            value = delayMs.toFloat(),
                            onValueChange = { delayMs = it.toInt() },
                            valueRange = 1000f..30000f,
                            steps = 28,
                            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1s", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                            Text("30s", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                        }

                        // Image size slider
                        Text("Image Size: ${(imageScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Slider(
                            value = imageScale,
                            onValueChange = { imageScale = it },
                            valueRange = 0.2f..2.0f,
                            colors = SliderDefaults.colors(thumbColor = Primary, activeTrackColor = Primary)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("20%", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                            Text("200%", style = MaterialTheme.typography.labelSmall, color = SubtleText)
                        }

                        // Rotation + Fullscreen buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RotationButton(
                                rotation = rotation,
                                onClick = { rotation = (rotation + 90) % 360 }
                            )
                            Spacer(Modifier.weight(1f))
                            FilledTonalButton(
                                onClick = { isFullscreen = !isFullscreen },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (isFullscreen) "Exit Fullscreen" else "Fullscreen")
                            }
                        }
                    }
                }
            }

            // EPD Image
            val imageUrl = "$baseUrl/screen.png?t=$timestamp"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = imageScale,
                        scaleY = imageScale,
                        rotationZ = rotation.toFloat()
                    ),
                shape = RoundedCornerShape(if (isFullscreen) 0.dp else 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = "EPD Screen",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentScale = ContentScale.Fit
                )
            }

            if (!isFullscreen) {
                Text(
                    text = "Click Fullscreen for expanded view · Interval: ${delayMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = SubtleText
                )
            }

            Spacer(Modifier.height(if (isFullscreen) 0.dp else 80.dp))
        }
    }
}

@Composable
private fun RotationButton(rotation: Int, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Rotate",
            modifier = Modifier.rotate(rotation.toFloat())
        )
        Spacer(Modifier.width(4.dp))
        Text("${rotation}°")
    }
}
