package io.sentry.samples.navigation3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Keys of the outer back stack. With auto-instrumentation, each is recorded under its
// kotlinx.serialization serial name: the fully qualified class name, or its @SerialName value.
@Serializable @SerialName("Home") data object Home

@Serializable @SerialName("Profile") data class Profile(val userId: String)

// No @SerialName, so it's recorded under its fully qualified class name.
@Serializable data object Settings

// Hosts a nested NavDisplay. Only the outer NavDisplay records; the nested one stays inactive.
@Serializable @SerialName("Tabs") data object Tabs

// Not @Serializable, so it's recorded as /unknown.
data class Legacy(val id: Int)

// Keys of the nested back stack inside Tabs.
@Serializable @SerialName("Feed") data object Feed

@Serializable @SerialName("FeedItem") data class FeedItem(val id: Int)

/**
 * A Navigation 3 app that doesn't call SentryNavEffect. The Sentry Android Gradle Plugin's
 * navigation3 auto-instrumentation records its navigation transactions, breadcrumbs, and screen
 * names.
 */
class Navigation3Activity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val backStack = remember { mutableStateListOf<Any>(Home) }

      NavDisplay(
        modifier = Modifier.safeDrawingPadding(),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
          entryProvider {
            entry<Home> {
              Screen("Home") {
                Button("Profile 42") { backStack.add(Profile("42")) }
                Button("Settings") { backStack.add(Settings) }
                Button("Tabs (nested NavDisplay)") { backStack.add(Tabs) }
                Button("Legacy (not @Serializable)") { backStack.add(Legacy(1)) }
              }
            }
            entry<Profile> { key ->
              Screen("Profile ${key.userId}") {
                Button("Settings") { backStack.add(Settings) }
                Button("Back") { backStack.removeLastOrNull() }
              }
            }
            entry<Settings> {
              Screen("Settings") {
                Button("Replace with Home") {
                  backStack.clear()
                  backStack.add(Home)
                }
                Button("Back") { backStack.removeLastOrNull() }
              }
            }
            entry<Tabs> { TabsScreen(onBack = { backStack.removeLastOrNull() }) }
            entry<Legacy> { key ->
              Screen("Legacy ${key.id}") { Button("Back") { backStack.removeLastOrNull() } }
            }
          },
      )
    }
  }
}

@Composable
private fun TabsScreen(onBack: () -> Unit) {
  val tabBackStack = remember { mutableStateListOf<Any>(Feed) }

  Column(modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
      Button("Back to outer stack", onClick = onBack)
    }
    NavDisplay(
      modifier = Modifier.weight(1f),
      backStack = tabBackStack,
      onBack = { tabBackStack.removeLastOrNull() },
      entryProvider =
        entryProvider {
          entry<Feed> {
            Screen("Feed (nested)") {
              Button("Item 1") { tabBackStack.add(FeedItem(1)) }
              Button("Item 2") { tabBackStack.add(FeedItem(2)) }
            }
          }
          entry<FeedItem> { key ->
            Screen("Feed item ${key.id} (nested)") {
              Button("Back") { tabBackStack.removeLastOrNull() }
            }
          }
        },
    )
  }
}

@Composable
private fun Screen(title: String, content: @Composable () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
  ) {
    BasicText(title)
    content()
  }
}

@Composable
private fun Button(text: String, onClick: () -> Unit) {
  BasicText(
    text = text,
    modifier = Modifier.border(1.dp, Color.Gray).clickable(onClick = onClick).padding(12.dp),
  )
}
