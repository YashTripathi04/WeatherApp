package com.example.weatherapp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Window
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.weatherapp.api.ApiInterface
import com.example.weatherapp.BuildConfig
import com.example.weatherapp.R
import com.example.weatherapp.data.WeatherApp
import com.example.weatherapp.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.PRIORITY_LOW_POWER
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


const val APP_ID: String = BuildConfig.API_KEY
const val BASE_URL: String = BuildConfig.BASE_URL

class MainActivity : AppCompatActivity() {

    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        geocoder = Geocoder(this, Locale.getDefault())
        getCurrentLocationWeather()
        searchCity()
    }

    private fun getCityFromCoordinates(
        latitude: Double,
        longitude: Double,
        callback: (String?) -> Unit
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (addresses.isNotEmpty()) {
                            val cityName = addresses[0].locality
                            callback(cityName)
                        }
                    }
                })
            } else {
                callback(null)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            callback(null)
        }
    }

    private fun searchCity() {
        val searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(city: String?): Boolean {
                if (city != null) {
                    fetchWeatherData(city.trim())
                }
                return true;
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return true
            }
        })
    }

    private fun fetchWeatherData(_cityName: String) {
        // creating a retrofit object
        val retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(BASE_URL)
            .build().create(ApiInterface::class.java)

        // making the call
        val response = retrofit.getWeatherData(_cityName, APP_ID, "metric")

        // put the call/request in the bg thread's queue so that thread can process it
        response.enqueue(object : Callback<WeatherApp> {

            // here we are specifying the callback methods to handle response & error
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    val temp = responseBody.main.temp
                    val humidityValue = responseBody.main.humidity
                    val windspeed = responseBody.wind.speed
                    val sunriseTime = responseBody.sys.sunrise
                    val sunsetTime = responseBody.sys.sunset
                    val seaLevel = responseBody.main.pressure
                    val maxTemperature = responseBody.main.temp_max
                    val minTemperature = responseBody.main.temp_min
                    val weatherCondition = responseBody.weather.firstOrNull()?.main ?: "unknown"

                    binding.apply{
                        temperature.text = "$temp °C"
                        weather.text = weatherCondition
                        maxTemp.text = "Max Temp: $maxTemperature °C"
                        minTemp.text = "Min Temp: $minTemperature °C"
                        humidity.text = "$humidityValue %"
                        windSpeed.text = "$windspeed m/s"
                        sunrise.text = "${getTime(sunriseTime.toLong())}"
                        sunset.text = "${getTime(sunsetTime.toLong())}"
                        sea.text = "$seaLevel hPa"
                        condition.text = "$weatherCondition"
                        day.text = getDay(System.currentTimeMillis())
                        date.text = getDate()
                        cityName.text = "$_cityName"
                    }
                    changeBgCondition(weatherCondition)
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                t.printStackTrace()
            }
        })
    }

    private fun changeBgCondition(_condition: String) {
        val window: Window = window
        when (_condition) {
            "Clear Sky", "Sunny", "Clear" -> {
                binding.apply {
                    root.setBackgroundResource(R.drawable.sunny_background)
                    lottieAnimationView.setAnimation(R.raw.sun)
                }
                window.statusBarColor = Color.BLACK
            }

            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                binding.apply {
                    root.setBackgroundResource(R.drawable.colud_background)
                    lottieAnimationView.setAnimation(R.raw.cloud)
                }
                window.statusBarColor = Color.BLACK
            }

            "Rain", "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain" -> {
                binding.apply {
                    root.setBackgroundResource(R.drawable.rain_background)
                    lottieAnimationView.setAnimation(R.raw.rain)
                }
                window.statusBarColor = Color.BLACK
            }

            "Light Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> {
                binding.apply {
                    root.setBackgroundResource(R.drawable.snow_background)
                    lottieAnimationView.setAnimation(R.raw.snow)
                }
                window.statusBarColor = Color.BLACK
            }

            else -> {
                binding.apply {
                    root.setBackgroundResource(R.drawable.sunny_background)
                    lottieAnimationView.setAnimation(R.raw.sun)
                }
                window.statusBarColor = Color.BLACK
            }
        }
        binding.lottieAnimationView.playAnimation()
    }

    private fun getTime(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timeStamp * 1000))
    }

    private fun getDate(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun getDay(timeStamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date())
    }

    /* Location methods */
    companion object {
        private const val PERMISSION_REQUEST_ACCESS_LOCATION = 100
    }

    private fun checkPermission(): Boolean {
        val fineLocation = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocation && coarseLocation) return true
        return false
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled((LocationManager.NETWORK_PROVIDER))
    }

    private fun requestPermission() {
        // as we want to req for multiple permission, we must pass it as an array
        // we also pass a "request code" to identify this set of permission later while checking if they are granted
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_ACCESS_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, // request code we defined
        permissions: Array<out String>, // names of permission requested for, in the same order of request
        grantResults: IntArray // result of the request to each permission, in the order of request
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // this method takes care of all permission requests made by our app
        // so we use the "request code" we defined while making permission request to check if those request are granted
        if (requestCode == PERMISSION_REQUEST_ACCESS_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "Location Access Granted !", Toast.LENGTH_SHORT)
                    .show()
                getCurrentLocationWeather()
            } else {
                Toast.makeText(applicationContext, "Location Access Denied !", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun getCurrentLocationWeather() {
        if (checkPermission()) {
            if (isLocationEnabled()) {
                // find latitude and longitude here
                fusedLocationProviderClient.getCurrentLocation(PRIORITY_LOW_POWER, null)
                    .addOnSuccessListener {
                        if (it != null) {
                            val lat = it.latitude
                            val long = it.longitude
                            getCityFromCoordinates(lat, long) { currentCity ->
                                fetchWeatherData(currentCity!!)
                            }
                        } else {
                            Toast.makeText(
                                applicationContext,
                                "System Not Responding",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                // open settings to enable location
                Toast.makeText(applicationContext, "Turn On Location", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            // ask for permission again
            requestPermission()
        }
    }
}
