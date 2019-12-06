package com.zjwop.ar.baggage

import android.util.Log
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.exceptions.*
import com.google.ar.sceneform.ux.BaseArFragment

/**
 * Created by zhaojianwu on 2019-11-21.
 */
class ArConfigFragment : BaseArFragment() {

    companion object {
        private const val TAG = "ArConfigFragment"
    }

    override fun isArRequired(): Boolean {
        return true
    }

    override fun getAdditionalPermissions(): Array<String> {
        return emptyArray()
    }

    override fun handleSessionException(sessionException: UnavailableException) {

        val message: String = when(sessionException) {
            is UnavailableArcoreNotInstalledException -> "Please install ARCore"
            is UnavailableApkTooOldException -> "Please install ARCore"
            is UnavailableSdkTooOldException -> "Please update this app"
            is UnavailableDeviceNotCompatibleException -> "This device does not support AR"
            else -> "Failed to create AR session"
        }
        Log.e(TAG, "Error: $message", sessionException)
        Toast.makeText(requireActivity(), message, Toast.LENGTH_LONG).show()
    }

    override fun getSessionConfiguration(session: Session): Config {
        return Config(session).apply {
            planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
        }
    }


    override fun getSessionFeatures(): Set<Session.Feature> {
        return emptySet()
    }
}
