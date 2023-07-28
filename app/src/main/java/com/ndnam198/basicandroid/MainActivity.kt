package com.ndnam198.basicandroid

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
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
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target

private val locationPermissions =
    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)

const val LOCATION_TAG = "LOCATION_TAG"
const val LOCATION_PERMISSION_REQUEST_CODE = 1001

class MainActivity : AppCompatActivity() {


    //A callback for receiving notifications from the FusedLocationProviderClient.
    private var locationCallback: LocationCallback? = null

    //The main entry point for interacting with the Fused Location Provider
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var isLocationUpdate = false

    /** State variables */

    /** Whether permission was granted to access approximate location. */
    private var accessCoarseLocationGranted by mutableStateOf(false)

    /** Whether permission was granted to access precise location. */
    private var accessFineLocationGranted by mutableStateOf(false)

    private var lastLocation by mutableStateOf<Location?>(null)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Setup location callback
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                lastLocation = p0.lastLocation
            }
        }

        // Check if the location permission is already granted
        if (hasPermissions()) {
            Log.d(LOCATION_TAG, "has permission")
            // Permission already granted, proceed with location-related functionality
            // Your code to access location goes here...
        } else {
            Log.d(LOCATION_TAG, "request permission")
            // Request location permission
            requestPermissions()
        }


        setContent {
            AppUI()
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                accessCoarseLocationGranted = true
                accessFineLocationGranted = true
                // Location permission granted, proceed with location-related functionality
                // Your code to access location goes here...
            } else {
                // Location permission denied, handle this scenario (e.g., show a message)
                // You may also check if "shouldShowRequestPermissionRationale()" returns true,
                // which indicates that the user denied the permission but did not select "Don't ask again".
            }
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            locationPermissions,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }


    private fun hasPermissions(): Boolean {
        accessCoarseLocationGranted =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        accessFineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        return accessCoarseLocationGranted && accessFineLocationGranted
    }


    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationCallback?.let {
            val request = LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 10_000 // 10 seconds
            }

//            val locationRequest = LocationRequest.Builder(
//                Priority.PRIORITY_HIGH_ACCURACY,
//                10000,
//            ).build()

            // Note: For this sample it's fine to use the main looper, so our callback will run on the
            // main thread. If your callback will perform any intensive operations (writing to disk,
            // making a network request, etc.), either change to a background thread from the callback,
            // or create a HandlerThread and pass its Looper here instead.
            // See https://developer.android.com/reference/android/os/HandlerThread.
            fusedLocationProviderClient.requestLocationUpdates(
                request, it, Looper.getMainLooper()
            )
        }

    }

    private fun toggleLocationUpdate() {
        if (!hasPermissions()) {
            return;
        }

        when (isLocationUpdate) {
            true -> stopLocationUpdate()
            false -> startLocationUpdates()
        }
    }


    private fun stopLocationUpdate() {
        locationCallback?.let {
            try {
                //Removes all location updates for the given callback.
                val removeTask =
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback!!)
                removeTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(LOCATION_TAG, "Location Callback removed.")
                    } else {
                        Log.d(LOCATION_TAG, "Failed to remove Location Callback.")
                    }
                }
            } catch (se: SecurityException) {
                Log.e(LOCATION_TAG, "Failed to remove Location Callback.. $se")
            }
        }

    }

    private fun openSettingPage() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    @Composable
    fun AppVersion() {
        var isDisplayAppVer by remember { mutableStateOf(false) }

        Text(text = "Version", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { isDisplayAppVer = !isDisplayAppVer }) {
            Text(text = "Display App version")
        }
        if (isDisplayAppVer) {
            Text(text = "Version :${BuildConfig.VERSION_NAME}")
            Text(text = "Build   :${BuildConfig.VERSION_CODE}")
        }
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
            picasso.load(url).placeholder(R.drawable.placeholder_3).error(R.drawable.critical_image)
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
            Image(
                bitmap = drawable!!.toBitmap().asImageBitmap(),
                contentDescription = "Drawable Image"
            )
        }
    }


    @Composable
    fun CurrentUserLocation() {
        Text(text = "Location", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { openSettingPage() }) {
            Text(text = "open setting page")
        }

        Button(onClick = { toggleLocationUpdate() }) {
            Text(text = "start location update")
        }
        lastLocation?.let {
            Text(text = "la     : ${lastLocation!!.latitude}")
            Text(text = "long   : ${lastLocation!!.longitude}")
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppUI() {
        MaterialTheme {
            Scaffold { contentPadding ->
                Surface(
                    modifier = Modifier
                        .padding(contentPadding)
                        .fillMaxSize(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AppVersion()
                        Spacer(modifier = Modifier.height(50.dp))
                        CurrentUserLocation()
                        Spacer(modifier = Modifier.height(50.dp))
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