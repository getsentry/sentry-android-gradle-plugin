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

        val originalTrack: Track? = intent.getSerializableExtra(TRACK_EXTRA_KEY) as? Track
        originalTrack?.run {
            nameInput.setText(name)
            composerInput.setText(composer)
            durationInput.setText(millis.toString())
            unitPriceInput.setText(price.toString())
        }

        findViewById<Toolbar>(R.id.toolbar).setOnMenuItemClickListener {
            if (it.itemId == R.id.action_save) {
                val transaction = Sentry.startTransaction(
                    "Track Interaction",
                    if (originalTrack == null) "ui.action.add" else "ui.action.edit",
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
                        "Some of the inputs are empty or have wrong format " +
                            "(duration/unitprice not a number)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    if (originalTrack == null) {
                        addNewTrack(name, composer, duration.toLong(), unitPrice.toFloat())

                        val createCount = SampleApp.analytics.getInt("create_count", 0) + 1
                        SampleApp.analytics.edit().putInt("create_count", createCount).apply()
                    } else {
                        originalTrack.update(name, composer, duration.toLong(), unitPrice.toFloat())

                        val editCount = SampleApp.analytics.getInt("edit_count", 0) + 1
                        SampleApp.analytics.edit().putInt("edit_count", editCount).apply()
                    }
                    transaction.finish(SpanStatus.OK)
                    finish()
                }
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
    }

    private fun addNewTrack(name: String, composer: String, duration: Long, unitPrice: Float) {
        val newTrack = Track(
            name = name,
            albumId = null,
            composer = composer,
            mediaTypeId = null,
            genreId = null,
            millis = duration,
            bytes = null,
            price = unitPrice
        )
        runBlocking {
            SampleApp.database.tracksDao().insert(newTrack)
        }
    }

    private fun Track.update(name: String, composer: String, duration: Long, unitPrice: Float) {
        val updatedTrack = copy(
            name = name,
            composer = composer,
            millis = duration,
            price = unitPrice
        )
        runBlocking {
            SampleApp.database.tracksDao().update(updatedTrack)
        }
    }

    companion object {
        const val TRACK_EXTRA_KEY = "EditActivity.Track"
    }
}
