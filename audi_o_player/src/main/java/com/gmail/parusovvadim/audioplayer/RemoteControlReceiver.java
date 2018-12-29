package com.gmail.parusovvadim.audioplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;


public class RemoteControlReceiver extends BroadcastReceiver {


    // Constructor is mandatory
    public RemoteControlReceiver() {
        super();
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {

            KeyEvent key = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

            if(key.getAction() != KeyEvent.ACTION_DOWN)
                return;

            switch (key.getKeyCode()) {

                case KeyEvent.KEYCODE_MEDIA_PLAY: {
                    Intent intentn = new Intent(context, MPlayer.class);
                    intentn.putExtra("CMD", MPlayer.CMD_PAUSE);
                    context.startService(intentn);
                    break;
                }

                case KeyEvent.KEYCODE_MEDIA_NEXT: {
                    Intent intentn = new Intent(context, MPlayer.class);
                    intentn.putExtra("CMD", MPlayer.CMD_NEXT);
                    context.startService(intentn);
                    break;
                }
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS: {

                    Intent intentn = new Intent(context, MPlayer.class);
                    intentn.putExtra("CMD", MPlayer.CMD_PREVIOUS);
                    context.startService(intentn);
                    break;
                }

                case KeyEvent.KEYCODE_MEDIA_PAUSE: {
                    Intent intentn = new Intent(context, MPlayer.class);
                    intentn.putExtra("CMD", MPlayer.CMD_PLAY_PAUSE);
                    context.startService(intentn);
                    break;
                }

                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: {
                    Intent intentn = new Intent(context, MPlayer.class);
                    intentn.putExtra("CMD", MPlayer.CMD_PLAY_PAUSE);
                    context.startService(intentn);
                    break;
                }

                case KeyEvent.KEYCODE_HEADSETHOOK: {
                    Intent intentn = new Intent(context, MPlayer.class);
                    intentn.putExtra("CMD", MPlayer.CMD_PLAY_PAUSE);
                    context.startService(intentn);
                    break;
                }

            }
        }
    }
}
