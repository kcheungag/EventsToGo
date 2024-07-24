package com.example.eventstogo_group6

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventstogo_group6.databinding.ActivityMainBinding
import com.example.eventstogo_group6.enums.SharedPrefRef
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.screens.EventDetails
import com.example.eventstogo_group6.screens.LoginActivity
import com.example.eventstogo_group6.ui.BaseUI
import com.example.eventstogo_group6.ui.MarkerWindowAdapter
import com.example.eventstogo_group6.ui.SearchEventsAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date


import java.util.Locale

class MainActivity : BaseUI(), OnMapReadyCallback {

    private val TAG: String = this@MainActivity.toString()
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SearchEventsAdapter
    lateinit var sharedPreferences: SharedPreferences
    lateinit var prefEditor: SharedPreferences.Editor
    private lateinit var recyclerView: RecyclerView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapView: SupportMapFragment
    private val mapZoomLevel = 12f
    private lateinit var googleMap: GoogleMap
    private lateinit var userLocation: LatLng
    private var isMapReady: Boolean = false
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val criteria: MutableMap<String, Any> = mutableMapOf()  // Initialize criteria to store filter options
    private val dateCriteria: MutableMap<String, Any> = mutableMapOf()
    private val priceCriteria: MutableMap<String, Any> = mutableMapOf()
    private val isAvailableCriteria: MutableMap<String, Any> = mutableMapOf()
    private var eventListener: ListenerRegistration? = null




    // Define your spinner options
    private val dateOptions = arrayOf("All","Choose time", "This Week", "This Month")
    private val priceOptions = arrayOf("All","Free", "Under $50", "Under $150")
    private val isAvailableOptions = arrayOf("All", "Active", "Inactive")
    //private val categoryOptions = arrayOf("Productive", "Music", "Networking", "Race", "Tour")
    //private val languageOptions = arrayOf("English", "French", "Mandarin", "Cantonese")


    private val APP_PERMISSIONS_LIST = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val multiplePermissionsResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultsList ->
            Log.d(TAG, resultsList.toString())

            var allPermissionsGrantedTracker = true

            for (item in resultsList.entries) {
                if (item.key in APP_PERMISSIONS_LIST && item.value == false) {
                    allPermissionsGrantedTracker = false
                }
            }

