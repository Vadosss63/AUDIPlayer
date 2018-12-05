package com.example.vadosss63.playeraudi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;

import com.example.vadosss63.playeraudi.encoder_uart.EncoderByteMainHeader;
import com.example.vadosss63.playeraudi.encoder_uart.EncoderFolders;
import com.example.vadosss63.playeraudi.encoder_uart.EncoderMainHeader;

import java.util.Vector;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener
{
    final static String BROADCAST_ACTION = "com.example.vadosss63.playeraudi";
    static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 1;

    final int MENU_CHANGE_DISC = 1;
    final int MENU_SELECT_ROOT_FOLDER = 2;
    final int MENU_SYNCHRONIZATION = 3;
    final int MENU_SEND_FOLDERS = 4;
    final int MENU_SEND_TRACKS = 5;
    final int MENU_EXIT = 6;

    private String m_rootDirectory = "/Music";

    private FragmentTransaction m_fragmentTransaction;
    private ControllerPlayerFragment m_controllerPlayerFragment = null;
    private ChangeFolderFragment m_changeFolderFragment = null;

    // ресивер для приема данных от сервиса
    private BroadcastReceiver m_broadcastReceiver;

    // дериктория для воспроизведения
    private MusicFiles m_musicFiles;
    // Текущая деректория показа
    private NodeDirectory m_currentDirectory;
    // Текущий выбранный трек
    private NodeDirectory m_currentTrack;

    // адаптер
    private ArrayAdapter<NodeDirectory> m_adapterPlayList;

    private ListView m_mainView;

    @SuppressLint ("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permissionStatus != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
        }

        if(permissionStatus == PackageManager.PERMISSION_GRANTED)
        {
            m_controllerPlayerFragment = new ControllerPlayerFragment();
            ChangeStateController();
        }

        m_changeFolderFragment = new ChangeFolderFragment();

        CreateMusicFiles();
        m_mainView = findViewById(R.id.playList);
        CreateAdapter();
        // создаем BroadcastReceiver
        m_broadcastReceiver = new BroadcastReceiver()
        {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent)
            {
                int task = intent.getIntExtra("CMD", 0);
                if(task == MPlayer.CMD_SEND_TIME)
                {
                    int folder = intent.getIntExtra("folder", 0);
                    int track = intent.getIntExtra("track", 0);
                    int time = intent.getIntExtra("time", 0);
                    NodeDirectory trackNode = m_musicFiles.GetTrack(folder, track);
                    if(trackNode != null)
                    {
                        if(m_currentTrack != trackNode)
                        {
                            m_currentTrack = trackNode;
                            m_adapterPlayList.notifyDataSetChanged();
                            ScrollToSelectTrack();
                        }

                        if(m_controllerPlayerFragment != null)
                        {
                            m_controllerPlayerFragment.SetTime(time);
                        }
                    }
                }
            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(m_broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        // дерегистрируем (выключаем) BroadcastReceiver
        unregisterReceiver(m_broadcastReceiver);
    }

    private void StartUART()
    {
        SendInfoFoldersToComPort();
        SendInfoTracksToComPort();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_CHANGE_DISC, 0, "Сменить диск");
        menu.add(0, MENU_SELECT_ROOT_FOLDER, 0, "Выбрать папку");
        menu.add(0, MENU_SYNCHRONIZATION, 0, "Синхронизировать");
        menu.add(0, MENU_SEND_FOLDERS, 0, "Отправить папки");
        menu.add(0, MENU_SYNCHRONIZATION, 0, "Отправить треки");
        menu.add(0, MENU_EXIT, 0, "Выход");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case MENU_SYNCHRONIZATION:
                StartUART();
                break;
            case MENU_SEND_FOLDERS:
                SendInfoFoldersToComPort();
                break;
            case MENU_SEND_TRACKS:
                SendInfoTracksToComPort();
                break;
            case MENU_CHANGE_DISC:

                ChangeDisk();
                break;
            case MENU_SELECT_ROOT_FOLDER:
                SelectRootFolder();
                break;

            case MENU_EXIT:
                Intent intentMP = new Intent(this, MPlayer.class);
                stopService(intentMP);
                Intent intentUART = new Intent(this, UARTService.class);
                stopService(intentUART);
                System.exit(0);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void SelectRootFolder()
    {
        ChangeStateSelectRoot();
//        String m_dirRoot = Environment.getExternalStorageDirectory().getPath();
//        m_musicFiles = new MusicFiles(m_dirRoot);
    }

    private void CreateAdapter()
    {
        m_adapterPlayList = new ArrayAdapter<NodeDirectory>(this, R.layout.music_track_item, m_musicFiles.GetAllFiles(1))
        {
            @SuppressLint ("InflateParams")
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent)
            {
                if(convertView == null)
                    convertView = getLayoutInflater().inflate(R.layout.music_track_item, null);

                TextView trackLabel = convertView.findViewById(R.id.textViewContent);
                ImageView folderImage = convertView.findViewById(R.id.folderImage);
                ImageView folderImageBack = convertView.findViewById(R.id.folderImageBack);
                folderImage.setVisibility(View.GONE);
                folderImageBack.setVisibility(View.GONE);
                trackLabel.setText(getItem(position).GetName());


                if(getItem(position).IsFolder())
                {
                    if(trackLabel.getText() == "вверх") folderImageBack.setVisibility(View.VISIBLE);
                    else folderImage.setVisibility(View.VISIBLE);
                }

                ImageView imageView = convertView.findViewById(R.id.TrackSelected);
                TextView trackTime = convertView.findViewById(R.id.TrackTime);
                if(m_currentTrack == getItem(position))
                {
                    imageView.setSelected(true);
                    trackLabel.setSelected(true);
                } else
                {
                    imageView.setSelected(false);
                    trackLabel.setSelected(false);
                    trackTime.setSelected(false);
                    trackTime.setText("");
                }
                return convertView;
            }
        };

        m_mainView.setAdapter(m_adapterPlayList);
        m_mainView.setOnItemClickListener(this);
        ScrollToSelectTrack();
    }

    private void CreateMusicFiles()
    {
        String m_dirRoot = Environment.getExternalStorageDirectory().getPath();
        String dirPath = m_dirRoot + m_rootDirectory;
        m_musicFiles = new MusicFiles(dirPath);
    }

    private void OpenDirectory()
    {
        m_adapterPlayList.clear();
        Vector<NodeDirectory> files = new Vector<>();
        NodeDirectory back = m_musicFiles.GetParentFolder(m_currentDirectory);
        if(back != null)
        {
            back.SetName("вверх");
            files.add(back);
        }
        files.addAll(m_musicFiles.GetAllFiles(m_currentDirectory.GetNumber()));
        m_adapterPlayList.addAll(files);
    }

    private void BackToParentFolder(NodeDirectory trackNode)
    {
        NodeDirectory nodeDirectory = m_musicFiles.GetParentFolder(trackNode);
        // Преходим в папку
        if(nodeDirectory == null) return;

        if(nodeDirectory == m_currentDirectory) return;

        m_currentDirectory = nodeDirectory;
        OpenDirectory();
    }

    private void ScrollToSelectTrack()
    {
        int scrollPos = m_adapterPlayList.getPosition(m_currentTrack);
        m_mainView.smoothScrollToPosition(scrollPos);
        m_adapterPlayList.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {   // обработка нажатий на элементах списка
        NodeDirectory nodeDirectory = (NodeDirectory) (parent.getItemAtPosition(position));
        // пока у нас есть треки мы их воспроизводим
        if(nodeDirectory.IsFolder())
        {
            m_currentDirectory = nodeDirectory;
            OpenDirectory();
        } else
        {
            m_currentTrack = nodeDirectory;
            SelectedTrack();
            m_adapterPlayList.notifyDataSetChanged();
        }
    }

    public void ChangeRoot()
    {
        m_rootDirectory = m_currentDirectory.GetPathDir();
        CreateMusicFiles();
        Intent intent = new Intent(this, MPlayer.class);
        intent.putExtra("CMD", MPlayer.CMD_CHANGE_ROOT);
        intent.putExtra("root", m_rootDirectory);
        startService(intent);
        ChangeStateController();

    }

    public void CancelChangeRoot()
    {
//        CreateMusicFiles();
        ChangeStateController();
    }

    // Отправка выбранного трека
    private void SelectedTrack()
    {
        Intent intent = new Intent(this, MPlayer.class);
        intent.putExtra("CMD", MPlayer.CMD_SELECT_TRACK);
        intent.putExtra("folder", m_currentTrack.GetParentNumber());
        intent.putExtra("track", m_currentTrack.GetNumber() + 1);
        startService(intent);
    }

    private void ChangeDisk()
    {
        Intent intent = new Intent(this, UARTService.class);
        intent.putExtra("CMD", UARTService.CMD_CHANGE_DISC);
        startService(intent);
    }

    private void SendInfoTracksToComPort()
    {

        Vector<NodeDirectory> folders = m_musicFiles.GetFolders();
        EncoderByteMainHeader.EncoderListTracks encoderListTracks = new EncoderByteMainHeader.EncoderListTracks();

        for(NodeDirectory folder : folders)
        {
            encoderListTracks.AddHeader(folder.GetNumber());
            Vector<NodeDirectory> tracks = m_musicFiles.GetTracks(folder.GetNumber());
            for(NodeDirectory track : tracks)
            {
                /// TODO уточнить
                encoderListTracks.AddTrackNumber(track.GetNumber() + 1);
                encoderListTracks.AddName(track.GetName());
            }

            encoderListTracks.AddEnd();

            // Добавляем заголовок
            EncoderMainHeader headerData = new EncoderMainHeader(encoderListTracks.GetVectorByte());
            headerData.AddMainHeader((byte) 0x03);

            Intent intent = new Intent(this, UARTService.class);
            intent.putExtra("CMD", UARTService.CMD_SEND_DATA);
            intent.putExtra("Data", headerData.GetDataByte());
            startService(intent);
        }
    }

    private void SendInfoFoldersToComPort()
    {
        Vector<NodeDirectory> folders = m_musicFiles.GetFolders();
        EncoderFolders encoderFolders = new EncoderFolders();
        encoderFolders.AddHeader();
        for(NodeDirectory folder : folders)
        {
            encoderFolders.AddName(folder.GetName());
            encoderFolders.AddNumber(folder.GetNumber());
            encoderFolders.AddNumberTracks(folder.GetNumberTracks());
            encoderFolders.AddParentNumber(folder.GetParentNumber());
        }
        encoderFolders.AddEnd();
        // Добавляем заголовок
        EncoderMainHeader headerData = new EncoderMainHeader(encoderFolders.GetVectorByte());
        headerData.AddMainHeader((byte) 0x02);

        Intent intent = new Intent(this, UARTService.class);
        intent.putExtra("CMD", UARTService.CMD_SEND_DATA);
        intent.putExtra("Data", headerData.GetDataByte());
        startService(intent);

    }

    public void ChangeStateSelectRoot()
    {
        m_fragmentTransaction = getFragmentManager().beginTransaction();
        m_fragmentTransaction.replace(R.id.mainFragment, m_changeFolderFragment);
        m_fragmentTransaction.commit();
    }

    public void ChangeStateController()
    {
        m_fragmentTransaction = getFragmentManager().beginTransaction();
        m_fragmentTransaction.replace(R.id.mainFragment, m_controllerPlayerFragment);
        m_fragmentTransaction.commit();
    }
}
