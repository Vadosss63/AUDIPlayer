package com.example.vadosss63.playeraudi;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

@SuppressLint ("ValidFragment")
class ChangeFolderFragment extends Fragment
{
    private View m_view = null;

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

    private void CreateButtons()
    {
        Button previousButton = m_view.findViewById(R.id.okButton);
        previousButton.setOnClickListener((View v)->{
            ((MainActivity) getActivity()).ChangeRoot();
        });

        Button playButton = m_view.findViewById(R.id.cancelButton);
        playButton.setOnClickListener((View v)->{
            ((MainActivity) getActivity()).CancelChangeRoot();
        });

    }
}
