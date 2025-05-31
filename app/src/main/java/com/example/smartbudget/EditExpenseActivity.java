
package com.example.smartbudget;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartbudget.databinding.ActivityEditExpenseBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;


public class EditExpenseActivity extends AppCompatActivity
        implements HistoryAdapter.Listener {

    /* ── ключи Intent ── */
    public static final String EXTRA_CATEGORY       = "extra_category";
    public static final String EXTRA_CURRENT        = "extra_current";
    public static final String EXTRA_EXPENSE        = "extra_expense";       // (старый путь)
    public static final String EXTRA_EXPENSE_DELTA  = "extra_expense_delta"; // новый: +/- сумма
    public static final String EXTRA_HISTORY        = "extra_history";

    /* ── UI ── */
    private ActivityEditExpenseBinding binding;

    /* ── состояние ── */
    private Category category;
    private double   currentBalance;          // остаток в кувшине
    private final ArrayList<ExpenseEntry> history = new ArrayList<>();
    private HistoryAdapter adapter;

    private double totalDelta = 0;            // суммарное изменение расходов

    /* ── формат ── */
    private final NumberFormat moneyFmt =
            NumberFormat.getCurrencyInstance(new Locale("ru","RU"));

    //───────────────────────────────────────────────────────────────
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        binding = ActivityEditExpenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* входящие данные */
        category       = (Category) getIntent().getSerializableExtra(EXTRA_CATEGORY);
        currentBalance = getIntent().getDoubleExtra(EXTRA_CURRENT, 0);

        @SuppressWarnings("unchecked")
        ArrayList<ExpenseEntry> in =
                (ArrayList<ExpenseEntry>) getIntent().getSerializableExtra(EXTRA_HISTORY);
        if (in != null) history.addAll(in);

        /* сортируем: новые сверху */
        Collections.sort(history, (a,b) -> Long.compare(b.timestamp, a.timestamp));

        binding.tvCategoryName.setText(getTitle(category));
        updateBalanceLabel();

        /* история */
        adapter = new HistoryAdapter(history, this);
        binding.rvHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.rvHistory.setAdapter(adapter);

        /* добавить новый расход */
        binding.btnSaveExpense.setOnClickListener(v -> addNewExpense());

        /* перехват системного «Назад» */
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { sendResultAndFinish(); }
        });
    }

    private void addNewExpense() {

        String txt = binding.etExpense.getText()
                .toString().replace(',','.').trim();

        double sum;
        try { sum = Double.parseDouble(txt); }
        catch (NumberFormatException e) { sum = -1; }

        if (sum<=0) { toast("Введите корректную сумму (>0)"); return; }
        if (sum>currentBalance) { toast("Расход превышает остаток"); return; }

        history.add(0, new ExpenseEntry(sum, System.currentTimeMillis()));
        adapter.notifyItemInserted(0);

        currentBalance -= sum;
        totalDelta     += sum;

        updateBalanceLabel();
        binding.etExpense.getText().clear();
    }


    @Override public void onItemClick(ExpenseEntry entry, int pos) {
        TextInputLayout input = (TextInputLayout) getLayoutInflater()
                .inflate(R.layout.dialog_input_amount, null);
        input.getEditText().setText(String.valueOf(entry.amount));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Изменить расход")
                .setView(input)
                .setPositiveButton("Сохранить", (d,w)->{

                    String t = input.getEditText().getText().toString()
                            .replace(',','.').trim();
                    double newAmt;
                    try { newAmt = Double.parseDouble(t); }
                    catch (Exception e){ newAmt = -1; }

                    if (newAmt<=0) { toast("Введите корректную сумму"); return; }

                    double diff = newAmt - entry.amount;             // >0 – растёт
                    if (diff>0 && diff>currentBalance) {
                        toast("Недостаточно средств"); return;
                    }

                    history.set(pos, new ExpenseEntry(newAmt, entry.timestamp));
                    adapter.notifyItemChanged(pos);

                    currentBalance -= diff;
                    totalDelta     += diff;
                    updateBalanceLabel();

                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    @Override public void onItemLongClick(ExpenseEntry entry, int pos) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Удалить расход " + moneyFmt.format(entry.amount) + "?")
                .setPositiveButton("Удалить", (d,w)->{

                    history.remove(pos);
                    adapter.notifyItemRemoved(pos);

                    currentBalance += entry.amount;
                    totalDelta     -= entry.amount;

                    updateBalanceLabel();
                    toast("Удалено");
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    //───────────────────────────────────────────────────────────────
    private void updateBalanceLabel() {
        binding.tvCurrentBalance.setText("Остаток: " + moneyFmt.format(currentBalance));
    }

    private void sendResultAndFinish() {
        Intent out = new Intent();
        out.putExtra(EXTRA_CATEGORY,       category);
        out.putExtra(EXTRA_EXPENSE_DELTA,  totalDelta);
        out.putExtra(EXTRA_HISTORY,        history);
        setResult(RESULT_OK, out);
        finish();
    }

    private void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }

    private String getTitle(Category c){
        switch (c){
            case NECESSARY:     return "Необходимые расходы";
            case ENTERTAINMENT: return "Развлечения";
            case LONGTERM:      return "Долгосрочные накопления";
            case EDUCATION:     return "Образование";
            case INVESTMENTS:   return "Инвестиции и накопления";
            case CHARITY:       return "Благотворительность";
            default:            return "";
        }
    }
}
