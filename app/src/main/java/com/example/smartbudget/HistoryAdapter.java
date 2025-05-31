//  File: app/src/main/java/com/example/smartbudget/HistoryAdapter.java
package com.example.smartbudget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;


public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

    public interface Listener {
        void onItemClick(ExpenseEntry entry, int position);
        void onItemLongClick(ExpenseEntry entry, int position);
    }

    private final List<ExpenseEntry> history;
    private final Listener           listener;

    private final NumberFormat moneyFmt =
            NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
    private final DateFormat dateFmt =
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT,
                    new Locale("ru", "RU"));

    public HistoryAdapter(List<ExpenseEntry> history, Listener listener) {
        this.history  = history;
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View item = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new VH(item);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ExpenseEntry e = history.get(pos);
        h.date  .setText(dateFmt.format(e.timestamp));
        h.amount.setText(moneyFmt.format(e.amount));

        if (listener != null) {
            h.itemView.setOnClickListener(
                    v -> listener.onItemClick(e, pos));
            h.itemView.setOnLongClickListener(v -> {
                listener.onItemLongClick(e, pos);
                return true;
            });
        }
    }

    @Override public int getItemCount() { return history.size(); }

    /* ────────────────────────── */
    static class VH extends RecyclerView.ViewHolder {
        final TextView date, amount;
        VH(View item) {
            super(item);
            date   = item.findViewById(android.R.id.text1);
            amount = item.findViewById(android.R.id.text2);
        }
    }
}