            if (allPermissionsGrantedTracker) {
                Log.d(TAG,"All permissions granted")
                populateLocationEditText()
            } else {
                Log.d(TAG,"Insufficient Permissions")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Initialize userLocation with a default value or some initial coordinates
        userLocation = LatLng(0.0, 0.0)

        // Initialize login info
        sharedPreferences = getSharedPreferences(SharedPrefRef.SHARED_PREF_NAME.value, MODE_PRIVATE)
        prefEditor = sharedPreferences.edit()
        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Initialize mapview
        mapView = supportFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment
        mapView.getMapAsync(this)
        // Initialize recyclerview
        recyclerView = binding.rvEvent
        // Initialize spinner
        initializeSpinner()
        multiplePermissionsResultLauncher.launch(APP_PERMISSIONS_LIST)

        // Fetch events from Firestore
        fetchEventsFromFirestore(criteria)

        binding.loginButton.setOnClickListener {
            this.login()
        }

        binding.currentLocationIcon.setOnClickListener {
            Log.d(TAG, "Current Location Fetching")
            // Check for permissions & do resulting actions
            multiplePermissionsResultLauncher.launch(APP_PERMISSIONS_LIST)
        }

        binding.searchIcon.setOnClickListener {
            val addressFromUI = binding.etSearchAddress.text.toString()
            Log.d(TAG, "Getting coordinates for $addressFromUI")

            searchLocation(addressFromUI)
        }


        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Show MAP, hide LIST
                mapView.view?.visibility = View.VISIBLE
                binding.rvEvent.visibility = View.GONE
            } else {
                // Show LIST, hide MAP
                mapView.view?.visibility = View.GONE
                binding.rvEvent.visibility = View.VISIBLE
            }
        }
        val applyButton: Button = findViewById(R.id.btnApply)
        applyButton.setOnClickListener {
            // Merge all criteria maps into the overall criteria map
            criteria.clear()
            criteria.putAll(dateCriteria)
            criteria.putAll(priceCriteria)
            criteria.putAll(isAvailableCriteria)
            // Merge other criteria maps if needed

            // Call the method to fetch events with the updated filter
            fetchEventsFromFirestore(criteria)
        }

    }


    private fun login() {
        val currentUser: String? = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")
        Log.d(TAG, "login: Login status: $currentUser")

        if (currentUser!="") {
            // Update UI
            binding.loginButton.visibility = View.GONE

        } else {
            // User is not logged in, open the login screen
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map ?: return
        isMapReady = true

        // Call the updateMapWithMarkers function here or whenever you want to update the map
        // e.g., updateMapWithMarkers(eventList)

        // Enable user interaction with the map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        // Set up initial camera position (e.g., centered at Toronto, you can change this)
        val toronto = LatLng(43.70, -79.42)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(toronto, 10f))

        // Add sample marker (you need to implement logic to add markers based on your requirements)
        val marker = MarkerOptions()
            .position(toronto)
            .title("Sample Event")
        googleMap.addMarker(marker)

        // Set the custom info window adapter
        val eventList: MutableList<Event> = mutableListOf() // Provide your list of events
        googleMap.setInfoWindowAdapter(MarkerWindowAdapter(this, eventList))

        // Set an OnInfoWindowClickListener to handle click events on the info window
        googleMap.setOnInfoWindowClickListener { clickedMarker ->
            // Extract event ID from the clicked marker's snippet
            val eventID = clickedMarker.snippet
            Log.d(TAG, "$TAG: Marker Clicked - Event ID: $eventID")

            // Open EventDetails activity with the event ID
            val intent = Intent(this, EventDetails::class.java)
            intent.putExtra("eventID", eventID)
            startActivity(intent)
        }
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
        binding.loginButton.visibility = if(userEmail.isEmpty())View.VISIBLE else View.GONE
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onStart(){
        super.onStart()
        // Start listening for changes in the events collection
        eventListener = firestore.collection("Events").addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e(TAG, "Error listening for events: $exception")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val eventList: MutableList<Event> = mutableListOf()
                for (doc in snapshot.documents) {
                    val event = doc.toObject(Event::class.java)
                    if (event != null) {
                        eventList.add(event)
                    }
                }
                // Update the RecyclerView and map with the new event list
                updateUI(eventList, userLocation)
    }}}

    override fun onStop() {
        super.onStop()
        // Stop listening for changes when the activity is stopped
        eventListener?.remove()
    }


    private fun updateUI(eventList: List<Event>, userLocation: LatLng) {
        // Update SearchEventsAdapter with the new event list
        setupRV(eventList)
        adapter.updateList(eventList)

        if (isMapReady) {
            // Update the map with markers
            updateMapWithMarkers(eventList, userLocation)
        }
    }


    private fun fetchEventsFromFirestore(criteria: Map<String, Any>) {
        // Replace "events" with the name of your collection in Firestore
        val eventsCollection = firestore.collection("Events")

        var query: com.google.firebase.firestore.Query = eventsCollection

        // Apply filters based on criteria
        for ((field, value) in criteria) {
            query = when (value) {
                is Double -> query.whereLessThanOrEqualTo(field, value)
                is Boolean -> query.whereEqualTo(field, value)
                is Timestamp -> query.whereLessThanOrEqualTo(field, value)
                /* is Pair<*, *> -> { // Handle date range
                    val startDateField = field + "Start"
                    val endDateField = field + "End"
                    val startDate = value.first as Timestamp
                    val endDate = value.second as Timestamp
                    query.whereGreaterThanOrEqualTo(startDateField, startDate)
                        .whereLessThanOrEqualTo(endDateField, endDate)
                }*/
                else -> {
                    // Handle unsupported data type or do nothing
                    query
                }
            }
        }

        query.get()
            .addOnSuccessListener { documents ->
                val eventList: MutableList<Event> = mutableListOf()

                for (doc in documents) {
                    val event = doc.toObject(Event::class.java)
                    eventList.add(event)
                }

                // Update SearchEventsAdapter with the new event list
                setupRV(eventList)

                if (isMapReady) {
                    updateMapAndRecyclerView(userLocation)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting events from Firestore: $exception")
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

    private fun starIconClicked(pos: Int) {
        val event = adapter.itemList[pos]

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Share Event")
        // Removed, "Twitter", "LinkedIn"
        builder.setItems(arrayOf("Text","Email")) { _, which ->
            when (which) {
                0 -> shareViaText(event)
                1 -> shareViaGmail(event)
                //1 -> shareViaTwitter(event) commented as crashed
                //2 -> shareViaLinkedIn(event) commented as crashed
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun shareIconClicked(pos: Int) {
        val event = adapter.itemList[pos]

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Share Event")
        // Removed, "Twitter", "LinkedIn"
        builder.setItems(arrayOf("Text","Email")) { _, which ->
            when (which) {
                0 -> shareViaText(event)
                1 -> shareViaGmail(event)
                //1 -> shareViaTwitter(event) commented as crashed
                //2 -> shareViaLinkedIn(event) commented as crashed
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun shareViaText(event: Event) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this event: ${event.name}")
        intent.putExtra(Intent.EXTRA_TEXT, getEventDetailsText(event))
        startActivity(Intent.createChooser(intent, "Share via Email"))
    }

    private fun shareViaGmail(event: Event) {
        // Launch email app with pre-populated text
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_SUBJECT, "Event Details")
        intent.putExtra(Intent.EXTRA_TEXT, getEventDetailsText(event))
        startActivity(intent)
    }

    private fun getEventDetailsText(event: Event): String {
        // Implement the logic to create a text containing event details
        return """
Event Name: ${event.name}

Date: ${event.scheduleStart}

Venue: ${event.building}, ${event.street}, ${event.city}, ${event.country}

Description: ${event.description}
    """
    }



    private fun updateMapWithMarkers(eventList: List<Event>, userLocation: LatLng) {
        // Clear existing markers
        googleMap.clear()

        // Add markers for each event in the list
        for (event in eventList) {
            val eventLocation = getLatLngFromAddress(
                event.building,
                event.street,
                event.city,
                event.country
            )

            if (eventLocation != null) {
                val markerOptions = MarkerOptions()
                    .position(eventLocation)
                    .title(event.name)
                    .snippet(event.eventID)

                // Set marker color based on availability
                if (!event.isAvailable) {
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                }

                googleMap.addMarker(markerOptions)


            }
        }

        // center the map at user's location
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, mapZoomLevel))

        // Set the custom info window adapter for the entire GoogleMap
        googleMap.setInfoWindowAdapter(MarkerWindowAdapter(this, eventList.toMutableList()))

    }


    private fun updateMapAndRecyclerView(userLocation: LatLng) {
        // Filter events based on geolocation and sorting option
        adapter.filter(userLocation, 50f, "DefaultSortingOption")

        // Update the map with markers
        updateMapWithMarkers(adapter.itemList, userLocation)

        // You may also want to notify the adapter about the changes
        // searchEventsAdapter.notifyDataSetChanged()
    }



    private fun populateLocationEditText() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location == null) {
                        Log.d(TAG, "Location is null")
                        return@addOnSuccessListener
                    }

                    try {
                        val geocoder = Geocoder(this, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(
                            location.latitude,
                            location.longitude,
                            1
                        )

                        if (addresses.isNullOrEmpty()) {
                            Log.e(TAG, "Geocoding failed. No addresses found.")
                            // Handle the case where no addresses are found
                            return@addOnSuccessListener
                        }

                        val cityName = addresses[0]?.locality
                        if (cityName != null) {
                            binding.etSearchAddress.setText(cityName)
                            searchLocation(cityName)

                            Log.d(TAG, "Current Location: $cityName at ${location.latitude}, ${location.longitude}")
                        } else {
                            Log.e(TAG, "City name is null in the obtained address.")
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error during geocoding: ${e.message}")
                        binding.etSearchAddress.setText("Toronto")
                    }
                }
        }}

    private fun searchLocation(addressFromUI: String){
        val location = getLatLngFromAddress(addressFromUI,"","","")
        if (location != null) {
            userLocation = location
            val message = "Coordinates are: ${location.latitude}, ${location.longitude}"
            Log.d(TAG, message)
            // Perform the logic of searching through DB and displaying UI here
            //binding.tvError.text = "Search results of $message"
            //binding.tvError.visibility = View.VISIBLE
            // Update map and rv based on user input
            updateMapAndRecyclerView(userLocation)
        } else {
            Log.e(TAG, "Geocoding failed. No coordinates found for address: $addressFromUI")
            //binding.tvError.text = "Geocoding failed. Please enter a valid address."
            //binding.tvError.visibility = View.VISIBLE
        }
    }

    private fun getTodayTimestamp(): Any {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time
        return Timestamp(Date(startOfDay.time))
    }

    private fun getThisWeekTimestamp(): Any {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysUntilEndOfWeek = Calendar.SATURDAY - currentDayOfWeek
        val endOfWeekCalendar = calendar.clone() as Calendar
        endOfWeekCalendar.add(Calendar.DAY_OF_MONTH, daysUntilEndOfWeek)
        endOfWeekCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endOfWeekCalendar.set(Calendar.MINUTE, 59)
        endOfWeekCalendar.set(Calendar.SECOND, 59)
        endOfWeekCalendar.set(Calendar.MILLISECOND, 999)
        // Start of the week
        val startOfWeekCalendar = endOfWeekCalendar.clone() as Calendar
        startOfWeekCalendar.add(Calendar.DAY_OF_MONTH, -6) // 7 days in a week - 1 day

        val startOfWeek = Timestamp(Date(startOfWeekCalendar.timeInMillis))
        val endOfWeek = Timestamp(Date(endOfWeekCalendar.timeInMillis))

        return endOfWeek //Pair(startOfMonth, endOfMonth)

    }

    private fun getThisMonthTimestamp(): Any {
        val calendar = Calendar.getInstance()
        val startOfMonthCalendar = calendar.clone() as Calendar
        startOfMonthCalendar.set(Calendar.DAY_OF_MONTH, 1)
        startOfMonthCalendar.set(Calendar.HOUR_OF_DAY, 0)
        startOfMonthCalendar.set(Calendar.MINUTE, 0)
        startOfMonthCalendar.set(Calendar.SECOND, 0)
        startOfMonthCalendar.set(Calendar.MILLISECOND, 0)

        val endOfMonthCalendar = startOfMonthCalendar.clone() as Calendar
        endOfMonthCalendar.add(Calendar.MONTH, 1)
        endOfMonthCalendar.add(Calendar.DAY_OF_MONTH, -1)
        endOfMonthCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endOfMonthCalendar.set(Calendar.MINUTE, 59)
        endOfMonthCalendar.set(Calendar.SECOND, 59)
        endOfMonthCalendar.set(Calendar.MILLISECOND, 999)

        val startOfMonth = Timestamp(Date(startOfMonthCalendar.timeInMillis))
        val endOfMonth = Timestamp(Date(endOfMonthCalendar.timeInMillis))
        return endOfMonth //Pair(startOfMonth, endOfMonth)

    }

    private fun getTimestampFromDate(date: Date): Timestamp {
        return Timestamp(date)
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                // Handle the selected date
                calendar.set(year, month, dayOfMonth)
                val selectedDate = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)
                // Now you can use `selectedDate` in your logic or pass it to fetchEventsFromFirestore
                getTimestampFromDate(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set a minimum date
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000

        datePicker.show()
    }


    private fun initializeSpinner(){
        // Initialize the spinners
        val spinnerDate: Spinner = findViewById(R.id.spinnerDate)
        val spinnerPrice: Spinner = findViewById(R.id.spinnerPrice)
        val spinnerIsAvailable: Spinner = findViewById(R.id.spinnerIsAvailable)
        val applyButton: Button = findViewById(R.id.btnApply)
        //val spinnerCategory: Spinner = findViewById(R.id.spinnerCategory)
        //val spinnerLanguage: Spinner = findViewById(R.id.spinnerLanguage)

        // Set up ArrayAdapter for spinners
        val dateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dateOptions)
        val priceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priceOptions)
        val isAvailableAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, isAvailableOptions)
        //val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryOptions)
        //val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languageOptions)

        // Specify the layout to use when the list of choices appears
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        priceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        isAvailableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Apply the adapters to the spinners
        spinnerDate.adapter = dateAdapter
        spinnerPrice.adapter = priceAdapter
        spinnerIsAvailable.adapter = isAvailableAdapter
        //spinnerCategory.adapter = categoryAdapter
        //spinnerLanguage.adapter = languageAdapter

        setupSpinnerListener(spinnerDate, dateOptions, "scheduleEnd", dateCriteria)
        setupSpinnerListener(spinnerPrice, priceOptions, "price", priceCriteria)
        setupSpinnerListener(spinnerIsAvailable, isAvailableOptions, "isAvailable", isAvailableCriteria)
        // Update the filter criteria based on the selected item
        // You might want to store the selected item in a variable and use it in the fetchEventsFromFirestore method


        // Call the method to fetch events with the updated filter
        fetchEventsFromFirestore(criteria)
    }

    private fun setupSpinnerListener(spinner: Spinner, options: Array<String>, key: String, criteriaMap: MutableMap<String, Any>) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                // Update criteria when spinner value changes
                val selectedValue = options[position]
                criteriaMap.clear() // Clear all criteria

                if (selectedValue != "All") {
                    when (key) {
                        "price" -> criteriaMap[key] = when (selectedValue) {
                            "Free" -> 0.0
                            "Under $30" -> 30.0
                            "Under $50" -> 50.0
                            "Under $150" -> 150.0
                            else -> {onNothingSelected(parentView)}
                        }
                        "isAvailable" -> criteriaMap[key] = when (selectedValue) {
                            "Active" -> true
                            "Inactive" -> false
                            else -> {onNothingSelected(parentView)}
                        }
                        "scheduleEnd" -> criteriaMap[key] = when (selectedValue) {
                            "This Week" -> getThisWeekTimestamp()
                            "This Month" -> getThisMonthTimestamp()
                            "Choose time" -> showDatePickerDialog()
                            else -> {onNothingSelected(parentView)}
                        }
                    }
                }
                // Now, criteria map contains only the selected value or is empty if "All" is selected
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Handle the case where nothing is selected
            }
        }
    }

    private fun setupRV(eventList: List<Event>){
        // Update SearchEventsAdapter with the new event list
        adapter = SearchEventsAdapter(
            eventList,
            shareIconHandler = { pos: Int -> shareIconClicked(pos) },
            starIconHandler = { pos: Int -> starIconClicked(pos) },

            )

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter.notifyDataSetChanged()
    }


}