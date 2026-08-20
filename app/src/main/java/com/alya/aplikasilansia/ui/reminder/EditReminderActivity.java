package com.alya.aplikasilansia.ui.reminder;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.alya.aplikasilansia.R;
import com.alya.aplikasilansia.data.Reminder;
import com.alya.aplikasilansia.messaging.ReminderScheduler;
import com.alya.aplikasilansia.ui.newreminder.IconReminderFragment;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditReminderActivity extends AppCompatActivity implements IconReminderFragment.OnIconSelectedListener {
    private Button backBtn, saveBtn, cancelBtn, editIconBtn;
    private TextView timePickerTv;
    private EditText titleEt, descEt;
    private ImageView iconImg;
    private Spinner daySpinner;
    private Spinner repeatSpinner;
    private String reminderId, daySelected, repeatSelected;
    private int selectedIcon;
    ReminderViewModel reminderViewModel;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reminder);

        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);

        iconImg = findViewById(R.id.img_ic_reminder_edit);
        titleEt = findViewById(R.id.et_remind_title);
        daySpinner = findViewById(R.id.edit_day_reminder);
        repeatSpinner = findViewById(R.id.spinner_repeat_reminder_edit);
        timePickerTv = findViewById(R.id.tv_hour_reminder_edit);
        descEt = findViewById(R.id.et_desc_reminder);
        editIconBtn = findViewById(R.id.btn_ic_remind_edit);
        saveBtn = findViewById(R.id.btn_save_edit_reminder);
        cancelBtn = findViewById(R.id.btn_cancel_edit_reminder);
        backBtn = findViewById(R.id.btn_back_editreminder);

        dialogIconReminder(editIconBtn);
        setTimePicker(timePickerTv);
        getReminderData();

        cancelBtn.setOnClickListener(v -> {
            Intent intent = new Intent(EditReminderActivity.this, ReminderActivity.class);
            startActivity(intent);
            finish();
        });

        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(EditReminderActivity.this, ReminderActivity.class);
            startActivity(intent);
            finish();
        });

        saveBtn.setOnClickListener(v -> {
            saveEditedData();
        });

        reminderViewModel.errorLiveData.observe(this, errorMessage -> {
            if (errorMessage != null) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getReminderData() {
        Intent intent = getIntent();
        reminderId = intent.getStringExtra("REMINDER_ID");
        titleEt.setText(intent.getStringExtra("REMINDER_TITLE"));
        daySelected = intent.getStringExtra("REMINDER_DAY");
        setDaySpinner(daySelected);
        timePickerTv.setText(intent.getStringExtra("REMINDER_TIME"));
        descEt.setText(intent.getStringExtra("REMINDER_DESC"));
        selectedIcon = intent.getIntExtra("REMINDER_ICON", 1);
        iconImg.setImageResource(selectedIcon);

        String repeatCode = intent.getStringExtra("REMINDER_REPEAT_TYPE");
        setRepeatSpinner(repeatCode);
    }

    private void setRepeatSpinner(String repeatCode) {
        CustomSpinnerAdapter repeatAdapter = new CustomSpinnerAdapter(
                this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.repeat_array)
        );
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeatSpinner.setAdapter(repeatAdapter);

        int position = 0; // default "Sekali saja"
        if (Reminder.REPEAT_HARIAN.equals(repeatCode)) {
            position = 1;
        } else if (Reminder.REPEAT_MINGGUAN.equals(repeatCode)) {
            position = 2;
        }
        repeatSpinner.setSelection(position);
    }

    private String mapRepeatLabelToCode(String label) {
        if (label == null) return Reminder.REPEAT_SEKALI;
        if (label.equals("Setiap Hari")) return Reminder.REPEAT_HARIAN;
        if (label.equals("Setiap Minggu")) return Reminder.REPEAT_MINGGUAN;
        return Reminder.REPEAT_SEKALI;
    }

    private void saveEditedData() {
        String newTitle = titleEt.getText().toString().trim();
        String newDay = daySpinner.getSelectedItem().toString();
        String newTime = timePickerTv.getText().toString().trim();
        String desc = descEt.getText().toString().trim();
        String timestamp = calculateTimestamp(newDay, newTime);
        String repeatType = mapRepeatLabelToCode(repeatSpinner.getSelectedItem().toString());

        if (reminderId != null){
            // Batalkan alarm LAMA dulu sebelum menjadwalkan yang baru.
            ReminderScheduler.cancelReminder(this, reminderId);

            reminderViewModel.editReminder(reminderId, newTitle, newDay, newTime, desc, timestamp, selectedIcon, repeatType, () -> {
                ReminderScheduler.scheduleReminder(EditReminderActivity.this, reminderId, newTitle, desc, timestamp, selectedIcon, repeatType);
                dataSavedDialog();
                Intent intent = new Intent(EditReminderActivity.this, ReminderActivity.class);
                startActivity(intent);
                finish();
            });

        }
    }

    private void dataSavedDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.data_saved_dialog, null);

        ImageView toastIcon = layout.findViewById(R.id.img_verif_sent);
        TextView toastText = layout.findViewById(R.id.text_verif_sent);

        String text = "Pengingat Berhasil Diperbarui";

        toastIcon.setImageResource(R.drawable.ic_checkmark);
        toastText.setText(text);

        Toast toast = new Toast(this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public void dialogIconReminder(Button editIconBtn) {
        editIconBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IconReminderFragment iconReminderFragment = new IconReminderFragment();
                iconReminderFragment.show(getSupportFragmentManager(), "IconReminderDialog");
                iconReminderFragment.setOnIconSelectedListener(EditReminderActivity.this);
            }
        });
    }

    @Override
    public void onIconSelected(int iconResId) {
        iconImg.setImageResource(iconResId);
        selectedIcon = iconResId;
    }

    private void setDaySpinner(String selectedDay) {
        CustomSpinnerAdapter spinnerAdapter = new CustomSpinnerAdapter(
                this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.day_array)
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        daySpinner.setAdapter(spinnerAdapter);

        if (selectedDay != null) {
            int position = spinnerAdapter.getPosition(selectedDay);
            daySpinner.setSelection(position);
        }
    }

    private void setTimePicker(TextView timePickerTv) {
        timePickerTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });
    }

    private void showTimePicker() {
        MaterialTimePicker materialTimePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .build();

        materialTimePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = materialTimePicker.getHour();
                int minute = materialTimePicker.getMinute();
                String selectedTime = String.format("%02d:%02d", hour, minute);
                timePickerTv.setText(selectedTime);
            }
        });

        materialTimePicker.show(getSupportFragmentManager(), "timePicker");
    }

    private String calculateTimestamp(String selectedDay, String selectedTime) {
        Calendar now = Calendar.getInstance();

        String[] timeParts = selectedTime.split(":");
        if (timeParts.length != 2) {
            return "";
        }

        int hour = 0;
        int minute = 0;

        try {
            hour = Integer.parseInt(timeParts[0]);
            minute = Integer.parseInt(timeParts[1]);
        } catch (NumberFormatException e) {
            return "";
        }

        int dayOfWeekNow = now.get(Calendar.DAY_OF_WEEK);
        int targetDayOfWeek = getDayOfWeek(selectedDay);
        if (targetDayOfWeek == -1) {
            return "";
        }
        int dayDifference = (targetDayOfWeek - dayOfWeekNow + 7) % 7;

        Calendar targetDate = (Calendar) now.clone();
        targetDate.add(Calendar.DAY_OF_MONTH, dayDifference);
        targetDate.set(Calendar.HOUR_OF_DAY, hour);
        targetDate.set(Calendar.MINUTE, minute);
        targetDate.set(Calendar.SECOND, 0);
        targetDate.set(Calendar.MILLISECOND, 0);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(targetDate.getTime());
    }

    private int getDayOfWeek(String day) {
        switch (day) {
            case "Senin":
                return Calendar.MONDAY;
            case "Selasa":
                return Calendar.TUESDAY;
            case "Rabu":
                return Calendar.WEDNESDAY;
            case "Kamis":
                return Calendar.THURSDAY;
            case "Jumat":
                return Calendar.FRIDAY;
            case "Sabtu":
                return Calendar.SATURDAY;
            case "Minggu":
                return Calendar.SUNDAY;
            default:
                return -1;
        }
    }
}
