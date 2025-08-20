package com.example.workonit;

import static com.example.workonit.BuildConfig.OPENAI_API_KEY;

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

    // carry notes/why into detail view storage
    private static final String KEY_NOTES_PREFIX = "notes_";
    private static final String KEY_WHY_PREFIX   = "why_";


    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private static final String OPENAI_MODEL = "gpt-4o-mini";

    // UI + schedule
    private TextView quoteText;
    private final Handler quoteHandler = new Handler();
    private final Runnable quoteTick = new Runnable() {
        @Override public void run() {
            refreshQuote(); // make sure we’re showing the correct (hour/day) quote right now
            quoteHandler.postDelayed(this, millisUntilNextBoundary()); // schedule next swap
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

        // list + adapter
        adapter = new GoalsAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // item clicks
        adapter.setOnGoalClick(goal -> {
            Intent intent = new Intent(MainActivity.this, GoalDetailActivity.class);
            intent.putExtra("goal", goal);
            startActivity(intent);
        });

        // 3‑dot actions
        adapter.setOnGoalAction(new GoalsAdapter.OnGoalAction() {
            @Override public void onActivate(Goal goal) {
                goal.markActive(); saveGoals(); updateList();
                Snackbar.make(findViewById(R.id.root_coordinator), "Marked active", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onHold(Goal goal) {
                goal.markOnHold(); saveGoals(); updateList();
                Snackbar.make(findViewById(R.id.root_coordinator), "Put on hold", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onComplete(Goal goal) {
                goal.markCompleted(); saveGoals(); updateList();
                Snackbar.make(findViewById(R.id.root_coordinator), "Marked completed", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onExpire(Goal goal) {
                goal.markExpired(); saveGoals(); updateList();
                Snackbar.make(findViewById(R.id.root_coordinator), "Marked expired", Snackbar.LENGTH_SHORT).show();
            }
            @Override public void onDelete(Goal goal) {
                goals.remove(goal); saveGoals(); updateList();
                Snackbar.make(findViewById(R.id.root_coordinator), "Deleted", Snackbar.LENGTH_SHORT).show();
            }
        });

        // load & show
        loadGoals();
        updateList();

        // FAB
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_add);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, AddGoalActivity.class);
                addGoalLauncher.launch(i);
            });
        }

        // activity result for adding goal
        addGoalLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String name  = data.getStringExtra("goal_name");
                        String type  = data.getStringExtra("goal_type");
                        String notes = data.getStringExtra("goal_notes");
                        String why   = data.getStringExtra("goal_why");
                        int durationDays = data.getIntExtra("goal_duration_days", 0); // <- from Add flow

                        Goal.Type goalType = "positive".equalsIgnoreCase(type)
                                ? Goal.Type.POSITIVE : Goal.Type.NEGATIVE;

                        // times_per_week removed → pass 0
                        Goal newGoal = new Goal(name, goalType, 0, System.currentTimeMillis());

                        //  set due date if a duration was chosen
                        if (durationDays > 0) {
                            java.util.Calendar end = java.util.Calendar.getInstance();
                            end.add(java.util.Calendar.DAY_OF_YEAR, durationDays);
                            end.set(java.util.Calendar.HOUR_OF_DAY, 23);
                            end.set(java.util.Calendar.MINUTE, 59);
                            end.set(java.util.Calendar.SECOND, 59);
                            end.set(java.util.Calendar.MILLISECOND, 0);
                            newGoal.dueAtUtc = end.getTimeInMillis();
                            newGoal.autoExpire = true; // uses your Goal.expireIfNeeded()
                        }

                        goals.add(newGoal);
                        saveGoals();

                        // persist notes + why for detail screen
                        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
                        SharedPreferences.Editor e = p.edit();
                        if (notes != null) e.putString(KEY_NOTES_PREFIX + name, notes);
                        if (why != null)   e.putString(KEY_WHY_PREFIX + name,   why);
                        e.apply();

                        updateList();

                        Snackbar.make(findViewById(R.id.root_coordinator),
                                "Added: " + name, Snackbar.LENGTH_LONG).show();
                    }
                }
        );

        // category buttons
        View btnHold = findViewById(R.id.btn_on_hold);
        View btnCompleted = findViewById(R.id.btn_completed);
        View btnExpired = findViewById(R.id.btn_expired);
        if (btnHold != null) btnHold.setOnClickListener(v -> openFiltered(Goal.Status.ON_HOLD, "On‑hold goals"));
        if (btnCompleted != null) btnCompleted.setOnClickListener(v -> openFiltered(Goal.Status.COMPLETED, "Completed goals"));
        if (btnExpired != null) btnExpired.setOnClickListener(v -> openFiltered(Goal.Status.EXPIRED, "Expired goals"));
    }

    private void openFiltered(Goal.Status status, String title) {
        Intent i = new Intent(this, FilteredGoalsActivity.class);
        i.putExtra("status", status.name());
        i.putExtra("title", title);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGoals();
        updateList();

        // refresh now
        refreshQuote();

        // reset the timer: remove any old callbacks, then post an immediate tick
        quoteHandler.removeCallbacks(quoteTick);
        quoteHandler.post(quoteTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        quoteHandler.removeCallbacks(quoteTick);
    }

    // Quote logic

    private enum Period { HOUR, DAY }

    private Period getPeriodFromPrefs() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        return p.getBoolean(PREF_HOURLY_QUOTES, false) ? Period.HOUR : Period.DAY;
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
        quoteText = findViewById(R.id.quote_text);
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

        new Thread(() -> {

            String ai = fetchAiQuote();
            if (ai == null || ai.trim().isEmpty()) {
                // rotate through local fallbacks based on the current key
                String[] fallbacks = new String[]{
                        "Small steps count. Keep going.",
                        "Your neshamah is stronger than today’s challenge.",
                        "You don’t have to finish—just don’t quit. (Pirkei Avos 2:16, paraphrased)",
                        "Focus on the next right thing.",
                        "Tiny habits → huge change over time.",
                        "You’re allowed to be new at this."
                };
                int idx = Math.abs(currentKey().hashCode()) % fallbacks.length;
                ai = fallbacks[idx];
            }
            String finalAi = ai.trim();


            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(PREF_QUOTE_CACHE_KEY, keyWanted)
                    .putString(PREF_QUOTE_CACHE_TEXT, finalAi)
                    .apply();

            runOnUiThread(() -> {
                if (!isFinishing()) {
                    TextView qt = findViewById(R.id.quote_text);
                    if (qt != null) qt.setText(finalAi);
                }
            });
        }).start();
    }

    private String fetchAiQuote() {
        try {
            String system = "You generate brief, wholesome, motivational quotes suitable for a frum/Jewish audience. " +
                    "Keep to 1–2 sentences. No emojis. Respect modesty and Torah values, but the quote can also be from a secular source. " +
                    "Always attribute if possible (paraphrased is fine).";
            String user = "Give one original motivational quote. If you echo sources, paraphrase with attribution.";

            JSONObject root = new JSONObject();
            root.put("model", OPENAI_MODEL);
            root.put("temperature", 0.8);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", system));
            messages.put(new JSONObject().put("role", "user").put("content", user));
            root.put("messages", messages);

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

        // fallback if API fails
        return randomFallback();
    }

    private String randomFallback() {
        String[] fallbacks = {
                "Small steps count. Keep going.",
                "Your neshamah is stronger than today’s challenge.",
                "You don’t have to finish—just don’t quit. (Pirkei Avos 2:16, paraphrased)",
                "Focus on the next right thing.",
                "Tiny habits → huge change over time.",
                "You’re allowed to be new at this.",
                "Start before you’re ready. (Mel Robbins)",
                "The light of one mitzvah leads to another. (Pirkei Avos 4:2)",
                "Courage is taking the next step even when you feel fear.",
                "Hashem believes in you more than you believe in yourself.",
                "Consistency beats intensity every time.",
                "Don’t compare your chapter 1 to someone else’s chapter 20."
        };
        return fallbacks[(int) (Math.random() * fallbacks.length)];
    }

    // ===== list helpers =====
    private void updateList() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        // 1) auto‑expire any goals past due
        for (Goal g : goals) {
            if (g != null && g.expireIfNeeded(now)) {
                changed = true;
            }
        }
        if (changed) {
            saveGoals(); // persist updated statuses
        }

        // 2) show ACTIVE only on main screen (others via buttons)
        List<Goal> active = new ArrayList<>();
        for (Goal g : goals) {
            if (g.status == null || g.status == Goal.Status.ACTIVE) active.add(g);
        }
        adapter.submitList(active);

        if (active.isEmpty()) {
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
            if (loaded != null) goals.addAll(loaded);
        }
    }

    // menu
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