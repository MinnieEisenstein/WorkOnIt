package com.example.workonit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.workonit.model.Goal;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.workonit.net.ChizzukService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.ClipboardManager;
import android.content.ClipData;

public class GoalDetailActivity extends AppCompatActivity {

    private static final String PREFS = "workonit_prefs";
    private static final String KEY_NOTES_PREFIX = "notes_";
    private static final String KEY_PROGRESS_PREFIX = "progress_";

    private Goal goal;
    private EditText etNotes;
    private CheckBox cbDoneToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goal_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_detail);
        toolbar.setNavigationOnClickListener(v -> finish());

        goal = (Goal) getIntent().getSerializableExtra("goal");

        TextView tvName = findViewById(R.id.tv_goal_name);
        TextView tvMeta = findViewById(R.id.tv_goal_meta);
        etNotes = findViewById(R.id.et_notes);
        cbDoneToday = findViewById(R.id.cb_done_today);
        MaterialButton btnChizzuk = findViewById(R.id.btn_chizzuk);
        MaterialButton btnRate = findViewById(R.id.btn_rate_progress);
        MaterialButton btnCalendar = findViewById(R.id.btn_view_calendar); // ✅ new button

        if (goal != null) {
            tvName.setText(goal.name);

            String typeText = (goal.type == Goal.Type.POSITIVE) ? "positive" : "negative";
            String freqText = (goal.timesPerWeek > 0) ? (goal.timesPerWeek + "/week") : "no target";
            String dateText = DateFormat.getDateInstance().format(new Date(goal.createdAtUtc));

            tvMeta.setText(typeText + " • " + freqText + " • created " + dateText);

            // restore notes + checkbox state
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            String savedNotes = prefs.getString(KEY_NOTES_PREFIX + goal.name, "");
            etNotes.setText(savedNotes);

            String todayKey = getTodayKey();
            boolean doneToday = prefs.getBoolean(KEY_PROGRESS_PREFIX + goal.name + "_" + todayKey, false);
            cbDoneToday.setChecked(doneToday);
        }

        // save notes when leaving focus
        findViewById(R.id.et_notes).setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && goal != null) {
                saveNotes();
            }
        });

        // save checkbox immediately when clicked
        cbDoneToday.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (goal != null) {
                SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                String todayKey = getTodayKey();
                prefs.edit()
                        .putBoolean(KEY_PROGRESS_PREFIX + goal.name + "_" + todayKey, isChecked)
                        .apply();
            }
        });

        // ai chizzuk button → stub (next step will call api)
        btnChizzuk.setOnClickListener(v -> startGenerateChizzuk(v));

        btnRate.setOnClickListener(v -> {
            if (goal != null) {
                Intent i = new Intent(this, RateProgressActivity.class);
                i.putExtra("goal", goal);
                startActivity(i);
            }
        });

        btnCalendar.setOnClickListener(v -> {
            if (goal != null) {
                Intent i = new Intent(this, ProgressCalendarActivity.class);
                i.putExtra("goal", goal);
                startActivity(i);
            }
        });
    }

    private void saveNotes() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_NOTES_PREFIX + goal.name, etNotes.getText().toString())
                .apply();
    }

    private String getTodayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
    }

    // stub for ai chizzuk – next step we’ll do the network call here
    private void startGenerateChizzuk(View anchor) {
        MaterialButton btn = findViewById(R.id.btn_chizzuk);
        btn.setEnabled(false);
        btn.setText("Generating…");

        String notes = etNotes.getText() != null ? etNotes.getText().toString() : "";
        String goalName = (goal != null && goal.name != null) ? goal.name : "my goal";
        boolean isPositive = goal != null && goal.type == Goal.Type.POSITIVE;

        Snackbar.make(anchor, "generating chizzuk…", Snackbar.LENGTH_SHORT).show();

        ChizzukService.generateChizzuk(
                GoalDetailActivity.this,
                goalName,
                isPositive,
                notes,
                new ChizzukService.ChizzukCallback() {
                    @Override public void onSuccess(String message) {
                        btn.setEnabled(true);
                        btn.setText("Get Chizzuk");

                        new MaterialAlertDialogBuilder(GoalDetailActivity.this)
                                .setTitle("AI Chizzuk")
                                .setMessage(message)
                                .setPositiveButton("copy", (d, w) -> {
                                    ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    if (cm != null) {
                                        cm.setPrimaryClip(ClipData.newPlainText("chizzuk", message));
                                        Snackbar.make(anchor, "copied", Snackbar.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton("close", null)
                                .show();
                    }

                    @Override public void onError(String error) {
                        btn.setEnabled(true);
                        btn.setText("Get Chizzuk 💪");
                        Snackbar.make(anchor, "error: " + error, Snackbar.LENGTH_LONG).show();
                    }
                }
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (goal != null && etNotes != null) {
            // ensure latest text is persisted even if user hits back immediately
            saveNotes();
        }
    }
}