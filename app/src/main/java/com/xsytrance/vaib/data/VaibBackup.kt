package com.xsytrance.vaib.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.xsytrance.vaib.data.entities.VaibEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Export vAIbs to JSON and import from JSON.
 */
object VaibBackup {

    private const val VERSION = 1

    /**
     * Export a list of vAIbs to a JSON file and share it.
     */
    suspend fun exportVaibs(context: Context, vaibs: List<VaibEntity>): Uri? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("version", VERSION)
                put("app", "vAIb out!")
                put("exportedAt", System.currentTimeMillis())
                put("vaibs", JSONArray().apply {
                    vaibs.forEach { vaib ->
                        put(JSONObject().apply {
                            put("vaibName", vaib.vaibName)
                            put("trackUri", vaib.trackUri)
                            put("trackName", vaib.trackName)
                            put("mood", vaib.mood)
                            put("visualizerStyle", vaib.visualizerStyle)
                            put("themeId", vaib.themeId)
                            put("createdAt", vaib.createdAt)
                            put("sourceType", vaib.sourceType)
                            put("eqPreset", vaib.eqPreset)
                        })
                    }
                })
            }

            val file = File(context.cacheDir, "vaib_backup_${System.currentTimeMillis()}.json")
            file.writeText(json.toString(2))

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Import vAIbs from a JSON URI. Returns list of VaibEntity to insert.
     */
    suspend fun importVaibs(context: Context, uri: Uri): List<VaibEntity> = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                ?: return@withContext emptyList()
            val root = JSONObject(json)
            val vaibs = root.getJSONArray("vaibs")
            val result = mutableListOf<VaibEntity>()
            for (i in 0 until vaibs.length()) {
                val obj = vaibs.getJSONObject(i)
                result.add(
                    VaibEntity(
                        vaibName = obj.optString("vaibName", "Imported vAIb"),
                        trackUri = obj.optString("trackUri", ""),
                        trackName = obj.optString("trackName", "Unknown Track"),
                        mood = obj.optString("mood", ""),
                        visualizerStyle = obj.optString("visualizerStyle", "NEBULA"),
                        themeId = obj.optString("themeId", "NEON_CYAN"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        sourceType = obj.optString("sourceType", "LOCAL"),
                        eqPreset = obj.optString("eqPreset", "FLAT"),
                    ),
                )
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }
}
