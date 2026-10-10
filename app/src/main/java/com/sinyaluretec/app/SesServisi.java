package com.sinyaluretec.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Ses çalarken uygulamanın arka planda ve ekran kapalıyken de çalışmasını sağlayan ön plan servisi.
 */
public class SesServisi extends Service {

    public static final String ACTION_DURDUR = "com.sinyaluretec.app.DURDUR";
    private static final String CHANNEL_ID = "ses";
    private static final int NOTIF_ID = 1;

    /** MainActivity tarafından ayarlanır; ana iş parçacığında çalıştırılır. */
    public static volatile Runnable durdurIstegi;

    private PowerManager.WakeLock wakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DURDUR.equals(intent.getAction())) {
            // Bildirimdeki "Durdur" düğmesi: sesi web tarafında durdur (uygulamayı öne getirmeden).
            Runnable r = durdurIstegi;
            if (r != null) {
                r.run();
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        createChannel();
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(NOTIF_ID, n);
        }
        acquireWakeLock();
        return START_NOT_STICKY;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Ses çalma", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Sinyal arka planda çalarken gösterilir");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(this, 0, open, piFlags);

        Intent stop = new Intent(this, SesServisi.class).setAction(ACTION_DURDUR);
        PendingIntent stopPi = PendingIntent.getService(this, 1, stop, piFlags);

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Sinyal Üreteci")
                .setContentText("Sinyal çalıyor")
                .setContentIntent(openPi)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        null, "Durdur", stopPi).build());
        return b.build();
    }

    private void acquireWakeLock() {
        if (wakeLock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SinyalUreteci:ses");
            wakeLock.setReferenceCounted(false);
        }
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
