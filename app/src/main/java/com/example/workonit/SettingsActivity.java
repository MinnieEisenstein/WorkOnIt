package com.example.workonit;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    // prefs keys
    private static final String PREFS = "workonit_prefs";
    private static final String PREF_HOURLY_QUOTES = "pref_hourly_quotes";
    private static final String PREF_DAILY_QUOTE  = "pref_daily_quote";
    private static final String PREF_AUTOSAVE     = "pref_autosave";

    // ui
    private MaterialSwitch switchHourly;
    private MaterialSwitch switchDaily;
    private MaterialSwitch switchAutosave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.settings_toolbar);
        toolbar.setTitle("Settings");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // find switches
        switchHourly   = findViewById(R.id.switch_hourly_quotes);
        switchDaily    = findViewById(R.id.switch_daily_quote);
        switchAutosave = findViewById(R.id.switch_autosave);

        // load once
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean hourly   = prefs.getBoolean(PREF_HOURLY_QUOTES, false);
        boolean daily    = prefs.getBoolean(PREF_DAILY_QUOTE,  true);
        boolean autosave = prefs.getBoolean(PREF_AUTOSAVE,     true);

        // set initial ui state
        switchHourly.setChecked(hourly);
        switchDaily.setChecked(daily);
        switchAutosave.setChecked(autosave);

        // save on toggle
        switchHourly.setOnCheckedChangeListener((btn, isChecked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_HOURLY_QUOTES, isChecked)
                        .apply()
        );

        switchDaily.setOnCheckedChangeListener((btn, isChecked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_DAILY_QUOTE, isChecked)
                        .apply()
        );

        switchAutosave.setOnCheckedChangeListener((btn, isChecked) ->
                getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                        .putBoolean(PREF_AUTOSAVE, isChecked)
                        .apply()
        );
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}