package com.example.vadosss63.playeraudi;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
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

public class SettingActivity extends Activity implements AdapterView.OnItemClickListener
{
    private ChangeFolderFragment m_changeFolderFragment = null;
    private List<String> m_pathList = null;
    private ListView m_pathView = null;

    private String root = "/"; // символ для корневого элемента
    private TextView mPathTextView;
    private String m_pathMusicFiles;

    final public static String SAVED_MUSIC_PATH = "music_path";
    SharedPreferences sPref;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);


        m_changeFolderFragment = new ChangeFolderFragment();
        m_pathView = findViewById(R.id.pathList);
        m_pathView.setOnItemClickListener(this);

        mPathTextView = (TextView) findViewById(R.id.PathMusicFiles);

        ChangeStateSelectRoot();
        root = Environment.getExternalStorageDirectory().getPath();
        LoadSetting();
        getDir(m_pathMusicFiles); // выводим список файлов и папок в корневой папке системы

    }

    public void ChangeStateSelectRoot()
    {
        FragmentTransaction m_fragmentTransaction = getFragmentManager().beginTransaction();
        m_fragmentTransaction.replace(R.id.settingFragment, m_changeFolderFragment);
        m_fragmentTransaction.commit();
    }

    private void getDir(String dirPath)
    {
        mPathTextView.setText("Путь: " + dirPath); // где мы сейчас
        m_pathMusicFiles = dirPath;
        List<String> itemList = new ArrayList<>();
        m_pathList = new ArrayList<>();
        File file = new File(dirPath);
        File[] filesArray = file.listFiles(); // получаем список файлов

        // если мы не в корневой папке
        if(!dirPath.equals(root))
        {
            itemList.add(root);
            m_pathList.add(root);
            itemList.add("../");
            m_pathList.add(file.getParent());
        }

        Arrays.sort(filesArray, fileComparator);

        // формируем список папок и файлов для передачи адаптеру
        for(File aFilesArray : filesArray)
        {
            file = aFilesArray;
            String filename = file.getName();

            // Работаем только с доступными папками и файлами
            if(!file.isHidden() && file.canRead()) if(file.isDirectory())
            {
                m_pathList.add(file.getPath());
                itemList.add(file.getName() + "/");
            } else if(filename.endsWith(".mp3") || filename.endsWith(".wma") || filename.endsWith(".ogg"))
            {
                m_pathList.add(file.getPath());
                itemList.add(file.getName());
            }
        }

        // Можно выводить на экран список
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, itemList);
        m_pathView.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        // обработка нажатий на элементах списка
        File file = new File(m_pathList.get(position));
        // если это папка
        if(file.isDirectory())
        {
            if(file.canRead()) // если она доступна для просмотра, то заходим в неё
            {
                getDir(m_pathList.get(position));
            } else
            { // если папка закрыта, то сообщаем об этом
                new AlertDialog.Builder(this).setIcon(R.mipmap.ic_launcher).setTitle("[" + file.getName() + "] папка не доступна!").setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                    }
                }).show();
            }
        }
    }

    // Компоратор для сортировки дерикторий музыкальных треков
    private Comparator<? super File> fileComparator = (Comparator<File>) (file1, file2)->{

        if(file1.isDirectory() && !file2.isDirectory()) return -1;

        if(file2.isDirectory() && !file1.isDirectory()) return 1;

        String pathLowerCaseFile1 = file1.getName().toLowerCase();
        String pathLowerCaseFile2 = file2.getName().toLowerCase();
        return String.valueOf(pathLowerCaseFile1).compareTo(pathLowerCaseFile2);
    };

    void SaveSetting()
    {
        sPref = getSharedPreferences("Setting", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(SAVED_MUSIC_PATH, m_pathMusicFiles);
        ed.commit();
    }

    void LoadSetting()
    {
        sPref = getSharedPreferences("Setting", MODE_PRIVATE);
        String savedText = sPref.getString(SAVED_MUSIC_PATH, "");

        File file = new File(savedText);
        // если это папка
        if(file.isDirectory())
        {
            m_pathMusicFiles = savedText;
        } else
        {
            m_pathMusicFiles = root;
        }
    }

}
