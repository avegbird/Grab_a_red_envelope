package com.codeboy.qianghongbao;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.codeboy.qianghongbao.job.AccessbilityJob;
import com.codeboy.qianghongbao.job.WechatAccessbilityJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Created by LeonLee on 15/2/17 下午10:25.</p>
 * <p><a href="mailto:codeboy2013@163.com">Email:codeboy2013@163.com</a></p>
 *
 * 抢红包外挂服务
 */
public class QiangHongBaoService extends AccessibilityService {

    private static final String TAG = "QiangHongBao";

    private static final Class[] ACCESSBILITY_JOBS= {
            WechatAccessbilityJob.class,
    };
    private static QiangHongBaoService service;

    private List<AccessbilityJob> mAccessbilityJobs;
    private HashMap<String, AccessbilityJob> mPkgAccessbilityJobMap;


    @Override
    public void onCreate() {
        super.onCreate();

        mAccessbilityJobs = new ArrayList<>();
        mPkgAccessbilityJobMap = new HashMap<>();

        //初始化辅助插件工作
        for(Class clazz : ACCESSBILITY_JOBS) {
            try {
                Object object = clazz.newInstance();
                if(object instanceof AccessbilityJob) {
                    AccessbilityJob job = (AccessbilityJob) object;
                    job.onCreateJob(this);
                    mAccessbilityJobs.add(job);
                    mPkgAccessbilityJobMap.put(job.getTargetPackageName(), job);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "qianghongbao service destory");
        if(mPkgAccessbilityJobMap != null) {
            mPkgAccessbilityJobMap.clear();
        }
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            for (AccessbilityJob job : mAccessbilityJobs) {
                job.onStopJob();
            }
            mAccessbilityJobs.clear();
        }

        service = null;
        mAccessbilityJobs = null;
        mPkgAccessbilityJobMap = null;
        //发送广播，已经断开辅助服务
        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_DISCONNECT);
        sendBroadcast(intent);
        stopForeground(true);
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "qianghongbao service interrupt");
        Toast.makeText(this, "中断抢红包服务", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        service = this;
        //发送广播，已经连接上了
        Intent intent = new Intent(Config.ACTION_QIANGHONGBAO_SERVICE_CONNECT);
        sendBroadcast(intent);
        Toast.makeText(this, "已连接抢红包服务", Toast.LENGTH_SHORT).show();
        //在状态栏上显示一个永久的Notification
        sendNotification();
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
                .setContentTitle("抢红包服务已打开")
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
        startForeground(1, notification);
        //notifyManager.notify(1, notification);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if(BuildConfig.DEBUG) {
            Log.d(TAG, "事件--->" + event );
        }
        String pkn = String.valueOf(event.getPackageName());
        if(mAccessbilityJobs != null && !mAccessbilityJobs.isEmpty()) {
            if(!getConfig().isAgreement()) {
                return;
            }
            for (AccessbilityJob job : mAccessbilityJobs) {
                Log.e(Config.TAG,"pkn="+pkn+"TargetPackageName="+job.getTargetPackageName());
                if((pkn.equals("com.tencent.mobileqq")||pkn.equals(job.getTargetPackageName())) && job.isEnable()) {
                    job.onReceiveJob(event);
                }
            }
        }
    }

    public Config getConfig() {
        return Config.getConfig(this);
    }

    /** 接收通知栏事件*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void handeNotificationPosted(IStatusBarNotification notificationService) {
        if(notificationService == null) {
            return;
        }
        if(service == null || service.mPkgAccessbilityJobMap == null) {
            return;
        }
        String pack = notificationService.getPackageName();
        AccessbilityJob job = service.mPkgAccessbilityJobMap.get(pack);
        if(job == null) {
            return;
        }
        job.onNotificationPosted(notificationService);
    }

    /**
     * 判断当前服务是否正在运行
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean isRunning() {
        if(service == null) {
            return false;
        }
        AccessibilityManager accessibilityManager = (AccessibilityManager) service.getSystemService(Context.ACCESSIBILITY_SERVICE);
        AccessibilityServiceInfo info = service.getServiceInfo();
        if(info == null) {
            return false;
        }
        List<AccessibilityServiceInfo> list = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC);
        Iterator<AccessibilityServiceInfo> iterator = list.iterator();

        boolean isConnect = false;
        while (iterator.hasNext()) {
            AccessibilityServiceInfo i = iterator.next();
            if(i.getId().equals(info.getId())) {
                isConnect = true;
                break;
            }
        }
        if(!isConnect) {
            return false;
        }
        return true;
    }

    /** 快速读取通知栏服务是否启动*/
    public static boolean isNotificationServiceRunning() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return false;
        }
        //部份手机没有NotificationService服务
        try {
            return QHBNotificationService.isRunning();
        } catch (Throwable t) {}
        return false;
    }


}
