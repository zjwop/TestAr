package com.zjwop.ar.baggage

import android.content.Context
import com.google.ar.core.PointCloud
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
/**
 * Created by zhaojianwu on 2019-12-06.
 */
class PointCloudNode(context: Context) : Node() {

    companion object {
        private val SIZE = Vector3.one().scaled(0.005F)
        private val EXTENT = SIZE.scaled(0.5F)

        private val UP = Vector3.up()
        private val DOWN = Vector3.down()
        private val FRONT = Vector3.forward()
        private val BACK = Vector3.back()
        private val LEFT = Vector3.left()
        private val RIGHT = Vector3.right()

        private val UV_00 = Vertex.UvCoordinate(0.0f, 0.0f)
        private val UV_10 = Vertex.UvCoordinate(1.0f, 0.0f)
        private val UV_01 = Vertex.UvCoordinate(0.0f, 1.0f)
        private val UV_11 = Vertex.UvCoordinate(1.0f, 1.0f)
    }

    private var timestamp: Long = 0
    private var material: Material? = null

    init {
        val r = 53 / 255f
        val g = 174 / 255f
        val b = 256 / 255f
        val color = Color(r, g, b, 1.0f)
        MaterialFactory.makeOpaqueWithColor(context, color).thenAccept { material = it }
    }

    override fun onUpdate(frameTime: FrameTime?)  {

        super.onUpdate(frameTime)
        if (!isEnabled) return
        val ar = scene?.view as? ArSceneView ?: return
        val frame = ar.arFrame ?: return
        frame.acquirePointCloud().use {
            render(it)
        }
    }


    private fun render(pointCloud: PointCloud) {
        timestamp = pointCloud.timestamp.takeIf { it != timestamp } ?: return
        val material = material ?: return
        val definition = makePointCloud(pointCloud, material) ?: return
        when (val render = renderable) {
            null -> ModelRenderable.builder().setSource(definition).build().thenAccept {
                renderable = it.apply {
                    isShadowCaster = false
                    isShadowReceiver = false
                    collisionShape = null
                }
            }
            else -> render.updateFromDefinition(definition).also {
                renderable?.collisionShape = null
            }
        }
    }


    private fun makePointCloud(pointCloud: PointCloud, material: Material): RenderableDefinition? {
        val buffer = pointCloud.points
        val points = buffer.limit() / 4
        if (points == 0) {
            return null
        }

        val vertices = mutableListOf<Vertex>()
        val triangleIndices = mutableListOf<Int>()

        for (i in 0 until points) {
            /* {x, y, z, confidence} */
            val x = buffer[i * 4]
            val y = buffer[i * 4 + 1]
            val z = buffer[i * 4 + 2]
            val center = Vector3(x, y, z)

            val p0 = Vector3(center.x + -EXTENT.x, center.y + -EXTENT.y, center.z + EXTENT.z)
            val p1 = Vector3(center.x + EXTENT.x, center.y + -EXTENT.y, center.z + EXTENT.z)
            val p2 = Vector3(center.x + EXTENT.x, center.y + -EXTENT.y, center.z + -EXTENT.z)
            val p3 = Vector3(center.x + -EXTENT.x, center.y + -EXTENT.y, center.z + -EXTENT.z)
            val p4 = Vector3(center.x + -EXTENT.x, center.y + EXTENT.y, center.z + EXTENT.z)
            val p5 = Vector3(center.x + EXTENT.x, center.y + EXTENT.y, center.z + EXTENT.z)
            val p6 = Vector3(center.x + EXTENT.x, center.y + EXTENT.y, center.z + -EXTENT.z)
            val p7 = Vector3(center.x + -EXTENT.x, center.y + EXTENT.y, center.z + -EXTENT.z)

            vertices.add(vertex(p0, DOWN, UV_01))
            vertices.add(vertex(p1, DOWN, UV_11))
            vertices.add(vertex(p2, DOWN, UV_10))
            vertices.add(vertex(p3, DOWN, UV_00))
            vertices.add(vertex(p7, LEFT, UV_01))
            vertices.add(vertex(p4, LEFT, UV_11))
            vertices.add(vertex(p0, LEFT, UV_10))
            vertices.add(vertex(p3, LEFT, UV_00))
            vertices.add(vertex(p4, FRONT, UV_01))
            vertices.add(vertex(p5, FRONT, UV_11))
            vertices.add(vertex(p1, FRONT, UV_10))
            vertices.add(vertex(p0, FRONT, UV_00))
            vertices.add(vertex(p6, BACK, UV_01))
            vertices.add(vertex(p7, BACK, UV_11))
            vertices.add(vertex(p3, BACK, UV_10))
            vertices.add(vertex(p2, BACK, UV_00))
            vertices.add(vertex(p5, RIGHT, UV_01))
            vertices.add(vertex(p6, RIGHT, UV_11))
            vertices.add(vertex(p2, RIGHT, UV_10))
            vertices.add(vertex(p1, RIGHT, UV_00))
            vertices.add(vertex(p7, UP, UV_01))
            vertices.add(vertex(p6, UP, UV_11))
            vertices.add(vertex(p5, UP, UV_10))
            vertices.add(vertex(p4, UP, UV_00))

            val offset = i * 24
            for (j in 0..5) {
                triangleIndices.add(offset + 3 + 4 * j)
                triangleIndices.add(offset + 1 + 4 * j)
                triangleIndices.add(offset + 0 + 4 * j)
                triangleIndices.add(offset + 3 + 4 * j)
                triangleIndices.add(offset + 2 + 4 * j)
                triangleIndices.add(offset + 1 + 4 * j)
            }
        }

        val submesh = RenderableDefinition.Submesh.builder().setMaterial(material).setTriangleIndices(triangleIndices).build()
        return RenderableDefinition.builder().setVertices(vertices).setSubmeshes(listOf(submesh)).build()
    }

    private fun vertex(position: Vector3, normal: Vector3, uv: Vertex.UvCoordinate): Vertex {
        return Vertex.builder().setPosition(position).setNormal(normal).setUvCoordinate(uv).build()
    }


}