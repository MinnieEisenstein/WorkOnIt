package com.example.workonit;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Locale;

public class AddGoalActivity extends AppCompatActivity {

    // progress (works with linearprogressindicator or progressbar)
    private View progressSteps;

    // step containers
    private View step1Card, step2Card, step3Card;

    // step 1: kind + duration
    private MaterialButtonToggleGroup groupGoalKind;
    private MaterialButton btnKindStart, btnKindStop;

    private ChipGroup chipsDuration;
    private Chip chip1Week, chip1Month, chipCustom;

    private LinearLayout rowCustomDuration;
    private TextInputEditText inputCustomNumber, inputCustomUnit;

    private MaterialButton btnStep1Next;

    // step 2: why
    private TextView tvSentencePrompt, tvSentencePreview;
    private TextInputEditText inputSentenceGoal;
    private MaterialButton btnStep2Back, btnStep2Next;

    // step 3: details + save
    private TextInputEditText inputGoalName, inputNotes;
    private MaterialButton btnStep3Back, btnSave, btnCancel;

    // state
    private final Calendar startCal = startOfToday(); // still kept for any duration math
    private int durationDays = 7;                      // default 1 week
    private String durationLabel = "1 week";           // preview-only

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.add_goal_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // bind common views
        progressSteps = findViewById(R.id.progress_steps);

        step1Card = findViewById(R.id.step1_card);
        step2Card = findViewById(R.id.step2_card);
        step3Card = findViewById(R.id.step3_card);

        // ---- step 1 ----
        groupGoalKind = findViewById(R.id.group_goal_kind);
        btnKindStart  = findViewById(R.id.btn_kind_start);
        btnKindStop   = findViewById(R.id.btn_kind_stop);
        groupGoalKind.check(R.id.btn_kind_start); // default = start

        // exact button text per your wording
        if (btnKindStart != null) btnKindStart.setText("I want to make this a part of my life");
        if (btnKindStop  != null) btnKindStop.setText("I want to stop doing this");

        chipsDuration = findViewById(R.id.chips_duration);
        chip1Week     = findViewById(R.id.chip_1_week);
        chip1Month    = findViewById(R.id.chip_1_month);
        chipCustom    = findViewById(R.id.chip_custom);

        rowCustomDuration = findViewById(R.id.row_custom_duration);
        inputCustomNumber = findViewById(R.id.input_custom_number);
        inputCustomUnit   = findViewById(R.id.input_custom_unit);

        btnStep1Next = findViewById(R.id.btn_step1_next);

        // default duration = 1 week
        chip1Week.setChecked(true);
        rowCustomDuration.setVisibility(View.GONE);

        chipsDuration.setOnCheckedStateChangeListener((group, checkedIds) -> {
            boolean custom = checkedIds.contains(R.id.chip_custom);
            rowCustomDuration.setVisibility(custom ? View.VISIBLE : View.GONE);

            if (checkedIds.contains(R.id.chip_1_week)) {
                durationDays = 7;
                durationLabel = "1 week";
            } else if (checkedIds.contains(R.id.chip_1_month)) {
                durationDays = 30;
                durationLabel = "1 month";
            } else if (custom) {
                durationLabel = "custom duration"; // finalized on next
            }
            updateWhyPrompt();
        });

        btnStep1Next.setOnClickListener(v -> {
            // validate/parse custom duration if picked
            if (chipCustom.isChecked()) {
                String numStr = getText(inputCustomNumber);
                String unitStr = getText(inputCustomUnit).toLowerCase(Locale.US);

                int n = safeInt(numStr);
                if (n <= 0) {
                    inputCustomNumber.setError("enter a number");
                    inputCustomNumber.requestFocus();
                    return;
                }
                if (unitStr.isEmpty()) unitStr = "weeks";

                // support days / weeks / months / years (new)
                if (unitStr.startsWith("day")) {
                    durationDays = n;
                    durationLabel = plural(n, "day");
                } else if (unitStr.startsWith("week")) {
                    durationDays = n * 7;
                    durationLabel = plural(n, "week");
                } else if (unitStr.startsWith("month")) {
                    durationDays = n * 30;
                    durationLabel = plural(n, "month");
                } else if (unitStr.startsWith("year")) {
                    durationDays = n * 365;
                    durationLabel = plural(n, "year");
                } else {
                    // default to weeks if unknown
                    durationDays = n * 7;
                    durationLabel = plural(n, "week");
                }
            }

            showStep(2);
            updateWhyPrompt();
            updatePreview();
        });

