package com.example.eventstogo_group6.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.eventstogo_group6.models.Event
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

class EventRepository(private val context: Context) {
    private val TAG = this.toString()
    private val db = Firebase.firestore
    private val storage = Firebase.storage.reference

    private val COLLECTION_EVENTS = "Events"
    private val FIELD_EVENTID = "eventID"
    private val FIELD_ORGANIZEREMAIL = "organizerEmail"
    private val FIELD_IMAGE = "image"
    private val FIELD_NAME = "name"
    private val FIELD_DESCRIPTION = "description"
    private val FIELD_STREET = "street"
    private val FIELD_BUILDING = "building"
    private val FIELD_CITY = "city"
    private val FIELD_COUNTRY = "country"
    private val FIELD_SCHEDULE_START = "scheduleStart"
    private val FIELD_SCHEDULE_END = "scheduleEnd"
    private val FIELD_NUMBEROFSLOTS = "numberOfSlots"
    private val FIELD_PRICE = "price"
    private val FIELD_ISAVAILABLE = "isAvailable"
    private val FIELD_ADVERTISING = "advertising"

    var allEvents: MutableLiveData<List<Event>> = MutableLiveData<List<Event>>()
    var updatedImageURL: MutableLiveData<String> = MutableLiveData<String>()

    private val singleEvent: MutableLiveData<Event> = MutableLiveData()

