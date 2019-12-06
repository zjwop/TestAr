package com.zjwop.ar.baggage

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import java.util.*
import com.zjwop.ar.R

class TestSceneformActivity : AppCompatActivity(), Scene.OnUpdateListener {

    private var arFragment: ArFragment? = null
    private var arSceneView: ArSceneView? = null
    private var andyRenderable: ModelRenderable? = null

    private var plane: Plane? = null

    private var txIncrement = 0f
    private val txSet = HashSet<Float>()
    private var tyIncrement = 0f
    private val tySet = HashSet<Float>()
    private var tzIncrement = 0f
    private val tzSet = HashSet<Float>()
    private var qxIncrement = 0f
    private val qxSet = HashSet<Float>()
    private var qyIncrement = 0f
    private val qySet = HashSet<Float>()
    private var qzIncrement = 0f
    private val qzSet = HashSet<Float>()
    private var qwIncrement = 0f
    private val qwSet = HashSet<Float>()

    private var txPlus: Button? = null
    private var txMinus: Button? = null
    private var tyPlus: Button? = null
    private var tyMinus: Button? = null
    private var tzPlus: Button? = null
    private var tzMinus: Button? = null
    private var qxPlus: Button? = null
    private var qxMinus: Button? = null
    private var qyPlus: Button? = null
    private var qyMinus: Button? = null
    private var qzPlus: Button? = null
    private var qzMinus: Button? = null
    private var qwPlus: Button? = null
    private var qwMinus: Button? = null

    override// CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }

        setContentView(R.layout.activity_ux)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        arSceneView = arFragment!!.arSceneView
        arSceneView!!.scene.addOnUpdateListener(this)
        txPlus = findViewById(R.id.tx_plus_btn)
        txMinus = findViewById(R.id.tx_minus_btn)
        tyPlus = findViewById(R.id.ty_plus_btn)
        tyMinus = findViewById(R.id.ty_minus_btn)
        tzPlus = findViewById(R.id.tz_plus_btn)
        tzMinus = findViewById(R.id.tz_minus_btn)
        qxPlus = findViewById(R.id.qx_plus_btn)
        qxMinus = findViewById(R.id.qx_minus_btn)
        qyPlus = findViewById(R.id.qy_plus_btn)
        qyMinus = findViewById(R.id.qy_minus_btn)
        qzPlus = findViewById(R.id.qz_plus_btn)
        qzMinus = findViewById(R.id.qz_minus_btn)
        qwPlus = findViewById(R.id.qw_plus_btn)
        qwMinus = findViewById(R.id.qw_minus_btn)

        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept { renderable -> andyRenderable = renderable }
                .exceptionally { throwable ->
                    val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }


        arFragment?.setOnTapArPlaneListener { _, plane, _ ->

            andyRenderable?.let {
                this.plane = plane
                Log.d("zhaojw_onTapArPlane", plane.hashCode().toString())
                showAndy(plane.createAnchor(plane.centerPose))

                val anchor = plane.createAnchor(plane.centerPose)
                val anchorNode = AnchorNode(anchor)
                anchorNode.setParent(arSceneView!!.scene)
                val solarSystem = createSolarSystem(plane)
                anchorNode.addChild(solarSystem)
            }

        }

        txPlus?.setOnClickListener {
            txIncrement++
            onTxChange()
        }

        txMinus?.setOnClickListener {
            txIncrement--
            onTxChange()
        }

        tyPlus?.setOnClickListener {
            tyIncrement++
            onTyChange()
        }

        tyMinus!!.setOnClickListener {
            tyIncrement--
            onTyChange()
        }

        tzPlus!!.setOnClickListener {
            tzIncrement++
            onTzChange()
        }

        tzMinus!!.setOnClickListener {
            tzIncrement--
            onTzChange()
        }

        tzPlus!!.setOnClickListener {
            tzIncrement++
            onTzChange()
        }

