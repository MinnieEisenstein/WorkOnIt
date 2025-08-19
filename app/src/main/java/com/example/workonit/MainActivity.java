package com.example.workonit;



import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ui.GoalsAdapter;
import com.example.workonit.model.Goal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> addGoalLauncher;
    private GoalsAdapter adapter;
    private RecyclerView rv;
    private TextView emptyView;
    private final List<Goal> goals = new ArrayList<>();
    private static final String PREFS = "workonit_prefs";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // recycler setup
        rv = findViewById(R.id.recycler_goals);
        emptyView = findViewById(R.id.empty_view);

        adapter = new GoalsAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        loadGoals();


        updateList(goals);

        adapter.setOnGoalClick(goal -> {
            Intent intent = new Intent(MainActivity.this, GoalDetailActivity.class);
            intent.putExtra("goal", goal);
            startActivity(intent);
        });

        addGoalLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        String name  = data.getStringExtra("goal_name");
                        String type  = data.getStringExtra("goal_type");
                        int times    = data.getIntExtra("times_per_week", 0);

                        Goal.Type goalType = type.equalsIgnoreCase("positive")
                                ? Goal.Type.POSITIVE
                                : Goal.Type.NEGATIVE;

                        Goal newGoal = new Goal(name, goalType, times, System.currentTimeMillis());
                        goals.add(newGoal);
                        saveGoals();
                        updateList(goals);

                        Snackbar.make(findViewById(R.id.root_coordinator),
                                "added: " + name, Snackbar.LENGTH_LONG).show();
                    }
                }
        );

        // fab → open AddGoalActivity
        FloatingActionButton fab = findViewById(R.id.fab_add);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, AddGoalActivity.class);
                addGoalLauncher.launch(i);
            });
        }
    }

    private void updateList(List<Goal> goals) {
        adapter.submitList(goals);
        if (goals == null || goals.isEmpty()) {
            rv.setVisibility(RecyclerView.GONE);
            emptyView.setVisibility(TextView.VISIBLE);
        } else {
            rv.setVisibility(RecyclerView.VISIBLE);
            emptyView.setVisibility(TextView.GONE);
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