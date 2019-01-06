package com.gmail.parusovvadim.audioplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

class SettingApp {

    private final static String SAVED_MUSIC_PATH = "music_path";

    // корневая папка
    private SharedPreferences m_setting;
    private Context m_context;
    private String m_pathMusicFiles;
    private String m_defaultPath = "/Music";
    private String m_storageDirectory;

    SettingApp(Context context) {
        m_context = context;
        m_storageDirectory = Environment.getExternalStorageDirectory().getPath();
    }

    String GetTrimmPath(String path)
    {
        return path.replace(m_storageDirectory,"");
    }

    String GetStorageDirectory()
    {
        return m_storageDirectory;
    }

    void SetMusicPath(String path) {
        m_pathMusicFiles = m_pathMusicFiles.replace(m_storageDirectory, "");
        m_pathMusicFiles = path;
    }

    String GetAbsolutePath()
    {
        if(m_pathMusicFiles.isEmpty())
        {
            m_pathMusicFiles = m_defaultPath;
        }

        return m_storageDirectory + m_pathMusicFiles;
    }


    String GetMusicPath() {
        return m_pathMusicFiles;
    }

    void SaveSetting() {
        m_setting = m_context.getSharedPreferences("Setting", MODE_PRIVATE);
        SharedPreferences.Editor ed = m_setting.edit();
        ed.putString(SAVED_MUSIC_PATH, m_pathMusicFiles);
        ed.apply();
    }

    void LoadSetting() {

        m_setting = m_context.getSharedPreferences("Setting", MODE_PRIVATE);
        String savedText = m_setting.getString(SAVED_MUSIC_PATH, "");
        m_pathMusicFiles = m_defaultPath;

        if (savedText.isEmpty())
            return;

        File file = new File(savedText);

        // если это папка
        if (file.isDirectory())
            m_pathMusicFiles = GetTrimmPath(savedText);

    }

}
