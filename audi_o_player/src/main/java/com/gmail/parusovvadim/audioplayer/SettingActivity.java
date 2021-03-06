package com.gmail.parusovvadim.audioplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SettingActivity extends Activity implements AdapterView.OnItemClickListener {

    private ChangeFolderFragment m_changeFolderFragment = null;
    private List<String> m_pathList = null;
    private ListView m_pathView = null;
    private SettingApp m_settingApp;
    private String m_currentDir;
    private TextView m_pathTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        m_settingApp = new SettingApp(this);

        m_changeFolderFragment = new ChangeFolderFragment();
        m_pathView = findViewById(R.id.pathList);
        m_pathView.setOnItemClickListener(this);
        m_pathTextView = (TextView) findViewById(R.id.PathMusicFiles);

        LoadSetting();
        getDir(m_settingApp.GetAbsolutePath()); // выводим список файлов и папок в корневой папке системы

    }

    @Override
    protected void onResume() {
        super.onResume();
        ChangeStateSelectRoot();
    }

    public void ChangeStateSelectRoot() {
        FragmentTransaction m_fragmentTransaction = getFragmentManager().beginTransaction();
        m_fragmentTransaction.replace(R.id.settingFragment, m_changeFolderFragment);
        m_fragmentTransaction.commit();
    }

    private void getDir(String dirPath) {
        m_pathTextView.setText("Путь: " + m_settingApp.GetTrimmPath(dirPath)); // где мы сейчас
        m_currentDir = dirPath;

        List<String> itemList = new ArrayList<>();
        m_pathList = new ArrayList<>();
        File file = new File(dirPath);

        if (file.exists()) {

            File[] filesArray = file.listFiles(); // получаем список файлов

            // если мы не в корневой папке
            if (!dirPath.equals(m_settingApp.GetStorageDirectory())) {
                itemList.add("../");
                m_pathList.add(file.getParent());
            }

            if (filesArray == null)
                return;

            Arrays.sort(filesArray, fileComparator);

            // формируем список папок и файлов для передачи адаптеру
            for (File aFilesArray : filesArray) {
                file = aFilesArray;
                String filename = file.getName();

                // Работаем только с доступными папками и файлами
                if (!file.isHidden() && file.canRead()) if (file.isDirectory()) {
                    m_pathList.add(file.getPath());
                    itemList.add(file.getName() + "/");
                } else if (filename.endsWith(".mp3") || filename.endsWith(".wma") || filename.endsWith(".ogg")) {
                    m_pathList.add(file.getPath());
                    itemList.add(file.getName());
                }
            }
        }else
        {
            getDir(m_settingApp.GetStorageDirectory());
            return;
        }
        // Можно выводить на экран список
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, itemList);
        m_pathView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // обработка нажатий на элементах списка
        File file = new File(m_pathList.get(position));
        // если это папка
        if (file.isDirectory()) {
            if (file.canRead()) // если она доступна для просмотра, то заходим в неё
            {
                getDir(m_pathList.get(position));
            } else { // если папка закрыта, то сообщаем об этом
                new AlertDialog.Builder(this).setIcon(R.mipmap.ic_launcher).setTitle("[" + file.getName() + "] папка не доступна!").setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            }
        }
    }

    // Компоратор для сортировки дерикторий музыкальных треков
    private Comparator<? super File> fileComparator = (Comparator<File>) (file1, file2) -> {

        if (file1.isDirectory() && !file2.isDirectory()) return -1;

        if (file2.isDirectory() && !file1.isDirectory()) return 1;

        String pathLowerCaseFile1 = file1.getName().toLowerCase();
        String pathLowerCaseFile2 = file2.getName().toLowerCase();
        return String.valueOf(pathLowerCaseFile1).compareTo(pathLowerCaseFile2);
    };

    void SaveSetting() {
        m_settingApp.SetMusicPath(m_currentDir);
        m_settingApp.SaveSetting();
    }

    void LoadSetting() {
        m_settingApp.LoadSetting();
        m_currentDir = m_settingApp.GetMusicPath();
    }

}
