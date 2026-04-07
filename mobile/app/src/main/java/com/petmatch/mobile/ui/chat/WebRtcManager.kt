package com.petmatch.mobile.ui.chat

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

/**
 * WebRtcManager – quản lý toàn bộ vòng đời WebRTC:
 *  • Khởi tạo PeerConnectionFactory
 *  • Tạo local audio/video tracks
 *  • Tạo PeerConnection với Google STUN
 *  • Offer / Answer / ICE candidate exchange
 *  • Gắn SurfaceViewRenderer cho local và remote video
 *
 * Thiết kế: **1 instance per call**. Gọi [release] khi kết thúc.
 */
class WebRtcManager(
    private val context: Context,
    val isVideoCall: Boolean,
    /** Callback khi có ICE candidate mới – gửi qua signaling. */
    private val onIceCandidate: (IceCandidate) -> Unit,
    /** Callback khi remote video track được thêm vào. */
    private val onRemoteVideoTrack: (VideoTrack) -> Unit,
    /** Callback khi ICE connection state thay đổi. */
    private val onIceConnectionChange: (PeerConnection.IceConnectionState) -> Unit = {}
) {
    companion object {
        private const val TAG = "WebRtcManager"
        private const val LOCAL_AUDIO_ID = "local_audio_track"
        private const val LOCAL_VIDEO_ID = "local_video_track"
        private val STUN_SERVERS = listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302"
        )
    }

    // ── WebRTC objects ────────────────────────────────────────────────────────
    private var eglBase: EglBase? = null
    private var factory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null

    // Tracks
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    private var surfaceHelper: SurfaceTextureHelper? = null
    private var videoSource: VideoSource? = null

    // Views
    private var localSurfaceView: SurfaceViewRenderer? = null
    private var remoteSurfaceView: SurfaceViewRenderer? = null

    // ICE candidates received before remote description is set
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var isRemoteDescriptionSet = false

    private val mainHandler = Handler(Looper.getMainLooper())

    // ── Init ──────────────────────────────────────────────────────────────────
    fun init() {
        Log.d(TAG, "Initializing WebRTC engine (isVideoCall=$isVideoCall)")

        // 1. EGL context (needed for video encode/decode)
        eglBase = EglBase.create()

        // 2. Initialize PeerConnectionFactory
        val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(initOptions)

        val encoderFactory = DefaultVideoEncoderFactory(eglBase!!.eglBaseContext, true, true)
        val decoderFactory = DefaultVideoDecoderFactory(eglBase!!.eglBaseContext)

        val audioDeviceModule: AudioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()

        factory = PeerConnectionFactory.builder()
            .setOptions(PeerConnectionFactory.Options())
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()

        // 3. PeerConnection configuration
        val iceServers = STUN_SERVERS.map {
            PeerConnection.IceServer.builder(it).createIceServer()
        }
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        }
        peerConnection = factory!!.createPeerConnection(rtcConfig, peerObserver)
            ?: error("Failed to create PeerConnection")

        // 4. Create local media tracks
        createLocalStream()
        Log.d(TAG, "WebRTC engine ready")
    }

    // ── Local stream ──────────────────────────────────────────────────────────
    private fun createLocalStream() {
        // Audio
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("echoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("noiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("autoGainControl", "true"))
        }
        val audioSource = factory!!.createAudioSource(audioConstraints)
        localAudioTrack = factory!!.createAudioTrack(LOCAL_AUDIO_ID, audioSource)
        localAudioTrack?.setEnabled(true)
        peerConnection?.addTrack(localAudioTrack!!, listOf("local_stream"))

        if (isVideoCall) createLocalVideo()
    }

    private fun createLocalVideo() {
        val enumerator = Camera2Enumerator(context)
        val frontCamera = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
            ?: enumerator.deviceNames.firstOrNull()

        if (frontCamera == null) {
            Log.w(TAG, "No camera found – skipping video")
            return
        }

        videoCapturer = enumerator.createCapturer(frontCamera, null) as? CameraVideoCapturer
            ?: run { Log.e(TAG, "Failed to create camera capturer"); return }

        surfaceHelper = SurfaceTextureHelper.create("CaptureThread", eglBase!!.eglBaseContext)
        videoSource = factory!!.createVideoSource(false)
        videoCapturer!!.initialize(surfaceHelper, context, videoSource!!.capturerObserver)
        videoCapturer!!.startCapture(1280, 720, 30)

        localVideoTrack = factory!!.createVideoTrack(LOCAL_VIDEO_ID, videoSource)
        localVideoTrack?.setEnabled(true)
        peerConnection?.addTrack(localVideoTrack!!, listOf("local_stream"))

        // Attach to local view if already set
        localSurfaceView?.let { localVideoTrack?.addSink(it) }
        Log.d(TAG, "Local video created, camera: $frontCamera")
    }

    // ── Surface views ─────────────────────────────────────────────────────────
    /** Gọi sau [init] và sau khi SurfaceViewRenderer đã được added vào composition. */
    fun initLocalSurface(view: SurfaceViewRenderer) {
        localSurfaceView = view
        view.init(eglBase!!.eglBaseContext, null)
        view.setMirror(true)
        view.setEnableHardwareScaler(true)
        localVideoTrack?.addSink(view)
    }

    fun initRemoteSurface(view: SurfaceViewRenderer) {
        remoteSurfaceView = view
        view.init(eglBase!!.eglBaseContext, null)
        view.setMirror(false)
        view.setEnableHardwareScaler(true)
    }

    // ── Offer / Answer ────────────────────────────────────────────────────────
    fun createOffer(onSuccess: (sdp: String) -> Unit, onError: (String) -> Unit = {}) {
        val constraints = makeCallConstraints()
        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                Log.d(TAG, "Offer created (${sdp.description.take(60)}...)")
                onSuccess(sdp.description)
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createOffer failed: $error")
                onError(error ?: "Unknown error")
            }
        }, constraints)
    }

    fun createAnswer(onSuccess: (sdp: String) -> Unit, onError: (String) -> Unit = {}) {
        val constraints = makeCallConstraints()
        peerConnection?.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                Log.d(TAG, "Answer created")
                onSuccess(sdp.description)
            }
            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "createAnswer failed: $error")
                onError(error ?: "Unknown error")
            }
        }, constraints)
    }

    fun setRemoteOffer(sdpString: String, onSet: () -> Unit = {}) {
        val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpString)
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainPendingIceCandidates()
                Log.d(TAG, "Remote OFFER set")
                onSet()
            }
        }, sdp)
    }

    fun setRemoteAnswer(sdpString: String) {
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, sdpString)
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                isRemoteDescriptionSet = true
                drainPendingIceCandidates()
                Log.d(TAG, "Remote ANSWER set")
            }
        }, sdp)
    }

    // ── ICE ───────────────────────────────────────────────────────────────────
    fun addRemoteIceCandidate(sdpMid: String?, sdpMLineIndex: Int, sdpCandidate: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdpCandidate)
        if (isRemoteDescriptionSet) {
            peerConnection?.addIceCandidate(candidate)
            Log.d(TAG, "ICE candidate added directly")
        } else {
            pendingIceCandidates.add(candidate)
            Log.d(TAG, "ICE candidate queued (remote desc not set yet)")
        }
    }

    private fun drainPendingIceCandidates() {
        pendingIceCandidates.forEach { peerConnection?.addIceCandidate(it) }
        Log.d(TAG, "Drained ${pendingIceCandidates.size} pending ICE candidates")
        pendingIceCandidates.clear()
    }

    // ── Controls ──────────────────────────────────────────────────────────────
    fun toggleMic(mute: Boolean) {
        localAudioTrack?.setEnabled(!mute)
    }

    fun toggleCamera(off: Boolean) {
        localVideoTrack?.setEnabled(!off)
    }

    fun switchCamera() {
        (videoCapturer as? CameraVideoCapturer)?.switchCamera(null)
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────
    fun release() {
        Log.d(TAG, "Releasing WebRTC resources")
        try {
            videoCapturer?.stopCapture()
        } catch (_: InterruptedException) {}
        videoCapturer?.dispose()
        surfaceHelper?.dispose()
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        videoSource?.dispose()
        peerConnection?.close()
        peerConnection?.dispose()
        factory?.dispose()
        localSurfaceView?.release()
        remoteSurfaceView?.release()
        eglBase?.release()
        pendingIceCandidates.clear()
        isRemoteDescriptionSet = false
    }

    // ── PeerConnection Observer ───────────────────────────────────────────────
    private val peerObserver = object : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate) {
            Log.d(TAG, "Local ICE candidate: ${candidate.sdp.take(60)}")
            onIceCandidate(candidate)
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver?.track() ?: return
            if (track is VideoTrack) {
                Log.d(TAG, "Remote VIDEO track received")
                mainHandler.post {
                    remoteSurfaceView?.let { track.addSink(it) }
                    onRemoteVideoTrack(track)
                }
            }
        }

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            Log.d(TAG, "ICE connection state: $state")
            mainHandler.post { onIceConnectionChange(state) }
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}
        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(channel: DataChannel) {}
        override fun onRenegotiationNeeded() {}
        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {
            Log.d(TAG, "Peer connection state: $state")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun makeCallConstraints() = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", if (isVideoCall) "true" else "false"))
    }
}

/** Convenience: SdpObserver with no-op defaults. */
open class SimpleSdpObserver : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String?) {}
    override fun onSetFailure(error: String?) {}
}
