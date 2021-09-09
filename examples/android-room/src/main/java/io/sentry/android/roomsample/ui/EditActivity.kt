package io.sentry.android.roomsample.ui

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.android.roomsample.R
import io.sentry.android.roomsample.SampleApp
import io.sentry.android.roomsample.data.Track
import kotlinx.coroutines.runBlocking

class EditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        val nameInput = findViewById<EditText>(R.id.track_name)
        val composerInput = findViewById<EditText>(R.id.track_composer)
        val durationInput = findViewById<EditText>(R.id.track_duration)
        val unitPriceInput = findViewById<EditText>(R.id.track_unit_price)

        findViewById<Toolbar>(R.id.toolbar).setOnMenuItemClickListener {
            if (it.itemId == R.id.action_save) {
                val transaction = Sentry.startTransaction(
                    "Add/Edit Track",
                    "ui.action.save",
                    true
                )

                val name = nameInput.text.toString()
                val composer = composerInput.text.toString()
                val duration = durationInput.text.toString()
                val unitPrice = unitPriceInput.text.toString()
                if (name.isEmpty() || composer.isEmpty() ||
                    duration.isEmpty() || duration.toLongOrNull() == null ||
                    unitPrice.isEmpty() || unitPrice.toFloatOrNull() == null
                ) {
                    Toast.makeText(
                        this,
                        "Some of the inputs are empty or have wrong format (duration/unitprice not a number)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val newTrack = Track(
                        name = name,
                        albumId = null,
                        composer = composer,
                        mediaTypeId = null,
                        genreId = null,
                        millis = duration.toLong(),
                        bytes = null,
                        price = unitPrice.toFloat()
                    )
                    runBlocking {
                        SampleApp.database.tracksDao().insert(newTrack)
                    }

                    transaction.finish(SpanStatus.OK)
                    finish()
                }
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
    }
}
