package io.sentry.samples.instrumentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.sentry.Sentry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class ViewModel {
    val randomNumber = mutableStateOf(42)

    fun onRandomNumberClicked() {
        GlobalScope.launch {
            val span = Sentry.getSpan()?.startChild("op.random_calculation")
            delay(1000)
            randomNumber.value = Random.nextInt(100)
            span?.finish()
        }
    }
}

class ComposeActivity : ComponentActivity() {

    private val model = ViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = Destination.Home.route
            ) {
                val pillShape = RoundedCornerShape(50)

                composable(Destination.Home.route) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BasicText(
                            modifier = Modifier
                                .border(2.dp, Color.Gray, pillShape)
                                .clip(pillShape)
                                .clickable(onClickLabel = "Details.Route") {
                                    navController.navigate(Destination.Details.route)
                                }
                                .padding(24.dp),
                            text = "Home. Tap to go to Details."
                        )

                        Spacer(modifier = Modifier.size(16.dp))

                        BasicText(
                            modifier = Modifier
                                .border(2.dp, Color.Gray, pillShape)
                                .clip(pillShape)
                                .clickable(onClickLabel = "Generate Random number") {
                                    model.onRandomNumberClicked()
                                }
                                .padding(24.dp),
                            text = "${model.randomNumber.value}. " +
                                "Tap to calculate a new random number."
                        )
                    }
                }
                composable(Destination.Details.route) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BasicText(
                            modifier = Modifier
                                .border(2.dp, Color.Gray, pillShape)
                                .clip(pillShape)
                                .clickable {
                                    navController.popBackStack()
                                }
                                .padding(24.dp),
                            text = "Details. Tap or press back to return."
                        )
                    }
                }
            }
        }
    }

    sealed class Destination(
        val route: String
    ) {
        object Home : Destination("home")
        object Details : Destination("details")
    }
}
