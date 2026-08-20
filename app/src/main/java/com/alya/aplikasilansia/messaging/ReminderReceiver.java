package com.alya.aplikasilansia.messaging;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.alya.aplikasilansia.R;
import com.alya.aplikasilansia.data.Reminder;
import com.alya.aplikasilansia.data.ReminderRepository;
import com.alya.aplikasilansia.ui.reminder.ReminderDetailActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "com.alya.aplikasilansia.ACTION_REMINDER";
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.w(TAG, "Null intent received");
            return;
        }

        String action = intent.getAction();

        if (ACTION_REMINDER.equals(action)) {
            handleReminderTrigger(context, intent);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            handleBootCompleted(context);
        } else {
            Log.w(TAG, "Unexpected or null intent action: " + action);
        }
    }

    private void handleReminderTrigger(Context context, Intent intent) {
        String reminderId = intent.getStringExtra("reminderId");
        String title = intent.getStringExtra("title");
        String description = intent.getStringExtra("desc");
        String timestamp = intent.getStringExtra("timestamp");
        int icon = intent.getIntExtra("icon", R.drawable.ic_remind_pumpkin);
        String repeatType = intent.getStringExtra("repeatType");
        if (repeatType == null) repeatType = Reminder.REPEAT_SEKALI;

        if (title == null) {
            Log.w(TAG, "Null title received, notifikasi dibatalkan");
            return;
        }

        NotificationHelper.createNotificationChannel(context);

        Intent notificationIntent = new Intent(context, ReminderDetailActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.putExtra("title", title);
        notificationIntent.putExtra("desc", description);
        notificationIntent.putExtra("formatted_time", formatDate(timestamp));
        notificationIntent.putExtra("icon", icon);

        int requestCode = reminderId != null ? reminderId.hashCode() : 0;
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, requestCode, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(NotificationHelper.getAppLogoBitmap(context))
                .setContentTitle(title)
                .setContentText(description)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(description))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            int notificationId = requestCode;
            notificationManager.notify(notificationId, builder.build());
        }

        // Reminder berulang (Harian/Mingguan): hitung jadwal berikutnya dan
        // schedule ulang alarm-nya, sekaligus geser timestamp di Firestore
        // supaya list Reminder tetap akurat menampilkan jadwal berikutnya.
        if (!Reminder.REPEAT_SEKALI.equals(repeatType) && reminderId != null && timestamp != null) {
            rescheduleNextOccurrence(context, reminderId, title, description, timestamp, icon, repeatType);
        }
    }

    private void rescheduleNextOccurrence(Context context, String reminderId, String title, String desc, String currentTimestamp, int icon, String repeatType) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        try {
            Date currentDate = sdf.parse(currentTimestamp);
            if (currentDate == null) return;

            Calendar next = Calendar.getInstance();
            next.setTime(currentDate);

            if (Reminder.REPEAT_HARIAN.equals(repeatType)) {
                next.add(Calendar.DAY_OF_YEAR, 1);
            } else if (Reminder.REPEAT_MINGGUAN.equals(repeatType)) {
                next.add(Calendar.DAY_OF_YEAR, 7);
            } else {
                return; // bukan repeat, tidak perlu reschedule
            }

            String newTimestamp = sdf.format(next.getTime());

            ReminderScheduler.scheduleReminder(context, reminderId, title, desc, newTimestamp, icon, repeatType);
            Log.d(TAG, "Reminder berulang " + reminderId + " dijadwalkan ulang ke " + newTimestamp);

            // Update Firestore fire-and-forget - alarm sudah aman terjadwal
            // duluan di atas, ini cuma supaya tampilan list konsisten.
            String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            if (userId != null) {
                new ReminderRepository().updateReminderTimestamp(userId, reminderId, newTimestamp);
            }
        } catch (ParseException e) {
            Log.e(TAG, "Gagal parse timestamp untuk reschedule: " + e.getMessage());
        }
    }

    /**
     * Ambil ulang semua reminder milik user dari Firestore, lalu jadwalkan
     * ulang alarm-nya satu per satu setelah HP reboot (AlarmManager tidak
     * persist alarm lewat reboot).
     */
    private void handleBootCompleted(Context context) {
        Log.d(TAG, "BOOT_COMPLETED diterima, reschedule semua reminder...");
        final PendingResult pendingResult = goAsync();

        ReminderRepository repository = new ReminderRepository();
        repository.fetchReminder().observeForever(new androidx.lifecycle.Observer<List<Reminder>>() {
            @Override
            public void onChanged(List<Reminder> reminders) {
                if (reminders != null) {
                    Date now = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    for (Reminder reminder : reminders) {
                        try {
                            Date reminderDate = sdf.parse(reminder.getTimestamp());
                            // Reminder berulang tetap di-reschedule walau timestamp
                            // tersimpannya sudah lewat (kasus HP mati lama) - alarm
                            // akan langsung fire begitu terpasang kalau waktunya
                            // sudah lewat, lalu otomatis geser ke jadwal berikutnya.
                            boolean isRepeating = !Reminder.REPEAT_SEKALI.equals(reminder.getRepeatType());
                            if (reminderDate != null && (reminderDate.after(now) || isRepeating)) {
                                ReminderScheduler.scheduleReminder(
                                        context,
                                        reminder.getId(),
                                        reminder.getTitle(),
                                        reminder.getDesc(),
                                        reminder.getTimestamp(),
                                        reminder.getIcon(),
                                        reminder.getRepeatType()
                                );
                            }
                        } catch (ParseException e) {
                            Log.e(TAG, "Gagal parse timestamp reminder saat boot reschedule: " + e.getMessage());
                        }
                    }
                    Log.d(TAG, "Reschedule selesai, total reminder diproses: " + reminders.size());
                }
                repository.fetchReminder().removeObserver(this);
                pendingResult.finish();
            }
        });
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private String formatDate(String timestamp) {
        if (timestamp == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("'Hari ini pukul' HH:mm", new Locale("id", "ID"));
        SimpleDateFormat tomorrowFormat = new SimpleDateFormat("'Besok pukul' HH:mm", new Locale("id", "ID"));
        SimpleDateFormat normalFormat = new SimpleDateFormat("EEEE, d MMMM 'pukul' HH:mm", new Locale("id", "ID"));

        try {
            Date date = sdf.parse(timestamp);
            Calendar calendar = Calendar.getInstance();
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            calendar.setTime(date);

            if (isSameDay(calendar, today)) {
                return outputFormat.format(date);
            } else {
                Calendar tomorrow = (Calendar) today.clone();
                tomorrow.add(Calendar.DAY_OF_YEAR, 1);
                if (isSameDay(calendar, tomorrow)) {
                    return tomorrowFormat.format(date);
                } else {
                    return normalFormat.format(date);
                }
            }
        } catch (ParseException e) {
            return timestamp;
        }
    }
}
