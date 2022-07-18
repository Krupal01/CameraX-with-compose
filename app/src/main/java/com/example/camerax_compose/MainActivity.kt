package com.example.camerax_compose

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.example.camerax_compose.composable.CameraView
import com.example.camerax_compose.composable.OnPermissionResult
import com.example.camerax_compose.ui.theme.CameraX_ComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var photoUri: Uri
    private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)

    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            val permissionState = rememberPermissionState(
                permission = Manifest.permission.CAMERA
            )
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(key1 = lifecycleOwner, effect = {
                val observer = LifecycleEventObserver{ _, evet ->
                    if(evet == Lifecycle.Event.ON_START){
                        permissionState.launchPermissionRequest()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            })

            CameraX_ComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {

                    OnPermissionResult(
                        status = permissionState.status,
                        isGranted = {
                            shouldShowCamera.value = true
                        },
                        shouldShowRationale = {Text(text = "Allow Camera permission")},
                        isPermanentlyDenied = {Text(text = " Camera Permission Permanently Denied , Allow it from Settings")}
                    )
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd){
                        if (shouldShowCamera.value) {
                            CameraView(
                                outputDirectory = outputDirectory,
                                executor = cameraExecutor,
                                onImageCaptured = ::handleImageCapture,
                                onError = { Log.e("TAG", "View error:", it) }
                            )
                        }
                        if (shouldShowPhoto.value) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUri),
                                contentDescription = null,
                                modifier = Modifier.padding(end = 50.dp, bottom = 20.dp).size(100.dp)
                                    .border(width = 2.dp, color = Color.White, shape = RectangleShape)
                                    .zIndex(2f),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                    }
                }
            }
        }
    }


    private fun handleImageCapture(uri: Uri) {
        Log.i("TAG", "Image captured: $uri")
        shouldShowCamera.value = false
        photoUri = uri
        shouldShowPhoto.value = true
    }

    private fun getOutputDirectory(): File {

        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
