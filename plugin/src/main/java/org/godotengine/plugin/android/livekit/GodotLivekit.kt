// TODO: Update to match your plugin's package name.
package org.godotengine.plugin.android.livekit

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.launch
import org.godotengine.godot.Godot
import org.godotengine.godot.plugin.GodotPlugin
import org.godotengine.godot.plugin.UsedByGodot
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import io.livekit.android.LiveKit

class GodotLivekit(godot: Godot): GodotPlugin(godot), LifecycleOwner {

    var wsURL = "wss://livekit.sizakgames.ir"
    val TAG = "LiveKit"
    lateinit var room: Room
    private var lifecycleRegistry = LifecycleRegistry(this)

    override fun getPluginName() = BuildConfig.GODOT_PLUGIN_NAME

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
                        is RoomEvent.Connected -> Log.i(TAG, "Event| Connected")
                        is RoomEvent.Disconnected -> Log.i(TAG, "Event| DisConnected")
                        is RoomEvent.FailedToConnect -> Log.i(TAG, "Event| FailedToConnect")
                        is RoomEvent.ActiveSpeakersChanged ->
                            if (event.speakers.isNotEmpty()) {
                                Log.i(TAG,
                                    "Event| ActiveSpeakersChanged ${event.speakers[0].name}")
                            }
                        is RoomEvent.ParticipantConnected -> Log.i(TAG,
                            "Event| ParticipantConnected${event.participant}")
                        is RoomEvent.ParticipantDisconnected -> Log.i(TAG,
                            "Event| ParticipantDisconnected$event")
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

    private fun onTrackSubscribed(event: RoomEvent.TrackSubscribed) {
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
