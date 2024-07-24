package com.example.eventstogo_group6.screens

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.EventRepository
import com.example.eventstogo_group6.databinding.ActivityEventDetailsBinding
import com.example.eventstogo_group6.databinding.ActivityRegisteredEventDetailsBinding
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.ui.StaticWindowAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale

class RegisteredEventDetails : AppCompatActivity(), OnMapReadyCallback {
    private val TAG: String = this@RegisteredEventDetails.toString()
    private lateinit var binding: ActivityRegisteredEventDetailsBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var event: Event? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisteredEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SupportMapFragment
        mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()

        mapFragment.getMapAsync(this)


        // Extract event ID from the intent data
        val eventID = intent.getStringExtra("eventID")
        Log.d(TAG, "EventDetails - Get Event ID: $eventID")

        // Check if eventId is not null or empty
        if (!eventID.isNullOrBlank()) {
            // Fetch event details from Firestore
            fetchEventDetails(eventID)
        } else {
            // Handle the case where eventId is null or empty
            // You can show an error message or navigate back to the previous screen
            Log.d("EventDetails", "Empty Intent Data Get.")
        }

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map ?: return

        // Get event details
        val eventID = intent.getStringExtra("eventID")
        if (!eventID.isNullOrBlank()) {
            fetchEventDetails(eventID)
        } else {
            Log.d(TAG, "Empty Intent Data Get.")
        }

        // Check if event is not null
        event?.let { setupMap(it) }
    }

    override fun onResume() {
        super.onResume()
        mapFragment.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapFragment.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapFragment.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapFragment.onLowMemory()
    }



    private fun fetchEventDetails(eventId: String) {
        val eventRepository = EventRepository(this)

        // Observe changes in the event with the specified ID
        eventRepository.fetchEventByID(eventId).observe(this) { event ->
            if (event != null) {
                // Populate the UI with event details
                populateUI(event)

                // Fetch coordinates from address and set up the map
                setupMap(event)
            } else {
                // Handle the case where the document does not exist or an error occurred
                Log.d(TAG, "Event document does not exist or an error occurred for ID: $eventId")
            }
        }
    }

    private fun populateUI(event: Event) {
        // Populate the UI with event details
        if (event.price != 0.0){
            binding.tvPrice.text = "Price: $${event.price}"
            binding.tvRefundPolicy.text = "Free cancellation before a week of notice."
        } else {
            binding.tvPrice.text = "Free"
            binding.tvRefundPolicy.text = "Not applicable."
        }

        binding.tvEventName.text = event.name

        binding.tvEventVenue.text = event.building
        binding.tvEventAddress.text = "${event.street}, " +
                "${event.city}, " +
                "${event.country}"
        // Convert datestamp to a readable date format
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val startDate = dateFormat.format(event.scheduleStart)
        val endDate = dateFormat.format(event.scheduleEnd)
        binding.tvEventDate.text = "From: $startDate"
        binding.tvEventTime.text = "Till: $endDate"
        binding.tvEventDescription.text = event.description
        Picasso.with(this).load(event.image).placeholder(getDrawable(R.drawable.ic_launcher_background)).into(binding.ivEventImage)
        // Add similar code to populate other views with event details
        Log.d(TAG, "Populated UI with event details: $event")

    }

    private fun setupMap(event: Event) {
        // Check if googleMap is initialized
        if (::googleMap.isInitialized) {
            // Get event location coordinates from the address
            val eventLocation = getLatLngFromAddress(event.building, event.street, event.city, event.country)

            // Check if coordinates are available
            if (eventLocation != null) {
                // Set up the map with the obtained coordinates
                googleMap.addMarker(MarkerOptions().position(eventLocation).title("Event Location"))
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 12f))
                googleMap.setInfoWindowAdapter(StaticWindowAdapter(this, event))
                googleMap.setOnInfoWindowClickListener {
                    // Open Google Maps with directions
                    val uri = Uri.parse("google.navigation:q=${eventLocation.latitude},${eventLocation.longitude}")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    startActivity(intent)
                }
            } else {
                Log.e(TAG, "Unable to get coordinates from address")
            }
        } else {
            Log.e(TAG, "googleMap is not initialized yet")
        }
    }


    private fun getLatLngFromAddress(building: String, street: String, city: String, country: String): LatLng? {
        val address = "$building, $street, $city, $country"
        val geocoder = Geocoder(this)
        val locationList: List<Address>?

        try {
            locationList = geocoder.getFromLocationName(address, 1)
            if (locationList == null || locationList.isEmpty()) {
                return null
            }

            val latitude = locationList[0].latitude
            val longitude = locationList[0].longitude

            return LatLng(latitude, longitude)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }
}