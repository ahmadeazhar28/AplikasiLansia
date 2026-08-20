package com.alya.aplikasilansia.messaging;

import static com.alya.aplikasilansia.messaging.ReminderReceiver.ACTION_REMINDER;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.alya.aplikasilansia.data.Reminder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderScheduler {
    private static final String TAG = "ReminderScheduler";

    /**
     * requestCode diturunkan dari reminderId (bukan hardcode 0), supaya
     * tiap reminder punya PendingIntent unik sendiri dan tidak saling
     * menimpa (lihat catatan versi sebelumnya).
     */
    public static void scheduleReminder(Context context, String reminderId, String title, String desc, String timestamp, int icon, String repeatType) {
        Log.d(TAG, "scheduleReminder called for reminderId=" + reminderId + " repeatType=" + repeatType);

        if (reminderId == null) {
            Log.e(TAG, "reminderId null, tidak bisa schedule (requestCode tidak bisa dibuat unik)");
            return;
        }

        NotificationHelper.createNotificationChannel(context);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date reminderDate = sdf.parse(timestamp);

            if (reminderDate != null) {
                long reminderTimeMillis = reminderDate.getTime();

                Intent intent = new Intent(context, ReminderReceiver.class);
                intent.setAction(ACTION_REMINDER);
                intent.putExtra("reminderId", reminderId);
                intent.putExtra("title", title);
                intent.putExtra("desc", desc);
                intent.putExtra("timestamp", timestamp);
                intent.putExtra("icon", icon);
                intent.putExtra("repeatType", repeatType != null ? repeatType : Reminder.REPEAT_SEKALI);

                int requestCode = requestCodeFor(reminderId);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                if (canScheduleExactAlarms(context, alarmManager)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
                    }
                    Log.d(TAG, "Scheduled reminder " + reminderId + " at: " + reminderTimeMillis + " (requestCode=" + requestCode + ")");
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Intent intentSettings = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intentSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intentSettings);
                    }
                    Log.e(TAG, "Cannot schedule exact alarms");
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "ParseException: " + e.getMessage());
        }
    }

    public static void cancelReminder(Context context, String reminderId) {
        if (reminderId == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.setAction(ACTION_REMINDER);

        int requestCode = requestCodeFor(reminderId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Cancelled reminder " + reminderId + " (requestCode=" + requestCode + ")");
        }
    }

    private static int requestCodeFor(String reminderId) {
        return reminderId.hashCode();
    }

    private static boolean canScheduleExactAlarms(Context context, AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return alarmManager.canScheduleExactAlarms();
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException: " + e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }
}
