package com.alya.aplikasilansia.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.alya.aplikasilansia.LoginActivity;
import com.alya.aplikasilansia.R;
import com.alya.aplikasilansia.data.BloodPressure;
import com.alya.aplikasilansia.data.QuizHistoryItem;
import com.alya.aplikasilansia.data.User;
import com.alya.aplikasilansia.data.inputMedHistory;
import com.alya.aplikasilansia.ui.bloodpressure.BloodPresViewModel;
import com.alya.aplikasilansia.ui.bloodpressure.BloodPressureActivity;
import com.alya.aplikasilansia.ui.check.CheckFragment;
import com.alya.aplikasilansia.ui.quiz.QuizInstructionActivity;
import com.alya.aplikasilansia.ui.quiz.QuizViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Objects;


public class ProfileHealthFragment extends Fragment {
    private LinearLayout profileMedHistory;
    private TextView tvCaregiver, tvMaritalStatus;
    private ProfileViewModel profileViewModel;
    private QuizViewModel quizViewModel;
    private BloodPresViewModel bloodPresViewModel;
    private Button signOut;
    private String userId;

    // Kartu ringkasan tes skrining
    private LinearLayout layoutSkriningFilled, layoutSkriningEmpty;
    private TextView tvSkriningKlasifikasi, tvSkriningScore, tvSkriningDate;
    private MaterialButton btnLihatSemuaSkrining, btnMulaiTesSkrining;

    // Kartu ringkasan tekanan darah
    private LinearLayout layoutTensiFilled, layoutTensiEmpty;
    private TextView tvTensiPressure, tvTensiPulse, tvTensiDate;
    private MaterialButton btnLihatSemuaTensi, btnCatatTensi;

