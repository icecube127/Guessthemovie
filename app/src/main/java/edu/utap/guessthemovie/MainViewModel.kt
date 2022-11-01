package edu.utap.guessthemovie

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import edu.utap.guessthemovie.api.MovieApi
import edu.utap.guessthemovie.api.MovieData
import edu.utap.guessthemovie.api.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private var displayName = MutableLiveData("Uninitialized")
    private var email = MutableLiveData("Uninitialized")
    private var uid = MutableLiveData("Uninitialized")

    // API related data
    private val movieApi = MovieApi.create()
    private val movieRepository = Repository(movieApi)
    private val movieMeta = MutableLiveData<MovieData>()

    fun getMovie(title: String) = viewModelScope.launch (
        context = viewModelScope.coroutineContext + Dispatchers.Default) {
        val myMovie = movieRepository.fetchMovie(title)
        movieMeta.postValue(myMovie)
        }

    fun observeMovie (): LiveData<MovieData> {
        return movieMeta
    }

    private fun userLogout() {
        displayName.postValue("No user")
        email.postValue("No email, no active user")
        uid.postValue("No uid, no active user")
    }

    fun updateUser() {
        // XXX Write me. Update user data in view model
        viewModelScope.launch(
            context = viewModelScope.coroutineContext
                    + Dispatchers.Default){
            // Please leave this delay
            delay(800)
            val user = FirebaseAuth.getInstance().currentUser
            println( "...... In update user ${user?.displayName} email ${user?.email}")
            displayName.postValue(user?.displayName.toString())
            email.postValue(user?.email)
            uid.postValue(user?.uid)
        }
    }

    fun observeDisplayName() : LiveData<String> {
        return displayName
    }
    fun observeEmail() : LiveData<String> {
        return email
    }
    fun observeUid() : LiveData<String> {
        return uid
    }
    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        userLogout()
    }
}