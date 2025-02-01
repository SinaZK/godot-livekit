// TODO: Update to match your plugin's package name.
package org.godotengine.plugin.android.livekit

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.SignalInfo
import org.godotengine.godot.plugin.UsedByGodot

class GodotLivekit(godot: Godot): GodotPlugin(godot), LifecycleOwner {

    var wsURL = "wss://livekit.sizakgames.ir"
    val TAG = "LiveKit"
    lateinit var room: Room
    private var lifecycleRegistry = LifecycleRegistry(this)

    override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

    override fun getPluginSignals(): MutableSet<SignalInfo> {
        val signals: MutableSet<SignalInfo> = HashSet()

        signals.add(SignalInfo("event", String::class.java))
        signals.add(SignalInfo("participant_connected", String::class.java))
        signals.add(SignalInfo("participant_disconnected", String::class.java))

        return signals
    }

    @UsedByGodot
    fun init(url:String) {
        wsURL = url
        room = activity?.let { LiveKit.create(it.baseContext) }!!
    }

    @UsedByGodot
    fun setMicEnabled(enable:Boolean) {
        room.setMicrophoneMute(!enable)
    }


    @UsedByGodot
    fun setSpeakerEnabled(enable:Boolean) {
        room.setSpeakerMute(!enable)
    }


    @UsedByGodot
    fun leaveRoom() {
        Log.i(TAG, "Disconnecting")
        room.disconnect()
    }


    @UsedByGodot
    fun joinRoom(token:String) {
        Log.i(TAG, "JoinRoom: $token")
        lifecycleScope.launch {

            // Setup event handling.
            launch {
                room.events.collect { event ->
                    when (event) {
                        is RoomEvent.TrackSubscribed -> onTrackSubscribed(event)
                        is RoomEvent.Connected -> onConnected()
                        is RoomEvent.Disconnected -> onDisconnected()
                        is RoomEvent.FailedToConnect -> onConnectionFailed()
//                        is RoomEvent.ActiveSpeakersChanged ->
//                            if (event.speakers.isNotEmpty()) {
//                                Log.i(TAG,
//                                    "Event| ActiveSpeakersChanged ${event.speakers[0].name}")
//                            }
                        is RoomEvent.Reconnecting -> onReconnecting()
                        is RoomEvent.Reconnected -> onReconnected()
                        is RoomEvent.ParticipantConnected -> onParticipantConnected(event)
                        is RoomEvent.ParticipantDisconnected -> onParticipantDisconnected(event)
                        else -> {}
                    }
                }
            }

            // Connect to server.
            room.connect(
                wsURL,
                token,
            )


            // Publish audio/video to the room
            val localParticipant = room.localParticipant
            localParticipant.setMicrophoneEnabled(true)
            localParticipant.setCameraEnabled(false)
        }
    }

    @UsedByGodot
    fun muteParticipant(username: String) {

        room.remoteParticipants[Participant.Identity(username)]?.trackPublications?.forEach {
            it.value.track?.enabled ?: false
        }
    }


    @UsedByGodot
    fun unmuteParticipant(username: String) {

        room.remoteParticipants[Participant.Identity(username)]?.trackPublications?.forEach {
            it.value.track?.enabled ?: true
        }
    }

    @UsedByGodot
    fun activeSpeakers(): Array<String?> {
        val ret: MutableList<String> = ArrayList()
        for (speaker in room.activeSpeakers) {
            ret.add(speaker.identity.toString())
        }

        return (ret as List<String>).toTypedArray()
    }

    private fun onParticipantConnected(event: RoomEvent.ParticipantConnected) {
        emitSignal("participant_connected", event.participant.identity.toString())
    }

    private fun onParticipantDisconnected(event: RoomEvent.ParticipantDisconnected) {
        emitSignal("participant_disconnected", event.participant.identity.toString())
    }

    private fun onReconnecting() {
        emitSignal("event", "reconnecting")
    }

    private fun onReconnected() {
        emitSignal("event", "reconnected")
    }

    private fun onConnected() {
        emitSignal("event", "connected")
    }

    private fun onDisconnected() {
        emitSignal("event", "disconnected")
    }

    private fun onConnectionFailed() {
        emitSignal("event", "connection_error")
    }

    private fun onTrackSubscribed(event: RoomEvent.TrackSubscribed) {
        emitSignal("event", "TrackSubscribed")

        val track = event.track
        if (track is VideoTrack) {
            attachVideo(track)
        }
    }

    private fun attachVideo(videoTrack: VideoTrack) {
//        videoTrack.addRenderer(findViewById<SurfaceViewRenderer>(R.id.renderer))
//        findViewById<View>(R.id.progress).visibility = View.GONE
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry


    fun onStart() {
        runOnUiThread{
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
    }

    fun onResume() {
        runOnUiThread {
            lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        }
    }

    fun onPause() {
        runOnUiThread {
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
    }

    fun onStop() {
        runOnUiThread {
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }
    }

    fun onDestroy() {
        runOnUiThread {
            lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        }
    }


    override fun onMainResume() {
        super.onMainResume()
        onResume()
    }

    override fun onGodotMainLoopStarted() {
        super.onGodotMainLoopStarted()
        onStart()
    }

    override fun onMainDestroy() {
        super.onMainDestroy()
        onDestroy()
    }

    override fun onMainPause() {
        super.onMainPause()
        onPause()
    }

}
