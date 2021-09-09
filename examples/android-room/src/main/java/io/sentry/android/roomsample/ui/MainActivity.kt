package io.sentry.android.roomsample.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.sentry.Sentry
import io.sentry.android.roomsample.R
import io.sentry.android.roomsample.SampleApp
import io.sentry.android.roomsample.data.Track
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = TrackAdapter()

        lifecycleScope.launchWhenStarted {
            SampleApp.database.tracksDao()
                .all()
                .collect {
                    (list.adapter as TrackAdapter).populate(it)
                    // this is to make sure that DB query spans are sent to Sentry, as they are attached to the Cold Start Transaction, otherwise they get dropped
                    Sentry.getSpan()?.finish()
                }
        }

        findViewById<Toolbar>(R.id.toolbar).setOnMenuItemClickListener {
            if (it.itemId == R.id.action_add) {
                startActivity(Intent(this, EditActivity::class.java))
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
    }
}

class TrackRow(
    context: Context,
    attrs: AttributeSet
) : LinearLayout(context, attrs) {

    @SuppressLint("SetTextI18n")
    fun populate(track: Track) {
        val mins = (track.millis / 1000) / 60
        val secs = (track.millis / 1000) % 60

        findViewById<TextView>(R.id.track_name).text = track.name
        findViewById<TextView>(R.id.track_duration).text = "${mins}m ${secs}s"
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
