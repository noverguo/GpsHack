package info.noverguo.gpshack.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import info.noverguo.gpshack.R;
import info.noverguo.gpshack.receiver.ActionReceiver;

/**
 * Created by noverguo on 2016/6/8.
 */
public class RemoteOffsetGpsService extends Service {
    NotificationManager notificationManager;
    GpsOffsetService gpsOffsetService;
    ActionReceiver actionReceiver;
    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        gpsOffsetService = GpsOffsetService.get(getApplicationContext());
        listenAction();
        showButtonNotify();
    }

    private void listenAction() {
        ActionReceiver.register(getApplicationContext()).setGpsOffsetService(gpsOffsetService);
    }

    private void showButtonNotify() {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        RemoteViews mRemoteViews = new RemoteViews(getPackageName(), R.layout.notifi_location);
        mRemoteViews.setOnClickPendingIntent(R.id.btn_action_left, ActionReceiver.getLeftPendingIntent(getApplicationContext()));
        mRemoteViews.setOnClickPendingIntent(R.id.btn_action_up, ActionReceiver.getUpPendingIntent(getApplicationContext()));
        mRemoteViews.setOnClickPendingIntent(R.id.btn_action_down, ActionReceiver.getDownPendingIntent(getApplicationContext()));
        mRemoteViews.setOnClickPendingIntent(R.id.btn_action_right, ActionReceiver.getRightPendingIntent(getApplicationContext()));

        mBuilder.setContent(mRemoteViews)
                .setWhen(System.currentTimeMillis())// 通知产生的时间，会在通知信息里显示
                .setPriority(Notification.PRIORITY_DEFAULT)// 设置该通知优先级
                .setOngoing(true)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setSmallIcon(R.mipmap.ic_launcher);
        Notification notify = mBuilder.build();
        notify.flags = Notification.FLAG_ONGOING_EVENT;
        notify.icon = R.mipmap.ic_launcher;
        notificationManager.notify(1001, notify);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return gpsOffsetService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(actionReceiver);
    }
}
