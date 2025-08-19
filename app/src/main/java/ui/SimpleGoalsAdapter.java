package ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.workonit.R;
import com.example.workonit.model.Goal;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A super-simple adapter for FilteredGoalsActivity.
 * - No headers, no grouping, no filtering — it just shows the list it’s given.
 * - Keeps the same row UI and 3‑dot actions as your main adapter.
 */
public class SimpleGoalsAdapter extends RecyclerView.Adapter<SimpleGoalsAdapter.VH> {

    public interface OnGoalClick { void onGoalClick(Goal goal); }
    public interface OnGoalAction {
        void onActivate(Goal goal);
        void onHold(Goal goal);
        void onComplete(Goal goal);
        void onExpire(Goal goal);
        void onDelete(Goal goal);
    }

    private final List<Goal> items = new ArrayList<>();
    private OnGoalClick onGoalClick;
    private OnGoalAction onGoalAction;

    public void setOnGoalClick(OnGoalClick listener) { this.onGoalClick = listener; }
    public void setOnGoalAction(OnGoalAction listener) { this.onGoalAction = listener; }

    public void submitList(List<Goal> goals) {
        items.clear();
        if (goals != null) items.addAll(goals);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_goal, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Goal g = items.get(position);
        final Context ctx = h.itemView.getContext();

        // title
        h.tvTitle.setText(g.name);

        // meta
        String typeText = (g.type == Goal.Type.POSITIVE) ? "positive" : "negative";
        String freqText = (g.timesPerWeek > 0) ? (g.timesPerWeek + "/week") : "no target yet";
        String dateText = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(g.createdAtUtc));
        h.tvMeta.setText(typeText + " • " + freqText + " • created " + dateText);

        // status pill + card stroke
        Goal.Status status = (g.status != null) ? g.status : Goal.Status.ACTIVE;
        h.tvStatusPill.setText(status.name());
        @ColorInt int color = statusColor(ctx, status);
        h.tvStatusPill.getBackground().setTint(color);
        h.card.setStrokeColor(color);
        h.card.setStrokeWidth(dp(ctx, 2));
        h.card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.goal_card_bg));

        // tap → open details (optional)
        h.itemView.setOnClickListener(v -> {
            if (onGoalClick != null) onGoalClick.onGoalClick(g);
        });

        // 3‑dot menu
        h.btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(ctx, h.btnMore);
            MenuInflater inflater = popup.getMenuInflater();
            inflater.inflate(R.menu.menu_goal_row, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (onGoalAction == null) return false;
                int id = item.getItemId();
                if (id == R.id.action_activate) { onGoalAction.onActivate(g); return true; }
                if (id == R.id.action_hold)     { onGoalAction.onHold(g);     return true; }
                if (id == R.id.action_complete) { onGoalAction.onComplete(g); return true; }
                if (id == R.id.action_expire)   { onGoalAction.onExpire(g);   return true; }
                if (id == R.id.action_delete)   { onGoalAction.onDelete(g);   return true; }
                return false;
            });
            popup.show();
        });
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvTitle;
        final TextView tvMeta;
        final TextView tvProgressHint;
        final TextView tvStatusPill;
        final ImageButton btnMore;
        VH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_goal);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvProgressHint = itemView.findViewById(R.id.tv_progress_hint);
            tvStatusPill = itemView.findViewById(R.id.tv_status_pill);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }

    // helpers
    private static int dp(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    @ColorInt
    private static int statusColor(Context ctx, Goal.Status s) {
        switch (s) {
            case ON_HOLD:   return ContextCompat.getColor(ctx, R.color.goal_on_hold);
            case COMPLETED: return ContextCompat.getColor(ctx, R.color.goal_completed);
            case EXPIRED:   return ContextCompat.getColor(ctx, R.color.goal_expired);
            case ACTIVE:
            default:        return ContextCompat.getColor(ctx, R.color.goal_active);
        }
    }
}