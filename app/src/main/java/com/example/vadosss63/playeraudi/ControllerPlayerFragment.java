package com.example.vadosss63.playeraudi;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ControllerPlayerFragment extends Fragment
{
    private View m_view = null;
    // время воспроизведения
    private TextView m_playTime = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        if(m_view == null)
        {
            m_view = inflater.inflate(R.layout.fragment_controller, null);
            CreateButtons();
        }
        return m_view;
    }

    public void SetTime(int msec)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
        String timeString = sdf.format(new Date(msec));
        m_playTime.setText(timeString);
    }

    private void CreateButtons()
    {
        m_playTime = m_view.findViewById(R.id.PlayTime);

        Button previousButton = m_view.findViewById(R.id.previousButton);
        previousButton.setOnClickListener((View v)->{
            Intent intent = new Intent(getActivity(), MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_PREVIOUS);
            getActivity().startService(intent);
        });

        Button playButton = m_view.findViewById(R.id.playButton);
        playButton.setOnClickListener((View v)->{
            Intent intent = new Intent(getActivity(), MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_PLAY);
            getActivity().startService(intent);
        });

        Button pauseButton = m_view.findViewById(R.id.pauseButton);
        pauseButton.setOnClickListener((View v)->{
            Intent intent = new Intent(getActivity(), MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_PAUSE);
            getActivity().startService(intent);
        });

        Button nextButton = m_view.findViewById(R.id.nextButton);
        nextButton.setOnClickListener((View v)->{
            Intent intent = new Intent(getActivity(), MPlayer.class);
            intent.putExtra("CMD", MPlayer.CMD_NEXT);
            getActivity().startService(intent);
        });
    }

}
