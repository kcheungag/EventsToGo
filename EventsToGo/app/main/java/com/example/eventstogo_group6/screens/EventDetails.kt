package com.example.eventstogo_group6.screens

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.eventstogo_group6.R
import com.google.firebase.firestore.FirebaseFirestore
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.databinding.ActivityEventDetailsBinding
import com.example.eventstogo_group6.ui.StaticWindowAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.text.SimpleDateFormat
import java.util.*
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import com.example.eventstogo_group6.api.UpdateCallback
import com.example.eventstogo_group6.database.EventRepository
import com.example.eventstogo_group6.database.FavouriteEventsRepository
import com.example.eventstogo_group6.database.FavouriteOrganizerRepository
import com.example.eventstogo_group6.database.UserRepository
import com.example.eventstogo_group6.enums.SharedPrefRef
import com.example.eventstogo_group6.models.BookmarkedEvent
import com.example.eventstogo_group6.models.FavouriteOrganizer
import com.example.eventstogo_group6.ui.EmailUtils
import com.squareup.picasso.Picasso


class EventDetails : AppCompatActivity(), OnMapReadyCallback, UpdateCallback {
    private val TAG: String = this@EventDetails.toString()
    private lateinit var binding: ActivityEventDetailsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private var event: Event? = null
    private var isBookmarked: Boolean = false
    private var isOrganizerFollowed: Boolean = false
    private val eventRepository = EventRepository(this)
    private val favouriteEventsRepository = FavouriteEventsRepository(this)
    private val favouriteOrganizerRepository = FavouriteOrganizerRepository(this)
    private var isUpdateInProgress: Boolean = false
    private var isAddingBookmark: Boolean = false


    override fun onUpdateComplete() {
        // This method will be called after each update operation is complete
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(SharedPrefRef.SHARED_PREF_NAME.value, MODE_PRIVATE)
        // Retrieve the current user's email from SharedPreferences
        val currentUserEmail = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")

        if (currentUserEmail == "") {
            // User is not logged in or email is not available
            binding.btnReserve.visibility = View.GONE
            binding.btnFakeReserve.text = getString(R.string.fake_reserve)
            binding.btnFakeReserve.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
                return@setOnClickListener
            }

        } else {
            // User is logged in, you can use currentUserEmail as needed
            binding.btnFakeReserve.visibility = View.GONE
            binding.btnReserve.text = getString(R.string.real_reserve)
            binding.btnReserve.setOnClickListener {
                showReservationDialog()
            }
        }

        // Initialize mapview
        /*mapView = findViewById(R.id.mapViewEventDetails)
            mapView.getMapAsync(this)*/

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

