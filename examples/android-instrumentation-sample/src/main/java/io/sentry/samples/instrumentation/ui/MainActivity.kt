package io.sentry.samples.instrumentation.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.TransactionOptions
import io.sentry.samples.instrumentation.R
import io.sentry.samples.instrumentation.SampleApp
import io.sentry.samples.instrumentation.network.TrackService
import io.sentry.samples.instrumentation.ui.list.TrackAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val list = findViewById<RecyclerView>(R.id.list)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = TrackAdapter()

        lifecycleScope.launchWhenStarted {
            Sentry.getSpan()?.finish()
            val transaction = Sentry.startTransaction(
                "Track Interaction",
                "ui.action.load",
                TransactionOptions().apply { isBindToScope = true }
            )
            SampleApp.database.tracksDao()
                .all()
                .map {
                    val remote = withContext(Dispatchers.IO) {
                        TrackService.instance.tracks("6188aa82-3102-436a-9a68-513e6ad9efcb")
                    }
                    remote + it
                }
                .collect {
                    (list.adapter as TrackAdapter).populate(it)
                    transaction.finish(SpanStatus.OK)
                }
        }

        findViewById<Toolbar>(R.id.toolbar).setOnMenuItemClickListener {
            if (it.itemId == R.id.action_add) {
                startActivity(Intent(this, EditActivity::class.java))
                return@setOnMenuItemClickListener true
            }
            if (it.itemId == R.id.action_compose) {
                startActivity(Intent(this, ComposeActivity::class.java))
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
    }
}
