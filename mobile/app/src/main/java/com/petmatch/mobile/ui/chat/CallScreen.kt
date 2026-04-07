package com.petmatch.mobile.ui.chat

import android.Manifest
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.petmatch.mobile.data.model.SignalingMessage
import com.petmatch.mobile.ui.theme.*
import kotlinx.coroutines.delay
import org.webrtc.PeerConnection
import org.webrtc.SurfaceViewRenderer

// ─────────────────────────────────────────────────────────────────────────────
// CallScreen – Cuộc gọi âm thanh / video thật (WebRTC P2P)
//
//  • isCallee = false → Caller flow: startCall REST → createOffer → gửi OFFER
//  • isCallee = true  → Callee flow: chờ OFFER → setRemoteDesc → createAnswer
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreen(
    navController: NavController,
    peerId: Long,
    peerName: String,
    callType: String,          // "AUDIO" | "VIDEO"
    isCallee: Boolean = false,
    incomingCallId: Long = 0L, // callId khi là callee (từ IncomingCallState)
    chatVm: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val ctx = LocalContext.current
    val gson = remember { Gson() }
    val isVideoCall = callType == "VIDEO"

    // ── State ─────────────────────────────────────────────────────────────────
    var callStatus by remember { mutableStateOf(if (isCallee) CallStatus.RINGING else CallStatus.CALLING) }
    var callDuration by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(true)  }
    var currentCallId by remember { mutableStateOf(incomingCallId) }
    var hasRemoteVideo by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val rtcSignal by chatVm.rtcSignal.collectAsState()
    val signalingConnected by chatVm.signalingConnected.collectAsState()

    // ── Permission launcher ───────────────────────────────────────────────────
    var permissionsGranted by remember { mutableStateOf(false) }
    val permissions = if (isVideoCall)
        arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    else
        arrayOf(Manifest.permission.RECORD_AUDIO)

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        permissionsGranted = granted.values.all { it }
    }

    // ── WebRtcManager (1 per call, released on dispose) ──────────────────────
    val webRtcManager = remember {
        WebRtcManager(
            context = ctx,
            isVideoCall = isVideoCall,
            onIceCandidate = { candidate ->
                val json = gson.toJson(mapOf(
                    "sdpMid"        to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "candidate"     to candidate.sdp
                ))
                chatVm.sendRtcSignal(SignalingMessage(
                    senderId   = 0L,   // filled by server from JWT
                    receiverId = peerId,
                    type       = "ICE_CANDIDATE",
                    data       = json
                ))
            },
            onRemoteVideoTrack = { hasRemoteVideo = true },
            onIceConnectionChange = { state ->
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED ->
                        callStatus = CallStatus.CONNECTED

                    PeerConnection.IceConnectionState.FAILED ->
                        errorMsg = "Kết nối thất bại. Thử lại?"

                    PeerConnection.IceConnectionState.DISCONNECTED ->
                        if (callStatus == CallStatus.CONNECTED) errorMsg = "Mất kết nối..."

                    else -> {}
                }
            }
        )
    }

    // ── Lifecycle: init WebRTC + check signaling ──────────────────────────────
    DisposableEffect(Unit) {
        permLauncher.launch(permissions)
        webRtcManager.init()
        chatVm.connectSignaling(ctx)
        onDispose {
            // Gửi HANG_UP nếu chưa ended
            if (callStatus != CallStatus.ENDED) {
                chatVm.sendRtcSignal(SignalingMessage(0L, peerId, "HANG_UP", null))
                currentCallId.let { id -> 
                    if (id > 0) {
                        chatVm.endCall(ctx, id, "MISSED", if (callDuration > 0) callDuration else null, peerId)
                    } else {
                        chatVm.isCallCancelled = true
                    }
                }
            }
            webRtcManager.release()
            // Do NOT disconnect signaling — it is shared globally across all screens
        }
    }

    // ── Caller flow: khi signaling connected → startCall REST → createOffer ──
    LaunchedEffect(signalingConnected) {
        if (!signalingConnected || isCallee) return@LaunchedEffect
        chatVm.startCall(ctx, peerId, callType) { res ->
            currentCallId = res.id
            // Chờ CALLEE_READY hoặc timeout 30s rồi create offer
        }
    }

    // ── Process incoming signals ──────────────────────────────────────────────
    LaunchedEffect(rtcSignal) {
        val signal = rtcSignal ?: return@LaunchedEffect
        when (signal.type) {

            // Callee nhận OFFER → setRemoteOffer → createAnswer
            "OFFER" -> if (isCallee) {
                val rawSdp = parseGsonField(gson, signal.data, "sdp") ?: return@LaunchedEffect
                webRtcManager.setRemoteOffer(rawSdp) {
                    webRtcManager.createAnswer(
                        onSuccess = { sdp ->
                            chatVm.sendRtcSignal(SignalingMessage(
                                senderId   = 0L,
                                receiverId = signal.senderId,
                                type       = "ANSWER",
                                data       = gson.toJson(mapOf("sdp" to sdp))
                            ))
                        }
                    )
                }
            }

            // Caller nhận ANSWER
            "ANSWER" -> if (!isCallee) {
                val rawSdp = parseGsonField(gson, signal.data, "sdp") ?: return@LaunchedEffect
                webRtcManager.setRemoteAnswer(rawSdp)
            }

            // Cả 2 nhận ICE_CANDIDATE
            "ICE_CANDIDATE" -> {
                val map   = parseGsonMap(gson, signal.data) ?: return@LaunchedEffect
                val mid   = map["sdpMid"] as? String ?: ""
                val idx   = (map["sdpMLineIndex"] as? Double)?.toInt() ?: 0
                val cand  = map["candidate"]     as? String ?: return@LaunchedEffect
                webRtcManager.addRemoteIceCandidate(mid, idx, cand)
            }

            // Callee sẵn sàng → caller gửi OFFER
            "CALLEE_READY" -> if (!isCallee) {
                webRtcManager.createOffer(
                    onSuccess = { sdp ->
                        chatVm.sendRtcSignal(SignalingMessage(
                            senderId   = 0L,
                            receiverId = peerId,
                            type       = "OFFER",
                            data       = gson.toJson(mapOf("sdp" to sdp))
                        ))
                    },
                    onError = { errorMsg = "Không tạo được kết nối: $it" }
                )
            }

            // Người kia cúp máy
            "HANG_UP", "REJECT" -> {
                callStatus = CallStatus.ENDED
                delay(800L)
                navController.popBackStack()
            }
        }
        chatVm.consumeRtcSignal()
    }

    // ── Callee: khi signaling connected → gửi CALLEE_READY ──────────────────
    LaunchedEffect(signalingConnected) {
        if (!signalingConnected || !isCallee) return@LaunchedEffect
        chatVm.sendRtcSignal(SignalingMessage(
            senderId   = 0L,
            receiverId = peerId,
            type       = "CALLEE_READY",
            data       = null
        ))
    }

    // ── Timer khi CONNECTED ───────────────────────────────────────────────────
    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.CONNECTED) {
            while (true) { delay(1000L); callDuration++ }
        }
    }

    // ── Callee: auto-reject nếu không nhận sau 60s ────────────────────────────
    LaunchedEffect(callStatus) {
        if (callStatus == CallStatus.RINGING) {
            delay(60_000L)
            if (callStatus == CallStatus.RINGING) {
                chatVm.sendRtcSignal(SignalingMessage(0L, peerId, "HANG_UP", null))
                currentCallId.let { if (it > 0) chatVm.endCall(ctx, it, "MISSED", null, peerId) }
                navController.popBackStack()
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // UI
    // ═════════════════════════════════════════════════════════════════════════
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D0512))) {

        // ── Video call: remote video full-screen ──────────────────────────────
        if (isVideoCall && callStatus == CallStatus.CONNECTED) {
            if (hasRemoteVideo) {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webRtcManager.initRemoteSurface(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Chờ remote video
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryPink, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Đang nhận video...", color = Color.White.copy(0.75f))
                    }
                }
            }
        } else {
            // Audio call hoặc chưa connected: blurred avatar background
            AsyncImage(
                model        = "https://placedog.net/400/800?r=$peerId",
                contentDescription = null,
                modifier     = Modifier.fillMaxSize().blur(24.dp),
                contentScale = ContentScale.Crop
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.70f)))
        }

        // ── Video call: local camera (PiP góc phải trên) ──────────────────────
        if (isVideoCall && callStatus == CallStatus.CONNECTED && !isCameraOff) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 16.dp)
                    .size(width = 104.dp, height = 148.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(2.dp, PrimaryPink, RoundedCornerShape(18.dp))
            ) {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webRtcManager.initLocalSurface(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── Nội dung chính ────────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(72.dp))

            // Ẩn khi video đã connected (remote stream chiếm toàn màn hình)
            val showCenterInfo = !(isVideoCall && callStatus == CallStatus.CONNECTED && hasRemoteVideo)
            if (showCenterInfo) {
                // Pulse ring khi đang gọi / ringing
                Box(contentAlignment = Alignment.Center) {
                    if (callStatus == CallStatus.CALLING || callStatus == CallStatus.RINGING) {
                        PulsingCallRing(size = 130.dp, color = PrimaryPink.copy(0.35f))
                        PulsingCallRing(size = 160.dp, color = PrimaryPink.copy(0.15f), delayMs = 400)
                    }
                    AsyncImage(
                        model              = "https://placedog.net/120/120?r=$peerId",
                        contentDescription = peerName,
                        modifier           = Modifier
                            .size(114.dp)
                            .clip(CircleShape)
                            .border(
                                3.dp,
                                Brush.linearGradient(listOf(GradientStart, GradientEnd)),
                                CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(20.dp))

                Text(
                    text  = peerName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold, color = Color.White
                    )
                )
                Spacer(Modifier.height(8.dp))
            }

            // Status line
            AnimatedContent(
                targetState = callStatus,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "status"
            ) { status ->
                Text(
                    text = when (status) {
                        CallStatus.CALLING  -> if (isVideoCall) "📹 Đang gọi video..." else "📞 Đang gọi..."
                        CallStatus.RINGING  -> "🔔 Đang đổ chuông..."
                        CallStatus.CONNECTED -> formatCallDuration(callDuration)
                        CallStatus.ENDED    -> "Cuộc gọi kết thúc"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(if (showCenterInfo) 0.9f else 0.85f)
                    ),
                    modifier = if (!showCenterInfo) Modifier.padding(top = 16.dp) else Modifier
                )
            }

            // Signal quality khi connected
            AnimatedVisibility(callStatus == CallStatus.CONNECTED) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.padding(top = 6.dp)
                ) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .width(4.dp).height((6 + i * 5).dp)
                                .background(LikeGreen, RoundedCornerShape(2.dp))
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Kết nối tốt",
                        style = MaterialTheme.typography.labelSmall,
                        color = LikeGreen
                    )
                }
            }

            // Error message
            errorMsg?.let {
                Spacer(Modifier.height(8.dp))
                Surface(color = DislikeRed.copy(0.2f), shape = RoundedCornerShape(8.dp)) {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall.copy(color = DislikeRed),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Controls ─────────────────────────────────────────────────────
            CallControlsPanel(
                callStatus  = callStatus,
                isVideoCall = isVideoCall,
                isMuted     = isMuted,
                isSpeakerOn = isSpeakerOn,
                isCameraOff = isCameraOff,
                onToggleMic = {
                    isMuted = !isMuted
                    webRtcManager.toggleMic(isMuted)
                },
                onToggleSpeaker = { isSpeakerOn = !isSpeakerOn },
                onToggleCamera = {
                    isCameraOff = !isCameraOff
                    webRtcManager.toggleCamera(isCameraOff)
                },
                onSwitchCamera = {
                    isFrontCamera = !isFrontCamera
                    webRtcManager.switchCamera()
                },
                onEndCall = {
                    val finalStatus = if (callDuration > 0) "ACCEPTED" else "MISSED"
                    callStatus = CallStatus.ENDED
                    chatVm.sendRtcSignal(SignalingMessage(0L, peerId, "HANG_UP", null))
                    currentCallId.let { id -> 
                        if (id > 0) {
                            chatVm.endCall(ctx, id, finalStatus, if (callDuration > 0) callDuration else null, peerId)
                        } else {
                            chatVm.isCallCancelled = true
                        }
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}

// ── Controls Panel ────────────────────────────────────────────────────────────
@Composable
private fun CallControlsPanel(
    callStatus: CallStatus,
    isVideoCall: Boolean,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isCameraOff: Boolean,
    onToggleMic: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onSwitchCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f)))
            )
            .padding(horizontal = 32.dp, vertical = 32.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Secondary controls
        Row(
            modifier                 = Modifier.fillMaxWidth(),
            horizontalArrangement   = Arrangement.SpaceEvenly,
            verticalAlignment        = Alignment.CenterVertically
        ) {
            CallCtrlBtn(
                icon    = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                label   = if (isSpeakerOn) "Loa ngoài" else "Tai nghe",
                onClick = onToggleSpeaker,
                active  = isSpeakerOn
            )
            CallCtrlBtn(
                icon        = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                label       = if (isMuted) "Bật mic" else "Tắt mic",
                onClick     = onToggleMic,
                active      = isMuted,
                activeColor = DislikeRed
            )
            if (isVideoCall) {
                CallCtrlBtn(
                    icon        = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    label       = if (isCameraOff) "Bật cam" else "Tắt cam",
                    onClick     = onToggleCamera,
                    active      = isCameraOff,
                    activeColor = DislikeRed
                )
                CallCtrlBtn(
                    icon    = Icons.Default.FlipCameraAndroid,
                    label   = "Đảo cam",
                    onClick = onSwitchCamera
                )
            }
        }

        // End Call button
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier
                        .size(72.dp)
                        .background(DislikeRed, CircleShape)
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        "Kết thúc",
                        tint     = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Kết thúc",
                    style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(0.75f))
                )
            }
        }
    }
}

