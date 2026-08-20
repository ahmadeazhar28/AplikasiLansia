package com.alya.aplikasilansia.ui.reminder;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.alya.aplikasilansia.data.Reminder;
import com.alya.aplikasilansia.data.ReminderRepository;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderViewModel extends ViewModel {
    private ReminderRepository reminderRepository;
    public MutableLiveData<List<Reminder>> reminderLiveData;
    public MutableLiveData<String> updateResultLiveData;
    public MutableLiveData<String> errorLiveData;


    public MutableLiveData<Reminder> firstReminderLiveData;

    public ReminderViewModel() {
        reminderLiveData = new MutableLiveData<>();
        updateResultLiveData = new MutableLiveData<>();
        firstReminderLiveData = new MutableLiveData<>();
        reminderRepository = new ReminderRepository();
        errorLiveData = new MutableLiveData<>();
        fetchReminders();
        fetchFirstReminder();
    }

    public LiveData<List<Reminder>> getReminderLiveData(){
        return reminderLiveData;
    }

    public MutableLiveData<Reminder> getFirstReminderLiveData() {
        return firstReminderLiveData;
    }
    public LiveData<String> getUpdateResultLiveData(){
        return updateResultLiveData;
    }

    public void fetchReminders() {
        reminderRepository.fetchReminder().observeForever(reminders -> {
            reminderLiveData.setValue(reminders);
            updateFirstReminder(reminders);
        });
    }

    public void fetchFirstReminder() {
        reminderRepository.fetchReminder().observeForever(reminders -> {
            updateFirstReminder(reminders);
        });
    }

    private void updateFirstReminder(List<Reminder> reminders) {
        if (reminders != null && !reminders.isEmpty()) {
            Reminder firstReminder = getFirstTodayOrUpcomingReminder(reminders);
            firstReminderLiveData.setValue(firstReminder);
        }
    }

    private Reminder getFirstTodayOrUpcomingReminder(List<Reminder> reminders) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Calendar today = Calendar.getInstance();

        Reminder firstUpcomingReminder = null;
        Reminder firstTodayReminder = null;

        for (Reminder reminder : reminders) {
            try {
                Date reminderDate = sdf.parse(reminder.getTimestamp());
                Calendar reminderCalendar = Calendar.getInstance();
                reminderCalendar.setTime(reminderDate);

                if (isSameDay(today, reminderCalendar)) {
                    if (reminderCalendar.after(today)) {
                        if (firstTodayReminder == null || reminderDate.before(sdf.parse(firstTodayReminder.getTimestamp()))) {
                            firstTodayReminder = reminder;
                        }
                    }
                } else if (reminderCalendar.after(today)) {
                    if (firstUpcomingReminder == null || reminderDate.before(sdf.parse(firstUpcomingReminder.getTimestamp()))) {
                        firstUpcomingReminder = reminder;
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return firstTodayReminder != null ? firstTodayReminder : firstUpcomingReminder;
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    public void editReminder(String reminderId, String title, String day, String time, String desc, String timestamp, Integer icon, String repeatType, Runnable onSuccess) {
        reminderRepository.editReminder(reminderId, title, day, time, desc, timestamp, icon, repeatType, errorLiveData, () -> {
            fetchReminders();
            if (onSuccess != null) {
                onSuccess.run();
            }
        });
    }

    public void deleteReminderData(String reminderId) {
        reminderRepository.deleteReminder(reminderId, errorLiveData, new ReminderRepository.OnReminderDeletedCallback() {
            @Override
            public void onReminderDeleted() {
                fetchReminders();
            }
        });
    }
}
