package com.gmail.parusovvadim.audioplayer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderByteMainHeader;
import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderFolders;
import com.gmail.parusovvadim.audioplayer.encoder_uart.EncoderMainHeader;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    final static String BROADCAST_ACTION = "com.gmail.parusovvadim.audioplayer";
    static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 1;

    final int MENU_CHANGE_DISC = 1;
    final int MENU_SYNCHRONIZATION = 3;
    final int MENU_SEND_FOLDERS = 4;
    final int MENU_SEND_TRACKS = 5;
    final int MENU_SETTING = 6;
    final int MENU_RESET_UART = 7;
    final int MENU_EXIT = 8;

    static final public int CMD_EXIT = -1;


    private ControllerPlayerFragment m_controllerPlayerFragment = null;

    // ресивер для приема данных от сервиса
    private BroadcastReceiver m_broadcastReceiver = null;

    // дериктория для воспроизведения
    private MusicFiles m_musicFiles = null;
    // Текущая деректория показа
    private NodeDirectory m_currentDirectory = null;
    // Текущий выбранный трек
    private NodeDirectory m_currentTrack = null;

    private Folder m_backFolder = new Folder("вверх");

    private TextView m_pathTextView;

    private SettingApp m_settingApp;

    // адаптер
    private ArrayAdapter<NodeDirectory> m_adapterPlayList = null;

    private ListView m_mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        m_controllerPlayerFragment = new ControllerPlayerFragment();
        ChangeStateController();
        Log.d("MyLogs", "onCreate");

    }

    private void CreateBroadCast() {
        // создаем BroadcastReceiver
        m_broadcastReceiver = new BroadcastReceiver() {
            // действия при получении сообщений
            public void onReceive(Context context, Intent intent) {
                int task = intent.getIntExtra(getString(R.string.CMD), 0);

                if (task == MPlayer.CMD_SEND_TIME) {

                    int folder = intent.getIntExtra("folder", 0);
                    int track = intent.getIntExtra("track", 0);
                    int time = intent.getIntExtra("time", 0);

                    NodeDirectory trackNode = m_musicFiles.GetTrack(folder, track);
                    if (trackNode != null) {
                        if (m_currentTrack != trackNode) {
                            m_currentTrack = trackNode;
                            m_adapterPlayList.notifyDataSetChanged();
                            ScrollToSelectTrack();
                        }

                        if (m_controllerPlayerFragment != null) {
                            m_controllerPlayerFragment.SetTime(time);
                        }
                    }
                }

                if (task == CMD_EXIT) {
                    ExitApp();
                }

            }
        };
        // создаем фильтр для BroadcastReceiver
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION);
        // регистрируем (включаем) BroadcastReceiver
        registerReceiver(m_broadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // дерегистрируем (выключаем) BroadcastReceiver
        if (m_broadcastReceiver != null) unregisterReceiver(m_broadcastReceiver);

        Log.d("MyLogs", "onDestroy");
    }

    @Override
    protected void onResume() {
        super.onResume();

        int permissionStatus = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
        }

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {

            if (m_currentTrack == null) {
                m_pathTextView = findViewById(R.id.pathShow);
                m_mainView = findViewById(R.id.playList);
                m_settingApp = new SettingApp(this);

                LoudSettings();
                CreateBroadCast();
                ShowCurrentDir();
            } else {
                ScrollToSelectTrack();
            }
            Log.d("MyLogs", "onResume");
        }

    }

    private void StartUART() {
        ChangeDisk();
        SendInfoFoldersToComPort();
        SendInfoTracksToComPort();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_CHANGE_DISC, 0, "Сменить диск");
        menu.add(0, MENU_SYNCHRONIZATION, 0, "Синхронизировать");
        menu.add(0, MENU_SEND_FOLDERS, 0, "Отправить папки");
        menu.add(0, MENU_SYNCHRONIZATION, 0, "Отправить треки");
        menu.add(0, MENU_RESET_UART, 0, "Reset OTG");
        menu.add(1, MENU_SETTING, 0, "Настройки");
        menu.add(1, MENU_EXIT, 0, "Выход");
