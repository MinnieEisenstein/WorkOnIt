package ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workonit.R;
import com.example.workonit.model.Goal;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// minimal adapter; click handling will come when we add the detail screen
public class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.VH> {

    public interface OnGoalClick {
        void onGoalClick(Goal goal);
    }

    private final List<Goal> items = new ArrayList<>();
    private OnGoalClick onGoalClick;

    public void setOnGoalClick(OnGoalClick listener) {
        this.onGoalClick = listener;
    }

    public void submitList(List<Goal> goals) {
        items.clear();
        if (goals != null) items.addAll(goals);
        notifyDataSetChanged();
    }

    @NonNull
    @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_goal, parent, false);
        return new VH(v);
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        final Goal g = items.get(position);

        h.tvTitle.setText(g.name);

        // build a simple meta line like: "positive • 3/week • created Aug 17, 2025"
        String typeText = (g.type == Goal.Type.POSITIVE) ? "positive" : "negative";
        String freqText = (g.timesPerWeek > 0) ? (g.timesPerWeek + "/week") : "no target yet";
        String dateText = DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(new Date(g.createdAtUtc));
        h.tvMeta.setText(typeText + " • " + freqText + " • created " + dateText);

        // click
        h.itemView.setOnClickListener(v -> {
            if (onGoalClick != null) onGoalClick.onGoalClick(g);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvMeta;
        final TextView tvProgressHint;
        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvProgressHint = itemView.findViewById(R.id.tv_progress_hint);
        }
    }
}