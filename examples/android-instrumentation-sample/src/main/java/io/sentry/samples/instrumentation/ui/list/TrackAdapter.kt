package io.sentry.samples.instrumentation.ui.list

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.samples.instrumentation.R
import io.sentry.samples.instrumentation.SampleApp
import io.sentry.samples.instrumentation.data.Track
import io.sentry.samples.instrumentation.ui.EditActivity
import io.sentry.samples.instrumentation.ui.LyricsActivity
import io.sentry.samples.instrumentation.util.Filesystem
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
      LayoutInflater.from(parent.context).inflate(R.layout.track_row, parent, false) as TrackRow
    )
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.row.populate(data[position])
    holder.row.deleteButton.setOnClickListener {
      val transaction =
        Sentry.startTransaction(
          "Track Interaction",
          "ui.action.delete",
          TransactionOptions().apply { isBindToScope = true },
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
        Intent(context, EditActivity::class.java).putExtra(EditActivity.TRACK_EXTRA_KEY, track)
      )
    }
    holder.row.infoButton.setOnClickListener {
      val context = holder.row.context
      val track = data[holder.bindingAdapterPosition]

      AlertDialog.Builder(context)
        .setTitle("Choose File API")
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        .setAdapter(
          ArrayAdapter(
            context,
            android.R.layout.select_dialog_item,
            listOf("FileIOStream", "FileReader/FileWriter", "Context.openFileInput/Output"),
          )
        ) { dialog, which ->
          context.startActivity(
            Intent(context, LyricsActivity::class.java)
              .putExtra(LyricsActivity.TRACK_EXTRA_KEY, track)
              .putExtra(LyricsActivity.FILESYSTEM_EXTRA_KEY, Filesystem.from(which))
          )
          dialog.dismiss()
        }
        .show()
    }
  }

  override fun onViewRecycled(holder: ViewHolder) {
    super.onViewRecycled(holder)
    holder.row.deleteButton.setOnClickListener(null)
    holder.row.editButton.setOnClickListener(null)
    holder.row.infoButton.setOnClickListener(null)
  }

  inner class ViewHolder(val row: TrackRow) : RecyclerView.ViewHolder(row)
}
