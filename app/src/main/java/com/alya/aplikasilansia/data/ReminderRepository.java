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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReminderRepository {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    public ReminderRepository() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * PENTING: sebelumnya method ini MENGHAPUS reminder yang sudah lewat
     * waktunya dari hasil fetch (reminders.removeIf(...)). Sekarang SEMUA
     * reminder (termasuk yang sudah lewat) tetap di-fetch, supaya filter
     * "Riwayat" di ReminderActivity bisa menampilkannya. Pemisahan
     * upcoming vs riwayat sekarang dilakukan di sisi UI (ReminderActivity),
     * bukan di query data.
     */
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

                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                String id = doc.getId();
                                String title = doc.getString("title");
                                String day = doc.getString("day");
                                String time = doc.getString("time");
                                String desc = doc.getString("desc");
                                String timestamp = doc.getString("timestamp");
                                Long iconLong = doc.getLong("icon");
                                Integer icon = iconLong != null ? iconLong.intValue() : 0;
                                String repeatType = doc.getString("repeatType"); // null utk data lama -> otomatis jadi SEKALI

                                Reminder reminder = new Reminder(userId, id, title, day, time, desc, timestamp, icon, repeatType);
                                reminders.add(reminder);
                            }
                            reminderLiveData.setValue(reminders);
                        } else {
                            Log.e("ReminderRepository", "Firestore error: ", task.getException());
                            reminderLiveData.setValue(null);
                        }
                    });
        }

        return reminderLiveData;
    }

    public interface OnReminderCreatedListener {
        void onSuccess(String reminderId);
        void onFailure(String error);
    }

    public void createReminder(String title, String day, String time, String desc, String timestamp, Integer icon, String repeatType,
                                MutableLiveData<FirebaseUser> reminderLiveData, MutableLiveData<String> errorLiveData,
                                OnReminderCreatedListener listener) {
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser != null) {
            String userId = firebaseUser.getUid();
            Log.d(TAG, "User ID: " + userId);

            DocumentReference newReminderRef = db.collection("users").document(userId).collection("reminders").document();
            String newReminderId = newReminderRef.getId();

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("title", title);
            data.put("day", day);
            data.put("time", time);
            data.put("desc", desc);
            data.put("timestamp", timestamp);
            data.put("icon", icon);
            data.put("repeatType", repeatType != null ? repeatType : Reminder.REPEAT_SEKALI);

            newReminderRef.set(data)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reminder added successfully, id=" + newReminderId);
                        reminderLiveData.postValue(firebaseUser);
                        if (listener != null) {
                            listener.onSuccess(newReminderId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add reminder: " + e.getMessage());
                        errorLiveData.postValue("Failed to add reminder: " + e.getMessage());
                        if (listener != null) {
                            listener.onFailure(e.getMessage());
                        }
                    });
        } else {
            errorLiveData.postValue("User not authenticated");
        }
    }

    public void editReminder(String reminderId, String title, String day, String time, String desc, String timestamp, Integer icon, String repeatType, MutableLiveData<String> errorLiveData, Runnable onSuccess) {
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
            updatedValues.put("repeatType", repeatType != null ? repeatType : Reminder.REPEAT_SEKALI);

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

    /**
     * Update ringan, cuma field timestamp - dipakai ReminderReceiver saat
     * reminder berulang (Harian/Mingguan) sudah bunyi dan perlu digeser
     * ke jadwal berikutnya, tanpa perlu kirim ulang semua field lain.
     */
    public void updateReminderTimestamp(String userId, String reminderId, String newTimestamp) {
        if (userId == null || reminderId == null) return;
        db.collection("users").document(userId).collection("reminders").document(reminderId)
                .update("timestamp", newTimestamp)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Reminder " + reminderId + " timestamp digeser ke " + newTimestamp))
                .addOnFailureListener(e -> Log.e(TAG, "Gagal update timestamp reminder berulang: " + e.getMessage()));
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
