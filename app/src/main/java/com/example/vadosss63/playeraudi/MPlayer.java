package com.example.vadosss63.playeraudi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MPlayer extends Service implements OnCompletionListener, MediaPlayer.OnErrorListener {


    static final public int CMD_SELECT_TRACK = 0x05;
    static final public int CMD_PLAY = 0x06;
    static final public int CMD_PAUSE = 0x07;
    static final public int CMD_PREVIOUS = 0x08;
    static final public int CMD_NEXT = 0x09;
    static final public int CMD_CHANGE_ROOT = 0x0A;
    static final public int CMD_SEND_TIME = 0x01;

    // Плеер для воспроизведения
    private MediaPlayer m_mediaPlayer;

    NoisyAudioStreamReceiver m_noisyAudioStreamReceiver;

    // аудио фокус
    AudioManager m_audioManager;
    AFListener m_afLiListener = new AFListener();

//    RemoteControlReceiver m_remoteControlReceiver = new RemoteControlReceiver();

    private Handler m_handler;
    private Runnable m_runnable;

    // Текущий выбранный трек
    private NodeDirectory m_currentTrack;// Установить на первую песню

    // дериктория для воспроизведения
    private MusicFiles m_musicFiles;

    // корневая папка для воспроизведения музыки
    private String m_rootPath = "/Music";
    private ComponentName mReceiverComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        m_audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        m_noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();

//        m_audioManager.registerMediaButtonEventReceiver();

        CreatePlayer();
        CreateMusicFiles();
        CreateTime();
        SelectTrack(1, 0);

        RegisterRemountControl();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ParserCMD(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (m_mediaPlayer != null)
            m_mediaPlayer.release();

        AbandonAudioFocus();
        StopPlayback();
//        UnregisterRemountControl();
    }

    private void AbandonAudioFocus() {
        if (m_afLiListener != null)
            m_audioManager.abandonAudioFocus(m_afLiListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void ShowNotificationPlay() {
        if (m_currentTrack == null) return;

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_controller_play);
//
//        File f = new File(m_currentTrack.GetPathDir());
//        ContentResolver musicResolver = getContentResolver();
//        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//        String[] projection = { MediaStore.Audio.Media._ID,
//                MediaStore.Audio.Media.ARTIST,
//                MediaStore.Audio.Media.TITLE,
//                MediaStore.Audio.Media.DATA,
//                MediaStore.Audio.Media.DISPLAY_NAME,
//                MediaStore.Audio.Media.DURATION};
//        Cursor musicCursor =  musicResolver.query(Uri.fromFile(f), projection, null, null, null);
//        String thisTitle = "";
//        if(musicCursor!=null && musicCursor.moveToFirst()){
//            //get columns
//            int titleColumn = musicCursor.getColumnIndex
//                    (android.provider.MediaStore.Audio.Media.TITLE);
//            int idColumn = musicCursor.getColumnIndex
//                    (android.provider.MediaStore.Audio.Media._ID);
//            int artistColumn = musicCursor.getColumnIndex
//                    (android.provider.MediaStore.Audio.Media.ARTIST);
//            //add songs to list
//            do {
//                long thisId = musicCursor.getLong(idColumn);
//                thisTitle = musicCursor.getString(titleColumn);
//                String thisArtist = musicCursor.getString(artistColumn);
//            }
//            while (musicCursor.moveToNext());
//        }

        String title = m_currentTrack.GetName().replace("_", " ");
        title = title.replace(".mp3", "");

        remoteViews.setTextViewText(R.id.TitleTrackPlay, title);
        //       remoteViews.setTextViewText(R.id.TitleTrackPlay, thisTitle);
        remoteViews.setOnClickPendingIntent(R.id.rootPlay, pendInt);

        Intent intentNext = new Intent(this, MPlayer.class);
        intentNext.putExtra("CMD", MPlayer.CMD_NEXT);
        PendingIntent pendNext = PendingIntent.getService(this, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.nextPlayerPlay, pendNext);

        Intent intentPrevious = new Intent(this, MPlayer.class);
        intentPrevious.putExtra("CMD", MPlayer.CMD_PREVIOUS);
        PendingIntent pendPrevious = PendingIntent.getService(this, 1, intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.previousPlayerPlay, pendPrevious);

        Intent intentPause = new Intent(this, MPlayer.class);
        intentPause.putExtra("CMD", MPlayer.CMD_PAUSE);
        PendingIntent pendPause = PendingIntent.getService(this, 2, intentPause, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.pausePlayerPlay, pendPause);

        Intent intentExit = new Intent(MainActivity.BROADCAST_ACTION);
        intentExit.putExtra("CMD", MainActivity.CMD_EXIT);

        PendingIntent pendExit = PendingIntent.getBroadcast(this, 3, intentExit, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.exitPlayerPlay, pendExit);

        Builder builder = new Builder(this);
        builder.setSmallIcon(R.drawable.song_image);
        builder.setCustomContentView(remoteViews);
        builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification not = builder.build();
        startForeground(1, not);
    }

    private void ShowNotificationPause() {
        if (m_currentTrack == null) return;

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_controller_pause);

        String title = m_currentTrack.GetName().replace("_", " ");
        title = title.replace(".mp3", "");

        remoteViews.setTextViewText(R.id.TitleTrackPause, title);
        remoteViews.setOnClickPendingIntent(R.id.rootPlay, pendInt);

        Intent intentNext = new Intent(this, MPlayer.class);
        intentNext.putExtra("CMD", MPlayer.CMD_NEXT);
        PendingIntent pendNext = PendingIntent.getService(this, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.nextPlayerPause, pendNext);

        Intent intentPrevious = new Intent(this, MPlayer.class);
        intentPrevious.putExtra("CMD", MPlayer.CMD_PREVIOUS);
        PendingIntent pendPrevious = PendingIntent.getService(this, 1, intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.previousPlayerPause, pendPrevious);

        Intent intentPlay = new Intent(this, MPlayer.class);
        intentPlay.putExtra("CMD", MPlayer.CMD_PLAY);
        PendingIntent pendPlay = PendingIntent.getService(this, 2, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.playPlayerPause, pendPlay);

        Intent intentExit = new Intent(MainActivity.BROADCAST_ACTION);
        intentExit.putExtra("CMD", MainActivity.CMD_EXIT);

        PendingIntent pendExit = PendingIntent.getBroadcast(this, 3, intentExit, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.exitPlayerPause, pendExit);


        Builder builder = new Builder(this);
        builder.setSmallIcon(R.drawable.song_image);
        builder.setCustomContentView(remoteViews);
        builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification not = builder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null)
            notificationManager.notify(1, not);
    }

    public void Play() {

        StartPlayback();
        m_audioManager.requestAudioFocus(m_afLiListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        m_mediaPlayer.start();
        ShowNotificationPlay();
    }

    public void Pause() {
        StopPlayback();
        m_mediaPlayer.pause();
        ShowNotificationPause();
    }

    public void Stop() {
        StopPlayback();
        AbandonAudioFocus();
        m_mediaPlayer.stop();
        ShowNotificationPause();
    }

    public void PlayNext() {
        if (m_currentTrack != null) {
            int indexTrack = m_currentTrack.GetNumber() + 1;
            SelectTrack(m_currentTrack.GetParentNumber(), indexTrack);
        }
    }

    public void PlayPrevious() {
        if (m_currentTrack != null) {
            int indexTrack = m_currentTrack.GetNumber() - 1;
            SelectTrack(m_currentTrack.GetParentNumber(), indexTrack);
        }
    }

    public boolean SelectTrack(int folder, int track) {
        NodeDirectory trackNode = m_musicFiles.GetTrack(folder, track);
        if (trackNode != null) {
            // запускаем трек
            m_currentTrack = trackNode;
            StartPlayer();
            return true;
        }
        return false;
    }

    private void StartPlayer() {
        // Устанавливаем дорожу
        SetupPlayer(m_currentTrack.GetPathDir());
        Play();
    }

    public boolean IsPlay() {
        return m_mediaPlayer.isPlaying();
    }

    // получение в времени в мсек
    public int GetCurrentPosition() {
        return m_mediaPlayer.getCurrentPosition();
    }

    // Установка гомкости плеера
    public void SetVolume(float leftVolume, float rightVolume) {
        m_mediaPlayer.setVolume(leftVolume, rightVolume);
    }

    // открывает деректорию с файлами
    private void CreateMusicFiles() {
        String m_dirRoot = Environment.getExternalStorageDirectory().getPath();
        String dirPath = m_dirRoot + m_rootPath;
        m_musicFiles = new MusicFiles(dirPath);
    }

    private void CreateTime() {
        m_runnable = () -> {

            if (IsPlay()) {
                int time = GetCurrentPosition();
                Intent intent = new Intent(MainActivity.BROADCAST_ACTION);
                intent.putExtra("CMD", CMD_SEND_TIME);
                intent.putExtra("folder", m_currentTrack.GetParentNumber());
                intent.putExtra("track", m_currentTrack.GetNumber());
                intent.putExtra("time", time);
                sendBroadcast(intent);

                Intent intentUart = new Intent(this, UARTService.class);
                intentUart.putExtra("CMD", CMD_SEND_TIME);
                intentUart.putExtra("folder", m_currentTrack.GetParentNumber());
                intentUart.putExtra("track", m_currentTrack.GetNumber() + 1);
                intentUart.putExtra("time", time);
                startService(intentUart);
            }
            m_handler.postDelayed(m_runnable, 900);
        };

        m_handler = new Handler();
        m_handler.post(m_runnable);
    }

    // создает плеер
    private void CreatePlayer() {
        m_mediaPlayer = new MediaPlayer();
        // Устанавливаем наблюдателя по оканчанию дорожки
        m_mediaPlayer.setOnCompletionListener(this);
    }

    // Устанавливаем дорожку для запуска плеера
    private void SetupPlayer(String audio) {
        try {
            m_mediaPlayer.reset();
            m_mediaPlayer.setDataSource(audio);
            m_mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            m_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            m_mediaPlayer.prepare();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        PlayNext();
    }

    private void ParserCMD(Intent intent) {
        int cmd = intent.getIntExtra("CMD", 0);
        switch (cmd) {
            case CMD_NEXT:
                PlayNext();
                break;
            case CMD_PREVIOUS:
                PlayPrevious();
                break;
            case CMD_PAUSE:
                Pause();
                break;
            case CMD_PLAY:
                Play();
                break;

            case CMD_CHANGE_ROOT:
                ChangeRoot();
                break;
            case CMD_SELECT_TRACK: {
                int folder = intent.getIntExtra("folder", -1);
                int track = intent.getIntExtra("track", 0) - 1;
                SelectTrack(folder, track);
                break;
            }
            default:
                break;
        }
    }

    private void ChangeRoot() {
        SharedPreferences sPref = getSharedPreferences("Setting", MODE_PRIVATE);
        String savedText = sPref.getString(SettingActivity.SAVED_MUSIC_PATH, "");

        File file = new File(savedText);
        // если это папка
        if (file.isDirectory()) {
            m_musicFiles = new MusicFiles(savedText);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    class AFListener implements AudioManager.OnAudioFocusChangeListener {

        @Override
        public void onAudioFocusChange(int i) {

            switch (i) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    Pause();
//                    UnregisterRemountControl();

                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Pause();
//                    UnregisterRemountControl();

                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    SetVolume(0.5f, 0.5f);
                    break;

                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!IsPlay())
                        Play();
                    SetVolume(1.0f, 1.0f);
//                    RegisterRemountControl();
                    break;
            }
        }
    }

    private void RegisterRemountControl() {

        try {

//            mReceiverComponent = new ComponentName(this, RemoteControlReceiver.class);
//            m_audioManager.registerMediaButtonEventReceiver(mReceiverComponent);

//           m_audioManager.registerMediaButtonEventReceiver(new ComponentName(getPackageName(), RemoteControlReceiver.class.getName()));
//            registerReceiver(m_remoteControlReceiver, intentFilterRemoteControl);
        } catch (IllegalArgumentException e) {

        }
    }

    private void UnregisterRemountControl() {
        try {
//            unregisterReceiver(m_remoteControlReceiver);
//            m_audioManager.unregisterMediaButtonEventReceiver(mReceiverComponent);
        } catch (IllegalArgumentException e) {
        }
    }

    private class NoisyAudioStreamReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Pause();
            }
        }
    }

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private IntentFilter intentFilterRemoteControl = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);

    private void StartPlayback() {
        try {
            registerReceiver(m_noisyAudioStreamReceiver, intentFilter);
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void StopPlayback() {
        try {
            unregisterReceiver(m_noisyAudioStreamReceiver);
        } catch (IllegalArgumentException e) {

        }
    }

    public class RemoteControlReceiver extends BroadcastReceiver {


        // Constructor is mandatory
        public RemoteControlReceiver ()
        {
            super ();
        }
        @Override
        public void onReceive(Context context, Intent intent) {

            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                Toast.makeText(context, intent.getAction(), Toast.LENGTH_SHORT);

//                switch (key.getKeyCode()) {
//
//                    case KeyEvent.KEYCODE_MEDIA_PLAY:
//                        Play();
//                        break;
//
//                    case KeyEvent.KEYCODE_MEDIA_NEXT:
//                        PlayNext();
//                        break;
//
//                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
//                        PlayPrevious();
//                        break;
//
//                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
//                        Pause();
//                        break;
//                }
            }
        }
    }

}