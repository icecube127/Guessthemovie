package edu.utap.guessthemovie

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import edu.utap.guessthemovie.api.MovieApi
import edu.utap.guessthemovie.api.MovieData
import edu.utap.guessthemovie.api.Repository
import edu.utap.guessthemovie.databinding.GameMainBinding
import edu.utap.guessthemovie.glide.Glide
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.cancel

class Game : AppCompatActivity(){
    private val scope = MainScope()
    // debug mode - the input hint will show the movie title
    private val Debug = true

    companion object{
        val TAG = this::class.java.simpleName
    }
    private val viewModel: MainViewModel by viewModels()

    // constant values
    private val blurMax : Float = 55F
    private val blurStep : Float = 9F
    private val maxChances : Int = 5
    private lateinit var stars : List<ImageView>

    // movie related data
    private lateinit var moviePoster : String
    private lateinit var movieTitle : String
    private var movieYear : String = ""
    private var movieDirector : String = ""
    private var movieActor : String = ""
    private var movieSynopsis : String = ""

    // game setting values
    private var blur = blurMax
    private var chances = maxChances
    private var userScore : Int = 0
    private var userName : String = ""
    private lateinit var binding : GameMainBinding
    private val user = FirebaseAuth.getInstance().currentUser

    // For Testing
    private var currentPosition = 0

    // background music
    private lateinit var player: MediaPlayer
    private val bgMusic = BGMusic()
    private var songResources = bgMusic.fetchData()
    private var songListInfo = songResources.values.toMutableList()
    private var currentSongPosition = 0
    private var musicStatus = false

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.rootView.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GameMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // initialize the music player
        binding.btnMusic.isChecked = musicStatus
        val song = songListInfo[currentSongPosition].rawID
        player = MediaPlayer.create(this, song)
        binding.btnMusic.setOnClickListener {
            if(binding.btnMusic.isChecked)
                startBGMusic()
            else
                player.stop()
        }

        setupGame()
        binding.btnNext.setOnClickListener { playGame() }
        binding.btnEndGame.setOnClickListener {
            if (binding.btnMusic.isChecked)
                player.stop()
            val selectionScreenIntent = Intent(this, ProfilePage::class.java)
            startActivity(selectionScreenIntent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupGame() {
        if(user != null) {
            viewModel.fetchScoreMeta()
            viewModel.observeScoreMeta().observe(this) {
                for (item in it) {
                    if (item.ownerUid == user.uid) {
                        Log.d("XXXXXXXXXXXXXXXXXXX", "user info, ${item.name}, ${item.score}")
                        userScore = item.score
                        binding.playerPoints.text = " X $userScore"
                    }
                }
            }
        } else {
            Log.d(TAG, "not logged in")
            userScore = 0
        }
        binding.playerStar.setImageResource(android.R.drawable.btn_star_big_on)
        binding.playerPoints.text = " X $userScore"
        playGame()
    }

    private fun playGame(){
        // resets the game parameters and set up the poster + Title hint
        resetGame()
        setupTitleHint(movieTitle)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.moviePoster.setRenderEffect(RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.MIRROR))
        }

        // check user's answer
        binding.bntSubmit.setOnClickListener {
            if (chances == 0) {
                // do nothing.
                Log.d(TAG, "GAME LOST")
            } else {
                val userInput = binding.userInput.text.toString()
                hideKeyboard()
                if (checkAnswer(movieTitle, userInput)) {
                    Log.d(TAG, "GAME WON")
                    winGame()
                }
                else {
                    chances -= 1
                    updateStar(chances)
                    if (chances > 0) {
                        binding.userInput.text.clear()
                        blur -= blurStep
                        showPoster(blur)
                    }
                    else {
                        // USER DID NOT GUESS RIGHT AFTER 5 TRIES
                        // show poster, show answer
                        blur = 1F
                        showPoster(blur)
                        binding.titleHint.setBackgroundColor(Color.RED)
                        binding.titleHint.text = movieTitle
                        userScore += 0
                    }
                }
            }
        }
    }

    private fun resetGame() {
        // reset game will return all things to initial state
        // also fetches data from API to get meta data for a new movie

        // Resetting all the game setting data reset stars
        blur = blurMax
        chances = maxChances
        binding.userInput.text.clear()
        binding.titleHint.setBackgroundColor(Color.WHITE)
        stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
        for(item in stars)
            item.setImageResource(android.R.drawable.btn_star_big_on)

        // get new movie title from Movies.kt which is a list of pre-determined movies
        val movieDB = Movies()
        val currentMovie = movieDB.fetchOneMovie()
        movieTitle = currentMovie.title

        // ===========================================
        // below is for testing only
//        val currentMovie = movieDB.fetchMovie(currentPosition)
//        currentPosition += 1
//        if (currentPosition == movieDB.listSize())
//            currentPosition = 0
        // ===========================================

        // code to get new movie meta data from network
        viewModel.getMovie(movieTitle)
        viewModel.observeMovie().observe(this) {
            movieYear = it.year
            movieDirector = it.director
            movieActor = it.actors
            movieSynopsis = it.plot
            moviePoster = it.poster

            Log.d("XXXXXXXXXXXXXXXXXXXXXXXXXXX", "Movie title: $movieTitle")
            Log.d("XXXXXXXXXXXXXXXXXXXXXXXXXXX", "Current position: $currentPosition")
            binding.yearHint.text = movieYear
            binding.directorHint.text = movieDirector
            binding.actorHint.text = movieActor
            binding.synopsisHint.text = movieSynopsis
            Glide.glideFetch(moviePoster, moviePoster, binding.moviePoster)
        }

        binding.yearHint.visibility = View.INVISIBLE
        binding.directorHint.visibility = View.INVISIBLE
        binding.actorHint.visibility = View.INVISIBLE
        binding.synopsisHint.visibility = View.INVISIBLE
    }

