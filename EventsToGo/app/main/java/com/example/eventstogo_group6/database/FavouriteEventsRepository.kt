package com.example.eventstogo_group6.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.eventstogo_group6.api.UpdateCallback
import com.example.eventstogo_group6.models.BookmarkedEvent
import com.example.eventstogo_group6.models.User
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class FavouriteEventsRepository(private val context: Context) {
    private val TAG = this.toString()
    private val db = Firebase.firestore

    private val COLLECTION_FAVOURITE_EVENTS = "FavouriteEvents"
    private val COLLECTION_USERS = "Users"
    private val FIELD_BOOKMARK_ID = "bookmarkID"
    private val FIELD_EVENT_ID = "eventID"
    private val FIELD_USER_EMAIL = "userEmail"

    var favouriteEvents: MutableLiveData<List<BookmarkedEvent>> = MutableLiveData<List<BookmarkedEvent>>()
    var userFavouriteEvents: MutableLiveData<List<User>> = MutableLiveData<List<User>>()

    fun addFavouriteEventToDB(newFavouriteEvent: BookmarkedEvent, callback: UpdateCallback) {
        try {
            val data: MutableMap<String, Any> = HashMap()

            data[FIELD_BOOKMARK_ID] = newFavouriteEvent.bookmarkID
            data[FIELD_EVENT_ID] = newFavouriteEvent.eventID
            data[FIELD_USER_EMAIL] = newFavouriteEvent.userEmail

            db.collection(COLLECTION_FAVOURITE_EVENTS).document(newFavouriteEvent.bookmarkID)
                .set(data).addOnSuccessListener { docRef ->
                    Log.d(
                        TAG,
                        "addFavouriteEventToDB: Favourite event document successfully created with ID $docRef"
                    )
                }.addOnFailureListener { ex ->
                    Log.e(
                        TAG,
                        "addFavouriteEventToDB: Unable to create favourite event document due to exception: $ex",
                    )
                }

        } catch (ex: Exception) {
            Log.e(TAG, "addFavouriteEventToDB: Couldn't add favourite event document $ex")
        }
    }

    fun getFavouriteEvents(email: String): LiveData<List<BookmarkedEvent>> {
        val favouriteEventsLiveData = MutableLiveData<List<BookmarkedEvent>>()

        try {
            db.collection(COLLECTION_FAVOURITE_EVENTS)
                .whereEqualTo(FIELD_USER_EMAIL, email)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(TAG, "getFavouriteEvents: $error")
                        return@EventListener
                    }

                    if (result != null) {
                        val tempList: MutableList<BookmarkedEvent> = ArrayList()

                        for (doc in result.documents) {
                            val currentDocument: BookmarkedEvent? =
                                doc.toObject(BookmarkedEvent::class.java)
                            if (currentDocument != null) {
                                tempList.add(currentDocument)
                            }
                        }

                        favouriteEventsLiveData.postValue(tempList)
                    } else {
                        Log.d(TAG, "getFavouriteEvents: No data")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getFavouriteEvents: $ex")
        }

        return favouriteEventsLiveData
    }


    fun deleteFavouriteEvent(bookmarkID: String) {
        try {
            db.collection(COLLECTION_FAVOURITE_EVENTS).document(bookmarkID).delete()
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "deleteFavouriteEvent: Favourite event with ID $bookmarkID successfully deleted"
                    )
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "deleteFavouriteEvent: $ex")
                }
        } catch (ex: Exception) {
            Log.e(TAG, "deleteFavouriteEvent: $ex")
        }
    }

}
