package com.lyd.player.nav

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.lyd.player.core.data.mediastore.MediaPermission
import com.lyd.player.core.design.EmptyState
import com.lyd.player.core.design.LydColors
import com.lyd.player.core.design.LydSpacing
import com.lyd.player.core.design.LydType
import com.lyd.player.core.design.PillButton

@Composable
fun MediaPermissionGate(
    onPermissionGranted: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(MediaPermission.isGranted(context)) }
    val onGranted by rememberUpdatedState(onPermissionGranted)
    var requestedOnce by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        granted = isGranted
        requestedOnce = true
    }

    // Best-effort: without this (API 33+), the lock-screen/notification media controls never
    // appear even though playback works fine, since Android silently drops the notification.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(granted) {
        if (granted) {
            onGranted()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (granted) {
        content()
    } else {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            Text("Lyd", style = LydType.display, color = LydColors.OnSurface)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = LydSpacing.lg))
            EmptyState(
                message = if (requestedOnce) {
                    "Lyd needs access to your music to scan your library. You can grant it in Settings, or try again below."
                } else {
                    "Lyd needs access to your device's audio files to build your library."
                },
                icon = Icons.Filled.LibraryMusic,
                action = {
                    PillButton(text = "Grant access") { launcher.launch(MediaPermission.permissionString) }
                },
            )
        }
    }
}
