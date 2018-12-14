package com.example.vadosss63.playeraudi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;


public class RemoteControlReceiver extends BroadcastReceiver {


    // Constructor is mandatory
    public RemoteControlReceiver() {
        super();
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
