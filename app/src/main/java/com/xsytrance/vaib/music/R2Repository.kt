package com.xsytrance.vaib.music

import android.content.Context
import android.net.Uri
import com.xsytrance.vaib.data.VaibDatabase
import com.xsytrance.vaib.data.entities.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONArray
import java.net.URL
import java.text.Normalizer

const val WORKER_URL = "https://vaibout-music.agenor.workers.dev/"

private const val ALBUM_ART_BASE_URL = "https://pub-e9f979edfc5542a1b6d5c37e32537565.r2.dev/album-art/"

private val FALLBACK_ALBUM_ART_BY_KEY: Map<String, String> = mapOf(
    "23respuestas" to "${ALBUM_ART_BASE_URL}23respuestas.png",
    "cairostilldancing" to "${ALBUM_ART_BASE_URL}cairostilldancing.png",
    "ceasefireinthestatic" to "${ALBUM_ART_BASE_URL}ceasefireinthestatic.png",
    "cocktailsandcode" to "${ALBUM_ART_BASE_URL}cocktailsandcode.png",
    "differentthissummer" to "${ALBUM_ART_BASE_URL}differentthissummer.png",
    "iwontbeyourfire-japanese" to "${ALBUM_ART_BASE_URL}iwontbeyourfire-japanese.png",
    "iwontbeyourfire" to "${ALBUM_ART_BASE_URL}iwontbeyourfire.png",
    "levelready" to "${ALBUM_ART_BASE_URL}levelready.png",
    "migente" to "${ALBUM_ART_BASE_URL}migente.png",
    "moveover" to "${ALBUM_ART_BASE_URL}moveover.png",
    "mysoullivesinseoul" to "${ALBUM_ART_BASE_URL}mysoullivesinseoul.png",
    "paperthatcutyou" to "${ALBUM_ART_BASE_URL}paperthatcutyou.png",
    "stillmestillyou" to "${ALBUM_ART_BASE_URL}stillmestillyou.png",
    "voidintogold-forgedabovegold" to "${ALBUM_ART_BASE_URL}voidintogold-forgedabovegold.png",
    "voidintogold" to "${ALBUM_ART_BASE_URL}voidintogold.png",
    "whistleontheriver" to "${ALBUM_ART_BASE_URL}whistleontheriver.png",
)

class R2Repository(context: Context) {

    private val dao = VaibDatabase.get(context).trackDao()
    private val prefs = context.getSharedPreferences("vaib_art_overrides", Context.MODE_PRIVATE)

    fun setArtworkOverride(trackKey: String, uri: String) {
        prefs.edit().putString(trackKey, uri).apply()
    }

    private fun artworkOverride(trackKey: String): String? = prefs.getString(trackKey, null)

    fun observeTracks(): Flow<List<Track>> = dao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun refresh(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val json = URL(WORKER_URL).readText()
            val arr  = JSONArray(json)
            val entities = (0 until arr.length()).map { i ->
                val obj      = arr.getJSONObject(i)
                val key      = obj.getString("key")
                val tagsJson = obj.optJSONArray("tags")
                val tags     = if (tagsJson != null)
                    (0 until tagsJson.length()).joinToString(",") { tagsJson.getString(it) }
                else ""
                val remoteArt = obj.optString("albumArt").nullIfEmpty()
                    ?: fallbackAlbumArtForKey(key)
                val resolvedArt = artworkOverride(key)
                    ?: remoteArt?.takeIf { urlLooksReachable(it) }
                TrackEntity(
                    key         = key,
                    url         = obj.getString("url"),
                    lrcUrl      = obj.optString("lrcUrl").nullIfEmpty(),
                    title       = obj.getString("title"),
                    artist      = obj.getString("artist"),
                    albumArtUrl = resolvedArt,
                    tags        = tags,
                    lyrics      = obj.optString("lyrics").nullIfEmpty(),
                    bpm         = if (obj.has("bpm") && !obj.isNull("bpm")) obj.getInt("bpm") else null,
                )
            }
            dao.replaceAll(entities)
            entities.size
        }.onFailure { Log.e("R2Repository", "refresh failed", it) }
    }
}

private fun fallbackAlbumArtForKey(key: String): String? {
    FALLBACK_ALBUM_ART_BY_KEY[key]?.let { return it }
    val normalized = key
        .lowercase()
        .removePrefix("xsytrance - ")
        .removeSuffix(".mp3")
        .let { Normalizer.normalize(it, Normalizer.Form.NFKD) }
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\([^)]*\\)"), "")
        .replace(Regex("[^a-z0-9]+"), "")
    return FALLBACK_ALBUM_ART_BY_KEY[normalized]?.ifBlank { null }
        ?: normalized.takeIf { it.isNotBlank() }?.let { "${ALBUM_ART_BASE_URL}${it}.png" }
}

private fun urlLooksReachable(url: String): Boolean = runCatching {
    val uri = Uri.parse(url)
    if (uri.scheme == "content") return true
    val conn = URL(url).openConnection()
    conn.connectTimeout = 4000
    conn.readTimeout = 4000
    conn.getInputStream().use { it.read(ByteArray(1)) }
    true
}.getOrElse { false }

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
