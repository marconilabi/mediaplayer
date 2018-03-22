package com.restpod.podmonitor.test1

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.util.Util


class RadioPlayerActivity : Activity() {

    private lateinit var player: SimpleExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initPlayer()
        /*setContentView(R.layout.activity_radio_player)

        val playButton = findViewById<View>(R.id.btn_play)
        val pauseButton = findViewById<View>(R.id.btn_stop)


        playButton.setOnClickListener{
            player.playWhenReady = true
        }

        pauseButton.setOnClickListener{
            player.playWhenReady = false
        }*/
    }

    fun initPlayer(){

        val bandwidthMeter = DefaultBandwidthMeter()
        val extractorsFactory = DefaultExtractorsFactory()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "mediaPlayerSample"), defaultBandwidthMeter)
        val mediaSource = ExtractorMediaSource(Uri.parse("http://restpod.com/audio/BrainGym.ogg"), dataSourceFactory, extractorsFactory, null, null)
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        player.prepare(mediaSource)
    }


    override fun onDestroy() {
        super.onDestroy()
        player.playWhenReady = false
    }
}