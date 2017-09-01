package com.codeboy.qianghongbao;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * <p>Created 16/2/4 下午11:16.</p>
 * <p><a href="mailto:codeboy2013@gmail.com">Email:codeboy2013@gmail.com</a></p>
 * <p><a href="http://www.happycodeboy.com">LeonLee Blog</a></p>
 *
 * @author LeonLee
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class QHBNotificationService extends NotificationListenerService {

    private static final String TAG = "QHBNotificationService";

    private static QHBNotificationService service;

    @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            onListenerConnected();
        }
    }

    private Config getConfig() {
        return Config.getConfig(this);
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "onNotificationRemoved");
        }
        if(!getConfig().isAgreement()) {
            return;
        }
        if(!getConfig().isEnableNotificationService()) {
            return;
        }
        QiangHongBaoService.handeNotificationPosted(new IStatusBarNotification() {
            @Override
            public String getPackageName() {
                return sbn.getPackageName();
            }

            @Override
            public Notification getNotification() {
                return sbn.getNotification();
            }
        });
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onNotificationRemoved(sbn);
        }
        if(BuildConfig.DEBUG) {
            Log.i(TAG, "onNotificationRemoved");
        }
    }

    @Override
    public void onListenerConnected() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onListenerConnected();
        }

        Log.i(TAG, "onListenerConnected");
        service = this;
        //发送广播，已经连接上了
        Intent intent = new Intent(Config.ACTION_NOTIFY_LISTENER_SERVICE_CONNECT);
        sendBroadcast(intent);
//        sendNotification();
    }
    private void sendNotification() {
        //获取PendingIntent
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        //获取NotificationManager实例
        //NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        //实例化NotificationCompat.Builde并设置相关属性
        Notification.Builder builder = new Notification.Builder(this)
                //设置小图标
                .setSmallIcon(R.drawable.ic_launcher_v2)
                //设置通知标题
                .setContentTitle("监听通知栏服务已打开")
                //设置通知内容
                .setContentText("请勿清除此消息")
                //设置点击不消失
                .setAutoCancel(false)
                //设置不允许取消
                .setOngoing(true)
                //设置点击自动呼出mainactivity
                .setContentIntent(mainPendingIntent);

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        startForeground(2, notification);
        //notifyManager.notify(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        service = null;
        //发送广播，已经连接上了
        Intent intent = new Intent(Config.ACTION_NOTIFY_LISTENER_SERVICE_DISCONNECT);
        sendBroadcast(intent);
//        stopForeground(true);
    }

    /** 是否启动通知栏监听*/
    public static boolean isRunning() {
        if(service == null) {
            return false;
        }
        return true;
    }
}
