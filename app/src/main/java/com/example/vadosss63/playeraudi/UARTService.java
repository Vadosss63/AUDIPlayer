package com.example.vadosss63.playeraudi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.example.vadosss63.playeraudi.encoder_uart.EncoderMainHeader;
import com.example.vadosss63.playeraudi.encoder_uart.EncoderTimeTrack;
import com.example.vadosss63.playeraudi.encoder_uart.EncoderTrack;

import java.util.ArrayDeque;
import java.util.Vector;

public class UARTService extends Service
{
    static final public int CMD_SEND_DATA = 0xAA;
    static final public int CMD_CHANGE_DISC = 0x04;
    // лист команд на выполнения
    private ArrayDeque<Intent> m_listCMD;
    // статус ответа
    private byte m_answer = 1;

    private SenderThread m_senderThread;
    private boolean m_isStartThread = true;


    // класс подключения для COM
    private UARTPort m_uartPort;

    @Override
    public void onCreate()
    {
        super.onCreate();
        m_uartPort = new UARTPort();
        m_listCMD = new ArrayDeque<Intent>();
        CreateUARTPort();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
//        Parser(intent);
        if(m_uartPort.GetIsUartConfigured())
            AddCMD(intent);

        return super.onStartCommand(intent, flags, startId);
    }

    synchronized private void AddCMD(Intent intent)
    {
        m_listCMD.addLast(intent);
        Log.d("MyTest", "add intent");
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

            case CMD_CHANGE_DISC:
                ChangeDisk();
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

        Vector<Byte> data = new Vector<Byte>();
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

                // запускаем поток отправки
                m_isStartThread = true;
                m_senderThread = new SenderThread(this);
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

        Context m_context;

        SenderThread(Context context)
        {
            m_context = context;
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
                    }else
                        // выполняем первую задачу
                        synchronized(m_listCMD)
                        {
                            if(!m_listCMD.isEmpty())
                            {
//                            Toast toasts = Toast.makeText(m_context, "read intent", Toast.LENGTH_SHORT);
//                            toasts.show();

                                Log.d("MyTest", "read intent");

                                int i = 0;
                                do // надо подумать как Сделать отправку
                                {
                                    Parser(m_listCMD.getFirst());
                                    i++;
                                } while(!CheckAnswer() && i < 4);

                                if(GetAnswer() != 0)
                                {
//                                Toast toast = Toast.makeText(m_context, "Ошибка отправки", Toast.LENGTH_SHORT);
//                                toast.show();
                                    Log.d("MyTest", "error intent");

                                }

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
            while(i < 10)
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
}
