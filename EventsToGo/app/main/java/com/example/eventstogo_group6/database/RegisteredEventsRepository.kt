package com.example.eventstogo_group6.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.models.Registration
import com.example.eventstogo_group6.models.User
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.checkerframework.checker.units.qual.C

class RegisteredEventsRepository(private val context: Context) {
    private val TAG = this.toString()
    private val db = Firebase.firestore

    private val COLLECTION_REGISTEREDEVENTS = "Registered Events"
    private val COLLECTION_USERS = "Users"
    private val FIELD_REGISTRATIONID = "registrationID"
    private val FIELD_EVENTID = "eventID"
    private val FIELD_USEREMAIL = "userEmail"
    private val FIELD_QUANTITY = "quantity"
    private val FIELD_SHIPPINGADDRESS = "shippingAddress"

    var registrations: MutableLiveData<List<Registration>> = MutableLiveData<List<Registration>>()
    var eventRegisteredUsers: MutableLiveData<List<User>> = MutableLiveData<List<User>>()

    fun addRegistrationToDB(newRegistration: Registration) {
        try {
            val data: MutableMap<String, Any> = HashMap()

            data[FIELD_REGISTRATIONID] = newRegistration.registrationID
            data[FIELD_EVENTID] = newRegistration.eventID
            data[FIELD_USEREMAIL] = newRegistration.userEmail
            data[FIELD_QUANTITY] = newRegistration.quantity
            data[FIELD_SHIPPINGADDRESS] = newRegistration.shippingAddress

            db.collection(COLLECTION_REGISTEREDEVENTS).document(newRegistration.registrationID)
                .set(data).addOnSuccessListener { docRef ->
                    Log.d(
                        TAG,
                        "addRegistrationToDB: Registration document successfully created with ID $docRef"
                    )
                }.addOnFailureListener { ex ->
                    Log.e(
                        TAG,
                        "addRegistrationToDB: Unable to create registration document due to exception : $ex",
                    )
                }

        } catch (ex: Exception) {
            Log.e(TAG, "addRegistrationToDB: Couldn't add user document $ex")
        }
    }

    fun getRegistrations(email: String) {
        try {
            db.collection(COLLECTION_REGISTEREDEVENTS).whereEqualTo(FIELD_USEREMAIL, email)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "getRegistrations:  $error",
                        )
                        return@EventListener
                    }

                    if (result != null) {
                        Log.d(TAG, "getRegistrations: ${result.size()} documents")
                        val tempList: MutableList<Registration> = ArrayList()

                        for (doc in result.documents) {
                            val currentDocument: Registration? =
                                doc.toObject(Registration::class.java)
                            if (currentDocument != null) {
                                tempList.add(currentDocument)
                            }
                        }

                        registrations.postValue(tempList)
                    } else {
                        Log.d(TAG, "getRegistrations: No data")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getRegistrations: $ex")
        }
    }

    fun deleteRegistration(registrationID: String) {
        try {
            db.collection(COLLECTION_REGISTEREDEVENTS).document(registrationID).delete()
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "deleteRegistration: Registration with ID $registrationID successfully deleted"
                    )
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "deleteRegistration: $ex")
                }
        } catch (ex: Exception) {
            Log.e(TAG, "deleteRegistration: $ex")
        }
    }

    fun getRegisteredUsers(eventID: String) {
        try {
            db.collection(COLLECTION_REGISTEREDEVENTS).whereEqualTo(FIELD_EVENTID, eventID)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "getRegisteredUsers:  $error",
                        )
                        return@EventListener
                    }

                    if (result != null) {
                        val tempList: MutableList<User> = ArrayList()
                        for (doc in result.documents) {
                            val currentDocument: Registration? =
                                doc.toObject(Registration::class.java)
                            Log.d(TAG, "getRegisteredUsers: $currentDocument")
                            if (currentDocument != null) {
                                db.collection(COLLECTION_USERS).document(currentDocument.userEmail)
                                    .get().addOnSuccessListener {
                                        Log.d(
                                            TAG,
                                            "getRegisteredUsers: ${it.toObject(User::class.java)!!.email}"
                                        )
                                        if(!tempList.contains(it.toObject(User::class.java)!!)) tempList.add(it.toObject(User::class.java)!!)
                                        eventRegisteredUsers.postValue(tempList)
                                    }
                            }
                        }
                    } else {
                        Log.d(TAG, "getRegisteredUsers: No data")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getRegisteredUsers: $ex")
        }
    }
}