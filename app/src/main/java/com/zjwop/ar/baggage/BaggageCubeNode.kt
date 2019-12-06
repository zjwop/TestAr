package com.zjwop.ar.baggage

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.rendering.ViewRenderable
import com.zjwop.ar.R

/**
 * Created by zhaojianwu on 2019-12-03.
 */
class BaggageCubeNode(private val context: Context,
                      private val baggageResultInfo: BaggageResultInfo) : Node() {


    override fun onActivate() {
        renderCube()
        renderCubeBounding()
        renderCubeText()
    }

    private fun renderCube() {
        MaterialFactory.makeTransparentWithColor(context, com.google.ar.sceneform.rendering.Color(Color.parseColor("#05333333")))
                .thenAccept {
                    Node().apply {
                        val size = Vector3(baggageResultInfo.x, baggageResultInfo.y, baggageResultInfo.z)
                        val center = Vector3(0f, -baggageResultInfo.y / 2, 0f)
                        val cube = ShapeFactory.makeCube(size, center, it)
                        renderable = cube
                        setParent(this@BaggageCubeNode)
                    }
                }
    }


    private fun renderCubeBounding() {
        val point1 = Vector3(baggageResultInfo.x / 2, 0f, baggageResultInfo.z / 2)
        val point2 = Vector3(baggageResultInfo.x / 2, 0f, -baggageResultInfo.z / 2)
        val point3 = Vector3(-baggageResultInfo.x / 2, 0f, baggageResultInfo.z / 2)
        val point4 = Vector3(-baggageResultInfo.x / 2, 0f, -baggageResultInfo.z / 2)
        val point5 = Vector3(baggageResultInfo.x / 2, -baggageResultInfo.y, baggageResultInfo.z / 2)
        val point6 = Vector3(baggageResultInfo.x / 2, -baggageResultInfo.y, -baggageResultInfo.z / 2)
        val point7 = Vector3(-baggageResultInfo.x / 2, -baggageResultInfo.y, baggageResultInfo.z / 2)
        val point8 = Vector3(-baggageResultInfo.x / 2, -baggageResultInfo.y, -baggageResultInfo.z / 2)

        renderPointsLines(listOf(point1, point2, point4, point3))
        renderPointsLines(listOf(point5, point6, point8, point7))
        renderPointsLines(listOf(point1, point5))
        renderPointsLines(listOf(point2, point6))
        renderPointsLines(listOf(point3, point7))
        renderPointsLines(listOf(point4, point8))
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
                    node.setParent(this@BaggageCubeNode)

                    val difference = Vector3.subtract(to, from)
                    val directionFromTopToBottom = difference.normalized()
                    val rotationFromAToB = Quaternion.lookRotation(directionFromTopToBottom, Vector3.up())
                    node.localPosition = to
                    node.localRotation = Quaternion.multiply(rotationFromAToB,
                            Quaternion.axisAngle(Vector3(1.0f, 0.0f, 0.0f), 90f))
                }
    }

    private fun renderCubeText() {
        ViewRenderable.builder()
                .setView(context, R.layout.tag_layout)
                .build()
                .thenAccept {
                    //需要旋转
                    Node().apply {
                        renderable = it
                        setParent(this@BaggageCubeNode)
                        localPosition = Vector3(-baggageResultInfo.x / 2, 0f, 0f)
                        localRotation = Quaternion(Vector3.up(), 90f)
                    }
                    it.view.findViewById<TextView>(R.id.tag_tv).apply {
                        //X tag展示Z axis的长度
                        text = baggageResultInfo.getZCm()
                    }
                }
                .exceptionally { throwable ->
                    val toast = Toast.makeText(context, "Unable to load tag renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        ViewRenderable.builder()
                .setView(context, R.layout.tag_layout)
                .build()
                .thenAccept {
                    Node().apply {
                        renderable = it
                        setParent(this@BaggageCubeNode)
                        localPosition = Vector3(0f, 0f, -baggageResultInfo.z / 2)
                    }
                    it.view.findViewById<TextView>(R.id.tag_tv).apply {
                        //Z tag展示X axis的长度
                        text = baggageResultInfo.getXCm()
                    }
                }
                .exceptionally { throwable ->
                    val toast = Toast.makeText(context, "Unable to load tag renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

        ViewRenderable.builder()
                .setView(context, R.layout.tag_layout)
                .build()
                .thenAccept {
                    Node().apply {
                        renderable = it
                        setParent(this@BaggageCubeNode)
                        localPosition = Vector3(-baggageResultInfo.x / 2, -baggageResultInfo.y/2, -baggageResultInfo.z / 2)
                        localRotation = Quaternion(Vector3.back(), 90f)
                    }
                    it.view.findViewById<TextView>(R.id.tag_tv).apply {
                        text = baggageResultInfo.getYCm()
                    }
                }
                .exceptionally { throwable ->
                    val toast = Toast.makeText(context, "Unable to load tag renderable", Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.CENTER, 0, 0)
                    toast.show()
                    null
                }

    }

}