        // ---- step 2 (why) ----
        tvSentencePrompt  = findViewById(R.id.tv_sentence_prompt);
        tvSentencePreview = findViewById(R.id.tv_sentence_preview);
        inputSentenceGoal = findViewById(R.id.input_sentence_goal);
        btnStep2Back      = findViewById(R.id.btn_step2_back);
        btnStep2Next      = findViewById(R.id.btn_step2_next);

        inputSentenceGoal.addTextChangedListener(simpleWatcher(this::updatePreview));

        btnStep2Back.setOnClickListener(v -> showStep(1));
        btnStep2Next.setOnClickListener(v -> showStep(3));

        // ---- step 3 (name + notes) ----
        inputGoalName = findViewById(R.id.input_goal_name);
        inputNotes    = findViewById(R.id.input_notes);

        btnStep3Back = findViewById(R.id.btn_step3_back);
        btnSave      = findViewById(R.id.btn_save);
        btnCancel    = findViewById(R.id.btn_cancel);

        btnStep3Back.setOnClickListener(v -> showStep(2));
        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = getText(inputGoalName);
            if (TextUtils.isEmpty(name)) {
                inputGoalName.setError("required");
                inputGoalName.requestFocus();
                return;
            }
            String notes = getText(inputNotes);
            String why   = getText(inputSentenceGoal);

            // goal type based on toggle (positive=start, negative=stop)
            String goalType = (groupGoalKind.getCheckedButtonId() == R.id.btn_kind_start)
                    ? "positive" : "negative";

            Intent result = new Intent();
            result.putExtra("goal_name", name);
            result.putExtra("goal_type", goalType);
            result.putExtra("goal_notes", notes);
            result.putExtra("goal_why", why);
            result.putExtra("goal_duration_days", durationDays);
            result.putExtra("goal_kind_label", goalType); // optional label

            setResult(RESULT_OK, result);
            finish();
        });

        // start at step 1
        showStep(1);
        updateWhyPrompt();
        updatePreview();
    }

    // -------- helpers --------

    private void showStep(int step) {
        step1Card.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        step2Card.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        step3Card.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        setStepProgress(progressSteps, step == 1 ? 33 : (step == 2 ? 66 : 100));
    }

    // progress helper for both linearprogressindicator and progressbar
    private static void setStepProgress(View progressView, int percent) {
        if (progressView == null) return;
        if (progressView instanceof LinearProgressIndicator) {
            LinearProgressIndicator lpi = (LinearProgressIndicator) progressView;
            lpi.setIndeterminate(false);
            lpi.setMax(100);
            lpi.setProgress(percent);
        } else if (progressView instanceof android.widget.ProgressBar) {
            android.widget.ProgressBar pb = (android.widget.ProgressBar) progressView;
            pb.setIndeterminate(false);
            pb.setMax(100);
            pb.setProgress(percent);
        }
    }

    private void updateWhyPrompt() {
        if (tvSentencePrompt == null) return;
        boolean start = groupGoalKind.getCheckedButtonId() == R.id.btn_kind_start;
        String verb = start ? "start" : "break";
        tvSentencePrompt.setText("Why do you want to " + verb + " this habit?");
    }

    private void updatePreview() {
        if (tvSentencePreview == null) return;
        String why = getText(inputSentenceGoal);
        if (TextUtils.isEmpty(why)) {
            tvSentencePreview.setText("Preview: (your why will appear here)");
        } else {
            tvSentencePreview.setText("Preview: " + why);
        }
    }

    private static Calendar startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static String plural(int n, String unit) {
        return n + " " + (n == 1 ? unit : unit + "s");
    }

    private static String getText(TextInputEditText et) {
        return et != null && et.getText() != null ? et.getText().toString().trim() : "";
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    private static TextWatcher simpleWatcher(Runnable onChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChange.run(); }
            @Override public void afterTextChanged(Editable s) {}
        };
    }
}