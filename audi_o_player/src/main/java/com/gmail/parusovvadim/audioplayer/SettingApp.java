package com.gmail.parusovvadim.audioplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

import static android.content.Context.MODE_PRIVATE;

public class SettingApp {

    private final static String SAVED_MUSIC_PATH = "music_path";

    // корневая папка
    private SharedPreferences m_setting;
    private Context m_context;
    private String m_pathMusicFiles;
    private String m_defaultPath = "/Music";
    private String m_storageDirectory;

    public SettingApp(Context context) {
        m_context = context;
        m_storageDirectory = Environment.getExternalStorageDirectory().getPath();
    }

    public String GetTrimmPath(String path)
    {
        return path.replace(m_storageDirectory,"");
    }

    public String GetStorageDirectory()
    {
        return m_storageDirectory;
    }

    public void SetMusicPath(String path) {
        m_pathMusicFiles = m_pathMusicFiles.replace(m_storageDirectory, "");
        m_pathMusicFiles = path;
    }

    public String GetAbsolutePath()
    {
        if(m_pathMusicFiles.isEmpty())
        {
            m_pathMusicFiles = m_defaultPath;
        }

        return m_storageDirectory + m_pathMusicFiles;
    }


    public String GetMusicPath() {
        return m_pathMusicFiles;
    }

    public void SaveSetting() {
        m_setting = m_context.getSharedPreferences("Setting", MODE_PRIVATE);
        SharedPreferences.Editor ed = m_setting.edit();
        ed.putString(SAVED_MUSIC_PATH, m_pathMusicFiles);
        ed.apply();
    }

    public void LoadSetting() {

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
