package com.example.code;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class MusicService extends Service {

    private MediaPlayer mediaPlayer;
    private static final int Notification_id = 1001;
    private NotificationManager notificationManager;
    private final IBinder iBinder = new MusicServiceBinder();
    private static final String CHANNEL_ID = "Music channel";
    private int notification_id = 1;
    public MusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(mediaPlayer == null){
            mediaPlayer = new MediaPlayer();
        }
    }

    @Override
    public void onDestroy() {
        if(mediaPlayer != null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        super.onDestroy();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //播放歌曲
        String path = intent.getStringExtra("Foreground_path");//歌曲的路径
        String title = intent.getStringExtra("Foreground_title");
        String arist = intent.getStringExtra("Foreground_arist");
        if(path != null){
            Uri uri = Uri.parse(path);
            if(mediaPlayer != null){
                mediaPlayer.reset();
                try {
                    mediaPlayer.setDataSource(getApplicationContext(),uri);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    Intent musicStartIntent = new Intent(MainActivity.ACTION_MUSIC_START);
                    sendBroadcast(musicStartIntent);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        }
        Log.d("notion","111");
        //创建前台服务，通知栏设置
        //创建NotificationManager和NotificationChannel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,"Music Channel",NotificationManager.IMPORTANCE_HIGH);
            if(notificationManager != null){
                notificationManager.createNotificationChannel(channel);
            }
        }
        Log.d("notion","222");
        //设置通知的延迟消息，⽤户点击该通知时的反应
        //隐式消息的设置，点击通知返回原来的界面而不是新建一个界面
        Intent appIntent = new Intent(Intent.ACTION_MAIN);
        intent.setAction(Intent.ACTION_MAIN);
        appIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        appIntent.setComponent(new ComponentName(this.getPackageName(), this.getPackageName() + ".MainActivity"));
        //设置启动模式
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);


        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, appIntent, PendingIntent.FLAG_IMMUTABLE);
        //设置notification的builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(),CHANNEL_ID);
        //通过builder构建Notification
        Log.d("notion","333");
        if(title != null && arist != null){
            Notification notification = builder.
                    setContentTitle(title).
                    setContentText(arist).
                    setSmallIcon(R.drawable.ic_action_music).
                    setContentIntent(pendingIntent).build();
            if(notification != null){
                Log.d("notion","444");

                startForeground(Notification_id,notification);
            }

        }

        return super.onStartCommand(intent, flags, startId);
    }

    public class MusicServiceBinder extends Binder{
        MusicService getService(){
            return MusicService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return iBinder;
    }

    //service提供的接口
    //暂停播放
    public void pause(){
        if(mediaPlayer != null){
            mediaPlayer.pause();
        }
    }
    //播放
    public void play(){
        if(mediaPlayer != null){
            mediaPlayer.start();
        }
    }
    //获取当前播放⾳乐总时长
    public int getDuration(){
        int time = 0;
        if(mediaPlayer != null){
            time = mediaPlayer.getDuration();
        }
        return time;
    }
    //获取当前⾳乐播放进度
    public  int getCurrentPosition(){
        int position = 0;
        if(mediaPlayer != null){
            position = mediaPlayer.getCurrentPosition();
        }
        return position;
    }
    //获取 MediaPlayer ⾳乐播放状态(播放还是暂停)
    public boolean isPlaying(){
        if(mediaPlayer != null){
            return mediaPlayer.isPlaying();
        }
        return false;
    }
}