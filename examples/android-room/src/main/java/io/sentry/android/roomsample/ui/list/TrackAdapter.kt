package io.sentry.android.roomsample.ui.list

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.android.roomsample.R
import io.sentry.android.roomsample.SampleApp
import io.sentry.android.roomsample.data.Track
import io.sentry.android.roomsample.ui.EditActivity
import io.sentry.android.roomsample.ui.LyricsActivity
import io.sentry.android.roomsample.ui.TrackRow
import kotlinx.coroutines.runBlocking

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
