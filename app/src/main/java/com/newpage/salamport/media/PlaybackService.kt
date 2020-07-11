package com.newpage.salamport.media

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.OnAudioFocusChangeListener
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.*
import com.google.android.exoplayer2.util.Util
import com.newpage.salamport.MainActivity
import okhttp3.OkHttpClient
import java.io.File
import com.newpage.salamport.R


class PlaybackService : Service() {
    private val NOTIFICATION_ID = 404
    private val NOTIFICATION_DEFAULT_CHANNEL_ID = "com.newpage.salamport"
    private var player: SimpleExoPlayer? = null
    private var mediaSessionCompat: MediaSessionCompat? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioFocusRequested = false
    private var extractorsFactory: ExtractorsFactory? = null
    private var dataSourceFactory: DataSource.Factory? = null
    private var cache: Cache? = null


    private lateinit var session: String
    private lateinit var token: String

    private var position = 0


    var status = 0
    private var currentUri = Uri.parse("http://google.ru")
    private val musicRepository: MusicRepository = MusicRepository()
    private val metadataBuilder = MediaMetadataCompat.Builder()
    private val stateBuilder = PlaybackStateCompat.Builder()
        .setActions(
            PlaybackStateCompat.ACTION_PLAY
                    or PlaybackStateCompat.ACTION_STOP
                    or PlaybackStateCompat.ACTION_PAUSE
                    or PlaybackStateCompat.ACTION_PLAY_PAUSE
                    or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        )
    private val exoPlayerListener: Player.EventListener = object : Player.EventListener {
        override fun onTracksChanged(
            trackGroups: TrackGroupArray,
            trackSelections: TrackSelectionArray
        ) {
        }

        override fun onLoadingChanged(isLoading: Boolean) {}
        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: Int
        ) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                mediaSessionCallback.onSkipToNext()
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {}
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
    }
    private val audioFocusChangeListener =
        OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_GAIN -> mediaSessionCallback.onPlay()
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaSessionCallback.onPause()
                else -> mediaSessionCallback.onPause()
            }
        }
    private val mediaSessionCallback: MediaSessionCompat.Callback =
        object : MediaSessionCompat.Callback() {
            override fun onPause() {
                player!!.playWhenReady = false
                mediaSessionCompat!!.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_PAUSED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f
                    ).build()
                )
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PAUSED)
                status = PlaybackStateCompat.STATE_PAUSED
            }

            override fun onSkipToNext() {
                onPause()
                musicRepository.next()
                onPlay()
            }

            override fun onSkipToPrevious() {
                onPause()
                musicRepository.prev()
                onPlay()
            }

            override fun onStop() {
                player!!.playWhenReady = false
                mediaSessionCompat!!.isActive = false
                mediaSessionCompat!!.setPlaybackState(
                    stateBuilder.setState(
                        PlaybackStateCompat.STATE_STOPPED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f
                    ).build()
                )
                status = PlaybackStateCompat.STATE_STOPPED
                refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_STOPPED)
                audioManager!!.abandonAudioFocus(audioFocusChangeListener)
                stopSelf()
            }

            override fun onPlay() {
                if (!player!!.playWhenReady) {
                    startService(Intent(applicationContext, PlaybackService::class.java))
                    val track: Track = musicRepository.current
                    updateMetadataFromTrack(track)
                    status = PlaybackStateCompat.STATE_PLAYING
                    //MetadataTask(
                    //    session, metadataBuilder, resources,
                    //    musicRepository, this@PlaybackService
                    //).execute()
                    prepareToPlay(track.url)
                    if (!audioFocusRequested) {
                        audioFocusRequested = true
                        val audioFocusResult: Int =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                audioManager!!.requestAudioFocus(audioFocusRequest!!)
                            } else {
                                audioManager!!.requestAudioFocus(
                                    audioFocusChangeListener,
                                    AudioManager.STREAM_MUSIC,
                                    AudioManager.AUDIOFOCUS_GAIN
                                )
                            }
                        if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
                    }
                    mediaSessionCompat!!.isActive = true // Сразу после получения фокуса
                    refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING)
                    player!!.playWhenReady = true
                }
            }
        }

    private fun updateMetadataFromTrack(track: Track) {
        metadataBuilder.putBitmap(
            MediaMetadataCompat.METADATA_KEY_ART,
            BitmapFactory.decodeResource(resources, track.bitmapResId)
        )
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
        metadataBuilder.putString(
            MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
            track.title
        )
        mediaSessionCompat!!.setMetadata(metadataBuilder.build())
    }

    private fun prepareToPlay(uri: String?) {
        if (uri != currentUri.path) {
            currentUri = Uri.parse(uri)
            val mediaSource =
                ExtractorMediaSource(currentUri, dataSourceFactory, extractorsFactory, null, null)
            player!!.prepare(mediaSource)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent)

        //MetadataTask(session, metadataBuilder, resources, musicRepository, this).execute()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") val notificationChannel =
                NotificationChannel(
                    NOTIFICATION_DEFAULT_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManagerCompat.IMPORTANCE_DEFAULT
                )
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
            val audioAttributes =
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(true)
                .setAudioAttributes(audioAttributes)
                .build()
        }
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSessionCompat = MediaSessionCompat(this, "PlayerService")
        mediaSessionCompat!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSessionCompat!!.setCallback(mediaSessionCallback)
        val appContext = applicationContext
        val activityIntent = Intent(appContext, MainActivity::class.java)
        mediaSessionCompat!!.setSessionActivity(
            PendingIntent.getActivity(
                appContext,
                0,
                activityIntent,
                0
            )
        )
        val mediaButtonIntent = Intent(
            Intent.ACTION_MEDIA_BUTTON, null, appContext,
            MediaButtonReceiver::class.java
        )
        mediaSessionCompat!!.setMediaButtonReceiver(
            PendingIntent.getBroadcast(
                appContext,
                0,
                mediaButtonIntent,
                0
            )
        )
        player = ExoPlayerFactory.newSimpleInstance(
            this,
            DefaultRenderersFactory(this),
            DefaultTrackSelector(),
            DefaultLoadControl()
        )
        player!!.addListener(exoPlayerListener)
        val httpDataSourceFactory: DataSource.Factory =
            OkHttpDataSourceFactory(
                OkHttpClient(),
                Util.getUserAgent(
                    this,
                    getString(R.string.app_name)
                )
            )
        cache = SimpleCache(
            File(this.cacheDir.absolutePath + "/exoplayer"),
            LeastRecentlyUsedCacheEvictor(1024 * 1024 * 100)
        ) // 100 Mb max
        dataSourceFactory = CacheDataSourceFactory(
            cache,
            httpDataSourceFactory,
            CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
        )
        extractorsFactory = DefaultExtractorsFactory()
        val handler = Handler()
        /*val task: Runnable = object : Runnable {
            override fun run() {
                MetadataTask(
                    session, metadataBuilder, resources, musicRepository,
                    this@PlaybackService
                ).execute()
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(task, 3000)
         */
    }

    fun refreshNotificationAndForegroundStatus(playbackState: Int) {
        updateMetadataFromTrack(musicRepository.current)
        when (playbackState) {
            PlaybackStateCompat.STATE_PLAYING -> {
                startForeground(NOTIFICATION_ID, getNotification(playbackState))
            }
            PlaybackStateCompat.STATE_PAUSED -> {
                // На паузе мы перестаем быть foreground, однако оставляем уведомление,
                // чтобы пользователь мог play нажать
                if (getNotification(playbackState) != null) {
                    NotificationManagerCompat.from(this@PlaybackService)
                        .notify(NOTIFICATION_ID, getNotification(playbackState)!!)
                    stopForeground(false)
                }
            }
            else -> {

                // Все, можно прятать уведомление
                stopForeground(true)
            }
        }
    }

    private fun getNotification(playbackState: Int): Notification? {
        val builder: NotificationCompat.Builder =
            MediaStyleHelper.from(this, mediaSessionCompat!!)
                ?: return null
        //builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous, getString(R.string.previous), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        if (playbackState == PlaybackStateCompat.STATE_PLAYING) builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                getString(R.string.pause),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PAUSE
                )
            )
        ) else builder.addAction(
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                getString(R.string.play),
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_PLAY
                )
            )
        )

        //builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next, getString(R.string.next), MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setStyle(
            androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0)
                .setShowCancelButton(true)
                .setCancelButtonIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setMediaSession(mediaSessionCompat!!.sessionToken)
        ) // setMediaSession требуется для Android Wear
        builder.setSmallIcon(R.drawable.exo_notification_small_icon)
        builder.setLargeIcon(randomBanner)
        builder.color = ContextCompat.getColor(
            this,
            R.color.orangevo
        ) // The whole background (in MediaStyle), not just icon background
        builder.setShowWhen(false)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setOnlyAlertOnce(true)
        builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID)
        return builder.build()
    }

    private val randomBanner: Bitmap
        private get() = BitmapFactory.decodeResource(
            resources,
            R.drawable.exo_notification_small_icon
        )

    override fun onDestroy() {
        super.onDestroy()
        player!!.release()
        cache!!.release()
        mediaSessionCompat!!.release()
    }

    override fun onBind(intent: Intent): IBinder? {
        session = intent.getStringExtra("session")
        token = intent.getStringExtra("token")
        position = intent.getIntExtra("position", 0)
        musicRepository.init(session = session, token = token, position = position)
        return PlaybackServiceBinder()
    }

    inner class PlaybackServiceBinder : Binder() {
        val token: MediaSessionCompat.Token
            get() = mediaSessionCompat!!.sessionToken

        val repository: MusicRepository
            get() = musicRepository
    }
}