            // Check if need to update UI
            checkBookmarkState(eventID)
            checkFollowState(eventID)
        } else {
            // Handle the case where eventId is null or empty
            // You can show an error message or navigate back to the previous screen
            Log.d("EventDetails", "Empty Intent Data Get.")
        }

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        binding.ivStar.setOnClickListener{
            updateFavouriteEvent()
        }
        binding.ivOrganiser.setOnClickListener{
            contactOrganizer()
        }
        binding.ivShare.setOnClickListener{
            showShareDialog()
        }
        binding.btnFollow.setOnClickListener{
            // Perform shortlist-eventOrganiser logic here
            setupFollowButton()
        }
        binding.btnReserve.setOnClickListener {
            showReservationDialog()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

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
        refreshUI()
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
    // Use the EventRepository to fetch event details by ID
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
        Picasso.with(this).load(event.image).placeholder(getDrawable(R.drawable.ic_launcher_background)).into(binding.ivPropertyImage)



        val salesAd = event.advertising

        if(event.isAvailable){if(salesAd != ""){
            binding.tvSales.text = salesAd
        }else{
            binding.tvSales.text = getString(R.string.isAvailable_true)
        }}else{
            binding.tvSales.text = getString(R.string.isAvailable_false)
        }

        if (event.price != 0.0){
            binding.tvPrice.text = "Price: $${event.price}"
            binding.tvRefundPolicy.text = getString(R.string.policy_paid)
        } else {
            binding.tvPrice.text = getString(R.string.free)
            binding.tvRefundPolicy.text = getString(R.string.policy_free)
        }

        binding.tvQuantity.text = "Quantity remained: ${event.numberOfSlots}"

        binding.tvEventName.text = event.name
        val name = EmailUtils.extractNameFromEmail(event.organizerEmail)
        binding.tvOrganiserName.text = name

        binding.tvEventVenue.text = event.building
        binding.tvEventAddress.text = "${event.street}, ${event.city}, ${event.country}"
        // Convert datestamp to a readable date format
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val startDate = dateFormat.format(event.scheduleStart)
        val endDate = dateFormat.format(event.scheduleEnd)
        binding.tvEventDate.text = "From: $startDate"
        binding.tvEventTime.text = "Till: $endDate"
        binding.tvEventDescription.text = event.description
        // Add similar code to populate other views with event details
        Log.d(TAG, "Populated UI with event details: $event")

        if(!event.isAvailable){
            binding.btnReserve.visibility = View.INVISIBLE
        }

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

    private fun getEventDetailsText(): String {
        // Generate the text containing event details
        val eventName = binding.tvEventName.text
        val eventDate = binding.tvEventDate.text
        val eventPlace = binding.tvEventAddress.text
        val eventDetails = binding.tvEventDescription.text

        return "Check out this event: $eventName\n\n $eventDate \n\n Venue: $eventPlace \n\n$eventDetails"
    }

    private fun showShareDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Share Event")
        //Removed, "Twitter", "LinkedIn"
        builder.setItems(arrayOf("Text","Email")) { _, which ->
            when (which) {
                0 -> shareViaText()
                1 -> shareViaEmail()
                //1 -> shareViaTwitter() commented as crashed, may need api or install app
                //2 -> shareViaLinkedIn() commented as crashed
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun shareViaText() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, "Check out this event:")
        intent.putExtra(Intent.EXTRA_TEXT, getEventDetailsText())
        startActivity(Intent.createChooser(intent, "Share via Email"))
    }

    private fun shareViaEmail() {
        // Launch email app with pre-populated text
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_SUBJECT, "Event Details")
        intent.putExtra(Intent.EXTRA_TEXT, getEventDetailsText())
        startActivity(intent)
    }

    private fun contactOrganizer() {

        val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:${binding.tvOrganiserName.text}@gmail.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "Event Inquiry")
            intent.putExtra(Intent.EXTRA_TEXT, "Dear Organiser,\n I am writing to enquire about the event - ${binding.tvEventName.text}...")
            startActivity(intent)

    }

    private fun showReservationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reserve, null)

        val etShippingAddress = dialogView.findViewById<EditText>(R.id.etShippingAddress)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etQuantity)
        val tvError = dialogView.findViewById<TextView>(R.id.tv_error)
        val tvSuccess = dialogView.findViewById<TextView>(R.id.tv_success)


        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Reserve Event")
        alertDialogBuilder.setCancelable(true)

        alertDialogBuilder.setPositiveButton("Submit") { dialog, _ ->
            val shippingAddress = etShippingAddress.text.toString()
            val quantity = etQuantity.text.toString()

            // Validate input (you may want to add more validation)
            if (shippingAddress.isNotEmpty() && quantity.isNotEmpty()) {
                val maxQuantity = binding.tvQuantity.text.toString().replace("Quantity remained: ", "").toInt()

                // Check if the entered quantity is valid
                if (quantity.toInt() in 1..maxQuantity) {
                    // Call a function to handle the submission
                    submitReservation(shippingAddress, quantity)

            } else {
                    // Show an error message or handle invalid input
                   val message = "Invalid quantity, please enter a value between 1 and $maxQuantity."
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()


                }} else {
                // Show an error message or handle invalid input
                val message = "Invalid input, please try again."
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            }
        }

        alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
            // Dismiss the dialog on cancel
            dialog.dismiss()
        }

        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun submitReservation(shippingAddress: String, quantity: String) {
        // Extract event ID from the intent data
        val eventID = intent.getStringExtra("eventID")

        val currentUserEmail = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")


        // Create a new document in the "collection_Orders" Firestore collection
        val ordersCollection = FirebaseFirestore.getInstance().collection("Registered Events")
        val registrationID = UUID.randomUUID().toString()
        val reservationData = hashMapOf(
            "registrationID" to registrationID,
            "eventID" to eventID,
            "userEmail" to currentUserEmail,
            "shippingAddress" to shippingAddress,
            "quantity" to quantity.toInt()
        )

        ordersCollection.document(registrationID).set(reservationData)
            .addOnSuccessListener {
                // Handle success, e.g., show a success message
                Log.d(TAG, "Reservation Order Added to DB")
                updateNumberOfSlots(eventID ?: "", quantity.toInt())

            }
            .addOnFailureListener {
                // Handle failure, e.g., show an error message
                Log.d(TAG, "Reservation Order WAS NOT Added to DB")
            }
    }
    private fun updateNumberOfSlots(eventID: String, quantity: Int) {
        // Retrieve the event document from Firestore
        firestore.collection("Events")
            .document(eventID)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Document exists, update the numberOfSlots
                    val event = documentSnapshot.toObject(Event::class.java)

                    // Update the numberOfSlots locally
                    event?.let {
                        it.numberOfSlots -= quantity

                        // Check if numberOfSlots is less than or equal to 0
                        if (it.numberOfSlots <= 0) {
                            it.isAvailable = false
                        }

                        // Update the numberOfSlots in Firestore
                        firestore.collection("Events")
                            .document(eventID)
                            .set(it)
                            .addOnSuccessListener {
                                // Successfully updated numberOfSlots in Firestore
                                val message = "Order Completed Successfully: $quantity tickets of ${event.name} are reserved"
                                Log.d(TAG, "Inventory minus $quantity become ${event.numberOfSlots} ")
                                // You can update your local UI or perform other actions here
                                fetchEventDetails(eventID)
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                // Handle the failure case
                                Log.d(TAG, "Order Added - $eventID has not updated quantity")

                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                // Handle the failure case
                Log.d(TAG, "Order Added - $e")

            }
    }
    private fun updateFavouriteEvent() {
        // Check if an update operation is already in progress
        if (isUpdateInProgress) {
            return
        }
        isUpdateInProgress = true

        val favouriteEventsRepository = FavouriteEventsRepository(this)
        val userRepository = UserRepository(this)

        // Extract event ID from the intent data
        val eventID = intent.getStringExtra("eventID")
        val currentUserEmail = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")

        // Check if the user is logged in
        if (currentUserEmail != "") {
            // Check if the event is bookmarked
            favouriteEventsRepository.getFavouriteEvents(currentUserEmail!!)
                .observe(this) { favouriteEvents ->
                    isBookmarked = favouriteEvents.any { it.eventID == eventID }
                    if (!isBookmarked && !isAddingBookmark) {
                        // Event is not bookmarked, and we are not in the process of adding a bookmark
                        // You can access bookmarkID if needed
                        val bookmarkedEvent = favouriteEvents.find { it.eventID == eventID }
                        if (bookmarkedEvent != null) {
                            //removeBookmarked(bookmarkedEvent.bookmarkID)
                            Toast.makeText(this, "You have already bookmarked this event", Toast.LENGTH_SHORT).show()
                        } else {
                            // Not bookmarked, add the bookmark
                            addBookmarked(eventID, currentUserEmail)
                        }
                    }
                }
        } else {
            // User is not logged in, show a message or redirect to the login screen
            // For example, you can display a Toast message
            Toast.makeText(this, "Please log in to bookmark events", Toast.LENGTH_SHORT).show()
            // Alternatively, you can navigate to the login screen
            // startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun addBookmarked(eventID: String?, userEmail: String) {
        if (!isBookmarked) {
            binding.ivStar.setImageResource(R.drawable.ic_star_filled)
            isAddingBookmark = true // Set the flag to indicate that we are adding a bookmark
        }

        val newFavouriteEvent = eventID?.let {
            BookmarkedEvent(
                UUID.randomUUID().toString(), it,
                userEmail
            )
        }

        if (newFavouriteEvent != null) {
            favouriteEventsRepository.addFavouriteEventToDB(newFavouriteEvent, this)
        }
        handleUpdateComplete()
    }

    // Function to handle update completion
    private fun handleUpdateComplete() {
        Toast.makeText(this, "Bookmark event successfully", Toast.LENGTH_SHORT).show()
        // Use a Handler to delay the execution of the update UI code
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            isUpdateInProgress = false
            isAddingBookmark = false // Reset the flag
        }, 500) // Adjust the delay time as needed
    }


    private fun checkBookmarkState(eventID: String?) {
        // Check if the user is logged in
        val currentUserEmail = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")

        if (currentUserEmail != "") {
            // Check if the event is bookmarked
            favouriteEventsRepository.getFavouriteEvents(currentUserEmail!!)
                .observe(this) { favouriteEvents ->
                    isBookmarked = favouriteEvents.any { it.eventID == eventID }
                    updateBookmarkIcon()
                }
        }
    }

    private fun updateBookmarkIcon() {
        if (isBookmarked) {
            binding.ivStar.setImageResource(R.drawable.ic_star_filled)
        } else {
            binding.ivStar.setImageResource(R.drawable.ic_star)
        }
    }

    private fun setupFollowButton() {
        // Extract event organizer's email and current user's email from your event details
        val organizerEmail = EmailUtils.createEmailFromName(binding.tvOrganiserName.text.toString())
        val currentUserEmail = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")

        // Check if the user is logged in
        if (currentUserEmail != "") {
            // Check if the organizer is already followed
            favouriteOrganizerRepository.getFavouriteOrganizers(currentUserEmail!!)
                .observe(this) { favouriteOrganizers ->
                    isOrganizerFollowed = favouriteOrganizers.any { it.organizerEmail == organizerEmail }

                    if (isOrganizerFollowed) {
                        // Organizer is already followed, you can show a message or handle accordingly
                        binding.btnFollow.text = "Followed"
                    } else {
                        // Organizer is not followed, create a new FavouriteOrganizer instance
                        val newFavouriteOrganizer = FavouriteOrganizer(
                            UUID.randomUUID().toString(), // Generate a unique favorite ID
                            organizerEmail,
                            currentUserEmail
                        )

                        // Add the new favorite organizer
                        favouriteOrganizerRepository.addFavouriteOrganizerToDB(newFavouriteOrganizer, this)

                        // Handle success, for example, update UI or show a success message
                        Toast.makeText(this, "Now following the organizer", Toast.LENGTH_SHORT).show()
                        binding.btnFollow.text = "Followed"
                        binding.btnFollow.isClickable = false
                    }
                    }
                }
         else {
            // User is not logged in, show a message or redirect to the login screen
            Toast.makeText(this, "Please log in to follow organizers", Toast.LENGTH_SHORT).show()
            // Alternatively, you can navigate to the login screen
            // startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun checkFollowState(eventID: String?) {
        // Check if the user is logged in
        val currentUserEmail = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")

        if (currentUserEmail != "" && eventID != null) {
            // Retrieve event details from the EventRepository
            eventRepository.fetchEventByID(eventID)
                .observe(this) { event ->
                    // Check if the organizer is followed
                    event?.let {
                        val organizerEmail = EmailUtils.createEmailFromName(binding.tvOrganiserName.text.toString())

                        // Retrieve the list of followed organizers from the FavouriteOrganizersRepository
                        favouriteOrganizerRepository.getFavouriteOrganizers(currentUserEmail!!)
                            .observe(this) { favouriteOrganizers ->
                                isOrganizerFollowed = favouriteOrganizers.any { it.organizerEmail == organizerEmail }
                                updateFollowButton()
                            }
                    }
                }
        }
    }

    private fun updateFollowButton() {
        if (isOrganizerFollowed) {
            // Organizer is followed, update the UI
            binding.btnFollow.text = "Followed"
            binding.btnFollow.isClickable = false // Make the button not clickable
        }
        // Do nothing if the organizer is not followed (button not pressed)
    }

    private fun refreshUI() {
        // Retrieve the current user's email from SharedPreferences
        val currentUserEmail = sharedPreferences.getString(SharedPrefRef.CURRENT_USER.value, "")

        if (currentUserEmail.isNullOrEmpty()) {
            // User is not logged in or email is not available
            binding.btnReserve.visibility = View.GONE
            binding.btnFakeReserve.visibility = View.VISIBLE
            binding.btnFakeReserve.text = getString(R.string.fake_reserve)
            binding.btnFakeReserve.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }

            // Hide or update other UI elements as needed
        } else {
            // User is logged in, update UI accordingly
            binding.btnFakeReserve.visibility = View.GONE
            binding.btnReserve.visibility = View.VISIBLE
            binding.btnReserve.text = getString(R.string.real_reserve)
            binding.btnReserve.setOnClickListener {
                showReservationDialog()
            }

            // Update star icon UI
            updateStarIconUI()

            // Show or update other UI elements as needed
            updateFollowButtonUI()
        }
    }

    private fun updateStarIconUI() {
        // Update star icon based on bookmark state
        if (isBookmarked) {
            binding.ivStar.setImageResource(R.drawable.ic_star_filled)
        } else {
            binding.ivStar.setImageResource(R.drawable.ic_star)
        }
    }

    private fun updateFollowButtonUI() {
        // Update follow button based on organizer follow state
        if (isOrganizerFollowed) {
            binding.btnFollow.text = "Followed"
            binding.btnFollow.isClickable = false // Make the button not clickable
        } else {
            binding.btnFollow.text = "Follow" // Set to the default text if not followed
            binding.btnFollow.isClickable = true  // Make the button clickable
        }
    }



}