// ── Control Button ─────────────────────────────────────────────────────────────
@Composable
private fun CallCtrlBtn(
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
            onClick  = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(
                    color  = if (active) activeColor else Color.White.copy(0.18f),
                    shape  = CircleShape
                )
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.78f))
    }
}

// ── Pulsing Ring Animation ─────────────────────────────────────────────────────
@Composable
private fun PulsingCallRing(size: Dp, color: Color, delayMs: Int = 0) {
    val transition = rememberInfiniteTransition(label = "pulse_$delayMs")
    val scale by transition.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.5f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1400, delayMillis = delayMs, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale_$delayMs"
    )
    val alpha by transition.animateFloat(
        initialValue   = 0.7f,
        targetValue    = 0f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1400, delayMillis = delayMs, easing = EaseOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha_$delayMs"
    )
    Box(
        modifier = Modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// IncomingCallScreen – Overlay hiển thị khi có cuộc gọi đến
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun IncomingCallOverlay(
    state: IncomingCallState,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier            = Modifier.padding(40.dp)
        ) {
            Spacer(Modifier.height(40.dp))

            // Pulsing avatar
            Box(contentAlignment = Alignment.Center) {
                PulsingCallRing(size = 120.dp, color = PrimaryPink.copy(0.4f))
                PulsingCallRing(size = 150.dp, color = PrimaryPink.copy(0.2f), delayMs = 500)
                AsyncImage(
                    model              = "https://placedog.net/110/110?r=${state.callerId}",
                    contentDescription = state.callerName,
                    modifier           = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .border(3.dp, PrimaryPink, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text  = state.callerName,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            )

            val typeLabel = if (state.callType == "VIDEO") "📹 Cuộc gọi video đến" else "📞 Cuộc gọi thoại đến"
            Text(
                text  = typeLabel,
                style = MaterialTheme.typography.bodyLarge.copy(color = Color.White.copy(0.8f)),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            // Accept / Reject
            Row(
                horizontalArrangement = Arrangement.spacedBy(80.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Reject
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick  = onReject,
                        modifier = Modifier
                            .size(72.dp)
                            .background(DislikeRed, CircleShape)
                    ) {
                        Icon(Icons.Default.CallEnd, "Từ chối", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Từ chối", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.75f))
                }

                // Accept
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val pulseAcc = rememberInfiniteTransition(label = "pulse_accept")
                    val scAcc by pulseAcc.animateFloat(
                        initialValue = 1f, targetValue = 1.12f,
                        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                        label = "sc_accept"
                    )
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(scAcc)
                            .background(LikeGreen, CircleShape)
                    ) {
                        Icon(Icons.Default.Call, "Nghe máy", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Nghe máy", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.75f))
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Enum + Helpers
// ═════════════════════════════════════════════════════════════════════════════
enum class CallStatus { CALLING, RINGING, CONNECTED, ENDED }

private fun formatCallDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}

private fun parseGsonField(gson: Gson, data: String?, field: String): String? = try {
    @Suppress("UNCHECKED_CAST")
    (gson.fromJson(data, Map::class.java) as? Map<String, Any>)?.get(field) as? String
} catch (_: Exception) { null }

private fun parseGsonMap(gson: Gson, data: String?): Map<*, *>? = try {
    gson.fromJson(data, Map::class.java)
} catch (_: Exception) { null }
