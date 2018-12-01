package com.example.vadosss63.playeraudi;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.sql.Time;
import java.util.Vector;

interface MusicPlayer
{
    void Play();

    void Pause();

    void Stop();

    void PlayNext();

    void PlayPrevious();

    boolean IsPlay();

    int GetCurrentPosition();

    String GetCurrentTimePlay();

    boolean SelectTrack(int folder, int track);

}

public class MPlayer extends Service implements OnCompletionListener, MusicPlayer
{
    // Плеер для воспроизведения
    private MediaPlayer m_mediaPlayer;

    static final public int CMD_SELECT_TRACK = 0x05;
    static final public int CMD_PLAY = 0x06;
    static final public int CMD_PAUSE = 0x07;
    static final public int CMD_PREVIOUS = 0x08;
    static final public int CMD_NEXT = 0x09;

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
        ShowNotifiction();
    }

    private void ShowNotifiction()
    {
        Intent notIntent = new Intent(this, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this);

        builder.setContentIntent(pendInt)
                .setSmallIcon(R.drawable.android_music_player_play)
                .setTicker(m_currentTrack.GetName())
                .setOngoing(true)
                .setContentTitle(m_currentTrack.GetName())
  .setContentText(m_currentTrack.GetName());
        Notification not = builder.build();

        startForeground(1, not);
    }

    public void Pause()
    {
        m_mediaPlayer.pause();
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
        ShowNotifiction();
    }

    // Устанавливаем дорожку для запуска плеера
    private void SetupPlayer(String audio)
    {
        try
        {
            m_mediaPlayer.reset();
            m_mediaPlayer.setDataSource(audio);
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
}