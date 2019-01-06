package com.gmail.parusovvadim.audioplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat.Builder;
import android.widget.RemoteViews;

class NotificationSoundControl {

    static private String CHANEL_ID = "AUDIo";
    static private String CHANEL_NAME = "AUDIo";
    static private int NOTIFICATION_ID = 1;

    private enum requestCode { MainActivity, Next, Previous, Play, Pause, Exit }

    private Service m_service;

    private PendingIntent m_pendMainActivity;
    private PendingIntent m_pendNext;
    private PendingIntent m_pendPrevious;
    private PendingIntent m_pendPlay;
    private PendingIntent m_pendPause;
    private PendingIntent m_pendExit;

    private RemoteViews remoteViews;

    NotificationSoundControl(Service service) {

        this.m_service = service;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationManager notificationManager = (NotificationManager) m_service.getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(CHANEL_ID, CHANEL_NAME, importance);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{0L});
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }

        CreatePendingIntent();
        CreateRemoteViews();
    }

    void ShowNotificationPlay(NodeDirectory m_currentTrack) {

        if (m_currentTrack == null) return;

//        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
//        mediaMetadataRetriever.setDataSource(m_currentTrack.GetPathDir());
//        String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
//        String tit = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

        String title = getTrimTitle(m_currentTrack);

        remoteViews.setTextViewText(R.id.titleTrackPlay, title);
        remoteViews.setInt(R.id.pausePlayer, "setBackgroundResource", android.R.drawable.ic_media_pause);
        remoteViews.setOnClickPendingIntent(R.id.pausePlayer, m_pendPause);

        CreateNotification(remoteViews);
    }

    void ShowNotificationPause(NodeDirectory m_currentTrack) {

        if (m_currentTrack == null) return;

        String title = getTrimTitle(m_currentTrack);

        remoteViews.setTextViewText(R.id.titleTrackPlay, title);
        remoteViews.setInt(R.id.pausePlayer, "setBackgroundResource", android.R.drawable.ic_media_play);
        remoteViews.setOnClickPendingIntent(R.id.pausePlayer, m_pendPlay);

        CreateNotification(remoteViews);
    }

    @NonNull
    private String getTrimTitle(NodeDirectory m_currentTrack) {
        String title = m_currentTrack.GetName().replace("_", " ");
        title = title.replace(".mp3", "");
        return title;
    }

    private void CreateRemoteViews() {

        remoteViews = new RemoteViews(m_service.getPackageName(), R.layout.notification_controller_play);

        remoteViews.setOnClickPendingIntent(R.id.rootPlayer, m_pendMainActivity);

        remoteViews.setOnClickPendingIntent(R.id.nextPlayer, m_pendNext);

        remoteViews.setOnClickPendingIntent(R.id.previousPlayer, m_pendPrevious);

        remoteViews.setOnClickPendingIntent(R.id.exitPlayer, m_pendExit);
    }


    private void CreatePendingIntent() {

        Intent notIntent = new Intent(m_service, MainActivity.class);
        notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        m_pendMainActivity = PendingIntent.getActivity(m_service, requestCode.MainActivity.ordinal(), notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentNext = new Intent(m_service, MPlayer.class);
        intentNext.putExtra("CMD", MPlayer.CMD_NEXT);
        m_pendNext = PendingIntent.getService(m_service, requestCode.Next.ordinal(), intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPrevious = new Intent(m_service, MPlayer.class);
        intentPrevious.putExtra("CMD", MPlayer.CMD_PREVIOUS);
        m_pendPrevious = PendingIntent.getService(m_service, requestCode.Previous.ordinal(), intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPlay = new Intent(m_service, MPlayer.class);
        intentPlay.putExtra("CMD", MPlayer.CMD_PLAY);
        m_pendPlay = PendingIntent.getService(m_service, requestCode.Play.ordinal(), intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentPause = new Intent(m_service, MPlayer.class);
        intentPause.putExtra("CMD", MPlayer.CMD_PAUSE);
        m_pendPause = PendingIntent.getService(m_service, requestCode.Pause.ordinal(), intentPause, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentExit = new Intent(MainActivity.BROADCAST_ACTION);
        intentExit.putExtra("CMD", MainActivity.CMD_EXIT);

        m_pendExit = PendingIntent.getBroadcast(m_service, requestCode.Exit.ordinal(), intentExit, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void CreateNotification(RemoteViews remoteViews) {

        Builder builder = new Builder(m_service, CHANEL_ID);
        builder.setContent(remoteViews);
        builder.setSmallIcon(R.drawable.play_pause);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        Notification notification = builder.build();
        m_service.startForeground(NOTIFICATION_ID, notification);
    }

}