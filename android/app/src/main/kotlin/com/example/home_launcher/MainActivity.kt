package com.example.home_launcher

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val CHANNEL = "launcher/native"
    private val REQ_AUTH = 1001
    private val PREFS = "launcher_prefs"
    private val KEY_TARGET = "locked_package"

    private var authInProgress = false
    private var adminMode = false

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "launchTarget" -> {
                        launchTargetApp()
                        result.success(null)
                    }
                    "getUserApps" -> {
                        result.success(getUserInstalledApps())
                    }
                    "lockTargetApp" -> {
                        val pkg = call.argument<String>("package")
                        if (pkg != null) {
                            saveTargetPackage(pkg)
                            result.success(true)
                        } else result.success(false)
                    }
                    "requestAdminAuth" -> {
                        requestAdminAuth()
                        result.success(null)
                    }
                    else -> result.notImplemented()
                }
            }
    }

    private fun saveTargetPackage(pkg: String) {
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TARGET, pkg).apply()
    }

    private fun getTargetPackage(): String? =
        getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TARGET, null)

    private fun launchTargetApp() {
        if (authInProgress || adminMode) return
        val pkg = getTargetPackage() ?: return
        val intent = packageManager.getLaunchIntentForPackage(pkg) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun getUserInstalledApps(): List<Map<String, String>> {
        val pm = packageManager
        return pm.getInstalledApplications(0)
            .filter {
                (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                pm.getLaunchIntentForPackage(it.packageName) != null
            }
            .map {
                mapOf(
                    "name" to pm.getApplicationLabel(it).toString(),
                    "package" to it.packageName
                )
            }
    }

    private fun requestAdminAuth() {
        if (authInProgress) return
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!km.isKeyguardSecure) {
            openSettings()
            return
        }
        authInProgress = true
        val intent = km.createConfirmDeviceCredentialIntent(
            "Akses Admin",
            "Masukkan pola untuk membuka Settings"
        )
        startActivityForResult(intent, REQ_AUTH)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_AUTH) {
            authInProgress = false
            if (resultCode == RESULT_OK) {
                adminMode = true
                openSettings()
                autoReturn()
            } else {
                adminMode = false
                launchTargetApp()
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun autoReturn() {
        Handler(Looper.getMainLooper()).postDelayed({
            adminMode = false
            launchTargetApp()
        }, 60000)
    }

    override fun onResume() {
        super.onResume()
        if (!adminMode && !authInProgress) launchTargetApp()
    }
}