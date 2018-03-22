package com.restpod.podmonitor

import android.util.Log
import android.app.Activity
import android.content.Context
import android.widget.TextView
import android.widget.CompoundButton
import android.widget.Switch
import java.net.URL

import com.fasterxml.jackson.module.kotlin.*
import android.net.Uri
import android.provider.MediaStore
import android.text.style.UpdateAppearance
import com.google.android.things.device.DeviceManager
import com.restpod.podmonitor.test1.*
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import com.google.android.things.update.UpdateManager
import com.google.android.things.update.UpdateManagerStatus
import com.google.android.things.update.StatusListener
import kotlinx.coroutines.experimental.async
import android.R.attr.start
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.media.*
import android.media.SoundPool.Builder
import android.os.*
import android.os.PowerManager.WakeLock
import com.github.kittinunf.fuel.Fuel
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.SampleStream
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.experimental.delay
import java.io.File
import java.sql.Time


class MainActivity : Activity(), CompoundButton.OnCheckedChangeListener {

    private lateinit var arduino: Arduino

    private var txtTemp: TextView? = null
    private var txtStatus1: TextView? = null
    private var txtStatus2: TextView? = null
    internal var txtArduino: TextView? = null
    private var txtApiError: TextView? = null
    private var txtSensorError: TextView? = null
    private var txtLastUpdatedDate: TextView? = null
    private var txtPodGuid: TextView? = null

    private var switchPodOverride: Switch? = null
    private var switchPumpOn: Switch? = null
    private var switchDownloadMusic: Switch? = null

    private var mediaPlayer: MediaPlayer? = null
    private var mediaPlayerCurrentUrl: String? = null
    private var currentTrackVolume: Float = 1.0f

    private var arduinoManager: ArduinoManager? = null
    private var lastReportedTemp: Double = 0.0

    private var audioManager: AudioManager? = null

    private var lastPushedTemp: LocalDateTime = LocalDateTime.MIN

    private val restpodDomain: String = "http://restpod.azurewebsites.net"
    private var podGuid: String? = null
    private var androidId: String? = null
    private var serialNumber: String? = null
    private var targetTemperature: Double = 96.6

    private var businessHours: PodBusinessHours? = null
    private var podSchedule: PodSchedule? = null

    protected var mWakeLock: PowerManager.WakeLock? = null

    private var isMusicMuted: Boolean = false
    private var currentLightIndex: Int = 1
    private var podStatus: PodStatus? = null

    private var currentTrackType: Int = 0

    private var audioBuffering: Boolean = false
    private var audioJustFinished: Boolean = false
    private var audioIsPlaying: Boolean = false
    private var audioCheckForReboot: Boolean = false

    private var mainThreadHandler = Handler(Looper.getMainLooper())
    private var podTaskHandler = Handler()
    private var arduinoHandler = Handler()
    private var musicTrackHandler = Handler()



