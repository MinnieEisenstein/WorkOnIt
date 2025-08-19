package com.example.workonit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

public class AddGoalActivity extends AppCompatActivity {

    private TextInputEditText inputGoalName;
    private RadioGroup groupGoalType;
    private RadioButton radioPositive, radioNegative;
    private TextInputEditText inputTimesPerWeek;
    private MaterialButton btnCancel, btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        // toolbar as action bar + back arrow
        MaterialToolbar toolbar = findViewById(R.id.add_goal_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // inputs
        inputGoalName = findViewById(R.id.input_goal_name);
        groupGoalType = findViewById(R.id.group_goal_type);
        radioPositive = findViewById(R.id.radio_positive);
        radioNegative = findViewById(R.id.radio_negative);
        inputTimesPerWeek = findViewById(R.id.input_times_per_week);

        // buttons
        btnCancel = findViewById(R.id.btn_cancel);
        btnSave = findViewById(R.id.btn_save);

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = inputGoalName.getText() != null ? inputGoalName.getText().toString().trim() : "";
            String timesStr = inputTimesPerWeek.getText() != null ? inputTimesPerWeek.getText().toString().trim() : "";
            String type = radioPositive.isChecked() ? "positive" : "negative";

            if (TextUtils.isEmpty(name)) {
                inputGoalName.setError("required");
                inputGoalName.requestFocus();
                return;
            }

            int times = 0;
            try { times = Integer.parseInt(timesStr); } catch (Exception ignored) {}

            // pack result
            Intent result = new Intent();
            result.putExtra("goal_name", name);
            result.putExtra("goal_type", type);
            result.putExtra("times_per_week", times);

            setResult(RESULT_OK, result);
            finish();
        });
    }
}
