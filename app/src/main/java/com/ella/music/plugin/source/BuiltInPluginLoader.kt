package com.ella.music.plugin.source

import android.content.Context
import com.ella.music.plugin.model.PluginCapability
import com.ella.music.plugin.model.PluginManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BuiltInPluginLoader(
    private val context: Context,
    private val json: Json = pluginJson
) {
    suspend fun loadPlugins(): List<BuiltInPluginSource> = withContext(Dispatchers.IO) {
        val root = BUILTIN_ROOT
        context.assets.list(root).orEmpty()
            .filter { it.isNotBlank() }
            .sorted()
            .mapNotNull { dir ->
                runCatching {
                    val assetDir = "$root/$dir"
                    val manifest = json.decodeFromString<PluginManifest>(readAsset("$assetDir/manifest.json"))
                    if (PluginCapability.SEARCH_SONGS !in manifest.capabilities ||
                        PluginCapability.GET_LYRICS !in manifest.capabilities
                    ) return@runCatching null
                    BuiltInPluginSource(
                        manifest = manifest,
                        assetDir = assetDir,
                        script = buildScript(assetDir, manifest)
                    )
                }.getOrNull()
            }
    }

    private fun buildScript(assetDir: String, manifest: PluginManifest): String {
        val includeSources = manifest.includeDirs
            .flatMap { includeDir ->
                listJsFiles("$assetDir/$includeDir")
                    .map { path ->
                        IncludedScript(
                            path = "$includeDir/${path.substringAfter("$assetDir/$includeDir/")}",
                            content = readAsset(path)
                        )
                    }
            }
            .sortedBy { it.path }
        val includePathSetJson = json.encodeToString(includeSources.map { it.path }.toSet())
        return buildString {
            append(
                """
                (function() {
                  var __lyricoDeclaredIncludes = $includePathSetJson;
                  var __lyricoDeclaredIncludeMap = Object.create(null);
                  __lyricoDeclaredIncludes.forEach(function(path) {
                    __lyricoDeclaredIncludeMap[path] = true;
                  });
                  globalThis.include = function(path) {
                    path = String(path || "");
                    if (!Object.prototype.hasOwnProperty.call(__lyricoDeclaredIncludeMap, path)) {
                      throw new Error("Include path is not declared in includeDirs: " + path);
                    }
                  };
                })();
                """.trimIndent()
            )
            includeSources.forEach { source ->
                append("\n;\n// ===== Platform include: ${source.path} =====\n")
                append(source.content)
                append("\n//# sourceURL=${source.path}\n")
            }
            append("\n;\n// ===== Platform entry: ${manifest.entry} =====\n")
            append(readAsset("$assetDir/${manifest.entry}"))
            append("\n//# sourceURL=${manifest.entry}\n")
        }
    }

    private fun listJsFiles(assetDir: String): List<String> {
        val children = context.assets.list(assetDir).orEmpty()
        if (children.isEmpty()) return if (assetDir.endsWith(".js", ignoreCase = true)) listOf(assetDir) else emptyList()
        return children.flatMap { child ->
            val childPath = "$assetDir/$child"
            val nested = context.assets.list(childPath).orEmpty()
            if (nested.isEmpty()) {
                listOf(childPath).filter { it.endsWith(".js", ignoreCase = true) }
            } else {
                listJsFiles(childPath)
            }
        }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private data class IncludedScript(val path: String, val content: String)

    companion object {
        const val BUILTIN_ROOT = "lyrico_plugins"
    }
}

val pluginJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}
