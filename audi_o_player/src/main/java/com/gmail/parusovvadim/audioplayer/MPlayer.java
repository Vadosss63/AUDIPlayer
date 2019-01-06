package com.gmail.parusovvadim.audioplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;

public class MPlayer extends Service implements OnCompletionListener, MediaPlayer.OnErrorListener
{
    static final public int CMD_SELECT_TRACK = 0x05;
    static final public int CMD_PLAY = 0x06;
    static final public int CMD_PAUSE = 0x07;
    static final public int CMD_PREVIOUS = 0x08;
    static final public int CMD_NEXT = 0x09;
    static final public int CMD_CHANGE_ROOT = 0x0A;
    static final public int CMD_PLAY_PAUSE = 0x0B;
    static final public int CMD_SEND_TIME = 0x01;

    NotificationSoundControl m_notificationSoundControl = null;

    // Плеер для воспроизведения
    private MediaPlayer m_mediaPlayer;

    NoisyAudioStreamReceiver m_noisyAudioStreamReceiver;

    // аудио фокус
    AudioManager m_audioManager;
    AFListener m_afLiListener = new AFListener();

    private Handler m_handler;
    private Runnable m_runnable;

    // Текущий выбранный трек
    private NodeDirectory m_currentTrack; // Установить на первую песню

    // дериктория для воспроизведения
    private MusicFiles m_musicFiles;

    // корневая папка для воспроизведения музыки
    private String m_rootPath = "/Music";
    private ComponentName m_receiverComponent;

    private SettingApp m_settingApp;

    @Override
    public void onCreate()
    {
        super.onCreate();
        m_settingApp = new SettingApp(this);
        m_audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        m_noisyAudioStreamReceiver = new NoisyAudioStreamReceiver();
        CreatePlayer();
        CreateMusicFiles();
        CreateTime();
        m_notificationSoundControl = new NotificationSoundControl(this);

        SelectTrack(1, 0);
        Pause();
        RegisterRemountControl();
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
        super.onDestroy();

        if(m_mediaPlayer != null) m_mediaPlayer.release();

        AbandonAudioFocus();
        StopPlayback();
        UnregisterRemountControl();
    }

    private void AbandonAudioFocus()
    {
        if(m_afLiListener != null) m_audioManager.abandonAudioFocus(m_afLiListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void ShowNotificationPlay()
    {
        m_notificationSoundControl.ShowNotificationPlay(m_currentTrack);
    }

    private void ShowNotificationPause()
    {
        m_notificationSoundControl.ShowNotificationPause(m_currentTrack);
    }

    public void Play()
    {
        StartPlayback();
        m_audioManager.requestAudioFocus(m_afLiListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        m_mediaPlayer.start();
        ShowNotificationPlay();
    }

    public void Pause()
    {
        StopPlayback();
        m_mediaPlayer.pause();
        ShowNotificationPause();
    }

    public void Stop()
    {
        StopPlayback();
        AbandonAudioFocus();
        m_mediaPlayer.stop();
        ShowNotificationPause();
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

    private void StartPlayer()
    {
        // Устанавливаем дорожу
        SetupPlayer(m_currentTrack.GetPathDir());
        Play();
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

    // Установка громкости плеера
    public void SetVolume(float leftVolume, float rightVolume)
    {
        m_mediaPlayer.setVolume(leftVolume, rightVolume);
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

            if(IsPlay())
            {
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
    private void CreatePlayer()
    {
        m_mediaPlayer = new MediaPlayer();
        // Устанавливаем наблюдателя по оканчанию дорожки
        m_mediaPlayer.setOnCompletionListener(this);
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
            case CMD_PLAY_PAUSE:
                if(IsPlay())
                {
                    Pause();
                } else
                {
                    Play();
                }
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
        m_settingApp.LoadSetting();
        m_musicFiles = new MusicFiles(m_settingApp.GetAbsolutePath());
        SelectTrack(1, 0);
        Pause();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1)
    {
        return false;
    }

    // действия при смене источника воспроизведения
    class AFListener implements AudioManager.OnAudioFocusChangeListener
    {

        @Override
        public void onAudioFocusChange(int i)
        {

            switch(i)
            {
                case AudioManager.AUDIOFOCUS_LOSS:
                    Pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Pause();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    SetVolume(0.5f, 0.5f);
                    break;

                case AudioManager.AUDIOFOCUS_GAIN:
//                    if(!IsPlay()) Play();
                    SetVolume(1.0f, 1.0f);
                    break;
            }
        }
    }

    // Управление кнопками воспроизведения
    private void RegisterRemountControl()
    {

        try
        {
            m_receiverComponent = new ComponentName(getPackageName(), RemoteControlReceiver.class.getName());
            m_audioManager.registerMediaButtonEventReceiver(m_receiverComponent);
        } catch(IllegalArgumentException e)
        {

        }
    }

    private void UnregisterRemountControl()
    {
        try
        {
            m_audioManager.unregisterMediaButtonEventReceiver(m_receiverComponent);
        } catch(IllegalArgumentException e)
        {
        }
    }


    // Остановка плеера при смене источника воспроизведения
    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    private class NoisyAudioStreamReceiver extends BroadcastReceiver
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            if(AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction()))
            {
                Pause();
            }
        }
    }

    private void StartPlayback()
    {
        try
        {
            registerReceiver(m_noisyAudioStreamReceiver, intentFilter);
        } catch(IllegalArgumentException ignored)
        {

        }
    }

    private void StopPlayback()
    {
        try
        {
            unregisterReceiver(m_noisyAudioStreamReceiver);
        } catch(IllegalArgumentException e)
        {

        }
    }

}