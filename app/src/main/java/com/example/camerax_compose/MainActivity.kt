package com.example.camerax_compose

import android.Manifest
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.example.camerax_compose.composable.CameraView
import com.example.camerax_compose.composable.OnPermissionResult
import com.example.camerax_compose.ui.theme.CameraX_ComposeTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var photoUri: Uri
    private lateinit var videoUri: Uri
    private var shouldShowPhoto: MutableState<Boolean> = mutableStateOf(false)
    private var shouldShowVideo : MutableState<Boolean> = mutableStateOf(false)
    private var shouldShowCamera: MutableState<Boolean> = mutableStateOf(false)
    private var shouldAudioRecord : MutableState<Boolean> = mutableStateOf(false)

    @SuppressLint("RememberReturnType")
    @ExperimentalPermissionsApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        outputDirectory = getOutputDirectory()
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            val permissionState = rememberMultiplePermissionsState(
                permissions = listOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)
            )
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(key1 = lifecycleOwner, effect = {
                val observer = LifecycleEventObserver{ _, evet ->
                    if(evet == Lifecycle.Event.ON_START){
                        permissionState.launchMultiplePermissionRequest()
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
                    val context = LocalContext.current
                    permissionState.permissions.forEach {
                        when(it.permission){
                            Manifest.permission.CAMERA->{
                                OnPermissionResult(
                                    status = it.status,
                                    isGranted = { shouldShowCamera.value = true },
                                    shouldShowRationale = {Text(text = "Allow Camera permission")},
                                    isPermanentlyDenied = {Text(text = " Camera Permission Permanently Denied , Allow it from Settings")}
                                )
                            }
                            Manifest.permission.RECORD_AUDIO->{
                                OnPermissionResult(
                                    status = it.status,
                                    isGranted = { shouldAudioRecord.value = true },
                                    shouldShowRationale = {Text(text = "Allow Audio Record permission")},
                                    isPermanentlyDenied = {Text(text = " Audio Record Permission Permanently Denied , Allow it from Settings")}
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter){
                        if (shouldShowCamera.value && shouldAudioRecord.value) {
                            CameraView(
                                outputDirectory = outputDirectory,
                                executor = cameraExecutor,
                                onImageCaptured = ::handleImageCapture,
                                onError = { Log.e("TAG", "View error:", it) },
                                onVideoRecord = ::handleVideoRecording
                            )
                        }
                        if (shouldShowPhoto.value) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUri),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(bottom = 130.dp)
                                    .size(150.dp)
                                    .border(
                                        width = 2.dp,
                                        color = Color.White,
                                        shape = RectangleShape
                                    )
                                    .zIndex(2f),
                                contentScale = ContentScale.FillBounds
                            )
                        }
                        if (shouldShowVideo.value){
                            val exoPlayer = remember(key1 = context) {
                                ExoPlayer.Builder(context).build().apply {
                                    val dataSourceFactory = DefaultDataSourceFactory(context,Util.getUserAgent(context,context.packageName))
                                    val source = ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(videoUri))
                                    prepare(source)
                                }
                            }

                            AndroidView(factory = { mContext ->
                                PlayerView(mContext).apply {
                                    player = exoPlayer
                                }
                            }, modifier = Modifier.size(500.dp).padding(bottom = 130.dp))
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

    private fun handleVideoRecording(uri: Uri){
        Log.i("TAG","Video Uri: $uri")
        videoUri = uri
        shouldShowVideo.value = true
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
