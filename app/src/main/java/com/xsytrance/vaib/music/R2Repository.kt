package com.xsytrance.vaib.music

import android.content.Context
import com.xsytrance.vaib.data.VaibDatabase
import com.xsytrance.vaib.data.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

const val WORKER_URL = "https://vaibout-music.agenor.workers.dev/"

class R2Repository(context: Context) {

    private val dao = VaibDatabase.get(context).trackDao()

    fun observeTracks(): Flow<List<Track>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = URL(WORKER_URL).readText()
            val arr  = JSONArray(json)
            val entities = (0 until arr.length()).map { i ->
                val obj      = arr.getJSONObject(i)
                val tagsJson = obj.optJSONArray("tags")
                val tags     = if (tagsJson != null)
                    (0 until tagsJson.length()).joinToString(",") { tagsJson.getString(it) }
                else ""
                TrackEntity(
                    key         = obj.getString("key"),
                    url         = obj.getString("url"),
                    lrcUrl      = obj.optString("lrcUrl").nullIfEmpty(),
                    title       = obj.getString("title"),
                    artist      = obj.getString("artist"),
                    albumArtUrl = obj.optString("albumArt").nullIfEmpty(),
                    tags        = tags,
                    lyrics      = obj.optString("lyrics").nullIfEmpty(),
                    bpm         = if (obj.has("bpm") && !obj.isNull("bpm")) obj.getInt("bpm") else null,
                )
            }
            dao.replaceAll(entities)
            entities.size
        }
    }
}

private fun String?.nullIfEmpty(): String? =
    if (isNullOrEmpty() || this == "null") null else this

private fun TrackEntity.toDomain() = Track(
    key         = key,
    url         = url,
    lrcUrl      = lrcUrl,
    title       = title,
    artist      = artist,
    albumArtUrl = albumArtUrl,
    tags        = if (tags.isBlank()) emptyList()
                  else tags.split(",").filter { it.isNotBlank() },
    lyrics      = lyrics,
    bpm         = bpm,
)
