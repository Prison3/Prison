package com.android.prisona.view.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.android.prison.core.PrisonCore
import com.android.prison.manager.PUserManager
import com.android.prisona.R
import com.android.prisona.FoxRiver
import com.android.prisona.databinding.ActivityMainBinding
import com.android.prisona.util.Resolution
import com.android.prisona.util.inflate
import com.android.prisona.view.apps.AppsFragment
import com.android.prisona.view.base.LoadingActivity
import com.android.prisona.view.list.ListActivity
import com.android.prisona.view.setting.SettingActivity
import com.android.prison.utils.Logger
import androidx.core.net.toUri

class MainActivity : LoadingActivity() {

    private val viewBinding: ActivityMainBinding by inflate()

    private lateinit var mViewPagerAdapter: ViewPagerAdapter

    private val fragmentList = mutableListOf<AppsFragment>()

    private var currentUser = 0

    companion object {
        private const val TAG = "MainActivity"
        
        fun start(context: Context) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            try {
                PrisonCore.get().appCallback.beforeMainActivityOnCreate(this)
            } catch (e: Exception) {
                Logger.e(TAG, "Error in onBeforeMainActivityOnCreate: ${e.message}")
            }

            setContentView(viewBinding.root)
            initToolbar(viewBinding.toolbarLayout.toolbar, R.string.app_name)
            initViewPager()
            initFab()
            initToolbarSubTitle()
            Logger.i(TAG, "checkStoragePermission");
            checkStoragePermission()
            try {
                PrisonCore.get().appCallback.afterMainActivityOnCreate(this)
            } catch (e: Exception) {
                Logger.e(TAG, "Error in onAfterMainActivityOnCreate: ${e.message}")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Critical error in onCreate: ${e.message}")
            // Show error dialog to user
            showErrorDialog("Failed to initialize app: ${e.message}")
        }
    }

    private fun checkStoragePermission() {
        try {
            if (!android.os.Environment.isExternalStorageManager()) {
                Logger.w(TAG, "MANAGE_EXTERNAL_STORAGE permission not granted")
                showStoragePermissionDialog()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error checking storage permission: ${e.message}")
        }
    }

    private fun showStoragePermissionDialog() {
        try {
            MaterialDialog(this).show {
                title(text = "Storage Permission Required")
                message(
                    text =
                        "This app needs 'All Files Access' permission to properly run sandboxed apps. Without this permission, some apps may not work correctly.\n\nPlease grant permission in the next screen."
                )
                positiveButton(text = "Grant Permission") { openAllFilesAccessSettings() }
                negativeButton(text = "Later") { Logger.w(TAG, "User postponed storage permission") }
                cancelable(false)
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error showing storage permission dialog: ${e.message}")
        }
    }

    private fun openAllFilesAccessSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = "package:$packageName".toUri()
            storagePermissionResult.launch(intent)
        } catch (e: Exception) {
            Logger.e(TAG, "Error opening storage settings: ${e.message}")
            // Fallback: open general manage all files settings
            try {
                val intent =
                    Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storagePermissionResult.launch(intent)
            } catch (e2: Exception) {
                Logger.e(TAG, "Error opening fallback storage settings: ${e2.message}")
            }
        }
    }