    fun addEvent(event: Event, image: ByteArray) {
        val data: MutableMap<String, Any> = HashMap();
        data[FIELD_EVENTID] = event.eventID
        data[FIELD_ORGANIZEREMAIL] = event.organizerEmail
        data[FIELD_NAME] = event.name
        data[FIELD_DESCRIPTION] = event.description
        data[FIELD_STREET] = event.street
        data[FIELD_BUILDING] = event.building
        data[FIELD_CITY] = event.city
        data[FIELD_COUNTRY] = event.country
        data[FIELD_SCHEDULE_START] = event.scheduleStart
        data[FIELD_SCHEDULE_END] = event.scheduleEnd
        data[FIELD_NUMBEROFSLOTS] = event.numberOfSlots
        data[FIELD_PRICE] = event.price
        data[FIELD_ISAVAILABLE] = event.isAvailable
        data[FIELD_ADVERTISING] = event.advertising

        val storageRef = storage.child("images").child("${event.eventID}.jpg")
        storageRef.putBytes(image).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG, "uploadImage: $uri")
                    try {
                        val downloadUrl = uri.toString()
                        data[FIELD_IMAGE] = downloadUrl
                        db.collection(COLLECTION_EVENTS).document(event.eventID).set(data)
                            .addOnSuccessListener {
                                Log.d(TAG, "addEvent: ${event.eventID} successfully added")
                            }.addOnFailureListener { ex ->
                                Log.e(TAG, "addEvent: $ex")
                            }
                    } catch (ex: Exception) {
                        Log.e(TAG, "addEvent: $ex")
                    }
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "uploadImage: $ex")
                }
        }
    }

    fun delEvent(event: Event) {
        try {
            db.collection(COLLECTION_EVENTS).document(event.eventID).delete().addOnSuccessListener {
                Log.d(TAG, "delEvent: ${event.eventID} successfully deleted")
            }.addOnFailureListener { ex ->
                Log.e(TAG, "delEvent: $ex")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "delEvent: $ex")
        }
    }

    fun updateEvent(event: Event, image: ByteArray) {
        val data: MutableMap<String, Any> = HashMap();
        data[FIELD_EVENTID] = event.eventID
        data[FIELD_ORGANIZEREMAIL] = event.organizerEmail
        data[FIELD_NAME] = event.name
        data[FIELD_DESCRIPTION] = event.description
        data[FIELD_STREET] = event.street
        data[FIELD_BUILDING] = event.building
        data[FIELD_CITY] = event.city
        data[FIELD_COUNTRY] = event.country
        data[FIELD_SCHEDULE_START] = event.scheduleStart
        data[FIELD_SCHEDULE_END] = event.scheduleEnd
        data[FIELD_NUMBEROFSLOTS] = event.numberOfSlots
        data[FIELD_PRICE] = event.price
        data[FIELD_ISAVAILABLE] = event.isAvailable
        data[FIELD_ADVERTISING] = event.advertising

        val storageRef = storage.child("images").child("${event.eventID}.jpg")
        storageRef.putBytes(image).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d(TAG, "uploadImage: $uri")
                    try {
                        val downloadUrl = uri.toString()
                        data[FIELD_IMAGE] = downloadUrl
                        db.collection(COLLECTION_EVENTS).document(event.eventID).set(data)
                            .addOnSuccessListener {
                                Log.d(TAG, "updateEvent: ${event.eventID} successfully updated")
                                updatedImageURL.postValue(downloadUrl)
                            }.addOnFailureListener { ex ->
                                Log.e(TAG, "updateEvent: $ex")
                            }
                    } catch (ex: Exception) {
                        Log.e(TAG, "addEvent: $ex")
                    }
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "uploadImage: $ex")
                }
        }
    }

    fun getEventUpdatedImage(eventID: String) {
        try {
            db.collection(COLLECTION_EVENTS).whereEqualTo(FIELD_EVENTID, eventID)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(TAG, "getEventUpdatedImage: $error")
                        return@EventListener
                    }

                    if (result != null && !result.isEmpty) {
                        val currentDocument: Event? =
                            result.documents[0].toObject(Event::class.java)
                        if (currentDocument != null) {
                            updatedImageURL.postValue(currentDocument.image)
                        }
                    } else {
                        Log.d(TAG, "getOrganizerEvents: No data")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getEventUpdatedImage: $ex")
        }
    }

    fun getOrganizerEvents(email: String) {
        try {
            db.collection(COLLECTION_EVENTS).whereEqualTo(FIELD_ORGANIZEREMAIL, email)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "getOrganizerEvents:  $error",
                        )
                        return@EventListener
                    }

                    if (result != null) {
                        val tempList: MutableList<Event> = ArrayList()

                        for (doc in result.documents) {
                            val currentDocument: Event? = doc.toObject(Event::class.java)
                            if (currentDocument != null) {
                                tempList.add(currentDocument)
                            }
                        }

                        allEvents.postValue(tempList)
                    } else {
                        Log.d(TAG, "getOrganizerEvents: No data")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getOrganizerEvents: $ex")
        }
    }

    fun getEvents() {
        try {
            db.collection(COLLECTION_EVENTS).addSnapshotListener(EventListener { result, error ->
                if (error != null) {
                    Log.e(
                        TAG,
                        "getEvents:  $error",
                    )
                    return@EventListener
                }

                if (result != null) {
                    Log.d(TAG, "getEvents: ${result.size()} documents")
                    val tempList: MutableList<Event> = ArrayList()

                    for (doc in result.documents) {
                        val currentDocument: Event? = doc.toObject(Event::class.java)
                        if (currentDocument != null) {
                            tempList.add(currentDocument)
                        }
                    }

                    tempList.sortBy { it.scheduleStart }
                    allEvents.postValue(tempList)
                } else {
                    Log.d(TAG, "getEvents: No data")
                }
            })
        } catch (ex: Exception) {
            Log.e(TAG, "getEvents: $ex")
        }
    }

    fun getRegisteredEvents(eventIDs: ArrayList<String>) {
        try {
            db.collection(COLLECTION_EVENTS).whereIn(FIELD_EVENTID, eventIDs)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "getRegisteredEvents:  $error",
                        )
                        return@EventListener
                    }

                    if (result != null) {
                        Log.d(TAG, "getRegisteredEvents: ${result.size()} documents")
                        val tempList: MutableList<Event> = ArrayList()

                        for (doc in result.documents) {
                            val currentDocument: Event? = doc.toObject(Event::class.java)
                            if (currentDocument != null) {
                                tempList.add(currentDocument)
                            }
                        }

                        allEvents.postValue(tempList)
                    } else {
                        Log.d(TAG, "getRegisteredEvents: No data")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getRegisteredEvents: $ex")
        }
    }

    fun fetchEventByID(eventID: String): LiveData<Event> {
        try {
            db.collection(COLLECTION_EVENTS).document(eventID)
                .addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "fetchEventByID: $error")
                        return@EventListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val event: Event? = snapshot.toObject(Event::class.java)
                        if (event != null) {
                            singleEvent.postValue(event)
                        }
                    } else {
                        Log.d(TAG, "fetchEventByID: Document not found")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "fetchEventByID: $ex")
        }

        return singleEvent
    }
}