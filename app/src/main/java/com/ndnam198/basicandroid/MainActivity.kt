package com.ndnam198.basicandroid

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target


class MainActivity : AppCompatActivity() {
    private val DEBUG_TAG = "BASIC_ANDROID"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val versionCode = BuildConfig.VERSION_CODE
    private val versionName = BuildConfig.VERSION_NAME
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let {
                    val latitude = it.latitude
                    val longitude = it.longitude
                    Log.d(DEBUG_TAG, "location data: $latitude, $longitude")
                }
            }
        }

        setContent {
            AppUI()
        }
    }

    override fun onStop() {
        super.onStop()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                // FIXME crash here
                this, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ), LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getLocation()

        }
//        if (checkSelfPermission(
//                android.Manifest.permission.ACCESS_FINE_LOCATION
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestPermissions(
//                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
//                LOCATION_PERMISSION_REQUEST_CODE
//            )
//        } else {
//            // Permissions are already granted, proceed with location retrieval
//            getLocation()
//        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        Log.d(DEBUG_TAG, "onRequestPermissionsResult called")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, proceed with location retrieval
                getLocation()
            } else {
                // Permission was denied, handle this case
                // For example, show a message or disable location-related features
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        Log.d(DEBUG_TAG, "getting location")

        // FIXME a lot of deprecation
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }


    @Composable
    fun AppVersion() {
        var isDisplayAppVer by remember { mutableStateOf(false) }

        Text(text = "Version", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { isDisplayAppVer = !isDisplayAppVer }) {
            Text(text = "Display App version")
        }
        if (isDisplayAppVer) {
            Text(text = "Version :$versionName")
            Text(text = "Build   :$versionCode")
        }
    }

    @Composable
    fun TestLocation() {
        Text(text = "Location", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { checkLocationPermissions() }) {
            Text(text = "Check location permission")
        }
        Text(text = "permission: $locationPermission")
    }

    @Composable
    fun PicassoImage() {
        val url =
            "https://www.aussietreesolutions.com.au/wp-content/uploads/2018/08/facts-about-trees.jpg"

        Text(text = "Picasso Image", style = MaterialTheme.typography.titleLarge)

        var image by remember { mutableStateOf<ImageBitmap?>(null) }
        var drawable by remember { mutableStateOf<Drawable?>(null) }

        DisposableEffect(url) {
            val picasso = Picasso.get()

            val target = object : Target {
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    drawable = placeHolderDrawable
                }

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    //Handle the exception here
                    drawable = errorDrawable
                }

                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {

                    //Here we get the loaded image
                    bitmap?.let {
                        image = it.asImageBitmap()
                    }
                }
            }
            picasso
                .load(url)
                .placeholder(R.drawable.placeholder_3)
                .error(R.drawable.critical_image)
                .into(target)

            onDispose {
                image = null
                drawable = null
                picasso.cancelRequest(target)
            }
        }

        if (image != null) {
            // Image is a pre-defined composable that lays out and draws a given [ImageAsset].
            Image(bitmap = image!!, contentDescription = "Bitmap image")
        } else if (drawable != null) {
            // Display the Image using Image composable
            Image(bitmap = drawable!!.toBitmap().asImageBitmap(), contentDescription = "Drawable Image")
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppUI() {
        MaterialTheme {
            Scaffold(topBar = {
                TopAppBar(
                    title = {
                        Text("Basic Android")
                    },
                )
            }) { contentPadding ->
                Surface(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppVersion()
                        Spacer(modifier = Modifier.height(50.dp))
//                        TestLocation()
                        PicassoImage()
                    }
                }
            }
        }
    }


    @Preview
    @Composable
    fun PreviewApp() {
        AppUI()
    }
}