package com.petmatch.mobile.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petmatch.mobile.ui.theme.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// CallScreen – Màn hình gọi điện / gọi video (WebRTC)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    navController: NavController,
    calleeId: Long,
    calleeName: String,
    callType: String,   // "AUDIO" or "VIDEO"
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    var callStatus by remember { mutableStateOf(CallStatus.CALLING) }
    var callDuration by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var currentCallId by remember { mutableStateOf<Long?>(null) }

    // Timer when connected
    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.CONNECTED) {
            while (true) {
                delay(1000L)
                callDuration++
            }
        }
    }

    // Simulate connect after 3s (in real app: driven by WebSocket INCOMING_CALL signal)
    LaunchedEffect(Unit) {
        chatVm.startCall(ctx, calleeId, callType) { res ->
            currentCallId = res.id
        }
        delay(3000L)
        callStatus = CallStatus.CONNECTED
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A0A0E))
    ) {
        // Background blurred avatar
        AsyncImage(
            model = "https://placedog.net/400/800?r=$calleeId",
            contentDescription = null,
            modifier = Modifier.fillMaxSize().blur(20.dp),
            contentScale = ContentScale.Crop
        )
        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
        )

        // Video call: self-view in corner
        if (callType == "VIDEO" && callStatus == CallStatus.CONNECTED && !isCameraOff) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
                    .size(width = 100.dp, height = 140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, PrimaryPink, RoundedCornerShape(16.dp))
                    .background(Color(0xFF2D1520))
            ) {
                Text(
                    "📷 Bạn",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.7f)
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // Callee info
            Box(contentAlignment = Alignment.Center) {
                // Pulse animation ring
                if (callStatus == CallStatus.CALLING) {
                    PulsingRing(size = 120.dp, color = PrimaryPink.copy(0.3f))
                }
                AsyncImage(
                    model = "https://placedog.net/120/120?r=$calleeId",
                    contentDescription = calleeName,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(3.dp, Brush.linearGradient(listOf(GradientStart, GradientEnd)), CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = calleeName,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(Modifier.height(8.dp))

            // Status text
            Text(
                text = when (callStatus) {
                    CallStatus.CALLING -> if (callType == "VIDEO") "📹 Đang kết nối video..." else "📞 Đang gọi..."
                    CallStatus.CONNECTED -> formatCallDuration(callDuration)
                    CallStatus.ENDED -> "Cuộc gọi kết thúc"
                },
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.85f))
            )

            // Call quality indicator (when connected)
            if (callStatus == CallStatus.CONNECTED) {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height((6 + i * 4).dp)
                                .background(LikeGreen, RoundedCornerShape(2.dp))
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Tốt", style = MaterialTheme.typography.labelSmall, color = LikeGreen)
                }
            }

            Spacer(Modifier.weight(1f))

            // Control buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Secondary controls row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                        label = if (isSpeakerOn) "Loa ngoài" else "Tai nghe",
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        active = isSpeakerOn
                    )
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Bật mic" else "Tắt mic",
                        onClick = { isMuted = !isMuted },
                        active = isMuted,
                        activeColor = DislikeRed
                    )
                    if (callType == "VIDEO") {
                        CallControlButton(
                            icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            label = if (isCameraOff) "Bật camera" else "Tắt camera",
                            onClick = { isCameraOff = !isCameraOff },
                            active = isCameraOff,
                            activeColor = DislikeRed
                        )
                    }
                }

                // End call button (center)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            callStatus = CallStatus.ENDED
                            currentCallId?.let { id ->
                                chatVm.endCall(ctx, id, "ACCEPTED")
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(DislikeRed, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            "Kết thúc",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    "Kết thúc",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(0.7f)),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Pulsing Ring Animation ────────────────────────────────────────────────────
@Composable
private fun PulsingRing(size: Dp, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )
    Box(
        modifier = Modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}


// ── Call Control Button ───────────────────────────────────────────────────────
@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    activeColor: Color = PrimaryPink
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = if (active) activeColor else Color.White.copy(0.15f),
                    shape = CircleShape
                )
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.75f))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
private enum class CallStatus { CALLING, CONNECTED, ENDED }

private fun formatCallDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
