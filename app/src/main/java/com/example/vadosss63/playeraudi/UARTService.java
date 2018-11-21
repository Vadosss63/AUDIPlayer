package com.example.vadosss63.playeraudi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;

import java.util.Vector;

public class UARTService extends Service
{

    static final public int CMD_SEND_DATA = 0xAA;
    // класс подключения для COM
    private UARTPort m_uartPort;

    @Override
    public void onCreate()
    {
        super.onCreate();
        m_uartPort = new UARTPort();
        CreateUARTPort();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {

        Parser(intent);
        return super.onStartCommand(intent, flags, startId);
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
                break;
            default:
                break;
        }

    }

    // Отправка произвольных данных
    private void SendDataByte(Intent intent)
    {
        byte[] track = intent.getByteArrayExtra("Data");
        if(m_uartPort.WriteData(track))
        {
            Toast toast = Toast.makeText(this, "Ok", Toast.LENGTH_SHORT);
            toast.show();

        } else
        {
            Toast toast = Toast.makeText(this, m_uartPort.GetTextLog(), Toast.LENGTH_SHORT);
            toast.show();
        }

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
        if(m_uartPort.WriteData(mainHeader.GetDataByte()))
        {
//            Toast toast = Toast.makeText(this, "Ok", Toast.LENGTH_SHORT);
//            toast.show();

        } else
        {
            Toast toast = Toast.makeText(this, m_uartPort.GetTextLog(), Toast.LENGTH_SHORT);
            toast.show();
        }

    }

    @Override
    public void onDestroy()
    {
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
        ///TODO для отладки
        String msg = null;
        if(m_uartPort.ConnectToManager(this))
        {
            m_uartPort.Connect();
            if(m_uartPort.GetIsUartConfigured())
            {
                m_uartPort.SetReadRunnable(()->{
                    ReadCommand();
                });
                // Запускаем прослушку команд управления
                m_uartPort.ReadData();
                msg = m_uartPort.GetTextLog();

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
        Toast toast;
        toast = Toast.makeText(this, "Command  " + data[2], Toast.LENGTH_SHORT);
        toast.show();

        /// TODO исправить прием команд
        if(data[2] == (byte) 0x05)
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

        } else if(data[2] == (byte) 0x06)
        {
            Intent intent = new Intent(this, MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_PLAY);
            startService(intent);

        } else if(data[2] == (byte) 0x07)
        {
            Intent intent = new Intent(this, MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_PAUSE);
            startService(intent);

        } else if(data[2] == (byte) 0x08)
        {
            Intent intent = new Intent(this, MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_PREVIOUS);
            startService(intent);

        } else if(data[2] == (byte) 0x09)
        {
            Intent intent = new Intent(this, MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_NEXT);
            startService(intent);
        }

    }
}
