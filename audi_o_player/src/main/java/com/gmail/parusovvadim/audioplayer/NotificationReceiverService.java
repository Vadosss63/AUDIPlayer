package com.gmail.parusovvadim.audioplayer;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderByteMainHeader;
import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderMainHeader;

import java.util.Vector;

public class NotificationReceiverService extends NotificationListenerService
{

    // Поддерживаемые плееры
    final static String YA_MUSIC = "ru.yandex.music";
    final static String GOOGLE_MUSIC = "com.google.android.music";
    final static String VK_MUSIC = "com.vkontakte.music";
    final static String ZYCEV_NET = "com.p74.player";
    final static String MI_PLAYER = "media.music.mp3player.musicplayer";
    final static String AUDIO_PLAYER = "com.gmail.parusovvadim.audioplayer";

    @Override
    public IBinder onBind(Intent intent)
    {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        String packageName = sbn.getPackageName();
        boolean onAUX = packageName.equals(YA_MUSIC) || packageName.equals(GOOGLE_MUSIC) || packageName.equals(VK_MUSIC) || packageName.equals(ZYCEV_NET) || packageName.equals(MI_PLAYER);

        if(onAUX)
        {
            onAUX((byte)0x01);

            String title = "No artist";
            String text = "No title";

            Bundle extras = sbn.getNotification().extras;

            if(extras != null && extras.getString("android.title") != null)
                title = extras.getString("android.title");

            if(extras != null && extras.getCharSequence("android.text") != null)
                text = extras.getCharSequence("android.text").toString();

            AUXSetup(title + text);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        String packageName = sbn.getPackageName();
        boolean offAUX = packageName.equals(YA_MUSIC) || packageName.equals(GOOGLE_MUSIC) || packageName.equals(VK_MUSIC) || packageName.equals(ZYCEV_NET) || packageName.equals(MI_PLAYER);

        if(offAUX)
            onAUX((byte)0x00);

    }

    private void AUXSetup(String title)
    {
        EncoderByteMainHeader.EncoderListTracks encoderListTracks = new EncoderByteMainHeader.EncoderListTracks();

        encoderListTracks.AddHeader(1);

        encoderListTracks.AddTrackNumber(1);
        encoderListTracks.AddName("Prev");

        encoderListTracks.AddTrackNumber(2);
        encoderListTracks.AddName(title);

        encoderListTracks.AddTrackNumber(3);
        encoderListTracks.AddName("Next");

        encoderListTracks.AddEnd();

        // Добавляем заголовок
        EncoderMainHeader headerData = new EncoderMainHeader(encoderListTracks.GetVectorByte());
        headerData.AddMainHeader((byte) 0x03);

        SendData(headerData);
    }

    private void SendData(EncoderMainHeader headerData)
    {
        Intent intent = new Intent(this, UARTService.class);
        intent.putExtra(getString(R.string.CMD), UARTService.CMD_SEND_DATA);
        intent.putExtra(getString(R.string.CMD_data), headerData.GetDataByte());
        startService(intent);
    }

    private void onAUX(byte on)
    {
        Vector<Byte> data = new Vector<>();
        data.add(on);
        EncoderMainHeader mainHeader = new EncoderMainHeader(data);
        mainHeader.AddMainHeader((byte) 0x0A);

        SendData(mainHeader);
    }
}