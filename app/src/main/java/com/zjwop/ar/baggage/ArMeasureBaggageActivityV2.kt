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
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.PlaneRenderer
import com.google.ar.sceneform.rendering.Texture
import com.google.ar.sceneform.ux.BaseArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.zjwop.ar.R
import kotlin.math.max
import kotlin.math.min

/**
 * 确认地面模式
 *
 * 手动确认地面
 * 自动上表面检测使用屏幕中心射线模式
 */
class ArMeasureBaggageActivityV2 : AppCompatActivity(), Scene.OnUpdateListener, BaseArFragment.OnTapArPlaneListener{

    companion object {
        private const val TAG = "ArMeasureBaggageActivity"
    }

    private lateinit var arFragment: ArConfigFragment
    private lateinit var arSceneView: ArSceneView
    private lateinit var userHitView: TextView
    private lateinit var resultView: TextView
    private lateinit var modeSwitch: TextView
    private lateinit var scanBtn: TextView

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
    private var isBaggageRendered = false

    private val baggageResultInfo = BaggageResultInfo()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkIsSupportedDeviceOrFinish(this)) {
            return
        }
        setContentView(R.layout.activity_measure_baggage)
        initView()
        initPointCloud()
        initReticleNode()
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
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    isScanning = false
                    scanBtn.setBackgroundColor(android.graphics.Color.BLACK)
                    if (isBaggageTopDetected) {
                        hideBaggageSideAndTopPlaneBounding()
                        renderBaggage()
                    }
                }
                else -> {}
            }
            true
        }
    }

    private fun initPointCloud() {
        PointCloudNode(this).apply {
            setParent(arSceneView.scene)
        }
    }

    private fun initReticleNode() {
        ReticleNode(this).apply {
            setParent(arSceneView.scene)
        }
    }

    private val invalidPlaneSet = hashSetOf<Plane>()
    override fun onUpdate(frameTime: FrameTime) {
        val frame = arSceneView.arFrame
        if (frame == null || frame.camera.trackingState != TrackingState.TRACKING || !isGroundDetected) {
            return
        }

        updatePlaneInfo()

        if (isAutoMode && isScanning) {
            if (isBaggageTopDetected) {
                renderMeasureResult()
            } else if (isBaggageSideDetected) {
                frame.hitTest(arSceneView.width * 0.5F, arSceneView.height * 0.5F).forEach {
                    val trackable = it.trackable
                    if (trackable is Plane && trackable.isPoseInPolygon(it.hitPose)) when {
                        trackable.trackingState != TrackingState.TRACKING -> Unit
                        groundPlane == trackable -> Unit
                        groundPlane == trackable.subsumedBy -> Unit
                        invalidPlaneSet.contains(trackable) -> Unit
                        !autoDetectBaggageTopPlane(trackable) -> invalidPlaneSet.add(trackable)
                    }
                }
            } else {
                val trackables = frame.getUpdatedTrackables(Plane::class.java)
                val iterator = trackables.iterator()
                while (iterator.hasNext()) {
                    val plane = iterator.next()
                    if (plane.trackingState != TrackingState.TRACKING) {
                        continue
                    }
                    if(autoDetectBaggageSidePlane(plane)) {
                        break
                    }
                }
            }
        } else {
            if (isBaggageTopDetected) {
                renderMeasureResult()
                if (!isBaggageRendered) {
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
        if (checkTopPlaneValid(plane)) {
            isBaggageTopDetected = true
            baggageTopPlane = plane
            val anchor = arSceneView.session?.createAnchor(plane.centerPose)
            baggageTopPlaneNode = AnchorNode(anchor).apply {
                setParent(arSceneView.scene)
            }
            setPlaneRendererEnabled(false)

        } else {
            Toast.makeText(this, "所选择平面不符合规则，请重新选择！", Toast.LENGTH_SHORT).show()
        }
    }


    private fun autoDetectBaggageTopPlane(plane: Plane): Boolean {
        val result = checkTopPlaneValid(plane)
        if (result) {
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
        return result
    }

    private fun autoDetectBaggageSidePlane(plane: Plane): Boolean {
        val result = checkSidePlaneValid(plane)
        if (result) {
            isBaggageSideDetected = true
            baggageSidePlane = plane
            val anchor = arSceneView.session?.createAnchor(plane.centerPose)
            baggageSidePlaneNode = AnchorNode(anchor).apply {
                setParent(arSceneView.scene)
            }
            renderBaggageSidePlaneBounding()
        }
        return result
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

    private fun checkTopPlaneValid(plane: Plane): Boolean {
        if (plane.type == Plane.Type.VERTICAL || plane.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING) {
            return false
        }
        return groundPlaneNode?.run {
            val anchorNode = AnchorNode(plane.createAnchor(plane.centerPose))
            val groundY = worldPosition.y
            val topPlaneY = anchorNode.worldPosition.y
            anchorNode.anchor?.detach()
            return@run min(plane.extentX, plane.extentZ) <= MAX_BAGGAGE_WIDTH
                    && max(plane.extentX, plane.extentZ) <= MAX_BAGGAGE_LENGTH
                    && topPlaneY - groundY >= MIN_BAGGAGE_HEIGHT
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
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept {
                    TransformableNode(arFragment.transformationSystem).apply{
                        renderable = it
                        setParent(groundPlaneNode)
                        select()
                    }
                }
                .exceptionally { throwable ->
                    val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

    }

    private fun renderBaggage() {
        isBaggageRendered = true
        baggageTopPlaneNode?.let {
            BaggageCubeNode(this, baggageResultInfo).apply {
                setParent(it)
            }
        }
    }


    private var sidePlaneBoundingNode: PlaneBoundingNode? = null
    private fun renderBaggageSidePlaneBounding() {
        sidePlaneBoundingNode = PlaneBoundingNode(this, baggageSidePlane).apply {
            setParent(baggageSidePlaneNode)
        }
    }

    private var topPlaneBoundingNode: PlaneBoundingNode? = null
    private fun renderBaggageTopPlaneBounding() {
        topPlaneBoundingNode = PlaneBoundingNode(this, baggageTopPlane).apply {
            setParent(baggageTopPlaneNode)
        }
    }
    private fun hideBaggageSideAndTopPlaneBounding() {
        sidePlaneBoundingNode?.isEnabled = false
        topPlaneBoundingNode?.isEnabled = false
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
