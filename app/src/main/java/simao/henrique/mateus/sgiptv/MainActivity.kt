package simao.henrique.mateus.sgiptv
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.gson.GsonBuilder
import com.squareup.picasso.Picasso
import okhttp3.*
import java.io.IOException
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {

    private lateinit var constraintLayoutRoot: ConstraintLayout
    private lateinit var exoPlayerVIew: PlayerView

    private lateinit var simpleExoPlayer: SimpleExoPlayer
    private lateinit var mediaSource: MediaSource

    private lateinit var urlType: URLType

    private var listChannels = requestChannels()
    private var position = 0

    private var y1 = 0f
    private var y2 = 0f

    private lateinit var imageView: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (listChannels.isEmpty()) {
            Toast.makeText(this, "Nenhum canal encontrado", Toast.LENGTH_SHORT).show()
            Thread.sleep(5000)
            exitProcess(0)

        } else {
            val toast = Toast.makeText(
                this,
                listChannels.size.toString() + " Canais encontrados",
                Toast.LENGTH_SHORT
            )
            toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0)
            toast.show()
            findView()
            initPlayer()
        }


    }



    private fun findView() {
        constraintLayoutRoot = findViewById(R.id.constraintLayoutRoot)
        exoPlayerVIew = findViewById(R.id.exoPlayerView)
    }

    private fun initPlayer() {
        simpleExoPlayer = SimpleExoPlayer.Builder(this).build()
        simpleExoPlayer.addListener(playerListener)
        exoPlayerVIew.player = simpleExoPlayer

        createMediaSource()

        simpleExoPlayer.setMediaSource(mediaSource)
        simpleExoPlayer.prepare()
    }

    private fun createMediaSource() {

        urlType = URLType.HLS
        urlType.url = listChannels[position].video
        Toast.makeText(this, listChannels[position].name, Toast.LENGTH_SHORT).show()
        exoPlayerVIew.useController = false
        iconImage()
        simpleExoPlayer.seekTo(0)

        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            this,
            Util.getUserAgent(this, applicationInfo.name)
        )
        mediaSource = HlsMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.fromUri(Uri.parse(urlType.url))
        )

    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val constraintSet = ConstraintSet()
        constraintSet.connect(
            exoPlayerVIew.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            0
        )
        constraintSet.connect(
            exoPlayerVIew.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            0
        )
        constraintSet.connect(
            exoPlayerVIew.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            0
        )
        constraintSet.connect(
            exoPlayerVIew.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            0
        )

        constraintSet.applyTo(constraintLayoutRoot)

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            hideSystemUI()
        } else {
            showSystemUI()

            val layoutParams = exoPlayerVIew.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.dimensionRatio = "16:9"
        }
        window.decorView.requestLayout()
    }

    private fun hideSystemUI() {
        actionBar?.hide()

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun showSystemUI() {
        actionBar?.show()

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
    }

    override fun onResume() {
        super.onResume()

        simpleExoPlayer.playWhenReady = true
        simpleExoPlayer.play()
    }


    override fun onDestroy() {
        super.onDestroy()

        simpleExoPlayer.removeListener(playerListener)
        simpleExoPlayer.stop()
        simpleExoPlayer.clearMediaItems()


        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private var playerListener = object : Player.Listener {

        override fun onRenderedFirstFrame() {
            super.onRenderedFirstFrame()
            exoPlayerVIew.useController = false
        }

        override fun onPlayerError(error: PlaybackException) {
            super.onPlayerError(error)
            Toast.makeText(
                this@MainActivity,
                "Erro em reproduzir o canal " + listChannels[position].name,
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    fun requestChannels(): ArrayList<Channel> {
        val client = OkHttpClient()
        val url = "http://192.168.0.10:30001/api/v1/channels"
        val result = ArrayList<Channel>()
        val request = Request.Builder()
            .url(url)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("TAG", "onFailure: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                val gson = GsonBuilder().create()
                val channels = gson.fromJson(body, Array<Channel>::class.java)
                result.addAll(channels)
                Log.d("TAG", "onResponse: ${result}")
            }
        })
        return result
    }

    fun playChannel() {
        if (position < 0) {
            this.position = listChannels.size - 1
        } else if (position > listChannels.size - 1) {
            this.position = 0
        } else {
            this.position = position
        }
        createMediaSource()
        simpleExoPlayer.setMediaSource(mediaSource)
        simpleExoPlayer.prepare()

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                y1 = event.y
            }
            MotionEvent.ACTION_UP -> {
                y2 = event.y
                val valueY = y2 - y1
                if (Math.abs(valueY) > 50) {
                    if (y2 > y1) {
                        position--
                        playChannel()
                    }else{
                        position++
                        playChannel()
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    position--
                    playChannel()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < listChannels.size - 1) {
                    position++
                    playChannel()
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    position--
                    playChannel()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < listChannels.size - 1) {
                    position++
                    playChannel()
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    position--
                    playChannel()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < listChannels.size - 1) {
                    position++
                    playChannel()
                }
            }
        }
        return super.onKeyLongPress(keyCode, event)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (position > 0) {
                    position--
                    playChannel()
                }
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (position < listChannels.size - 1) {
                    position++
                    playChannel()
                }
            }
        }
        return super.onKeyMultiple(keyCode, repeatCount, event)
    }

    fun iconImage(){
        imageView = findViewById(R.id.imageView)

        Picasso.get().load(listChannels[position].image)
            .into(imageView)
    }
}


enum class URLType(var url: String) {
    HLS("")
}

