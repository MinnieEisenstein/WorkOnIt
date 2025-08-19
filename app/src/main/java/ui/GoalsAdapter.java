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
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class GoalsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnGoalClick { void onGoalClick(Goal goal); }
    public interface OnGoalAction {
        void onActivate(Goal goal);
        void onHold(Goal goal);
        void onComplete(Goal goal);
        void onExpire(Goal goal);
        void onDelete(Goal goal);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM   = 1;

    // single backing list that mixes headers + items
    private final List<Row> rows = new ArrayList<>();
    private OnGoalClick onGoalClick;
    private OnGoalAction onGoalAction;

    public void setOnGoalClick(OnGoalClick listener) { this.onGoalClick = listener; }
    public void setOnGoalAction(OnGoalAction listener) { this.onGoalAction = listener; }

    /**
     * Submit ALL goals. Adapter will:
     * - keep only ACTIVE + ON_HOLD for main screen
     * - add "Active" header (if any), then "On hold" header (if any)
     * - sort inside each section by created date (newest first)
     */
    public void submitList(List<Goal> goals) {
        rows.clear();
        if (goals == null) {
            notifyDataSetChanged();
            return;
        }

        List<Goal> active = new ArrayList<>();
        List<Goal> onHold = new ArrayList<>();

        for (Goal g : goals) {
            Goal.Status s = (g.status != null) ? g.status : Goal.Status.ACTIVE;
            if (s == Goal.Status.ACTIVE) active.add(g);
            else if (s == Goal.Status.ON_HOLD) onHold.add(g);
            // COMPLETED / EXPIRED are intentionally ignored here (separate screen)
        }

        // newest first inside each section
        Collections.sort(active, (a, b) -> Long.compare(b.createdAtUtc, a.createdAtUtc));
        Collections.sort(onHold, (a, b) -> Long.compare(b.createdAtUtc, a.createdAtUtc));

        if (!active.isEmpty()) {
            rows.add(Row.header("Active"));
            for (Goal g : active) rows.add(Row.item(g));
        }
        if (!onHold.isEmpty()) {
            rows.add(Row.header("On hold"));
            for (Goal g : onHold) rows.add(Row.item(g));
        }

        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) {
        return rows.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
    }

    @NonNull
    @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View v = inf.inflate(R.layout.row_section_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inf.inflate(R.layout.row_goal, parent, false);
            return new ItemVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        Row row = rows.get(position);
        if (row.isHeader) {
            ((HeaderVH) h).bind(row.headerTitle);
        } else {
            ((ItemVH) h).bind(row.goal);
        }
    }

    @Override public int getItemCount() { return rows.size(); }

    // ---- view holders ----

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;
        HeaderVH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.section_title);
        }
        void bind(String t) { title.setText(t); }
    }

    class ItemVH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView tvTitle;
        final TextView tvMeta;
        final TextView tvProgressHint;
        final TextView tvStatusPill;
        final ImageButton btnMore;

        ItemVH(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_goal);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvProgressHint = itemView.findViewById(R.id.tv_progress_hint);
            tvStatusPill = itemView.findViewById(R.id.tv_status_pill);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        void bind(final Goal g) {
            final Context ctx = itemView.getContext();

            // title
            tvTitle.setText(g.name);

            // meta
            String typeText = (g.type == Goal.Type.POSITIVE) ? "positive" : "negative";
            String freqText = (g.timesPerWeek > 0) ? (g.timesPerWeek + "/week") : "no target yet";
            String dateText = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(g.createdAtUtc));
            tvMeta.setText(typeText + " • " + freqText + " • created " + dateText);

            // status pill + card stroke
            Goal.Status status = (g.status != null) ? g.status : Goal.Status.ACTIVE;
            tvStatusPill.setText(status.name());
            @ColorInt int color = statusColor(ctx, status);
            tvStatusPill.getBackground().setTint(color);
            card.setStrokeColor(color);
            card.setStrokeWidth(dp(ctx, 2));
            card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.goal_card_bg));

            // clicks
            itemView.setOnClickListener(v -> {
                if (onGoalClick != null) onGoalClick.onGoalClick(g);
            });

            // 3-dot popup menu
            btnMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(ctx, btnMore);
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
    }

    // ---- row model ----
    private static class Row {
        final boolean isHeader;
        final String headerTitle; // when header
        final Goal goal;          // when item

        private Row(boolean isHeader, String headerTitle, Goal goal) {
            this.isHeader = isHeader;
            this.headerTitle = headerTitle;
            this.goal = goal;
        }

        static Row header(String title) { return new Row(true, title, null); }
        static Row item(Goal g)         { return new Row(false, null, g); }
    }

    // ---- helpers ----
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