    public ProfileHealthFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileViewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        quizViewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        bloodPresViewModel = new ViewModelProvider(this).get(BloodPresViewModel.class);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile_health, container, false);

        profileMedHistory = view.findViewById(R.id.profile_medhistory);
        tvCaregiver = view.findViewById(R.id.tv_caregiver);
        tvMaritalStatus = view.findViewById(R.id.tv_marital_stat);
        signOut = view.findViewById(R.id.btn_sign_out_2);

        layoutSkriningFilled = view.findViewById(R.id.layout_skrining_filled);
        layoutSkriningEmpty = view.findViewById(R.id.layout_skrining_empty);
        tvSkriningKlasifikasi = view.findViewById(R.id.tv_skrining_klasifikasi);
        tvSkriningScore = view.findViewById(R.id.tv_skrining_score);
        tvSkriningDate = view.findViewById(R.id.tv_skrining_date);
        btnLihatSemuaSkrining = view.findViewById(R.id.btn_lihat_semua_skrining);
        btnMulaiTesSkrining = view.findViewById(R.id.btn_mulai_tes_skrining);

        layoutTensiFilled = view.findViewById(R.id.layout_tensi_filled);
        layoutTensiEmpty = view.findViewById(R.id.layout_tensi_empty);
        tvTensiPressure = view.findViewById(R.id.tv_tensi_pressure);
        tvTensiPulse = view.findViewById(R.id.tv_tensi_pulse);
        tvTensiDate = view.findViewById(R.id.tv_tensi_date);
        btnLihatSemuaTensi = view.findViewById(R.id.btn_lihat_semua_tensi);
        btnCatatTensi = view.findViewById(R.id.btn_catat_tensi);

        signOut.setOnClickListener(v -> {
            showLogoutDialog();
        });

        // "Lihat Semua" skrining -> tab riwayat tes lengkap (CheckFragment)
        btnLihatSemuaSkrining.setOnClickListener(v -> {
            Fragment checkFragment = CheckFragment.newInstance();
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment_activity_main, checkFragment)
                    .addToBackStack(null)
                    .commit();
        });
        btnMulaiTesSkrining.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), QuizInstructionActivity.class)));

        // "Lihat Semua" tensi & CTA catat tensi -> halaman Tekanan Darah
        btnLihatSemuaTensi.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), BloodPressureActivity.class)));
        btnCatatTensi.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), BloodPressureActivity.class)));

        setupSkriningSummary();
        setupTensiSummary();

        getData();
        return view;

    }

    private void setupSkriningSummary() {
        quizViewModel.getQuizHistoryLiveData().observe(getViewLifecycleOwner(), quizHistoryItems -> {
            if (quizHistoryItems != null && !quizHistoryItems.isEmpty()) {
                // Item pertama = paling baru (sudah di-sort desc di QuizRepository)
                QuizHistoryItem latest = quizHistoryItems.get(0);
                tvSkriningKlasifikasi.setText(latest.getClassifiedScore());
                tvSkriningScore.setText(getString(R.string.tes_skrining) + " - Skor: " + latest.getTotalScore());
                tvSkriningDate.setText(latest.getDate());
                layoutSkriningFilled.setVisibility(View.VISIBLE);
                layoutSkriningEmpty.setVisibility(View.GONE);
            } else {
                layoutSkriningFilled.setVisibility(View.GONE);
                layoutSkriningEmpty.setVisibility(View.VISIBLE);
            }
        });
        if (userId != null) {
            quizViewModel.fetchQuizHistory(userId);
        }
    }

    private void setupTensiSummary() {
        bloodPresViewModel.getLatestBloodPressureData().observe(getViewLifecycleOwner(), latest -> {
            if (latest != null) {
                tvTensiPressure.setText(latest.getBloodPressure() + " mmHg");
                tvTensiPulse.setText("Nadi: " + latest.getPulse() + " bpm");
                tvTensiDate.setText(latest.getBpDate());
                layoutTensiFilled.setVisibility(View.VISIBLE);
                layoutTensiEmpty.setVisibility(View.GONE);
            } else {
                layoutTensiFilled.setVisibility(View.GONE);
                layoutTensiEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    public void showLogoutDialog() {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.confirm_logout_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        dialog.show();

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.custom_corner_rounded);

        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(params);

        Button buttonConfirm = dialogView.findViewById(R.id.btn_logout_confirmed);
        Button buttonCancel = dialogView.findViewById(R.id.btn_cancel_logout);

        buttonConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileViewModel.signOut();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                dialog.dismiss();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    public void getData(){
        profileViewModel.getUserLiveData().observe(getViewLifecycleOwner(), new Observer<User>() {
            @Override
            public void onChanged(User user) {
                if (user != null) {
                    Log.d("ProfileHealthFragment", "User data received: " + user.toString());
                    tvCaregiver.setText(user.getCaregiver());
                    tvMaritalStatus.setText(user.getMaritalStatus());
                    profileMedHistory(user.getMedHistory());
                } else {
                    Log.d("ProfileHealthFragment", "User data is null");
                }
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        profileViewModel.fetchUser();
        getData();
        if (userId != null) {
            quizViewModel.fetchQuizHistory(userId);
        }
        if (profileMedHistory != null) {
            profileMedHistory.post(() -> {
                profileMedHistory.scrollTo(0, 0);
            });
        }
    }

    private void profileMedHistory(List<inputMedHistory> medHistory){
        if (medHistory == null || medHistory.isEmpty()) {
            Log.d("ProfileHealthFragment", "Medical history is null or empty");
            return;
        }

        profileMedHistory.removeAllViews();

        for (inputMedHistory history : medHistory) {
            Log.d("ProfileHealthFragment", "Med history: " + history.toString());
            View itemView = getLayoutInflater().inflate(R.layout.profile_view_medhistory, profileMedHistory, false);

            TextView tvPenyakit = itemView.findViewById(R.id.tv_profile_penyakit);
            TextView tvMedYears = itemView.findViewById(R.id.tv_profile_tahun);
            TextView tvMedMonths = itemView.findViewById(R.id.tv_profile_bulan);

            tvPenyakit.setText(history.getPenyakit());
            tvMedYears.setText(history.getLamanya());
            tvMedMonths.setText(history.getLamanyaBulan());

            profileMedHistory.addView(itemView);
        }
    }
}