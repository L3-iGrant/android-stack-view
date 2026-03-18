package io.igrant.stackview.sample

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import io.igrant.stackview.StackConfig
import io.igrant.stackview.StackLayoutManager
import io.igrant.stackview.sample.databinding.ActivityMainBinding
import io.igrant.stackview.sample.databinding.DialogAddMovieBinding
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var stackLayoutManager: StackLayoutManager
    private lateinit var adapter: CardAdapter
    private val movies = mutableListOf<CardItem>()

    private val cardColors = listOf(
        // Deep & dark
        "#1A1A2E", "#16213E", "#2C3E50", "#0F3460", "#1B1B2F",
        // Reds & pinks
        "#E74C3C", "#C0392B", "#FF6B6B", "#E84393", "#D63031",
        // Oranges & yellows
        "#D35400", "#E67E22", "#F39C12", "#FDCB6E", "#F9CA24",
        // Greens
        "#27AE60", "#1ABC9C", "#00B894", "#55EFC4", "#A3CB38",
        // Blues
        "#4A90D9", "#2980B9", "#0984E3", "#74B9FF", "#6C5CE7",
        // Purples
        "#8E44AD", "#9B59B6", "#A29BFE", "#6C5CE7", "#5F27CD",
        // Teals & cyans
        "#00CEC9", "#81ECEC", "#22A6B3", "#7ED6DF", "#38ADA9",
        // Warm tones
        "#E17055", "#FAB1A0", "#FF7675", "#FD79A8", "#FFEAA7",
        // Cool grays
        "#636E72", "#B2BEC3", "#DFE6E9", "#2D3436", "#95A5A6"
    )

    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == MovieDetailActivity.RESULT_REMOVED) {
            reloadMovies()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val density = resources.displayMetrics.density
        val collapsedPeekPx = (45 * density).toInt()

        stackLayoutManager = StackLayoutManager(
            config = StackConfig(
                collapsedPeekHeight = collapsedPeekPx,
                stackTopMargin = (10 * density).toInt(),
                animationDuration = 350L
            )
        )

        loadMovies()

        adapter = CardAdapter(movies) { position ->
            stackLayoutManager.presentCard(position, binding.recyclerView)
        }

        stackLayoutManager.onPresentedCardClicked = { position ->
            val movie = movies[position]
            val intent = Intent(this, MovieDetailActivity::class.java).apply {
                putExtra(MovieDetailActivity.EXTRA_MOVIE_ID, movie.id)
                putExtra(MovieDetailActivity.EXTRA_TITLE, movie.title)
                putExtra(MovieDetailActivity.EXTRA_GENRE, movie.genre)
                putExtra(MovieDetailActivity.EXTRA_YEAR, movie.year)
                putExtra(MovieDetailActivity.EXTRA_DIRECTOR, movie.director)
                putExtra(MovieDetailActivity.EXTRA_RATING, movie.rating)
                putExtra(MovieDetailActivity.EXTRA_BG_COLOR, movie.backgroundColor)
            }
            detailLauncher.launch(intent)
        }

        binding.recyclerView.layoutManager = stackLayoutManager
        binding.recyclerView.adapter = adapter

        binding.fabAdd.setOnClickListener { showAddMovieDialog() }
    }

    private fun loadMovies() {
        val saved = MoviePreferences.loadMovies(this)
        movies.clear()
        if (saved.isEmpty()) {
            val defaults = MoviePreferences.getDefaultMovies()
            movies.addAll(defaults)
            MoviePreferences.saveMovies(this, movies)
        } else {
            movies.addAll(saved)
        }
    }

    private fun reloadMovies() {
        val saved = MoviePreferences.loadMovies(this)
        adapter.updateItems(saved)
        stackLayoutManager.refresh(binding.recyclerView)
    }

    private fun showAddMovieDialog() {
        val dialogBinding = DialogAddMovieBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setTitle("Add Movie")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { _, _ ->
                val title = dialogBinding.etTitle.text.toString().trim()
                val genre = dialogBinding.etGenre.text.toString().trim()
                val yearStr = dialogBinding.etYear.text.toString().trim()
                val director = dialogBinding.etDirector.text.toString().trim()
                val ratingStr = dialogBinding.etRating.text.toString().trim()

                if (title.isEmpty()) {
                    Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val year = yearStr.toIntOrNull() ?: 2024
                val rating = ratingStr.toFloatOrNull() ?: 0f
                val color = Color.parseColor(cardColors.random())

                val movie = CardItem(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    genre = genre.ifEmpty { "Unknown" },
                    year = year,
                    director = director.ifEmpty { "Unknown" },
                    rating = rating.coerceIn(0f, 10f),
                    backgroundColor = color
                )

                adapter.addItem(movie)
                MoviePreferences.saveMovies(this, movies)
                stackLayoutManager.refresh(binding.recyclerView)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
