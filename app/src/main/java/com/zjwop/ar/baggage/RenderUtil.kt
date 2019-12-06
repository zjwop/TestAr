package com.zjwop.ar.baggage

import android.content.Context
import android.graphics.Color
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ShapeFactory

/**
 * Created by zhaojianwu on 2019-12-03.
 */
 fun addLineBetweenPoints(context: Context, arSceneView: ArSceneView, from: Vector3, to: Vector3): Node {
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
    val colorOrange = com.google.ar.sceneform.rendering.Color(Color.parseColor("#ffa71c"))

    // 1. make a material by the color
    MaterialFactory.makeOpaqueWithColor(context, colorOrange)
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