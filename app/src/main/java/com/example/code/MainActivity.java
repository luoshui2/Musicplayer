package com.example.code;

import static java.lang.Math.min;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE ,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private ContentResolver mContentResolver;
    private ListView mPlaylist;
    private MediaCursorAdapter mCursorAdapter;
    private final String SELECTION = MediaStore.Audio.Media.IS_MUSIC + " = ? " + " AND " + MediaStore.Audio.Media.MIME_TYPE + " LIKE ? ";
    private final String[] SELECTION_ARGS = {Integer.toString(1), "audio/mpeg"};

    private BottomNavigationView navigationView;
    private TextView buttom_title;
    private TextView buttom_arist;
    private ImageView buttom_img;
    private ImageView buttom_pause;
    private MediaPlayer mMediaPlayer = null;
    private MusicService mservice;
    private boolean mbound;
    //区分message的类别
    public static final int UPDATE_PROGRESS = 1;
    private ProgressBar progressBar;
    //service要发送的广播信息
    public static final String ACTION_MUSIC_START =  "com.glriverside.xgqin.ggmusic.ACTION_MUSIC_START";
    public static final String ACTION_MUSIC_STOP =  "com.glriverside.xgqin.ggmusic.ACTION_MUSIC_STOP";
    private MusicReceiver musicReceiver;
    //MusicReceiver 类⽤于监听⾳乐的广播
    public class MusicReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if(mservice != null){
                if(progressBar != null){
                    //启动子线程查询音乐播放进度
                    progressBar.setMax(mservice.getDuration());
                    new Thread(new MusicProgressRunnable()).start();
                }
            }
        }
    }
    //处理传递过来的信息
    private Handler mhandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what == UPDATE_PROGRESS){
                int position = msg.arg1;
                if(progressBar != null){
                    Log.d("12gg",""+position);
                    progressBar.setProgress(position);
                }
            }
        }
    };
    //子线程来查询歌曲的进度并且发送信息给handler进行处理
    private class MusicProgressRunnable implements Runnable{
        @Override
        public void run() {
            boolean flag = true;
            while (flag){
                if(mservice != null){
                    int position = mservice.getCurrentPosition();
                    Message message = new Message();
                    message.what = UPDATE_PROGRESS;
                    message.arg1 = position;
                    mhandler.sendMessage(message);
                }
                flag = mservice.isPlaying();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    //绑定服务的connect
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicService.MusicServiceBinder binder = (MusicService.MusicServiceBinder) iBinder;
            mservice = binder.getService();
            mbound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mservice = null;
            mbound = false;
        }
    };
    //listview的点击事件
    private  ListView.OnItemClickListener itemClickListener = new ListView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            Cursor cursor = mCursorAdapter.getCursor();//从适配器获取游标
            if(cursor != null && cursor.moveToPosition(i)){
                //通过游标移动到指定位置拿到数据
                int index_title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);//歌曲名字
                int index_arist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);//歌曲作者
                int index_album = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);//歌曲专辑图片
                int index_data = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);//歌曲的位置
                String title = cursor.getString(index_title);
                String arist = cursor.getString(index_arist);
                Long album = cursor.getLong(index_album);
                String data = cursor.getString(index_data);

                //加载数据
                navigationView.setVisibility(View.VISIBLE);
                //歌曲名字
                if(buttom_title != null){
                    buttom_title.setText(title);
                    Log.d("abc","title = "+title);
                }
                //歌曲作者
                if(buttom_arist != null){
                    buttom_arist.setText(arist);
                }
                //歌曲专辑图片

                Uri uri_album = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,album);//构造uri
                Cursor cursor_album = mContentResolver.query(uri_album,null,null,null,null);//查询album
                if(cursor_album != null && cursor_album.getCount() > 0){
                    cursor_album.moveToNext();
                    int index_am = cursor_album.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                    String am = cursor_album.getString(index_am);
                    if(am != null){
                        Glide.with(MainActivity.this).load(am).into(buttom_img);
                    }else{
                        //假如数据库里面没有图片位置则获取MP3的内嵌图片
                        Uri uri = Uri.parse(data);//歌曲的路径
                        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                        retriever.setDataSource(MainActivity.this,uri);
                        byte[] img = retriever.getEmbeddedPicture();
                        if(img != null){
                            Log.d("abcde","img");
                            Glide.with(MainActivity.this).load(img).into(buttom_img);
                        }else{
                            //没有内嵌图片，使用默认图片
                            buttom_img.setImageResource(R.drawable.image4);
                        }
                        try {
                            retriever.release();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    cursor_album.close();
                }

                //启动service播放音乐

                Intent music_service = new Intent(MainActivity.this,MusicService.class);
                music_service.putExtra("Foreground_path",data);
                music_service.putExtra("Foreground_title",title);
                music_service.putExtra("Foreground_arist",arist);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(music_service);
                    buttom_pause.setImageResource(R.drawable.ic_action_pause);
                }

            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //注册广播
        musicReceiver = new MusicReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_MUSIC_START);
        intentFilter.addAction(ACTION_MUSIC_STOP);
        registerReceiver(musicReceiver,intentFilter);

        //获取mediostore数据库进行音乐查询和适配器的设置
        mContentResolver = getContentResolver();
        mCursorAdapter = new MediaCursorAdapter(MainActivity.this);
        mPlaylist = findViewById(R.id.lists);
        mPlaylist.setAdapter(mCursorAdapter);
        mPlaylist.setOnItemClickListener(itemClickListener);

        //底部导航的加载
        navigationView = findViewById(R.id.nav);
        LayoutInflater.from(MainActivity.this).inflate(R.layout.bottom_media_toolba,navigationView,true);

        //获取底部导航的组件
        buttom_title = navigationView.findViewById(R.id.tv_bottom_title);
        buttom_arist = navigationView.findViewById(R.id.tv_bottom_artist);
        buttom_img = navigationView.findViewById(R.id.iv_thumbnail);
        buttom_pause = navigationView.findViewById((R.id.iv_play));
        progressBar = navigationView.findViewById(R.id.progress);

        //设置播放按钮的点击事件
        if(buttom_pause != null){
            buttom_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mservice != null){
                        boolean flag = mservice.isPlaying();
                        if(flag){
                            mservice.pause();
                            buttom_pause.setImageResource(R.drawable.ic_action_play);
                        }else{
                            buttom_pause.setImageResource(R.drawable.ic_action_pause);
                            mservice.play();
                        }
                    }
                }
            });
        }


        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) !=  PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){

            }else{
                requestPermissions(PERMISSIONS_STORAGE , REQUEST_EXTERNAL_STORAGE);
            }
        }
        init();


    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(MainActivity.this,MusicService.class);
        bindService(intent,connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(connection);
        mbound = false;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if(mservice != null){
            mservice.onDestroy();
        }
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //处理获取的权限
        if(requestCode == REQUEST_EXTERNAL_STORAGE){
            if(grantResults.length > 0 && grantResults[0] ==  PackageManager.PERMISSION_GRANTED){
                init();
            }
        }
    }

    public void init(){
        String select = MediaStore.Audio.Media.ARTIST + "!= null";
        Cursor contentValues = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,null,null,null,null );
        Log.d("abc","init");
        int i = contentValues.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
        Log.d("abc","init i = "+i);
        if(contentValues != null){
            Log.d("abc","number = "+contentValues.getCount());
            while(contentValues.moveToNext()){
                Log.d("abc","000");
                Log.d("abc","ss"+contentValues.getString(i));
            }
        }

        mCursorAdapter.swapCursor(contentValues);
        mCursorAdapter.notifyDataSetChanged();

    }
}
class MediaCursorAdapter extends CursorAdapter {

