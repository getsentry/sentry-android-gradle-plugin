#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")
@file:DependsOn("com.squareup.moshi:moshi:1.15.2")
@file:DependsOn("com.squareup.moshi:moshi-kotlin:1.15.2")
@file:DependsOn("com.squareup.okhttp3:okhttp:4.12.0")
@file:DependsOn("com.google.guava:guava:33.4.0-jre")
@file:DependsOn("io.github.z4kn4fein:semver-jvm:3.0.0")
@file:DependsOn("org.jsoup:jsoup:1.17.2")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.main
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.toVersion
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import org.jsoup.Jsoup
import java.net.URL

/** Represents a matrix consumed by GitHub actions */
class Matrix(val include: List<Map<String, String>>)

data class VersionRange(val min: Version, val max: Version) {
  fun inRange(version: Version): Boolean {
    return version >= min && version <= max
  }
}

/** Generates a matrix of different build tools we use. */
class GenerateMatrix : CliktCommand() {

  // TODO: introduce params if needed
//  private val gradleProperties: File by option("--gradle-properties").file().required()
//  private val versionsToml: File by option("--versions-toml").file().required()

  @OptIn(ExperimentalStdlibApi::class)
  override fun run() {
    val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    /**
     * we test against AGP:
     * - Latest stable
     * - Pre-release alpha
     * - Pre-release beta/rc
     * - Latest version from previous major release
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    val agpVersions = try {
      fetchAgpVersions()
    } catch (e: Exception) {
      print(e.printStackTrace())
      echo("Error parsing AGP versions")
      throw ProgramResult(1)
    }

    val agpToGradle = try {
      val (legacyVersions, _) = fetchAgpCompatibilityTable(agpVersions, legacy = true)
      val (currentVersions, latestVersion) = fetchAgpCompatibilityTable(agpVersions)
      buildMap<Version, Version> {
        for (agpVersion in agpVersions) {
          put(agpVersion, legacyVersions[agpVersion] ?: currentVersions[agpVersion] ?: latestVersion)
        }
      }
    } catch (e: Exception) {
      print(e.printStackTrace())
      echo("Error parsing AGP compatibility table")
      throw ProgramResult(1)
    }

    // Fetch Kotlin minimum supported Gradle version
    val kotlinToGradleMap = try {
      fetchKotlinGradleCompatibility()
    } catch (e: Exception) {
      print(e.printStackTrace())
      echo("Error parsing Kotlin Gradle compatibility")
      throw ProgramResult(1)
    }

    // TODO: for now this is manual, but we could try get it from Gradle's github in the future
    val gradleToGroovy = mapOf("7.5".toVersion(strict = false) to "1.2", "8.11".toVersion(strict = false) to "1.7.1")
    // TODO: make it dynamic too
    val kotlinVersion = "2.1.0".toVersion()
    val baseIncludes = buildList {
      for (entry in agpToGradle.entries) {
        add(
          buildMap {
            put("agp", entry.key.toString())
            // Gradle does not use .patch if it's 0 ¯\_(ツ)_/¯
            val gradle = entry.value

            // Check if the Gradle version meets Kotlin's minimum requirement
            // Use the current Kotlin version's minimum requirement
            val kotlinMinGradle = kotlinToGradleMap.entries.find { (kotlin, _) -> kotlin.inRange(kotlinVersion) }?.value?.min
            val finalGradle = if (kotlinMinGradle != null && gradle < kotlinMinGradle) {
              echo("Warning: Gradle ${gradle} for AGP ${entry.key} is below Kotlin minimum ${kotlinMinGradle}")
              kotlinMinGradle
            } else {
              gradle
            }
            
            val (finalMajor, finalMinor, finalPatch) = finalGradle
            put("gradle", if (finalPatch == 0) "${finalMajor}.${finalMinor}" else finalGradle.toString())
            // TODO: if needed we can test against different Java versions
            put("java", "17")
            val groovy = gradleToGroovy.entries.findLast { finalGradle >= it.key }?.value
            if (groovy != null) {
              put("groovy", groovy)
            }
          }
        )
      }
    }

    val allIncludes = baseIncludes + extraIncludes(baseIncludes)

    val json = moshi.adapter<Matrix>().toJson(Matrix(allIncludes))

    // Example output: {"include":[{"agp":"8.11.0-alpha08","gradle":"8.11.1","java":"17","groovy":"1.7.1"},{"agp":"8.10.0-rc04","gradle":"8.11.1","java":"17","groovy":"1.7.1"},{"agp":"8.9.2","gradle":"8.11.1","java":"17","groovy":"1.7.1"},{"agp":"7.4.2","gradle":"7.5","java":"17","groovy":"1.2"}]}
    echo(json)
  }

  /**
   * Add extra configuration
   */
  @Suppress("UNUSED_PARAMETER")
  private fun extraIncludes(baseIncludes: List<Map<String, String>>): List<Map<String, String>> {
    return emptyList()
    //    return baseIncludes.map { matrix ->
    //      matrix.toMutableMap().apply {
    //        put("kotlin", "x.y.z")
    //      }
    //    }
  }

  /**
   * Fetches Kotlin minimum supported Gradle version mapping from the official Kotlin documentation
   * @return Map where key is Kotlin version range and value is Gradle version range
   */
  private fun fetchKotlinGradleCompatibility(): Map<VersionRange, VersionRange> {
    return try {
      // Parse the official Kotlin documentation page for compatibility info
      val html = URL("https://kotlinlang.org/docs/gradle-configure-project.html").readText()
      val doc = Jsoup.parse(html)
      
      // Look for the compatibility table
      val tables = doc.select("table")
      val compatibilityTable = tables.find { table ->
        val headers = table.select("th").map { it.text() }
        headers.any { it.contains("KGP version", ignoreCase = true) } &&
        headers.any { it.contains("Gradle", ignoreCase = true) }
      }
      
      val kotlinToGradleMap = mutableMapOf<VersionRange, VersionRange>()
      
      if (compatibilityTable != null) {
        val rows = compatibilityTable.select("tr")
        // Skip header row and process all data rows
        rows.drop(1).forEach { row ->
          val cells = row.select("td").map { it.text().trim() }
          if (cells.size >= 2) {
            val kotlinVersionRange = cells[0]
            val gradleVersionRange = cells[1]
            
            try {
              val kotlinRange = parseVersionRange(kotlinVersionRange)
              val gradleRange = parseVersionRange(gradleVersionRange)
              
              kotlinToGradleMap[kotlinRange] = gradleRange
            } catch (e: Exception) {
              echo("Warning: Could not parse Kotlin/Gradle versions: $kotlinVersionRange, $gradleVersionRange - ${e.message}")
            }
          }
        }
      }
      
      kotlinToGradleMap
    } catch (e: Exception) {
      echo("Warning: Could not fetch Kotlin compatibility from docs: ${e.message}")
      emptyMap()
    }
  }

  /**
   * Parses a version range string and returns a VersionRange object
   * Examples:
   * - "2.2.0" -> VersionRange(Version("2.2.0"), Version("2.2.0"))
   * - "2.1.20-2.1.21" -> VersionRange(Version("2.1.20"), Version("2.1.21"))
   * - "7.6.3–8.14" -> VersionRange(Version("7.6.3"), Version("8.14"))
   */
  private fun parseVersionRange(versionRangeString: String): VersionRange {
    // Check if it's a range or single version
    val rangeSeparators = listOf("–", "-", "—") // different dash types
    val separator = rangeSeparators.find { versionRangeString.contains(it) }
    
    if (separator != null) {
      // It's a range
      val parts = versionRangeString.split(separator, limit = 2)
      if (parts.size == 2) {
        val startVersion = sanitizeVersion(parts[0].trim())
        val endVersion = sanitizeVersion(parts[1].trim())
        
        val start = startVersion.toVersion(strict = false)
        val end = endVersion.toVersion(strict = false)
        
        return VersionRange(start, end)
      } else {
        throw IllegalArgumentException("Invalid range format: $versionRangeString")
      }
    } else {
      // Single version
      val cleanVersion = sanitizeVersion(versionRangeString)
      val version = cleanVersion.toVersion(strict = false)
      return VersionRange(version, version)
    }
  }

  /**
   * Sanitizes version strings by removing or converting special characters
   * that are not compatible with semantic versioning
   */
  private fun sanitizeVersion(versionString: String): String {
    return versionString
      .replace("*", "") // Remove asterisks (e.g., "8.10*" -> "8.10")
      .replace("+", "") // Remove plus signs (e.g., "8.10+" -> "8.10")
      .replace("\\", "") // Remove backslashes
      .trim()
  }

  private fun fetchAgpVersions(): List<Version> {
    val semvers = mutableListOf<Version>()
    val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val document = documentBuilder.parse("https://dl.google.com/dl/android/maven2/com/android/tools/build/group-index.xml")
    document.documentElement.normalize()

    val root = document.documentElement
    val gradleElements = root.getElementsByTagName("gradle")
    for (i in 0 until gradleElements.length) {
      val element = gradleElements.item(i) as Element
      val versions = element.getAttribute("versions")
      versions.split(",").forEach { version ->
        val semver = Version.parse(version, strict = false)
        semvers += semver
      }
    }

    // AGP usually has two pre-releases at the same time
    val latestPreRelease = semvers.last { it.isPreRelease }
    val secondToLatestPreRelease = semvers.last { it.isPreRelease && it.minor == (latestPreRelease.minor - 1) }
    val latest = semvers.last { it.isStable }
    val previousMajorLatest = semvers.last { it.isStable && it.major == (latest.major - 1)}
    return listOf(latestPreRelease, secondToLatestPreRelease, latest, previousMajorLatest)
  }

  private fun fetchAgpCompatibilityTable(agpVersions: List<Version>, legacy: Boolean = false): Pair<Map<Version, Version>, Version> {
    val gradleVersions = mutableMapOf<Version, Version>()
    val html = URL("https://developer.android.com/build/releases/gradle-plugin#updating-gradle").readText()
    val doc = Jsoup.parse(html)
    val tables = doc.select("table") ?: error("No table found")
    val table = if (legacy) tables[1] else tables[0]
    val rows = table.select("tr")
    val headers = rows.first()?.select("th")?.map { it.text() } ?: listOf()

    if (headers.none { it.contains("plugin", ignoreCase = true) }) {
      error("Wrong table selected")
    }

    // the table is in format
    // AGP version (without .patch) - Gradle version
    val agpToGradle = LinkedHashMap<Version, Version>()
    for (row in rows) {
        val cells = row.select("td").map { it.text() }
        if (cells.size > 0) {
          val agp = try {
            Version.parse(cells[0], strict = false)
          } catch (e: Throwable) {
            // if version cant be parsed, we're probably past the versions we're interested in
            break
          }
          val gradle = try {
            Version.parse(cells[1], strict = false)
          } catch (e: Throwable) {
            // if version cant be parsed, we're probably past the versions we're interested in
            break
          }
          agpToGradle[agp] = gradle
        }
    }

    val latest = agpToGradle.entries.first()
    for (agpVersion in agpVersions) {
      // the compat table does not contain the .patch part, so we compare major and minor 
      val entry = agpToGradle.entries.find { it.key.major == agpVersion.major && it.key.minor == agpVersion.minor }
      if (entry != null) {
        gradleVersions[agpVersion] = entry.value
      }
    }

    return gradleVersions to latest.value
  }
}

GenerateMatrix().main(args)
