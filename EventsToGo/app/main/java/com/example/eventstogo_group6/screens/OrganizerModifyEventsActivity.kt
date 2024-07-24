package com.example.eventstogo_group6.screens

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.eventstogo_group6.R
import com.example.eventstogo_group6.database.EventRepository
import com.example.eventstogo_group6.databinding.ActivityOrganizerModifyEventsBinding
import com.example.eventstogo_group6.enums.ExtrasRef
import com.example.eventstogo_group6.models.Event
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.integrity.internal.c
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class OrganizerModifyEventsActivity : AppCompatActivity(), OnClickListener {
    private val TAG = this.toString()
    private lateinit var binding: ActivityOrganizerModifyEventsBinding

    private lateinit var eventRepository: EventRepository

    private lateinit var currEvent: Event
    private var isUpdate = false
    private var currentUser = ""
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2

    private lateinit var filePhoto: File
    private var fileName = "temp_"

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted: Boolean ->
            if (permissionGranted) {
                try {
                    val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val providerFile = FileProvider.getUriForFile(
                        this, "com.example.eventstogo_group6.fileprovider", filePhoto
                    )
                    takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
                    takePhotoIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE)

                    val galleryIntent = Intent(Intent.ACTION_PICK, providerFile)
                    galleryIntent.setAction(Intent.ACTION_PICK)

                    val chooserIntent = Intent.createChooser(galleryIntent, "Select Source")
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(takePhotoIntent))

                    startActivityForResult(chooserIntent, REQUEST_IMAGE_CAPTURE)
                } catch (ex: Exception) {
                    Log.e(TAG, "permissionsLauncher: $ex")
                }
            } else {
                Log.e(TAG, "permissionsLauncher: RESTRICTED")
                Snackbar.make(
                    binding.root,
                    "Please allow the app to access the camera in the device Settings",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

    private fun getPhotoFile(fName: String): File {
        val directoryStorage = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fName, ".jpg", directoryStorage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityOrganizerModifyEventsBinding.inflate(layoutInflater)
        setContentView(this.binding.root)

        setSupportActionBar(this.binding.menuToolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)

        binding.etEStartScheduleDate.setOnClickListener(this)
        binding.etEStartScheduleTime.setOnClickListener(this)
        binding.etEStartScheduleDate.keyListener = null
        binding.etEStartScheduleTime.keyListener = null

        binding.etEEndScheduleDate.setOnClickListener(this)
        binding.etEEndScheduleTime.setOnClickListener(this)
        binding.etEEndScheduleDate.keyListener = null
        binding.etEEndScheduleTime.keyListener = null


        binding.btnEAddEvent.setOnClickListener(this)
        binding.btnEUpdateEvent.setOnClickListener(this)
        binding.btnEDeleteEvent.setOnClickListener(this)
        binding.btnEOpenCamera.setOnClickListener(this)

        if (intent != null) {
            isUpdate = intent.getBooleanExtra(ExtrasRef.IS_EVENT_UPDATE.toString(), false)
            currentUser = intent.getStringExtra(ExtrasRef.CURRENT_USER.toString()).toString()

            if (isUpdate) initForUpdate()
        }

        filePhoto = getPhotoFile(fileName)

        eventRepository = EventRepository(applicationContext)
    }

    private fun initForUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            currEvent =
                intent.getSerializableExtra(ExtrasRef.CURRENT_EVENT.toString(), Event::class.java)!!
        } else {
            currEvent = intent.getSerializableExtra(ExtrasRef.CURRENT_EVENT.toString()) as Event
        }
        fileName = "${currEvent.eventID}.jpg"

        supportActionBar?.setTitle("Update Event")

        binding.btnEAddEvent.visibility = View.GONE

        binding.swEAvailability.visibility = View.VISIBLE
        binding.tvEAvailability.visibility = View.VISIBLE
        binding.btnEUpdateEvent.visibility = View.VISIBLE

        val startDate = currEvent.scheduleStart
        val endDate = currEvent.scheduleEnd

        val dateFormat = SimpleDateFormat("MM/dd/yyyy")
        val timeFormat = SimpleDateFormat("HH:mm")

        binding.etEName.setText(currEvent.name)
        binding.etEAdDisplay.setText(currEvent.advertising)
        binding.etEDescription.setText(currEvent.description)
        binding.etEStartScheduleDate.setText(dateFormat.format(startDate))
        binding.etEStartScheduleTime.setText(timeFormat.format(startDate))
        binding.etEEndScheduleDate.setText(dateFormat.format(endDate))
        binding.etEEndScheduleTime.setText(timeFormat.format(endDate))
        binding.etEPrice.setText(currEvent.price.toString())
        binding.etESlots.setText(currEvent.numberOfSlots.toString())
        binding.etEBuilding.setText(currEvent.building)
        binding.etEStreet.setText(currEvent.street)
        binding.etECity.setText(currEvent.city)
        binding.etECountry.setText(currEvent.country)
        binding.etEImage.setText(currEvent.image)
        binding.swEAvailability.isChecked = currEvent.isAvailable

        try {
            Picasso.with(this).load(currEvent.image)
                .placeholder(getDrawable(R.drawable.ic_launcher_background)).into(binding.imgEImage)
        } catch (ex: Exception) {
            Log.e(TAG, "refreshUI: $ex")
            binding.imgEImage.setImageDrawable(getDrawable(R.drawable.ic_launcher_background))
        }
    }

    override fun onClick(v: View?) {
        when (v!!) {
            binding.etEStartScheduleDate -> {
                showDatePicker(binding.etEStartScheduleDate)
            }

            binding.etEStartScheduleTime -> {
                showTimePicker(binding.etEStartScheduleTime)
            }

            binding.etEEndScheduleDate -> {
                showDatePicker(binding.etEEndScheduleDate)
            }

            binding.etEEndScheduleTime -> {
                showTimePicker(binding.etEEndScheduleTime)
            }

            binding.btnEAddEvent -> {
                if (validateFields()) {
                    val startDate = binding.etEStartScheduleDate.text.toString()
                    val startTime = binding.etEStartScheduleTime.text.toString()
                    val endDate = binding.etEEndScheduleDate.text.toString()
                    val endTime = binding.etEEndScheduleTime.text.toString()

                    val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm")
                    val startDT = sdf.parse("$startDate $startTime")
                    val endDT = sdf.parse("$endDate $endTime")

                    val bmd = binding.imgEImage.drawable as BitmapDrawable
                    val imageBitmap = bmd.bitmap
//                    val imageBitmap = BitmapFactory.decodeFile(filePhoto.absolutePath)
                    val baos = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val image = baos.toByteArray()

                    eventRepository.addEvent(
                        Event(
                            organizerEmail = currentUser,
                            image = binding.etEImage.text.toString(),
                            name = binding.etEName.text.toString(),
                            description = binding.etEDescription.text.toString(),
                            street = binding.etEStreet.text.toString(),
                            building = binding.etEBuilding.text.toString(),
                            city = binding.etECity.text.toString(),
                            country = binding.etECountry.text.toString(),
                            scheduleStart = startDT!!,
                            scheduleEnd = endDT!!,
                            numberOfSlots = binding.etESlots.text.toString().toInt(),
                            price = binding.etEPrice.text.toString().toDouble(),
                            isAvailable = true,
                            advertising = binding.etEAdDisplay.text.toString()
                        ), image
                    )
                    finish()
                }
            }

            binding.btnEUpdateEvent -> {
                if (validateFields()) {
                    val startDate = binding.etEStartScheduleDate.text.toString()
                    val startTime = binding.etEStartScheduleTime.text.toString()
                    val endDate = binding.etEEndScheduleDate.text.toString()
                    val endTime = binding.etEEndScheduleTime.text.toString()

                    val sdf = SimpleDateFormat("MM/dd/yyyy HH:mm")
                    val startDT = sdf.parse("$startDate $startTime")
                    val endDT = sdf.parse("$endDate $endTime")

                    currEvent.image = binding.etEImage.text.toString()
                    currEvent.name = binding.etEName.text.toString()
                    currEvent.description = binding.etEDescription.text.toString()
                    currEvent.street = binding.etEStreet.text.toString()
                    currEvent.building = binding.etEBuilding.text.toString()
                    currEvent.city = binding.etECity.text.toString()
                    currEvent.country = binding.etECountry.text.toString()
                    currEvent.scheduleStart = startDT!!
                    currEvent.scheduleEnd = endDT!!
                    currEvent.numberOfSlots = binding.etESlots.text.toString().toInt()
                    currEvent.price = binding.etEPrice.text.toString().toDouble()
                    currEvent.isAvailable = binding.swEAvailability.isChecked
                    currEvent.advertising = binding.etEAdDisplay.text.toString()

                    val bmd = binding.imgEImage.drawable as BitmapDrawable
                    val imageBitmap = bmd.bitmap
//                    val imageBitmap = BitmapFactory.decodeFile(filePhoto.absolutePath)
                    val baos = ByteArrayOutputStream()
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                    val image = baos.toByteArray()

                    eventRepository.updateEvent(currEvent, image)

                    intent.putExtra(ExtrasRef.CURRENT_EVENT.toString(), currEvent)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                }
            }

            binding.btnEDeleteEvent -> {
                val builder = AlertDialog.Builder(this@OrganizerModifyEventsActivity)
                builder.setMessage("Are you sure you want to delete this event?")
                    .setCancelable(false).setNegativeButton("Yes") { dialog, id ->
                        eventRepository.delEvent(currEvent)
                        finish()
                    }.setPositiveButton("NO") { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.getButton(DialogInterface.BUTTON_POSITIVE)
                    ?.setTextColor(getColor(R.color.red))
                alert.getButton(DialogInterface.BUTTON_NEGATIVE)
                    ?.setTextColor(getColor(R.color.green))
                alert.show()
            }

            binding.btnEOpenCamera -> {
                permissionsLauncher.launch(android.Manifest.permission.CAMERA)
            }
        }
    }

    private fun showDatePicker(et: EditText) {
        val calendar: Calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val datePickerDialog = DatePickerDialog(
            this@OrganizerModifyEventsActivity, { view, year, monthOfYear, dayOfMonth ->
                et.error = null
                et.setText("${padZero(monthOfYear + 1)}/${padZero(dayOfMonth)}/$year")
            }, year, month, day
        )

        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun showTimePicker(et: EditText) {
        val timePickerDialog = TimePickerDialog(
            this@OrganizerModifyEventsActivity, { view, hour, mins ->
                et.error = null
                et.setText("${padZero(hour)}:${padZero(mins)}")
            }, 0, 0, false
        )
        timePickerDialog.show()
    }

    private fun padZero(num: Int): String {
        return num.toString().padStart(2, '0')
    }

    private fun validateFields(): Boolean {
        binding.btnEOpenCamera.setBackgroundColor(getColor(R.color.themeColor))
        binding.tvEImageLbl.visibility = View.GONE

        var isValid = true
        isValid = checkIfFilled(binding.etEName) && isValid
        isValid = checkIfFilled(binding.etEAdDisplay) && isValid
        isValid = checkIfFilled(binding.etEDescription) && isValid
        isValid =
            checkIfDateTime(binding.etEStartScheduleDate, binding.etEStartScheduleTime) && isValid
        isValid = checkIfDateTime(binding.etEEndScheduleDate, binding.etEEndScheduleTime) && isValid
        isValid = checkDateTimeRange(
            binding.etEStartScheduleDate,
            binding.etEStartScheduleTime,
            binding.etEEndScheduleDate,
            binding.etEEndScheduleTime
        )
        isValid = checkAddress(
            binding.etEStreet, binding.etEBuilding, binding.etECity, binding.etECountry
        ) && isValid
        isValid = checkIfFilled(binding.etEPrice) && isValid
        isValid = checkIfFilled(binding.etESlots) && isValid
        isValid = checkIfFilled(binding.etEImage) && isValid
        return isValid
    }

    private fun checkIfFilled(et: EditText): Boolean {
        val isNullOrEmpty = et.text.isNullOrEmpty()
        if (et.text.isNullOrEmpty()) {
            et.error = "Required"
            if (et.id == R.id.etEImage) {
                binding.btnEOpenCamera.setBackgroundColor(getColor(R.color.red))
                binding.tvEImageLbl.visibility = View.VISIBLE
            }
        }
        return !isNullOrEmpty
    }

    private fun checkIfDateTime(etDate: EditText, etTime: EditText): Boolean {
        val isDateFilled = !checkIfFilled(etDate)
        val isTimeFilled = !checkIfFilled(etTime)
        if (isDateFilled || isTimeFilled) return false

        var isValid = true
        try {
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            LocalDate.parse(etDate.text, formatter)
        } catch (ex: Exception) {
            etDate.error = "Invalid Value"
            isValid = false
        }
        try {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            LocalTime.parse(etTime.text, formatter)
        } catch (ex: Exception) {
            etTime.error = "Invalid Value"
            isValid = false
        }

        return isValid
    }

    private fun checkDateTimeRange(
        etStartDate: EditText, etStartTime: EditText, etEndDate: EditText, etEndTime: EditText
    ): Boolean {
        val startDate = etStartDate.text.toString()
        val startTime = etStartTime.text.toString()
        val endDate = etEndDate.text.toString()
        val endTime = etEndTime.text.toString()

        return try {
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm")
            val startDT = LocalDateTime.parse("$startDate $startTime", formatter)
            val endDT = LocalDateTime.parse("$endDate $endTime", formatter)

            etEndDate.error = null
            etEndTime.error = null

            if (endDT <= startDT) {
                etEndDate.error = "Check start date/time"
                etEndTime.error = "Check start date/time"
            }

            endDT > startDT
        } catch (ex: Exception) {
            Log.e(TAG, "checkDateTimeRange: $ex")
            false
        }
    }

    private fun checkAddress(
        etStreet: EditText, etBuilding: EditText, etCity: EditText, etCountry: EditText
    ): Boolean {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val address = "${etBuilding} ${etStreet.text} ${etCity.text} ${etCountry.text}".trim()
            val result = geocoder.getFromLocationName(address, 1) ?: throw IllegalArgumentException(
                "Empty Result"
            )
            val location = result[0]

            etCity.text.clear()
            etCountry.text.clear()

            etCity.setText(location.locality)
            etCountry.setText(location.countryName)

            etStreet.error = null
            etBuilding.error = null
            etCity.error = null
            etCountry.error = null

            var isValid = true
            isValid = checkIfFilled(etStreet)
            isValid = checkIfFilled(etBuilding) && isValid
            isValid = checkIfFilled(etCity) && isValid
            isValid = checkIfFilled(etCountry) && isValid

            isValid
        } catch (ex: Exception) {
            etStreet.error = "Invalid Address"
            etBuilding.error = "Invalid Address"
            etCity.error = "Invalid Address"
            etCountry.error = "Invalid Address"
            Log.e(TAG, "checkAddress: $ex")
            false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                if(data.data == null) {
                    val takenPhoto = BitmapFactory.decodeFile(filePhoto.absolutePath)
                    binding.imgEImage.setImageBitmap(takenPhoto)
                } else {
                    binding.imgEImage.setImageURI(data.data)
//                    ImageDecoder.createSource(this.contentResolver, data.data!!)
//                    binding.imgEImage.setImageBitmap(MediaStore.Images.Media.getBitmap(this.contentResolver, data.data!!))
                }

                binding.btnEOpenCamera.setBackgroundColor(getColor(R.color.themeColor))
                binding.tvEImageLbl.visibility = View.GONE
                binding.etEImage.setText("IMAGE")
            } else {
                val takenPhoto = BitmapFactory.decodeFile(filePhoto.absolutePath)
                binding.imgEImage.setImageBitmap(takenPhoto)
                binding.btnEOpenCamera.setBackgroundColor(getColor(R.color.themeColor))
                binding.tvEImageLbl.visibility = View.GONE
                binding.etEImage.setText("IMAGE")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            Snackbar.make(
                binding.root, "Failed to take a photo. Please try again.", Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}