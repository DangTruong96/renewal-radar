package io.github.dangtruong96.renewalradar.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dangtruong96.renewalradar.data.Subscription
import io.github.dangtruong96.renewalradar.notify.NotificationHelper
import io.github.dangtruong96.renewalradar.notify.ReminderWorker

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result shown in-app */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.ensureChannel(this)
        ReminderWorker.scheduleDaily(this)
        requestNotificationPermissionIfNeeded()

        setContent {
            RenewalRadarTheme {
                RenewalRadarApp()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/** Simple state-based navigation: home list, or the editor for one item. */
@Composable
fun RenewalRadarApp(viewModel: RenewalViewModel = viewModel()) {
    // null = home; NEW_ITEM_ID = adding; any other value = editing that id.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }

    if (showEditor) {
        val existing = editingId?.let { id ->
            viewModel.subscriptions.value.firstOrNull { it.id == id }
        }
        EditSubscriptionScreen(
            existing = existing,
            onSave = { sub: Subscription ->
                if (sub.id == 0L) viewModel.add(sub) else viewModel.update(sub)
                showEditor = false
            },
            onDelete = { sub: Subscription ->
                viewModel.delete(sub)
                showEditor = false
            },
            onBack = { showEditor = false }
        )
    } else {
        HomeScreen(
            viewModel = viewModel,
            onAdd = {
                editingId = null
                showEditor = true
            },
            onEdit = { sub ->
                editingId = sub.id
                showEditor = true
            }
        )
    }
}
