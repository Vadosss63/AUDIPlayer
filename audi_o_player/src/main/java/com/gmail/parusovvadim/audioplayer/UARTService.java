package com.gmail.parusovvadim.audioplayer;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;

import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderMainHeader;
import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderTimeTrack;
import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderTrack;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Vector;

public class UARTService extends Service
{
    static final public int CMD_SEND_DATA = 0xAA;
    static final public int CMD_STATE_AUX = 0x0A;
    static final public int CMD_CHANGE_DISC = 0x04;
    static final public int CMD_RESET = 0x00;
    // лист команд на выполнения
    private ArrayDeque<Intent> m_listCMD;
    // статус ответа
    private byte m_answer = 1;

    private boolean m_isStartThread = true;

    // класс подключения для COM
    private UARTPort m_uartPort;

    @Override
    public void onCreate()
    {
        super.onCreate();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
        {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
        ResetUART();
    }

    private void ResetUART()
    {
        m_uartPort = new UARTPort();
        m_listCMD = new ArrayDeque<>();
        CreateUARTPort();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(m_uartPort.GetIsUartConfigured()) AddCMD(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    synchronized private void AddCMD(Intent intent)
    {
        m_listCMD.addLast(intent);
    }

    private void Parser(Intent intent)
    {
        if(!m_uartPort.GetIsUartConfigured()) return;
        int cmd = intent.getIntExtra("CMD", 0);
        switch(cmd)
        {
            case MPlayer.CMD_SEND_TIME:
                SendTime(intent);
                break;

            case CMD_SEND_DATA:
                SendDataByte(intent);
//                Toast toast = Toast.makeText(this, "DAta", Toast.LENGTH_SHORT);
//                toast.show();
                break;

            case CMD_CHANGE_DISC:
                ChangeDisk();
                break;

            case CMD_RESET:
                ResetUART();
                break;
            default:
                break;
        }

    }

    private void ChangeDisk()
    {
        int a = 0; // Начальное значение диапазона - "от"
        int b = 255; // Конечное значение диапазона - "до"

        int random_number = a + (int) (Math.random() * b);

        Vector<Byte> data = new Vector<>();
        data.add((byte) 0x00);
        data.add((byte) 0x00);
        data.add((byte) 0x00);
        data.add((byte) random_number);

        EncoderMainHeader mainHeader = new EncoderMainHeader(data);
        mainHeader.AddMainHeader((byte) CMD_CHANGE_DISC);
        m_uartPort.WriteData(mainHeader.GetDataByte());
    }

    // Отправка произвольных данных
    private void SendDataByte(Intent intent)
    {
        byte[] track = intent.getByteArrayExtra("Data");
        m_uartPort.WriteData(track);
    }

    private void SendTime(Intent intent)
    {
        int folder = intent.getIntExtra("folder", 0);
        int track = intent.getIntExtra("track", 0);
        int time = intent.getIntExtra("time", 0);
        EncoderTimeTrack timeTrack = new EncoderTimeTrack();
        timeTrack.AddHeader(folder);
        timeTrack.AddTrackNumber(track);
        timeTrack.AddCurrentTimePosition(time);
        EncoderMainHeader mainHeader = new EncoderMainHeader(timeTrack.GetVectorByte());
        mainHeader.AddMainHeader((byte) 0x01);
        m_uartPort.WriteData(mainHeader.GetDataByte());
    }

    @Override
    public void onDestroy()
    {
        m_isStartThread = false;
        m_uartPort.DisconnectFunction();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void CreateUARTPort()
    {
        String msg = null;
        if(m_uartPort.ConnectToManager(this))
        {
            m_uartPort.Connect();
            if(m_uartPort.GetIsUartConfigured())
            {
                m_uartPort.SetReadRunnable(this::ReadCommand);
                // Запускаем прослушку команд управления
                m_uartPort.ReadData();
                msg = m_uartPort.GetTextLog();

                // запускаем поток отправки
                m_isStartThread = true;
                SenderThread m_senderThread = new SenderThread();
                m_senderThread.start();

            } else
            {
                msg = "OTG не подключен";
            }
        } else
        {
            msg = "Error";
        }

        Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        toast.show();
    }

    // Обработка пришедших команд с порта
    private void ReadCommand()
    {
        byte[] data = m_uartPort.GetReadDataByte();

        if(data.length == 1)
        {
            SetAnswer(data[0]);
            return;
        }

        byte cmd = data[2];
        switch(cmd)
        {
            case (byte) MPlayer.CMD_SELECT_TRACK:
            {
                SelectTrack(data);
                break;
            }
            case (byte) MPlayer.CMD_PLAY:
            {
                SendKey(KeyEvent.KEYCODE_MEDIA_PLAY);
                break;
            }
            case (byte) MPlayer.CMD_PAUSE:
            {
                SendKey(KeyEvent.KEYCODE_MEDIA_PAUSE);
                break;
            }
            case (byte) MPlayer.CMD_PREVIOUS:
            {
                SendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                break;
            }
            case (byte) MPlayer.CMD_NEXT:
            {
                SendKey(KeyEvent.KEYCODE_MEDIA_NEXT);
                break;
            }
        }
    }

    private void SelectTrack(byte[] data)
    {
        Vector<Byte> dataTrack = new Vector<>();

        for(int i = 5; i < data.length - 1; i++)
            dataTrack.add(data[i]);

        EncoderTrack encoderTrack = new EncoderTrack(dataTrack);
        int folder = encoderTrack.GetFolder();
        int track = encoderTrack.GetTrackNumber();

        Intent intent = new Intent(this, MPlayer.class);
        intent.putExtra("CMD", MPlayer.CMD_SELECT_TRACK);
        intent.putExtra("folder", folder);
        intent.putExtra("track", track);
        startService(intent);
    }

    private synchronized void SetAnswer(byte answer)
    {
        m_answer = answer;
    }

    private synchronized byte GetAnswer()
    {
        return m_answer;
    }

    private class SenderThread extends Thread
    {

        SenderThread()
        {
            this.setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run()
        {
            while(m_isStartThread)
            {
                try
                {
                    if(m_listCMD.isEmpty())
                    {
                        Thread.sleep(50);
                    } else
                        // выполняем первую задачу
                        synchronized(m_listCMD)
                        {
                            if(!m_listCMD.isEmpty())
                            {

                                int i = 0;
                                do // надо подумать как Сделать отправку
                                {
                                    Parser(m_listCMD.getFirst());
                                    i++;
                                } while(!CheckAnswer() && i < 2);

                                SetAnswer((byte) 1);
                                m_listCMD.pollFirst();
                            }
                        }
                } catch(InterruptedException e)
                {
                }
            }
        }

        boolean CheckAnswer()
        {
            int i = 0;
            while(i < 100)
            {
                if(GetAnswer() != 0)
                {
                    try
                    {
                        Thread.sleep(50);
                    } catch(InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                } else return true;
                i++;
            }
            return false;
        }
    }

    void SendKey(int key)
    {
        MediaSessionManager mediaSessionManager = (MediaSessionManager) getApplicationContext().getSystemService(Context.MEDIA_SESSION_SERVICE);
        try
        {
            List<MediaController> mediaControllerList = mediaSessionManager.getActiveSessions(new ComponentName(getApplicationContext(), NotificationReceiverService.class));
            for(MediaController m : mediaControllerList)
            {
                m.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
                m.dispatchMediaButtonEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
            }
        } catch(SecurityException e)
        {
            Log.d("My", e.toString());
        }
    }

}
