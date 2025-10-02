package io.sentry.samples.instrumentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.sentry.samples.instrumentation.ui.ComposeActivity.Destination

class ComposeActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val navController = rememberNavController()

      NavHost(navController = navController, startDestination = Destination.Home.route) {
        val pillShape = RoundedCornerShape(50)

        composable(Destination.Home.route) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            HomeText(navController, pillShape)
          }
        }
        composable(Destination.Details.route) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            DetailsText(navController, pillShape)
          }
        }
      }
    }
  }

  sealed class Destination(val route: String) {
    object Home : Destination("home")

    object Details : Destination("details")
  }
}

@Composable
fun HomeText(navController: NavController, pillShape: RoundedCornerShape) {
  BasicText(
    modifier =
      Modifier.border(2.dp, Color.Gray, pillShape)
        .clip(pillShape)
        .clickable { navController.navigate(Destination.Details.route) }
        .padding(24.dp),
    text = "Home. Tap to go to Details.",
  )
}

@Composable
fun DetailsText(navController: NavController, pillShape: RoundedCornerShape) {
  BasicText(
    modifier =
      Modifier
        .border(2.dp, Color.Gray, pillShape)
        .clip(pillShape)
        .clickable { navController.popBackStack() }
        .padding(24.dp),
    text = "Details. Tap or press back to return.",
  )
}
