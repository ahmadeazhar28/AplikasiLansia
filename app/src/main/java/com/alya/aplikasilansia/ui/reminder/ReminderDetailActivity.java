package com.alya.aplikasilansia.ui.reminder;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alya.aplikasilansia.R;

/**
 * Halaman yang muncul saat notifikasi reminder diklik (poin 5).
 * Menampilkan detail lengkap reminder yang aktif, bukan langsung
 * lempar ke dashboard supaya user tau persis apa yang harus dikerjakan.
 */
public class ReminderDetailActivity extends AppCompatActivity {

    private TextView tvTitle, tvTime, tvDesc;
    private ImageView imgReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_detail);

        tvTitle = findViewById(R.id.tv_detail_title);
        tvTime = findViewById(R.id.tv_detail_time);
        tvDesc = findViewById(R.id.tv_detail_desc);
        imgReminder = findViewById(R.id.img_detail_reminder);

        getDataFromIntent();

        findViewById(R.id.btn_selesai_reminder).setOnClickListener(v -> {
            // "Selesai" cukup nutup halaman ini - datanya sendiri sudah
            // otomatis hilang dari daftar setelah lewat waktu (lihat
            // ReminderRepository.fetchReminder yang filter reminder lampau)
            navigateToReminderList();
        });

        findViewById(R.id.btn_lihat_semua_reminder).setOnClickListener(v -> navigateToReminderList());
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String desc = intent.getStringExtra("desc");
        String time = intent.getStringExtra("formatted_time");
        int icon = intent.getIntExtra("icon", R.drawable.ic_remind_pumpkin);

        tvTitle.setText(title != null ? title : "Pengingat");
        tvDesc.setText((desc != null && !desc.isEmpty()) ? desc : "Tidak ada catatan tambahan.");
        tvTime.setText(time != null ? time : "");
        imgReminder.setImageResource(icon);
    }

    private void navigateToReminderList() {
        Intent intent = new Intent(this, ReminderActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
