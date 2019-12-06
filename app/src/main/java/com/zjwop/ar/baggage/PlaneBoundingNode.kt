package com.zjwop.ar.baggage

import android.content.Context
import android.graphics.Color
import com.google.ar.core.Plane
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

/**
 * Created by zhaojianwu on 2019-12-03.
 */
class PlaneBoundingNode(private val context: Context,
                      private val plane: Plane?) : Node() {

    override fun onActivate() {
        renderBoundingLines()
    }

    private fun renderBoundingLines() {

        plane?.let {
            val floatBuffer = it.polygon
            val points = mutableListOf<Vector3>()
            for (i in 0 until floatBuffer.array().size step 2) {
                val x = floatBuffer.get(i)
                val z = floatBuffer.get(i + 1)
                points.add(Vector3(x, 0f, z))
            }
            renderPointsLines(points)
        }
    }


    private fun renderPointsLines(points: List<Vector3>) {
        for (i in points.indices) {
            val from = points[i]
            val to = points[(i + 1) % points.size]
            addLineBetweenPoints(from, to)
        }
    }

    private fun addLineBetweenPoints(from: Vector3, to: Vector3) {

        val lineLength = Vector3.subtract(from, to).length()
        val colorOrange = com.google.ar.sceneform.rendering.Color(Color.parseColor("#ffa71c"))

        MaterialFactory.makeOpaqueWithColor(context, colorOrange)
                .thenAccept { material ->
                    val model = ShapeFactory.makeCylinder(0.0025f, lineLength,
                            Vector3(0f, lineLength / 2, 0f), material)
                    model.isShadowReceiver = false
                    model.isShadowCaster = false

                    val node = Node()
                    node.renderable = model
                    node.setParent(this@PlaneBoundingNode)

                    val difference = Vector3.subtract(to, from)
                    val directionFromTopToBottom = difference.normalized()
                    val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
                    node.localPosition = to
                    node.localRotation = Quaternion.multiply(rotationFromAToB,
                            Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f))
                }
    }

}
