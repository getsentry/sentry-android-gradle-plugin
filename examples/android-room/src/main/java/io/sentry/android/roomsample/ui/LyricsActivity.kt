package io.sentry.android.roomsample.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.Toolbar
import io.sentry.ITransaction
import io.sentry.Sentry
import io.sentry.SpanStatus
import io.sentry.android.roomsample.R
import io.sentry.android.roomsample.data.Track
import java.io.File
import java.io.FileInputStream

@SuppressLint("SetTextI18n")
class LyricsActivity : ComponentActivity() {
    private lateinit var file: File
    private lateinit var lyricsInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lyrics)

        val transaction = Sentry.startTransaction(
            "Track Interaction",
            "ui.action.lyrics",
            true
        )

        lyricsInput = findViewById(R.id.lyrics)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        val track: Track = intent.getSerializableExtra(TRACK_EXTRA_KEY) as Track
        toolbar.title = "Lyrics for ${track.name}"

        val dir = File("$filesDir${File.separatorChar}lyrics")
        dir.mkdirs()

        file = File(dir, "${track.id}.txt")
        if (file.exists()) {
            lyricsInput.setText(file.readText())
        }
        transaction.finish(SpanStatus.OK)
    }

    override fun onBackPressed() {
        val transaction = Sentry.getSpan() ?: Sentry.startTransaction(
            "Track Interaction",
            "ui.action.lyrics_finish",
            true
        )
        if (!file.exists()) {
            file.createNewFile()
        }
        file.writeText(lyricsInput.text.toString())
        transaction.finish(SpanStatus.OK)
        super.onBackPressed()
    }

    companion object {
        const val TRACK_EXTRA_KEY = "LyricsActivity.Track"
    }
}
