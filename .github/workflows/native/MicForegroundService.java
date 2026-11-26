package com.caveirax.micbooster;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/** Mantém o app vivo (e o microfone liberado) em segundo plano. */
public class MicForegroundService extends Service {
    private static final String CH = "caveirax_bg";
    private PowerManager.WakeLock wl;

    @Override public int onStartCommand(Intent i, int f, int id) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel(CH, "Caveira X em segundo plano", NotificationManager.IMPORTANCE_LOW));
        }
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        Notification n = b.setContentTitle("Caveira X ativo")
                .setContentText("Processando o microfone em segundo plano")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pi).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(1001, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1001, n);
        }
        if (wl == null) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "caveirax:mic");
            wl.acquire();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (wl != null && wl.isHeld()) wl.release();
        wl = null;
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
