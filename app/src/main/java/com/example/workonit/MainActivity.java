package com.example.workonit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workonit.model.Goal;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import ui.GoalsAdapter;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> addGoalLauncher;
    private GoalsAdapter adapter;
    private RecyclerView rv;
    private TextView emptyView;
    private final List<Goal> goals = new ArrayList<>();

    private static final String PREFS = "workonit_prefs";
    private static final String PREF_HOURLY_QUOTES = "pref_hourly_quotes";
    private static final String PREF_DAILY_QUOTE  = "pref_daily_quote";

    // cache keys
    private static final String PREF_QUOTE_CACHE_TEXT = "quote_cache_text";
    private static final String PREF_QUOTE_CACHE_KEY  = "quote_cache_key";

    // ***** DEMO ONLY: store your key in local.properties → OPENAI_API_KEY=sk-... then read via BuildConfig *****
    // TODO: move this to your own backend to avoid shipping an API key inside the app.
    private static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY; // define in gradle
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini"; // small, fast, good enough for short quotes

    // UI + schedule
    private TextView quoteText;
    private final Handler quoteHandler = new Handler();
    private final Runnable quoteTick = new Runnable() {
        @Override public void run() {
            refreshQuote();
            quoteHandler.postDelayed(this, millisUntilNextBoundary());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        quoteText = findViewById(R.id.quote_text);

        rv = findViewById(R.id.recycler_goals);
        emptyView = findViewById(R.id.empty_view);

        adapter = new GoalsAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        loadGoals();
        updateList(goals);

        // quick actions from the 3‑dot menu
        adapter.setOnGoalAction(new GoalsAdapter.OnGoalAction() {
            @Override public void onActivate(Goal goal) {
                goal.markActive();
                saveGoals(); updateList(goals);
                Snackbar.make(findViewById(R.id.root_coordinator), "Marked active", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onHold(Goal goal) {
                goal.markOnHold();
                saveGoals(); updateList(goals);
                Snackbar.make(findViewById(R.id.root_coordinator), "Put on hold", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onComplete(Goal goal) {
                goal.markCompleted();
                saveGoals(); updateList(goals);
                Snackbar.make(findViewById(R.id.root_coordinator), "Marked completed", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onExpire(Goal goal) {
                goal.markExpired();
                saveGoals(); updateList(goals);
                Snackbar.make(findViewById(R.id.root_coordinator), "Marked expired", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onDelete(Goal goal) {
                goals.remove(goal);
                saveGoals(); updateList(goals);
                Snackbar.make(findViewById(R.id.root_coordinator), "Deleted", Snackbar.LENGTH_SHORT).show();
            }
        });

        adapter.setOnGoalClick(goal -> {
            Intent intent = new Intent(MainActivity.this, GoalDetailActivity.class);
            intent.putExtra("goal", goal);
            startActivity(intent);
        });

        // big FAB → add goal
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, AddGoalActivity.class);
                addGoalLauncher.launch(i);
            });
        }

        addGoalLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String name  = data.getStringExtra("goal_name");
                        String type  = data.getStringExtra("goal_type");
                        int times    = data.getIntExtra("times_per_week", 0);

                        Goal.Type goalType = "positive".equalsIgnoreCase(type)
                                ? Goal.Type.POSITIVE : Goal.Type.NEGATIVE;

                        Goal newGoal = new Goal(name, goalType, times, System.currentTimeMillis());
                        goals.add(newGoal);
                        saveGoals();
                        updateList(goals);

                        Snackbar.make(findViewById(R.id.root_coordinator),
                                "added: " + name, Snackbar.LENGTH_LONG).show();
                    }
                }
        );

        // --- NEW: category buttons ---
        findViewById(R.id.btn_on_hold).setOnClickListener(v ->
                openFiltered(Goal.Status.ON_HOLD, "On‑hold goals"));
        findViewById(R.id.btn_completed).setOnClickListener(v ->
                openFiltered(Goal.Status.COMPLETED, "Completed goals"));
        findViewById(R.id.btn_expired).setOnClickListener(v ->
                openFiltered(Goal.Status.EXPIRED, "Expired goals"));
        // -----------------------------
    }

    private void openFiltered(Goal.Status status, String title) {
        Intent i = new Intent(this, FilteredGoalsActivity.class); // we create next
        i.putExtra("status", status.name());
        i.putExtra("title", title);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshQuote();
        quoteHandler.removeCallbacks(quoteTick);
        quoteHandler.postDelayed(quoteTick, millisUntilNextBoundary());
    }

    @Override
    protected void onPause() {
        super.onPause();
        quoteHandler.removeCallbacks(quoteTick);
    }

    // ===== Quote logic (AI) =====

    private enum Period { HOUR, DAY }

    private Period getPeriodFromPrefs() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean hourly = p.getBoolean(PREF_HOURLY_QUOTES, false);
        boolean daily  = p.getBoolean(PREF_DAILY_QUOTE,  true);
        return hourly ? Period.HOUR : (daily ? Period.DAY : Period.DAY);
    }

    private String currentKey() {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int day = c.get(Calendar.DAY_OF_YEAR);
        if (getPeriodFromPrefs() == Period.HOUR) {
            int hr = c.get(Calendar.HOUR_OF_DAY);
            return String.format(Locale.US, "Y%04d-D%03d-H%02d", year, day, hr);
        } else {
            return String.format(Locale.US, "Y%04d-D%03d", year, day);
        }
    }

    private long millisUntilNextBoundary() {
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        if (getPeriodFromPrefs() == Period.HOUR) {
            next.add(Calendar.HOUR_OF_DAY, 1);
            next.set(Calendar.MINUTE, 0);
        } else {
            next.add(Calendar.DAY_OF_YEAR, 1);
            next.set(Calendar.HOUR_OF_DAY, 0);
            next.set(Calendar.MINUTE, 0);
        }
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        long diff = next.getTimeInMillis() - now.getTimeInMillis();
        return Math.max(1000L, diff);
    }

    private void refreshQuote() {
        if (quoteText == null) return;

        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String keyWanted = currentKey();
        String cachedKey = p.getString(PREF_QUOTE_CACHE_KEY, null);
        String cachedText = p.getString(PREF_QUOTE_CACHE_TEXT, null);

        if (cachedText != null && keyWanted.equals(cachedKey)) {
            quoteText.setText(cachedText);
            return;
        }

        quoteText.setText("…thinking of something uplifting…");

        // fetch on background thread (no external libs)
        new Thread(() -> {
            String ai = fetchAiQuote();
            if (ai == null || ai.trim().isEmpty()) {
                ai = "Trust in Hashem and keep doing good.";
            }
            String finalAi = ai.trim();

            // cache
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_QUOTE_CACHE_KEY, keyWanted)
                    .putString(PREF_QUOTE_CACHE_TEXT, finalAi)
                    .apply();

            runOnUiThread(() -> {
                if (!isFinishing() && quoteText != null) {
                    quoteText.setText(finalAi);
                }
            });
        }).start();
    }

    private String fetchAiQuote() {
        try {
            String system = "You generate brief, wholesome, motivational quotes suitable for a frum/Jewish audience. " +
                    "Keep to 1-2 sentences. No emojis. Respect modesty and general Torah values, but the quote can be from a secular source. Make sure to credit the person it is quoted from.";
            String user = "Give one original motivational quote. If you echo sources, paraphrase with attribution.";

            JSONObject root = new JSONObject();
            root.put("model", OPENAI_MODEL);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", system));
            messages.put(new JSONObject().put("role", "user").put("content", user));
            root.put("messages", messages);
            root.put("temperature", 0.8);

            URL url = new URL(OPENAI_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(10000);
            con.setReadTimeout(15000);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            con.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);

            OutputStream os = new BufferedOutputStream(con.getOutputStream());
            os.write(root.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = con.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 200 && code < 300 ? con.getInputStream() : con.getErrorStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();

            if (code >= 200 && code < 300) {
                JSONObject resp = new JSONObject(sb.toString());
                JSONArray choices = resp.optJSONArray("choices");
                if (choices != null && choices.length() > 0) {
                    JSONObject msg = choices.getJSONObject(0).getJSONObject("message");
                    String content = msg.optString("content", "").trim();
                    if (content.contains("\n")) content = content.split("\n")[0].trim();
                    if (content.length() > 120) content = content.substring(0, 120).trim();
                    return content;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    // ===== list helpers =====

    private void updateList(List<Goal> goals) {
        adapter.submitList(goals);
        if (goals == null || goals.isEmpty()) {
            rv.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            rv.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }
    }

    private void saveGoals() {
        String json = new Gson().toJson(goals);
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString("all_goals", json)
                .apply();
    }

    private void loadGoals() {
        String json = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getString("all_goals", null);
        if (json != null) {
            Type type = new TypeToken<List<Goal>>() {}.getType();
            List<Goal> loaded = new Gson().fromJson(json, type);
            goals.clear();
            goals.addAll(loaded);
        }
    }

    // ===== Menu =====

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}