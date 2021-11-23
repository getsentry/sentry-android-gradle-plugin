package io.sentry.android.roomsample.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.android.roomsample.R
import io.sentry.android.roomsample.SampleApp
import io.sentry.android.roomsample.data.Track
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

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
                    val transaction = Sentry.startTransaction(
                        "Track Interaction",
                        "ui.action.load",
                        true
                    )
                    (list.adapter as TrackAdapter).populate(it)
                    transaction.finish(SpanStatus.OK)
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

    val deleteButton: View get() = findViewById(R.id.delete_track)
    val editButton: View get() = findViewById(R.id.edit_track)
    val infoButton: View get() = findViewById(R.id.track_info)

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
        holder.row.deleteButton.setOnClickListener {
            val transaction = Sentry.startTransaction(
                "Track Interaction",
                "ui.action.delete",
                true
            )
            runBlocking {
                SampleApp.database.tracksDao().delete(data[holder.bindingAdapterPosition])
                val deleteCount = SampleApp.analytics.getInt("delete_count", 0) + 1
                SampleApp.analytics.edit().putInt("delete_count", deleteCount).apply()
            }
            transaction.finish(SpanStatus.OK)
        }
        holder.row.editButton.setOnClickListener {
            val context = holder.row.context
            val track = data[holder.bindingAdapterPosition]
            context.startActivity(
                Intent(
                    context,
                    EditActivity::class.java
                ).putExtra(EditActivity.TRACK_EXTRA_KEY, track)
            )
        }
        holder.row.infoButton.setOnClickListener {
            val context = holder.row.context
            val track = data[holder.bindingAdapterPosition]
            context.startActivity(
                Intent(
                    context,
                    LyricsActivity::class.java
                ).putExtra(LyricsActivity.TRACK_EXTRA_KEY, track)
            )
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.row.deleteButton.setOnClickListener(null)
        holder.row.editButton.setOnClickListener(null)
    }

    inner class ViewHolder(val row: TrackRow) : RecyclerView.ViewHolder(row)
}
