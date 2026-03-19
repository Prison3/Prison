package com.android.prisona

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import com.android.prison.base.PActivityThread
import com.android.prison.base.AppCallback
import com.android.prison.core.PrisonCore
import com.android.prison.base.Settings
import java.io.File
import com.android.prison.utils.Logger

class FoxRiver : Application() {

    companion object {
        private const val TAG = "FoxRiver"
        private const val SP_NAME_REMARK = "UserRemark"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private lateinit var mContext: Context

        fun getContext(): Context = mContext

        fun getRemarkSharedPreferences(): SharedPreferences {
            return mContext.getSharedPreferences(SP_NAME_REMARK, MODE_PRIVATE)
        }

        private val prefsHolder by lazy {
            object {
                var mDaemonEnable by AppSharedPreferenceDelegate(getContext(), false)
                var mShowShortcutPermissionDialog by AppSharedPreferenceDelegate(getContext(), true)
            }
        }

        private inline fun <T> safeGet(default: T, operation: String, block: () -> T): T {
            return runCatching(block).getOrElse { e ->
                Logger.e(TAG, "Error getting $operation: ${e.message}", e)
                default
            }
        }

        private inline fun safeSet(operation: String, block: () -> Unit) {
            runCatching(block).onFailure { e ->
                Logger.e(TAG, "Error setting $operation: ${e.message}", e)
            }
        }

        fun daemonEnable(): Boolean = safeGet(false, "daemonEnable") { prefsHolder.mDaemonEnable }

        fun setDaemonEnable(enable: Boolean) = safeSet("daemonEnable") {
            prefsHolder.mDaemonEnable = enable
        }

        fun showShortcutPermissionDialog(): Boolean = safeGet(true, "showShortcutPermissionDialog") {
            prefsHolder.mShowShortcutPermissionDialog
        }

        fun setShowShortcutPermissionDialog(show: Boolean) = safeSet("showShortcutPermissionDialog") {
            prefsHolder.mShowShortcutPermissionDialog = show
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        base ?: return
        mContext = base
        PrisonCore.get().startUp(this, createSettings(base), createAppCallback())
    }

    override fun onCreate() {
        super.onCreate()
        safeExecute("onCreate") {
            doOnCreate()
        }
    }

    private fun createAppCallback() = object : AppCallback {
        override fun beforeMainLaunchApk(packageName: String?, userid: Int) {
            // Not implemented
        }

        override fun onStoragePermissionNeeded(
            packageName: String?,
            userId: Int
        ): Boolean {
            try {
                Logger.w(TAG, "Storage permission needed for launching: $packageName")
                // Broadcast to request storage permission
                // The main activity should listen for this and show permission dialog
                val intent = android.content.Intent("top.niunaijun.blackboxa.REQUEST_STORAGE_PERMISSION")
                intent.putExtra("package_name", packageName)
                intent.putExtra("user_id", userId)
                intent.setPackage(FoxRiver.getContext().packageName)
                FoxRiver.getContext().sendBroadcast(intent)
                // Return false to NOT block the launch - the app will launch anyway
                // but the user will be notified to grant permission
                // Change to 'true' if you want to block launch until permission is granted
                return false
            } catch (e: Exception) {
                Logger.e(TAG, "Error in onStoragePermissionNeeded: ${e.message}")
                return false
            }
        }

        override fun beforeMainActivityOnCreate(activity: Activity?) {
            // Not implemented
        }

        override fun afterMainActivityOnCreate(activity: Activity?) {
            // Not implemented
        }

        override fun beforeCreateApplication(packageName: String?, processName: String?, context: Context?, userId: Int) {
            safeExecute("beforeCreateApplication") {
                val currentUserId = PActivityThread.getUserId()
                Logger.d(TAG, "beforeCreateApplication: pkg=$packageName, process=$processName, userId=$currentUserId")
            }
        }

        override fun beforeApplicationOnCreate(packageName: String?, processName: String?, application: Application?, userId: Int) {
            safeExecute("beforeApplicationOnCreate") {
                Logger.d(TAG, "beforeApplicationOnCreate: pkg=$packageName, process=$processName")
            }
        }

        override fun afterApplicationOnCreate(packageName: String?, processName: String?, application: Application?, userId: Int) {
            safeExecute("afterApplicationOnCreate") {
                Logger.d(TAG, "afterApplicationOnCreate: pkg=$packageName, process=$processName")
                RockerManager.init(application, userId)
            }
        }

        override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
            // Not implemented
        }

        override fun onActivityStarted(activity: Activity?) {
            // Not implemented
        }

        override fun onActivityResumed(activity: Activity?) {
            // Not implemented
        }

        override fun onActivityPaused(activity: Activity?) {
            // Not implemented
        }

        override fun onActivityStopped(activity: Activity?) {
            // Not implemented
        }

        override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {
            // Not implemented
        }

        override fun onActivityDestroyed(activity: Activity?) {
            // Not implemented
        }

        override fun onBeforeCreateApplication(packageName: String?, processName: String?, context: Context?, userId: Int) {
            // Not implemented
        }
    }

    private fun createSettings(context: Context) = object : Settings() {
        override fun getHostPackageName(): String {
            return runCatching { context.packageName }.getOrDefault("unknown")
        }

        override fun isEnableDaemonService(): Boolean = FoxRiver.daemonEnable()

        override fun requestInstallPackage(file: File?, userId: Int): Boolean {
            if (file == null) {
                Logger.w(TAG, "requestInstallPackage: file is null")
                return false
            }
            return runCatching {
                context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                false
            }.onFailure { e ->
                Logger.e(TAG, "requestInstallPackage failed: ${e.message}", e)
            }.getOrDefault(false)
        }
    }

    private fun doOnCreate() {
        try {
            PrisonCore.get().initializeServices()
            registerServiceAvailableCallback()
            Logger.d(TAG, "Prison initialization completed successfully")
        } catch (e: Exception) {
            Logger.e(TAG, "Unexpected error: ${e.message} (${e.javaClass.simpleName})", e)
        } finally {
            registerServiceAvailableCallback()
        }
    }

    private fun registerServiceAvailableCallback() {
        safeExecute("registerServiceAvailableCallback") {
            PrisonCore.get().addServiceAvailableCallback {
                Logger.d(TAG, "Services became available")
            }
        }
    }

    private inline fun safeExecute(operation: String, block: () -> Unit) {
        runCatching(block).onFailure { e ->
            Logger.e(TAG, "Error in $operation: ${e.message}", e)
        }
    }
}
