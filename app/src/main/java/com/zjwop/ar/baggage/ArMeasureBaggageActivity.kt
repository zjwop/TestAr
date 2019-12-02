/*
 * Copyright 2018 Google LLC. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.*
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.zjwop.ar.R

/**
 * This is an example activity that uses the Sceneform UX package to make common AR tasks easier.
 */
class ArMeasureBaggageActivity : AppCompatActivity(), Scene.OnUpdateListener, BaseArFragment.OnTapArPlaneListener{

    companion object {
        private const val TAG = "ArMeasureBaggageActivity"
        private const val MIN_OPENGL_VERSION = 3.0
        //行李箱暂定最小高度为30cm
        private const val MIN_BAGGAGE_HEIGHT = 0.3f
    }

    private lateinit var arFragment: ArConfigFragment
    private lateinit var arSceneView: ArSceneView
    private lateinit var userHitView: TextView
    private lateinit var resultView: TextView
    private lateinit var modeSwitch: TextView
    private lateinit var scanBtn: TextView

    private var andyRenderable: ModelRenderable? = null
    private var xTagRenderable: ViewRenderable? = null
    private var yTagRenderable: ViewRenderable? = null
    private var zTagRenderable: ViewRenderable? = null
    private var cubeMaterial: Material? = null

    private var groundPlane: Plane? = null
    private var groundPlaneNode: AnchorNode? = null

    private var baggageTopPlane: Plane? = null
    private var baggageTopPlaneNode: AnchorNode? = null

    private var baggageSidePlane: Plane? = null
    private var baggageSidePlaneNode: AnchorNode? = null

    private var isGroundDetected = false
    private var isBaggageTopDetected = false
    private var isBaggageSideDetected = false
    private var isAutoMode = false
    private var isScanning = false
    private var isRenderBaggage = false