    private Context mContext;
    private LayoutInflater mLayoutInflater;

    public MediaCursorAdapter(Context context){
        super(context,null,0);
        this.mContext = context;
        mLayoutInflater = LayoutInflater.from(context);

    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
        View view = mLayoutInflater.inflate(R.layout.list_music,viewGroup,false);
        if(view != null){
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.tvTitle = view.findViewById(R.id.tv_title);
            viewHolder.tvArthor = view.findViewById(R.id.tv_arthor);
            viewHolder.tvOrder = view.findViewById(R.id.tv_order);
            viewHolder.divider = view.findViewById(R.id.tv_divide);
            view.setTag(viewHolder);
            return view;
        }
        return null;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder v = (ViewHolder)view.getTag();
        int titleidex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int arthoridex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        String title = cursor.getString(titleidex);
        String arthor = cursor.getString(arthoridex);

        if(arthor != "<unknown>"){
            Log.d("abcd","arthor = "+arthor);
        }
        int position = cursor.getPosition();
        if(v != null){
            v.tvTitle.setText(title);
            v.tvArthor.setText(arthor);
            v.tvOrder.setText(Integer.toString(position+1));
        }
    }

    public class ViewHolder {
        TextView tvTitle;
        TextView tvArthor;
        TextView tvOrder;
        View divider;
    }
}




