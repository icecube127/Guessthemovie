package edu.utap.guessthemovie

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import edu.utap.guessthemovie.databinding.GameMainBinding
import edu.utap.guessthemovie.glide.Glide


class Game : AppCompatActivity() {
    companion object{
        val TAG = this::class.java.simpleName
    }
    private val blurMax : Float = 55F
    private val blurStep : Float = 9F
    private val maxChances : Int = 5
    private lateinit var stars : List<ImageView>

    // movie related data
    private lateinit var moviePoster : String
    private lateinit var movieTitle : String
    private var movieYear : Int = 0
    private lateinit var movieDirector : String
    private lateinit var movieActor : String
    private lateinit var movieSynopsis : String

    private var blur = blurMax
    private var chances = maxChances
    private var userScore : Int = 0
    private var userName : String = ""
    private lateinit var binding : GameMainBinding

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(window.decorView.rootView.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GameMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get user score and user name from selection screen
        val activityThatCalled = intent
        userScore = activityThatCalled.extras?.get("userScore") as Int
        userName = activityThatCalled.extras?.getString("userName").toString()

        // set up and play the game
        setupGame()
        binding.btnNext.setOnClickListener { playGame() }
        binding.btnEndGame.setOnClickListener {
            val selectionScreenIntent = Intent(this, SelectionPage::class.java)
            selectionScreenIntent.putExtra("username", userName)
            startActivity(selectionScreenIntent)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupGame() {
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
                Log.d(TAG, "game is over")
            } else {
                val userInput = binding.userInput.text.toString()
                hideKeyboard()
                if (checkAnswer(movieTitle, userInput)) {
                    // USER ANSWERED CORRECTLY.
                    // NEED TO ADD CODE FOR SCORING.
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
        blur = blurMax
        chances = maxChances
        binding.userInput.text.clear()
        binding.titleHint.setBackgroundColor(Color.WHITE)
        stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)
        for(item in stars)
            item.setImageResource(android.R.drawable.btn_star_big_on)

        // get new movie meta data
        val movieDB = Movies()
        val currentMovie = movieDB.fetchOneMovie()
        movieTitle = currentMovie.title
        movieYear = currentMovie.year

        moviePoster = "https://m.media-amazon.com/images/M/MV5BNGVjNWI4ZGUtNzE0MS00YTJmLWE0ZDctN2ZiYTk2YmI3NTYyXkEyXkFqcGdeQXVyMTkxNjUyNQ@@._V1_SX300.jpg"
        Glide.glideFetch(moviePoster, moviePoster, binding.moviePoster)
        binding.yearHint.text = movieYear.toString()

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
                flipStar(stars[4])
                //binding.yearHint.visibility = View.VISIBLE
                binding.yearHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.yearHint.visibility = View.VISIBLE
            }
            3 -> {
                flipStar(stars[3])
                //binding.directorHint.visibility = View.VISIBLE
                binding.directorHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.directorHint.visibility = View.VISIBLE
            }
            2 -> {
                flipStar(stars[2])
                //binding.actorHint.visibility = View.VISIBLE
                binding.actorHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.actorHint.visibility = View.VISIBLE
            }
            1 -> {
                flipStar(stars[1])
                //binding.synopsisHint.visibility = View.VISIBLE
                binding.synopsisHint.startAnimation(fadeIn)
                fadeIn.duration = 1200
                binding.synopsisHint.visibility = View.VISIBLE
            }
            0 -> flipStar(stars[0])
            else -> Log.d(TAG, "Do nothing")
        }
    }

    private fun flipStar(star : ImageView){
        // takes the image view of the star and run the flip animation
        val handler = Handler(Looper.getMainLooper())
        star.animate().apply {
            duration = 1000
            rotationYBy(360f)
        }.start()
        handler.postDelayed({
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
    private fun winGame(){
        blur = 1F
        userScore += chances
        binding.playerPoints.text = " X $userScore"
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

        binding.yearHint.visibility = View.VISIBLE
        binding.directorHint.visibility = View.VISIBLE
        binding.actorHint.visibility = View.VISIBLE
        binding.synopsisHint.visibility = View.VISIBLE
    }
}