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
     * - Previous latest major
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
      fetchAgpCompatibilityTable(agpVersions)
    } catch (e: Exception) {
      print(e.printStackTrace())
      echo("Error parsing AGP compatibility table")
      throw ProgramResult(1)
    }
    // TODO: for now this is manual, but we could try get it from Gradle's github in the future
    val gradleToGroovy = mapOf("7.5".toVersion(strict = false) to "1.2", "8.11".toVersion(strict = false) to "1.7.1")

    val baseIncludes = buildList {
      for (entry in agpToGradle.entries) {
        add(
          buildMap {
            put("agp", entry.key.toString())
            // Gradle does not use .patch if it's 0 ¯\_(ツ)_/¯
            val gradle = entry.value
            val (gradleMajor, gradleMinor, gradlePatch) = gradle
            put("gradle", if (gradlePatch == 0) "${gradleMajor}.${gradleMinor}" else gradle.toString())
            // TODO: if needed we can test against different Java versions
            put("java", "17")
            val groovy = gradleToGroovy.entries.findLast { gradle >= it.key }?.value
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

  private fun fetchAgpCompatibilityTable(agpVersions: List<Version>): Map<Version, Version> {
    val gradleVersions = mutableMapOf<Version, Version>()
    val html = URL("https://developer.android.com/build/releases/gradle-plugin#updating-gradle").readText()
    val doc = Jsoup.parse(html)
    val table = doc.selectFirst("table") ?: error("No table found")
    val rows = table.select("tr")
    val headers = rows.first()?.select("th")?.map { it.text() } ?: listOf()

    // the table is in format
    // AGP version (without .patch) - Gradle version
    val agpToGradle = LinkedHashMap<Version, Version>()
    for (row in rows) {
        val cells = row.select("td").map { it.text() }
        if (cells.size > 0) {
          val agp = Version.parse(cells[0], strict = false)
          val gradle = Version.parse(cells[1], strict = false)
          agpToGradle[agp] = gradle
        }
    }

    val latest = agpToGradle.entries.first()
    for (agpVersion in agpVersions) {
      // the compat table does not contain the .patch part, so we compare major and minor 
      val entry = agpToGradle.entries.find { it.key.major == agpVersion.major && it.key.minor == agpVersion.minor }
      if (entry != null) {
        gradleVersions[agpVersion] = entry.value
      } else {
        // it's a pre-release so we use the latest compat entry
        gradleVersions[agpVersion] = latest.value
      }
    }

    return gradleVersions
  }
}

GenerateMatrix().main(args)
