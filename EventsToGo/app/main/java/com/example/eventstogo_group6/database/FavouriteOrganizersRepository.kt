package com.example.eventstogo_group6.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.eventstogo_group6.api.UpdateCallback
import com.example.eventstogo_group6.models.FavouriteOrganizer
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FavouriteOrganizerRepository(private val context: Context) {
    private val TAG = this.toString()
    private val db = Firebase.firestore

    private val COLLECTION_FAVOURITE_ORGANIZERS = "FavouriteOrganizers"
    private val FIELD_FAVORITE_ID = "favoriteID"
    private val FIELD_ORGANIZER_EMAIL = "organizerEmail"
    private val FIELD_USER_EMAIL = "userEmail"

    var favouriteOrganizers: MutableLiveData<List<FavouriteOrganizer>> =
        MutableLiveData<List<FavouriteOrganizer>>()

    fun addFavouriteOrganizerToDB(
        newFavouriteOrganizer: FavouriteOrganizer,
        callback: UpdateCallback
    ) {
        try {
            val data: MutableMap<String, Any> = HashMap()

            data[FIELD_FAVORITE_ID] = newFavouriteOrganizer.favoriteID
            data[FIELD_ORGANIZER_EMAIL] = newFavouriteOrganizer.organizerEmail
            data[FIELD_USER_EMAIL] = newFavouriteOrganizer.userEmail

            db.collection(COLLECTION_FAVOURITE_ORGANIZERS)
                .document(newFavouriteOrganizer.favoriteID)
                .set(data)
                .addOnSuccessListener { docRef ->
                    Log.d(
                        TAG,
                        "addFavouriteOrganizerToDB: Favourite organizer document successfully created with ID $docRef"
                    )
                }.addOnFailureListener { ex ->
                    Log.e(
                        TAG,
                        "addFavouriteOrganizerToDB: Unable to create favourite organizer document due to exception: $ex",
                    )
                }

        } catch (ex: Exception) {
            Log.e(TAG, "addFavouriteOrganizerToDB: Couldn't add favourite organizer document $ex")
        }
    }

    fun getFavouriteOrganizers(email: String): LiveData<List<FavouriteOrganizer>> {
        val favouriteOrganizersLiveData = MutableLiveData<List<FavouriteOrganizer>>()

        try {
            db.collection(COLLECTION_FAVOURITE_ORGANIZERS)
                .whereEqualTo(FIELD_USER_EMAIL, email)
                .addSnapshotListener(EventListener { result, error ->
                    if (error != null) {
                        Log.e(TAG, "getFavouriteOrganizers: $error")
                        return@EventListener
                    }

                    if (result != null) {
                        val tempList: MutableList<FavouriteOrganizer> = ArrayList()

                        for (doc in result.documents) {
                            val currentDocument: FavouriteOrganizer? =
                                doc.toObject(FavouriteOrganizer::class.java)
                            if (currentDocument != null) {
                                tempList.add(currentDocument)
                            }
                        }

                        favouriteOrganizersLiveData.postValue(tempList)
                    } else {
                        Log.d(TAG, "getFavouriteOrganizers: No data")
                    }
                })
        } catch (ex: Exception) {
            Log.e(TAG, "getFavouriteOrganizers: $ex")
        }

        return favouriteOrganizersLiveData
    }

    fun deleteFavOrganizer(favID: String) {
        try {
            db.collection(COLLECTION_FAVOURITE_ORGANIZERS).document(favID).delete()
                .addOnSuccessListener {
                    Log.d(
                        TAG,
                        "deleteFavOrganizer: Favorite Organizer with ID $favID successfully deleted"
                    )
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "deleteFavOrganizer: $ex")
                }
        } catch (ex: Exception) {
            Log.e(TAG, "deleteFavOrganizer: $ex")
        }
    }
}