        qxPlus!!.setOnClickListener {
            qxIncrement++
            onQxChange()
        }

        qxMinus!!.setOnClickListener {
            qxIncrement--
            onQxChange()
        }

        qyPlus!!.setOnClickListener {
            qyIncrement++
            onQyChange()
        }

        qyMinus!!.setOnClickListener {
            qyIncrement--
            onQyChange()
        }

        qzPlus!!.setOnClickListener {
            qzIncrement++
            onQzChange()
        }

        qzMinus!!.setOnClickListener {
            qzIncrement--
            onQzChange()
        }

        qwPlus!!.setOnClickListener {
            qwIncrement++
            onQwChange()
        }

        qwMinus!!.setOnClickListener {
            qwIncrement--
            onQwChange()
        }
    }


    private fun createSolarSystem(plane: Plane): Node {
        val extentX = plane.extentX
        val extentZ = plane.extentZ
        val base = Node()

        val sun1 = Node()
        sun1.setParent(base)
        sun1.renderable = andyRenderable
        sun1.localPosition = Vector3(extentX / 2, 0f, extentZ / 2)
        sun1.localScale = Vector3(0.5f, 0.5f, 0.5f)

        val sun2 = Node()
        sun2.setParent(base)
        sun2.renderable = andyRenderable
        sun2.localPosition = Vector3(extentX / 2, 0f, -extentZ / 2)
        sun2.localScale = Vector3(0.5f, 0.5f, 0.5f)

        val sun3 = Node()
        sun3.setParent(base)
        sun3.renderable = andyRenderable
        sun3.localPosition = Vector3(-extentX / 2, 0f, extentZ / 2)
        sun3.localScale = Vector3(0.5f, 0.5f, 0.5f)

        val sun4 = Node()
        sun4.setParent(base)
        sun4.renderable = andyRenderable
        sun4.localPosition = Vector3(-extentX / 2, 0f, -extentZ / 2)
        sun4.localScale = Vector3(0.5f, 0.5f, 0.5f)


        for (i in 0..4) {
            val sun = Node()
            sun.setParent(base)
            sun.renderable = andyRenderable
            sun.localPosition = Vector3(0.1.toFloat() * i, 0f, 0f)
            sun.localScale = Vector3(0.5f, 0.5f, 0.5f)

        }

        //        FloatBuffer floatBuffer = plane.getPolygon();
        //            for(int i = 0; i < floatBuffer.array().length / 2; i ++) {
        //                float x = floatBuffer.get(i);
        //                float z = floatBuffer.get(i + 1);
        //                Node sun = new Node();
        //                sun.setParent(base);
        //                sun.setRenderable(andyRenderable);
        //                sun.setLocalPosition(new Vector3(x, 0f, z));
        //                sun.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));
        //            }

        return base
    }

    private fun onTxChange() {
        if (!txSet.contains(txIncrement)) {
            txSet.add(txIncrement)
            if (plane == null) {
                return
            }
            val centerPose = plane!!.centerPose
            val targetPose = Pose.IDENTITY.compose(Pose.makeTranslation(0.1f * txIncrement, 0f, 0f)).compose(centerPose)
            showAndy(plane!!.createAnchor(targetPose))
        }
    }

    private fun onTyChange() {
        if (!tySet.contains(tyIncrement)) {
            tySet.add(tyIncrement)
            if (plane == null) {
                return
            }
            val centerPose = plane!!.centerPose
            val targetPose = Pose.IDENTITY.compose(Pose.makeTranslation(0f, 0.1f * tyIncrement, 0f)).compose(centerPose)
            showAndy(plane!!.createAnchor(targetPose))
        }
    }

    private fun onTzChange() {
        if (!tzSet.contains(tzIncrement)) {
            tzSet.add(tzIncrement)
            if (plane == null) {
                return
            }
            val centerPose = plane!!.centerPose
            val targetPose = Pose.IDENTITY.compose(Pose.makeTranslation(0f, 0f, 0.1f * tzIncrement)).compose(centerPose)
            showAndy(plane!!.createAnchor(targetPose))
        }
    }

    private fun onQxChange() {
        if (!qxSet.contains(qxIncrement)) {
            qxSet.add(qxIncrement)
            if (plane == null) {
                return
            }
            val centerPose = plane!!.centerPose
            val quaternion = floatArrayOf(centerPose.qx() + 0.1f * qxIncrement, centerPose.qy(), centerPose.qz(), centerPose.qw())
            val targetPost = Pose(centerPose.translation, quaternion)
            showAndy(plane!!.createAnchor(targetPost))
        }
    }

    private fun onQyChange() {
        if (!qySet.contains(qyIncrement)) {
            qySet.add(qyIncrement)
            if (plane == null) {
                return
            }
            val centerPose = plane!!.centerPose
            val quaternion = floatArrayOf(centerPose.qx(), centerPose.qy() + 0.1f * qyIncrement, centerPose.qz(), centerPose.qw())
            val targetPost = Pose(centerPose.translation, quaternion)
            showAndy(plane!!.createAnchor(targetPost))
        }
    }

    private fun onQzChange() {
        if (!qzSet.contains(qzIncrement)) {
            qzSet.add(qzIncrement)
            if (plane == null) {
                return
            }
            val centerPose = plane!!.centerPose
            val quaternion = floatArrayOf(centerPose.qx(), centerPose.qy(), centerPose.qz() + 0.1f * qzIncrement, centerPose.qw())
            val targetPost = Pose(centerPose.translation, quaternion)
            showAndy(plane!!.createAnchor(targetPost))
        }
    }

    private fun onQwChange() {
        if (!qwSet.contains(qwIncrement)) {
            qwSet.add(qwIncrement)
            if (plane == null) {
                return
            }
            val centerPose = plane!!.centerPose
            val quaternion = floatArrayOf(centerPose.qx(), centerPose.qy(), centerPose.qz(), centerPose.qw() + 0.1f * qwIncrement)
            val targetPost = Pose(centerPose.translation, quaternion)
            showAndy(plane!!.createAnchor(targetPost))
        }
    }

    private fun showAndy(anchor: Anchor) {

        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment!!.arSceneView.scene)
        // Create the transformable andy and add it to the anchor.
        val andy = TransformableNode(arFragment!!.transformationSystem)
        andy.setParent(anchorNode)
        andy.renderable = andyRenderable
        andy.select()

    }


    override fun onUpdate(frameTime: FrameTime) {
        val frame = arSceneView!!.arFrame ?: return
        val trackables = frame.getUpdatedTrackables(Plane::class.java)
        Log.d("zhaojw_onUpdate", trackables.size.toString())
        for (plane in trackables) {
            if (plane.trackingState == TrackingState.TRACKING) {
                Log.d("zhaojw_onUpdate_tracking", plane.hashCode().toString())
            } else if (plane.trackingState == TrackingState.PAUSED) {
                Log.d("zhaojw_onUpdate_paused", plane.hashCode().toString())
            } else if (plane.trackingState == TrackingState.STOPPED) {
                Log.d("zhaojw_onUpdate_stopped", plane.hashCode().toString())
            }
        }
    }

    companion object {
        private val TAG = TestSceneformActivity::class.java.simpleName
        private val MIN_OPENGL_VERSION = 3.0

        /**
         * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
         * on this device.
         *
         *
         * Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
         *
         *
         * Finishes the activity if Sceneform can not run
         */
        fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
            if (Build.VERSION.SDK_INT < VERSION_CODES.N) {
                Log.e(TAG, "Sceneform requires Android N or later")
                Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show()
                activity.finish()
                return false
            }
            val openGlVersionString = (activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                    .deviceConfigurationInfo
                    .glEsVersion
            if (java.lang.Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
                Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later")
                Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                        .show()
                activity.finish()
                return false
            }
            return true
        }
    }
}
