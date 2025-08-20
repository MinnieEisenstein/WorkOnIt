package com.example.workonit;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    // prefs bucket
    private static final String PREFS = "workonit_prefs";

    // quote mode keys (kept same names for compatibility)
    private static final String PREF_HOURLY_QUOTES = "pref_hourly_quotes";
    private static final String PREF_DAILY_QUOTE  = "pref_daily_quote";

    // quote cache keys (so MainActivity fetches a fresh one after change)
    private static final String PREF_QUOTE_CACHE_TEXT = "quote_cache_text";
    private static final String PREF_QUOTE_CACHE_KEY  = "quote_cache_key";

    // ui
    private MaterialSwitch switchHourly;
    private MaterialSwitch switchDaily;

    // prevent infinite toggle loops
    private boolean isUpdatingSwitches = false;

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
        switchHourly = findViewById(R.id.switch_hourly_quotes);
        switchDaily  = findViewById(R.id.switch_daily_quote);

        // load prefs
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean hourly = prefs.getBoolean(PREF_HOURLY_QUOTES, false);
        boolean daily  = prefs.getBoolean(PREF_DAILY_QUOTE,  true);

        // normalize: if both same, default to daily
        if (hourly == daily) {
            hourly = false;
            daily  = true;
            prefs.edit()
                    .putBoolean(PREF_HOURLY_QUOTES, false)
                    .putBoolean(PREF_DAILY_QUOTE, true)
                    .apply();
        }

        // set initial ui
        switchHourly.setChecked(hourly);
        switchDaily.setChecked(daily);

        // listeners (mutually exclusive)
        switchHourly.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isUpdatingSwitches) return;
            isUpdatingSwitches = true;

            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            if (isChecked) {
                switchDaily.setChecked(false);
                p.edit()
                        .putBoolean(PREF_HOURLY_QUOTES, true)
                        .putBoolean(PREF_DAILY_QUOTE, false)
                        .remove(PREF_QUOTE_CACHE_KEY)
                        .remove(PREF_QUOTE_CACHE_TEXT)
                        .apply();
            } else {
                switchDaily.setChecked(true);
                p.edit()
                        .putBoolean(PREF_HOURLY_QUOTES, false)
                        .putBoolean(PREF_DAILY_QUOTE, true)
                        .remove(PREF_QUOTE_CACHE_KEY)
                        .remove(PREF_QUOTE_CACHE_TEXT)
                        .apply();
            }

            isUpdatingSwitches = false;
        });

        switchDaily.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isUpdatingSwitches) return;
            isUpdatingSwitches = true;

            SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
            if (isChecked) {
                switchHourly.setChecked(false);
                p.edit()
                        .putBoolean(PREF_DAILY_QUOTE, true)
                        .putBoolean(PREF_HOURLY_QUOTES, false)
                        .remove(PREF_QUOTE_CACHE_KEY)
                        .remove(PREF_QUOTE_CACHE_TEXT)
                        .apply();
            } else {
                switchHourly.setChecked(true);
                p.edit()
                        .putBoolean(PREF_DAILY_QUOTE, false)
                        .putBoolean(PREF_HOURLY_QUOTES, true)
                        .remove(PREF_QUOTE_CACHE_KEY)
                        .remove(PREF_QUOTE_CACHE_TEXT)
                        .apply();
            }

            isUpdatingSwitches = false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}