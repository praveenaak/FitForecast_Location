package com.example.fitforecast_location

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor

import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult

import android.os.Looper

import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import android.location.Location

import com.google.android.libraries.places.api.Places
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private const val TAG = "MyActivity"

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    private lateinit var buttonhc1: Button
    private lateinit var buttonhc2: Button
    private lateinit var buttonhc3: Button
    private lateinit var buttona1: Button
    private lateinit var buttona2: Button
    private lateinit var buttona3: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonhc1 = findViewById(R.id.buttonhc1)
        buttonhc2 = findViewById(R.id.buttonhc2)
        buttonhc3 = findViewById(R.id.buttonhc3)
        buttona1 = findViewById(R.id.buttona1)
        buttona2 = findViewById(R.id.buttona2)
        buttona3 = findViewById(R.id.buttona3)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Assume you have a method to get current location
        getCurrentLocation()

        buttonhc1.setOnClickListener {
            val hospitalName = buttonhc1.text.toString()
            if (hospitalName != "Hospital Not Found") {
                if (hasCalendarPermission()) {
                    addToCalendar(hospitalName)
                } else {
                    requestCalendarPermission()
                }
            }
        }

        buttonhc2.setOnClickListener {
            val hospitalName = buttonhc2.text.toString()
            if (hospitalName != "Hospital Not Found") {
                if (hasCalendarPermission()) {
                    addToCalendar(hospitalName)
                } else {
                    requestCalendarPermission()
                }
            }
        }

        buttonhc3.setOnClickListener {
            val hospitalName = buttonhc3.text.toString()
            if (hospitalName != "Hospital Not Found") {
                if (hasCalendarPermission()) {
                    addToCalendar(hospitalName)
                } else {
                    requestCalendarPermission()
                }
            }
        }

        if (hasCalendarReadPermission()) {
            findGapsInCalendar()
        } else {
            requestCalendarReadPermission()
        }
    }



    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location in locationResult.locations){
                        // Fetch hospitals and update UI
                        fetchNearbyHospitals(location) { hospitals ->
                            runOnUiThread {
                                buttonhc1.text = hospitals.getOrNull(0) ?: "Hospital Not Found"
                                buttonhc2.text = hospitals.getOrNull(1) ?: "Hospital Not Found"
                                buttonhc3.text = hospitals.getOrNull(2) ?: "Hospital Not Found"
                            }
                        }
                    }
                }
            },
            Looper.getMainLooper())
    }
    private fun calculateDistance(startLocation: Location, endLatitude: Double, endLongitude: Double): Float {
        val endLocation = Location("").apply {
            latitude = endLatitude
            longitude = endLongitude
        }
        return startLocation.distanceTo(endLocation) // Distance in meters
    }

    private fun fetchNearbyHospitals(location: Location?, callback: (List<String>) -> Unit) {
        if (location == null) {
            callback(emptyList())
            return
        }

        val httpClient = OkHttpClient()
        val latitude = location.latitude
        val longitude = location.longitude
        val radius = 10000 // Search radius in meters
        val type = "hospital"
        val apiKey = "AIzaSyCJ0xv8NRQa8ymM71e7RzEPlkxYH9u72QQ" // Replace with your actual API key

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=$latitude,$longitude&radius=$radius&type=$type&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure
                callback(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                val responseString = response.body?.string()
                val hospitals = mutableListOf<String>()

                if (responseString != null) {
                    val jsonResponse = JSONObject(responseString)
                    val results = jsonResponse.getJSONArray("results")
                    for (i in 0 until results.length()) {
                        val place = results.getJSONObject(i)
                        val name = place.getString("name")
                        val placeLocation = place.getJSONObject("geometry").getJSONObject("location")
                        val hospitalLatitude = placeLocation.getDouble("lat")
                        val hospitalLongitude = placeLocation.getDouble("lng")

                        val distance = calculateDistance(location, hospitalLatitude, hospitalLongitude)
                        val distanceInKm = distance / 1000  // Convert meters to kilometers
                        val hospitalInfo = "$name - ${"%.2f".format(distanceInKm)} km"

                        hospitals.add(hospitalInfo)
                    }
                }

                callback(hospitals)
            }
        })
    }
    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val PERMISSIONS_REQUEST_CALENDAR = 2
        private const val PERMISSIONS_REQUEST_READ_CALENDAR = 3
    }

    // Check Calendar Read Permission
    private fun hasCalendarReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    // Request Calendar Read Permission
    private fun requestCalendarReadPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR), PERMISSIONS_REQUEST_READ_CALENDAR)
    }
    // Utility function to check if calendar permissions are granted
    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }

    // Utility function to request calendar permissions
    private fun requestCalendarPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            PERMISSIONS_REQUEST_CALENDAR)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                }
            }
            PERMISSIONS_REQUEST_CALENDAR -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Calendar permission granted
                    // You can now perform calendar operations
                }
            }
        }
    }


    private fun addToCalendar(hospitalName: String) {
        val startTime = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, 2) // 2 weeks from now
            set(Calendar.HOUR_OF_DAY, 8) // 8 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        val endTime = startTime.clone() as Calendar
        endTime.add(Calendar.HOUR_OF_DAY, 1) // Assuming the event lasts for 1 hour

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.timeInMillis)
            putExtra(CalendarContract.Events.TITLE, "Hospital Visit to $hospitalName")
            putExtra(CalendarContract.Events.EVENT_LOCATION, hospitalName)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }
        startActivity(intent)
    }


    private fun findGapsInCalendar() {
        val calendar = Calendar.getInstance()
        val startOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfDay = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ?)"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
        val projection = arrayOf(CalendarContract.Events.DTSTART, CalendarContract.Events.DTEND)

        val cursor: Cursor? = contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val events = mutableListOf<Pair<Long, Long>>() // List of event start and end times
        while (cursor?.moveToNext() == true) {
            val start = cursor.getLong(0)
            val end = cursor.getLong(1)
            events.add(start to end)
        }
        cursor?.close()

        // Sort events by start time
        events.sortBy { it.first }

        val gaps = findGaps(events)
        displayGaps(gaps)
    }


    private fun findGaps(events: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
        val gaps = mutableListOf<Pair<Long, Long>>()

        // Iterate through events and find gaps
        for (i in 0 until events.size - 1) {
            val gapStart = events[i].second
            val gapEnd = events[i + 1].first

            if (gapStart < gapEnd) {
                gaps.add(gapStart to gapEnd)
            }
        }

        return gaps.take(3) // Take the first three gaps
    }

    private fun displayGaps(gaps: List<Pair<Long, Long>>) {
        val buttons = listOf(buttona1, buttona2, buttona3)
        gaps.forEachIndexed { index, gap ->
            val startTime = Date(gap.first)
            val endTime = Date(gap.second)
            val gapDuration = endTime.time - startTime.time // Duration in milliseconds
            val durationMinutes = gapDuration / 60000 // Convert to minutes
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val text = "${format.format(startTime)} - ${format.format(endTime)} ($durationMinutes min)"
            buttons.getOrNull(index)?.text = text
        }
    }
}
