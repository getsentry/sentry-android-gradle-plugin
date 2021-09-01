package io.sentry.android.roomsample.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.sentry.Sentry
import io.sentry.android.roomsample.R
import io.sentry.android.roomsample.SampleApp
import io.sentry.android.roomsample.data.Track

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = TrackAdapter()

        lifecycleScope.launchWhenStarted {
            val tracks: List<Track> =
                SampleApp.database.tracksDao().allByArtist("Red Hot Chili Peppers")
            (list.adapter as TrackAdapter).populate(tracks)

            // this is to make sure that DB query spans are sent to Sentry, as they are attached to the Cold Start Transaction, otherwise they get dropped
            Sentry.getSpan()?.finish()
        }
    }
}

class TrackRow(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {
    fun populate(track: Track) {
        findViewById<TextView>(R.id.track_name).text = track.name
        findViewById<TextView>(R.id.track_duration).text = track.millis.toString()
        findViewById<TextView>(R.id.band_name).text = track.composer
    }
}

class TrackAdapter : RecyclerView.Adapter<TrackAdapter.ViewHolder>() {

    private var data: List<Track> = listOf()

    fun populate(data: List<Track>) {
        this.data = data
        notifyDataSetChanged()
    }

    override fun getItemCount() = data.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.track_row,
                parent,
                false
            ) as TrackRow
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.row.populate(data[position])
    }

    inner class ViewHolder(val row: TrackRow) : RecyclerView.ViewHolder(row)
}
