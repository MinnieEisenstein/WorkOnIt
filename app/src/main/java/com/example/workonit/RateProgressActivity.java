package com.example.workonit;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.example.workonit.model.Goal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RateProgressActivity extends AppCompatActivity {

    private static final String PREFS = "workonit_prefs";
    private static final String KEY_RATING_PREFIX = "rating_";

    private SeekBar sbDifficulty, sbMotivation, sbSuccess;
    private TextView tvDifficultyVal, tvMotivationVal, tvSuccessVal;
    private Goal goal;

    // this will hold either today’s date OR the one passed from calendar
    private String dateKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rate_progress);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_rate);
        toolbar.setNavigationOnClickListener(v -> finish());

        goal = (Goal) getIntent().getSerializableExtra("goal");

        // check if a dateKey was passed in, otherwise default to today
        String passedDateKey = getIntent().getStringExtra("dateKey");
        if (passedDateKey != null) {
            dateKey = passedDateKey;
        } else {
            dateKey = getTodayKey();
        }

        TextView tvTitle = findViewById(R.id.tv_goal_title);
        if (goal != null) {
            tvTitle.setText("Rate progress: " + goal.name);
        }

        sbDifficulty = findViewById(R.id.sb_difficulty);
        sbMotivation = findViewById(R.id.sb_motivation);
        sbSuccess    = findViewById(R.id.sb_success);

        tvDifficultyVal = findViewById(R.id.tv_difficulty_val);
        tvMotivationVal = findViewById(R.id.tv_motivation_val);
        tvSuccessVal    = findViewById(R.id.tv_success_val);

        setupSeekBar(sbDifficulty, tvDifficultyVal);
        setupSeekBar(sbMotivation, tvMotivationVal);
        setupSeekBar(sbSuccess, tvSuccessVal);

        // restore saved values for this date if they exist
        restoreRatings();

        MaterialButton btnSubmit = findViewById(R.id.btn_submit_rating);
        btnSubmit.setOnClickListener(v -> {
            if (goal != null) {
                int diff = sbDifficulty.getProgress();
                int moti = sbMotivation.getProgress();
                int succ = sbSuccess.getProgress();

                saveRatings(diff, moti, succ);

                String msg = "Saved ratings → Difficulty: " + diff +
                        " | Motivation: " + moti +
                        " | Success: " + succ;
                Snackbar.make(v, msg, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setupSeekBar(SeekBar sb, TextView tv) {
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv.setText(String.valueOf(progress));
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void saveRatings(int diff, int moti, int succ) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_diff", diff)
                .putInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_moti", moti)
                .putInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_succ", succ)
                .apply();
    }

    private void restoreRatings() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        int diff = prefs.getInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_diff", 0);
        int moti = prefs.getInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_moti", 0);
        int succ = prefs.getInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_succ", 0);

        sbDifficulty.setProgress(diff);
        sbMotivation.setProgress(moti);
        sbSuccess.setProgress(succ);

        tvDifficultyVal.setText(String.valueOf(diff));
        tvMotivationVal.setText(String.valueOf(moti));
        tvSuccessVal.setText(String.valueOf(succ));
    }

    private String getTodayKey() {
        return new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());
    }
}