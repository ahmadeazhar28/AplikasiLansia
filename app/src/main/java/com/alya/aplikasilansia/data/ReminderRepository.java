package com.alya.aplikasilansia.data;

import static android.content.ContentValues.TAG;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReminderRepository {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public ReminderRepository() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public MutableLiveData<List<Reminder>> fetchReminder() {
        MutableLiveData<List<Reminder>> reminderLiveData = new MutableLiveData<>();
        FirebaseUser firebaseUser = mAuth.getCurrentUser();

        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            db.collection("users").document(userId).collection("reminders")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            QuerySnapshot snapshot = task.getResult();
                            List<Reminder> reminders = new ArrayList<>();
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            Date now = new Date();

                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                String id = doc.getId();
                                String title = doc.getString("title");
                                String day = doc.getString("day");
                                String time = doc.getString("time");
                                String desc = doc.getString("desc");
                                String timestamp = doc.getString("timestamp");
                                Long iconLong = doc.getLong("icon");
                                Integer icon = iconLong != null ? iconLong.intValue() : 0;

                                Reminder reminder = new Reminder(userId, id, title, day, time, desc, timestamp, icon);
                                reminders.add(reminder);
                            }
                            reminders.removeIf(reminder -> {
                                try {
                                    Date reminderDate = sdf.parse(reminder.getTimestamp());
                                    return reminderDate.before(now);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    return false;
                                }
                            });
                            reminderLiveData.setValue(reminders);
                        } else {
                            Log.e("ReminderRepository", "Firestore error: ", task.getException());
                            reminderLiveData.setValue(null);
                        }
                    });
        }

        return reminderLiveData;
    }

    public void createReminder(String title, String day, String time, String desc, String timestamp, Integer icon, MutableLiveData<FirebaseUser> reminderLiveData, MutableLiveData<String> errorLiveData) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            Log.d(TAG, "User ID: " + userId);

            DocumentReference newReminderRef = db.collection("users").document(userId).collection("reminders").document();

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("title", title);
            data.put("day", day);
            data.put("time", time);
            data.put("desc", desc);
            data.put("timestamp", timestamp);
            data.put("icon", icon);

            newReminderRef.set(data)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reminder added successfully");
                        reminderLiveData.postValue(firebaseUser);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add reminder: " + e.getMessage());
                        errorLiveData.postValue("Failed to add reminder: " + e.getMessage());
                    });
        } else {
            errorLiveData.postValue("User not authenticated");
        }
    }

    public void editReminder(String reminderId, String title, String day, String time, String desc, String timestamp, Integer icon, MutableLiveData<String> errorLiveData, Runnable onSuccess) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DocumentReference reminderRef = db.collection("users").document(userId).collection("reminders").document(reminderId);

            Map<String, Object> updatedValues = new HashMap<>();
            updatedValues.put("title", title);
            updatedValues.put("day", day);
            updatedValues.put("time", time);
            updatedValues.put("desc", desc);
            updatedValues.put("timestamp", timestamp);
            updatedValues.put("icon", icon);

            reminderRef.update(updatedValues)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reminder updated successfully");
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update reminder: " + e.getMessage());
                        errorLiveData.postValue("Failed to update reminder: " + e.getMessage());
                    });
        } else {
            errorLiveData.postValue("User not authenticated");
        }
    }

    public interface OnReminderDeletedCallback {
        void onReminderDeleted();
    }

    public void deleteReminder(String reminderId, MutableLiveData<String> errorLiveData, OnReminderDeletedCallback callback) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            DocumentReference reminderRef = db.collection("users").document(userId).collection("reminders").document(reminderId);

            reminderRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reminder deleted successfully");
                        callback.onReminderDeleted();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete reminder: " + e.getMessage());
                        errorLiveData.postValue("Failed to delete reminder: " + e.getMessage());
                    });
        } else {
            errorLiveData.postValue("User not authenticated");
        }
    }
}