package com.example.workonit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.example.workonit.model.Goal;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProgressCalendarActivity extends AppCompatActivity {

    private static final String PREFS = "workonit_prefs";
    private static final String KEY_RATING_PREFIX = "rating_";

    private Goal goal;
    private String selectedKey;
    private TextView tvSelectedDate, tvRatingResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress_calendar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_calendar);
        toolbar.setNavigationOnClickListener(v -> finish());

        goal = (Goal) getIntent().getSerializableExtra("goal");

        tvSelectedDate = findViewById(R.id.tv_selected_date);
        tvRatingResult = findViewById(R.id.tv_rating_result);

        CalendarView calendarView = findViewById(R.id.calendarView);

        // default to today on open
        Calendar today = Calendar.getInstance();
        calendarView.setDate(today.getTimeInMillis(), false, true);
        updateSelectedDate(today);

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth);
            updateSelectedDate(cal);
        });

        MaterialButton btnRateThisDay = findViewById(R.id.btn_rate_this_day);
        btnRateThisDay.setOnClickListener(v -> {
            if (goal != null && selectedKey != null) {
                Intent i = new Intent(this, RateProgressActivity.class);
                i.putExtra("goal", goal);
                i.putExtra("dateKey", selectedKey); // pass which day we’re rating
                startActivity(i);
            }
        });
    }

    private void updateSelectedDate(Calendar cal) {
        Date date = cal.getTime();

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        selectedKey = fmt.format(date);

        SimpleDateFormat niceFmt = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvSelectedDate.setText("Selected: " + niceFmt.format(date));

        loadRatingsForDate(selectedKey);
    }

    private void loadRatingsForDate(String dateKey) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        int diff = prefs.getInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_diff", -1);
        int moti = prefs.getInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_moti", -1);
        int succ = prefs.getInt(KEY_RATING_PREFIX + goal.name + "_" + dateKey + "_succ", -1);

        if (diff == -1 && moti == -1 && succ == -1) {
            tvRatingResult.setText("No rating saved for this date");
        } else {
            tvRatingResult.setText("Difficulty: " + diff +
                    " | Motivation: " + moti +
                    " | Success: " + succ);
        }
    }
}