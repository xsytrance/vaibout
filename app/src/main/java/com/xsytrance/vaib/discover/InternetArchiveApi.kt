package com.xsytrance.vaib.discover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Thin, no-extra-dep wrapper around the Internet Archive public API.
 *
 * Search  → advancedsearch.php (JSON)
 * Resolve → metadata/{identifier} (JSON)
 * Stream  → https://archive.org/download/{id}/{filename}
 *
 * All calls are suspended and dispatched to Dispatchers.IO.
 */
object InternetArchiveApi {

    private const val BASE = "https://archive.org"

    // Preferred audio formats in resolution priority order
    private val AUDIO_EXTS = listOf(".mp3", ".ogg", ".opus", ".flac")

    /**
     * Fetches audio items from Internet Archive.
     *
     * Empty [query] → top netlabels CC music (curated default).
     * Non-empty [query] → keyword search across all IA audio.
     */
    suspend fun fetchItems(query: String = ""): List<ArchiveItem> = withContext(Dispatchers.IO) {
        try {
            val q = if (query.isBlank()) {
                "collection:netlabels AND mediatype:audio"
            } else {
                "${query.trim()} AND mediatype:audio"
            }
            val encoded = URLEncoder.encode(q, "UTF-8")
            val url = "$BASE/advancedsearch.php" +
                "?q=$encoded" +
                "&fl[]=identifier&fl[]=title&fl[]=creator" +
                "&rows=25&sort[]=downloads%20desc&output=json"
            parseSearch(get(url))
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun resolveStreamUrl(id: String): String? = withContext(Dispatchers.IO) {
        try {
            parseStreamUrl(id, get("$BASE/metadata/$id"))
        } catch (_: Exception) {
            null
        }
    }

    // ── HTTP ──────────────────────────────────────────────────────────

    private fun get(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout    = 20_000
        conn.setRequestProperty("User-Agent", "vAIb-Android/1.0")
        return try {
            conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
        } finally {
            conn.disconnect()
        }
    }

    // ── Parsers ───────────────────────────────────────────────────────

    private fun parseSearch(json: String): List<ArchiveItem> = try {
        val docs = JSONObject(json)
            .getJSONObject("response")
            .getJSONArray("docs")
        (0 until docs.length()).mapNotNull { i ->
            val obj = docs.getJSONObject(i)
            val id = obj.optString("identifier").ifEmpty { return@mapNotNull null }
            ArchiveItem(
                id      = id,
                title   = obj.optString("title", "Untitled"),
                creator = extractCreator(obj),
            )
        }
    } catch (_: Exception) { emptyList() }

    private fun extractCreator(obj: JSONObject): String {
        if (!obj.has("creator")) return ""
        return try {
            when (val c = obj.get("creator")) {
                is JSONArray -> c.optString(0, "")
                else         -> c.toString()
            }
        } catch (_: Exception) { "" }
    }

    private fun parseStreamUrl(id: String, json: String): String? {
        return try {
            val files = JSONObject(json).getJSONArray("files")
            for (ext in AUDIO_EXTS) {
                for (i in 0 until files.length()) {
                    val name = files.getJSONObject(i).optString("name", "")
                    if (name.endsWith(ext, ignoreCase = true)) {
                        return "$BASE/download/$id/$name"
                    }
                }
            }
            null
        } catch (_: Exception) { null }
    }
}
