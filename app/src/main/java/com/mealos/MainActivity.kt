package com.mealos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.mealos.ui.MealOsApp
import com.mealos.ui.theme.MealOsTheme
import com.mealos.timer.TimerNotifications

class MainActivity : ComponentActivity() {
    private val viewModel: MealOsViewModel by viewModels()
    private var timerAlertsAvailable by mutableStateOf(false)
    private var preciseTimersAvailable by mutableStateOf(false)
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshTimerAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleRecipeIntent(intent)
        enableEdgeToEdge()
        setContent {
            val fold by produceState<FoldingFeature?>(initialValue = null) {
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { info -> value = info.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull() }
            }
            MealOsTheme {
                MealOsApp(
                    viewModel = viewModel,
                    fold = fold,
                    timerAlertsAvailable = timerAlertsAvailable,
                    preciseTimersAvailable = preciseTimersAvailable,
                    onEnableTimerAlerts = ::enableTimerAlerts,
                    onEnablePreciseTimers = ::enablePreciseTimers
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRecipeIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshTimerAccess()
        viewModel.rescheduleRunningTimers()
    }

    private fun refreshTimerAccess() {
        timerAlertsAvailable = TimerNotifications.canPostAlerts(this)
        preciseTimersAvailable = TimerNotifications.canScheduleExactAlarms(this)
    }

    private fun enableTimerAlerts() {
        val runtimePermissionMissing = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        val permissionHistory = getSharedPreferences("mealos_permissions", MODE_PRIVATE)
        val askedBefore = permissionHistory.getBoolean("notification_asked", false)
        if (runtimePermissionMissing && (!askedBefore || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            permissionHistory.edit { putBoolean("notification_asked", true) }
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startActivity(TimerNotifications.notificationSettingsIntent(this))
        }
    }

    private fun enablePreciseTimers() {
        if (Build.VERSION.SDK_INT < 31) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)
    }

    private fun handleRecipeIntent(intent: Intent?) {
        if (intent == null || intent.action == Intent.ACTION_MAIN) return
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        val sharedFile = intent.sharedStreamUri()
        val content = sharedText ?: (intent.data ?: sharedFile ?: intent.clipData?.getItemAt(0)?.uri)?.let { uri ->
            runCatching { contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
        }
        if (!content.isNullOrBlank()) viewModel.importRecipe(content)
    }

    @Suppress("DEPRECATION")
    private fun Intent.sharedStreamUri(): Uri? = if (Build.VERSION.SDK_INT >= 33) {
        getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        getParcelableExtra(Intent.EXTRA_STREAM)
    }
}
