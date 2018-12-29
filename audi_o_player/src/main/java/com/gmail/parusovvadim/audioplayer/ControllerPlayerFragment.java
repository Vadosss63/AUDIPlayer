package com.gmail.parusovvadim.audioplayer;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ControllerPlayerFragment extends Fragment {
    private View m_view = null;
    // время воспроизведения
    private TextView m_playTime = null;

    private Handler m_handler = null;
    private Runnable m_runnable = null;
    private Button m_play_pauseButton = null;
    private boolean m_isReceiveTime = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (m_view == null) {
            m_view = inflater.inflate(R.layout.fragment_controller, null);
            CreateButtons();
            CreateTime();
        }
        return m_view;
    }

    public void SetTime(int msec) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String timeString = sdf.format(new Date(msec));
        m_playTime.setText(timeString);
        m_play_pauseButton.setSelected(true);
        m_isReceiveTime = true;
    }

    private void CreateButtons() {
        m_playTime = m_view.findViewById(R.id.PlayTime);

        Button previousButton = m_view.findViewById(R.id.previousButton);
        previousButton.setOnClickListener((View v) -> {
            Intent intent = new Intent(getActivity(), MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_PREVIOUS);
            getActivity().startService(intent);
        });

        m_play_pauseButton = m_view.findViewById(R.id.play_pause_Button);
        m_play_pauseButton.setOnClickListener((View v) -> {
            Intent intent = new Intent(getActivity(), MPlayer.class);
            if (m_play_pauseButton.isSelected()) {
                intent.putExtra("CMD", MPlayer.CMD_PAUSE);
                m_play_pauseButton.setSelected(false);
            } else {
                intent.putExtra("CMD", MPlayer.CMD_PLAY);
                m_play_pauseButton.setSelected(true);
            }
            getActivity().startService(intent);
        });

        Button nextButton = m_view.findViewById(R.id.nextButton);
        nextButton.setOnClickListener((
                View v) ->

        {
            Intent intent = new Intent(getActivity(), MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_NEXT);
            getActivity().startService(intent);
        });
    }

    private void CreateTime() {
        m_runnable = () -> {

            if(m_isReceiveTime)
            {
                m_isReceiveTime = false;
            }
            else
            {
                m_play_pauseButton.setSelected(false);
            }
            m_handler.postDelayed(m_runnable, 1200);
        };

        m_handler = new Handler();
        m_handler.post(m_runnable);
    }

}
