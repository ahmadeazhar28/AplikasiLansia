package com.alya.aplikasilansia.messaging;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.alya.aplikasilansia.R;

/**
 * Setup channel notifikasi reminder (poin 4: berbunyi + prioritas tinggi
 * supaya heads-up notification tetap muncul walau aplikasi sedang dibuka -
 * ini yang menjawab poin 1 "muncul di dalam aplikasi").
 */
public class NotificationHelper {

    public static final String CHANNEL_ID = "reminder_channel";
    private static final String CHANNEL_NAME = "Pengingat Aplikasi Nula";
    private static final String CHANNEL_DESC = "Notifikasi pengingat jadwal Anda";

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager == null) return;

            // Cek supaya tidak bikin ulang channel yang sudah ada
            if (manager.getNotificationChannel(CHANNEL_ID) != null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // wajib HIGH supaya heads-up + bunyi + muncul walau app lagi dibuka
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 400, 200, 400});

            Uri defaultSoundUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(defaultSoundUri, audioAttributes);

            manager.createNotificationChannel(channel);
        }
    }

    /**
     * Convert drawable/vector logo Nula (ic_nula_app) jadi Bitmap,
     * dipakai sebagai large icon notifikasi (poin 6: notifikasi pakai
     * logo aplikasi Nula, bukan cuma ikon alarm generik).
     */
    public static Bitmap getAppLogoBitmap(Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_nula_app);
        if (drawable == null) return null;

        int width = drawable.getIntrinsicWidth() > 0 ? drawable.getIntrinsicWidth() : 128;
        int height = drawable.getIntrinsicHeight() > 0 ? drawable.getIntrinsicHeight() : 128;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
