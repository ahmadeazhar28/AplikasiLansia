package com.alya.aplikasilansia.ui.reminder;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alya.aplikasilansia.LoginActivity;
import com.alya.aplikasilansia.R;
import com.alya.aplikasilansia.data.Reminder;
import com.alya.aplikasilansia.messaging.ReminderScheduler;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Halaman khusus buat riwayat pengingat (reminder yang sudah lewat waktunya).
 * Sebelumnya ini nempel jadi salah satu opsi filter di ReminderActivity,
 * sekarang dipisah jadi halaman sendiri supaya lebih jelas: ReminderActivity
 * fokus ke pengingat aktif, halaman ini fokus ke histori.
 */
public class RiwayatReminderActivity extends AppCompatActivity implements DeleteReminderFragment.OnReminderDeletedListener {

    private RecyclerView riwayatRV;
    private FilteredReminderAdapter adapter;
    private ReminderViewModel reminderViewModel;
    private TextView tvNoRiwayat;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat_reminder);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        findViewById(R.id.btn_back_riwayat_reminder).setOnClickListener(v -> finish());

        tvNoRiwayat = findViewById(R.id.tv_no_riwayat);
        riwayatRV = findViewById(R.id.rv_riwayat_reminder);
        riwayatRV.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FilteredReminderAdapter(new ArrayList<>(), this);
        riwayatRV.setAdapter(adapter);

        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);
        loadRiwayat();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRiwayat();
    }

    private void loadRiwayat() {
        reminderViewModel.getReminderLiveData().observe(this, reminders -> {
            if (reminders == null) {
                return;
            }
            List<Object> items = buildRiwayatItems(reminders);
            adapter.updateList(items);
            if (items.isEmpty()) {
                tvNoRiwayat.setVisibility(View.VISIBLE);
            } else {
                tvNoRiwayat.setVisibility(View.GONE);
            }
        });
        reminderViewModel.fetchReminders();
    }

    // Sama persis dengan logic "Riwayat" yang dulu ada di
    // ReminderActivity.filterReminders(), dipindah ke sini apa adanya.
    private List<Object> buildRiwayatItems(List<Reminder> reminders) {
        List<Object> items = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date now = new Date();

        List<Reminder> pastReminders = new ArrayList<>();
        for (Reminder reminder : reminders) {
            try {
                Date reminderDate = sdf.parse(reminder.getTimestamp());
                if (reminderDate != null && reminderDate.before(now)) {
                    pastReminders.add(reminder);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error parsing date riwayat: " + e.getMessage());
            }
        }
        pastReminders.sort((r1, r2) -> {
            try {
                Date date1 = sdf.parse(r1.getTimestamp());
                Date date2 = sdf.parse(r2.getTimestamp());
                return date2.compareTo(date1); // terbaru dulu
            } catch (ParseException e) {
                return 0;
            }
        });

        items.addAll(pastReminders);
        return items;
    }

    @Override
    public void onReminderDeleted(String reminderId) {
        adapter.removeReminder(reminderId);
        ReminderScheduler.cancelReminder(this, reminderId);
        reminderViewModel.deleteReminderData(reminderId);
        loadRiwayat();
    }
}
