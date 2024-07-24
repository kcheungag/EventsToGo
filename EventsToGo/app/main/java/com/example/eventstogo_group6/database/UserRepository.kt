package com.example.eventstogo_group6.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.eventstogo_group6.models.BookmarkedEvent
import com.example.eventstogo_group6.models.Event
import com.example.eventstogo_group6.models.FavouriteOrganizer
import com.example.eventstogo_group6.models.User
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase

class UserRepository(private val context: Context) {
    private val TAG = this.toString()
    private val db = Firebase.firestore


    private val COLLECTION_USERS = "Users"
    private val FIELD_EMAIL = "email"
    private val FIELD_PASSWORD = "password"
    private val FIELD_NAME = "name"
    private val FIELD_CITY = "city"
    private val FIELD_ROLE = "role"

    var singleUser: MutableLiveData<User> = MutableLiveData()
    var favouriteOrganizers: MutableLiveData<List<User>> = MutableLiveData<List<User>>()


    fun getUserFromDB(email: String): LiveData<User> {
        try {
            db.collection(COLLECTION_USERS)
                .document(email)
                .addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "getUserFromDB: $error")
                        return@EventListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val user: User? = snapshot.toObject(User::class.java)
                        if (user != null) {
                            singleUser.postValue(user)
                        }
                    } else {
                        Log.d(TAG, "getUserFromDB: Document not found")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getUserFromDB: $ex")
        }
        return singleUser
    }

    fun addUserToDB(newUser : User){
        try{
            val data : MutableMap<String, Any> = HashMap()

            data[FIELD_EMAIL] = newUser.email
            data[FIELD_PASSWORD] = newUser.password
            data[FIELD_NAME] = newUser.name
            data[FIELD_CITY] = newUser.city
            data[FIELD_ROLE] = newUser.role

            db.collection(COLLECTION_USERS)
                .document(newUser.email)
                .set(data)
                .addOnSuccessListener { docRef ->
                    Log.d(TAG, "addUserToDB: User document successfully created with ID $docRef")
                }
                .addOnFailureListener { ex ->
                    Log.e(TAG, "addUserToDB: Unable to create user document due to exception : $ex", )
                }

        }catch (ex : Exception){
            Log.e(TAG, "addUserToDB: Couldn't add user document $ex", )
        }
    }

    fun updateUser(user: User){
        val data: MutableMap<String, Any> = HashMap();
        data[FIELD_EMAIL] = user.email
        data[FIELD_PASSWORD] = user.password
        data[FIELD_NAME] = user.name
        data[FIELD_CITY] = user.city
        data[FIELD_ROLE] = user.role
        try {
            db.collection(COLLECTION_USERS).document(user.email).set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "updateUser: ${user.email} account successfully updated")
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "updateUser: $ex")
                }
        } catch (ex: Exception) {
            Log.e(TAG, "updateUser: $ex")
        }
    }

    fun getUsersByID(emailList: ArrayList<String>): LiveData<List<User>> {
        try {
            db.collection(COLLECTION_USERS).whereIn(FIELD_EMAIL, emailList)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(
                            TAG,
                            "getUsersByID:  $error",
                        )
                        return@EventListener
                    }

                    if (result != null) {
                        Log.d(TAG, "getUsersByID: ${result.size()} documents")
                        val tempList: MutableList<User> = ArrayList()

                        for (doc in result.documents) {
                            val currentDocument: User? = doc.toObject(User::class.java)
                            if (currentDocument != null) {
                                tempList.add(currentDocument)
                            }
                        }
                        favouriteOrganizers.postValue(tempList)

                    } else {
                        Log.d(TAG, "getRegisteredEvents: No data")
                    }
                })

        } catch (ex: Exception) {
            Log.e(TAG, "getRegisteredEvents: $ex")
        }
        return favouriteOrganizers
    }
}