# Musicplayer
## 简介
基于android的简易音乐播放器
## 功能
1.只能读取本地的音乐文件进行播放；  
2.支持后台播放,采用service进行后台播放。
## 技术
1.添加了Foreground前台通知；  
2.在musicservice类里面添加了可以被组件调用的公共接口：
  * pause():暂停播放;
  * play():播放;
  * getDuration():获取当前播放⾳乐总时长;
  * getCurrentPosition():获取当前⾳乐播放进度
  * isPlaying():⾳乐播放状态(播放还是暂停)
  
3.通过绑定服务对musicservice进行播放的控制操作;  
4.通过handler进行进程的通话，更新音乐的播放进度；  
5.通过BroadcastReceiver进行进程的同步，在音乐开始播放时启动音乐进度更新的进程。
## 效果展示
![Image](https://github.com/luoshui2/Musicplayer/blob/master/app/src/main/res/drawable/Screenshot_20230924_115744.png)
