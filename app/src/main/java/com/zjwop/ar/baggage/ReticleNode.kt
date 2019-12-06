package com.zjwop.ar.baggage

import android.content.Context
import com.google.ar.core.Plane
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.zjwop.ar.R

/**
 * Created by zhaojianwu on 2019-12-06.
 */

class ReticleNode(context: Context) : Node() {

    companion object {
        val INVISIBLE_SCALE: Vector3 = Vector3.zero()
        val VISIBLE_SCALE: Vector3 = Vector3.one()
    }

    init {
        ModelRenderable.builder()
                .setSource(context.applicationContext, R.raw.sceneform_footprint)
                .build()
                .thenAccept { renderable = it.apply { collisionShape = null } }
    }

    override fun onUpdate(frameTime: FrameTime?) {
        super.onUpdate(frameTime)
        val ar = scene?.view as? ArSceneView ?: return
        val frame = ar.arFrame ?: return
        val hit = frame.hitTest(ar.width * 0.5F, ar.height * 0.5F).firstOrNull {
            val trackable = it.trackable
            when {
                trackable is Plane && trackable.isPoseInPolygon(it.hitPose) -> true
                else -> false
            }
        }
        when (hit) {
            null -> localScale = INVISIBLE_SCALE
            else -> {
                    hit.hitPose?.let {
                    worldPosition = Vector3(it.tx(), it.ty(), it.tz())
                    worldRotation = Quaternion(it.qx(), it.qy(), it.qz(), it.qw())
                    localScale = VISIBLE_SCALE
                }

            }
        }
    }



}