    private val baggageResultInfo = BaggageResultInfo()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_measure_baggage)
        initView()
        initRenderable()
        setPlaneRendererEnabled(true)
    }

    private fun initView() {
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArConfigFragment
        arFragment.setOnTapArPlaneListener(this)
        arSceneView = arFragment.arSceneView
        arSceneView.scene.addOnUpdateListener(this)

        userHitView = findViewById(R.id.user_hint_tv)
        resultView = findViewById(R.id.result_tv)
        modeSwitch = findViewById(R.id.mode_switch)
        modeSwitch.setOnClickListener {
            isAutoMode = !isAutoMode
            modeSwitch.text = if (isAutoMode) "自动" else "手动"
            scanBtn.visibility = if (isAutoMode) View.VISIBLE else View.GONE
        }
        scanBtn = findViewById(R.id.begin_scan)
        scanBtn.setOnTouchListener { v, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isScanning = true
                    scanBtn.setBackgroundColor(android.graphics.Color.BLUE)
                }
                MotionEvent.ACTION_UP -> {
                    isScanning = false
                    scanBtn.setBackgroundColor(android.graphics.Color.BLACK)
                    if (isBaggageTopDetected) {
                        hideBaggageSideAndTopPlaneBounding()
                        renderBaggage()
                    }
                }
                else -> {
                    isScanning = true
                    scanBtn.setBackgroundColor(android.graphics.Color.BLUE)
                }
            }
            true
        }
    }

    private fun initRenderable() {
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

        ViewRenderable.builder()
                .setView(this, R.layout.tag_layout)
                .build()
                .thenAccept { renderable -> xTagRenderable = renderable}
                .exceptionally { throwable ->
                    val toast = Toast.makeText(this, "Unable to load tag renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        ViewRenderable.builder()
                .setView(this, R.layout.tag_layout)
                .build()
                .thenAccept { renderable -> yTagRenderable = renderable}
                .exceptionally { throwable ->
                    val toast = Toast.makeText(this, "Unable to load tag renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        ViewRenderable.builder()
                .setView(this, R.layout.tag_layout)
                .build()
                .thenAccept { renderable -> zTagRenderable = renderable}
                .exceptionally { throwable ->
                    val toast = Toast.makeText(this, "Unable to load tag renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        MaterialFactory.makeTransparentWithColor(this, Color(android.graphics.Color.parseColor("#05333333")))
                .thenAccept { material ->
                    cubeMaterial = material
                }
    }

    override fun onUpdate(frameTime: FrameTime) {
        val frame = arSceneView.arFrame
        if (frame == null || !isGroundDetected) {
            return
        }

        updatePlaneInfo()

        if (isAutoMode && isScanning) {
            val trackables = frame.getUpdatedTrackables(Plane::class.java)
            val iterator = trackables.iterator()
            while (iterator.hasNext()) {
                val plane = iterator.next()
                if (plane.trackingState != TrackingState.TRACKING) {
                    continue
                }
                if (groundPlane == plane) {
                    continue
                }
                if (groundPlane == plane.subsumedBy) {
                    continue
                }
                if (isBaggageTopDetected) {
                    renderMeasureResult()
                } else if (isBaggageSideDetected) {
                    autoDetectBaggageTopPlane(plane)
                } else {
                    autoDetectBaggageSidePlane(plane)
                }
            }
        } else {
            if (isBaggageTopDetected) {
                renderMeasureResult()
                if (!isRenderBaggage) {
                    renderBaggage()
                }
            }
        }

    }

    private fun updatePlaneInfo() {
        if (isGroundDetected && isBaggageTopDetected) {
            val x = baggageTopPlane!!.extentX
            val y = baggageTopPlaneNode!!.worldPosition.y - groundPlaneNode!!.worldPosition.y
            val z = baggageTopPlane!!.extentZ
            baggageResultInfo.x = x
            baggageResultInfo.y = y
            baggageResultInfo.z = z
        }
    }

    override fun onTapPlane(hitResult: HitResult, plane: Plane, motionEvent: MotionEvent) {
        if (groundPlane != null && baggageTopPlane != null) {
            return
        }
        if (!isGroundDetected) {
            detectGroundPlane(hitResult, plane)
            return
        }
        if (!isBaggageTopDetected && !isAutoMode) {
            manualDetectBaggageTopPlane(plane)
        }
    }


    private fun detectGroundPlane(hitResult: HitResult, plane: Plane) {
        isGroundDetected = true
        groundPlane = plane
        val anchor = arSceneView.session?.createAnchor(hitResult.hitPose)
        groundPlaneNode = AnchorNode(anchor).apply {
            setParent(arSceneView.scene)
        }
        userHitView.text = "开始扫描行李箱"
        renderGroundAndy()
    }


    private fun manualDetectBaggageTopPlane(plane: Plane) {
        if (checkTopPlaneValid(plane, false)) {
            isBaggageTopDetected = true
            baggageTopPlane = plane
            val anchor = arSceneView.session?.createAnchor(plane.centerPose)
            baggageTopPlaneNode = AnchorNode(anchor).apply {
                setParent(arSceneView.scene)
            }
            setPlaneRendererEnabled(false)

        } else {
            Toast.makeText(this, "请调整摄像头以扫描行李箱上表面", Toast.LENGTH_SHORT).show()
        }
    }


    private fun autoDetectBaggageTopPlane(plane: Plane) {
        if (checkTopPlaneValid(plane, true)) {
            isBaggageTopDetected = true
            baggageTopPlane = plane
            val anchor = arSceneView.session?.createAnchor(plane.centerPose)
            baggageTopPlaneNode = AnchorNode(anchor).apply {
                setParent(arSceneView.scene)
            }
            renderBaggageTopPlaneBounding()
            renderMeasureResult()
            setPlaneRendererEnabled(false)
        }
    }

    private fun autoDetectBaggageSidePlane(plane: Plane) {
        if (checkSidePlaneValid(plane)) {
            isBaggageSideDetected = true
            baggageSidePlane = plane
            val anchor = arSceneView.session?.createAnchor(plane.centerPose)
            baggageSidePlaneNode = AnchorNode(anchor).apply {
                setParent(arSceneView.scene)
            }
            renderBaggageSidePlaneBounding()
        }
    }

    private fun checkSidePlaneValid(plane: Plane): Boolean {
        if (plane.type == Plane.Type.HORIZONTAL_UPWARD_FACING || plane.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING) {
            return false
        }

        return groundPlaneNode?.run {
            val anchorNode = AnchorNode(plane.createAnchor(plane.centerPose))
            val groundY = worldPosition.y
            val sidePlaneCenterY = anchorNode.worldPosition.y
            anchorNode.anchor?.detach()
            sidePlaneCenterY - groundY >= MIN_BAGGAGE_HEIGHT / 2
        } ?:false

    }

    private fun checkTopPlaneValid(plane: Plane, isAutoMode: Boolean): Boolean {
        if (plane.type == Plane.Type.VERTICAL) {
            return false
        }
        return groundPlaneNode?.run {
            val anchorNode = AnchorNode(plane.createAnchor(plane.centerPose))
            val groundY = worldPosition.y
            val topPlaneY = anchorNode.worldPosition.y
            anchorNode.anchor?.detach()
            if (isAutoMode) {
                baggageSidePlaneNode?.run {
                    val sidePlaneY = worldPosition.y
                    topPlaneY - groundY >= MIN_BAGGAGE_HEIGHT && topPlaneY - groundY >= sidePlaneY - groundY
                }
            } else {
                topPlaneY - groundY >= MIN_BAGGAGE_HEIGHT
            }
        } ?:false

    }


    private fun renderMeasureResult() {
        userHitView.text = "行李箱扫描完毕"
        resultView.apply {
            visibility = View.VISIBLE
            text = StringBuilder().apply {
                append("长:${baggageResultInfo.getLengthCm()} ")
                append("宽:${baggageResultInfo.getWidthCm()} ")
                append("高:${baggageResultInfo.getHeightCm()} ")
            }
        }
    }


    private fun renderGroundAndy() {
        andyRenderable?.let {
            TransformableNode(arFragment.transformationSystem).apply{
                renderable = it
                setParent(groundPlaneNode)
                select()
            }

        }
    }

    private fun renderBaggage() {
        isRenderBaggage = true
        renderBaggageCube()
        renderBaggageCubeBounding()
        renderBaggageCubeText()
    }

    private fun renderBaggageCube() {
        cubeMaterial?.let {
            Node().apply {
                val size = Vector3(baggageResultInfo.x, baggageResultInfo.y, baggageResultInfo.z)
                val center = Vector3(0f, -baggageResultInfo.y / 2, 0f)
                val cube = ShapeFactory.makeCube(size, center, cubeMaterial)
                renderable = cube
                setParent(baggageTopPlaneNode)
            }
        }
    }

    private fun renderBaggageCubeBounding() {
        baggageTopPlane?.apply{

            val point1 = centerPose.transformPoint(floatArrayOf(baggageResultInfo.x / 2, 0f, baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            val point2 = centerPose.transformPoint(floatArrayOf(baggageResultInfo.x / 2, 0f, -baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            val point3 = centerPose.transformPoint(floatArrayOf(-baggageResultInfo.x / 2, 0f, baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            val point4 = centerPose.transformPoint(floatArrayOf(-baggageResultInfo.x / 2, 0f, -baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            val point5 = centerPose.transformPoint(floatArrayOf(baggageResultInfo.x  / 2, -baggageResultInfo.y, baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            val point6 = centerPose.transformPoint(floatArrayOf(baggageResultInfo.x  / 2, -baggageResultInfo.y, -baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            val point7 = centerPose.transformPoint(floatArrayOf(-baggageResultInfo.x  / 2, -baggageResultInfo.y, baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            val point8 = centerPose.transformPoint(floatArrayOf(-baggageResultInfo.x  / 2, -baggageResultInfo.y, -baggageResultInfo.z / 2)).let {
                Vector3(it[0], it[1], it[2])
            }

            renderPointsLines(listOf(point1, point2, point4, point3))
            renderPointsLines(listOf(point5, point6, point8, point7))
            renderPointsLines(listOf(point1, point5))
            renderPointsLines(listOf(point2, point6))
            renderPointsLines(listOf(point3, point7))
            renderPointsLines(listOf(point4, point8))
        }
    }

    private fun renderBaggageCubeText() {
        //需要旋转
        xTagRenderable?.let {
            Node().apply {
                renderable = it
                setParent(baggageTopPlaneNode)
                localPosition = Vector3(-baggageResultInfo.x / 2, 0f, 0f)
                localRotation = Quaternion(Vector3.up(), 90f)
            }
            it.view.findViewById<TextView>(R.id.tag_tv).apply {
                //X tag展示Z axis的长度
                text = baggageResultInfo.getZCm()
            }
        }

        zTagRenderable?.let {
            Node().apply {
                renderable = it
                setParent(baggageTopPlaneNode)
                localPosition = Vector3(0f, 0f, -baggageResultInfo.z / 2)
            }
            it.view.findViewById<TextView>(R.id.tag_tv).apply {
                //Z tag展示X axis的长度
                text = baggageResultInfo.getXCm()
            }
        }

        yTagRenderable?.let {
            Node().apply {
                renderable = it
                setParent(baggageTopPlaneNode)
                localPosition = Vector3(-baggageResultInfo.x / 2, -baggageResultInfo.y/2, -baggageResultInfo.z / 2)
                localRotation = Quaternion(Vector3.back(), 90f)
            }
            it.view.findViewById<TextView>(R.id.tag_tv).apply {
                text = baggageResultInfo.getYCm()
            }
        }
    }


    private val sidePlaneBoundingNodeList = mutableListOf<Node>()
    private fun renderBaggageSidePlaneBounding() {
        sidePlaneBoundingNodeList.clear()
        baggageSidePlane?.let {
            val floatBuffer = it.polygon
            val points = mutableListOf<Vector3>()
            for (i in 0 until floatBuffer.array().size step 2) {
                val x = floatBuffer.get(i)
                val z = floatBuffer.get(i + 1)
                val pointFloat = it.centerPose.transformPoint(floatArrayOf(x, 0f, z))
                val pointVector = Vector3(pointFloat[0], pointFloat[1], pointFloat[2])
                points.add(pointVector)
            }
            sidePlaneBoundingNodeList.addAll(renderPointsLines(points))
        }
    }

    private val topPlaneBoundingNodeList = mutableListOf<Node>()
    private fun renderBaggageTopPlaneBounding() {
        topPlaneBoundingNodeList.clear()
        baggageTopPlane?.let{
            val floatBuffer = it.polygon
            val points = mutableListOf<Vector3>()
            for (i in 0 until floatBuffer.array().size step 2) {
                val x = floatBuffer.get(i)
                val z = floatBuffer.get(i + 1)
                val pointFloat = it.centerPose.transformPoint(floatArrayOf(x, 0f, z))
                val pointVector = Vector3(pointFloat[0], pointFloat[1], pointFloat[2])
                points.add(pointVector)
            }
            topPlaneBoundingNodeList.addAll(renderPointsLines(points))
        }

    }

    private fun hideBaggageSideAndTopPlaneBounding() {
        sidePlaneBoundingNodeList.forEach {
            it.isEnabled = false
        }
        topPlaneBoundingNodeList.forEach {
            it.isEnabled = false
        }
    }


    private fun renderPointsLines(points: List<Vector3>): List<Node>{
        val nodeList = mutableListOf<Node>()
        for (i in points.indices) {
            val from = points[i]
            val to = points[(i + 1) % points.size]
            var node = addLineBetweenPoints(from, to)
            nodeList.add(node)
        }
        return nodeList
    }

    private fun addLineBetweenPoints(from: Vector3, to: Vector3): Node{
        // prepare an anchor position
        val camQ = arSceneView.scene.camera.worldRotation
        val f1 = floatArrayOf(to.x, to.y, to.z)
        val f2 = floatArrayOf(camQ.x, camQ.y, camQ.z, camQ.w)
        val anchorPose = Pose(f1, f2)

        // make an ARCore Anchor
        val anchor = arSceneView.session?.createAnchor(anchorPose)
        // Node that is automatically positioned in world space based on the ARCore Anchor.
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arSceneView.scene)

        // Compute a line's length
        val lineLength = Vector3.subtract(from, to).length()

        // Prepare a color
        val colorOrange = Color(android.graphics.Color.parseColor("#ffa71c"))

        // 1. make a material by the color
        MaterialFactory.makeOpaqueWithColor(this, colorOrange)
                .thenAccept { material ->
                    // 2. make a model by the material
                    val model = ShapeFactory.makeCylinder(0.0025f, lineLength,
                            Vector3(0f, lineLength / 2, 0f), material)
                    model.isShadowReceiver = false
                    model.isShadowCaster = false

                    // 3. make node
                    val node = Node()
                    node.renderable = model
                    node.setParent(anchorNode)

                    // 4. set rotation
                    val difference = Vector3.subtract(to, from)
                    val directionFromTopToBottom = difference.normalized()
                    val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
                    node.worldRotation = Quaternion.multiply(rotationFromAToB,
                            Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f))
                }
        return anchorNode
    }

    private fun setPlaneRendererEnabled(isEnabled: Boolean) {
        arSceneView.planeRenderer?.let {
            it.isEnabled = isEnabled
            if (isEnabled) {
                val sampler = Texture.Sampler.builder().setMinFilter(Texture.Sampler.MinFilter.LINEAR).setMagFilter(Texture.Sampler.MagFilter.LINEAR).setWrapMode(Texture.Sampler.WrapMode.REPEAT).build()
                val build = Texture.builder().setSource(this, R.drawable.plane_texture).setSampler(sampler).build()
                it.material?.thenAcceptBoth(build) { material, texture ->
                    material.setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture)
                }
            }
        }

    }


    private fun checkIsSupportedDeviceOrFinish(activity: Activity): Boolean {
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
