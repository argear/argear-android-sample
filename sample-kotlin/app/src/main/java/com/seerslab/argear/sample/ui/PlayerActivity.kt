package com.seerslab.argear.sample.ui

import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.seerslab.argear.sample.R
import com.seerslab.argear.sample.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var dataBinding: ActivityPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val uriString = intent.getStringExtra(INTENT_URI)

        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_player)

        //Creating MediaController
        val mediaController = MediaController(this)
        mediaController.setAnchorView(dataBinding.videoView)

        //specify the location of media file
        val uri = Uri.parse(uriString)

        dataBinding.videoView.setOnPreparedListener { mp -> mp.isLooping = true }
        //Setting MediaController and URI, then starting the videoView
        dataBinding.videoView.setMediaController(mediaController)
        dataBinding.videoView.setVideoURI(uri)
        dataBinding.videoView.requestFocus()
        dataBinding.videoView.start()
    }

    override fun onPause() {
        super.onPause()
        if (dataBinding.videoView.isPlaying) {
            dataBinding.videoView.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!dataBinding.videoView.isPlaying) {
            dataBinding.videoView.resume()
            dataBinding.videoView.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        var INTENT_URI = "player_uri"
    }
}