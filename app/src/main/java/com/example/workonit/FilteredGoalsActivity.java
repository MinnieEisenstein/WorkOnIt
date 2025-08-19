package com.example.workonit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workonit.model.Goal;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ui.SimpleGoalsAdapter;

public class FilteredGoalsActivity extends AppCompatActivity {

    private static final String PREFS = "workonit_prefs";

    private RecyclerView rv;
    private TextView empty;

    private Goal.Status targetStatus;
    private final List<Goal> allGoals = new ArrayList<>();
    private final List<Goal> filtered = new ArrayList<>();
    private SimpleGoalsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_filtered_goals);

        MaterialToolbar tb = findViewById(R.id.toolbar);
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String title = getIntent().getStringExtra("title");
        if (title == null) title = "Goals";
        getSupportActionBar().setTitle(title);

        String statusName = getIntent().getStringExtra("status");
        targetStatus = Goal.Status.valueOf(statusName);

        rv = findViewById(R.id.recycler);
        empty = findViewById(R.id.empty_view);

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleGoalsAdapter();
        rv.setAdapter(adapter);

        // open details on tap
        adapter.setOnGoalClick(goal -> {
            Intent intent = new Intent(FilteredGoalsActivity.this, GoalDetailActivity.class);
            intent.putExtra("goal", goal);
            startActivity(intent);
        });

        // 3‑dot actions save + refresh list
        adapter.setOnGoalAction(new SimpleGoalsAdapter.OnGoalAction() {
            @Override public void onActivate(Goal goal) { goal.markActive(); persistAndRefresh("Marked active"); }
            @Override public void onHold(Goal goal)     { goal.markOnHold(); persistAndRefresh("Put on hold"); }
            @Override public void onComplete(Goal goal) { goal.markCompleted(); persistAndRefresh("Marked completed"); }
            @Override public void onExpire(Goal goal)   { goal.markExpired(); persistAndRefresh("Marked expired"); }
            @Override public void onDelete(Goal goal)   { allGoals.remove(goal); persistAndRefresh("Deleted"); }
        });

        // initial load
        loadAllGoals();
        applyFilter();
    }

    @Override protected void onResume() {
        super.onResume();
        // re-load in case something changed from details screen
        loadAllGoals();
        applyFilter();
    }

    private void persistAndRefresh(String msg) {
        saveAllGoals();
        applyFilter();
        Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_SHORT).show();
    }

    private void applyFilter() {
        filtered.clear();
        for (Goal g : allGoals) if (g.status == targetStatus) filtered.add(g);

        adapter.submitList(new ArrayList<>(filtered));
        boolean none = filtered.isEmpty();
        rv.setVisibility(none ? View.GONE : View.VISIBLE);
        empty.setVisibility(none ? View.VISIBLE : View.GONE);
    }

    private void loadAllGoals() {
        SharedPreferences p = getSharedPreferences(PREFS, MODE_PRIVATE);
        String json = p.getString("all_goals", null);
        allGoals.clear();
        if (json != null) {
            Type t = new TypeToken<List<Goal>>(){}.getType();
            allGoals.addAll(new Gson().fromJson(json, t));
        }
    }

    private void saveAllGoals() {
        String json = new Gson().toJson(allGoals);
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString("all_goals", json)
                .apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}