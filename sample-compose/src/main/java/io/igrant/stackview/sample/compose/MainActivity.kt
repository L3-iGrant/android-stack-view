package io.igrant.stackview.sample.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.igrant.stackview.StackConfig
import io.igrant.stackview.compose.StackView
import io.igrant.stackview.compose.rememberStackViewState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MovieStackScreen()
            }
        }
    }
}

data class Movie(
    val id: String,
    val title: String,
    val genre: String,
    val year: Int,
    val director: String,
    val rating: Float,
    val color: Color,
)

@Composable
private fun MovieStackScreen() {
    val movies = remember { mutableStateListOf(*defaultMovies().toTypedArray()) }
    val stackState = rememberStackViewState()

    // StackConfig distances are in pixels — convert from dp once.
    val density = LocalDensity.current
    val config = remember(density) {
        with(density) {
            StackConfig(
                collapsedPeekHeight = 48.dp.roundToPx(),
                stackTopMargin = 12.dp.roundToPx(),
                animationDuration = 350L,
            )
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var detailMovie by remember { mutableStateOf<Movie?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Add movie") },
                icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
                onClick = { showAddDialog = true },
            )
        }
    ) { innerPadding ->
        StackView(
            items = movies,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            state = stackState,
            config = config,
            onPresentedCardClick = { index -> detailMovie = movies[index] },
        ) { _, movie ->
            MovieCard(movie)
        }
    }

    if (showAddDialog) {
        AddMovieDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title ->
                movies.add(0, randomMovie(title))
                showAddDialog = false
                stackState.refresh()
            },
        )
    }

    detailMovie?.let { movie ->
        MovieDetailDialog(
            movie = movie,
            onRemove = {
                val idx = movies.indexOfFirst { it.id == movie.id }
                if (idx >= 0) movies.removeAt(idx)
                detailMovie = null
                stackState.refresh()
            },
            onDismiss = { detailMovie = null },
        )
    }
}

@Composable
private fun MovieCard(movie: Movie) {
    val textColor = if (movie.color.isLight()) Color(0xFF333333) else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(movie.color)
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = movie.title,
                color = textColor,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "${movie.genre} • ${movie.year}",
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Dir. ${movie.director} — ★ ${movie.rating}",
                color = textColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AddMovieDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add movie") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title.ifBlank { "Untitled" }) },
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun MovieDetailDialog(movie: Movie, onRemove: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(movie.title) },
        text = {
            Text("${movie.genre} • ${movie.year}\nDir. ${movie.director}\nRating ★ ${movie.rating}")
        },
        confirmButton = {
            TextButton(onClick = onRemove) { Text("Remove") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

private fun Color.isLight(): Boolean {
    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
    return luminance > 0.5
}

private val palette = listOf(
    0xFF6C5CE7, 0xFFE17055, 0xFF2C3E50, 0xFFD63031, 0xFF0984E3,
    0xFF1B1B2F, 0xFFE84393, 0xFF00B894, 0xFF27AE60, 0xFFC0392B,
    0xFF74B9FF, 0xFFF39C12, 0xFF2D3436, 0xFFFD79A8, 0xFFD35400,
).map { Color(it) }

private var nextId = 100

private fun randomMovie(title: String): Movie {
    // No Math.random in this environment-agnostic sample either; rotate deterministically.
    val color = palette[nextId % palette.size]
    return Movie(
        id = (nextId++).toString(),
        title = title,
        genre = "Unknown",
        year = 2024,
        director = "Unknown",
        rating = 0f,
        color = color,
    )
}

private fun defaultMovies(): List<Movie> = listOf(
    Movie("1", "Inception", "Sci-Fi", 2010, "Christopher Nolan", 8.8f, Color(0xFF6C5CE7)),
    Movie("2", "The Shawshank Redemption", "Drama", 1994, "Frank Darabont", 9.3f, Color(0xFFE17055)),
    Movie("3", "The Dark Knight", "Action", 2008, "Christopher Nolan", 9.0f, Color(0xFF2C3E50)),
    Movie("4", "Pulp Fiction", "Crime", 1994, "Quentin Tarantino", 8.9f, Color(0xFFD63031)),
    Movie("5", "Interstellar", "Sci-Fi", 2014, "Christopher Nolan", 8.6f, Color(0xFF0984E3)),
    Movie("6", "The Godfather", "Crime", 1972, "Francis Ford Coppola", 9.2f, Color(0xFF1B1B2F)),
    Movie("7", "Fight Club", "Drama", 1999, "David Fincher", 8.8f, Color(0xFFE84393)),
    Movie("8", "The Matrix", "Sci-Fi", 1999, "The Wachowskis", 8.7f, Color(0xFF27AE60)),
    Movie("9", "Parasite", "Thriller", 2019, "Bong Joon-ho", 8.5f, Color(0xFFF39C12)),
    Movie("10", "Whiplash", "Drama", 2014, "Damien Chazelle", 8.5f, Color(0xFF2D3436)),
)
