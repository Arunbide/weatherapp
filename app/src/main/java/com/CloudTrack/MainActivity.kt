package com.CloudTrack

import Apiinterface
import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.util.Calendar
import android.widget.SearchView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.CloudTrack.databinding.ActivityMainBinding
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private val REQUEST_CHECK_SETTINGS = 2
    private var isFetchingCurrentLocationWeather = true

        private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        searchCity()
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            checkLocationSettings()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                checkLocationSettings()
            } else {
                proceedWithoutLocation()
            }
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this)
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            getLocationAndProceed()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    proceedWithoutLocation()
                }
            } else {
                proceedWithoutLocation()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                getLocationAndProceed()
            } else {
                proceedWithoutLocation()
            }
        }
    }

    private fun getLocationAndProceed() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            locationRequest = LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val location: Location? = locationResult.lastLocation
                    if (location != null && isFetchingCurrentLocationWeather) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        proceedWithLocation(latitude, longitude)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        proceedWithLocation(latitude, longitude)
                    } else {
                        requestNewLocation()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to get location", Toast.LENGTH_SHORT).show()
                    proceedWithoutLocation()
                }
        } else {
            proceedWithoutLocation()
        }
    }

    private fun requestNewLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun proceedWithLocation(latitude: Double, longitude: Double) {
        getCityData(latitude, longitude)
    }

    private fun proceedWithoutLocation() {
        Toast.makeText(this, "Location permission not granted. Proceeding without location.", Toast.LENGTH_SHORT).show()
    }

    private fun searchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    isFetchingCurrentLocationWeather = false
                    fetchWeatherData(query)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun fetchWeatherData(cityName: String) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build().create(Apiinterface::class.java)
        val response = retrofit.getWeatherData(cityName, "fe34dc6be0cc15f888155c5118c6629e", "metric")
        response.enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val temperature = responseBody.main.temp.toString()
                    val humidity = responseBody.main.humidity
                    val windspeed = responseBody.wind.speed
                    val sunrise = responseBody.sys.sunrise.toLong()
                    val sunset = responseBody.sys.sunset.toLong()
                    val seaLevel = responseBody.main.pressure
                    val condition = responseBody.weather.firstOrNull()?.main ?: "unknown"
                    val maxTemp = responseBody.main.temp_max
                    val minTemp = responseBody.main.temp_min

                    binding.temp.text = "$temperature °C"
                    binding.windspeed.text = "$windspeed m/s"
                    binding.humidity.text = "$humidity %"
                    binding.max.text = "Max: $maxTemp °C"
                    binding.min.text = "Min: $minTemp°C"
                    binding.sunrise.text = "${time(sunrise)}"
                    binding.sunset.text = "${time(sunset)}"
                    binding.sea.text = "$seaLevel hPa"
                    binding.sunny.text = "$condition"
                    binding.Sunn.text = "$condition"
                    binding.day.text = dayName(System.currentTimeMillis())
                    binding.date.text = date()
                    binding.textView4.text = cityName

                    changeImage(condition)
                    Log.d("WeatherApp", "Temperature: $temperature")
                } else {
                    Log.d("WeatherApp", "Response unsuccessful or body is null")
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                Log.d("WeatherApp", "API call failed: ${t.message}")
            }
        })
    }

    private fun getCityData(latitude: Double, longitude: Double) {
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/geo/1.0/")
            .build()
            .create(apicity::class.java)

        val apiKey = "fe34dc6be0cc15f888155c5118c6629e"
        val response = retrofit.getCityData(latitude.toString(), longitude.toString(), apiKey)
        response.enqueue(object : Callback<List<GetCity>> {
            override fun onResponse(call: Call<List<GetCity>>, response: Response<List<GetCity>>) {
                if (response.isSuccessful) {
                    val city = response.body()?.firstOrNull()?.name
                    if (city != null) {
                        binding.textView4.text = city
                        fetchWeatherData(city)
                    } else {
                        Log.d("CityData", "City not found")
                    }
                } else {
                    Log.d("CityData", "Failed to get city data: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<GetCity>>, t: Throwable) {
                Log.d("CityData", "Failed to fetch city data: ${t.message}")
            }
        })
    }

    private fun changeImage(condition: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        when (condition) {
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                binding.root.setBackgroundResource(R.drawable.colud_background)
                binding.lottieAnimationView.setAnimation(R.raw.cloud)
                binding.lottieAnimationView.playAnimation()
                binding.lottieAnimationView.loop(true)
            }
            "Haze", "Clear Sky", "Sunny", "Clear" -> {
                if (hour >= 18 || hour < 6) {
                    binding.root.setBackgroundResource(R.drawable.nightbackgroud)
                    binding.lottieAnimationView.setAnimation(R.raw.cloud)
                    binding.lottieAnimationView.playAnimation()
                    binding.lottieAnimationView.loop(true)
                } else {
                    binding.root.setBackgroundResource(R.drawable.sunny_background)
                    binding.lottieAnimationView.setAnimation(R.raw.sun)
                    binding.lottieAnimationView.playAnimation()
                    binding.lottieAnimationView.loop(true)
                }
            }
            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Rain" -> {
                binding.root.setBackgroundResource(R.drawable.rain_background)
                binding.lottieAnimationView.setAnimation(R.raw.rain)
                binding.lottieAnimationView.playAnimation()
                binding.lottieAnimationView.loop(true)
            }
            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard", "Snow" -> {
                binding.root.setBackgroundResource(R.drawable.snow_background)
                binding.lottieAnimationView.setAnimation(R.raw.snow)
                binding.lottieAnimationView.playAnimation()
                binding.lottieAnimationView.loop(true)
            }
            else -> {
                binding.root.setBackgroundResource(R.color.blue)
                binding.lottieAnimationView.playAnimation()
                binding.lottieAnimationView.loop(true)
            }
        }
    }

    private fun date(): CharSequence {
        val sdf = SimpleDateFormat("dd MMMM YYYY", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun time(timestamp: Long): CharSequence {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp * 1000))
    }

    private fun dayName(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }
}
