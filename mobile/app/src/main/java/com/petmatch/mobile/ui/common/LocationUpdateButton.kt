package com.petmatch.mobile.ui.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.petmatch.mobile.ui.theme.AccentPurple

/**
 * Composable nút "Cập nhật vị trí GPS".
 * Tự xử lý runtime permission → lấy vị trí → gọi onLocationObtained(lat, lon).
 */
@SuppressLint("MissingPermission")
@Composable
fun LocationUpdateButton(
    modifier: Modifier = Modifier,
    onLocationObtained: (Double, Double) -> Unit,
    onPermissionDenied: () -> Unit = {}
) {
    val ctx = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocation(ctx, onSuccess = { lat, lon ->
                isLoading = false
                onLocationObtained(lat, lon)
            }, onFailure = { isLoading = false })
            isLoading = true
        } else {
            onPermissionDenied()
        }
    }

    Button(
        onClick = {
            val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                fetchLocation(ctx, onSuccess = { lat, lon ->
                    isLoading = false
                    onLocationObtained(lat, lon)
                }, onFailure = { isLoading = false })
                isLoading = true
            } else {
                permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        },
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
        enabled = !isLoading
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text("Đang lấy vị trí...")
        } else {
            Icon(
                Icons.Default.GpsFixed,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Cập nhật vị trí GPS")
        }
    }
}

@SuppressLint("MissingPermission")
private fun fetchLocation(
    ctx: Context,
    onSuccess: (Double, Double) -> Unit,
    onFailure: () -> Unit
) {
    try {
        val client = LocationServices.getFusedLocationProviderClient(ctx)
        val cts = CancellationTokenSource()
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    // Fallback: last known location
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) onSuccess(last.latitude, last.longitude)
                        else onFailure()
                    }.addOnFailureListener { onFailure() }
                }
            }
            .addOnFailureListener { onFailure() }
    } catch (e: Exception) {
        onFailure()
    }
}
