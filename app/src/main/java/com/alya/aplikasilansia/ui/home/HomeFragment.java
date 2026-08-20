package com.alya.aplikasilansia.ui.home;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.alya.aplikasilansia.LoginActivity;
import com.alya.aplikasilansia.R;
import com.alya.aplikasilansia.data.User;
import com.alya.aplikasilansia.ui.bloodpressure.BloodPresViewModel;
import com.alya.aplikasilansia.ui.bloodpressure.BloodPressureActivity;
import com.alya.aplikasilansia.ui.healthcare.HealthCareActivity;
import com.alya.aplikasilansia.ui.profile.ProfileFragment;
import com.alya.aplikasilansia.ui.profile.ProfileViewModel;
import com.alya.aplikasilansia.ui.reminder.ReminderActivity;
import com.alya.aplikasilansia.ui.reminder.ReminderViewModel;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class HomeFragment extends Fragment implements View.OnClickListener {
    private ProfileViewModel profileViewModel;
    private ReminderViewModel reminderViewModel;
    private BloodPresViewModel bloodPresViewModel;
    private TextView userNameHome;
    private ImageView profileImage;
    private Button toHealthCare;
    private Button toReminder;
    private Button toBP;
    private Button dfBp, dfHc, dfRem;
    private TextView tvTitleRemind, tvTimeRemind;
    private TextView tvPressure, tvPulse;
    private ImageView imgRemind;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        reminderViewModel = new ViewModelProvider(this).get(ReminderViewModel.class);
        bloodPresViewModel = new ViewModelProvider(this).get(BloodPresViewModel.class);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        tvTitleRemind = view.findViewById(R.id.txt_remind1);
        tvTimeRemind = view.findViewById(R.id.txt_remind2);
        imgRemind = view.findViewById(R.id.img_remind);

        // Now you can find your views within the inflated layout
        userNameHome = view.findViewById(R.id.txt_name);
        profileImage = view.findViewById(R.id.profile_image_home);

        tvPressure = view.findViewById(R.id.tv_pressure);
        tvPulse = view.findViewById(R.id.tv_pulse);

        if (mAuth.getCurrentUser() == null) {
            userNameHome.setText("Pengguna Baru");
            tvTitleRemind.setText("Buat Pengingatmu!");
            tvTimeRemind.setText("Belum ada pengingat terjadwal.");
            imgRemind.setImageResource(R.drawable.ic_remind_med);

            dfBp = view.findViewById(R.id.btn_to_bloodpresure);
            dfHc = view.findViewById(R.id.btn_to_healthcare);
            dfRem = view.findViewById(R.id.btn_to_reminder);

            dfBp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                }
            });

            dfHc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                }
            });

            dfRem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    startActivity(intent);
                }
            });

        } else {
            toHealthCare = view.findViewById(R.id.btn_to_healthcare);
            toHealthCare.setOnClickListener(this);

            toReminder = view.findViewById(R.id.btn_to_reminder);
            toReminder.setOnClickListener(this);

            toBP = view.findViewById(R.id.btn_to_bloodpresure);
            toBP.setOnClickListener(this);

            profileViewModel.getUserLiveData().observe(getViewLifecycleOwner(), new Observer<User>() {
                @Override
                public void onChanged(User user) {
                    if (user != null) {
                        userNameHome.setText(user.getUserName());
                        if (user.getProfileImageUrl() != null) {
                            Glide.with(HomeFragment.this)
                                    .load(user.getProfileImageUrl())
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.img);
                        }
                    }
                }
            });
            profileImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getId() == R.id.profile_image_home) {
                        Fragment profileFragment = new ProfileFragment();
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.nav_host_fragment_activity_main, profileFragment);
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                }
            });
            bloodPresViewModel.getLatestBloodPressureData().observe(getViewLifecycleOwner(), latestBloodPressure -> {
                if (latestBloodPressure != null) {
                    tvPressure.setText(latestBloodPressure.getBloodPressure());
                    tvPulse.setText(latestBloodPressure.getPulse());
                } else {
                    tvPressure.setText("-");
                    tvPulse.setText("-");
                }
            });
        }
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        reminderViewModel.fetchFirstReminder();
        updateFirstTodayReminderUI();
        reminderViewModel.fetchReminders();
    }

    /**
     * PENTING - FIX BUG: sebelumnya teks default "Buat Pengingatmu!" cuma
     * di-set lewat observer getReminderLiveData() saat reminders.size()==0.
     * Sejak ReminderRepository berhenti menghapus reminder yang sudah lewat
     * (demi fitur Riwayat), reminders.size() hampir tidak pernah 0 lagi kalau
     * user sudah pernah bikin reminder - walau tidak ada satupun yang akan
     * datang. Akibatnya teks default itu tidak pernah ke-trigger lagi dan
     * layar menampilkan teks placeholder lama dari XML yang tidak pernah
     * di-update.
     *
     * Fix: satu-satunya sumber kebenaran sekarang adalah firstReminder null
     * atau tidak (firstReminderLiveData sudah benar menghitung "reminder
     * akan datang berikutnya", terlepas dari riwayat). Observer kedua yang
     * lama (getReminderLiveData dengan reminders.size()==0) dihapus supaya
     * tidak saling menimpa/berebut state dengan observer ini.
     */
    private void updateFirstTodayReminderUI() {
        reminderViewModel.getFirstReminderLiveData().observe(getViewLifecycleOwner(), firstReminder -> {
            if (firstReminder != null) {
                Log.d(TAG, "Updating home with first today reminder: " + firstReminder.getTitle());
                tvTitleRemind.setText(firstReminder.getTitle());
                tvTimeRemind.setText(formatDate(firstReminder.getTimestamp()));
                imgRemind.setImageResource(firstReminder.getIcon());
            } else {
                String textName = "Buat Pengingatmu!";
                String textDate = "Belum ada pengingat terjadwal.";
                tvTitleRemind.setText(textName);
                tvTimeRemind.setText(textDate);
                imgRemind.setImageResource(R.drawable.ic_remind_med);
                Log.d(TAG, "No first today reminder to update UI with");
            }
        });
    }
    private String formatDate(String timestamp) {
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
            e.printStackTrace();
            return timestamp;
        }
    }

    private boolean isSameDay(Calendar cal1, Calendar cal2) {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_to_healthcare) {
            Intent intent = new Intent(getActivity(), HealthCareActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.btn_to_reminder) {
            Intent intent = new Intent(getActivity(), ReminderActivity.class);
            startActivity(intent);
        } else if (v.getId() == R.id.btn_to_bloodpresure) {
            Intent intent = new Intent(getActivity(), BloodPressureActivity.class);
            startActivity(intent);
        }
    }
}
