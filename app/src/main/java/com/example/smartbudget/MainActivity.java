
package com.example.smartbudget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.example.smartbudget.databinding.ActivityMainBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    /* ───── ViewBinding ───── */
    private ActivityMainBinding binding;

    /* ───── Балансы 6 кувшинов ───── */
    private double totalBalance,
            necessaryBalance,
            entertainmentBalance,
            longTermBalance,
            educationBalance,
            investmentsBalance,
            charityBalance;

    /* ───── SharedPreferences ───── */
    private SharedPreferences prefs;
    private static final String KEY_TOT = "bal_total";
    private static final String KEY_NEC = "bal_nec";
    private static final String KEY_ENT = "bal_ent";
    private static final String KEY_LT  = "bal_lt";
    private static final String KEY_EDU = "bal_edu";
    private static final String KEY_INV = "bal_inv";
    private static final String KEY_CHA = "bal_cha";

    /* ───── История расходов (persisted) ───── */
    private final Map<Category, ArrayList<ExpenseEntry>> histories =
            new EnumMap<>(Category.class);

    /* ───── Цели ───── */
    private final ArrayList<Goal> goals = new ArrayList<>();

    /* ───── Формат валюты ───── */
    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));

    /* ───── Навигационный флаг от SettingsActivity ───── */
    public static final String EXTRA_NAV_TO_STATS = "extra_nav_to_stats";

    /* ───── Activity Result API ───── */
    private ActivityResultLauncher<Intent> expenseLauncher;
    private ActivityResultLauncher<Intent> goalsLauncher;

    //────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* --- 0. онбординг при первом запуске --- */
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("onboarding_complete", false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* --- 1. баланс --- */
        reloadBalancesFromPrefs();

        /* --- 2. цели --- */
        goals.addAll(PrefsHelper.loadGoals(this));

        /* --- 3. история расходов (persist) --- */
        histories.clear();
        histories.putAll(PrefsHelper.loadHistories(this));

        updateUiBalances();
        binding.bottomNav.setSelectedItemId(R.id.nav_home);

        /* ───────── обработчики кнопок ───────── */

        binding.btnDistribute.setOnClickListener(v -> distributeIncome());

        binding.btnEditNecessary    .setOnClickListener(
                v -> launchEditScreen(Category.NECESSARY,     necessaryBalance));
        binding.btnEditEntertainment.setOnClickListener(
                v -> launchEditScreen(Category.ENTERTAINMENT, entertainmentBalance));
        binding.btnEditLongterm     .setOnClickListener(
                v -> launchEditScreen(Category.LONGTERM,      longTermBalance));
        binding.btnEditEducation    .setOnClickListener(
                v -> launchEditScreen(Category.EDUCATION,     educationBalance));
        binding.btnEditInvestments  .setOnClickListener(
                v -> launchEditScreen(Category.INVESTMENTS,   investmentsBalance));
        binding.btnEditCharity      .setOnClickListener(
                v -> launchEditScreen(Category.CHARITY,       charityBalance));

        /* результаты дочерних активити */
        expenseLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getResultCode() == RESULT_OK && res.getData() != null)
                        handleExpenseResult(res.getData());
                });

        goalsLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                res -> {
                    if (res.getResultCode() == RESULT_OK && res.getData() != null) {
                        @SuppressWarnings("unchecked")
                        ArrayList<Goal> list =
                                (ArrayList<Goal>) res.getData()
                                        .getSerializableExtra(GoalsActivity.EXTRA_GOALS);
                        if (list != null) {
                            goals.clear();
                            goals.addAll(list);
                            PrefsHelper.saveGoals(this, goals);
                        }
                        reloadBalancesFromPrefs();
                        updateUiBalances();
                    }
                });

        /* ───────── нижняя навигация ───────── */
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_stats) {
                launchStatisticsScreen();  return true;
            } else if (id == R.id.nav_goals) {
                launchGoalsScreen();       return true;
            } else if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });

        /* deep-link из SettingsActivity? */
        handleDeepLink(getIntent());
    }

    /** повторные интенты (FLAG_ACTIVITY_SINGLE_TOP) */
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_NAV_TO_STATS, false)) {
            intent.removeExtra(EXTRA_NAV_TO_STATS);
            launchStatisticsScreen();
        }
    }

    //────────────────────────────────────────────────────────────────
    @Override protected void onResume() {
        super.onResume();
        reloadBalancesFromPrefs();
        updateUiBalances();
        binding.bottomNav.setSelectedItemId(R.id.nav_home);
    }

    /* ------------------- BALANCES ------------------- */

    private void reloadBalancesFromPrefs() {
        totalBalance         = Double.longBitsToDouble(prefs.getLong(KEY_TOT, 0));
        necessaryBalance     = Double.longBitsToDouble(prefs.getLong(KEY_NEC, 0));
        entertainmentBalance = Double.longBitsToDouble(prefs.getLong(KEY_ENT, 0));
        longTermBalance      = Double.longBitsToDouble(prefs.getLong(KEY_LT , 0));
        educationBalance     = Double.longBitsToDouble(prefs.getLong(KEY_EDU, 0));
        investmentsBalance   = Double.longBitsToDouble(prefs.getLong(KEY_INV, 0));
        charityBalance       = Double.longBitsToDouble(prefs.getLong(KEY_CHA, 0));
    }

    private void saveBalances() {
        SharedPreferences.Editor ed = prefs.edit();
        ed.putLong(KEY_TOT, Double.doubleToRawLongBits(totalBalance));
        ed.putLong(KEY_NEC, Double.doubleToRawLongBits(necessaryBalance));
        ed.putLong(KEY_ENT, Double.doubleToRawLongBits(entertainmentBalance));
        ed.putLong(KEY_LT,  Double.doubleToRawLongBits(longTermBalance));
        ed.putLong(KEY_EDU, Double.doubleToRawLongBits(educationBalance));
        ed.putLong(KEY_INV, Double.doubleToRawLongBits(investmentsBalance));
        ed.putLong(KEY_CHA, Double.doubleToRawLongBits(charityBalance));
        ed.apply();
    }

    /* ------------------- ДОХОД ------------------- */

    private void distributeIncome() {
        String txt = binding.etIncome.getText().toString().replace(',', '.').trim();

        double income;
        try { income = Double.parseDouble(txt); }
        catch (NumberFormatException e) { income = -1; }

        if (income <= 0) {
            Toast.makeText(this, "Введите корректную сумму (>0)", Toast.LENGTH_SHORT).show();
            return;
        }

        double pctNec = parsePct(prefs.getString("pref_pct_necessary",     "55"));
        double pctEnt = parsePct(prefs.getString("pref_pct_entertainment", "10"));
        double pctLT  = parsePct(prefs.getString("pref_pct_longterm",      "10"));
        double pctEdu = parsePct(prefs.getString("pref_pct_education",     "10"));
        double pctInv = parsePct(prefs.getString("pref_pct_investments",   "10"));
        double pctCha = parsePct(prefs.getString("pref_pct_charity",       "5"));

        double sumPct = pctNec + pctEnt + pctLT + pctEdu + pctInv + pctCha;
        if (Math.abs(sumPct - 100) > 0.1) {
            Toast.makeText(this, "Сумма процентов ≠ 100 — проверьте настройки",
                    Toast.LENGTH_LONG).show();
            return;
        }

        double incNec = income * pctNec / 100.0;
        double incEnt = income * pctEnt / 100.0;
        double incLT  = income * pctLT  / 100.0;
        double incEdu = income * pctEdu / 100.0;
        double incInv = income * pctInv / 100.0;
        double incCha = income * pctCha / 100.0;

        totalBalance         += income;
        necessaryBalance     += incNec;
        entertainmentBalance += incEnt;
        longTermBalance      += incLT;
        educationBalance     += incEdu;
        investmentsBalance   += incInv;
        charityBalance       += incCha;

        updateUiBalances();
        saveBalances();
        binding.etIncome.getText().clear();
    }

    private double parsePct(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0; }
    }

    /* ------------------- РЕДАКТИРОВАНИЕ РАСХОДОВ ------------------- */

    private void launchEditScreen(Category c, double current) {
        Intent i = new Intent(this, EditExpenseActivity.class);
        i.putExtra(EditExpenseActivity.EXTRA_CATEGORY, c);
        i.putExtra(EditExpenseActivity.EXTRA_CURRENT,  current);
        i.putExtra(EditExpenseActivity.EXTRA_HISTORY, histories.get(c));
        expenseLauncher.launch(i);
    }

    private void handleExpenseResult(Intent data) {

        Category c = (Category) data.getSerializableExtra(EditExpenseActivity.EXTRA_CATEGORY);

        /* 1. новая история кувшина */
        @SuppressWarnings("unchecked")
        ArrayList<ExpenseEntry> updated =
                (ArrayList<ExpenseEntry>) data.getSerializableExtra(EditExpenseActivity.EXTRA_HISTORY);
        if (updated != null) histories.put(c, updated);

        /* 2. дельта расходов */
        double delta = data.hasExtra(EditExpenseActivity.EXTRA_EXPENSE_DELTA)
                ? data.getDoubleExtra(EditExpenseActivity.EXTRA_EXPENSE_DELTA, 0)
                : data.getDoubleExtra(EditExpenseActivity.EXTRA_EXPENSE, 0);

        if (Math.abs(delta) < 0.01) return;

        switch (c) {
            case NECESSARY:     necessaryBalance     -= delta; break;
            case ENTERTAINMENT: entertainmentBalance -= delta; break;
            case LONGTERM:      longTermBalance      -= delta; break;
            case EDUCATION:     educationBalance     -= delta; break;
            case INVESTMENTS:   investmentsBalance   -= delta; break;
            case CHARITY:       charityBalance       -= delta; break;
        }
        totalBalance -= delta;

        updateUiBalances();
        saveBalances();

        /* 3. сохраняем обновлённую историю расходов */
        PrefsHelper.saveHistories(this, histories);
    }

    /* ------------------- ОБНОВЛЕНИЕ UI ------------------- */

    private void updateUiBalances() {

        binding.tvTotalBalance.setText("Общий баланс: " + currency.format(totalBalance));

        setText(binding.tvAmountNecessary,     necessaryBalance);
        setText(binding.tvAmountEntertainment, entertainmentBalance);
        setText(binding.tvAmountLongterm,      longTermBalance);
        setText(binding.tvAmountEducation,     educationBalance);
        setText(binding.tvAmountInvestments,   investmentsBalance);
        setText(binding.tvAmountCharity,       charityBalance);

        updateJarLabels();
    }

    private void setText(TextView tv, double v) { tv.setText(currency.format(v)); }

    private void updateJarLabels() {

        int pctNec = (int) parsePct(prefs.getString("pref_pct_necessary",     "55"));
        int pctEnt = (int) parsePct(prefs.getString("pref_pct_entertainment", "10"));
        int pctLT  = (int) parsePct(prefs.getString("pref_pct_longterm",      "10"));
        int pctEdu = (int) parsePct(prefs.getString("pref_pct_education",     "10"));
        int pctInv = (int) parsePct(prefs.getString("pref_pct_investments",   "10"));
        int pctCha = (int) parsePct(prefs.getString("pref_pct_charity",       "5"));

        binding.tvLabelNecessary    .setText("Необходимые расходы ("      + pctNec + "%)");
        binding.tvLabelEntertainment.setText("Развлечения ("              + pctEnt + "%)");
        binding.tvLabelLongterm     .setText("Долгосрочные накопления ("   + pctLT  + "%)");
        binding.tvLabelEducation    .setText("Образование ("               + pctEdu + "%)");
        binding.tvLabelInvestments  .setText("Инвестиции и накопления ("   + pctInv + "%)");
        binding.tvLabelCharity      .setText("Благотворительность ("       + pctCha + "%)");
    }

    /* ------------------- НАВИГАЦИЯ ------------------- */

    private void launchStatisticsScreen() {
        Intent i = new Intent(this, StatisticsActivity.class);
        i.putExtra(StatisticsActivity.EXTRA_HISTORIES, new EnumMap<>(histories));
        startActivity(i);
    }

    private void launchGoalsScreen() {
        goals.clear();
        goals.addAll(PrefsHelper.loadGoals(this));

        Intent i = new Intent(this, GoalsActivity.class);
        i.putExtra(GoalsActivity.EXTRA_GOALS, new ArrayList<>(goals));
        i.putExtra(GoalsActivity.EXTRA_TOTAL_BALANCE, totalBalance);
        goalsLauncher.launch(i);
    }
}