    private var soundPoolBuilder: Builder? = null
    private val mapper = jacksonObjectMapper()

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        when (buttonView.id) {
            R.id.switchPodOverride -> {
                // Trigger an update check immediately
                if (!isChecked) {
                    var mUpdateManager = UpdateManager.getInstance()

                    mUpdateManager.addStatusListener(StatusListener { status ->
                        when (status.currentState) {
                            UpdateManagerStatus.STATE_CHECKING_FOR_UPDATES -> {
                                txtArduino!!.text = "Checking For Updates"
                            }
                            UpdateManagerStatus.STATE_UPDATE_AVAILABLE -> {
                                txtArduino!!.text = "Update Available"
                            }
                            UpdateManagerStatus.STATE_DOWNLOADING_UPDATE -> {
                                txtArduino!!.text = "Downloading Update"
                            }
                            UpdateManagerStatus.STATE_FINALIZING_UPDATE -> {
                                txtArduino!!.text = "Finalizing Update"
                            }
                            UpdateManagerStatus.STATE_IDLE -> {
                                txtArduino!!.text = "Update Manager Idle"
                            }
                        }
                    })
                    mUpdateManager.performUpdateNow(UpdateManager.POLICY_APPLY_AND_REBOOT)
                }
            }
            R.id.switchDownloadMusic ->
            {
                if (!isChecked)
                {
                    podTaskHandler = Handler()
                    arduinoHandler = Handler()
                    musicTrackHandler = Handler()

                    txtArduino!!.text = "Starting"
                    var files = File("storage/emulated/0/Music/").listFiles()
                    files.forEach {
                        it.delete()
                    }

                     downloadAllFies()

                }
            }
            R.id.switchPumpOn ->
            {
                if (!isChecked) {
                    DeviceManager.getInstance().reboot()
                }
            }
        }
    }

    private fun downloadAllFies()
    {
        downloadFile(35, "/audio/BrainGym.ogg")
        downloadFile(36, "/audio/Sleepmaker.ogg")
        downloadFile(37, "/audio/PainAway.ogg")
        downloadFile(38, "/audio/MyStudy.ogg")
        downloadFile(39, "/audio/KickButts.ogg")
        downloadFile(40, "/audio/InYourRightMind.ogg")
        downloadFile(41, "/audio/HowToCreateAnything.ogg")
        downloadFile(42, "/audio/RoadToRiches.ogg")
        downloadFile(43, "/audio/PublicSpeaking.ogg")
        downloadFile(44, "/audio/FearOfFlying.ogg")
        downloadFile(45, "/audio/FearOfExams.ogg")
        downloadFile(46, "/audio/131643108423601427_IntroShowerTest.ogg")
        downloadFile(47, "/audio/131643108646151174_IntroPodTrack.ogg")
        downloadFile(48, "/audio/131643108812484570_MusicTest.ogg")
        downloadFile(49, "/audio/131643109003938901_WakeupTest.ogg")
        downloadFile(50, "/audio/131643109256672906_OutroShowerTrack.ogg")
        downloadFile(51, "/audio/FTFWelcome.ogg")
        downloadFile(52, "/audio/FTFEnterPod.ogg")
        downloadFile(53, "/audio/FTFWakeUp.ogg")
        downloadFile(54, "/audio/FTFGoodbye.ogg")
        downloadFile(57, "/audio/NFTFWakeUp.ogg")
        downloadFile(58, "/audio/NFTFGoodbye.ogg")
        downloadFile(59, "/audio/NFTFEnterPod.ogg")
        downloadFile(60, "/audio/NFTFWelcome.ogg")
        downloadFile(61, "/audio/HeartChakra.ogg")
        downloadFile(62, "/audio/PeacefulJungle.ogg")
        downloadFile(63, "/audio/MemoryFocus.ogg")
        downloadFile(64, "/audio/SoothingPiano.ogg")
        downloadFile(65, "/audio/gentle_rain.ogg")
        downloadFile(66, "/audio/7Chakra.ogg")
        downloadFile(67, "/audio/ConfidentMe.ogg")
        downloadFile(68, "/audio/MentalDetox.ogg")
        downloadFile(69, "/audio/StressBuster.ogg")
        downloadFile(70, "/audio/TrimAndSlim.ogg")
    }

    fun downloadFile(id: Int, url: String)
    {
        var audioPath = "storage/emulated/0/Music/$id.ogg"
        //var file = File(audioPath)
        //Fuel.download(restpodDomain + url, ).destination { response, url ->
        //    file
        //}.response { req, res, result ->
       // }

        var request = DownloadManager.Request(Uri.parse(restpodDomain + url))
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "$id.ogg")
        var manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        if (id > 44 && id < 61)
            Thread.sleep(5000)
        else
            Thread.sleep(60000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        //androidId = android.provider.Settings.Secure.getString(contentResolver,
        //        android.provider.Settings.Secure.ANDROID_ID)

        serialNumber = android.os.Build.SERIAL

        txtTemp = findViewById<TextView>(R.id.txtTemp)
        txtStatus1 = findViewById<TextView>(R.id.txtStatus1)
        txtStatus2 = findViewById<TextView>(R.id.txtStatus2)
        txtArduino = findViewById<TextView>(R.id.txtArduino)
        txtApiError = findViewById<TextView>(R.id.txtApiError)
        txtSensorError = findViewById<TextView>(R.id.txtSensorError)
        txtLastUpdatedDate = findViewById<TextView>(R.id.txtLastUpdatedDate)
        txtPodGuid = findViewById<TextView>(R.id.txtPodGuid)
        txtPodGuid!!.text = serialNumber

        switchPodOverride = findViewById<Switch>(R.id.switchPodOverride)
        switchPumpOn = findViewById<Switch>(R.id.switchPumpOn)
        switchDownloadMusic = findViewById<Switch>(R.id.switchDownloadMusic)

        switchPodOverride!!.setOnCheckedChangeListener(this)
        switchPumpOn!!.setOnCheckedChangeListener(this)
        switchDownloadMusic!!.setOnCheckedChangeListener(this)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager


        audioManager!!.setStreamVolume(AudioManager.STREAM_MUSIC, 100, 0)
        arduino = Arduino(this)

        podGuid = "fc3d39d4-bf9d-e711-80c2-0003ffb2ff9e"

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag")
        mWakeLock!!.acquire()

        val policy = StrictMode.ThreadPolicy.Builder()
                .permitAll().build()
        StrictMode.setThreadPolicy(policy)

        podTaskHandler.postDelayed({getMissionControlCommandsNew()}, 500)
        arduinoHandler.postDelayed({getArduinoResponse()}, 2000)
        musicTrackHandler.postDelayed({ SetupMusicPlaylist()}, 4000)

        //mGcmNetworkManager = GcmNetworkManager.getInstance(this)

/*

Uncaught remote exception!  (Exceptions are not yet supported across processes.)
              android.view.ViewRootImpl$CalledFromWrongThreadException: Only the original thread that created a view hierarchy can touch its views.

        var mUpdateManager = UpdateManager()
        mUpdateManager.addStatusListener(StatusListener { status ->
            when (status.currentState) {
                UpdateManagerStatus.STATE_CHECKING_FOR_UPDATES -> {
                    txtArduino!!.text = "Checking For Updates"
                }
                UpdateManagerStatus.STATE_UPDATE_AVAILABLE -> {
                    txtArduino!!.text = "Update Available"
                }
                UpdateManagerStatus.STATE_DOWNLOADING_UPDATE -> {
                    txtArduino!!.text = "Downloading Update"
                }
                UpdateManagerStatus.STATE_FINALIZING_UPDATE -> {
                    txtArduino!!.text = "Finalizing Update"
                }
                UpdateManagerStatus.STATE_IDLE -> {
                    txtArduino!!.text = "Update Manager Idle"
                }
            }
        })*/

        //getPodSchedule()
        //getPodBusinessHours
    }

    private fun getPodSchedule() {
        try {
            val jsonResult = URL("$restpodDomain/GetPodSchedule?podGuid=$podGuid")
                    .readText()
            podSchedule = mapper.readValue<PodSchedule>(jsonResult)
        }
        catch (e: Exception){
            //txtArduino!!.text = e.message
        }
        Handler().postDelayed({getPodSchedule()}, 21600000) // 6 hours
    }

    private fun getPodBusinessHours() {
        try {
            val jsonResult = URL("$restpodDomain/GetPodBusinessHours?podGuid=$podGuid")
                    .readText()
            val mapper = jacksonObjectMapper()
            businessHours = mapper.readValue<PodBusinessHours>(jsonResult)
        }
        catch (e: Exception){
            //txtArduino!!.text = e.message
        }
        val handler = Handler()
        handler.postDelayed({getPodBusinessHours()}, 21600000) // 6 hours
    }

    private fun getMissionControlCommandsNew() {
        try {
            getPodStatus()

            if (arduinoManager != null && podStatus != null)
            {
                IssueArduinoCommand(arduinoManager!!.getArduinoCommand(podStatus!!))
                //if (arduinoManager!!.setupSession)
                //{
                    //SetupMusicPlaylist()
                //}
            }
            else if (arduinoManager == null && podStatus != null)
            {
                arduinoManager = ArduinoManager(ArduinoResponse(false, false,
                        false, false, false, false,
                        false, 70.0, "off"), podStatus!!)
                //SetupMusicPlaylist()
            }
        }
        catch (e: Exception){
            //txtArduino!!.text = e.message
        }
        podTaskHandler.postDelayed({getMissionControlCommandsNew()}, 7000)
    }

    private fun SetupMusicPlaylist()
    {
        try {
          /*  if (audioCheckForReboot)
            {
                if (!audioIsPlaying)
                    DeviceManager.getInstance().reboot()
            }*/
            var currentTrack: PodStatusMusicTrack? = null
            if (podStatus == null || podStatus!!.musicList == null)
            {
                musicTrackHandler.postDelayed({ SetupMusicPlaylist() }, 5000)
                return
            }
            val currentDate = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
            for (track in podStatus!!.musicList!!) {
                if (track.startTime < currentDate && track.endTime > currentDate)
                    currentTrack = track
            }
            if (currentTrack == null) // Does this ever get called
            {
                musicTrackHandler.postDelayed({ SetupMusicPlaylist() }, 5000)
                return
            }

            // if correct track is currently playing or buffering, and last song didn't JUST finish, wait a second
            if (currentTrack.trackType == currentTrackType && mediaPlayer != null &&
                    (audioIsPlaying || audioBuffering || audioJustFinished))
            {
                musicTrackHandler.postDelayed({ SetupMusicPlaylist() }, 1000)
                return
            }

            PlayTrack(currentTrack!!)

            musicTrackHandler.postDelayed({ SetupMusicPlaylist() }, 120000)
            return
        }
        catch (e: Exception){
            //txtArduino!!.text = e.message
        }

        //val delayMillis = currentTrack!!.endTime.time - currentDate.time - Causing weird stuff
        musicTrackHandler.postDelayed({ SetupMusicPlaylist() }, 3000)
    }

    private fun PlayTrack(musicTrack: PodStatusMusicTrack) {
        audioBuffering = false
        audioJustFinished = false
        audioIsPlaying = false

        if (musicTrack.trackType == 1 || musicTrack.trackType == 5) {
            currentLightIndex = -1
            arduino.write("Q")
        }
        if (musicTrack.trackType == 2) {
            currentLightIndex = 5
            arduino.write("O")
        }


        if (musicTrack.id == 6)
        {//TRACK IS "NO MUSIC"
            audioIsPlaying = true
            currentTrackType = musicTrack.trackType
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        else {



            var audioPath = "storage/emulated/0/Music/${musicTrack.id}.ogg"
            val soundTrack = File(audioPath)




            if (soundTrack.exists())
                RunMediaPlayer(musicTrack.trackType, audioPath)
            else { //IF THE TRACK IS NOT "NO MUSIC"
                /*Fuel.download(restpodDomain + musicTrack.url).destination { response, url ->
                    Log.e("Fuel", "stuatus: " + response.statusCode.toString())
                    soundTrack
                }.response { req, res, result ->
                    RunMediaPlayer(musicTrack.trackType, restpodDomain + musicTrack.url) //audioPath)
                }*/
                var request = DownloadManager.Request(Uri.parse(restpodDomain + musicTrack.url))
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, "${musicTrack.id}.ogg")
                var manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                Thread.sleep(60000)
                RunMediaPlayer(musicTrack.trackType, audioPath)
            }
        }
    }

    private fun buildDataSourceFactory(useBandwidthMeter: Boolean): DataSource.Factory {
        return (application as MainActivity)
                .buildDataSourceFactory(false)
    }

    private fun RunExoPlayer(musicTrackType: Int, audioPath: String)
    {
        /*
        // 1. Create a default TrackSelector
        var mainHandler = Handler()
        var bandwidthMeter = DefaultBandwidthMeter()
        var trackFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        var trackSelector = DefaultTrackSelector(trackFactory)
        var player = ExoPlayerFactory.newSimpleInstance(this.applicationContext, trackSelector)
        var mediaSorce = ExtractorMediaSource.Factory(buildDataSourceFactory(true))
        mediaSorce.createMediaSource("mnt/")
        DataSource.Factory { audioPath }
        var dsd = DataSource.Factory(() -> buildDataSourceFactory)
        var dsource = DataSource.Factory().createDataSource().
 var dsds = ExtractorMediaSource.Factory(DataSource.Factory { "f" }
        var d = ConcatenatingMediaSource(

        )

        ExtractorMediaSource.Factory()
var dataSourceFactory = DefaultDataSourceFactory(this, Util.getUserAgent(this, "mediaPlayerSample"), defaultBandwidthMeter);
MediaSource
var mediaSource = ExtractorMediaSource(Uri.parse("http://rs1.radiostreamer.com:8030"), dataSourceFactory, extractorsFactory, null, null);

player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

player.prepare(mediaSource);

player.setPlayWhenReady(true);

        MediaSource[] mediaSources = new MediaSource[videos.size()];
for (int i = 0; i < videos.size(); i++) {
    mediaSources[i] = buildMediaSource(Uri.parse(videos.get(i).getVideoPath()));
}
MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
        : new ConcatenatingMediaSource(mediaSources);
player.seekTo(position,0);

      player.setPlayWhenReady(true);
        ExtractorMediaSource.Factory(DataSource.Factory {  })
        var fact = DataSource.Factory()
        fact.createDataSource()
        var mediaDataSourceFactory = buildDataSourceFactory(true)
        var mediaSource =
        player.prepare()

 var dataSourceFactory = DefaultDataSourceFactory(this.applicationContext,
         Util.getUserAgent(this.applicationContext, "yourApplicationName"))
// This is the MediaSource representing the media to be played.
var audioSource = ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(mp4VideoUri)
var me = SampleStream()
        player.prepare()
Handler mainHandler = new Handler();
BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
TrackSelection.Factory videoTrackSelectionFactory =
    new AdaptiveTrackSelection.Factory(bandwidthMeter);
TrackSelector trackSelector =
    new DefaultTrackSelector(videoTrackSelectionFactory);

// 2. Create the player
SimpleExoPlayer player =
    ExoPlayerFactory.newSimpleInstance(context, trackSelector);

        if (mediaPlayer != null)
        {
            mediaPlayer!!.reset()
        }
        else
        {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setOnErrorListener { mp: MediaPlayer, what: Int, extra: Int ->
                mp.reset()
                audioIsPlaying = false
                Log.e("Media Player", "Error, What: " + what + " extra: " + extra)
                true
            }
            mediaPlayer!!.setOnInfoListener{ mp: MediaPlayer, what: Int, extra: Int ->
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
                    audioBuffering = true
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
                {
                    audioBuffering = false
                    mp.start()
                }
                Log.e("Media Player", "Buffer. What: " + what + " extra: " + extra)
                true
            }
        }
        mediaPlayer!!.setOnPreparedListener( {
            it.start()
            //val currentDate = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
            //it.seekTo((currentDate.time - musicTrack.startTime.time).toInt())
            audioIsPlaying = true
            currentTrackType = musicTrackType
            //txtArduino!!.text = "Playing Track " + currentTrackType + ": " + musicTrack.url
            it.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                audioJustFinished = true
                if (currentTrackType == 5) // turn off after youre done
                {
                    mediaPlayer = null
                    DeviceManager.getInstance().reboot()
                }
            })
        })
        if (musicTrackType == 1 || musicTrackType == 2 || musicTrackType == 5)
            mediaPlayer!!.setVolume(currentTrackVolume, currentTrackVolume)
        else if (musicTrackType == 3 || musicTrackType == 4)
            mediaPlayer!!.setVolume(currentTrackVolume, 0.0f)

        mediaPlayer!!.setDataSource(audioPath)
        mediaPlayer!!.prepare()

*/
        val bandwidthMeter = DefaultBandwidthMeter()
        val extractorsFactory = DefaultExtractorsFactory()
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(trackSelectionFactory)
        val defaultBandwidthMeter = DefaultBandwidthMeter()
        val dataSourceFactory = DefaultDataSourceFactory(this,
                Util.getUserAgent(this, "mediaPlayerSample"), defaultBandwidthMeter)
        val mediaSource = ExtractorMediaSource(Uri.parse("http://restpod.com/audio/BrainGym.ogg"), dataSourceFactory, extractorsFactory, null, null)
        var player = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        player.prepare(mediaSource)
        player.playWhenReady= true
    }

    private fun RunMediaPlayer(musicTrackType: Int, audioPath: String)
    {
        if (mediaPlayer != null)
        {
            mediaPlayer!!.reset()
        }
        else
        {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setOnErrorListener { mp: MediaPlayer, what: Int, extra: Int ->
                mp.reset()
                audioIsPlaying = false
                Log.e("Media Player", "Error, What: " + what + " extra: " + extra)
                true
            }
            mediaPlayer!!.setOnInfoListener{ mp: MediaPlayer, what: Int, extra: Int ->
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START)
                    audioBuffering = true
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END)
                {
                    audioBuffering = false
                    mp.start()
                }
                Log.e("Media Player", "Buffer. What: " + what + " extra: " + extra)
                true
            }
        }
        mediaPlayer!!.setOnPreparedListener( {
            it.start()
            //val currentDate = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))
            //it.seekTo((currentDate.time - musicTrack.startTime.time).toInt())
            audioIsPlaying = true
            currentTrackType = musicTrackType
            //txtArduino!!.text = "Playing Track " + currentTrackType + ": " + musicTrack.url
            it.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                audioJustFinished = true
                if (currentTrackType == 5) // turn off after youre done
                {
                    mediaPlayer = null
                    DeviceManager.getInstance().reboot()
                }
            })
        })
        if (musicTrackType == 1 || musicTrackType == 2 || musicTrackType == 5)
            mediaPlayer!!.setVolume(currentTrackVolume, currentTrackVolume)
        else if (musicTrackType == 3 || musicTrackType == 4)
            mediaPlayer!!.setVolume(currentTrackVolume, 0.0f)

        mediaPlayer!!.setDataSource(audioPath)
        mediaPlayer!!.prepare()
    }

    private fun getArduinoResponse() {
        arduino.write("Z")
        arduinoHandler.postDelayed({getArduinoResponse()}, 30000)
    }

    private fun getPodStatus()  {
        try {
            val jsonResult = URL("$restpodDomain/GetPodStatusByAndroidId?androidId=" + serialNumber).readText()
            podStatus = mapper.readValue<PodStatus>(jsonResult)
        } catch (e: Exception) {
            //txtArduino!!.text = e.message
        }

    }

    private fun IssueArduinoCommand(command: ArduinoCommand)
    {
        if (command == ArduinoCommand.TurnPumpOn) {
            //txtArduino!!.text = "Pump On"
            arduino.write("A")
        }
        else if (command == ArduinoCommand.TurnHeaterOn) {
            //txtArduino!!.text = "Heater On"
            arduino.write("B")
        }
        else if (command == ArduinoCommand.TurnOff) {
            //txtArduino!!.text = "Pump Off"
            arduino.write("C")
        }
        else if (command == ArduinoCommand.TurnDoser1On)
            arduino.write("I")
        else if (command == ArduinoCommand.TurnDoser1Off)
            arduino.write("J")
        else if (command == ArduinoCommand.TurnDoser2On)
            arduino.write("K")
        else if (command == ArduinoCommand.TurnDoser2Off)
            arduino.write("L")
        else if (command == ArduinoCommand.TurnLightRed)
            arduino.write("M")
        else if (command == ArduinoCommand.TurnLightGreen)
            arduino.write("N")
        else if (command == ArduinoCommand.TurnLightBlue)
            arduino.write("O")
        else if (command == ArduinoCommand.TurnLightPurple)
            arduino.write("P")
        else if (command == ArduinoCommand.TurnLightAqua)
            arduino.write("Q")
        else if (command == ArduinoCommand.TurnLightOff)
            arduino.write("R")
    }

    fun handleButtonPress(buttonNumber: Int)
    {
        if (buttonNumber == 1)
        {
            //txtArduino!!.text = "Button 1 Pressed"
            currentLightIndex++
            if (currentLightIndex % 2 == 0)
                IssueArduinoCommand(ArduinoCommand.TurnLightOff)
            else if (currentLightIndex == 1)
                IssueArduinoCommand(ArduinoCommand.TurnLightRed)
            else if (currentLightIndex == 3)
                IssueArduinoCommand(ArduinoCommand.TurnLightGreen)
            else if (currentLightIndex == 5)
                IssueArduinoCommand(ArduinoCommand.TurnLightBlue)
            else if (currentLightIndex == 7)
                IssueArduinoCommand(ArduinoCommand.TurnLightPurple)
            else if (currentLightIndex == 9) {
                IssueArduinoCommand(ArduinoCommand.TurnLightAqua)
                currentLightIndex = -1
            }
        }
        else if (buttonNumber == 2)
        {
            //txtArduino!!.text = "Button 2 Pressed"
            if (currentTrackType == 3) {
                isMusicMuted = !isMusicMuted
                if (isMusicMuted) {
                    mediaPlayer!!.setVolume(0.0f, 0.0f)
                } else {
                    mediaPlayer!!.setVolume(currentTrackVolume, 0.0f)
                }
            }
        }
    }

    fun handleArduinoResponse(arduinoResponse: ArduinoResponse){
        try {
            if (arduinoManager == null && podStatus != null) {
                arduinoManager = ArduinoManager(arduinoResponse, podStatus!!)
            }
            IssueArduinoCommand(arduinoManager!!.getArduinoCommand(arduinoResponse))

            if (lastReportedTemp == 0.0 || Math.abs((lastReportedTemp - arduinoResponse.temperature)) > .3)
            {
                postPodState(arduinoResponse)
                lastReportedTemp = arduinoResponse.temperature
            }
            else if (arduinoManager!!.pumpChanged || arduinoManager!!.heaterChanged)
            {
                postPodState(arduinoResponse)
            }
            //Temp!!.text = arduinoResponse.temperature.toString() + " °F"
        }
        catch (e: Exception){
            //txtArduino!!.text = e.message

        }
    }

    /*private fun getMissionControlCommands(){
        try {
            val jsonResult = URL("http://restpod.azurewebsites.net/GetPodStatus?podGuid=fc3d39d4-bf9d-e711-80c2-0003ffb2ff9e").readText()
            val mapper = jacksonObjectMapper()

            val state = mapper.readValue<PodStatus>(jsonResult)
            val podState = getPodStatus()
            val currentDate = Date.from(LocalDateTime.now().toInstant(ZoneOffset.UTC))

            var pumpRelayOn: Boolean? = null
            var heaterRelayOn: Boolean? = null
            var currentTemperature: Double? = null

            if (state.musicUrl != null)
            {
                if (!mediaPlayer.isPlaying || (mediaPlayer.isPlaying && mediaPlayerCurrentUrl != state.musicUrl)) {
                    try {
                        mediaPlayerCurrentUrl = state.musicUrl
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource("http://restpod.azurewebsites.net" + state.musicUrl)
                        mediaPlayer.prepareAsync()
                        mediaPlayer.setOnPreparedListener({
                            mediaPlayer.start()
                        })
                    } catch (e: Exception) {
                        var d = e.message
                    }
                }
            }

            arduino.write("Z")
            Thread.sleep(400)

            val arduinoResponse = arduino.read()

            // START NEW CODE

            val parsedArduinoResponse = mapper.readValue<ArduinoResponse>(arduinoResponse)

            if (arduinoManager == null)
            {
                arduinoManager = ArduinoManager(parsedArduinoResponse, podState)
            }

            IssueArduinoCommand(arduinoManager!!.getArduinoCommand(parsedArduinoResponse))

            if (arduinoManager!!.temperatureChanged)
            {
                if (lastReportedTemp == 0.0 || Math.abs((lastReportedTemp - parsedArduinoResponse.temperature)) != .1)
                {
                    postPodState(parsedArduinoResponse)
                    lastReportedTemp = parsedArduinoResponse.temperature
                }
                txtTemp!!.text = parsedArduinoResponse.temperature.toString() + " °F"
            }
            else if (arduinoManager!!.pumpChanged || arduinoManager!!.heaterChanged)
            {
                postPodState(parsedArduinoResponse)
            }


            // END NEW CODE

            if (arduinoResponse.contains("PumpRelayOn = "))
            {
                var index = arduinoResponse.indexOf("PumpRelayOn = ") + 14
                var answer = arduinoResponse.substring(index, index + 1)

                pumpRelayOn = (answer == "t")
            }
            if (arduinoResponse.contains("HeaterRelayOn = "))
            {
                var index = arduinoResponse.indexOf("HeaterRelayOn = ") + 16
                var answer = arduinoResponse.substring(index, index + 1)

                heaterRelayOn = (answer == "t")
            }
            if (arduinoResponse.contains("Temperature is: "))
            {
                var index = arduinoResponse.indexOf("Temperature is: ") + 16
                var answer = arduinoResponse.substring(index, index + 4)

                currentTemperature = answer.toDoubleOrNull()
            }

            if (currentTemperature != null && lastPushedTemp < LocalDateTime.from(Date().toInstant().atZone(ZoneId.of("UTC")))) {
                lastPushedTemp = LocalDateTime.from(Date().toInstant().atZone(ZoneId.of("UTC"))).plusMinutes(2)
                var tempUrl = "http://restpod.azurewebsites.net/SavePodTemp?podGuid=fc3d39d4-bf9d-e711-80c2-0003ffb2ff9e&temp="
                tempUrl += currentTemperature.toString()
                URL(tempUrl).readText()
            }

            if (state.sessionEnd != null && currentDate < state.sessionEnd &&
                    pumpRelayOn != null && pumpRelayOn)
            // If in session and pump is on, turn off
            {
                arduino.write("B")
            }
            else if (state.targetTemperature != null && currentTemperature != null && currentTemperature < state.targetTemperature - 1 &&
                    heaterRelayOn != null && !heaterRelayOn && state.sessionEnd == null && pumpRelayOn != null && !pumpRelayOn) //replace 95 with read temp
            // if currentTemp < targetTemp -1 and pump is not on, and heater is not on, turn pump and heater on
            {
                arduino.write("C")
            }
            else if (state.targetTemperature != null && currentTemperature != null && currentTemperature < state.targetTemperature &&
                    heaterRelayOn != null && !heaterRelayOn && state.sessionEnd == null && pumpRelayOn != null && pumpRelayOn) //replace 95 with read temp
            // if currentTemp < targetTemp and pump is on, and heater is not on, turn pump and heater on
            {
                arduino.write("C")
            }
            else if (state.cleaningEnd != null && currentDate < state.cleaningEnd && state.sessionEnd == null &&
                    pumpRelayOn != null && !pumpRelayOn)
            // if cleaning, and pump is not on, turn pump on
            {
                arduino.write("A")
            }
            else if (pumpRelayOn != null && pumpRelayOn && currentTemperature != null && state.targetTemperature != null &&
                    currentTemperature >= state.targetTemperature && state.cleaningEnd != null)
            // if not cleaning, and pump is on and currentTemp >= targetTemp, turn pump on
            {
                arduino.write("A")
            }
            else if (pumpRelayOn != null && pumpRelayOn && currentTemperature != null && state.targetTemperature != null &&
                    currentTemperature >= state.targetTemperature && state.cleaningEnd == null)
            // if cleaning, and pump is on and currentTemp >= targetTemp, turn pump off
            {
                arduino.write("B")
            }
            Thread.sleep(100)
            txtTemp!!.text = currentTemperature.toString() + " °F"
            txtArduino!!.text = "Music: " + state.musicUrl
        }
        catch (e: Exception){
            txtArduino!!.text = e.message
        }
        val handler = Handler()
        handler.postDelayed({getMissionControlCommands()}, 3000)
    }*/

    private fun postPodState(arduinoResponse: ArduinoResponse){
        try {
            if (arduinoResponse.temperature > 0.0) {
                URL("$restpodDomain/SavePodTempByAndroidId?androidId=$serialNumber" +
                        "&temp=${arduinoResponse.temperature}" +
                        "&pumpOn=${arduinoResponse.isPumpRelayOn}" +
                        "&heaterOn=${arduinoResponse.isHeaterRelayOn}")
                        .readText()
            }
        }
        catch (e: Exception){
            //txtArduino!!.text = e.message
        }
    }

    override fun onDestroy() {

        this.mWakeLock!!.release()
        if (mediaPlayer != null) {

            if (mediaPlayer!!.isPlaying) {
                mediaPlayer!!.pause()
                mediaPlayer!!.stop()
            }
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        arduino.close()
        super.onDestroy()
    }



    /*
    currentTrackVolume = (musicTrack.volume / 100).toFloat()
    isMusicMuted = false

    soundPoolBuilder = Builder()

    var audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    soundPoolBuilder!!.setAudioAttributes(audioAttributes)

    var sound = soundpool.load(meow3, 1)
    soundpool.setOnLoadCompleteListener { sp: SoundPool, what: Int, wha: Int ->
        sp.play(sound, 1.0f, 1.0f, 1, 0, 1.0f)
    }*/
}

