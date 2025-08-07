package io.sentry.samples.instrumentation.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.sentry.samples.instrumentation.R
import io.sentry.samples.instrumentation.data.Track

class TrackRow(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

  val deleteButton: View
    get() = findViewById(R.id.delete_track)

  val editButton: View
    get() = findViewById(R.id.edit_track)

  val infoButton: View
    get() = findViewById(R.id.track_info)

  @SuppressLint("SetTextI18n")
  fun populate(track: Track) {
    val mins = (track.millis / 1000) / 60
    val secs = (track.millis / 1000) % 60

    findViewById<TextView>(R.id.track_name).text = track.name
    findViewById<TextView>(R.id.track_duration).text = "${mins}m ${secs}s"
    findViewById<TextView>(R.id.band_name).text = track.composer
  }
}
