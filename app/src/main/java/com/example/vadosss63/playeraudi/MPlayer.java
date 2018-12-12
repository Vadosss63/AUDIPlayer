package com.example.vadosss63.playeraudi;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.Vector;

public class MPlayer extends Service implements OnCompletionListener, MediaPlayer.OnErrorListener
{
    // Плеер для воспроизведения
    private MediaPlayer m_mediaPlayer;

    static final public int CMD_SELECT_TRACK = 0x05;
    static final public int CMD_PLAY = 0x06;
    static final public int CMD_PAUSE = 0x07;
    static final public int CMD_PREVIOUS = 0x08;
    static final public int CMD_NEXT = 0x09;
    static final public int CMD_CHANGE_ROOT = 0x0A;


    static final public int CMD_SEND_TIME = 0x01;

    private Handler m_handler;
    private Runnable m_runnable;

    // Текущий выбранный трек
    private NodeDirectory m_currentTrack;// Установить на первую песню

    // дериктория для воспроизведения
    private MusicFiles m_musicFiles;

    // корневая папка для воспроизведения музыки
    private String m_rootPath = "/Music";

    @Override
    public void onCreate()
    {
        super.onCreate();
        CreatePlayer();
        CreateMusicFiles();
        CreateTime();
        SelectTrack(1, 0);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        ParserCMD(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy()
    {
        m_mediaPlayer.release();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public void Play()
    {
        m_mediaPlayer.start();
        ShowNotificationPlay();
    }

    private void ShowNotificationPlay()
    {
        if(m_currentTrack == null) return;

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


        remoteViews.setTextViewText(R.id.TitleTrackPlay, m_currentTrack.GetName());
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.drawable.ic_backgraund).setCustomContentView(remoteViews).setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification not = builder.build();
        startForeground(1, not);
    }

    private void ShowNotificationPause()
    {
        if(m_currentTrack == null) return;

        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendInt = PendingIntent.getActivity(this, 0, notIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification_controller_pause);
        remoteViews.setTextViewText(R.id.TitleTrackPause, m_currentTrack.GetName());
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


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this).setSmallIcon(R.mipmap.ic_launcher).setCustomContentView(remoteViews).setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification not = builder.build();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(1, not);
    }

    public void Pause()
    {
        m_mediaPlayer.pause();
        ShowNotificationPause();
    }

    public void Stop()
    {
        m_mediaPlayer.stop();
    }

    public void PlayNext()
    {
        if(m_currentTrack != null)
        {
            int indexTrack = m_currentTrack.GetNumber() + 1;
            SelectTrack(m_currentTrack.GetParentNumber(), indexTrack);
        }
    }

    public void PlayPrevious()
    {
        if(m_currentTrack != null)
        {
            int indexTrack = m_currentTrack.GetNumber() - 1;
            SelectTrack(m_currentTrack.GetParentNumber(), indexTrack);
        }
    }

    public boolean SelectTrack(int folder, int track)
    {
        NodeDirectory trackNode = m_musicFiles.GetTrack(folder, track);
        if(trackNode != null)
        {
            // запускаем трек
            m_currentTrack = trackNode;
            StartPlayer();
            return true;
        }
        return false;
    }

    // получение времени
    @SuppressLint("DefaultLocale")
    public String GetCurrentTimePlay()
    {
        String timeString = "00:00";
        if(m_mediaPlayer.isPlaying())
        {
            Time time = new Time(m_mediaPlayer.getCurrentPosition());
            timeString = String.format("%02d:%02d", time.getMinutes(), time.getSeconds());
        }
        return timeString;
    }

    public boolean IsPlay()
    {
        return m_mediaPlayer.isPlaying();
    }

    // получение в времени в мсек
    public int GetCurrentPosition()
    {
        return m_mediaPlayer.getCurrentPosition();
    }

    // возврощает текущий трек
    public NodeDirectory GetCurrentTrack()
    {
        return m_currentTrack;
    }

    // Возврощает список файлов из папки
    public Vector<NodeDirectory> GetPlayList(int folder)
    {
        return m_musicFiles.GetAllFiles(folder);
    }

    // открывает деректорию с файлами
    private void CreateMusicFiles()
    {
        String m_dirRoot = Environment.getExternalStorageDirectory().getPath();
        String dirPath = m_dirRoot + m_rootPath;
        m_musicFiles = new MusicFiles(dirPath);
    }

    private void CreateTime()
    {
        m_runnable = ()->{

            if(m_mediaPlayer.isPlaying())
            {
                int time = m_mediaPlayer.getCurrentPosition();
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
    private void CreatePlayer()
    {
        m_mediaPlayer = new MediaPlayer();
        // Устанавливаем наблюдателя по оканчанию дорожки
        m_mediaPlayer.setOnCompletionListener(this);
    }

    private void StartPlayer()
    {
        // Устанавливаем дорожу
        SetupPlayer(m_currentTrack.GetPathDir());
        // Запускаем
        m_mediaPlayer.start();
        ShowNotificationPlay();
    }

    // Устанавливаем дорожку для запуска плеера
    private void SetupPlayer(String audio)
    {
        try
        {
            m_mediaPlayer.reset();
            m_mediaPlayer.setDataSource(audio);
            m_mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            m_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            m_mediaPlayer.prepare();

        } catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        PlayNext();
    }


    private void ParserCMD(Intent intent)
    {
        int cmd = intent.getIntExtra("CMD", 0);
        switch(cmd)
        {
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
            case CMD_SELECT_TRACK:
            {
                int folder = intent.getIntExtra("folder", -1);
                int track = intent.getIntExtra("track", 0) - 1;
                SelectTrack(folder, track);
                break;
            }
            default:
                break;
        }
    }

    private void ChangeRoot()
    {
        SharedPreferences sPref = getSharedPreferences("Setting", MODE_PRIVATE);
        String savedText = sPref.getString(SettingActivity.SAVED_MUSIC_PATH, "");

        File file = new File(savedText);
        // если это папка
        if(file.isDirectory())
        {
            m_musicFiles = new MusicFiles(savedText);
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }
}