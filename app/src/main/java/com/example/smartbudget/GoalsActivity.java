//  File: app/src/main/java/com/example/smartbudget/GoalsActivity.java
package com.example.smartbudget;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartbudget.databinding.ActivityGoalsBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;


public class GoalsActivity extends AppCompatActivity
        implements GoalAdapter.Listener {

    public static final String EXTRA_GOALS         = "extra_goals";
    public static final String EXTRA_TOTAL_BALANCE = "extra_total_balance";

    private static final String KEY_TOT = "bal_total";
    private static final String KEY_NEC = "bal_nec";
    private static final String KEY_ENT = "bal_ent";
    private static final String KEY_LT  = "bal_lt";
    private static final String KEY_EDU = "bal_edu";
    private static final String KEY_INV = "bal_inv";
    private static final String KEY_CHA = "bal_cha";

    private static final String[] JAR_KEYS = {
            KEY_NEC, KEY_ENT, KEY_LT, KEY_EDU, KEY_INV, KEY_CHA
    };

    private ActivityGoalsBinding     binding;
    private final ArrayList<Goal>    goals = new ArrayList<>();
    private double                   totalBalance = 0;
    private GoalAdapter              adapter;
    private ActivityResultLauncher<Intent> addGoalLauncher;

    private final NumberFormat moneyFmt =
            NumberFormat.getCurrencyInstance(new Locale("ru","RU"));

    //───────────────────────────────────────────────────────────────
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGoalsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        goals.addAll(PrefsHelper.loadGoals(this));

        @SuppressWarnings("unchecked")
        ArrayList<Goal> list =
                (ArrayList<Goal>) getIntent().getSerializableExtra(EXTRA_GOALS);
        if (list != null) {
            goals.clear();
            goals.addAll(list);
        }

        if (getIntent().hasExtra(EXTRA_TOTAL_BALANCE)) {
            totalBalance = getIntent().getDoubleExtra(EXTRA_TOTAL_BALANCE, 0);
        } else {
            totalBalance = loadTotalBalance();
        }

        adapter = new GoalAdapter(goals, this);
        binding.rvGoals.setLayoutManager(new LinearLayoutManager(this));
        binding.rvGoals.setAdapter(adapter);
        updateEmptyState();

        binding.fabAddGoal.setOnClickListener(v -> launchAddGoal());

        addGoalLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getResultCode() == RESULT_OK && res.getData() != null) {
                        Goal g = (Goal) res.getData()
                                .getSerializableExtra(AddGoalActivity.EXTRA_GOAL);
                        if (g != null) {
                            goals.add(g);
                            saveAndRefresh();
                        }
                    }
                });

        setupBottomNav();
    }

    //───────────────────────────────────────────────────────────────
    /** Нижняя панель навигации */
    private void setupBottomNav() {
        binding.bottomNav.setSelectedItemId(R.id.nav_goals);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                PrefsHelper.saveGoals(this, goals);
                saveTotalBalance();

                Intent home = new Intent(this, MainActivity.class);
                home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(home);

                finish();
                return true;

            } else if (id == R.id.nav_stats) {
                PrefsHelper.saveGoals(this, goals);
                saveTotalBalance();

                Intent s = new Intent(this, StatisticsActivity.class);
                s.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(s);
                finish();
                return true;

            } else if (id == R.id.nav_goals) {
                return true;

            } else if (id == R.id.nav_settings) {
                PrefsHelper.saveGoals(this, goals);
                saveTotalBalance();

                Intent set = new Intent(this, SettingsActivity.class);
                set.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(set);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_goals);
    }

    //───────────────────────────────────────────────────────────────
    /** Запуск AddGoalActivity */
    private void launchAddGoal() {
        addGoalLauncher.launch(new Intent(this, AddGoalActivity.class));
    }

    //────────────────────────── callbacks адаптера ────────────────
    @Override public void onFundClicked(Goal goal) {

        /* ── Шаг 1: ввод суммы ── */
        TextInputLayout input = (TextInputLayout) getLayoutInflater()
                .inflate(R.layout.dialog_input_amount, null);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Пополнить «" + goal.name + "»")
                .setView(input)
                .setPositiveButton("Далее", (d,w)->{

                    String txt = input.getEditText().getText()
                            .toString().replace(',','.').trim();

                    double sum;
                    try { sum = Double.parseDouble(txt); }
                    catch (NumberFormatException e){ sum = -1; }

                    if (sum<=0) {
                        Toast.makeText(this,"Введите корректную сумму",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (sum>totalBalance) {
                        Toast.makeText(this,"Недостаточно средств",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    /* ── Шаг 2: выбор кувшина-донора ── */
                    showJarPicker(goal, sum);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /** Диалог выбора кувшина-донора и само списание средств. */
    private void showJarPicker(Goal goal, double sum) {

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        /* подготовка меток с текущими остатками */
        String[] labels = new String[Category.values().length];
        double[] balances = new double[labels.length];

        int i = 0;
        for (Category c : Category.values()) {
            double bal = Double.longBitsToDouble(sp.getLong(JAR_KEYS[i], 0));
            balances[i] = bal;
            labels[i]   = getTitle(c) + " — " + moneyFmt.format(bal);
            i++;
        }

        final int[] chosen = {0};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Из какого кувшина списать?")
                .setSingleChoiceItems(labels, 0, (dlg, which) -> chosen[0] = which)
                .setPositiveButton("Списать", (dlg,w)->{

                    int idx = chosen[0];
                    double jarBal = balances[idx];

                    if (sum > jarBal) {
                        Toast.makeText(this,
                                "Недостаточно средств в выбранном кувшине",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    /* списываем */
                    jarBal       -= sum;
                    totalBalance -= sum;
                    goal.savedAmount += sum;
                    if (goal.savedAmount >= goal.targetAmount)
                        goal.completed = true;

                    /* сохраняем SharedPreferences */
                    sp.edit()
                            .putLong(JAR_KEYS[idx], Double.doubleToRawLongBits(jarBal))
                            .putLong(KEY_TOT,        Double.doubleToRawLongBits(totalBalance))
                            .apply();

                    /* сохраняем цели и обновляем UI */
                    saveAndRefresh();

                    Toast.makeText(this,
                            "Пополнено на " + moneyFmt.format(sum),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    //───────────────────────────────────────────────────────────────
    @Override public void onLongClicked(Goal goal) {

        CharSequence[] ops = {"Завершить цель","Удалить цель","Отмена"};

        new AlertDialog.Builder(this)
                .setTitle(goal.name)
                .setItems(ops,(d,i)->{
                    if (i==0){                           // завершить
                        goal.completed = true;
                        goal.savedAmount = goal.targetAmount;
                        saveAndRefresh();

                    } else if (i==1){                    // удалить
                        int pos = goals.indexOf(goal);
                        if (pos>=0){
                            goals.remove(pos);
                            saveAndRefresh();
                        }
                    }
                }).show();
    }

    private void saveAndRefresh() {
        adapter.notifyDataSetChanged();
        updateEmptyState();
        PrefsHelper.saveGoals(this, goals);
        saveTotalBalance();              // фиксируем общий баланс
    }

    private void updateEmptyState() {
        binding.tvEmptyState.setVisibility(
                goals.isEmpty() ? View.VISIBLE : View.GONE);
    }

    //───────────────────────────────────────────────────────────────
    /** Загружает общий баланс из SharedPreferences */
    private double loadTotalBalance() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return Double.longBitsToDouble(sp.getLong(KEY_TOT, 0));
    }

    /** Сохраняет общий баланс в SharedPreferences */
    private void saveTotalBalance() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putLong(KEY_TOT, Double.doubleToRawLongBits(totalBalance))
                .apply();
    }

    private String getTitle(Category c) {
        switch (c) {
            case NECESSARY:     return "Необходимые";
            case ENTERTAINMENT: return "Развлечения";
            case LONGTERM:      return "Долгосрочные";
            case EDUCATION:     return "Образование";
            case INVESTMENTS:   return "Инвестиции";
            case CHARITY:       return "Благотворительность";
            default:            return "";
        }
    }
}