    private val storagePermissionResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                if (android.os.Environment.isExternalStorageManager()) {
                    Logger.d(TAG, "Storage permission granted!")
                } else {
                    Logger.w(TAG, "Storage permission still not granted")
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error handling storage permission result: ${e.message}")
            }
        }

    private fun showErrorDialog(message: String) {
        try {
            MaterialDialog(this).show {
                title(text = "Error")
                message(text = message)
                positiveButton(text = "OK") {
                    finish()
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error showing error dialog: ${e.message}")
            finish()
        }
    }

    private fun initToolbarSubTitle() {
        try {
            updateUserRemark(0)
            //hack code
            viewBinding.toolbarLayout.toolbar.getChildAt(1)?.setOnClickListener {
                try {
                    MaterialDialog(this).show {
                        title(res = R.string.userRemark)
                        input(
                            hintRes = R.string.userRemark,
                            prefill = viewBinding.toolbarLayout.toolbar.subtitle
                        ) { _, input ->
                            try {
                                FoxRiver.getRemarkSharedPreferences().edit {
                                    putString("Remark$currentUser", input.toString())
                                    viewBinding.toolbarLayout.toolbar.subtitle = input
                                }
                            } catch (e: Exception) {
                                Logger.e(TAG, "Error saving user remark: ${e.message}")
                            }
                        }
                        positiveButton(res = R.string.done)
                        negativeButton(res = R.string.cancel)
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error showing remark dialog: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error in initToolbarSubTitle: ${e.message}")
        }
    }

    private fun initViewPager() {
        try {
            val userList = PUserManager.get().getUsers()
            userList.forEach {
                fragmentList.add(AppsFragment.newInstance(it.id))
            }

            currentUser = userList.firstOrNull()?.id ?: 0
            fragmentList.add(AppsFragment.newInstance(userList.size))

            mViewPagerAdapter = ViewPagerAdapter(this)
            mViewPagerAdapter.replaceData(fragmentList)
            viewBinding.viewPager.adapter = mViewPagerAdapter
            viewBinding.dotsIndicator.setViewPager2(viewBinding.viewPager)
            viewBinding.viewPager.registerOnPageChangeCallback(object :
                ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    try {
                        super.onPageSelected(position)
                        currentUser = fragmentList[position].userID
                        updateUserRemark(currentUser)
                        showFloatButton(true)
                    } catch (e: Exception) {
                        Logger.e(TAG, "Error in onPageSelected: ${e.message}")
                    }
                }
            })
        } catch (e: Exception) {
            Logger.e(TAG, "Error in initViewPager: ${e.message}")
        }
    }

    private fun initFab() {
        try {
            viewBinding.fab.setOnClickListener {
                try {
                    val userId = viewBinding.viewPager.currentItem
                    val intent = Intent(this, ListActivity::class.java)
                    intent.putExtra("userID", userId)
                    apkPathResult.launch(intent)
                } catch (e: Exception) {
                    Logger.e(TAG, "Error launching ListActivity: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error in initFab: ${e.message}")
        }
    }

    fun showFloatButton(show: Boolean) {
        try {
            val tranY: Float = Resolution.convertDpToPixel(120F, FoxRiver.getContext())
            val time = 200L
            if (show) {
                viewBinding.fab.animate().translationY(0f).alpha(1f).setDuration(time)
                    .start()
            } else {
                viewBinding.fab.animate().translationY(tranY).alpha(0f).setDuration(time)
                    .start()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error in showFloatButton: ${e.message}")
        }
    }

    fun scanUser() {
        try {
            val userList = PUserManager.get().getUsers()

            if (fragmentList.size == userList.size) {
                fragmentList.add(AppsFragment.newInstance(fragmentList.size))
            } else if (fragmentList.size > userList.size + 1) {
                fragmentList.removeLast()
            }

            mViewPagerAdapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Logger.e(TAG, "Error in scanUser: ${e.message}")
        }
    }

    private fun updateUserRemark(userId: Int) {
        try {
            var remark = FoxRiver.getRemarkSharedPreferences().getString("Remark$userId", "User $userId")
            if (remark.isNullOrEmpty()) {
                remark = "User $userId"
            }

            viewBinding.toolbarLayout.toolbar.subtitle = remark
        } catch (e: Exception) {
            Logger.e(TAG, "Error updating user remark: ${e.message}")
            viewBinding.toolbarLayout.toolbar.subtitle = "User $userId"
        }
    }

    private val apkPathResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            try {
                if (it.resultCode == RESULT_OK) {
                    it.data?.let { data ->
                        val userId = data.getIntExtra("userID", 0)
                        val source = data.getStringExtra("source")
                        if (source != null) {
                            fragmentList[userId].installApk(source)
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "Error handling APK path result: ${e.message}")
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        try {
            menuInflater.inflate(R.menu.menu_main, menu)
            return true
        } catch (e: Exception) {
            Logger.e(TAG, "Error creating options menu: ${e.message}")
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        try {
            when (item.itemId) {
                R.id.main_git -> {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Prison3/Prison"))
                    startActivity(intent)
                }

                R.id.main_setting -> {
                    SettingActivity.start(this)
                }

                R.id.main_tg -> {
                    val intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Prison7"))
                    startActivity(intent)
                }

            }

            return true
        } catch (e: Exception) {
            Logger.e(TAG, "Error handling menu item selection: ${e.message}")
            return false
        }
    }
}
