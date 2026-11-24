package com.example.domain.golu2

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var currentFile: File? = null
    private var startedAtMs: Long = 0L

    fun startRecording(): Result<File> = runCatching {
        stopRecording()
        val dir = File(context.filesDir, "golu_recordings").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(dir, "golu_rec_$stamp.m4a")
        val mr =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
        mr.setAudioSource(MediaRecorder.AudioSource.MIC)
        mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mr.setAudioEncodingBitRate(96_000)
        mr.setAudioSamplingRate(44_100)
        mr.setOutputFile(outFile.absolutePath)
        mr.prepare()
        mr.start()
        recorder = mr
        currentFile = outFile
        startedAtMs = System.currentTimeMillis()
        outFile
    }

    fun stopRecording(): Pair<File?, Int> {
        val duration =
            if (startedAtMs > 0L) ((System.currentTimeMillis() - startedAtMs) / 1000L).toInt().coerceAtLeast(1)
            else 0
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        startedAtMs = 0L
        return currentFile to duration
    }

    fun playFile(path: String, onCompleted: () -> Unit = {}): Boolean {
        if (path.isBlank() || !File(path).exists()) return false
        stopPlayback()
        return runCatching {
            player =
                MediaPlayer().apply {
                    setDataSource(path)
                    prepare()
                    setOnCompletionListener {
                        onCompleted()
                    }
                    start()
                }
            true
        }.getOrDefault(false)
    }

    fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    fun deleteLocalFile(path: String) {
        if (path.isBlank()) return
        runCatching { File(path).delete() }
    }

    fun release() {
        stopRecording()
        stopPlayback()
    }
}
