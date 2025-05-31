
package com.example.smartbudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartbudget.databinding.GoalItemBinding;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.VH> {

    public interface Listener {
        void onFundClicked(Goal goal);
        void onLongClicked(Goal goal);
    }

    private final ArrayList<Goal> goals;
    private final Listener        listener;

    private final NumberFormat moneyFmt =
            NumberFormat.getCurrencyInstance(new Locale("ru","RU"));
    private final DateFormat   dateFmt  =
            DateFormat.getDateInstance(DateFormat.SHORT, new Locale("ru","RU"));

    public GoalAdapter(ArrayList<Goal> goals, Listener listener) {
        this.goals    = goals;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        GoalItemBinding b = GoalItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new VH(b);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Goal g = goals.get(pos);

        h.tvName.setText(g.name);
        h.tvProgress.setText(moneyFmt.format(g.savedAmount) + " / "
                + moneyFmt.format(g.targetAmount));

        int percent = (int)(g.getProgress() * 100);
        h.tvPercent.setText("(" + percent + "%)");
        h.bar.setProgress(percent);

        if (g.dueDate > 0)
            h.tvDue.setText("До " + dateFmt.format(g.dueDate));
        else
            h.tvDue.setText("Срок не задан");

        if (g.completed) {
            h.bar.setIndicatorColor(ContextCompat.getColor(
                    h.itemView.getContext(), R.color.cat_investments));
            h.btnFund.setEnabled(false);
            h.itemView.setAlpha(0.5f);
        } else {
            h.bar.setIndicatorColor(ContextCompat.getColor(
                    h.itemView.getContext(), R.color.md_theme_primary));
            h.btnFund.setEnabled(true);
            h.itemView.setAlpha(1f);
        }

        h.btnFund.setOnClickListener(v -> listener.onFundClicked(g));
        h.itemView.setOnLongClickListener(v -> { listener.onLongClicked(g); return true; });
    }

    @Override public int getItemCount() { return goals.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvName, tvProgress, tvPercent, tvDue;
        final LinearProgressIndicator bar;
        final View btnFund;
        VH(GoalItemBinding b) {
            super(b.getRoot());
            tvName    = b.tvGoalName;
            tvProgress= b.tvGoalProgress;
            tvPercent = b.tvGoalPercent;
            tvDue     = b.tvGoalDue;
            bar       = b.progressBar;
            btnFund   = b.btnFund;
        }
    }
}
