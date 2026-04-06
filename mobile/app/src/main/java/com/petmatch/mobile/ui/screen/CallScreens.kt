package com.petmatch.mobile.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petmatch.mobile.ui.theme.*

@Composable
fun AudioCallScreen(
    contactName: String = "Trần Thị Lan",
    petName: String = "Bella",
    onEndCall: () -> Unit = {}
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf("00:42") }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse1"
    )
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "pulse2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D1A),
                        Color(0xFF1A0A12),
                        Color(0xFF0D0D1A)
                    )
                )
            )
    ) {
        // Decorative dots
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(PrimaryPink.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 30.dp)
                .clip(CircleShape)
                .background(AccentPink.copy(alpha = 0.06f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Status
            Text(
                text = "Đang gọi...",
                fontSize = 16.sp,
                color = White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = callDuration,
                fontSize = 14.sp,
                color = AccentPink
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Avatar with pulse rings
            Box(contentAlignment = Alignment.Center) {
                // Outer pulse ring 2
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(pulseScale2)
                        .clip(CircleShape)
                        .background(PrimaryPink.copy(alpha = 0.08f))
                )
                // Outer pulse ring 1
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale1)
                        .clip(CircleShape)
                        .background(PrimaryPink.copy(alpha = 0.15f))
                )
                // Gradient ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPink, AccentPink)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF5C3317)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐕", fontSize = 48.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Contact name
            Text(
                text = contactName,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Pets,
                    contentDescription = null,
                    tint = AccentPink,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Chủ của $petName",
                    fontSize = 14.sp,
                    color = White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Call controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mute button
                CallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Bỏ tắt tiếng" else "Tắt tiếng",
                    backgroundColor = White.copy(alpha = 0.15f),
                    iconTint = White,
                    onClick = { isMuted = !isMuted }
                )

                // End call button (large red)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onEndCall,
                        modifier = Modifier.size(72.dp),
                        containerColor = ActionRed,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "End call",
                            tint = White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Kết thúc", fontSize = 12.sp, color = White.copy(alpha = 0.7f))
                }

                // Speaker button
                CallControlButton(
                    icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    label = if (isSpeakerOn) "Tắt loa" else "Loa ngoài",
                    backgroundColor = if (isSpeakerOn) PrimaryPink else White.copy(alpha = 0.15f),
                    iconTint = White,
                    onClick = { isSpeakerOn = !isSpeakerOn }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = backgroundColor,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = White.copy(alpha = 0.7f))
    }
}


// ─── Video Call Screen ──────────────────────────────────────────────────────

@Composable
fun VideoCallScreen(
    contactName: String = "Trần Thị Lan",
    onEndCall: () -> Unit = {}
) {
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Remote video (full screen background - simulated with pet image)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3D2010),
                            Color(0xFF5C3317),
                            Color(0xFF3D2010)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("🐕", fontSize = 120.sp)
        }

        // Dark gradient overlay at top and bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xCC000000), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xCC000000))
                    )
                )
        )

        // Top bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp, vertical = 52.dp)
        ) {
            Text(
                text = contactName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Text(
                text = "01:24",
                fontSize = 14.sp,
                color = White.copy(alpha = 0.7f)
            )
        }

        // Self-view (small PIP in top right)
        Box(
            modifier = Modifier
                .size(90.dp, 130.dp)
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 50.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isCameraOff) Color(0xFF2A2A2A)
                    else Color(0xFF8B6050)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCameraOff) {
                Icon(
                    Icons.Default.VideocamOff,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Text("🙂", fontSize = 40.sp)
            }
        }

        // Call controls at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera toggle
                VideoCallControlButton(
                    icon = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    label = if (isCameraOff) "Bật camera" else "Tắt camera",
                    isActive = !isCameraOff,
                    onClick = { isCameraOff = !isCameraOff }
                )

                // End call
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = onEndCall,
                        modifier = Modifier.size(68.dp),
                        containerColor = ActionRed,
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "End",
                            tint = White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Kết thúc", fontSize = 11.sp, color = White.copy(alpha = 0.7f))
                }

                // Mute
                VideoCallControlButton(
                    icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (isMuted) "Bỏ tắt" else "Tắt mic",
                    isActive = !isMuted,
                    onClick = { isMuted = !isMuted }
                )
            }
        }
    }
}

@Composable
fun VideoCallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = if (isActive) White.copy(alpha = 0.2f) else White.copy(alpha = 0.1f),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(0.dp)
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = White.copy(alpha = 0.7f))
    }
}