//        menu.add(1, 2, 0, "Next");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
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
            case MENU_RESET_UART: {
                Intent intent = new Intent(this, UARTService.class);
                stopService(intent);
                startService(intent);
            }
            break;

            case MENU_SETTING: {
                Intent intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, 1);
                break;
            }
            case MENU_EXIT:
                ExitApp();
                break;
            case 2:
            {

                Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                KeyEvent downKeyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
                downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downKeyEvent);
                sendBroadcast(downIntent);


                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                        KeyEvent upKeyEvent = new KeyEvent( KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT);
                        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upKeyEvent);
                        sendBroadcast(upIntent);
                    }
                }, 100);

            }
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ExitApp() {
        Intent intentMP = new Intent(this, MPlayer.class);
        stopService(intentMP);
        Intent intentUART = new Intent(this, UARTService.class);
        stopService(intentUART);
        finish();
        System.exit(0);
    }

    private void CreateAdapter() {

        if (m_musicFiles.GetFolders().isEmpty()) {
            if (m_adapterPlayList != null)
                m_adapterPlayList.clear();
            return;
        }
        m_currentDirectory = m_musicFiles.GetFolders().get(0);

        m_adapterPlayList = new ArrayAdapter<NodeDirectory>(this, R.layout.music_track_item, m_musicFiles.GetAllFiles(1)) {
            @SuppressLint("InflateParams")
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {

                if (getItem(position).IsFolder()) {
                    if (getItem(position).IsFolderUp()) {
                        convertView = getLayoutInflater().inflate(R.layout.folder_up_item, null);
                    } else {
                        convertView = getLayoutInflater().inflate(R.layout.folder_item, null);
                        TextView titleFolder = convertView.findViewById(R.id.titleFolder);
                        titleFolder.setText(getItem(position).GetName());
                    }
                } else {

                    convertView = getLayoutInflater().inflate(R.layout.music_track_item, null);

                    TextView trackLabel = convertView.findViewById(R.id.titleTrack);

                    ImageView imageView = convertView.findViewById(R.id.TrackSelected);

                    String title = getItem(position).GetName().replace("_", " ");
                    title = title.replace(".mp3", "");
                    trackLabel.setText(title);

                    if (m_currentTrack == getItem(position)) {
                        imageView.setSelected(true);
                        trackLabel.setSelected(true);
                    } else {
                        imageView.setSelected(false);
                        trackLabel.setSelected(false);
                    }
                }
                return convertView;
            }
        };

        m_mainView.setAdapter(m_adapterPlayList);
        m_mainView.setOnItemClickListener(this);
        ScrollToSelectTrack();
    }

    private void OpenDirectory() {
        m_adapterPlayList.clear();
        Vector<NodeDirectory> files = new Vector<>();
        NodeDirectory back = m_musicFiles.GetParentFolder(m_currentDirectory);
        if (back != null) {

            m_backFolder.setPath(back.GetPathDir());
            m_backFolder.setNumber(back.GetNumber());
            m_backFolder.setParentNumber(back.GetParentNumber());
            m_backFolder.setIsFolderUp(true);
            files.add(m_backFolder);

        }
        files.addAll(m_musicFiles.GetAllFiles(m_currentDirectory.GetNumber()));
        m_adapterPlayList.addAll(files);
    }

    private void ScrollToSelectTrack() {
        int scrollPos = m_adapterPlayList.getPosition(m_currentTrack);
        m_mainView.smoothScrollToPosition(scrollPos);
        m_adapterPlayList.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {   // обработка нажатий на элементах списка
        NodeDirectory nodeDirectory = (NodeDirectory) (parent.getItemAtPosition(position));
        // пока у нас есть треки мы их воспроизводим
        if (nodeDirectory.IsFolder()) {

            if (nodeDirectory.IsFolderUp()) {
                m_currentDirectory = m_musicFiles.GetFolders().get(nodeDirectory.GetNumber() - 1);
            } else {
                m_currentDirectory = nodeDirectory;
            }

            ShowCurrentDir();
            OpenDirectory();
        } else {
            m_currentTrack = nodeDirectory;
            SelectedTrack();
            m_adapterPlayList.notifyDataSetChanged();
        }
    }

    private void ShowCurrentDir() {
        m_pathTextView.setText(m_settingApp.GetMusicPath());
    }

    public void ChangeRoot() {

        m_musicFiles = new MusicFiles(m_settingApp.GetAbsolutePath());

        Intent intent = new Intent(this, MPlayer.class);
        intent.putExtra(getString(R.string.CMD), MPlayer.CMD_CHANGE_ROOT);
        startService(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (data == null) {
            return;
        }

        if (resultCode == RESULT_OK) {
            LoudSettings();
        }
    }

    private void LoudSettings() {
        m_settingApp.LoadSetting();
        ChangeRoot();
        CreateAdapter();
    }

    // Отправка выбранного трека
    private void SelectedTrack() {
        Intent intent = new Intent(this, MPlayer.class);
        intent.putExtra(getString(R.string.CMD), MPlayer.CMD_SELECT_TRACK);
        intent.putExtra("folder", m_currentTrack.GetParentNumber());
        intent.putExtra("track", m_currentTrack.GetNumber() + 1);
        startService(intent);
    }

    private void ChangeDisk() {
        Intent intent = new Intent(this, UARTService.class);
        intent.putExtra(getString(R.string.CMD), UARTService.CMD_CHANGE_DISC);
        startService(intent);
    }

    private void SendInfoTracksToComPort() {

        Vector<NodeDirectory> folders = m_musicFiles.GetFolders();
        EncoderByteMainHeader.EncoderListTracks encoderListTracks = new EncoderByteMainHeader.EncoderListTracks();

        for (NodeDirectory folder : folders) {
            encoderListTracks.AddHeader(folder.GetNumber());
            Vector<NodeDirectory> tracks = m_musicFiles.GetTracks(folder.GetNumber());
            for (NodeDirectory track : tracks) {
                encoderListTracks.AddTrackNumber(track.GetNumber() + 1);
                encoderListTracks.AddName(track.GetName());
            }

            encoderListTracks.AddEnd();

            // Добавляем заголовок
            EncoderMainHeader headerData = new EncoderMainHeader(encoderListTracks.GetVectorByte());
            headerData.AddMainHeader((byte) 0x03);

            Intent intent = new Intent(this, UARTService.class);
            intent.putExtra(getString(R.string.CMD), UARTService.CMD_SEND_DATA);
            intent.putExtra("Data", headerData.GetDataByte());
            startService(intent);
        }
    }

    private void SendInfoFoldersToComPort() {

        Vector<NodeDirectory> folders = m_musicFiles.GetFolders();
        EncoderFolders encoderFolders = new EncoderFolders();
        encoderFolders.AddHeader();

        for (NodeDirectory folder : folders) {
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
        intent.putExtra(getString(R.string.CMD), UARTService.CMD_SEND_DATA);
        intent.putExtra(getString(R.string.CMD_data), headerData.GetDataByte());
        startService(intent);

    }

    public void ChangeStateController() {
        FragmentTransaction m_fragmentTransaction = getFragmentManager().beginTransaction();
        m_fragmentTransaction.replace(R.id.mainFragment, m_controllerPlayerFragment);
        m_fragmentTransaction.commit();
    }
}
