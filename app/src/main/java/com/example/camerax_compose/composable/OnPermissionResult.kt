package com.example.camerax_compose.composable

import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale

@ExperimentalPermissionsApi
@Composable
fun OnPermissionResult(
    status: PermissionStatus,
    isGranted : @Composable ()->Unit = {},
    shouldShowRationale : @Composable ()->Unit = {},
    isPermanentlyDenied : @Composable ()->Unit = {},
) {
    when{
        status.isGranted->{
            isGranted()
        }
        status.shouldShowRationale->{
            shouldShowRationale()
        }
        status.isPermanentlyDenied()->{
            isPermanentlyDenied()
        }
    }
}

@ExperimentalPermissionsApi
fun PermissionStatus.isPermanentlyDenied():Boolean{
    return !shouldShowRationale && !isGranted
}