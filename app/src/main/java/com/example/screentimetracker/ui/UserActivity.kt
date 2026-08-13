package com.example.screentimetracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.screentimetracker.R
import com.example.screentimetracker.databinding.ActivityUserBinding
import com.example.screentimetracker.service.ScreenTimeForegroundService
import com.example.screentimetracker.util.TrackingPrefs
import com.example.screentimetracker.util.UsageAccessHelper
import com.example.screentimetracker.data.ServerPinger
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class UserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserBinding

    /** True while we've sent the user to system Settings and are waiting
     *  for them to come back with Usage Access granted. */
    private var awaitingUsageAccessResult = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Whether granted or denied, continue the flow -- a foreground
            // service can still run without POST_NOTIFICATIONS, it just
            // won't show a visible notification to the user on API 33+.
            checkUsageAccessAndStart()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartTracking.setOnClickListener { onStartTrackingClicked() }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatusLabel()
        refreshActiveSessionState()

        if (awaitingUsageAccessResult && UsageAccessHelper.hasUsageAccess(this)) {
            awaitingUsageAccessResult = false
            ensureNotificationPermissionThenStart()
        }
    }

    private fun refreshPermissionStatusLabel() {
        val granted = UsageAccessHelper.hasUsageAccess(this)
        binding.tvPermissionStatus.text = if (granted) {
            getString(R.string.label_permission_granted)
        } else {
            getString(R.string.label_permission_not_granted)
        }
    }

    private fun refreshActiveSessionState() {
        if (TrackingPrefs.isTracking(this)) {
            val userName = TrackingPrefs.getUserName(this)
            binding.tvSessionStatus.visibility = android.view.View.VISIBLE
            binding.tvSessionStatus.text = getString(R.string.label_tracking_active, userName)
            binding.btnStartTracking.isEnabled = false
            binding.btnStartTracking.text = getString(R.string.btn_tracking_in_progress)
        } else {
            binding.tvSessionStatus.visibility = android.view.View.GONE
            binding.btnStartTracking.isEnabled = true
            binding.btnStartTracking.text = getString(R.string.btn_start_tracking)
        }
    }

    private fun onStartTrackingClicked() {
        val userName = binding.etUserName.text?.toString()?.trim().orEmpty()
        if (userName.isEmpty()) {
            binding.tilUserName.error = getString(R.string.error_name_required)
            return
        }
        binding.tilUserName.error = null

        // Remember the name so it survives a round trip to system
        // Settings (for the Usage Access / notification permission
        // prompts) without marking a session as active yet.
        TrackingPrefs.saveDraftUserName(this, userName)

        ensureNotificationPermissionThenStart()
    }

    private fun ensureNotificationPermissionThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        checkUsageAccessAndStart()
    }

    private fun checkUsageAccessAndStart() {
        if (!UsageAccessHelper.hasUsageAccess(this)) {
            Toast.makeText(
                this,
                getString(R.string.toast_enable_usage_access),
                Toast.LENGTH_LONG
            ).show()
            awaitingUsageAccessResult = true
            UsageAccessHelper.openUsageAccessSettings(this)
            return
        }
        startTrackingSession()
    }

    private fun startTrackingSession() {
        val userName = binding.etUserName.text?.toString()?.trim().orEmpty()
        if (userName.isEmpty()) return

        TrackingPrefs.startSession(this, userName)
        ScreenTimeForegroundService.start(this, userName)

        // Ping the server to register connection
        lifecycleScope.launch {
            ServerPinger.pingConnect(userName)
        }

        refreshActiveSessionState()
        Toast.makeText(this, getString(R.string.toast_welcome, userName), Toast.LENGTH_SHORT).show()
    }
}
