package io.sentry.samples.instrumentation.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar
//import io.sentry.Sentry
//import io.sentry.SpanStatus
import io.sentry.samples.instrumentation.R
import io.sentry.samples.instrumentation.data.Track
import io.sentry.samples.instrumentation.util.Filesystem
import java.io.File

@SuppressLint("SetTextI18n")
class LyricsActivity : ComponentActivity() {
    private lateinit var lyricsInput: EditText
    private lateinit var filesystem: Filesystem
    private lateinit var track: Track

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lyrics)

//        val transaction = Sentry.startTransaction(
//            "Track Interaction",
//            "ui.action.lyrics",
//            true
//        )

        lyricsInput = findViewById(R.id.lyrics)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        track = intent.getSerializableExtra(TRACK_EXTRA_KEY) as Track
        filesystem = intent.getSerializableExtra(FILESYSTEM_EXTRA_KEY) as Filesystem
        toolbar.title = "Lyrics for ${track.name}"

        val dir = File("$filesDir${File.separatorChar}lyrics")
        dir.mkdirs()

        lyricsInput.setText(filesystem.read(this, "${track.id}.txt"))
//        transaction.finish(SpanStatus.OK)
    }

    override fun onBackPressed() {
//        val transaction = Sentry.getSpan() ?: Sentry.startTransaction(
//            "Track Interaction",
//            "ui.action.lyrics_finish",
//            true
//        )
        filesystem.write(this, "${track.id}.txt", lyricsInput.text.toString())
//        transaction.finish(SpanStatus.OK)
        super.onBackPressed()
    }

    companion object {
        const val TRACK_EXTRA_KEY = "LyricsActivity.Track"
        const val FILESYSTEM_EXTRA_KEY = "LyricsActivity.Filesystem"
    }
}
