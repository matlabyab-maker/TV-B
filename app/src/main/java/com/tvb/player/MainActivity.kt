package com.tvb.player

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.setPadding
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.net.URL

class MainActivity : Activity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var urlBox: EditText
    private lateinit var channelList: LinearLayout
    private val channels = mutableListOf<Pair<String, String>>()
    private val prefs by lazy { getSharedPreferences("tvb", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12); setBackgroundColor(0xFF101820.toInt()) }
        val title = TextView(this).apply { text = "TV B  •  تلویزیون آنلاین"; textSize = 22f; gravity = Gravity.CENTER; setTextColor(-1); setPadding(8) }
        root.addView(title, ViewGroup.LayoutParams(-1, -2))
        playerView = PlayerView(this).apply { player = this@MainActivity.player; useController = true }
        root.addView(playerView, LinearLayout.LayoutParams(-1, 220))
        urlBox = EditText(this).apply { hint = "نشانی پخش یا فایل فهرست M3U/M3U8"; setSingleLine(true); setTextColor(-1); setHintTextColor(0xFFB0BEC5.toInt()) }
        root.addView(urlBox)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val load = Button(this).apply { text = "افزودن فهرست"; setOnClickListener { loadM3u(urlBox.text.toString().trim()) } }
        val play = Button(this).apply { text = "پخش نشانی"; setOnClickListener { playUrl(urlBox.text.toString().trim()) } }
        actions.addView(load, LinearLayout.LayoutParams(0, -2, 1f)); actions.addView(play, LinearLayout.LayoutParams(0, -2, 1f)); root.addView(actions)
        val record = Button(this).apply { text = "● شروع ضبط مستقیم"; setOnClickListener {
            val u = urlBox.text.toString().trim()
            if (!u.startsWith("http")) { toast("ابتدا نشانی مستقیم HLS را وارد کنید"); return@setOnClickListener }
            RecorderService.start(this@MainActivity, u)
            toast("درخواست ضبط ارسال شد")
        } }
        val stop = Button(this).apply { text = "توقف ضبط"; setOnClickListener { RecorderService.stop(this@MainActivity) } }
        actions.addView(record, LinearLayout.LayoutParams(0, -2, 1f)); actions.addView(stop, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(actions)
        val saved = Button(this).apply { text = "فهرست ضبط‌شده‌ها"; setOnClickListener { showRecordings() } }
        root.addView(saved)
        val label = TextView(this).apply { text = "کانال‌ها"; textSize = 18f; setTextColor(-1); setPadding(6) }; root.addView(label)
        val scroll = ScrollView(this); channelList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; scroll.addView(channelList)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        val default = "https://iptv-org.github.io/iptv/index.m3u"
        urlBox.setText(prefs.getString("playlist", default))
    }

    private fun loadM3u(url: String) {
        if (url.isBlank()) return
        prefs.edit().putString("playlist", url).apply()
        Thread {
            try {
                val text = URL(url).openConnection().apply { connectTimeout = 12000; readTimeout = 20000 }.getInputStream().bufferedReader().use { it.readText() }
                val parsed = mutableListOf<Pair<String, String>>()
                var name = "کانال"
                text.lineSequence().forEach { line ->
                    when {
                        line.startsWith("#EXTINF", true) -> name = line.substringAfterLast(",", "کانال").trim()
                        line.isNotBlank() && !line.startsWith("#") && line.startsWith("http") -> parsed.add(name to line.trim())
                    }
                }
                runOnUiThread {
                    channels.clear(); channels.addAll(parsed); channelList.removeAllViews()
                    channels.take(500).forEach { (n, u) ->
                        val b = Button(this).apply { text = n; isAllCaps = false; setOnClickListener { urlBox.setText(u); playUrl(u) } }
                        channelList.addView(b, ViewGroup.LayoutParams(-1, -2))
                    }
                    toast("${channels.size} کانال دریافت شد")
                }
            } catch (e: Exception) { runOnUiThread { toast("خطا در دریافت فهرست: ${e.message}") } }
        }.start()
    }

    private fun playUrl(url: String) {
        if (url.isBlank()) return
        try { player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play() }
        catch (e: Exception) { toast("نشانی پخش معتبر نیست") }
    }

    private fun showRecordings() {
        val dir = getExternalFilesDir("Recordings") ?: filesDir
        val files = dir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }.orEmpty()
        val names = files.map { it.name }.toTypedArray()
        if (names.isEmpty()) { toast("هنوز فایلی ضبط نشده است"); return }
        android.app.AlertDialog.Builder(this).setTitle("ضبط‌ها").setItems(names) { _, which ->
            val f = files[which]
            android.app.AlertDialog.Builder(this).setItems(arrayOf("پخش", "حذف")) { _, action ->
                if (action == 0) playUrl(f.toURI().toString()) else { f.delete(); toast("حذف شد") }
            }.show()
        }.setNegativeButton("بستن", null).show()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    override fun onDestroy() { player.release(); super.onDestroy() }
}
