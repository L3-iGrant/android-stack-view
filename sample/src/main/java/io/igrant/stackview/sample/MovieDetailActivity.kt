package io.igrant.stackview.sample

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.igrant.stackview.sample.databinding.ActivityMovieDetailBinding

class MovieDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MOVIE_ID = "movie_id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_GENRE = "genre"
        const val EXTRA_YEAR = "year"
        const val EXTRA_DIRECTOR = "director"
        const val EXTRA_RATING = "rating"
        const val EXTRA_BG_COLOR = "bg_color"
        const val RESULT_REMOVED = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val movieId = intent.getStringExtra(EXTRA_MOVIE_ID) ?: return finish()
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val genre = intent.getStringExtra(EXTRA_GENRE) ?: ""
        val year = intent.getIntExtra(EXTRA_YEAR, 0)
        val director = intent.getStringExtra(EXTRA_DIRECTOR) ?: ""
        val rating = intent.getFloatExtra(EXTRA_RATING, 0f)
        val bgColor = intent.getIntExtra(EXTRA_BG_COLOR, Color.WHITE)

        binding.cardHeader.setCardBackgroundColor(bgColor)

        val isLight = isColorLight(bgColor)
        val headerTextColor = if (isLight) Color.parseColor("#333333") else Color.WHITE

        binding.tvDetailTitle.text = title
        binding.tvDetailTitle.setTextColor(headerTextColor)
        binding.tvDetailGenreYear.text = "$genre • $year"
        binding.tvDetailGenreYear.setTextColor(headerTextColor)
        binding.tvDetailDirector.text = director
        binding.tvDetailRating.text = "★ $rating / 10"

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnRemove.setOnClickListener {
            MoviePreferences.removeMovie(this, movieId)
            setResult(RESULT_REMOVED)
            finish()
        }
    }

    private fun isColorLight(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
        return luminance > 0.5
    }
}
