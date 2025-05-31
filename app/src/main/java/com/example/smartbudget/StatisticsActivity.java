//  File: app/src/main/java/com/example/smartbudget/StatisticsActivity.java
package com.example.smartbudget;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.example.smartbudget.databinding.ActivityStatisticsBinding;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Экран «Статистика расходов».
 * Исправление: если через Intent не передана история расходов,
 *              берём актуальные данные из SharedPreferences.
 */
public class StatisticsActivity extends AppCompatActivity {

    public static final String EXTRA_HISTORIES = "extra_histories";

    /* ─── view binding ─── */
    private ActivityStatisticsBinding binding;

    /* ─── данные ─── */
    private Map<Category, ArrayList<ExpenseEntry>> histories;

    /* ─── выбранный диапазон (по умолчанию — весь период) ─── */
    private long rangeStart = Long.MIN_VALUE;
    private long rangeEnd   = Long.MAX_VALUE;

    /* ─── форматирование ─── */
    private final NumberFormat moneyFmt =
            NumberFormat.getCurrencyInstance(new Locale("ru", "RU"));
    private final DateFormat   dateFmt  =
            DateFormat.getDateInstance(DateFormat.MEDIUM, new Locale("ru", "RU"));

    //───────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        /* 1. Нижняя навигационная панель */
        setupBottomNavigation();

        /* 2. Загружаем историю расходов.
              Сначала читаем SharedPreferences как источник "по умолчанию",
              затем (если есть) перекрываем их данными из Intent. */
        histories = new EnumMap<>(Category.class);
        histories.putAll(PrefsHelper.loadHistories(this));          // ← всегда что-то есть

        //noinspection unchecked
        Map<Category, ArrayList<ExpenseEntry>> extra =
                (Map<Category, ArrayList<ExpenseEntry>>) getIntent()
                        .getSerializableExtra(EXTRA_HISTORIES);
        if (extra != null) {
            histories.clear();
            histories.putAll(extra);
        }

        /* 3. Кнопка выбора диапазона дат */
        binding.btnPickRange.setOnClickListener(v -> showDateRangePicker());

        /* 4. Первая отрисовка статистики (весь период) */
        updateStatistics();
    }

    //───────────────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        binding.bottomNav.setSelectedItemId(R.id.nav_stats);
    }

    //───────────────────────────────────────────────────────────────
    /** Диалог выбора диапазона дат */
    private void showDateRangePicker() {

        MaterialDatePicker<Pair<Long, Long>> picker =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Выберите период")
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                rangeStart = selection.first;
                rangeEnd   = selection.second + 86_399_999L;   // +23:59:59

                String caption = dateFmt.format(rangeStart) + " – "
                        + dateFmt.format(selection.second);
                binding.btnPickRange.setText(caption);

                updateStatistics();
            }
        });

        picker.addOnNegativeButtonClickListener(v -> {
            rangeStart = Long.MIN_VALUE;
            rangeEnd   = Long.MAX_VALUE;
            binding.btnPickRange.setText("Весь период");
            updateStatistics();
        });

        picker.show(getSupportFragmentManager(), "date_range");
    }

    //───────────────────────────────────────────────────────────────
    /** Пересчёт сумм и обновление UI */
    private void updateStatistics() {

        double totalSpent = 0;
        Map<Category, Double> spentPerCat = new EnumMap<>(Category.class);

        for (Category c : Category.values()) {
            double sum = 0;
            ArrayList<ExpenseEntry> list = histories.get(c);
            if (list != null) {
                for (ExpenseEntry e : list) {
                    boolean inRange = (e.timestamp >= rangeStart)
                            && (e.timestamp <= rangeEnd);
                    if (inRange) sum += e.amount;
                }
            }
            spentPerCat.put(c, sum);
            totalSpent += sum;
        }

        setupPieChart(binding.pieChart, spentPerCat);
        binding.tvTotalSpent.setText("Всего потрачено: " + moneyFmt.format(totalSpent));
    }

    //───────────────────────────────────────────────────────────────
    /** Построение круговой диаграммы */
    private void setupPieChart(PieChart chart, Map<Category, Double> data) {

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer>  colors  = new ArrayList<>();

        for (Category c : Category.values()) {
            float v = data.get(c).floatValue();
            if (v > 0) {
                entries.add(new PieEntry(v, getTitle(c)));
                colors.add(getColorFor(c));
            }
        }

        PieDataSet ds = new PieDataSet(entries, "");
        ds.setSliceSpace(2f);
        ds.setValueTextSize(12f);

        /* читаем актуальный colorOnSurface (он разный в light/dark) */
        int onSurface = MaterialColors.getColor(
                chart, com.google.android.material.R.attr.colorOnSurface, Color.WHITE);

        ds.setValueTextColor(onSurface);
        ds.setColors(colors);

        chart.setData(new PieData(ds));
        chart.setUsePercentValues(true);
        chart.getDescription().setEnabled(false);
        chart.setEntryLabelColor(onSurface);
        chart.invalidate();

        Legend legend = chart.getLegend();
        legend.setWordWrapEnabled(true);
        legend.setTextSize(12f);
        legend.setTextColor(onSurface);
    }

    //───────────────────────────────────────────────────────────────
    /** Нижняя навигационная панель */
    private void setupBottomNavigation() {
        binding.bottomNav.setSelectedItemId(R.id.nav_stats);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
                finish();
                return true;

            } else if (id == R.id.nav_stats) {
                return true;

            } else if (id == R.id.nav_goals) {
                Intent g = new Intent(this, GoalsActivity.class);
                g.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(g);
                return true;

            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    //───────────────────────────────────────────────────────────────
    /** Цвет, закреплённый за каждой категорией */
    @ColorInt
    private int getColorFor(Category c) {
        switch (c) {
            case NECESSARY:     return ContextCompat.getColor(this, R.color.cat_necessary);
            case ENTERTAINMENT: return ContextCompat.getColor(this, R.color.cat_entertainment);
            case LONGTERM:      return ContextCompat.getColor(this, R.color.cat_longterm);
            case EDUCATION:     return ContextCompat.getColor(this, R.color.cat_education);
            case INVESTMENTS:   return ContextCompat.getColor(this, R.color.cat_investments);
            case CHARITY:       return ContextCompat.getColor(this, R.color.cat_charity);
            default:            return Color.GRAY;
        }
    }

    /** Человекочитаемое название категории */
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