    private fun showPoster(blur : Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.moviePoster.setRenderEffect(RenderEffect.createBlurEffect(blur,blur, Shader.TileMode.MIRROR))
        }
    }

    private fun updateStar(chance : Int) {
        // this function will remove the star in order
        // also reveal hint in order
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        when (chance) {
            4 -> {
                flipStar(stars[4], false)
                //binding.yearHint.visibility = View.VISIBLE
                binding.yearHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.yearHint.visibility = View.VISIBLE
            }
            3 -> {
                flipStar(stars[3], false)
                //binding.directorHint.visibility = View.VISIBLE
                binding.directorHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.directorHint.visibility = View.VISIBLE
            }
            2 -> {
                flipStar(stars[2], false)
                //binding.actorHint.visibility = View.VISIBLE
                binding.actorHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.actorHint.visibility = View.VISIBLE
            }
            1 -> {
                flipStar(stars[1], false)
                //binding.synopsisHint.visibility = View.VISIBLE
                binding.synopsisHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.synopsisHint.visibility = View.VISIBLE
            }
            0 -> flipStar(stars[0], false)
            else -> Log.d(TAG, "Do nothing")
        }
    }

    private fun flipStar(star : ImageView, on : Boolean){
        // takes the image view of the star and run the flip animation
        val handler = Handler(Looper.getMainLooper())
        star.animate().apply {
            duration = 1000
            rotationYBy(360f)
        }.start()
        handler.postDelayed({
            if(on)
                star.setImageResource(android.R.drawable.btn_star_big_on)
            else
                star.setImageResource(android.R.drawable.btn_star_big_off)
        }, 750)
    }

    private fun setupTitleHint(movieTitle : String) {
        //val titleInChars = moveTitle.toList()
        var titleHint = ""
        for(character in movieTitle) {
            titleHint += if (character == ' ')
                "  "
            else
                "_ "
        }
        binding.titleHint.text = titleHint
        if(Debug)
            binding.userInput.hint = movieTitle
    }

    private fun checkAnswer(movieTitle : String, userInput : String) : Boolean{
        // this function takes in both movieTitle and userInput and compare the two.
        // If any character is guessed correctly in the right position, then character will show
        // do a for loop on the shorter list and check each character
        var theHint = ""
        var matched = true
        val movieTitleList = movieTitle.toList()
        val userInputList = userInput.toList()
        val userInputLength = userInputList.size

        for ((index, value) in movieTitleList.withIndex()) {
            if(userInputLength > index){
                if(userInputList[index].lowercaseChar() == value.lowercaseChar())
                    theHint += ("$value ")
                else {
                    matched = false
                    theHint += if (value == ' ')
                        "  "
                    else
                        "_ "
                }
            } else {
                matched = false
                theHint += if (value == ' ')
                    "  "
                else
                    "_ "
            }
        }
        // display the matched characters in hint
        binding.titleHint.text = theHint
        // if user added extra characters at the end, the hint will display the full movie title
        // however, the answer is still incorrect.
        if (movieTitleList.size != userInputLength)
            matched = false
        return matched
    }

    @SuppressLint("SetTextI18n")
    private fun winGame() {
        // update the score. If user is logged in, also update the score in DB
        // do the answer reveal animation
        userScore += chances
        if (user != null) {
            viewModel.updateScoreMeta(userScore)
            Log.d("XXXXXXXXXXXXXXXXXX", "user score updated")
        }

        blur = 1F
        chances = 0
        binding.titleHint.setBackgroundColor(Color.GREEN)

        val handler = Handler(Looper.getMainLooper())
        binding.moviePoster.animate().apply {
            duration = 1000
            rotationYBy(360f)
        }.start()
        handler.postDelayed({
            showPoster(blur)
        }, 750)

        handler.postDelayed({
            flipStar(binding.playerStar, true)
        }, 1000)

        handler.postDelayed({
            binding.playerPoints.text = " X $userScore"
        }, 2000)

        binding.yearHint.visibility = View.VISIBLE
        binding.directorHint.visibility = View.VISIBLE
        binding.actorHint.visibility = View.VISIBLE
        binding.synopsisHint.visibility = View.VISIBLE
    }

    private fun startBGMusic() {
        player.start()
        player.setOnCompletionListener {
            currentSongPosition += 1
            if (currentSongPosition == 2)
                currentSongPosition = 0
            player.reset()
            val nextSong = songListInfo[currentSongPosition].rawID
            player.setDataSource(resources.openRawResourceFd(nextSong))
            player.prepare()
            player.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.reset()
        player.release()
    }
}