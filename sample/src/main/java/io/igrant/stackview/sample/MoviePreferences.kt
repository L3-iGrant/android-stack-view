package io.igrant.stackview.sample

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

object MoviePreferences {

    private const val PREF_NAME = "movie_prefs"
    private const val KEY_MOVIES = "movies"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveMovies(context: Context, movies: List<CardItem>) {
        val jsonArray = JSONArray()
        movies.forEach { movie ->
            val obj = JSONObject().apply {
                put("id", movie.id)
                put("title", movie.title)
                put("genre", movie.genre)
                put("year", movie.year)
                put("director", movie.director)
                put("rating", movie.rating)
                put("backgroundColor", movie.backgroundColor)
            }
            jsonArray.put(obj)
        }
        prefs(context).edit().putString(KEY_MOVIES, jsonArray.toString()).apply()
    }

    fun loadMovies(context: Context): MutableList<CardItem> {
        val json = prefs(context).getString(KEY_MOVIES, null) ?: return mutableListOf()
        val jsonArray = JSONArray(json)
        val list = mutableListOf<CardItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                CardItem(
                    id = obj.getString("id"),
                    title = obj.getString("title"),
                    genre = obj.getString("genre"),
                    year = obj.getInt("year"),
                    director = obj.getString("director"),
                    rating = obj.getDouble("rating").toFloat(),
                    backgroundColor = obj.getInt("backgroundColor")
                )
            )
        }
        return list
    }

    fun removeMovie(context: Context, movieId: String) {
        val movies = loadMovies(context)
        movies.removeAll { it.id == movieId }
        saveMovies(context, movies)
    }

    fun getDefaultMovies(): List<CardItem> = listOf(
        CardItem("1", "Inception", "Sci-Fi", 2010, "Christopher Nolan", 8.8f, Color.parseColor("#6C5CE7")),
        CardItem("2", "The Shawshank Redemption", "Drama", 1994, "Frank Darabont", 9.3f, Color.parseColor("#E17055")),
        CardItem("3", "The Dark Knight", "Action", 2008, "Christopher Nolan", 9.0f, Color.parseColor("#2C3E50")),
        CardItem("4", "Pulp Fiction", "Crime", 1994, "Quentin Tarantino", 8.9f, Color.parseColor("#D63031")),
        CardItem("5", "Interstellar", "Sci-Fi", 2014, "Christopher Nolan", 8.6f, Color.parseColor("#0984E3")),
        CardItem("6", "The Godfather", "Crime", 1972, "Francis Ford Coppola", 9.2f, Color.parseColor("#1B1B2F")),
        CardItem("7", "Fight Club", "Drama", 1999, "David Fincher", 8.8f, Color.parseColor("#E84393")),
        CardItem("8", "Forrest Gump", "Drama", 1994, "Robert Zemeckis", 8.8f, Color.parseColor("#00B894")),
        CardItem("9", "The Matrix", "Sci-Fi", 1999, "The Wachowskis", 8.7f, Color.parseColor("#27AE60")),
        CardItem("10", "Goodfellas", "Crime", 1990, "Martin Scorsese", 8.7f, Color.parseColor("#C0392B")),
        CardItem("11", "Spirited Away", "Animation", 2001, "Hayao Miyazaki", 8.6f, Color.parseColor("#74B9FF")),
        CardItem("12", "Parasite", "Thriller", 2019, "Bong Joon-ho", 8.5f, Color.parseColor("#F39C12")),
        CardItem("13", "Whiplash", "Drama", 2014, "Damien Chazelle", 8.5f, Color.parseColor("#2D3436")),
        CardItem("14", "The Grand Budapest Hotel", "Comedy", 2014, "Wes Anderson", 8.1f, Color.parseColor("#FD79A8")),
        CardItem("15", "Mad Max: Fury Road", "Action", 2015, "George Miller", 8.1f, Color.parseColor("#D35400")),
        CardItem("16", "Blade Runner 2049", "Sci-Fi", 2017, "Denis Villeneuve", 8.0f, Color.parseColor("#22A6B3")),
        CardItem("17", "La La Land", "Musical", 2016, "Damien Chazelle", 8.0f, Color.parseColor("#FDCB6E")),
        CardItem("18", "No Country for Old Men", "Thriller", 2007, "Coen Brothers", 8.2f, Color.parseColor("#636E72")),
        CardItem("19", "Django Unchained", "Western", 2012, "Quentin Tarantino", 8.4f, Color.parseColor("#5F27CD")),
        CardItem("20", "Everything Everywhere All at Once", "Sci-Fi", 2022, "Daniel Kwan", 8.0f, Color.parseColor("#00CEC9"))
    )
}
