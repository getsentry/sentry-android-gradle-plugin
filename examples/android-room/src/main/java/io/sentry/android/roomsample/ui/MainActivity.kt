package io.sentry.android.roomsample.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.android.roomsample.R
import io.sentry.android.roomsample.SampleApp
import io.sentry.android.roomsample.ui.list.TrackAdapter
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
