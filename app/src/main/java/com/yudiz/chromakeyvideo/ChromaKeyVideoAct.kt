package com.yudiz.chromakeyvideo

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment

class ChromaKeyVideoAct : AppCompatActivity() {

    private var arFragment: ArFragment? = null
    private var videoRenderable: ModelRenderable? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.act_chromakey_video)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?

        val texture = ExternalTexture()

        mediaPlayer = MediaPlayer.create(this, R.raw.dance)
        mediaPlayer!!.setSurface(texture.surface)
        mediaPlayer!!.isLooping = true

        ModelRenderable.builder()
                .setSource(this, R.raw.chroma_key_video)
                .build()
                .thenAccept { renderable ->
                    videoRenderable = renderable
                    renderable.material.setExternalTexture("videoTexture", texture)
                    renderable.material.setFloat4("keyColor", CHROMA_KEY_COLOR)
                }
                .exceptionally { throwable ->
                    Log.e(TAG, "Error loading video renderable")
                    null
                }

        arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
            if (videoRenderable == null) {
                return@setOnTapArPlaneListener
            }

            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor)
            anchorNode.setParent(arFragment!!.arSceneView.scene)

            val videoNode = Node()
            videoNode.setParent(anchorNode)

            val videoWidth = mediaPlayer!!.videoWidth.toFloat()
            val videoHeight = mediaPlayer!!.videoHeight.toFloat()
            videoNode.localScale = Vector3(
                    VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f)

            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer!!.start()

                texture
                        .surfaceTexture
                        .setOnFrameAvailableListener { surfaceTexture: SurfaceTexture ->
                            videoNode.renderable = videoRenderable
                            texture.surfaceTexture.setOnFrameAvailableListener(null)
                        }
            } else {
                videoNode.renderable = videoRenderable
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    companion object {
        private val TAG = "chromakey"
        private val MIN_OPENGL_VERSION = 3.0
        private val CHROMA_KEY_COLOR = Color(0.1843f, 1.0f, 0.098f)
        private val VIDEO_HEIGHT_METERS = 0.85f

        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                Log.e(TAG, "OpenGL requires to be updated")
                activity.finish()
                return false
            }
            return true
        }
    }
}