package com.CloudTrack

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class LocationAccess(private val activity: Activity) {

    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

    var latitude: Double? = null
    var longitude: Double? = null

    init {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            getLastLocation()
        }
    }


    fun getLastLocation() {
        try {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            latitude = it.latitude
                            longitude = it.longitude
                            Log.d("LA", "✅✅✅✅✅✅Latitude: $latitude, Longitude: $longitude")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("LocationAccess", "Error getting location: ${exception.message}")
                    }
            } else {
                Log.e("LocationAccess", "Permission not granted")
            }
        } catch (e: SecurityException) {
            Log.e("ss", "SecurityException: ${e.message}")
        }
    }

}
